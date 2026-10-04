# SPEC_ORGANISATION_DENOMINATION_V2 — Modèle d'organisation, flux d'entrée, console plateforme et transfert

> **STATUT** : spécification **VALIDÉE** par le produit le 04/10/2026. Prête à exécuter.
> **DESTINATAIRES** : agents d'implémentation **backend (Java/Spring Boot)**, **web (React/TS)**, **mobile (Flutter)**.
> **RÈGLE D'OR** : aucun agent ne code avant d'avoir lu les §0 à §4. Les §5 à §10 sont des ordres d'exécution, pas des suggestions.
> **HÉRITAGE** : prolonge `docs/SPEC_ONBOARDING_FLOWS.md` (spéc initiale). **Ne pas relire celle-ci pour le modèle d'organisation** : elle décrit un tenant = une église, ce qui est désormais faux. Voir §1.2.

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

Il est déjà connu de la plateforme. Il lui faut : **un transfert** + **le code de sa nouvelle église**. C'est écrit noir sur blanc dans le code (§4.4, §7.1 T-B5) et testé (§7.4 T-Q1). Détecteur : `root_tenant_id` identique.
---

## §2. AUDIT — L'ÉTAT RÉEL AU 04/10/2026 (à lire avant de coder)

Le commit `7390651` a livré un socle **réel et fonctionnel**. **Il ne faut pas le refaire.** L'audit a établi ceci, source par source :

| Besoin | État | Preuve |
|---|---|---|
| 2 CTA sur le landing | FAIT | `frontend/src/components/landing/Hero.tsx:73,77` |
| Création église = `TENANT_OWNER` direct | FAIT | `backend/.../tenants/domain/SelfServiceChurchService.java` |
| Codes `PREFIXE-XXXX` non ambigus | FAIT | `JoinCodeService.java` (alphabet sans `I L O U 0 1`) |
| Code saisi une seule fois | FAIT | `TenantMembership` + `ActiveTenantService.resolveTokenTenantId` |
| Annonces publiques + modération | FAIT | `PublicAnnouncementsController`, V220 |
| Gouvernance (blocage/ban/litiges) | FAIT | `PlatformGovernancePage.tsx`, V221 |
| Délégation / abdication propriétaire | FAIT | `TenantOwnershipService` (transfer/promote/demote) |
| Console plateforme isolée | **PARTIELLE** | failles F1, F2 |
| Modèle dénomination | **ABSENT** | faille F5 |
| Transfert de membre | **ABSENT** | faille F6 |

### Les six failles à corriger

**F1 — La fuite de menus a une sortie de secours.**
`frontend/src/workspaces.ts:742-763` — `navForRole(activeRole)` retourne `FULL_NAV` en `default`. `Sidebar.tsx:127` et `CommandPalette.tsx:65` basculent bien sur `PLATFORM_NAV`, mais la garantie « jamais de menus d'église » n'est **pas structurelle** : elle est *par composant*. Tout nouveau composant appelant `navForRole()` réintroduit la fuite.
→ **Correctif (T-W1)** : changer la signature en `navForRole(user)` pour rendre l'obtention de `FULL_NAV` impossible à un platform admin. On corrige la cause, pas les symptômes.

**F2 — Les routes d'église ne sont pas gardées.**
`frontend/src/App.tsx:285-324` (`ProtectedRoute`) redirige `/` vers `/platform/dashboard`, mais `/dashboard`, `/souls`, `/crm/faiseur` restent accessibles en dur. Un Super Admin qui saisit l'URL voit un dashboard de chef de famille — exactement le reproche initial.
→ **Correctif (T-W2)** : `ProtectedRoute` refuse toute route tenant quand `user.platformSuperAdmin === true`.

**F3 — `window.prompt` dans la console de production.**
`frontend/src/pages/PlatformGovernancePage.tsx:54,67,78,89` — motif de bannissement, message d'avertissement, objet de litige et résolution sont saisis via des `window.prompt` du navigateur. Bannir une église via une boîte de dialogue système est inacceptable en prod **et non testable**.
→ **Correctif (T-W3)** : modales in-app, champs contrôlés, validation.

