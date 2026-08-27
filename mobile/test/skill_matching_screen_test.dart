import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/skill_matching/skill_matching_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/api/v1/skill-matching') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'content': [
        {'membreNom': 'Jean-Pierre M.', 'departement': 'Louange', 'competence': 'Animation', 'score': 92},
      ]});
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('SkillMatchingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SkillMatchingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Matching Compétences'), findsOneWidget);
    });
    testWidgets('shows matches', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SkillMatchingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Jean-Pierre M.'), findsOneWidget);
    });
  });
}
