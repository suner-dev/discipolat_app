import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/users/permissions_screen.dart';

/// ApiService factice : sert le contrat CANONIQUE (projection éditeur + catalogue
/// des permissions) et enregistre les PUT avec leur corps.
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler, {this.failPuts = false}) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final bool failPuts;
  final List<String> getPaths = [];
  final List<String> putPaths = [];
  final List<dynamic> putBodies = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params, Map<String, dynamic>? queryParameters}) async {
    getPaths.add(path);
    return _handler(path, params);
  }

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    putBodies.add(data);
    if (failPuts) throw DioException(requestOptions: RequestOptions(path: path));
    return _json(path, {});
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

/// Projection `GET /admin/roles` : deux rôles RÉELLEMENT exercés ici. La matrice
/// legacy (`role`/`permission` en texte) a été remplacée par la jointure
/// `(role_id, permission_id)` — l'écran ne doit plus rien lui demander.
const _roles = [
  {
    'id': 'role-admin-1',
    'key': 'ADMIN',
    'label': 'Administrateur',
    'permissions': ['USER_READ'],
    'local': false,
    'effectiveForTenant': true,
  },
  {
    'id': 'role-faiseur-2',
    'key': 'FAISEUR',
    'label': 'Faiseur',
    'permissions': <String>[],
    'local': true,
    'effectiveForTenant': true,
  },
];

/// Surcouche locale de la même clé : le rôle global homonyme ne décide plus rien,
/// il ne doit pas apparaitre une seconde fois en colonne.
const _rolesWithShadow = [
  ..._roles,
  {
    'id': 'role-admin-global',
    'key': 'ADMIN',
    'label': 'Administrateur (catalogue global)',
    'permissions': ['USER_READ'],
    'local': false,
    'effectiveForTenant': false,
  },
];

const _catalog = [
  {'key': 'USER_READ', 'label': 'Voir les utilisateurs', 'category': 'USERS'},
  {'key': 'USER_CREATE', 'label': 'Créer des utilisateurs', 'category': 'USERS'},
];

void _setRole(String role, {List<String> roles = const []}) {
  AuthState().setAuthenticated(true, userData: {
    'userId': 'user-1',
    'email': 'admin@discipolat.test',
    'roles': roles.isEmpty ? [role] : roles,
    'activeRole': role,
  });
}

/// Routeur du faux : chaque chemin canonique a sa réponse.
Response<dynamic> _route(String path, Map<String, dynamic>? params, List rolesData) {
  if (path == '/admin/roles') return _json(path, rolesData);
  if (path == '/admin/roles/permissions') return _json(path, _catalog);
  return _json(path, []);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() => _setRole('ADMIN'));
  tearDown(() => AuthState().logout());

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.pumpWidget(MaterialApp(home: PermissionsScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('lit le modèle canonique, plus la table legacy /permissions', (tester) async {
    final api = _FakeApiService((path, params) => _route(path, params, _roles));
    await pumpScreen(tester, api);

    expect(api.getPaths, containsAll(<String>['/admin/roles', '/admin/roles/permissions']));
    // CONTRÔLE NÉGATIF : `/permissions` (role_permissions au format texte) n'a
    // plus de colonnes `role`/`permission` depuis V135 — y retourner ferait un
    // 42703 et viderait l'écran.
    expect(api.getPaths.any((p) => p == '/permissions'), isFalse);

    expect(find.text('Permissions'), findsOneWidget);
    expect(find.text('Matrice des permissions'), findsOneWidget);
    // Libellés servis par la base (paramétrables par l'église), pas de dictionnaire
    // écrit en dur dans l'écran.
    expect(find.text('Voir les utilisateurs'), findsOneWidget);
    expect(find.text('USER_READ'), findsOneWidget);
    expect(find.text('Administrateur'), findsOneWidget);
    expect(find.text('Faiseur'), findsOneWidget);
    // USER_READ est actif chez Administrateur (check) et éteint chez Faiseur
    // (close) — une seule ligne, celle portée par au moins un rôle.
    expect(find.byIcon(Icons.check), findsOneWidget);
    expect(find.byIcon(Icons.close), findsOneWidget);
  });

  testWidgets('une surcouche locale prime : le rôle global doublé nest pas colonné', (tester) async {
    final api = _FakeApiService((path, params) => _route(path, params, _rolesWithShadow));
    await pumpScreen(tester, api);

    expect(find.text('Administrateur'), findsOneWidget);
    expect(find.text('Administrateur (catalogue global)'), findsNothing);
  });

  testWidgets('affiche « Aucune permission configurée » quand rien nest exercé', (tester) async {
    final api = _FakeApiService((path, params) => _route(path, params, <dynamic>[]));
    await pumpScreen(tester, api);

    expect(find.text('Aucune permission configurée'), findsOneWidget);
  });

  testWidgets('bascule une cellule → PUT /admin/roles/{id}/permissions avec la matrice complète', (tester) async {
    final api = _FakeApiService((path, params) => _route(path, params, _roles));
    await pumpScreen(tester, api);

    // Une seule cellule éteinte : USER_READ / Faiseur.
    await tester.tap(find.byIcon(Icons.close).first);
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/admin/roles/role-faiseur-2/permissions'));
    // Le contrat remplace la matrice entière : on renvoie la liste complète, pas
    // la seule clé cochée (sinon le rôle perdrait tout le reste).
    final body = api.putBodies.first as Map<String, dynamic>;
    expect((body['permissionKeys'] as List).cast<String>(), contains('USER_READ'));
    expect(api.putPaths.any((p) => p.startsWith('/permissions/')), isFalse);
  });

  testWidgets('échec de mise à jour → SnackBar d’erreur', (tester) async {
    final api = _FakeApiService((path, params) => _route(path, params, _roles), failPuts: true);
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.close).first);
    await tester.pumpAndSettle();

    expect(find.text('Erreur lors de la mise à jour'), findsOneWidget);
  });

  testWidgets('erreur de chargement → état vide (pas de crash)', (tester) async {
    final api = _FakeApiService((path, params) {
      throw DioException(requestOptions: RequestOptions(path: path));
    });
    await pumpScreen(tester, api);

    expect(find.text('Aucune permission configurée'), findsOneWidget);
  });
}
