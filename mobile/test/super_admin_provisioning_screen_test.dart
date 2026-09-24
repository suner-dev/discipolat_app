import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/platform/super_admin_provisioning_screen.dart';

/// ApiService factice : simule la transaction atomique du provisionnement.
class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  final List<String> postPaths = [];
  final List<Map<String, dynamic>> postDatas = [];

  @override
  Future<Response> get(String path,
      {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async =>
      _json(path, {});

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postDatas.add(data is Map<String, dynamic> ? data : <String, dynamic>{});
    final map = data is Map<String, dynamic> ? data : <String, dynamic>{};
    if (path == '/platform/admin/provisioning') {
      return _json(path, {
        'tenant': {
          'id': 'tenant-1',
          'name': map['name'],
          'slug': map['slug'],
          'plan': map['plan'],
          'status': 'ACTIVE',
        },
        'church': {
          'id': 'church-1',
          'tenantId': 'tenant-1',
          'name': map['churchName'],
          'code': 'ROOT_CHURCH_ABC',
          'path': 'root',
          'level': 0,
        },
        'department': {
          'id': 'dept-1',
          'tenantId': 'tenant-1',
          'nom': map['departmentName'],
          'responsableId': 'resp-1',
        },
        'family': {
          'id': 'fam-1',
          'tenantId': 'tenant-1',
          'nom': map['familyName'],
          'chefFamilleId': 'chef-1',
        },
      });
    }
    return _json(path, {});
  }

  Response<dynamic> _json(String path, Object data) => Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 201,
        data: data,
      );
}

Future<void> _enter(WidgetTester tester, int fieldIndex, String value) async {
  final field = find.byType(TextField).at(fieldIndex);
  await tester.ensureVisible(field);
  await tester.enterText(field, value);
  await tester.pump();
}

Future<void> _tapButton(WidgetTester tester, String label) async {
  final btn = find.text(label);
  await tester.ensureVisible(btn);
  await tester.tap(btn);
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 400));
  await tester.pump(const Duration(milliseconds: 400));
}

void main() {
  testWidgets('flux complet tenant > église > département > famille > récap',
      (tester) async {
    tester.view.physicalSize = const Size(900, 2000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    final api = _FakeApiService();
    await tester.pumpWidget(MaterialApp(
      home: SuperAdminProvisioningScreen(apiService: api),
    ));
    await tester.pumpAndSettle();

    expect(find.text('Provisionnement guidé'), findsWidgets);
    expect(find.text("1. Organisation (tenant)"), findsOneWidget);

    // Étape 1 — organisation
    await _enter(tester, 0, 'Église Bethel');
    await _tapButton(tester, 'Continuer');
    await tester.pumpAndSettle();

    // Étape 2 — église (pré-remplie)
    expect(find.text("2. Église racine"), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Église Bethel — Église principale'), findsOneWidget);
    await _tapButton(tester, 'Continuer');
    await tester.pumpAndSettle();
    // Étape 3 — département (mode nouveau responsable par défaut)
    expect(find.text('3. Département'), findsWidgets); // stepper + titre de section
    await _enter(tester, 0, 'Accueil & Louange');
    await _enter(tester, 2, 'Jean');
    await _enter(tester, 3, 'Mpoudi');
    await _enter(tester, 4, 'jean@bethel.cm');
    await _tapButton(tester, 'Continuer');
    await tester.pumpAndSettle();

    // Étape 4 — famille (mode nouveau chef par défaut)
    expect(find.text('4. Famille'), findsWidgets); // stepper + titre de section
    // Le champ nom famille est le 1er TextField de l'étape (nom pré-rempli écrasé)
    // Champs famille : 0=nom, 1=prénom, 2=nom, 3=email, 4=téléphone
    await _enter(tester, 0, 'Famille Mbarga');
    await _enter(tester, 1, 'Pierre');
    await _enter(tester, 2, 'Mbarga');
    await _enter(tester, 3, 'pierre@bethel.cm');
    await _tapButton(tester, "Provisionner l'organisation");
    await tester.pumpAndSettle();

    // Récapitulatif
    expect(find.text('Organisation provisionnée !'), findsOneWidget);
    expect(find.text('Église Bethel'), findsOneWidget);
    expect(find.text('Accueil & Louange'), findsOneWidget);
    expect(find.text('Famille Mbarga'), findsOneWidget);

    expect(api.postPaths, ['/platform/admin/provisioning']);
    expect(api.postDatas, hasLength(1));
    expect(api.postDatas[0]['slug'], 'eglise-bethel');
    expect(api.postDatas[0]['churchName'], 'Église Bethel — Église principale');
    expect(api.postDatas[0]['departmentName'], 'Accueil & Louange');
    expect(api.postDatas[0]['familyName'], 'Famille Mbarga');
    expect(api.postDatas[0]['createNewResponsable'], true);
    expect(api.postDatas[0]['createNewChef'], true);

    // Bouton retour au dashboard cliquable (go_router non monté : vérif présence)
    expect(find.text('Dashboard'), findsOneWidget);
    expect(find.text('Nouveau'), findsOneWidget);
  });

  testWidgets('validation : étape 1 bloquée sans nom d\'organisation',
      (tester) async {
    tester.view.physicalSize = const Size(900, 1600);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);

    final api = _FakeApiService();
    await tester.pumpWidget(MaterialApp(
      home: SuperAdminProvisioningScreen(apiService: api),
    ));
    await tester.pumpAndSettle();

    await _tapButton(tester, 'Continuer');
    await tester.pumpAndSettle();

    // Aucun appel API, on reste à l'étape 1, erreur affichée
    expect(api.postPaths, isEmpty);
    expect(find.text("1. Organisation (tenant)"), findsOneWidget);
    expect(find.text("Le nom de l'organisation est requis"), findsOneWidget);
  });
}

