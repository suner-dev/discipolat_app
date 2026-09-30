# ARCHITECTURE FRONTEND CIBLE — proposition

> **Statut** : proposition, à valider avant implémentation. Ce document est écrit le 2026-09-29
> contre l'état réel du dépôt (`fix/onboarding-tenant-clients`, HEAD `282a464e`).
> **Il accompagne** `docs/AUDIT_ARCHITECTURE_FRONTEND.md` (constats) — ici on ne décrit que la cible.
> **Hypothèses de dimensionnement** (si l'une est fausse, la conclusion change : voir §16) :
> 1-2 développeurs, ~240 écrans, SPA derrière authentification, pas de SEO critique, feature flags
> par plan SaaS à venir, 6 langues dont RTL.

---

## 0. LA PROPOSITION EN UNE PHRASE

> **Des tranches verticales (une fonctionnalité = un dossier qui possède tout : son contrat API,
> son état, son UI, ses tests, ses traductions), des dépendances entre couches rendues
> **illisibles par le linter**, un contrat API **généré** depuis l'OpenAPI, et des routes comme
> **unique source de vérité** — le tout introduit par vagues de 1 à 2 semaines, sans jamais arrêter
> la production de fonctionnalités et sans jamais réécrire une page qui marche.**

**Pourquoi cette forme et pas une autre** : c'est la seule qui règle *les cinq* causes racines
mesurées (pages géantes, `any`, clés de cache, zéro frontière, zéro test de contrat) sans introduire
un niveau d'indirection que 1-2 personnes ne peuvent pas maintenir. Les 4 autres architectures
discutées en §15 sont rejetées **avec raison**, pas par principe.

---

## 1. LES 7 DÉCISIONS STRUCTURANTES (chacune répond à une cause mesurée)

| # | Décision | Cause racine qu'elle traite | Coût de non-décision |
|---|---|---|---|
| **D1** | **Tranches verticales** : une feature possède son API, son modèle, son UI, ses tests | 240 pages à plat, 32 pages > 500 lignes | « si je réécris le module Finance, qu'est-ce qui casse ? » → sans réponse |
| **D2** | **Contrat-first** : types **générés** depuis l'OpenAPI + validation zod à la frontière | 259 `any`, 484 assertions `as`, 0 validation runtime | Chaque dérive d'API se corrige à la main, comme cela arrive depuis 3 campagnes |
| **D3** | **Taxonomie d'état à 4 casiers** : serveur / URL / formulaire / client-global | 462 `useQuery` + 14 `useState` dans un contexte + fetch manuel dans 5 contextes | Bugs de cache et de resynchronisation impossibles à raisonner |
| **D4** | **Frontières de couches rendues illisibles** (ESLint `no-restricted-imports`, `no-cycle`) | 0 règle d'import, alias `@/` libre | Dérive silencieuse : le design system finit par importer une feature |
| **D5** | **Le routeur est la source de vérité** (nav et rôles dérivés de la table de routes) | 242 routes dans `App.tsx` vs 304 `href` en dur dans `workspaces.ts` | Un menu peut pointer une route inexistante : rien ne le détecte |
| **D6** | **Erreurs = valeurs + observabilité** (`errorElement` par route + Sentry) | 1 ErrorBoundary global, 0 journalisation | 1 exception = écran blanc global, cause inconnue, 0 régulation |
| **D7** | **i18n par clés + ICU + chargement paresseux** | `tText` par valeur + scan de 2 571 entrées + 784 Ko au boot | Traductions qui cassent en silence, 1,2 Mo inutiles |

---

## 2. L'ARBRE CIBLE — et ce que devient chaque dossier existant

```
frontend/src/
│
├── app/                          ← NOUVEAU. Bootstrap, providers, router, politiques globales
│   ├── router.tsx                    createBrowserRouter ; errorElement PAR ROUTE (D6)
│   ├── routes.tsx                    LA table de routes = source de vérité unique (D5)
│   ├── providers.tsx                 I18n > Auth > Tenant > Impersonation > Settings > Platform > Query
│   ├── query-client.ts               défauts + PLUGIN de préfixe tenant (D3)
│   └── bootstrap.ts                  ce qui doit s'exécuter avant le 1er render (branding, SW, flags)
│
├── shared/                        ← LE SEUL code réellement partagé. Dépend de RIEN en dehors.
│   ├── ui/                          DESIGN SYSTEM UNIQUE (fusion : voir §8)
│   │   ├── Button/ Card/ Modal/ Table/ EmptyState/ Skeleton/ Toast/ Field/ Tabs/ Stepper/
│   │   └── index.ts                  point d'entrée unique (interdit l'import par fichier)
│   ├── lib/                         http client, formatters Intl, guards, result type
│   ├── hooks/                        useDebounce, useMediaQuery, useKeyboardNav, useEventStream
│   ├── config/                      feature flags, constantes, schéma de config serveur
│   └── i18n/                         1 fichier par locale, ICU, lazy (D7)
│
├── generated/                     ← GÉNÉRÉ, jamais édité (D2)
│   └── api.ts                        ← orval / openapi-typescript depuis /openapi.json
│
├── entities/                      ← UN DOSSIER PAR CONCEPT MÉTIER. Dépend de shared/ seul.
│   ├── soul/                         api.ts · model.ts (schemas zod + queryKey factory)
│   ├── family/  ├── department/  ├── event/  ├── member/  ├── tenant/  ├── invitation/ …
│   └── <concept>/
│
├── features/                      ← UNE TRANCHE VERTICALE PAR ACTION MÉTIER
│   ├── soul-detail/
│   │   ├── api.ts                     3 à 6 endpoints, rien d'autre
│   │   ├── model.ts                   types zod, clés de cache, règles métier pures
│   │   ├── ui/                        composants de la slice (ex. SoulTimeline.tsx)
│   │   ├── SoulDetailPage.tsx         COMPOSITION, < 120 lignes
│   │   └── __tests__/                 Vitest (msw) + *.spec.ts (Playwright)
│   ├── soul-create/  soul-edit/  invitations-manage/  subscription-manage/  onboarding-wizard/ …
│   └── <feature>/
│
├── pages/                         ← RÉTROCOMPATIBILITÉ. Composition minimale, 0 fetch, 0 type.
│   └── <route>.tsx                   wrapper : <ProtectedRoute><FeaturePage/></ProtectedRoute>
│
├── hooks/                         ← sera vidé migrant vers shared/hooks ou features/*/
├── components/                    ← sera vidé : admin/ departments/ landing/ pasteur/ users/
│                                   → migrés dans entities/ et features/ ; shared/ et ui/ → shared/ui
├── contexts/                      ← réduit à 3 : Auth, Tenant, Impersonation (D3)
├── lib/                           → migré dans shared/lib ; api.ts devient shared/lib/http.ts
├── types/index.ts                 → découpé : types métier dans entities/, API dans generated/
├── workspaces.ts                  → devient un DERIVÉ de app/routes.tsx (D5), plus une liste
└── i18n/                          → shared/i18n, lazy
```

**Le principe de migration** : *aucun* de ces déplacements n'est obligatoire pour livrer une
fonctionnalité. On crée la nouvelle structure **parallèlement**, et on migre un écran à la fois.
Rien n'est supprimé avant d'avoir son remplaçant testé.

---

## 3. LE CŒUR : LES RÈGLES DE DÉPENDANCE (c'est ici que se gagne l'architecture)

D5 règles, **lisibles par le linter**. Une règle non exécutée n'est pas une règle.

```
   app  ──►  features  ──►  entities  ──►  shared
   │           │              │            │
   └───────────┴──────────────┴────────────┘
   (aucun retour vers la gauche, aucun raccourci entre sommets non adjacents)
```

| # | Règle | Interdit | Raison |
|---|---|---|---|
| R1 | `shared/` n'importe **que** `shared/` | `shared/ui` qui importe une feature | Un design system qui connaît la feature ne peut plus être extrait ni partagé |
| R2 | `entities/*` n'importe **que** `shared/` + son dossier + `generated/` | `entities/soul` qui importe `entities/family` | Les concepts métier ne se connaissent pas ; la coordination passe par `features/` |
| R3 | `features/*` n'importe **que** `shared/`, `entities/*`, `generated/` | `features/x` qui importe `features/y` | Une feature est autonome : on peut la supprimer ou la déplacer |
| R4 | `pages/` ne fait **que** composer : ni `api`, ni `useQuery`, ni type inline | `pages/**` qui importe `@/lib/api` | Les pages deviennent des fichiers de 30 lignes, donc sûrs |
| R5 | **Aucun cycle** dans tout le graphe | imports croisés | Un cycle rend le chargement paresseux non déterministe |

**Configuration ESLint à poser tel quel** (c'est la Vague V0 de l'audit, ~2 jours) :

```js
// frontend/eslint.config.js — ajout
const LAYERS = ['app', 'shared', 'generated', 'entities', 'features', 'pages', 'contexts'];

// interdit : shared -> (hors shared) tout le reste
{ files: ['src/shared/**/*.{ts,tsx}'],
  rules: { 'no-restricted-imports': ['error', {
    patterns: [
      { group: ['@/app/*', '@/entities/*', '@/features/*', '@/pages/*', '@/contexts/*', '@/generated/*'],
        message: 'shared/ ne doit importer que shared/. (règle R1)' },
    ] }] } },

// interdit : entities -> features|pages|app
{ files: ['src/entities/**/*.{ts,tsx}'],
  rules: { 'no-restricted-imports': ['error', {
    patterns: [{ group: ['@/features/*', '@/pages/*', '@/app/*'],
      message: 'Un concept métier ne connaît pas une feature ni une page. (règle R2)' }] }] },

// interdit : features -> features|pages
{ files: ['src/features/**/*.{ts,tsx}'],
  rules: { 'no-restricted-imports': ['error', {
    patterns: [{ group: ['@/features/*', '@/pages/*'],
      message: 'Une feature est autonome. (règle R3)' }] }] },

// interdit : pages -> api|useQuery (on ne peut pas lister useQuery, mais on interdit l'API)
{ files: ['src/pages/**/*.{ts,tsx}'],
  rules: { 'no-restricted-imports': ['error', {
    patterns: [{ group: ['@/lib/api', '@/lib/apiRaw', '@/shared/lib/http', '@/generated/*'],
      message: 'Une page ne parle pas à l API. (règle R4)' }] }] },
```

```js
// plus, pour tout src/**
'import/no-cycle': 'error',
'import/no-self-import': 'error',
'no-restricted-syntax': ['error', {
  selector: "TSTypeAliasDeclaration > TSAnyKeyword",   // interdit `type X = any`
  message: 'Utilise unknown + un schéma zod, pas any.' }],
'@typescript-eslint/no-explicit-any': 'warn',           // -> 0 warning requis
'@typescript-eslint/consistent-type-imports': 'error',
'jsdoc/require-jsdoc': ['warn', { publicOnly: true }],  // sur l'API publique d'une feature
```

> **Pourquoi `--max-warnings 1000` doit tomber à 0** : tant que le seuil est laxiste, aucune de ces
> règles n'est réellement appliquée. C'est le même changement qu'un `set -e` dans un script.

---

## 4. ANATOMIE D'UNE TRANCHE — l'exemple réel (`soul-detail`, aujourd'hui 1 244 lignes)

```ts
// ─── features/soul-detail/api.ts ────────────────────────────────────────────
import { http } from '@/shared/lib/http';
import { SoulSchema, SoulDetailSchema } from './model';
import { soulKeys } from '@/entities/soul/model';

export const soulDetailApi = {
  get:  (id: string) => http.get(`/souls/${id}`).parse(SoulDetailSchema),          // zod → type garanti
  listNotes:    (id: string) => http.get(`/souls/${id}/notes`).parse(z.array(SoulNoteSchema)),
  listHistory:   (id: string) => http.get(`/souls/${id}/history`).parse(z.array(HistorySchema)),
  createNote:    (id: string, body: NewNote) => http.post(`/souls/${id}/notes`, body).parse(SoulNoteSchema),
  discipline:    (id: string) => http.get(`/souls/${id}/discipline`).parse(DisciplinePageSchema),
};
```

```ts
// ─── features/soul-detail/model.ts ──────────────────────────────────────────
import { z } from 'zod';
import { soulKeys } from '@/entities/soul/model';

// 1) Les DONNÉES sont validées au_runtime : la réponse est soit conforme, soit une erreur explicite.
export const SoulDetailSchema = z.object({ id: z.string().uuid(), nom: z.string().min(1), ... });

// 2) Les CLÉS de cache sontcentralisées et préfixées par tenant (D3) — jamais littérales.
export const soulDetailKeys = {
  all:      () => [...soulKeys.all(), 'detail'] as const,
  detail:   (id: string) => [...soulKeys.all(), 'detail', id] as const,
  history:  (id: string) => [...soulKeys.all(), 'detail', id, 'history'] as const,
  notes:    (id: string) => [...soulKeys.all(), 'detail', id, 'notes'] as const,
};

// 3) Les RÈGLES MÉTIER pures, testables sans React ni réseau.
export const isEditable = (soul: Soul, ctx: { role: UserRole; tenantId: string }) => ...;
```

```tsx
// ─── features/soul-detail/SoulDetailPage.tsx — 40 lignes ───────────────────
export function SoulDetailPage() {
  const { id = '' } = useParams();
  const q = useSoulDetail(id);            // hook local, pas dans le composant
  const tab = useTabParam('tab');         // état d'UI dans l'URL → partageable, pas de state local

  if (q.isLoading)  return <DetailSkeleton />;
  if (q.isError)    return <ErrorState error={q.error} onRetry={q.refetch} />;   // jamais un toast muet
  if (!q.data)      return <EmptyState title={t('soul.detail.empty')} action={…} />;

  return (
    <Layout title={q.data.nom} badge={<OnboardingStepper …/>}>
      <TabNav value={tab} onChange={setTab} />       {/* clavier + aria-current gérés par le DS */}
      {tab === 'history' && <SoulHistory id={id} />} {/* chaque onglet charge à la demande */}
      {tab === 'notes'   && <SoulNotes   id={id} />}
    </Layout>
  );
}
```

```ts
// ─── features/soul-detail/useSoulDetail.ts ─────────────────────────────────
export const useSoulDetail = (id: string) =>
  useQuery({ queryKey: soulDetailKeys.detail(id), queryFn: () => soulDetailApi.get(id), enabled: !!id });
```

**Résultat mesurable** : le fichier de 1 244 lignes devient 5 fichiers dont **le plus long fait
~150 lignes**, chaque partie est testable isolément, et l'écran se charge sans recharger 1 000 lignes
de JS pour afficher un onglet.

---

## 5. D3 — LA TAXONOMIE D'ÉTAT (la règle qui clarifie 90 % des cas litigieux)

| Ce que l'on stocke | Où | Exemples |
|---|---|---|
| **État serveur** (la vérité vient du backend) | `useQuery` + clé de cache, **jamais** de copie locale | âmes, familles, départements, quotas, invitation, abonnement |
| **État d'URL** (l'utilisateur doit pouvoir partager/back) | `useSearchParams` / segment de route | onglet actif d'un détail, page courante d'un tableau, tri, filtres, recherche |
| **État de formulaire** | `react-hook-form` + zod | création d'un Registre, édition, filtres avancés |
| **État client global** (vraiment global) | Context, **maximum 3** : `Auth`, `Tenant`, `Impersonation` | identité, tenant courant, usurpation en cours |
| **État éphémère local** | `useState` dans le composant, une seule fois | champ non encore soumis, l'ouverture/fermeture d'un panneau |

