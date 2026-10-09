# SPEC_ORGANISATION_MODULABLE_V3 — Hiérarchie configurable, rôles modulables, modules par nœud, agrégats & propulsion

> **STATUT** : proposition de spécification — **à valider par le produit**. Découle des arbitrages pris le 05/10/2026 (voir §2).
> **VERSION DU DOCUMENT** : v3.0 — rédigée contre `main` au commit `ded3ab4b`.
> **DESTINATAIRES** : agents d'implémentation **backend (Java/Spring Boot)**, **web (React/TS)**, **mobile (Flutter)**, **QA/CI**.
> **RÈGLE D'OR** : aucun agent ne code avant d'avoir lu §0 à §3 et vérifié l'**état des lieux §1** (ce qui est FAIT ne doit pas être refait).
> **HÉRITAGE** : prolonge **`SPEC_ORGANISATION_DENOMINATION_V2.md`** (VALIDÉE, partiellement mergée V219–V223). V2 pose le socle *organisation / transfert / console plateforme*. **V3 ajoute la couche « tout est modulable »** que V2 ne couvre pas. Les §1.2 à §1.4 de V2 (couches, mode hybride, transfert) restent la référence et **ne sont pas remis en cause**.

---

## §0. OBJET & PÉRIMÈTRE

### 0.1 Problem statement

V2 permet déjà de modéliser **une dénomination qui fédère des églises** (`Tenant.kind/parent/root`, `OrganizationNode`, transfert sans réinscription, console plateforme). Mais le besoin produit réel est plus fort :

> **Tout doit être modulable à chaque échelle** — qu'une dénomination puisse **définir sa propre structure** (niveaux qui lui appartiennent : « région », « zone », « district », « circonscription »…), **ses propres noms de rôles** (ce qu'ICC appelle « pasteur assistant », une autre l'appelle « diacre », une troisième « ancien »), **ses propres fonctionnalités activées par église/campus**, **sa propre identité visuelle**, et que chaque niveau ait **son admin**, avec des **vues agrégées** (nombre de fidèles, progression, sermons, sujets de prière…) en drill-down.

V2 **ne couvre pas** ces 5 dimensions. V3 les spécifie.

### 0.2 Ce que V3 AJOUTE (et rien d'autre)

