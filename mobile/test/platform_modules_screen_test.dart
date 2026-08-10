import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/platform/platform_modules_screen.dart';

/// ApiService factice : renvoie les modules et enregistre les POST / PUT /
/// DELETE (avec mode d'échec pour les chemins d'erreur).
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final List<String> postPaths = [];
  final List<Map<String, dynamic>?> postDatas = [];
  final List<String> putPaths = [];
  final List<Map<String, dynamic>?> putDatas = [];
  final List<String> deletePaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postDatas.add(data is Map<String, dynamic> ? data : null);
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
    return _json(path, {});
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

const _MODULES = [
  {
    'key': 'DASHBOARD',
    'label': 'Tableaux de bord',
    'description': 'Vue d\u2019ensemble et pilotage',
    'icon': 'LayoutDashboard',
    'section': 'Pilotage',
    'ordre': 1,
    'enabled': true,
  },
  {
    'key': 'SOULS',
    'label': 'Âmes & disciples',
    'description': 'Registre des disciples',
    'icon': 'Heart',
    'section': 'Discipolat',
    'ordre': 4,
    'enabled': false,
  },
  {
    'key': 'MAP',
    'label': 'Cartographie',
    'description': null,
    'icon': 'Map',
    'section': 'Pilotage',
    'ordre': 3,
    'enabled': true,
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
    // Surface haute : l'éditeur (bottom sheet à 90 % de la hauteur) doit
    // tenir entièrement à l'écran pour que « Enregistrer » soit tappable.
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: PlatformModulesScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche les modules groupés par section avec statut', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _MODULES));
    await pumpScreen(tester, api);

    expect(find.text('Modules'), findsOneWidget);
    expect(find.text('PILOTAGE'), findsOneWidget);
    expect(find.text('DISCIPOLAT'), findsOneWidget);
    expect(find.text('Tableaux de bord'), findsOneWidget);
    expect(find.text('Âmes & disciples'), findsOneWidget);
    expect(find.text('Actif'), findsNWidgets(2));
    expect(find.text('Inactif'), findsOneWidget);
    expect(find.text('DASHBOARD'), findsOneWidget);
  });

  testWidgets('affiche « Aucun module » quand la liste est vide', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, <dynamic>[]));
    await pumpScreen(tester, api);

    expect(find.text('Aucun module'), findsOneWidget);
  });

  testWidgets('bascule d’activation → PUT /platform/modules/{key} avec enabled', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _MODULES));
    await pumpScreen(tester, api);

    // DASHBOARD est actif → on le désactive.
    await tester.tap(find.byType(Switch).first);
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/platform/modules/DASHBOARD'));
    expect(api.putDatas.last?['enabled'], false);
  });

  testWidgets('crée un module → POST /platform/modules avec le payload complet', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _MODULES));
    await pumpScreen(tester, api);

    await tester.ensureVisible(find.text('Nouveau module'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Nouveau module'));
    await tester.pumpAndSettle();

    expect(find.text('Nouveau module'), findsNWidgets(2)); // bouton + titre de l'éditeur

    await tester.enterText(find.widgetWithText(TextField, 'Clé (unique)'), 'PRAYERS');
    await tester.enterText(find.widgetWithText(TextField, 'Libellé'), 'Prières');
    await tester.ensureVisible(find.text('Enregistrer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/platform/modules'));
    final payload = api.postDatas.last;
    expect(payload?['key'], 'PRAYERS');
    expect(payload?['label'], 'Prières');
    expect(payload?['enabled'], true);
    expect(find.text('Module créé'), findsOneWidget);
  });

  testWidgets('modifie un module → PUT /platform/modules/{key}/edit', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _MODULES));
    await pumpScreen(tester, api);

    // Ouvrir l'éditeur depuis la carte DASHBOARD (icône crayon).
    await tester.tap(find.byIcon(Icons.edit_rounded).first);
    await tester.pumpAndSettle();

    expect(find.text('Modifier le module'), findsOneWidget);
    // La clé est pré-remplie dans le champ (en lecture seule en édition).
    expect(find.widgetWithText(TextField, 'DASHBOARD'), findsOneWidget);

    await tester.ensureVisible(find.text('Enregistrer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/platform/modules/DASHBOARD/edit'));
    expect(api.putDatas.last?['label'], 'Tableaux de bord');
    expect(find.text('Module modifié'), findsOneWidget);
  });

  testWidgets('supprime avec confirmation → DELETE /platform/modules/{key}', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _MODULES));
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.delete_rounded).first);
    await tester.pumpAndSettle();

    expect(find.text('Supprimer le module « Tableaux de bord » ?'), findsOneWidget);
    await tester.tap(find.text('Oui'));
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/platform/modules/DASHBOARD'));
    expect(find.text('Module « Tableaux de bord » supprimé'), findsOneWidget);
  });
}
