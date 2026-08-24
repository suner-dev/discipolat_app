import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/reverse_mentoring/reverse_mentoring_screen.dart';

void main() {
  group('ReverseMentoringScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ReverseMentoringScreen()));
      expect(find.text('🔄 Mentorat Inversé'), findsOneWidget);
    });

    testWidgets('shows explanation card', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ReverseMentoringScreen()));
      expect(find.textContaining('demander de l\'aide'), findsOneWidget);
    });

    testWidgets('shows active requests', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ReverseMentoringScreen()));
      expect(find.text('Demandes actives'), findsOneWidget);
      expect(find.text('Cas difficile: Famille en crise'), findsOneWidget);
    });

    testWidgets('shows available mentors', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ReverseMentoringScreen()));
      expect(find.text('Mentors disponibles'), findsOneWidget);
      expect(find.text('Pasteur Samuel'), findsOneWidget);
    });

    testWidgets('FAB shows help button', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const ReverseMentoringScreen()));
      expect(find.text('Demander de l\'aide'), findsOneWidget);
    });
  });
}
