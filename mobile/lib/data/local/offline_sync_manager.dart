import 'dart:async';
import 'dart:convert';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:drift/drift.dart' show Value;
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';
import '../../tenant_config.dart';
import '../services/api_service.dart';
import 'database.dart';
import 'sync_lock.dart';
import 'sync_service.dart' show SyncService;

/// §G5.7 — File d'écriture hors-ligne CIBLÉE, idempotente, avec toggle tenant.
///
/// Chaque opération critique saisie sur le terrain porte un `clientUuid`
/// (UUID v4 généré À LA SAISIE, jamais régénéré) et un horodatage local
/// (`clientAt`, base du LWW serveur). À la reconnexion, les opérations sont
/// envoyées PAR LOT à `POST /sync/batch` : le serveur rejette silencieusement
/// tout rejeu (doublon → SKIPPED_DUPLICATE), applique les gardes réelles des
/// endpoints en ligne, et signale les conflits LWW sans perte silencieuse.
///
/// Toggle `offline_mode` du tenant (§57-59 / G1.2) :
/// - LECTURE   → aucune écriture hors-ligne n'est mise en file ;
/// - FIELD_OPS → opérations critiques terrain seules (pointage QR, sortie /
///   retour matériel + photo de dommage) ;
/// - FULL      → toutes les opérations connues.
class OfflineSyncManager {
  final AppDatabase _db;
  final ApiService _api;
  static const _uuidGen = Uuid();
  StreamSubscription<List<ConnectivityResult>>? _connectivitySub;
  bool _isSyncing = false;
  int _pendingCount = 0;
  String? _offlineModeCache;
  final _syncController = StreamController<OfflineSyncResult>.broadcast();

  /// Opérations éligibles au mode FIELD_OPS (parité backend SyncBatchService).
  static const fieldOpsTypes = {
    'QR_CHECKIN',
    'QR_CHECKIN_MEMBER',
    'ASSET_CHECKOUT',
    'ASSET_RETURN',
    'ASSET_DAMAGE_PHOTO',
  };

  static const _maxRetries = 5;
  static const _batchSize = 50;

  OfflineSyncManager(this._db, this._api);

  int get pendingCount => _pendingCount;
  bool get isSyncing => _isSyncing;
  Stream<OfflineSyncResult> get syncResults => _syncController.stream;

  /// Current tenant ID for multi-tenant isolation
  Future<String> _tenant() async =>
      (await TenantConfig.resolveOrgId()) ?? 'default';

  /// Start listening for connectivity changes and auto-sync
  void startListening() {
    _connectivitySub = Connectivity().onConnectivityChanged.listen((results) {
      final isOnline = results.any((r) =>
          r == ConnectivityResult.wifi ||
          r == ConnectivityResult.mobile ||
          r == ConnectivityResult.ethernet);
      if (isOnline) {
        syncPendingItems();
      }
    });
    _refreshPendingCount();
  }

  void stopListening() {
    _connectivitySub?.cancel();
    _syncController.close();
  }

  Future<void> _refreshPendingCount() async {
    final tenantId = await _tenant();
    final items = await _db.getPendingSyncItems(tenantId);
    _pendingCount = items.length;
  }

  // ==================== toggle tenant ====================

  /// `offline_mode` du tenant (GET /admin/settings), mis en cache en mémoire.
  /// En cas d'échec réseau, défaut prudent : FIELD_OPS (le terrain reste
  /// opérationnel pour les critiques — le serveur re-vérifie le mode au batch).
  Future<String> offlineMode({bool forceRefresh = false}) async {
    if (!forceRefresh && _offlineModeCache != null) return _offlineModeCache!;
    try {
      final res = await _api.get('/admin/settings');
      final data = res.data;
      final mode = data is Map ? data['offlineMode']?.toString() : null;
      _offlineModeCache = (mode == null || mode.isEmpty) ? 'LECTURE' : mode;
    } catch (_) {
      _offlineModeCache ??= 'FIELD_OPS';
    }
    return _offlineModeCache!;
  }

  @visibleForTesting
  set offlineModeOverride(String? mode) => _offlineModeCache = mode;

  /// Le tenant autorise-t-il la mise en file de cette opération ?
  Future<bool> canQueueOffline(String syncType) async {
    final mode = await offlineMode();
    switch (mode.toUpperCase()) {
      case 'FULL':
        return true;
      case 'FIELD_OPS':
        return fieldOpsTypes.contains(syncType);
      default:
        return false; // LECTURE = pas d'écriture hors-ligne.
    }
  }

  // ==================== file d'écriture ====================

