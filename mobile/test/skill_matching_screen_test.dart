import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/skill_matching/skill_matching_screen.dart';

void main() {
  group('SkillMatchingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillMatchingScreen()));
      expect(find.text('🧩 Matching Compétences'), findsOneWidget);
    });

    testWidgets('shows AI match button', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillMatchingScreen()));
      expect(find.text('Lancer le matching IA'), findsOneWidget);
    });

    testWidgets('shows proposed matches', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillMatchingScreen()));
      expect(find.text('Jean-Pierre M.'), findsOneWidget);
      expect(find.text('Marie K.'), findsOneWidget);
      expect(find.text('92'), findsOneWidget);
    });

    testWidgets('shows declared skills', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillMatchingScreen()));
      expect(find.text('Mes compétences déclarées'), findsOneWidget);
    });
  });
}
