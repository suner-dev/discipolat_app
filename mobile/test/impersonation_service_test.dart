import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/impersonation_service.dart';
import 'package:discipolat_mobile/tenant_config.dart';

class _RecordingApiService extends ApiService {
  _RecordingApiService() : super(baseUrl: 'http://fake');

  final List<String> postPaths = [];
  final List<Map<String, dynamic>> postData = [];
  bool failStop = false;

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postData.add(
        data is Map ? Map<String, dynamic>.from(data) : <String, dynamic>{});
    if (path == '/platform/admin/impersonation') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: {
          'impersonationToken': 'impersonation-token',
          'targetUserId': 'target-user',
          'tenantId': 'tenant-target',
          'tenantName': 'Église Cible',
          'targetRole': 'MEMBRE',
          'startTime': '2026-09-24T10:00:00Z',
          'expiresAt': '2026-09-24T10:30:00Z',
        },
      );
    }
    if (path == '/platform/admin/impersonation/stop' && failStop) {
      throw DioException(
        requestOptions: RequestOptions(path: path),
        type: DioExceptionType.unknown,
      );
    }
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 204,
      data: null,
    );
  }
}

const _storage = FlutterSecureStorage();
const _metadataKey = 'impersonation_metadata';
const _adminAccessTokenKey = 'impersonation_admin_access_token';
const _adminRefreshTokenKey = 'impersonation_admin_refresh_token';

Future<void> _resetStorage() async {
  FlutterSecureStorage.setMockInitialValues({});
  SharedPreferences.setMockInitialValues({});
  await _storage.deleteAll();
  await TenantConfig.clearOrgId();
}

Future<void> _setAdminSession() async {
  await _storage.write(key: 'access_token', value: 'admin-access');
  await _storage.write(key: 'refresh_token', value: 'admin-refresh');
  AuthState().setAuthenticated(true, userData: {
    'userId': 'admin-user',
    'email': 'admin@platform.test',
    'role': 'PLATFORM_SUPER_ADMIN',
    'roles': ['PLATFORM_SUPER_ADMIN'],
    'platformRoles': ['PLATFORM_SUPER_ADMIN'],
    'platformSuperAdmin': true,
    'activeRole': 'PLATFORM_SUPER_ADMIN',
    'orgId': 'admin-org',
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() async {
    await _resetStorage();
    await _setAdminSession();
  });

  tearDown(() async {
    AuthState().logout();
    await _resetStorage();
  });

  test('start swaps and persists the target identity with the exact contract',
      () async {
    final api = _RecordingApiService();
    final service = ImpersonationService(apiService: api);

    await service.start(
      targetEmail: 'target@example.com',
      tenantId: ' tenant-target ',
      reason: 'Diagnostic request',
    );

    expect(api.postPaths, ['/platform/admin/impersonation']);
    expect(api.postData.single, {
      'tenantId': 'tenant-target',
      'targetUserEmail': 'target@example.com',
      'reason': 'Diagnostic request',
    });
    expect(service.isImpersonating, isTrue);
    expect(service.targetUserId, 'target-user');
    expect(service.targetTenantId, 'tenant-target');
    expect(AuthState().userId, 'target-user');
    expect(AuthState().isPlatformSuperAdmin, isFalse);
    expect(TenantConfig.currentOrgId, 'tenant-target');
    expect(await _storage.read(key: 'access_token'), 'impersonation-token');
    expect(await _storage.read(key: 'refresh_token'), isNull);
    expect(await _storage.read(key: _adminAccessTokenKey), 'admin-access');
    expect(await _storage.read(key: _adminRefreshTokenKey), 'admin-refresh');
    final metadata = jsonDecode((await _storage.read(key: _metadataKey))!);
    expect(metadata['impersonationToken'], 'impersonation-token');
    expect(metadata['originalOrgId'], 'admin-org');
  });

  test('stop restores the administrator even when server logging fails',
      () async {
    final api = _RecordingApiService();
    final service = ImpersonationService(apiService: api);
    await service.start(
      targetEmail: 'target@example.com',
      tenantId: 'tenant-target',
      reason: 'Diagnostic request',
    );
    api.failStop = true;

    await service.stop();

    expect(api.postPaths.last, '/platform/admin/impersonation/stop');
    expect(api.postData.last, {'impersonationToken': 'impersonation-token'});
    expect(await _storage.read(key: 'access_token'), 'admin-access');
    expect(await _storage.read(key: 'refresh_token'), 'admin-refresh');
    expect(await _storage.read(key: _metadataKey), isNull);
    expect(AuthState().userId, 'admin-user');
    expect(AuthState().isPlatformSuperAdmin, isTrue);
    expect(TenantConfig.currentOrgId, 'admin-org');
    expect(service.isImpersonating, isFalse);
  });

  test('restore reconstructs impersonation without another API call', () async {
    final api = _RecordingApiService();
    final service = ImpersonationService(apiService: api);
    await service.start(
      targetEmail: 'target@example.com',
      tenantId: 'tenant-target',
      reason: 'Diagnostic request',
    );

    final restored = ImpersonationService(apiService: api);
    await restored.restoreIfNeeded();

    expect(api.postPaths, hasLength(1));
    expect(restored.isImpersonating, isTrue);
    expect(restored.targetEmail, 'target@example.com');
    expect(AuthState().userId, 'target-user');
    expect(AuthState().isPlatformSuperAdmin, isFalse);
    expect(TenantConfig.currentOrgId, 'tenant-target');
  });

  test('restore rolls back an interrupted token swap', () async {
    await _storage.write(key: _adminAccessTokenKey, value: 'admin-access');
    await _storage.write(key: _adminRefreshTokenKey, value: 'admin-refresh');
    await _storage.write(
      key: _metadataKey,
      value: jsonEncode({
        'impersonationToken': 'impersonation-token',
        'targetUserId': 'target-user',
        'targetEmail': 'target@example.com',
        'targetRole': 'MEMBRE',
        'targetTenantId': 'tenant-target',
        'targetTenantName': 'Église Cible',
        'expiresAt': '2026-09-24T10:30:00Z',
        'originalOrgId': 'admin-org',
        'adminUser': {
          'userId': 'admin-user',
          'email': 'admin@platform.test',
          'role': 'PLATFORM_SUPER_ADMIN',
          'roles': ['PLATFORM_SUPER_ADMIN'],
          'platformRoles': ['PLATFORM_SUPER_ADMIN'],
          'platformSuperAdmin': true,
          'activeRole': 'PLATFORM_SUPER_ADMIN',
          'orgId': 'admin-org',
        },
      }),
    );

    final service = ImpersonationService(apiService: _RecordingApiService());
    await service.restoreIfNeeded();

    expect(service.isImpersonating, isFalse);
    expect(await _storage.read(key: 'access_token'), 'admin-access');
    expect(await _storage.read(key: 'refresh_token'), 'admin-refresh');
    expect(await _storage.read(key: _metadataKey), isNull);
    expect(AuthState().userId, 'admin-user');
    expect(AuthState().isPlatformSuperAdmin, isTrue);
    expect(TenantConfig.currentOrgId, 'admin-org');
  });
}
