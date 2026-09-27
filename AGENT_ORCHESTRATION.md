# ORCHESTRATION AGENTS — DISCIPOLAT

> **Document de pilotage pour agents autonomes.** Tout agent (humain ou IA) doit lire
> ce fichier AVANT de commencer, puis le mettre à jour après chaque tâche.
> **Règle absolue : NE SUPPRIMER AUCUN CODE/FONCTIONNALITÉ EXISTANTE.**
> Améliorer, réparer, compléter — jamais dégrader.

---

## 0. MISSION

> ### ⚠️ COORDINATION AVEC UN PLAN EXISTANT
>
> Un plan antérieur existe : **`PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md`**
> (périmètre : correctifs onboarding + gestion tenant, 13 tâches `A1..A13` / `B1..B13`).
>
> **Les deux plans sont compatibles** : même découpage Agent A = backend,
> Agent B = clients, mêmes branches (`fix/onboarding-tenant-backend` /
> `fix/onboarding-tenant-clients`), mêmes worktrees.
>
> **Règle de coexistence** : un agent qui exécute un plan NE DOIT PAS
> exécuter les tâches de l'autre plan tant que le premier n'est pas
> terminé et mergé. Sinon : conflits de merge sur `SecurityConfig`,
> `application.yml`, `frontend/src/App.tsx`, `docker-compose.yml`.
>
> **Ordre recommandé** : terminer d'abord
> `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md` (il corrige des bugs
> fonctionnels), puis enchaîner sur ce fichier (il débloque le
> commercial et l'échelle mondiale).
>
> Ce fichier est **complémentaire**, pas concurrent.

Amener Discipolat au niveau **production mondiale** :
- **Couverture planétaire** : aucune logique métier codée en dur pour l'Afrique
  ou le français. Support natif de toutes les régions, langues, cultes, monnaies, réglements.
- **Échelle industrielle** : capable d'héberger **plusieurs millions de tenants**
  (organisations) représentant **plusieurs centaines de millions d'églises/sites**.
- **Qualité professionnelle** : sécurité, observabilité, i18n, tests, documentation.

---

## 1. ÉTAT RÉEL MESURÉ (2026-09-27) — NE PAS SE FIER AUX ANCIENS RAPPORTS

> ⚠️ **Les rapports `reports/COMMERCIALIZATION_AUDIT.md` (22/08/2026),
> `IMPLEMENTATION_STATUS.md` (31/08/2026) et `CAHIER_DE_CHARGE.md` sont PÉRIMÉS.**
> Ils listent comme "manquants" des éléments qui existent déjà. Vérifier par
> `grep`/`glob` AVANT de conclure quoi que ce soit.

### 1.1 Métriques réelles (mesurées, pas estimées)

| Composant | Fichiers | LOC main | LOC tests |
|-----------|----------|----------|-----------|
| Backend (Spring Boot 3.4.7 / Java 21) | 1 266 `.java` | 113 709 | 27 610 |
| Frontend (React 19 / Vite / TS strict) | 404 `.ts`/`.tsx` | 113 679 | 8 045 |
| Mobile (Flutter 3 / Riverpod / Drift) | 386 `.dart` | 135 399 | 8 988 |
| Migrations Flyway | 146 `.sql` | — | — |
| Fichiers de test | ~273 (137 BE + 4 + 47 FE + 85 MB) | — | — |
| **TOTAL** | **~2 400** | **~363 000** | **~44 600** |

### 1.2 Ce qui est DÉJÀ FAIT (ne pas refaire)

| Domaine | Preuve dans le code |
|---------|--------------------|
| i18n 6 langues (FR/EN/PT/ES/SW/AR) | `frontend/src/i18n/{fr,en,pt,es,sw,ar}.ts` |
| i18n mobile 6 langues | `mobile/lib/l10n/intl_{fr,en,pt,es,sw,ar}.arb` |
| Magic Link | `SocialAuthController.java:135,162` |
| Google OAuth (id_token) | `SocialAuthController.java:202` |
| Rate limiting login (Bucket4j + Redis) | `PerIpRateLimiter.java`, `RedisRateLimiterConfig.java` |
| Rate limit sur 8 endpoints auth | `AuthController.java:42,62,92,121,148,174,187,200` |
| Onboarding wizard 5 étapes | `OnboardingWizardService.java`, `OnboardingWizardPage.tsx` |
| PWA (manifest + service worker) | `frontend/public/manifest.json`, `frontend/public/sw.js` |
| Rôles + RBAC + 2FA TOTP | `SecurityConfig.java`, `TwoFactorServiceTest.java` |
| Isolation multi-tenant | `TenantContext`, filtre Hibernate, `MultiTenantSecurityTests.java` |
| Push token FCM (enregistrement) | `PushTokenController.java` |
| CI/CD + infra | `render.yaml`, `docker-compose.yml`, `.github/workflows/` |
| Monitoring | `infra/monitoring/prometheus.yml`, `grafana-cache-dashboard.json` |

### 1.3 VRAIS MANQUES (vérifiés par grep le 2026-09-27)

| # | Manque | Preuve de l'absence | Sévérité |
|---|--------|---------------------|----------|
| M1 | **Envoi push FCM réel** — seul `PushTokenController` (enregistrement token) existe. Aucun `FirebaseMessaging` / `sendEach` / `sendMulticast` dans tout le backend. Le mobile a `firebase_messaging` mais **ne reçoit jamais rien**. | `grep -r "FirebaseMessaging\|sendEach\|sendMulticast" backend/` → 0 résultat | 🔴 P0 |
| M2 | **Module Backup Java absent** — aucun `backend/.../modules/backup/`. Le cahier de charge annonce `POST /backups/{id}/verify`. Seuls des scripts shell (`scripts/backup.sh`) existent. | `glob backend/**/modules/backup/**` → 0 fichier | 🔴 P0 |
| M3 | **Notifications non câblées dans l'outbox** | `OutboxConsumers.java:141` → `// TODO: Déléguer à NotificationService` | 🔴 P0 |
| M4 | **i18n : chaînes françaises non traduites** dans `sw.ts` (lignes ~1387, 1525, 1539, 1554-1556) et probablement `ar.ts`/`pt.ts`/`es.ts`. Traductions de mauvaise qualité. | `frontend/src/i18n/sw.ts` contient `'Aucun passeport émis'` | 🟠 P1 |
| M5 | **Config STT/Whisper absente de `application.yml`** — `IMPLEMENTATION_STATUS.md` documente `app.speech.api-url/api-key/model` mais la config n'existe pas. | `grep "speech" backend/src/main/resources/application.yml` → 0 | 🟠 P1 |
| M6 | **Ollama codé en dur sur localhost** | `application.yml:190` → `${OLLAMA_URL:http://localhost:11434}` | 🟡 P2 |
| M7 | **Échelle : sharding / partitionnement absent** — mono-PostgreSQL. Objectif 10⁶-10⁸ tenants non atteignable. | `docker-compose.yml` = 1 service `db` | 🔴 P0 (bloquant commercial) |
| M8 | **Fournisseurs de paiement hors Afrique absents** — uniquement MTN/Orange/M-Pesa. Aucun Stripe/PayPal/virement SEPA pour Europe/Amérique. | `grep -r "stripe\|paypal\|sepa" backend/` → 0 | 🔴 P0 (bloquant mondial) |
| M9 | **Régllements non abstraits** — logique de don/transaction liée à XOF et pays africains. | inspecter `modules/payments` | 🔴 P0 |
| M10 | **Aucun test E2E navigateur** — seulement `puppeteer-core` dans `scripts/`. Pas de suite CI bloquante. | `frontend/package.json` → pas de `playwright` | 🟠 P1 |
| M11 | **Aucun test de charge exécuté en CI** — `performance-tests/` existe mais hors CI. | `.github/workflows/` sans k6 | 🟠 P1 |
| M12 | **Écrans Flutter > 1000 lignes** — dette de maintenabilité. | `Etat_fonctionnalité.md:35` le reconnaît | 🟡 P2 |
| M13 | **Aucun test d'isolation inter-tenant sur le flux complet HTTP** — tests unitaires seulement, pas de test bout-en-bout cross-tenant. | `TenantIsolationIntegrationTest.java` existe (couverture réelle) mais à étendre | 🟠 P1 |

---

## 2. VISION MONDIALE — EXIGENCES STRUCTURANTES

### 2.1 Neutralité géographique (à appliquer dans TOUTE nouvelle feature)

Toute fonctionnalité nouvelle **doit** être conçue ainsi dès le départ :

| Dimension | Exigence |
|-----------|----------|
| **Langue** | Aucun texte métier codé en dur côté serveur. Clés i18n. Toute langue ajoutable sans redéploiement (DB + admin). |
| **Fuseau** | IANA timezone par tenant. Jamais d'UTC implicite côté UI. |
| **Monnaie** | ISO-4217 par tenant. Formatage via `Intl.NumberFormat` (web) / `intl` (mobile). Conversion avec taux historisés. |
| **Réglementation** | Payout providers derrière une interface `PayoutProvider` pluggable (Mobile Money, SEPA, ACH, Stripe, PayPal, virement, espèces). |
| **Religieux** | Aucune terminologie confessionnelle codée en dur côté plateforme. "Âme/disciple/faiseur/pasteur" = termes français par défaut, **traductibles et remplaçables par tenant** (dictionnaire de glossaire). |
| **Structure** | Hiérarchie `OrganizationNode` déjà modélisée : ROOT_CHURCH → REGION → CHURCH → SUB_CHURCH → CAMPUS → ASSEMBLY → DEPARTMENT → GROUP. Ne pas la court-circuiter. |
| **Fiscalité** | Reçus configurables par tenant (numéro fiscal, TVA, exemptions, mentions légales). |

### 2.2 Cible d'échelle

| Métrique | Cible | Implication technique |
|----------|--------|-----------------------|
| Tenants | 10⁶ (1 million) | Routage tenant par sharding, pas de `tenant_id` seul indexé |
| Églises/sites | 10⁸ (100 millions) | Agrégation, pas de jointure cross-tenant, OLAP séparé |
| Utilisateurs | 10⁸ | Cache multi-niveau, CDN, files asynchrones |
| Débit | 50 000 req/s | CDN + edge cache + read replicas + partitioning |
| Latence P95 | < 200 ms (lecture) | Cache Redis aggressive, pagination par clé, pas d'offset giant |
| Disponibilité | 99.95 % | Multi-AZ, basculement, pas de point unique de défaillance |

### 2.3 Stratégie de scalabilité (cible, pas implémentation 1-shot)

```
Niveau 1 — Now        : 1 PostgreSQL, partitioning par tenant_id, read replicas
Niveau 2 — 10k+       : sharding applicatif (tenant_id → shard), pool par shard
Niveau 3 — 100k+      : Citus / sharding horizontal, cache Redis par shard
Niveau 4 — 1M+        : multi-cluster, multi-région, routage par région
Niveau 5 — 100M+      : OLAP (ClickHouse/BigQuery) pour analytics, events Kafka
```

**Action immédiate** : poser les fondations du Niveau 1-2 sans casser le
fonctionnement (migrations Flyway, `tenant_id` partout, accès DB derrière une
abstraction `TenantDataSource` permettant le routage futur).

---

## 3. RÈGLES ABSOLUES (tout agent)

1. ❌ **NE SUPPRIMER RIEN** — ni fichier, ni fonction, ni migration, ni chaîne i18n.
   Si quelque chose est cassé, **réparer**. Si obsolète, **désactiver par config**.
2. ✅ **Migration Flyway = ADDITIVE UNIQUEMENT.** Jamais de `DROP`, jamais de
   `ALTER ... TYPE` cassant, jamais de modification d'une migration déjà appliquée.
   Corriger une migration existante = créer une nouvelle migration V+1.
3. ✅ **Commit + push après chaque tâche livrée.** Message de commit conventional
   commits, en français, avec corps expliquant le *pourquoi*.
4. ✅ **Rien ne part sans build vert.** Backend `mvn -q compile` + `mvn test`.
   Frontend `tsc -b` + `vitest run` + `vite build`. Mobile `flutter analyze` + `flutter test`.
5. ✅ **Sécurité fail-closed.** Toute feature flag absente = comportement sûr
   (voir l'existant : quotas fail-closed, bootstrap super-admin durci).
6. ✅ **Aucun secret committé.** Variables d'environnement uniquement.
7. ✅ **Aucun mock/fake en production.** `503` honnête si non configuré (précedent
   existant : `SpeechToTextProvider` → `503 STT_NOT_CONFIGURED`).
8. ✅ **i18n obligatoire** : toute chaîne visible par l'utilisateur passe par une
   clé de traduction dans les 6 langues existantes.
9. ✅ **Tests obligatoires** pour toute correction de bug (test de régression) et
   toute nouvelle feature.

---

## 4. RÉPARTITION PARALLÈLE — 2 AGENTS

### 4.1 Règle de non-conflit (CRITIQUE)

Les deux agents travaillent en parallèle sur des arbres **strictement disjoints**.

| Agent |Propriétaire EXCLUSIF | Interdit |
|-------|----------------------|----------|
| **AGENT A** — Platform & Backend | `backend/**`, `docker-compose.yml`, `render.yaml`, `infra/**`, `scripts/**`, `.github/workflows/**` | ne jamais toucher `frontend/**`, `mobile/**` |
| **AGENT B** — Frontend & Mobile | `frontend/**`, `mobile/**`, `docs/**` | ne jamais toucher `backend/**`, `infra/**` |

**Zones partagées interdites sans accord :**
- `frontend/src/i18n/*.ts` → **Agent B seul** (Agent A ne traduit rien)
- `mobile/lib/l10n/*.arb` → **Agent B seul**
- `docker-compose.yml` → **Agent A seul** (Agent B n'ajoute pas de service)

**Protocole de synchronisation :** chaque agent travaille dans son worktree
(`feat/platform-monde`, `feat/clients-monde`), push sa branche, et ne
fusionne dans `main` que lorsque **son build est vert**. Les deux branches
n'empiètent jamais sur les fichiers de l'autre → fusion sans conflit.

### 4.2 Branches et worktrees

Le plan antérieur impose des **worktrees git séparés** — c'est la bonne
pratique (évite les collisions de `target/`, `node_modules/`,
`.dart_tool/` et de HEAD). **Utilise la même approche :**

```bash
git checkout main && git pull

# Agent A
git worktree add ..\discipolat_app-agentA -b feat/platform-monde

# Agent B
git worktree add ..\discipolat_app-agentB -b feat/clients-monde
```

Chaque agent travaille dans SON worktree. Interdits : `git push` vers la
branche de l'autre, `--force`, `rebase` de la branche de l'autre.

---

## 5. PROMPTS — AGENT A (Platform & Backend)

> À copier-coller tel quel. Exécuter **une tâche à la fois**, dans l'ordre.
> Après chaque tâche : build + tests + commit + push.

---

### ✅ PROMPT A0 — Amorçage et vérification

```
Tu es l'AGENT A du projet Discipolat (plateforme SaaS multi-tenant de gestion
ecclésiastique, Spring Boot 3.4.7 / Java 21 / PostgreSQL 16 / Redis 7).
LIS EN PREMIER : AGENT_ORCHESTRATION.md à la racine du dépôt. Respecte
STRICTEMENT les règles absolues de la section 3, en particulier :
NE SUPPRIMER RIEN, migrations Flyway ADDITIVES uniquement, commit+push
après chaque tâche, build vert obligatoire.

CONTEXTE CRITIQUE : les rapports dans reports/ sont PÉRIMÉS. Ne t'y fie pas.
Vérifie toujours par grep/glob avant d'affirmer qu'un truc manque.

TA MISSION IMMÉDIATE :
1. git worktree add ..\discipolat_app-agentA -b feat/platform-monde
2. Lis AGENT_ORCHESTRATION.md, puis backend/src/main/resources/application.yml
   en entier, puis backend/src/main/java/com/discipolat/common/infrastructure/config/SecurityConfig.java
3. Établis l'état réel de ces 4 points et inscris tes constats dans
   AGENT_ORCHESTRATION.md section 1.3 (mets à jour la colonne "Preuve de l'absence") :
   - M1 : envoi push FCM réel (cherche FirebaseMessaging, sendEach, sendMulticast, FCM_TOKEN)
   - M2 : module Backup Java (cherche le dossier modules/backup)
   - M3 : câblage notifications dans OutboxConsumers
   - M7 : evidence de l'absence de sharding
4. Lance la suite de tests complète et RAPPORTE le résultat réel
   (nombre de tests, nombre d'échecs) : cd backend && mvn -q test
5. Ne code rien pour l'instant. Commit uniquement le rapport de constats :
   "docs(platform): etat reel verifie des manques M1-M3 M7 (rapport perime)"
   et push.

CONTRAINTE : ne modifie aucun code fonctionnel à cette étape.
```

---

### ✅ PROMPT A1 — M1 : Envoi push FCM réel (P0)

```
Tu es l'AGENT A. Contexte : AGENT_ORCHESTRATION.md, worktree agentA (branche feat/platform-monde).
TÂCHE : implémenter l'ENVOI push FCM réel (manquant M1).

ÉTAT ACTUEL : backend/src/main/java/com/discipolat/modules/notifications/api/
PushTokenController.java enregistre les tokens. MAIS aucun envoi n'existe :
aucune dépendance Firebase, aucun appel réseau. L'app mobile (firebase_messaging)
ne reçoit donc jamais de notification. C'est un mensonge fonctionnel à corriger.

OBJECTIF : un service backend qui envoie réellement des notifications push.

ÉTAPES (respecte l'architecture existante) :
1. Ajouter la dépendance Google Firebase Bootlin/admin au backend/pom.xml
   (com.google.firebase:firebase-admin, version gérée par Spring Boot
   dependency management — ne pas figer une version obsolète).
2. Créer une interface PushGateway dans modules/notifications/domain/ avec
   une implémentation FirebaseAdminPushGateway. Une seule méthode
   `send(List<String> deviceTokens, PushMessage message) : PushResult`.
3. Ajouter une implémentation NoOpPushGateway conditionnée par propriété
   `app.push.enabled=false` (défaut). Si false → log warn UNE SEULE fois au
   démarrage, et les envois sont no-op. CE N'EST PAS un échec silencieux
   silencieux : le log doit être explicite. (Voir le précédent
   SpeechToTextProvider → 503 STT_NOT_CONFIGURED.)
4. Configurer Firebase via `app.push.credentials-path` (chemin vers le JSON
   de service account) — JAMAIS le contenu du JSON dans une variable
   d'environnement en clair, JAMAIS committé. Ajouter au .gitignore.
5. Câbler l'envoi : quand une Notification est créée par le scheduler
   (ScheduledJobs) ou par un événement, diffuser via l'outbox
   (modules/core/service/OutboxConsumers.java — c'est le TODO ligne 141) :
   créer la Notification en base (in-app) ET pousser via PushGateway aux
   tokens enregistrés de l'utilisateur, en respectant
   NotificationPreference (l'utilisateur a pu désactiver le canal push).
6. Gérer : token invalide (FirebaseMessagingException INVALID_ARGUMENT /
   UNREGISTERED) → supprimer le token de la base, ne pas boucler.
7. Endpoint GET /api/v1/notifications/push-status → {enabled, configured, reason}
   pour que l'UI et le mobile puissent afficher un état honnête.
8. Configurer application.yml : app.push.enabled (défaut false),
   app.push.credentials-path, app.push.dry-run (défaut true : journalise au
   lieu d'envoyer — indispensable avant d'avoir de vraies credentials).

TESTS (obligatoires) :
- PushGatewayRegistryTest : NoOp sélectionné quand app.push.enabled=false
- PushTokenCleanupTest : token invalide supprimé, pas de boucle infinie
- NotificationPreferencePushTest : push respecté/refusé selon préférence
- Un test d'intégration qui prouve qu'une Notification créée produit bien
  une tentative d'envoi via le gateway (mocké)
- AUCUN test ne doit tenter un vrai appel réseau Firebase.

VALIDATION : cd backend && mvn -q compile && mvn test
Le nombre de tests doit AUGMENTER et aucun test existant ne doit échouer.
Si un test échoue à cause de ta modification, répare ta modification
(jamais le test existant sauf s'il teste un bug avéré).

COMMIT : "feat(push): envoi FCM reel via firebase-admin + outbox cablee
+ push-status honnete (ferme M1, M3)"
PUSH : git push -u origin feat/platform-monde

Si un point exige de SUPPRIMER quelque chose, STOPPE-toi et demande.
```

---

### ✅ PROMPT A2 — M2 : Module Backup/Restore Java (P0)

```
Tu es l'AGENT A. Contexte : AGENT_ORCHESTRATION.md, worktree agentA (branche feat/platform-monde).
TÂCHE : créer le module Backup/Restore côté Java (manquant M2).

ÉTAT ACTUEL : seuls des scripts shell existent (scripts/backup.sh,
scripts/backup-render.sh, scripts/restore.sh, scripts/test-restore.sh).
Le cahier de charge annonce une API `POST /backups/{id}/verify` qui
N'EXISTE PAS. Aucun contrôleur, aucun service, aucune table.

CONTRAINTE FORTE : NE PAS SUPPRIMER les scripts shell. Ils restent
 fonctionnels pour l'infrastructure. Tu AJOUTES une couche applicative.

OBJECTIF : API de backup/restore par tenant, multi-tenant-safe.

ÉTAPES :
1. Nouvelle migration Flyway V{max+1} (lis le plus haut numéro dans
   backend/src/main/resources/db/migration/ et ajoute le suivant, ex V179)
   créant `backup_snapshots` :
   id UUID PK, tenant_id UUID NOT NULL, type VARCHAR (MANUAL|SCHEDULED|PRE_RESTORE),
   status VARCHAR (PENDING|RUNNING|COMPLETED|FAILED|EXPIRED),
   storage_key VARCHAR (chemin S3-compatible, PAS l'URL publique),
   size_bytes BIGINT, checksum_sha256 VARCHAR(64),
   started_at, completed_at, expires_at, error_message TEXT,
   created_by UUID, created_at, deleted BOOLEAN, deleted_at, deleted_by
   + index sur (tenant_id, created_at DESC) et (status)
   + FK tenant_id → tenants(id)
   ADDITIF, pas de DROP.
2. Entité JPA `BackupSnapshot` dans un NOUVEAU module
   `com.discipolat.modules.backup` (api/ + domain/ + infrastructure/)
   — respecte la structure des modules voisins (ex: modules/tontine).
3. Repository avec finding.findByTenantIdOrderByCreatedAtDesc(tenantId) —
   JAMAIS de méthode qui allows.findAll() sans filtre tenant.
4. `BackupService` :
   - `createSnapshot(tenantId, createdBy)` : déclenche la sauvegarde.
   - Implémentation S3-compatible via l'abstraction de stockage déjà
     présente (cherche FileStorageService dans modules/tenants/service/
     et réutilise-la si elle est générique ; sinon crée une interface
     `BlobStore` + une implémentation locale filesystem pour le dev).
   - Propriété `app.backup.enabled` (défaut false) + `app.backup.local-dir`
     pour le développement. Si disabled → 503 honnête avec code
     BACKUP_DISABLED (comme le précédent STT_NOT_CONFIGURED).
   - CRITIQUE : vérifie que le répertoire de destination est isolé par
     tenant (chemin storage_key = "{tenantId}/{yyyy}/{MM}/{dd}/{id}.dump")
     et que la lecture d'un snapshot vérifie Systematicement le tenantId
     courant AVANT d'ouvrir le fichier (protection IDOR + isolation
     fichiers, le point faible identifié dans COMMERCIALIZATION_AUDIT).
5. `verifySnapshot(tenantId, id)` : recalcule SHA-256 et compare au
   checksum_sha256 stocké. Retourne {valid, expected, actual}.
   Route : POST /api/v1/backups/{id}/verify
6. `listSnapshots(tenantId)` : GET /api/v1/backups — paginé, filtré tenant.
7. `restoreSnapshot(tenantId, id, confirmedBySuperAdmin)` : POST
   /api/v1/backups/{id}/restore — exige @PreAuthorize("hasRole('ADMIN')")
   ET une double confirmation (un champ `confirm: true` explicite dans
   le body, sinon 400). Journalise dans audit_logs AVANT et APRÈS.
   Refuse la restauration depuis un snapshot expiré.
8. Ajouter les secrets au .gitignore si nouveau. Aucun secret en clair.
9. Ajouter les entrées de configuration dans application.yml sous
   app.backup.* avec des valeurs par défaut sûres.

TESTS (obligatoires) :
- BackupServiceTest : création, list-Isolation (tenant A ne voit pas
  les snapshots de tenant B), vérification checksum OK et KO,
  refus si disabled (503), refus si expiré.
- BackupSecurityTest : un utilisateur du tenant B ne peut PAS appeler
  verify/restore sur un snapshot du tenant A → 403/404 (jamais 200).
- RestoreConfirmationTest : restore sans confirm=true → 400.
- Chaque test doit prouver l'ISOLATION (c'est le risque n°1 du produit).

VALIDATION : mvn -q compile && mvn test
COMMIT : "feat(backup): module backup/restore java multi-tenant (S3-compatible,
verify sha256, restauration controlee) — ferme M2"
PUSH : git push origin feat/platform-monde
```

---

### ✅ PROMPT A3 — M7 + M8 + M9 : Échelle mondiale et paiements universels (P0)

```
Tu es l'AGENT A. Contexte : AGENT_ORCHESTRATION.md, worktree agentA (branche feat/platform-monde).
TÂCHE : poser les fondations de l'ÉCHELLE MONDIALE. C'est le chantier le
plus structurant. Ne le bâcle pas.

CONTEXTE UTILISATEUR : le produit doit accueillir plusieurs MILLIONS de
tenants et viser plusieurs CENTAINES DE MILLIONS d'églises à terme, et
fonctionner sur TOUTE la planète (pas seulement l'Afrique / francophonie).

RÈGLE : tu ne peux pas résoudre 10^6 tenants en une tâche. Tu poses les
FONDATIONS non-rétrocompatibles et tu documentes la trajectoire. Priorité
au (1) qui est le plus risqué techniquement.

PARTIE 1 — Paiements universels (M8, M9) — LE PLUS BLOQUANT COMMERCIALEMENT

1. Lis modules/payments/ en entier. Identifie TOUT ce qui est lié à
   XOF / MTN / Orange / M-Pesa / pays africains, en particulier :
   - le validateur de devise
   - le formatage des montants
   - les frais / commissions
   - la logique de reçu
2. Introduis une abstraction `PayoutProvider` (interface) :
   `initiate(PayoutRequest) : PayoutResult`,
   `queryStatus(providerRef) : PayoutStatus`,
   `verifyWebhookSignature(headers, body) : boolean`
   Implémentations : MtnMoMoPayoutProvider, OrangeMoneyPayoutProvider,
   MPesaPayoutProvider (EXISTANTES, à adapter — NE PAS SUPPRIMER),
   + nouvelles : StripePayoutProvider (Stripe Connect / PaymentIntents),
     PayPalPayoutProvider, SepaDirectDebitProvider, BankTransferProvider.
   Toutes sous `app.payments.providers.{mtn,orange,mpesa,stripe,paypal,
   sepa,bank}.enabled` (défaut false).
3. Devise : migration V{max+1} + entité. Toute transaction porte
   désormais (montant_minor BIGINT, devise CHAR(3) ISO-4217, taux_vers_base,
   montant_base) pour permettre l'audit multi-devises. Remplis les
   colonnes existantes par le taux 1 pour ne pas casser les données.
4. Formatage : côté serveur, expose GET /api/v1/platform/currencies
   (liste ISO-4217 avec symboles et décimales) pour que le frontend
   utilise Intl.NumberFormat et ne connaisse rien de figé.
5. Reçus : rends numéro fiscal, mentions légales et devise
   CONFIGURABLES PAR TENANT (table tenant_settings ou extension de
   church_settings existante). Pas de valeur globale unique.

PARTIE 2 — Fondations du sharding (M7)

6. Crée une abstraction `TenantDataSource` (interface) qui encapsule
   l'accès base de données par tenant. Implémentation par défaut :
   `SingleDatabaseTenantDataSource` qui délègue au DataSource courant —
   COMPORTEMENT IDENTIQUE à aujourd'hui, zéro régression.
   La signature doit permettre demain un routage vers un autre cluster
   (règle : shard = hash(tenantId) % N, N configurable).
7. Ajoute une migration V{max+1} qui pose les fondements du partitionnement :
   - sur la table la plus volumineuse (identifie-la par un
     SELECT reltuples sur pg_class, documente ton choix),
     CONVERTIR en partitionnement RANGE sur tenant_id
     (ATTENTION : conversion sur une table vide seulement ; sur une table
     peuplée, crée la table partitionnée en parallèle et un script de migration
     documenté dans scripts/ — NE SUPPRIME PAS la table existante).
   - index composé (tenant_id, <colonne frequently filtrée>) partout où
     il manque, sur les 10 tables les plus chaudes. Documente chaque
     choix dans une migration commentée.
8. Documente la trajectoire complète (Niveau 1 à Niveau 5) dans
   docs/SCALING.md : architecture cible, ordre de migration, coût
   estimé par niveau, points de bascule. Sois honnête sur ce qui
   reste à faire.

PARTIE 3 — Nettoyage de l'hypothèse « Afrique/Français »

9. Grep TODO/FIXME/chaînes codées en dur :
   grep -rn "XOF\|FCFA\|Afrique\|MTN\|Orange Money\|M-Pesa\|Pays Africa"
   backend/src/main/java
   Pour chaque occurrence, évalue : est-ce une valeur par défaut
   RAISONNABLE (un défaut neutre est acceptable), ou une hypothèse
   structurante qui casse pour un autre marché ?
   Ne change QUE ce qui casse réellement. Un défaut « XOF » dans une
   config est acceptable si la config est modifiable par tenant ; un
   validateur qui refuse toute devise ≠ XOF est un bug à corriger.
10. Les termes religieux (« âme », « disciple », « pasteur ») doivent
    être surchargeables par tenant via le dictionnaire existant
    (PlatformDictionaryService). Vérifie que ce chemin existe et
    complète-le si manquant.

INTERDITS : ne supprime aucun provider de paiement existant, ne migre
aucune donnée vers un autre schéma, ne renomme aucune table/endpoint
existant sans fournir un alias de compatibilité.

TESTS :
- PayoutProviderRegistryTest : chaque provider s'active/désactive par config
- CurrencyValidationTest : XOF, EUR, USD, KES, BRL, INR acceptés ;
  montants avec décimales rejetés quand devise 0-décimal (JPY, KRW)
- ShardingRoutingTest : hash(tenantId) est stable et distribué
- IsolationBackupCurrencyTest : snapshot d'un tenant illisible depuis un autre

VALIDATION : mvn -q compile && mvn test
COMMIT : "feat(scale): fondations mondiale — payout providers ISO-4217, devises
auditables, sharding abstraction, partitionnement (M7 M8 M9)"
PUSH : git push origin feat/platform-monde
Si une conversion de table partitionnée est risquée à ce stade, NE LA FAIS
PAS : laisse un script documenté dans scripts/ et explique pourquoi.
```

---

### ✅ PROMPT A4 — M5, M6, durcissement configuration honnête

```
Tu es l'AGENT A. Contexte : AGENT_ORCHESTRATION.md, worktree agentA (branche feat/platform-monde).
TÂCHE : rendre la configuration HONNÊTE et SÉCURISÉE (M5, M6, durcissement).

1. M5 — STT/Whisper : IMPLEMENTATION_STATUS.md documente
   `app.speech.api-url/api-key/model` et un provider
   WhisperSpeechToTextProvider existe déjà, mais application.yml ne
   contient AUCUNE clé app.speech.*. Résultat : la feature est
   impossible à configurer sans recompiler. Ajoute le bloc de config
   avec des défauts sûrs, documente dans application.yml (commentaires)
   et dans docs/RUNBOOK.md. Ajoute
   app.speech.timeout-seconds et app.speech.max-file-bytes avec des
   PLAFORMES DURCIES (ex: 25 Mo max) — un upload non borné est un DoS.
2. M6 — Ollama : `ollama-url: ${OLLAMA_URL:http://localhost:11434}`.
   Un défaut localhost en PRODUCTION fait que l'IA tente de joindre
   localhost:11434 et échoue silencieusement. Change le défaut à vide,
   et quand vide → le moteur déterministe de fallback s'active et un
   warning clair est journalisé au démarrage. Ajoute
   app.ai.timeout-seconds (défaut 30) — un LLM lent ne doit pas
   bloquer un thread de requête indéfiniment.
3. Durcissement systemique : grep tous les @Value avec des valeurs par
   défaut qui sont DANGEREUSES en production (secrets, URLs, timeouts
   infinis, "permitAll"). Pour chacun :
   - soit tu le rends fail-closed (défaut sûr),
   - soit tu documentes pourquoi le défaut est sûr.
   Ne casse rien : le mode dev doit continuer à marcher.
4. Ajoute un endpoint GET /api/v1/system/config-summary strictement
   NON sensible : pour chaque feature flag, {key, enabled, configured}.
   AUCUN secret, AUCUNE URL interne complète (masque les credentials dans
   les URLs). Alimente une future page de diagnostic.

TESTS : ConfigSummaryTest (aucun secret dans la réponse — test
d'assertion explicite qu'aucune valeur de env.Privée/SECRET/PASSWORD/
KEY/TOKEN n'apparaît), SpeechConfigTest, AiFallbackTest.

VALIDATION : mvn -q compile && mvn test
COMMIT : "config: durcissement systemique, defauts fail-closed, config-summary
non sensible (ferme M5 M6)"
PUSH : git push origin feat/platform-monde
```

---

### ✅ PROMPT A5 — CI/CD : tests E2E, charge, sécurité bloquants

```
Tu es l'AGENT A. Contexte : AGENT_ORCHESTRATION.md, worktree agentA (branche feat/platform-monde).
TÂCHE : rendre la CI bloquante sur la qualité (M10, M11, M13).

ÉTAT : .github/workflows/ existe mais n'exécute pas de test E2E navigateur,
ni test de charge, ni scan de sécurité. performance-tests/ (k6, JMeter)
existe mais n'est jamais exécuté.

1. Lis tous les fichiers de .github/workflows/ et DUPLIQUE l'existant
   plutôt que d'en créer un nouveau concurrent.
2. Workflow CI principal — doit BLOQUER le merge si :
   - mvn test (backend) échoue
   - tsc -b OU vitest run (frontend) échoue  [étape à confirmer avec Agent B]
   - flutter analyze signale une erreur  [Agent B]
3. Nouveau workflow security.yml (plan gratuit) :
   - gitleaks (scan de secrets — AUCUN secret ne doit jamais être committé)
   - dependency review / npm audit backend / flutter pub audit
   - Bandit ou equivalent pour le code Python des scripts/
4. Nouveau workflow e2e.yml : Playwright côté web. Attention : tu ne
   touches PAS au code frontend (propriété Agent B) — tu crées
   UNIQUEMENT le workflow CI et la configuration dans e2e/ (racine).
   Lejour où Agent B livre la suite, elle s'exécute automatiquement.
5. Nouveau workflow perf.yml (déclenché sur main et sur tag, ou en
   manuel/schedule) : k6 avec le scénario existing performance-tests/k6-load-test.js.
   Seuils : si P95 > 2000 ms → échec (trace le résultat dans l'artifact).
6. Étends la couverture d'isolation multi-tenant : lis
   backend/src/test/java/com/discipolat/security/TenantIsolationIntegrationTest.java
   et ajoute un test bout-en-bout HTTP qui prouve qu'un utilisateur
   authentifié du tenant A obtient 403/404 sur les endpoints
   REST de CHAQUE module du tenant B. Automatise : parcours au moins
   souls, families, departments, events, reports, payments, users,
   settings, backups. C'est le test qui protège contre le risque
   juridique n°1 du produit.
7. Upload des rapports de tests comme artifacts (crucial pour diagnostiquer).

CONTRAINTE : les workflows doivent tourner sur le plan gratuit GitHub
(minutes limitées). Mets en cache Maven/NPM/Pub agressivement.

VALIDATION : valide la syntaxe YAML de chaque workflow
(python -c "import yaml,sys;yaml.safe_load(open(f))" ou équivalent).
Ne peux pas exécuter la CI localement : assure-toi qu'elle est correcte
par relecture, et dis-le explicitement dans ton rapport final.

COMMIT : "ci: e2e + charge + securite + isolation multi-tenant bout-en-bout
bloquantes (ferme M10 M11 M13)"
PUSH : git push origin feat/platform-monde
```

---

### ✅ PROMPT A6 — Documentation professionnelle

```
Tu es l'AGENT A. Contexte : AGENT_ORCHESTRATION.md, worktree agentA (branche feat/platform-monde).
TÂCHE : documentation API et exploitation. Manque majeur reconnu :
"Documentation 4/10 — aucune doc utilisateur, README minimal".

1. README.md : réécris-le. Aujourd'hui il fait 21 lignes. Il doit
   contenir : ce que c'est, les 6 langues, capture d'architecture ASCII,
   stack technique, prérequis (Java 21, Node 22, Flutter 3.35+, Docker),
   démarrage local pas-à-pas (docker compose up puis start-local.sh),
   comptes de démo par RÔLE (ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE,
   FAISEUR, MEMBRE) — génère la liste RÉELLE depuis start-local.sh et
   les seeds, ne les invente pas, variables d'environnement requises,
   section "Architecture mondiale" avec renvoi docs/SCALING.md.
2. docs/API.md : génère la référence depuis le code (Springdoc est déjà
   la dépendance : /v3/api-docs). Ne fais pas ça à la main : écris un
   script scripts/generate-api-docs.sh qui appelle l'endpoint et produit
   openapi.json + un Markdown lisible, committé. Vérifie le path exact
   du springdoc dans application.yml d'abord.
3. docs/DEPLOYMENT.md : déploiement production réel. Reprends les
   informations déjà présentes dans render.yaml et docker-compose.yml
   (ils sont la source de vérité, ne les contredis pas). Couvre :
   variables d'environnement SECRETS (la liste exacte, avec quels sont
   obligatoires vs optionnels), migrations Flyway en production,
   sauvegardes, rotation des clés JWT, plan de rollback.
4. docs/RUNBOOK.md : procédures d'exploitation. Incidents
   courants ET leur résolution : API 5xx, pool PostgreSQL saturé,
   Redis indisponible (le rate limiting doit dégrader proprement,
   pas tout casser — vérifie le comportement de PerIpRateLimiter),
   migrations échouées, fuite mémoire JVM, certificat expiré.
   Format : symptôme / diagnostic / résolution, en tableaux.
5. Mets à jour AGENT_ORCHESTRATION.md section 1.2 pour y inscrire ce
   qui est désormais livré.

RÈGLE : ne promets dans la doc que ce que le code fait VRAIMENT.
Vérifie chaque affirmation par grep. Une doc fausse est pire que pas
de doc.

COMMIT : "docs: README, API, deployment, runbook professionnels et veridiques"
PUSH : git push origin feat/platform-monde
```

---

## 6. PROMPTS — AGENT B (Frontend & Mobile)

> Mêmes règles absolues. Mêmes discipline de build. Mêmes interdits.

---

### ✅ PROMPT B0 — Amorçage et audit de l'i18n

```
Tu es l'AGENT B du projet Discipolat. Frontend React 19 + Vite + TypeScript
strict + TailwindCSS ; Mobile Flutter 3 + Riverpod + Drift + GoRouter.
LIS EN PREMIER : AGENT_ORCHESTRATION.md. Respecte la section 3.
NE SUPPRIMER RIEN — surtout pas une clé de traduction manquante :
une clé manquante se COMPLÈTE ou se MAPPE, jamais ne se supprime.

TA MISSION IMMÉDIATE :
1. git worktree add ..\discipolat_app-agentB -b feat/clients-monde
2. Lance les builds de référence et RAPPORTE les résultats RÉELS :
   cd frontend && npm ci && npx tsc -b && npx vitest run 2>&1 | tail -30
   cd mobile && flutter analyze 2>&1 | tail -20 && flutter test 2>&1 | tail -20
   Note : l'environnement peut ne pas avoir Flutter/Node. Si un outil
   manque, DIS-LE clairement et travaille sur ce que tu peux
   (lecture, analyse statique, tests Vitest uniquement).
3. Audit i18n — c'est ton chantier prioritaire (M4). Fais un script
   scripts/... NON, c'est la zone Agent A : mets le script dans
   frontend/scripts/i18n-audit.mjs (zone Agent B).
   Le script doit, pour les 6 langues :
   - lister les clés présentes dans fr.ts (référence) et absentes ailleurs
   - lister les clés présentes ailleurs et absentes de fr.ts
   - détecter les clés NON TRADUITES, c'est-à-dire dont la valeur
     est identique à la valeur française pour les langues non-anglaises,
     OU dont la valeur contient des caractères latins pour ar.ts
     alors que la clé est censée être traduite
   - vérifier que les 6 fichiers exportent le même type TypeScript
     (sinon tsc échoue)
   Exécute-le et RAPPORTE le chiffre exact par langue.
4. Rédige le rapport dans AGENT_ORCHESTRATION.md section 1.3 en
   remplaçant la ligne M4 par les chiffres réels.
5. Commit : "docs(frontend): audit i18n 6 langues — X cles manquantes,
   Y non traduites" et push.

NE CORRIGE PAS encore les traductions à cette étape. Mesure d'abord.
```

---

### ✅ PROMPT B1 — M4 : Qualité i18n (P1)

```
Tu es l'AGENT B. Contexte : AGENT_ORCHESTRATION.md, worktree agentB (branche feat/clients-monde).
TÂCHE : corriger la qualité de l'i18n web et mobile. 6 langues doivent être
parfaitement cohérentes : FR (référence), EN, PT, ES, SW, AR (+ RTL).

Le rapport de B0 a établi le nombre exact de clés à traiter. TRAITE-LES.

RÈGLES :
- NE SUPPRIME AUCUNE CLÈDE TRADUCTION.
- Pour chaque clé manquante dans une langue, AJOUTE la traduction.
  Si tu n'es pas sûr de la traduction, le comportement par défaut doit
  être un fallback propre vers le français, et la clé doit rester
  présente avec la meilleure traduction dont tu es sûr.
- Vérifie que la gestion RTL est réelle pour ar.ts : dir="rtl" sur <html>,
  pas de margin-left/margin-right hardcodés dans les composants qui
  cassent en RTL, icônes directionnelles inversées.

TÂCHES CONCRÈTES :
1. Complète les clés manquantes dans les 6 fichiers frontend/src/i18n/*.ts
2. Corrige les valeurs non traduites repérées (notamment les chaînes
   françaises laissées dans sw.ts et les autres langues)
3. Même travail sur mobile/lib/l10n/intl_*.arb (6 fichiers) puis
   flutter gen-l10n si l'outil est disponible, sinon édite le
   app_localizations.dart généré À LA MAIN de façon cohérente
4. Ajoute un contrôle CI-friendly : le script i18n-audit.mjs doit
   exposer un exit code non-zéro si le nombre de clés manquantes
   régresse (échec si > 0). Il doit pouvoir être appelé par l'Agent A
   dans la CI.
5. Ajoute un test Vitest qui échoue si les 6 langues n'ont pas le même
   ensemble de clés. Ce test est la garantie permanente.
6. Interface : vérifie et corrige les libellés user-facing hardcodés en
   français dans les pages/éléments qui ne passent PAS par i18n.
   grep les chaînes françaises en dur dans frontend/src/pages et
   frontend/src/components, et migre-les vers i18n.

VALIDATION :
  cd frontend && npx tsc -b && npx vitest run
  Les tests existants ne doivent pas échouer. Si un test attend une
  chaîne française et que tu la traduis, le test doit être adapté
  (c'est legitimate : le comportement attendu change).

COMMIT : "feat(i18n): 6 langues coherentes, 0 cle manquante, test de non-regression
i18n, RTL corrige (ferme M4)"
PUSH : git push origin feat/clients-monde
```

---

### ✅ PROMPT B2 — PWA offline réelle + notifications (P0)

```
Tu es l'AGENT B. Contexte : AGENT_ORCHESTRATION.md, worktree agentB (branche feat/clients-monde).
TÂCHE : rendre la PWA réellement utilisable offline et brancher l'état du push.

ÉTAT ACTUEL : frontend/public/manifest.json et frontend/public/sw.js
existent mais le service worker est basique. Le backend a PushTokenController
(Agent A ajoute l'envoi FCM). Sans travail frontend, l'utilisateur ne
   recevra rien et ne pourra pas fonctionner offline.

PARTIE 1 — Service worker professionnel
1. Lis frontend/public/sw.js en entier. Réécris-le proprement SANS
   supprimer les fonctionnalités existantes :
   - stratégie de cache explicite par type de ressource :
     * navigation (HTML) : network-first avec fallback offline page
     * assets buildés (hachés dans filename) : cache-first, immuables
     * API GET : stale-while-revalidate, AVEC QUOTA STRICTE (ne jamais
       mettre en cache une réponse tenant-sensible sans le dire)
   - invalidation propre à chaque nouveau déploiement (version de cache
     = hash du build, purge des anciens)
   - stratégie de mise à jour : le nouveau SW attend, l'UI propose
     "Mise à jour disponible → Recharger" (pas de reload forcé en
     plein milieu d'une saisie)
2. Ajoute une page offline.html soignée (dans le ton visuel de l'app,
   pas une page HTML brute) : explication honnête, bouton "Réessayer",
   et liste des pages accessibles hors ligne.
3. Enregistre le SW dans l'app React avec une stratégie propre
   (composant hook dédié) : seulement en production, seulement une fois,
   avec gestion de l'échec d'enregistrement (silencieux mais journalisé).
4. Affiche un indicateur Offline/Online cohérent (le mobile a déjà
   OfflineBanner — inspires-en, ne le duplique pas).
5. Sécurité : le SW ne doit JAMAIS mettre en cache les réponses API
   contenant des données d'un autre tenant. Comme l'API est par JWT et
   non par cookie, le cache navigateur est cloisonné par navigateur :
   documente ce point honnêtement dans un commentaire et limite le cache
   API aux endpoints explicitement listés comme non-sensibles
   (ex: config plateforme publique, plans tarifaires). Tout le reste
   → réseau seul.

PARTIE 2 — Push
6. Vérifie si le web reçoit déjà des notifications push. Si non, il
   faut : permission demandée au bon moment (pas à l'installation),
   service worker capable de recevoir push et d'afficher une notification.
   Branche sur GET /api/v1/notifications/push-status (Agent A) pour
   afficher un état honnête : si le push n'est pas configuré côté
   serveur, l'UI doit le dire clairement plutôt que d'échouer
   silencieusement. (Cohérent avec la philosophie existante 503 honnête.)
7. Enregistre le token : POST /api/v1/notifications/register-token
   (route déjà présente côté backend) et deregister à la déconnexion.
   Gère le cas "permission refusée" sans casser l'app.

PARTIE 3 — Bottom navigation mobile
8. L'audit signale que la navigation basse mobile manque. Ajoute-la
   dans mobile/ : 4-5 destinations les plus utilisées par rôle,
   cohérentes avec les rôles existants (MEMBRE n'a pas accès dashboard
   pasteur, etc.). Badge de notification non lus. État actif clair.
   Les 159 routes GoRouter existent déjà : ne les modifie pas, branche-toi dessus.

TESTS (Vitest) :
- sw.js : test du comportement de cache sur asset vs API
- PushPermissionTest : permission demandée au bon moment, refus géré
- OfflineBannerTest : visible offline, caché online
- BottomNavTest (Flutter) : destinations correctes par rôle
CRITIQUE : n'écris aucun test qui dépende d'un vrai service worker dans
jsdom (utilise des mocks).

VALIDATION : cd frontend && npx tsc -b && npx vitest run && npx vite build
COMMIT : "feat(pwa): service worker prod (strategies de cache, offline page,
update strategy) + push web branche + bottom nav mobile"
PUSH : git push origin feat/clients-monde
```

---

### ✅ PROMPT B3 — Performance web (P0)

```
Tu es l'AGENT B. Contexte : AGENT_ORCHESTRATION.md, worktree agentB (branche feat/clients-monde).
TÂCHE : performance de l'application web. Objectif : P95 < 200 ms API,
web < 2 s de chargement perçu. L'audit signale un chunk Recharts
~443 KB et des dashboards lourds.

1. MESURE D'ABORD. Lance un build et relève la taille réelle des chunks :
   cd frontend && npx vite build 2>&1 | tail -40
   Note chaque chunk > 150 KB. Ne devine pas.

2. Découpe Recharts : chaque page qui l'utilise doit l'importer
   DYNAMIQUEMENT, ou mieux, remplacer les graphiques lourds par du SVG
   maison léger pour les cas simples (sparkline, jauge, mini barre).
   Un mini-graphique ne doit pas coûter 400 KB.
   Ne supprime pas Recharts : il reste utile pour les vrais graphiques.

3. Leaflet : charge uniquement sur les pages carte (lazy), et ne charge
   pas les tuiles tant que la carte n'est pas visible.

4. TanStack Query : audit des requêtes. Corrige :
   - clés de cache qui ne sont PAS tenant-aware → BUG DE FUITE DE DONNÉES.
     Cherche les queryKey qui n'incluent pas l'identifiant du tenant.
     C'est le point le plus grave de cette tâche.
   - refetchOnWindowFocus trop agressif (multiplie la charge serveur)
   - pagination : toutes les listes doivent être paginées côté serveur,
     jamais de fetch de liste complète.
   - déduplication et staleTime adaptés par type de donnée.

5. Images : loading="lazy" + dimensions réservées (évite le CLS).

6. Accessibilité perçue : squelettes de chargement (shimmer) sur les
   pages longues, états vides explicites et actionnables.
   Vérifie qu'il existe un composant EmptyState réutilisable (il en
   existe un dans components/ui) — UTILISE-LE partout où il manque,
   ne le duplique pas.

7. Confirmation avant action destructive : vérifie qu'un ConfirmDialog
   existe (il existe dans components/ui) et qu'il est utilisé sur toutes
   les suppressions. grep les handlers de suppression et vérifie.

TESTS : ne casse aucun test existant. Ajoute un test si tu introduces
un composant.

VALIDATION : npx tsc -b && npx vitest run && npx vite build
RAPPORTE le gain de taille obtenu (avant/après) dans le message de commit.
COMMIT : "perf(web): chunks decoupes, Recharts allonge, query keys tenant-aware,
pagination serveur, CLS et skeletons"
PUSH : git push origin feat/clients-monde
```

---

### ✅ PROMPT B4 — Mobile : dette et robustesse (P1)

```
Tu es l'AGENT B. Contexte : AGENT_ORCHESTRATION.md, worktree agentB (branche feat/clients-monde).
TÂCHE : mobile Flutter — dette de maintenabilité et robustesse.

CONTEXTE : 386 fichiers .dart, 135 399 lignes, 157 écrans, 159 routes
GoRouter, 85 fichiers de test. L'IMPLEMENTATION_STATUS.md signale
6 tests network_screen_test en échec (timeout pumpAndSettle) et
4 écrans > 1000 lignes.

1. RÉPARE D'ABORD LES TESTS EN ÉCHEC. Les 6 tests
   mobile/test/network_screen_test.dart échouent sur un timeout
   pumpAndSettle. C'est un bug RÉEL de la suite de tests, pas de la
   production. Diagnostique : pumpAndSettle ne termine pas quand il y a
   une animation/processus continu (progress indicator, stream, etc.).
   Utilise pump() avec des durées explicites là où pumpAndSettle ne
   converge pas. Si le widget a une boucle infinie légitime (loading
   en rotation), mock-la dans le test.
   OBJECTIF : mobile/test en 100 % vert. Ne désactive JAMAIS un test
   avec un @Skip ou en commentant des assertions.

2. Refactorise les écrans > 1000 lignes : extrais les widgets, les
   modèles de vue et les helpers dans des fichiers dédiés sous
   lib/presentation/. REFACTORISATION PURE : comportement identique,
   tests existants toujours verts. Va-y écran par écran, un commit
   par écran, jamais un commit qui casse.

3. Offline-first : vérifie que les parcours CRITIQUES fonctionnent
   hors ligne (consulter mon profil, voir mes âmes assignées, saisir
   un rapport de visite, prier). Le module offline_sync_manager existe.
   Corrige ce qui casse. Un rapport de visite saisi hors ligne et
   perdu est une perte de données inacceptable.

4. Sécurité mobile : vérifie FLAG_SECURE (Android), secure storage,
   pas de token en clair dans SharedPreferences. Ajoute une détection
   de root/jailbreak si absente (avec un comportement dégradé honnête,
   pas un blocage qui empêcherait l'usage légitime).

5. Accessibilité : contraste, cibles tactiles >= 48dp, libellés
   accessibles sur les icônes, navigation clavier/screen-reader.
   Ajoute Semantics() là où il manque sur les actions critiques.

6. Offline banner, empty states, error states : cohérents avec le web.

TESTS : tout doit rester vert. Ajoute des tests pour chaque
comportement corrigé.

VALIDATION : cd mobile && flutter analyze && flutter test
Si Flutter n'est pas installé sur ta machine, DIS-LE et fais au minimum
la revue statique + les tests Vitest/web. Ne prétends jamais avoir
exécuté une commande que tu n'as pas exécutée.

COMMIT : "fix(mobile): 6 tests network verts, refactoring ecran X, offline-first
critique, accessibilite"
PUSH : git push origin feat/clients-monde
```

---

### ✅ PROMPT B5 — Documentation utilisateur (P0 commercial)

```
Tu es l'AGENT B. Contexte : AGENT_ORCHESTRATION.md, worktree agentB (branche feat/clients-monde).
TÂCHE : documentation utilisateur. L'audit donne "Documentation 4/10 —
aucune doc utilisateur". C'est un BLOCKER COMMERCIAL : une église ne sait
pas utiliser l'app → elle ne paie pas.

1. Crée docs/UTILISATEUR/ avec un guide par RÔLE (il y en a 6 :
   ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR, MEMBRE).
   Pour chaque rôle, en français ET en anglais :
   - Ce que ce rôle peut faire / ne peut pas faire (tableau clair)
   - Ses 5 premiers gestes concrets, étape par étape
   - Les 3 erreurs les plus fréquentes et leur solution
   - Les raccourcis qui font gagner du temps
   VERIFIE chaque affirmation contre le code (lis workspaces.ts pour
   la navigation réelle de chaque rôle, et les @PreAuthorize côté backend
   si tu as besoin). Ne documente pas une fonctionnalité qui n'existe pas.

2. Guide de démarrage pour UNE ÉGLISE (5 minutes) : de l'inscription
   au premier rapport submitted. Numéroté, avec captures référencées par
   emplacement (mets des placeholders clairement nommés si tu ne peux pas
   produire les captures — ne fabrique pas de fausses captures).

3. Guide ADMINISTRATEUR PLATEFORME : provisionner un tenant, gérer les
   plans, les quotas, les feature flags (tout existe déjà dans
   modules/platform et modules/tenants).

4. FAQ (20 questions) : les questions qu'un pasteur poserait réellement.
   ("Comment rattraper un membre absent depuis 6 mois ?",
   "Comment fonctionne la note d'un disciple ?",
   "Puis-je annuler un don ?", "CommentImporter mon Excel ?"...)

5. Dans l'APPLICATION elle-même : ajoute un lien "Aide" vers la doc dans
   le menu utilisateur, et des tooltips contextuels sur les 15 actions
   les moins intuitives (créer un rapport, lancer un scan QR, faire un
   don, etc.). Un tooltip = 1 phrase, pas un roman.

6. Les docs doivent être navigables depuis docs/ avec un index
   (README.md dans docs/UTILISATEUR/).

VALIDATION : npx tsc -b && npx vitest run
COMMIT : "docs(user): guides par role x6 bilingue, demarrage 5 min, FAQ 20,
tooltips in-app"
PUSH : git push origin feat/clients-monde
```

---

## 7. SÉQUENCE RECOMMANDÉE

```
Jour 1    A0 + B0          (état réel, mesures)        — parallèle
Jour 2-3  A1 + B1          (FCM push + i18n qualité)   — parallèle, sans conflit
Jour 4-5  A2 + B2          (Backup Java + PWA/push)    — parallèle, sans conflit
Jour 6-8  A3               (monde + échelle)           — seul (structurant)
Jour 6-8  B3               (perf web)                   — parallèle de A3
Jour 9-10 A4 + B4          (durcissement + mobile)      — parallèle
Jour 11   A5 + B5          (CI bloquante + doc)         — parallèle
Jour 12+  Merge main, revue croisée, tag bêta
```

**Contrainte de fusion :** `main` n'est fusionné que si la CI est verte
ET qu'un agent a relu le travail de l'autre. Ne jamais fusionner deux
branches qui modifient les mêmes fichiers.

---

## 8. MODÈLE DE RAPPORT (à remplir en fin de tâche)

Chaque agent termine sa tâche par un rapport dans ce format :

```
## RAPPORT — <TÂCHE> — <DATE>

### Fait
- <livrable concret avec chemin de fichier>

### VÉRIFIÉ (avec preuve)
- <commande exécutée> → <résultat réel, chiffres>

### NON FAIT / REFUSÉ
- <ce qui n'a pas été fait, et pourquoi>

### RISQUES INTRODUITS
- <ou AUCUN>

### PROCHAINE ÉTAPE
- <tâche suivante>
```

## 9. RÈGLE DE SUPPRESSION

Si une tâche exige absolument de supprimer du code existant :
1. **NE SUPPRIME PAS.**
2. Écris dans ce fichier une section `## 🔴 DEMANDE DE SUPPRESSION` avec :
   - le fichier et les lignes concernées
   - la preuve que c'est un problème (erreurs, failles, duplications)
   - ce qui se passe SI on ne supprime pas
   - ce qui se passe SI on supprime (conséquences précis)
   - une proposition alternative non destructive si elle existe
3. Passe à la tâche suivante en attendant la décision.

---

*Dernière mise à jour : 2026-09-27 par l'agent orchestrateur.*
