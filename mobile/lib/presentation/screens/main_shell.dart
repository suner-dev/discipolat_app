import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../data/services/session_timeout_service.dart';
import '../widgets/bottom_nav_bar.dart';

/// Shell d'onglets racine (ADMIN / PASTEUR), construit par la
/// [StatefulShellRoute.indexedStack] du routeur.
///
/// Il est le **seul propriétaire du pied de page** : la barre d'onglets
/// ([BottomNavBar] sur mobile, [NavigationRail] sur tablette ≥ 900dp) vit ici,
/// une seule fois, et pilote l'index de branche via [StatefulNavigationShell].
/// Les écrans de chaque branche ne fournissent que leur header ; les écrans
/// « poussés » (détail, création…) ouvrent sur le navigateur racine, par‑dessus
/// le shell, donc **sans** barre d'onglets — comportement conforme aux
/// applications mobiles de référence.
///
/// Chaque branche conserve sa propre pile de navigation et son état
/// (sélection d'onglet, scroll) lors des allers-retours, ce que l'ancien
/// [MainScaffold] à [IndexedStack] + `setState` ne garantissait pas.
class MainShell extends StatelessWidget {
  final StatefulNavigationShell navigationShell;

  const MainShell({super.key, required this.navigationShell});

  void _goBranch(int index) {
    SessionTimeoutService.instance.resetTimer();
    navigationShell.goBranch(
      index,
      // Re-taper l'onglet courant ramène sa racine (comportement standard).
      initialLocation: index == navigationShell.currentIndex,
    );
  }

  @override
  Widget build(BuildContext context) {
    // Réinitialise l'expiration de session sur toute interaction.
    return GestureDetector(
      onTap: () => SessionTimeoutService.instance.resetTimer(),
      onPanDown: (_) => SessionTimeoutService.instance.resetTimer(),
      child: Scaffold(
        // P3 #99 — Breakpoint tablette : NavigationRail sur écrans larges
        // (≥ 900dp), bottom navigation bar sur mobile.
        body: LayoutBuilder(
          builder: (context, constraints) {
            final isTablet = constraints.maxWidth >= 900;
            if (isTablet) {
              return Row(
                children: [
                  NavigationRail(
                    selectedIndex: navigationShell.currentIndex,
                    onDestinationSelected: _goBranch,
                    labelType: NavigationRailLabelType.all,
                    minExtendedWidth: 180,
                    destinations: const [
                      NavigationRailDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: Text('Accueil')),
                      NavigationRailDestination(icon: Icon(Icons.inbox_outlined), selectedIcon: Icon(Icons.inbox), label: Text('Demandes')),
                      NavigationRailDestination(icon: Icon(Icons.volunteer_activism_outlined), selectedIcon: Icon(Icons.volunteer_activism), label: Text('Prières')),
                      NavigationRailDestination(icon: Icon(Icons.chat_bubble_outline), selectedIcon: Icon(Icons.chat_bubble), label: Text('Messages')),
                      NavigationRailDestination(icon: Icon(Icons.apps), label: Text('Plus')),
                    ],
                  ),
                  Expanded(child: navigationShell),
                ],
              );
            }
            return navigationShell;
          },
        ),
        bottomNavigationBar: LayoutBuilder(
          builder: (context, constraints) {
            // Masque la barre inférieure sur tablette (le rail la remplace).
            if (constraints.maxWidth >= 900) return const SizedBox.shrink();
            return BottomNavBar(
              currentIndex: navigationShell.currentIndex,
              onTap: _goBranch,
            );
          },
        ),
      ),
    );
  }
}
