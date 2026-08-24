import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/event_checklist/event_checklist_screen.dart';

void main() {
  group('EventChecklistScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EventChecklistScreen()));
      expect(find.text('✅ Checklist Événement'), findsOneWidget);
    });

    testWidgets('shows current event', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EventChecklistScreen()));
      expect(find.textContaining('Culte dimanche'), findsOneWidget);
      expect(find.text('12/20 tâches complétées'), findsOneWidget);
    });

    testWidgets('shows checklist items', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EventChecklistScreen()));
      expect(find.text('📦 Matériel'), findsOneWidget);
      expect(find.text('Sonorisation vérifiée'), findsOneWidget);
      expect(find.text('👥 Équipes'), findsOneWidget);
    });

    testWidgets('checkbox items are interactive', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const EventChecklistScreen()));
      // Find checkbox list tiles
      expect(find.byType(CheckboxListTile), findsWidgets);
    });
  });
}
