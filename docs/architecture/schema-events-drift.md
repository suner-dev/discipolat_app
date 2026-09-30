# Dérive entité/schema : le module `events` pointe sur une table morte

Découvert en cherchant à implémenter le moteur de géolocalisation, en migrant
une base **de zéro** sur PostgreSQL 16 réel. Ce n'est pas une hypothèse : c'est
constaté.

## Le fait

| | Attendu par le code | Réel en base |
|---|---|---|
| Table | `events` (pluriel) | **`event`** (singulier) |
| Entité | `Event.java` | `ChurchEvent.java` |
| Statut | mapping actif | `events` a été **renommée** `legacy_events` en V158 |

V3 créait `events`. **V158 a renommé `events` → `legacy_events`** et créé la
table de remplacement `event`. `Event.java` n'a pas suivi : il mappe toujours
`events`, une table qui **n'existe plus**.

Conséquence : **tous** les endpoints `EventController` — donc tout le module
`events` du client mobile, y compris la réécriture des routes réelles —
échoueraient en production avec `relation "events" does not exist`.

## Pourquoi les tests ne l'ont jamais vu

`src/test/resources/application-test.yml` :

```yaml
ddl-auto: create-drop
flyway:
  enabled: false
```

Les tests ne jouent **jamais** les migrations : Hibernate recrée le schéma à
partir des annotations. La table `events` est donc recréée par l'annotation
`@Table(name = "events")`, et les tests de vérification de schéma valident un
schéma **qui n'existe pas en production**. 1 498 tests verts, et un module
entier mort au déploiement.

C'est le même angle mort que les identifiants `int` côté mobile : une
convention de test qui masque une divergence de contrat.

## L'écart de colonnes est total

`events` (attendu) → `event` (réel) :

```
organisateur_id, famille_id, department_id, resource_scope,
organization_unit_id, type_evenement, titre, date_debut, date_fin,
limite_places, nb_inscrits, statut, compte_rendu, deleted
        vs
title, type, status, start_at, end_at, timezone, is_recurring,
recurrence_rule, visibility, organizer_id, created_by, deleted_at,
search_vector
```

Seuls `tenant_id`, `description`, `lieu` et `created_at` sont communs. Ce n'est
pas un problème de nommage à corriger : ce sont **deux modèles métier
différents**, l'un familial (`famille_id`, `limite_places`, `compte_rendu`),
l'autre paroissial et multi-tenant (`visibility`, `organizer_id`, recherche
plein texte). V158 a délibérément basculé du premier vers le second et a
déplacé les données.

## Ce que j'ai fait dans l'immédiat

Les migrations V200 et V201 sont rendues sûres (`ALTER TABLE IF EXISTS`, blocs
`DO` pour index et `COMMENT ON`). **Sans cela, V200 — déjà poussé sur la
branche — faisait échouer le déploiement.** Un `NOTICE` signale la dérive dans
les logs au lieu de la laisser passer en silence.

Vérifié : les 157 migrations s'appliquent sans erreur sur une base neuve, et
l'avertissement `V201: table 'events' absente` apparaît bien dans les logs.

## Le vrai correctif, et pourquoi il vous revient

Il n'y a pas de petite correction. Les options sont :

1. **Aligner le module sur la table vivante `event`** (recommandé) — un seul
   modèle, la recherche plein texte et le multi-tenant sont conservés. Il faut
   arbitrage produit sur `limite_places`, `compte_rendu`, `famille_id` : ces
   colonnes n'ont pas d'équivalent et le modèle vivant est paroissial, pas
   familial. Le contrat API change (`titre`→`title`, `date_debut`→`start_at`,
   `statut`→`status`), donc le modèle mobile est à recâbler une seconde fois.
2. **Ressusciter `events`** — contredit la bascule de V158 et duplique les
   données que V158 a déjà déplacées.
3. **Ajouter les colonnes manquantes à `event`** et recâbler — le compromis le
   plus proche du contrat actuel, au prix de deux modèles d'événements qui
   coexistent.

Tant que ce point n'est pas arbitré, le module `events` ne peut pas être
qualifié « prêt pour la production », quel que soit le nombre de tests verts.

## Ce qui est DÉJÀ aligné (vérifié sur base neuve)

`event_registrations` (inscriptions, présences, preuve de pointage) est
**correcte** : sa clé étrangère `event_registrations_event_id_fkey` référence
`event`, la table vivante. Donc le moteur de géolocalisation et le pointage sont
persistés sur le bon modèle ; seule la lecture/écriture de l'événement est
cassée.