| ID | Dimension | Apport V3 |
|---|---|---|
| **A** | **Niveaux hiérarchiques configurables** | Un **référentiel de niveaux par racine** (dénomination) : l'admin crée/renomme/ordonne ses propres niveaux, au lieu de l'enum figé `OrganizationNodeType`. |
| **B** | **Rôles : capacité ≠ intitulé** | Séparer le **rôle-capacité** (jeu d'autorisations portable) de l'**intitulé affiché** (renommable par église/niveau). Panoplie de rôles custom. |
| **C** | **Affiliation multi-églises d'un membre** | Un membre peut porter un rôle sur **plusieurs nœuds** (ex. un « ancien » accrédité sur 3 campus) et être rattaché à d'autres membres. |
| **D** | **Modules & branding par nœud (indépendance totale)** | Chaque église/campus **choisit ses modules et son thème** ; la dénomination **aggrège** (ne force pas). |
| **E** | **Dashboard agrégé du pasteur (drill-down)** | Région → zone → campus → église : compteurs **remontés** (fidèles, églises, progression temporelle, sermons, prières) + responsable. |

### 0.3 Ce que V3 NE CHANGE PAS (interdits explicites)

- **Ne pas** retoucher le format des codes de rejointure (`PREFIXE-XXXX`, D9 V2). **Ne pas** toucher aux migrations `V ≤ 223` (**D14** : montantes uniquement, ici à partir de **`V224`**).
- **Ne pas** réintroduire `hasAnyRole('TENANT_OWNER','TENANT_ADMIN')` (autorité JWT inatteignable — **F10 V2**, déjà corrigé par `@authz.isTenantAdmin()/isTenantOwner()`). Les nouveaux gardes lisent `tenant_memberships`.
- **Ne pas** casser l'`OrganizationNode` existant ni le `ConfigurationResolver` (héritage `DEFAULT/INHERITED/OVERRIDDEN` déjà en place) — on les **étend**.
- **Ne pas** créer de régression : `mvn verify` vert, parité **entités ↔ schéma PG** vérifiée sur PostgreSQL réel (pas seulement H2), **0 test supprimé**.

---

## §1. ÉTAT DES LIEUX — FAIT / PARTIEL / ABSENT (à vérifier avant de coder)

> Référence = `main` @ `ded3ab4b`. Chaque ligne cite le fichier réel. **Un agent doit re-vérifier la preuve avant d'agir** (le code bouge vite sur ce dépôt).

### 1.1 FAIT — socle à réutiliser, NE PAS REFAIRE

| Brique | Fichier / preuve | Ce que ça donne |
|---|---|---|
| Nature de l'organisation | `tenants/domain/TenantKind.java` (`CHURCH, DENOMINATION, ASSOCIATION, ORGANIZATION, MEGA_ASSOCIATION`) ; `Tenant.java:65-102` (`kind`, `parentTenantId`, `rootTenantId`, `effectiveRootTenantId()`) ; migration `V222__tenant_organization_model.sql` | Une dénomination = une racine ; les églises pendent via `parent_tenant_id`. |
| Transfert sans réinscription | `TenantTransferService.java` ; `V223__membership_transfer.sql` ; `JoinOutcome` porte `accessToken/refreshToken` (`TenantJoinService.java:107-119`) | **F7 V2 soldé** : la bascule réémet les jetons. |
| Gardes d'accès scopées | `AuthorizationService.java:139-186` (`isTenantAdmin()`, `isTenantOwner()`) ; `TenantJoinCodeController.java:44`, `TenantOwnershipController.java:54-93` | **F10 V2 soldé** : `@authz.*` lit les memberships. |
| Arborescence | `OrganizationNode.java` (`parent_id`, `path`, `level`, `responsible_id`, `color`, `icon`, `metadata_json`, `config_source`, `resolved_config_json`) ; `OrganizationHierarchyService.java` (`getDescendants/getAncestors/moveNode`) | Drill-down structurel déjà possible ; **un responsable par nœud** existe. |
| Héritage de config | `ConfigurationResolver.java` (`resolveConfig`, `setConfigSource`, `updateLocalConfig`, `invalidateResolvedConfig`) ; `OrganizationNode.ConfigSource = DEFAULT\|INHERITED\|OVERRIDDEN` | Mécanisme de remontée + override + cache + événement d'invalidation **déjà écrit**. |
| Catalogue modules | `ModuleDefinition.java` (code/nom/catégorie/`features_json`), `ModuleCatalogService`, `ModuleRouter` | Source de vérité des modules activables. |
| Modules par tenant | `TenantFeature.java` (`module_code`, `enabled`, `configuration_json`, `limits_json`) ; `FeatureAccessService` | Activer un module à l'échelle **tenant**. |
| Modules par espace | `SpaceModule.java` (`space_id`, `module_code`, `enabled`, `configuration_json`, `display_order`) | Activer un module à l'échelle d'un **Space**. |
| Rôles custom | `Role.java` (`tenant_id` NULL=globaux, `key`, `label`, `system`, `priority`, `permissions` M2M) ; `RoleManagementService.java` (`createCustomRole/updateRole/deleteRole`, `getCustomRoles`) | Créer un rôle + son label **par tenant**. |
| Permissions & portée | `Permission.java` (`key`, `label`, `description`), `PermissionMatrixService`, `PermissionScope` (`GLOBAL,TENANT,CHURCH,SUB_CHURCH,DEPARTMENT,FAMILY,OWN`) ; `MembershipScopeType` (`TENANT,REGION,CHURCH,SUB_CHURCH,CAMPUS,DEPARTMENT,FAMILY,ASSIGNED,OWN`) | Grille d'autorisations + périmètres **déjà énumérés**. |
| Spaces liés à un nœud | `spaces/domain/Space.java` (`organization_unit_id` → nœud, `space_type=DEPARTMENT\|FAMILY\|SUB_TEAM`, `color`, `configuration_json`, `visible_people_scope`) | Départements/familles rattachés à une unité d'organisation. |
| Service organisation | `TenantOrganizationService.java` (`createDenomination`, `createChildChurch`, `childrenOf`, `networkOf`, `roots`, `view`) ; `TenantOrganizationController.java` (`GET /tenant/organization`, `/network`, `POST /sub-churches`) | Créer dénomination + église enfant, naviguer le réseau. |
| Écrans web existants | `OrganizationBrowserPage.tsx`, `TenantOrganizationPage.tsx`, `TenantAdminBrandingPage.tsx`, `TenantAdminRolesPage.tsx`, `TenantAdminModulesPage.tsx`, `TenantAdminSpacesPage.tsx`, `TenantJoinManagementPage.tsx`, `TenantOwnershipPage.tsx`, `TenantSwitcherPage.tsx` | Base UI sur laquelle brancher V3. |
| Écrans mobile existants | `presentation/screens/tenant/{organizations,roles,modules}_screen.dart`, `network/network_screen.dart`, `transfer*/…`, `login/join_church_screen.dart` | Parité à étendre. |
| Console plateforme | `PlatformModulesPage.tsx`, `PlatformMenusPage.tsx`, `platform_modules_screen.dart` | Vue flotte super-admin. |

### 1.2 PARTIEL — à compléter (existant mais incomplet)

| Sujet | Ce qui manque | Tâche |
|---|---|---|
| **Noms de nœuds personnalisables** | `OrganizationNodeType` est un **enum Java figé** (`ROOT_CHURCH, CAMPUS, SUB_CHURCH, ASSEMBLY, REGION, DISTRICT, DEPARTMENT, GROUP`). Un `code`/`slug`/`icon`/`color` par nœud existe, mais **pas de définition de niveau par dénomination**. | A → T-B10 |
| **Intitulés de rôles par église** | `Role.label` est modifiable **par tenant**, mais **pas par nœud/église**, et il n'y a **pas de distinction capacité/intitulé** (le même objet porte les permissions ET le nom affiché). | B → T-B11 |
| **Affiliation multi-nœuds** | `TenantMembership` porte un **seul** `scopeType`/`scopeId`. F17 (V2) signale que les memberships multiples `(user,tenant)` font exploser des `Optional` (`TenantOwnershipService`, `RoleManagementService`, `AiModuleService`). | C → T-B12 (nécessite T-B-fix-F17) |
| **Modules par nœud** | `TenantFeature` = tenant ; `SpaceModule` = space. **Rien n'accorde un module à un `OrganizationNode`** (un campus précis). | D → T-B13 |
| **Branding par nœud** | `OrganizationNode.color` existe (couleur unique) ; `TenantSettings` = thème complet **par tenant**. **Pas de thème complet héritable/override par nœud.** | D → T-B14 |
| **Agrégats descendants** | `childCount`/`memberCount` vus côté plateforme (V2). Côté **tenant**, pas de remontée de compteurs par échelle ni de séries temporelles (progression). | E → T-B15 |

### 1.3 ABSENT — à créer de zéro

| Sujet | Raison |
|---|---|
| Référentiel de niveaux (`organization_levels`) | L'enum figé ne permet pas « zone/circonscription/section ». |
| Table `member_role_assignments` (rôle portable multi-nœuds) | Modèle d'affiliation « capacité » découplée de l'appartenance. |
| Table `role_titles` (intitulés par scope) | Nommer « Diacre » ici, « Pasteur assistant » là. |
| Table `organization_node_features` | Choisir les modules **à la création d'un campus** et par nœud. |
| Service + endpoints de **drill-down agrégé** côté tenant | Vue pasteur région→campus. |
| **Templates/presets** de dénomination (cloner structure + rôles + modules) | Propulsion §8. |
| **Délégation d'admin par nœud** (admin d'une région, d'un district) | « chaque niveau a son admin ». |
| **Communication en cascade** (annonce → tous les campus sous une région) | Propulsion §8. |
| **Carte / annuaire public du réseau d'églises** | Propulsion §8. |

### 1.4 Dette & garde-fous à respecter (état du dépôt)

- **Parité entités↔PG** : déjà faillie par le passé (`45 colonnes accentuées absentes`, commit `811d72f6`). **Toute** colonne V3 doit exister en migration PG **et** dans le schéma de test. Gate : `FlywayMigrationChainPostgreSqlTest` (exige Docker).
- **CI `NoNullUnsafeMapLiteralTest`** : crémaillère `AUDIT_..._CEILING` décroissante. En position d'audit, **jamais** `Map.of(...)` brut → utiliser `Payloads.of(...)`. **Ne jamais** remonter le plafond.
- **H4 `crossTenant`** : toute bascule de tenant passe par `CrossTenantScopeAccess.callForTenantSwitch` + restauration `finally`.
- **i18n** : les écrans V2 sont sortis **sans clés** (F26 V2). Toute UI V3 doit ajouter ses clés dans les **5 langues**.

---

## §2. PRINCIPES PRODUIT — arbitrages validés 05/10/2026

| # | Décision | Détail | Conséquence technique |
|---|---|---|---|
| **V3-A** | **Niveaux entièrement configurables par dénomination.** | On remplace la dépendance à l'enum par un **référentiel de niveaux** propre à la racine. L'enum reste comme **type sémantique interne** (pour la logique transverse), mais l'**affichage et la navigation** utilisent le niveau custom. | Table `organization_levels` ; `organization_nodes.level_id` (FK, nullable → fallback type). |
| **V3-B** | **Capacité ≠ intitulé.** Un **rôle-capacité** (`Role`, jeu de permissions, portable) ; un **intitulé** par scope (`role_titles`) qui renomme l'affichage (« Diacre » / « Pasteur assistant » / « Ancien »). | L'autorisation ne dépend **jamais** de l'intitulé. Deux églises d'une même dénomination peuvent nommer pareil rôle différemment. | `role_titles (role_id, scope_node_id NULL=tenant, label)` ; `Role.capabilityKey` stable. |
| **V3-C** | **Affiliation multiple.** Un membre porte un rôle-capacité sur **0..n nœuds** (`member_role_assignments`), distinct de son **appartenance** (`TenantMembership`). | Gère « je nomme X ancien et je lui affili ces 3 campus / ces membres ». | Nouvelle table + **correction F17** des `Optional` avant usage. |
| **V3-D** | **Indépendance totale des modules & branding par nœud** (arbitré « independence »). | Chaque église/campus **choisit** ses modules et son thème **dès la création**. La dénomination **aggrège** et peut **proposer des presets**, mais **ne force pas** l'héritage. | `organization_node_features` avec défaut = **vide/indépendant** ; `config_source` par défaut `DEFAULT` (non `INHERITED`) pour les nœuds. |
| **V3-E** | **Dashboard agrégé drill-down.** | Toute échelle expose les compteurs **remontés** de sa descendance + une **progression** (série temporelle). | Service de lecture `NodeAggregateService` + snapshots. |

> **Note de cohérence V3-D vs héritage V2** : le `ConfigurationResolver` sait faire `INHERITED`/`OVERRIDDEN`. On ne le supprime pas — on change **la valeur par défaut des nouveaux nœuds** à `DEFAULT` (indépendant) et on n'expose l'option « hériter du parent » que comme **confort optionnel**, jamais comme contrainte.

---

## §3. MODÈLE MENTAL ENRICHI

### 3.1 Quatre objets, quatre rôles

```
DENOMINATION (Tenant racine, kind=DENOMINATION)   ← « La Vie de Christ »
│   possède un RÉFÉRENTIEL DE NIVEAUX (organization_levels) :
│     niveau #1 "Région" · niveau #2 "Zone/District" · niveau #3 "Campus/Église" · …
│   (profondeur et noms = libres ; chaque niveau déclare : parent autorisé, icône, ordre)
│
├── OrganizationNode  (léger : région, zone, campus, groupe … dans le même tenant)
│     ├─ level_id      → le niveau custom (nom affiché = libellé du niveau)
│     ├─ responsible_id→ le pasteur/référent de CE nœud (déjà existant)
│     ├─ config_source = DEFAULT (indépendant, V3-D)
│     └─ organization_node_features → modules activés pour CE nœud
│
├── Tenant enfant (autonome : église avec ses membres/facturation)  [V2]
│
└── Membre :
      TenantMembership          → « j'appartiens à cette organisation » (1 ACTIVE / scope) [V2]
      member_role_assignments   → « je porte ce rôle-capacité sur ces nœuds » (0..n)  [V3-C]
      role_titles               → « ici ce rôle s'appelle Diacre, là Ancien »          [V3-B]
```

### 3.2 Règles d'invariants (à coder/enforcer)

1. **Un niveau custom appartient à une racine** (`root_tenant_id`) ; deux dénomination n'ont jamais les mêmes ids de niveau.
2. **Tout nœud a un `level_id`** valide de SA racine, **ou** retombe sur l'enum `type` (back-compat). `level_id` est la source d'affichage ; `type` la source de logique.
3. **Un intitulé ne change jamais une permission.** Autorisation = `role.permissions` ∩ portée de l'assignation (`member_role_assignments.node_id`), **jamais** via le label.
4. **Modules par nœud = indépendant par défaut.** L'activation d'un module sur la racine **n'implique pas** son activation sur les enfants ; l'agrégat « qui a ce module ? » se calcule en descendant.
5. **Les compteurs agrégés sont recalculés** (snapshot) et **non dérivés nominatifs** côté super-admin (D7 V2 inchangé).

---

## §4. MODÈLE DE DONNÉES — migrations (à partir de `V224`)

> **D14** : montantes uniquement, **jamais** éditer `V ≤ 223`. Cible **PostgreSQL** ; **toute** colonne doit être répliquée dans le schéma de test et validée par le gate PG. Pas de `CHECK`/`COMMENT` multi-lignes non portés en test.

### V224__organization_levels.sql — référentiel de niveaux configurable (A)

```sql
-- Un « niveau » = une étape de hiérarchie définie PAR la dénomination (racine).
-- Ex. Région(1) > Zone(2) > Campus(3). Le nom, l'ordre, la parenté sont libres.
CREATE TABLE IF NOT EXISTS organization_levels (
    id                  UUID PRIMARY KEY,
    root_tenant_id      UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name                VARCHAR(120) NOT NULL,        -- libellé affiché (« Zone »)
    plural_name         VARCHAR(120),                 -- (« Zones »)
    description         TEXT,
    depth_order         INTEGER NOT NULL,             -- 1 = juste sous la racine
    -- Type sémantique interne : conserve la logique transverse (agrégats, règles).
    semantic_type       VARCHAR(30) NOT NULL DEFAULT 'CUSTOM'
        CHECK (semantic_type IN ('ROOT_CHURCH','CAMPUS','SUB_CHURCH','ASSEMBLY','REGION','DISTRICT','DEPARTMENT','GROUP','CUSTOM')),
    parent_level_id     UUID REFERENCES organization_levels(id) ON DELETE SET NULL, -- null = sous la racine
    icon                VARCHAR(100),
    color               VARCHAR(7),
    is_branching        BOOLEAN NOT NULL DEFAULT TRUE, -- peut avoir des enfants du même niveau ?
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    CONSTRAINT uk_org_level_root_order UNIQUE (root_tenant_id, depth_order)
);
CREATE INDEX IF NOT EXISTS idx_org_level_root ON organization_levels(root_tenant_id);
CREATE INDEX IF NOT EXISTS idx_org_level_parent ON organization_levels(parent_level_id);

-- Le nœud référence un niveau custom (nullable → back-compat sur l'enum `type`).
ALTER TABLE organization_nodes ADD COLUMN IF NOT EXISTS level_id UUID
    REFERENCES organization_levels(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_org_node_level ON organization_nodes(level_id);
```

### V225__role_titles.sql — intitulés par scope (B)

```sql
-- L'intitulé AFFICHÉ d'un rôle-capacité, redéfinissable par église/nœud.
-- La permission ne bouge pas : seul le nom change.
CREATE TABLE IF NOT EXISTS role_titles (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    node_id       UUID REFERENCES organization_nodes(id) ON DELETE CASCADE, -- NULL = libellé par défaut du tenant
    label         VARCHAR(120) NOT NULL,        -- « Diacre », « Pasteur assistant », « Ancien »
    label_plural  VARCHAR(120),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    -- Un seul intitulé par (rôle, nœud) ; node_id NULL = défaut tenant.
    CONSTRAINT uk_role_title_tenant_role_node UNIQUE (tenant_id, role_id, node_id)
);
CREATE INDEX IF NOT EXISTS idx_role_title_lookup ON role_titles(role_id, node_id);
```

> **Note H2/PG** : `UNIQUE (… node_id)` avec `node_id` NULL admet **plusieurs** NULL en PG standard → ajouter une **index partiel unique** `CREATE UNIQUE INDEX … ON role_titles(role_id, tenant_id) WHERE node_id IS NULL;` pour garantir « un défaut ». Vérifier le comportement en test.

### V226__member_role_assignments.sql — affiliation multi-nœuds (C)

```sql
-- « X porte le rôle R sur le nœud N » (0..n). Découplé de l'appartenance.
CREATE TABLE IF NOT EXISTS member_role_assignments (
    id             UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id        UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    node_id        UUID REFERENCES organization_nodes(id) ON DELETE CASCADE, -- NULL = portée tenant
    assigned_by    UUID REFERENCES users(id),
    assigned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE','SUSPENDED','ENDED')),
    ended_at       TIMESTAMPTZ,
    -- Empêche le doublon actif (user, role, node).
    CONSTRAINT uk_mra_active UNIQUE (user_id, role_id, node_id, status)
);
CREATE INDEX IF NOT EXISTS idx_mra_user ON member_role_assignments(user_id, status);
CREATE INDEX IF NOT EXISTS idx_mra_node ON member_role_assignments(node_id, role_id);
```

> Un **rattachement de personne à personne** (« un ancien accompagne ces membres ») réutilise `mentoring`/`succession`/`referrals` existants — **ne pas** inventer une nouvelle table ; exposer une vue « assignments ∪ followers ». À confirmer en T-B12.

### V227__organization_node_features.sql — modules par nœud (D)

```sql
-- Activer/configurer un module pour un NŒUD précis (campus/église), indépendant par défaut.
CREATE TABLE IF NOT EXISTS organization_node_features (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    node_id           UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    module_code       VARCHAR(50) NOT NULL,       -- référence module_definition.code
    enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    configuration_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    limits_json        JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ,
    CONSTRAINT uk_node_feature UNIQUE (node_id, module_code)
);
CREATE INDEX IF NOT EXISTS idx_node_feature_tenant ON organization_node_features(tenant_id, module_code);
```

### V228__node_branding_and_aggregates.sql — thème par nœud + snapshots agrégés (D+E)

```sql
-- Thème complet héritable/override par nœud (subset de TenantSettings).
ALTER TABLE organization_nodes ADD COLUMN IF NOT EXISTS theme_json JSONB;      -- override local (V3-D : null = indépendant)

-- Snapshot des agrégats recalculés (fidèles, églises, progression) par nœud.
CREATE TABLE IF NOT EXISTS node_aggregate_snapshots (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    node_id           UUID NOT NULL REFERENCES organization_nodes(id) ON DELETE CASCADE,
    snapshot_at       TIMESTAMPTZ NOT NULL,
    member_count      BIGINT NOT NULL DEFAULT 0,   -- fidèles rattachés au sous-arbre
    church_count      BIGINT NOT NULL DEFAULT 0,   -- nœuds « église/campus » descendants
    leader_count      BIGINT NOT NULL DEFAULT 0,   -- porteurs d'un rôle-capacité de direction
    sermon_count      BIGINT NOT NULL DEFAULT 0,
    prayer_topic_count BIGINT NOT NULL DEFAULT 0,
    metrics_json      JSONB NOT NULL DEFAULT '{}'::jsonb,  -- extension (progression, attendance…)
    UNIQUE (node_id, snapshot_at)
);
CREATE INDEX IF NOT EXISTS idx_node_agg_lookup ON node_aggregate_snapshots(node_id, snapshot_at DESC);
```

### 4.1 Backfill & compatibilité

- **V224 backfill** : pour chaque racine `kind IN (DENOMINATION, MEGA_ASSOCIATION, ASSOCIATION, ORGANIZATION)`, **graine** des niveaux par défaut moulés sur l'enum (`ROOT_CHURCH→1`, `REGION→2`, `DISTRICT→3`, `CAMPUS→4`, `GROUP→5`) via `organization_levels` ; puis `UPDATE organization_nodes SET level_id = …` par correspondance `type`. Un nœud `type=CUSTOM` sans `level_id` reste autorisé (repli sur `type`).
- **Zéro suppression de colonne.** `type` (enum) **reste NOT NULL** et continue d'orchestrer la logique ; `level_id` n'apporte que le **libellé/ordre/parenté** métier.
- **Rétro-compat mobile/web** : les clients qui ignorent `level_id` affichent `type` ; ceux qui le connaissent affichent le nom du niveau.

---

## §5. SURFACE API (à créer)

> Préfixe `/api/v1`. `permitAll` **uniquement** sous `/public/**`. Gardes = `@authz.isTenantAdmin()` / `isTenantOwner()` / `isPlatformSuperAdmin()` **lisant `tenant_memberships`** (JAMAIS `hasAnyRole` JWT). Toute bascule de portée = `crossTenant.callForTenantSwitch` (H4).

### 5.1 Niveaux configurables (A) — admin tenant

| Méthode | Route | Garde | Payload / réponse |
|---|---|---|---|
| `GET` | `/tenant/organization/levels` | `isTenantAdmin()` | liste des niveaux de la racine courante, ordonnés |
| `POST` | `/tenant/organization/levels` | `isTenantOwner()` *ou* admin délégué (§8.2) | `{name, pluralName?, semanticType, depthOrder, parentLevelId?, icon?, color?}` → level |
| `PATCH` | `/tenant/organization/levels/{id}` | idem | renommage / réordonnancement / activation |
| `DELETE` | `/tenant/organization/levels/{id}` | `isTenantOwner()` | refus si des nœuds l'utilisent (409) → proposer migration vers autre niveau |

### 5.2 Rôles, intitulés & affiliations (B, C)

| Méthode | Route | Garde | Détail |
|---|---|---|---|
| `GET` | `/tenant/roles` | `isTenantAdmin()` | rôles globaux + custom du tenant (existant) |
| `GET` | `/tenant/roles/{id}/titles?nodeId=` | `isTenantAdmin()` | intitulés résolus pour un nœud (fallback tenant) |
| `PUT` | `/tenant/roles/{id}/titles` | `isTenantAdmin()` | `{nodeId?, label, labelPlural?}` upsert |
| `GET` | `/tenant/members/{userId}/assignments` | `isTenantAdmin()` | rôles-capacités + nœuds du membre |
| `POST` | `/tenant/members/{userId}/assignments` | `isTenantAdmin()` | `{roleId, nodeId?, note?}` → assignment |
| `DELETE` | `/tenant/members/{userId}/assignments/{assignmentId}` | `isTenantAdmin()` | fin (status=ENDED, pas suppression physique) |

### 5.3 Modules & branding par nœud (D)

| Méthode | Route | Garde | Détail |
|---|---|---|---|
| `GET` | `/tenant/organization/nodes/{nodeId}/features` | `isTenantAdmin()` | modules activés pour ce nœud |
| `PUT` | `/tenant/organization/nodes/{nodeId}/features` | admin du nœud (§8.2) | `{modules:[{code,enabled,configurationJson?}]}` (défaut indépendant) |
| `GET` | `/tenant/organization/nodes/{nodeId}/theme` | `isTenantAdmin()` | thème résolu (`theme_json` override sinon indépendant) |
| `PATCH` | `/tenant/organization/nodes/{nodeId}/theme` | admin du nœud | override de thème |

### 5.4 Agrégats & drill-down (E)

| Méthode | Route | Garde | Détail |
|---|---|---|---|
| `GET` | `/tenant/organization/tree` | `isTenantAdmin()` | arbre complet avec `levelName`, `responsibleName`, compteurs inline |
| `GET` | `/tenant/organization/nodes/{nodeId}/aggregate` | `isTenantAdmin()` | dernier snapshot + progression (série) |
| `GET` | `/tenant/organization/nodes/{nodeId}/children` | `isTenantAdmin()` | enfants + leur aggregate (vue « clique région → voir zones ») |

### 5.5 Réutilisation existante

`TenantOrganizationController` (`/tenant/organization`, `/network`, `POST /sub-churches`) et `TenantJoinCodeController` (`orgNodeId`) **restent** ; on les **étend** (champs `levelId`, `levelName`) sans casser les contrats. Les `CreateSubChurchRequest` acceptent un `levelId?` optionnel (défaut = niveau « église/campus » de la racine).


---

## §6. FRONTEND WEB (React / TypeScript)

### 6.1 Accès & navigation (rappel des contraintes V2)

- `workspaces.ts` → `navForRole(user, activeRole)` (signature **déjà** en objet, cf. V2 T-W1). Toute nouvelle page org doit être **rattachée à un rôle tenant**, jamais exposée au super-admin (**D6 V2**).
- `routeAccess.ts` + `ProtectedRoute` (`App.tsx`) : ajouter les routes V3 au **scope tenant**. Le super-admin ne doit **pas** pouvoir joindre `/tenant/organization/*` (garder la redirection V2 T-W2).
- i18n : **ajouter les clés dans les 5 langues** (faute commise en V2/F26). Les libellés de niveaux/rôles sont **dynamiques** (venus de l'API) → ne pas les figer en dur.

### 6.2 Écrans à créer / étendre

| Écran | Fichier | Nouveau / étendu | Contenu |
|---|---|---|---|
| **Éditeur de niveaux** (A) | `pages/admin/OrganizationLevelsPage.tsx` (nouveau) | N | Liste ordonnée glisser-déposer des niveaux de la dénomination ; création/renommage ; choix `semanticType` ; parenté. |
| **Navigateur d'organisation** (E) | `pages/OrganizationBrowserPage.tsx` | **étendu** | Drill-down région→zone→campus→église : pour chaque nœud = **nom du niveau**, **responsable**, **compteurs** (fidèles, églises, progression), clic → enfants. Remplace l'affichage brut `type` par `levelName`. |
| **Fiche nœud** | `pages/admin/OrganizationNodeDetailPage.tsx` (nouveau) | N | Onglets : Identité · Responsable & équipe (assignments) · Modules (features) · Thème · Agrégats/progression · Codes de rejointure. |
| **Rôles & intitulés** (B) | `pages/TenantAdminRolesPage.tsx` | **étendu** | Pour chaque rôle-capacité : éditer l'**intitulé par église/nœud** (matrice rôle × nœud). Séparer visuellement « permissions » (capacité) et « affichage » (intitulé). |
| **Assignations membre** (C) | `pages/admin/MemberRolesPage.tsx` (nouveau) ou panneau dans l'annuaire | N | Pour un membre : affecter un rôle-capacité à 0..n nœuds (« nommer X ancien → affili 3 campus »). |
| **Création église/campus** (D) | `TenantOrganizationPage.tsx` + modale `CreateNodeWizard` | **étendu** | Assistant en 3 étapes : 1) niveau & nom · 2) **sélection des modules** (cases, défaut indépendant) · 3) thème (reprendre / perso). |
| **Modules par nœud** (D) | `pages/TenantAdminModulesPage.tsx` | **étendu** | Scope selector : tenant **ou** nœud (via `organization_node_features`). |
| **Branding par nœud** (D) | `pages/TenantAdminBrandingPage.tsx` | **étendu** | Permet thème **racine** et thème **par nœud** (override), aperçu live. |
| **Console plateforme** | `PlatformAdminDashboard.tsx`, `PlatformModulesPage.tsx` | **étendu** | Colonne « niveaux personnalisés » en lecture seule (agrégats seulement, D7). |

