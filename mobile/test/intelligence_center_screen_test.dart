import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/intelligence/intelligence_center_screen.dart';

void main() {
  group('IntelligenceCenterScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const IntelligenceCenterScreen()));
      expect(find.text('🏛️ Centre d\'Intelligence'), findsOneWidget);
    });

    testWidgets('shows alert card', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const IntelligenceCenterScreen()));
      expect(find.text('Alerte: 3 membres à risque'), findsOneWidget);
    });

    testWidgets('shows KPI grid', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const IntelligenceCenterScreen()));
      expect(find.text('Effectifs'), findsOneWidget);
      expect(find.text('156'), findsOneWidget);
      expect(find.text('Présence'), findsOneWidget);
    });

    testWidgets('shows warnings', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const IntelligenceCenterScreen()));
      await tester.scrollUntilVisible(
        find.text('⚠️ Signes avant-coureurs'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.text('⚠️ Signes avant-coureurs'), findsOneWidget);
    });

    testWidgets('shows quick actions', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const IntelligenceCenterScreen()));
      await tester.scrollUntilVisible(
        find.text('🚀 Actions rapides'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.text('🚀 Actions rapides'), findsOneWidget);
    });
  });
}
