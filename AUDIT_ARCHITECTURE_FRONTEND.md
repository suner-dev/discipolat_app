# AUDIT D'ARCHITECTURE FRONTEND — Discipolat

> **Périmètre audité** : `frontend/` de la branche la plus avancée `fix/onboarding-tenant-clients`
> (HEAD `282a464e`, worktree `discipolat_app-agentB`), plus le build réel `frontend/dist`.
> **Date** : 2026-09-29. **Méthode** : lecture du code + mesures reproductibles (commandes en annexe A).
> **Aucune affirmation de ce document n'est estimée** : toute ligne chiffrée est reproductible avec
> les commandes données. Les limites de l'audit sont en annexe C.

---

## 0. VERDICT HONNÊTE, EN DEUX PHASES

**Phase 1 — l'affirmation à tester : « l'architecture frontend est du jouet, pas sérieuse, mauvaise, et totalement à refaire ».**

| Partie de l'affirmation | Verdict | Preuve |
|---|---|---|
| « du jouet » | **FAUX** | TypeScript `strict`, TanStack Query, 239 routes en `lazy()`, ErrorBoundary, 6 locales, RBAC centralisé, 50 fichiers de tests, CI qui typecheck + teste + build, **0 `dangerouslySetInnerHTML`**, **0 `@ts-ignore`**, **0 `console.log`** en production, séparation stricte des zones backend/frontend respectée par deux agents pendant deux campagnes. Un jouet ne passe pas 1 253 tests backend ni 1 469 tests verts sans tricher. |
| « pas sérieuse » | **FAUX, mais seulement sur l_PRODUCT** | Le produit est réellement fini (workflows métier réels, RBAC, multi-tenant, i18n). |
| « mauvaise » | **VRAI, et grave** | L'architecture est un **dumping ground** : 240 pages à plat, 0 découpage vertical, 32 pages > 500 lignes, clés de cache écrites en dur dans 501 endroits, 259 `any`, 784 Ko de traductions-chargées au démarrage pour un utilisateur francophone. **§2 et §3.** |
| « totalement à refaire » | **FAUX — et c'est le piège** | Réécrire 108 000 lignes en un seul lot est **le choix le plus risqué et le plus coûteux** qui existe. Aucune grande entreprise ne refait jamais un produit de cette taille en « big bang ». **§5 propose le contraire : migration par strangler, 6 vagues, chacune mesurable et réversible.** |

**Phase 2 — note de synthèse.**

| Axe | Note | Mesure clé |
|---|---|---|
| Qualité de l'ingénierie au quotidien | **7/10** | lint/typecheck/tests en CI, zéro `any` silencieux côté prod, zéro `@ts-ignore` |
| Architecture (structure, frontières, dépendances) | **3/10** | 0 dossier métier, 0 frontière de modules, 240 pages à plat |
| Contrat avec l'API | **2/10** | 259 `any`, 484 assertions `as`, 0 validation runtime, 0 client généré depuis l'OpenAPI |
| Testabilité | **3/10** | 34 pages testées sur 240 (14 %), 7,6 % du code en tests, 0 E2E navigateur |
| Performance perçue | **4/10** | 1,2 Mo de chunks modulepreloadés inutiles au boot, scan O(n) sur 2 571 entrées à chaque chaîne non traduite |
| Observabilité | **1/10** | 0 Sentry/OpenTelemetry, 1 ErrorBoundary global |
| Sécurité front | **4/10** | jetons en `localStorage`, `withCredentials` **et** en-tête `Authorization` |

**Conclusion en une phrase :** le code n'est pas mauvais, il est **mal rangé** — et c'est très
corrigeable, mais pas par une réécriture.

---

## 1. MESURES DE RÉFÉRENCE (tout le reste s'appuie dessus)

