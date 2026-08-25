import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/prayer_journal/prayer_journal_screen.dart';

void main() {
  group('PrayerJournalScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PrayerJournalScreen()));
      expect(find.text('🙏 Journal de Prière'), findsOneWidget);
    });

    testWidgets('shows filter chips', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PrayerJournalScreen()));
      expect(find.text('Toutes'), findsOneWidget);
      expect(find.text('En attente'), findsWidgets);
      expect(find.text('Répondues'), findsOneWidget);
    });

    testWidgets('shows prayers list', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PrayerJournalScreen()));
      expect(find.text('Guérison maman'), findsOneWidget);
      expect(find.text('Emploi David'), findsOneWidget);
    });

    testWidgets('FAB opens new prayer sheet', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const PrayerJournalScreen()));
      await tester.tap(find.byIcon(Icons.add));
      await tester.pumpAndSettle();
      expect(find.text('Nouvelle prière'), findsOneWidget);
    });
  });
}