### 6.3 Composants réutilisables à créer (`components/organization/`)

- `<LevelPicker root>` — sélection d'un niveau (A).
- `<OrgTreeNav>` — arbre drill-down avec compteurs (E).
- `<RoleTitleMatrix>` — matrice intitulés rôle×nœud (B).
- `<ModuleMultiSelect>` — choix de modules avec presets (D).
- `<ProgressionSparkline>` — mini-courbe de progression (E).

### 6.4 Clients d'API (`lib/api` / hooks)

Nouveaux hooks : `useOrgLevels()`, `useOrgTree()`, `useNodeAggregate(id)`, `useNodeFeatures(id)`, `useRoleTitles(roleId)`, `useMemberAssignments(userId)`. **Adapter** `useOrgNetwork()` pour transporter `levelId`/`levelName`.

---

## §7. MOBILE (Flutter) — parité web

### 7.1 Modèle de parité

Le mobile suit la **même** surface API. Priorité aux écrans **terrain** (pasteur/référent de zone), pas à l'admin lourd.

| Écran mobile | Fichier | État | Contenu |
|---|---|---|---|
| **Réseau / arbre** | `presentation/screens/network/network_screen.dart`, `organizations_screen.dart` | **étendu** | Drill-down par **niveau custom** + compteurs agrégés + responsable (E). |
| **Fiche campus** | (nouveau) `presentation/screens/network/node_detail_screen.dart` | N | Berger : pasteurs/anciens (assignments), fidèles, progression, sermons, prières. |
| **Rôles & intitulés** | `presentation/screens/tenant/roles_screen.dart` | **étendu** | Édition d'intitulé par nœud (B), lecture des assignments (C). |
| **Modules** | `presentation/screens/tenant/modules_screen.dart` | **étendu** | Activation par nœud si l'utilisateur est admin du nœud (D). |
| **Création campus** | `presentation/screens/tenant/organizations_screen.dart` | **étendu** | Assistant niveau + modules + thème (D). |