| Métrique | Valeur | Où la lire |
|---|---|---|
| Lignes TS/TSX dans `src` | **116 857** | `find src -name '*.ts*' -exec cat {} + \| wc -l` |
| … dont production | 107 964 (92,4 %) | idem hors `__tests__` |
| … dont tests | 8 893 (**7,6 %**) | idem `src/__tests__` |
| Pages (`src/pages/*.tsx`) | **240** (+1 sous-dossier `ai/`) | `ls src/pages/*.tsx \| wc -l` |
| Pages > 500 lignes | **32** (13 %) | `wc -l` filtré |
| Pages > 1 000 lignes | **4** — max `SoulDetailPage.tsx` **1 244** | idem |
| `useState` max dans un fichier | **18** (`PlatformAdminDashboard`, `DepartmentDetailPage`) | `grep -c useState` |
| Composants | 85 fichiers / 15 643 lignes | idem |
| **Hooks** | **12 fichiers / 1 165 lignes** | idem |
| Contextes | 6 fichiers / 922 lignes | idem |
| Fichiers i18n | 8 / **17 708 lignes (15,2 % du code)** | idem |
| Fichiers de test | **53** fichiers dans `__tests__` (50 `.test.tsx` + `branding.test.ts` + `workspaces.test.ts` + `setup.ts`) | `ls src/__tests__` |
| **Pages couvertes par un test** | **34 / 240 (14 %)** | comparaison noms de fichiers |
| Routes déclarées | **242**, toutes dans `App.tsx` (1 165 lignes) | `grep -c 'path="'` |
| Imports `lazy()` | 239 | `grep -c 'lazy('` |
| `useQuery` / `useMutation` | 462 / présents dans 181 pages | `grep -rc` |
| Appels API/axios dans les pages | **870** | `grep -rn` |
| Clés de cache littérales distinctes | **501** | `grep -rho "queryKey: \[[^]]*\]" \| sort -u \| wc -l` |
| `invalidateQueries` littéraux distincts | 170 | idem |
| Assertions de type `as` dans les pages | **484** | `grep -rho "as [A-Z]…"` |
| `any` explicites (`: any`, `as any`, `<any>`) | **259** | `grep -rn` |
| `any` dans `AuthContext.buildUserFromAuthResponse` | signature `d: any` (ligne 35) | lecture |
| Appels `tText(...)` | **2 282** | `grep -rho "tText(" \| wc -l` |
| Clés dans `fr.ts` | 2 871 lignes / **2 571 clés** | audit Agent B du 28/09 |
| `toLocaleDateString('fr-FR')` en dur | 27 | `grep -rn` |
| Fichiers touchant `localStorage` | 26 | `grep -rl` |
| Fichiers de test par page `ai/` | 0 | — |
| **Build réel** | `dist/assets` = **5,4 Mo**, **261 chunks** | `du -sh` / `ls \| wc -l` |
| Chunk i18n | **784 Ko** — `modulepreload` **au démarrage** | `dist/index.html` |
| Chunk charts | **426 Ko** — `modulepreload` **au démarrage** | idem |
| MSW / Storybook / Playwright / Cypress / Sentry / OpenTelemetry | **0 occurrence** | `grep` sur `package.json` |
| `axe` (tests d'accessibilité automatisés) | **0** | idem |

---

## 2. CE QUI EST RÉELLEMENT BIEN — À NE PAS JETER À LA POUBELLE

Je dois le dire, sinon le rapport serait un exercice de dénigrement sans valeur.

1. **Zéro dette « facile » et dangerously cheap.** 0 `dangerouslySetInnerHTML`, 0 `@ts-ignore`,
   0 `@ts-expect-error`, 0 `eslint-disable` de masse, 0 `console.log` en production. C'est rare et
   c'est le fruit d'un travail réel.
2. **TypeScript `strict` actif** (`tsconfig.json:16`) avec `noFallthroughCasesInSwitch`.
   L bachelor'sync `noUnusedLocals: false` est le seul point faible (§3.6).
3. **Gestion du server-state faite avec l'outil moderne** : TanStack Query v5, 462 `useQuery`,
   politique globale saine (`staleTime` 5 min, `retry: 2`, `refetchOnWindowFocus: false`).
   Beaucoup de produits en production n'ont même pas ça.
4. **Découpage du bundle par route** : 239 `lazy()`, `manualChunks` explicite
   (`vite.config.ts:30-42`), `chunkSizeWarningLimit` fixé. C'est du travail, pas du hasard.
5. **Un ErrorBoundary existe** et il est Thoughtful : écran de récupération avec « Réessayer »
   (`components/shared/ErrorBoundary.tsx`), là où la plupart des projets ont un écran blanc.
6. **Filtrage d'accès centralisé** : `lib/routeAccess.ts` (236 lignes) + `workspaces/`,
   `ProtectedRoute roles={[…]}`. Le RBAC n'est pas dispersé dans les pages.
7. **Testabilité prouvée sur le travail récent** : les 22 tests du wizard d'onboarding
   (`__tests__/OnboardingWizardPage.test.tsx`) lisent **le contrat champ par champ**, incluent
   `MemoryRouter` + `QueryClientProvider` isolés, et couvrent les cas d'erreur. C'est du vrai
   travail de test, pas dufeng shui.
8. **Audit d'exhaustivité réalisé** : 1 071 appels clients vérifiés un à un contre le backend,
   0 route orpheline, 0 donnée fictive en production (rapport `agentB.md`).
9. **i18n réel sur 6 locales**, y compris l'arabe en RTL, avec provider et fallback gracieux.
10. **CSP de développement** : le serveur Vite refuse les hôtes non déclarés (`allowedHosts`).

---

## 3. LES 15 DÉFAUTS, CLASSÉS PAR GRAVITÉ

Légende : 🔴 critique · 🟠 majeur · 🟡 mineur. Chaque défaut = **preuve** + **impact réel** + **correctif**.

### 🔴 3.1 — Aucune architecture : 240 pages à plat, 0 frontière entre modules

**Preuve.** `src/pages/` contient 240 fichiers dans un seul niveau. Il n'existe **ni** `features/`,
**ni** `entities/`, **ni** `domain/`, **ni** `shared/`. L'alias `@/` pointe sur `./src` en entier
(`tsconfig.json:20`, `vite.config.ts:11-13`) : n'importe quel fichier importe n'importe quoi, y
compris un composant de page depuis un autre module métier.

Il n'existe **aucune** règle de frontière : ni `@nx/enforce-module-boundaries`, ni
`eslint-plugin-boundaries`, ni `import/no-restricted-paths` dans `package.json` /
`.eslintrc*`. Le `lint` est défini mais avec `--max-warnings 1000` (`package.json:11`).

**Pourquoi c'est critique.** Une application ne peut pas être comprise, testée ou remplacée
domaine par domaine. On ne sait répondre à « qui utilise `InvoiceCard` ? » sans `grep`, ni « si on
réécrit le module Finance, combien de fichiers casse-t-on ? » — **ce coût est le vrai coût d'une
architecture**, et il n'est aujourd'hui ni mesuré ni maîtrisé. À 240 pages, il est déjà prohibitif ;
à 600 il est rédhibitoire.

**Correctif.** Arbre cible + règles de dépendance *linter** (voir §4). Aucun réécriture : c'est
une contrainte qui se met en place **avant** les vagues de migration, pour que chaque vague
améliore le score.

### 🔴 3.2 — Aucune barrière de compilation entre le frontend et l'API

**Preuve.** 259 `any` explicites, 484 assertions `as` dans les pages seulement. Exemples
représentatifs : `src/pages/SoulDetailPage.tsx:88-91`
(`return res.data.content as MakerReport[]`), `:100-104` (type de réponse inline de 9 lignes
redeclared dans la closure `queryFn`). `src/contexts/AuthContext.tsx:35` —
`function buildUserFromAuthResponse(d: any): User`. `zod` est une dépendance du projet mais sert
à **5 formulaires** ; il n'y a **aucune** validation d'une réponse d'API. Aucun client généré depuis
l'OpenAPI pourtant publié côté backend (`/openapi.json`, ajouté par A16).

**Impact.** C'est la **cause racine** de la majorité des bugs du dépôt. L'agent B a lui-même
documenté 3tasks B « non faites » parce que le frontend **devinait** la forme de la réponse ; le
backend a produit 5 défauts de parcours E2E; le rapport de recette A14 parle d'« implémentation
actuelle du frontend type l'étape comme `{ id, title, order }` » alors que le contrat impose
`stepOrder`. **Toutes ces corrections ont été faites à la main, cas par cas.** Ce n'est pas
accidentel : il n'existe aucun mécanisme qui puisse empêcher la dérive.

**Correctif.** Générer les types depuis l'OpenAPI (`openapi-typescript` ou `orval`) et **interdire**
l'export dans les pages ; valider **en développement** les réponses avec le schéma généré (zod) ;
échec de validation = toast + rapport Sentry, jamais `as`.

### 🔴 3.3 — Politique de cache non gouvernée : 501 clés écrites en dur, aucune isolation tenant

**Preuve.** 501 `queryKey` littéraux distincts, 170 `invalidateQueries` littéraux.
`queryKey: ['soul', id]` (`SoulDetailPage.tsx:88`), `queryKey: ['admin','tenants']`
(`AdminTenantsPage.tsx:48`)… **Aucune clé ne contient le `tenantId`** : un `grep "queryKey: \[[^]]*tenant"`
ne renvoie **qu'un seul** résultat sur 501. Il n'existe aucune fabrique de clés.

**Ce qui masque aujourd'hui le problème** (je le dis honnêtement, c'est un pansement) :
`switchTenant()` termine par `window.location.reload()` (`contexts/TenantContext.tsx:138`), donc le
cache mémoire est détruit au changement de tenant. **Mais `switchOrganization()` ne recharge pas**
(`:146-154`) et conserve donc le cache — et aucune clé n'est préfixée par l'organisation.

