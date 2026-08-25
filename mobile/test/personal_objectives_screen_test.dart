import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/personal_objectives/personal_objectives_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/personal-objectives') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'titre': 'Lire la Bible en un an',
                  'description': 'Un chapitre par jour',
                  'objectifCible': 10,
                  'progressionActuelle': 3,
                  'statut': 'EN_COURS',
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('PersonalObjectivesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PersonalObjectivesScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('🎯 Objectifs personnels'), findsOneWidget);
    });

    testWidgets('shows objective with progress from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PersonalObjectivesScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Lire la Bible en un an'), findsOneWidget);
      expect(find.text('Un chapitre par jour'), findsOneWidget);
      expect(find.text('3 / 10'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PersonalObjectivesScreen(apiService: _FakeApiService(empty: true))));
      await tester.pumpAndSettle();
      expect(find.text('Aucun objectif défini.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await tester.pumpWidget(MaterialApp(home: PersonalObjectivesScreen(apiService: _FakeApiService(fail: true))));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger vos objectifs.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
