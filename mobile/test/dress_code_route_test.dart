// Audit Dress Code (§G3.4) — vérifie que l'entrée de drawer « Tenues de
// service » pointe vers une ROUTE RÉELLEMENT ENREGISTRÉE et que l'écran
// d'édition est joignable.
//
// Sans ce test, une route morte (import + drawer présents, GoRoute absent)
// mènerait l'utilisateur vers la page 404 — le défaut constaté sur
// /parallel-followups.
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:discipolat_mobile/app.dart';
import 'package:discipolat_mobile/presentation/widgets/glass_theme.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  // `AuthState().logout()` purge le tenant via SharedPreferences : sans mock,
  // le canal `shared_preferences` lève MissingPluginException et la fuite
  // async fait échouer le test après coup.
  setUpAll(() => SharedPreferences.setMockInitialValues(<String, Object>{}));

  setUp(() {
    // ADMIN : rôle dont le drawer expose « Tenues de service ».
    AuthState().setAuthenticated(true, userData: {
      'userId': 'user-admin',
      'email': 'admin@discipolat.test',
      'roles': ['ADMIN'],
      'activeRole': 'ADMIN',
    });
  });

  tearDown(() {
    AuthState().logout();
  });

  /// Ouvre le drawer de l'écran courant.
  ///
  /// Le `Scaffold` du [MainShell] (barre d'onglets) est le PREMIER trouvé
  /// par `find.byType(Scaffold)` et ne possède PAS de drawer : on cible donc
  /// explicitement le `Scaffold` qui en expose un.
  Future<void> openDrawer(WidgetTester tester) async {
    Finder? drawerScaffold;
    for (final element in find.byType(Scaffold).evaluate()) {
      final scaffold = element.widget as Scaffold;
      if (scaffold.drawer != null) {
        drawerScaffold = find.byWidget(scaffold);
        break;
      }
    }
    expect(drawerScaffold, isNotNull,
        reason: 'Aucun Scaffold porteur de drawer sur l\'écran courant');
    tester.state<ScaffoldState>(drawerScaffold!).openDrawer();
    await tester.pumpAndSettle();
  }

  /// Fait défiler le drawer jusqu'à l'entrée « Tenues de service ».
  Future<void> scrollToDressCodeEntry(WidgetTester tester) async {
    for (var i = 0; i < 12; i++) {
      if (find.text('Tenues de service').evaluate().isNotEmpty) break;
      await tester.drag(find.byType(Drawer), const Offset(0, -200));
      await tester.pumpAndSettle();
    }
  }

  /// Avance l'horloge de quelques frames SANS attendre la stabilisation
  /// complète. La liste des tenues ([DressCodeScreen]) affiche un
  /// `CircularProgressIndicator` dont l'animation est infinie tant que sa
  /// requête réseau (réelle, jamais mockée dans ce test d'intégration route)
  /// reste en vol : `pumpAndSettle` ne reviendrait jamais. Or le test ne vise
  /// que l'enregistrement de la route et le rendu de l'écran (son AppBar),
  /// acquis dès la fin de la transition — un pompage borné suffit.
  Future<void> pumpTransition(WidgetTester tester) async {
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pump(const Duration(milliseconds: 400));
  }

  testWidgets('le drawer affiche « Tenues de service » pour un ADMIN',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: GlassTheme.darkTheme,
          routerConfig: appRouter,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await openDrawer(tester);

    expect(find.text('Discipolat'), findsOneWidget);

    // Le lien doit exister dans le drawer : on scrolle jusqu'à le trouver.
    await scrollToDressCodeEntry(tester);

    expect(
      find.text('Tenues de service'),
      findsOneWidget,
      reason: 'Le drawer doit exposer « Tenues de service » pour un ADMIN',
    );
  });

  testWidgets('le drawer → « Tenues de service » rend l\'écran (pas la 404)',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: GlassTheme.darkTheme,
          routerConfig: appRouter,
        ),
      ),
    );
    await tester.pumpAndSettle();

    await openDrawer(tester);
    await scrollToDressCodeEntry(tester);

    await tester.tap(find.text('Tenues de service'));
    await pumpTransition(tester);

    // L'écran se rend : AppBar « Dress Codes ».
    expect(find.text('Dress Codes'), findsOneWidget);
    // Aucun fallback 404.
    expect(find.text('404'), findsNothing);
    expect(find.text('Page introuvable'), findsNothing);
  });

  testWidgets('navigation directe vers /dress-codes (sans le drawer)',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: GlassTheme.darkTheme,
          routerConfig: appRouter,
        ),
      ),
    );
    await pumpTransition(tester);

    // Le singleton appRouter conserve sa position entre les tests : quitter
    // d'abord la cible pour que la navigation exerce réellement le redirect
    // + le matching de route (indépendant de l'ordre d'exécution).
    appRouter.go('/souls');
    await tester.pumpAndSettle();

    appRouter.go('/dress-codes');
    await pumpTransition(tester);

    expect(find.text('Dress Codes'), findsOneWidget);
    expect(find.text('404'), findsNothing);
    expect(find.text('Page introuvable'), findsNothing);
  });

  testWidgets('la route /dress-codes/editor rend l\'écran d\'édition',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        child: MaterialApp.router(
          theme: GlassTheme.darkTheme,
          routerConfig: appRouter,
        ),
      ),
    );
    await pumpTransition(tester);

    appRouter.go('/souls');
    await tester.pumpAndSettle();

    appRouter.go('/dress-codes/editor');
    await tester.pumpAndSettle();

    expect(find.text('Créer une tenue'), findsOneWidget);
    expect(find.text('404'), findsNothing);
    expect(find.text('Page introuvable'), findsNothing);
  });
}