### 7.2 Data / sync (attention dette)

- Le mobile a **deux moteurs de sync** (`sync_service.dart` + `offline_sync_manager.dart`) — **dette connue**. Les nouvelles tables (`organization_levels`, `member_role_assignments`, `role_titles`, `node_aggregate_snapshots`) doivent être ajoutées **aux deux** schémas locaux **ou** consolidées sur un seul moteur. **Décision à prendre en T-M0** : ne pas écrire des données V3 dans un seul moteur (sinon conflits).
- Modèles Dart à créer dans `data/models/` : `organization_level.dart`, `member_role_assignment.dart`, `role_title.dart`, `node_aggregate.dart`.
- Les **agrégats** sont **en lecture seule** côté mobile (recalcul serveur) → pas d'écriture offline sur `node_aggregate_snapshots`.

---

## §8. FONCTIONS DE « PROPULSION » (valeur ajoutée, phase 2)

> À planifier **après** A–E. Chaque fonction est une tâche autonome, sans régression sur le socle.

| ID | Fonction | Pourquoi | Rattachement |
|---|---|---|---|
| **P1** | **Templates de dénomination** | Cloner une structure complète (niveaux + rôles + presets modules + thème) pour lancer une dénomination en 5 min. Un « preset ICC-like » vs « preset presbytérien ». | `organization_levels` + `ModuleDefinition` |
| **P2** | **Délégation d'admin par nœud** | « Chaque niveau a son admin » : un `member_role_assignment` de portée **nœud** confère l'admin **sur ce sous-arbre** (via `PermissionScope`/`MembershipScopeType` déjà là). | §5.2 + `AuthorizationService.scopeAllows(node)` |
| **P3** | **Communication en cascade** | Une annonce « région Afrique centrale » diffuse à toutes les églises sous le nœud. | `announcements`/`broadcast` + `OrganizationHierarchyService.getDescendants` |
| **P4** | **Carte & annuaire public du réseau** | Page publique « trouvez une église près de vous » par dénomination, alimentée par les nœuds géolocalisés (`country/city/timezone` de `OrganizationNode`). | `PublicChurchesController` (existant) |
| **P5** | **Analytics avancés & progression** | Cohortes de fidèles, taux de croissance par zone, projection (réutilise `growthProjection`), KPI par échelle. | `node_aggregate_snapshots` + `engagementAnalytics` |
| **P6** | **Pipeline de leaders & succession** | Identifier les assistants/diacres « en couveuse » vers un rôle supérieur ; plan de succession par campus. | `succession` + `mentoring` + `skillMatching` |
| **P7** | **White-label par dénomination** | Sous-domaine / domaine propre (`{dénomination}.discipolat.app`) avec thème & logo du tenant racine. | `TenantSettings` + config DNS/Render |
| **P8** | **Reporting PDF par échelle** | Export « rapport de zone », « rapport régional » avec agrégats + progression. | `reports`/`exports` (existants) |
| **P9** | **RGPD & audit hiérarchiques** | Le droit d'accès/effacement tient compte des affiliations multi-nœuds ; qui a vu quel agrégat (journalisation). | `gdpr` + `audit` |
| **P10** | **Import/CSV du réseau** | Importer 500 églises (région/zone/campus + responsables) d'un coup, avec mapping sur les niveaux custom. | `imports`/`dataMigration` |

