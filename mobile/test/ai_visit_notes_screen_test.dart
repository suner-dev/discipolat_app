import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/ai_visit_notes/ai_visit_notes_screen.dart';

void main() {
  group('AiVisitNotesScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiVisitNotesScreen()));
      expect(find.text('🎙️ Notes IA Visites'), findsOneWidget);
    });

    testWidgets('shows last visit card', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiVisitNotesScreen()));
      expect(find.text('Dernière visite: Marie Dupont'), findsOneWidget);
      expect(find.textContaining('Transcription'), findsOneWidget);
    });

    testWidgets('shows recent notes', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiVisitNotesScreen()));
      expect(find.text('Notes récentes'), findsOneWidget);
      expect(find.text('Jean M.'), findsOneWidget);
      expect(find.text('Famille K.'), findsOneWidget);
    });

    testWidgets('FAB shows new note', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const AiVisitNotesScreen()));
      expect(find.text('Nouvelle note'), findsOneWidget);
    });
  });
}
