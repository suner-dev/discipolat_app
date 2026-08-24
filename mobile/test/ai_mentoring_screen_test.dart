import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/mentoring/ai_mentoring_screen.dart';

void main() {
  group('AiMentoringScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiMentoringScreen()));
      expect(find.text('🎓 Mentorat IA'), findsOneWidget);
    });

    testWidgets('shows AI suggestion', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiMentoringScreen()));
      expect(find.text('Suggestion IA'), findsOneWidget);
      expect(find.text('Appliquer'), findsOneWidget);
      expect(find.text('Passer'), findsOneWidget);
    });

    testWidgets('shows chef de famille profiles', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiMentoringScreen()));
      expect(find.text('Jean-Pierre M.'), findsOneWidget);
      expect(find.text('Famille Grâce'), findsOneWidget);
    });

    testWidgets('shows recommended approaches', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiMentoringScreen()));
      expect(find.text('Approches recommandées'), findsOneWidget);
    });
  });
}
