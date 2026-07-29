# Décisions Architecturales

Ce document consigne les hypothèses et arbitrages pris durant le développement, en l'absence de précision explicite dans le cahier des charges.

## Architecture

### Monolithe modulaire (Spring Modulith)
- **Contexte** : Le cahier des charges évoque une possible extraction en microservices (V2+)
- **Décision** : Utilisation de Spring Modulith pour structurer le code en modules forts, facilitant une éventuelle séparation future
- **Modules** : `authentication`, `users`, `departments`, `families`, `souls`, `reports`, `parallel-followups`, `alerts`, `notifications`, `dashboard`, `audit`

### Architecture hexagonale
- **Contexte** : Indépendance du domaine vis-à-vis de l'infrastructure
- **Décision** : Les entités JPA (@Entity) sont placées directement dans le package `domain` pour des raisons pragmatiques de productivité. Une version strictement hexagonale séparerait les entités JPA (infrastructure) des entités métier (domain)
- **Compromis accepté** : Les annotations JPA dans le `domain`; simplicité et testabilité via les repositories Spring Data

## Déploiement

### Stratégie de démarrage
- **Contexte** : Le CDC propose Render ou AWS
- **Décision** : Configuration compatible Render en premier lieu; Docker Compose pour le développement local. Documentation AWS disponible pour la montée en charge

### Base de données
- **Contexte** : Données semi-structurées (présences par culte, statistiques)
- **Décision** : Utilisation de JSONB PostgreSQL pour la flexibilité sans perdre les avantages relationnels

## Sécurité

### JWT RS256
- **Contexte** : Le CDC mentionne JWT sans préciser l'algorithme
- **Décision** : RS256 (asymétrique) pour permettre la validation des tokens par d'autres services sans partager le secret. Génération de clés via `setup-keys.sh`

### Rate Limiting
- **Contexte** : Protection contre le brute-force sur le login
- **Décision** : Bucket4j avec limite de 10 requêtes/minute sur `/auth/login`

## API

### Pagination
- **Contexte** : Nécessité de paginer les listes
- **Décision** : Pagination via paramètres `page` et `size` (convention Spring Data), réponse encapsulée dans `PageResponse`

### Format des réponses d'erreur
- **Contexte** : Gestion globale des exceptions
- **Décision** : RFC 7807 (Problem Details) pour les erreurs API

## Tests

### Données de seed
- **Contexte** : Nécessité de données de démonstration fiables
- **Décision** : Les mots de passe des comptes de démonstration sont initialisés à "password123" via `DataInitializer` au démarrage (hash BCrypt coût 12)

### Base de test
- **Contexte** : Tests d'intégration sans dépendance PostgreSQL
- **Décision** : H2 en mémoire pour les tests unitaires, Testcontainers pour les tests d'intégration

## Frontend

### Gestion d'état
- **Contexte** : Pas de spécification précise
- **Décision** : TanStack Query pour la gestion du cache serveur, React Context pour l'état global (authentification)

### Styling
- **Contexte** : Design moderne requis
- **Décision** : Tailwind CSS avec design system personnalisé (couleurs, composants réutilisables)
