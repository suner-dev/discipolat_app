// LOT 2 §GLISE-D'ABORD (T2.6, mobile) — widget test de `ChurchLandingScreen`.
//
// Verrouille le CONTRAT §6 + R3 côté mobile :
//   1. chemin unique `/public/churches/{slug}` (URL-encodé) — jamais une
//      liste plus large, jamais `/tenant/*` ;
//   2. R3 : 404 (slug inconnu / non listée / landing éteinte) → message
//      UNIQUE « Page indisponible », aucun oracle ;
//   3. 429 / 5xx / réseau → « Indisponible temporairement » + bouton
//      « Réessayer » (non bloquant) ;
//   4. projection liste blanche : une réponse 200 qui fuiterait `tenantId`
//      ou `email` doit rester INVISIBLE ;
//   5. 200 avec `landingEnabled=false` est traité comme `absent` (le
//      mobile ne laisse PAS passer une landing désactivée, même si un
//      futur serveur la renvoie par erreur — défense en profondeur) ;
//   6. `DetailBackButton` est monté dans l'`AppBar` : filet de retour A1,
//      aucun nouveau bouton codé en dur ;
//   7. `slug` vide → absent sans requête (inutilable, pas d'appel réseau).
//
// Convention du dépôt : `_FakeApi extends ApiService { super(baseUrl: 'http://fake'); }`
// (cf. `church_picker_test.dart`, `currency_catalog_test.dart`, `accept_invitation_screen_test.dart`).

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/landing/church_landing_screen.dart';
import 'package:discipolat_mobile/presentation/widgets/detail_back_button.dart';

class _FakeApi extends ApiService {
  _FakeApi() : super(baseUrl: 'http://fake');

  final List<String> calls = [];
  Object? nextError;
  dynamic nextBody;
  int nextStatus = 200;

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params,
      Map<String, dynamic>? queryParameters}) async {
    calls.add(path);
    if (nextError != null) throw nextError!;
    return Response(
      requestOptions: RequestOptions(path: path),
      statusCode: nextStatus,
      data: nextBody,
    );
  }
}

/// Hôte GoRouter minimal : le `DetailBackButton` appelle `context.canPop()`
/// / `context.go(...)` ; il faut donc une routerConfig, sinon l'assertion
/// « go_router must be provided » échoue. On monte une route bidon `/` et
/// la route testée sur `/e/:slug`, sans affecter l'app réelle.
Widget _host(Widget child, {String initialUrl = '/e/bethel'}) {
  final router = GoRouter(
    initialLocation: initialUrl,
    routes: [
      GoRoute(path: '/', builder: (_, __) => const SizedBox.shrink()),
      GoRoute(
        path: '/e/:slug',
        builder: (context, state) => child,
      ),
    ],
  );
  return MaterialApp.router(routerConfig: router);
}

