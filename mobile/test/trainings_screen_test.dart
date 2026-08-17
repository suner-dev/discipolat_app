import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/trainings/trainings_screen.dart';

/// ApiService factice : renvoie le catalogue / inscriptions / certificats / stats.
class _FakeApiService extends ApiService {
  _FakeApiService(this._handler) : super(baseUrl: 'http://fake');

  final Response<dynamic> Function(String path, Map<String, dynamic>? params) _handler;
  final List<String> getPaths = [];

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    getPaths.add(path);
    return _handler(path, params);
  }
}

Response<dynamic> _json(String path, Object data) =>
    Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: data);

final _stats = {
  'nbCours': 3,
  'nbInscrits': 12,
  'nbCertificats': 4,
  'progressionMoyenne': 67,
  'parCategorie': {'DISCIPOLAT': 2, 'MINISTERE': 1},
  'parStatut': {'TERMINE': 2},
};

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Future<void> pumpScreen(WidgetTester tester, ApiService api) async {
    await tester.binding.setSurfaceSize(const Size(800, 1600));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(MaterialApp(home: TrainingsScreen(apiService: api)));
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

  testWidgets('affiche les KPIs réels de la formation pour un ADMIN', (tester) async {
    final api = _FakeApiService((path, params) {
      if (path == '/trainings/stats') return _json(path, _stats);
      if (path == '/trainings/courses') return _json(path, <dynamic>[]);
      if (path == '/trainings/my-enrollments') return _json(path, <dynamic>[]);
      return _json(path, <dynamic>[]);
    });
    await pumpScreen(tester, api);

    expect(find.text('Cours'), findsOneWidget);
    expect(find.text('3'), findsOneWidget);
    expect(find.text('Inscrits'), findsOneWidget);
    expect(find.text('12'), findsOneWidget);
    // « Certificats » : KPI + onglet TabBar → plusieurs occurrences.
    expect(find.text('Certificats'), findsWidgets);
    expect(find.text('4'), findsOneWidget);
    expect(find.text('Progression'), findsOneWidget);
    expect(find.text('67%'), findsOneWidget);
  });

  testWidgets('pas de stats pour un MEMBRE : aucun appel /trainings/stats', (tester) async {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'u2',
      'email': 'membre@test',
      'role': 'MEMBRE',
      'roles': ['MEMBRE'],
      'activeRole': 'MEMBRE',
    });
    final api = _FakeApiService((path, params) {
      if (path == '/trainings/courses') return _json(path, <dynamic>[]);
      if (path == '/trainings/my-enrollments') return _json(path, <dynamic>[]);
      return _json(path, <dynamic>[]);
    });
    await pumpScreen(tester, api);

    expect(find.text('Cours'), findsNothing);
    expect(api.getPaths, isNot(contains('/trainings/stats')));
  });
}