  /// Enfile une opération critique (idempotente par clientUuid).
  /// Retourne false si le toggle du tenant l'interdit (LECTURE/ FIELD_OPS hors
  /// périmètre) — l'appelant doit alors afficher l'erreur en ligne.
  Future<bool> queueOperation({
    required String syncType,
    required Map<String, dynamic> payload,
    String? clientUuid,
    String? photoBase64,
    String photoMime = 'image/jpeg',
    String photoName = 'photo.jpg',
  }) async {
    if (!await canQueueOffline(syncType)) return false;
    final tenantId = await _tenant();
    final now = DateTime.now();
    final id = clientUuid ?? _uuidGen.v4();
    await _db.addToSyncQueue(SyncQueueTableCompanion.insert(
      id: id,
      tenantId: tenantId,
      operation: 'CREATE',
      endpoint: '/sync/batch',
      payload: jsonEncode(payload),
      createdAt: now.toIso8601String(),
      clientUuid: Value(id),
      syncType: Value(syncType),
      status: const Value('PENDING'),
      clientAt: Value(now.toUtc().toIso8601String()),
      photoBase64: Value(photoBase64 ?? ''),
      photoMime: Value(photoMime),
      photoName: Value(photoName),
    ));
    _pendingCount++;
    debugPrint('[OfflineSync] Queued $syncType ($id)');
    return true;
  }

  /// Pointage QR hors-ligne : le code (ou contenu) scanné sera rejoué à la
  /// reconnexion via /sync/batch → même endpoint, mêmes gardes.
  Future<bool> queueQrCheckin(String code) =>
      queueOperation(syncType: 'QR_CHECKIN', payload: {'code': code});

  Future<bool> queueAssetCheckout({
    required String itemId,
    required String memberId,
    String condition = 'GOOD',
    String? notes,
  }) =>
      queueOperation(syncType: 'ASSET_CHECKOUT', payload: {
        'itemId': itemId,
        'memberId': memberId,
        'condition': condition,
        'notes': notes ?? 'Check-out hors-ligne terrain',
      });

  Future<bool> queueAssetReturn({
    required String itemId,
    String condition = 'GOOD',
    String? notes,
  }) =>
      queueOperation(syncType: 'ASSET_RETURN', payload: {
        'itemId': itemId,
        'condition': condition,
        'notes': notes ?? 'Retour hors-ligne terrain',
      });

  /// Photo de dommage capturée hors-ligne (base64, jamais perdue).
  Future<bool> queueDamagePhoto({
    required String itemId,
    required List<int> bytes,
    String mime = 'image/jpeg',
    String filename = 'damage.jpg',
  }) =>
      queueOperation(
        syncType: 'ASSET_DAMAGE_PHOTO',
        payload: {'itemId': itemId},
        photoBase64: base64Encode(bytes),
        photoMime: mime,
        photoName: filename,
      );

  Future<bool> queueTaskStatus({required String taskId, required String statut}) =>
      queueOperation(syncType: 'TASK_STATUS', payload: {
        'taskId': taskId,
        'statut': statut,
      });

  Future<bool> queueNewVisitor(Map<String, dynamic> visitor) =>
      queueOperation(syncType: 'NEW_VISITOR', payload: {'visitor': visitor});

  // ==================== legacy per-item operations ====================

  /// Queue a presence entry for offline sync (batch PRESENCE_SUBMIT).
  Future<bool> queuePresenceEntry({
    required String departmentId,
    required String date,
    required List<Map<String, dynamic>> items,
  }) =>
      queueOperation(syncType: 'PRESENCE_SUBMIT', payload: {
        'departmentId': departmentId,
        'request': {'semaine': date, 'presences': items},
      });

  Future<void> _queueLegacy({
    required String prefix,
    required String endpoint,
    required Map<String, dynamic> payload,
  }) async {
    final tenantId = await _tenant();
    final now = DateTime.now();
    final id = _uuidGen.v4();
    await _db.addToSyncQueue(SyncQueueTableCompanion.insert(
      id: '$prefix-${now.millisecondsSinceEpoch}',
      tenantId: tenantId,
      operation: 'CREATE',
      endpoint: endpoint,
      payload: jsonEncode(payload),
      createdAt: now.toIso8601String(),
      clientUuid: Value(id),
      clientAt: Value(now.toUtc().toIso8601String()),
    ));
    _pendingCount++;
  }

  /// Queue a discipline event for offline sync
  Future<void> queueDisciplineEvent({required Map<String, dynamic> event}) async {
    final soulId = event['soulId'] ?? '';
    await _queueLegacy(
        prefix: 'discipline',
        endpoint: '/souls/$soulId/discipline',
        payload: event);
    debugPrint('[OfflineSync] Queued discipline event');
  }

  /// Queue a prayer/action de grâce for offline sync
  Future<void> queuePrayer({required Map<String, dynamic> prayer}) =>
      _queueLegacy(prefix: 'prayer', endpoint: '/prayers/actions-de-grace', payload: prayer);