**Les 4 fautes interdites, nommées :**
1. `useState` qui copie une donnée serveur (→ `useQuery`) — *cause n°1 des affichages périmés*.
2. `useEffect` + `api.get` dans un composant (→ `useQuery`) — *cause n°1 des requêtes en double*.
3. Fetch manuel dans un Context (→ `useQuery` dans un provider) — *cause n°1 des caches non invalidables*.
4. Clé de cache écrite en littéral (→ fabrique) — *cause n°1 des invalidations forgotten*.

---

## 6. D2 — LA COUCHE API, en 4 briques

1. **Génération** : `openapi.json` (le backend l'expose déjà, A16) →
   ```bash
   npx orval --input http://localhost:8080/v3/api-docs --output src/generated/api.ts --mode tags-split
   ```
   → `generated/api.ts` (lecture seule, en-tête `// AUTO-GENERATED — ne pas éditer`).
2. **Client unique** : `shared/lib/http.ts` — un seul point d'interception (401/refresh, trace,
   abort, `baseURL`). **Fusionne `lib/api.ts` et `lib/apiRaw.ts`, qui sont deux clients
   concurrents** (`/api/v1` vs `/api`).
3. **Schémas zod à la frontière** : chaque réponse est parsée. En dev, un échec de schéma =
   toast + rapport Sentry avec le diff ; en prod, on affiche une erreur propre. **C'est le seul
   moyen de transformer « dérive de contrat » en « non-régression visible ».**
