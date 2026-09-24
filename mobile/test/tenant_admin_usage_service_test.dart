import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/tenant_admin_usage_service.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({
    this.dashboard = const {},
    this.quotas = const {},
    this.settings = const {},
    this.failPaths = const {},
  }) : super(baseUrl: 'http://fake');

  final dynamic dashboard;
  final dynamic quotas;
  final dynamic settings;
  final Set<String> failPaths;
  final List<String> paths = [];

  @override
  Future<Response> get(
    String path, {
    Map<String, dynamic>? params,
    Map<String, dynamic>? queryParameters,
  }) async {
    paths.add(path);
    if (failPaths.contains(path)) {
      throw DioException(requestOptions: RequestOptions(path: path));
    }
    final data = switch (path) {
      '/admin/dashboard/overview' => dashboard,
      '/admin/quotas/usage' => quotas,
      '/admin/settings' => settings,
      _ => <String, dynamic>{},
    };
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: 200,
      data: data,
    );
  }
}

void main() {
  test('loads persisted tenant metrics and the quota endpoint', () async {
    final api = _FakeApiService(
      dashboard: {
        'tenantName': 'Église test',
        'users': {
          'total': 12,
          'active': 10,
          'membersByRole': {'ADMIN': 2, 'PASTEUR': 1},
        },
        'organizations': {
          'churches': 2,
          'departments': 4,
        },
      },
      quotas: {
        'users': {'used': 12, 'limit': 20, 'percent': 60},
        'storage': {'usedMb': 5, 'limitMb': 10, 'percent': 50},
      },
      settings: {'analyticsEnabled': true},
    );

    final snapshot = await TenantAdminUsageService(apiService: api).load();

    expect(snapshot.dashboard?.tenantName, 'Église test');
    expect(snapshot.dashboard?.totalUsers, 12);
    expect(snapshot.dashboard?.activeUsers, 10);
    expect(snapshot.dashboard?.membersByRole['ADMIN'], 2);
    expect(snapshot.quotaUsage?.metric('users')?.used, 12);
    expect(snapshot.quotaUsage?.metric('storage')?.limit, 10);
    expect(snapshot.analyticsEnabled, isTrue);
    expect(
      api.paths,
      unorderedEquals(const [
        '/admin/dashboard/overview',
        '/admin/quotas/usage',
        '/admin/settings',
      ]),
    );
  });

  test('keeps missing quota fields unavailable', () async {
    final api = _FakeApiService(
      quotas: const {},
      settings: const {'analyticsEnabled': false},
    );

    final snapshot = await TenantAdminUsageService(apiService: api).load();

    expect(snapshot.quotaUsage, isNotNull);
    expect(snapshot.quotaUsage!.metrics, isEmpty);
    expect(snapshot.analyticsEnabled, isFalse);
    expect(snapshot.quotaFailed, isFalse);
  });

  test('reports endpoint failures without replacing them with values',
      () async {
    final api = _FakeApiService(
      failPaths: const {
        '/admin/dashboard/overview',
        '/admin/quotas/usage',
        '/admin/settings',
      },
    );

    final snapshot = await TenantAdminUsageService(apiService: api).load();

    expect(snapshot.dashboard, isNull);
    expect(snapshot.quotaUsage, isNull);
    expect(snapshot.analyticsEnabled, isNull);
    expect(snapshot.dashboardFailed, isTrue);
    expect(snapshot.quotaFailed, isTrue);
    expect(snapshot.settingsFailed, isTrue);
  });
}
