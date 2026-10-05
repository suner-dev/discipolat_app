import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Bouton de retour des écrans de détail — LOT 2 §BK (parité web).
///
/// **Pourquoi il existe.** Sur mobile, l'`AppBar` n'affiche un bouton de retour
/// que si l'on peut faire `pop`. Or l'application navigue majoritairement avec
/// `context.go(...)`, qui *remplace* la pile : `canPop()` est donc `false` et le
/// bouton disparaît. Résultat : depuis une fiche de détail, l'utilisateur est coincé.
///
/// **La stratégie** reprend celle du web (`src/navigation/back.ts`) :
/// `pop` si l'historique existe, sinon le parent **déduit** du chemin. On ne
/// touche donc à aucune sémantique de navigation existante — `go` reste `go`.
///
/// Le parent est déduit, pas codé en dur : `onPressed` reçoit la route parente
/// calculée par l'appelant, et le repli final est la racine.
class DetailBackButton extends StatelessWidget {
  const DetailBackButton({super.key, this.parentRoute, this.fallbackRoute = '/'});

  /// Route parente, ex. `/souls` depuis `/souls/123`.
  final String? parentRoute;

  /// Dernier recours quand rien n'est déductible : ne jamais laisser un bouton
  /// qui ne fait rien.
  final String fallbackRoute;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return IconButton(
      icon: const Icon(Icons.arrow_back_rounded),
      tooltip: MaterialLocalizations.of(context).backButtonTooltip,
      onPressed: () {
        // 1) L'historique existe-t-il ? C'est le retour le plus fidèle.
        if (context.canPop()) {
          context.pop();
          return;
        }
        // 2) Parent déduit : on revient à la liste dont on vient.
        final parent = parentRoute;
        if (parent != null && parent.isNotEmpty) {
          context.go(parent);
          return;
        }
        // 3) Rien de déductible : la racine, plutôt qu'un bouton inerte.
        context.go(fallbackRoute);
      },
      color: theme.appBarTheme.foregroundColor,
    );
  }
}