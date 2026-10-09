# migration_map — PEOPLES (G4.6)

Source legacy `souls` → moteurs Church OS `person` + `membership`.

Moteur : `com.discipolat.modules.dataMigration.domain.LegacyMigrationService#migratePeoples`
Toggle : `tenant_settings.legacy_migration_enabled` (§G1.2) — bloque TOUTE écriture réelle.

## Mapping des colonnes

| Source (`souls`)        | Cible                    | Règle                                                                 |
|-------------------------|--------------------------|-----------------------------------------------------------------------|
| `id`                    | `migration_audit.source_id` | Identifiant de traçabilité (jamais réutilisé comme id cible).       |
| `tenant_id`             | `person.tenant_id`       | **Scope strict** : la requête source est filtrée `WHERE tenant_id = ?`. |
| `nom`                   | `person.last_name`       | Copie directe.                                                         |
| `prenom`                | `person.first_name`      | Repli sur `nom` si vide.                                               |
| `email`                 | `person.email_normalized`| `normalizeEmail` : trim + minuscules ; clé de dédoublonnage.           |
| `telephone`             | `person.phone_normalized`| `normalizePhone` : E.164-lite (chiffres + `+`) ; clé secondaire.       |
| `adresse`               | `person.address`         | Copie directe.                                                         |
| `date_naissance`        | `person.birth_date`      | Conversion `LocalDate` multi-format.                                   |
| `statut`                | `membership.membership_status` | `mapSoulStatut` : EN_INTEGRATION→NOUVEAU_CONVERTI, etc.          |
| `date_integration`      | `membership.joined_at`   | Repli `LocalDate.now()` si absent.                                     |

## Champs non mappables (traités ailleurs)

- `type_disciple`, `niveau_croissance`, `etat_spirituel`, `notes_pasteur`, `suivi` :
  relèvent du parcours de discipolat **faith_journey (§G3.7)**, pas de la fiche d'identité.
  Ils sont signalés dans le rapport `unmappableFields` et **ne sont jamais perdus** (la
  table source `souls` reste intacte).

## Garanties

- **Idempotence** : index unique partiel `uk_migration_audit_source` sur
  `(tenant_id, source_table, source_id) WHERE status IN ('MIGRATED','MERGED')`.
  Rejouer ne crée aucun doublon ; les lignes déjà traitées passent en `SKIPPED`.
- **Dédoublonnage à la création** : si une `person` existe déjà (email/téléphone normalisés
  scopés tenant) → `MERGED`, aucune écriture cible.
- **Conflit interne à la source** : deux `souls` de même identité dans le même lot →
  `CONFLICT` (fusion assistée §G3.1 requise), aucune écriture silencieuse.
- **Rollback** : snapshot des `person` créées ; l'annulation ne retire que ces lignes
  (+ leurs `membership`), la source n'est jamais touchée.
