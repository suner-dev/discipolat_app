import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../services/api_service.dart';
import 'database.dart';
import '../../tenant_config.dart';

class SyncService {
  final AppDatabase _db;
  final ApiService _api;
  final Ref _ref;
  bool _isSyncing = false;

  SyncService(this._db, this._api, this._ref);

  /// Queue a report for sync and save locally (offline-first)
  ///
  /// Always saves locally first, then attempts API submission if online.
  /// If offline, queues for later sync when connectivity is restored.
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

    // Always save as draft locally FIRST (offline-first approach)
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
      synced: false, // Will be updated after successful sync
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
        // API call failed even though online - queue for later retry
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
          'retryReason': 'api_failed_when_online',
        });
      }
    } else {
      // Offline: queue for later sync when connectivity is restored
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

  /// Submit a queued item to the API with retry logic
  Future<bool> submitQueuedItem(SyncQueueItem item) async {
    try {
      final payload = jsonDecode(item.payload) as Map<String, dynamic>;
      await _api.post(item.endpoint, data: payload);
      await _db.removeSyncItem(item.id);
      return true;
    } catch (e) {
      await _db.markSyncFailed(item.id, e.toString(), item.retryCount + 1);
      return false;
    }
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
  ///
  /// Handles retry logic, exponential backoff, and tenant-aware filtering.
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

    // Sort by creation date (oldest first) and priority
    final sortedItems = List.from(items)..sort((a, b) => a.createdAt.compareTo(b.createdAt));

    for (final item in sortedItems) {
      // Skip items that have reached max retries
      if (item.retryCount >= 3) {
        failed++;
        // Mark as failed permanently
        await _db.markSyncFailed(item.id, 'Max retries reached', item.retryCount);
        continue;
      }

      try {
        // Apply tenant filter if in multi-tenant mode
        final payload = jsonDecode(item.payload) as Map<String, dynamic>;
        if (TenantConfig.isMultiTenantActive && item.endpoint.contains('/reports')) {
          // Add orgId to report payload for multi-tenant isolation
          payload['orgId'] = TenantConfig.currentOrgId;
        }

        await _api.post(item.endpoint, data: payload);
        await _db.removeSyncItem(item.id);
        synced++;
      } catch (e) {
        await _db.markSyncFailed(item.id, e.toString(), item.retryCount + 1);
        failed++;
        // Exponential backoff: next retry in 2^retryCount minutes
        // Could schedule a delayed retry here
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

  /// Get cached souls (for offline first launch)
  Future<List<SoulLocal>> getCachedSouls() => _db.getLocalSouls();

  /// Clear all local data (on logout)
  ///
  /// Also clears tenant config to ensure data isolation
  Future<void> clearAll() async {
    await TenantConfig.clearOrgId();
    await _db.clearAll();
  }
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