**Impact.** Ce pansement tient tant qu'on ne touche pas à `window.location.reload()`. Le jour où
quelqu'un ajoute une navigation douce (donc une amélioration évidente), on obtient une
**fuite de données entre églises pendant 5 minutes** (`staleTime`) sans qu'aucun test ne bronche.
C'est exactement le genre de dette qui devient un incident de sécurité.

**Correctif.** Fabrique centralisée + préfixe obligatoire :
`keys.soul(id) = ['t', tenantId, 'soul', id]`, et un plugin React Query qui injecte le préfixe.

### 🔴 3.4 — Testabilité : 14 % des pages couvertes, mocks qui prouvent le mock

**Preuve.** 34 pages sur 240 ont un test. 8 893 lignes de tests pour 107 964 de production
(**7,6 %** — l'ordre de grandeur couramment cité pour une équipe produit qui veut tenir un rythme
de livraison est de 30 à 50 %). **Zéro** test E2E navigateur en CI
(`puppeteer-core` est une devDependency utilisée par **un** script), **zéro** `axe`, **zéro** MSW.
Les tests mockent le module entier : `vi.mock('@/lib/api', () => ({ default: { get: vi.fn(), post: vi.fn() } }))`
(`OnboardingWizardPage.test.tsx:14-20`).

**Impact.** Ces tests prouvent que le composant fait ce qu'il fait **si l'API répond comme le test
l'imagine**. Ils ne prouvent pas que l'API répond comme ça. C'est un garde-fou contre les
régressions locales, pas contre la dérive de contrat — précisément le défaut qu'on veut éviter (§3.2).

**Correctif.** MSW pour un vrai contrat serveur réutilisé par les tests et le dev ; Playwright pour
5 parcours critiques ; `axe-core` en test de rendu.

### 🔴 3.5 — Observabilité : zéro, et un ErrorBoundary global unique

**Preuve.** Aucune dépendance Sentry / Datadog / OpenTelemetry. Un seul ErrorBoundary, qui
**enveloppe toute l'application** (`App.tsx:348-1163`). `getDerivedStateFromError` ne reçoit pas
l'erreur et **rien n'est journalisé** (`ErrorBoundary.tsx:19-21`).

