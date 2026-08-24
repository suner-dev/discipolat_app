import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/personal_objectives/personal_objectives_screen.dart';

void main() {
  group('PersonalObjectivesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PersonalObjectivesScreen()));
      expect(find.text('🎯 Mes Objectifs Spirituels'), findsOneWidget);
    });

    testWidgets('shows overview stats', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PersonalObjectivesScreen()));
      expect(find.text('Progression'), findsOneWidget);
      expect(find.text('4'), findsOneWidget); // actifs
      expect(find.text('6'), findsOneWidget); // terminés
    });

    testWidgets('shows active objectives', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PersonalObjectivesScreen()));
      expect(find.text('Objectifs actifs'), findsOneWidget);
      expect(find.textContaining('Lire la Bible'), findsOneWidget);
    });

    testWidgets('shows achievements', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PersonalObjectivesScreen()));
      expect(find.text('🏆 Accomplissements'), findsOneWidget);
    });
  });
}
