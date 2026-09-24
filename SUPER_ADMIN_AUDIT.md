# Audit Super Admin — Discipolat

Date de contrôle : 2026-09-24

## Verdict

Le socle Super Admin est présent sur les trois clients, avec provisioning atomique, approbation des inscriptions, invitations expirables à usage unique et stockées uniquement sous forme de hash, rotation des refresh tokens par famille, impersonation tenant-scoped, audit, plans et feature flags persistants. Le système n'est toutefois pas encore validé comme production SaaS sans risque : il reste la couverture complète des feature flags, les quotas/usage, l'association native des liens d'invitation et l'infrastructure réelle PostgreSQL/Redis.

| Couche | État actuel | Points bloquants restants |
|---|---:|---|
| Backend | 98/100 | Couverture complète des feature flags, validation PostgreSQL/Redis, contrôles opérationnels du refresh token family |
| Web | 93/100 | Quotas/usage, tests de contrat complémentaires, dette de warnings lint |
| Mobile | 95/100 | App Links de production, quotas/usage, consolidation du bootstrap tenant et dette analyzer |

## Contrôles réalisés

### Backend

- Contrôleur plateforme consolidé dans `SuperAdminController`.
- Feature flags persistés via `platform_feature_flags` et migration `V168`.
- Création de tenant validée : nom, slug, fuseau, plan actif, pagination bornée.
- Réponses dashboard corrigées selon le contrat imbriqué backend.
- Réponses nullable sécurisées pour détail tenant, subscription et feature flags.
- Actions tenant auditées avec l'acteur réel et upsert de plan capable de réactiver un plan inactif.
- Réponse d'authentification enrichie avec `platformRoles` et `platformSuperAdmin`.
- Les rôles plateforme sont résolus par une requête native tenant-agnostic afin de survivre au changement de tenant.
- Configuration plateforme, SaaS, cache, santé système, marque et reset beta réservés au Super Admin.
- Les refresh tokens sont refusés par le filtre Bearer et validés explicitement par le flux de refresh.
- Les événements asynchrones transportent le tenant et les webhooks/notifications email s'exécutent via `TenantContext.runAsTenant`.
- L'impersonation est liée à l'appelant, son token est révoqué à l'arrêt et l'endpoint/context web utilise le contrat backend correct.
- La cible d'impersonation est résolue par le couple tenant/email ; le tenant est obligatoire dans les contrats canonique, web et mobile.
- Changement de tenant retourne de nouveaux access/refresh tokens.
- Provisionnement atomique web/mobile : tenant → église → département → famille dans une transaction.
- L'inscription publique crée une demande en attente d'approbation ; l'approbation crée le tenant, l'église racine, le pasteur et sa membership.
- La révocation persistante des refresh et impersonation tokens est stockée par hash et purgée après expiration.
- La taille de `event_publication.serialized_event` est élargie pour PostgreSQL par `V171` et pour H2 par `src/test/resources/import.sql`.
- L’acceptation d’invitation est centralisée dans `InvitationService`, transactionnelle, protégée par verrou pessimiste, avec expiration inclusive, usage unique, binding tenant/email/rôle/scope persisted et rejet des rôles plateforme.
- Le code d’invitation n’est plus journalisé ni accepté par l’inscription publique : ce flux renvoie vers la page d’acceptation dédiée.
- La migration `V172` aligne PostgreSQL sur `invitations.organization_node_id` et son index.
- Les tokens d’invitation sont persistés uniquement via `token_hash` SHA-256 ; `V173` backfill les lignes existantes, puis `V175` supprime la colonne plaintext et son index.
- Les endpoints publics d’invitation ignorent le tenant d’un éventuel Bearer token incident et renvoient des réponses `Cache-Control: no-store`.
- Les recherches de compte à la création et à l’acceptation sont tenant-scoped ; la validation expose `accountExists` sans autoriser le client à fournir email, rôle ou scope.
- Les refresh tokens sont maintenus par famille via `V174`; la rotation consomme atomiquement l’ancien token et révoque toute la famille en cas de reuse.

### Web

- `TenantProvider` monté dans l'arbre React.
- Identité plateforme restaurée via `/auth/me` sans confondre `ADMIN` tenant et `PLATFORM_SUPER_ADMIN`.
- Routes plateforme protégées par `user.platformSuperAdmin`.
- Dashboard web : métriques imbriquées, édition tenant, suspendre/reactiver, plans, feature flags, états loading/error/retry.
- Page dédiée Plans SaaS : création, édition et activation/désactivation.
- Page Audit plateforme : consultation paginée des événements globaux.
- Page dédiée Impersonation avec démarrage, restauration et arrêt du contexte.
- Page d’approbation des demandes d’inscription.
- Routes `/platform/tenants` et routes de configuration globale corrigées.
- Wizard de provisioning web regroupé en une requête finale atomique.
- Page d’acceptation d’invitation : validation GET sans consommation, chemin encodé, gestion des comptes existants sans formulaire d’identifiants, puis POST d’acceptation et redirection vers `/login`.

### Mobile