---

## §9. DÉCOUPAGE DE TÂCHES — fullstack + mobile (exécutable par agents)

> Format par tâche : **Files** (chemins réels) · **Action** · **Dépendances** 🔗 · **DoD** (Definition of Done, critères testables). **Ordre en §10.**

### 9.0 Prérequis de consolidation (hérité V2 — à re-vérifier, possiblement déjà soldé)

- **T-B-fix-F17** · *Durcir les requêtes multi-memberships* 🔗 *aucune*
  Files : `TenantOwnershipService.java` (lignes `Optional` sur `(user,tenant,ACTIVE)`), `RoleManagementService.java:299`, `AiModuleService.java:58`.
  Action : remplacer tout `Optional`/`getSingleResult` sur `(userId, tenantId, status=ACTIVE)` par une **liste** + règle de résolution déterministe, car V3-C introduit légitimement **plusieurs lignes actives**.
  DoD : test de reproduction (2 memberships actifs même tenant) → **aucun 500** ; gate PG passe.

### 9.1 Backend — A : niveaux configurables

- **T-B10** · *Référentiel de niveaux* 🔗 *T-B-fix-F17*
  Files : migration `V224__organization_levels.sql` ; nouveau `tenants/domain/OrganizationLevel.java` + `OrganizationLevelRepository/Service` ; champ `levelId` dans `OrganizationNode.java` ; endpoints §5.1 dans `TenantOrganizationController.java` (ou nouveau `OrganizationLevelController.java`) ; **backfill** des niveaux par défaut.
  DoD : créer/renommer/réordonner des niveaux ; un nœud peut référencer un niveau ; repli sur `type` si `levelId` null ; `GET /tenant/organization/tree` renvoie `levelName` ; migration **testée sur PG réel** (T-Q3).

