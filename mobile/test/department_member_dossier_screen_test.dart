import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/departments/department_member_dossier_screen.dart';

/// ApiService factice pour le dossier membre : renvoie le payload du dossier
/// et enregistre les POST / PUT / DELETE (objectifs, rapports, notes).
class _FakeApiService extends ApiService {
  _FakeApiService(this._dossier) : super(baseUrl: 'http://fake');

  final Map<String, dynamic> _dossier;
  final List<String> postPaths = [];
  final List<Map<String, dynamic>?> postDatas = [];
  final List<String> putPaths = [];
  final List<String> deletePaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.endsWith('/dossier')) return _json(path, _dossier);
    return _json(path, <String, dynamic>{});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postDatas.add(data is Map<String, dynamic> ? data : null);
    return _json(path, <String, dynamic>{});
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    return _json(path, <String, dynamic>{});
  }

  @override
  Future<Response> delete(String path) async {
    deletePaths.add(path);
    return _json(path, <String, dynamic>{});
  }

  Response<dynamic> _json(String path, Object data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);
}

Map<String, dynamic> _dossier() => {
      'profil': {
        'id': 'soul-1',
        'nomComplet': 'Marie Martin',
        'prenom': 'Marie',
        'nom': 'Martin',
        'statut': 'ACTIF',
        'origine': 'MANUEL',
        'email': 'marie@test.com',
        'telephone': '0612345678',
        'ajoutePar': 'Paul Dupont',
      },
      'alertes': [
        {'id': 'a1', 'titre': 'Absences répétées', 'message': '3 absences consécutives'},
      ],
      'affectations': [
        {'id': 'aff1', 'actif': true, 'teamNom': 'Équipe Chorale', 'positionNom': 'Chef', 'role': 'ADJOINT'},
      ],
      'objectifs': [
        {
          'id': 'obj1',
          'titre': 'Être confirmée',
          'description': 'Critère 1 et 2',
          'echeance': '2026-12-31',
          'avancement': 60,
          'statut': 'EN_COURS',
          'enRetard': false,
          'creeParNom': 'Paul Dupont',
        },
      ],
      'rapportsResponsable': [
        {'id': 'r1', 'type': 'PROGRESSION', 'contenu': 'Bonne progression', 'auteurNom': 'Paul Dupont', 'createdAt': '2026-08-01T10:00:00'},
      ],
      'rapports': {
        'soumis': 1,
        'total': 2,
        'liste': [
          {'id': 'fr1', 'semaine': '2026-08-03', 'soumis': true},
        ],
      },
      'notes': [
        {'id': 'n1', 'contenu': 'Besoin de suivi sur la prière', 'auteurNom': 'Paul Dupont', 'createdAt': '2026-08-02T10:00:00'},
      ],
      'activite': [
        {'id': 'act1', 'action': 'NOTE_ADDED', 'details': 'Note ajoutée', 'actorNom': 'Paul Dupont', 'createdAt': '2026-08-02T10:00:00'},
      ],
    };

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.binding.setSurfaceSize(const Size(500, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(
      home: DepartmentMemberDossierScreen(
        departmentId: 'dept-1',
        memberId: 'soul-1',
        apiService: api,
      ),
    ));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche le profil, les objectifs, les rapports et les notes', (tester) async {
    final api = _FakeApiService(_dossier());
    await pumpScreen(tester, api);

    // Header : nom + statut + alertes visibles sur le premier onglet (Profil)
    expect(find.text('Dossier · Marie Martin'), findsOneWidget);
    expect(find.text('Marie Martin'), findsWidgets);
    expect(find.text('Absences répétées'), findsOneWidget);
    expect(find.textContaining('Équipe Chorale'), findsOneWidget);
    expect(find.text('marie@test.com'), findsOneWidget);

    // Onglet Objectifs
    await tester.tap(find.text('Objectifs'));
    await tester.pumpAndSettle();
    expect(find.text('Être confirmée'), findsOneWidget);
    expect(find.text('1 en cours'), findsOneWidget);

    // Onglet Rapports
    await tester.tap(find.text('Rapports'));
    await tester.pumpAndSettle();
    expect(find.text('Bonne progression'), findsOneWidget);
    expect(find.textContaining('Rapports du faiseur'), findsOneWidget);
    expect(find.text('Semaine du 2026-08-03'), findsOneWidget);

    // Onglet Notes
    await tester.tap(find.text('Notes'));
    await tester.pumpAndSettle();
    expect(find.textContaining('Besoin de suivi sur la prière'), findsOneWidget);
  });

  testWidgets('crée un rapport responsable → POST reports', (tester) async {
    final api = _FakeApiService(_dossier());
    await pumpScreen(tester, api);

    await tester.tap(find.text('Rapports'));
    await tester.pumpAndSettle();

    // Bouton d'ajout (icône add_circle) — le premier est dans Objectifs, on
    // prend l'icône présente dans l'onglet Rapports visible.
    await tester.tap(find.byIcon(Icons.add_circle).last);
    await tester.pumpAndSettle();

    expect(find.text('Nouveau rapport'), findsOneWidget);
    await tester.enterText(find.widgetWithText(TextField, 'Contenu *'), 'Rapport de test');
    await tester.tap(find.text('Ajouter'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/departments/dept-1/members/soul-1/reports'));
    expect(api.postDatas.last?['contenu'], 'Rapport de test');
  });

  testWidgets('supprime un rapport responsable → DELETE reports/{id}', (tester) async {
    final api = _FakeApiService(_dossier());
    await pumpScreen(tester, api);

    await tester.tap(find.text('Rapports'));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.delete_outline).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Supprimer').last);
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/departments/dept-1/reports/r1'));
  });

  testWidgets('crée un objectif → POST objectives', (tester) async {
    final api = _FakeApiService(_dossier());
    await pumpScreen(tester, api);

    await tester.tap(find.text('Objectifs'));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.add_circle).first);
    await tester.pumpAndSettle();

    expect(find.text('Nouvel objectif'), findsOneWidget);
    await tester.enterText(find.widgetWithText(TextField, 'Objectif *'), 'Objectif test');
    await tester.tap(find.text('Créer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/departments/dept-1/members/soul-1/objectives'));
    expect(api.postDatas.last?['titre'], 'Objectif test');
  });

  testWidgets('met à jour un objectif → PUT objectives/{id}', (tester) async {
    final api = _FakeApiService(_dossier());
    await pumpScreen(tester, api);

    await tester.tap(find.text('Objectifs'));
    await tester.pumpAndSettle();

    final slider = find.byType(Slider);
    expect(slider, findsOneWidget);
    await tester.drag(slider, const Offset(100, 0));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/departments/dept-1/objectives/obj1'));
  });

  testWidgets('supprime un objectif → DELETE objectives/{id}', (tester) async {
    final api = _FakeApiService(_dossier());
    await pumpScreen(tester, api);

    await tester.tap(find.text('Objectifs'));
    await tester.pumpAndSettle();

    await tester.tap(find.byIcon(Icons.delete_outline).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Supprimer').last);
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/departments/dept-1/objectives/obj1'));
  });

  testWidgets('affiche l’activité', (tester) async {
    final api = _FakeApiService(_dossier());
    await pumpScreen(tester, api);

    await tester.ensureVisible(find.text('Activité'));
    await tester.tap(find.text('Activité'));
    await tester.pumpAndSettle();

    expect(find.textContaining('note added'), findsOneWidget);
  });
}
