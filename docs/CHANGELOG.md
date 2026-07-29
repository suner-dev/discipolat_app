# Changelog

## [1.0.0] - 2026-07-15

### Backend
- Authentification JWT RS256 avec refresh token
- RBAC complet (Pasteur, Responsable, Chef de famille, Faiseur)
- CRUD des départements, familles de disciples, âmes
- Reporting hebdomadaire à deux niveaux (faiseur + famille)
- Suivis parallèles
- Alertes automatiques 48h et rappels de rapport
- Dashboard décisionnel avec KPI
- Notifications in-app et email
- Journal d'audit
- Jobs planifiés (vérification absence, rappels)
- Architecture Spring Modulith avec modules indépendants

### Frontend
- Application React 19 avec TypeScript
- Tableau de bord avec graphiques Recharts
- Gestion complète des âmes, familles, départements
- Saisie de rapport hebdomadaire par âme
- Rapport de famille consolidé
- Gestion des suivis parallèles
- Alertes et notifications
- Mode sombre/clair
- Design responsive
- Protection des routes par RBAC

### Infrastructure
- Docker Compose (API + Frontend + DB + Nginx)
- Nginx reverse proxy
- Pipeline CI/CD GitHub Actions
- Script de génération de clés JWT
- Configuration multi-environnements (dev, docker, prod)

### Mobile (Structure)
- Structure Flutter prête pour le développement
- Architecture clean avec séparation des couches
- Modèles de données synchronisés avec le backend

### Documentation
- README complet avec guide de démarrage
- ARCHITECTURE.md détaillée
- API.md avec tous les endpoints
- DATABASE.md avec schéma
- DEPLOYMENT.md avec procédure
- SECURITY.md avec politique de sécurité
- DECISIONS.md avec arbitrages
- CHANGELOG.md
