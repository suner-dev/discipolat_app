import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/users/users_list_screen.dart';

/// ApiService factice : renvoie la liste des utilisateurs + la charge de
/// travail, et enregistre les POST / PATCH / DELETE (chemins et payloads).
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler, {this.failPosts = false}) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final bool failPosts;
  final List<String> postPaths = [];
  final List<Map<String, dynamic>?> postDatas = [];
  final List<String> patchPaths = [];
  final List<Map<String, dynamic>?> patchDatas = [];
  final List<String> deletePaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async => _handler(path, params);

  @override
  Future<Response> post(String path, {dynamic data}) async {
    postPaths.add(path);
    postDatas.add(data is Map<String, dynamic> ? data : null);
    if (failPosts) throw DioException(requestOptions: RequestOptions(path: path));
    return _json(path, {});
  }

  @override
  Future<Response> patch(String path, {dynamic data}) async {
    patchPaths.add(path);
    patchDatas.add(data is Map<String, dynamic> ? data : null);
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

/// Utilisateurs de démonstration (variété de rôles et statuts).
const _users = {
  'content': [
    {
      'id': 'u1', 'firstName': 'Jean', 'lastName': 'Dupont', 'email': 'jean@discipolat.test',
      'role': 'PASTEUR', 'statut': 'ACTIVE',
    },
    {
      'id': 'u2', 'firstName': 'Marie', 'lastName': 'Martin', 'email': 'marie@discipolat.test',
      'role': 'FAISEUR', 'statut': 'ACTIVE',
    },
    {
      'id': 'u3', 'firstName': 'Paul', 'lastName': 'Robert', 'email': 'paul@discipolat.test',
      'role': 'RESPONSABLE', 'statut': 'INACTIVE',
    },
  ],
};

/// Dataset minimal (un seul membre) pour les tests de sélection de rôle :
/// évite les collisions de textes entre les badges de la liste et les options
/// du menu déroulant.
const _minimalUsers = {
  'content': [
    {
      'id': 'u9', 'firstName': 'Anna', 'lastName': 'Blanc', 'email': 'anna@discipolat.test',
      'role': 'MEMBRE', 'statut': 'ACTIVE',
    },
  ],
};

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

  Future<void> pumpScreen(WidgetTester tester, ApiService api, {Object? users = _users}) async {
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: UsersListScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  _FakeApiService fakeWith({Object? users = _users, bool failPosts = false, Object? evalScores}) {
    return _FakeApiService((path, params) {
      if (path == '/users') return _json(path, users ?? {'content': []});
      if (path == '/users/faiseur-workload') return _json(path, <dynamic>[]);
      if (path == '/users/evaluation-scores') return _json(path, evalScores ?? <String, dynamic>{});
      return _json(path, {'content': []});
    }, failPosts: failPosts);
  }

  testWidgets('affiche la liste des utilisateurs avec nom, email, rôle et statut', (tester) async {
    final api = fakeWith();
    await pumpScreen(tester, api);

    expect(find.text('Utilisateurs'), findsOneWidget);
    expect(find.text('Jean Dupont'), findsOneWidget);
    expect(find.text('jean@discipolat.test'), findsOneWidget);
    expect(find.text('Pasteur'), findsOneWidget);
    expect(find.text('Faiseur'), findsOneWidget);
    expect(find.text('Responsable'), findsOneWidget);
    expect(find.text('Actif'), findsNWidgets(2)); // Jean + Marie
    expect(find.text('Inactif'), findsOneWidget); // Paul
  });

  testWidgets('affiche « Aucun utilisateur » quand la liste est vide', (tester) async {
    final api = fakeWith(users: {'content': []});
    await pumpScreen(tester, api);

    expect(find.text('Aucun utilisateur'), findsOneWidget);
  });

  testWidgets('ouvre le formulaire de création via le bouton +', (tester) async {
    final api = fakeWith();
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    expect(find.text('Nouvel utilisateur'), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Prénom'), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Nom'), findsOneWidget);
    expect(find.widgetWithText(TextField, 'Email'), findsOneWidget);
    expect(find.widgetWithText(DropdownButtonFormField<String>, 'Rôle'), findsOneWidget);
    expect(find.text('Créer'), findsOneWidget);
  });

  testWidgets('crée un utilisateur → POST /users avec le rôle FAISEUR par défaut', (tester) async {
    final api = fakeWith();
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextField, 'Prénom'), 'Luc');
    await tester.enterText(find.widgetWithText(TextField, 'Nom'), 'Bernard');
    await tester.enterText(find.widgetWithText(TextField, 'Email'), 'luc@discipolat.test');
    await tester.tap(find.text('Créer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/users'));
    final payload = api.postDatas.last;
    expect(payload?['firstName'], 'Luc');
    expect(payload?['lastName'], 'Bernard');
    expect(payload?['email'], 'luc@discipolat.test');
    expect(payload?['role'], 'FAISEUR'); // rôle par défaut
    expect(payload?['password'], 'password123');
    expect(find.text('Compte créé avec succès'), findsOneWidget);
  });

  testWidgets('échec de création → SnackBar d’erreur et le formulaire reste ouvert', (tester) async {
    final api = fakeWith(failPosts: true);
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextField, 'Prénom'), 'Luc');
    await tester.enterText(find.widgetWithText(TextField, 'Email'), 'luc@discipolat.test');
    await tester.tap(find.text('Créer'));
    await tester.pumpAndSettle();

    expect(api.postPaths, contains('/users'));
    expect(find.text('Erreur lors de la création'), findsOneWidget);
    // Le formulaire reste ouvert pour corriger.
    expect(find.text('Nouvel utilisateur'), findsOneWidget);
  });

  testWidgets('sélectionne un rôle dans le menu déroulant → le payload porte le rôle choisi', (tester) async {
    final api = fakeWith(users: _minimalUsers);
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.add));
    await tester.pumpAndSettle();

    // Ouverture du menu déroulant « Rôle » puis choix de « Responsable ».
    await tester.tap(find.byType(DropdownButtonFormField<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Responsable').last);
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextField, 'Prénom'), 'Emma');
    await tester.enterText(find.widgetWithText(TextField, 'Email'), 'emma@discipolat.test');
    await tester.tap(find.text('Créer'));
    await tester.pumpAndSettle();

    expect(api.postDatas.last?['role'], 'RESPONSABLE');
  });

  testWidgets('promotion d’un faiseur → PATCH /users/{id}/promote-faiseur', (tester) async {
    final api = fakeWith();
    await pumpScreen(tester, api);

    // Jean (u1, PASTEUR) et Paul (u3, RESPONSABLE) exposent la promotion
    // (flèche vers le haut) ; le premier de la liste est Jean.
    await tester.tap(find.byIcon(Icons.arrow_upward).first);
    await tester.pumpAndSettle();

    expect(find.text('Promouvoir en Faiseur'), findsOneWidget);
    expect(find.text('Promouvoir Jean Dupont au rôle de Faiseur de disciples ?'), findsOneWidget);
    await tester.tap(find.text('Promouvoir'));
    await tester.pumpAndSettle();

    expect(api.patchPaths, contains('/users/u1/promote-faiseur'));
  });

  testWidgets('rétrogradation → PATCH /users/{id}/demote avec le nouveau rôle', (tester) async {
    final api = fakeWith();
    await pumpScreen(tester, api);

    // Marie (u2, FAISEUR) expose la rétrogradation (flèche vers le bas).
    await tester.tap(find.byIcon(Icons.arrow_downward).first);
    await tester.pumpAndSettle();

    expect(find.text('Rétrograder'), findsNWidgets(2)); // titre + bouton
    // Sélection d'un nouveau rôle dans le modal (modification avec sélecteur).
    await tester.tap(find.byType(DropdownButtonFormField<String>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Responsable').last);
    await tester.pumpAndSettle();

    // Le bouton du modal est un FilledButton.icon (sous-classe) ; bySubtype
    // matche aussi bien le titre que le bouton, on prend le dernier (bouton).
    await tester.tap(find.text('Rétrograder').last);
    await tester.pumpAndSettle();

    expect(api.patchPaths, contains('/users/u2/demote'));
    expect(api.patchDatas.last?['newRole'], 'RESPONSABLE'); // rôle choisi
  });

  testWidgets('affiche le badge moyenne d’évaluation avec étoiles et compteur', (tester) async {
    final api = fakeWith(evalScores: {
      'u2': {
        'RESPONSABLE': {'moyenne': 4.0, 'total': 2},
        'FAISEUR': {'moyenne': 5.0, 'total': 1},
      },
    });
    await pumpScreen(tester, api);

    // Marie (u2) : moyenne (4+5)/2 = 4.5 → 5 étoiles pleines.
    expect(find.text('4.5'), findsOneWidget);
    expect(find.text('(3)'), findsOneWidget);
    // Jean (u1) et Paul (u3) n'ont pas d'évaluation → aucun badge.
    expect(find.byIcon(Icons.star_rounded), findsNWidgets(5));
  });

  testWidgets('suppression définitive → dialogue → DELETE /users/{id}/hard-delete', (tester) async {
    final api = fakeWith();
    await pumpScreen(tester, api);

    await tester.tap(find.byIcon(Icons.delete_forever).first);
    await tester.pumpAndSettle();

    expect(find.text('Suppression définitive'), findsOneWidget);
    expect(find.text('Supprimer'), findsOneWidget);
    await tester.tap(find.text('Supprimer'));
    await tester.pumpAndSettle();

    // Le premier de la liste est Jean (u1, PASTEUR).
    expect(api.deletePaths, contains('/users/u1/hard-delete'));
  });
}
