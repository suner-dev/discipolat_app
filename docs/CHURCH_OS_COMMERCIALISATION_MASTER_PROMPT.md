# DISCIPOLAT — CHURCH OS · PROMPT MAÎTRE DE COMMERCIALISATION (v1)

> **Document contractuel unique.** Fusion officielle de :
> - `docs/À faire.md` — le chantier multi-tenant inachevé (erreurs de compilation + sections §27-72)
> - `docs/PRMPT.pdf` (et son extraction `docs/PRMPT.txt`) — le Church Operating System, maître-contrat 43 sections, phases 00→22
> - Exigences supplémentaires utilisateur : **Dress Code & patrimoine événementiel**, **FAMILY = espace configurable de premier rang**, **inscription automatique au répertoire**, **rôles vivants (changement d'interface en temps réel)**, **pasteur principal → transfert/nomination de pasteurs de campus**, **tout modulable par responsable (pages, boutons, couleurs, sous-équipes)**, **import/export**.
>
> ⚠️ **Périmètre v1 = TOUT.** Aucune fonctionnalité n'est reportée après commercialisation.
> L'ordre des étapes suit les **dépendances techniques**, pas les priorités commerciales.

---

## 0. MODE D'EMPLOI — À LIRE EN ENTIER AVANT DE COMMENCER

### 0.1 Rôle unique de l'agent

Tu es l'agent principal de développement de Discipolat. Tu agis tour à tour comme :

Architecte · Ingénieur Backend (Spring Boot) · Ingénieur Frontend (React 19 / Vite / Tailwind) · Ingénieur Flutter · Architecte de données PostgreSQL · Product Manager · Designer UX/UI · Ingénieur Sécurité · Ingénieur QA · DevOps · Relecteur de code.

Tu travailles **sur le code EXISTANT du dépôt**. Tu ne repars jamais de zéro. Tu ne supprimes jamais une fonctionnalité existante sans justification écrite dans l'étape.

### 0.2 Sources normatives (hiérarchie en cas de conflit)

1. **Ce document** (instructions d'étape) — fait foi en cas de contradiction.
2. `docs/À faire.md` (détail des erreurs et des §27-72 — reproduit en annexes B et C).
3. `docs/PRMPT.pdf` / `docs/PRMPT.txt` (maître-contrat 43 sections — reproduit en annexe D).
4. `docs/tenant.md` (prompt maître multi-tenant existant) et `docs/MULTI_TENANT_ARCHITECTURE_AUDIT.md` (audit).
5. Les rapports dans `docs/rapports/` (règles d'audit déjà établies).

### 0.3 Règles absolues (ne jamais enfreindre)

1. **NE JAMAIS créer une table par département ni par famille.** Un espace (département/famille/équipe) est une *configuration*, jamais une fonctionnalité codée en dur.
2. **NE jamais faire confiance au `tenantId` envoyé par le frontend.** Le tenant est déduit côté backend du contexte authentifié (`CurrentTenantResolver`).
3. **Interdiction formelle de `repository.findById(id)`** sur une donnée sensible multi-tenant → toujours `findByIdAndTenantId(...)` ou équivalent scoped.
4. **Les permissions ne sont JAMAIS appliquées côté frontend comme source d'autorisation** : le backend vérifie `AuthorizationService` + scope à chaque endpoint.
5. **Les données sensibles (pastoral, finances, personnel) ne sont jamais exposées à un simple membre** : confidentialité + scope + audit d'accès.
6. **Ne jamais supprimer physiquement une donnée historique critique** (soft delete + historisation).
7. **Pas de mock, pas de fausse API, pas de bouton sans logique, pas d'écran décoratif.**
8. **Toute modif visible web doit être propagée au mobile en temps réel** (couleurs, noms, ajout de membre, désignation, config).
9. **Un rôle/designation modifié ⇒ l'interface de la personne change automatiquement** (web + mobile) à sa prochaine action.
10. **Tout événement métier majeur ⇒ événement de domaine + outbox + consumers** (notification, audit, historique, finance, analytics).
11. **Un département OU une famille peut exister sans remplir la totalité des champs demandés par son template.**

### 0.4 Workflow type d'une étape (17 actions — obligatoire à chaque étape)

1. Analyser la demande et son contexte (lire les fichiers concernés).
2. Planifier en max. 3 sous-tâches simultanées.
3. Base de données / migration Flyway versionnée (`V<timestamp>__description.sql`).
4. Backend (entité → repository scoped → service → DTO).
5. Tests backend.
6. API REST + documentation OpenAPI.
7. Frontend (React).
8. Tests frontend.
9. Mobile (Flutter).
10. Tests mobile.
11. Intégration réelle (backend + web + mobile branchés sur les vraies APIs).
12. Sécurité / tenant-scope sur les routes touchées (+ test IDOR si pertinent).
13. Revue UX de l'écran (questionnaire §0.9).
14. Documentation (API.md, architecture, guide si besoin).
15. Validation complète (commandes §0.10 sur les 3 couches).
16. Mise à jour de la **Fiche de suivi** (§0.7).
17. Commit atomique (convention §0.6).

> ⛔ Un agent ne fait **jamais** deux étapes du document dans le même commit. Une étape n'est "verte" qu'après validation complète.

### 0.5 Definition of Done (DoD) global — avant de clore une étape

- [ ] `cd backend && mvn verify -B` ✅ (compile + tests)
- [ ] `cd frontend && npm run lint && npm run build && npm run test` ✅
- [ ] `cd mobile && flutter analyze --no-pub && flutter test --no-pub` ✅
- [ ] Migrations Flyway appliquées et rejouables (aucune modif manuelle de la prod)
- [ ] API documentées (OpenAPI) pour les routes nouvelles/modifiées
- [ ] Sécurité vérifiée : tenant-scope + RBAC scopé sur les routes touchées, pas d'IDOR
- [ ] Aucun mock / bouton mort / erreur console / erreur de compilation
- [ ] Fiche de suivi (§0.7) à jour
- [ ] Documentation mise à jour si impact

### 0.6 Conventions de commits (atomiques)

```
feat(core): ...
feat(config): ...
feat(department): ...
feat(family): ...
feat(dresscode): ...
feat(event): ...
feat(asset): ...
feat(finance): ...
feat(mobile): ...
feat(offline): ...
feat(realtime): ...
feat(security): ...
feat(tenant): ...
feat(saas): ...
feat(people): ...
feat(discipleship): ...
feat(pastoral): ...
feat(prayer): ...
feat(media): ...
feat(workflow): ...
feat(audit): ...
feat(ui): ...
fix(security): ...
refactor(ui): ...
test(security): ...
docs: ...
```
Un commit = une étape verte. Toujours laisser le working tree propre en fin d'étape. Les fichiers `docs/PRMPT.txt`, `frontend-local.log`, `backend-local.log` etc. ne doivent **jamais** être commités.

### 0.7 Fiche de suivi d'avancement (à tenir à jour à chaque étape)

Copier ce tableau en tête du futur fichier `docs/ETAT_AVANCEMENT_CHURCH_OS.md` (à créer à l'étape 0.1) et le mettre à jour à chaque étape verte. C'est la **mémoire de session** de l'agent.

| Étape | Statut (⬜ / 🔵 en cours / ✅ vert) | Date | Commit | Note |
|---|---|---|---|---|
| G0.1 | ✅ | 2026-09-12 | `a1b2c3` | État des lieux produit |
| … | | | | |

> ⛔ Toute étape dont le statut n'est pas `✅ vert` est considérée comme **à refaire entièrement** à la reprise.

### 0.8 Protocole de reprise (si l'agent est interrompu/redémarré)

1. Lire la fiche de suivi `docs/ETAT_AVANCEMENT_CHURCH_OS.md`.
2. Lancer les validations des trois couches (commandes §0.10).
3. Reprendre la première étape non verte **dans l'ordre**.
4. Ne jamais commencer une étape dont la précédente n'est pas verte.
5. En cas d'ambiguïté bloquante (données manquantes, choix business) : écrire la question + 2-3 réponses candidates dans l'étape concernée, marquer l'étape `🚧 BLOQUÉE`, et **s'arrêter** (protocole STOP-NEED-HELP). Ne jamais inventer une réponse business.

### 0.9 Questionnaire de revue UX (à chaque écran créé/modifié)

1. Quel est l'objectif de cet écran ?
2. Quelle est l'action principale et est-elle à un clic ?
3. Quelles informations sont prioritaires (hiérarchie visuelle) ?
4. L'état est-il immédiatement compréhensible (statut, couleurs, icônes) ?
5. Peut-on réduire les clics (actions contextuelles, raccourcis, inline editing) ?
6. L'historique est-il accessible sans quitter la page ?
7. L'écran est-il vide-state premium (pas de tableau vide brut) ?
8. Mobile : l'action principale est-elle atteignable au pouce ?
9. Les erreurs de saisie sont-elles prévenues (validation inline) ?
10. Undo/optimistic UI appliqués là où c'est sûr ?

### 0.10 Commandes canoniques de validation

```bash
# BACKEND (Spring Boot 3.4.7 / Maven)
cd backend && mvn compile -q
cd backend && mvn test -q -Dspring.profiles.active=test
cd backend && mvn verify -B          # validation complète (DoD)

# FRONTEND (React 19 / Vite / Tailwind / Vitest)
cd frontend && npm ci
cd frontend && npm run lint
cd frontend && npm run build
cd frontend && npm run test

# MOBILE (Flutter 3.24 / Riverpod / Drift / Dio)
cd mobile && flutter pub get
cd mobile && flutter analyze --no-pub
cd mobile && flutter test --no-pub

# Démarrage local de référence
bash start-local.sh                   # backend :8080, frontend :5173
```

### 0.11 Business Inputs — à fournir par l'équipe AVANT le lancement

Ces éléments sont marqués `[BUSINESS_INPUT]` dans les étapes concernées. L'agent implémente les mécanismes avec des valeurs par défaut raisonnables, mais **ne décide jamais seul** des paramètres commerciaux définitifs. Le **modèle commercial Dual-Market a été validé avec l'équipe le 12/09/2026** (Annexe F) ; les 4 plans y figurent avec prix EUR ◈ FCFA, quotas et crédits IA :

- ✅ **Plans SaaS** : 4 plans (DÉCOUVERTE / DÉMARRAGE / CROISSANCE / RÉSEAU & CAMPUS), géo-pricing **EUR/FCFA/USD**, quotas + crédits IA par plan — **cf. Annexe F** (validé).
- ⏳ **Paiements** : comptes réels MTN MoMo / Orange Money (Afrique), Stripe/SEPA (Europe), Stripe (Amériques) — comptes sandbox + clés à fournir `[BUSINESS_INPUT]`.
- ⏳ **Serveur SMTP transactionnel** (invitations, notifications) et expéditeur `[BUSINESS_INPUT]`.
- ⏳ **Nom de domaine public** + sous-domaines (app.xxx, api.xxx) `[BUSINESS_INPUT]`.
- ⏳ **Églises pilotes** : **>3 églises (Afrique + Europe + Amériques — Asie/Océanie en v1.1)** pour la beta `[BUSINESS_INPUT]`.
- ⏳ **Juridique** : CGU + politique de confidentialité par région (RGPD Europe ; Cameroun/Afrique) `[BUSINESS_INPUT]`.
- ⏳ **Marque** : nom commercial final (`business_name`, paramètre tenant_settings §G1.2) ; logo, couleurs de la page vitrine.
- ✅ **Modularité par défaut (principe Odoo)** : *tout* — modules, champs personnalisés, workflows, devises, canaux low-band (WhatsApp/USSD), annuaire public, faiseur « par défaut », migration legacy — est **activable / configurable / personnalisable par chaque tenant** (Super-Admin → Tenant Admin → responsable d'espace → membre). Aucune valeur business ou UI n'est *hardcoded* : un toggle ou une clé de configuration décide.
- Exemples territoriaux de démarrage (paroisse, répertoire des équipes).

---
# PORTE G0 — STABILISATION (build vert sur les 3 couches)

> **Verrou de sortie :** le backend compile, les trois couches passent leurs validations, le dépôt est propre et taggé. Aucun travail architectural ne commence avant cette porte.

---

## Étape G0.1 — État des lieux complet du dépôt

**Objectif :** connaître le code existant avant toute modification (exigence PRMPT PHASE 0). Sorties : 3 documents.

**Références :** PRMPT §37 PHASE 0 ; `docs/tenant.md` §2 ; `docs/rapports/`.

**Tâches :**
1. Créer `docs/ETAT_AVANCEMENT_CHURCH_OS.md` avec la fiche de suivi (§0.7).
2. Produire `docs/architecture/current-state.md` : inventaire backend (modules `backend/src/main/java/com/discipolat/modules/*`), frontend (`frontend/src`), mobile (`mobile/lib`), base (les 110 migrations Flyway), CI/CD, scripts de lancement.
3. Produire `docs/architecture/gap-analysis.md` : croiser l'existant avec l'annexe C (§27-72) et l'annexe D (43 sections PRMPT) → pour chaque item : ✅ existant / ⚠️ partiel / ❌ manquant, avec fichiers concernés.
4. Produire `docs/architecture/target-architecture.md` : cible Church OS (schéma G2), espaces configurables (départements + familles), moteurs (module/template/custom field/workflow/event bus/audit/real-time), UX 2 niveaux.
5. Inventorier le **working tree non commité** (aujourd'hui : modifications multi-tenant PHASE 10 non commitées + `docs/PRMPT.pdf` non suivi). Lister chaque fichier modifié et son but présumé.
6. Mettre à jour la fiche de suivi.

**DoD :** 3 documents écrits + fiche de suivi créée + `git status` documenté.

**Commit :** `docs: state of the art audit before Church OS v1`

---

## Étape G0.2 — Sauvegarde de l'existant (point de rétablissement)

**Objectif :** garantir un point de retour sûr avant corrections massives.

**Tâches :**
1. Commiter les travaux multi-tenant **en cours** dans des commits atomiques **séparés et signifiants** (les modifs non commitées actuelles portent sur DataInitializer, AiKpiNarrativeService, AiModuleService, ExportServiceImpl, WebSocketConfig, BrandingController, ImpersonationController, ModuleFeatureController, OrganizationManagementController, SoulRepository, OrganizationNode(+Service), SaasPlanService, TenantMembershipRepository(+Service), TenantDestinationUserNameProvider). Préserver scrupuleusement ces travaux — ils représentent la PHASE 10 en cours.
2. Vérifier l'absence de secrets commités (`.env`, `keys/`, logs) → les ajouter à `.gitignore` si nécessaire (ne PAS les committer).
3. Créer un tag `v0.10-snapshot-pre-church-os`.
4. Ne pas corriger d'erreurs de compilation dans cette étape : **seulement sauvegarder**.

**DoD :** branche propre, aucune donnée sensible commitée, tag créé.

**Commit :** `chore: preserve WIP multitenancy phase 10 as v0.10-snapshot-pre-church-os`

---

## Étape G0.3 — Corriger toutes les erreurs de compilation (backend)

**Objectif :** `mvn compile` passe. C'est le préalable absolu (~100 erreurs listées dans `docs/À faire.md` — voir annexe B). **Règle d'or :** chaque correction est la plus petite possible, ne casse aucune fonctionnalité existante, n'introduit aucun mock, et ajoute un test quand le correctif touche de la logique.

**Sous-étape 1 — Domaine tenants/plateforme/SaaS :**
1. `SuperAdminController` : créer `countByStatut(...)` (méthode de service dédiée, requête scopée par tenant) · ajouter les getters manquants sur `SaasPlan` (`getIsPublic()`, `getFeatures()`, `getSeatsLimit()`) · remplacer les `Map.of(` > 10 entrées par des constructions lisibles (ex. `LinkedHashMap`) sans changer le contrat de la réponse.
2. `OrganizationManagementController` : ajouter `slug` à `OrganizationNode` (colonne de migration si absente + index unique par tenant), compléter `metadata` (JSON flexible validé), implémenter les méthodes manquantes du service (création/suppression/déplacement de nœud avec recalcul des `path`/`level`).
3. `ModuleFeatureController` : props ci-dessus + aligner avec `TenantFeatureService`.
4. `OrganizationManagementController` / `SaasPlanService` / `TenantMembershipService` : compiler puis ajuster les signatures appelées par les contrôleurs.

**Sous-étape 2 — Services IA/exports/souls :**
1. `AiModuleService` & `AiKpiNarrativeService` : implémenter/aligner les méthodes référencées par leurs contrôleurs (génération de récit KPI, services LLM disponibles dans le module `ai`), compiler les 2 services.
2. `ExportServiceImpl` : ajouter `getTitle()` et `isRead()` en lecture de contrat sur `Alert`/`Notification` (propriétés de présentation, pas de logique métier).
3. `SmartAlertService` & `BenchmarkController` : ajouter `findByDeletedFalse()` à `SoulRepository` (JPQL `where deleted = false` + ORDER BY) et l'utiliser.

**Sous-étape 3 — Recherche & divers :**
1. `SpiritualHealthService`, `KingdomMappingController`, `ContextualReminderScheduler` : compléter les repositories de domaines concernés avec `findByDeletedFalse()` et les méthodes attendues.
2. Lancer `cd backend && mvn compile -q` ; corriger **toutes** les erreurs restantes signalées par le compilateur (pas seulement la liste ci-dessus). Pour chaque correctif, ajouter une ligne dans la fiche de suivi (fichier → nature du correctif).

**DoD G0.3 :** `mvn compile -q` ✅ **sans erreur** · aucun changement de comportement métier · les tests existants ne sont pas modifiés sauf exigence de compilation · `mvn test -q -Dspring.profiles.active=test` ✅.

**Commit :** `fix(backend): resolve all pre-existing compilation errors`
## Étape G0.4 — Frontend : build, lint et tests verts

**Objectif :** `npm run lint && npm run build && npm run test` passent sur le frontend existant (qui compile aujourd'hui mais doit être sanitisé).

**Tâches :**
1. `cd frontend && npm ci && npm run lint` → corriger erreurs/warnings (max-warnings 0) sans changer les comportements.
2. `npm run build` → corriger toute erreur TS (`tsc -b`).
3. `npm run test` (Vitest) → corriger les tests rouges **sans affaiblir les assertions** ; si un test est obsolète par rapport à un comportement corrigé dans G0.3, le mettre à jour avec justification dans la fiche de suivi.
4. Nettoyer les imports morts / composants inutilisés visés par le linter, sans refonte.

**DoD :** lint 0 warning ✅ · build ✅ · tests ✅.

**Commit :** `chore(frontend): green lint build and tests`

---

## Étape G0.5 — Mobile : analyze et tests verts

**Objectif :** `flutter analyze --no-pub` et `flutter test --no-pub` passent.

**Tâches :**
1. `cd mobile && flutter pub get && flutter analyze --no-pub` → corriger les erreurs d'analyse (warnings traités comme erreurs).
2. `flutter test --no-pub` → corriger les tests rouges (ne pas supprimer des tests).
3. Signaler dans la fiche de suivi tout test désactivé (`skip`) avec raison.

**DoD :** analyze ✅ · tests ✅.

**Commit :** `chore(mobile): green analyze and tests`

---

## Étape G0.6 — Verrou G0 (Gate)

**Objectif :** ne rien construire d'architectural tant que cette checklist complète n'est pas cochée.

**Checklist de sortie G0 :**
- [ ] `mvn verify -B` ✅ (backend complet)
- [ ] `npm run lint && npm run build && npm run test` ✅ (frontend)
- [ ] `flutter analyze --no-pub && flutter test --no-pub` ✅ (mobile)
- [ ] 3 documents d'architecture produits (G0.1)
- [ ] Fiche de suivi créée et à jour
- [ ] Working tree propre, tag `v0.10-snapshot-pre-church-os` présent
- [ ] Aucune donnée sensible dans le dépôt

Si tout est coché : dernier commit de porte → prochain gate G1. Sinon : retour arrière sur l'étape en échec (interdiction de contourner).

**Commit :** `chore: Gate G0 stabilization green`

---

# PORTE G1 — CONTRATS MULTI-TENANT (finir les §27-72 bloquants du doc À faire)

> Objectif : verrouiller la fondation SaaS multi-tenant avant de poser les moteurs Church OS.
> Source : `docs/À faire.md` sections 1-2 (annexe C) + `docs/tenant.md`.

## Étape G1.1 — Cartographie fine de l'existant multi-tenant

**Objectif :** ne pas refaire ce qui existe déjà (PHASES 1→10 commitées).

**Tâches :**
1. Lire `docs/MULTI_TENANT_ARCHITECTURE_AUDIT.md`, `docs/FINAL_AUDIT_REPORT.md`, `docs/FEATURE_MATRIX.md`, `docs/tenant.md`.
2. Inventorier : entités tenant-aware (426 fichiers référencent tenantId), migrations, contrôleurs, `TenantContext.tsx`, `TenantSwitcher`, écrans Super Admin / Tenant Admin (web + mobile).
3. Remplir dans la fiche de suivi une ligne par § (27→72) avec statut `✅/⚠️/❌` et **preuve** (fichier + endpoint + écran).

**DoD :** carte §27-72 remplie avec preuves (ce sera la base des étapes G1.2→G1.11).

**Commit :** `docs: multitenancy contract mapping (27-72)`

---

## Étape G1.2 — §27-28 : Tenant Settings & Branding dynamique complet

**Objectif :** chaque église configure son identité complète, appliquée **partout** (web + mobile + emails + PDF), avec héritage par sous-unité (voir G1.7).

**Tâches :**
1. **Backend** : entité/Tables `tenant_settings` (clés typées : nom d'affichage, **nom commercial `business_name`** — affiché partout à la place du nom canonique quand défini, slogan, logo_url, cover_url, couleurs `primary/accent/surface`, polices, langue, fuseau, devise, format date/heure, pied de page, textes d'invitation) + **drapeaux booléens de toggle module/canal** (`low_band_enabled`, `public_directory_enabled`, `legacy_migration_enabled`…) ; mettre à jour la migration et `BrandingController` existant → exposition GET/PUT `tenant-settings`, authentifiée RBAC scopé.
2. **Web** : page `TenantAdminBrandingPage` complète (aperçu en direct, upload logo/cover → isolation fichiers existante §39), application des tokens CSS via variables (`--brand-*`) côté client, pas de hardcode des couleurs.
3. **Mobile** : `tenant_config.dart` + écran paramètres tenant → appliquer branding globalement (thème Riverpod, `flutter_localizations`).
4. **Propagation** : WebSocket `tenant-settings:changed` → les clients connectés rafraîchissent sans reconnexion forcée (exigence temps réel).
5. **Tests** : auth (seul tenant admin peut modifier), isolation (une église ne lit pas les settings d'une autre), propagation.

**DoD :** §27-28 → ✅ avec preuve (écran + test + propagation vérifiée sur 2 clients).

**Commit :** `feat(tenant): complete tenant settings and dynamic branding (27-28)`
## Étape G1.3 — §29 : TenantFeature — modules activables par tenant (CRUD complet)

**Objectif :** finir le CRUD de la table `tenant_features` (créée en migration mais incomplète) et la brancher à l'UX.

**Tâches :**
1. **Migration** : compléter `tenant_features` (`id`, `tenant_id`, `module_code`, `enabled`, `configuration_json`, `limits_json`, `created_at`, `updated_at`, contrainte d'unicité `(tenant_id, module_code)`).
2. **Backend** : entité `TenantFeature` + repository scoped + service (activation/désactivation, plafonds, validation `module_code` contre le catalogue §G2.2) + controller REST `GET/PUT/DELETE /api/admin/tenant-features`.
3. **Seed** : à la création d'un tenant, activer par défaut les modules du plan (§G1.4) + les modules "cœur" (people, events, notifications).
4. **Web** : page Super Admin "Modules du tenant" (liste + toggle + plafonds) ; page Tenant Admin (lecture + demande d'activation).
5. **Mobile** : écran admin de consultation des modules (l'activation/désactivation reste web ou API).
6. **Enforcement** : les `RequireFeature` guards (§G5.4) et le backend refusent l'accès à un module désactivé (403 + message).

**DoD :** CRUD complet ✅ · enforcement testé (un module désactivé est refusé web+mobile+API) · tests IDOR.

**Commit :** `feat(tenant): complete tenant feature CRUD and enforcement (29)`

---

## Étape G1.4 — §30-31 : Plans SaaS & Quotas (CRUD complet + application réelle)

**Objectif :** rendre les plans vendables **administrables, publics et appliqués**, dans le cadre du **modèle Dual-Market validé** (Annexe F) : 4 plans, prix EUR ◈ FCFA selon la région du visiteur, quotas + crédits IA par plan.

**Tâches :**
1. **Modèle** : quasi-totalité présente (`SaasPlan`). Ajouter ce qui manque : `is_public`, `features_json`, `seats_limit`, `storage_limit_mb`, `modules_included_json`, `ai_credits_limit`, `price_eur`, `price_xaf`, `price_usd`, `billing_period`, `trial_days`, `annual_discount_pct` (17 % = 2 mois offerts), `regions_json` (production Europe/Afrique/Amériques), `status`.
2. **API Super Admin** : CRUD complet des plans + contrôle des prix (devise normalisée EUR/XAF), historique de version des plans ; activation desktop du plan DÉCOUVERTE (gratuit) pour la diffusion virale.
3. **Affectation** : API d'assignation plan↔tenant (admin plateforme), suivi de la consommation : `GET /api/saas/usage` (membres, espaces, stockage, événements actifs, **crédits IA consommés / mois**) par rapport aux quotas.
4. **Enforcement quotas** (web+mobile+API) : membres, espaces, stockage, **crédits IA**, événements actifs → dépassement = 403 explicite + notification admin (dégradation gracieuse : lecture OK, écriture bloquée ; pour l'IA : file d'attente avec message « quota IA atteint »).
5. **Géo-prise en charge du prix** : détection de la région (IP/paramètre) → affichage **EUR (Europe) / FCFA (Afrique) / USD (Amériques & international)** et moyens de paiement associés (carte/SEPA en Europe ; MTN MoMo/Orange Money en Afrique ; carte/Stripe aux Amériques) ; le prix facturé est gelé au moment de la souscription.
6. **Page vitrine publique** : `/pricing` (4 plans : DÉCOUVERTE / DÉMARRAGE / CROISSANCE / RÉSEAU & CAMPUS — cf. Annexe F), badge marketing « 🤖 IA incluse » (fer de lance, cf. G6.2), essai 30 j, mention « annuel = 2 mois offerts », CTA vers l'onboarding (§G1.5).
7. **Tests** : calcul de quota (y compris crédits IA), cycle complet trial→payant→dépassement→downgrade avec prorata, géo-pricing (IP UE → EUR ; IP CM → FCFA ; IP US → USD).

**DoD :** §30-31 ✅ · quotas réellement appliqués (membres +10 % marge, espaces, stockage, **crédits IA**) · `/pricing` en ligne avec 4 plans géo-pricing · tests de dégradation gracieuse ✅.

**Commit :** `feat(saas): dual market plans quotas ai credits and pricing page (30-31)`

---

## Étape G1.5 — §50-51 : Onboarding & création de sous-église / campus (wizard)

**Objectif :** le pasteur principal crée une sous-église ou un campus sans passer par le super admin ; onboarding guidé pour toute nouvelle église.

**Tâches :**
1. **API existante** `/admin/invitations` + `OrganizationHierarchyController` → compléter pour :
   - `POST /api/org/campus` (pasteur principal) : création nœud CAMPUS/ORGANIZATION sous le tenant, avec rattachement d'un pasteur de campus (§G4.3) et jeu de modules par défaut ;
   - validation des droits (seul `PASTOR_PRINCIPAL` ou admin tenant).
2. **Backend** : chaine « invitation → création compte → rattachement au campus → export settings hérités ».
3. **Web** : wizard d'onboarding en 6 étapes pour une nouvelle église : 1) profil église (settings §G1.2) → 2) structure (organisation + campus + ministères de départ) → 3) modules de départ → 4) rôles/équipe (pasteur, admin, responsable) → 5) import initial (optionnel, §G4.5) → 6) activation plan (§G1.4).
4. **Mobile** : mini-wizard d'accueil post-login pour premier tenant (au moins étapes 1, 2, 4).

**DoD :** création sous-église par pasteur principal ✅ · wizard complet ✅ · restrictions de droits testées.

**Commit :** `feat(org): onboarding wizard and campus creation (50-51)`

---

## Étape G1.6 — §52 : Invitations — workflow complet (email + page d'acceptation + token)

**Objectif :** une invitation suit tout son cycle de vie, de l'email à l'activation du membre dans son espace.

**Tâches :**
1. **Backend** : table `invitation` (`id`, `tenant_id`, `organization_unit_id`, `email`, `role_code`, `token_hash`, `expires_at`, `status`, `invited_by`, `accepted_at`, `created_at`) — l'email d'invitation est **envoyé réellement** (SMTP configurable — `[BUSINESS_INPUT]`) ; lien = page publique d'acceptation.
2. **Cycle de vie** : PENDING → ACCEPTED / EXPIRED / REVOKED ; réinvitation avec rotation de token ; anti-rejeu, expiration 72h, rate-limit.
3. **Page web publique** `/accept-invitation?token=…` : pré-remplit le compte, demande confirmation des données, crée l'utilisateur + `Membership` + inscription automatique au **répertoire de l'église** (§G3.1) + rattachement au rôle/unité proposé.
4. **Mobile** : page d'acceptation Flutter consommant la même API (au minimum : acceptation et arrivée dans son espace).
5. **Tests** : expiration, rejeu, mauvais token, invitation cross-tenant impossible (IDOR).

**DoD :** email réellement envoyé (vérifiable en staging) · acceptation côté web + mobile · tests cycle de vie ✅.

**Commit :** `feat(tenant): complete invitation lifecycle with real email (52)`

## Étape G1.7 — §53 : Héritage de configuration (DEFAULT / INHERITED / OVERRIDDEN)

**Objectif :** toute configuration (settings, branding, modules, statuts, permissions) suit une chaîne d'héritage tenant → organisation → campus → ministère → département → famille → sous-équipe, chaque nœud pouvant **redéfinir** (`OVERRIDDEN`) ou **hériter** (`INHERITED`).

**Tâches :**
1. **Modèle** : sur `organization_unit` et sur les tables de config : champ `config_source` (`DEFAULT | INHERITED | OVERRIDDEN`) + `resolved_config_json` calculé (résolution en profondeur ; un `OVERRIDDEN` stoppe la remontée).
2. **Moteur de résolution** : `ConfigurationResolver` (service backend) avec cache invalidé par événement `config-changed` (outbox §G2.8) ; API `GET /api/config/resolved?unitId=&keys=` (RBAC scopé).
3. **UI** : dans chaque écran de config d'espace (§G2.6/G2.7), bandeau « Hérité de : [parent] » + bouton « Personnaliser » (→ OVERRIDDEN) / « Rétablir héritage ».
4. **Tests** : chaîne sur 3 niveaux + cas limite (parent supprimé → remontée au grand-parent).
5. **Mobile** : lecture résolue uniquement (édition côté web/API).

**DoD :** résolution testée · UI bandeau héritage ✅ · invalidation par événement ✅.

**Commit :** `feat(config): config inheritance DEFAULT INHERITED OVERRIDDEN (53)`

---

## Étape G1.8 — §54 : Ressources GLOBAL / LOCAL

**Objectif :** distinguer les ressources partagées (`TENANT_GLOBAL`) des ressources propres à une unité (`ORGANIZATION_LOCAL`, `UNIT_LOCAL`).

**Tâches :**
1. **Modèle** : champ `scope` + `organization_unit_id` (nullable = global) sur assets, événements, documents, médias, modèles.
2. **Règles** : GLOBAL visible/éditable selon permissions héritées ; LOCAL visible uniquement dans son unité et descendants ; filtrage systématique `scope + unit_id` en requête (jamais uniquement par scope).
3. **UI** : badge 🌐 global / 📍 local + sélecteur de portée + filtre.
4. **Tests** : un membre d'un département ne voit pas les ressources LOCALES d'un autre département.

**DoD :** §54 ✅ · tests de cloisonnement unité ✅.

**Commit :** `feat(core): global/local resource scoping (54)`

---

## Étape G1.9 — §43 : Impersonation Super Admin (UI + workflow complet)

**Objectif :** le super admin plateforme peut « prendre la place » d'un utilisateur (diagnostic) avec **journalisation** et **sans élévation de privilège**.

**Tâches :**
1. **Backend** : compléter `ImpersonationController` → validation super admin, JWT `impersonation` (révocable, TTL court, sans élévation), trace `impersonation_audit` (qui/quoi/quand/IP/durée).
2. **Règles d'intégrité** : re-validation mot de passe super admin pour les actions à risque (suppression tenant, changement de plan, secrets).
3. **Web** : bandeau permanent « 👁 Impersonation de X — Quitter » + accès depuis la fiche utilisateur (Super Admin).
4. **Mobile** : bandeau d'impersonation + sortie.
5. **Tests** : l'impersonateur n'obtient que les permissions de la cible ; journal complet ; impossible d'impersoner un super admin.

**DoD :** §43 ✅ · journal rempli · tests d'élévation négatifs ✅.

**Commit :** `feat(admin): complete super admin impersonation workflow (43)`
## Étape G1.10 — §44-45 : Security Matrix + extension des tests multi-tenant

**Objectif :** rendre la sécurité **vérifiable par matrice** : ressource × action × rôle × scope, chaque cellule prouvée par un test automatisé.

**Tâches :**
1. **Matrice** : `docs/security/SECURITY_MATRIX.md` — lignes ressource/action, colonnes rôle/scope, cellule `AUTORISÉ/REFUSÉ`. Ressources : members, departments, events, assets, finance, settings, invitations, impersonation, custom fields, workflow, dress code, pastoral (confidentialité).
2. **Base de tests** : étendre `MultiTenantSecurityTests` : cross-tenant, cross-scope, escalation (membre→responsable), IDOR, mass assignment, module désactivé, accès pastoral par membre.
3. **Génération** : un test par ligne de matrice (`@ParameterizedTest`). Cellule rouge ⇒ étape bloquée.
4. **CI** : la matrice tourne dans `mvn verify`.

**DoD :** matrice complète · 100% des cellules ⚠️ éliminées · CI verte.

**Commit :** `test(security): full security matrix automation (44-45)`

---

## Étape G1.11 — §46 : AuthorizationService appliqué à tous les contrôleurs sensibles

**Objectif :** plus aucun `if (role == …)` ad hoc ; tout passe par `AuthorizationService` (RBAC scopé) via annotation/guard centralisé.

**Tâches :**
1. Inventaire : `grep -rE "getRole|hasRole|isAdmin|ROLE_" backend/src/main/java/com/discipolat/modules/*/api`.
2. Remplacer chaque vérification ad hoc par `AuthorizationService.can(actor, action, resource, scope)` ; documenter chaque remplacement dans la fiche de suivi.
3. **Non-régression** : la Security Matrix reste verte après chaque remplacement (par paquet de contrôleurs, pas en un seul commit géant).
4. Annoter `@PublicEndpoint` (ou équivalent) les routes publiques légitimes (auth, acceptation invitation, pricing).

**DoD :** 0 autorisation ad hoc sur contrôleurs sensibles · matrice verte.

**Commit :** `refactor(security): centralize authorization via AuthorizationService (46)`

---

## Étape G1.12 — Verrou G1 (Gate)

**Checklist de sortie G1 :**
- [ ] §27-28, 30-31 : settings/branding + plans + quotas ✅ (web+mobile+API)
- [ ] §29 : tenant_features CRUD + enforcement ✅
- [ ] §43, 44-45, 46 : impersonation + matrice + AuthorizationService ✅
- [ ] §50-51, 52 : onboarding + invitations ✅ (email réel)
- [ ] §53, 54 : héritage + GLOBAL/LOCAL ✅
- [ ] Security Matrix verte, CI verte
- [ ] DoD global satisfait sur toutes les étapes de la porte

**Commit :** `chore: Gate G1 multitenancy contracts green`
# PORTE G2 — CŒUR CHURCH OS (Configuration & Moteurs fondamentaux)

> **Objectif central :** `ONE CORE, MANY CHURCHES, INFINITE CONFIGURATIONS`. Chaque département/famille est une **configuration**, jamais du code. Les étapes G2.1→G2.10 sont la colonne vertébrale ; elles portent un **contrat de données contractuel** (annexe A) que l'agent doit respecter à la lettre.
> **Méthode d'intégration des ~130 modules existants** (Q2 validée) : **façade par déclaration** — chaque module existant est déclaré dans le catalogue comme `source = EXISTING`, et le routage vers son implémentation actuelle est assuré par feature-flag. On n'unifie les doublons que lorsqu'un espace réel les consomme.

## Étape G2.1 — OrganizationUnit généralisée & hiérarchie infinie

**Objectif :** `organization_unit` devient un arbre universel multi-niveaux (CAMPUS, MINISTRY, DEPARTMENT, SUB_DEPARTMENT, TEAM, CELL, GROUP, FAMILY…) sans profondeur figée.

**Tâches :**
1. **Migration** : compléter `organization_unit` : `id, tenant_id, parent_id, name, code, type (enum flexible), description, status, icon, color, sort_order, config_source, created_at, updated_at` + index `(tenant_id, parent_id)` et `(tenant_id, type)`. Préserver les données existantes (les lignes actuelles restent valides).
2. **Backend** : service arbre : `children()`, `descendants()`, `ancestors()`, `move()` (avec revalidation des cycles), `subtree()` ; suppression douce (soft delete) avec archivage.
3. **API** : `GET /api/org/tree` (RBAC scopé), `POST/PATCH/DELETE /api/org/units` (+ type FAMILY autorisé dès cette étape pour préparer G4).
4. **Tests** : cycles interdits, profondeur >= 7 niveaux OK, isolation tenant (deux églises = deux arbres).

**DoD :** arbre universel ✅ · move/delete testés · données existantes préservées.

**Commit :** `feat(core): generalized organization unit hierarchy`

---

## Étape G2.2 — Module Engine & catalogue de modules (façade sur l'existant)

**Objectif :** un catalogue unique `module_definition` que chaque espace consomme ; les ~130 modules existants y sont **déclarés sans modification de leur code interne**.

**Tâches :**
1. **Migration** : `module_definition` (`id, code, name, description, category, version, enabled, icon, source [CORE|EXISTING|ENGINE], features_json`), `department_module` → renommer concept en **`space_module`** (`space_id` polymorphique) avec `enabled, configuration_json, display_order`.
2. **Déclaration du catalogue** : à partir de l'audit G0.1, déclarer les ~130 modules backend existants comme `ModuleDefinition(source=EXISTING)` ; déclarer les modules CORE (people, org, events, notifications, audit) et les modules ENGINE construits ici (config, workflow, custom_fields, dress_code, **health**, famille, media…).
3. **Routeur** : `ModuleRouter` (serveur) + configuration de features flags par tenant (G1.3) : un module `EXISTING` route vers l'implémentation actuelle ; un module `ENGINE` route vers le nouveau code. Aucun doublon de route.
4. **API** : `GET /api/modules/catalog`, `GET/PUT /api/spaces/:id/modules`, validation (un espace n'active que des modules du catalogue).
5. **Tests** : catalogue complet (comptage == inventaire G0.1), activation/désactivation, 404 si module inconnu.

**DoD :** catalogue déclaré ✅ · routeur feature-flag testé ✅ (un module EXISTING répond par l'ancien chemin, un ENGINE par le nouveau).

**Commit :** `feat(config): module catalogue and feature-flag router`

---

## Étape G2.3 — Template Engine (départements, familles, dress code)

**Objectif :** créer un espace en 1 clic depuis des templates métier, chacun **modifiable après création**.

**Tâches :**
1. **Migration** : `department_template` → `space_template` (`code, name, description, icon, color, modules_json, default_workflows_json, default_statuses_json, default_dashboards_json, version`).
2. **Templates à créer (seed)** : AUDIOVISUAL, CHOIR, LOGISTICS, FINANCE, PRAYER, CHILDREN, YOUTH, FAMILY, EVANGELISM, MAINTENANCE, BOOKSTORE, MEDIA, DISCIPLESHIP, ACCUEIL, PROTOCOLE, COORDINATION, INTERCESSION, SECURITE, COMMUNICATION, **HEALTH** (20 templates, avec modules typiques : ex. AUDIOVISUAL → people, teams, assets, inventory, maintenance, events, tasks, finance, reports, archive ; CHOIR → + rehearsal, repertoire ; ACCUEIL → + dress_code ; **HEALTH/Infirmerie → people, patients, consultations, pharmacie, gardes, campagnes médicales, kits — cf. G3.11**).
3. **API** : `POST /api/spaces/from-template` (validation droits : admin/pasteur pour tout espace ; §G2.6 pour les chefs) + clonage de template.
4. **UI** : « + Nouvel espace » → sélection template → aperçu des modules → création (wizard court).
5. **Tests** : un espace créé reçoit exactement les modules du template ; modification post-création OK.

**DoD :** 20 templates seedés ✅ · création par template ✅.

**Commit :** `feat(config): space template engine with 20 seed templates`
## Étape G2.4 — Custom Field Engine

**Objectif :** chaque espace (et chaque domaine configurable : personne, événement, asset, tâche…) reçoit des champs personnalisés **sans migration** par église. Types supportés : TEXT, LONG_TEXT, NUMBER, DECIMAL, BOOLEAN, DATE, DATETIME, TIME, SELECT, MULTI_SELECT, USER, PERSON, DEPARTMENT, SPACE, TEAM, FILE, IMAGE, URL, PHONE, EMAIL, CURRENCY (PRMPT §9).

**Tâches :**
1. **Modèle** : `custom_field_definition` (`id, tenant_id, entity_type, space_id nullable, field_code, label, field_type, required, options_json, validation_json, visibility_scope, display_order, created_at`) + `custom_field_value` (`id, tenant_id, field_id, entity_id, value_json, updated_by, updated_at`) — index `(entity_type, entity_id)`.
2. **Backend** : service CRUD (définition + valeurs) ; **validation backend systématique** selon `field_type` (regex, min/max, enum, référence PERSON valide dans le tenant) — aucune validation frontend seule.
3. **API** : `GET/POST/PUT/DELETE /api/custom-fields/definitions`, `GET/PUT /api/custom-fields/values/{entityType}/{entityId}` — RBAC scopé (gérer les définitions = admin espace ; remplir les valeurs = acteur autorisé).
4. **Rendu** : composant frontend « form générique » qui rend le formulaire à partir des définitions (réutilisé partout : fiche personne, asset, événement, membre d'équipe) ; éditeur de définition avec aperçu en direct.
5. **Sécurité** : une valeur `FILE/IMAGE` passe par le service de fichiers isolé (§39) ; les champs de type PASTORAL_SENSITIVE sont interdits (les données sensibles pastorales vivent dans §G3.8, pas en custom fields).
6. **Tests** : validation backend (type incorrect refusé), autorisation, isolation tenant.

**DoD :** 19 types fonctionnels · validation backend ✅ · rendu générique réutilisé sur 2+ domaines · isolation testée.

**Commit :** `feat(config): custom field engine with backend validation`

---

## Étape G2.5 — Workflow Engine (configurable, approbations, délais, escalade, auto-actions)

**Objectif :** chaque espace modélise ses processus (ex. « nouveau matériel → validation responsable → validation finance → achat → réception → inventaire ») **sans code**.

**Tâches :**
1. **Modèle (annexe A §A.5)** : `workflow_definition` (entity_type, code, name, space_id nullable, enabled, version) · `workflow_step` (order, type [APPROVAL|AUTO_ACTION|NOTIFY|EXPENSE|ASSET_STATUS|FORM]), conditions_json, assignee_role/scope, timeout_hours, escalation) · `workflow_transition` (from_step, to_step, event) · `workflow_instance` (entity_id, current_step, status, started_at, updated_at) · `workflow_task` (instance_id, step_id, assignee_id, status, due_at, resolved_at, comment).
2. **Exécution** : moteur de transition événementiel — événement entrant (outbox §G2.8) ou action utilisateur → avance du workflow → crée `workflow_task`, envoie notif, déclenche auto-actions (création dépense, changement statut asset) → délai dépassé → escalade au supérieur.
3. **API** : CRUD definitions (admin espace), `POST /api/workflows/instances`, `POST /api/workflows/tasks/{id}/approve|reject|comment|reset`.
4. **UI** : **éditeur visuel** de workflow (nœuds/transitions, drag & drop), volet « Mon approbation » (web + mobile), timeline d'instance.
5. **Tests** : 3 scénarios complets (approbation simple, rejet, escalade par timeout), permissions par étape, isolation.

**DoD :** moteur complet ✅ · éditeur visuel ✅ · 3 scénarios testés ✅.

**Commit :** `feat(workflow): configurable workflow engine with approvals and escalation`
## Étape G2.6 — Espaces configurables unifiés (Département = Famille = Sous-équipe)

**Objectif (exigence utilisateur) :** un DÉPARTEMENT, une FAMILLE ou une sous-équipe sont le **même objet** : un **Espace** (`space`) configurable par ses responsables. Droits de customisation :
- **Admin / Pasteur** → créent et configurent **tous** les espaces ;
- **Chef de département** → configure **son** département (fonctionnalités, pages, boutons, couleurs) ;
- **Chef de famille** → configure **sa** famille (suivi des âmes : visites, événements, réceptions, comptes-rendus).

**Tâches :**
1. **Modèle** : `space` (`id, tenant_id, organization_unit_id, space_type [DEPARTMENT|FAMILY|SUB_TEAM], template_code, name, code, icon, color, description, status, configuration_json, created_at, updated_at`) + `space_module` (G2.2). `space` et `organization_unit` restent liés (un espace vit dans l'unité qui l'accueille) sans fusion.
2. **Feature Picker** : composant « la plateforme propose des tonnes de fonctionnalités, je choisis ce que je veux » → catalogue des modules activables pour l'espace + toggles détaillés (pages, boutons, widgets) ; ordre modifiable par drag & drop.
3. **Personnalisation par rôle** : backend `canCustomize(actor, space)` (admin/pasteur = tout ; chef = son espace) exposé à l'UI ; chaque modification → événement `space-config-changed` (outbox).
4. **UI** : page « Configurer l'espace » (nom, icône, couleur, modules, pages, widgets, statuts, workflows, champs, rôles, dashboard) en onglets ; prévisualisation en direct.
5. **Propagation temps réel** : tout changement de config visible immédiatement (web ouvert + mobile connecté) — le cache config du client est invalidé par WebSocket (§G2.10).
6. **Tests** : chef de famille ne configure QUE sa famille ; admin configure tout ; un changement de couleur est visible sur un second client < 5s.

**DoD :** espace unifié ✅ · picker ✅ · matrice de droits G2.6 testée ✅ · propagation vérifiée ✅.

**Commit :** `feat(config): unified configurable spaces department family sub-team`

---

## Étape G2.7 — Custom Statuses & Statuts configurables

**Objectif :** aucun statut codé en dur par domaine métier : chaque espace définit ses statuts (ex. Audiovisuel : Préproduction→Production→Postproduction→Validé→Publié ; autre : À faire→En cours→Terminé).

**Tâches :**
1. **Modèle** : `custom_status_set` (`id, tenant_id, entity_type, space_id nullable, code, name, color, icon, order, initial, final, allowed_transitions_json`) — un statut est une configuration, pas un enum.
2. **Backend** : service transitions (une transition non déclarée est refusée), événement `status-changed` (outbox) pour audit + notif + workflows.
3. **Migration des enums existants** : là où le code actuel utilise des enums rigides pour des statuts métier **configurables**, exposer le statut via la configuration avec fallback aux valeurs actuelles (aucune donnée perdue).
4. **UI** : éditeur de statuts (kanban visuel, couleurs, transitions) + affichage dynamique des badges dans les listes.
5. **Tests** : transition interdite refusée, kanban groupe par statuts perso, héritage (G1.7) sur les statuts.

**DoD :** éditeur ✅ · transitions contrôlées ✅ · aucun enum rigide bloquant sur 2+ domaines ✅.

**Commit :** `feat(config): custom status engine with controlled transitions`
## Étape G2.8 — Event Bus & Transactional Outbox

**Objectif (PRMPT §23) :** toute mutation critique est **committée dans la même transaction** qu'un événement d'outbox, puis publiée de façon **fiable** (pas de perte si le consumer tombe).

**Tâches :**
1. **Modèle** : `outbox_event` (`id, tenant_id, aggregate_type, aggregate_id, event_type, payload_json, status [PENDING|PUBLISHED|FAILED|DISCARDED], attempts, created_at, published_at`), dispatcher polling/DB (ou Spring Integration si déjà présent — vérifier l'existant avant d'ajouter une techno).
2. **Événements canoniques (annexe E)** : AssetCheckedOut, AssetReturned, AssetDamaged, MaintenanceStarted, MaintenanceCompleted, ExpenseCreated, MemberTransferred, RoleAssigned, RoleEnded, EventCreated, TaskAssigned, TaskCompleted, AttendanceRecorded, PrayerSessionCompleted, SermonPublished, DressCodePublished, SpaceConfigChanged, StatusChanged, InvitationAccepted.
3. **Publication** : producteurs (services métier) écrivent outbox en transaction ; dispatcher publie (ex. Spring `ApplicationEventPublisher` + WebSocket) avec retry exponentiel et dead-letter.
4. **Consumers** : NOTIFY (notifications), AUDIT, BUSINESS_HISTORY, FINANCE (auto-création de dépense), ANALYTICS (compteurs), REALTIME (push clients).
5. **Idempotence** : chaque consumer enregistre `processed_events` (concurrence sûre) ; rejeu possible.
6. **Tests** : perte du consumer → événement retenté ; transaction échouée → aucun événement fantôme ; rejeu idempotent.

**DoD :** outbox opérationnelle · 20+ événements · consumers NOTIFY/AUDIT/HISTORY/FINANCE · tests de fiabilité ✅.

**Commit :** `feat(core): transactional outbox event bus`

---

## Étape G2.9 — Audit Engine ≠ Business History

**Objectif (PRMPT §21-22) :** deux systèmes distincts — « qui a modifié quoi » (audit) et « qu'est-il arrivé à l'objet métier » (historique), tous deux sécurisés.

**Tâches :**
1. **Audit technique** : `audit_event` (`id, tenant_id, actor_id, actor_email, action, entity, entity_id, old_value_json, new_value_json, ip, user_agent, timestamp`) → **hash chain** (champ `prev_hash` + `hash` calculé sur la ligne : un record falsifié casse la chaîne — vérifié périodiquement) ; rétention paramétrable (95 jours par défaut) + **export** (JSON/CSV chiffré) ; suppression interdite (seul un admin plateforme peut archiver → `audit_archive`).
2. **Historique métier** : `business_history` générique (`id, tenant_id, object_type, object_id, event_type, summary, detail_json, actor_id, actor_role, happened_at, space_id`) + vues dédiées pour : asset_history, role_history, membership_history, event_history, financial_history, config_history, dresscode_history (une table générique + vues = plus simple et évolutif).
3. **API** : `GET /api/audit?entity=&entityId=` (RBAC scopé, admin espace/tenant), `GET /api/history/{objectType}/{objectId}` (avec filtre par plage de dates) ; UI timeline « Historique » dans chaque fiche métier.
4. **UI** : timeline double (accès audit visible uniquement aux admins ; historique visible selon scope).
5. **Tests** : hash chain (modification d'un ancien enregistrement détectée), isolation, permissions de consultation.

**DoD :** audit + history opérationnels · hash chain vérifiée par un test · export ✅.

**Commit :** `feat(audit): audit engine with hash chain and business history`

---

## Étape G2.10 — Real-time Engine (WebSocket ciblé + notifications)

**Objectif (PRMPT §24, §41) :** les changements critiques sont propagés immédiatement (inventaire, tâches, événements, config, rôles, notifications).

**Tâches :**
1. **WebSocket** : s'appuyer sur l'existant (`WebSocketConfig`, `TenantDestinationUserNameProvider` à préserver) → destinations typées par **tenant/user/space** ; s'abonner aux événements d'outbox (§G2.8) ; tests d'isolation (un client ne reçoit jamais les événements d'un autre tenant) — **priorité haute** (c'est un §40 du À faire).
2. **Notifications** : moteur multi-canal (in-app via WS, push mobile via Firebase déjà présent dans `mobile/pubspec.yaml`, email pour les cas asynchrones) ; `notification_rules` configurables par espace (G2.6) ; centre de notifications web + mobile ; badges en temps réel.
3. **Scopes de réception** : personne, son espace, ses rôles, tout le tenant pour les admins (aucune fuite cross-tenant).
4. **Web** : client WS avec reconnexion + backoff, échec silencieux (mode online/offline visible), rafraîchissement des données d'un écran en cours d'affichage.
5. **Mobile** : canal WS (`web_socket_channel` présent) pour les écrans actifs + réveil par notification push.
6. **Tests** : isolation §40 ✅, propagation config < 5s, reconnexion sans perte de données.

**DoD :** WS isolé ✅ · notifications multi-canal ✅ · tests isolation - reconnexion ✅.

**Commit :** `feat(realtime): realtime engine and multi-channel notifications (40-41)`

---

## Étape G2.11 — Verrou G2 (Gate)

**Checklist de sortie G2 :**
- [ ] G2.1 arbre universel ✅ · G2.2 catalogue + routeur ✅ · G2.3 templates ✅
- [ ] G2.4 custom fields ✅ · G2.5 workflows ✅ · G2.7 statuts ✅
- [ ] G2.6 espaces unifiés + droits de customisation ✅
- [ ] G2.8 outbox ✅ · G2.9 audit/history ✅ · G2.10 realtime ✅
- [ ] migrations annexe A appliquées · Security Matrix toujours verte
- [ ] DoD global sur toutes les étapes de la porte

**Commit :** `chore: Gate G2 church os core engines green`
# PORTE G3 — MOTEURS DOMAINE (réutiliser l'existant, construire le manquant)

> **Méthode :** pour chaque moteur, l'agent démarre par l'inventaire G0.1 (le domaine existe-t-il déjà, partiellement ou pas ?). **On ne réécrit pas ce qui fonctionne** ; on l'enveloppe ou on le complète. Chaque étape produit son contrat fonctionnel + DDL validé (approche hybride Q5) et respecte la règle de livraison PRMPT §38.

## Étape G3.1 — People Engine : identité unique + inscription automatique

**Objectif (exigence utilisateur) :** une personne = **une seule fiche** dans l'église. Quand un membre s'inscrit au nom d'une église, il est **immédiatement ajouté au répertoire** de tous les utilisateurs ; les responsables le voient dans la liste **« sans espace »** et peuvent l'ajouter à leur département/famille.

**Tâches :**
1. **Modèle** : `person` (identité, annexe A §A.3) unique par tenant (clé de dédoublonnage `email` + `phone` normalisé) ; `membership` (statut, dates, source).
2. **Inscription** : au self-signup (web + mobile), après vérification (email/téléphone), création **transactionnelle** de : compte + `person` + `membership(statut = MEMBRE/Nouveau)` → **événement `MemberRegistered`** (outbox) → la personne apparaît dans le répertoire et dans la liste « Sans espace » (filtre `space_membership IS EMPTY`).
3. **Répertoire** : `GET /api/people` (RBAC scopé) avec recherche/ filtres (campus, statut, sans espace, sans famille) — base pour les affectations.
4. **Affectation par responsable** : un chef de département/famille ou admin/pasteur **sélectionne dans la liste des utilisateurs sans espace** et l'affecte (→ `space_membership`, §G3.2) ; l'utilisateur reçoit une notification « Vous avez été ajouté à [Espace] par [X] ».
5. **Dédoublonnage** : détection de doublons (même email/phone) avec fusion assistée (l'admin choisit la fiche maîtresse) — jamais deux fiches pour la même personne.
6. **Tests** : inscription → visible dans le répertoire d'un second compte en temps réel ; un chef n'affecte que dans ses espaces ; dédoublonnage.

**DoD :** inscription auto ✅ · liste sans espace ✅ · affectation par responsable ✅ web+mobile · tests ✅.

**Commit :** `feat(people): unique people registry with auto self-registration`

---

## Étape G3.2 — Membership, SpaceMembership & RoleAssignment (3 dimensions + historisation)

**Objectif (PRMPT §11-15) :** distinguer **fonction permanente** (rôle dans l'organisation) / **fonction d'espace** (appartenance) / **fonction événementielle** (affectation à un événement), toutes historisées (qui dirigeait l'audiovisuel en 2023 ?).

**Tâches :**
1. **Modèle** : `space_membership` (`id, tenant_id, person_id, space_id, joined_at, left_at, status, membership_type, responsibility, notes`) ; `role_assignment` (`id, tenant_id, person_id, org_unit_id, role_id, started_at, ended_at, status, reason`) ; `role` + `permission` + `role_permission` (catalogue) ; `event_assignment` (G3.3).
2. **Rôles du catalogue (seeds)** : PASTOR_PRINCIPAL, PASTOR_CAMPUS, ADMIN_TENANT, DEPARTMENT_HEAD, DEPARTMENT_MEMBER, FAMILY_HEAD, FAMILY_MEMBER, MAKER_DISCIPLE (faiseur), SUPER_ADMIN (+ permissions associées avec scopes). **Le rôle MAKER_DISCIPLE est activé d'office par les templates FAMILY et DISCIPLESHIP (G2.3)** à la création de l'espace.
3. **Historisation** : jamais d'écrasement : clôturer (`ended_at`, `status=ENDED`) puis recréer une nouvelle affectation (le cas « Jean était chef streaming 01/01→30/06 puis chef vidéo » doit être reconstituable).
4. **Choix d'affectation** : wizard « Affecter une personne » (personne → espace → rôle → dates → motif) ; vue « Affectations passées » dans les fiches personne/espace.
5. **Événements** : `RoleAssigned`, `RoleEnded`, `MemberTransferred` (transfert avec motif), `SpaceMembershipChanged` → consumers NOTIFY + HISTORY + REALTIME.
6. **Tests** : reconstitution d'historique, transfert sans écrasement, permissions selon l'affectation actuelle.

**DoD :** 3 dimensions ✅ · historisation ✅ · wizard d'affectation ✅ · transfert ✅.

**Commit :** `feat(people): memberships role assignments and history`

---

## Étape G3.3 — Event Engine transversal

**Objectif (PRMPT §13, 16-18) :** l'événement est central : il relie espaces, personnes, tâches, assets, budget, dépenses, lieux, présence, documents — vue opérationnelle WHO/WHAT/WHERE/WHEN/STATUS.

**Tâches :**
1. **Modèle (annexe A §A.8)** : `event` (+ locations), `event_space` (N:N espaces), `event_team`, `event_assignment` (personne + rôle + location + créneau), `event_task`, `event_asset`, `event_expense`, `event_attendance`, `event_document`, `event_schedule`.
2. **API** : CRUD événement + création à partir d'un espace ou multi-espaces ; calendrier (`GET /api/events/calendar?from=&to=&spaceId=`).
3. **Affectation événementielle** : assigner des personnes à des postes (ex. Caméra 1 = Paul) **sans modifier** leurs fonctions permanentes ; vue de planning visuel (par lieu : SCÈNE / RÉGIE / STREAMING / ENTRÉE / PARKING) avec statuts 🟢/🟠/🔴 (À l'heure / En retard / Absent).
4. **Présence** : check-in (web + mobile + scan QR badge) **+ flash de secours par numéro de téléphone** (saisie rapide, pour membres sans smartphone évolué) ; `AttendanceRecorded` (outbox) → BIRT côté discipleship/analytics.
5. **Documents/médias** : rattachement isolé §39 ; historique (G2.9) sur l'événement.
6. **Tests** : un événement multi-espaces, assignations distinctes des rôles permanents, présence, isolation.

**DoD :** calendrier ✅ · planning visuel par lieu ✅ · check-in ✅ · affectations événementielles indépendantes ✅.

**Commit :** `feat(event): transversal event engine with planning and check-in`
## Étape G3.4 — DRESS CODE & PATRIMOINE ÉVÉNEMENTIEL (exigence utilisateur)

**Objectif :** chaque espace (Accueil, Chorale, Enfants, Protocole…) programme des **dress codes** par événement/service et par groupe ; les membres les voient dans leur compte (web + mobile) **avec notifications** ; tout (dress code, responsables internes, répertoires musicaux, debriefs) est **archivé** et consultable.

**Modèle de données (annexe A §A.9) :**
- `dress_code` (`id, tenant_id, space_id, event_id nullable, service_name, title, begins_at, ends_at, status, created_by, archived`) ;
- `dress_code_rule` (`dress_code_id, group_name [ex. "Hommes", "Femmes", "Garçons", "Filles", "Lead", "Chœurs"], description, image_url`) ;
- `dress_code_audience` (`dress_code_id, audience_type [SPACE_MEMBER|ROLE|GROUP_NAME], role_code nullable`) → cible = qui reçoit la notification ;
- `event_repertoire` (`event_id, space_id, song_title, order, role_or_pupitres_json [ex. Lead: Andréas ; Soprano: Francs…], notes`) ;
- `event_debrief` (`event_id, space_id, what_went_well, what_failed, improvements, service_dates_json, created_by`) ;
- `space_internal_role` (`space_id, person_id, internal_role_code [RESPONSABLE_TECHNIQUE_VOCALE, LEAD, SOPRANO…], title, started_at, ended_at`) — pour « responsables internes » historisés.

**Tâches :**
1. **Backend + migrations** : les 5+2 tables ci-dessus (index `(tenant_id, space_id)`, `(tenant_id, event_id)`), CRUD scopés, validation (group_name requis, dates cohérentes, event doit appartenir au même tenant).
2. **Publication & notification** : `POST /api/dress-codes/publish` → événement `DressCodePublished` (outbox) → NOTIFY aux destinataires (audience) → push mobile + in-app + badge.
3. **Visibilité membre** : API « Ma tenue » (`GET /api/me/dress-codes?from=`) filtrée par l'appartenance espace/rôle de l'utilisateur — un membre ne voit que les dress codes des espaces dont il fait partie.
4. **Archives intégrales** : archivage de l'événement complet (dress codes + répertoire + debrief + responsables de service) versionné au clôturage ; écran « Archives » consultable par années/mois/espace (ex. « 4 mars 2023 — Séminaire des célibataires 14h-19h, Orateurs : Pasteur Étienne, Pasteur Mira, Dress code Accueil… »).
5. **Répertoires musicaux** : saisie par événement + répertoire courant par espace (Chorale) ; historique des chants exécutés par date ; consultation archivée.
6. **Debrief/Coordination** : écran post-événement « Qu'est-ce qui n'a pas marché ? Qui était de service à telle date ? » → `event_debrief` + recherche d'affectations par date ; alerte au responsable coordination si débrief manquant sous 48h.
7. **UI web** : pages Espace → « Dress Code » (création type calendrier + aperçu « ce que verront les membres »), « Répertoire », « Debrief », « Archives » (toutes générées par l'UIConfig G2.6 → activables/désactivables par le responsable).
8. **UI mobile** : écran « Ma tenue » (prochains événements + tenue à porter), notification push à la publication, saisie rapide de présence/post-mortem terrain.
9. **Tests** : ciblage audience (seule la chorale reçoit), archivage rejouable, isolation tenant, un membre ne voit pas le dress code d'un espace dont il ne fait pas partie.

**DoD :** dress codes programmés/consultés/notifiés ✅ · archives consultables (exemples de l'exigence : service dimanche, campagne stade Olembé, camp de couples 2022) ✅ · répertoires + debrief ✅.

**Commit :** `feat(dresscode): dress code engine repertoire and event archives`
## Étape G3.5 — Asset Engine (Assets, Inventaire, Checkout, Maintenance, Transferts, TCO)

**Objectif (PRMPT §19-24) :** distinguer `asset` (bien identifié : Console Yamaha, SN XXX) de `inventory_item` (stock générique : 100 câbles XLR) ; cycle de vie complet + **historique non écrasé** + **coût total de possession (TCO)**.

**Tâches :**
1. **Modèle (annexe A §A.10)** : `asset` (identity, purchase, ownership, location, status, warranty, lifecycle), `inventory_item` (+ mouvements de stock `inventory_movement`), `asset_checkout` (sortie → événement/personne, condition avant/après, dates), `maintenance_record` (PREVENTIVE/CORRECTIVE/REPAIR/INSPECTION, diagnostic, technicien, coûts, pièces, documents), `asset_transfer` (département→église annexe / église→église / don — tout reste dans l'historique), `asset_expense` (lien dépense→asset pour le TCO).
2. **TCO** : agrégation (achat + transport + maintenance + réparations + pièces + assurance) affichée par asset ; lien vers Finance (§G3.6) via `Expense.asset_id`.
3. **QR** : génération QR par asset (identifiant signé, tenant-aware), scan mobile (§G5.6) pour checkout/retour/état.
4. **Workflow d'auto-propagation (PRMPT §35-36)** : « Console retournée endommagée » → un événement `AssetReturned(condition=DAMAGED)` crée automatiquement le ticket maintenance, prévient le chef, crée une dépense potentielle, met à jour le statut et archive. **L'utilisateur ne fait pas 5 allers-retours** : le moteur abaisse les actions des consumers (outbox §G2.8).
5. **Inventaire** : mouvements entrée/sortie, seuils (alerte bas stock), inventaire préventif.
6. **UI web** : fiche asset (timeline complète), balayage de retour avec photo dommage (mobile), liste inventaire (virtualisée), tableau de bord TCO.
7. **Tests** : cycle complet, transfert historique, TCO exact, workflows automatiques.

**DoD :** asset/inventory/checkout/maintenance/transfert ✅ · TCO ✅ · QR+scan ✅ · workflw automatique testé ✅.

**Commit :** `feat(asset): full asset lifecycle tco and QR checkout`

---

## Étape G3.6 — Finance Engine central (une seule finance pour toutes les dépenses)

**Objectif (PRMPT §25-27) :** PAS de « finance audiovisuel » séparée : un **cœur financier** auquel pointent tous les domaines (dépense → espace/événement/asset/budget/fournisseur).

**Tâches :**
1. **Modèle (annexe A §A.11)** : `financial_account`, `financial_transaction`, `expense` (+ liens `space_id/event_id/asset_id/supplier_id/budget_id`), `income`, `donation`, `contribution`, `budget`, `budget_line`, `payment`, `ledger_entry`, `reconciliation` — réutiliser les tables `finances` existantes et les compléter par migration (vérifier d'abord G0.1).
2. **Règles d'intégrité** : double écriture interdite (une dépense est créée une fois, source traçable), écritures (ledger) en double entrée, rapprochement bancaire, devise du tenant (settings §G1.2), taux (module currency existant).
3. **Paiements** : paiements internes + **Mobile Money** (MTN MoMo, Orange Money — adapters par provider, sandbox en staging) `[BUSINESS_INPUT]` ; reçu/justificatif PDF logo tenant.
4. **Budgets par espace/événement** : budget, lignes, suivi consommation (dashboard), alertes dépassement.
5. **Automatisation** : événements domaines (AssetDamaged→dépense, EventCreated→lignes budgétaires, Maintenance→coûts) via consumers outbox ; une dépense liée à un asset alimente le TCO (§G3.5).
6. **Rapports** : rapport mensuel par espace/catégorie, export Excel/PDF, rapprochement ; accès strict (finance = rôle dédié + scope tenant, jamais membe simple).
7. **Tests** : immutabilité du ledger (pas de masquage), montants arrondis (2 décimales, pas de flottant), isolation tenant, IDOR finance.

**DoD :** cœur unique ✅ · budgets ✅ · Mobile Money sandbox branché ✅ · rapports ✅ · tests d'intégrité ✅.

**Commit :** `feat(finance): central finance engine with budgets and mobile money`
## Étape G3.7 — Discipleship Engine configurable (parcours, étapes, mentors, groupes)

**Objectif (PRMPT §28-30) :** chaque église définit SON parcours (Visiteur→Converti→Nouveau croyant→Disciple→Serviteur→Leader, OU un autre). **Aucun enum codé en dur.**

**Tâches :**
1. **Modèle (annexe A §A.12)** : `faith_journey` (person), `journey_definition`, `journey_stage_definition` (order, color, icon), `journey_transition` (conditions), `journey_event` (historique des étapes franchies), `discipleship_group` (groupe/famille de disciple), `discipleship_group_member`, `mentor_assignment` (faiseur ↔ disciple, dates).
2. **Gestion par l'église** : éditeur de parcours (étapes, transitions, statuts visuels via G2.7) ; les groupes/familles de disciple sont des espaces `FAMILY` (G2.6) → rattachables à un département, un ministère ou l'église (`responsible_organization_unit_id`).
3. **Faiseur de disciples** : assignation mentor↔disciple avec suivi ; le statut de « faiseur » est un **rôle vivant** (§G4.4) : dès que la personne devient faiseur, son interface s'enrichit (onglet « Mes disciples ») automatiquement.
4. **Événements** : `JourneyStageChanged`, `MentorAssigned` → notifications + history.
5. **UI** : fiche personne (parcours, étapes, mentor), vue globale « progression », tableau de suivi par responsable.
6. **Tests** : deux églises = deux parcours distincts, transitions contrôlées, roles.

**DoD :** parcours configurable ✅ · familles = espaces ✅ · mentorat ✅ · historique ✅.

**Commit :** `feat(discipleship): configurable discipleship journey engine`

---

## Étape G3.8 — Pastoral Care sécurisé

**Objectif (PRMPT §31, 18) :** suivi pastoral **séparé** des données publiques, confidentialité stricte, audit d'accès.

**Tâches :**
1. **Modèle (annexe A §A.13)** : `pastoral_case` (concerned_person, assigned_to, status, confidentiality_level [STRICT|INTERNAL|GENERAL], visibility_scope), `pastoral_interaction` (notes sensibles), `pastoral_action`, `follow_up` (rappels).
2. **Accès** : seuls les assignés + rôles dédiés (`PASTOR`, `PASTORAL_TEAM`) selon `confidentiality_level` ; **chaque consultation = audit_event** (« qui a lu la fiche de qui, quand ») ; jamais exposé via custom fields (G2.4 l'interdit) ni via une API de membre.
3. **UI** : fiche confidentielle différenciée (fond distinct), verrou de lecture, historique d'accès visible par le pasteur principal.
4. **Tests** : un membre ne peut jamais lire/bruter une fiche pastorale ; l'audit d'accès est inaltérable.

**DoD :** confidentialité + audit d'accès ✅ · séparation stricte ✅.

**Commit :** `feat(pastoral): secured pastoral care with access audit`

---

## Étape G3.9 — Prayer Engine (programmes, sessions, créneaux, requêtes, jeûnes)

**Objectif (PRMPT §32, 19) :** programmation de prière par créneaux (ex. 08:00→10:00 Paul ; 10:00→10:30 Marie), requêtes, jeûnes, archives par mois/année.

**Tâches :**
1. **Modèle (annexe A §A.14)** : `prayer_program`, `prayer_session`, `prayer_slot` (assignations personne + créneau + statut), `prayer_request` (avec confidentialité optionnelle), `fasting_program`.
2. **API/UI** : planification visuelle des créneaux ; écran « Mes créneaux » mobile ; requêtes anonymes possibles ; archivage mensuel des programmes (clôture → historique).
3. **Événements** : `PrayerSessionCompleted` → history + analytics.
4. **Tests** : chevauchement de créneaux pour une même personne interdit, confidentialité des requêtes.

**DoD :** planification ✅ · requêtes ✅ · archives ✅.

**Commit :** `feat(prayer): prayer programs slots and requests`

---

## Étape G3.10 — Media / Sermon Engine

**Objectif (PRMPT §33, 20) :** prédication riche (speaker, passage biblique, résumé, YouTube, audio, vidéo, photos, documents, culte associé), médiathèque et documents, archives.

**Tâches :**
1. **Modèle (annexe A §A.15)** : `sermon` (+ liens event/speaker/verse/résumé/urls), `media_asset`, `media_collection`, `document` — réutiliser les tables `sermon`/`media` existantes (vérifier G0.1), compléter par migration.
2. **API/UI** : fiche prédication, upload/image/audio via stockage isolé §39 ; recherche plein texte ; collection (série).
3. **Archives** : catalogue par année, par prédicateur, par passage biblique.
4. **Tests** : liens intègres, isolation fichiers.

**DoD :** sermon riche ✅ · médiathèque ✅ · recherche ✅.

**Commit :** `feat(media): sermon and media engine`

---

## Étape G3.11 — Santé / Infirmerie (gestion hospitalière, campagnes médicales, kits)

**Objectif (exigence utilisateur) :** l'espace SANTÉ/INFIRMERIE dispose des fonctionnalités d'un logiciel de gestion de santé adapté à une église : **dossiers patients**, consultations de soins, **pharmacie** (inventaire + expirations), planning des **gardes**, **campagnes médicales** (vaccination, dépistage, sensibilisation, dons de sang) et **distribution de kits médicaux** avec traçabilité. Données **confidentielles** (même modèle que le Pastoral §G3.8) et **intégralement archivées**.

**Modèle (annexe A §A.17) :** `patient_record` (dossier par personne : groupe sanguin, allergies, antécédents, mesures, médecin traitant, notes sensibles) · `medical_consultation` (triage / consultation / suivi : motif, constantes vitales, diagnostic, traitement, résultat, praticien) · `prescription` (médicament, posologie, durée, statut) · `pharmacy_item` + `pharmacy_stock` (lot, quantité, seuil, **date d'expiration**, fournisseur) · `pharmacy_movement` (entrée/sortie liée prescription ou distribution) · `health_campaign` (titre, type, dates, lieu, responsable, objectif, statut) · `campaign_participant` (enrôlement + résultat par personne) · `medical_kit` (définition + `content_json`) · `kit_distribution` (kit, lot, destinataire, quantité, date, responsable, **preuve/signature**) · `health_referral` (évacuation vers un établissement externe, motif, suivi, historisé) · `staff_duty` (gardes du personnel de santé).

**Tâches :**
1. **Migration + modèle** (annexe A §A.17) : tables avec `tenant_id`, soft delete, `business_history` ; réutiliser l'existant quand pertinent (`appointments`, `events`, `presence` — vérifier G0.1) via façade, ne dupliquer que ce qui manque.
2. **API + RBAC scopé** : rôles d'espace `HEALTH_STAFF` / `HEALTH_LEAD` (ajoutés au catalogue de rôles G3.2 avec permissions scopées) ; endpoints REST `GET/POST/PUT /api/health/...` (patient, consultation, prescription, stock, campagne, kit) — tous scopés tenant+espace.
3. **Confidentialité stricte (modèle §G3.8)** : dossier médical **séparé** de la fiche publique ; visibilité limitée aux assignés + rôle santé de l'espace ; **chaque consultation de dossier = `audit_event`** ; jamais exposé via custom fields ni via les API « membres » ; le membre voit ses propres prescriptions/suivis (lecture seule) si l'église l'active.
4. **Pharmacie & expirations** : stock par lot, seuils bas, alertes « stock faible » et « médicaments proches de l'expiration » (notifications + dashboard) ; sorties liées aux prescriptions (traçabilité totale).
5. **Planning des gardes** : réutilise Event/Scheduling (G3.3) avec `staff_duty` (personne, créneau, statut) ; affectations historisées.
6. **Campagnes médicales** : création + planification (dates, lieu, type, responsable), enrôlement des participants (depuis People / liste « sans espace » / QR), suivi des résultats en direct (mobile terrain), **rapport de campagne** (nombre vus, résultats, taux) + archive.
7. **Kits médicaux** : catalogue de kits (ex. kit pansement, kit vaccin, kit soin) avec contenu, lots et stock ; **distribution** à une personne / une famille / un événement avec quantité, motif, responsable et **preuve de réception** (signature ou scan) ; alarme si le stock ne couvre pas la distribution ; rapport des distributions.
8. **Workflows (G2.5)** : « consultation → prescription → distribution → rappel suivi » ; « stock bas → alerte + commande fournisseur » ; « campagne terminée → clôture + archivage » ; notifications associées (G2.10).
9. **UI web (générée par G5.3)** : onglets Patients / Consultations / Pharmacie / Gardes / **Campagnes** / **Kits** / Rapports ; fiche patient avec **timeline médicale** ; dashboards (attente, alertes, campagnes en cours, stocks critiques).
10. **UI mobile (G5.6/G5.7)** : consultation terrain (constantes vitales, photo de lésion), enrôlement campagne + résultats, **distribution de kit par scan du QR de l'âme**, rappels (RDV, campagne, prise de médicament).
11. **Tests** : RBAC scopé (infirmier d'un espace ne voit pas l'autre), confidentialité (membre ≠ accès aux dossiers), IDOR (patient cross-tenant/espace), stock (impossible de distribuer plus que le stock), expiration, isolation tenant, workflow prescription→distribution.

**DoD :** espace SANTÉ complet (hospitalier) ✅ · campagnes médicales ✅ · kits tracés + preuves ✅ · confidentialité + audit d'accès ✅ · mobile terrain ✅ · Security Matrix étendue au domaine santé ✅.

**Commit :** `feat(health): infirmary health engine with campaigns and medical kits`

---

## Étape G3.12 — Verrou G3 (Gate)

**Checklist de sortie G3 :**
- [ ] G3.1 people + inscription auto + sans espace ✅
- [ ] G3.2 membreships/role assignments historisés ✅
- [ ] G3.3 event engine ✅ · G3.4 dress code/archives/répertoires/debrief ✅
- [ ] G3.5 assets ✅ · G3.6 finance ✅
- [ ] G3.7 discipleship ✅ · G3.8 pastoral ✅ · G3.9 prayer ✅ · G3.10 media ✅ · **G3.11 santé/infirmerie ✅**
- [ ] Security Matrix verte (y compris pastoral + finance)
- [ ] DoD global sur toutes les étapes de la porte

**Commit :** `chore: Gate G3 domain engines green`
# PORTE G4 — FAMILY OS & RÔLES VIVANTS (exigences utilisateur)

> Cette porte matérialise les exigences « 1:06 PM » : familles configurables, chefs de famille, pasteur principal → transferts/nominations, et **interface qui change automatiquement selon le rôle**.

## Étape G4.1 — Family OS : suivi des âmes dans l'espace FAMILY

**Objectif :** une famille de disciple est un espace `FAMILY` « presque un département, axé sur le suivi des âmes » : événements, visites, comptes-rendus, réceptions — avec les mêmes outils qu'un département.

**Tâches :**
1. **Modules par défaut FAMILY** (template G2.3) : people, visits (`family_visit` : date, âme visitée, rapport), receptions (`family_reception`), events, reports, archive — activables/désactivables (feature picker G2.6).
2. **Backend** : CRUD `family_visit` + `family_reception` + `family_meeting` (envelopper l'existant `families`/`familyMeeting`/`familyResources` — inventaire G0.1 d'abord) ; champs customs (§G2.4) pour les suivis spécifiques.
3. **Compte-rendu** : modèle de CR (âme, sujet, décisions, prochain rdv, suivi) ; rappel du prochain contact (follow-up).
4. **UI web+mobile** : Dashboard famille (prochaines visites, âmes en suivi, réceptions), fiche âme (parcours §G3.7 + visites + notes), bouton « Créer une activité » multi-type.
5. **Tests** : le chef de famille gère son espace via la **même** mécanique qu'un chef de département ; aucun code spécifique « famille » dans les moteurs (tout est configuration).

**DoD :** visites/événements/réceptions/comptes-rendus ✅ · mêmes outils qu'un département ✅.

**Commit :** `feat(family): family os soul follow-up workspace`

---

## Étape G4.2 — Chef de famille : rechercher & ajouter des membres (église/campus)

**Objectif (exigence utilisateur) :** le chef de famille consulte la liste des membres **autorisés** de l'église ou du campus et ajoute une âme dans sa famille.

**Tâches :**
1. **API** : `GET /api/people/search?scope=CAMPUS|CHURCH` (RBAC scopé selon `visible_people_scope` de l'espace FAMILY, configurable par admin/pasteur) ; `POST /api/spaces/:id/members` (le chef de famille ajoute → `space_membership` + notification à l'âme).
2. **Confidentialité** : la recherche n'expose que les données de contact minimales du scope (jamais pastoral/finance) ; un membre choisit sa visibilité dans l'annuaire.
3. **UI** : « + Ajouter une âme » dans l'Espace Famille → recherche → ajout (rôle/motif optionnel).
4. **Mobile** : même parcours (écran Famille).
5. **Tests** : scope respecté ; impossible d'ajouter hors scope.

**DoD :** recherche & ajout ✅ · scope + confidentialité testés ✅.

**Commit :** `feat(family): family head member search and assignment`

---

## Étape G4.3 — Pastorate : transferts & nominations de pasteurs

**Objectif (exigence utilisateur) :** le pasteur principal transfère un pasteur et le nomme pasteur d'un campus, etc. — avec historique des mandats.

**Tâches :**
1. **Backend** : `POST /api/pastorate/transfer` (pasteur → église/campus cible, motif, date d'effet, clôture ou maintien du mandat précédent via `role_assignment` historisé §G3.2) ; `POST /api/pastorate/appoint` (nomination pasteur de campus / adjoint / principal).
2. **Règles** : seul `PASTOR_PRINCIPAL` (ou super admin) transfère/nomme ; pas de double mandat actif sur la même unité ; événement `PastorAppointed` → notification + history.
3. **UI** : écran « Ministère pastoral » (organigramme des pasteurs par campus, mandats en cours/passés) + wizard de transfert avec aperçu des impacts.
4. **Mobile** : consultation organigramme + notifications de nomination.
5. **Tests** : un pasteur transféré conserve son historique (« qui dirigeait le campus A en 2024 ? ») ; les permissions suivent la nouvelle affectation (UI recablée §G4.4).

**DoD :** transfert/nomination ✅ · historique ✅ · permissions recalées ✅.

**Commit :** `feat(pastorate): pastor transfers and campus appointments`
## Étape G4.4 — Rôles vivants : l'interface change automatiquement (web + mobile)

**Objectif (exigence utilisateur) :** dès qu'un membre est désigné responsable, chef de famille, faiseur, etc., **son interface web ET mobile change automatiquement, en temps réel**, sans déconnexion.

**Tâches :**
1. **Moteur** : `PermissionResolver` (backend) calcule l'ensemble des permissions/scopes d'un utilisateur à partir de SES affectations actives (`role_assignment`, `space_membership`, `event_assignment` non expirés), versionnées (`permission_version`).
2. **Propagation** : à chaque `RoleAssigned/RoleEnded/MemberTransferred/PastorAppointed/SpaceConfigChanged` → outbox → `permissions-changed {userId, version}` → poussé par WS (web) et notification silencieuse (mobile) ; les clients refont `GET /api/me/permissions` et re-rendent nav/menus/actions à chaud.
3. **Web** : `AuthContext` + gardes `RequirePermission`/`RequireRole` dynamiques ; composant « Ce qui a changé pour vous » (toast + re-rendu du menu).
4. **Mobile** : `PermissionController` Riverpod réagit au push, met à jour menu et écrans visibles ; **pas de déconnexion forcée**.
5. **Objectif de service** : un changement de permission est actif **< 5 secondes** sur un client connecté (test E2E automatisé).
6. **Tests** : désignation (web) → UI mobile recablée ; rétrogradation → droits retirés immédiatement ; token volé ne conserve aucun droit au prochain appel (JWT court + refresh).

**DoD :** changement de rôle → UI recablée web+mobile < 5s · rétrogradations ✅ · test E2E.

**Commit :** `feat(realtime): living roles real-time interface reconfiguration`

---

## Étape G4.5 — Import / Export des espaces, configurations & données

**Objectif :** importer/exporter un espace (config + données), une église entière, et des jeux de données (exigence utilisateur + préparation lancement).

**Tâches :**
1. **Export** : format JSON canonique versionné (`space_export_v1`) : config (modules, statuts, workflows, champs, UIConfig) + données (membres, événements, assets, finances filtrées selon droits) ; export complet d'un tenant par le super admin.
2. **Import** : validation + rapport précis (ligne/erreur) ; import idempotent (UUID clients préservés, conflits signalés) ; aucun écrasement silencieux.
3. **Web** : boutons Importer/Exporter (espace) + centre d'import (réutiliser la page d'import CSV existante pour le format canonique).
4. **Mobile** : import de config d'espace sans fil (v1.0 : lecture seule du format ; écriture en v1.1 — justifier en TODO si non faisable proprement).
5. **Tests** : round-trip (export→import→export identique), rejet des fichiers invalides, isolation.

**DoD :** export/import round-trip ✅ · rapport d'erreur ✅.

**Commit :** `feat(core): space and tenant export/import`

---

## Étape G4.6 — Migration des données legacy (engine, dry-run & toggle tenant)

**Objectif :** permettre aux églises existantes (données legacy) de migrer vers les tables moteurs **sans interruption ni perte** : un *engine* de migration pilote par un **toggle tenant** (`§G1.2` `legacy_migration_enabled`), avec **dry-run + rapport + replay idempotent + rollback**. **[BUSINESS_INPUT : priorité & scope des modules legacy à migrer en beta]**

**Tâches :**
1. **Inventaire** : croiser `docs/DATA_SYNC_AUDIT.md` + les tables legacy existantes par entité (personnes, finance, événements, prière, médias, documents, groupes familiaux) ; produire `migration_map_<module>.md` (source → cible, règles, champs non mappables).
2. **Engine** : `LegacyMigrator` (consumer d'outbox `LegacyDataNeedMigration`, cf. G2.8) → lit **la source legacy du tenant concerné**, valide les champs, écrit dans la table cible (scope tenant strict), trace chaque ligne dans `migration_audit` (table + statut + `source_id`). **Aucune donnée source n'est effacée** tant que la migration n'est validée par un responsable.
3. **Dry-run & rapport** : mode simulation par tenant → rapport par table (lignes migrées / doublons fusionnables / champs non mappables) ; mappage manuel des conflits (rejets ligne par ligne).
4. **Replay idempotent + rollback** : relance sans doublon (déduplication par `source_id + type`) ; rollback sur snapshot si clôturé ≤ 30 jours & validé réversible, sinon statut *définitif*.
5. **UI** : section Migration (écran espace) → aperçu → dry-run → exécution → validation différée ; action **déclenchée par le toggle §G1.2 `legacy_migration_enabled`**.
6. **Tests** : migration rejouable, dry-run détecte 100 % des conflits connus, rollback validé.

**DoD :** G4.6 ✅ · toggle `legacy_migration_enabled` ✅ · dry-run + rapport + replay + rollback ✅ · tests.

**Commit :** `feat(core): legacy data migration engine with dry-run, replay and rollback`

---

## Étape G4.7 — Verrou G4 (Gate)

**Checklist de sortie G4 :**
- [ ] G4.1 family OS ✅ · G4.2 recherche/ajout chef de famille ✅
- [ ] G4.3 pastorat transferts ✅ · G4.4 rôles vivants < 5s ✅
- [ ] G4.5 import/export ✅ · **G4.6 migration legacy ✅**
- [ ] Security Matrix verte (scopes famille inclus)
- [ ] DoD global sur toutes les étapes de la porte

**Commit :** `chore: Gate G4 family os, living roles and legacy migration green`
# PORTE G5 — UX 2 NIVEAUX & MOBILE (Commercialisation)

> **Exigence UX (PRMPT §22, 25, 29-30) :** INTERDIT le « sidebar + tableau CRUD + formulaire bootstrap ». Deux **niveaux** : le **Church OS** (vue globale) et le **Department/Family OS** (expérience spécialisée générée par la configuration).

## Étape G5.1 — Design System premium & fondations UX transverses

**Objectif :** tokens de design (couleurs brand-dynamiques §G1.2, light/dark, typographie), composants premium réutilisables, command palette, recherche globale, raccourcis clavier, a11y.

**Tâches :**
1. **Tokens CSS/TS** : variables `--brand-*` (héritées §G1.7), thème light/dark complet, échelle typographique ; palette nominale respectée sur TOUS les écrans.
2. **Composants** : DataTable (tri, filtre, pagination cursor, colonnes personnalisables, virtualisation), Kanban, calendrier, timeline, graphiques, drawer/modal, form builder, empty states premium, skeleton loading, optimistic UI, undo (vérifier les bibliothèques présentes en G0.1 avant d'ajouter la moindre dépendance).
3. **Command palette** (`Cmd+K`) : recherche globale (personnes, espaces, événements, actions) → navigation directe.
4. **Raccourcis clavier** + accessibilité (focus visible, contrastes WCAG AA, navigation clavier, labels, lecteurs d'écran).
5. **i18n (dès le premier écran neuf)** : toutes les chaînes UI en **clés** (fichier `fr.json` par défaut, **fallback `en.json`**) via la bibliothèque standard vérifiée en G0.1 ; traduction EN complète planifiée en v1.1 (architecture + clés prêtes).
6. **Tests** : a11y minimaux (contraste, focus), cohérence de palette.

**DoD :** design system ✅ · command palette ✅ · recherche globale ✅ · a11y ✅.

**Commit :** `feat(ui): premium design system command palette and a11y`

---

## Étape G5.2 — Church OS (niveau 1 : vue globale de l'église)

**Objectif (PRMPT §22 Niveau 1) :** un écran d'accueil donnant l'état de l'église en un coup d'œil : 👥 membres · 🏢 espaces · 📅 événements · ❤️ finance / pastoral / intercession · recherche · accès aux espaces.

**Tâches :**
1. **Dashboard global** : cartes KPI **réelles** (API, jamais de mock), activité récente (feed), accès rapides aux espaces (tuiles icône+couleur), événements à venir, alertes (personnes sans espace, dépassements de quota, dress codes à venir).
2. **App shell** : sidebar + command palette + recherche globale + centre de notification temps réel ; navigateur d'organisation (arbre G2.1, drag & drop pour admin).
3. **Responsive** : shell utilisable en tablette / mobile-web.
4. **Tests** : aucun écran mort, KPI = vraies données.

**DoD :** Church OS ✅ · KPI réels ✅ · temps réel ✅.

**Commit :** `feat(ui): church os global shell and dashboard`

---

## Étape G5.3 — Department/Family OS (niveau 2 : expérience générée)

**Objectif (PRMPT §26) :** route `/app/spaces/:id` — menu, widgets, actions et pages sont **générés depuis la configuration** de l'espace, jamais codés par espace.

**Tâches :**
1. **App shell dynamique** : `GET /api/spaces/:id/bootstrap` (config résolue + modules + statuts + champs + permissions + UIConfig) → construit nav, boutons d'action et widgets ; chaque module activé monte ses pages (Audiovisuel : Overview, Équipe, Équipements, Inventaire, Maintenance, Événements, Planning, Tâches, Budget, Dépenses, Documents, Rapports, Archives).
2. **Customisation visible** : couleurs/icônes/ordre/visibilité appliqués en direct (§G2.7 propagation).
3. **Dashboards par espace** : widgets paramétrables (matériel sorti, prochains événements, retards, présences, maintenance, budget, tâches, équipe) ; admin peut verrouiller certains widgets.
4. **Tests** : deux espaces → deux expériences distinctes depuis la même base de code ; changement de config → re-rendu.

**DoD :** `/app/spaces/:id` généré ✅ · deux espaces = deux OS distincts ✅.

**Commit :** `feat(ui): generated space operating system shell`
## Étape G5.4 — Frontend Providers/Guards complets (§55-56)

**Objectif :** gardes centralisées cohérentes avec le backend ; jamais d'autorisation frontend seule, mais UX désactivée proprement quand le droit manque.

**Tâches :**
1. **`TenantContext` finalisé** : résolution du tenant courant (session/domaine), switch de tenant sans perte de contexte, chargement unique.
2. **Guards** : `RequireAuth`, `RequireTenantAccess`, `RequireScope(resource, action)`, `RequireFeature(module_code)`, `RequireRole(role_codes)` — branchées sur les permissions **résolues** du backend (§G4.4) ; 403 → écran « Accès refusé » + bouton « Demander l'accès » (notification au responsable).
3. **États UI** : actions non autorisées masquées OU désactivées avec tooltip (option par espace) ; cohérence totale avec la Security Matrix (chaque règle front a sa règle back).
4. **Tests** : jeux de rôles (membre, responsable, chef de famille, admin) → menus attendus.

**DoD :** guards ✅ · parité front/back documentée · tests rôle par rôle.

**Commit :** `feat(ui): complete tenant context and scope guards (55-56)`

---

## Étape G5.5 — Super Admin / Tenant Admin Web complets + Mobile adapté (§57-59)

**Objectif :** terminer les pages d'administration existantes (créées mais fonctionnalités limitées).

**Tâches :**
1. **Super Admin** : tenanciers (création, suspension, plan, quotas, impersonation §G1.9, exports §G4.5), plans (§G1.4), marque plateforme, métriques plateforme.
2. **Tenant Admin** : organisation (arbre + drag&drop), membres (affectations §G3.2), espaces (création/config), rôles/permissions (éditeur scopé), paramètres (§G1.2), invitations (§G1.6), modules (§G1.3), journaux (audit + history §G2.9).
3. **Mobile admin** : compléter les écrans d'administration actuels pour les actions terrain fréquentes (validation de présence, approbations workflow, statuts) — configuration lourde côté web.
4. **Tests** : un test par écran admin + Security Matrix étendue aux routes admin.

**DoD :** §57-58 ✅ · §59 ✅ · aucun bouton mort.

**Commit :** `feat(admin): complete super admin and tenant admin ux (57-59)`
## Étape G5.6 — Mobile terrain connecté (opérations terrain réelles)

**Objectif (PRMPT §27) :** Flutter consomme les **vraies APIs** (interdiction de mock/fake/écran simulé) : scan QR asset, check-in/out équipement, retour + photo dommage, tâches, présence, planning, notifications, consultation de personnes, événements, documents.

**Tâches :**
1. **Connecteur** : adapter le client API existant (Dio) — endpoints versionnés, refresh token, en-tête tenant, gestion 401/403 (→ écran de reconnexion).
2. **Écrans terrain** : checker-in/out (QR §G3.5), présence événement (§G3.3), tâches, planning, notifications, fiche personne (selon droits), dress code « Ma tenue » (§G3.4), approbations workflow (§G2.5), suivi famille (§G4.1/4.2).
3. **Design mobile** : même design system (tokens partagés via API §G1.2), dark/light, mobile-first thumb-friendly.
4. **Tests** : chaque écran a un test widget + test d'API contre un backend de test ; zéro mock UI.

**DoD :** §27 mobile réel ✅ · scan QR ✅ · aucun écran simulé ✅.

**Commit :** `feat(mobile): field operations on real APIs`

---

## Étape G5.7 — Mobile offline ciblé (lecture cache + file d'écriture + résolution)

**Objectif (Q3 validée) :** offline **terrain ciblé** : lecture hors-ligne (personnes, événements, dress codes, config UI) + file d'écriture pour les opérations critiques (check-in, présence, retour matériel + photo, tâche, nouveau visiteur) — avec retry, idempotence, résolution de conflits LWW. Toggle par tenant (`offline_mode = LECTURE | FIELD_OPS | FULL`).

**Tâches :**
1. **Base locale** : réutiliser Drift + `tenant_session.dart` (§37 existant) → schéma local par tenant avec `updated_at`, `dirty` flags, `client_uuid` par entité créée hors-ligne.
2. **Cache de lecture** : refraîchissement (plein puis delta) ; les listes de référence sont toujours disponibles hors-ligne (hors-ligne visible via bandeau).
3. **File d'écriture** : `sync_queue` (opération, payload, client_uuid, retry_count, status) ; envoi par lot à la reconnexion ; **idempotence** côté serveur (rejet du doublon par `client_uuid`), retry exponentiel.
4. **Résolution de conflits** : LWW horodatée (par défaut) ; conflit sur champ sensible → « conflit détecté » côté web pour le responsable (réconciliation manuelle documentée, pas de perte silencieuse).
5. **Toggle tenant** : `tenant_settings.offline_mode` respecté (mode LECTURE = pas d'écriture hors-ligne).
6. **Tests** : coupure réseau simulée → opérations OK → reconnexion → sync exacte (y compris photos), aucun doublon après rejeu, conflit LWW.

**DoD :** lecture hors-ligne ✅ · file d'écriture ✅ · idempotence ✅ · conflits ✅ (§37 vert).

**Commit :** `feat(mobile): targeted offline field operations with sync`

---

## Étape G5.8 — Synchronisation & temps réel Web ↔ Mobile

**Objectif (exigence 1:06 PM) :** *« ce que je modifie sur le web modifie automatiquement aussi en mobile »* — couleurs, noms, ajout de membre, désignation d'un responsable, chef de famille, faiseur… et réciproquement.

**Tâches :**
1. **Canal temps réel** : les clients (web + mobile) reçoivent les événements outbox (§G2.8) auxquels ils sont abonnés (par user / par espace / par tenant selon rôles) via WS (web_socket_channel côté Flutter).
2. **Bascule** : les écrans actifs re-rendent à la volée (config §G2.7, membres §G3.1, roles §G4.4, dress codes §G3.4, tâches, événements).
3. **Écriture mobile → web** : toute mutation passe par l'API (ou la file §G5.7) → événement → les autres clients se mettent à jour (ex. check-in mobile visible immédiatement dans le planning web).
4. **Contrôles** : version de données (`updated_at`), rafraîchissement total si delta manquant (trous de réseau), indicateur de fraîcheur.
5. **Tests E2E** : deux clients (1 web + 1 mobile) — modification web → mise à jour mobile < 5s ; modification mobile → mise à jour web < 5s.

**DoD :** sync web↔mobile < 5s démontrée par test E2E · aucune donnée perdue à la coupure.

**Commit :** `feat(realtime): web mobile realtime synchronization`

---

## Étape G5.9 — Portail basse connexion (WhatsApp / USSD)

**Objectif (recommandation validée) :** dans un marché à couverture data inégale, chaque membre — même sans smartphone évolué — accède à l'essentiel et agit via **WhatsApp** et **USSD** : dress codes, planning, notifications, dons Mobile Money, présence par flash.

**Tâches :**
1. **Backend** : passerelle `lowband` unique (s'appuyant sur les modules `whatsapp`/`ussd` existants — vérifier G0.1) : numéros entrants normalisés, session légère (numéro + OTP), commandes typées, journal d'interactions.
2. **WhatsApp (sortant)** : webhook abonné aux événements d'outbox (G2.8) → notifications ciblées : dress code à venir (G3.4), rappels de garde/tâche/RDV, désignation (rôle vivant G4.4), alertes stock santé (G3.11).
3. **WhatsApp (entrant)** : requêtes naturelles/simples : « Ma tenue ce dimanche » · « Planning du jour » · « Don 1 000 » (lien sécurisé Mobile Money §G3.6) · « Ma présence » (flash).
4. **USSD** : menu `*code#` configurable : 1-Événements · 2-Ma tenue · 3-Présence (flash) · 4-Dons · 5-Notifications ; sécurisation OTP ; journalisation.
5. **Opt-in & confidentialité** : le canal low-band est **activé/désactivé par le tenant** via le toggle `low_band_enabled` (§G1.2) ; chaque membre opt-in individuellement (réglage de profil) ; aucune donnée sensible (pastoral, finance personnelle) ne transite par SMS/USSD ; liens tokenisés et courts.
6. **Tests** : simulation de canaux (messages entrants/sortants), isolation tenant des numéros, opt-out respecté, journal complet.

**DoD :** WhatsApp + USSD opérationnels ✅ · « ma tenue » / planning / don testés ✅ · opt-in + confidentialité ✅.

**Commit :** `feat(lowband): whatsapp and ussd member portal`

---

## Étape G5.10 — Verrou G5 (Gate)

**Checklist de sortie G5 :**
- [ ] G5.1 design system ✅ · G5.2 church os ✅ · G5.3 department/family os ✅
- [ ] G5.4 guards ✅ · G5.5 admins ✅
- [ ] G5.6 mobile terrain ✅ · G5.7 offline ciblé ✅ · G5.8 sync < 5s ✅ · **G5.9 low-band (WhatsApp/USSD) ✅**
- [ ] Security Matrix verte (routes mobiles incluses)
- [ ] DoD global sur toutes les étapes de la porte

**Commit :** `chore: Gate G5 ux and mobile green`
# PORTE G6 — QUALITÉ, SÉCURITÉ & GO / NO-GO COMMERCIAL

> Dernière porte : tout doit être vert — web, mobile, backend, DB, sécurité, offline, realtime — avant le Go/No-Go final (annexe G).

## Étape G6.1 — Search / Export / Delete tenant-aware (§60-62)

**Objectif :** recherche globale, export et suppression respectent tenant-scope et validation de scope.

**Tâches :**
1. **Search** : back-end de recherche plein texte (PostgreSQL + trigramme ou index FTS — vérifier ce qui existe) scopée par tenant + permissions ; autocomplete temps réel (palette §G5.1) ; résultats filtrés par droits (un membre ne voit pas les personnes pastorales/financières).
2. **Export** : tous les exports (CSV/Excel/PDF) sont scopés et **audités** (qui exporte quoi) ; export limité par plan (quotas §G1.4).
3. **Delete** : toute suppression de donnée multi-tenant est un **soft delete** + `business_history` (qui/quand/pourquoi) ; suppression physique réservée au super admin avec double confirmation + audit ; cascade contrôlée (jamais de cascade silencieuse sur données historisées).
4. **Tests** : recherche cross-tenant impossible, export audité, suppression historisée.

**DoD :** §60-62 ✅ · tests de scope ✅.

**Commit :** `feat(core): tenant aware search export and soft delete (60-62)`

---

## Étape G6.2 — Modules IA / Academy / Chat / Payments / Analytics (§63-68)

**Objectif :** faire de **« L'IA Discipolat »** le **fer de lance marketing** de l'application (badge `🤖 IA incluse` sur `/pricing`, argument commercial n°1), tout en s'appuyant sur l'existant (`ai`, `aiKpiNarrative`, `predictions`, `intelligence`, `dashboard`, `engagementAnalytics`…) **sans refonte**.

**Tâches :**
1. **« L'IA Discipolat »** : assistant intelligent brandé pour les responsables — résumés d'événements & prédications, **récits KPI** (synthèses de fin de mois/campagne), **prédictions de présence**, **suggestions de suivi des âmes** (prochaine visite, relance), propositions de dress codes/planning. Branchée sur les événements (outbox §G2.8) et sur les données **du tenant uniquement** (aucun appel hors-tenant sans anonymisation stricte — clé provider `[BUSINESS_INPUT]`).
2. **Crédits IA par plan** : consommation comptée (`GET /api/saas/usage`, annexe F) ; quota atteint → file d'attente + message clair (« Rechargez votre plan pour + de réponses IA ») ; aucun blocage silencieux ; dashboard admin d'usage IA par église.
3. **Academy/formations** : lier formations/trainings existants aux espaces et aux parcours discipleship.
4. **Chat/messagerie** : s'appuyer sur `messages`/`communications` existants ; messages scopés espace/tenant ; notifications (G2.10).
5. **Payments** : dons/contributions avec Mobile Money (§G3.6) + reçus ; rapprochement ; géo-pricing (annexe F).
6. **Analytics** : tableaux de bord d'engagement (présence, parcours, activités) alimentés par les événements (consumer ANALYTICS §G2.8) ; agrégations par tenant/space/date.
7. **Tests** : chaque intégration a un test de bout en bout réel (sandbox), zéro mock IA ; test de quota IA (consommation → blocage gracieux → recharge).

**DoD :** §63-68 ✅ · IA brandée + crédits appliqués ✅ · intégrations réelles testées ✅.

**Commit :** `feat(core): discipolat ai flagship with per-plan credits (63-68)`

---

## Étape G6.3 — Redis tenant-aware (§38)

**Objectif :** auditer et sécuriser les clés Redis (éviter toute fuite cross-tenant via des clés partagées).

**Tâches :**
1. **Audit des clés** : inventaire de toutes les clés Redis utilisées (sessions, cache, file d'attente, verrous) ; chaque clé contenant des données par tenant est préfixée `tenant:{id}:` (ou équivalent immutable).
2. **Correction** : `TenantAwareRedisManager` étendu à tous les caches du backend ; les caches invalides tiennent compte du tenant ; aucun cache global de données sensibles.
3. **Tests** : deux tenants ne partagent jamais de clé ; invalidation tenant-isolée.

**DoD :** §38 ✅ · revue de code Redis couverte par un test.

**Commit :** `fix(cache): redis tenant isolation audit (38)`

---

## Étape G6.4 — Tests de non-régression automatisés (§70)

**Objectif :** verrouiller les parcours critiques contre toute régression future (avant commercialisation).

**Tâches :**
1. **Parcours critiques à couvrir** : inscription auto → répertoire → affectation responsable ; création espace depuis template → personnalisation → propagation ; cycle événement+dress code+archives ; cycle asset (checkout / dommage → maintenance → finance) ; workflow approbation ; changement de rôle → interface ; invitation → email → acceptation ; finance : rapprochement.
2. **Stack** : tests d'intégration Spring (WebTestClient) + tests UI (Vitest + composants) + E2E critiques (Playwright web / integration_test Flutter pour les flows mobiles critiques) — restreindre aux parcours de la liste (pas de duplication exponentielle).
3. **Pipelines CI** : ajouter les E2E critiques au workflow `ci-cd.yml` (avec cache, sans `|| true` pour l'essentiel critique).
4. **Rapport** : `reports/REGRESSION_REPORT.md` (parcours → résultat, sauvegardé à chaque exécution).

**DoD :** §70 ✅ · chaque parcours de la liste est automatisé et vert · CI verte.

**Commit :** `test: critical path regression suite (70)`

---

## Étape G6.5 — Tests de performance (§71)

**Objectif :** prouver le dimensionnement commercial : 1K–10K tenants, 10 000 personnes, 100+ espaces, 100 000+ événements, sans architecture fragile.

**Tâches :**
1. **Données de charge** : générateur de dataset réaliste (personnes, espaces, événements, assets, finances, dress codes) en sandbox dédiée.
2. **Scénarios** : connexion + dashboard Church OS, ouverture d'un espace (bootstrap config), recherche globale, liste virtuelle, calendrier chargé, synchro mobile (varier l'offline), push 1 000 notifications.
3. **Outils** : JMeter/k6 ou équivalent (vérifier l'existant G0.1) ; mesures : latence p95/p99, débit, utilisation CPU/DB.
4. **Corrections** : indexes manquants (explain), N+1, projections DTO, pagination cursor, virtualisation — documenter chaque correctif dans la fiche de suivi.
5. **Budget** : p95 < 500 ms sur les écrans critiques en charge ; bootstrap d'espace < 1 s ; sync mobile < 5 s.

**DoD :** §71 ✅ · rapport `reports/PERFORMANCE_REPORT.md` ✅ · budgets atteints.

**Commit :** `perf: load tests 1k-10k tenants and query optimizations (71)`
## Étape G6.6 — Audit sécurité final exhaustif

**Objectif :** certification sécurité avant commercialisation (tenant isolation, IDOR, élévation, données sensibles, uploads, secrets, rate limiting).

**Tâches :**
1. **Revue offensive** : tester chaque ressource sensible : isolation tenant (compte A → ressource B), IDOR (ids devinables), élévation (membre→admin, chef→parent), mass assignment, module désactivé, uploads (type/taille/chemin/serveur isolé), fichiers (traversal, symlink), CORS, rate limiting, session/JWT (expiration, révocation, impersonation).
2. **Secrets** : scan des dépôts (tokens, clés), rotation si fuite, `.env.example` complet sans valeurs réelles, clés de signature JWT hors dépôt (voir `setup-keys.sh`).
3. **Dépendances** : `mvn dependency-check` et `npm audit`/`flutter pub outdated` — corriger les vulnérabilités critiques.
4. **Livrable** : `reports/SECURITY_AUDIT_REPORT.md` avec matrice §44-45 réactualisée (aucune cellule ⚠️).

**DoD :** aucun IDOR/cross-tenant/élévation détecté · vulnérabilités critiques = 0 · rapport signé.

**Commit :** `fix(security): final security audit and remediation`

---

## Étape G6.7 — QA global (web / mobile / offline / realtime)

**Objectif :** chasse aux défauts résiduels sur TOUTES les couches, pilotée par scénarios.

**Tâches :**
1. **Script de QA** (`docs/qa/QA_SCENARIOS.md`) : ~40 scénarios couvrant les 4 couches (web, mobile, offline, realtime) + les parcours métier (espaces, dress code, famille, pastorat, finance, pastoral).
2. **Exécution** : suivre le script, corriger tout défaut ; chaque correction = test automatisé ajouté (règle anti-régression).
3. **Points d'attention** : erreurs console (web), crash mobile, perte de données offline, temps de sync, cohérence des badges notifications.
4. **Vérification finale** : triade de validation §0.10 verte + CI verte.

**DoD :** 100% des scénarios verts · rapport `reports/QA_REPORT.md`.

**Commit :** `test: global qa scenarios green`

---

## Étape G6.8 — Documentation complète (§72)

**Objectif :** livrer une documentation exploitable par l'équipe et les premières églises.

**Tâches :**
1. **Développeur** : `docs/ARCHITECTURE.md` mis à jour (Church OS : moteurs, espaces, outbox, multi-tenant), `docs/API.md` (OpenAPI à jour), `docs/DEPLOYMENT.md`, `docs/ENV_TEMPLATE.md`, `docs/DATABASE.md` (schéma canonique + migrations).
2. **Utilisateur** : `docs/GUIDE_UTILISATEUR.md` (rôles : membre, chef d'espace, chef de famille, pasteur, admin) + fiches d'aide intégrées (modales « ? » dans les écrans clés).
3. **Super Admin / Commercial** : guide de back-office + procédure de vente (création église, plan, données de l'église cliente).
4. **Opérations** : runbook incidents, sauvegardes (§6.9), monitoring.

**DoD :** §72 ✅ · liens de docs vérifiés · aucun lien mort.

**Commit :** `docs: complete church os documentation (72)`

---

## Étape G6.9 — Préparation production (staging, beta, monitoring, sauvegardes)

**Objectif :** rendre l'application déployable commercialement (s'appuyer sur les workflows existants `deploy-beta.yml`, `ci-cd.yml`, `backup-postgres.yml`, `infra/`, `docker-compose.yml`).

**Tâches :**
1. **Environnements** : staging (données anonymisées de démo) + beta (églises pilotes **>3, réparties Afrique + Europe + Amériques** — Asie/Océanie en v1.1 `[BUSINESS_INPUT]`) + production (fr/en, multi-devises EUR/FCFA/USD).
2. **Déploiement** : images Docker 3 couches, migrations automatiques (Flyway), variables d'environnement toutes documentées, secrets par secret-manager.
3. **Monitoring** : Prometheus + Grafana (dashboards existants dans `infra/monitoring`) : alertes uptime, latence, erreurs 5xx, queue Redis, DB.
4. **Sauvegardes** : plan de sauvegarde PostgreSQL (existant `backup-postgres.yml`) + fichiers (stockage §39) + restauration testée en staging.
5. **Pages web** : site vitrine + `/pricing` (G1.4) + page d'acceptation invitation + **annuaire public « Églises sur Discipolat »** (toggle `public_directory_enabled` §G1.2 ; chaque église opt-in individuellement → vitrine + lien de souscription) + mentions/conditions privacité `[BUSINESS_INPUT]`.
6. **Migration des données legacy (engine §G4.6)** : les scripts/toggle `legacy_migration_enabled` construits en G4.6 sont **exécutés en dry-run + replay** pour l'import réel des églises pilotes avant AQ ; rapport par table + contrôle de non-régression (aucune donnée source n'est effacée avant validation).
7. **Tests de lancement** : parcours « nouvel églises → plan → onboarding → import → beta » sur staging avec les églises pilotes des 3 régions.

**DoD :** staging + beta déployés ✅ · monitoring actif ✅ · restauration testée ✅ · annuaire public en ligne ✅ · migration legacy dry-run + rejouée ✅.

**Commit :** `ops: production preparation staging beta monitoring`

---

## Étape G6.10 — Checklist commerciale GO / NO-GO (annexe G)

**Objectif :** décision formelle de mise sur le marché, sur **preuves**, pas sur intuition.

**Tâches :**
1. Exécuter la checklist de l'annexe G **en entier** (7 portes + sécurité + perf + docs + ops + business inputs).
2. Produire `reports/GO_NO_GO_REPORT.md` : chaque critère ✓/✗ + preuve (commit, rapport, capture) + **vérification finale** des 3 couches.
3. Si un critère ✗ : retour sur l'étape concernée (interdiction de lancer avec un ✗ non justifié documenté).
4. Tag de livraison : `v1.0-commercial-release` + `git tag` annoté + release notes (`docs/CHANGELOG.md` mis à jour).

**DoD :** GO/NO-GO documenté · tag v1.0 · CHANGELOG à jour.

**Commit :** `chore: v1.0 commercial release gate and go-no-go report`
# ANNEXES

> Les annexes font partie intégrante du contrat. L'agent les respecte telles quelles, en les **adaptant aux conventions déjà présentes** (noms de colonnes camelCase/snake_case selon l'existant), sans en changer le sens ni perdre de champ.

## ANNEXE A — Contrat de données (tables canoniques)

> **Approche contractuelle (Q5) :** les tables ci-dessous A.1→A.7 (fondations) sont un **DDL contractuel** : l'agent les crée/modifie via migration Flyway avec ces colonnes, clés et index (adaptés aux conventions du code existant). Les tables A.8→A.16 (domaines) sont un **contrat fonctionnel** : l'agent produit le DDL et le fait valider par la perspective de l'annexe (contraintes + index + tenant_id + historisation). Toute table historique critique porte les champs `deleted_at` (soft) et alimente `business_history`.

### A.1 Organisation & multi-tenant (fondations)

```sql
-- tenant : existe (enrichir si manquant : timezone, currency, offline_mode, ui_json)
tenant(id uuid pk, name, slug unique, legal_name, logo_url, cover_url, description,
       country, city, timezone, currency, language, status, offline_mode, created_at, updated_at)

organization_unit(id uuid pk, tenant_id fk not null, parent_id fk null, name, code,
       type varchar check (type in 'CHURCH','CAMPUS','MINISTRY','DEPARTMENT',
       'SUB_DEPARTMENT','TEAM','CELL','GROUP','FAMILY'), description, status,
       icon, color, sort_order, config_source check ('DEFAULT','INHERITED','OVERRIDDEN'),
       path ltree|text, level int, created_at, updated_at, deleted_at)
-- index (tenant_id, parent_id), (tenant_id, type), unique (tenant_id, code) where code not null

space(id uuid pk, tenant_id fk, organization_unit_id fk not null,
       space_type check ('DEPARTMENT','FAMILY','SUB_TEAM'), template_code, name, code,
       icon, color, description, status, visible_people_scope check ('CAMPUS','CHURCH'),
       configuration_json jsonb, created_at, updated_at, deleted_at)

module_definition(id uuid pk, code unique, name, description, category, version,
       enabled, icon, source check ('CORE','EXISTING','ENGINE'),
       features_json jsonb, created_at, updated_at)

space_module(id uuid pk, space_id fk, module_definition_id fk, enabled bool,
       configuration_json jsonb, display_order int, created_at, updated_at)
-- unique (space_id, module_definition_id)

space_template(id uuid pk, code unique, name, description, icon, color, version,
       modules_json jsonb, default_workflows_json jsonb, default_statuses_json jsonb,
       default_dashboards_json jsonb, created_at, updated_at)
```

### A.2 Identité, rôles & permissions (fondations)

```sql
person(id uuid pk, tenant_id fk not null, first_name, last_name, display_name, gender,
       birth_date, phone_normalized, email_normalized, address, photo_url, status,
       visibility_scope, created_at, updated_at, deleted_at)
-- unique (tenant_id, email_normalized) where not null ; unique (tenant_id, phone_normalized) where not null ; index (tenant_id, deleted_at)

membership(id uuid pk, tenant_id fk, person_id fk, membership_status, joined_at,
       left_at, source, notes, created_at, updated_at)

space_membership(id uuid pk, tenant_id fk, person_id fk, space_id fk,
       joined_at, left_at null, status, membership_type, responsibility, notes, created_at, updated_at)
-- index (space_id, status), (person_id, status)

role(id uuid pk, tenant_id fk null, name, code, description, scope_type)
permission(id uuid pk, code unique, name, description)
role_permission(role_id fk, permission_id fk, pk(role_id, permission_id))
role_assignment(id uuid pk, tenant_id fk, person_id fk, organization_unit_id fk null,
       space_id fk null, role_id fk, started_at, ended_at null, status, reason,
       created_at, updated_at) -- index (person_id, status), (role_id, status)

invitation(id uuid pk, tenant_id fk, organization_unit_id fk, space_id fk null, email,
       role_code, token_hash, expires_at, status, invited_by, accepted_at, created_at, updated_at)
```

### A.3 Configuration (fondations)

```sql
custom_field_definition(id uuid pk, tenant_id fk, entity_type, space_id fk null, field_code,
       label, field_type, required bool, options_json jsonb, validation_json jsonb,
       visibility_scope, display_order, created_at, updated_at)
-- unique (tenant_id, entity_type, space_id nulls_not_distinct, field_code)

custom_field_value(id uuid pk, tenant_id fk, field_id fk, entity_id uuid, value_json jsonb,
       updated_by, updated_at) -- index (entity_type_code via field, entity_id)

custom_status_set(id uuid pk, tenant_id fk, entity_type, space_id fk null, code, name, color,
       icon, display_order, initial bool, final bool, allowed_transitions_json jsonb,
       created_at, updated_at)
-- unique (tenant_id, entity_type, space_id nulls_not_distinct, code) ; [enrichir tenant_features/G1.3]

workflow_definition(id uuid pk, tenant_id fk, entity_type, code, name, space_id fk null,
       enabled bool, version, created_at, updated_at)
workflow_step(id uuid pk, workflow_id fk, step_order, step_type
       check ('APPROVAL','AUTO_ACTION','NOTIFY','EXPENSE','ASSET_STATUS','FORM'),
       name, conditions_json jsonb, assignee_role, assignee_scope, timeout_hours,
       escalation_role, auto_action_json jsonb, created_at, updated_at)
workflow_transition(from_step_id fk, to_step_id fk, on_event, pk(from_step_id, to_step_id, on_event))
workflow_instance(id uuid pk, tenant_id, workflow_id fk, entity_id uuid, current_step_id fk,
       status, started_at, updated_at)
workflow_task(id uuid pk, tenant_id, instance_id fk, step_id fk, assignee_id fk, status,
       due_at, resolved_at, comment, created_at, updated_at)
```
### A.4 Outbox & événements (fondations)

```sql
outbox_event(id bigserial pk, tenant_id, aggregate_type, aggregate_id uuid,
       event_type, payload_json jsonb, status check ('PENDING','PUBLISHED','FAILED','DISCARDED'),
       attempts int, available_at timestamptz, created_at, published_at)
-- index (status, available_at), (aggregate_type, aggregate_id)

processed_event(id bigserial pk, consumer varchar, event_id fk, processed_at)
-- unique (consumer, event_id)   -- idempotence
```

### A.5 Audit & historique métier (fondations)

```sql
audit_event(id bigserial pk, tenant_id, actor_id, actor_email, action, entity,
       entity_id uuid, old_value_json jsonb, new_value_json jsonb, ip, user_agent,
       prev_hash char(64), hash char(64), timestamp)
-- index (tenant_id, entity, entity_id, timestamp) ; chaîne calculée ; contrôle d'intégrité périodique

business_history(id bigserial pk, tenant_id, object_type, object_id uuid, event_type,
       summary text, detail_json jsonb, actor_id, actor_role, space_id fk null,
       happened_at) -- index (object_type, object_id, happened_at), (space_id, happened_at)
```

### A.6 Espaces, modules, resources (fondations, compléments)

```sql
-- [vide volontaire : A.1 couvre space/space_module ; ajouter si besoin tables de ressources (G1.8) :
resource_scope(entity_type, entity_id, scope check ('TENANT_GLOBAL','ORGANIZATION_LOCAL','UNIT_LOCAL'),
       organization_unit_id fk null) -- implémenté en champs scope/unit_id sur les tables concernées]
```

### A.7 Notifications & fichiers (fondations)

```sql
notification(id uuid pk, tenant_id, user_id fk, title, body, type, link, read_at,
       channel check ('IN_APP','PUSH','EMAIL'), event_id fk null, created_at)
-- index (user_id, read_at), (tenant_id, created_at)

notification_rule(id uuid pk, tenant_id, space_id fk null, event_type, channel,
       audience_json jsonb, enabled bool, created_at, updated_at)
```

### A.8 Événements & assignments (contrat fonctionnel) — G3.3

```sql
event / location / event_space / event_team / event_assignment / event_task /
event_asset / event_expense / event_attendance / event_document / event_schedule
-- exigences : event.tenant_id ; event_space.space_id ; event_assignment(person_id, role,
--   location_id, start_at, end_at, status) ; event_attendance(unique (event_id, person_id)) ;
--   toutes les tables : deleted_at + business_history
```

### A.9 Dress Code & patrimoine événementiel (contrat fonctionnel) — G3.4

```sql
dress_code(id uuid pk, tenant_id fk, space_id fk, event_id fk null, service_name,
       title, begins_at timestamptz, ends_at timestamptz, status, created_by, archived bool)
dress_code_rule(dress_code_id fk, group_name, description, image_url null)
dress_code_audience(dress_code_id fk, audience_type check ('SPACE_MEMBER','ROLE','GROUP_NAME'),
       role_code null)
event_repertoire(id uuid pk, tenant_id, event_id fk, space_id fk, song_title, song_order,
       roles_json jsonb, notes)
event_debrief(id uuid pk, tenant_id, event_id fk, space_id fk, what_went_well text,
       what_failed text, improvements text, service_dates_json jsonb, created_by, created_at)
space_internal_role(id uuid pk, tenant_id, space_id fk, person_id fk,
       internal_role_code, title, started_at, ended_at null)
-- index (tenant_id, space_id), (tenant_id, event_id) ; archives = statut archived + history
```

### A.10 Assets & inventaire (contrat fonctionnel) — G3.5

```sql
asset(id, tenant_id, space_id fk null, name, serial_number, category, status, purchase_date,
       purchase_price, ownership, location, warranty_until, tco_cached numeric, qr_token_hash, …)
inventory_item / inventory_movement(unique par lot, seuils) / asset_checkout(condition avant/après,
       dates, event/personne) / maintenance_record(type, diagnostic, technicien, coûts, pièces,
       documents) / asset_transfer(depuis/vers, motif, type) / asset_expense
-- exigences : TCO = somme des dépenses liées ; historique (asset_history) ; deleted_at
```

### A.11 Finance (contrat fonctionnel) — G3.6

```sql
financial_account / financial_transaction / expense (liens space_id, event_id, asset_id,
       supplier_id, budget_id) / income / donation / contribution / budget / budget_line /
payment (provider, mobile_money ref, statut) / led_ger_entry (double entrée, immuable) /
reconciliation
-- exigences : monnaie = devise tenant ; montants NUMERIC(14,2) (jamais float) ;
--   immutabilité de l'écriture ; isolation tenant stricte
```

### A.12-A.16 Parcours, pastoral, prière, média (contrats fonctionnels) — G3.7→G3.10

```sql
-- A.12 Discipleship : faith_journey, journey_definition, journey_stage_definition,
--   journey_transition, journey_event, discipleship_group (+membre), mentor_assignment
--   (exigences : étapes configurées par église ; historique des étapes franchies)
-- A.13 Pastoral : pastoral_case, pastoral_interaction, pastoral_action, follow_up
--   (exigences : confidentiality_level, visibility_scope, assigned_to ; audit d'accès ; séparé du public)
-- A.14 Prayer : prayer_program, prayer_session, prayer_slot, prayer_request,
--   fasting_program (créneaux sans chevauchement ; archives)
-- A.15 Media : sermon (+ event, speaker, verses, résumé, urls), media_asset, media_collection, document
-- A.16 Famille : family_visit, family_reception, family_meeting (liens space FAMILY + person)
-- A.17 Santé/Infirmerie : patient_record, medical_consultation, prescription,
--   pharmacy_item, pharmacy_stock, pharmacy_movement, health_campaign, campaign_participant,
--   medical_kit, kit_distribution, health_referral, staff_duty
--   (exigences : confidentialité stricte = modèle A.13 ; traçabilité totale des distributions ;
--   alertes stock/expiration ; historique complet)
```
## ANNEXE B — Erreurs de compilation pré-existantes (source : `docs/À faire.md`, à traiter en G0.3)

| Contrôleur / Service | Problèmes connus |
|---|---|
| `SuperAdminController` | `countByStatut` manquant ; getters manquants sur `SaasPlan` ; `Map.of` > 10 arguments |
| `OrganizationManagementController` | champ `slug` manquant sur `OrganizationNode` ; champ `metadata` ; méthodes manquantes sur le service |
| `ModuleFeatureController` | `getIsPublic()`, `getFeatures()`, `getSeatsLimit()` manquants sur `SaasPlan` |
| `AiModuleService`, `AiKpiNarrativeService` | méthodes manquantes sur les services IA |
| `SmartAlertService`, `BenchmarkController` | `findByDeletedFalse()` manquant sur `SoulRepository` |
| `ExportServiceImpl` | `getTitle()`, `isRead()` manquants sur `Alert`/`Notification` |
| `SpiritualHealthService`, `KingdomMappingController`, `ContextualReminderScheduler` | `findByDeletedFalse()` manquant |
| **Autres** | Toutes erreurs signalées par le compilateur (règle : ne pas s'arrêter à la liste) |

> Champ obligatoire de la fiche de suivi : `fichier | nature du correctif | test ajouté (oui/non) | commit`.

## ANNEXE C — Mapping du chantier multi-tenant `docs/À faire.md` (§27-72) → porte/étape

| § | Sujet | Où c'est traité |
|---|---|---|
| §27-28 | Tenant Settings / Branding complet | **G1.2** |
| §29 | Modules par tenant (TenantFeature) | **G1.3** |
| §30-31 | Plans SaaS / Quotas complets | **G1.4** |
| §32 | TenantContext strict | **G5.4** |
| §33-35 | Tenant/Org Switcher complet | **G5.4 + G5.2** (switcher déjà présent, à finaliser) |
| §37 | Offline Mobile tenant-aware | **G5.7** |
| §38 | Redis tenant-aware | **G6.3** |
| §39 | File Storage isolation complète | **G1.2** (logo/upload) + **G3.10/G5.6** (fichiers) + **G6.6** (audit uploads) |
| §40 | WebSocket isolation complète | **G2.10** |
| §41 | Notifications multi-tenant | **G2.10** |
| §42 | Audit Log robuste (hash chain, export, rétention) | **G2.9** |
| §43 | Impersonation Super Admin | **G1.9** |
| §44-45 | IDOR Tests / Security Matrix | **G1.10 + G6.6** |
| §46 | AuthorizationService complet | **G1.11** |
| §50-51 | Onboarding / Création sous-église | **G1.5** |
| §52 | Invitations workflow complet | **G1.6** |
| §53 | Héritage configs DEFAULT/INHERITED/OVERRIDDEN | **G1.7** |
| §54 | Ressources GLOBAL/LOCAL | **G1.8** |
| §55-56 | Frontend Providers/Guards | **G5.4** |
| §57-58 | Super Admin / Tenant Admin Web | **G5.5** |
| §59 | Mobile Administration | **G5.5** |
| §60-62 | Search/Export/Delete tenant-aware | **G6.1** |
| §63-68 | IA/Academy/Chat/Payments/Analytics | **G6.2** |
| §70-71 | Tests non-régression / Performance | **G6.4 + G6.5** |
| §72 | Documentation | **G6.8** |

<!-- RAPPEL : le tri « À faire » priorisait Haut/Moyen/Basse ; ici TOUT est en v1 ordonné par dépendance. -->

## ANNEXE D — Couverture des 43 sections du maître-contrat PRMPT

| Section PRMPT | Où c'est honoré |
|---|---|
| 1 Rôle / 2 Vision / 3 Règle absolue | §0.1-0.3 |
| 4 Architecture multi-tenant | G1 (toutes) + §0.3 |
| 5 Organization Engine | G2.1 |
| 6 Department Engine | G2.6 (espaces) |
| 7 Module Engine | G2.2 |
| 8 Template Engine | G2.3 |
| 9 Custom Field Engine | G2.4 |
| 10 Workflow Engine | G2.5 |
| 11 People Engine | G3.1 |
| 12 Role Engine (RBAC+scope) | G3.2 + G1.11 + G4.4 |
| 13 Event Engine | G3.3 |
| 14 Asset Engine | G3.5 |
| 15 Maintenance Engine | G3.5 |
| 16 Finance Engine | G3.6 |
| 17 Discipleship Engine | G3.7 |
| 18 Pastoral Care | G3.8 |
| 19 Prayer Engine | G3.9 |
| 20 Media Engine | G3.10 |
| 21 Audit Engine | G2.9 |
| 22 Business History | G2.9 |
| 23 Event Bus | G2.8 |
| 24 Real-Time | G2.10 |
| 25 Frontend (SaaS premium) | G5.1-G5.5 |
| 26 Department as an App | G5.3 |
| 27 Mobile (vraies APIs) | G5.6 |
| 28 Offline | G5.7 |
| 29 UX Exceptionnelle | G5.1 + §0.9 |
| 30 Accessibilité | G5.1 |
| 31 Performance | G6.5 |
| 32 Database | Annexe A + workflow d'étape |
| 33 Data Integrity | Annexe A + G6.1 |
| 34 API | workflow d'étape (6) |
| 35 Security | G1.10-G1.11 + G6.6 |
| 36 Tests | G6.4 + workflow d'étape (5/8/10) |
| 37 Phases 0→20 | G0→G6 (mapping ci-dessous) |
| 38 Règle de livraison | §0.4-0.5 |
| 39 Interdictions | §0.3 |
| 40 Workflow de l'agent | §0.4 |
| 41 Git | §0.6 |
| 42 Definition of Done | §0.5 |
| 43 Règle finale (ONE CORE…) | G2-G4 + annexe G |

**Mapping PHASES PRMPT → Portes :** Phase 0 Audit → G0.1 · 1 Foundation → G1 · 2 Configuration → G2.1-2.5 · 3 People → G3.1-3.2 · 4 Event → G3.3-3.4 · 5 Asset → G3.5 · 6 Finance → G3.6 · 7 Discipleship → G3.7 · 8 Pastoral → G3.8 · 9 Prayer → G3.9 · 10 Media → G3.10 · 11 Workflow+Bus → G2.5/G2.8 · 12 Web → G5.1-5.5 · 13 Department Apps → G5.3 · 14 Mobile → G5.6 · 15 Offline → G5.7 · 16 Realtime → G5.8 · 17 UX Polish → G5 (continu) · 18 Security → G6.6 · 19 Performance → G6.5 · 20 QA → G6.7 · (exigence utilisateur Family/DressCode/Rôles vivants) → G4/G3.4 · (exigence utilisateur Santé/Infirmerie hospitalière, campagnes, kits) → G3.11.
## ANNEXE E — Événements de domaine canoniques (outbox §G2.8)

```
AssetCheckedOut · AssetReturned · AssetDamaged · MaintenanceStarted · MaintenanceCompleted
ExpenseCreated · MemberRegistered · MemberTransferred · RoleAssigned · RoleEnded
PastorAppointed · EventCreated · TaskAssigned · TaskCompleted · AttendanceRecorded
PrayerSessionCompleted · SermonPublished · DressCodePublished · SpaceConfigChanged
StatusChanged · InvitationAccepted · PermissionsChanged · JourneyStageChanged · MentorAssigned
HealthConsultationCreated · PrescriptionIssued · CampaignStarted · KitDistributed · StockLowAlert
MedicineExpiring · HealthReferralCreated
```

Règle : un producteur écrit `outbox_event` **dans la même transaction** que la mutation. Un consumer n'agit **qu'une fois** par événement (`processed_event`). Tout nouvel événement ajouté doit l'être dans cette annexe (mise à jour du document comprise).

## ANNEXE F — Business Inputs & modèle commercial (marqués `[BUSINESS_INPUT]` dans les étapes)

### Plans SaaS — **VALIDÉ avec l'équipe le 12/09/2026** (modèle Dual-Market)

| Plan | Membres | Espaces | Stockage | Crédits IA / mois | Événements actifs | **Europe €/mois** | **Afrique FCFA/mois** | **Amériques & intl $/mois** | Annuel (selon zone) |
|---|---|---|---|---|---|---|---|---|---|
| **DÉCOUVERTE** (gratuit, viral) | 50 | 1 | 100 Mo | 10 | 5 | 0 | 0 | 0 | 0 |
| **DÉMARRAGE** | 250 | 10 | 1 Go | 30 | illimité | 9 € | 5 000 | 10 $ | 90 € / 50 000 F / 100 $ |
| **CROISSANCE** | 2 000 | 50 | 20 Go | 100 | illimité | 29 € | 15 000 | 32 $ | 290 € / 150 000 F / 320 $ |
| **RÉSEAU & CAMPUS** | 10 000 | 150+ | 100 Go | 500 | illimité | 79 € | 35 000 | 85 $ | 790 € / 350 000 F / 850 $ |

**Règles commerciales appliquées (G1.4 / G6.2) :**
- Essai **30 jours** toutes fonctionnalités (tous plans hormis DÉCOUVERTE).
- **Annuel = 2 mois offerts** (17 %) pour DÉMARRAGE → RÉSEAU.
- **Géo-pricing** : prix gelé à la souscription selon la région (IP/sélecteur) ; **EUR (Europe) / FCFA (Afrique) / USD (Amériques & international)** ; XAF ≈ 655,96/€, tarifs ajustés au marché.
- **Paiements** : Afrique → **MTN MoMo / Orange Money** · Europe → **carte / SEPA** · Amériques → **carte / Stripe**. Comptes réels à fournir `[BUSINESS_INPUT]`.
- **Crédits IA** : quotas mensuels par plan (cf. tableau) ; consommés par les récits KPI, prédictions, suggestions de suivi et assistances ; file d'attente gracieuse en cas de dépassement.
- **Module SANTÉ/INFIRMERIE** (G3.11) : inclus par défaut dans CROISSANCE et RÉSEAU & CAMPUS ; activable en option sur DÉCOUVERTE/DÉMARRAGE par le Super Admin (paramétrable avant lancement).
- **Frais d'installation & formation** (one-shot, recommandé Afrique) : Europe **390 €** · Afrique **150 000 FCFA** · Amériques **425 $** → mise en route, import des données, formation responsables.
- **Upgrade / downgrade en self-service** avec prorata ; changement visible sous 24 h.
- Plans **entièrement paramétrables** depuis le Super Admin (prix, quotas, modules inclus, crédits IA).

### Autres inputs requis avant le GO (`[BUSINESS_INPUT]`)

| Domaine | Élément attendu | Usage |
|---|---|---|
| Monnaie & devises | Devise de facturation par région (EUR / XAF / USD) + symboles d'affichage | G1.4 / G3.6 |
| Paiements | Comptes réels Mobile Money (Afrique) + Stripe/SEPA (Europe/Amériques), webhooks, clés sandbox/prod | G3.6 / G6.2 |
| Emailing | SMTP transactionnel, expéditeur, nom de marque des emails | G1.6 |
| Domaines | domaines public (app, api), certificats, noms de sous-domaines par église (option) | G6.9 |
| Églises pilotes | **>3 églises beta : Afrique + Europe + Amériques** (Asie/Océanie en v1.1), données de démo réelles (consentement signé) | G6.9 |
| Juridique | CGU + politique de confidentialité **par région** (RGPD Europe ; loi camerounaise/africaine ; États-Unis) — **CGU par défaut rédigées dans la livraison, à faire valider par un juriste** | G6.9 |
| Base de marque | logo final, couleurs app (si différent du branding dynamique), faire-part de lancement | G1.2 / G6.9 |

## ANNEXE G — Checklist commerciale GO / NO-GO finale

### 1. Les 7 portes
- [ ] **G0** : build vert 3 couches · dépôt propre · tag `v0.10-snapshot-pre-church-os` (étape G0.6)
- [ ] **G1** : contrats multi-tenant complets (G1.12)
- [ ] **G2** : moteurs Church OS complets + migrations annexe A appliquées (G2.11)
- [ ] **G3** : moteurs domaine complets, y compris Dress Code & patrimoine et **Santé/Infirmerie** (G3.12)
- [ ] **G4** : Family OS, pastorat, rôles vivants < 5s + migration legacy (gate G4.7)
- [ ] **G5** : UX 2 niveaux, mobile terrain, **portail basse connexion**, offline ciblé, sync < 5s (G5.10)
- [ ] **G6** : recherche/export/delete, IA/paiements, Redis, non-régression, perf, sécurité, QA, docs, ops (G6.1→G6.9)

### 2. Sécurité & qualité
- [ ] Security Matrix : aucune cellule ⚠️ (G1.10/G6.6)
- [ ] IDOR / cross-tenant / élévation : 0 (G6.6)
- [ ] Performance : budgets p95 atteints à 1K-10K tenants (G6.5)
- [ ] Non-régression : parcours critiques verts (G6.4)
- [ ] Offline : aucune perte de donnée à la coupure (G5.7)
- [ ] Temps réel : web↔mobile < 5s démontré (G5.8)

### 3. Produit & utilisateurs
- [ ] Recette fonctionnelle : les 4 parcours de démo (église 5 espaces / église 150 espaces / famille / campus) aboutissent de bout en bout
- [ ] Guide utilisateur + aide intégrée (G6.8)
- [ ] Thème branding de 2 églises pilotes appliqué sans code (G1.2)
- [ ] Page `/pricing` en ligne : 4 plans **Dual-Market** (DÉCOUVERTE → RÉSEAU), prix EUR ◈ FCFA, badges « 🤖 IA incluse » (G1.4)
- [ ] Crédits IA par plan appliqués et testés (consommation → file gracieuse → recharge) (G6.2)

### 4. Opérations
- [ ] Staging et beta déployés (G6.9) · sauvegarde restaurée avec succès en staging (G6.9)
- [ ] Monitoring alerté et testé (G6.9)
- [ ] Tous les `[BUSINESS_INPUT]` de l'annexe F reçus et intégrés

### 5. Décision
- [ ] `reports/GO_NO_GO_REPORT.md` produit : chaque critère ✓ avec preuve (commit/rapport/capture)
- [ ] Aucun ✗ non justifié · tag `v1.0-commercial-release` + CHANGELOG mis à jour (G6.10)
- [ ] Décision GO/NO-GO formalisée avec l'équipe (le document ne décide pas seul : il fournit l'état des preuves)

---

> **Fin du contrat.** L'agent exécute G0.1 → G6.10 dans l'ordre, une étape par commit, en tenant à jour `docs/ETAT_AVANCEMENT_CHURCH_OS.md`. En cas d'ambiguïté : protocole STOP-NEED-HELP (§0.8). Le Go/No-Go est une décision d'équipe fondée sur les preuves de l'annexe G.
