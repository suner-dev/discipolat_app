import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

part 'database.g.dart';

// ==================== TABLES ====================

/// Stocke localement les âmes pour l'accès hors-ligne.
/// La colonne [tenantId] garantit l'isolation entre organisations sur le même
/// appareil : les données de l'église A ne sont jamais visibles connecté à B.
@DataClassName('SoulLocal')
class SoulsTable extends Table {
  TextColumn get id => text()();
  TextColumn get tenantId => text()();
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
  Set<Column> get primaryKey => {id, tenantId};
}

/// Stocke les brouillons de rapport non soumis (hors-ligne).
@DataClassName('ReportDraft')
class ReportDraftsTable extends Table {
  TextColumn get id => text()(); // UUID generated locally
  TextColumn get tenantId => text()();
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

/// Cache hors-ligne des ressources partagées du réseau inter-églises.
@DataClassName('NetworkResourceLocal')
class NetworkResourcesTable extends Table {
  TextColumn get id => text()();
  TextColumn get tenantId => text()();
  TextColumn get title => text()();
  TextColumn get description => text().nullable()();
  TextColumn get category => text()();
  TextColumn get resourceType => text()();
  TextColumn get fileUrl => text().nullable()();
  TextColumn get content => text().nullable()();
  BoolColumn get sharedWithPublic => boolean()();
  IntColumn get downloads => integer().withDefault(const Constant(0))();
  BoolColumn get isActive => boolean().withDefault(const Constant(true))();
  TextColumn get lastSyncAt => text()();

  @override
  Set<Column> get primaryKey => {id};
}

/// Cache hors-ligne des événements inter-églises.
@DataClassName('NetworkEventLocal')
class NetworkEventsTable extends Table {
  TextColumn get id => text()();
  TextColumn get tenantId => text()();
  TextColumn get title => text()();
  TextColumn get description => text().nullable()();
  TextColumn get eventType => text()();
  TextColumn get location => text().nullable()();
  TextColumn get city => text().nullable()();
  TextColumn get country => text().nullable()();
  TextColumn get startsAt => text()();
  TextColumn get endsAt => text().nullable()();
  IntColumn get maxParticipants => integer().nullable()();
  IntColumn get currentParticipants => integer().withDefault(const Constant(0))();
  BoolColumn get isVirtual => boolean().withDefault(const Constant(false))();
  BoolColumn get sharedWithPublic => boolean()();
  BoolColumn get isActive => boolean().withDefault(const Constant(true))();
  BoolColumn get joinedByMe => boolean().withDefault(const Constant(false))();
  TextColumn get lastSyncAt => text()();

  @override
  Set<Column> get primaryKey => {id};
}

/// Cache hors-ligne du répertoire des églises.
@DataClassName('NetworkDirectoryLocal')
class NetworkDirectoryTable extends Table {
  TextColumn get id => text()();
  TextColumn get tenantId => text()();
  TextColumn get churchName => text().nullable()();
  TextColumn get city => text().nullable()();
  TextColumn get country => text().nullable()();
  TextColumn get denomination => text().nullable()();
  TextColumn get pastorName => text().nullable()();
  TextColumn get contactEmail => text().nullable()();
  TextColumn get contactPhone => text().nullable()();
  IntColumn get memberCount => integer().nullable()();
  BoolColumn get isListed => boolean().withDefault(const Constant(false))();
  TextColumn get lastSyncAt => text()();

