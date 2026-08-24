import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/volunteers/volunteers_screen.dart';

void main() {
  group('VolunteersScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const VolunteersScreen()));
      expect(find.text('🙋 Bénévoles'), findsOneWidget);
    });

    testWidgets('shows stats', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const VolunteersScreen()));
      expect(find.text('Actifs'), findsOneWidget);
      expect(find.text('Disponibles'), findsOneWidget);
      expect(find.text('24'), findsOneWidget);
    });

    testWidgets('shows match button', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const VolunteersScreen()));
      expect(find.text('Matcher bénévoles → Événement'), findsOneWidget);
    });

    testWidgets('shows volunteer list', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const VolunteersScreen()));
      expect(find.text('Jean-Pierre M.'), findsOneWidget);
      expect(find.text('Marie K.'), findsOneWidget);
    });
  });
}
