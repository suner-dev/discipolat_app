import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/family_cohesion/family_cohesion_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/api/v1/family-cohesion') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'score': 7.5, 'indicators': {'Participation': 82}});
    }
    if (path == '/api/v1/families') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'content': [
        {'nom': 'Famille Grâce', 'cohesionScore': 8.2},
      ]});
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('FamilyCohesionScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyCohesionScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Cohésion Familiale'), findsOneWidget);
    });
    testWidgets('shows families', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyCohesionScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Famille Grâce'), findsOneWidget);
    });
  });
}
