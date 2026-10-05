/// Modèles « organisation modulable » — SPEC_ORGANISATION_MODULABLE_V3 §7.
///
/// Point d'entrée (barrel) §7.2 : les classes sont découpées en un fichier par
/// objet (niveaux A, intitulés B, affiliations C, agrégats E) et réexportées
/// ici pour que les importateurs (`organization_v3_api`, `node_detail_screen`,
/// `roles_screen`) n'aient qu'un seul chemin à connaître.
///
/// Miroir strict des contrats web (§5). Les compteurs sont en LECTURE SEULE
/// (recalcul serveur) — aucune écriture hors-ligne sur
/// `node_aggregate_snapshots` (décision T-M0).
library;

export 'organization_level.dart';
export 'role_title.dart';
export 'member_role_assignment.dart';
export 'node_aggregate.dart';
