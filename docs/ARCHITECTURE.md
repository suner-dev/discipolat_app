# Architecture Technique

## Vue d'ensemble

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   React App  │    │  Flutter App │    │  Postman/CLI │
│  :3000/5173  │    │  Android/iOS │    │              │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │ HTTPS / REST
                           ▼
                    ┌──────────────┐
                    │   Nginx      │
                    │ Reverse Proxy│
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  Spring Boot │
                    │  API :8080   │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  PostgreSQL  │
                    │  :5432       │
                    └──────────────┘
```

## Backend (Spring Boot 3)

### Structure modulaire

Chaque module métier suit le pattern :

```
module/
├── api/              # Contrôleurs REST, DTOs
├── domain/           # Entités, services, repositories
├── infrastructure/   # Implémentations techniques
└── config/           # Configuration du module
```

### Modules identifiés

| Module | Responsabilité | Ports entrants | Ports sortants |
|---|---|---|---|
| authentication | Login, JWT, refresh | AuthController | AuthService, JwtTokenProvider |
| users | CRUD utilisateurs, rôles | UserController | UserService, UserRepository |
| departments | CRUD départements | DepartmentController | DepartmentService, DepartmentRepository |
| families | CRUD familles de disciples | FamilyController | FamilyService, FamilyRepository |
| souls | CRUD âmes, historique | SoulController | SoulService, SoulRepository |
| reports | Rapports faiseur + famille | MakerReportController | ReportService |
| parallel-followups | Suivis parallèles | ParallelFollowupController | ParallelFollowupService |
| alerts | Alertes automatiques | AlertController | AlertService |
| notifications | Notifications push/email | NotificationController | NotificationService |
| dashboard | KPI et indicateurs | DashboardController | DashboardService |
| audit | Journalisation des actions | - | AuditService |

## Frontend (React 19 + TypeScript)

### Architecture

```
src/
├── components/       # Composants réutilisables
│   ├── layout/       # Sidebar, Navbar, Layouts
│   └── shared/       # DataTable, modals, etc.
├── contexts/         # React Contexts (Auth)
├── lib/              # API client, utilitaires
├── pages/            # Pages de l'application
├── types/            # Types TypeScript
├── App.tsx           # Routes et configuration
├── main.tsx          # Point d'entrée
└── index.css         # Styles Tailwind
```

### Principales dépendances
- **TanStack Query** : Cache serveur, mutations
- **React Router** : Navigation, routes protégées
- **React Hook Form + Zod** : Formulaires avec validation
- **Recharts** : Graphiques du dashboard
- **Axios** : Client HTTP avec intercepteurs
- **Lucide React** : Icônes

## Mobile (Flutter)

### Architecture

```
lib/
├── core/             # Constantes, thème, utilitaires
├── data/             # Services API, repositories
├── domain/           # Modèles, use cases
├── presentation/     # Écrans, widgets, providers
└── main.dart         # Point d'entrée
```
