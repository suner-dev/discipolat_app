import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/users/permissions_screen.dart';

/// ApiService factice : renvoie la matrice des permissions et enregistre les PUT.
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler, {this.failPuts = false}) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final bool failPuts;
  final List<String> putPaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> put(String path, {dynamic data}) async {
    putPaths.add(path);
    if (failPuts) throw DioException(requestOptions: RequestOptions(path: path));
    return _json(path, {});
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

const _PERMISSIONS = [
  {'permission': 'USER_CREATE', 'role': 'ADMIN', 'enabled': false},
  {'permission': 'USER_CREATE', 'role': 'FAISEUR', 'enabled': false},
  {'permission': 'USER_READ', 'role': 'ADMIN', 'enabled': true},
];

void _setRole(String role, {List<String> roles = const []}) {
  AuthState().setAuthenticated(true, userData: {
    'userId': 'user-1',
    'email': 'admin@discipolat.test',
    'roles': roles.isEmpty ? [role] : roles,
    'activeRole': role,
  });
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() => _setRole('ADMIN'));
  tearDown(() => AuthState().logout());

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.pumpWidget(MaterialApp(home: PermissionsScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche la matrice : libellés, codes et colonnes par rôle', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _PERMISSIONS));
    await pumpScreen(tester, api);

    expect(find.text('Permissions'), findsOneWidget);
    expect(find.text('Matrice des permissions'), findsOneWidget);
    // Libellé + code de chaque permission, et en-têtes de colonnes par rôle
    expect(find.text('Créer utilisateur'), findsOneWidget);
    expect(find.text('USER_CREATE'), findsOneWidget);
    expect(find.text('Voir utilisateurs'), findsOneWidget);
    expect(find.text('USER_READ'), findsOneWidget);
    expect(find.text('Admin'), findsOneWidget);
    expect(find.text('Faiseur'), findsOneWidget);
    // USER_READ/ADMIN est activé (check vert) ; les autres cellules sont à
    // false (close) — y compris USER_READ/FAISEUR absent de la matrice
    // (défaut enabled=false).
    expect(find.byIcon(Icons.check), findsOneWidget);
    expect(find.byIcon(Icons.close), findsNWidgets(3));
  });

  testWidgets('affiche « Aucune permission configurée » quand la matrice est vide', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, <dynamic>[]));
    await pumpScreen(tester, api);

    expect(find.text('Aucune permission configurée'), findsOneWidget);
  });

  testWidgets('bascule une permission → PUT /permissions/{role}/{permission}', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _PERMISSIONS));
    await pumpScreen(tester, api);

    // Première cellule désactivée (USER_CREATE / ADMIN) → icône close
    await tester.tap(find.byIcon(Icons.close).first);
    await tester.pumpAndSettle();

    expect(api.putPaths, contains('/permissions/ADMIN/USER_CREATE'));
  });

  testWidgets('échec de mise à jour → SnackBar d’erreur', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _PERMISSIONS), failPuts: true);
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
