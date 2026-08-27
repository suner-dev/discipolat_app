import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/platform/platform_menus_screen.dart';

/// ApiService factice : renvoie les menus + modules et enregistre les
/// POST / PUT / DELETE (chemins et payloads).
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final List<String> postPaths = [];
  final List<Map<String, dynamic>?> postDatas = [];
  final List<dynamic> reorderDatas = [];
  final List<String> putPaths = [];
  final List<Map<String, dynamic>?> putDatas = [];
  final List<String> deletePaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    if (data is List) {
      reorderDatas.add(data);
    } else {
      postDatas.add(data as Map<String, dynamic>);
    }
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

const _modules = [
  {'key': 'DASHBOARD', 'label': 'Tableaux de bord', 'enabled': true},
  {'key': 'SOULS', 'label': 'Âmes & disciples', 'enabled': true},
];

const _menus = [
  {
    'id': 'm1',
    'key': 'dashboard',
    'label': 'Tableau de bord',
    'href': '/dashboard',
    'icon': 'LayoutDashboard',
    'section': 'Pilotage',
    'ordre': 1,
    'roles': ['ADMIN', 'PASTEUR'],
    'moduleKey': 'DASHBOARD',
    'enabled': true,
  },
  {
    'id': 'm2',
    'key': 'dashboard-pasteur',
    'label': 'Pilotage Pasteur',
    'href': '/dashboard/pasteur',
    'icon': 'Sparkles',
    'section': 'Pilotage',
    'ordre': 2,
    'roles': ['ADMIN', 'PASTEUR'],
    'moduleKey': '',
    'enabled': false,
  },
  {
    'id': 'm3',
    'key': 'souls',
    'label': 'Âmes',
    'href': '/souls',
    'icon': 'Heart',
    'section': 'Discipolat',
    'ordre': 1,
    'roles': <String>[],
    'moduleKey': 'SOULS',
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
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: PlatformMenusScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche les menus groupés par section avec libellés, URL, rôles et module', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/platform/modules') return _json(path, _modules);
      return _json(path, _menus);
    });
    await pumpScreen(tester, api);

    expect(find.text('Menus'), findsOneWidget);
    expect(find.text('PILOTAGE'), findsOneWidget);
    expect(find.text('DISCIPOLAT'), findsOneWidget);
    expect(find.text('Tableau de bord'), findsOneWidget);
    expect(find.text('Pilotage Pasteur'), findsOneWidget);
    expect(find.text('/dashboard'), findsOneWidget);
    expect(find.text('DASHBOARD'), findsOneWidget);
    expect(find.text('ADMIN'), findsNWidgets(2)); // m1 + m2
    expect(find.text('PASTEUR'), findsNWidgets(2));
    expect(find.text('Âmes'), findsOneWidget);
  });

  testWidgets('affiche « Aucun menu » quand la liste est vide', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, <dynamic>[]));
    await pumpScreen(tester, api);

    expect(find.text('Aucun menu configuré'), findsOneWidget);
  });

  testWidgets('bascule d’activation → PUT /platform/menus/{id} avec l’objet complet', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/platform/modules') return _json(path, _modules);
      return _json(path, _menus);
    });
    await pumpScreen(tester, api);

    // m1 est actif → on le désactive.
    await tester.tap(find.byType(Switch).first);
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/platform/menus/m1'));
    expect(api.putDatas.last?['enabled'], false);
    expect(api.putDatas.last?['id'], 'm1');
  });

  testWidgets('réordonne une section → POST /platform/menus/reorder', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/platform/modules') return _json(path, _modules);
      return _json(path, _menus);
    });
    await pumpScreen(tester, api);

    // Descendre « Tableau de bord » (1er de Pilotage) → on appelle reorder.
    await tester.tap(find.byIcon(Icons.arrow_downward).first);
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/platform/menus/reorder'));
    // Le payload est une liste d'éléments {id, ordre, section} : la section
    // Pilotage passe de [m1, m2] à [m2, m1] avec des ordres séquentiels.
    expect(api.reorderDatas, hasLength(1));
    final reorder = (api.reorderDatas.first as List).cast<Map<String, dynamic>>();
    expect(reorder.map((e) => e['id']), ['m2', 'm1']);
    expect(reorder.every((e) => e['section'] == 'Pilotage'), isTrue);
    expect(reorder.map((e) => e['ordre']), [0, 1]);
  });

  testWidgets('crée un menu → POST /platform/menus avec le payload complet', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/platform/modules') return _json(path, _modules);
      return _json(path, _menus);
    });
    await pumpScreen(tester, api);

    await tester.ensureVisible(find.text('Ajouter un menu'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ajouter un menu'));
    await tester.pumpAndSettle();

    expect(find.text('Ajouter un menu'), findsNWidgets(2)); // bouton + titre de l'éditeur

    await tester.enterText(find.widgetWithText(TextField, 'Clé'), 'prayers');
    await tester.enterText(find.widgetWithText(TextField, 'Libellé'), 'Prières');
    await tester.enterText(find.widgetWithText(TextField, 'URL'), '/prayers');
    await tester.ensureVisible(find.text('Enregistrer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/platform/menus'));
    final payload = api.postDatas.last;
    expect(payload?['key'], 'prayers');
    expect(payload?['label'], 'Prières');
    expect(payload?['href'], '/prayers');
    expect(payload?['enabled'], true);
    expect(find.text('Menu sauvegardé'), findsOneWidget);
  });

  testWidgets('modifie un menu → PUT /platform/menus/{id}', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/platform/modules') return _json(path, _modules);
      return _json(path, _menus);
    });
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.edit_rounded).first);
    await tester.pumpAndSettle();

    expect(find.text('Modifier le menu'), findsWidgets);

    await tester.ensureVisible(find.text('Enregistrer'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Enregistrer'));
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/platform/menus/m1'));
    expect(api.putDatas.last?['label'], 'Tableau de bord');
    expect(api.putDatas.last?['section'], 'Pilotage');
    expect(find.text('Menu sauvegardé'), findsOneWidget);
  });

  testWidgets('supprime avec confirmation → DELETE /platform/menus/{id}', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/platform/modules') return _json(path, _modules);
      return _json(path, _menus);
    });
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.delete_rounded).first);
    await tester.pumpAndSettle();    expect(find.text('Supprimer le menu « Tableau de bord » ?'), findsWidgets);
    await tester.tap(find.text('Oui'));
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/platform/menus/m1'));
    expect(find.text('Menu « Tableau de bord » supprimé'), findsOneWidget);
  });
}
