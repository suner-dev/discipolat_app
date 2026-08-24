import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/church_comparison/church_comparison_screen.dart';

void main() {
  group('ChurchComparisonScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ChurchComparisonScreen()));
      expect(find.text('⚖️ Comparaison d\'Églises'), findsOneWidget);
    });

    testWidgets('shows benchmark bars', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ChurchComparisonScreen()));
      expect(find.text('Présence'), findsOneWidget);
      expect(find.text('Rétention'), findsOneWidget);
      expect(find.text('Conversion'), findsOneWidget);
    });

    testWidgets('shows church list', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ChurchComparisonScreen()));
      expect(find.text('Église Espoir'), findsOneWidget);
      expect(find.text('Église Paix'), findsOneWidget);
      expect(find.text('Église Grâce'), findsOneWidget);
    });
  });
}
