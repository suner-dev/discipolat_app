import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/reports/reports_screen.dart';

/// ApiService factice : renvoie des réponses JSON selon le chemin demandé.
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    return _handler(path, params);
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

/// Écran placeholder pour vérifier qu'une navigation a bien eu lieu.
class _Marker extends StatelessWidget {
  final String label;
  const _Marker(this.label);

  @override
  Widget build(BuildContext context) =>
      Scaffold(body: Center(child: Text(label)));
}

/// Routeur de test : `/reports` rend ReportsScreen, les routes cibles
/// rendent des placeholders pour vérifier la navigation.
GoRouter _testRouter(ApiService api) => GoRouter(
      initialLocation: '/reports',
      routes: [
        GoRoute(
          path: '/reports',
          builder: (context, state) => ReportsScreen(apiService: api),
        ),
        GoRoute(
          path: '/reports/maker',
          builder: (context, state) => const _Marker('MAKER_REPORT'),
        ),
        GoRoute(
          path: '/reports/family',
          builder: (context, state) => const _Marker('FAMILY_REPORT'),
        ),
        GoRoute(
          path: '/departments/:id/report',
          builder: (context, state) => const _Marker('DEPT_REPORT'),
        ),
      ],
    );

void _setRole(String role, {List<String> roles = const []}) {
  AuthState().setAuthenticated(true, userData: {
    'userId': 'user-1',
    'email': 'resp@discipolat.test',
    'roles': roles.isEmpty ? [role] : roles,
    'activeRole': role,
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    // Par défaut : responsable actif (cas nominal du hub Rapports).
    _setRole('RESPONSABLE');
  });

  tearDown(() {
    AuthState().logout();
  });

  Future<void> pumpReports(WidgetTester tester, ApiService api) async {
    await tester.pumpWidget(MaterialApp.router(routerConfig: _testRouter(api)));
    await tester.pumpAndSettle();
  }

  ApiService statsApi({int total = 5, int soumis = 3, double taux = 60}) {
    return _FakeApiService((path, params) {
      if (path == '/dashboard/report-completion') {
        return _json(path, {
          'totalRapports': total,
          'rapportsSoumis': soumis,
          'tauxCompletion': taux,
        });
      }
      if (path.startsWith('/departments/by-responsable/')) {
        return _json(path, [
          {'id': 'dept-1', 'nom': 'Département A'},
          {'id': 'dept-2', 'nom': 'Département B'},
        ]);
      }
      if (path == '/departments') {
        return _json(path, {
          'content': [
            {'id': 'dept-1', 'nom': 'Département A'},
          ],
        });
      }
      throw StateError('Chemin inattendu: $path');
    });
  }

  testWidgets('affiche les statistiques de complétion après chargement', (tester) async {
    await pumpReports(tester, statsApi());

    // Titre + compteur du header
    expect(find.text('Rapports'), findsOneWidget);
    expect(find.text('3 / 5 rapports'), findsOneWidget);
    // Mini-statistiques
    expect(find.text('Soumis'), findsOneWidget);
    expect(find.text('En attente'), findsOneWidget);
    expect(find.text('Taux'), findsOneWidget);
    // Le pourcentage apparaît dans l'anneau ET la mini-stat Taux
    expect(find.text('60%'), findsNWidgets(2));
  });

  testWidgets('affiche les cartes de navigation vers les rapports', (tester) async {
    await pumpReports(tester, statsApi());

    expect(find.text('Rapport du faiseur'), findsOneWidget);
    expect(find.text('Rapport de famille'), findsOneWidget);
  });

  testWidgets('le RESPONSABLE voit la carte « Rapport du département »', (tester) async {
    await pumpReports(tester, statsApi());

    expect(find.text('Rapport du département'), findsOneWidget);
  });

  testWidgets('le FAISEUR ne voit pas la carte « Rapport du département »', (tester) async {
    _setRole('FAISEUR');
    await pumpReports(tester, statsApi());

    // Les cartes génériques restent visibles, la carte département est isolée.
    expect(find.text('Rapport du faiseur'), findsOneWidget);
    expect(find.text('Rapport de famille'), findsOneWidget);
    expect(find.text('Rapport du département'), findsNothing);
  });

  testWidgets('navigue vers le rapport du faiseur au tap', (tester) async {
    await pumpReports(tester, statsApi());

    await tester.tap(find.text('Rapport du faiseur'));
    await tester.pumpAndSettle();

    expect(find.text('MAKER_REPORT'), findsOneWidget);
  });

  testWidgets('navigue vers le rapport de famille au tap', (tester) async {
    await pumpReports(tester, statsApi());

    await tester.tap(find.text('Rapport de famille'));
    await tester.pumpAndSettle();

    expect(find.text('FAMILY_REPORT'), findsOneWidget);
  });

  testWidgets('ouvre le sélecteur de département et navigue vers le rapport', (tester) async {
    await pumpReports(tester, statsApi());

    await tester.tap(find.text('Rapport du département'));
    await tester.pumpAndSettle();

    // Bottom sheet avec les départements du responsable
    expect(find.text('Choisir un département'), findsOneWidget);
    expect(find.text('Département A'), findsOneWidget);
    expect(find.text('Département B'), findsOneWidget);

    await tester.tap(find.text('Département A'));
    await tester.pumpAndSettle();

    expect(find.text('DEPT_REPORT'), findsOneWidget);
  });

  testWidgets('en cas d’erreur de stats, affiche la carte d’erreur mais garde la navigation', (tester) async {
    final api = _FakeApiService((path, params) {
      throw DioException(requestOptions: RequestOptions(path: path));
    });
    await pumpReports(tester, api);

    expect(find.text('Impossible de charger les statistiques'), findsOneWidget);
    expect(find.text('Réessayer'), findsOneWidget);
    // Les cartes de navigation restent accessibles (parité web).
    expect(find.text('Rapport du faiseur'), findsOneWidget);
    expect(find.text('Rapport de famille'), findsOneWidget);
  });

  testWidgets('« Réessayer » recharge les statistiques après une erreur', (tester) async {
    var shouldFail = true;
    final api = _FakeApiService((path, params) {
      if (shouldFail) {
        throw DioException(requestOptions: RequestOptions(path: path));
      }
      return _json(path, {
        'totalRapports': 5,
        'rapportsSoumis': 3,
        'tauxCompletion': 60,
      });
    });

    await pumpReports(tester, api);
    expect(find.text('Impossible de charger les statistiques'), findsOneWidget);

    shouldFail = false;
    await tester.tap(find.text('Réessayer'));
    await tester.pumpAndSettle();

    expect(find.text('Impossible de charger les statistiques'), findsNothing);
    expect(find.text('3 / 5 rapports'), findsOneWidget);
  });
}
