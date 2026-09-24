import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/invitations/accept_invitation_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<String> getPaths = [];
  final List<String> postPaths = [];
  final List<Map<String, dynamic>> postData = [];
  DioException? getError;
  DioException? postError;
  bool accountExists = false;

  @override
  Future<Response> get(
    String path, {
    Map<String, dynamic>? params,
    Map<String, dynamic>? queryParameters,
  }) async {
    getPaths.add(path);
    if (getError != null) throw getError!;
    return _json(path, {
      'valid': true,
      'email': 'invitee@example.com',
      'role': 'MEMBER',
      'scopeType': 'TENANT',
      'tenantName': 'Église Bethel',
      'organizationName': 'Département Accueil',
      'expiresAt': '2026-10-01T12:00:00Z',
      'accountExists': accountExists,
    });
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postData.add(
        data is Map ? Map<String, dynamic>.from(data) : <String, dynamic>{});
    if (postError != null) throw postError!;
    return _json(path, {'success': true});
  }

  Response<dynamic> _json(String path, Object data) => Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: data,
      );
}

DioException _httpError(int status, Object data) => DioException(
      requestOptions: RequestOptions(path: '/invitation'),
      response: Response(
        requestOptions: RequestOptions(path: '/invitation'),
        statusCode: status,
        data: data,
      ),
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  Future<void> pumpScreen(
    WidgetTester tester,
    ApiService api, {
    String? token,
    VoidCallback? onCompleted,
  }) async {
    await tester.binding.setSurfaceSize(const Size(900, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(
      MaterialApp(
        home: AcceptInvitationScreen(
          initialToken: token,
          apiService: api,
          onCompleted: onCompleted,
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> fillNewAccountForm(WidgetTester tester) async {
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Prénom'),
      ' Jean ',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Nom'),
      ' Dupont ',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Mot de passe'),
      'password123',
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Confirmer le mot de passe'),
      'password123',
    );
  }

  testWidgets('validates without accepting and never renders the token',
      (tester) async {
    const token = 'abcdef0123456789abcdef0123456789';
    final api = _FakeApiService();
    await pumpScreen(tester, api, token: token);

    expect(api.getPaths, ['/admin/invitations/validate/$token']);
    expect(api.postPaths, isEmpty);
    expect(find.text('invitee@example.com'), findsOneWidget);
    expect(find.textContaining('Église Bethel'), findsWidgets);
    expect(find.text('Département Accueil'), findsOneWidget);
    expect(find.text(token), findsNothing);
  });

  testWidgets('creates a new account with the exact acceptance payload',
      (tester) async {
    const token = 'abcdef0123456789abcdef0123456789';
    final api = _FakeApiService();
    var completed = false;
    await pumpScreen(
      tester,
      api,
      token: token,
      onCompleted: () => completed = true,
    );
    await fillNewAccountForm(tester);

    await tester.tap(find.text('Créer mon compte'));
    await tester.pumpAndSettle();

    expect(api.postPaths, ['/admin/invitations/accept/$token']);
    expect(api.postData.single, {
      'firstName': 'Jean',
      'lastName': 'Dupont',
      'password': 'password123',
    });
    expect(completed, isTrue);
  });

  testWidgets('accepts an existing account without asking for credentials',
      (tester) async {
    const token = 'abcdef0123456789abcdef0123456789';
    final api = _FakeApiService()..accountExists = true;
    var completed = false;
    await pumpScreen(
      tester,
      api,
      token: token,
      onCompleted: () => completed = true,
    );

    expect(find.text('Prénom'), findsNothing);
    expect(find.text('Mot de passe'), findsNothing);
    await tester.tap(find.text('Accepter l’invitation'));
    await tester.pumpAndSettle();

    expect(api.postData.single, isEmpty);
    expect(completed, isTrue);
  });

  testWidgets('rejects a malformed manual token without an API call',
      (tester) async {
    final api = _FakeApiService();
    await pumpScreen(tester, api);

    await tester.enterText(
      find.widgetWithText(TextField, 'Code d’invitation'),
      'invalid',
    );
    await tester.tap(find.text('Vérifier l’invitation'));
    await tester.pump();

    expect(api.getPaths, isEmpty);
    expect(find.text('Code d’invitation invalide.'), findsOneWidget);
  });

  testWidgets('maps expired invitations to a generic terminal error',
      (tester) async {
    const token = 'abcdef0123456789abcdef0123456789';
    final api = _FakeApiService()
      ..getError = _httpError(410, {
        'title': 'INVITATION_EXPIRED',
        'detail': 'Invitation expirée',
      });
    await pumpScreen(tester, api, token: token);

    expect(
      find.text('Cette invitation est invalide, expirée ou déjà utilisée.'),
      findsOneWidget,
    );
    expect(find.textContaining(token), findsNothing);
  });

  testWidgets('treats a lost acceptance response as completed after commit',
      (tester) async {
    const token = 'abcdef0123456789abcdef0123456789';
    final api = _FakeApiService()
      ..postError = _httpError(410, {
        'title': 'INVITATION_NOT_PENDING',
        'details': {'status': 'ACCEPTED'},
      });
    var completed = false;
    await pumpScreen(
      tester,
      api,
      token: token,
      onCompleted: () => completed = true,
    );
    await fillNewAccountForm(tester);

    await tester.tap(find.text('Créer mon compte'));
    await tester.pumpAndSettle();

    expect(completed, isTrue);
  });
}
