# SPEC_ORGANISATION_DENOMINATION_V2 — Modèle d'organisation, flux d'entrée, console plateforme et transfert

> **STATUT** : spécification **VALIDÉE** par le produit le 04/10/2026.
> **VERSION DU DOCUMENT** : v2.1 — complétée le 04/10/2026 par l'**audit strict du commit `7390651`** (§2.1 : 14 failles relevées, dont **3 P0 bloquants**). Les Flows §4 inachevés ont été complétés, les sections §7.2 orphelines ont été remises à leur place.
> **DESTINATAIRES** : agents d'implémentation **backend (Java/Spring Boot)**, **web (React/TS)**, **mobile (Flutter)**.
> **RÈGLE D'OR** : aucun agent ne code avant d'avoir lu les §0 à §4. Les §5 à §10 sont des ordres d'exécution, pas des suggestions.
> **HÉRITAGE** : prolonge `docs/SPEC_ONBOARDING_FLOWS.md` (spéc V1). **Ne pas relire celle-ci pour le modèle d'organisation** : elle décrit un tenant = une église, ce qui est désormais faux. Voir §1.2. En revanche V1 reste la référence pour le **format des codes** et le **workflow des annonces**.

> ### ⚠️ ORDRE D'EXÉCUTION IMPÉRATIF (ajouté par l'audit)
> **T-B0, T-W0 et T-B0bis (§7.0) ne sont pas optionnels : ce sont des corrections de production qui rendent le reste testable.**
> Le commit `7390651` est **non livrable** en l'état : la console d'église répond `403` à son utilisateur cible, l'écran fondateur renvoie vers `/login`, et la rejointure ne bascule pas de tenant. **Construire `V222` par-dessus sans les corriger revient à empiler des migrations sur une base non fonctionnelle.**

---

## §0. POURQUOI CE DOCUMENT EXISTE

Un audit du dépôt (`7390651`, 04/10/2026 07:24) a mis au jour que la proposition initiale — « un tenant = une église » — ne couvre pas la réalité du produit. Un tenant est, selon le client :

- une **église** isolée ;
- une **dénomination** qui fédère plusieurs églises ;
- une **association**, une **organisation**, un **organisme** ;
- une **méga-association** avec une structure hiérarchique.

Ces derniers cas sont aujourd'hui **impossibles à modéliser** : `Tenant` n'a aucune notion de parenté, de nature, ni de racine. Un membre qui change d'église doit se réinscrire. La console Super Admin laisse fuiter des menus d'église. Ce document ferme ces trous.

---

## §1. MODÈLE MENTAL

### 1.1 Le principe fondateur

> **On appartient à une ORGANISATION, pas à un tenant.**

Le compte (`User`) est **global** : une identité, un mot de passe, une seule fois pour toutes. Ce qui est local, c'est **l'appartenance** (`TenantMembership`). Deux conséquences immédiates :

1. Changer d'église **n'est jamais** une réinscription. C'est un **transfert** d'appartenance.
2. Le Super Admin n'appartient à aucune église : il administre des organisations. Il n'a donc **aucune** interface d'église.

### 1.2 Les trois couches

| Couche | Table porteuse | Rôle | Qui la voit |
|---|---|---|---|
| **Identité** | `users` | La personne. Unique, globale, jamais dupliquée. | La personne + admins de ses organisations |
| **Organisation** | `tenants` (+ `parent_tenant_id`, `root_tenant_id`, `kind`) | L'entité : dénomination, église, asso, méga-asso. | Ses membres |
| **Appartenance** | `tenant_memberships` | Le lien personne ↔ organisation, avec rôle + périmètre. | Idem |

`OrganizationNode` (nœuds : `ROOT_CHURCH`, `CAMPUS`, `SUB_CHURCH`, `GROUP`…) reste **à l'intérieur** d'un tenant. C'est la couche « légère » : un campus, un groupe, un quartier. Un tenant peut donc contenir des nœuds **et** être rattaché à un parent.

### 1.3 Les deux modes de sous-église (modèle hybride, validé)

```
┌──────────────────────────────────────────────────────────────────┐
│  ORGANISATION RACINE   kind = DENOMINATION | MEGA_ASSOCIATION   │
│  (le « groupe WhatsApp »)   —   le propriétaire = LE ROI        │
└───────────────┬───────────────────────────────┬──────────────────┘
                │                               │
   MODE LÉGER (nœud)                MODE AUTONOME (tenant fils)
   ┌────────────────────────┐         ┌────────────────────────┐
   │ Campus Nord            │         │ Église de la Grâce        │
   │  = OrganizationNode    │         │  = tenant.kind=CHURCH   │
   │  parent=ROOT_CHURCH    │         │  parent_tenant_id =     │
   │  MÊMES données que     │         │          la racine      │
   │  l'église parente      │         │  PROPRES membres,       │
   │  → simple re-périmètre │         │  PROPRE facturation     │
   │    de la membership    │         │  → tenant distinct      │
   └────────────────────────┘         └────────────────────────┘
```

- **Mode LÉGER** : aucun tenant créé. On change le `scopeId`/`scopeType` de la `TenantMembership`. **Zéro migration de données.** Adapté aux groupes, secteurs, ministères.
- **Mode AUTONOME** : un vrai tenant fils, avec son stock de membres, ses quotas, sa facturation. Adapté aux églises d'une dénomination qui veulent leur autonomie.

**Le roi** (owner de la racine) gouverne, délègue (`promote-admin` existe déjà), et voit des **agrégats** de ses enfants — jamais leurs membres nominatifs.

### 1.4 Le transfert : la pièce manquante

> **Un membre qui passe de l'église A à l'église B, dans la MÊME dénomination, ne se réinscrit pas.**

Il est déjà connu de la plateforme. Il lui faut : **un transfert** + **le code de sa nouvelle église**. C'est écrit noir sur blanc dans le code (§4.4, §7.1 T-B5). Détecteur : `root_tenant_id` identique.

---

## §1bis. RÉSUMÉ POUR DÉCIDEUR — l'état en une lecture

**Ce que le commit `7390651` a livré de solide** : les 2 CTA du landing, le modèle de code `PREFIXE-XXXX`, les migrations V219-V221 (parité entités↔schéma **correcte**, vérifiée à la main), le workflow de modération des annonces, la gouvernance (blocage / ban / avertissements / litiges), les endpoints de propriété. **Le socle est bon et ne doit pas être refait.**

**Ce qui manque pour que ce soit un produit** : trois P0 (§2.1) qui rendent les parcours **non fonctionnels pour l'utilisateur visé**, plus **deux interfaces entières manquantes** qui sont la demande centrale du client.

| La demande du client | Où elle en est |
|---|---|
| « codes **par sous-église** » | API ✅ / **interface ❌** (F14) |
| « **un lien** avec le pseudo de l'église **ou** un code » | code seul à l'écran ; lien **en cours** (F21) |
| « il peut **déléguer**, se défaire du rôle, **choisir un remplaçant** » | API ✅ / **interface ❌** (F13) |
| « un tenant = une communauté qui accueille plusieurs sous-tenant » | modèle de données ❌ (F5) + **transfert de membre ❌** (F6) |
| « espace annonce / pub visible sur le landing » | ✅ mais **XSS stocké** à corriger (F8) |
| « le Super Admin : uniquement la configuration des tenants » | **partiel** — routes et actions rapides encore ouvertes (F1/F2/F12) |

**Budget** : P0 + garde-fou CI ≈ **4 h**. Les deux interfaces manquantes (F13, F14) ≈ **1 jour**. Le reste (§5, §7.1–7.4) est du travail de fond déjà planifié.

---

## §2. AUDIT — L'ÉTAT RÉEL AU 04/10/2026 (à lire avant de coder)

Le commit `7390651` a livré un **socle réel** : il ne faut pas le refaire. Mais l'audit strict a **corrigé trois lignes du tableau d'origine** (qui déclaraient « FAIT » des choses cassées) et ajoute **20 failles**, dont **3 P0 bloquants**. **Lire §2.1 avant toute tâche.**

| Besoin | État réel | Preuve |
|---|---|---|
| 2 CTA sur le landing | ✅ FAIT | `Hero.tsx:73,77` |
| **Écran fondateur : lien + copie + WhatsApp** | 🟡 **EN COURS** (hors commit `7390651`) | `RegisterPage.tsx` modifié + `RegisterPageInviteLink.test.tsx` non commités → §11 |
| Création église = `TENANT_OWNER` direct | ⚠️ FAIT **mais session cassée** | `SelfServiceChurchService` ✔ / `RegisterPage.tsx:113` ✘ → **F9** |
| Codes `PREFIXE-XXXX` non ambigus | ✅ FAIT | `JoinCodeService` (alphabet sans `I L O U 0 1`) |
| **Code saisi une seule fois** | ❌ **FAUX** | `markActiveTenant` sans réémission de jeton → **F7** |
| **Console d'église accessible à l'admin** | ❌ **403** | `hasAnyRole('TENANT_OWNER')` inatteignable → **F10** |
| Annonces publiques + modération | ⚠️ FAIT **mais XSS stocké** | V220 ✔ / `SectionAnnouncements:86-90` ✘ → **F8** |
| Gouvernance (blocage/ban/litiges) | ⚠️ PARTIELLE — **motif non tracé** | `TenantGovernanceService:176-188` → **F11** |
| Délégation / abdication propriétaire | ⚠️ **API seule, ZÉRO interface** | 5 endpoints morts ; `grep ownership frontend/src` = 0 → **F13** |
| Console plateforme isolée | ⚠️ PARTIELLE | failles **F1, F2, F12** |
| Modèle dénomination | ❌ ABSENT | faille **F5** |
| Transfert de membre | ❌ ABSENT | faille **F6** |
| **Code par sous-église** | ⚠️ **API seule, ZÉRO interface** | `TenantJoinManagementPage:46-51` n'envoie jamais `orgNodeId` → **F14** |
| **CI verte** | ❌ **ROUGE** | `NoNullUnsafeMapLiteralTest` 123 > 122 → **F15** |

### §2.1 Les failles relevées par l'audit strict du `7390651`

> **Priorité = ordre d'exécution.** Les P0 sont des corrections de production, pas des finitions. Un agent qui commence par `V222` sans les corriger empile des migrations sur une base non fonctionnelle.

#### 🔴 P0 — bloquants

