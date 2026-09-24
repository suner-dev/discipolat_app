import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/platform/platform_registration_requests_screen.dart';

class _RecordingApiService extends ApiService {
  _RecordingApiService() : super(baseUrl: 'http://fake');

  final List<String> getPaths = [];
  final List<Map<String, dynamic>> getQueries = [];
  final List<String> postPaths = [];
  final List<Map<String, dynamic>> postData = [];
  bool failFirstGet = false;

  @override
  Future<Response> get(
    String path, {
    Map<String, dynamic>? params,
    Map<String, dynamic>? queryParameters,
  }) async {
    getPaths.add(path);
    getQueries.add(Map<String, dynamic>.from(queryParameters ?? const {}));
    if (failFirstGet) {
      failFirstGet = false;
      throw DioException(
        requestOptions: RequestOptions(path: path),
        type: DioExceptionType.connectionError,
      );
    }
    return _json(path, {
      'content': [
        {
          'id': 'request-1',
          'organizationName': 'Église de la Grâce',
          'firstName': 'Marie',
          'lastName': 'Dupont',
          'email': 'marie@example.com',
          'createdAt': '2026-09-24T10:00:00Z',
        },
      ],
      'totalPages': 2,
    });
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postData.add(
        data is Map ? Map<String, dynamic>.from(data) : <String, dynamic>{});
    return _json(path, {});
  }

  Response<dynamic> _json(String path, Object data) => Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: data,
      );
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
    AuthState().setAuthenticated(true, userData: {
      'userId': 'admin-user',
      'email': 'admin@platform.test',
      'role': 'PLATFORM_SUPER_ADMIN',
      'roles': ['PLATFORM_SUPER_ADMIN'],
      'platformRoles': ['PLATFORM_SUPER_ADMIN'],
      'platformSuperAdmin': true,
      'activeRole': 'PLATFORM_SUPER_ADMIN',
    });
  });

  tearDown(() => AuthState().logout());

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.binding.setSurfaceSize(const Size(900, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp(
          home: PlatformRegistrationRequestsScreen(apiService: api),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('loads and paginates registration requests', (tester) async {
    final api = _RecordingApiService();
    await pumpScreen(tester, api);

    expect(api.getPaths, ['/platform/admin/registration-requests']);
    expect(api.getQueries.first, {'page': 0, 'size': 50});
    expect(find.text('Église de la Grâce'), findsOneWidget);
    expect(find.text('Marie Dupont'), findsOneWidget);
    expect(find.text('marie@example.com'), findsOneWidget);
    expect(find.text('Demandée le 24/9/2026'), findsOneWidget);

    await tester.tap(find.text('Suivant'));
    await tester.pumpAndSettle();

    expect(api.getQueries.last, {'page': 1, 'size': 50});
  });

  testWidgets('approves with a trimmed reason and reloads', (tester) async {
    final api = _RecordingApiService();
    await pumpScreen(tester, api);

    await tester.tap(find.text('Approuver'));
    await tester.pumpAndSettle();
    await tester.enterText(
        find.widgetWithText(TextField, 'Motif'), '  Dossier complet  ');
    await tester.tap(find.text('Confirmer'));
    await tester.pumpAndSettle();

    expect(api.postPaths,
        ['/platform/admin/registration-requests/request-1/approve']);
    expect(api.postData.single, {'reason': 'Dossier complet'});
    expect(api.getPaths, hasLength(2));
  });

  testWidgets('shows an error and retries a failed initial load',
      (tester) async {
    final api = _RecordingApiService()..failFirstGet = true;
    await pumpScreen(tester, api);

    expect(find.text('Impossible de charger les demandes.'), findsOneWidget);
    expect(find.text('Réessayer'), findsOneWidget);

    await tester.tap(find.text('Réessayer'));
    await tester.pumpAndSettle();

    expect(find.text('Église de la Grâce'), findsOneWidget);
    expect(api.getPaths, hasLength(2));
  });
}
