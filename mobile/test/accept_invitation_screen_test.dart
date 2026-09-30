// B11 — Auto-login après acceptation d'invitation (constat MO3/mineur).
//
// Couvre le parcours réel :
// - compte créé (mot de passe saisi) → POST /auth/login + saveTokens + AuthState,
//   puis redirection vers l'espace du rôle (roleHome) ;
// - compte existant (aucun mot de passe) → AUCUN login, écran « connectez-vous » ;
// - crossTenantIdentity → redirection /tenant-selection après login ;
// - échec du login auto → repli non bloquant (jamais de boucle, un seul appel).
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/invitations/accept_invitation_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<String> getPaths = [];
  final List<String> postPaths = [];
  final List<Map<String, dynamic>> postData = [];
  DioException? getError;

  bool accountExists = false;
  bool crossTenant = false;
  bool loginShouldFail = false;

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    if (getError != null) throw getError!;
    return _json(path, {
      'valid': true,
      'email': 'invitee@example.com',
      'role': 'MEMBRE',
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

    if (path.startsWith('/admin/invitations/accept/')) {
      return _json(path, {
        'success': true,
        'email': 'invitee@example.com',
        'crossTenantIdentity': crossTenant,
      });
    }
    if (path == '/auth/login') {
      if (loginShouldFail) {
        throw DioException(
          requestOptions: RequestOptions(path: path),
          response: Response(
            requestOptions: RequestOptions(path: path),
            statusCode: 401,
            data: {'detail': 'identifiants invalides'},
          ),
        );
      }
      // Réponse de login conforme au contrat mobile (accessToken + rôle actif).
      return _json(path, {
        'accessToken': 'jwt-access',
        'refreshToken': 'jwt-refresh',
        'userId': 'u-1',
        'email': 'invitee@example.com',
        'role': 'MEMBRE',
        'roles': ['MEMBRE'],
        'activeRole': 'MEMBRE',
        'platformSuperAdmin': false,
      });
    }
    return _json(path, {'success': true});
  }

  Response<dynamic> _json(String path, Object data) => Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: data,
      );
}

const String token = 'abcdef0123456789abcdef0123456789';

/// Monte l'écran dans un vrai GoRouter pour observer la destination finale.
Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
  await tester.binding.setSurfaceSize(const Size(900, 1800));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final router = GoRouter(
    initialLocation: '/accept',
    routes: [
      GoRoute(
        path: '/accept',
        builder: (_, __) =>
            AcceptInvitationScreen(initialToken: token, apiService: api),
      ),
      GoRoute(
          path: '/login',
          builder: (_, __) => const Scaffold(body: Text('LOGIN_PAGE'))),
      GoRoute(
          path: '/tenant-selection',
          builder: (_, __) => const Scaffold(body: Text('SELECT_PAGE'))),
      GoRoute(
          path: '/dashboard/membre',
          builder: (_, __) => const Scaffold(body: Text('MEMBRE_HOME'))),
    ],
  );
  await tester.pumpWidget(MaterialApp.router(routerConfig: router));
  await tester.pumpAndSettle();
}

Future<void> fillNewAccountForm(WidgetTester tester) async {
  await tester.enterText(find.widgetWithText(TextFormField, 'Prénom'), ' Jean ');
  await tester.enterText(find.widgetWithText(TextFormField, 'Nom'), ' Dupont ');
  await tester.enterText(
      find.widgetWithText(TextFormField, 'Mot de passe'), 'password123');
  await tester.enterText(find.widgetWithText(TextFormField, 'Confirmer le mot de passe'),
      'password123');
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    // FlutterSecureStorage (utilisé par saveTokens) est un canal natif :
    // on le neutralise pour les tests widget.
    const channel =
        MethodChannel('plugins.it_nomads.com/flutter_secure_storage');
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async => null);
  });

  testWidgets('compte créé → login automatique + redirection roleHome',
      (tester) async {
    final api = _FakeApiService();
    await pumpScreen(tester, api);

    await fillNewAccountForm(tester);
    await tester.tap(find.text('Créer mon compte'));
    await tester.pumpAndSettle();

    // contrat §3.4 : acceptation, puis login automatique (un seul appel).
    expect(api.postPaths, [
      '/admin/invitations/accept/$token',
      '/auth/login',
    ]);
    expect(api.postPaths.where((p) => p == '/auth/login').length, 1);
    // Corps de login : email de l'invitation + mot de passe saisi.
    expect(api.postData[1], {
      'email': 'invitee@example.com',
      'password': 'password123',
    });
    // Destination : espace du rôle actif.
    expect(find.text('MEMBRE_HOME'), findsOneWidget);
  });

  testWidgets('compte existant → aucun login, écran « connectez-vous »',
      (tester) async {
    final api = _FakeApiService()..accountExists = true;
    await pumpScreen(tester, api);

    // Aucun champ de créance pour un compte existant.
    expect(find.text('Prénom'), findsNothing);
    expect(find.text('Mot de passe'), findsNothing);
    await tester.tap(find.text('Accepter l’invitation'));
    await tester.pumpAndSettle();

    // Pas de tentative de login automatique.
    expect(api.postPaths, ['/admin/invitations/accept/$token']);
    expect(find.text('Invitation acceptée. Connectez-vous avec votre mot de passe.'),
        findsOneWidget);
    expect(find.text('Se connecter'), findsOneWidget);
  });

  testWidgets('crossTenantIdentity → redirection /tenant-selection après login',
      (tester) async {
    final api = _FakeApiService()..crossTenant = true;
    await pumpScreen(tester, api);

    await fillNewAccountForm(tester);
    await tester.tap(find.text('Créer mon compte'));
    await tester.pumpAndSettle();

    expect(api.postPaths, [
      '/admin/invitations/accept/$token',
      '/auth/login',
    ]);
    expect(find.text('SELECT_PAGE'), findsOneWidget);
  });

  testWidgets('échec du login auto → repli non bloquant, un seul appel',
      (tester) async {
    final api = _FakeApiService()..loginShouldFail = true;
    await pumpScreen(tester, api);

    await fillNewAccountForm(tester);
    await tester.tap(find.text('Créer mon compte'));
    await tester.pumpAndSettle();

    // Un unique appel /auth/login, puis repli (jamais de boucle).
    expect(api.postPaths.where((p) => p == '/auth/login').length, 1);
    expect(find.textContaining('Finalisez la connexion depuis'), findsOneWidget);
    expect(find.text('Se connecter'), findsOneWidget);
  });

  testWidgets('invitation invalide → erreur terminale, aucun accept',
      (tester) async {
    final api = _FakeApiService()
      ..getError = DioException(
        requestOptions: RequestOptions(path: '/validate'),
        response: Response(
          requestOptions: RequestOptions(path: '/validate'),
          statusCode: 410,
          data: {'title': 'INVITATION_EXPIRED'},
        ),
      );
    await tester.binding.setSurfaceSize(const Size(900, 1800));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(
      home: AcceptInvitationScreen(initialToken: token, apiService: api),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Cette invitation est invalide, expirée ou déjà utilisée.'),
        findsOneWidget);
    expect(api.postPaths, isEmpty);
  });
}
