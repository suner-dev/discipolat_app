import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/family_meeting/family_meeting_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService({this.fail = false, this.empty = false}) : super(baseUrl: 'http://fake');
  final bool fail;
  final bool empty;

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (fail) throw DioException(requestOptions: RequestOptions(path: path));
    if (path == '/family-meetings') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: empty
            ? <dynamic>[]
            : [
                {
                  'familyId': 'fam12345-abcd',
                  'status': 'SCHEDULED',
                  'agenda': 'Point mensuel et prière pour les enfants.',
                  'attendeesCount': 5,
                },
              ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('FamilyMeetingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyMeetingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('👨‍👩‍👧 Réunions famille'), findsOneWidget);
    });

    testWidgets('shows meeting from API with readable status label', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyMeetingScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Famille #fam12345'), findsOneWidget);
      expect(find.text('Point mensuel et prière pour les enfants.'), findsOneWidget);
      expect(find.text('Planifié'), findsOneWidget);
      expect(find.text('5 participants'), findsOneWidget);
    });

    testWidgets('shows empty state', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyMeetingScreen(apiService: _FakeApiService(empty: true))));
      await tester.pumpAndSettle();
      expect(find.text('Aucune réunion programmée.'), findsOneWidget);
    });

    testWidgets('shows error state with retry', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FamilyMeetingScreen(apiService: _FakeApiService(fail: true))));
      await tester.pumpAndSettle();
      expect(find.text('Impossible de charger les réunions.'), findsOneWidget);
      expect(find.text('Réessayer'), findsOneWidget);
    });
  });
}
