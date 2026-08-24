import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/directory/church_directory_screen.dart';

void main() {
  group('ChurchDirectoryScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ChurchDirectoryScreen()));
      expect(find.text('📒 Annuaire de l\'Église'), findsOneWidget);
    });

    testWidgets('shows search bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ChurchDirectoryScreen()));
      expect(find.byType(TextField), findsOneWidget);
    });

    testWidgets('shows members list', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ChurchDirectoryScreen()));
      expect(find.text('Jean-Pierre M.'), findsOneWidget);
      expect(find.text('Marie K.'), findsOneWidget);
      expect(find.text('David L.'), findsOneWidget);
    });

    testWidgets('shows public visibility indicator', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ChurchDirectoryScreen()));
      // Should have visibility icons
      expect(find.byIcon(Icons.visibility), findsWidgets);
    });
  });
}
