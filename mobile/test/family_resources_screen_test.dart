import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/family_resources/family_resources_screen.dart';

void main() {
  group('FamilyResourcesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyResourcesScreen()));
      expect(find.text('📚 Ressources Familiales'), findsOneWidget);
    });

    testWidgets('shows categories', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyResourcesScreen()));
      expect(find.text('Catégories'), findsOneWidget);
      expect(find.textContaining('Études bibliques'), findsOneWidget);
      expect(find.textContaining('Vidéos'), findsOneWidget);
    });

    testWidgets('shows recent resources', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyResourcesScreen()));
      expect(find.text('Ressources récentes'), findsOneWidget);
      expect(find.textContaining('Étude Jean 3:16'), findsOneWidget);
    });
  });
}
