import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/ai_visit_notes/ai_visit_notes_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/ai-visit-notes') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'visitId': 'vis12345-abcd',
                  'aiSummary': 'Visite positive, famille encourageante.',
                  'aiSentiment': 'POSITIVE',
                  'aiActionItems': '["Planifier une suivante", "Envoyer un encouragement"]',
                  'isVerified': true,
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('AiVisitNotesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: AiVisitNotesScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('📝 Notes IA visites'), findsOneWidget);
    });

    testWidgets('shows visit notes from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: AiVisitNotesScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Visite #vis12345'), findsOneWidget);
      expect(find.text('Visite positive, famille encourageante.'), findsOneWidget);
      expect(find.text('Planifier une suivante'), findsOneWidget);
      expect(find.text('Envoyer un encouragement'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await tester.pumpWidget(MaterialApp(home: AiVisitNotesScreen(apiService: _FakeApiService(empty: true))));
      await tester.pumpAndSettle();
      expect(find.text('Aucune note de visite analysée.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await tester.pumpWidget(MaterialApp(home: AiVisitNotesScreen(apiService: _FakeApiService(fail: true))));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les notes IA.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
