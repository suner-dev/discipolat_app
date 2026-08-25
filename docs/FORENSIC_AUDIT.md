# FORENSIC AUDIT — DISCIPOLAT APPLICATION
**Date:** 2026-08-25 | **Scope:** Fullstack (Backend + Frontend + Mobile + DB + API + Security)

---

## PROBLÈMES IDENTIFIÉS

| ID | MODULE | PAGE/COMPONENT | TYPE | DESCRIPTION | IMPACT | PRIORITÉ | STATUT |
|----|--------|---------------|------|-------------|--------|----------|--------|
| F-001 | Security | SecurityConfig.java | SECURITY | 13+ contrôleurs sans aucune autorisation (@PreAuthorize) | Tout utilisateur authentifié peut effectuer des opérations CRUD sensibles | P0 | OUVERT |
| F-002 | Security | PermissionService.java | SECURITY | Modèle d'autorisation permissif par défaut (retourne true si pas de ligne de permission) | Toute nouvelle feature sans permission explicite est accessible à tous | P0 | OUVERT |
| F-003 | Security | SecurityConfig.java | SECURITY | CORS wildcard avec credentials pour ngrok/cloudflared | N'importe quel attaquant avec un tunnel peut faire des requêtes cross-origin avec credentials | P0 | OUVERT |
| F-004 | Security | PaymentController.java | SECURITY | Webhook de paiement sans authentification quand secret vide | Fausse confirmation de paiement possible | P0 | OUVERT |
| F-005 | Security | WhatsAppWebhookController.java | SECURITY | Webhook WhatsApp sans vérification de signature (X-Hub-Signature) | Faux messages WhatsApp possibles | P1 | OUVERT |
| F-006 | Security | DataInitializer.java | SECURITY | Mot de passe par défaut "password123" logué en clair dans les logs | Exposition de credentials dans les logs | P1 | OUVERT |
| F-007 | Security | application.yml | SECURITY | Clé AES de chiffrement hardcodée par défaut | Données "chiffrées" sans protection réelle en prod | P0 | OUVERT |
| F-008 | Security | SecurityConfig.java | SECURITY | Swagger/API docs accessibles sans authentification | Reconnaissance de surface d'attaque facilitée | P2 | OUVERT |
| F-009 | Security | SecurityConfig.java | SECURITY | Pas de mécanisme de révocation des refresh tokens (validité 7 jours) | Token volé reste valide pendant 7 jours | P2 | OUVERT |
| F-010 | Security | WorkflowConfigController.java | SECURITY | Map statique mutable partagée entre tous les tenants | Fuite de données inter-tenant + thread-unsafe | P0 | OUVERT |
| F-011 | Security | RewardController.java | SECURITY | tenantId accepté comme paramètre sans vérification d'appartenance | Accès inter-tenant possible | P1 | OUVERT |
| F-012 | Security | MarketplaceController.java | SECURITY | tenantId accepté comme paramètre sans vérification d'appartenance | Accès inter-tenant possible | P1 | OUVERT |
| F-013 | Security | DepartmentKpiController.java | SECURITY | tenantId accepté comme paramètre sans vérification d'appartenance | Accès inter-tenant possible | P1 | OUVERT |
| F-014 | Security | Multiple controllers | SECURITY | IDOR: endpoints avec ID sans vérification de propriétaire | Lecture/modification/suppression de données d'autres utilisateurs | P0 | OUVERT |
| F-015 | Security | Multiple controllers | SECURITY | 15+ endpoints acceptent Map<String, Object> sans @Valid | Injection de données, champs non attendus acceptés | P1 | OUVERT |
| F-016 | Security | IntelligenceController | API | URL /api/intelligence au lieu de /api/v1/intelligence | Incohérence API, potentiel bypass filtre URL | P1 | OUVERT |
| F-017 | Security | ExecutiveInsightsController | API | URL /api/executive-insights au lieu de /api/v1/executive-insights | Incohérence API | P1 | OUVERT |
| F-018 | Security | OnboardingWizardController | API | URL /api/onboarding-wizard au lieu de /api/v1/onboarding-wizard | Incohérence API | P1 | OUVERT |
| F-019 | Frontend | 20 pages | MISSING | 20 pages avec 100% de données mock, aucune intégration API | Fonctionnalités purement décoratives | P0 | OUVERT |
| F-020 | Frontend | App.tsx | BUG | Routes dupliquées: /my-team et /notification-preferences | Comportement indéterminé du routeur | P1 | OUVERT |
| F-021 | Frontend | AuthContext.tsx | BUG | Aucun mécanisme de refresh du token JWT côté frontend | Déconnexion après expiration du token sans renouvellement | P0 | OUVERT |
| F-022 | Frontend | AuthContext.tsx | BUG | Aucune détection d'expiration JWT ni auto-logout | Utilisateur voit des erreurs 401 au lieu d'être déconnecté proprement | P1 | OUVERT |
| F-023 | Frontend | workspaces.ts | MISSING | ADMIN_ONLY_HREFS est vide — Pasteur a le même accès que Admin | Pas de restriction Admin-only pour les Pasteur | P1 | OUVERT |
| F-024 | Frontend | routeAccess.ts | SECURITY | Routes avec restrictions dans App.tsx absentes de ROUTE_ROLES | Routes protégées côté route mais pas côté navigation | P2 | OUVERT |
| F-025 | Frontend | App.tsx | SECURITY | /portal dans MainLayout sans ProtectedRoute | Page accessible par tout utilisateur sans authentification | P1 | OUVERT |
| F-026 | Frontend | 33+ routes | SECURITY | Routes sans restriction de rôle | Toute opération accessible à tous les rôles y compris MEMBRE | P2 | OUVERT |
| F-027 | Frontend | 6+ pages | UX | Error swallowing: catch vides sans feedback utilisateur | L'utilisateur ne sait pas que l'opération a échoué | P2 | OUVERT |
| F-028 | Frontend | 5+ pages | UX | Messages d'erreur génériques "Erreur" sans détails | Diagnostique impossible pour l'utilisateur | P3 | OUVERT |
| F-029 | Frontend | 8+ directories | ARCHITECTURE | Répertoires vides (pages/dashboard/, components/dashboard/, etc.) | Organisation du code inachevée | P3 | OUVERT |
| F-030 | Frontend | 5+ pages | UX | Imports d'icônes inutilisés | Code mort | P3 | OUVERT |
| F-031 | Frontend | workspaces.ts | UX | 3 items nav RESPONSABLE pointent vers /departments | Doublon dans la navigation | P3 | OUVERT |
| F-032 | Mobile | app_localizations.dart | INTERNATIONALIZATION | 2 systèmes i18n en parallèle (map manuelle + ARB) | Confusion, incohérence des traductions | P2 | OUVERT |
| F-033 | Mobile | Multiple screens | INTERNATIONALIZATION | Strings françaises hardcodées dans les widgets | Traduction incomplète | P2 | OUVERT |
| F-034 | Mobile | data/services/ | ARCHITECTURE | Double service session (SessionManager + SessionTimeoutService) | Duplication fonctionnelle | P2 | OUVERT |
| F-035 | Mobile | data/services/ | ARCHITECTURE | Double protection screenshot (ScreenshotProtectionService + SecureScreenService) | Duplication fonctionnelle | P3 | OUVERT |
| F-036 | Mobile | app.dart | ARCHITECTURE | AuthState singleton non-Riverpod | Incohérence avec le pattern state management du reste | P2 | OUVERT |
| F-037 | Mobile | Multiple screens | ARCHITECTURE | Screens >1000 lignes (department screens) | Maintenance difficile | P2 | OUVERT |
| F-038 | Mobile | core/, features/, shared/ | ARCHITECTURE | Répertoires d'architecture vides | Architecture prévue mais inappliquée | P3 | OUVERT |
| F-039 | Backend | BenchmarkController.java | MOCK | Données benchmark mockées en dur | Données fausses | P2 | OUVERT |
| F-040 | Backend | AdminIntegrationController.java | ARCHITECTURE | Config intégrations stockée en mémoire (ConcurrentHashMap) | Données perdues au redémarrage, partagées inter-tenant | P2 | OUVERT |
| F-041 | Backend | AuthContext.tsx | BUG | social login définit refreshToken à '' | Pas de refresh pour les utilisateurs social auth | P1 | OUVERT |
| F-042 | Backend | JwtTokenProvider.java | SECURITY | Log du chemin de la clé privée JWT | Fuite d'information dans les logs | P2 | OUVERT |
| F-043 | Backend | PermissionService.java | SECURITY | SQL par concaténation de colonne (switch mitigé mais fragile) | Risque d'injection SQL futur | P2 | OUVERT |
| F-044 | Frontend | streaming/ | MOCK | StreamingPage + StreamingChat = 100% mock | Fonctionnalité non implémentée | P2 | OUVERT |
| F-045 | Frontend | AiPredictionsPage.tsx | MOCK | Bouton "Générer" = setTimeout sans appel API | Fonctionnalité décorative | P2 | OUVERT |
| F-046 | Frontend | VoiceAssistantPage.tsx | MOCK | Mode démo avec setTimeout | Fonctionnalité non prête | P2 | OUVERT |
| F-047 | Mobile | secure_screen.dart | MOCK | IDs utilisateur 'current_user'/'current_org' en dur | Audit logging non fonctionnel | P1 | OUVERT |
| F-048 | Mobile | api_config.dart | CONFIG | URL production hardcodée en dur | Déploiement fragile | P2 | OUVERT |
| F-049 | Backend | DataInitializer.java | SECURITY | Logs du mot de passe par défaut pour chaque user créé | Exposition massive de credentials dans logs | P0 | OUVERT |
| F-050 | Backend | Multiple controllers | API | Contrôleurs sans validation d'entrée (@Valid) | Requêtes mal formées acceptées | P1 | OUVERT |

---

## SYNTHÈSE PAR SÉVÉRITÉ

| Priorité | Nombre | Description |
|----------|--------|-------------|
| P0 | 8 | Bloquants — Sécurité critique, données exposées, fonctionnalités fausses |
| P1 | 18 | Critiques — IDOR, webhooks ouverts, refresh token manquant, mock critique |
| P2 | 16 | Importants — Swagger public, double services, i18n incomplet, config fragile |
| P3 | 8 | Améliorations — Code mort, organisation, messages génériques |

**TOTAL: 50 problèmes identifiés**
