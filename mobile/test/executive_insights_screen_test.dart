import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/executive_insights/executive_insights_screen.dart';

void main() {
  group('ExecutiveInsightsScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ExecutiveInsightsScreen()));
      expect(find.text('🧠 Insights Exécutifs'), findsOneWidget);
    });

    testWidgets('shows AI insight card', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ExecutiveInsightsScreen()));
      expect(find.text('Insight IA'), findsOneWidget);
      expect(find.textContaining('présence a baissé'), findsOneWidget);
    });

    testWidgets('shows KPI cards', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ExecutiveInsightsScreen()));
      expect(find.text('KPIs Clés'), findsOneWidget);
      expect(find.text('78%'), findsOneWidget);
      expect(find.text('Présence'), findsOneWidget);
    });

    testWidgets('shows trends', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ExecutiveInsightsScreen()));
      expect(find.text('Tendances'), findsOneWidget);
      expect(find.textContaining('Croissance baptized'), findsOneWidget);
    });
  });
}