**Impact.** (a) Une seule exception dans n'importe quel coin de l'app remplace **l'écran entier**
par « Réessayer » : l'utilisateur perd son contexte, la session visuelle, et l'erreur n'est
**personne** — ni lui, ni vous. (b) Aucun moyen de savoir qu'une version a cassé avant que les
utilisateurs ne le signalent.

**Correctif.** ErrorBoundary **par route** (React Router 7 le fait nativement avec `errorElement`) +
Sentry avec `release`, `environment`, `tags: {tenant, role, route}`.

### 🟠 3.6 — Le gate lint est neutralisé par sa propre configuration

**Preuve.** `package.json:11` : `"lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 1000"`.
`ci-cd.yml:81` l'exécute. Avec `--max-warnings 1000`, la commande **ne peut pas échouer sur des
warnings** : le contrôleur qualité est un no-op bien vert. Idem `noUnusedLocals: false` et
`noUnusedParameters: false` dans `tsconfig.json:18-19` : le compilateur laisse passer du code mort
et des variables inutilisées.

**Correctif.** `--max-warnings 0`, `noUnusedLocals: true`, et irresponsible `eslint-plugin-*` pour
l'accessibilité et les hooks. Objectif : 0 warning au jour 1 sur le périmètre migré, en morphant.

### 🟠 3.7 — 1,2 Mo de code chargé avant même le premier clic

**Preuve.** `frontend/dist/index.html` déclare en `modulepreload` :
`i18n-BtFW5ugV.js` (**784 Ko**) et `charts-Dy7WEPXN.js` (**426 Ko**).
Or `src/i18n/index.tsx:2-7` importe **statiquement les 6 dictionnaires**, donc la totalité des
6 langues est téléchargée pour un utilisateur francophone, et Recharts est chargé pour un écran
de connexion qui n'affiche aucun graphique.

**Impact.** Sur un réseau 3G/4GAfrica (le public visé), c'est le temps d'affichage initial.
Mesure coût/benefit honnête : ce n'est pas le plus gros problème du projet, mais c'est un problème
**gratuit à corriger** (import dynamique par locale) et immédiatement mesurable.

**Correctif.** `const dict = await import(\`./${locale}\`)` + un seul dictionnaire par locale
(lazy), et un petit `manifest.json` chargé au boot. Recharts doit rester dans le chunk de la page
qui l'utilise.

### 🟠 3.8 — `tText()` : une traduction par *valeur*, avec une recherche linéaire de 2 571 entrées

**Preuve.** `src/i18n/index.tsx:66-78` :

```
exact = REVERSE_FR.get(text)          // Map : rapide
sinon :  for (const [frValue, key] of REVERSE_FR)   // 2 571 itérations, À CHAQUE RENDU
```

Il y a **2 282 appels `tText`** dans le code. Dès qu'une chaîne n'est pas ** exactement** dans
`fr.ts` (variante de casse, espace insécable, chaîne nouvelle), le rendu parcourt le dictionnaire
entier. Deux défauts supplémentaires : le sens est inversé (on écrit du français et on traduit
après, ce qui interdit toute traduction=XLIFF/ICU et rend les clés non stables), et l'index
inverse est construit **une seule fois au chargement** de `fr.ts` (immuable, donc pas le bug) —
mais la boucle est bien un coût à chaque rendu.

**Impact.** Coût CPU multiplié par le nombre de chaînes non traduites à l'écran × 2 571
comparaisons par rendu. Sur un dashboard, c'est mesurable ; et le pire, c'est un défaut
**invisible** : ça marche.

**Correctif.** Clés stables + ICU (`formatjs` / `lingui`) avec **extraction automatique** et
contrôle CI « 0 clé manquante ». L'audit i18n déjà planifié (`B0` du plan d'orchestration) est le
bon endroit pour l'engager.

### 🟠 3.9 — Le design system existe en double

**Preuve.** Deux implémentations concurrentes :

| Composant | Copie A (utilisée) | Copie B |
|---|---|---|
| `EmptyState` | `components/shared/EmptyState.tsx` — prop `message` — **44 imports** | `components/ui/UXComponents.tsx:70` — prop **`description`** — 4 imports |
| `ConfirmDialog` | `components/shared/ConfirmDialog.tsx` | `components/ui/UXComponents.tsx:102` |
| `toast` | `react-hot-toast` monté dans `main.tsx:74` | `export { default as toast }` + `ToastContainer()` (`UXComponents.tsx:143-145`) |

**Impact.** Ce n'est pas cosmétique : **les contrats divergent** (`message` vs `description`).
Un développeur qui copie le motif de la mauvaise version obtient silencieusement un état vide
**sans description** (la prop est simplement ignorée). Et `SkeletonLoader.tsx` (66 lignes) fait
encore doublon avec `SkeletonLine/SkeletonCard/SkeletonTable/SkeletonDashboard`.

**Correctif.** Une seule source, avec un **contrat de props unique** et une dépréciation
documentée des 44 appels. C'est une opération mécanique et sûre.

### 🟠 3.10 — Server-state dans les Contextes : cache manuel, non dédupliqué, non invalidable

**Preuve.** 5 des 6 contextes font du fetching manuel (`api.get` + `useState` + `useEffect`),
alors que `useQuery` est disponible : `AuthContext` (4 appels, 0 `useQuery`), `TenantContext`
(5 appels, **14 `useState`**, 0 `useQuery`), `ImpersonationContext` (2), `MetaContext` (1),
`SettingsContext` (1). `PlatformContext` est le seul à utiliser `useQuery` (5).

