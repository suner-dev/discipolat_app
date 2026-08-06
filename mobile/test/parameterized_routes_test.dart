import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/departments/department_report_screen.dart';
import 'package:discipolat_mobile/presentation/screens/souls/soul_detail_screen.dart';
import 'package:discipolat_mobile/presentation/screens/souls/pastoral_360_screen.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// ApiService factice : enregistre les chemins appelés et répond selon le chemin.
class _RecordingApiService extends ApiService {
  _RecordingApiService() : super(baseUrl: 'http://fake');

  final List<String> requestedPaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    requestedPaths.add(path);
    final pathNoQuery = path.split('?').first;

    // ===== Âmes =====
    if (pathNoQuery.startsWith('/souls/') && pathNoQuery.endsWith('/pastoral-360')) {
      return _json(path, <String, dynamic>{
        'informations': <String, dynamic>{'prenom': 'Jean', 'nom': 'Dupont'},
        'spirituel': <String, dynamic>{'typeDisciple': 'NOUVEAU_CONVERTI', 'statut': 'ACTIF'},
        'encadrement': <String, dynamic>{},
        'indices': <String, dynamic>{},
        'alertesAutomatiques': <dynamic>[],
        'timeline': <dynamic>[],
        'evaluations': <String, dynamic>{},
        'notes': <dynamic>[],
      });
    }
    if (pathNoQuery.startsWith('/souls/') && pathNoQuery.endsWith('/spiritual-score')) {
      return _json(path, <String, dynamic>{'scoreGlobal': 75, 'presence': 80, 'fidelite': 70});
    }
    if (pathNoQuery.startsWith('/souls/') && pathNoQuery.endsWith('/history')) {
      return _json(path, <dynamic>[]);
    }
    if (pathNoQuery.startsWith('/souls/')) {
      return _json(path, <String, dynamic>{
        'id': 'soul-123',
        'nom': 'Dupont',
        'prenom': 'Jean',
        'email': 'jean.dupont@test.com',
        'typeDisciple': 'NOUVEAU_CONVERTI',
        'statut': 'ACTIF',
        'dateIntegration': '2026-01-01',
        'faiseurId': 'faiseur-1',
      });
    }

    // ===== Département =====
    if (pathNoQuery.startsWith('/departments/') && pathNoQuery.endsWith('/detail')) {
      return _json(path, <String, dynamic>{'id': 'dept-456', 'nom': 'Département A'});
    }
    if (pathNoQuery.startsWith('/departments/') && pathNoQuery.endsWith('/report')) {
      return _json(path, <String, dynamic>{
        'totalFamilles': 2,
        'familyReportsSoumis': 1,
        'totalPresents': 5,
        'totalAbsents': 1,
        'totalSorties': 0,
        'totalMaintenus': 1,
        'presenceMoyenne': 83.3,
        'statsParFamille': <String, dynamic>{
          'fam-1': <String, dynamic>{
            'familleNom': 'Famille Alpha',
            'soumis': true,
            'presenceMoyenne': 90,
            'totalPresents': 4,
            'totalAbsents': 0,
            'totalSorties': 0,
            'totalMaintenus': 0,
          },
        },
      });
    }
    if (pathNoQuery.startsWith('/departments/') && pathNoQuery.endsWith('/kpi')) {
      return _json(path, <String, dynamic>{
        'tauxSoumission': 50,
        'tauxPresence': 80,
        'rapportsSoumisSemaine': 1,
        'rapportsAttendusSemaine': 2,
        'totalFaiseurs': 3,
      });
    }

    throw StateError('Chemin inattendu: $path');
  }

  Response<dynamic> _json(String path, Object data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);
}

/// Écran d'accueil du routeur de test : boutons de navigation vers les routes
/// paramétrées, pour vérifier la transmission du paramètre :id.
class _HomeScreen extends StatelessWidget {
  const _HomeScreen();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ElevatedButton(
              onPressed: () => context.go('/souls/soul-123'),
              child: const Text('Ouvrir âme'),
            ),
            ElevatedButton(
              onPressed: () => context.go('/souls/soul-123/pastoral-360'),
              child: const Text('Ouvrir 360'),
            ),
            ElevatedButton(
              onPressed: () => context.go('/departments/dept-456/report'),
              child: const Text('Ouvrir rapport dept'),
            ),
          ],
        ),
      ),
    );
  }
}

