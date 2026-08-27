import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/dev_plans/dev_plan_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.contains('/development-plans/by-member')) {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: [
        {'titre': 'Améliorer la présence', 'statut': 'ACTIF', 'priorite': 'HAUTE', 'progression': 60},
        {'titre': 'Formation accueil', 'statut': 'TERMINE', 'priorite': 'FAIBLE', 'progression': 100},
      ]);
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('DevelopmentPlanScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: DevelopmentPlanScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('📈 Mon Plan de Développement'), findsOneWidget);
    });
    testWidgets('shows active objectives', (tester) async {
      await tester.pumpWidget(MaterialApp(home: DevelopmentPlanScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Améliorer la présence'), findsOneWidget);
    });
    testWidgets('shows completed objectives', (tester) async {
      await tester.pumpWidget(MaterialApp(home: DevelopmentPlanScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Formation accueil'), findsOneWidget);
    });
  });
}
