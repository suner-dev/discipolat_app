import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/tenant/tenant_admin_dashboard_screen.dart';

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

  @override
  Future<Response> get(
    String path, {
    Map<String, dynamic>? params,
    Map<String, dynamic>? queryParameters,
  }) async {
    if (failPaths.contains(path)) {
      throw DioException(requestOptions: RequestOptions(path: path));
    }
    final data = switch (path) {
      '/admin/dashboard' => dashboard,
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

Future<void> _pump(
  WidgetTester tester,
  ApiService api,
) async {
  await tester.binding.setSurfaceSize(const Size(800, 1600));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(
    MaterialApp(home: TenantAdminDashboardScreen(apiService: api)),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('renders finite and unlimited persisted metrics', (tester) async {
    await _pump(
      tester,
      _FakeApiService(
        dashboard: {
          'tenantName': 'Église réelle',
          'totalUsers': 12,
          'activeUsers': 10,
          'churchCount': 2,
        },
        quotas: {
          'users': {'used': 12, 'limit': 20, 'percent': 60},
          'storage': {'usedMb': 5, 'limitMb': null},
        },
        settings: {'analyticsEnabled': true},
      ),
    );

    expect(find.text('Église réelle'), findsOneWidget);
    expect(find.text('12'), findsWidgets);
    expect(find.text('12 / 20'), findsOneWidget);
    expect(find.text('5 / Illimité'), findsOneWidget);
    expect(find.text('Analytics disponibles'), findsOneWidget);
  });

  testWidgets('renders disabled analytics without requesting usage data', (tester) async {
    final api = _FakeApiService(
      quotas: const {},
      settings: const {'analyticsEnabled': false},
    );

    await _pump(tester, api);

    expect(find.text('Analytics désactivés par le tenant'), findsOneWidget);
    expect(find.text('Données de consommation indisponibles'), findsOneWidget);
  });

  testWidgets('renders explicit unavailable states for missing fields', (tester) async {
    await _pump(
      tester,
      _FakeApiService(
        dashboard: const {},
        quotas: const {},
        settings: const {},
      ),
    );

    expect(find.text('Indisponible'), findsWidgets);
    expect(find.text('Données du tableau de bord indisponibles'), findsNothing);
    expect(find.text('Données de consommation indisponibles'), findsNothing);
    expect(find.text('Statut des analytics indisponible'), findsWidgets);
  });

  testWidgets('renders explicit error states when endpoints fail', (tester) async {
    await _pump(
      tester,
      _FakeApiService(
        failPaths: const {
          '/admin/dashboard',
          '/admin/quotas/usage',
          '/admin/settings',
        },
      ),
    );

    expect(find.text('Données du tableau de bord indisponibles'), findsOneWidget);
    expect(find.text('Données de consommation indisponibles'), findsOneWidget);
    expect(find.text('Statut des analytics indisponible'), findsWidgets);
  });
}
