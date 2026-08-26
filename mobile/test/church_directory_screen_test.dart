import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/directory/church_directory_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/api/v1/directory/all') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: [
          {'prenom': 'Jean-Pierre', 'nom': 'M.', 'famille': 'Grâce', 'role': 'Chef de famille', 'publicProfil': true},
          {'prenom': 'Marie', 'nom': 'K.', 'famille': 'Espoir', 'role': 'Animateur', 'publicProfil': true},
          {'prenom': 'Paul', 'nom': 'T.', 'famille': 'Joie', 'role': 'Enseignant', 'publicProfil': false},
        ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}

void main() {
  group('ChurchDirectoryScreen', () {
    testWidgets('renders app bar with i18n title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ChurchDirectoryScreen(apiService: _FakeApiService())));
      expect(find.text('Annuaire de l\'Église'), findsOneWidget);
    });

    testWidgets('shows search bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ChurchDirectoryScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.byType(TextField), findsOneWidget);
    });

    testWidgets('shows members list from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ChurchDirectoryScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Jean-Pierre M.'), findsOneWidget);
      expect(find.text('Marie K.'), findsOneWidget);
      expect(find.text('Paul T.'), findsOneWidget);
    });

    testWidgets('shows public visibility indicator', (tester) async {
      await tester.pumpWidget(MaterialApp(home: ChurchDirectoryScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.byIcon(Icons.visibility), findsWidgets);
      expect(find.byIcon(Icons.visibility_off), findsOneWidget);
    });
  });
}