- Dashboard plateforme : métriques, dernières églises, plans, feature flags, actions rapides.
- Écran Audit plateforme mobile avec pagination et détail des événements.
- Gestion des tenants : recherche, pagination, suspendre, réactiver, archiver, provisioning.
- Drawer et routing dédiés au Super Admin.
- Sélection de tenant normalisée sur `tenantId/tenantName` et changement de token persisté.
- Restauration de session, logout révoquant le refresh token et réinitialisation du contexte tenant.
- Wizard de provisioning mobile regroupé en une requête finale atomique.
- Activation/désactivation des plans depuis le dashboard plateforme.
- Écran d’audit plateforme mobile dédié.
- Écran d’approbation des demandes d’inscription avec pagination, décision motivée, rechargement et gestion d’erreur.
- Impersonation mobile tenant-scoped avec sauvegarde/restauration sécurisée des tokens, bannière globale, arrêt et rollback après interruption du switch.
- Logout mobile met fin à l’impersonation avant de restaurer la session Super Admin et de nettoyer les tokens.
- Écran mobile d’acceptation d’invitation : token 32 hexadécimal validé, GET sans consommation, fallback par saisie manuelle, aucun stockage du token, création de compte ou acceptation d’un compte tenant existant.
- Route invitation explicitement publique avant onboarding ; les autres routes non authentifiées échouent désormais fermement vers connexion/onboarding au lieu d’être implicitement autorisées.
- Le canal Dart de `FLAG_SECURE` Android est aligné sur le canal natif `discipolat/secure_screen`.

## Matrice de parité

| Fonction | Backend | Web | Mobile |
|---|---:|---:|---:|
| Dashboard métriques | ✅ | ✅ | ✅ |
| Liste/filtre tenants | ✅ | ✅ | ✅ |
| Création tenant + onboarding | ✅ | ✅ | ✅ |
| Église → département → famille | ✅ transactionnel | ✅ | ✅ |
| Modifier/suspendre/reactiver/archiver tenant | ✅ | ✅ | ✅ |
| Plans | ✅ | ✅ | ✅ activation/désactivation dans le dashboard |
| Feature flags | ✅ persistant | ✅ | ✅ |
| Inscription publique en attente | ✅ | ✅ approbation | ✅ approbation |
| Acceptation d’invitation tenant | ✅ expiration + usage unique | ✅ page dédiée | ✅ écran + route ; App Links production à associer |
| Impersonation | ✅ tenant-scoped | ✅ page dédiée | ✅ dédiée + restauration sécurisée |
| Audit plateforme query | ✅ | ✅ | ✅ |
| Quotas/usage par tenant | ⚠️ | ⚠️ | ❌ |
| Sélection multi-tenant persistante | ✅ tokens | ✅ | ⚠️ bootstrap à consolider |

## Risques sécurité non clos

1. Les migrations `V172`, `V173`, `V174` et `V175` doivent être exécutées et vérifiées sur chaque base PostgreSQL de production ; les tests H2 ne couvrent pas les backfills, suppressions de colonne et contraintes SQL.
2. La rotation des refresh tokens est désormais protégée par famille et reuse, mais il reste à vérifier le comportement en cas de déploiement multi-instance PostgreSQL et à couvrir les scénarios de logout concurrents par tests d’intégration.
3. Les feature flags sont persistés mais ne sont pas encore appliqués par tous les gates métier.
4. Les quotas et la consommation par tenant ne sont pas exposés de façon complète au Super Admin.
5. Aucun scénario d’intégration n’a encore été exécuté contre une base PostgreSQL et un cache Redis réels.
6. Le reset beta et la suppression de caches sont protégés, mais une garde environnement fail-closed mérite une vérification dédiée avant déploiement.
7. L’audit ne couvre pas encore de façon exhaustive les actions de provisioning, d’approbation et de rotation de tokens dans tous les clients.
8. L’écran invitation mobile est disponible via sa route interne et la saisie manuelle, mais l’ouverture native du lien HTTPS nécessite les fingerprints Android et l’Associated Domain iOS de production.

## Validation exécutée

- Backend `mvn -q test` : ✅
- Backend `mvn -q -Dtest=SchemaVerificationIntegrationTest test` : ✅, avec assertion que `serialized_event` dépasse 255 caractères en H2.
- Backend ciblé `PeopleCriticalPathIntegrationTest,PropagationChainIntegrationTest,PropagationConsistencyTest,TenantServiceTest` : ✅ 30 tests.
- Backend `MultiTenantSecurityTests.SuperAdminTests` : ✅ 7 tests, dont la résolution d’un email dupliqué par tenant.
- Frontend `npm run build` : ✅
- Frontend `npm test -- --run` : ✅ 325/325, dont 4 tests d’acceptation d’invitation.
- Backend `InvitationServiceTest` : ✅ 6 tests ; `InvitationControllerTest` : ✅ 3 tests ; `TenantFilterInterceptorTest` : ✅ 2 tests.
- Backend `RefreshTokenSessionServiceTest` : ✅ 4 tests ; `PeopleCriticalPathIntegrationTest` : ✅ 8 tests.
- Frontend `npm run lint` : 0 erreur, 345 warnings historiques.
- Mobile `flutter test --no-pub` : ✅ 356 tests.
- Mobile ciblé invitation : ✅ 13 tests couvrant le token, l’écran, la route publique et le canal `FLAG_SECURE`.
- Mobile ciblé `impersonation_service_test.dart` + `platform_registration_requests_screen_test.dart` : ✅ 7 tests.
- Mobile `flutter analyze --no-pub` : 528 remarques historiques, aucune erreur et aucune remarque introduite dans les nouveaux écrans/services.
- `git diff --check` : ✅

## Priorité suivante

1. Associer le lien d’invitation HTTPS aux builds Android/iOS de production avec les fingerprints et domains réellement signés.
2. Étendre les feature flags aux gates métier et exposer quotas/usage par tenant.
3. Exécuter les parcours critiques et les migrations V172/V173/V174/V175 sur PostgreSQL et Redis réels.
4. Ajouter les tests de contrat dashboard/audit/tenants/provisioning sur les deux clients.
5. Réduire progressivement la dette lint/analyzer sans masquer les warnings par une hausse des seuils.
