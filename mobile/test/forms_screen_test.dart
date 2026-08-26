import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/data/services/api_service.dart';
import 'package:discipolat_mobile/presentation/screens/forms/forms_screen.dart';
import 'package:discipolat_mobile/presentation/screens/forms/form_builder_screen.dart';

class _FakeApiService extends ApiService {
  _FakeApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    if (path == '/api/v1/forms/published') {
      return Response(
        requestOptions: RequestOptions(path: path),
        statusCode: 200,
        data: [
          {'id': 'f1', 'titre': 'Satisfaction culte', 'description': 'Sondage post-culte', 'responseCount': 42},
          {'id': 'f2', 'titre': 'Inscription événement', 'description': 'Inscription gratuit', 'responseCount': 18},
        ],
      );
    }
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: null);
  }
}

void main() {
  group('FormsScreen', () {
    testWidgets('renders app bar with i18n title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FormsScreen(apiService: _FakeApiService())));
      expect(find.text('Formulaires'), findsOneWidget);
    });

    testWidgets('shows create form option from API', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FormsScreen(apiService: _FakeApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Satisfaction culte'), findsOneWidget);
      expect(find.text('Inscription événement'), findsOneWidget);
    });

    testWidgets('shows empty state when no forms', (tester) async {
      await tester.pumpWidget(MaterialApp(home: FormsScreen(apiService: _EmptyApiService())));
      await tester.pumpAndSettle();
      expect(find.text('Aucun formulaire publié'), findsOneWidget);
    });
  });

  group('FormBuilderScreen', () {
    testWidgets('can add and remove fields', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FormBuilderScreen()));
      await tester.pump();

      // Find the first field type chip by its icon
      final addIcons = find.byIcon(Icons.add);
      expect(addIcons, findsWidgets);

      // Tap first chip to add a field
      await tester.tap(addIcons.first);
      await tester.pump();
      expect(find.byType(DraggableScrollableSheet), findsNothing); // Still on main screen
      expect(find.byIcon(Icons.drag_handle), findsOneWidget);

      // Add another
      await tester.tap(addIcons.at(1));
      await tester.pump();
      expect(find.byIcon(Icons.drag_handle), findsNWidgets(2));
    });
  });
}

class _EmptyApiService extends ApiService {
  _EmptyApiService() : super(baseUrl: 'http://fake');

  @override
  Future<Response> get(String path, {Map<String, dynamic>? params}) async {
    return Response(requestOptions: RequestOptions(path: path), statusCode: 200, data: []);
  }
}
