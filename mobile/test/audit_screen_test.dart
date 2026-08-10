import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/date_symbol_data_local.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/users/audit_screen.dart';

/// ApiService factice : renvoie une page du journal d'audit et enregistre les
/// paramètres de chaque requête (pour vérifier filtres et pagination).
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final List<Map<String, dynamic>?> getParams = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    getParams.add(params);
    return _handler(path, params);
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

Map<String, dynamic> _page({int totalPages = 1}) => {
  'content': [
    {
      'action': 'USER_CREATE',
      'entiteType': 'USER',
      'createdAt': '2026-08-10T10:00:00',
      'emailUtilisateur': 'admin@discipolat.com',
      'details': 'Création du compte Paul',
    },
    {
      'action': 'SOUL_UPDATE',
      'entiteType': 'SOUL',
      'createdAt': '2026-08-10T11:30:00',
      'utilisateurId': '12345678-1234-1234-1234-123456789012',
      'details': null,
    },
  ],
  'totalPages': totalPages,
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

  setUpAll(() async {
    await initializeDateFormatting('fr_FR');
  });

  setUp(() => _setRole('ADMIN'));
  tearDown(() => AuthState().logout());

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.pumpWidget(MaterialApp(home: AuditScreen(apiService: api)));
    await tester.pumpAndSettle();
  }

  testWidgets('affiche les entrées du journal avec action, utilisateur et entité', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _page()));
    await pumpScreen(tester, api);

    expect(find.text('Journal d\'audit'), findsOneWidget);
    expect(find.text('USER_CREATE'), findsOneWidget);
    expect(find.text('SOUL_UPDATE'), findsOneWidget);
    expect(find.text('admin@discipolat.com'), findsOneWidget);
    expect(find.text('USER'), findsOneWidget);
    expect(find.text('SOUL'), findsOneWidget);
    // Détails affichés uniquement quand présents
    expect(find.text('Création du compte Paul'), findsOneWidget);
  });

  testWidgets('filtre par entité → rechargement avec le paramètre entiteType', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _page()));
    await pumpScreen(tester, api);

    // Ouvrir le sélecteur puis choisir « Utilisateurs »
    await tester.tap(find.text('Toutes les entités'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Utilisateurs').last);
    await tester.pumpAndSettle();

    // Dernier appel : page 0, taille 20, filtre entité USER
    expect(api.getParams.last?['entiteType'], 'USER');
    expect(api.getParams.last?['page'], '0');
    expect(api.getParams.last?['size'], '20');
  });

  testWidgets('paginate : page suivante → rechargement avec page=1', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, _page(totalPages: 3)));
    await pumpScreen(tester, api);

    expect(find.text('Page 1 / 3'), findsOneWidget);

    await tester.tap(find.byIcon(Icons.chevron_right));
    await tester.pumpAndSettle();

    expect(api.getParams.last?['page'], '1');
    expect(find.text('Page 2 / 3'), findsOneWidget);
  });

  testWidgets('affiche « Aucune entrée d’audit » quand le journal est vide', (tester) async {
    final api = _FakeApiService((path, params) => _json(path, {
      'content': <dynamic>[],
      'totalPages': 0,
    }));
    await pumpScreen(tester, api);

    expect(find.text('Aucune entrée d\'audit'), findsOneWidget);
  });

  testWidgets('erreur de chargement → état vide (pas de crash)', (tester) async {
    final api = _FakeApiService((path, params) {
      throw DioException(requestOptions: RequestOptions(path: path));
    });
    await pumpScreen(tester, api);

    expect(find.text('Aucune entrée d\'audit'), findsOneWidget);
  });
}
