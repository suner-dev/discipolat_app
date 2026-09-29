// B7 — Bannière d'onboarding (constat MO1).
//
// Bandeau informatif affiché sur le tableau de bord tant que la configuration
// du tenant n'est pas terminée (`OnboardingStatus.completed == false`). Elle :
// - interroge le statut via le service (aucune donnée en dur, gate G-B.4) ;
// - propose un lien vers l'écran d'onboarding (`/tenant/onboarding`) ;
// - est dismissible pour la session (« Ne plus afficher ») ;
// - reste non intrusive : masquée si déjà complète OU si le statut est
//   indisponible (erreur réseau) — on ne nagge jamais sur un état inconnu.
//
// La navigation est injectable (`onNavigate`) pour découpler le widget du
// routeur (go_router) et le rendre testable sans arborescence de routes.
import 'package:flutter/material.dart';

import '../../data/services/tenant_onboarding_service.dart';
import 'glass_theme.dart';

class OnboardingBanner extends StatefulWidget {
  const OnboardingBanner({
    super.key,
    this.service,
    this.route = '/tenant/onboarding',
    this.onNavigate,
  });

  /// Service de statut (injectable pour les tests).
  final TenantOnboardingService? service;

  /// Chemin de la route d'onboarding.
  final String route;

  /// Navigation personnalisée (ex. `context.push`). Par défaut on retombe sur
  /// `Navigator.pushNamed`, ce qui garde le widget utilisable hors go_router.
  final void Function(BuildContext context, String route)? onNavigate;

  /// Le rejet est partagé pour toute la session applicative.
  static bool _sessionDismissed = false;

  /// Réinitialise l'état « dismissed » (utilisé par les tests ; sans effet en
  /// production où chaque lancement repart d'un état vierge).
  @visibleForTesting
  static void debugResetSession() => _sessionDismissed = false;

  @visibleForTesting
  static bool get sessionDismissed => _sessionDismissed;

  @override
  State<OnboardingBanner> createState() => _OnboardingBannerState();
}

class _OnboardingBannerState extends State<OnboardingBanner> {
  late final TenantOnboardingService _service =
      widget.service ?? TenantOnboardingService();

  bool _loading = true;
  bool _completed = false;
  bool _unavailable = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final status = await _service.fetchStatus();
      if (!mounted) return;
      setState(() {
        _completed = status.completed;
        _unavailable = false;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      // Statut indisponible : on masque plutôt que d'afficher un faux « à faire ».
      setState(() {
        _unavailable = true;
        _loading = false;
      });
    }
  }

  void _dismiss() {
    OnboardingBanner._sessionDismissed = true;
    if (mounted) setState(() {});
  }

  void _open() {
    final onNavigate = widget.onNavigate;
    if (onNavigate != null) {
      onNavigate(context, widget.route);
    } else {
      Navigator.of(context).pushNamed(widget.route);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const SizedBox.shrink();
    if (_completed || _unavailable) return const SizedBox.shrink();
    if (OnboardingBanner._sessionDismissed) return const SizedBox.shrink();

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      child: Material(
        color: AppColors.primary.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: _open,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(14, 10, 8, 10),
            child: Row(
              children: <Widget>[
                Icon(Icons.auto_awesome, color: AppColors.primary, size: 22),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Finalisez la configuration de votre église',
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                ),
                IconButton(
                  onPressed: _dismiss,
                  icon: const Icon(Icons.close, size: 20),
                  tooltip: 'Ne plus afficher',
                ),
                const Icon(Icons.chevron_right),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
