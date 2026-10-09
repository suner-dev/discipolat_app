# migration_map — EVENTS (G4.6)

Sources `legacy_events` (défaut) ou `events` → cible `event`.
La bascule physique a déjà été opérée par la migration **Flyway V158** ; ce module assure la
**traçabilité ligne à ligne** et la détection des événements manquants dans la cible.

Moteur : `LegacyMigrationService#migrateEvents`
Toggle : `tenant_settings.legacy_migration_enabled` (§G1.2).

## Comportement

| Cas                                             | Statut d'audit | Action                                        |
|-------------------------------------------------|----------------|-----------------------------------------------|
| `id` présent dans `event` (même tenant)         | `MERGED`       | Rien à écrire, tracé comme déjà migré (V158).  |
| `id` absent de la cible                          | `CONFLICT`     | Signalé pour rejeu manuel — aucune perte.      |
| Déjà tracé (`MIGRATED`/`MERGED`)                 | `SKIPPED`      | Idempotent au rejeu.                           |

## Champs non mappables

- Colonnes propres au schéma legacy des événements non reprises par le modèle `event`
  cible : listées dans `unmappableFields`, jamais supprimées de la source.

## Garanties

- **Scope strict** : lecture `WHERE tenant_id = ?`.
- **Non destructeur** : le moteur lit la source et vérifie la cible ; il n'efface rien.
- **Rollback** : le module EVENTS n'ayant aucune écriture cible propre (déléguée à V158),
  l'annulation est un non-événement ; la traçabilité est conservée dans `migration_audit`.
