import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/dashboard/discipleship_path_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.contains('/discipleship-paths/member')) {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: [
        {'key': 'DISCOVERY', 'label': 'Découverte', 'description': 'Premiers pas'},
        {'key': 'FOUNDATION', 'label': 'Fondations', 'description': 'Bases de la foi'},
      ]);
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  setUp(() {
    AuthState().setAuthenticated(true, userData: {
      'userId': 'test-user-1',
      'email': 'test@test.com',
      'roles': ['MEMBRE'],
      'activeRole': 'MEMBRE',
    });
  });
  tearDown(() => AuthState().logout());

  group('DiscipleshipPathScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: DiscipleshipPathScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Parcours de discipolat'), findsOneWidget);
    });
    testWidgets('shows stages', (tester) async {
      await tester.pumpWidget(MaterialApp(home: DiscipleshipPathScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Découverte'), findsOneWidget);
    });
  });
}
