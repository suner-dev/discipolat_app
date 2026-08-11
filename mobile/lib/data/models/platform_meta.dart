/// Méta-données publiques de la plateforme (endpoint `GET /api/v1/public/meta`,
/// accessible sans authentification).
///
/// Pilote côté client l'affichage du badge BÊTA, du bandeau « environnement de
/// test » et des comptes de démonstration. **Fail-closed** : toute valeur
/// absente ou tout échec réseau retombe sur un environnement neutre
/// (aucun mode bêta, aucun compte de démonstration) — jamais de données de
/// test affichées en production.
class PlatformMeta {
  final String appName;
  final String version;
  final String environment;
  final bool betaMode;
  final bool demoAccountsEnabled;

  const PlatformMeta({
    this.appName = 'Discipolat',
    this.version = '',
    this.environment = 'dev',
    this.betaMode = false,
    this.demoAccountsEnabled = false,
  });

  factory PlatformMeta.fromJson(Map<String, dynamic> json) {
    return PlatformMeta(
      appName: json['appName'] as String? ?? 'Discipolat',
      version: json['version'] as String? ?? '',
      environment: json['environment'] as String? ?? 'dev',
      betaMode: json['betaMode'] as bool? ?? false,
      demoAccountsEnabled: json['demoAccountsEnabled'] as bool? ?? false,
    );
  }
}
