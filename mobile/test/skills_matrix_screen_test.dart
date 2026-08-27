import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/skills_matrix/skills_matrix_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/api/v1/skills/matrix') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: [
        {'nom': 'Animation', 'available': 4, 'needed': 6},
      ]);
    }
    if (path == '/api/v1/skills') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'content': []});
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('SkillsMatrixScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SkillsMatrixScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Matrice de Compétences'), findsOneWidget);
    });
    testWidgets('shows skills', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SkillsMatrixScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Animation'), findsOneWidget);
    });
  });
}