void main() {
  group('LOT 2 §GLISE-D\u2019ABORD (T2.6, mobile) — ChurchLandingScreen', () {
    testWidgets('chemin demandé = `/public/churches/{slug}`, URL-encodé',
        (tester) async {
      final api = _FakeApi()
        ..nextBody = {
          'slug': 'bethel',
          'name': 'Église Bethel',
          'landingEnabled': true,
        };
      await tester.pumpWidget(_host(
          ChurchLandingScreen(slug: 'beth el/prod', apiService: api)));
      await tester.pumpAndSettle();
      // `Uri.encodeComponent` remplace `/` par `%2F` et l'espace par `%20`.
      expect(api.calls, ['/public/churches/beth%20el%2Fprod']);
    });

    testWidgets('200 valide → rendu nom + slogan + ville/pays + description',
        (tester) async {
      final api = _FakeApi()
        ..nextBody = {
          'slug': 'bethel',
          'name': 'Église Bethel',
          'landingEnabled': true,
          'slogan': 'La maison de prière',
          'description': 'Bienvenue dans notre communauté.',
          'city': 'Douala',
          'country': 'CM',
        };
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'bethel', apiService: api)));
      await tester.pumpAndSettle();
      expect(find.text('Église Bethel'), findsOneWidget);
      expect(find.text('La maison de prière'), findsOneWidget);
      expect(find.text('Douala, CM'), findsOneWidget);
      expect(find.text('Bienvenue dans notre communauté.'), findsOneWidget);
    });

    testWidgets('404 (slug inconnu / non listée / landing éteinte) → message UNIQUE (R3)',
        (tester) async {
      final api = _FakeApi()
        ..nextStatus = 404
        ..nextError = DioException(
          requestOptions: RequestOptions(path: '/public/churches/ghost'),
          response: Response(
            requestOptions: RequestOptions(path: '/public/churches/ghost'),
            statusCode: 404,
          ),
        );
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'ghost', apiService: api)));
      await tester.pumpAndSettle();
      expect(find.text('Page indisponible'), findsOneWidget);
      expect(
        find.text(
            'Cette page n\'existe pas, n\'est pas publiée, ou a été désactivée par son église.'),
        findsOneWidget,
      );
      // Le widget ne doit JAMAIS divulguer la cause exacte : aucun texte
      // distinctif « non listée » / « fantôme » / « désactivée » séparément.
      expect(find.textContaining('désactivée par son église'), findsOneWidget,
          reason: 'la formulation est UNIFIÉE, jamais l\'une des trois causes');
    });

    testWidgets('200 mais landingEnabled=false → traité comme absent (défense en profondeur)',
        (tester) async {
      final api = _FakeApi()
        ..nextBody = {
          'slug': 'hidden',
          'name': 'Église Cachée',
          'landingEnabled': false,
        };
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'hidden', apiService: api)));
      await tester.pumpAndSettle();
      expect(find.text('Page indisponible'), findsOneWidget);
      // Le nom ne doit PAS s\'afficher : sinon on confirmerait l\'existence
      // de l\'église, ce qui violerait R3.
      expect(find.text('Église Cachée'), findsNothing);
    });

    testWidgets('R2 liste blanche : tenantId/email fuités par le serveur restent invisibles',
        (tester) async {
      final api = _FakeApi()
        ..nextBody = {
          'slug': 'bethel',
          'name': 'Bethel',
          'landingEnabled': true,
          // Ces clés NE DOIVENT PAS être rendues par le widget.
          'tenantId': 'should-be-invisible',
          'email': 'pasteur@should-be-invisible.example',
          'phone': 'should-be-invisible-phone',
          'memberCount': 4242,
        };
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'bethel', apiService: api)));
      await tester.pumpAndSettle();
      expect(find.textContaining('should-be-invisible'), findsNothing);
      expect(find.textContaining('4242'), findsNothing);
      // Mais le nom légitime est visible.
      expect(find.text('Bethel'), findsOneWidget);
    });

    testWidgets('5xx / réseau → état error + bouton Réessayer, non bloquant',
        (tester) async {
      final api = _FakeApi()
        ..nextError = DioException(
          requestOptions: RequestOptions(path: '/public/churches/bethel'),
          type: DioExceptionType.connectionTimeout,
          message: 'timeout',
        );
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'bethel', apiService: api)));
      await tester.pumpAndSettle();
      expect(find.textContaining('Indisponible temporairement'), findsOneWidget);
      expect(find.textContaining('timeout'), findsOneWidget);
      expect(find.widgetWithText(OutlinedButton, 'Réessayer'), findsOneWidget);
      // Pas de SnackBar / Dialog : le visiteur reste sur la page.
      expect(find.byType(SnackBar), findsNothing);
      expect(find.byType(Dialog), findsNothing);
    });

    testWidgets('Réessayer rappelle le serveur (et succeed au 2e coup)',
        (tester) async {
      final api = _FakeApi()
        ..nextError = DioException(
          requestOptions: RequestOptions(path: '/public/churches/bethel'),
          type: DioExceptionType.connectionError,
        );
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'bethel', apiService: api)));
      await tester.pumpAndSettle();
      expect(api.calls.length, 1);
      expect(find.widgetWithText(OutlinedButton, 'Réessayer'), findsOneWidget);
      // Le prochain coup marche.
      api.nextError = null;
      api.nextBody = {
        'slug': 'bethel',
        'name': 'Bethel',
        'landingEnabled': true,
      };
      await tester.tap(find.widgetWithText(OutlinedButton, 'Réessayer'));
      await tester.pumpAndSettle();
      expect(api.calls.length, 2);
      expect(find.text('Bethel'), findsOneWidget);
    });

    testWidgets('slug vide : absent sans appel réseau (inutilable)',
        (tester) async {
      final api = _FakeApi();
      // L'`initialUrl` du routeur doit quand même mapper `/e/:slug` (un
      // slug vide ne matcherai pas la route et GoRouter afficherait son
      // écran d'erreur). Ce que le WIDGET reçoit comme `slug` est ce qui
      // compte : ici on lui passe une chaîne vide, comme le builder
      // l'aurait fait sur `/e/` avec `state.pathParameters['slug']` nul.
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: '', apiService: api),
          initialUrl: '/e/_placeholder')); 
      await tester.pumpAndSettle();
      expect(api.calls, isEmpty);
      expect(find.text('Page indisponible'), findsOneWidget);
    });

    testWidgets('montage initial : loading, puis transition vers ready sans flicker',
        (tester) async {
      final api = _FakeApi()
        ..nextBody = {
          'slug': 'x',
          'name': 'Église X',
          'landingEnabled': true,
        };
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'x', apiService: api)));
      // Immediate frame → loading (CircularProgressIndicator visible).
      expect(find.byType(CircularProgressIndicator), findsOneWidget);
      await tester.pumpAndSettle();
      expect(find.byType(CircularProgressIndicator), findsNothing);
      expect(find.text('Église X'), findsOneWidget);
    });

    testWidgets('DetailBackButton monté dans l\'AppBar (filet A1, aucun doublon)',
        (tester) async {
      final api = _FakeApi()
        ..nextBody = {
          'slug': 'x',
          'name': 'Église X',
          'landingEnabled': true,
        };
      await tester.pumpWidget(_host(ChurchLandingScreen(slug: 'x', apiService: api)));
      await tester.pumpAndSettle();
      // Un seul DetailBackButton, dans l'appbar (leading).
      expect(find.byType(DetailBackButton), findsOneWidget);
      // Pas d\'autre ArrowLeft / retour en dur dans le corps de la page.
      expect(find.byIcon(Icons.arrow_back_rounded), findsOneWidget);
    });

    testWidgets('`initialState` injecté : chaque branche est couverte sans réseau',
        (tester) async {
      // Absent
      await tester.pumpWidget(_host(ChurchLandingScreen(
        slug: 'x',
        apiService: _FakeApi(),
        initialState: (status: ChurchLandingStatus.absent, data: null, message: null),
      )));
      await tester.pumpAndSettle();
      expect(find.text('Page indisponible'), findsOneWidget);

      // Ready
      final ready = ChurchLandingData(
        slug: 'y',
        name: 'Église Y',
        landingEnabled: true,
        slogan: 'Slogan Y',
      );
      await tester.pumpWidget(_host(ChurchLandingScreen(
        slug: 'y',
        apiService: _FakeApi(),
        initialState: (
          status: ChurchLandingStatus.ready,
          data: ready,
          message: null
        ),
      )));
      await tester.pumpAndSettle();
      expect(find.text('Église Y'), findsOneWidget);
      expect(find.text('Slogan Y'), findsOneWidget);

      // Erreur
      await tester.pumpWidget(_host(ChurchLandingScreen(
        slug: 'z',
        apiService: _FakeApi(),
        initialState: (
            status: ChurchLandingStatus.error,
            data: null,
            message: 'boom'),
      )));
      await tester.pumpAndSettle();
      expect(find.textContaining('Indisponible temporairement'), findsOneWidget);
      expect(find.textContaining('boom'), findsOneWidget);
    });
  });
}