### 9.2 Backend — B : capacité vs intitulé

- **T-B11** · *Intitulés par scope* 🔗 *T-B10*
  Files : `V225__role_titles.sql` ; `tenants/domain/RoleTitle.java` + service ; `RoleManagementService` (exposer `getEffectiveLabel(roleId, nodeId)` = title node → tenant → global) ; endpoints §5.2 (titles).
  DoD : deux églises affichent des labels différents pour le même rôle ; **la permission ne change pas** (test : autorisation identique malgré label différent) ; index partiel unique « un défaut » respecté.

### 9.3 Backend — C : affiliation multi-nœuds

- **T-B12** · *Assignations membre×rôle×nœud* 🔗 *T-B-fix-F17, T-B11*
  Files : `V226__member_role_assignments.sql` ; `MemberRoleAssignment.java` + `MemberRoleAssignmentService` ; endpoints §5.2 (assignments) ; **brancher l'autorisation** : une permission est accordée si le membre porte un rôle la contenant sur le nœud **ou un ancêtre** (descendance via `path`).
  DoD : affecter un rôle à un membre sur 3 campus ; l'`AuthorizationService` accorde/refuse selon le périmètre du nœud ; fin d'assignation = `ENDED` (pas de purge) ; test multi-tenant (isolation stricte).