  /// Queue a message for offline send
  Future<void> queueMessage({
    required String conversationId,
    required String content,
  }) =>
      _queueLegacy(
          prefix: 'message',
          endpoint: '/conversations/$conversationId/messages',
          payload: {'content': content});

  /// Queue a badge evaluation for offline sync
  Future<void> queueBadgeEvaluation({required String memberId}) =>
      _queueLegacy(
          prefix: 'badge',
          endpoint: '/members/$memberId/badges/evaluate',
          payload: {});

  /// Queue a visit report (pastoral visit note) for offline sync
  Future<void> queueVisitReport({
    required String soulId,
    required Map<String, dynamic> report,
  }) =>
      _queueLegacy(
          prefix: 'visit', endpoint: '/souls/$soulId/visits', payload: report);

  /// Queue an evaluation (360° feedback) for offline sync
  Future<void> queueEvaluation({
    required String memberId,
    required Map<String, dynamic> evaluation,
  }) =>
      _queueLegacy(
          prefix: 'eval',
          endpoint: '/members/$memberId/evaluations',
          payload: evaluation);

  /// Queue an appointment (rendez-vous) for offline sync
  Future<void> queueAppointment({required Map<String, dynamic> appointment}) =>
      _queueLegacy(prefix: 'appt', endpoint: '/appointments', payload: appointment);

  /// Queue a form submission for offline sync
  Future<void> queueFormSubmission({
    required String formId,
    required Map<String, dynamic> data,
  }) =>
      _queueLegacy(
          prefix: 'form', endpoint: '/forms/$formId/submit', payload: data);

  /// Queue a document upload metadata for offline sync
  Future<void> queueDocumentUpload({
    required String path,
    required Map<String, dynamic> metadata,
  }) =>
      _queueLegacy(
          prefix: 'doc',
          endpoint: '/files/upload',
          payload: {...metadata, 'localPath': path});

  // ==================== flush ====================

  /// Manually trigger sync of all pending items
  /// Verrou partagé avec SyncService : les deux moteurs drainent la même
  /// file et ne doivent jamais l'envoyer en parallèle (doubles POST).
  Future<OfflineSyncResult> syncPendingItems() =>
      syncFlushLock.run(_syncPendingItems);

  /// Retry exponentiel : 30 s · 1 min · 2 min · 4 min · 8 min (plafond).
  @visibleForTesting
  static Duration backoffFor(int retryCount) =>
      Duration(seconds: 30 * (1 << (retryCount.clamp(0, 5))));

  bool _backoffElapsed(SyncQueueItem item) {
    if (item.retryCount == 0) return true;
    final createdAt = DateTime.tryParse(item.createdAt);
    if (createdAt == null) return true;
    return DateTime.now()
        .isAfter(createdAt.add(backoffFor(item.retryCount - 1)));
  }

  Future<OfflineSyncResult> _syncPendingItems() async {
    if (_isSyncing) return OfflineSyncResult(isSyncing: true);

    _isSyncing = true;
    final tenantId = await _tenant();
    final items = await _db.getPendingSyncItems(tenantId);

    if (items.isEmpty) {
      _isSyncing = false;
      _pendingCount = 0;
      final result = OfflineSyncResult(synced: 0, failed: 0);
      _syncController.add(result);
      return result;
    }

    int synced = 0;
    int failed = 0;
    int deferred = 0;

    // 1) Opérations idempotentes par lot → POST /sync/batch.
    final batchItems =
        items.where((i) => i.syncType.isNotEmpty && _backoffElapsed(i)).toList();
    for (var start = 0; start < batchItems.length; start += _batchSize) {
      final chunk = batchItems.sublist(
          start, (start + _batchSize).clamp(0, batchItems.length));
      try {
        final res = await _api.post('/sync/batch', data: {
          'operations': chunk
              .map((i) => {
                    'clientUuid': i.clientUuid,
                    'type': i.syncType,
                    'payload': jsonDecode(i.payload),
                    'at': i.clientAt.isEmpty ? null : i.clientAt,
                    if (i.photoBase64.isNotEmpty) ...{
                      'photoBase64': i.photoBase64,
                      'photoMime': i.photoMime,
                      'photoName': i.photoName,
                    },
                  })
              .toList(),
        });
        synced += _applyBatchResults(res.data, chunk);
      } catch (e) {
        // Réseau retombé en panne en plein envoi : backoff sur tout le chunk.
        for (final i in chunk) {
          await _markRetry(i, e.toString());
        }
        debugPrint('[OfflineSync] Batch failed (retry scheduled): $e');
      }
    }

    // 2) Items legacy (types hors batch) : replay individuel, comportement historique.
    for (final item in items.where((i) => i.syncType.isEmpty)) {
      if (!_backoffElapsed(item)) {
        deferred++;
        continue;
      }
      if (item.retryCount >= _maxRetries) {
        await _db.markSyncFailed(item.id, 'max retries', item.retryCount);
        await _db.setSyncStatus(item.id, 'FAILED_MAX_RETRIES');
        failed++;
        continue;
      }
      try {
        final payload = jsonDecode(item.payload) as Map<String, dynamic>;
        // Items legacy : l'ancien endpoint d'export (GET, lecture seule)
        // est redirigé vers le vrai endpoint d'écriture POST.
        final endpoint = SyncService.migrateEndpoint(item.endpoint);
        // Même injection que SyncService : POST /reports/maker-weekly exige
        // faiseurId (@NotNull) — résolu depuis le cache local des âmes.
        if (endpoint.contains('/reports/maker-weekly') &&
            payload['faiseurId'] == null &&
            payload['ameId'] is String) {
          final faiseurId = await SyncService.resolveFaiseurId(
              _db, tenantId, payload['ameId'] as String);
          if (faiseurId == null) {
            throw StateError('faiseurId introuvable pour ame ${payload['ameId']}');
          }
          payload['faiseurId'] = faiseurId;
        }
        await _api.post(endpoint, data: payload);
        await _db.removeSyncItem(item.id);
        synced++;
        debugPrint('[OfflineSync] Synced legacy: ${item.endpoint}');
      } catch (e) {
        await _markRetry(item, e.toString());
        failed++;
        debugPrint('[OfflineSync] Failed legacy: ${item.endpoint} — $e');
      }
    }

    await _refreshPendingCount();
    _isSyncing = false;

    final result = OfflineSyncResult(
        synced: synced, failed: failed + deferred, hasPending: _pendingCount > 0);
    _syncController.add(result);
    debugPrint('[OfflineSync] Done: $synced synced, $failed failed');
    return result;
  }