  @override
  Set<Column> get primaryKey => {id};
}

/// File d'attente des rapports à synchroniser à la reconnexion.
@DataClassName('SyncQueueItem')
class SyncQueueTable extends Table {
  TextColumn get id => text()(); // UUID
  TextColumn get tenantId => text()();
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

@DriftDatabase(tables: [SoulsTable, ReportDraftsTable, SyncQueueTable, NetworkResourcesTable, NetworkEventsTable, NetworkDirectoryTable])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 3;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) async {
          await m.createAll();
        },
        onUpgrade: (m, from, to) async {
          // Cache hors-ligne : on peut reconstruire à volonté sans perte
          // définitive (les sources de vérité restent côté serveur).
          // La montée v1 → v2 ajoute la colonne tenant_id (isolation).
          for (final table in allTables) {
            await m.deleteTable(table.actualTableName);
          }
          await m.createAll();
        },
      );

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

  Future<List<SoulLocal>> getLocalSouls(String tenantId) =>
      (select(soulsTable)..where((t) => t.tenantId.equals(tenantId))).get();

  Future<void> clearSouls(String tenantId) =>
      (delete(soulsTable)..where((t) => t.tenantId.equals(tenantId))).go();

  // ==================== DRAFTS ====================

  Future<void> saveDraft(ReportDraft draft) {
    return into(reportDraftsTable).insertOnConflictUpdate(draft);
  }

  Future<List<ReportDraft>> getUnsyncedDrafts(String tenantId) =>
      (select(reportDraftsTable)..where((t) => t.tenantId.equals(tenantId))).get();

  Future<ReportDraft?> getDraft(String ameId, String semaine, {String? tenantId}) {
    final query = select(reportDraftsTable)
      ..where((t) =>
          t.ameId.equals(ameId) &
          t.semaine.equals(semaine) &
          (tenantId == null ? const Constant(true) : t.tenantId.equals(tenantId)));
    return query.getSingleOrNull();
  }

  Future<void> markDraftSynced(String id, {String? tenantId}) {
    final query = update(reportDraftsTable)..where((t) =>
        t.id.equals(id) &
        (tenantId == null ? const Constant(true) : t.tenantId.equals(tenantId)));
    return query.write(const ReportDraftsTableCompanion(synced: Value(true)));
  }

  Future<void> deleteDraft(String id, {String? tenantId}) {
    final query = delete(reportDraftsTable)..where((t) =>
        t.id.equals(id) &
        (tenantId == null ? const Constant(true) : t.tenantId.equals(tenantId)));
    return query.go();
  }

  // ==================== SYNC QUEUE ====================

  Future<void> addToSyncQueue(SyncQueueItem item) {
    return into(syncQueueTable).insert(item);
  }

  Future<List<SyncQueueItem>> getPendingSyncItems(String tenantId) {
    return (select(syncQueueTable)
          ..where((t) => t.tenantId.equals(tenantId))
          ..orderBy([(t) => OrderingTerm(expression: t.createdAt, mode: OrderingMode.asc)]))
        .get();
  }

  Future<void> removeSyncItem(String id, {String? tenantId}) {
    final query = delete(syncQueueTable)..where((t) =>
        t.id.equals(id) &
        (tenantId == null ? const Constant(true) : t.tenantId.equals(tenantId)));
    return query.go();
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

  // ==================== NETWORK CACHE ========================

  Future<void> saveNetworkResources(List<NetworkResourceLocal> resources) async {
    await batch((batch) {
      for (final r in resources) {
        batch.insert(networkResourcesTable, r, mode: InsertMode.replace);
      }
    });
  }

  Future<List<NetworkResourceLocal>> getLocalNetworkResources() =>
      select(networkResourcesTable).get();

  Future<void> clearNetworkResources() =>
      delete(networkResourcesTable).go();

  Future<void> saveNetworkEvents(List<NetworkEventLocal> events) async {
    await batch((batch) {
      for (final e in events) {
        batch.insert(networkEventsTable, e, mode: InsertMode.replace);
      }
    });
  }

  Future<List<NetworkEventLocal>> getLocalNetworkEvents() =>
      select(networkEventsTable).get();

  Future<void> clearNetworkEvents() =>
      delete(networkEventsTable).go();

  Future<void> saveNetworkDirectory(List<NetworkDirectoryLocal> entries) async {
    await batch((batch) {
      for (final e in entries) {
        batch.insert(networkDirectoryTable, e, mode: InsertMode.replace);
      }
    });
  }

  Future<List<NetworkDirectoryLocal>> getLocalNetworkDirectory() =>
      select(networkDirectoryTable).get();

  Future<void> clearNetworkDirectory() =>
      delete(networkDirectoryTable).go();

  Future<void> clearNetworkCache() async {
    await delete(networkResourcesTable).go();
    await delete(networkEventsTable).go();
    await delete(networkDirectoryTable).go();
  }

  /// Purge les données d'un tenant donné (à la déconnexion d'une organisation).
  Future<void> clearTenant(String tenantId) async {
    await (delete(soulsTable)..where((t) => t.tenantId.equals(tenantId))).go();
    await (delete(reportDraftsTable)..where((t) => t.tenantId.equals(tenantId))).go();
    await (delete(syncQueueTable)..where((t) => t.tenantId.equals(tenantId))).go();
    await delete(networkResourcesTable).go();
    await delete(networkEventsTable).go();
    await delete(networkDirectoryTable).go();
  }

  /// Clear all data (e.g., on logout)
  Future<void> clearAll() async {
    await delete(soulsTable).go();
    await delete(reportDraftsTable).go();
    await delete(syncQueueTable).go();
    await delete(networkResourcesTable).go();
    await delete(networkEventsTable).go();
    await delete(networkDirectoryTable).go();
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
