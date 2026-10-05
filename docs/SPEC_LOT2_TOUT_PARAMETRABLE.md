# SPEC_LOT2_TOUT_PARAMETRABLE — Onglets groupés, retour, noms & boutons paramétrables, animations

> **STATUT** : **IMPLÉMENTÉ ET VÉRIFIÉ** sur `main` au commit `6389312a`.
> **VERSION** : v1.0 — rédigée et appliquée pendant la campagne du 05/10/2026.
> **DESTINATAIRES** : maintenance, Evolutions, QA.
> **HÉRITAGE** : prolonge `SPEC_ORGANISATION_MODULABLE_V3.md` (rôles paramétrables,
> niveaux custom, affiliation multi-nœuds — Lot 1) et
> `SPEC_ORGANISATION_DENOMINATION_V2.md` (socle organisation, **inchangé**).
> **AUCUNE migration `V ≤ 228` n'a été modifiée** (D14). Nouvelles migrations :
> `V229`, `V230`.

---

## §0. OBJET

Le besoin produit, formulé en cinq points :

1. les **rôles ne sont pas fixes** — une église appelle « responsable » ce qu'une
   autre appelle « dirigeant », et l'admin choisit le nom ;
2. le menu ne doit pas être une **longue liste d'onglets**, mais des **groupes
   d'onglets** qui, au clic, révèlent leurs sous-onglets ;
3. **flèche / bouton retour** disponible sur toute page ;
4. **tout est paramétrable** — chaque nom, chaque bouton, et le choix
   d'**ajouter ou retirer** des fonctionnalités sur tel écran ;
5. de **belles animations et transitions**, et **rien de codé en dur**.

Le point 1 est couvert par le **Lot 1** (`SPEC_ORGANISATION_MODULABLE_V3`
§B : `role_titles`, capacité ≠ intitulé). Le présent document décrit le
**Lot 2**, qui couvre les points 2 à 5.

---

## §1. PRINCIPES DIRECTEURS

Trois principes ont gouverné chaque décision. Ils sont la substance du travail,
plus que les fonctionnalités elles-mêmes.

### 1.1 « Un groupe est un regroupement, jamais un filtre »

La tentation naturelle d'un regroupement est de **cacher** ce qui n'est pas
affecté. C'est un piège : au premier réglage enregistré par un admin, toute
fonctionnalité non explicitement listée disparaît.

**Règle appliquée partout** (`frontend/src/navigation/grouping.ts`,
`NavigationGroupService`) : une entrée de menu sans groupe affecté **reste
visible**. Elle est rendue dans un groupe « résiduel » — donc en fin de liste,
comme avant le paramétrage. Supprimer un groupe ne retire **aucune**
fonctionnalité de l'application.

Conséquence vérifiable : `buildGroupedNav` expose `totalIn` / `totalOut` et
`totalIn === totalOut` est une assertion de test, pas une intention.

### 1.2 « Un seul point d'injection, sinon rien ne reste paramétrable »

Rendre « chaque nom paramétrable » ne peut pas consistanter à ajouter une
chaîne au code pour chaque besoin. Il faut que l'administration puisse
**renommer ce qui existe déjà**.

**Règle appliquée** : la surcharge de l'église est consultée dans `t()` et
`tText()`, **avant** le dictionnaire i18n. Ces deux fonctions sont appelées
partout dans l'application. Une seule modification rend donc **toute chaîne
déjà traduite** renommable, sans qu'une seule page soit touchée.

Le magasin est `module-level` (`src/config/uiCustomization.ts`) et non un
contexte React : `t()` est appelée hors composant, un contexte ne l'aurait pas
servie sans la rendre asynchrone.

### 1.3 « Dégradé sans régression »

Aucun réglage ne doit pouvoir rendre l'application inutilisable :

| Situation | Comportement |
|---|---|
| API des groupes ne répond pas | repli sur la navigation statique du rôle |
| Réponse de personnalisation incomplète | champs vides ignorés, dictionnaire inchangé |
| Migration non appliquée / API absente | magasin vide ⇒ comportement **strictement identique** à avant |
| Aucune cible de retour déductible | bouton de retour **masqué** (jamais inerte) |
| Étiquette `disabledWithoutDocker` (gate PG) | SKIP **comptabilisé**, jamais faux PASS |

---

## §2. §GR — GROUPES D'ONGLETS (le menu)

