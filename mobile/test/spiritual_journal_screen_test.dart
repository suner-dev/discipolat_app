import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/spiritual_journal/spiritual_journal_screen.dart';

void main() {
  group('SpiritualJournalScreen', () {
    testWidgets('renders app bar with title', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualJournalScreen()));
      expect(find.text('📓 Journal Spirituel'), findsOneWidget);
    });

    testWidgets('shows streak banner', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualJournalScreen()));
      expect(find.text('🔥 Série de 5 jours consécutifs !'), findsOneWidget);
    });

    testWidgets('shows filter chips', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualJournalScreen()));
      expect(find.text('Tous'), findsOneWidget);
      expect(find.text('Prière'), findsWidgets);
      expect(find.text('Réflexion'), findsOneWidget);
      expect(find.text('Remerciement'), findsOneWidget);
    });

    testWidgets('shows journal entries', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualJournalScreen()));
      expect(find.text('Matin de prière'), findsOneWidget);
      expect(find.text('Réflexion sur la Parole'), findsOneWidget);
      expect(find.text('Remerciement — Guérison maman'), findsOneWidget);
    });

    testWidgets('FAB opens new entry sheet', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualJournalScreen()));
      await tester.tap(find.byIcon(Icons.add));
      await tester.pumpAndSettle();
      expect(find.text('Nouvelle entrée'), findsOneWidget);
    });
  });
}
