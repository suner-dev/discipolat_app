import 'package:flutter/material.dart';

// ignore: implementation_imports
import '../../app.dart' show appRouter;
import '../../l10n/app_localizations.dart';

/// Routes dont l'écran est encore alimenté par des DONNÉES DE DÉMONSTRATION
/// (aucun appel API). Ces écrans doivent être branchés avant la release.
/// Référence : docs/rapports/AUDIT_VERIFICATION_REEL.md §4bis.
const Set<String> kDemoDataRoutes = <String>{};

/// Overlay global : affiche un bandeau discret « Données de démonstration »
/// en haut de l'écran lorsque la route courante est dans [kDemoDataRoutes].
///
/// Un seul point d'insertion (builder du MaterialApp) au lieu de modifier
/// chaque écran — sûr, réversible, et supprimable d'un coup quand les
/// écrans seront branchés sur les APIs réelles.
class DemoDataOverlay extends StatelessWidget {
  final Widget child;

  const DemoDataOverlay({super.key, required this.child});

  String get _currentLocation {
    try {
      return appRouter.routerDelegate.currentConfiguration.uri.path;
    } catch (_) {
      return '';
    }
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: appRouter.routerDelegate,
      builder: (context, _) {
        final bool isDemo = kDemoDataRoutes.contains(_currentLocation);
        if (!isDemo) return child;
        return Stack(
          children: [
            child,
            Positioned(
              left: 0,
              right: 0,
              top: 0,
              child: SafeArea(
                bottom: false,
                child: Material(
                  color: Colors.transparent,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    color: const Color(0xB3FF8F00), // amber 800 ~63% opacité
                    child: Row(
                      children: [
                        const Icon(Icons.info_outline, size: 14, color: Colors.white),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            AppLocalizations.of(context).demoDataBanner,
                            style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.95)),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}
