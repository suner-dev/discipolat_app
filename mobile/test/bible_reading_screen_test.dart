import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/bible_reading/bible_reading_screen.dart';

void main() {
  group('BibleReadingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BibleReadingScreen()));
      expect(find.text('📖 Plan de Lecture Biblique'), findsOneWidget);
    });

    testWidgets('shows progress', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BibleReadingScreen()));
      expect(find.text('Mon progression'), findsOneWidget);
      expect(find.text('65% — Jour 236/365'), findsOneWidget);
    });

    testWidgets('shows today reading', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BibleReadingScreen()));
      expect(find.text('Lecture du jour'), findsOneWidget);
      expect(find.text('Jean 3:16-21'), findsOneWidget);
    });

    testWidgets('shows available plans', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BibleReadingScreen()));
      expect(find.text('Plans disponibles'), findsOneWidget);
      expect(find.text('Parcours 365 jours'), findsOneWidget);
    });

    testWidgets('shows family sharing', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BibleReadingScreen()));
      expect(find.text('Partagé avec ma famille'), findsOneWidget);
      expect(find.text('Jean-Pierre'), findsOneWidget);
    });
  });
}