4. **Fabrique de clés + préfixe tenant automatique** :
   ```ts
   // app/query-client.ts
   const tenantKey = () => ['t', getActiveTenantId() ?? 'none'] as const;
   // plugin: les clés sont réécrites ['t', tenantId, ...clé]
   ```
   → une bascule de tenant invalide **mécaniquement** tout le cache, sans `window.location.reload()`.
   On supprime alors le pansement (audit §3.3) **et** on gagne en fluidité.

---

## 7. D6 — ERREURS, OBSERVABILITÉ, ET « ERREUR = VALEUR »

- **Un `errorElement` par layout et par feature** : `/souls/:id` qui plante affiche l'erreur dans le
  cadre du layout, pas un écran plein écran. `react-router` 7 le fait nativement.
- **Zéro `try/catch` qui avale** : le motif unique est
  `if (error) return <ErrorState error={error} onRetry={…} />`.
- **Sentry** (ou équivalent) avec `release`, `environment`, et tags
  `{ tenant, role, route, feature }`. Le `build-info` du CI alimente `release`.
- **`Result<T, E>`** dans `shared/lib` pour les cas où une erreur est **une valeur attendue**
  (validation métier, quota atteint) et non une exception. Cela évite les `toast` muets.

---

## 8. D5 + §8 — DESIGN SYSTEM ET NAVIGATION

