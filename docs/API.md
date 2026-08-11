# API REST

## Base URL

```bash
http://localhost:8080/api/v1
```

## Authentification

Tous les endpoints (sauf `/auth/login` et `/auth/refresh`) nécessitent un token JWT dans le header `Authorization: Bearer <token>`.

## Endpoints

### Authentification

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| POST | `/auth/login` | Authentification | Public |
| POST | `/auth/refresh` | Rafraîchir le token | Authentifié |

### Utilisateurs

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/users` | Liste des utilisateurs | PASTEUR, RESPONSABLE |
| POST | `/users` | Créer un utilisateur | PASTEUR, RESPONSABLE |
| GET | `/users/{id}` | Détail d'un utilisateur | Authentifié |
| PUT | `/users/{id}` | Modifier un utilisateur | PASTEUR |
| PATCH | `/users/{id}/transfer` | Transférer un faiseur vers une famille (**workflow** — retourne la demande de transfert) | ADMIN, PASTEUR |

### Départements

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/departments` | Liste des départements | Tous |
| POST | `/departments` | Créer un département | PASTEUR |
| GET | `/departments/{id}` | Détail d'un département | Tous |
| PUT | `/departments/{id}` | Modifier un département | PASTEUR |
| DELETE | `/departments/{id}` | Archiver un département | PASTEUR |

### Familles de disciples

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/families` | Liste des familles | Tous |
| POST | `/families` | Créer une famille | PASTEUR, RESPONSABLE |
| GET | `/families/{id}` | Détail d'une famille | Tous |
| PUT | `/families/{id}` | Modifier une famille | PASTEUR, RESPONSABLE |
| PATCH | `/families/{id}/chief` | Changer le chef de famille (**workflow** — retourne la demande de transfert) | ADMIN, PASTEUR, CHEF_DE_FAMILLE |
| DELETE | `/families/{id}` | Archiver une famille | PASTEUR, RESPONSABLE |

### Âmes (Disciples)

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/souls` | Liste des âmes (filtrable) | Tous (scope adapté) |
| POST | `/souls` | Créer une âme | FAISEUR, CHEF |
| GET | `/souls/{id}` | Détail d'une âme | Tous (scope adapté) |
| PUT | `/souls/{id}` | Modifier une âme | FAISEUR assigné |
| PATCH | `/souls/{id}/reassign` | Réaffecter l'âme à un autre faiseur (**workflow** — retourne la demande de transfert) | ADMIN, PASTEUR, RESPONSABLE |
| DELETE | `/souls/{id}` | Archiver une âme | FAISEUR assigné |
| GET | `/souls/{id}/history` | Historique d'une âme | Tous (scope adapté) |

### Rapports

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| POST | `/reports/maker-weekly` | Soumettre rapport faiseur | FAISEUR |
| GET | `/reports/maker-weekly` | Consulter rapports faiseurs | CHEF, RESPONSABLE, PASTEUR |
| POST | `/reports/family-weekly` | Soumettre rapport famille | CHEF |
| GET | `/reports/family-weekly/{familyId}` | Rapport famille consolidé | RESPONSABLE, PASTEUR |

### Suivis parallèles

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| POST | `/parallel-followups` | Déclarer un suivi parallèle | FAISEUR, CHEF |
| GET | `/parallel-followups` | Liste des suivis parallèles | CHEF, RESPONSABLE, PASTEUR |
| PATCH | `/parallel-followups/{id}` | Modifier un suivi | Initiateur |
| DELETE | `/parallel-followups/{id}` | Supprimer un suivi | Initiateur |

### Alertes

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/alerts` | Liste des alertes | CHEF, RESPONSABLE, PASTEUR |
| PATCH | `/alerts/{id}/resolve` | Résoudre une alerte | CHEF, RESPONSABLE |

### Notifications

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/notifications` | Mes notifications | Authentifié |
| PATCH | `/notifications/{id}/read` | Marquer comme lu | Authentifié |
| PATCH | `/notifications/read-all` | Tout marquer comme lu | Authentifié |

### Dashboard

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/dashboard/kpi` | KPI consolidés | PASTEUR |
| GET | `/dashboard/kpi/department/{id}` | KPI par département | PASTEUR, RESPONSABLE |
| GET | `/dashboard/kpi/family/{id}` | KPI par famille | PASTEUR, RESPONSABLE, CHEF |

### Workflow de transfert

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/transfers` | Liste des demandes (filtrée par statut/type, scopée par rôle actif) | Authentifié |
| GET | `/transfers/configurations` | Types de transfert que je peux initier | Authentifié |
| GET | `/transfers/{id}` | Détail d'une demande (circuit, décisions, pièces jointes) | Demandeur, concerné, validateur |
| POST | `/transfers` | Créer une demande | Rôle initiateur (config) |
| PUT | `/transfers/{id}` | Modifier un brouillon | Demandeur |
| POST | `/transfers/{id}/submit` | Soumettre (exécution immédiate si circuit vide) | Demandeur |
| POST | `/transfers/{id}/decide` | Décision motivée (approbation, refus, infos, correction) | Validateur (rôle actif) |
| POST | `/transfers/{id}/cancel` | Annuler | Demandeur |
| POST | `/transfers/{id}/archive` | Archiver | ADMIN, PASTEUR |
| GET | `/transfers/{id}/history` | Historique immuable | Visibilité scopée |
| GET | `/transfers/{id}/decisions` | Décisions | Visibilité scopée |
| GET/PUT/POST/DELETE | `/admin/transfers/workflows` | Paramétrage du workflow (configs + étapes) | ADMIN, PASTEUR |

### Bêta-testing & retours testeurs (V50)

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/public/meta` | Méta-données publiques (nom, version, environnement, betaMode, demoAccountsEnabled) — **aucun token** | Public |
| POST | `/feedback` | Soumettre un retour (catégorie, priorité, sujet, description, page, navigateur, OS, appareil) | Authentifié |
| GET | `/admin/feedback` | Liste des retours (les plus récents d'abord, email émetteur résolu) | ADMIN, PASTEUR |
| GET | `/admin/feedback/stats` | Statistiques (total, par statut, par catégorie) | ADMIN, PASTEUR |
| PATCH | `/admin/feedback/{id}/status` | Changer le statut (`NOUVEAU`/`EN_COURS`/`RESOLU`/`REJETE`) | ADMIN |
| GET | `/admin/beta/status` | État de l'environnement (environment, resetEnabled) | ADMIN |
| POST | `/admin/beta/reset` | Réinitialiser l'environnement bêta (tronque les données testeurs, restaure le seed démo, recrée les comptes) — **refusé en prod et si désactivé** | ADMIN (profil beta) |

> 💡 Le reset bêta est protégé par une **double garde** : refus si
> `app.environment=prod` ET refus si `app.beta-testing.reset-enabled` n'est pas
> actif (seul le profil Spring `beta` l'active). Jamais accessible en production.

## Pagination

Tous les endpoints GET qui retournent des listes supportent la pagination :

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

Paramètres : `?page=0&size=20&sort=nom,asc`

## Codes d'erreur

| Code | Description |
|---|---|
| 400 | Bad Request (validation) |
| 401 | Non authentifié |
| 403 | Accès refusé (RBAC) |
| 404 | Ressource non trouvée |
| 409 | Conflit métier |
| 429 | Rate limit dépassé |
| 500 | Erreur interne |