### 9.4 Backend — D : modules & branding par nœud

- **T-B13** · *Modules par nœud* 🔗 *T-B10*
  Files : `V227__organization_node_features.sql` ; `OrganizationNodeFeature.java` + service ; **résolution d'accès** dans `FeatureAccessService` (nœud → `DEFAULT` indépendant, sinon `INHERITED/OVERRIDDEN` via `ConfigurationResolver`) ; endpoints §5.3 features ; `createChildChurch`/`createNode` accepte une **liste de modules** à la création.
  DoD : cocher des modules en créant un campus ; le campus voisine **indépendant** par défaut ; agrégat « nœuds ayant module M » correct.
- **T-B14** · *Thème par nœud* 🔗 *T-B13*
  Files : `organization_nodes.theme_json` (V228) ; résolution thème (override local, sinon indépendant) ; endpoint §5.3 theme ; **invalider le cache** `invalidateResolvedConfig` à chaque mutation.
  DoD : thème différent par campus ; `GET /theme/{node}` renvoie le thème résolu ; pas de fuite de thème d'une dénomination à l'autre.

### 9.5 Backend — E : agrégats & drill-down

- **T-B15** · *Snapshot & agrégation* 🔗 *T-B10, T-B12, T-B13*
  Files : `node_aggregate_snapshots` (V228) ; `NodeAggregateService` qui **remonte** par sous-arbre (`getDescendants`) les compteurs : fidèles (via memberships/assignments), églises/campus (nœuds `semanticType`), leaders (assignments de direction), sermons (`sermon`), prières (`prayers`/`prayerJournal`) ; **recalcul** sur mutation (événements) + **job planifié** ; endpoints §5.4 tree/aggregate/children.
  DoD : `GET /tenant/organization/nodes/{region}/aggregate` = somme du sous-arbre ; la progression (série de snapshots) est cohérente ; **aucun PII nominatif** renvoyé au super-admin (D7).

### 9.6 Backend — propulsion (phase 2)

- **T-B16** · *Délégation d'admin par nœud (P2)* 🔗 *T-B12* — conférer l'admin sur un sous-arbre ; étendre `isTenantAdmin()` en `isNodeAdmin(nodeId)`.
- **T-B17** · *Templates de dénomination (P1)* 🔗 *T-B10,B11,B13* — cloner niveaux + rôles + presets + thème.
- **T-B18** · *Communication en cascade (P3)* 🔗 *T-B10* — cible = sous-arbre d'un nœud.
- **T-B19** · *Carte/annuaire public (P4)* 🔗 *T-B10* — étendre `PublicChurchesController`.
- **T-B20** · *Reporting / analytics / succession / RGPD hiérarchique / import réseau (P5–P10)* — **1 ticket par fonction**, dépendent de T-B15.

### 9.7 Web

- **T-W10** · *Éditeur de niveaux* 🔗 *T-B10* — `OrganizationLevelsPage` + `<LevelPicker>`.
- **T-W11** · *Drill-down agrégé* 🔗 *T-B15* — étendre `OrganizationBrowserPage`, `<OrgTreeNav>`, `<ProgressionSparkline>`.
- **T-W12** · *Rôles & intitulés* 🔗 *T-B11* — matrice `<RoleTitleMatrix>` dans `TenantAdminRolesPage`.
- **T-W13** · *Assignations membre* 🔗 *T-B12* — `MemberRolesPage` + panneau annuaire.
- **T-W14** · *Assistant création campus (modules+thème)* 🔗 *T-B13, T-B14* — `CreateNodeWizard`, scopes dans `TenantAdminModulesPage`/`TenantAdminBrandingPage`.
- **T-W15** · *Garde de routes/i18n* 🔗 *T-W10..14* — routes scope tenant, clés 5 langues, tests `workspaces.test.ts`/`Sidebar.test.tsx` étendus.

### 9.8 Mobile

- **T-M0** · *Décision sync* 🔗 *aucune* — trancher : les 5 nouvelles tables dans **les deux** moteurs **ou** consolidation sur un seul. **Bloquant** avant toute écriture offline.
- **T-M10** · *Arbre par niveau + agrégats* 🔗 *T-B10, T-B15* — `network_screen`/`organizations_screen`.
- **T-M11** · *Fiche campus* 🔗 *T-M10* — `node_detail_screen`.
- **T-M12** · *Rôles/intitulés/assignations* 🔗 *T-B11, T-B12* — `roles_screen`.
- **T-M13** · *Modules par nœud* 🔗 *T-B13* — `modules_screen` (admin nœud).

