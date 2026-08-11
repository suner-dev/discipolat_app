import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/models/platform_meta.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/data/services/providers.dart';
import 'package:discipolat_mobile/presentation/screens/login/login_screen.dart';
import 'package:discipolat_mobile/presentation/widgets/app_drawer.dart';
import 'package:discipolat_mobile/presentation/widgets/beta_badge.dart';
import 'package:discipolat_mobile/presentation/widgets/feedback_sheet.dart';

/// ApiService factice : sert les méta-données publiques et enregistre les
/// soumissions de feedback (POST /feedback), avec mode d'échec simulable.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<Map<String, dynamic>> feedbackPosts = [];
  bool shouldFailFeedback = false;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/public/meta') {
      return _json(path, {
        'appName': 'Discipolat',
        'version': '1.0.0',
        'environment': 'beta',
        'betaMode': true,
        'demoAccountsEnabled': true,
      });
    }
    throw StateError('Chemin inattendu: $path');
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    if (path == '/feedback') {
      if (shouldFailFeedback) {
        throw DioException(requestOptions: RequestOptions(path: path));
      }
      feedbackPosts.add((data as Map<String, dynamic>).cast<String, dynamic>());
      return _json(path, {'id': 'fb-1', 'status': 'NOUVEAU'});
    }
    throw StateError('Chemin inattendu: $path');
  }

  Response<dynamic> _json(String path, Object data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);
}

/// Harness : bouton qui ouvre la feuille de feedback (contexte Scaffold réel).
class _FeedbackHarness extends StatelessWidget {
  const _FeedbackHarness({required this.apiService});
  final ApiService apiService;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Builder(
        builder: (ctx) => Center(
          child: ElevatedButton(
            onPressed: () => showFeedbackSheet(ctx, apiService: apiService, pageUrl: '/crm-faiseur'),
            child: const Text('Ouvrir le retour'),
          ),
        ),
      ),
    );
  }
}

Future<void> _pumpFeedbackSheet(WidgetTester tester, _FakeApiService api) async {
  await tester.pumpWidget(MaterialApp(home: _FeedbackHarness(apiService: api)));
  await tester.tap(find.text('Ouvrir le retour'));
  await tester.pumpAndSettle();
}

