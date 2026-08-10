import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/transfers/transfer_admin_screen.dart';

/// ApiService factice : renvoie les configurations de workflow et enregistre
/// les PATCH / PUT / DELETE (avec mode d'échec pour les chemins d'erreur).
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler, {this.failDeletes = false}) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final bool failDeletes;
  final List<String> patchPaths = [];
  final List<String> putPaths = [];
  final List<Map<String, dynamic>?> putDatas = [];
  final List<String> deletePaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> patch(String path, {dynamic data}) async {
    patchPaths.add(path);
    return _json(path, {});
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    putDatas.add(data is Map<String, dynamic> ? data : null);
    return _json(path, {});
  }

  @override
  Future<Response> delete(String path) async {
    deletePaths.add(path);
    if (failDeletes) throw DioException(requestOptions: RequestOptions(path: path));
    return _json(path, {});
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

const _CONFIGS = [
  {
    'id': 'cfg-1',
    'label': 'Transfert faiseur',
    'transferType': 'FAISEUR_FAMILLE_TRANSFERT',
    'modeValidation': 'SEQUENTIEL',
    'delaiTraitementHeures': 72,
    'actif': true,
    'rolesInitiateurs': ['PASTEUR'],
    'nombreValidationsRequises': 1,
    'steps': [
      {'etapeOrdre': 1, 'rolesValidateurs': ['RESPONSABLE'], 'label': 'Validation responsable'},
      {'etapeOrdre': 2, 'rolesValidateurs': ['PASTEUR'], 'label': 'Validation pasteur'},
    ],
  },
  {
    'id': 'cfg-2',
    'label': 'Changement de faiseur',
    'transferType': 'FAISEUR_DISCIPLE_CHANGEMENT',
    'modeValidation': 'PARALLELE',
    'delaiTraitementHeures': 24,
    'actif': false,
    'rolesInitiateurs': ['PASTEUR'],
    'nombreValidationsRequises': 1,
    'steps': <dynamic>[],
  },
];

void _setRole(String role) {
  AuthState().setAuthenticated(true, userData: {
    'userId': 'user-1',
    'email': 'admin@discipolat.test',
    'roles': [role],
    'activeRole': role,
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() => _setRole('ADMIN'));
  tearDown(() => AuthState().logout());

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    // Surface haute : l'éditeur (bottom sheet à 85 % de la hauteur) doit tenir
    // entièrement à l'écran pour que « Enregistrer » / « Supprimer » soient
    // directement tapables (sinon le tap rate le hit-test après scroll).
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: TransferAdminScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche les configurations avec statut et synthèse du circuit', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _CONFIGS));
    await pumpScreen(tester, api);

    expect(find.text('Workflow de transfert'), findsOneWidget);
    expect(find.text('Transfert faiseur'), findsOneWidget);
    expect(find.text('Actif'), findsOneWidget);
    expect(find.text('SEQUENTIEL · 72h · 2 étape(s)'), findsOneWidget);
    expect(find.text('Changement de faiseur'), findsOneWidget);
    expect(find.text('Inactif'), findsOneWidget);
    expect(find.text('PARALLELE · 24h · 0 étape(s)'), findsOneWidget);
  });

  testWidgets('affiche « Aucune configuration » quand la liste est vide', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, <dynamic>[]));
    await pumpScreen(tester, api);

    expect(find.text('Aucune configuration'), findsOneWidget);
  });

  testWidgets('bascule d’activation → PATCH /admin/transfers/workflows/{id}/toggle', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _CONFIGS));
    await pumpScreen(tester, api);

    // cfg-1 est actif → icône power_settings_new (power_on)
    await tester.tap(find.byIcon(Icons.power_settings_new).first);
    await tester.pumpAndSettle();

    expect(api.patchPaths, contains('/admin/transfers/workflows/cfg-1/toggle'));
  });

  testWidgets('ouvre l’éditeur depuis une carte', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _CONFIGS));
    await pumpScreen(tester, api);

    await tester.tap(find.text('Transfert faiseur'));
    await tester.pumpAndSettle();

    expect(find.text('Rôles initiateurs'), findsOneWidget);
    expect(find.text('Mode de validation'), findsOneWidget);
    expect(find.text('Étapes du circuit'), findsOneWidget);
    // L'étape 1 du circuit est visible dans l'éditeur
    expect(find.text('Étape 1 — Validation responsable'), findsOneWidget);
  });

  testWidgets('enregistre une configuration → PUT + SnackBar de confirmation', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _CONFIGS));
    await pumpScreen(tester, api);

    await tester.tap(find.text('Transfert faiseur'));
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text('Enregistrer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/admin/transfers/workflows/cfg-1'));
    // Le payload reflète la configuration éditée (mode, requis, délai)
    expect(api.putDatas.last?['modeValidation'], 'SEQUENTIEL');
    expect(api.putDatas.last?['nombreValidationsRequises'], 1);
    expect(api.putDatas.last?['delaiTraitementHeures'], 72);
    expect(find.text('Configuration enregistrée'), findsOneWidget);
  });

  testWidgets('supprime avec confirmation → dialogue « Oui » → DELETE', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _CONFIGS));
    await pumpScreen(tester, api);

    await tester.tap(find.text('Transfert faiseur'));
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text('Supprimer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Supprimer'));
    await tester.pumpAndSettle();

    expect(find.text('Supprimer cette configuration ?'), findsOneWidget);
    await tester.tap(find.text('Oui'));
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/admin/transfers/workflows/cfg-1'));
  });

  testWidgets('suppression refusée (config utilisée) → SnackBar explicative', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _CONFIGS), failDeletes: true);
    await pumpScreen(tester, api);

    await tester.tap(find.text('Transfert faiseur'));
    await tester.pumpAndSettle();

    await tester.ensureVisible(find.text('Supprimer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Supprimer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Oui'));
    await tester.pumpAndSettle();

    expect(find.text('Suppression impossible : des demandes utilisent cette configuration'), findsOneWidget);
  });
}
