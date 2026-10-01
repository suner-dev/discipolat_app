import 'dart:convert';
import 'dart:ffi';

import 'package:dio/dio.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sqlite3/open.dart' as sqlite_open;

import 'package:discipolat_mobile/data/local/database.dart';
import 'package:discipolat_mobile/data/local/offline_sync_manager.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/tenant_config.dart';

/// §G5.7 — File d'écriture hors-ligne : idempotence, toggle tenant, batch
/// /sync/batch, retry exponentiel. Faux ApiService sur les VRAIES routes.
class _FakeApi extends ApiService {
  _FakeApi({this.mode}) : super(baseUrl: 'http://fake');
  final String? mode;

  final List<({String path, dynamic data})> calls = [];
  List<Map<String, dynamic>> batchResults = [];
  bool failBatch = false;

  Response _ok(String path, dynamic data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

  DioException _offline(String path) => DioException(
      requestOptions: RequestOptions(path: path),
      type: DioExceptionType.connectionError);

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    calls.add((path: path, data: params));
    if (path == '/admin/settings') {
      if (mode == null) throw _offline(path);
      return _ok(path, {'offlineMode': mode});
    }
    return _ok(path, {});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    calls.add((path: path, data: data));
    if (path == '/sync/batch') {
      if (failBatch) throw _offline(path);
      return _ok(path, {'results': batchResults, 'applied': batchResults.length});
    }
    return _ok(path, {});
  }
}

