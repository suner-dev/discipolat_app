# Contribuer au projet Discipolat

## Prérequis

- Java 23+ (Temurin)
- Node.js 23+
- Docker & Docker Compose
- PostgreSQL 16 (optionnel, Docker fourni)
- Flutter SDK 3.5+ (pour le mobile)

## Structure du projet

```
discipolat/
├── backend/      # API Spring Boot
├── frontend/     # Application React
├── mobile/       # Application Flutter
├── docs/         # Documentation
├── infra/        # Configuration infrastructure
└── .github/      # CI/CD
```

## Configuration initiale

```bash
# 1. Générer les clés JWT
chmod +x setup-keys.sh && ./setup-keys.sh

# 2. Lancer la base de données
docker compose up db -d

# 3. Backend
cd backend && ./mvnw compile -q

# 4. Frontend
cd frontend && npm install && npm run dev

# 5. Mobile
cd mobile && flutter pub get
```

## Workflow de développement

### Branches
- `main` : Production
- `develop` : Intégration
- `feature/*` : Nouvelles fonctionnalités
- `fix/*` : Corrections de bugs

### Commits
Convention : `type(scope): description`
- `feat(backend): add family report endpoint`
- `fix(frontend): resolve pagination bug`
- `docs(readme): update deployment guide`

### Processus
1. Créer une branche depuis `develop`
2. Implémenter et tester
3. Créer une Pull Request vers `develop`
4. Review par un membre de l'équipe
5. Merge après validation des tests CI

## Backend

### Commandes

```bash
cd backend

# Compilation
mvn compile

# Tests
mvn test

# Tests d'intégration
mvn verify

# Build sans tests
mvn package -DskipTests
```

### Ajouter un module
1. Créer le package `modules/<nom>/api/`, `modules/<nom>/domain/`
2. Ajouter l'entité, le repository, le service et le contrôleur
3. Ajouter la migration Flyway dans `resources/db/migration`
4. Enregistrer les endpoints dans API.md

## Frontend

### Commandes

```bash
cd frontend

# Développement
npm run dev

# Build
npm run build

# Tests
npm test

# Lint
npm run lint
```

### Conventions
- Composants dans `src/components/`
- Pages dans `src/pages/`
- Types dans `src/types/`
- Services API dans `src/lib/`
- Utiliser TanStack Query pour les appels API
- Utiliser React Hook Form + Zod pour les formulaires

## Mobile (Flutter)

### Commandes

```bash
cd mobile

# Obtenir les dépendances
flutter pub get

# Développement
flutter run

# Tests
flutter test

# Build Android
flutter build apk

# Build iOS
flutter build ios
```

### Conventions
- Écrans dans `presentation/screens/`
- Widgets dans `presentation/widgets/`
- Modèles dans `data/models/`
- Services dans `data/services/`
- Utiliser Riverpod pour la gestion d'état

## Tests

### Backend
- Tests unitaires : JUnit 5 + Mockito
- Tests d'intégration : Spring Boot Test + Testcontainers
- Coverage : JaCoCo (minimum 80%)

### Frontend
- Tests unitaires : Vitest + Testing Library
- Tests de composants : Vitest

### Mobile
- Tests unitaires : flutter_test
- Tests de widgets : flutter_test

## Déploiement

Voir [DEPLOYMENT.md](DEPLOYMENT.md) pour les procédures de déploiement.
