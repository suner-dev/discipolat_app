import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../services/api_service.dart';
import 'database.dart';

class SyncService {
  final AppDatabase _db;
  final ApiService _api;
  final Ref _ref;
  bool _isSyncing = false;

  SyncService(this._db, this._api, this._ref);

  /// Queue a report for sync and save locally
  Future<String> saveReportLocally({
    required String ameId,
    required String semaine,
    required Map<String, bool> presencesParCulte,
    String? absenceRaison,
    String? absenceCommentaire,
    String? difficultes,
    String? notesComplementaires,
    int nbSorties = 0,
    int nbMaintenus = 0,
    List<String>? fichierIds,
  }) async {
    final isOnline = _ref.read(isOnlineProvider);
    final draftId = const Uuid().v4();

    // Save as draft locally
    await _db.saveDraft(ReportDraft(
      id: draftId,
      ameId: ameId,
      semaine: semaine,
      presencesParCulte: jsonEncode(presencesParCulte),
      absenceRaison: absenceRaison,
      absenceCommentaire: absenceCommentaire,
      difficultes: difficultes,
      notesComplementaires: notesComplementaires,
      nbSorties: nbSorties,
      nbMaintenus: nbMaintenus,
      updatedAt: DateTime.now().toIso8601String(),
      synced: isOnline, // If online, mark as synced after API call
    ));

    if (isOnline) {
      try {
        await _submitToApi(
          ameId: ameId,
          semaine: semaine,
          presencesParCulte: presencesParCulte,
          absenceRaison: absenceRaison,
          absenceCommentaire: absenceCommentaire,
          difficultes: difficultes,
          notesComplementaires: notesComplementaires,
          nbSorties: nbSorties,
          nbMaintenus: nbMaintenus,
          fichierIds: fichierIds,
        );
        await _db.markDraftSynced(draftId);
      } catch (e) {
        // Queue for later sync
        await _queueForSync(draftId, {
          'ameId': ameId,
          'semaine': semaine,
          'presencesParCulte': presencesParCulte,
          'absenceRaison': absenceRaison,
          'absenceCommentaire': absenceCommentaire,
          'difficultes': difficultes,
          'notesComplementaires': notesComplementaires,
          'nbSorties': nbSorties,
          'nbMaintenus': nbMaintenus,
          if (fichierIds != null && fichierIds.isNotEmpty) 'fichierIds': fichierIds,
        });
      }
    } else {
      // Offline: queue for later
      await _queueForSync(draftId, {
        'ameId': ameId,
        'semaine': semaine,
        'presencesParCulte': presencesParCulte,
        'absenceRaison': absenceRaison,
        'absenceCommentaire': absenceCommentaire,
        'difficultes': difficultes,
        'notesComplementaires': notesComplementaires,
        'nbSorties': nbSorties,
        'nbMaintenus': nbMaintenus,
        if (fichierIds != null && fichierIds.isNotEmpty) 'fichierIds': fichierIds,
      });
    }

    return draftId;
  }

  Future<void> _submitToApi({
    required String ameId,
    required String semaine,
    required Map<String, bool> presencesParCulte,
    String? absenceRaison,
    String? absenceCommentaire,
    String? difficultes,
    String? notesComplementaires,
    int nbSorties = 0,
    int nbMaintenus = 0,
    List<String>? fichierIds,
  }) async {
    await _api.post('/reports/maker-weekly', data: {
      'ameId': ameId,
      'semaine': semaine,
      'presencesParCulte': presencesParCulte,
      'absenceRaison': absenceRaison,
      'absenceCommentaire': absenceCommentaire?.isNotEmpty == true ? absenceCommentaire : null,
      'difficultes': difficultes?.isNotEmpty == true ? difficultes : null,
      'notesComplementaires': notesComplementaires?.isNotEmpty == true ? notesComplementaires : null,
      'nbSorties': nbSorties,
      'nbMaintenus': nbMaintenus,
      if (fichierIds != null && fichierIds.isNotEmpty) 'fichierIds': fichierIds,
    });
  }

  Future<void> _queueForSync(String draftId, Map<String, dynamic> payload) async {
    await _db.addToSyncQueue(SyncQueueItem(
      id: const Uuid().v4(),
      operation: 'CREATE',
      endpoint: '/reports/maker-weekly',
      payload: jsonEncode(payload),
      createdAt: DateTime.now().toIso8601String(),
      retryCount: 0,
    ));
  }

  /// Sync all pending items. Called when connectivity is restored.
  Future<SyncResult> syncPending() async {
    if (_isSyncing) return SyncResult(isSyncing: true);
    _isSyncing = true;

    final items = await _db.getPendingSyncItems();
    if (items.isEmpty) {
      _isSyncing = false;
      return SyncResult(synced: 0, failed: 0);
    }

    int synced = 0;
    int failed = 0;

    for (final item in items) {
      if (item.retryCount >= 3) {
        failed++;
        continue; // Max retries reached, skip
      }

      try {
        final payload = jsonDecode(item.payload) as Map<String, dynamic>;
        await _api.post(item.endpoint, data: payload);
        await _db.removeSyncItem(item.id);
        synced++;
      } catch (e) {
        await _db.markSyncFailed(item.id, e.toString());
        failed++;
      }
    }

    _isSyncing = false;
    return SyncResult(synced: synced, failed: failed);
  }

  /// Get all unsynced drafts for display
  Future<List<ReportDraft>> getUnsyncedDrafts() => _db.getUnsyncedDrafts();

  /// Get a specific draft
  Future<ReportDraft?> getDraft(String ameId, String semaine) =>
      _db.getDraft(ameId, semaine);

  /// Save souls locally for offline access
  Future<void> cacheSouls(dynamic responseData) async {
    final souls = (responseData['content'] as List).map((e) {
      return SoulLocal(
        id: e['id'] as String,
        nom: e['nom'] as String,
        prenom: e['prenom'] as String?,
        email: e['email'] as String?,
        telephone: e['telephone'] as String?,
        typeDisciple: e['typeDisciple'] as String,
        statut: e['statut'] as String,
        dateIntegration: e['dateIntegration'] as String,
        faiseurId: e['faiseurId'] as String,
        familleId: e['familleId'] as String?,
        dateDernierContact: e['dateDernierContact'] as String?,
        lastSyncAt: DateTime.now().toIso8601String(),
      );
    }).toList();
    await _db.saveSouls(souls);
  }

  /// Get cached souls
  Future<List<SoulLocal>> getCachedSouls() => _db.getLocalSouls();

  /// Clear all local data (on logout)
  Future<void> clearAll() => _db.clearAll();
}

class SyncResult {
  final int synced;
  final int failed;
  final bool isSyncing;

  SyncResult({this.synced = 0, this.failed = 0, this.isSyncing = false});
}

// Provider
final syncServiceProvider = Provider<SyncService>((ref) {
  final db = ref.read(databaseProvider);
  final api = ApiService();
  return SyncService(db, api, ref);
});