GoRouter _testRouter(ApiService api) => GoRouter(
      initialLocation: '/',
      routes: [
        GoRoute(path: '/', builder: (context, state) => const _HomeScreen()),
        GoRoute(
          path: '/souls/:id',
          name: 'soul-detail',
          builder: (context, state) => SoulDetailScreen(
            soulId: state.pathParameters['id']!,
            apiService: api,
          ),
        ),
        GoRoute(
          path: '/souls/:id/pastoral-360',
          name: 'soul-pastoral-360',
          builder: (context, state) => Pastoral360Screen(
            soulId: state.pathParameters['id']!,
            apiService: api,
          ),
        ),
        GoRoute(
          path: '/departments/:id/report',
          name: 'department-report',
          builder: (context, state) => DepartmentReportScreen(
            departmentId: state.pathParameters['id']!,
            apiService: api,
          ),
        ),
      ],
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUpAll(() async {
    // Pastoral360Screen formate les dates en fr_FR (intl).
    await initializeDateFormatting('fr_FR', null);
  });

  Future<_RecordingApiService> pumpApp(WidgetTester tester) async {
    final api = _RecordingApiService();
    await tester.pumpWidget(
      MaterialApp.router(
        theme: GlassTheme.darkTheme,
        routerConfig: _testRouter(api),
      ),
    );
    await tester.pumpAndSettle();
    return api;
  }

  testWidgets('navigue vers /souls/:id et transmet le paramètre à SoulDetailScreen', (tester) async {
    final api = await pumpApp(tester);

    await tester.tap(find.text('Ouvrir âme'));
    await tester.pumpAndSettle();

    // L'écran a reçu l'id du paramètre : le nom de l'âme est affiché (état chargé, pas l'erreur)
    expect(find.text('Jean Dupont'), findsWidgets);
    expect(find.text('Score Spirituel'), findsOneWidget);
    expect(find.text('Âme non trouvée'), findsNothing);
    // Le paramètre :id a bien été transmis (appel API avec le bon id)
    expect(api.requestedPaths, contains('/souls/soul-123'));
    expect(api.requestedPaths, contains('/souls/soul-123/pastoral-360'));
  });

  testWidgets('navigue vers /souls/:id/pastoral-360 et transmet le paramètre', (tester) async {
    final api = await pumpApp(tester);

    await tester.tap(find.text('Ouvrir 360'));
    await tester.pumpAndSettle();

    // L'écran 360 s'affiche avec le nom issu du paramètre (pas l'état vide)
    expect(find.text('Dossier Pastoral 360°'), findsOneWidget);
    expect(find.text('Jean Dupont'), findsWidgets);
    expect(find.text('Membre non trouvé'), findsNothing);
    // Le paramètre :id a bien été transmis
    expect(api.requestedPaths, contains('/souls/soul-123/pastoral-360'));
  });

  testWidgets('navigue vers /departments/:id/report et transmet le paramètre', (tester) async {
    final api = await pumpApp(tester);

    await tester.tap(find.text('Ouvrir rapport dept'));
    await tester.pumpAndSettle();

    // L'écran du rapport s'affiche avec le nom du département (pas l'état d'erreur)
    expect(find.text('Département A'), findsOneWidget);
    expect(find.text('Détail par famille'), findsOneWidget);
    expect(find.text('Famille Alpha'), findsOneWidget);
    expect(find.text('Impossible de charger le rapport'), findsNothing);
    // Le paramètre :id a bien été transmis (3 appels avec le bon id)
    expect(api.requestedPaths, contains('/departments/dept-456/detail'));
    expect(api.requestedPaths, contains('/departments/dept-456/report'));
    expect(api.requestedPaths, contains('/departments/dept-456/kpi'));
  });
}
