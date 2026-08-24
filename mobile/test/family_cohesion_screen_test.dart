import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/family_cohesion/family_cohesion_screen.dart';

void main() {
  group('FamilyCohesionScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyCohesionScreen()));
      expect(find.text('💞 Cohésion Familiale'), findsOneWidget);
    });

    testWidgets('shows cohesion score', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyCohesionScreen()));
      expect(find.text('7.5/10'), findsOneWidget);
      expect(find.text('Bon — Maintenir les efforts'), findsOneWidget);
    });

    testWidgets('shows indicator bars', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyCohesionScreen()));
      expect(find.text('Participation événements'), findsOneWidget);
      expect(find.text('Diversité âmes'), findsOneWidget);
    });

    testWidgets('shows family list', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyCohesionScreen()));
      expect(find.text('Famille Grâce'), findsOneWidget);
      expect(find.text('Famille Espoir'), findsOneWidget);
    });
  });
}
