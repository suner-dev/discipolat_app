# migration_map — SPACES (G4.6)

Sources legacy `departments` / `families` → moteurs `organization_nodes` + `spaces`.

Moteur : `LegacyMigrationService#migrateSpaces`
Toggle : `tenant_settings.legacy_migration_enabled` (§G1.2).

## Mapping

| Source (`departments`/`families`) | Cible                          | Règle                                                          |
|-----------------------------------|--------------------------------|---------------------------------------------------------------|
| `id`                              | `migration_audit.source_id`    | Traçabilité ligne à ligne.                                     |
| `tenant_id`                       | `spaces.tenant_id`             | Scope strict `WHERE tenant_id = ?`.                            |
| `nom`                             | `organization_nodes.name` + `spaces.name` | Copie du libellé.                                   |
| —                                 | `organization_nodes`           | Nœud créé sous la racine ; `createRootChurch` si aucune racine.|
| —                                 | `spaces.space_type`            | `DEPARTMENT` pour departments, `FAMILY` pour families.         |
| —                                 | `spaces.code`                  | `DEP-<sourceId[:8]>` / `FAM-<sourceId[:8]>` (déterministe).    |

## Champs non mappables

- `membres_ids` (liste de membres du département/famille legacy) : l'affectation se fait
  désormais via `space_membership` (§G3.2), modèle relationnel différent d'une simple liste.
  Le lien est signalé dans `unmappableFields` et doit être rejoué par l'administrateur.

## Garanties

- **Racine** : si `organization_nodes` n'a pas de racine pour le tenant, `createRootChurch`
  en crée une (acteur = créateur du job, sinon identifiant technique pour l'exécution outbox).
- **Idempotence** : un `space` existant avec le même `code` → `MERGED`, pas de doublon.
- **Rollback** : les `spaces` créées sont archivées (`deleted_at`, statut `ARCHIVED`) —
  soft-delete, jamais de purge de la source.