**F4 — Paramètre d'URL divergent entre spec et code.**
`Hero.tsx:73` navigue vers `/register?mode=church` ; `SPEC_ONBOARDING_FLOWS.md` §A documente `?intent=create-church` ; `RegisterPage.tsx:60` lit `mode`. Fonctionnellement correct, mais **le document est faux** : un agent qui le suivra cassera le CTA.
→ **Correctif (T-W4 + T-Q3)** : aligner la spec sur le code réel (`mode=church`) et le documenter comme contrat figé.

**F5 — Le tenant n'a ni nature, ni parent, ni racine.**
`backend/.../tenants/domain/Tenant.java` : `id, name, slug, status, plan, country, currency, timezone, locale, brandingJson, featuresJson, settingsJson, trialEndsAt, onboardingCompletedAt…`. **Aucun** `kind`, `parentTenantId`, `rootTenantId`. Impossible de modéliser « une dénomination qui contient des églises ».
→ **Correctif (T-B1)** : migration `V222__tenant_organization_model.sql` (§5).

**F6 — Aucun transfert de membre.**
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
- Le code est copiable en **1 clic** et partageable vers WhatsApp (le canal réel de ces utilisateurs).
- **Aucun** email de vérification : le mot de passe est saisi dans le formulaire, le compte est actif. Une friction supprimée.
- URL `?mode=church` = **contrat figé** (corrige F4). Ne pas renommer.

### 4.2 FLOW B — La règle d'or : le code ne se saisit qu'une fois

