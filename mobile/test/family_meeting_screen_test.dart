import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/family_meeting/family_meeting_screen.dart';

void main() {
  group('FamilyMeetingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyMeetingScreen()));
      expect(find.text('🏠 Réunion de Famille'), findsOneWidget);
    });

    testWidgets('shows next meeting', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyMeetingScreen()));
      expect(find.text('Prochaine réunion'), findsOneWidget);
      expect(find.textContaining('31 août'), findsOneWidget);
    });

    testWidgets('shows AI-generated agenda', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyMeetingScreen()));
      expect(find.textContaining('Ordre du jour généré par IA'), findsOneWidget);
    });

    testWidgets('shows past meetings', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const FamilyMeetingScreen()));
      expect(find.text('Réunions précédentes'), findsOneWidget);
    });
  });
}