### 2.1 Modèle — `V229__navigation_groups.sql`

```sql
navigation_groups(
  id, tenant_id NULL,        -- NULL = groupe GLOBAL livré par défaut
  key, label, description, icon,
  parent_group_id,           -- hiérarchie : un groupe contient des sous-groupes
  display_order, roles[], module_key,
  enabled, collapsed_by_default, show_count,
  created_at, updated_at)

navigation_group_items(
  id, group_id, tenant_id, item_key, href, display_order, created_at,
  UNIQUE (group_id, href))
```

**Choix de conception à connaître avant de modifier :**

- **`tenant_id` NULLABLE.** `NULL` = réglage global ; renseigné = réglage d'une
  église qui **écrase** le global de même `key`. Même sémantique que
  `ConfigurationResolver` (`DEFAULT` / `INHERITED` / `OVERRIDDEN`).
  Un `UNIQUE (tenant_id, key)` seul admet plusieurs `NULL` en PostgreSQL :
  deux **index uniques partiels** le verrouillent (§4.1).
- **Résolution EXPLICITE**, pas de filtre Hibernate `tenantFilter` : c'est ce qui
  la rend testable et interdit toute fuite cross-tenant (H4).
- **`href` comme clé de jointure logique**, volontairement dénormalisé : la même
  table regroupe les entrées `menu_entries` (qui existent en base) **et** les
  ~175 entrées du menu statique frontend, qu'aucune clé étrangère ne peut
  référencer.

### 2.2 Résolution côté web — `frontend/src/navigation/grouping.ts`

Fichier **pur** (aucune dépendance React, aucun accès réseau), donc entièrement
prouvé par tests. Ordre d'affectation d'une entrée :

1. **affectation explicite** (`assignments[href]`) ;
2. sinon le groupe dont la **clé ou le libellé correspond à la section**
   d'origine (comparaison normalisée : casse et accents ignorés) ; en cas
   d'homonymie, le groupe **le plus spécifique** (le plus profond) gagne ;
3. sinon la section reste porteuse (groupe résiduel).

Cas limites traités et testés : affectation vers un groupe supprimé, cycles de
parentés, groupes au parent absent (deviennent racines), href dupliqué entre
sections, section partiellement réaffectée (fusion sans doublon), menu vide.

**Repli automatique** : sans aucun groupe configuré, un groupe est synthétisé
par section. L'amélioration est donc immédiate, sans configuration, et
réversible.

### 2.3 Rendu — `NavGroupSection.tsx`

