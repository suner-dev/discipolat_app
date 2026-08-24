import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/skills_matrix/skills_matrix_screen.dart';

void main() {
  group('SkillsMatrixScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillsMatrixScreen()));
      expect(find.text('🧩 Matrice de Compétences'), findsOneWidget);
    });

    testWidgets('shows overview stats', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillsMatrixScreen()));
      expect(find.text('Vue d\'ensemble'), findsOneWidget);
      expect(find.text('15'), findsOneWidget); // competences count
      expect(find.text('24'), findsOneWidget); // members evaluated
    });

    testWidgets('shows skills by department', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillsMatrixScreen()));
      expect(find.text('Animation'), findsOneWidget);
      expect(find.text('Musique'), findsOneWidget);
      expect(find.text('Accueil'), findsOneWidget);
    });

    testWidgets('shows gaps', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const SkillsMatrixScreen()));
      expect(find.text('🔍 Gaps identifiés'), findsOneWidget);
    });
  });
}