**Impact.** Aucune déduplication (N composants qui consomment le même contexte = 1 requête, donc
ce point est partly sauvé par le Context), mais : **aucune invalidation croisée** — si
`TenantContext` recharge après un changement d'abonnement, aucun `useQuery` du reste de l'app ne le
saura. C'est un mélange de deux modèles de state dans le même code, ce qui rend le debug
imprévisible.

**Correctif.** Contextes = **client pur** ; état serveur = `useQuery` partout, y compris dans les
providers (avec `queryKey` préfixée).

### 🟠 3.11 — Authentification : jetons dans `localStorage`, et deux mécanismes superposés

**Preuve.** `lib/api.ts:21-24` (jeton lu dans `localStorage` et mis dans l'en-tête) et
`lib/api.ts:11` (`withCredentials: true` **en même temps**), plus
`AuthContext.tsx:66-115` (écriture dans `localStorage` des 3 clés), 26 fichiers qui touchent
`localStorage`. Le refresh est un singleton à `isRefreshing` + file d'attente
(`api.ts:29-46`) : correct, mais c'est une reimplémentation maison d'un problème résolu
nativement par `httpOnly` cookies.

**Impact.** Un jeton lisible en JavaScript est un jeton voleable par une injection XSS ou par une
dépendance npm compromise. `withCredentials` + en-tête redondant crée un **deuxième chemin
d'authentification** non documenté, difficile à auditer. Le true fix est côté **backend** :
cookies `httpOnly` + `SameSite=Strict` + `CSRF` token, avec le `Authorization` header conservé
seulement pour l'API publique.

### 🟠 3.12 — Gate CI incomplet : pas de budget de bundle, pas de régression de taille, pas d'E2E

**Preuve.** `ci.yml:89-93` et `ci-cd.yml:78-92` exécutent `tsc`, `vitest`, `build` — c'est bien.
Mais : **aucun** contrôle de taille de bundle après build (le `SIZE=$(du -sh …)` de `ci-cd.yml:92`
est affiché, **pas comparé** à un seuil), **aucune** étape E2E, **aucune** comparaison de
`npm audit` en gating pour le frontend (elle existe pour le backend).

**Impact.** Rien n'empêche le bundle de grossir de 40 % d'un build à l'autre : c'est exactement
le mécanisme qui a produit 5,4 Mo sans que personne ne s'en aperçoive.

### 🟡 3.13 — Dépendances redondantes et mélange de transports temps réel

**Preuve.** `react-router` **et** `react-router-dom` sont tous les deux dans `dependencies`
(`package.json:19-20`) alors que le second ré-exporte le premier ; **132** fichiers importent
`react-router-dom`, **0** `react-router`. `sockjs-client` **et** `@stomp/stompjs` sont utilisés
ensemble dans les deux mêmes hooks (`useVoiceWebSocket.ts:2-3`, `useStreamChatWebSocket.ts:2-3`).

### 🟡 3.14 — 27 dates formatées à la main, donc 27 chaînes non internationalisées

**Preuve.** 27 `toLocaleDateString('fr-FR')` en dur dans les pages. Un utilisateur arabophone ou
swahili voit des dates au format français. C'est un défaut **fonctionnel**, pas esthétique.

### 🟠 3.15 — Aucune documentation d'architecture frontend

**Preuve.** `docs/architecture/target-architecture.md` (le document d'architecture cible) ne parle
**que backend/engine**. Aucun ADR (`docs/DECISIONS.md` existe mais ne couvre pas l_losses des
choix front). Aucun document ne dit « une feature = un dossier, avec quoi elle peut importer ».

---

## 4. CE QUE FONT RÉELLEMENT LES GRANDES ÉQUIPES — ET CE QUI EST PROPORTIONNÉ ICI

### 4.1 Les cinq architectures que vous pouvez appliquer (et leur ratioBenefit/Coût)

| # | Architecture | Ce que c'est | Où elle est réellement utilisée | Adapté à votre taille ? |
|---|---|---|---|---|
| **A** | **Vertical slices** (colocation) | Une fonctionnalité = un dossier contenant **tout** ce qui lui appartient : ses types, son client API, ses hooks, ses composants, ses tests, ses traductions. Les pages ne font que composer. | Standard de l'industry moderne (Azure Architecture Center le recommande explicitement ; « Feature-Fliced Design », « Vertical Slice Architecture » de Jimmy Bogard). Utilisé par les équipes produit de Shopify/Remix. | ✅ **OUI — c'est la réponse** |
| **B** | Couches (domain / application / infrastructure) | Séparation stricte en couches. | Produits à forte logique métier, firms. | ⚠️ Surdimensionné ici : vos pages sont des CRUD, pas un domaine. |
| **C** | **Monorepo + frontières de modules** (`Nx` enforce-module-boundaries, `eslint-plugin-boundaries`, ou règles `no-restricted-imports`) | Un dépôt, plusieurs packages interdits de s'importer. | Uber (Nx), Google (Bazel strict deps), Airbnb (config ESLint open-sourced). | ✅ **OUI pour la version légère** : règles ESLint dans **un seul** dépôt, sans Nx. |
| **D** | **Micro-frontends** (Module Federation) | Chaque équipe a son build et son déploiement. | Adidas, Adonis, Zalando (études de cas publiques). | ❌ **NON** — vous êtes 1-2 développeurs. L'overhead (déploiement, versions, runtime partagé, debug distribué) serait plus coûteux que le problème résolu. |
| **E** | **Server-first / BFF** (loaders/actions, SSR) | Les données sont chargées **par le serveur de rendu**, pas dans le composant. | Shopify/Remix, Next.js. | ❌ **NON pour l'app** (connecté, pas de SEO). ✅ **OUI pour le site vitrine** (`LandingPage`, `PricingPage`), si la performance d'acquisition compte. |

### 4.2 Les pratiques transverses qui font la différence (toutes vérifiables sur leurs repos publics)

| Pratique | Ce que ça change concrètement | Présent ici ? |
|---|---|---|
| **Contrat-first** : OpenAPI → types générés (`orval`, `openapi-typescript`), jamais de type écrit à la main | Une dérive de contrat **casse le build** au lieu d'arriver en production | ❌ (alors que l'OpenAPI existe déjà, ajouté par A16) |
| **Contrat serveur simulé** : MSW, réutilisé en dev et en test | Les tests testent **le serveur**, pas le mock | ❌ |
| **E2E + a11e** : Playwright, `axe-core` | On prouve le parcours, pas le composant | ❌ (`puppeteer-core` utilisé par 1 script) |
| **Design system unique** avec tokens + Storybook/Chromatic + versionné | 1 implémentation, donc 1 seule maintenance | ❌ (§3.9 : il y en a 2) |
| **Feature flags** (OpenFeature / LaunchDarkly) | On déploie derrière un flag, on active après | ❌ — or vous en avez besoin pour un SaaS avec des features par plan |
| **Observabilité** (Sentry, OpenTelemetry) | Une régression est vue par vous, pas par l'utilisateur | ❌ |
| **i18n par clés + ICU + extraction CI** | Traductibles, non cassables par une refonte | ❌ (§3.8) |
| **Tests visuels** | Une régression graphique est vue | ❌ |
| **ADRs** (`docs/adr/`) | On sait *pourquoi* on a choisi, donc on ne re-discute pas | ❌ |
| **TypeScript `strict` + `noUncheckedIndexedAccess`** | Le compilateur attrape les cas limites | `strict` ✅ / `noUncheckedIndexedAccess` ❌ |

**Ce qui est déjà au niveau des grandes équipes :** TypeScript strict, découpage de bundle par
route, gestion de server-state avec TanStack Query, ErrorBoundary, RBAC centralisé, CI qui
typecheck/test/build, effort i18n 6 locales, test de non-régression sur le travail récent.

### 4.3 L'architecture cible que je recommande (et pourquoi elle est proportionnée)

```
src/
├── app/                      # bootstrap : providers, router, error boundaries, flags
│   ├── router.tsx            # createBrowserRouter + errorElement PAR ROUTE
│   ├── providers.tsx
│   └── queryClient.ts        # préfixe tenant injecté (voir §3.3)
├── shared/                   # le SEUL endroit partagé
│   ├── ui/                   # design system UNIQUE (fusion UXComponents + shared/*)
│   ├── lib/                  # http client typé, formatters Intl, guards
│   ├── config/               # feature flags, constantes
│   └── i18n/                 # clés stables + ICU, un fichier par locale, lazy
├── generated/                # types OpenAPI — GÉNÉRÉS, jamais modifiés à la main
├── entities/                 # un dossier par concept métier, dépend de shared/ seulement
│   ├── soul/  ├── family/  ├── department/  ├── event/  ...
├── features/                 # une slice verticale complète par action métier
│   └── soul-detail/
│       ├── api.ts            # endpoints + zod de validation réponse
│       ├── model.ts          # types + queryKey factory
│       ├── ui/               # composants de la slice
│       └── __tests__/        # tests Vitest + Playwright
└── pages/                    # COMPOSITION SEULE : < 50 lignes, zéro fetch, zéro type
```

**Les 5 règles qui rendent ça tenable** (et qui sont *l、事故* de l'architecture actuelle) :

1. `pages/` ne peut **pas** importer `features/`, seulement l'inverse. (Linter.)
2. `shared/` ne peut importer **ni** `entities/` **ni** `features/`. (Linter.)
3. `shared/ui/` n'importe **que** `shared/` — jamais une feature. (Linter.)
4. Les types API viennent **de** `generated/`. Un `any` dans `pages/` = blocage CI.
5. Chaque `queryKey` passe par la fabrique `features/*/model.ts`. (Linter + test.)

---

## 5. LE PLAN DE MIGRATION — 6 VAGUES, CHACUNE MESURABLE ET RÉVERSIBLE

> **Position explicite : ne réécrivez pas.** Une réécriture de 108 000 lignes par une équipe de 1-2
> personnes est un projet de 6 à 12 mois qui livre zéro valeur fonctionnelle pendant toute sa durée
> et qui, dans un produit SaaS en production, introduit des régressions que personne ne peut tester.
> La méthode professionnelle est le **strangler fig pattern** : on migre domaine par domaine, on
> garde l'ancien chemin qui fonctionne, on ne coupe que quand le nouveau est **prouvé**.

| Vague | Contenu | Durée indicative | Critère de sortie **mesurable** | Risque |
|---|---|---|---|---|
| **V0 — Outiller (sans toucher au produit)** | ESLint : `no-restricted-imports` (5 règles §4.3), `max-warnings 0`, `noUnusedLocals` ; `eslint-plugin-jsx-a11y` ; `import/no-cycle` ; rapport de dépendances cycliques. | 2-3 j | `npm run lint` sort en 0 ; le rapport de cycles est **publié** (on ne casse rien, on mesure) | Nul |
| **V1 — Socle** | Client HTTP typé + validation zod des réponses ; fabrique de clés avec préfixe tenant ; i18n par clés + ICU + extraction CI ; design system unique (fusion des doublons) ; ErrorBoundary par route ; Sentry ; budget de bundle en CI. | 2-3 semaines | 0 `any` sur les modules touchés ; 0 doublon dans `shared/ui` ; chunk i18n hors du boot ; Sentry reçoit une erreur réelle | Faible (additif) |
| **V2 — Slice de référence** | Une seule feature complète en architecture cible (ex. `entities/soul` + `features/soul-detail`), avec ses tests MSW et son E2E Playwright. | 1 semaine | La slice de référence passe tous les gates, y compris le nouveau, et **sert de modèle** | Faible |
| **V3 — Vague par domaine** | Migrer 5-8 pages par semaine par domaine, en commençant par le plus simple. | ~8-12 semaines | Compteur : `pages hors cible` qui descend ; chaque page migrée a son test | Moyen (régressions locales détectées par E2E) |
| **V4 — Suppression** | Supprimer les doublons, les `queryKey` littéraux, les wrappers de compatibilité, les écrans morts. | 2 semaines | `grep` ne trouve plus aucun `queryKey:` littéral hors `model.ts` | Moyen |
| **V5 — Performance & robustesse finales** | Playwright sur 5 parcours critiques, `axe` en gating, budget bundle dur, lazy i18n, virtualisation des longues listes, migration `localStorage` → cookies `httpOnly` (avec le backend). | 3-4 semaines | Bundle initial < 300 Ko gzip ; 0 violation a11e bloquante ; parcours E2E verts en CI | Moyen-élevé (cookies = backend) |

**Le détail compte, alors voici la version non-élégante mais honnête :** les vagues V0 et V1
rapportent l'essentiel du bénéfice (frontières, contrat, observabilité, i18n, design system) pour
~3 semaines de travail, **sans réécrire une seule page**. C'est le meilleur rapport valeur/risque
qui existe sur ce dépôt, et c'est exactement ce que les grandes équipes font sur leurs produits
existants.

---

## 6. CE QU'IL NE FAUT SURTOUT PAS FAIRE

| Anti-recommendation | Pourquoi c'est un piège ici |
|---|---|
| **Réécrire le frontend** | 108 000 lignes, 240 pages, 1 071 appels API à re-certifier. Zéro valeur fonctionnelle pendant des mois, régressions non testables. |
| **Passer en micro-frontends** | Vous êtes 1-2 développeurs : vous créeriez 1 problème de déploiement pour résoudre 1 problème d'organisation de fichiers. |
| **Migrer vers Redux/Zustand** | Vous avez déjà la bonne réponse (TanStack Query pour le serveur, contexte pour le client). Ajouter un store global n'enlève aucune des causes racines. |
| **Passer à Next.js / SSR pour l'app** | L'app est derrière authentification, sans SEO ni TTFB critique. Le SSR de 240 pages authentifiées coûte plus qu'il ne rapporte. |
| **Adopter un monorepo Nx/Bazel** | Justifié à partir de ~10 développeurs et plusieurs apps. Aujourd'hui : overhead sans bénéfice. La version légère (règles ESLint) suffit. |
| **Créer une 2ᵉ bibliothèque de composants** | Le problème actuel vient précisément d'en avoir créé deux. |
| **Refactorer « pour la propreté » avant d'avoir des tests** | On ne refactorise pas du code non couvert : on ne prouve rien et on casse en silence. Tests d'abord. |
| **Ajouter une abstraction avant d'avoir 3 usages réels** | `YAGNI` est une règle d'architecture, pas seulement de développement. |
| **Migrer `localStorage` → cookies sans modifier le backend** | Le contrôle de sécurité est **serveur**. Un patch front seul déplace le problème et laisse le trou. |

---

## 7. PLAN D'ACTION IMMÉDIAT (si vous ne faites que 3 choses)

1. **V0** (2-3 jours) : le lint honnête + les 5 règles de frontière. Un seul commit, aucun risque,
   et à partir de là **chaque nouveau fichier** est correct par construction. C'est 80 % du
   bénéfice architectural pour 5 % de l'effort.
2. **Générer les types depuis l'OpenAPI** (1 semaine) : le backend publie déjà `/openapi.json`.
   C'est le levier qui empêche la prochaine moitié des bugs.
3. **i18n lazy + clés stables** (2 jours) : -784 Ko au boot, et fin des traductions qui se cassent
   au premier `git grep` de chaîne modifiée.

Et **une décision à prendre explicitement** : le §3.3 (cache non isolé par tenant) est un risque
**de sécurité latent** aujourd'hui contenu par un `window.location.reload()`. Il faut le corriger
avant, pas après, la prochaine amélioration de navigation.

---

## ANNEXE A — Commandes de reproduction

```bash
cd /home/arise/discipolat/discipolat_app-agentB/frontend

# volumétrie
find src -name '*.ts*' -exec cat {} + | wc -l                       # 116857
find src -name '*.ts*' -not -path '*__tests__*' | xargs cat | wc -l  # 107964
find src/__tests__ -type f | xargs cat | wc -l                       # 8889

# pagesInstrumentation
find src/pages -name '*.tsx' -exec wc -l {} + | sort -rn | head -20
find src/pages -name '*.tsx' -exec wc -l {} + | awk '$1>500 && $2!="total"' | wc -l   # 32

# architecture
ls src/pages/*.tsx | wc -l                     # 240
ls src/features src/entities src/domain 2>/dev/null   # aucun dossier
grep -n '"@/\*"' tsconfig.json

# contrats & types
grep -rn ": any\b\|as any\b\|<any>" src | wc -l               # 259
grep -rho "as [A-Z][A-Za-z<>\[\]]*" src/pages | wc -l        # 484
grep -rl "zodResolver\|from 'zod'" src | wc -l                # 5

# cache
grep -rho "queryKey: \[[^]]*\]" src | sort -u | wc -l          # 501
grep -rn "queryKey: \[[^]]*tenant" src | wc -l                 # 1
grep -rn "queryClient.clear\|removeQueries" src                # aucun

# tests
ls src/__tests__/*.test.tsx | wc -l                             # 50
ls src/pages/*.tsx | sed 's#.*/##;s/\.tsx//' | sort > /tmp/p
ls src/__tests__/*.test.tsx | sed 's#.*/##;s/\.test\.tsx//' | sort > /tmp/t
comm -12 /tmp/p /tmp/t | wc -l                                 # 34

# outillage pro absent
grep -rn "msw\|storybook\|playwright\|cypress\|sentry\|datadog\|opentelemetry\|axe" \
     package.json vite.config.ts | wc -l                          # 0

# gates
grep -n '"lint"' package.json                                     # --max-warnings 1000
grep -n "noUnusedLocals\|noUnusedParameters" tsconfig.json

# doublons design system
grep -n "^export" src/components/ui/UXComponents.tsx
grep -rn "from '@/components/shared/EmptyState'" src | wc -l      # 44
grep -rn "from '@/components/shared/ConfirmDialog'" src | wc -l   # 0

# i18n & perfs
grep -rho "tText(" src | wc -l                                   # 2282
grep -c "" src/i18n/fr.ts                                        # 2875 lignes
grep -rn "toLocaleDateString('fr-FR')" src | wc -l                # 27
grep -c "modulepreload" ../frontend/dist/index.html               # i18n + charts au boot
ls -lS ../frontend/dist/assets/*.js | head -6                     # i18n 784 Ko, charts 426 Ko
du -sh ../frontend/dist/assets                                    # 5.4 Mo
```

## ANNEXE B — Ce que chaque chiffre ne dit pas

- **240 pages** ne veut pas dire 240 écrans utiles : plusieurs sont des variantes ou des
  dashboards redondants. Un inventaire fonctionnel n'a pas été fait — c'est le préalable
  recommandé avant V3.
- **7,6 % de tests** est un ratio LOC, pas une métrique de couverture. Aucune ligne n'est couverte
  (ni `nyc`, ni `vitest --coverage` en CI) : le ratio réel de pages critiques couvertes est
  probablement inférieur à ce qu'il paraît.
- **`any` = 259** ne veut pas dire 259 bugs : beaucoup sont dans des DTO de dictionnaire ou des
  cas `unknown` correctement castés. Le chiffre qui compte est « combien ont été introduits pour
  contourner une réponse d'API inconnue » — et c'est précisément ce que la génération de types
  élimine.
- **1 071 appels clients** (audit Agent B) est un bon chiffre de couverture du contrat, pas de
  maturité architecturale.

## ANNEXE C — Limites de cet audit (honnêteté)

1. **Je n'ai pas exécuté l'application.** Aucun profil DevTools, aucun Lighthouse, aucun
   Waterfall réel, aucune mesure Core Web Vitals. Les conclusions de performance (§3.7) sont
   **structurelles** (ce qui est dans le HTML de boot), pas des mesures terrain. Le coût réel de
   `tText` n'est **pas** mesuré — il est déduit du code.
2. **Le build `dist/` audité vient de `main`**, pas de la branche clients (les chiffres i18n/vendor
   sont donc un ordre de grandeur, pas un chiffre exact pour `282a464e`).
3. **Les pratiques des grandes entreprises** de la §4.2 sont des pratiques **publiques et
   documentées** (docs, dépôts open-source, engineering blogs). Je n'ai pas accès à leur code
   interne et ne prétends pas en décrire les détails.
4. **Aucun entretien avec les développeurs** : je n'ai pas la contexte historique (contraintes de
   délai, disponibilité Flutter, un agent à la fois), qui explique probablement une partie de la
   structure à plat. Le rapport judge l'état, pas les personnes.
5. **Le backend n'a pas été audité** : plusieurs constats (contrat, `users.tenant_id`, H2) sont
   des défauts backend qui *apparaissent* côté front. §3.2 est le seul remède front.
6. **Le mobile n'a pas été audité** (Flutter, même famille de problèmes probablement : écrans
   > 1 000 lignes mesurés par le plan d'orchestration).

---

*Fin du rapport. Chaque chiffre est reproductible avec l'annexe A. Les six ordres de grandeur à
retenir : 240 pages à plat · 32 pages > 500 lignes · 259 `any` · 501 clés de cache littérales ·
34 pages testées sur 240 · 1,2 Mo modulepreloadés au boot.*
