import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({'onboarding_complete': false});
    AuthState().logout();
  });

  tearDown(() {
    AuthState().logout();
    appRouter.go('/login');
  });

  testWidgets('invitation route stays public before onboarding completion',
      (tester) async {
    appRouter.go('/login');
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: GlassTheme.darkTheme,
          routerConfig: appRouter,
        ),
      ),
    );
    await tester.pumpAndSettle();

    appRouter.go('/accept-invitation');
    await tester.pumpAndSettle();

    expect(find.text('Accepter une invitation'), findsOneWidget);
    expect(find.text('Bienvenue'), findsNothing);
    expect(find.text('Page introuvable'), findsNothing);
  });

  testWidgets('unauthenticated private routes fail closed to login',
      (tester) async {
    final preferences = await SharedPreferences.getInstance();
    await preferences.setBool('onboarding_complete', true);
    appRouter.go('/login');
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: GlassTheme.darkTheme,
          routerConfig: appRouter,
        ),
      ),
    );
    await tester.pumpAndSettle();

    appRouter.go('/dashboard');
    await tester.pumpAndSettle();

    expect(find.text('Se connecter'), findsOneWidget);
    expect(find.text('Tableau de bord'), findsNothing);
  });
}