Accordéon animé par `grid-template-rows: 0fr → 1fr` : hauteur réellement animée,
**aucune mesure JavaScript**. Replié = `visibility: hidden`, donc les
sous-onglets sortent du parcours de tabulation et de l'arbre d'accessibilité tout
en restant montés (pas de perte d'état).

Un groupe contenant la route active s'ouvre toujours — sinon l'utilisateur
perdrait son repère « je suis ici ».

---

## §3. §BK — RETOUR ARRIÈRE ET FIL D'ARIANE

### 3.1 Le constat

Le dépôt comptait **16 boutons « Retour »** copiés-collés, incohérents entre
eux (les uns `navigate(-1)`, les autres un parent codé en dur), et **zéro** fil
d'Ariane. Ajouter un 17e bouton aurait perpetué le défaut.

### 3.2 La solution

`frontend/src/navigation/back.ts` (pur) déduit le parent **depuis la table de
routes existante** (`routeAccess.ROUTE_ROLES`) : ajouter une route rend
automatiquement son parent et son fil d'Ariane. Aucun quatrième endroit à
penser à remplir.

Stratégie, du plus fiable au plus dépendant de l'historique :

| Ordre | Source | Remarque |
|---|---|---|
| 1 | cible explicite `state.from` | navigation par lien |
| 2 | **parent déduit** | `/souls/123` → `/souls` |
| 3 | historique du navigateur | `navigate(-1)` |
| 4 | **rien** → bouton masqué | un retour inerte est pire qu'aucun retour |

`MainLayout` monte `BackButton` + `Breadcrumbs` **une seule fois** : toutes les
pages en profitent, y compris à l'avenir.

Les 16 boutons existants sont **laissés en place** : les retirer aurait été une
régression de confort sur des écrans où l'emplacement du retour est choisi.

---

## §4. §LB — TOUT PARAMÉTRABLE (noms, boutons, fonctionnalités)

### 4.1 Modèle — `V230__ui_labels_and_page_features.sql`

```sql
ui_label_overrides(
  id, tenant_id NULL, node_id, label_key, locale,   -- '*' = toutes langues
  value, description, enabled, created_at, updated_at)

ui_page_features(
  id, tenant_id NULL, node_id, page_key, feature_key,
  label_override, enabled, display_order, module_key, description, …)
```

Index uniques **partiels** : `UNIQUE (tenant_id, label_key, locale)` admet
plusieurs `NULL` en PostgreSQL standard, ce qui laisserait passer deux réglages
globaux concurrents.

### 4.2 Règles de résolution

- hiérarchie **nœud → église → global** (le plus spécifique gagne) ;
- langue **exacte** avant « toutes langues » ; `fr` couvre `fr-FR` ;
- `enabled = false` → ignorée, **pas supprimée** (l'admin peut suspendre un
  renommage sans perdre son texte) ;
- clé absente ⇒ **aucune** entrée ⇒ le frontend retombe sur son dictionnaire ;
- fonctionnalité absente ⇒ **active**.

**Déterminisme.** La résolution est triée par spécificité décroissante puis
`putIfAbsent`. Une première version dépendait de l'ordre arbitraire de
restitution de la base : le nom affiché pouvait changer entre deux
rafraîchissements. Attrapé par les tests
(`tenantBeatsGlobalRegardlessOfOrder`, `resolutionIsOrderIndependent`).

### 4.3 Le défaut « ouvert » — arbitrage explicite

Une fonctionnalité **absente** reste **visible**. Un défaut « caché » ferait
disparaître l'application entière au premier réglage enregistré par un admin.
L'admin écrit donc « je **désactive** ce bouton » plutôt que « j'active 200
boutons » : c'est plus sûr, et plus intuitif.

### 4.4 Point d'injection frontend

```
t(clé)      → surcharge → dict[locale] → dict[fr] → clé
tText(fr)   → surcharge (libellé source ET clé déduite) → traduction → source
```

`tText()` est aussi instrumenté : les centaines de chaînes écrites en dur dans
les pages passent par lui. Sans ce point, la moitié de l'application serait
restée figée.

`FeatureGate` / `useFeatureEnabled` / `useFeatureLabel` couvrent
« ajouter ou retirer une fonctionnalité sur tel écran ».

### 4.5 Écran d'administration — `/tenant/customization`

Trois sections : groupes d'onglets (créer / renommer / ordonner / imbriquer /
plier), libellés (par langue), fonctionnalités (par écran). Le bouton de
suppression y est lui-même placé derrière une `FeatureGate` : l'écran est
paramétrable par sa propre mécanique.

---

## §5. §AN — ANIMATIONS ET TRANSITIONS

`src/lib/motion.ts` centralise 3 durées, 4 courbes et `usePrefersReducedMotion`.
Le dépôt avait 25 animations et **10+ styles** de barres d'onglets, tous écrits
à la main dans les pages.

`GroupedTabs.tsx` fournit les « groupes d'onglets » **dans une page**
(14 onglets → 4 groupes), ARIA correct, navigation clavier. C'est un point
d'entrée **opt-in** : les 34 barres d'onglets existantes fonctionnent inchangées.

### 5.1 Deux corrections de bugs d'animation

| CSS | Constat | Correction |
|---|---|---|
| `.page-enter` / `.page-enter-active` | déclarées, **jamais référencées** : la transition de page ne se jouait pas et la navigation semblait figée | réellement utilisées ; `MainLayout` pose la clé sur `location.pathname` pour rejouer l'animation |
| `.scrollbar-hide` | utilisée par 5 écrans, **jamais définie** en dehors d'une media query mobile → ascenseur visible | définie globalement |

**Piège évité** : l'état de départ (`opacity: 0`) est désormais porté par les
keyframes de l'animation et non plus par le CSS. Avec `prefers-reduced-motion`,
l'ancien code pouvait laisser du contenu **invisible** — le pire endroit pour ça.

---

## §6. MOBILE (parité)

- **Drawer** : groupes repliables, même endpoint et même résolution que le web
  (`GET /tenant/navigation/groups`) — web et mobile ne peuvent donc pas afficher
  deux menus différents pour la même personne. Un `AnimatedSize` anime la
  hauteur réelle. Repli : `NavigationShape.empty` ⇒ affichage d'origine.