  /// Applique les résultats serveur (`APPLIED`/`SKIPPED_DUPLICATE` ⇒ sortie de
  /// file ; `CONFLICT_LWW` ⇒ sortie de file aussi — la trace est côté serveur)
  /// et reprogramme un backoff sur les échecs réseau-réels.
  int _applyBatchResults(dynamic data, List<SyncQueueItem> chunk) {
    final byUuid = {for (final i in chunk) i.clientUuid: i};
    var synced = 0;
    if (data is Map && data['results'] is List) {
      for (final r in (data['results'] as List)) {
        if (r is! Map) continue;
        final uuid = r['clientUuid']?.toString();
        final status = r['status']?.toString();
        final item = byUuid[uuid];
        if (item == null) continue;
        if (status == 'APPLIED' || status == 'SKIPPED_DUPLICATE' ||
            status == 'CONFLICT_LWW') {
          // Le serveur a tracé (ou ignoré le doublon) : plus rien à rejouer.
          unawaited(_db.removeSyncItem(item.id));
          synced++;
        } else if (status == 'REJECTED_OFFLINE_MODE') {
          // Interdit par le toggle : on ne bourdonne pas le serveur.
          unawaited(_db.markSyncFailed(
              item.id, 'mode hors-ligne refusé', _maxRetries));
          unawaited(_db.setSyncStatus(item.id, 'FAILED_MAX_RETRIES'));
        } else {
          unawaited(_db.markSyncFailed(item.id,
              r['error']?.toString() ?? status ?? '?', item.retryCount + 1));
        }
      }
    }
    return synced;
  }

  Future<void> _markRetry(SyncQueueItem item, String error) async {
    final next = item.retryCount + 1;
    await _db.markSyncFailed(item.id, error, next);
    if (next >= _maxRetries) {
      await _db.setSyncStatus(item.id, 'FAILED_MAX_RETRIES');
    }
  }
}

class OfflineSyncResult {
  final int synced;
  final int failed;
  final bool isSyncing;
  final bool hasPending;

  OfflineSyncResult(
      {this.synced = 0,
      this.failed = 0,
      this.isSyncing = false,
      this.hasPending = false});

  bool get hasErrors => failed > 0;
}

final offlineSyncManagerProvider = Provider<OfflineSyncManager>((ref) {
  final db = ref.read(databaseProvider);
  final api = ApiService();
  final manager = OfflineSyncManager(db, api);
  manager.startListening();
  ref.onDispose(() => manager.stopListening());
  return manager;
});

OfflineSyncManager? _sharedManager;

/// Instance partagée pour les écrans non-Riverpod (injection de test oblige) :
/// évite deux connexions DB concurrentes si plusieurs écrans terrain sont ouverts.
OfflineSyncManager sharedOfflineSyncManager(ApiService api) =>
    _sharedManager ??= OfflineSyncManager(AppDatabase(), api)
      ..startListening();
