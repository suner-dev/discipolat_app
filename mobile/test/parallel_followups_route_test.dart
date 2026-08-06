import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

/// Test de régression de la route `/parallel-followups` (anciennement morte :
/// l'import et le drawer existaient, mais aucun GoRoute n'était enregistré →
/// tout lien tombait sur la page 404).
///
/// Utilise le ROUTEUR COMPLET (`appRouter`) : authentification réelle via le
/// singleton AuthState, redirect du login vers l'espace du rôle, puis drawer.
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    // FAISEUR : rôle dont le drawer affiche « Suivis parallèles ».
    AuthState().setAuthenticated(true, userData: {
      'userId': 'user-faiseur',
      'email': 'faiseur@discipolat.test',
      'roles': ['FAISEUR'],
      'activeRole': 'FAISEUR',
    });
  });

  tearDown(() {
    AuthState().logout();
  });

  testWidgets('drawer → /parallel-followups rend l’écran (pas la page 404)', (tester) async {
    await tester.pumpWidget(
      MaterialApp.router(
        theme: GlassTheme.darkTheme,
        routerConfig: appRouter,
      ),
    );
    await tester.pumpAndSettle();

    // Le redirect du login mène l'espace FAISEUR (CRM), qui possède le drawer.
    // Ouvrir le drawer via le Scaffold (plus fiable que le tap sur l'icône).
    final scaffoldState =
        tester.state<ScaffoldState>(find.byType(Scaffold).first);
    scaffoldState.openDrawer();
    await tester.pumpAndSettle();

    // Le drawer est ouvert (header de l'app).
    expect(find.text('Discipolat'), findsOneWidget);

    // L'entrée « Suivis parallèles » est dans la liste (item ~9/14) :
    // scroller le drawer jusqu'à elle, puis naviguer.
    await tester.scrollUntilVisible(
      find.text('Suivis parallèles'),
      100,
      scrollable: find
          .descendant(of: find.byType(Drawer), matching: find.byType(Scrollable))
          .first,
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Suivis parallèles'));
    await tester.pumpAndSettle();

    // L'écran se rend : AppBar + onglets « Actifs » / « Tous ».
    expect(find.text('Suivis parallèles'), findsOneWidget);
    expect(find.text('Actifs'), findsOneWidget);
    expect(find.text('Tous'), findsOneWidget);
    // Aucun fallback 404.
    expect(find.text('404'), findsNothing);
    expect(find.text('Page introuvable'), findsNothing);
  });

  testWidgets('navigation directe vers /parallel-followups (sans le drawer)', (tester) async {
    await tester.pumpWidget(
      MaterialApp.router(
        theme: GlassTheme.darkTheme,
        routerConfig: appRouter,
      ),
    );
    await tester.pumpAndSettle();

    // Le singleton appRouter conserve sa position entre les tests : quitter
    // d'abord la cible pour que la navigation ci-dessous exerce réellement le
    // redirect + le matching de route (indépendant de l'ordre d'exécution).
    appRouter.go('/souls');
    await tester.pumpAndSettle();
    expect(find.text('404'), findsNothing);

    // Forcer la navigation directe sur la route autrefois morte.
    appRouter.go('/parallel-followups');
    await tester.pumpAndSettle();

    expect(find.text('Actifs'), findsOneWidget);
    expect(find.text('Tous'), findsOneWidget);
    expect(find.text('404'), findsNothing);
    expect(find.text('Page introuvable'), findsNothing);
  });
}