- **Retour** : `DetailBackButton`. Sur mobile, l'`AppBar` n'affiche un retour que
  si `canPop()` ; or l'application navigue surtout avec `context.go(...)`, qui
  *remplace* la pile — d'où des boutons **absents** sur les fiches de détail. Le
  widget applique la même stratégie que le web (`pop` → parent déduit → racine)
  **sans changer la sémantique** de `go`/`push`.
- `NavigationGroup` / `NavigationShape` + 8 tests Dart.

---

## §7. API

`GET /tenant/navigation/groups` · `GET|PATCH|DELETE /tenant/navigation/groups/{id}` ·
`PUT /tenant/navigation/groups/{id}/items` — `isAuthenticated` en lecture
(filtrée rôles + église), `isTenantAdmin()` en écriture.

`GET /tenant/customization` · `GET /tenant/customization/admin` ·
`POST /tenant/customization/labels|features` · `DELETE …/{id}` — mêmes gardes.

Garde d'accès : `@authz.isTenantAdmin()` / `isTenantOwner()`, qui lisent
`tenant_memberships` — **jamais** le claim du JWT (F10). Un réglage d'une autre
église répond **404 et non 403** : on ne confirme pas son existence. Les
réglages **globaux** sont en lecture seule depuis une église.

---

## §8. VÉRIFICATION EXÉCUTÉE

| Gate | Résultat |
|---|---|
| `mvn test` (services + contrôleurs) | **1165 / 1165** |
| `FlywayMigrationChainPostgreSqlTest` (**PostgreSQL 16 réel**, Docker) | **11 / 11** — chaîne V1→V230 + parité entités↔schéma |
| `tsc --noEmit` | 0 erreur |
| `vitest run` | **663 / 663** (83 fichiers) |
| `flutter analyze` (7 fichiers touchés) | 0 issue |
| `flutter test` (navigation + 4 suites liées) | **29 / 29** |

Un défaut a été trouvé **par** le gate de parité : l'entité
`NavigationGroupItem` mappait `created_at` sans que `V229` ne crée la colonne —
un 500 systématique en production sur toute insertion d'affectation. Corrigé à
la source (migration neuve, jamais appliquée).

**Correctif de build hors périmètre, mais nécessaire** : le projet compile en
Java 21 ; sur un JDK ≥ 25, Byte Buddy refuse d'instrumenter les classes
concrètes et **tout mock de classe** échoue — y compris sur des tests déjà
commités (`SpaceServiceTest`). `net.bytebuddy.experimental=true` a été ajouté à
surefire (mécanisme officiel d'opt-in de Byte Buddy, sans effet sur un JDK ≤ 24).

---

## §9. LIMITES ASSUMÉES

1. **Migrations des écrans existants vers `GroupedTabs` non faite.** Le
   composant est fourni et testé ; convertir les 34 barres d'onglets est un
   chantier par écran, hors périmètre de ce lot.
2. **`FeatureGate` n'est appliqué qu'à l'écran de personnalisation** pour l'instant.
   Le mécanisme est là ; le poser sur les ~100 écrans qui ont des boutons est un
   travail par lots.
3. **Le refi `MainLayout` (`lg:ml-64` en dur)** ne réagit toujours pas à l'état
   replié de la sidebar — Bug préexistant, non corrigé ici (le rail 80 px laisse
   un espace de 176 px).
4. **Deux nav hrefs sans route** (`/exports`, `/import`) : dérive préexistante
   entre `workspaces.ts` et `App.tsx`, non corrigée.
5. La suite Flutter complète (108 fichiers) n'a pas été exécutée dans cet
   environnement (saturation) ; seules les suites couvrant le périmètre ont été
   lancées.

---

## §10. SUITE RECOMMANDÉE

| ID | Objet |
|---|---|
| P1 | Affectation d'onglets à un groupe depuis l'écran d'admin (l'API est prête, l'UI manque) |
| P2 | `GroupedTabs` sur `PasteurDashboardPage` (14 onglets) et `DepartmentManagementPage` |
| P3 | `FeatureGate` sur les écrans à boutons (export, import, suppression) |
| P4 | Délégation d'admin par nœud (P2 de la V3) branchée sur `member_role_assignments` |
| P5 | Fixer `MainLayout` : marge latérale pilotée par l'état replié de la sidebar |