### 9.9 Tests, CI, docs

- **T-Q1** · Tests backend : isolation multi-tenant (404 sur fuite), capacité≠intitulé, agrégat sous-arbre, features indépendantes, garde-fou `Payloads.of`. 
- **T-Q2** · Tests web : `OrgTreeNav` rendu des `levelName`, matrice intitulés, `navForRole` **sans** fuite sur `/tenant/organization/*`.
- **T-Q3** · **Gate PG obligatoire** : appliquer `V224..V228` sur PostgreSQL réel (`FlywayMigrationChainPostgreSqlTest`, **avec Docker**) ; vérifier parité colonnes↔entités (leçon `811d72f6`).
- **T-Q4** · Mettre à jour `SPEC_ONBOARDING_FLOWS.md` et `docs/rapports/` ; **RAG** de `STATUS.md`/`PROGRESSION.md`.

---

## §10. ORDRE D'EXÉCUTION

```
T-B-fix-F17 ─┬─► T-B10 (niveaux) ─┬─► T-B11 (titres) ─► T-B12 (affiliations) ─┐
             │                    ├─► T-B13 (modules nœud) ─► T-B14 (thème) ───┤─► T-B15 (agrégats) ─► propulsion
             │                    └─► (backfill)                                │
             └─────────────────────────────────────────────────────────────────┘

Web     : T-W10 → T-W11 → T-W12 → T-W13 → T-W14 → T-W15
Mobile  : T-M0 (décision) → T-M10 → T-M11 → T-M12 → T-M13
Tests   : T-Q1/T-Q2 au fil de l'eau ; T-Q3 (PG) bloquant avant merge ; T-Q4 en fin
```

**Chapitrage recommandé** : **Lot 1** = consolidation + A (T-B-fix-F17, T-B10, T-W10, T-M10) ; **Lot 2** = B+C (T-B11, T-B12, T-W12, T-W13, T-M12) ; **Lot 3** = D (T-B13/14, T-W14, T-M13) ; **Lot 4** = E (T-B15, T-W11, T-M11) ; **Lot 5** = propulsion P1–P10.

---

## §11. GARDE-FOUS NON NÉGOCIABLES

1. **Migrations montantes** `V224+` ; **jamais** éditer `V≤223` (D14).
2. **Parité entités↔PG** : chaque colonne new = entité **+ migration + schéma de test**, validée par gate **Docker/PG** (T-Q3). Le H2 seul **masque** les dérives (leçon apprise).
3. **Autorisations** : **jamais** `hasAnyRole('TENANT_OWNER','TENANT_ADMIN')` (F10). Lire `tenant_memberships` / `member_role_assignments` avec périmètre de **nœud**.
4. **Capacité ≠ intitulé** : une permission ne dépend **jamais** d'un `label`/intitulé (V3-B).
5. **Indépendance par défaut (V3-D)** : activer un module sur la racine **n'active pas** les enfants ; héritage = **option explicite**, jamais silencieuse.
6. **Multi-memberships gérés** : V3-C **crée** plusieurs lignes actives → **T-B-fix-F17 est un prérequis dur**, sinon 500 en cascade (leçon F17 V2).
7. **Agrégats sans PII côté plateforme** (D7) : le super-admin voit des **nombres**, pas des noms.
8. **CrossTenant H4** : toute bascule de portée = `callForTenantSwitch` + restauration `finally`.
9. **CI `NoNullUnsafeMapLiteralTest`** : `Payloads.of(...)`, **jamais** remonter le plafond.
10. **Sync mobile** : **T-M0** tranche avant d'écrire les nouvelles tables (dette double moteur).
11. **i18n 5 langues** + **RGPD 3 consentements** inchangés (D11 V2).
12. **Aucune régression** : `mvn verify` vert, `tsc` + tests web verts, `flutter analyze`/`flutter test` verts, **0 test supprimé**.

---

## §12. RISQUES & POINTS DE VIGILANCE

| Risque | Impact | Mitigation |
|---|---|---|
| Dérive **entité↔schéma PG** invisible (déjà arrivée) | Crash prod / colonnes absentes | Gate PG Docker systématique + test de parité automatique |
| **Multi-memberships** non corrigés (F17) | 500 en cascade ownership/roles/IA | T-B-fix-F17 **avant** T-B12 |
| **Performance** agrégats sur 500+ nœuds | Drill-down lent | Snapshots pré-calculés + index `path` + pagination ; pas de recalcul synchrone à chaud |
| **Ambiguïté enum vs level** | Deux sources de vérité | `type` = logique, `level_id` = affichage ; documenter + tester le repli |
| **Confusion niveau × type sémantique** | Règles transverse cassées | Toujours mapper un level → un `semanticType` |
| Dette **double sync** mobile | Conflits de données offline | T-M0 : consolider ou écrire dans les deux |
| **Escalade de portée** (un admin de zone voit trop) | Fuite inter-niveaux | Résolution par `path` ancêtre + tests d'isolation |

---

## §13. MODÈLE DE RAPPORT AGENT (à remplir en fin de tâche)

```
## <taskId> — <titre>
- Statut : DONE | PARTIEL | BLOCKED
- Fichiers touchés : …
- Migrations ajoutées : V2xx (montantes uniquement)
- Tests ajoutés : … (backend / web / mobile)
- Gate PG exécuté : OUI/NON — résultat
- Prérequis respectés : F17 ? crossTenant ? Payloads.of ? i18n 5 langues ?
- Ce qui reste partiel / prochaine main : …
- Capture / URL de démo (si UI) : …
```

---

## §14. RÉCAP — ce qui change pour l'utilisateur final

- Un **fondateur** crée « La Vie de Christ », définit ses **niveaux** (Région → Zone → Campus), nomme ses **rôles** (Diacre / Ancien / Pasteur assistant) **par église**, **active les modules** qui plaisent **à chaque campus**, et **suit la progression** de chaque échelle en drill-down.
- Un **admin de zone** gère **sa** zone (délégation par nœud), sans voir les autres.
- Un **pasteur de campus** personnalise son église (thème, modules, équipe) **indépendamment**, tout en restant rattaché à la dénomination (transfert, annuaires, agrégats remontés).
- Le **super-admin** n'administre **que** la flotte (agrégats), jamais le contenu — invariance V2 préservée.
