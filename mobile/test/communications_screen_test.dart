import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/communications/communications_screen.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// ApiService factice : renvoie les annonces et enregistre les POST / PUT / DELETE.
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final List<String> postPaths = [];
  final List<String> putPaths = [];
  final List<Map<String, dynamic>?> putDatas = [];
  final List<String> deletePaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    return _json(path, {'id': 'comm-new', 'statut': 'BROUILLON', 'destinataires': 1});
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

final _published = [
  {
    'id': 'comm-1',
    'titre': 'Rentrée de septembre',
    'contenu': 'La rentrée aura lieu le 7 septembre.',
    'cible': 'TOUS',
    'roles': <dynamic>[],
    'statut': 'PUBLIEE',
    'datePublication': '2026-08-17T10:00:00',
  },
];

final _all = [
  ..._published,
  {
    'id': 'comm-2',
    'titre': 'Conseil des responsables',
    'contenu': 'Réunion du conseil jeudi.',
    'cible': 'ROLE',
    'roles': <dynamic>['RESPONSABLE'],
    'statut': 'BROUILLON',
  },
];

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: CommunicationsScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  setUp(() {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'u1',
      'email': 'admin@test',
      'role': 'ADMIN',
      'roles': ['ADMIN'],
      'activeRole': 'ADMIN',
    });
  });

  testWidgets('affiche la gestion (ADMIN) et les annonces publiées', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/communications') return _json(path, _published);
      return _json(path, _all); // /communications/admin
    });
    await pumpScreen(tester, api);

    expect(find.text('Annonces'), findsOneWidget);
    expect(find.text('Gestion des annonces'), findsOneWidget);
    // Brouillon dans la gestion.
    expect(find.text('Conseil des responsables'), findsOneWidget);
    // Annonce publiée visible pour l'utilisateur courant.
    expect(find.text('Rentrée de septembre'), findsWidgets);
    expect(find.text('La rentrée aura lieu le 7 septembre.'), findsWidgets);
  });

  testWidgets('publie une annonce → POST /communications/admin/{id}/publish', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/communications') return _json(path, _published);
      return _json(path, _all);
    });
    await pumpScreen(tester, api);

    // L'icône « envoyer » existe aussi (désactivée) sur les annonces publiées :
    // cibler celle de la carte du brouillon « Conseil des responsables ».
    final sendInDraft = find.descendant(
      of: find.ancestor(of: find.text('Conseil des responsables'), matching: find.byType(GlassCard)),
      matching: find.byIcon(Icons.send_rounded),
    );
    await tester.tap(sendInDraft);
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/communications/admin/comm-2/publish'));
    expect(find.textContaining('diffusée à 1 destinataire'), findsOneWidget);
  });

  testWidgets('supprime une annonce avec confirmation → DELETE', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/communications') return _json(path, _published);
      return _json(path, _all);
    });
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.delete_rounded).first);
    await tester.pumpAndSettle();

    expect(find.text('Supprimer l’annonce ?'), findsOneWidget);
    await tester.tap(find.text('Oui'));
    await tester.pumpAndSettle();

    expect(api.deletePaths, contains('/communications/admin/comm-1'));
    expect(find.text('Annonce supprimée'), findsOneWidget);
  });

  testWidgets('lecture seule pour un MEMBRE : pas de gestion, pas de GET /admin', (tester) async {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'u2',
      'email': 'membre@test',
      'role': 'MEMBRE',
      'roles': ['MEMBRE'],
      'activeRole': 'MEMBRE',
    });
    final paths = <String>[];
    final api = _FakeApiService((path, params) {
      paths.add(path);
      return _json(path, _published);
    });
    await pumpScreen(tester, api);

    expect(find.text('Rentrée de septembre'), findsWidgets);
    expect(find.text('Gestion des annonces'), findsNothing);
    expect(paths, isNot(contains('/communications/admin')));
  });
}
