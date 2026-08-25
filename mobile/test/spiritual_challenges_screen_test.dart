import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/spiritual_challenges/spiritual_challenges_screen.dart';

void main() {
  group('SpiritualChallengesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualChallengesScreen()));
      expect(find.text('⚡ Défis Spirituels'), findsOneWidget);
    });

    testWidgets('shows active challenge', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualChallengesScreen()));
      expect(find.text('Défi en cours'), findsOneWidget);
      expect(find.text('30 jours de prière'), findsOneWidget);
      expect(find.textContaining('Jour 12/30'), findsOneWidget);
    });

    testWidgets('shows available challenges', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualChallengesScreen()));
      expect(find.text('Défis disponibles'), findsOneWidget);
      expect(find.textContaining('Lecture biblique'), findsOneWidget);
    });

    testWidgets('shows completed challenges', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SpiritualChallengesScreen()));
      await tester.scrollUntilVisible(
        find.text('Défis complétés'),
        300,
        scrollable: find.byType(Scrollable).first,
      );
      expect(find.text('Défis complétés'), findsOneWidget);
      expect(find.text('Témoignage partagé'), findsOneWidget);
    });
  });
}
