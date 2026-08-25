import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/dev_plans/dev_plan_screen.dart';

void main() {
  group('DevelopmentPlanScreen', () {
    testWidgets('renders app bar with title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const DevelopmentPlanScreen()));
      expect(find.text('📈 Mon Plan de Développement'), findsOneWidget);
    });

    testWidgets('shows progress overview', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const DevelopmentPlanScreen()));
      expect(find.text('Progression globale'), findsOneWidget);
      expect(find.text('45% — 3 objectifs actifs sur 6'), findsOneWidget);
    });

    testWidgets('shows active objectives', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const DevelopmentPlanScreen()));
      expect(find.text('Objectifs actifs'), findsOneWidget);
      expect(find.text('Améliorer la présence aux cultes'), findsOneWidget);
      expect(find.text('Développer les compétences de leadership'), findsOneWidget);
    });

    testWidgets('shows completed objectives', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const DevelopmentPlanScreen()));
      expect(find.text('Objectifs terminés'), findsOneWidget);
    });

    testWidgets('shows auto-generate option', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const DevelopmentPlanScreen()));
      await tester.scrollUntilVisible(
        find.text('Générer des objectifs automatiquement'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.text('Générer des objectifs automatiquement'), findsOneWidget);
    });
  });
}
