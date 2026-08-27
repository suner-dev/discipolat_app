import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/platform/platform_pages_screen.dart';

/// ApiService factice : renvoie les pages et enregistre les POST / DELETE.
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final List<String> postPaths = [];
  final List<Map<String, dynamic>?> postDatas = [];
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
  Future<Response> delete(String path) async {
    deletePaths.add(path);
    return _json(path, {});
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

const _pages = [
  {
    'id': 'p1',
    'key': 'APERCU',
    'title': 'Vue d’ensemble de l’église',
    'slug': 'apercu-eglise',
    'layout': 'GRID_2',
    'blocks': <dynamic>[],
    'roles': ['ADMIN', 'PASTEUR'],
    'enabled': true,
    'published': true,
    'version': 2,
  },
  {
    'id': 'p2',
    'key': 'BROUILLON',
    'title': 'Brouillon',
    'slug': 'brouillon',
    'layout': 'STACK',
    'blocks': <dynamic>[],
    'roles': <dynamic>[],
    'enabled': true,
    'published': false,
    'version': 1,
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
    await tester.pumpWidget(MaterialApp(home: PlatformPagesScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche les pages avec titre, adresse, badges et rôles', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _pages));
    await pumpScreen(tester, api);

    expect(find.text('Pages'), findsOneWidget);
    expect(find.text('Vue d’ensemble de l’église'), findsOneWidget);
    expect(find.text('/pages/apercu-eglise'), findsOneWidget);
    expect(find.text('Publiée · v2'), findsOneWidget);
    // « Brouillon » = titre de la page ET badge de statut.
    expect(find.text('Brouillon'), findsWidgets);
    expect(find.text('ADMIN'), findsOneWidget);
    expect(find.text('PASTEUR'), findsOneWidget);
  });

  testWidgets('affiche « Aucune page personnalisée » quand la liste est vide', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, <dynamic>[]));
    await pumpScreen(tester, api);

    expect(find.text('Aucune page configurée'), findsOneWidget);
  });

  testWidgets('bascule de publication → POST /pages/{id}/publish', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _pages));
    await pumpScreen(tester, api);

    // p1 est publiée → on la dépublie (premier Switch).
    await tester.tap(find.byType(Switch).first);
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/pages/p1/publish'));
    expect(api.postDatas.last?['published'], false);
  });

  testWidgets('supprime avec confirmation → DELETE /pages/{id}', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _pages));
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.delete_rounded).first);
    await tester.pumpAndSettle();

    expect(find.text('Supprimer la page « Vue d’ensemble de l’église » ?'), findsOneWidget);
    await tester.tap(find.text('Oui'));
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/pages/p1'));
    expect(find.text('Page « Vue d’ensemble de l’église » supprimée'), findsOneWidget);
  });
}
