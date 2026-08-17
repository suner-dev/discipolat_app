import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

part 'database.g.dart';

// ==================== TABLES ====================

/// Stores souls locally for offline access
@DataClassName('SoulLocal')
class SoulsTable extends Table {
  TextColumn get id => text()();
  TextColumn get nom => text()();
  TextColumn? get prenom => text().nullable()();
  TextColumn? get email => text().nullable()();
  TextColumn? get telephone => text().nullable()();
  TextColumn get typeDisciple => text()();
  TextColumn get statut => text()();
  TextColumn get dateIntegration => text()();
  TextColumn get faiseurId => text()();
  TextColumn? get familleId => text().nullable()();
  TextColumn? get dateDernierContact => text().nullable()();
  TextColumn get lastSyncAt => text()(); // ISO timestamp

  @override
  Set<Column> get primaryKey => {id};
}

/// Stores draft reports that haven't been submitted yet (offline)
@DataClassName('ReportDraft')
class ReportDraftsTable extends Table {
  TextColumn get id => text()(); // UUID generated locally
  TextColumn get ameId => text()();
  TextColumn get semaine => text()();
  TextColumn get presencesParCulte => text()(); // JSON string
  TextColumn? get absenceRaison => text().nullable()();
  TextColumn? get absenceCommentaire => text().nullable()();
  TextColumn? get difficultes => text().nullable()();
  TextColumn? get notesComplementaires => text().nullable()();
  IntColumn get nbSorties => integer().withDefault(const Constant(0))();
  IntColumn get nbMaintenus => integer().withDefault(const Constant(0))();
  TextColumn get updatedAt => text()(); // ISO timestamp
  BoolColumn get synced => boolean().withDefault(const Constant(false))();

  @override
  Set<Column> get primaryKey => {id};
}

/// Queue for reports that need to be synced when online
@DataClassName('SyncQueueItem')
class SyncQueueTable extends Table {
  TextColumn get id => text()(); // UUID
  TextColumn get operation => text()(); // 'CREATE' | 'UPDATE'
  TextColumn get endpoint => text()(); // e.g., '/reports/maker-weekly'
  TextColumn get payload => text()(); // JSON body
  TextColumn get createdAt => text()(); // ISO timestamp
  IntColumn get retryCount => integer().withDefault(const Constant(0))();
  TextColumn? get lastError => text().nullable()();

  @override
  Set<Column> get primaryKey => {id};
}

// ==================== DATABASE ====================

@DriftDatabase(tables: [SoulsTable, ReportDraftsTable, SyncQueueTable])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 1;

  // ==================== SOULS ====================

  Future<void> saveSouls(List<SoulLocal> souls) async {
    await batch((batch) {
      for (final soul in souls) {
        batch.insert(
          soulsTable,
          soul,
          mode: InsertMode.replace,
        );
      }
    });
  }

  Future<List<SoulLocal>> getLocalSouls() =>
      select(soulsTable).get();

  Future<void> clearSouls() => delete(soulsTable).go();

  // ==================== DRAFTS ====================

  Future<void> saveDraft(ReportDraft draft) {
    return into(reportDraftsTable).insertOnConflictUpdate(draft);
  }

  Future<List<ReportDraft>> getUnsyncedDrafts() =>
      select(reportDraftsTable).get();

  Future<ReportDraft?> getDraft(String ameId, String semaine) {
    return (select(reportDraftsTable)
          ..where((t) => t.ameId.equals(ameId) & t.semaine.equals(semaine)))
        .getSingleOrNull();
  }

  Future<void> markDraftSynced(String id) {
    return (update(reportDraftsTable)..where((t) => t.id.equals(id)))
        .write(const ReportDraftsTableCompanion(synced: Value(true)));
  }

  Future<void> deleteDraft(String id) {
    return (delete(reportDraftsTable)..where((t) => t.id.equals(id))).go();
  }

  // ==================== SYNC QUEUE ====================

  Future<void> addToSyncQueue(SyncQueueItem item) {
    return into(syncQueueTable).insert(item);
  }

  Future<List<SyncQueueItem>> getPendingSyncItems() {
    return (select(syncQueueTable)..orderBy([(t) => OrderingTerm(expression: t.createdAt, mode: OrderingMode.asc)])).get();
  }

  Future<void> removeSyncItem(String id) {
    return (delete(syncQueueTable)..where((t) => t.id.equals(id))).go();
  }

  Future<void> incrementRetry(String id, {String? error}) async {
    // Reads the current value then increments — avoids drift expression limits.
    final current = await (select(syncQueueTable)..where((t) => t.id.equals(id))).getSingleOrNull();
    if (current == null) return;
    await (update(syncQueueTable)..where((t) => t.id.equals(id)))
        .write(SyncQueueTableCompanion(
      retryCount: Value(current.retryCount + 1),
      lastError: Value(error),
    ));
  }

  Future<void> markSyncFailed(String id, String error, int retryCount) {
    return (update(syncQueueTable)..where((t) => t.id.equals(id)))
        .write(SyncQueueTableCompanion(retryCount: Value(retryCount), lastError: Value(error)));
  }

  /// Clear all data (e.g., on logout)
  Future<void> clearAll() async {
    await delete(soulsTable).go();
    await delete(reportDraftsTable).go();
    await delete(syncQueueTable).go();
  }
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'discipolat.db'));
    return NativeDatabase(file);
  });
}

// ==================== PROVIDERS ====================

final databaseProvider = Provider<AppDatabase>((ref) => AppDatabase());

final connectivityProvider = StreamProvider<List<ConnectivityResult>>((ref) {
  return Connectivity().onConnectivityChanged;
});

final isOnlineProvider = Provider<bool>((ref) {
  final connectivity = ref.watch(connectivityProvider);
  return connectivity.when(
    data: (results) => results.any((r) =>
        r == ConnectivityResult.wifi ||
        r == ConnectivityResult.mobile ||
        r == ConnectivityResult.ethernet),
    loading: () => false,
    error: (_, __) => false,
  );
});
