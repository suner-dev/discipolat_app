# Base de données

## Technologies

- **SGBD** : PostgreSQL 16
- **ORM** : Spring Data JPA (Hibernate 6)
- **Migrations** : Flyway

## Migrations

Les migrations Flyway sont versionnées et exécutées automatiquement au démarrage :

| Fichier | Description |
|---|---|
| `V1__initial_schema.sql` | Création des tables, index, contraintes |
| `V2__seed_data.sql` | Données de démonstration |

## Schéma conceptuel

### Entités principales

```
users (1) ──< departments (responsable)
users (1) ──< families (chef)
users (1) ──< souls (faiseur)
users (1) ──< maker_reports (faiseur)
users (1) ──< notifications

departments (1) ──< families
families (1) ──< souls
families (1) ──< family_reports

souls (1) ──< maker_reports
souls (1) ──< parallel_followups
souls (1) ──< alerts
souls (1) ──< soul_history
```

### Tables

#### `users`
Stocke les utilisateurs avec leurs rôles et le lien vers la famille gérée (pour les chefs de famille).

#### `departments`
Départements de l'église, rattachés à un responsable.

#### `families`
Familles de disciples, rattachées à un département et supervisées par un chef de famille.

#### `souls`
Âmes/disciples suivis avec leur type (nouvel arrivant / nouveau converti), statut, et faiseur assigné.

#### `maker_reports`
Rapports hebdomadaires des faiseurs par âme, avec présence par culte (JSONB).

#### `family_reports`
Rapports consolidés de famille avec statistiques agrégées (JSONB).

#### `parallel_followups`
Suivis parallèles (accompagnements hors périmètre formel).

#### `alerts`
Alertes d'absence 48h et de non-soumission de rapport.

#### `notifications`
Notifications push/email/in-app pour les utilisateurs.

#### `audit_logs`
Journal d'audit des actions sensibles.

#### `soul_history`
Timeline des interactions avec une âme.

#### `culte_config`
Configuration des cultes de l'église.

#### `dashboard_metrics`
Cache des KPI pré-calculés pour le tableau de bord.

## Colonnes JSONB

| Table | Colonne | Usage |
|---|---|---|
| `maker_reports` | `presences_par_culte` | Présence par culte `{"Dimanche Matin": true, "Mercredi Soir": false}` |
| `family_reports` | `stats_agregees` | Statistiques de présence agrégées |
| `family_reports` | `repartition_sorties` | Répartition des sorties par motif |
| `family_reports` | `faiseurs_sans_rapport` | Liste des IDs des faiseurs en défaut |
| `audit_logs` | `ancien_valeur`, `nouvelle_valeur` | Avant/après des modifications |
| `soul_history` | `metadata` | Données contextuelles de l'événement |
| `dashboard_metrics` | `valeur` | Valeur de la métrique calculée |

## Index clés

- Index sur les clés étrangères (relations)
- Index sur les statuts (filtrage fréquent)
- Index sur les dates de création (tri chronologique)
- Index sur `deleted` pour le soft delete
- Index sur `email` pour l'authentification rapide

## Soft Delete

Toutes les tables principales supportent le soft delete via les colonnes :
- `deleted` (BOOLEAN)
- `deleted_at` (TIMESTAMP)
- `deleted_by` (UUID)