```
   1re FOIS (adhésion)              CHAQUE FOIS APRÈS
────────────────────────           ───────────────────────
Code  ou  Lien /j/slug                Se connecter
        │                                  │
        ▼                                  ▼
   POST /tenant/join                Résolution automatique
        │                             de l'organisation
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
---

## §7. RÉPARTITION DES TÂCHES

> **Ordre d'exécution imposé** : `§7.1 Backend` → `§7.2 Web` → `§7.3 Mobile` → `§7.4 Tests & CI`.
> Les dépendances sont notées 🔗. **Ne pas commencer une tâche dont la dépendance n'est pas verte.**

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

- [ ] Liste enrichie avec `childCount` / `memberCount`
- [ ] `GET /platform/tenants/{id}` inclut la descendance
- [ ] Test : `PLATFORM_SUPER_ADMIN` → 200 avec `kind` ; `MEMBRE` → 403 ; **aucun email dans le corps**

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

#### T-B4 · Code de rejointure par sous-église 🔗 *T-B1*

**Fichiers** : `JoinCodeService.java`, `TenantJoinCodeController.java`, `TenantJoinCode.java`

`org_node_id` existe déjà en base (V219) mais n'est pas exposé par l'API. L'exposer dans `POST /tenant/join-codes` (requête) et dans `GET /tenant/join-codes` (réponse). Le `JoinLookup` public retourne déjà `orgNodeLabel` (ligne 53 de `JoinCodeService`).

- [ ] `orgNodeId` accepté + retourné
- [ ] Un code par nœud, `code` unique (index partiel V219)
- [ ] Test : 2 sous-églises → 2 codes distincts, memberships scopées

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
---

### 7.2 WEB (React / TypeScript) — corriger la fuite et exposer le transfert

#### T-W1 · `navForRole(user)` : rendre la fuite IMPOSSIBLE 🔗 *T-B2 (le flag existe déjà)*

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
- [ ] Test `Sidebar.test.tsx` (existant) : platform admin → aucun menu d'église — **le conserver**

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

#### T-W3 · Modales de gouvernance (supprimer `window.prompt`) 🔗 *aucune*

**Fichier** : `frontend/src/pages/PlatformGovernancePage.tsx`

Corrige **F3**. Les 4 `window.prompt` (lignes 54, 67, 78, 89 : motif, message, objet, résolution) deviennent des **modales in-app** à champs contrôlés, en suivant les patterns UI déjà présents dans le projet. Motif de bannissement **obligatoire** (non vide) — c'est la trace d'audit.

- [ ] Zéro `window.prompt` dans la page
- [ ] Motif de bannissement bloquant si vide
- [ ] Test : soumission avec motif vide → action refusée

#### T-W4 · Landing : cohérence du CTA « créer une église » 🔗 *aucune*

**Fichier** : `frontend/src/components/landing/Hero.tsx` (lignes 73, 77)

Corrige **F4**. Le Hero émet `?mode=church` ; `RegisterPage.tsx:60` lit `mode`. **C'est le contrat figé** — ne pas renommer le paramètre. Vérifier que les deux CTA pointent vers `/register?mode=church` et `/join`, et que le document §4.1 fait autorité.

- [ ] `?mode=church` confirmé (contrat figé)
- [ ] CTA « Rejoindre une église » → `/join`
- [ ] Test : le Hero navigue vers `mode=church`

#### T-W5 · Page réseau d'une dénomination 🔗 *T-B3, T-B4*
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

#### T-Q1 · Tests backend à ajouter

| Test cible | Vérifie | Faille couverte |
|---|---|---|
| `TenantKindTest` | `kind` par défaut `CHURCH`, racine `rootTenantId == id` | F5 |
| `TenantOrganizationServiceTest` | hiérarchie, propagation racine, anti-boucle | F5 |
| `TenantTransferServiceTest` | TRANSFERT / adhésion / idempotence | F6 |
| `TenantTransferControllerTest` | `preview` + `transfer` | F6 |
| `PlatformTenantControllerTest` | liste enrichie, agrégats sans PII | D7 |
| `PublicAnnouncementsControllerTest` | pas de `access_ref` public | D8 |

⚠️ **Contrainte Mockito/JDK** : l'audit a montré que **7 tests** de `AuthControllerRegistrationStatusTest` échouent sous **JDK 26** (le mock-maker inline ne supporte pas officiellement les JDK > 24). **Lancer la suite backend sous JDK 21**, comme le fait la CI. Ce n'est **pas** une régression du projet — ne pas « corriger » ces tests pour masquer une erreur d'environnement.

- [ ] 6 classes de test ajoutées, vertes sous JDK 21
- [ ] `mvn -q verify` vert, **0 test supprimé**

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
| `GET` | `/public/announcements` | Annonces publiées (**sans `access_ref`**, D8) |
   │
   ├─ Rédige une annonce : titre, visuel, ville, date, description
   ├─ VOITURE de modération obligatoire (D13)
   └─ PUBLICATION → visible sur le LANDING
         │
         └─ carte : titre · église · ville · date
              [ Je m'inscris ]  →  /join?slug=<slug>
```

**Point de sécurité arbitré (D8)** : le **code n'est JAMAIS affiché** sur la carte publique. On publie un **lien `/j/<slug>`**. Un code exposé publiquement = une église ouverte à tous, sans contrôle — inacceptable. Le lien mène à l'inscription, où le mode `OPEN`/`APPROVAL` du code **règne toujours**. Le champ `access_ref` de `V220` existe en base mais **n'est pas** renvoyé par `GET /public/announcements`.
        ▼                             (membership ACTIVE)
  MEMBRE RATTACHÉ                            │
        │                                   ▼
        └──────────────────────────► Dashboard. Zéro code.
```

**Ce qui garantit la promesse** : l'adhésion est en base (`TenantMembership` ACTIVE). Au login suivant, `ActiveTenantService.resolveTokenTenantId` la résout **sans intervention**. Le code est alors **inutile** — et c'est correct : il a rempli son rôle.
Un membre qui change d'église doit aujourd'hui se réinscrire (nouveau compte, nouveau mot de passe). C'est faux, coûteux, et contraire au besoin utilisateur central : il appartient déjà à la dénomination.
→ **Correctif (T-B5)** : `TenantTransferService` + endpoint (§6).