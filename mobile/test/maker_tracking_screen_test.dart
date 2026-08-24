import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/maker_tracking/maker_tracking_screen.dart';

void main() {
  group('MakerTrackingScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const MakerTrackingScreen()));
      expect(find.text('🌱 Mon Parcours de Faiseur'), findsOneWidget);
    });

    testWidgets('shows summary cards', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const MakerTrackingScreen()));
      expect(find.text('Formations'), findsOneWidget);
      expect(find.text('Compétences'), findsOneWidget);
      expect(find.text('5'), findsOneWidget); // Formations count
    });

    testWidgets('shows timeline', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const MakerTrackingScreen()));
      expect(find.text('Timeline'), findsOneWidget);
      expect(find.text('Formation accueil'), findsOneWidget);
    });

    testWidgets('shows points total', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const MakerTrackingScreen()));
      expect(find.text('1,250 points'), findsOneWidget);
    });
  });
}
