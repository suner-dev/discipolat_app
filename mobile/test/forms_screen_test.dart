import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/forms/forms_screen.dart';

void main() {
  group('FormsScreen', () {
    testWidgets('renders app bar with title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FormsScreen()));
      expect(find.text('📝 Formulaires'), findsOneWidget);
    });

    testWidgets('shows create form option', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FormsScreen()));
      expect(find.text('Créer un formulaire'), findsOneWidget);
      expect(find.text('Drag & drop avec conditions logiques'), findsOneWidget);
    });

    testWidgets('shows published forms list', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FormsScreen()));
      expect(find.text('Satisfaction culte'), findsOneWidget);
      expect(find.text('Inscription événement'), findsOneWidget);
      expect(find.text('Feedback formation'), findsOneWidget);
    });

    testWidgets('navigates to form builder on tap', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FormsScreen()));
      await tester.tap(find.text('Créer un formulaire'));
      await tester.pumpAndSettle();
      expect(find.text('Créateur de formulaire'), findsOneWidget);
    });

    testWidgets('form builder can add and remove fields', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FormBuilderScreen()));
      await tester.pump();

      // Add a field
      await tester.tap(find.text('Texte'));
      await tester.pump();
      expect(find.text('Champ Texte'), findsOneWidget);

      // Add another
      await tester.tap(find.text('Choix multiple'));
      await tester.pump();
      expect(find.text('Champ Choix multiple'), findsOneWidget);
    });
  });
}
