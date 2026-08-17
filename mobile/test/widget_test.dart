import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:discipolat_mobile/main.dart';

void main() {
  testWidgets('DiscipolatApp renders the login screen', (WidgetTester tester) async {
    // Onboarding déjà complété : le router redirige directement vers le login.
    SharedPreferences.setMockInitialValues({'onboarding_complete': true});

    await tester.pumpWidget(
      const ProviderScope(
        child: DiscipolatApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Discipolat'), findsOneWidget);
    expect(find.text('Se connecter'), findsOneWidget);
  });

  testWidgets('DiscipolatApp renders the onboarding screen on first launch',
      (WidgetTester tester) async {
    // Première connexion : aucun onboarding complété.
    SharedPreferences.setMockInitialValues({});

    await tester.pumpWidget(
      const ProviderScope(
        child: DiscipolatApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Bienvenue sur Discipolat'), findsOneWidget);
    expect(find.text('Suivant'), findsOneWidget);
  });
}
