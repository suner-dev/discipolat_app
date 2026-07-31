# Discipolat

**Application de Gestion du Discipolat** pour les églises.

Discipolat est une solution complète de gestion et de suivi du discipolat en église. Elle permet de structurer l'accompagnement spirituel des membres autour de **familles de disciples**, avec un reporting hebdomadaire à deux niveaux et un tableau de bord décisionnel pour le pasteur.

## 🚀 Démarrage rapide

```bash
# 1. Générer les clés JWT
chmod +x setup-keys.sh && ./setup-keys.sh

# 2. Lancer l'application
docker compose up -d

# 3. Accéder à l'application
# Frontend: http://localhost:3000
# API:      http://localhost:8081
# Swagger:  http://localhost:8081/swagger-ui.html (rôle ADMIN/PASTEUR requis)
```

> **Comptes de démonstration** (mot de passe : `password123`)
> - Admin (ADMIN + PASTEUR) : `admin@discipolat.com`
> - Pasteur : `pasteur@discipolat.com`
> - Responsable (RESPONSABLE + FAISEUR) : `responsable@discipolat.com`
> - Chef de famille (FAISEUR + CHEF_DE_FAMILLE) : `chef@discipolat.com`
> - Faiseur : `faiseur@discipolat.com`
> - Membre : `membre@discipolat.com`

En développement, le frontend Vite tourne sur `http://localhost:5173` (proxy `/api` → `localhost:8080`).

## 🏗️ Architecture

```
discipolat/
├── backend/          # API Spring Boot (Java 23)
├── frontend/         # Application React/TypeScript
├── mobile/           # Application Flutter/Dart
├── infra/            # Configuration infrastructure
│   └── nginx/        # Reverse proxy Nginx
├── keys/             # Clés JWT (générées)
├── docs/             # Documentation
├── .github/          # CI/CD GitHub Actions
└── docker-compose.yml
```

## 📚 Documentation

| Document | Description |
|---|---|
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Architecture technique détaillée |
| [API.md](docs/API.md) | Documentation des endpoints REST |
| [DATABASE.md](docs/DATABASE.md) | Modèle de données et migrations |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | Procédure de déploiement |
| [SECURITY.md](docs/SECURITY.md) | Politiques et configuration de sécurité |
| [DECISIONS.md](docs/DECISIONS.md) | Décisions architecturales |
| [CHANGELOG.md](docs/CHANGELOG.md) | Historique des versions |

## 📋 Fonctionnalités

### Gestion des rôles (RBAC)
- **Pasteur** : Vision globale, tableau de bord, validation
- **Responsable de département** : Gestion des familles, validation des rapports
- **Chef de famille** : Supervision des faiseurs, rapport de famille consolidé
- **Faiseur de disciples** : Suivi des âmes, rapport hebdomadaire

### Familles de disciples
- Organisation hiérarchique : Département → Famille → Faiseur → Âme
- Cumul Chef de famille / Faiseur (un faiseur peut aussi être chef)

### Reporting hebdomadaire
- Rapport du faiseur par âme (présence, difficultés, sorties)
- Rapport de famille consolidé (statistiques agrégées)
- Validation à chaque niveau (faiseur → chef → responsable → pasteur)

### Suivis parallèles
- Traçage des accompagnements hors périmètre formel
- Visibilité complète pour le pasteur

### Alertes automatiques
- Alerte 48h après absence détectée sans suivi
- Rappel de soumission de rapport
- Escalade automatique (faiseur → chef → responsable)

### Dashboard décisionnel
- KPI de santé spirituelle (présence, mouvements, risques)
- Filtres par période, département, famille, faiseur
- Drill-down jusqu'à la fiche individuelle de l'âme

## 🔧 Stack technique

| Composant | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3, Spring Modulith |
| Frontend | React 19, TypeScript, Vite, TailwindCSS |
| Mobile | Flutter 3, Dart |
| Base de données | PostgreSQL 16 |
| Cache / Rate limiting | Redis 7 + Bucket4j |
| Authentification | JWT RS256, Spring Security |
| ORM | Spring Data JPA, Flyway |
| API | REST, OpenAPI 3, Swagger |
| Conteneurisation | Docker, Docker Compose |
| CI/CD | GitHub Actions (Backend, Frontend, Docker, Render) |
| Hébergement | Render (PostgreSQL, Redis, API, Web) |

## 🧪 Tests

```bash
# Backend (tests unitaires + intégration Redis ; nécessite un Redis local ou REDIS_URL)
cd backend && mvn verify -B

# Frontend
cd frontend && npm test && npm run build

# Mobile
cd mobile && flutter analyze && flutter test
```

## 📄 Licence

Projet privé - Tous droits réservés.
