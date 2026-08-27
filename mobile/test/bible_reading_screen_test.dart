import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/bible_reading/bible_reading_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/api/v1/bible-reading/stats') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: {'streak': 12, 'totalRead': 236, 'totalEntries': 365});
    }
    if (path == '/api/v1/bible-reading/plans') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: [
        {'titre': 'Parcours 365 jours', 'description': 'Bible entière en 1 an', 'joursTotal': 365, 'joursCompletes': 236},
      ]);
    }
    if (path == '/api/v1/bible-reading/today') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: [
        {'id': '1', 'referenceVerset': 'Jean 3:16-21', 'categorie': 'Évangile', 'theme': 'Amour de Dieu', 'lu': false},
      ]);
    }
    if (path == '/api/v1/bible-reading/family-progress') {
      return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('BibleReadingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BibleReadingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('📖 Plan de Lecture Biblique'), findsOneWidget);
    });
    testWidgets('shows today reading', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BibleReadingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Jean 3:16-21'), findsOneWidget);
    });
    testWidgets('shows available plans', (tester) async {
      await tester.pumpWidget(MaterialApp(home: BibleReadingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Parcours 365 jours'), findsOneWidget);
    });
  });
}
