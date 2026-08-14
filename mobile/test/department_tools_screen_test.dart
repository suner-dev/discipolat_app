import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/departments/department_tools_screen.dart';

/// ApiService factice : renvoie un rapport, une checklist et un équipement,
/// et enregistre les POST / PUT / DELETE.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<String> postPaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.endsWith('/reports/list')) {
      return _json(path, [
        {
          'id': 'report-1',
          'titre': 'Rapport hebdomadaire — du 2026-08-10 au 2026-08-16',
          'type': 'HEBDOMADAIRE',
          'statut': 'SOUMIS',
          'periodeDebut': '2026-08-10',
          'periodeFin': '2026-08-16',
          'contenu': 'EFFECTIF\n- 2 membres au total\nASSIDUITÉ\n- Taux de présence : 90.0 %',
        },
      ]);
    }
    if (path.endsWith('/checklists')) {
      return _json(path, [
        {
          'id': 'checklist-1',
          'titre': 'Préparation dimanche',
          'cibleType': 'GENERAL',
          'statut': 'OUVERTE',
          'progression': 50,
          'items': [
            {'id': 'item-1', 'libelle': 'Sono testée', 'fait': true},
            {'id': 'item-2', 'libelle': 'Caméras prêtes', 'fait': false},
          ],
        },
      ]);
    }
    if (path.endsWith('/equipment')) {
      return _json(path, [
        {
          'id': 'equip-1',
          'nom': 'Caméra Sony A7',
          'quantite': 2,
          'etat': 'BON',
          'localisation': 'Salle 3',
        },
      ]);
    }
    if (path.endsWith('/documents')) {
      return _json(path, [
        {
          'id': 'doc-1',
          'titre': "Procédure d'accueil",
          'type': 'PROCEDURE',
          'description': 'Accueil des nouveaux membres',
          'statut': 'ACTIF',
        },
      ]);
    }
    if (path.endsWith('/settings')) {
      return _json(path, {
        'departmentId': 'dept-1',
        'absenceSeuil': 2,
        'absencePeriode': 3,
        'inactiviteMois': 3,
        'tacheRetardAlerte': true,
        'eventRappelJours': 1,
      });
    }
    return _json(path, <String, dynamic>{});
  }

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    return _json(path, <String, dynamic>{});
  }

  final List<String> putPaths = [];

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    return _json(path, <String, dynamic>{});
  }

  Response<dynamic> _json(String path, Object data) =>
      Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);
}

Widget _wrap(Widget child) => MaterialApp(home: child);

void main() {
  testWidgets('affiche les rapports puis les checklists et l’inventaire par onglet', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentToolsScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    // Onglet Rapports (par défaut)
    expect(find.text('Rapport hebdomadaire — du 2026-08-10 au 2026-08-16'), findsOneWidget);

    // Onglet Checklists
    await tester.tap(find.text('Checklists'));
    await tester.pumpAndSettle();
    expect(find.text('Préparation dimanche'), findsOneWidget);
    expect(find.text('Sono testée'), findsOneWidget);

    // Onglet Inventaire
    await tester.tap(find.text('Inventaire'));
    await tester.pumpAndSettle();
    expect(find.text('Caméra Sony A7'), findsOneWidget);
  });

  testWidgets('génère un rapport → POST /reports/generate', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentToolsScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    // Ouvre le dialogue de génération puis valide
    await tester.tap(find.byIcon(Icons.add_circle));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Générer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/departments/dept-1/reports/generate'));
  });

  testWidgets('crée un équipement → POST /equipment', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentToolsScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Inventaire'));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.add_circle));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, 'Micro Shure SM58');
    await tester.tap(find.text('Créer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/departments/dept-1/equipment'));
  });

  testWidgets('affiche la documentation puis ajoute un document', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentToolsScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Documentation'));
    await tester.pumpAndSettle();
    expect(find.text("Procédure d'accueil"), findsOneWidget);

    await tester.tap(find.byIcon(Icons.add_circle));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField).first, 'Guide son');
    await tester.tap(find.text('Ajouter'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/departments/dept-1/documents'));
  });

  testWidgets('enregistre les seuils d\'alertes → PUT /settings', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentToolsScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Paramètres'));
    await tester.pumpAndSettle();
    expect(find.text('Paramètres des alertes'), findsOneWidget);

    // Modifie le premier champ (absenceSeuil 2 → 4) puis enregistre
    final seuilField = find.byType(TextField).first;
    await tester.enterText(seuilField, '4');
    await tester.drag(find.byType(ListView), const Offset(0, -300));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enregistrer les paramètres'));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/departments/dept-1/settings'));
  });

  testWidgets('configure le rappel des événements → PUT /settings', (tester) async {
    final api = _FakeApiService();
    await tester.pumpWidget(_wrap(DepartmentToolsScreen(
      departmentId: 'dept-1',
      apiService: api,
    )));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Paramètres'));
    await tester.pumpAndSettle();
    expect(find.text('Rappel événement (0–30 jours)'), findsOneWidget);

    final rappelField = find.widgetWithText(TextField, '1').first;
    await tester.enterText(rappelField, '7');
    await tester.drag(find.byType(ListView), const Offset(0, -300));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enregistrer les paramètres'));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/departments/dept-1/settings'));
  });
}