Et le modèle vivant n'est pas un squelette : `ChurchEvent` +
`ChurchEventService` (package `service/`) + `ChurchEventRepository` +
`ChurchEventController` (23 endpoints sur `/api/v1/church-events`, dont
calendar, spaces, teams, tasks) sont câblés et fonctionnels. Cinq autres
services le consomment.

L'enflure est donc plus étroite qu'il n'y paraît :

| cassé | sain |
|---|---|
| `Event.java` → `events` (morte) | `ChurchEvent` → `event` (vivante, câblée) |
| `EventService` + `EventController` (`/api/v1/events`) | `event_registrations` → `event` (FK correcte) |

## Le plan retenu (arbitrage : aligner sur `event`)

Deux leviers réduisent le chantier sans le contourner :

1. **Aligner l'entité, pas le contrat d'API.** `EventResponse` garde ses noms
   français (`titre`, `dateDebut`, `statut`, `familleId`…). Le client mobile
   vient d'être recâblé sur ce DTO ; le renommer le DTO serait du travail sans
   valeur. Le DTO est le contrat public, l'entité est interne : les deux ont
   le droit de ne pas se ressembler.
2. **Une seule entité sur `event`.** `Event` devient l'unique entité de la
   table vivante ; `ChurchEvent`, `ChurchEventRepository` et
   `ChurchEventService` sont recâblés dessus puis retirés, pour ne pas laisser
   deux entités sur la même table.

   ⚠ **Arbitrage du 2026-09-30 : ce retrait n'a PAS eu lieu.** Instruction
   explicite de l'orchestrateur : *« ne supprime rien, améliore juste ce qui
   existe déjà »*. `ChurchEvent`, `ChurchEventRepository`, `ChurchEventService`
   et les 23 endpoints `/api/v1/church-events` (dont 3 consommés par le mobile)
   sont donc **conservés**. Il reste **deux entités sur une table** : dette
   connue et assumée, dont le premier effet mesurable a été
   `is_recurring NOT NULL` sans défaut dans le DDL Hibernate de test, qui
   faisait échouer en 500 toute écriture par `Event` — corrigé en alignant
   l'annotation sur le schéma réel. La refonte qui n'en garderait qu'une reste
   un chantier distinct.

Colonnes à ajouter à `event` (V202) — l'arbitrage produit, tracé :

| colonnes | décision | motif |
|---|---|---|
| `image_url`, `tags`, `requires_registration`, `has_checkin`, `stream_id` | **ajouter** | contrat déjà exposé par les clients |
| ~~`is_public`~~ | **ne pas ajouter** | `visibility` existe déjà, est contrainte, et fait autorité. Un booléen parallèle rendrait la question « cet événement est-il public ? » ambiguë. Le DTO public `isPublic` est une **lecture** de `visibility` |
| `latitude`, `longitude`, `geofence_radius_m` | **ajouter** | moteur de géolocalisation |
| `limite_places`, `compte_rendu` | **ajouter** | fonctionnalité existante côté client ; les supprimer créerait des contrôles fantômes, exactement ce que ce chantier combat |
| `famille_id`, `department_id`, `organization_unit_id`, `resource_scope` | **ajouter** — *arbitrage du 2026-09-30, contre la décision ci-dessus* | le périmètre **est** un besoin produit, et la raison donnée pour le retirer était fausse : `WorkspaceScopeService` scope par **rôle actif** (`FAISEUR`, `CHEF_DE_FAMILLE`, `RESPONSABLE`) via `canAccessFamily` / `canAccessDepartment`, dont dépendent `EventService.canAccessEvent` et `canManageEvent`. Le filtre de tenant ne remplace pas ce scope — il est lui-même porté par `tenant_id`. Sans ces colonnes, **tout membre d'un tenant verrait tout événement du tenant** : régression d'autorisation *intra*-tenant, invisible pour un test qui ne vérifie que l'isolation *entre* tenants |
| `nb_inscrits` | **calculé** | c'est un compteur, pas une colonne : à compter sur `event_registrations` |
| `statut` | **mappé sur `status`**, CHECK **élargi à l'union des deux lexiques** — *arbitrage du 2026-09-30* | l'entité écrit toujours le vocabulaire du produit (dictionnaires `EVENT_TYPE` / `EVENT_STATUS` de V42, déjà porté par V62 sur la table morte). Le lexique Church OS de V158 **survit** : remplacer serait *perteux* — `REUNION` et `VISITE` convergent tous deux vers `MEETING`, donc l'aller-retour n'est pas réversible — et `POST /api/v1/church-events` accepte encore un corps libre. On ne prétend donc pas à un lexique unique tant que la conversion n'est pas lossy et subie par tous les écrivains |
| `deleted` | **`deleted_at`** | la table vivante est en suppression logique horodatée |
