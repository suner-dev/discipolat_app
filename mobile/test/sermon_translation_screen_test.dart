import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/sermon_translations/sermon_translation_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');
  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path.contains('/sermons/translations')) {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'content': [
        {'titre': 'Culte du 24 août', 'langues': '5 langues', 'statut': 'TERMINE'},
      ]});
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('SermonTranslationScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SermonTranslationScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('🌍 Traduction des sermons'), findsOneWidget);
    });
    testWidgets('shows translations', (tester) async {
      await tester.pumpWidget(MaterialApp(home: SermonTranslationScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Culte du 24 août'), findsOneWidget);
    });
  });
}