**F10 — Toute la console d'église du nouveau chantier répond `403` à son utilisateur cible.**
`JwtAuthenticationFilter.java:59-64` ne construit les autorités Spring **qu'à partir du claim `role`** du JWT, qui ne peut contenir qu'un `UserRole` legacy : `ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR, MEMBRE` (CHECK SQL `V16:7`). **`TENANT_OWNER` / `TENANT_ADMIN` sont mathématiquement inatteignables comme autorité.** Or quatre nouveaux contrôleurs utilisent ce motif :
`TenantJoinCodeController:23` · `TenantJoinController:54,70,79` · `TenantAnnouncementController:20` · `TenantOwnershipController:43,56,66,75`.
Le fondateur a `UserRole.ADMIN` + membership `TENANT_OWNER` → autorités `{ROLE_ADMIN, ROLE_PASTEUR}` → **403 sur `/tenant/join-codes`, `/join-requests`, `/tenant/announcements`, `/ownership/*`**. Pendant ce temps `routeAccess.ts:81-82` ouvre les pages : **l'IHM s'affiche, chaque appel 403.**
Aggravant : **le dépôt a déjà diagnostiqué et corrigé ce motif** — `InvitationController.java:38-47` documente noir sur blanc que `hasAnyRole('TENANT_OWNER','TENANT_ADMIN')` est cassé (double raison : ne reflète pas l'admin réel, et ignore le tenant visé → fuite inter-tenant, « SKIP E2E-9/E2E-10b ») et l'a remplacé par `@authz.isTenantAdmin()` qui lit `tenant_memberships`. **Le nouveau code réintroduit l'anti-pattern déjà documenté comme faille.** → **T-B0**

**F9 — « Créer une église » fonctionne, puis renvoie le fondateur vers `/login`.**
`RegisterPage.tsx:111-113` écrit les jetons puis fait **`localStorage.removeItem('user')`** avant `window.location.href='/dashboard'`. Or `AuthContext.tsx:81-97` exige `localStorage.user` **sinon il `return` avant d'appeler `/auth/me`**, et `isAuthenticated: !!user` (`:294`). Aucun autre chemin d'hydratation n'existe.
*Preuve exécutée* : test de rendu `AuthProvider` avec `accessToken` seul → `isAuthenticated === false`.
Tous les autres chemins (`login`, `loginWithSocialToken`, `switchRole`) écrivent `user` : le schéma a été copié puis une ligne inversée. → **T-W0**

**F7 — La promesse « plus jamais de code » est fausse.** Détail et diagramme en §4.2. → **T-B0bis**

#### 🟠 P1 — sérieux

**F1 — La fuite de menus a une sortie de secours.** `navForRole(activeRole)` retourne `FULL_NAV` en `default` (`workspaces.ts:742-763`) : la garantie « jamais de menus d'église » est *par composant*, pas structurelle. Tout nouveau composant appelant `navForRole()` réintroduit la fuite. → T-W1

**F2 — Les routes d'église ne sont pas gardées.** `ProtectedRoute` (`App.tsx:285-324`) ne connaît pas `platformSuperAdmin` pour les routes tenant : `/dashboard`, `/souls`, `/crm/*` restent atteignables par URL — exactement le reproche initial. → T-W2

**F12 — La fuite du Super Admin est plus large que F1/F2.** `CommandPalette.tsx:90-104` : les **actions rapides** (`/souls/new`, `/search`) filtrent par `activeRole` sans le flag → un Super Admin cherche et **crée des âmes** depuis la console plateforme. `DashboardGate:330-339` n'a pas de branche plateforme. Le test `Sidebar.test.tsx:232-261` ne vérifie que des libellés de menu. → T-W1, T-W2

**F3 — `window.prompt` dans la console de production.** `PlatformGovernancePage.tsx:54,67,78,89` : motif de bannissement, message d'avertissement, objet de litige et résolution saisis via des boîtes de dialogue système. Bannir une église ainsi est **inacceptable en prod et non testable**, et le motif n'est **pas obligatoire**. → T-W3

**F16 — Deux codes « principaux » actifs → `500` sur le lien public.** `TenantJoinCodeController.create` (`:50-57`) accepte `orgNodeId` absent, et `JoinCodeService.generate()` (`:77-104`) **ne désactive aucun principal existant**, alors que `TenantJoinCodeRepository:19` renvoie un `Optional` (`findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrue`) consommé par `JoinCodeService:158` et `:185`. Dès le **2ᵉ code principal** : `IncorrectResultSizeDataAccessException` → **500 sur `/join?slug=` et sur `POST /tenant/join {slug}`**. Le D9 (« un code actif par cible ») n'est garanti que par le code, pas par la donnée. → T-B4

**F17 — Le scoping sous-église crée des memberships multiples → `500` en cascade.** `TenantJoinService.grantMembership():316-329` teste `existsExactActiveMembership(scopeType, scopeId)` : un membre qui rejoint la racine **puis** une sous-église obtient **2 lignes ACTIVE pour le même (user, tenant)**. Toute requête `Optional` sur ce triplet explose ensuite : `TenantOwnershipService:107,138,160,187,222` (transfert, promotion, rétrogradation), `RoleManagementService:299`, `AiModuleService:58`. C'est le tarif du modèle « sous-communautés » : il **crée** la condition que les requêtes existantes ne savent pas traiter. → T-B4

**F18 — Le rate-limit anti-énumération des codes est contournable.** `PublicJoinController:58-66` est le seul garde-fou (12/min/IP), mais `PerIpRateLimiter.extractClientIp():311-315` prend **`X-Forwarded-For.split(",")[0]`**, valeur **contrôlée par le client** (Render *ajoute* la chaîne, ne la filtre pas). Un en-tête forgé = un seau neuf à chaque requête, et l'oracle `lookupByCode` → nom d'église redevient exploitable (~30⁴ suffixes). → T-B0ter

**F8 — XSS stocké via `link_url` sur la page publique.** Détail et diagramme en §4.5. → T-B6

**F19 — `access_ref` publié publiquement, sans réversibilité.** `publicFeed():199-201` renvoie le code tel quel ; le flux ne résout ni `isActive` ni `joinMode`. Une église qui colle son code dans une annonce **s'ouvre à quiconque lit la page d'accueil**, et le code y reste même après rotation. Arbitrage D8 à appliquer. → T-B6

**F11 — La raison d'un blocage/ban n'est pas tracée.** `TenantGovernanceService.changeStatus():176-188` n'écrit **aucune** ligne en `tenant_warnings` ; l'audit ne reçoit que `logSimple("TENANT_BLOCKED","TENANT",id)` (nom d'action, pas de métadonnée). Or l'IHM promet « chaque action est auditée » (`PlatformGovernancePage.tsx:106`) et prompt « Motif (**traçé dans l'audit**) » (`:54`). **Un bannissement est inexpliquable après coup** — inacceptable sur une console de modération inter-églises. → T-B2

**F20 — Création de tenant libre-service sans vérification d'email ni quota.** `AuthController:141-154` crée tenant ACTIVE + user ACTIVE + session, en contournant l'approbation (D1 assumé), sur la seule base de `existsByEmailIgnoreCase`, sans envoi d'email → **squat d'email** (le futur pasteur ne peut plus créer son église) et spam de tenants en `DISCOVERY`. Décision à assumer, mais à encadrer. → T-B3

#### 🟡 P2 — fonctionnel (vos demandes non livrées)

**F13 — Zéro interface de propriété/délégation.** `grep ownership frontend/src` = **0 résultat** : les 5 endpoints de `TenantOwnershipController` sont du code mort. La demande « déléguer, se défaire du rôle, choisir un remplaçant » **n'est pas livrée**.
**F14 — Zéro interface de code par sous-église.** `TenantJoinManagementPage:46-51` n'envoie jamais `orgNodeId` et n'a aucun sélecteur de nœud. Le backend sait le faire, l'écran non.
**F21 — Le lien `/j/<slug>` n'est proposé nulle part.** `TenantJoinCodeController.toView()` ne renvoie pas le `slug` → le fondateur n'a que le code à dicter. *(en cours de correction par un autre agent sur `RegisterPage` — voir §11)*
**F22 — Aucun email avec le code au membre**, alors que cette alternative a été explicitement demandée. Seul un email aux admins en mode APPROVAL existe.
**F23 — Code mort backend** : `TenantGovernanceService.listOpenDisputes()` n'est exposé nulle part ; `requestReplacement()` **ne persiste rien** alors que sa réponse promet « la plateforme arbitrera » — engagement fantôme.
**F4 — Divergence spec/code** : le Hero émet `?mode=church`, `RegisterPage:60` lit `mode`, mais `SPEC_ONBOARDING_FLOWS §A` documente `?intent=create-church`. Le code est correct, **le document est faux** : un agent qui le suivra cassera le CTA. → T-W4

#### ⚪ P3 — dette & vérification

**F15 — Le commit rend ROUGE un garde-fou de CI.** `NoNullUnsafeMapLiteralTest.auditPayloadsUsePayloads` est une crémaillère : `AUDIT_POSITION_BACKLOG_CEILING = 122`, « *le plafond ne peut que décroître* ». Le commit ajoute 2 sites en position d'audit avec `Map.of` brut (`SelfServiceChurchService.java:161`, `TenantOwnershipService.java:201`) → **123 > 122 ⇒ ÉCHEC**. Les deux fichiers étant **nouveaux** dans ce commit, `HEAD~1` était à 121 ⇒ **vert avant, rouge après**. La CI (temurin 21) **ne verrait pas** les erreurs Mockito mais verrait **ce** failure : **la branche ne peut pas passer en l'état.** → T-Q0. Correctif : `Payloads.of(...)`, **jamais** toucher au plafond.

**F24 — Les migrations `V219/V220/V221` n'ont jamais été exécutées.** `application-test.yml:12-14` (`ddl-auto: create-drop`, `flyway.enabled: false`) et le seul gate PG (`FlywayMigrationChainPostgreSqlTest`) est `disabledWithoutDocker` → skippé. *Vérification manuelle de l'audit* : la parité colonnes↔entités des 4 tables est **correcte** (index partiel `WHERE is_active`, CHECK d'énumération, `TIMESTAMPTZ`), mais **non vérifiée mécaniquement**. → T-Q0

**F25 — Dépendance à une ligne créée hors test.** `grantMembership` résout le rôle par la clé `"MEMBRE"`, qui n'existe que grâce à la migration `V215` (pont legacy). Ni `DataInitializer` (qui ne sème que `MEMBER`) ni le schéma H2 ne l'ont → dérive invisible dans les deux sens.

**F26 — i18n non fait.** `tText()` ne traduit que les chaînes déjà présentes dans le dictionnaire FR (`i18n/index.tsx:66-75`) ; aucune clé ajoutée → **les 6 nouveaux écrans sont en français dans les 5 langues**.

**F27 — `PlatformGovernancePage.tsx:133` affiche `{t.plan}`** alors que `toView` ne renvoie pas `plan` → `slug · undefined` à l'écran. `/platform/tenants` est en outre **non paginé** et en doublon de l'existant. → T-B2

**F5 — Le tenant n'a ni nature, ni parent, ni racine.** `backend/.../tenants/domain/Tenant.java` : `id, name, slug, status, plan, country, currency, timezone, locale, brandingJson, featuresJson, settingsJson, trialEndsAt, onboardingCompletedAt…`. **Aucun** `kind`, `parentTenantId`, `rootTenantId`. Impossible de modéliser « une dénomination qui contient des églises ». → **T-B1**

**F6 — Aucun transfert de membre.** Un membre qui change d'église doit aujourd'hui se réinscrire (nouveau compte, nouveau mot de passe). C'est faux, coûteux, et contraire au besoin central : il appartient déjà à la dénomination. → **T-B5**

**Ce qui est sain et ne doit pas être refait** : parité entités↔migrations correcte sur les 4 tables nouvelles ; `SelfServiceChurchService` respecte le pattern H4 (`crossTenant.callForTenantSwitch` + restauration en `finally`) et valide l'email avant toute écriture ; le flux legacy « demande d'église approuvée » est préservé ; `TenantStatusChangedEvent` invalide bien le cache `TenantStatusGuard` ; les réponses `/public/**` ne contiennent ni PII ni `tenant_id` ; le lien vanity `/j/:slug` fonctionnera en prod (rewrite SPA `render.yaml:201-204`).
---

## §2bis. DÉCISIONS PROVISOIRES DE L'AUDIT (à valider avant §3)

Ces décisions **n'ont pas** été arbitrées par le produit. Elles sont proposées parce que les failles correspondantes rendent des parcours non fonctionnels ; **un agent ne doit pas les appliquer seul** si le produit n'a pas répondu.

| # | Question ouverte | Position de l'audit | Impact si non tranchée |
|---|---|---|---|
| **Q1** | **Auto-termination d'une église en libre-service ?** F20 : `POST /auth/register {createChurch:true}` crée un tenant **ACTIVE** + un compte actif, sans vérification d'email, avec seulement 5 inscriptions/min/IP comme frein. | Deux options défendables : (a) **email de validation obligatoire** avant activation du tenant (comme le flux `/join`, qui envoie déjà un email d'activation) ; (b) quantum par IP + file de modération des nouveaux tenants. **En l'état : squat d'email + spam de tenants.** | Spam en production, pollution du landing (annonces), coût d'hébergement |
| **Q2** | **Le Super Admin doit-il pouvoir voir le contenu d'une église ?** D7 dit « non, agrégats seulement ». Le provisionnement et le support en ont besoin. | **Garder l'impersonation seule voie** (déjà journalisée, limitée, révocable) — ne pas rouvrir les routes tenant. Mais **préciser la durée d'impersonation** et la **journalisation dans la console**. | Fuite de données via une dérogation mal conçue |
| **Q3** | **Sous-église : nœud ou tenant fils ?** F17 montre que le mode « nœud » crée des memberships multiples, que les requêtes existantes ne gèrent pas. | **Trancher explicitement** : si une sous-église a sa propre facturation → **tenant fils**. Sinon nœud + corriger les `Optional`. Le modèle hybride (D1) est bon, mais **le mode par défaut doit être déclaré**. | 500 en cascade sur le transfert de propriété |
| **Q4** | **`access_ref` : code ou lien ?** F19 : aujourd'hui le code est publié sur la page d'accueil. D8 décide « jamais de code ». | **Appliquer D8** : publier le lien `/j/<slug>`. L churches qui veulent recruter le mettront dans leur description. | Une église s'ouvre publiquement sans le savoir |
| **Q5** | **Un membre peut-il être « transféré » vers une dénomination différente ?** §4.4 distingue même/différente racine. | **Non** — le transfert suppose une racine commune. Une dénomination différente = nouvelle adhésion. C'est le flux §4.4 cas 2, il faut juste **rendre la distinction visible dans l'IHM**. | Un utilisateur découvre qu'il a « perdu » son historique |
| **Q6** | **Faut-il garder l'exigence de vérification d'email pour l'inscription par code ?** | **Oui** — c'est le seul garde-fou contre la création de comptes fantômes dans une église. D10 (zéro code) ne l'implique pas. | Spam de comptes dans les directories |

---

## §3. DÉCISIONS FIGÉES

Ne pas les remettre en question. Validées par le produit le 04/10/2026.

| # | Décision | Conséquence |
|---|---|---|
| **D1** | **Modèle hybride** : sous-églises en nœuds (léger) **et** tenants fils (autonome). L'admin choisit le mode. | `OrganizationNode` reste ; `tenants` gagne une hiérarchie. |
| **D2** | **`kind` sur le tenant** : `CHURCH` (défaut), `DENOMINATION`, `ASSOCIATION`, `ORGANIZATION`, `MEGA_ASSOCIATION`. | Auto-documenté dans les données et l'IHM. |
| **D3** | **`root_tenant_id`** = dénomination de rattachement, propagé à toute la descendance. **Détecteur du transfert.** | Colonne NOT NULL, index non unique. |
| **D4** | **L'identité est globale.** Un membre connu qui rejoint une église de sa **même** racine = **TRANSFERT**, jamais réinscription. | `TenantTransferService`. |
| **D5** | **Le transfert préserve l'historique** pastoral (âmes, familles, présences) : le membre garde son parcours. | Le dossier n'est **pas** perdu. |
| **D6** | **Le Super Admin n'a AUCUNE interface d'église.** `/platform/**` strict ; les routes tenant le redirigent. | `navForRole(user)` + garde de routes. |
| **D7** | **Le Super Admin ne voit que des agrégats.** Le contenu d'une église s'obtient **par impersonation** journalisée, limitée, révocable. | `ImpersonationService` existant, à câbler. |
| **D8** | **Le code n'est jamais affiché publiquement.** Les annonces renvoient un **lien `/j/<slug>`**, jamais le code. | `access_ref` (V220) **non exposé**. |
| **D9** | **Codes** : `PREFIXE-XXXX`, sans `I L O U 0 1`. Un code actif par cible. Rotation = l'ancien meurt. | Déjà fait. **Ne pas toucher au format.** |
| **D10** | **Règle d'or** : code saisi **une seule fois**. Ensuite la connexion résout l'organisation seule. | Garanti par `TenantMembership`. **À tester (T-Q1).** |
| **D11** | **Les 3 consentements RGPD restent obligatoires** (art. 7 / 9). | Aucun allègement. |
| **D12** | **Le fondateur est `TENANT_OWNER` immédiatement**, sans approbation. Il peut déléguer puis abdiquer. | Déjà fait. |
| **D13** | **Modération obligatoire des annonces** : `DRAFT` → `PENDING_MODERATION` → `PUBLISHED/REJECTED`, `EXPIRED` auto. | Déjà fait. |
| **D14** | **Migrations montantes uniquement** (`V222+`). **Ne jamais éditer** une migration `V≤221`. | Gate Flyway en CI. |
| **D15** | **Aucune régression** : flux existants, H2 et tests restent verts. Le flux legacy « demande approuvée » reste intact. | `mvn verify` vert, **0 test supprimé**. |
| **D16** | **Les gardes d'accès tenant se lisent dans `tenant_memberships`, jamais dans le claim `role` du JWT.** | `@authz.isTenantAdmin()` / `isTenantOwner()`. Motif documenté dans `InvitationController.java:38-47` (F10). |
| **D17** | **Toute bascule de tenant réémet les jetons.** | `POST /tenant/join` renvoie la paire (F7). `markActiveTenant` seul ne suffit jamais. |
| **D18** | **Une action destructrice est toujours motivée, persistée et confirmée.** | Motif obligatoire en 400, persisté en `tenant_warnings`, confirmé dans l'IHM (F11). |
| **D19** | **Les URLs saisies par un admin ne sont validées que côté serveur.** | `link_url` en `http(s)` strict, test `javascript:` (F8). |
| **D20** | **Les tests qui prouvent un P0 sont obligatoires avant de merge.** | `SelfServiceJoinEndToEndIT`, `TenantJoinTokenIT`, `TenantAdminGuardIT` (T-Q1). |

---

## §4. LES FLOWS (contrat fonctionnel figé)

### 4.1 FLOW A — Landing : deux portes, décision en 6 secondes

```
                  ┌──────────────────────────────┐
                  │  LANDING  /                  │
                  │  Annonces publiques (carousel)│
                  └──────────────┬───────────────┘
         ┌───────────────────────┴───────────────────────┐
 [ Créer une église ]                        [ Rejoindre une église ]
         │                                                 │
         ▼                                                 ▼
 /register?mode=church                            /join   (1 seul champ)
 Nom église                                      [ Lien /j/slug ]  [ Code ]
 Prénom · Nom · Email · Mot de passe                        │
 3 consentements (RGPD) — non pré-cochés                    ▼
         │                                          Église trouvée
         │  1 seule saisie, zéro email                { name, mode, slug }
         ▼                                                   │
 POST /auth/register  createChurch=true          ┌────────────┴────────────┐
         │                                    [Je m'inscris]        [J'ai un compte]
         ▼                                          │                      │
 ┌──────────────────────────────┐          /register?joinCode=      /login?joinCode=
 │  ÉCRAN FONDATEUR            │                      └──────────┬───────────┘
 │  Ton église est créée       │                                 ▼
 │  Ton code : BETHEL-7K2X     │                    Membre rattaché
 │  Lien  : /j/eglise-de-grace │                                 ▼
 │  [Copier] [Partager WhatsApp]│                  Dashboard de l'église.
 │  [Inviter par email]        │                  Plus jamais de code.
 └──────────────────────────────┘
```

**Règles de rigueur**
- L'écran fondateur est le **seul** endroit où le code apparaît en clair.
- Le code est copiable en **1 clic** et partageable vers WhatsApp (le canal réel de ces utilisateurs). ✅ *en cours hors `7390651` — §11*
- **Aucun** email de vérification : le mot de passe est saisi dans le formulaire, le compte est actif. Une friction supprimée.
  ⚠️ **Cette friction a un coût** (F20) : sans validation d'email, `POST /auth/register {createChurch:true}` est un **créateur de tenant public** — squat d'email et spam. Décision à trancher (§2bis Q1), pas à subir.
- URL `?mode=church` = **contrat figé** (corrige F4). Ne pas renommer.
- ⚠️ **Le flow ne s'achève pas correctement aujourd'hui** : après l'écran fondateur, le fondateur est renvoyé vers `/login` (F9). Le flow tel que dessiné suppose `T-W0` fait.

### 4.2 FLOW B — La règle d'or : le code ne se saisit qu'une fois

```
   1re FOIS (adhésion)              CHAQUE FOIS APRÈS
────────────────────────           ───────────────────────
Code  ou  Lien /j/slug                Se connecter
        │                                  │
        ▼                                  ▼
   POST /tenant/join                Résolution automatique
        │                             de l'organisation
        ▼                                    │
   membership ACTIVE                          │
   (TenantMembership, ACTIVE)                 │
        │                                     ▼
        └──────────────────────────► Dashboard. Zéro code.
```

**Ce qui garantit la promesse** : l'adhésion est en base (`TenantMembership` ACTIVE). Au login suivant, `ActiveTenantService.resolveTokenTenantId` la résout **sans intervention**. Le code est alors **inutile** — et c'est correct : il a rempli son rôle.

> #### 🔴 FAILLE F7 — cette promesse est **FAUSSE en l'état** (P0 — corriger par T-B0bis)
> `TenantJoinService.joinWithCode()` appelle `activeTenantService.markActiveTenant(user, tenant)` **sans réémettre de jeton**. Le claim `tenantId` du access token courant reste celui de l'ancienne organisation, et `TenantInterceptor` le relit à chaque requête.
> - **Web** (`JoinChurchPage.tsx:88-94`) : `POST /tenant/join` puis `window.location.href='/dashboard'` → l'utilisateur **atterrit dans l'église précédente**.
> - **Mobile** (`join_church_screen.dart:88-95`) : `POST /tenant/join` puis `GET /auth/me` → or `AuthService.getCurrentUser()` renvoie `AuthResult(null, null, user)` : il **ne résout pas** `active_tenant_id` et ne rend **aucun token**. Le client se croit dans la nouvelle église, **toutes les requêtes suivantes s'exécutent dans l'ancien tenant** (split-brain silencieux).
>
> **Exigence de correction** : `POST /api/v1/tenant/join` doit renvoyer la **paire de jetons** portant le nouveau tenant (même contrat que `ActiveTenantService.switchTenant`), et les deux clients doivent l'adopter avant de naviguer. **D10 est donc « à corriger » et non « à tester »** : le test T-Q1 doit devenir bloquant.

### 4.3 FLOW C — Console Super Admin : il administre une flotte, il ne visite pas une église

```
┌─────────────────────────────────────────────────────────────────┐
│  CONSOLE PLATEFORME          (aucun menu d'église, D6)         │
├─────────────────────────────────────────────────────────────────┤
│ ▸ Vue d'ensemble — 248 églises · 12 suspendues · 3 litiges      │
│ ▸ Églises (tenants)  recherche + filtres (statut, kind, plan)   │
│      ┌───────────────────────────────────────────────────┐      │
│      │ Église de la Grâce   ACTIVE   CHURCH   Plan Pro    │      │
│      │   ├── Consulter (Agrégats seulement, D7)           │      │
│      │   ├── Avertir     INFO │ FORMAL │ FINAL           │      │
│      │   ├── Ouvrir litige                                │      │
│      │   ├── Bloquer    SUSPENDED + motif tracé           │      │
│      │   ├── Bannir    CANCELLED (irréversible)           │      │
│      │   ├── Réactiver                                    │      │
│      │   └── Impersonation (journalisée, temporelle)      │      │
│      └───────────────────────────────────────────────────┘      │
│ ▸ Modération annonces · Litiges · Audit · Impersonation        │
│ ▸ Plans & quotas · Branding · Santé système                     │
└─────────────────────────────────────────────────────────────────┘
```

**Trois séparation inscrites dans le code** (c'est le cœur de la sécurité) :
1. **Routes** — `/platform/**` est strict ; toute route tenant redirige un platform admin.
2. **Navigation** — `navForRole(user)` rend `FULL_NAV` **structurellement impossible** à un platform admin.
3. **Données** — agrégats seulement. Voir le contenu d'une église = **impersonation** journalisée et limitée dans le temps. Jamais d'accès transparent.

### 4.4 FLOW D — TRANSFERT : changer d'église sans se réinscrire (le besoin central)

> Un membre qui passe de l'église A à l'église B **de la même dénomination** ne crée pas de compte. Il est déjà connu. Il lui faut **le code de B** et **un transfert**.

```
Cas 1 — MÊME DÉNOMINATION (root_tenant_id identique)  = TRANSFERT

  Membre [Jean]  ──ROOT=DENOMINATION X──┬── Église A   (membre actif)
                                       └── Église B   (nouveau code)
                            │
  Jean saisit le code de B sur /join
                            │
                            ▼
        Le backend compare root_tenant_id(Jean) == root_tenant_id(B)  → OUI
                            │
                            ▼
   ┌──────────────────────────────────────────────────────────┐
   │  « Bienvenue ! Jean, vous êtes déjà membre de la         │
   │    Dénomination X. Vous rejoignez l'Église B.            │
   │    Votre parcours pastoral est conservé. »               │
   │         [ Confirmer le transfert ]                        │
   └──────────────────────────────────────────────────────────┘
                            │
                            ▼
     POST /api/v1/tenant/transfer  { toTenantId | joinCode, reason? }
                            │
       ┌────────────────────┴────────────────────┐
       │  Transaction atomique :                  │
       │  1. membership(A) → status TRANSFERRED    │
       │     (traçable, pas supprimé)              │
       │  2. membership(B) créée, rôle MEMBRE     │
       │  3. Les ÂMES / FAMILLES de Jean restent  │
       │     rattachées à SON dossier (D5)        │
       │  4. active_tenant_id → B                 │
       │  5. AuditService : TENANT_TRANSFER       │
       │  6. Email de confirmation                │
       └────────────────────┬────────────────────┘
                            ▼
              Jean entre dans l'Église B. Zéro réinscription.
              Prochaine connexion : B directement.

Cas 2 — DÉNOMINATION DIFFÉRENTE (root_tenant_id différent) = nouvelle adhésion

  Jean saisit le code d'une église d'une AUTRE dénomination
        │
        ▼
  Pas de transfert (racines différentes). Flux d'adhésion classique :
  il rejoint sans changer de compte si déjà connu, sinon inscription.
  Un audit REGISTER_CROSS_ROOT documente ce cas.
```

**Pourquoi ce design** : l'identité (`users`) est **globale**. Un membre, un pasteur, un visiteur qui change de dénomination reste **la même personne** partout sur Discipolat. Ce qui change, c'est son **appartenance** (`TenantMembership`) et son **organisation active**.

### 4.5 FLOW E — Annonces publiques sur le landing

```
ÉGLISE (a souscrit le service « pub »)
   │
   ├─ Rédige une annonce : titre, visuel, ville, date, description
   ├─ VOITURE de modération obligatoire (D13)
   │     DRAFT → PENDING_MODERATION → PUBLISHED | REJECTED
   └─ PUBLICATION → visible sur le LANDING
         │
         └─ carte : titre · église · ville · date
              [ Je m'inscris ]  →  /join?slug=<slug>
```

**Point de sécurité arbitré (D8)** : le **code n'est JAMAIS affiché** sur la carte publique. On publie un **lien `/j/<slug>`**. Un code exposé publiquement = une église ouverte à tous, sans contrôle — inacceptable. Le lien mène à l'inscription, où le mode `OPEN`/`APPROVAL` du code **règne toujours**. Le champ `access_ref` de `V220` existe en base mais **n'est pas** renvoyé par `GET /public/announcements`.

> #### 🔴 FAILLE F8 — `link_url` est un XSS stocké sur la page publique (P1 — corriger par T-B6)
> `PublicAnnouncementService.create/update` stocke `linkUrl` **sans validation de schéma**, et `SectionAnnouncements.tsx:86-90` le rend en `href={a.linkUrl}` avec `target="_blank"`. La CSP de `render.yaml:199` autorise `'unsafe-inline'` : un `javascript:…` ne survit qu'à la validation d'un modérateur et s'exécute sur le domaine public. Même remarque pour `imageUrl` (pixel de tracking).
> **Exigence** : validation `http(s)` **côté serveur**, au seul endroit fiable, + test négatif sur `javascript:` / `data:`.

---

## §5. MODÈLE DE DONNÉES — migrations à créer

> **D14** : migrations **montantes uniquement**, à partir de **V222**. **Ne jamais éditer** `V≤221`.

### V222__tenant_organization_model.sql

```sql
-- Nature de l'organisation (D2) : une tenant n'est plus « une église »,
-- c'est une ENTITÉ dont la nature est explicite.
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS kind VARCHAR(30) NOT NULL DEFAULT 'CHURCH'
    CHECK (kind IN ('CHURCH','DENOMINATION','ASSOCIATION','ORGANIZATION','MEGA_ASSOCIATION'));

-- Hiérarchie organisationnelle (D1) : une tenant peut être l'enfant d'une autre.
-- parent_tenant_id = rattachement direct ; root_tenant_id = la dénomination
-- de référence (elle-même pour une racine). C'est le DÉTECTEUR DU TRANSFERT (D3).
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS parent_tenant_id UUID
    REFERENCES tenants(id) ON DELETE RESTRICT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS root_tenant_id UUID
    REFERENCES tenants(id) ON DELETE RESTRICT;

-- Rétro-compatible : une tenant existante est sa propre racine.
UPDATE tenants SET root_tenant_id = id WHERE root_tenant_id IS NULL;
ALTER TABLE tenants ALTER COLUMN root_tenant_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_tenants_root      ON tenants(root_tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_parent    ON tenants(parent_tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenants_root_kind ON tenants(root_tenant_id, kind);

COMMENT ON COLUMN tenants.root_tenant_id IS
    'Racine du reseau (= la denomination). Detecteur du transfert de membre (D3/D4).';
COMMENT ON COLUMN tenants.kind IS
    'Nature de l''organisation : CHURCH | DENOMINATION | ASSOCIATION | ORGANIZATION | MEGA_ASSOCIATION';
```

**Choix de conception à respecter strictement** :
- `ON DELETE RESTRICT` sur `parent_tenant_id` : **on ne supprime pas une dénomination** qui a des enfants. La suppression passe par le statut `CANCELLED`. Cela protège l'historique pastoral.
- `root_tenant_id` est **NOT NULL** : une tenant est toujours rattachée à une racine (elle-même si isolée). Le transfert compare donc **toujours** deux racines non nulles — logique totale, pas de cas « null » à gérer.
- ⚠️ **MySQL/H2** : ce dépôt cible PostgreSQL. La chaîne multi-lignes `CHECK` et les `COMMENT ON` sont spécifiques PG. Vérifier le profil de test (`test`) : si H2 est utilisé pour `mvn test`, adapter la syntaxe ou exclure ces lignes via un placeholder Flyway. **Se renseigner sur le gate Flyway CI avant d'écrire.**

### V223__membership_transfer.sql

```sql
-- Le transfert de membre (§4.4, D4/D5) : l'appartenance n'est pas
-- supprimée, elle est TRAÇÉE. Une organisation ne perd pas son historique.
ALTER TABLE tenant_memberships ADD COLUMN IF NOT EXISTS
    transferred_at TIMESTAMPTZ;
ALTER TABLE tenant_memberships ADD COLUMN IF NOT EXISTS
    transferred_to_membership_id UUID;
ALTER TABLE tenant_memberships ADD COLUMN IF NOT EXISTS
    transfer_reason VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_memberships_transferred
    ON tenant_memberships(tenant_id, transferred_at DESC)
    WHERE transferred_at IS NOT NULL;
```

**Pourquoi tracer plutôt que supprimer (D5)** : supprimer l'appartenance ferait perdre l'historique pastoral (qui était membre, depuis quand, dans quelle église). La traçabilité est aussi une exigence d'audit. Le transfert **garde** les deux lignes : `TRANSFERRED` (passé) et `ACTIVE` (présent).

---

## §6. API — surface à créer

> Conventions : préfixe `/api/v1`. `permitAll` sous `/api/v1/public/**` uniquement. Toute bascule de tenant passe par `CrossTenantScopeAccess.callForTenantSwitch`.

### Plateforme (`@authz.isPlatformSuperAdmin()`)

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/platform/tenants` | Liste **enrichie** : `kind`, `parentTenantId`, `rootTenantId`, `childCount`, `memberCount` (agrégats, D7) |
| `GET` | `/platform/tenants/{id}` | Détail + **descendance** (enfants de la dénomination) |
| `POST` | `/platform/tenants` | Créer une **dénomination** (`kind=DENOMINATION`) |
| `POST` | `/platform/tenants/{id}/children` | Créer une **église enfant autonome** (`kind=CHURCH`, `parentTenantId`) |
| `POST` | `/platform/tenants/{id}/suspend\|resume\|ban\|unban` | Gouvernance (existe — vérifier) |
| `POST` | `/platform/tenants/{id}/warnings` | Avertissement (existe) |
| `POST` | `/platform/tenants/{id}/disputes` | Litige (existe) |

### Tenant admin (`@authz.isTenantAdmin()`)

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/tenant/join-codes` | Codes de l'église **et des sous-églises** (existe) |
| `POST` | `/tenant/join-codes` | `{orgNodeId?, label, joinMode}` — **ajouter `orgNodeId`** (T-B4) |
| `POST` | `/tenant/join-codes/{id}/rotate` | Rotation (existe) |
| `GET` | `/tenant/ownership` | Vue propriétaire + délégués (existe) |
| `POST` | `/tenant/ownership/transfer` | Cession / abdication (existe) |
| `GET` | `/tenant/sub-churches` | **NOUVEAU** : sous-églises (nœuds + tenants enfants) |
| `POST` | `/tenant/sub-churches` | **NOUVEAU** : créer une sous-église (mode `light` \| `autonomous`) |

### Membre (authentifié)

| Méthode | Route | Rôle |
|---|---|---|
| `POST` | `/tenant/join` | `{code}` — adhésion (existe) |
| `GET` | `/tenant/transfer/preview?code=` | **NOUVEAU** : détecte même/différente racine (D4) |
| `POST` | `/tenant/transfer` | **NOUVEAU** : `{toTenantId\|joinCode, reason?}` — le transfert |

### Public (rate-limité)

| Méthode | Route | Rôle |
|---|---|---|
| `GET` / `POST` | `/public/join/lookup` | Résolution code/slug (existe) |
| `GET` | `/public/announcements` | Annonces publiées (**sans `access_ref`**, D8) |
---

## §7. RÉPARTITION DES TÂCHES

> **Ordre d'exécution imposé** : **`§7.0 Remédiation P0`** → `§7.1 Backend` → `§7.2 Web` → `§7.3 Mobile` → `§7.4 Tests & CI`.
> Les dépendances sont notées 🔗. **Ne pas commencer une tâche dont la dépendance n'est pas verte.**

### 7.0 REMÉDIATION DES P0 (à faire AVANT toute autre tâche) 🔗 *aucune dépendance*

> Ces quatre tâches ne sont pas des finitions : **sans elles, le reste du chantier est non livrable.** Elles sont volontairement hors de l'ordre Back → Web → Mobile, parce qu'elles conditionnent **tous** les tests qui suivent.

#### T-B0 · Les gardes d'autorisation des 4 contrôleurs d'église 🔴 *P0 — F10*

**Fichiers** :
- `TenantJoinCodeController.java:23`, `TenantJoinController.java:54,70,79`, `TenantAnnouncementController.java:20`, `TenantOwnershipController.java:43,56,66,75`

`hasAnyRole('TENANT_OWNER','TENANT_ADMIN')` est **inatteignable** : `JwtAuthenticationFilter:59-64` ne tire les autorités que du claim `role`, or ce claim ne contient qu'un `UserRole` legacy. Remplacer par le garde scopé tenant déjà éprouvé :

```java
// au niveau classe
@PreAuthorize("@authz.isTenantAdmin()")
```

Pour les opérations réservées au propriétaire (`transfer`, `promote-admin`, `demote-admin`), **`isTenantAdmin()` ne suffit pas** — un `TENANT_ADMIN` délégué ne doit pas pouvoir céder la propriété. Ajouter dans `AuthorizationService` :

```java
/** membership ACTIVE de portée TENANT avec roleLegacy == TENANT_OWNER, pour le tenant courant */
public boolean isTenantOwner() {
    UUID userId = SecurityUtils.getCurrentUserId();
    UUID tenantId = TenantContext.requireTenantId();
    return isTenantOwner(userId, tenantId);
}
```
puis `@PreAuthorize("@authz.isTenantOwner()")` sur ces trois endpoints. **Le service `TenantOwnershipService.requireOwnerMembership()` reste la garde métier** (défense en profondeur) — ne pas le supprimer au motif que le décorateur suffit.

⚠️ **Ne pas** « corriger » en ajoutant `ADMIN`/`PASTEUR` à la liste `hasAnyRole` : cela rouvrirait la fuite inter-tenant documentée dans `InvitationController.java:38-47`. Le problème n'est pas la liste, c'est le **mécanisme** (autorité JWT au lieu de l'appartenance).

- [ ] 4 contrôleurs sur `@authz.isTenantAdmin()`
- [ ] `AuthorizationService.isTenantOwner()` + 3 endpoints de propriété
- [ ] Test `TenantJoinCodeControllerSecurityTest` : `ADMIN` (légacy) + membership `TENANT_OWNER` → **200** et non 403
- [ ] Test négatif : membre `MEMBRE` → 403
- [ ] Test multi-tenant : token du tenant B + code du tenant A → **404** (fuite inter-tenant, cf. SKIP E2E-9)

#### T-B0bis · `/tenant/join` doit renvoyer les jetons 🔴 *P0 — F7*

**Fichier** : `TenantJoinService.java`, `TenantJoinController.java`

`markActiveTenant(user, tenantId)` persiste `users.active_tenant_id` mais **ne réémet rien** ; le claim `tenantId` du access token courant reste l'ancien, et `TenantInterceptor` le relit à chaque requête. Résultat : le client atterrit dans l'organisation précédente (web), ou **croit** être dans la nouvelle alors que toutes ses requêtes partent vers l'ancienne (mobile).

Deux options, **au choix** (recommandé : la première) :
- **(a)** `JoinOutcome` porte `accessToken` + `refreshToken` réémis par `ActiveTenantService.switchTenant(userId, targetTenantId)`, et les deux clients les adoptent avant de naviguer. Un seul aller-retour, pas de course.
- **(b)** Le client appelle `POST /auth/switch-tenant` après le join. Acceptable mais laisse une fenêtre où le token est périmé.

⚠️ **`GET /auth/me` ne peut pas servir de rafraîchissement** : `AuthService.getCurrentUser():514-519` renvoie `AuthResult(null, null, user)` — il ne résout pas `active_tenant_id` et ne rend aucun token. Le code mobile actuel s'appuie à tort dessus.

- [ ] `JoinOutcome` porte les deux jetons (ou le contrôleur les émet)
- [ ] `JoinChurchPage.tsx` : adopte les jetons **puis** navigue (plus de rechargement complet)
- [ ] `join_church_screen.dart:88-95` : idem, et supprime l'appel `/auth/me`
- [ ] Test : `POST /tenant/join` → le nouveau access token porte le `tenantId` cible

#### T-B0ter · Le rate-limit doit résister à l'usurpation d'IP 🔴 *P0 — F18*

**Fichier** : `PerIpRateLimiter.java:311-321`

`extractClientIp` lit `X-Forwarded-For.split(",")[0]` — la **valeur la plus à gauche est choisie par le client**. En Render (et tout reverse-proxy qui *ajoute* la chaîne sans la filtrer), un en-tête forgé donne un seau neuf : le rate-limit de 12/min sur `/public/join/lookup` devient inexistant et l'oracle d'énumération des codes redevient exploitable.

```java
public static String extractClientIp(HttpServletRequest request) {
    // On prend la DROITE : c'est la valeur ajoutée par le proxy de confiance.
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
        String[] hops = xff.split(",");
        String candidate = hops[hops.length - 1].trim();
        if (!candidate.isEmpty() && !"unknown".equalsIgnoreCase(candidate)) return candidate;
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) return realIp.trim();
    return request.getRemoteAddr();
}
```

⚠️ **Piège** : ce correctif n'est sûr que si le proxy de confiance **écrase** `X-Forwarded-For` au lieu de le concatener. Or la plupart des reverse-proxies **ajoutent** à la chaîne existante — le client peut donc toujours injecter un préfixe, mais il ne peut plus choisir la **dernière** valeur, qui est celle ajoutée par le proxy. Vérifier la config nginx (`infra/nginx/nginx.conf`) et le comportement réel de Render, puis **le tester** (deux requêtes, `XFF` différents, même IP réelle → le rate-limit doit s'appliquer). Ne pas supposer.

- [ ] IP de confiance = valeur proxy, jamais le client
- [ ] Test : deux requêtes avec `XFF` différent mais même IP réelle → **même** seau (2ᵉ → 429)
- [ ] Config nginx vérifiée et documentée

#### T-W0 · La session doit être adoptée, pas reconstruite à la main 🔴 *P0 — F9*

**Fichiers** : `frontend/src/contexts/AuthContext.tsx`, `frontend/src/pages/RegisterPage.tsx:108-119`

`RegisterPage` écrit `accessToken`/`refreshToken` puis fait `localStorage.removeItem('user')`, en pensant que « régénéré via /auth/me ». Or `AuthContext:90-97` : sans `user` en localStorage il `return` **avant** d'appeler `/auth/me` → `isAuthenticated` reste `false` → `ProtectedRoute` redirige vers `/login`.

**Correctif recommandé — un point d'entrée unique dans `AuthContext`**, plutôt qu'une nouvelle écriture manuelle dans `RegisterPage` :

```tsx
/** Adopte une session déjà émise par le backend (register social, createChurch, join). */
const adoptSession = useCallback((d: any) => {
  const userData = buildUserFromAuthResponse(d);
  localStorage.setItem('accessToken', d.accessToken);
  localStorage.setItem('refreshToken', d.refreshToken ?? '');
  localStorage.setItem('user', JSON.stringify(userData));
  api.defaults.headers.common['Authorization'] = `Bearer ${d.accessToken}`;
  setUser(userData);
  return userData;
}, []);
```
`RegisterPage` appelle `adoptSession(payload.session)` puis `navigate('/dashboard')` — **sans rechargement complet** (le rechargement marche aussi, mais il masque le bug : c'est pour ça qu'il n'a pas été vu).

⚠️ Ne pas « corriger » en ré-écrivant `user` à la main dans `RegisterPage` : le prochain flux (join, social, import) reproduira l'erreur.

- [ ] `adoptSession` dans `AuthContext`
- [ ] `RegisterPage` : plus de `localStorage.setItem` direct, plus de `removeItem('user')`
- [ ] Test `RegisterPage.createChurch` : après succès, `isAuthenticated === true` et **pas** de redirection vers `/login`

---

### 7.1 BACKEND (Java / Spring Boot) — le socle

#### T-B1 · `Tenant` : porter le modèle d'organisation 🔗 *aucune dépendance*

**Fichiers** :
- `backend/src/main/java/com/discipolat/modules/tenants/domain/Tenant.java` (modifier)
- `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantKind.java` (nouveau)

```java
@Enumerated(EnumType.STRING)
@Column(name = "kind", nullable = false)
private TenantKind kind = TenantKind.CHURCH;

@Column(name = "parent_tenant_id")
private UUID parentTenantId;

@Column(name = "root_tenant_id", nullable = false)
private UUID rootTenantId;
```

```java
public enum TenantKind { CHURCH, DENOMINATION, ASSOCIATION, ORGANIZATION, MEGA_ASSOCIATION }
```

⚠️ **Garde-fou applicatif** : le trigger SQL ne s'exécute qu'en base. Dans `TenantService.create`, après `save`, si `rootTenantId == null` alors `rootTenantId = id` — sinon les requêtes de transfert lèveront un NPE sur les tenants créés par le code.

**`TenantRepository`** — ajouter :
```java
List<Tenant> findByRootTenantId(UUID rootTenantId);
List<Tenant> findByParentTenantId(UUID parentTenantId);
long countByRootTenantId(UUID rootTenantId);
```

- [ ] `TenantKind` créé
- [ ] 3 champs ajoutés à `Tenant` + garde dans `TenantService.create`
- [ ] 3 méthodes de repository ajoutées
- [ ] Test `TenantKindTest` : défaut `CHURCH` ; une racine a `rootTenantId == id`

#### T-B2 · Console plateforme : agrégats enrichis 🔗 *T-B1*

**Fichier** : `backend/src/main/java/com/discipolat/modules/tenants/api/PlatformTenantGovernanceController.java`

Enrichir `GET /api/v1/platform/tenants` : exposer `kind`, `parentTenantId`, `rootTenantId`, `childCount` (`countByRootTenantId` − 1), `memberCount` (un `COUNT`, **jamais** une liste).

⚠️ **Sécurité (D7)** : `memberCount` est un nombre. Si la réponse contenait ne serait-ce qu'un email, ce serait une fuite. Test négatif explicite obligatoire.

**À corriger dans la même tâche (écarts du §2.1) :**

- **F11 — le motif d'un blocage/ban n'est pas persisté.** `TenantGovernanceService.changeStatus():176-188` n'écrit rien en `tenant_warnings` et l'audit ne reçoit que le nom de l'action. Or l'IHM promet « chaque action est auditée » (`PlatformGovernancePage.tsx:106`) et prompt « Motif (traçé dans l'audit) » (`:54`). Un bannissement est aujourd'hui **inexplicable après coup**. → `changeStatus` doit créer une ligne `tenant_warnings` (`severity = FINAL` pour un ban, `FORMAL` pour un blocage) portant le motif, **et** transmettre le motif à l'audit via `AuditService.log(..., metadata = {motif, ancienStatut, nouveauStatut})`.
- **F27 — `PlatformGovernancePage.tsx:133` affiche `{t.plan}`** alors que `toView` ne renvoie pas `plan` → `slug · undefined` à l'écran. Le renvoyer. Et `/platform/tenants` est **non paginé** et en doublon de `/platform/admin/tenants` : le fusionner ou le paginer.
- **Motif obligatoire** : `ReasonRequest` est aujourd'hui `required = false` et `reasonOf()` accepte `null`. Le backend doit **refuser** un motif vide sur `block`/`ban` (400), pas seulement l'IHM.

- [ ] Liste enrichie avec `childCount` / `memberCount` / `plan`
- [ ] `GET /platform/tenants/{id}` inclut la descendance
- [ ] **Motif persisté** (`tenant_warnings` + audit) et **motif vide refusé en 400**
- [ ] Liste paginée
- [ ] Test : `PLATFORM_SUPER_ADMIN` → 200 avec `kind` ; `MEMBRE` → 403 ; **aucun email dans le corps**
- [ ] Test : `POST /ban {reason: ""}` → 400 ; `POST /ban {reason: "x"}` → motif retrouvé via `GET /{id}/warnings`

#### T-B3 · Création de dénomination + église enfant 🔗 *T-B1, T-B2*

**Fichier** : `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantOrganizationService.java` *(nouveau)*

```java
public Tenant createDenomination(String name, UUID creatorUserId, TenantKind kind);
public Tenant createChildChurch(UUID parentTenantId, String name, UUID creatorUserId);
```

Réutiliser **exactement** le pattern de `SelfServiceChurchService` : `crossTenant.callForTenantSwitch(...)`, `TenantContext.setTenantId`, restauration en `finally`. Les deux doivent créer via `TenantService.create`, propager `rootTenantId` depuis le parent, créer le nœud racine (`OrganizationNodeService.createRootChurch`), et enregistrer le propriétaire comme `TENANT_OWNER`.

- [ ] `createDenomination` : crée la racine (`rootTenantId = id`)
- [ ] `createChildChurch` : hérite `rootTenantId`, `parentTenantId` renseigné
- [ ] Audit `TENANT_ORG_CREATED`
- [ ] Test : une dénomination + 2 églises filles → les 2 partagent le même `rootTenantId`

#### T-B4 · Code de rejointure par sous-église 🔗 *T-B1, T-B0*

**Fichiers** : `JoinCodeService.java`, `TenantJoinCodeController.java`, `TenantJoinCode.java`

`org_node_id` existe déjà en base (V219) mais n'est pas exposé par l'API. L'exposer dans `POST /tenant/join-codes` (requête) et dans `GET /tenant/join-codes` (réponse). Le `JoinLookup` public retourne déjà `orgNodeLabel` (ligne 53 de `JoinCodeService`).

⚠️ **F16 — unicité du code principal (corrige un `500` en production).** `POST /tenant/join-codes` accepte `orgNodeId` absent, et `JoinCodeService.generate():77-104` **ne désactive aucun code principal existant**. Or `TenantJoinCodeRepository:19` renvoie un `Optional` (`findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrue`), consommé par `findPrimaryActiveCode():158` et `lookupBySlug():185`. Dès le **2ᵉ code principal** créé par un admin : `IncorrectResultSizeDataAccessException` → **`500` sur la page publique `/join?slug=`**. Le D9 (« un code actif par cible ») n'est aujourd'hui garanti que par le code, pas par la donnée.

→ Dans `generate()`, si `orgNodeId == null`, **désactiver le principal existant** avant d'insérer le nouveau. Renforcer le repository : passer `findByTenantIdAndOrgNodeIdIsNullAndIsActiveTrue` en `List<…>` (`findPrimaryActiveCodes`) pour ne plus jamais dependre d'un `Optional` multi-lignes.

⚠️ **F17 — le scoping crée des memberships multiples (corrige un `500` en cascade).** `grantMembership():316-329` teste `existsExactActiveMembership(scopeType, scopeId)` : un membre qui rejoint la racine **puis** une sous-église obtient **2 lignes ACTIVE pour le même `(user, tenant)`**. Toute requête `Optional` sur ce triplet explose ensuite — `TenantOwnershipService:107,138,160,187,222`, `RoleManagementService:299`, `AiModuleService:58`. C'est le prix du modèle « sous-communautés » : il **crée** la condition que les requêtes existantes ne savent pas traiter.

→ Trois corrections complémentaires :

1. Utiliser `findAllByUserIdAndTenantIdAndStatus` + un tri déterministe (portée `TENANT` d'abord) partout où un `Optional` est attendu.
2. **Renseigner le `slug` du tenant dans `toView()`** pour que le frontend puisse construire le lien `/j/<slug>` (F21).
3. **Trancher la portée** (cf. §2bis Q3) : une sous-église est-elle un **nouveau** tenant (mode autonome) ou le même tenant avec un nœud (mode léger) ? Si c'est le même tenant, **deux memberships ACTIVE sont normales** et le modèle de données doit les assumer explicitement — c'est un choix, pas un bug.

- [ ] `orgNodeId` accepté + retourné
- [ ] **Un seul code principal actif** (désactivation de l'ancien + repository en `List`)
- [ ] `slug` du tenant retourné dans `toView()`
- [ ] Plus aucun `Optional` sur `(userId, tenantId, status)` dans le chemin ownership/roles/IA
- [ ] Test : 2 sous-églises → 2 codes distincts, memberships scopées
- [ ] Test : 2ᵉ code principal créé → `/public/join/resolve?slug=` répond **200**, pas 500
- [ ] Test : membre racine + sous-église → `transfer`/`promote` répondent **200**, pas 500

#### T-B5 · SERVICE DE TRANSFERT (le cœur) 🔗 *T-B1, T-B4*

**Fichier** : `backend/src/main/java/com/discipolat/modules/tenants/domain/TenantTransferService.java` *(nouveau)*

```java
public record TransferPreview(boolean sameNetwork, boolean alreadyMember,
                              String fromChurch, String toChurch, String toKind) {}

public record TransferOutcome(String status, UUID fromTenantId, UUID toTenantId,
                              String toChurch) {}

public TransferPreview preview(UUID userId, String rawCode);
public TransferOutcome transfer(UUID userId, String rawCode, String reason);
```

**Algorithme de `transfer` (atomique, `@Transactional`)** :
1. Résoudre le code → tenant cible `T`. Refuser si `T.status != ACTIVE` (`TenantStatusGuard`).
2. Charger l'adhésion ACTIVE courante → tenant source `S`. Si `S == T` → `ALREADY_MEMBER`.
3. **Détecteur D4** : si `S.rootTenantId.equals(T.rootTenantId)` → **TRANSFERT**. Sinon → adhésion classique (pas de transfert).
4. Marquer l'adhésion `S` : `status = TRANSFERRED`, `transferredAt = now`, `transferReason = reason`.
5. Créer l'adhésion `T` : rôle `MEMBRE`, `status = ACTIVE`.
6. **Préserver le dossier pastoral** (D5) : les âmes / familles restent rattachées à la personne. Le transfert change une appartenance, pas une identité.
7. `activeTenantId = T` + réémission des tokens (`ActiveTenantService.switchTenant`).
8. `AuditService.log(..., "TENANT_TRANSFER", ...)`.
9. Email de confirmation (**non bloquant** : `try/catch` + log, pattern `InvitationService`).

**Fichier** : `TenantTransferController.java` *(nouveau)* — `GET /transfer/preview`, `POST /transfer`.

⚠️ **Piège à éviter** : la comparaison `S.rootTenantId` / `T.rootTenantId` doit se faire **après** avoir chargé les deux entities. Ne jamais comparer des slugs.

- [ ] `preview` distingue même/différente racine (`sameNetwork`)
- [ ] `transfer` transactionnel + **idempotent** (rejouer = `ALREADY_MEMBER`, pas de doublon)
- [ ] Adhésion source `TRANSFERRED` (pas supprimée, D5)
- [ ] Tokens réémis vers le tenant cible
- [ ] Test `TenantTransferServiceTest` : même racine → TRANSFERT ; racine différente → adhésion ; rejeu → `ALREADY_MEMBER`

#### T-B6 · Annonces publiques sans `access_ref` 🔗 *aucune*

**Fichiers** : `PublicAnnouncementsController.java`, `PublicAnnouncementService.java`

`GET /public/announcements` ne doit **pas** inclure `access_ref`. Elle renvoie un **lien `/j/{slug}`** (D8).

- [ ] `access_ref` absent de la réponse publique
- [ ] `slug` / lien présent à la place
- [ ] Test : la réponse publique ne contient aucun motif de code (regex `XXX-XXXX` absente)

⚠️ **F8 — `link_url` est un XSS stocké (à corriger ICI, pas plus tard).** `PublicAnnouncementService.create/update` stocke `linkUrl` **sans validation de schéma**, et `SectionAnnouncements.tsx:86-90` le rend en `href={a.linkUrl}` avec `target="_blank"`. La CSP de `render.yaml:199` autorise `'unsafe-inline'` : un `javascript:…` ne survit qu'à la validation d'un modérateur et s'exécute sur **le domaine public**. Même remarque pour `imageUrl` (pixel de suivi). → Validation `http(s)` **côté serveur**, au seul endroit fiable ; test négatif sur `javascript:` et `data:`.

⚠️ **F19 — `access_ref` est publié publiquement aujourd'hui.** `publicFeed():199-201` renvoie le code tel quel, sans vérifier `isActive` ni `joinMode`. Une église qui colle son code dans une annonce **s'ouvre à quiconque lit la page d'accueil**, et le code y reste même après rotation. D8 tranche : on publie le **lien `/j/<slug>`**, jamais le code. C'est un arbitrage produit — mais il doit être **explicite et assumé**, pas subi par défaut.
---

### 7.2 WEB (React / TypeScript) — corriger la fuite et exposer le transfert

#### T-W1 · `navForRole(user)` : rendre la fuite IMPOSSIBLE 🔗 *T-B2 (le flag existe déjà)*

> **F12 — la fuite est plus large que « les menus ».** `CommandPalette.tsx:90-104` : les **actions rapides** (`/souls/new`, `/search`) filtrent par `activeRole` **sans** regarder `platformSuperAdmin` → un Super Admin cherche et **crée des âmes** depuis la console plateforme. `DashboardGate:330-339` n'a **aucune** branche plateforme. Le test `Sidebar.test.tsx:232-261` ne vérifie que des **libellés de menu**, donc il passe alors que la fuite est réelle. **T-W1 doit couvrir la palette ET le dashboard**, pas seulement la sidebar.

**Fichier** : `frontend/src/workspaces.ts` (lignes 741-763)

Corrige **F1 structurellement**. Changer la signature pour recevoir l'objet utilisateur : un platform admin ne peut **plus** obtenir `FULL_NAV`, quel que soit le composant appelant.

```typescript
export function navForRole(
  user: { platformSuperAdmin?: boolean; role?: string } | null | undefined,
  activeRole?: string | null,
): WorkspaceSection[] {
  if (user?.platformSuperAdmin === true) return PLATFORM_NAV;   // garde structurelle
  switch (activeRole ?? user?.role) {
    case 'RESPONSABLE': return RESPONSABLE_NAV;
    case 'FAISEUR': return FAISEUR_NAV;
    case 'CHEF_DE_FAMILLE': return CHEF_DE_FAMILLE_NAV;
    case 'MEMBRE': return MEMBRE_NAV;
    case 'ADMIN':
    case 'PASTEUR': return FULL_NAV;      // super-utilisateurs d'ÉGLISE uniquement
    default: return FULL_NAV;
  }
}
```

**Appelants à mettre à jour** : `Sidebar.tsx:141`, `CommandPalette.tsx:71`, + **tous** les autres (grep `navForRole(` dans `frontend/src`). Le test existant `Sidebar.test.tsx` appelle aussi `navForRole(role)` : le mettre à jour vers `navForRole({ role })`.

- [ ] Signature changée + garde `platformSuperAdmin`
- [ ] **Tous** les appelants mis à jour (grep `navForRole(`)
- [ ] **CommandPalette** : `quickActions` filtrées par le flag (F12), pas seulement `navCommands`
- [ ] **`DashboardGate`** : branche plateforme (F12) — sinon `/` mène à un dashboard d'église
- [ ] Test `Sidebar.test.tsx` (existant) : platform admin → aucun menu d'église — **le conserver**
- [ ] Test `CommandPalette` : platform admin → **aucune** action rapide d'église (F12)

#### T-W2 · Garde de routes : `/platform/**` strict 🔗 *T-W1*

**Fichier** : `frontend/src/App.tsx` — `ProtectedRoute` (ligne ~285)

Corrige **F2**. Un `platformSuperAdmin` qui ouvre une route tenant (`/dashboard`, `/souls`, `/crm/*`) doit être **redirigé vers `/platform/dashboard`**.

```typescript
// Un platform admin n'a AUCUNE interface d'église (D6).
// Placé AVANT la logique `roles`. Pas de boucle : /platform/* a scope="platform".
if (user?.platformSuperAdmin === true && scope !== 'platform') {
  return <Navigate to="/platform/dashboard" replace />;
}
```

- [ ] Platform admin + route tenant → redirect `/platform/dashboard`
- [ ] Platform admin + `/platform/*` → rendu normal
- [ ] Test : rendre `/souls` avec `platformSuperAdmin` → path final = `/platform/dashboard`

⚠️ **Le redirect ne suffit pas (D6/D7).** Un platform admin qui saisit `/dashboard` sera redirigé vers `/platform/dashboard` — c'est correct. Mais l'**impersonation** doit rester le **seul** chemin d'accès au contenu d'une église, et rester **journalisée, limitée et révocable** (D7). Ne pas rendre les routes tenant accessibles « en lecture seule » comme échappatoire : ce serait une régression de D7.

#### T-W3 · Modales de gouvernance (supprimer `window.prompt`) 🔗 *aucune*

**Fichier** : `frontend/src/pages/PlatformGovernancePage.tsx`

Corrige **F3**. Les 4 `window.prompt` (lignes 54, 67, 78, 89 : motif, message, objet, résolution) deviennent des **modales in-app** à champs contrôlés, en suivant les patterns UI déjà présents dans le projet. Motif de bannissement **obligatoire** (non vide) — c'est la trace d'audit.

- [ ] Zéro `window.prompt` dans la page
- [ ] Motif de bannissement bloquant si vide
- [ ] **Confirmation explicite avant un `ban`** (action quasi irréversible : `CANCELLED`) — today un simple `window.prompt` valide suffit
- [ ] `{t.plan}` rendu (F27) — le backend doit renvoyer `plan`
- [ ] Test : soumission avec motif vide → action refusée

#### T-W4 · Landing : cohérence du CTA « créer une église » 🔗 *aucune*

**Fichier** : `frontend/src/components/landing/Hero.tsx` (lignes 73, 77)

Corrige **F4**. Le Hero émet `?mode=church` ; `RegisterPage.tsx:60` lit `mode`. **C'est le contrat figé** — ne pas renommer le paramètre. Vérifier que les deux CTA pointent vers `/register?mode=church` et `/join`, et que le document §4.1 fait autorité.

- [ ] `?mode=church` confirmé (contrat figé)
- [ ] CTA « Rejoindre une église » → `/join`
- [ ] Test : le Hero navigue vers `mode=church`

#### T-W7 · Écran Propriété & délégation 🔴 *F13 — la demande du client, ZÉRO UI aujourd'hui* 🔗 *T-B0, T-B1*

**Fichier** : `frontend/src/pages/TenantOwnershipPage.tsx` *(nouveau)* — route `/tenant/ownership`

**Pourquoi c'est prioritaire** : les 5 endpoints de `TenantOwnershipController` existent, sont testés côté service, et **ne sont appelés par aucun composant** (`grep ownership frontend/src` → 0 résultat). Le client a demandé explicitement « il peut déléguer d'autres admins » et « se défaire de ce rôle et choisir un remplaçant ». **Sans cet écran, la fonctionnalité n'existe pas pour l'utilisateur.**

Contenu, pour `TENANT_OWNER` / `TENANT_ADMIN` :
- **Vue propriétaire** : nom + email du propriétaire courant (`GET /tenant/ownership`)
- **Délégués** : liste des `TENANT_ADMIN`, avec `promote-admin` / `demote-admin`
- **Cession** : sélectionner un membre → `POST /tenant/ownership/transfer` → **confirmation explicite** (« vous n You'll plus être propriétaire »)
- **Demande de remplacement** : `POST /tenant/ownership/request-replacement` avec motif

⚠️ **Deux gardes à respecter** : (a) `transfer` / `promote` / `demote` exigent `@authz.isTenantOwner()` (T-B0) — l'IHM ne doit les proposer qu'à l'owner, mais **la sécurité reste côté serveur** ; (b) `requestReplacement` **ne persiste rien** aujourd'hui (F23) : soit on ne l'expose pas, soit on l'assume comme simple trace d'audit — **ne pas afficher « la plateforme arbitrera » si aucun écran plateforme ne le consomme**.

- [ ] `/tenant/ownership` : propriétaire, délégués, cession, demande de remplacement
- [ ] `promote` / `demote` câblés sur la vraie API
- [ ] Item de menu ajouté dans `workspaces.ts` (section Administration) **et** dans `routeAccess.ts` (`ADMIN`, `PASTEUR`)
- [ ] Test : owner → sees la liste et peut céder ; `MEMBRE` → page non rendue
- [ ] Test : la cession affiche une confirmation avant `POST /transfer`

#### T-W8 · Sélecteur de sous-église dans la gestion des codes 🔴 *F14* 🔗 *T-B4*

**Fichier** : `frontend/src/pages/TenantJoinManagementPage.tsx` (modifier)

**Pourquoi c'est prioritaire** : le client a demandé « produire un code ou un lien **pour chaque sous-église** ». Le backend sait le faire (`orgNodeId` existe dans `V219`, `POST /tenant/join-codes` l'accepte), mais `TenantJoinManagementPage:46-51` **n'envoie jamais `orgNodeId`** et n'offre aucun sélecteur : l'administrateur **ne peut pas** créer le code d'une sous-église.

Remplace les deux `window.prompt()` (libellé + mode) par un formulaire in-app, et ajoute un **sélecteur de nœud** (`GET /organization/nodes` filtré sur les types `CHURCH` / `CAMPUS` / `SUB_CHURCH`) avec une option « Église principale (racine) ».

- [ ] Formulaire in-app : libellé, nœud cible, mode `OPEN` / `APPROVAL` — **zéro `window.prompt`**
- [ ] Sélecteur de nœud ; « racine » = `orgNodeId` absent
- [ ] Colonne « sous-église » dans la liste des codes
- [ ] Test : créer un code pour un `SUB_CHURCH` → `orgNodeId` présent dans le `POST`

#### T-W5 · Page réseau d'une dénomination 🔗 *T-B3, T-B4*

**Fichier** : `frontend/src/pages/TenantOrganizationPage.tsx` *(nouveau)* — route `/tenant/organization`

Pour `TENANT_OWNER` / `TENANT_ADMIN` : voir l'arborescence de son réseau (racine + enfants), **créer une sous-eglise** (mode `light` ou `autonomous`), **générer un code par sous-eglise**, voir les **délégués**. Route enregistrée dans `App.tsx` sous `<ProtectedRoute scope="tenant">`. Ajouter l'item dans `workspaces.ts` (`ADMIN_NAV` ou équivalent).

- [ ] Arborescence réseau affichée (racine + enfants)
- [ ] Création sous-eglise (2 modes) → `POST /tenant/sub-churches`
- [ ] Codes par sous-eglise (réutilise la logique de `TenantJoinManagementPage`)
- [ ] Test : la page rend les 2 sous-églises et déclenche la création

#### T-W6 · UI de transfert (le membre) 🔗 *T-B5*

**Fichier** : `frontend/src/pages/TransferPage.tsx` *(nouveau)* — route `/transfer`

Flux : le membre colle son nouveau code → `GET /tenant/transfer/preview` → si `sameNetwork`, afficher « Bienvenue, vous êtes déjà membre de [Dénomination]. Confirmer le transfert. » → `POST /tenant/transfer`. Si racine différente, proposer l'adhésion classique.

Intégrer aussi dans `JoinChurchPage.tsx` : quand un membre **connecté** saisit un code de sa **même** dénomination, le bouton devient « **Rejoindre par transfert** » au lieu de « Rejoindre ».

- [ ] `/transfer` : preview + confirmation
- [ ] `JoinChurchPage` détecte `sameNetwork` → CTA « transfert »
- [ ] Après transfert : redirection vers le dashboard de la nouvelle église
- [ ] Test : preview `sameNetwork=true` → libellé de transfert ; confirmation → POST appelé

---

### 7.3 MOBILE (Flutter) — parité web

#### T-M1 · API : helpers de transfert 🔗 *T-B5*

**Fichiers** : `mobile/lib/data/services/auth_service.dart`, modèles dans `mobile/lib/data/models/`

```dart
Future<TransferPreview> previewTransfer(String code);
Future<TransferOutcome> transfer(String code, {String? reason});
```
`TransferPreview { sameNetwork, alreadyMember, fromChurch, toChurch, toKind }`.

- [ ] `previewTransfer` + `transfer` + parsing des modèles
- [ ] Erreurs API gérées (code inconnu, tenant inactif)

#### T-M2 · Écran de transfert 🔗 *T-M1*

**Fichier** : `mobile/lib/screens/transfer_screen.dart` *(nouveau)*

Miroir du flux web §4.4 : saisir le code → preview → si `sameNetwork`, « Vous êtes déjà membre de [Dénomination]. Confirmer le transfert. » → confirmation → dashboard de la nouvelle église.

- [ ] Preview + confirmation
- [ ] Libellés FR (i18n existante)
- [ ] Test widget : preview `sameNetwork` → bouton de transfert actif

#### T-M3 · Register : choisir la nature de l'organisation 🔗 *T-B3*

**Fichier** : `mobile/lib/screens/register_screen.dart` (existe déjà en multi-modes)

Ajouter le choix `kind` lors du mode « Créer mon église » : **Église** (défaut) / **Dénomination** / **Association** / **Organisation**. Mapping vers `createChurch` + `kind` dans le payload register.

- [ ] Sélecteur de `kind` sur l'écran de création
- [ ] Payload register porte `kind`
- [ ] Test : choisir « Dénomination » → payload `kind=DENOMINATION`

#### T-M4 · Isolation console plateforme sur mobile 🔗 *T-W1 (miroir)*

**Fichiers** : `mobile/lib/app.dart`, écran de login

Si `platformSuperAdmin == true`, le menu d'église ne s'affiche **pas** ; afficher un écran « Console disponible sur le web » (le mobile n'expose pas la console super admin, hors périmètre).

- [ ] Platform admin → pas de menu d'église
- [ ] Écran « console sur le web »

---

### 7.4 TESTS, CI ET DOCUMENTATION

#### T-Q0 · Lever le garde-fou CI rouge 🔴 *F15 — AVANT tout merge*

**Fichier** : `backend/src/test/java/com/discipolat/common/domain/NoNullUnsafeMapLiteralTest.java` (le test, **pas** le plafond)

`auditPayloadsUsePayloads` est une **crémaillère** : `AUDIT_POSITION_BACKLOG_CEILING = 122`, « *le plafond ne peut que décroître* », et son message d'échec prescrit le remplacement par `Payloads.of(...)`. Le commit `7390651` ajoute **2 sites** en position d'audit avec `Map.of` brut :

- `SelfServiceChurchService.java:161` → `Map.of("slug", slug, "founderId", …)`
- `TenantOwnershipService.java:201` → `Map.of("reason", …, "suggestedCandidate", …)`

→ **123 > 122 ⇒ ÉCHEC.** Les deux fichiers étant **nouveaux** dans ce commit, `HEAD~1` était à 121 : **vert avant, rouge après**. La CI tourne en temurin **21** (`.github/workflows/ci.yml:45`), donc **la branche ne peut pas passer en l'état**.

**Correctif : remplacer par `Payloads.of(...)`** (la variante null-safe du dépôt). **NE JAMAIS remonter le plafond** pour faire passer le test : ce garde-fou existe précisément pour empêcher la dette de régresser.

- [ ] `Payloads.of(...)` dans les 2 sites
- [ ] `NoNullUnsafeMapLiteralTest` vert, **plafond inchangé à 122**

#### T-Q1 · Tests backend à ajouter

| Test cible | Vérifie | Faille couverte |
|---|---|---|
| `TenantKindTest` | `kind` par défaut `CHURCH`, racine `rootTenantId == id` | F5 |
| `TenantOrganizationServiceTest` | hiérarchie, propagation racine, anti-boucle | F5 |
| `TenantTransferServiceTest` | TRANSFERT / adhésion / idempotence | F6 |
| `TenantTransferControllerTest` | `preview` + `transfer` | F6 |
| `PlatformTenantControllerTest` | liste enrichie, agrégats sans PII | D7 |
| `PublicAnnouncementsControllerTest` | pas de `access_ref` public ; `link_url` `javascript:` rejeté | D8, F8 |
| **`SelfServiceJoinEndToEndIT`** | **créer une église → la session est active** | **F9** |
| **`TenantJoinTokenIT`** | **`POST /tenant/join` → le jeton porte le tenant cible** | **F7** |
| **`TenantAdminGuardIT`** | **`ADMIN` + membership `TENANT_OWNER` → 200 sur les 6 endpoints** | **F10** |
| **`TenantJoinLookupResilienceIT`** | **2ᵉ code principal → `/public/join/resolve` = 200 ; IP forgée → 429** | **F16, F18** |

⚠️ **Les trois tests en gras sont ceux qui manquaient et qui auraient attrapé les P0.** Un test unitaire à mocks sur la normalisation d'une chaîne ne prouve rien : `7390651` est « FAIT » selon son message de commit alors que ses trois chemins critiques sont cassés.

⚠️ **Contrainte Mockito/JDK** : sous **JDK 26**, le mock-maker inline de ByteBuddy refuse d'instrumenter (`IllegalArgumentException: Java 26`) et la suiteBackend affiche **1 144 erreurs** — un artefact d'environnement, **pas** une régression du projet. Le projet cible Java 21 (`pom.xml:22-24`) et la CI utilise temurin 21. **Lancer la suite sous JDK 21**, et ne **jamais** « corriger » ces tests pour masquer une erreur d'environnement. *Seul le `NoNullUnsafeMapLiteralTest` (F15) est un échec réel, imputable au commit.*

- [ ] 9 classes de test ajoutées, vertes sous JDK 21
- [ ] `mvn -q verify` vert, **0 test supprimé**, **plafond de crémaillère inchangé**

#### T-Q2 · Tests web à ajouter

| Test cible | Vérifie |
|---|---|
| `Sidebar.test.tsx` (existant) | platform admin → aucun menu d'église (**conserver**) |
| `App.platformRoutes.test.tsx` *(nouveau)* | route tenant + platform admin → redirect (F2) |
| `workspaces.test.ts` *(étendre)* | `navForRole(user)` structurellement protégé (F1) |
| `PlatformGovernancePage.test.tsx` *(nouveau)* | modales, motif obligatoire, zéro `window.prompt` (F3) |
| `TransferPage.test.tsx` *(nouveau)* | preview `sameNetwork` + confirmation (F6) |

⚠️ **Timeouts** : `vitest.config.ts` fixe `testTimeout: 15000`. Les suites qui montent `App` complet sont lentes ; ne pas réduire le délai global pour faire passer un test.

- [ ] `npx vitest run` vert
- [ ] `npx tsc --noEmit` : 0 erreur
- [ ] `npm run build` OK

#### T-Q3 · Documentation à mettre à jour

- [ ] `docs/SPEC_ONBOARDING_FLOWS.md` §A : aligner sur `?mode=church` (F4) + renvoi vers ce document
- [ ] `docs/MULTI_TENANT_ARCHITECTURE.md` : ajouter le modèle dénomination (`kind`, `root_tenant_id`)
- [ ] `docs/RBAC.md` : préciser que `PLATFORM_SUPER_ADMIN` n'a aucun accès église (D6) et l'isolation par impersonation (D7)
- [ ] `docs/CHANGELOG.md` : entrée pour ce lot
- [ ] `docs/API.md` : routes `/transfer`, `/tenant/sub-churches`, `/platform/tenants` enrichi

#### T-Q4 · Vérifications finales (gate de livraison)

```powershell
cd backend; $env:JAVA_HOME='<JDK21>'; mvn -q verify
cd frontend; npx tsc --noEmit; npx vitest run; npm run build
cd mobile; flutter analyze      # si SDK disponible
```
- [ ] `mvn verify` **vert sous JDK 21**
- [ ] `npx tsc --noEmit` **0 erreur**
- [ ] `npx vitest run` **vert**
- [ ] `npm run build` **OK**
- [ ] `git status` propre : **ne pas committer** `.run-all.ps1`, `.up-infra.ps1`, `.check-docker.ps1`, `audit/*.log`

#### T-Q5 · Exécuter les migrations sur PostgreSQL réel 🔗 *T-B0, T-B4*

`V219`, `V220`, `V221` **n'ont jamais été exécutées par un test** : le profil `test` tourne sur H2 avec `ddl-auto: create-drop` et `flyway.enabled: false` (`application-test.yml:12-14`), et le seul gate PG (`FlywayMigrationChainPostgreSqlTest`) est `disabledWithoutDocker` → **skippé** dès que le daemon Docker est absent.

L'audit a vérifié **à la main** la parité colonnes↔entités des 4 tables (correcte), mais **aucun processus automatisé ne le vérifie**. C'est la famille de bugs « H » déjà subie trois fois sur ce dépôt (`events` vs `legacy_events`, `analyse` mot réservé PG, 45 colonnes accentuées absentes du schéma). **Un `500` en production sur `/tenant/join-codes` ou `/public/announcements` serait exactement le symptôme.**

- [ ] `FlywayMigrationChainPostgreSqlTest` exécuté au moins une fois (Docker local ou runner containerisé) → chaîne verte jusqu'à `V221`
- [ ] Gate PG **bloquant** dans le workflow, ou `disabledWithoutDocker` résolu
- [ ] Après migration complète : `tenant_join_codes`, `tenant_join_requests`, `public_announcements`, `tenant_warnings`, `tenant_disputes` présents
- [ ] `EntityBuilderDefaultsTest` / `SchemaVerificationIntegrationTest` couvre les 4 entités nouvelles

---

## §8. GARDE-FOUS (non négociables)

1. **Ne jamais exposer** `tenant_id`, email, ni code dans une réponse `/public/**`.
2. **Toute bascule de tenant** passe par `CrossTenantScopeAccess.callForTenantSwitch` (pattern H4), restauration du `TenantContext` en `finally`.
3. **Emails d'échec non bloquants** (`try/catch` + log), pattern `InvitationService`.
4. **Transfert transactionnel** : soit il réussit entièrement, soit rien (pas d'adhésion orpheline).
5. **Le Super Admin ne voit que des agrégats.** Toute vue nominative passe par impersonation journalisée.
6. **Suppression d'une dénomination** : jamais de `DELETE` si elle a des enfants (`ON DELETE RESTRICT`) — passer par `CANCELLED`.
7. **Migrations montantes** (`V222+`). Jamais d'édition d'une migration existante.
8. **Aucune régression** : le flux legacy « demande de création approuvée » reste intact. **0 test supprimé.**
9. **Le Super Admin n'a aucun rôle d'église.** Le `PLATFORM_SUPER_ADMIN` n'ouvre que `/platform/**`.
10. **JAMAIS `hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')`** sur un endpoint tenant : ces autorités n'existent pas dans le JWT (F10). Utiliser `@authz.isTenantAdmin()` / `isTenantOwner()`.
11. **Toute URL fournie par un utilisateur est validée côté serveur** avant stockage (F8) — `http`/`https` uniquement, jamais `javascript:`, `data:`, `vbscript:`.
12. **Un motif d'action de gouvernance est obligatoire, persisté et affiché** (F11). Une action destructrice sans trace exploitable est interdite.
13. **Un test qui prouve un P0 est obligatoire** avant merge (D20). Un test unitaire à mocks sur une fonction pure ne prouve pas un flux de bout en bout.
14. **Ne jamais lire `payload.church` / `session` en écrivant `localStorage` à la main** hors `AuthContext` (F9) : utiliser `adoptSession()`.

---

## §9. RÉCAPITULATIF — ce qui change pour l'utilisateur final

| Avant | Après |
|---|---|
| Créer une église = demande d'approbation | **Immédiat**, fondateur = propriétaire |
| Membre change d'église = se réinscrit | **Transfert**, 0 réinscription |
| Sous-églises = groupes cachés | **Codes par sous-eglise** + réseau (dénomination) |
| Super Admin voit des menus d'église | **Console plateforme** isolée (D6) |
| Tenant = « une église » | **Dénomination / église / asso / orga / méga-asso** (D2) |
| Code affiché publiquement | **Jamais** ; lien `/j/slug` (D8) |

---

## §12. ORDRE D'EXÉCUTION RÉCAPITULATIF

```
PHASE 0 — REMÉDIATION (bloquante, ~4 h)
   T-B0      gardes @authz.isTenantAdmin() / isTenantOwner()      F10  ← débloque tout
   T-W0      adoptSession() dans AuthContext                     F9
   T-B0bis   /tenant/join renvoie les jetons                      F7
   T-B0ter   extractClientIp : IP de confiance                   F18
   T-Q0      Payloads.of() → NoNullUnsafeMapLiteralTest vert      F15
   ───────────────────────────────────────────────────────────────────
PHASE 1 — SÉCURITÉ PUBLIQUE (~1 h)
   T-B6      link_url http(s) + retrait de access_ref             F8, F19
   T-B2      motif de blocage persisté + plan + pagination        F11, F27
   T-W1/T-W2 navForRole(user) + garde de routes + palette         F1, F2, F12
   T-W3      modales de gouvernance, motif obligatoire            F3
PHASE 2 — LES DEUX INTERFACES MANQUANTES (~1 jour, la demande du client)
   T-W7      écran Propriété & délégation (F13) — ZÉRO UI aujourd'hui
   T-W8      sélecteur de sous-église dans la gestion des codes (F14)
   T-B4      unicité du code principal + memberships multiples   F16, F17
PHASE 3 — MODÈLE DÉNOMINATION (§5, §6, T-B1 → T-B5)
   V222, V223, TenantTransferService, TransferPage, réseau
PHASE 4 — MOBILE (§7.3), puis TESTS & CI (§7.4)
   T-Q5      migrations V219-V221 exécutées sur PostgreSQL réel  F24
```

**Règle absolue** : ne pas commencer la Phase 3 sans la Phase 2 verte. `V222` modifie `tenants` — empiler une migration de hiérarchie sur une base dont les six endpoints d'administration d'église renvoient `403` ne produit rien d'utilisable.

---

## §11. ÉTAT DES TRAVAUX CONCURRENTS (04/10/2026, 10h)

> À lire avant de toucher à `RegisterPage.tsx` : **un autre agent travaille dessus en ce moment.**

**Non commité dans l'arbre de travail** (`git status` : `M frontend/src/pages/RegisterPage.tsx`, `?? frontend/src/__tests__/RegisterPageInviteLink.test.tsx`) : ajout du **lien `/j/<slug>`** sur l'écran fondateur, du **copier-coller** et d'un **partage WhatsApp** (`wa.me`) — c'est-à-dire la §4.1 de ce document. Le test `RegisterPageInviteLink.test.tsx` verrouille la présence des **deux** portes (code **et** lien).

**Ce que ce travail ne corrige pas** : `localStorage.removeItem('user')` est **toujours là** (`RegisterPage.tsx:143`) → **F9 reste ouvert**. Le rechargement complet masque le symptôme pendant les tests manuels : c'est exactement pour cela qu'il n'a pas été vu. Ne pas considérer l'écran fondateur comme « fini » tant que **T-W0** n'est pas fait.

**Règle de coordination** : ne pas éditer `RegisterPage.tsx` ni ce document en parallèle sans vérifier `git status` — un écrasement silencieux coûte le travail de l'autre agent.

---

## §10. MODÈLE DE RAPPORT (à remplir par chaque agent en fin de tâche)

Copier ce bloc dans `docs/rapports/RAPPORT_<taskId>.md` et le remplir factuellement.

```markdown
## <taskId> — <titre>
- Fichiers créés / modifiés (chemins exacts) :
- Migration(s) ajoutée(s) : V___ (ou « aucune »)
- Tests ajoutés / exécutés : <commande> → <résultat chiffré>
- Décisions prises (et pourquoi) :
- Points non faits / reportés (et pourquoi) :
- Risques / régressions possibles :
```
