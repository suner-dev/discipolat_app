import 'dart:async';
import 'dart:convert';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/api_service.dart';
import 'database.dart';

/// Manages automatic sync of pending items when connectivity is restored.
/// Extends SyncService to handle presences, discipline, and prayers offline.
class OfflineSyncManager {
  final AppDatabase _db;
  final ApiService _api;
  StreamSubscription<List<ConnectivityResult>>? _connectivitySub;
  bool _isSyncing = false;
  int _pendingCount = 0;
  final _syncController = StreamController<OfflineSyncResult>.broadcast();

  OfflineSyncManager(this._db, this._api);

  int get pendingCount => _pendingCount;
  bool get isSyncing => _isSyncing;
  Stream<OfflineSyncResult> get syncResults => _syncController.stream;

  /// Start listening for connectivity changes and auto-sync
  void startListening() {
    _connectivitySub = Connectivity().onConnectivityChanged.listen((results) {
      final isOnline = results.any((r) =>
          r == ConnectivityResult.wifi ||
          r == ConnectivityResult.mobile ||
          r == ConnectivityResult.ethernet);
      if (isOnline) {
        _syncPendingItems();
      }
    });
    _refreshPendingCount();
  }

  void stopListening() {
    _connectivitySub?.cancel();
    _syncController.close();
  }

  Future<void> _refreshPendingCount() async {
    final items = await _db.getPendingSyncItems();
    _pendingCount = items.length;
  }

  /// Queue a presence entry for offline sync
  Future<void> queuePresenceEntry({
    required String departmentId,
    required String date,
    required List<Map<String, dynamic>> items,
  }) async {
    final id = 'presence-${DateTime.now().millisecondsSinceEpoch}';
    await _db.addToSyncQueue(SyncQueueItem(
      id: id,
      operation: 'CREATE',
      endpoint: '/departments/$departmentId/presence',
      payload: jsonEncode({'date': date, 'items': items}),
      createdAt: DateTime.now().toIso8601String(),
      retryCount: 0,
    ));
    _pendingCount++;
    debugPrint('[OfflineSync] Queued presence entry: $id');
  }

  /// Queue a discipline event for offline sync
  Future<void> queueDisciplineEvent({
    required Map<String, dynamic> event,
  }) async {
    final id = 'discipline-${DateTime.now().millisecondsSinceEpoch}';
    final soulId = event['soulId'] ?? '';
    await _db.addToSyncQueue(SyncQueueItem(
      id: id,
      operation: 'CREATE',
      endpoint: '/souls/$soulId/discipline',
      payload: jsonEncode(event),
      createdAt: DateTime.now().toIso8601String(),
      retryCount: 0,
    ));
    _pendingCount++;
    debugPrint('[OfflineSync] Queued discipline event: $id');
  }

  /// Queue a prayer/action de grâce for offline sync
  Future<void> queuePrayer({
    required Map<String, dynamic> prayer,
  }) async {
    final id = 'prayer-${DateTime.now().millisecondsSinceEpoch}';
    await _db.addToSyncQueue(SyncQueueItem(
      id: id,
      operation: 'CREATE',
      endpoint: '/prayers/actions-de-grace',
      payload: jsonEncode(prayer),
      createdAt: DateTime.now().toIso8601String(),
      retryCount: 0,
    ));
    _pendingCount++;
    debugPrint('[OfflineSync] Queued prayer: $id');
  }

  /// Queue a message for offline send
  Future<void> queueMessage({
    required String conversationId,
    required String content,
  }) async {
    final id = 'message-${DateTime.now().millisecondsSinceEpoch}';
    await _db.addToSyncQueue(SyncQueueItem(
      id: id,
      operation: 'CREATE',
      endpoint: '/conversations/$conversationId/messages',
      payload: jsonEncode({'content': content}),
      createdAt: DateTime.now().toIso8601String(),
      retryCount: 0,
    ));
    _pendingCount++;
    debugPrint('[OfflineSync] Queued message: $id');
  }

  /// Queue a badge evaluation for offline sync
  Future<void> queueBadgeEvaluation({
    required String memberId,
  }) async {
    final id = 'badge-${DateTime.now().millisecondsSinceEpoch}';
    await _db.addToSyncQueue(SyncQueueItem(
      id: id,
      operation: 'CREATE',
      endpoint: '/members/$memberId/badges/evaluate',
      payload: jsonEncode({}),
      createdAt: DateTime.now().toIso8601String(),
      retryCount: 0,
    ));
    _pendingCount++;
  }

  /// Manually trigger sync of all pending items
  Future<OfflineSyncResult> syncPendingItems() => _syncPendingItems();

  Future<OfflineSyncResult> _syncPendingItems() async {
    if (_isSyncing) return OfflineSyncResult(isSyncing: true);

    _isSyncing = true;
    final items = await _db.getPendingSyncItems();

    if (items.isEmpty) {
      _isSyncing = false;
      _pendingCount = 0;
      final result = OfflineSyncResult(synced: 0, failed: 0);
      _syncController.add(result);
      return result;
    }

    int synced = 0;
    int failed = 0;
    const maxRetries = 3;

    for (final item in items) {
      if (item.retryCount >= maxRetries) {
        failed++;
        continue;
      }

      try {
        final payload = jsonDecode(item.payload) as Map<String, dynamic>;
        await _api.post(item.endpoint, data: payload);
        await _db.removeSyncItem(item.id);
        synced++;
        debugPrint('[OfflineSync] Synced: ${item.endpoint}');
      } catch (e) {
        await _db.markSyncFailed(item.id, e.toString(), item.retryCount + 1);
        failed++;
        debugPrint('[OfflineSync] Failed: ${item.endpoint} — $e');
      }
    }

    _pendingCount = items.length - synced;
    _isSyncing = false;

    final result = OfflineSyncResult(synced: synced, failed: failed);
    _syncController.add(result);
    debugPrint('[OfflineSync] Done: $synced synced, $failed failed');
    return result;
  }
}

class OfflineSyncResult {
  final int synced;
  final int failed;
  final bool isSyncing;

  OfflineSyncResult({this.synced = 0, this.failed = 0, this.isSyncing = false});

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
