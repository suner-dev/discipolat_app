import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/family_resources/family_resources_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.contains('/family-resources')) {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'content': [
        {'titre': 'Étude Jean 3:16', 'categorie': 'biblique', 'createdAt': '2025-08-22'},
      ]});
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('FamilyResourcesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyResourcesScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Ressources Familiales'), findsOneWidget);
    });
    testWidgets('shows resources', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyResourcesScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Étude Jean 3:16'), findsOneWidget);
    });
  });
}
