import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/succession/succession_screen.dart';

void main() {
  group('SuccessionScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SuccessionScreen()));
      expect(find.text('👥 Plan de Succession'), findsOneWidget);
    });

    testWidgets('shows overview counts', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SuccessionScreen()));
      expect(find.text('Prêts'), findsOneWidget);
      expect(find.text('En formation'), findsWidgets);
      expect(find.text('À identifier'), findsOneWidget);
    });

    testWidgets('shows ready candidates', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SuccessionScreen()));
      expect(find.text('Faiseurs prêts'), findsOneWidget);
      expect(find.text('Jean-Pierre M.'), findsOneWidget);
    });

    testWidgets('shows open positions', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SuccessionScreen()));
      await tester.scrollUntilVisible(find.text('Postes ouverts'), 300);
      expect(find.text('Postes ouverts'), findsOneWidget);
      expect(find.text('Responsable Louange'), findsOneWidget);
    });
  });
}