**Design system** : une seule source. Aujourd'hui il y en a **deux** avec des contrats divergents
(`EmptyState` : prop `message` dans `shared/` — 44 usages — contre prop `description` dans `ui/`).
Décision : `shared/ui/` gagne, `components/shared/{EmptyState,ConfirmDialog,SkeletonLoader,Toast}.tsx`
sont dépréciés, un `codemod` de 44 appels est écrit, et un test de non-régression vérifie qu'aucun
`EmptyState` ne reçoit les deux props.

**Tokens d'abord** : `index.css` a déjà `glass-card`, `btn-primary`, `bg-gradient-mesh`,
`shadow-glow`. L'étape suivante n'est pas « plus de composants », c'est **un fichier de tokens**
(espacement, rayons, ombres, z-index, typographie) dont les composants ne sont que l'application —
c'est ce qui permet themes clair/sombre et RTL sans dupliquer.

**Navigation** : `app/routes.tsx` décrit chaque route avec
```ts
{ path: '/souls/:id', element: <SoulDetailPage/>, nav: { section: 'people', roles: ['ADMIN','PASTEUR'], labelKey: 'nav.souls' } }
```
et `navForRole(role)` **devient une sélection** sur cette table. Les 5 tableaux dupliqués
(`workspaces.ts`, 304 `href` en dur) disparaissent. Un test vérifie que **toute route marquée
`nav` est atteignable et que toute entrée de menu pointe une route existante** — plus jamais de
menu cassé.

