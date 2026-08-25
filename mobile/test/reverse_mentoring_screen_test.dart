import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/reverse_mentoring/reverse_mentoring_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/reverse-mentoring') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'topic': 'Compréhension des jeunes',
                  'description': 'Mieux encadrer la génération TikTok.',
                  'status': 'PENDING',
                  'urgencyLevel': 4,
                },
                {
                  'topic': 'Outils numériques',
                  'description': '',
                  'status': 'ACCEPTED',
                  'urgencyLevel': 2,
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('ReverseMentoringScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ReverseMentoringScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('🔄 Mentorat inversé'), findsOneWidget);
    });

    testWidgets('shows requests from API with readable status', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ReverseMentoringScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Compréhension des jeunes'), findsOneWidget);
      expect(find.text('Mieux encadrer la génération TikTok.'), findsOneWidget);
      expect(find.text('En attente'), findsOneWidget);
      expect(find.text('Acceptée'), findsOneWidget);
      expect(find.text('Urgence 4/5'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ReverseMentoringScreen(apiService: _FakeApiService(empty: true))));
      await tester.pumpAndSettle();
      expect(find.text('Aucune demande de mentorat inversé.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ReverseMentoringScreen(apiService: _FakeApiService(fail: true))));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les demandes.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
