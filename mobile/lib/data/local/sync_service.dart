import 'dart:convert';
import 'package:drift/drift.dart' show Value;
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../services/api_service.dart';
import 'database.dart';
import 'sync_lock.dart';
import '../../tenant_config.dart';

class SyncService {
  final AppDatabase _db;
  final ApiService _api;
  final Ref _ref;
  bool _isSyncing = false;

  SyncService(this._db, this._api, this._ref);

  /// Tenant courant pour l'isolation du cache local ; repli sur la valeur
  /// persistée ou `'default'` si aucun tenant n'est encore connu.
  Future<String> _tenant() async =>
      (await TenantConfig.resolveOrgId()) ?? 'default';

  /// Queue a report for sync and save locally (offline-first)
  ///
  /// Always saves locally first, then attempts API submission if online.
  /// If offline, queues for later sync when connectivity is restored.
  /// Le tenant courant est tamponné sur le brouillon et la file afin qu'aucune
  /// donnée d'une organisation ne soit visible depuis une autre.
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
    final tenantId = await _tenant();

    // Always save as draft locally FIRST (offline-first approach)
    await _db.saveDraft(ReportDraft(
      id: draftId,
      tenantId: tenantId,
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

  /// Résout le faiseur assigné à une âme depuis le cache local Drift.
  /// Requis par POST /reports/maker-weekly (champ `faiseurId` @NotNull).
  /// Statique et publique : partagée avec OfflineSyncManager.
  static Future<String?> resolveFaiseurId(
      AppDatabase db, String tenantId, String ameId) async {
    final souls = await db.getLocalSouls(tenantId);
    for (final s in souls) {
      if (s.id == ameId) return s.faiseurId;
    }
    return null;
  }

  Future<String?> _resolveFaiseurId(String tenantId, String ameId) =>
      resolveFaiseurId(_db, tenantId, ameId);

  /// Normalise l'endpoint d'un item en file : les items legacy pointaient
  /// vers GET /reports/export/maker-weekly (un export CSV LECTURE SEULE —
  /// les rapports n'étaient jamais persistés). Ils sont redirigés vers le
  /// vrai endpoint d'écriture POST /reports/maker-weekly.
  static String migrateEndpoint(String endpoint) =>
      endpoint.replaceAll('/reports/export/maker-weekly', '/reports/maker-weekly');

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
    final tenantId = await _tenant();
    final faiseurId = await _resolveFaiseurId(tenantId, ameId);
    if (faiseurId == null) {
      // Sans faiseur connu (âme pas encore en cache local), on refuse le
      // send direct : l'appelant met l'item en file, qui sera rejouée
      // une fois les âmes synchronisées. Jamais de perte silencieuse.
      throw StateError('faiseurId introuvable pour ame $ameId — rapport en file');
    }
    await _api.post('/reports/maker-weekly', data: {
      'faiseurId': faiseurId,
      'ameId': ameId,
      'semaine': semaine,
      'presencesParCulte': presencesParCulte,
      if (absenceRaison != null && absenceRaison.isNotEmpty) 'absenceRaison': absenceRaison,
      if (absenceCommentaire != null && absenceCommentaire.isNotEmpty)
        'absenceCommentaire': absenceCommentaire,
      if (difficultes != null && difficultes.isNotEmpty) 'difficultes': difficultes,
      if (notesComplementaires != null && notesComplementaires.isNotEmpty)
        'notesComplementaires': notesComplementaires,
      'nbSorties': nbSorties,
      'nbMaintenus': nbMaintenus,
      if (fichierIds != null && fichierIds.isNotEmpty) 'fichierIds': fichierIds,
    });
  }

  /// Submit a queued item to the API with retry logic
  Future<bool> submitQueuedItem(SyncQueueItem item) async {
    try {
      final payload = jsonDecode(item.payload) as Map<String, dynamic>;
      await _submitPayload(item, payload, await _tenant());
      await _db.removeSyncItem(item.id);
      return true;
    } catch (e) {
      await _db.markSyncFailed(item.id, e.toString(), item.retryCount + 1);
      return false;
    }
  }

  /// Envoie la payload d'un item en file vers le bon endpoint.
  /// Cas des rapports : legacy `/reports/export/...` → POST maker-weekly,
  /// avec injection du `faiseurId` résolu depuis le cache local si absent.
  Future<void> _submitPayload(
      SyncQueueItem item, Map<String, dynamic> payload, String tenantId) async {
    var endpoint = migrateEndpoint(item.endpoint);
    if (endpoint.contains('/reports/maker-weekly') &&
        payload['faiseurId'] == null &&
        payload['ameId'] is String) {
      final faiseurId = await _resolveFaiseurId(tenantId, payload['ameId'] as String);
      if (faiseurId == null) {
        // retryCount ≥ 3 → l'item reste en file « définitivement en échec »
        // au lieu d'être supprimé : aucune perte silencieuse.
        throw StateError('faiseurId introuvable pour ame ${payload['ameId']}');
      }
      payload['faiseurId'] = faiseurId;
    }
    await _api.post(endpoint, data: payload);
  }

  Future<void> _queueForSync(String draftId, Map<String, dynamic> payload) async {
    final tenantId = await _tenant();
    // Injecter le faiseur dès la mise en file quand le cache local le permet :
    // la rejouée ultérieure n'aura pas à le re-résoudre.
    final ameId = payload['ameId'] as String?;
    if (ameId != null && payload['faiseurId'] == null) {
      final faiseurId = await _resolveFaiseurId(tenantId, ameId);
      if (faiseurId != null) payload['faiseurId'] = faiseurId;
    }
    // PORT Develop1 (§G5.7) — idempotence par client_uuid : UUID généré à la
    // saisie terrain, jamais régénéré ; rejeu après réseau instable → doublon
    // ignoré côté serveur (V208). Le companion accepte les défauts de colonnes.
    final now = DateTime.now();
    await _db.addToSyncQueue(SyncQueueTableCompanion.insert(
      id: const Uuid().v4(),
      tenantId: tenantId,
      operation: 'CREATE',
      endpoint: '/reports/maker-weekly',
      payload: jsonEncode(payload),
      createdAt: now.toIso8601String(),
      clientUuid: Value(Uuid().v4()),
      clientAt: Value(now.toUtc().toIso8601String()),
    ));
  }

  /// Sync all pending items. Called when connectivity is restored.
  ///
  /// Handles retry logic, exponential backoff, and tenant-aware filtering.
  /// Synchronise la file d'attente du tenant courant uniquement.
  ///
  /// Handles retry logic, exponential backoff, and tenant-aware filtering.
  Future<SyncResult> syncPending() =>
      // Verrou partagé avec OfflineSyncManager : empêche le double envoi
      // des mêmes items par les deux moteurs simultanés.
      syncFlushLock.run(_syncPendingLocked);

  Future<SyncResult> _syncPendingLocked() async {
    if (_isSyncing) return SyncResult(isSyncing: true);
    _isSyncing = true;

    final tenantId = await _tenant();
    final items = await _db.getPendingSyncItems(tenantId);
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

        await _submitPayload(item, payload, tenantId);
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

  /// Get all unsynced drafts (scoped au tenant courant)
  Future<List<ReportDraft>> getUnsyncedDrafts() async {
    final tenantId = await _tenant();
    return _db.getUnsyncedDrafts(tenantId);
  }

  /// Get a specific draft (scoped au tenant courant)
  Future<ReportDraft?> getDraft(String ameId, String semaine) async {
    final tenantId = await _tenant();
    return _db.getDraft(ameId, semaine, tenantId: tenantId);
  }

  /// Save souls locally for offline access (scoped au tenant courant)
  Future<void> cacheSouls(dynamic responseData) async {
    final tenantId = await _tenant();
    final souls = (responseData['content'] as List).map((e) {
      return SoulLocal(
        id: e['id'] as String,
        tenantId: tenantId,
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

  /// Get cached souls (for offline first launch) — scoped au tenant courant
  Future<List<SoulLocal>> getCachedSouls() async {
    final tenantId = await _tenant();
    return _db.getLocalSouls(tenantId);
  }

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
