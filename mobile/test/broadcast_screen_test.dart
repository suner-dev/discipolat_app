import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/screens/broadcast/broadcast_screen.dart';

void main() {
  group('BroadcastScreen', () {
    testWidgets('renders app bar', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BroadcastScreen()));
      expect(find.text('📢 Diffusion / Broadcast'), findsOneWidget);
    });

    testWidgets('shows stats', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BroadcastScreen()));
      expect(find.text('Envoyées'), findsOneWidget);
      expect(find.text('12'), findsOneWidget);
      expect(find.text('Lues'), findsOneWidget);
    });

    testWidgets('shows recent broadcasts', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BroadcastScreen()));
      expect(find.text('Diffusions récentes'), findsOneWidget);
      expect(find.textContaining('Rappel: Culte spécial'), findsOneWidget);
    });

    testWidgets('shows targeting options', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BroadcastScreen()));
      expect(find.text('Ciblage'), findsOneWidget);
      expect(find.text('Tous les membres'), findsOneWidget);
    });

    testWidgets('FAB shows new broadcast button', (tester) async {
      await tester.pumpWidget(MaterialApp(home: const BroadcastScreen()));
      expect(find.text('Nouvelle diffusion'), findsOneWidget);
    });
  });
}