---

## 9. D7 — i18N : clés stables, ICU, lazy

```ts
// shared/i18n/index.tsx — un seul point d'entrée
const loaders = {
  fr: () => import('./locales/fr.json'),
  en: () => import('./locales/en.json'),
  ar: () => import('./locales/ar.json'),   // …
};
// fr.json embarqué (le cas nominal), les 5 autres en chunk lazy → -784 Ko au boot
// les messages utilisent ICU : {count, plural, one {#participant} other {#participants}}
```

Clés = **noms de domaine** (`soul.detail.history.empty`), jamais du texte français.
Extraction automatique (`@formatjs/cli extract`) + contrôle CI « 0 clé manquante, 0 clé orpheline ».
C'est ce qui supprime définitivement le mécanisme `tText` (par valeur) et sa boucle linéaire.

---

## 10. TESTS — Pyramide par risque, pas par pourcentage de couverture

| Couche | Quoi | Outil | Cible réaliste |
|---|---|---|---|
| Unitaire | règles métier pures, schémas zod, formatatters | Vitest | ~400 tests, < 2 s |
| Composant | une slice, 5 états, avec **MSW** (le vrai contrat) | Vitest + MSW | les 12 features critiques |
| Intégration API | la frontière : réponse backend → schéma → UI | Vitest + MSW + vrai payload OpenAPI | 100 % des endpoints générés |
| E2E | 5 parcours, Playwright | Playwright | signup → onboarding → 1er événement ; connexion+2FA ; invitation ; changement de plan ; bascule de tenant |
| a11y | automatique, dans le pipeline | `axe-core` | 0 violation bloquante sur les parcours |
| Non-régression | contrat de navigation, clés i18n, frontières | scripts CI | 3 checks |

