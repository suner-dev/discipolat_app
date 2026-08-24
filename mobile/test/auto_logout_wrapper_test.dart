import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:discipolat_mobile/presentation/widgets/auto_logout_wrapper.dart';

void main() {
  Widget wrap(Widget child) => MaterialApp(home: Scaffold(body: child));

  // ═══════════════════════════════════════════════════════════
  // P2 #73 — AUTO LOGOUT WRAPPER TESTS
  // ═══════════════════════════════════════════════════════════

  group('AutoLogoutWrapper', () {
    testWidgets('renders child widget', (tester) async {
      await tester.pumpWidget(wrap(
        const AutoLogoutWrapper(
          child: Text('Protected content'),
        ),
      ));
      expect(find.text('Protected content'), findsOneWidget);
    });

    testWidgets('does not show warning initially', (tester) async {
      await tester.pumpWidget(wrap(
        const AutoLogoutWrapper(
          timeout: Duration(minutes: 15),
          child: Text('Content'),
        ),
      ));
      expect(find.text('Session expire dans 2 minutes. Touchez pour continuer.'), findsNothing);
    });

    testWidgets('accepts custom timeout', (tester) async {
      await tester.pumpWidget(wrap(
        const AutoLogoutWrapper(
          timeout: Duration(minutes: 30),
          child: Text('Content'),
        ),
      ));
      expect(find.text('Content'), findsOneWidget);
    });

    testWidgets('has GestureDetector for user interaction', (tester) async {
      await tester.pumpWidget(wrap(
        const AutoLogoutWrapper(
          child: Text('Content'),
        ),
      ));
      expect(find.byType(GestureDetector), findsOneWidget);
    });

    testWidgets('has Stack for overlay', (tester) async {
      await tester.pumpWidget(wrap(
        const AutoLogoutWrapper(
          child: Text('Content'),
        ),
      ));
      expect(find.byType(Stack), findsWidgets);
    });
  });
}
