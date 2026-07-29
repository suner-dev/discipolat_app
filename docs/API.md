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
| PATCH | `/families/{id}/chief` | Changer le chef de famille | PASTEUR, RESPONSABLE |
| DELETE | `/families/{id}` | Archiver une famille | PASTEUR, RESPONSABLE |

### Âmes (Disciples)

| Méthode | Endpoint | Description | Rôle requis |
|---|---|---|---|
| GET | `/souls` | Liste des âmes (filtrable) | Tous (scope adapté) |
| POST | `/souls` | Créer une âme | FAISEUR, CHEF |
| GET | `/souls/{id}` | Détail d'une âme | Tous (scope adapté) |
| PUT | `/souls/{id}` | Modifier une âme | FAISEUR assigné |
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
