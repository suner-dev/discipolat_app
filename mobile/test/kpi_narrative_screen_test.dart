import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/kpi_narrative/kpi_narrative_screen.dart';

void main() {
  group('KpiNarrativeScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const KpiNarrativeScreen()));
      expect(find.text('📖 KPI Narratif'), findsOneWidget);
    });

    testWidgets('shows main KPI narrative', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const KpiNarrativeScreen()));
      expect(find.text('Taux de présence'), findsOneWidget);
      expect(find.textContaining('Le taux de présence a augmenté'), findsOneWidget);
    });

    testWidgets('shows drill-down categories', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const KpiNarrativeScreen()));
      expect(find.text('Explorer par catégorie'), findsOneWidget);
      expect(find.text('Présence par département'), findsOneWidget);
    });

    testWidgets('shows other narratives', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const KpiNarrativeScreen()));
      await tester.scrollUntilVisible(
        find.text('Autres récits'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.text('Autres récits'), findsOneWidget);
      expect(find.text('Conversions'), findsOneWidget);
    });
  });
}