void _setRole(String role) {
  AuthState().setAuthenticated(true, userData: {
    'userId': 'user-1',
    'email': 'testeur@discipolat.test',
    'roles': [role],
    'activeRole': role,
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() => _setRole('FAISEUR'));
  tearDown(() => AuthState().logout());

  group('BetaBadge', () {
    Widget wrap(PlatformMeta meta) => ProviderScope(
          overrides: [metaProvider.overrideWith((ref) async => meta)],
          child: const MaterialApp(
            home: Scaffold(body: Center(child: BetaBadge())),
          ),
        );

    testWidgets('masqué quand le serveur ne déclare pas le mode bêta (prod)', (tester) async {
      await tester.pumpWidget(wrap(const PlatformMeta(betaMode: false)));
      await tester.pumpAndSettle();
      expect(find.text('BÊTA'), findsNothing);
    });

    testWidgets('affiché quand le serveur déclare le mode bêta', (tester) async {
      await tester.pumpWidget(wrap(const PlatformMeta(betaMode: true)));
      await tester.pumpAndSettle();
      expect(find.text('BÊTA'), findsOneWidget);
    });
  });

  group('LoginScreen — comptes de démonstration conditionnels', () {
    Widget wrap(PlatformMeta meta) => ProviderScope(
          overrides: [metaProvider.overrideWith((ref) async => meta)],
          child: const MaterialApp(home: LoginScreen()),
        );

    testWidgets('masqués en production (demoAccountsEnabled=false — fail-closed)', (tester) async {
      await tester.pumpWidget(wrap(const PlatformMeta(betaMode: false, demoAccountsEnabled: false)));
      await tester.pumpAndSettle();

      expect(find.textContaining('Comptes de démonstration'), findsNothing);
      expect(find.text('pasteur@discipolat.com'), findsNothing);
      expect(find.text('BÊTA'), findsNothing);
    });

    testWidgets('affichés en environnement bêta, avec le badge BÊTA', (tester) async {
      await tester.pumpWidget(wrap(const PlatformMeta(betaMode: true, demoAccountsEnabled: true)));
      await tester.pumpAndSettle();

      expect(find.textContaining('Comptes de démonstration (bêta)'), findsOneWidget);
      expect(find.text('pasteur@discipolat.com'), findsOneWidget);
      expect(find.text('admin@discipolat.com'), findsOneWidget);
      expect(find.text('paul@discipolat.com'), findsOneWidget);
      expect(find.text('Mot de passe : password123'), findsOneWidget);
      expect(find.text('BÊTA'), findsOneWidget);
    });
  });

  group('AppDrawer — entrée « Un retour ? »', () {
    testWidgets('l\'entrée ouvre la feuille de feedback depuis le drawer', (tester) async {
      _setRole('MEMBRE'); // espace court : l'entrée est visible sans trop scroller
      await tester.pumpWidget(
        ProviderScope(
          child: MaterialApp(
            home: Scaffold(
              appBar: AppBar(title: const Text('Mon espace')),
              drawer: const AppDrawer(),
              body: const SizedBox(),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      // Ouvrir le drawer (programmatiquement, plus fiable que le tap).
      tester.state<ScaffoldState>(find.byType(Scaffold).first).openDrawer();
      await tester.pumpAndSettle();

      // L'entrée existe et la feuille s'ouvre au tap.
      await tester.scrollUntilVisible(
        find.text('Un retour ?'),
        100,
        scrollable: find
            .descendant(of: find.byType(Drawer), matching: find.byType(Scrollable))
            .first,
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text('Un retour ?'));
      await tester.pumpAndSettle();

      expect(find.text('Envoyer un retour'), findsOneWidget);
      expect(find.text('Sujet *'), findsOneWidget);
    });
  });

  group('Feuille de feedback', () {
    testWidgets('ouvre avec catégorie, priorité, sujet et description', (tester) async {
      await _pumpFeedbackSheet(tester, _FakeApiService());

      expect(find.text('Envoyer un retour'), findsOneWidget);
      expect(find.text('Catégorie'), findsOneWidget);
      expect(find.text('Priorité'), findsOneWidget);
      expect(find.text('Sujet *'), findsOneWidget);
      expect(find.text('Description'), findsOneWidget);
      expect(find.text('Envoyer le retour'), findsOneWidget);
    });

    testWidgets("sujet trop court → message d'erreur, aucun envoi", (tester) async {
      final api = _FakeApiService();
      await _pumpFeedbackSheet(tester, api);

      await tester.enterText(find.widgetWithText(TextField, 'Sujet *'), 'ab');
      await tester.tap(find.text('Envoyer le retour'));
      await tester.pump();

      expect(find.textContaining('au moins 3 caractères'), findsOneWidget);
      expect(api.feedbackPosts, isEmpty);
    });

    testWidgets('soumission → POST /feedback avec le payload complet (contexte technique inclus)', (tester) async {
      final api = _FakeApiService();
      await _pumpFeedbackSheet(tester, api);

      await tester.enterText(find.widgetWithText(TextField, 'Sujet *'), 'Le bouton export ne répond pas');
      await tester.enterText(find.widgetWithText(TextField, 'Description'), 'Étapes : ouvrir, cliquer, rien.');
      await tester.tap(find.text('Envoyer le retour'));
      await tester.pumpAndSettle();

      expect(api.feedbackPosts, hasLength(1));
      final body = api.feedbackPosts.single;
      expect(body['category'], 'BUG');
      expect(body['priority'], 'MOYENNE');
      expect(body['subject'], 'Le bouton export ne répond pas');
      expect(body['description'], 'Étapes : ouvrir, cliquer, rien.');
      expect(body['pageUrl'], '/crm-faiseur');
      expect(body['browser'], 'App mobile');
      expect(body['os'], isNotNull);
      expect(body['device'], isNotNull);
      // La feuille se ferme et le succès est confirmé
      expect(find.text('Envoyer un retour'), findsNothing);
      expect(find.textContaining('Merci !'), findsOneWidget);
    });

    testWidgets('échec réseau → message d\'erreur, la feuille reste ouverte', (tester) async {
      final api = _FakeApiService()..shouldFailFeedback = true;
      await _pumpFeedbackSheet(tester, api);

      await tester.enterText(find.widgetWithText(TextField, 'Sujet *'), 'Un bug critique');
      await tester.tap(find.text('Envoyer le retour'));
      await tester.pumpAndSettle();

      expect(find.textContaining('Échec de l\u2019envoi du retour'), findsOneWidget);
      expect(find.text('Envoyer un retour'), findsOneWidget); // toujours ouverte
      expect(api.feedbackPosts, isEmpty);
    });

    testWidgets('la priorité peut être changée et est transmise', (tester) async {
      final api = _FakeApiService();
      await _pumpFeedbackSheet(tester, api);

      // Priorité → Critique
      await tester.tap(find.byType(DropdownButtonFormField<String>).at(1));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Critique').last);
      await tester.pumpAndSettle();

      await tester.enterText(find.widgetWithText(TextField, 'Sujet *'), 'Blocage complet');
      await tester.tap(find.text('Envoyer le retour'));
      await tester.pumpAndSettle();

      expect(api.feedbackPosts.single['priority'], 'CRITIQUE');
    });
  });
}