**Ce qu'on fait des 53 fichiers de tests existants** : rien n'est supprimé. Ils sont **recopiés dans
la feature correspondante** quand la feature est migrée (donc toujours verts), puis les doublons
(`EmptyState` 2 versions) sont dépréciés.

---

## 11. PERFORMANCE — budgets chiffrés (et non « ça va »)

| Métrique | Aujourd'hui | Cible | Comment on tient |
|---|---|---|---|
| Chunk i18n au boot | 784 Ko | 0 Ko (fr embarqué, autres lazy) | import dynamique par locale |
| Recharts au boot | 426 Ko | 0 Ko (dans la page qui l'utilise) | ne plus être dans un chunk préchargé |
| Requests par écran | non mesuré | 1 par écran, 0 par onglet | clé par onglet + `enabled` |
| Cache inter-tenant | pansement `reload()` | isolation par clé | plugin de préfixe |
| Budget CI | aucun | **échec si le bundle initial > 300 Ko gzip** ou +5 % vs PR précédente | étape CI de comparaison |
| Listes longues | rendu naïf | virtualisation au-delà de 200 lignes | `@tanstack/react-virtual` (ou équivalent) |

---

## 12. AUTHENTIFICATION : la trajectoire en 3 étapes (le correctif est côté backend)

1. **Court terme (1 j, front)** : supprimer la double authentification.today `withCredentials: true`
   **et** l'en-tête `Authorization` (les deux sont actifs : `lib/api.ts:11` et `:21-24`). On garde
   l'en-tête seul, plus lisible à auditer.
2. **Court terme (1 j, front)** : sortir `buildUserFromAuthResponse(d: any)` (259e `any` n°1) par
   un schéma zod sur la réponse de `/auth/me`.
3. **Moyen terme (backend, 1-2 semaines)** : `httpOnly` + `SameSite=Strict` + `CSRF`, refresh token
   en base. Le front ne lit plus rien dans `localStorage`. **À faire avec le backend, jamais
   seul** : déplacer le secret côté client sans changer le modèle de menace, c'est déplacer le
   problème.

---

## 13. LE PLAN DE MIGRATION — 4 phases, 1 slice par PR, jamais d'arrêt de production

**Règle de conduite, non négociable :** chaque PR = 1 slice migrée,tests au vert, zéro
comportement modifié. Une PR de migration **n'apporte aucune fonctionnalité** ; une PR de
fonctionnalité **n'est pas attendue d'être conforme** mais doit passer dans une tranche existante.
Les deux flux cohabitent.

| Phase | Durée | Contenu | Critère de sortie mesurable |
|---|---|---|---|
| **P0 — Outiller** | 2-3 j | ESLint : 5 règles de frontière, `max-warnings 0`, `no-explicit-any` à 0, `no-cycle` ; rapport de cycles publié | `npm run lint` en 0 ; **tout nouveau** fichier est conforme par construction |
| **P1 — Socle** | 2-3 sem. | `shared/lib/http` (2 clients fusionnés) + types OpenAPI générés + zod à la frontière + fabrique de clés + préfixe tenant + i18n lazy/ICU + `shared/ui` unifié + `errorElement` + Sentry + budget CI | 0 `any` sur les modules touchés ; 1 071 appels inchangés **et** prouvés par un test de contrat ; −1,2 Mo au boot |
| **P2 — Slice de référence** | 1 sem. | `entities/soul` + `features/soul-detail` + `features/soul-create` migrées entièrement, avec MSW + E2E Playwright | 1 244 lignes → 5 fichiers ; parcours Playwright vert ; sert de **modèle copié** ensuite |
| **P3 — Migration par domaine** | ~3 mois | 5-8 pages par semaine, par domaine ; nav dérivée de `routes.tsx` ; suppression des doublons au fur et à mesure | compteur `pages hors cible` → 0 ; `grep 'queryKey: \['` hors `model.ts` → 0 |

**Ordre des domaines recommandé** (du plus rentable au plus risqué) :
`invitations` → `abonnement/quotas` → `onboarding` → `départements` → `familles` → `âmes` →
`finance` → `tableaux de bord` → `ia/realtime`. On commence par ce qui a le plus de tests déjà
(le wizard : 22 tests) et le moins de risque.

**Le piège à éviter, nommé** : la migration échoue si on la fait « quand on aura le temps ». Elle ne
marche que si chaque PR de migration est **petit** (≤ 500 lignes) et **immédiatement mergé**.

---

## 14. CE QUE CETTE ARCHITECTURE RÉPARE CONCRÈTEMENT

| Constat mesuré (audit §3) | Qui le répare | Vérifiable par |
|---|---|---|
| 240 pages à plat, 0 frontière | P0 | `eslint` refuse un import interdit |
| 32 pages > 500 lignes | P2, P3 | `find src/features -name '*.tsx' -exec wc -l` : max < 200 |
| 259 `any`, 484 `as` | P1 | `grep -c ': any' src` → 0 (hors `generated/`) |
| 501 clés de cache littérales, pas de tenant | P1 | `grep 'queryKey: \[' src --include=*.tsx \| grep -v model.ts` → 0 |
| Cache tenu par un `location.reload()` | P1 | suppression de la ligne + test de non-régression |
| 5 contextes qui fetchent à la main | P1, P2 | `grep 'api.get' src/contexts` → 0 |
| 784 Ko + 426 Ko au boot | P1 | `dist/index.html` : 1 seul modulepreload vendor |
| 2 design systems concurrents | P1 | un seul `EmptyState`, contrat unique |
| 1 ErrorBoundary global | P1 | `grep 'ErrorBoundary' src` → 1 implémentation, N `errorElement` |
| 242 routes vs 304 href en dur | P3 | test « toute entrée de menu pointe une route » |
| 14 % de pages testées | P1, P2, P3 | 5 parcours E2E en CI + contract tests MSW |
| 0 observabilité | P1 | une erreur réelle dans Sentry, avec `release` |
| lint qui ne peut pas échouer | P0 | `--max-warnings 0` |

---

## 15. LES ARCHITECTURES QUE JE REFUSE — ET POURQUOI (contre-expertise)

| Refusée | Raison précise pour **ce** dépôt |
|---|---|
| **Micro-frontends** (Module Federation) | 1-2 développeurs : on créerait un problème de versions partagées et de déploiement pour résoudre un problème d'organisation de fichiers. Le gain (déploiement indépendant par équipe) n'existe pas sans équipe. |
| **Monorepo Nx / Bazel** | Utile à partir de ~10 devs et plusieurs apps. Ici, la version légère (5 règles ESLint) apporte 90 % du bénéfice pour 5 % du coût. Nx ici = ceremony sans benefit. |
| **Redux / Zustand** | Le serveur est déjà bien géré (TanStack Query). Ajouter un store global ne corrige aucun des 12 constats ; cela en ajoute un (source de vérité concurrente). |
| **Next.js / SSR pour l'application** | L'app est authentifiée, sans SEO ni TTFB critique. SSR de 240 pages protégées coûte plus qu'il ne rapporte. Le SSR **serait** justifié pour la vitrine publique (`LandingPage`, `PricingPage`) si l'acquisition devient une priorité — décision à part. |
| **Réécriture (« on recommence proprement »)** | 108 000 lignes, 1 071 appels API à re-certifier, 0 valeur fonctionnelle pendant des mois, régressions non testables. C'est le choix le plus coûteux et le moins défendable de la liste. |
| **Design system « maison » giant** | Il en existe déjà 2 moitiés. Le problème n'est pas le volume, c'est la **duplication**. Fusionner avant d'agrandir. |
| **Migration Big Bang en 6 semaines** | Impossible à mi-parcourir : les deux moitiés ne peuvent pas cohabiter (dès qu'une page change de version de contrat). Le strangler est obligatoire ici. |

---

## 16. LE COÛT, HONNÊTEMENT

| Phase | Charge | Ce qu'il faut savoir |
|---|---|---|
| P0 | 2-3 j | Rentabilité immédiate, risque nul. **À faire même si le reste n'est jamais fait.** |
| P1 | 2-3 sem. | C'est le socle. C'est aussi là que se décide le vrai sujet : les cookies `httpOnly` exigent le **backend**. |
| P2 | 1 sem. | Le moment de vérité : si la slice de référence demande plus d'une semaine, c'est que le design est trop ambitieux → le couper. |
| P3 | ~3 mois (à 1-2 j/semaine) | **C'est le vrai chiffre honnête** : à raison de 5-8 pages par semaine hors charge fonctionnelle. Full-time : ~8-10 semaines. |
| **Total** | **~4-5 mois à temps partiel, ~2,5 mois à temps plein** | Plus le temps de fonctionnalité, qui ne s'arrête pas. |

**Le risque principal n'est pas technique, c'est l'arrêt en cours de route.** Une migration
architecturale échoue presque toujours par lassitude, pas par obstacle. La parade proposée : P0 et
P1 livrent de la valeur **immédiate** (lint honnête, bundle allégé, erreurs visibles) donc aucune
suspicion ne s'installe.

**Ce sur quoi je ne peux pas me prononcer sans vous :** (a) le nombre réel de développeurs
(1 ou 2 change le budget de 3 à 5 mois) ; (b) si le produit doit sortir une release dans les
6 semaines — alors P0 seul est jouable, P1 partiellement, et P3 devient un sujet 2027 ; (c) si la
vitrine publique a un objectif SEO, ce qui rend le SSR pertinent pour 2 pages (et change le
périmètre, pas la cible).

---

## 17. LA DÉCISION QUE JE RECOMMANDE AUJOURD'HUI

**Valider P0 + P1 (5 semaines), et ne décider P2/P3 qu'après P1**, quand on aura des chiffres réels
(bundle, erreurs capturées, temps de build) et non des estimations. Une architecture se décide avec
des données de production, pas sur un diagramme.

Trois décisions à prendre maintenant, dans l'ordre :

1. **`shared/ui` gagne** contre `components/shared/` (fin de la duplication) — 1 jour.
2. **Supprimer `withCredentials`** (double authentification) — 1 jour.
3. **Activer le lint honnête** (`--max-warnings 0` + les 5 règles) — 2 jours, et c'est ce qui
   rend tout le reste automatique.

Chacune est petite, réversible, et mesurable. C'est exactement le bon point de départ.

---

*Aucun chiffre de ce document n'est inventé : les mesures viennent de `docs/AUDIT_ARCHITECTURE_FRONTEND.md`
annexe A, exécutées sur `fix/onboarding-tenant-clients` @ `282a464e` le 2026-09-29.*
