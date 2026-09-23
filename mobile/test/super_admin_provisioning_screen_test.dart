import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/platform/super_admin_provisioning_screen.dart';

/// ApiService factice : simule les 4 POST du flux de provisionnement
/// (tenant → église → département → famille) et enregistre les payloads.
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
    if (path == '/platform/admin/tenants') {
      return _json(path, {
        'id': 'tenant-1',
        'name': map['name'],
        'slug': map['slug'],
        'plan': map['plan'],
        'status': 'ACTIVE',
      });
    }
    if (path.endsWith('/provisioning/church')) {
      return _json(path, {
        'id': 'church-1', 'tenantId': map['tenantId'], 'name': map['name'],
        'code': 'ROOT_CHURCH_ABC', 'path': 'root', 'level': 0, 'status': 'ACTIVE',
      });
    }
    if (path.endsWith('/provisioning/department')) {
      return _json(path, {
        'id': 'dept-1', 'tenantId': map['tenantId'], 'nom': map['nom'],
        'responsableId': 'resp-1', 'statut': 'ACTIVE',
      });
    }
    if (path.endsWith('/provisioning/family')) {
      return _json(path, {
        'id': 'fam-1', 'tenantId': map['tenantId'], 'nom': map['nom'],
        'chefFamilleId': 'chef-1', 'statut': 'ACTIVE',
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
    await _tapButton(tester, "Créer l'organisation");
    await tester.pumpAndSettle();

    // Étape 2 — église (pré-remplie)
    expect(find.text("2. Église racine"), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Église Bethel — Église principale'), findsOneWidget);
    await _tapButton(tester, "Créer l'église");
    await tester.pumpAndSettle();
    // Étape 3 — département (mode nouveau responsable par défaut)
    expect(find.text('3. Département'), findsWidgets); // stepper + titre de section
    await _enter(tester, 0, 'Accueil & Louange');
    await _enter(tester, 2, 'Jean');
    await _enter(tester, 3, 'Mpoudi');
    await _enter(tester, 4, 'jean@bethel.cm');
    await _tapButton(tester, 'Créer le département');
    await tester.pumpAndSettle();

    // Étape 4 — famille (mode nouveau chef par défaut)
    expect(find.text('4. Famille'), findsWidgets); // stepper + titre de section
    // Le champ nom famille est le 1er TextField de l'étape (nom pré-rempli écrasé)
    // Champs famille : 0=nom, 1=prénom, 2=nom, 3=email, 4=téléphone
    await _enter(tester, 0, 'Famille Mbarga');
    await _enter(tester, 1, 'Pierre');
    await _enter(tester, 2, 'Mbarga');
    await _enter(tester, 3, 'pierre@bethel.cm');
    await _tapButton(tester, 'Créer la famille');
    await tester.pumpAndSettle();

    // Récapitulatif
    expect(find.text('Organisation provisionnée !'), findsOneWidget);
    expect(find.text('Église Bethel'), findsOneWidget);
    expect(find.text('Accueil & Louange'), findsOneWidget);
    expect(find.text('Famille Mbarga'), findsOneWidget);

    // Vérification des appels API dans l'ordre du flux
    expect(api.postPaths, [
      '/platform/admin/tenants',
      '/platform/admin/provisioning/church',
      '/platform/admin/provisioning/department',
      '/platform/admin/provisioning/family',
    ]);
    // Le tenantId est propagé aux étapes 2-4
    expect(api.postDatas[1]['tenantId'], 'tenant-1');
    expect(api.postDatas[2]['tenantId'], 'tenant-1');
    expect(api.postDatas[3]['tenantId'], 'tenant-1');
    expect(api.postDatas[2]['createNewResponsable'], true);
    expect(api.postDatas[3]['createNewChef'], true);
    expect(api.postDatas[0]['slug'], 'eglise-bethel');

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

    await _tapButton(tester, "Créer l'organisation");
    await tester.pumpAndSettle();

    // Aucun appel API, on reste à l'étape 1, erreur affichée
    expect(api.postPaths, isEmpty);
    expect(find.text("1. Organisation (tenant)"), findsOneWidget);
    expect(find.text("Le nom de l'organisation est requis"), findsOneWidget);
  });
}