void main() {
  // Environnement de test hors-ligne : libsqlite3 versionné .so.0 uniquement.
  setUpAll(() {
    sqlite_open.open.overrideFor(
        sqlite_open.OperatingSystem.linux,
        () => DynamicLibrary.open('libsqlite3.so.0'));
  });

  late AppDatabase db;
  late _FakeApi api;
  late OfflineSyncManager manager;

  const tenant = '11111111-2222-3333-4444-555555555555';

  setUp(() async {
    db = AppDatabase.forTesting(NativeDatabase.memory());
    api = _FakeApi();
    manager = OfflineSyncManager(db, api);
    TenantConfig.currentOrgId = tenant;
  });

  tearDown(() async {
    manager.stopListening();
    await db.close();
    TenantConfig.currentOrgId = null;
  });

  Future<List<SyncQueueItem>> pending() => db.getPendingSyncItems(tenant);

  group('toggle tenant offline_mode', () {
    test('LECTURE refuse toute mise en file', () async {
      manager.offlineModeOverride = 'LECTURE';
      expect(await manager.queueQrCheckin('discipolat:soul:abc'), isFalse);
      expect(await manager.queueAssetReturn(itemId: 'i1'), isFalse);
      expect(await pending(), isEmpty);
    });

    test('FIELD_OPS accepte les critiques terrain, refuse le reste', () async {
      manager.offlineModeOverride = 'FIELD_OPS';
      expect(await manager.queueQrCheckin('c1'), isTrue);
      expect(await manager.queueAssetCheckout(itemId: 'i1', memberId: 'm1'), isTrue);
      expect(await manager.queueAssetReturn(itemId: 'i1'), isTrue);
      expect(await manager.queueDamagePhoto(itemId: 'i1', bytes: [1, 2, 3]), isTrue);
      expect(await manager.queueTaskStatus(taskId: 't1', statut: 'FAITE'), isFalse);
      expect(await manager.queueNewVisitor({'prenom': 'Paul'}), isFalse);
      expect(await pending(), hasLength(4));
    });

    test('FULL accepte tout', () async {
      manager.offlineModeOverride = 'FULL';
      expect(await manager.queueTaskStatus(taskId: 't1', statut: 'FAITE'), isTrue);
      expect(await manager.queueNewVisitor({'prenom': 'Paul'}), isTrue);
      expect(await pending(), hasLength(2));
    });

    test('mode lu depuis /admin/settings puis mis en cache', () async {
      api = _FakeApi(mode: 'FULL');
      manager = OfflineSyncManager(db, api);
      expect(await manager.offlineMode(), 'FULL');
      // 2e appel : cache mémoire, plus de requête réseau.
      expect(await manager.offlineMode(), 'FULL');
      expect(
          api.calls.where((c) => c.path == '/admin/settings'), hasLength(1));
    });

    test('settings injoignables → défaut prudent FIELD_OPS', () async {
      api = _FakeApi(mode: null);
      manager = OfflineSyncManager(db, api);
      expect(await manager.offlineMode(), 'FIELD_OPS');
      expect(await manager.queueQrCheckin('c1'), isTrue);
    });
  });

  group('file idempotente + batch /sync/batch', () {
    test('clientUuid unique généré à la saisie et transporté dans le lot',
        () async {
      manager.offlineModeOverride = 'FIELD_OPS';
      await manager.queueQrCheckin('code-a');
      await manager.queueAssetReturn(itemId: 'i1', condition: 'DAMAGED');
      api.batchResults = [
        {'clientUuid': 'x', 'status': 'APPLIED'},
      ];
      final items = await pending();
      expect(items.map((i) => i.clientUuid).toSet(), hasLength(2));
      expect(items.every((i) => i.clientUuid.isNotEmpty), isTrue);

      await manager.syncPendingItems();
      final batchCall =
          api.calls.firstWhere((c) => c.path == '/sync/batch');
      final ops = (batchCall.data['operations'] as List).cast<Map>();
      expect(ops, hasLength(2));
      expect(ops.map((o) => o['type']), containsAll(['QR_CHECKIN', 'ASSET_RETURN']));
      expect(ops.every((o) => (o['clientUuid'] as String).isNotEmpty), isTrue);
      expect(ops.every((o) => o['at'] != null), isTrue);
    });

    test('APPLIED et SKIPPED_DUPLICATE sortent de la file (pas de rejeu)',
        () async {
      manager.offlineModeOverride = 'FIELD_OPS';
      await manager.queueQrCheckin('code-a');
      await manager.queueQrCheckin('code-b');
      final items = await pending();
      api.batchResults = [
        {'clientUuid': items[0].clientUuid, 'status': 'APPLIED'},
        {'clientUuid': items[1].clientUuid, 'status': 'SKIPPED_DUPLICATE'},
      ];
      final result = await manager.syncPendingItems();
      expect(result.synced, 2);
      expect(await pending(), isEmpty);
    });

    test('CONFLICT_LWW sort de la file (trace côté serveur, pas de boucle)',
        () async {
      manager.offlineModeOverride = 'FULL';
      await manager.queuePresenceEntry(
          departmentId: 'd1', date: '2026-09-14', items: [
        {'soulId': 's1', 'present': true}
      ]);
      final items = await pending();
      expect(items.single.syncType, 'PRESENCE_SUBMIT');
      api.batchResults = [
        {'clientUuid': items.single.clientUuid, 'status': 'CONFLICT_LWW'},
      ];
      await manager.syncPendingItems();
      expect(await pending(), isEmpty);
    });

    test('photo de dommage voyage en base64 dans le lot', () async {
      manager.offlineModeOverride = 'FIELD_OPS';
      await manager.queueDamagePhoto(itemId: 'i1', bytes: [0xFF, 0xD8, 0x01]);
      api.batchResults = [];
      await manager.syncPendingItems();
      final ops = (api.calls
              .firstWhere((c) => c.path == '/sync/batch')
              .data['operations'] as List)
          .cast<Map>();
      final op = ops.single;
      expect(op['type'], 'ASSET_DAMAGE_PHOTO');
      expect(base64Decode(op['photoBase64'] as String), [0xFF, 0xD8, 0x01]);
      expect(op['photoMime'], 'image/jpeg');
    });

    test('échec réseau : backoff exponentiel, pas de mitraillette', () async {
      manager.offlineModeOverride = 'FIELD_OPS';
      await manager.queueQrCheckin('code-a');
      api.failBatch = true;
      await manager.syncPendingItems(); // tentative 1 → retryCount 1
      final after = (await pending()).single;
      expect(after.retryCount, 1);
      // Tentative 2 immédiatement : backoff non écoulé → rien n'est renvoyé.
      await manager.syncPendingItems();
      expect(api.calls.where((c) => c.path == '/sync/batch'), hasLength(1));
    });

    test('max retries atteint → FAILED_MAX_RETRIES, sort des pending',
        () async {
      manager.offlineModeOverride = 'FIELD_OPS';
      await manager.queueQrCheckin('code-a');
      final item = (await pending()).single;
      await db.markSyncFailed(item.id, 'x', 5);
      await db.setSyncStatus(item.id, 'FAILED_MAX_RETRIES');
      expect(await pending(), isEmpty);
      final result = await manager.syncPendingItems();
      expect(result.synced, 0);
      expect(api.calls.where((c) => c.path == '/sync/batch'), isEmpty);
    });

    test('backoffFor croît et est plafonné', () {
      expect(OfflineSyncManager.backoffFor(0), const Duration(seconds: 30));
      expect(OfflineSyncManager.backoffFor(1), const Duration(minutes: 1));
      expect(OfflineSyncManager.backoffFor(2), const Duration(minutes: 2));
      expect(OfflineSyncManager.backoffFor(9), const Duration(minutes: 16));
    });

    test('items legacy (syncType vide) restent en replay individuel',
        () async {
      await db.addToSyncQueue(SyncQueueTableCompanion.insert(
        id: 'legacy-1',
        tenantId: tenant,
        operation: 'CREATE',
        endpoint: '/prayers/actions-de-grace',
        payload: jsonEncode({'contenu': 'x'}),
        createdAt: DateTime.now().toIso8601String(),
      ));
      final result = await manager.syncPendingItems();
      expect(result.synced, 1);
      expect(
          api.calls.any((c) => c.path == '/prayers/actions-de-grace'), isTrue);
      expect(api.calls.where((c) => c.path == '/sync/batch'), isEmpty);
      expect(await pending(), isEmpty);
    });
  });
}
