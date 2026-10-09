# TODO v2 — AJOUTS SANS RUPTURE : « église d'abord », landing par église, retour partout

> **Version 2**, rédigée le **2026-10-09**. La v1 était un plan d'audit ; celle-ci est un
> **plan d'ajouts**. Changement d'angle décidé avec l'humain : *80 % du travail existe déjà et
> fonctionne — on ne le change pas, on ajoute des options autour.*
>
> Base de preuve identique (audit du dépôt à `ffd7eb93`, `main` = `origin/main`, 5 worktrees
> propres). Chaque fait cité en §2 a été vérifié dans le fichier et la ligne indiqués.
>
> **Ce document n'est pas un fichier d'autorité.** Il pointe vers `CAHIER_DE_CHARGE.md`,
> `AGENT_ORCHESTRATION.md`, `PLAN_CORRECTIFS_ONBOARDING_TENANT_2AGENTS.md`,
> `SPEC_BACKEND_SERVICES_MOBILES_V233.md`. Il ne contient que ce qui reste à faire.

---

## 0. Décisions validées par l'humain (ne pas les rouvrir)

| ID | Decision | Statut |
|---|---|---|
| **D1** | Le choix d'une église **ne donne jamais** l'appartenance à lui seul. Église trouvée → **code d'église**, **lien d'invitation**, ou **demande d'approbation**. | **VALIDÉE** (recommandation retenue) |
| **D2** | Une église non listée (`isListed=false`) reste invisible du sélecteur ; réponse strictement identique à « inexistant ». | **VALIDÉE** |
| **D3** | Chemin de la landing par église : **`/e/:slug`**, public. `/pages/:slug` (protégé) n'est pas touché. | **VALIDÉE** |
| **D4** | L'église personnalise sa landing elle-même, via les champs **déjà existants** de `TenantSettings` + un nouveau toggle. | **VALIDÉE** |
| **D5** | À la connexion, le choix d'église est **facultatif** et ne filtre pas l'authentification (l'email reste l'identifiant global ; le sélecteur de rôle existant n'est pas modifié). | **VALIDÉE** |
| **D6** | Mobile : conversion `go()` → `push()` **sur liste mesurée**, écran par écran, un commit par conversion. Jamais systématique. | **VALIDÉE** |

---

## 1. Principe directeur : ADDITION STRICTE (règles A1→A8)

Elles priment sur toute tâche individuelle. Une tâche qui les viole est refusée en revue.

- **A1 — Aucune suppression.** Aucun fichier, aucune route, aucun champ de DTO, aucune clé
  i18n, aucun bouton existant n'est retiré. Exception unique : cas de force majeure, prouvé par
  exécution, **signalé et accepté par l'humain avant commit**, et journalisé en §8.
- **A2 — Un nouveau comportement n'est jamais obligatoire.** Il s'active par absence de contexte
  (pas de paramètre d'URL) ou par opt-in explicite (`landing_enabled=false` par défaut). Tout ce
  qui fonctionnait hier fonctionne identiquement aujourd'hui, **y compris par lien profond**.
- **A3 — Les ajouts parlent la langue de l'existant.** Le nouveau sélecteur d'église **émet les
  paramètres d'URL déjà implémentés** (`?church=`, `?joinCode=`, `?mode=church`, `?tenant=`) au
  lieu d'inventer un canal d'état. Conséquence : le `payload` d'inscription et le contrôleur
  serveur **ne bougent pas** (`RegisterPage.tsx:59-65,135-140` ; `AuthController.java:113-140`).
- **A4 — Contract freeze.** Les endpoints existants gardent leurs champs, leurs codes HTTP et
  leurs en-têtes. Les nouveaux endpoints sont ajoutés **à côté**. Les DTO existants peuvent
  recevoir un champ **optionnel additional only** ; jamais de champ requis nouveau.
- **A5 — DB additive only.** Migrations Flyway **numérotées à la suite** de la chaîne (jamais
  insérées au milieu : la chaîne V est validée par un gate dédié), colonnes **nullables avec
  défaut rétro-compatible**, aucune back-fill destructrice, rollback = l'app fonctionne avec la
  colonne absente ou à false.
- **A6 — Aucune mutation des fonctions partagées.** Là où une fonction globale existe
  (`applyBranding` écrit sur `document.documentElement`, `branding.ts:107-132`), on ajoute une
  **variante scopée** (`applyScopedBranding`) au lieu de modifier l'originale. Les appelants
  actuels sont intacts, le nouveau chemin est isolé.
- **A7 — Dark launch.** Chaque lot derrière un interrupteur : toggle de réglage, ou simple
  absence de lien d'entrée. Déploiement sans exposition, activation progressive.
- **A8 — Preuve par exécution.** Gates du dépôt (§7) + pour chaque ajout, un test **rouge avant /
  vert après**, et un test de **non-régression** qui échoue si l'ancien comportement change.

---

## 2. Le socle qui existe déjà (intouchable — preuves)

| Brique | Preuve |
|---|---|
| Annuaire public avec recherche par nom `?q=`, réponse `{total, content[]}`, **sans PII**, limité aux églises en opt-in | `backend/.../modules/network/api/PublicChurchesController.java:44-56`, `96-120` |
| Résolution par **code** ou **slug**, rate-limitée par IP (`X-RateLimit-Remaining`, `Retry-After`) | `backend/.../modules/tenants/api/PublicJoinController.java:24-81` |
| Inscription **créateur** : `createChurch` (+ `churchName`, `kind`) → tenant + `TENANT_OWNER` + code immédiat + session réémise | `AuthController.java:105-140` |
| Inscription **rejoignant** : `joinCode` (OPEN → activation, APPROVAL → demande), `tenantSlug`/`tenantId` legacy, exclusivité stricte, header `X-Invite-Code` | `AuthController.java:113-140` |
| Web : `/register?mode=church`, `?joinCode=`, `?tenant=`, `?church=`, `?plan=` déjà consommés | `frontend/src/pages/RegisterPage.tsx:51-77,135-140` |
| Web : `/join`, `/j/:slug`, `/j/:slug/:code`, `/accept-invitation`, `/eglises`, `/churches`, gestion des demandes côté tenant | `App.tsx:413,415,1305-1312`, `JoinChurchPage.tsx` (361 l.), `TenantJoinManagementPage.tsx`, `workspaces.ts:368` |
| Mobile : `/join` (avec `/public/join/lookup`), `/register?mode=church`, `/accept-invitation`, `/register` | `mobile/lib/presentation/screens/login/join_church_screen.dart:8-58`, `login_screen.dart:338-375` |
| Marque par tenant **déjà en base** : `businessName`, `slogan`, `legalName`, `description`, `logoUrl`, `logoDarkUrl`, `coverUrl`, `faviconUrl`, 6 couleurs | `backend/.../modules/tenants/domain/TenantSettings.java:32-73` |
| Application de marque + dérive de nuances + cache (web), et palette dérivée avant `MaterialApp` (mobile) | `frontend/src/lib/branding.ts:107-176`, `contexts/SettingsContext.tsx:23-36` **[…à vérifier : source exacte de l'appel, globale ou par tenant]** ; `mobile/lib/main.dart:131-143`, `mobile/lib/tenant_config.dart` |
| Landing modulaire par sections, `HomeGate` sur `/` | `pages/LandingPage.tsx`, `components/landing/*`, `App.tsx:380-392,1303` |
| **Retour déjà centralisé** pour toute la zone authentifiée : `<BackButton/>` + `<Breadcrumbs/>` montés une seule fois | `layouts/MainLayout.tsx:102-108` (« LOT 2 §BK … au lieu des 16 boutons copiés-collés qui divergeaient ») |
| Module de résolution du retour, avec cible `from`/`parent`/`history`, et son prop `fallbackTo` | `frontend/src/navigation/back.ts:61-110`, `components/navigation/BackButton.tsx:20-28` |

**Conclusion operative :** les trois demandes se règlent par **surfaçage et branchement**, pas
par refonte. Les zones réellement manquantes sont mesurées en §3.

---

## 3. Recommandations par problème — regard senior, sans risque

### PROBLÈME 1 — Choisir son église à la connexion comme à l'inscription

**Pourquoi maintenant.** Le parcours « rejoindre » existe mais n'est **pas découvrable** : il
faut déjà connaître le code ou avoir reçu le lien. L'utilisateur seul face au formulaire ne
voit aucune porte d'entrée vers son église. C'est un problème d'**accès à une capacité existante**,
pas d'absence de capacité.

**Option retenue (additive).** Un composant **nouveau** `<ChurchPicker/>` (une implémentation,
deux hôtes), qui :
- champ libre + suggestions issues de `GET /api/v1/public/churches?q=` (déjà là) ;
- état `notFound` → texte explicite « Cette église n'existe pas » + **deux CTA vers les pages
  déjà existantes** (`/accept-invitation`, `/join`) ;
- église trouvée → **écrit les paramètres d'URL existants** et affiche le formulaire tel qu'il
  est aujourd'hui (A3). Aucune nouvelle field dans le payload d'inscription.
- À la connexion (D5) : le picker **n'est pas obligatoire** et n'entre pas dans `LoginRequest`
  (qui reste `email` + `password`, `AuthController.java:86-100`). Après connexion réussie, si le
  slug choisi ne fait pas partie des memberships, un **panneau additif** est proposé à côté du
  sélecteur de rôle existant — le sélecteur de rôle n'est ni remplacé ni reordonné.

**Ce qui est touché :** ajout de 1 composant web, de 1 écran Dart, de 2 endpoints publics
**nouveaux**, de 1 slot d'affichage dans `RegisterPage`/`LoginPage`, de clés i18n **nouvelles**.
**Ce qui ne l'est pas :** le contrat `/auth/register` et `/auth/login`, les 5 options de
`orgKind`, les URLs profondes, le flux d'activation, `switch-role`, l'atterrissage super-admin,
les tests existants.

**Rayon d'impact :** faible et visible — 2 pages publiques. **Coût du délai :** élevé et continu ;
chaque inscription sans porte d'entrée vers l'église est un abandon silencieux, et les
administrateurs d'église continuent à distribuer leurs codes « à la main ».

**Risque critique écarté :** l'adhésion auto-déclarée (D1) serait une **faille d'intrusion
inter-tenant** — un problème légal et de corruption de données, pas une dette technique. Le
design retenu le ferme à la source : le serveur n'accepte qu'un `joinCode`, une invitation ou
une approbation, et le picker ne fait que **raccourcir le chemin vers ces preuves**.
Second risque écarté : l'énumération d'existences d'églises (D2/S2) — le nouvel endpoint
`/exists` répond **identiquement** pour « inexistante » et « non listée ».

**Tâches.**
- **T1.1** [BE] `GET /api/v1/public/churches/suggest?q=` (top 10, champs de la vue publique
  existante uniquement) et `GET /api/v1/public/churches/exists?q=` ; réutiliser `toPublicView`
  et le filtre `isListed` ; `PerIpRateLimiter` sur les deux. **Aucune ligne modifiée dans
  `list()`** — uniquement des méthodes en plus.
  *Test discriminant* : église `isListed=false` → absente de `suggest`, et `exists` renvoie la
  même réponse qu'un nom fantôme. *Test de non-régression* : réponse de `/public/churches`
  inchangée au bit près.
- **T1.2** [FE] `components/auth/ChurchPicker.tsx`, 5 états (`idle/searching/found/notFound/error`),
  combobox accessible (`aria-expanded`, flèches, Entrée, Échap), debounce 300 ms.
- **T1.3** [FE] Branchement sur `/register` **conditionnel** : le picker ne s'affiche que si
  **aucun** contexte n'est présent dans l'URL (`mode`, `joinCode`, `tenant`, `church`). Les liens
  existants (e-mails, QR, mobile) court-circuitent le picker et produisent le parcours d'aujourd'hui.
- **T1.4** [FE] Branchement sur `/login` en mode facultatif (D5) + panneau additif post-connexion.
- **T1.5** [FE/i18n] Chaînes dans **les six** locales (`frontend/src/i18n/{fr,en,es,pt,sw,ar}.ts`),
  garde-fou `__tests__/uiCustomizationI18n.test.tsx`.
- **T1.6** [MOB] `presentation/screens/login/church_picker.dart` + branchement sur
  `login_screen.dart`/`register_screen.dart` ; **les 4 boutons existants restent en place**.
  Traductions dans les 6 `.arb` de `mobile/lib/l10n/` puis `flutter gen-l10n`.
- **T1.7** [CONTRAT] Test de contrat web ↔ mobile ↔ serveur sur les deux nouveaux endpoints
  (discipline déjà pratiquée dans ce dépôt).

---

### PROBLÈME 2 — Le logo ramène au landing ; chaque église peut avoir sa landing

**Constat mesuré.** Le logo n'est cliquable **nulle part** : `Sidebar.tsx:154-172` est une
`<div>` sans lien, `AuthLayout.tsx:55-66` idem, et `LandingNavbar.tsx:54-66` ne fait que remonter
à l'ancre héro (utile sur le landing, trompeur ailleurs). `Navbar.tsx` **ne rend pas la marque** :
**ne rien y inventer** — si l'humain veut un logo en barre supérieure, ce sera une tâche distincte,
validée. Côté landing par église, **les données existent déjà** (`TenantSettings`, §2) : il manque
simplement une projection publique et une route.

**Option retenue (additive).**
1. **Atome partagé `<BrandLink/>`** (nouveau) : lien vers `/`. On remplace la `<div>` marque par
   `<BrandLink/>` **sans changer le rendu visuel** (mêmes classes, même logo, mêmes pastilles
   animées). Sur `/` déjà chargé, le comportement reste « remonter au héro ».
2. **`/e/:slug` → `ChurchLandingPage`** (nouvelle route publique, lazy) qui **réutilise les
   sections existantes** `components/landing/*`. La marque est appliquée via une **variante
   scopée** `applyScopedBranding(node, branding)` (A6) : `applyBranding` global n'est pas
   modifié, donc la plateforme ne peut pas être dégradée par une landing d'église.
3. **Seulement deux colonnes ajoutées** (`landing_enabled`, `landing_sections`) sur `TenantSettings`,
   **défaut `false`** → aucune page publique n'apparaît pour aucune église existante tant que
   l'église ne l'a pas demandé (A5/A2, dark launch naturel).
4. `GET /api/v1/public/churches/{slug}` : projection **stricte liste blanche** ; `404` si non
   listée **ou** `landing_enabled=false` (le 404 est identique aux deux cas, anti-énumération).
5. Écran d'admin tenant : **un onglet « Page publique » de plus**, les réglages existants intacts.

**Ce qui est touché :** 3 fichiers de layout pour le remplacement `<div>` → `<BrandLink/>`, 1
nouvelle route, 1 nouveau endpoint, 2 colonnes nullables, 1 composant de plus dans `branding.ts`
(nouvelle fonction, pas de modification de l'existante). **Ce qui ne l'est pas :** le contrat
`/public/churches`, `/pages/:slug` (protégé), le `SettingsContext` et la marque plateforme, le
model `TenantSettings` existant, le rendu visuel des blocs logo.

**Rayon d'impact :** très faible au départ (le toggle est `false` partout) puis progressif à
l'opt-in. **Coût du délai :** moyen — la personnalisation par église est un argument commercial
direct (white-label) ; le retard se paie en opportunité, pas en incident.

**Risque critique à traiter avec la plus grande prudence (juridique, pas technique) :** exposer
publiquement l'identité d'un lieu de culte est une **donnée sensible au sens du RGPD** (article 9).
Trois verrous, non négociables, sont déjà imposés par l'existant et doivent être conservés :
opt-in d'annuaire (`isListed`) **et** opt-in de landing (`landing_enabled`) — double consentement ;
projection liste blanche (jamais l'objet complet, jamais de donnée membre, jamais de compteur
interne) ; `404` indistinguable pour tout ce qui n'est pas doublement opt-in. Le cache/`noindex`
de la page publique doit être posé dès T2.4 (pas d'indexation de pages religieuses par surprise).
Attention aussi à `RegisterPage.tsx:80` (`inviteUrl`) qui connaît déjà la convention `/j/:slug` :
la cohérence `/e/:slug` ↔ `/j/:slug` est un point de revue, pas un détail.

**Tâches.**
- **T2.1** [FE] `<BrandLink/>` + branchement `Sidebar`, `AuthLayout`, `LandingNavbar`.
  *Test* : clic depuis chacun des 3 hôtes → `/` ; aspect mesuré identique avant/après
  (capture ou assertion de classes) ; accessible au clavier (c'est un `<a>`, pas un `<div>`).
- **T2.2** [BE] endpoint public de projection (§3, liste blanche) + tests : aucune clé hors liste ;
  `isListed=false` → 404 ; `landing_enabled=false` → 404 **identique**.
- **T2.3** [DB] Migration Flyway additive, numérotée à la suite de la chaîne, 2 colonnes nullables,
  défaut `false`. *Test discriminant* : la porte de gate Flyway existante passe ; rollback vérifié.
- **T2.4** [FE] `/e/:slug` + `applyScopedBranding` + `noindex` + `og:*`, favicon et titre par
  église. *Test* : deux onglets (`/` et `/e/:slug`) — **aucune fuite de palette** l'une vers l'autre.
- **T2.5** [FE/admin] onglet « Page publique » (prévisualisation + lien partageable réutilisant
  la convention `inviteUrl`).
- **T2.6** [MOB] `church_landing_screen.dart` + route `/e/:slug` (deeplink), palette lue via la
  config tenant existante (`tenant_config.dart`, `main.dart:131-143`).

---

### PROBLÈME 3 — Impossible de revenir à la page précédente

**Le point le plus important de cet audit : le chiffre brut est un faux positif.** Sur 261 pages
web, 220 n'ont aucun contrôle de retour *local* — mais elles **héritent** du bloc
`MainLayout.tsx:102-108` qui monte `<BackButton/>` + `<Breadcrumbs/>` une fois pour toutes. Ce
mécanisme est un travail propre, déjà testé (`__tests__/navigationBack.test.ts`) : **le remplacer
ou dupliquer 220 boutons serait une régression**, pas une amélioration.

**Vrais trous (mesurés) :**
- **Web, zone publique** : `layouts/AuthLayout.tsx` ne contient **aucun** contrôle de retour
  (`grep BackButton|ArrowLeft` → 0). C'est exactement le ressenti utilisateur : `/login`,
  `/register`, `/join`, `/accept-invitation`, `/forgot-password`, `/reset-password`, `/activate`.
  Ajoutons-y la zone vitrine (`/eglises`, `/churches`, futures `/e/:slug`).
- **Mobile** : la cause n'est pas l'absence de widget (`DetailBackButton` existe) mais **l'absence
  de pile** : **143 appels `context.go(`** contre **48 `context.push(`**. GoRouter avec `go()`
  **écrase la pile** → rien à popper → pas de flèche système. `DetailBackButton` n'est utilisé que
  par **3 écrans**.

**Option retenue (additive) — FAITE, avec une correction du plan.**
- Web : **insérer** `<BackButton/>` dans `AuthLayout`. Le plan annonçait « aucune modification du
  composant, le prop `fallbackTo` existe déjà » : **c'était faux**, vérifié dans le code. `fallbackTo`
  n'est consulté que quand `target === null` (`BackButton.tsx:37-40`), or le composant appelait
  `resolveBack(pathname, from, **true**)` — l'historique était *supposé* exploitable. Sur `/register`
  en entrée directe, `target` valait donc `-1` et le bouton restait **muet** : le `fallbackTo` ne
  se déclenchait jamais. La correction est restée additive : **une prop optionnelle de plus**
  (`detectHistory`, par défaut `false`) qui remplace l'hypothèse par une mesure de
  `window.history.state.idx` (`hasUsableHistory`, nouvel export de `back.ts`). `<BackButton />`
  sans prop — la seule forme employée par `MainLayout.tsx:106` — garde un rendu et une résolution
  **identiques**, et un test le verrouille. Les 41 contrôles locaux existants **restent en place**
  (A1) ; leur redondance éventuelle est seulement **journalisée** pour une décision humaine ultérieure.
- Mobile : traiter la **cause** (la pile), avec le bouton existant comme **filet de sécurité** ;
  classement écran par écran, et **les entrées d'espace de rôle restent en `go()`** (un `push()`
  là produirait une pile infinie et un retour qui tourne en rond).

**Ce qui est touché :** le montage d'un contrôle dans `AuthLayout`, une prop optionnelle et un
export pur en plus ; la pile de navigation des écrans explicitement listés en annexe B.
**Ce qui ne l'est pas :** le layout authentifié, les 41 boutons locaux, `Breadcrumbs.tsx`,
les signatures existantes de `back.ts`, les entrées d'espace de rôle.

**Rayon d'impact :** web = quasi nul (insertion dans un layout) ; mobile = moyen, **à conduire par
petits commits**. **Coût du délai :** faible mais perçu — c'est un irritant quotidien qui donne
une impression d'inachevé, et il touche d'abord les parcours d'entrée (les plus fréquentés).

**Risque à surveiller :** sur mobile, convertir un `go()` en `push()` **au hasard** crée des piles
incohérentes, des retours qui ramènent à un écran mort, et peut casser des tests de navigation
existants. D'où D6 : un inventaire, trois classes (`ENTREE_ESPACE` / `SOUS_ECRAN` / `POST_AUTH`),
une conversion par commit, un widget test par conversion.

**Tâches.**
- **T3.0** [MESURE] Liste des routes **par layout** (celles hors `MainLayout` : zone `AuthLayout`,
  vitrine, `/e/:slug`). **Ne pas traiter les héritières de `MainLayout`.**
- **T3.1** [FE] ✅ **FAIT** — `<BackButton detectHistory fallbackTo="/"/>` monté dans `AuthLayout`.
  *Tests discriminants passés* : entrée directe sur `/register` (aucun historique) → libellé
  « Retour à l'accueil » et atterrissage sur `/` ; avec historique (`idx > 0`) → « Retour » et
  `navigate(-1)` ; **non-régression** → sans la prop, `data-back-source="history"` comme avant.
  Preuve rouge exécutée (2 tests tombent quand on neutralise le correctif, 4 tiennent).
  Clé `nav.backHome` ajoutée dans les **six** locales, avec test de parité.
- **T3.2** [FE] Zone vitrine : retour `/eglises` ⇄ fiche, `Breadcrumbs` sur les routes publiques.
- **T3.3** [FE] Journal des 41 doublons éventuels — **aucune suppression** sans accord humain (A1).
- **T3.4** [TEST] Étendre `navigationBack.test.ts` aux nouvelles routes (`/e/:slug`, `?church=`) ;
  test par hôte de la présence/absence du contrôle selon le layout ; accessibilité (nom
  accessible, focus visible, libellé traduit dans les six locales).
- **T4.0→T4.4** [MOB] Inventaire annexe B → classement → conversions `push()` testées →
  `DetailBackButton` comme filet → vérification du **bouton matériel Android** (`PopScope`,
  idiome déjà employé dans le dépôt).

---

## 4. Matrice de risques consolidée

| # | Risque | Classe | Traitement retenu | Verrou de preuve |
|---|---|---|---|---|
| R1 | Adhésion auto-déclarée à une église → intrusion inter-tenant | **Critique — sécurité/données** | D1 : preuve obligatoire (code / invitation / approbation) | Test : `register` avec `tenantSlug` seul refusé ; picker incapable d'envoyer une adhésion sans preuve |
| R2 | Exposition publique d'une donnée sensible (culte, art. 9 RGPD) | **Critique — juridique** | Double opt-in (`isListed` **et** `landing_enabled`), projection liste blanche, `noindex` | Test : aucun champ hors liste ; `404` indistinguable ; entête `X-Robots-Tag` |
| R3 | Énumération des églises (le nom devient un oracle d'existence) | **Critique — confidentialité** | Réponse binaire identique + `PerIpRateLimiter` | Test : même corps/code pour « fantôme » et « non listée » |
| R4 | Une landing d'église écrase la marque plateforme | Élevé — régression visuelle | A6 : `applyScopedBranding`, `applyBranding` intact | Test deux onglets, zéro fuite de palette |
| R5 | Suppression/duplication du système de retour déjà centralisé | Moyen — dette, pas incident | A1 : rien retiré ; insertion ciblée dans `AuthLayout` uniquement | Test de non-régression sur `MainLayout` |
| R6 | Pile de navigation mobile incohérente (`go` → `push` en masse) | Moyen | D6 : liste mesurée, un écran par commit | Widget test par conversion (`canPop`) |
| R7 | Régression des liens déjà distribués (e-mails, QR, mobile) | Moyen | A2/A3 : picker jamais obligatoire, émission des paramètres existants | Test : les 4 URLs profondes produisent le parcours d'avant |
| R8 | Migration Flyway insérée dans la chaîne V | Faible mais bloquant en CI | A5 : numérotation à la suite, nullables + défaut | Gate Flyway du dépôt |
| R9 | Clé i18n ajoutée dans une seule langue | Faible — dette | Six locales web + six `.arb` mobile | `uiCustomizationI18n.test.tsx` |

---

## 5. Ordre d'exécution recommandé (du plus sûr au plus visible)

| # | Lot | Risque | Effet perçu | Pré-requis |
|---|---|---|---|---|
| 1 | **T3.1** retour dans `AuthLayout` | quasi nul | immédiat (parcours d'entrée) | **Fait `34008a03`** |
| 2 | **T2.1** logo cliquable (`<BrandLink/>`) | quasi nul | immédiat | **Fait `9fac4933`** |
| 3 | **T1.1→T1.5** picker web | faible (endpoints neufs) | fort | arbitrage de wording |
| 4 | **T2.2→T2.5** landing par église | moyen (DB + RGPD) | fort, argument commercial | R2 verrouillé |
| 5 | **T1.6** + **T4.x** mobile | moyen | fort | parité de contrat (T1.7) |
| 6 | **T5.x** recette bout-en-bout + docs | nul | crédibilité | tout vert |

Déploiement : chaque lot séparément, `landing_enabled=false` par défaut (R2), et pour le picker
une activation par simple retrait du lien d'entrée si besoin (A7) — **aucun interrupteur
dangereux à inventer côté serveur**.

---

## 6. Contrat à geler avant la première ligne

**Endpoints ajoutés** (tous sous `/api/v1/public/**`, rate-limités, `permitAll` déjà en place) :

| Méthode + chemin | Entrée | Réponse | Interdits |
|---|---|---|---|
| `GET /api/v1/public/churches/suggest` | `q` (≥ 2 car.) | `{total, items:[{name, slug?, city?, country?}]}` ≤ 10 | tout champ non public ; toute église `isListed=false` |
| `GET /api/v1/public/churches/exists` | `q` (nom exact) | `{found, slug?, name?}` ou `{found:false}` | distinguer « non listée » de « inexistante » |
| `GET /api/v1/public/churches/{slug}` | `slug` | projection liste blanche de `TenantSettings` + `landingEnabled` + sections | `tenant_id`, e-mail, téléphone, adresse, compteurs, données membres |

**Routes web ajoutées :** `/e/:slug`. **Aucune route existante modifiée.**
**Nouvelles colonnes :** `tenant_settings.landing_enabled` (défaut `false`), `landing_sections`
(JSON nullable). **Aucun index supprimé, aucune contrainte modifiée.**

---

## 7. Gates et commandes (exécuter réellement, archiver la sortie)

```bash
# Backend — JDK 21 via SDKMAN, depuis backend/ ; jamais en parallèle d'un autre build
cd /home/arise/discipolat/discipolat_app/backend && mvn clean && mvn -q test

# Frontend
cd /home/arise/discipolat/discipolat_app/frontend && npx tsc -b && npx vitest run

# Mobile
cd /home/arise/discipolat/discipolat_app/mobile && flutter analyze && flutter test
```

`mvn clean` après toute fusion (les artefacts périmés font de faux échecs). Sous contention CPU,
`vitest` « Failed to start forks worker » est un **faux échec** : relancer sérialisé. Les preuves
rouges se font dans un worktree **isolé hors du chemin des worktrees actifs** (le dépôt connaît
le piège des checkout qui détruisent les correctifs non committés). `git push` peut renvoyer
`Internal Server Error` côté GitHub : `git push --no-thin origin main` passe. Jamais de `--force`.

---

## 8. Journal d'avancement et de dérogations

| Tâche | État | Preuve (sortie archivée / test) | Commit | Date |
|---|---|---|---|---|
| T3.1 retour `AuthLayout` | **FAIT** | `authZoneBackNavigation.test.tsx` (6) + `navigationBack.test.ts` (19) ; suite complète au vert ; `tsc -b` 0 ; `eslint` 0 erreur ; `vite build` ok. Preuve rouge : 2 tests échouent sans le correctif. | `34008a03` | 2026-10-09 |
| T2.1 logo cliquable | **FAIT** | `brandLink.test.tsx` (6) + 1 test d'intégration dans `Sidebar.test.tsx` ; `LandingPage.test.tsx` (9) inchangé au vert = rendu préservé ; suite complète **755 tests / 96 fichiers** au vert. Preuve rouge : 2 tests échouent sans l'interception same-route. | `9fac4933` | 2026-10-09 |
| T1.1→T1.7 picker web | **FAIT** | BE `PublicChurchesSuggestExistsTest` (10) ; FE `ChurchPicker` + `churchPickerI18n` (16/16) ; contrat `churchesSuggestExistsContract` (12, web↔mobile↔serveur) ; `tsc -b` 0 ; suite FE au vert. | non committé (politique) | 2026-10-09 |
| T2.2 `/public/churches/{slug}` | **FAIT** | BE `PublicChurchesLandingTest` (5) : projection liste blanche, 404 **identique** pour slug inconnu / non listée / landing éteinte, entête `X-Robots-Tag` ; `PublicChurchesSuggestExistsTest` (10) inchangé = non-régression. | non committé | 2026-10-09 |
| T2.3 migration V241 | **FAIT** | `V241__church_landing_flags.sql` (PostgreSQL, `ADD COLUMN IF NOT EXISTS`, `landing_enabled BOOLEAN NOT NULL DEFAULT FALSE`, `landing_sections JSONB`) ; entité `TenantSettings` + défaut `false` au `@PrePersist`. Flyway éteint sous H2 (tests en `create-drop`), gate PG à la recette. | non committé | 2026-10-09 |
| T2.4 landing `/e/:slug` | **FAIT** | `ChurchLandingPage` (lazy, hors layout) + `applyScopedBranding` (A6, `applyBranding` intact) ; `noindex`+`og:*` posés dès l'origine et nettoyés au démontage. `ChurchLandingPage.test.tsx` (7) : R4 zéro fuite palette `:root`, R3 message 404 unique, titre restauré. | non committé | 2026-10-09 |
| T2.5 onglet « Page publique » | **FAIT** | FE `TenantAdminBrandingPage` : 6ᵉ onglet additif (les 5 existants intacts), bascule `landing_enabled` (défaut faux), lien partageable sur la convention vanity `/e/:slug`, aperçu projection. i18n 6 locales + `landingAdminI18n` (17) ; `TenantAdminBrandingPublicPage` (8). BE plumbing `TenantSettingsRequest`/`Response` (+ champ) ; **casse rattrapée** : appelant positionnel `OnboardingStepActions.settingsRequest` mis à jour (2 `null`), sinon compilation KO. Suite FE **830/104** au vert. | non committé | 2026-10-09 |
| T3.2 vitrine : retour + fil d'Ariane | **FAIT** | Helpers **pur additifs** dans `navigation/back.ts` (`PUBLIC_DIRECTORY_PATH`, `isPublicShowcaseRoute`, `derivePublicParentPath`, `publicBreadcrumbTrail`) — `deriveParentPath` / `ROUTE_ROLES` / `Breadcrumbs` du `MainLayout` **intacts** (A6). Composant dédié `PublicBreadcrumbs.tsx` (66 l., `<nav aria-label>`, `aria-current="page"`, `ChevronRight` et `House` `aria-hidden`) branché sur `ChurchLandingPage` et `PublicChurchesPage` ; `<BackButton fallbackTo="/eglises">` posé sur la fiche, et **uniquement** sur la variante standalone `/churches` de l'annuaire (AuthLayout T3.1 sert déjà `/eglises`, sinon double controle → Annexe C #1). i18n 3 clés `publicNav.*` × 6 locales. **Régression rattrapée** : le `currentLabel={data.name}` dupliquait le `<h1>` et faisait tomber 4 tests T2.4 (`findByText` ambigu) → le fil retombe sur `t('publicNav.church')` ; le prop `currentLabel` reste disponible pour d'autres consolateurs. Suite FE **862/106** au vert. | non committé | 2026-10-09 |
| T3.3 journal doublons (A1) | **FAIT — journal, AUCUNE suppression** | `.e2e-tmp/t33_back_controls.txt` (46 entrées) : 13 fichiers avec appel impératif `navigate(-1)`/`history.back()`, 44 avec icône `ArrowLeft`/`ChevronLeft`/`CornerUpLeft`, 3 avec label texte. L'écart avec le « 41 » d'Annexe A vient du critère élargi (les pages utilisant une flèche hors contexte retour sont comptées, pour ne **RIEN** rater). Le fichier n'engage à **aucune** suppression : Annexe C #10 impose une ligne en §8 avec accord humain horodaté pour chaque retrait. Tout est laissé en place. | non committé | 2026-10-09 |
| T3.4 tests navigationBack + a11y | **FAIT** | `navigationBack.test.ts` : +6 tests additifs (bloc `describe('LOT 3 §BK (T3.2/T3.4) — vitrine publique')`) → 31/31. Reconnaissance `/eglises`/`/churches`/`/e/:slug`, refus hors vitrine, tolérance `?church=` et `#fragment`, mapping parent fiche→annuaire→racine, `PUBLIC_DIRECTORY_PATH` single-source, `currentLabel` qui gagne sur `publicNav.church`. `publicNavI18n.test.ts` (6 : parité 3 clés × 6 locales + filet FR-leak + unicité intra-locale). `PublicBreadcrumbs.test.tsx` (14 : 3 maillons fiche, 2 maillons annuaire, null hors vitrine sur `/dashboard` `/` `/souls/12` `/e/x/edit`, `aria-current` span non-anchor, `aria-label`, icônes `aria-hidden`, query `?church=` reconnue). Suite ciblée verte. | non committé | 2026-10-09 |
| T1.6 picker mobile | **FAIT** | `presentation/screens/login/church_picker.dart` (543 l., additif) : 5 états `idle/searching/found/notFound/error`, debounce `Timer(300 ms)`, séquentialisation `_requestSeq` (une frappe annule la réponse en vol), lecture **défensive** de `fromJson` qui ne garde QUE `name/slug/city/country` (R2/R3) ; les 3 CTA « non trouvée » sont des `Semantics` passifs (navigation laissée à l'hôte, Annexe C #1). Branchement **conditionnel** dans `RegisterScreen` : le picker ne s'affiche que si `!createChurch && (joinCode==null‖empty)` ; les trois gestes existants (`?mode=church`, `?joinCode=`, classique) restent intacts (A1/A3/R7). Le `onSelect` écrit un **hint local** (`_pickedChurchName`, `_pickedChurchSlug`) et n'ajoute RIEN au payload `/auth/register` (D1/R1 : le slug public ne vaut pas preuve d'adhésion). i18n : bloc `churchPicker` ajouté dans les SIX `.arb` de `lib/l10n/` (JSON valide, aucune duplication de clé). `flutter analyze lib/presentation/screens/login/` → 0 erreur, 2 `info` préexistants (lignes 284/301, présentes avant mon delta). `test/church_picker_test.dart` (10/10 vert) : contrat §6 verrouillé (chemins, debounce, minLength, CTA passifs, réponse `/exists` indistinguable, non-blocage en erreur réseau). T1.7 web `churchesSuggestExistsContract` revérifié : **12/12 vert** — le ratchet mobile a sifflé au premier brouillon (commentaire nommant les clés interdites), puis s'est tu une fois le commentaire réécrit sans sous-chaînes. | non committé | 2026-10-09 |
| T2.6 landing mobile `/e/:slug` | **FAIT** | `presentation/screens/landing/church_landing_screen.dart` (468 l., additif) : 4 états `loading/absent/error/ready`, lecture DÉFENSIVE qui ne consomme QUE la liste blanche §6 (`slug name landingEnabled slogan description logoUrl coverUrl website city country`) — un `tenantId` ou un `email` renvoyé par accident par le serveur reste INVISIBLE ; R3 message « Page indisponible » **unique** pour fantôme/non listée/landing éteinte ; R4 **aucune mutation de palette** (le plan dit explicitement « palette lue via la config tenant existante » — l'écran consomme `AppColors` tels que posés par `main.dart:131-143`, et n'y touche pas ; le `applyScopedBranding` web reste un raffinement futur hors périmètre). `AppBar.title` figé sur « Page publique » : le nom de l'église s'affiche en h1 dans le corps, le duplicater rendrait les `find.text(...)` ambigus (même piège que T3.2 web avec le fil d'Ariane sur `<h1>`). `DetailBackButton` monté dans le `leading` (filet A1, widget existant §LOT 2 §BK, aucun nouveau bouton codé en dur). Route GoRouter `/e/:slug` déclarée **après** `/accept-invitation` (aucune route existante modifiée). `_publicRoutePrefixes = <String>['/e/']` **additif** : `_publicRoutes` littéral inchangé (les six entrées d'origine intacts), un helper pur `_isPublicLocation` ajoute la exemption par préfixe. **Casse rattrapée** : le premier brouillon de `isExempt`ORvia `_isPublicLocation(...)` élargissait accidentellement l'exemption ONBOARDING à `/login` (qui est bien dans `_publicRoutes` mais volontairement PAS dans `firstRunExemptRoutes` — un visiteur fraichement installé doit finir sur l'onboarding, pas sur `/login`). `widget_test.dart` le prouve en séquence (test 1 laisse le router sur `/login`, test 2 doit être renvoyé vers `/onboarding`) : **échec → correction** (`isExempt = firstRunExemptRoutes.contains(…) ∪ _publicRoutePrefixes.any(…)`, strictement le préfixe NEUF, pas l'ancien `_publicRoutes`). `test/church_landing_screen_test.dart` (11/11 vert) : URL-encodage du slug (`Beth el/prod` → `beth%20el%2Fprod`), projection liste blanche (4 clés serveur fuitées → invisibles), `landingEnabled=false` → absent (défense en profondeur), 404 → message unique, 5xx → non-bloquant + bouton Réessayer, `Réessayer` rappelle le serveur, slug vide → absent sans réseau, montage initial (loading puis ready, sans flicker), `DetailBackButton` monté ×1 (aucun doublon), `initialState` injecté (3 branches couvertes sans réseau). T1.7 web `churchesSuggestExistsContract` revérifié : **12/12** (le ratchet Dart ne scanne que les fichiers qui touchent `/suggest` ou `/exists` ; `church_landing_screen.dart` consomme `/public/churches/{slug}` — hors périmètre, aucune dérive). `flutter analyze` mobile : 0 issue sur `church_landing_screen.dart`, 0 issue sur `app.dart`. Suite mobile complète : **648/648** (baseline 627 + 10 picker T1.6 + 11 landing T2.6, tous additifs), exit 0. | non committé | 2026-10-09 |
| T4.0, T4.1→T4.4, T5.x | À faire | | | |

**Écart / faux échec journalisé** : `b12_provisioning_no_forced_values.test.tsx` est tombé une fois sur
la suite complète (contention CPU/mémoire, `getAllByLabelText` après une transition de wizard) puis
est passé **seul**, **en paire avec `b6`**, et sur **relance sérialisée** — cf. §7 « relancer sérialisé ».
Non dû aux correctifs T2.5 (additifs, aucune shared-function modifiée).

**Écart constaté pendant l'exécution** (journalisé, pas masqué) : la v2 affirmait « aucune
modification du composant `BackButton` » pour T3.1. Le code a montré que `fallbackTo` était
inatteignable tant que l'historique était supposé exploitable ; la correction reste additive
(prop optionnelle, défaut = comportement d'avant verrouillé par test). Leçon pour la suite :
*vérifier dans le composant, pas dans la signature de prop*.

**Section dérogations à A1 (aucune suppression)** — remplir pour chaque retrait : fichier, ligne,
raison de force majeure, preuve d'exécution, **accord humain horodaté**. Sans cette ligne, une
suppression est une violation du plan.

**Définition de fini :** les 3 problèmes résolus **sans qu'aucun comportement antérieur ne change**
(tests de non-régression R1/R5/R7 au vert), gates complets sur `main`, recette web **et** mobile
exécutée (T5.1), docs d'avancement du dépôt à jour (`PROGRESSION.md`, `STATUS.md`), et ce fichier
journalisé tâche par tâche.

---

## Annexe A — Faits mesurés le 2026-10-09 (à revérifier, ils bougent)

- `main` = `origin/main` = `ffd7eb93` ; 0 modif non committée dans les 5 worktrees ; 3 330 fichiers
  suivis, identiques côté GitHub ; 0 commit orphelin (`git fsck --dangling`).
- Web : 261 pages ; `BackButton` importé par **un seul** fichier (le layout) ; 41 pages avec contrôle local ;
  `AuthLayout` : **0** contrôle de retour.
- Mobile : **143** `context.go(` / **48** `context.push(` ; `DetailBackButton` utilisé par **3** écrans ;
  243 fichiers avec `AppBar(`, 76 avec contrôle de retour explicite.
- i18n : 6 locales web (8 fichiers dans `frontend/src/i18n/`) ; 6 fichiers `.arb` dans `mobile/lib/l10n/`.

## Annexe B — Inventaire mobile à produire (pré-requis de T4.0)

```bash
cd /home/arise/discipolat/discipolat_app/mobile/lib && grep -rn "context.go(" --include=*.dart . \
  | sed 's#^\./##' > /tmp/mobile_go_calls.txt && wc -l /tmp/mobile_go_calls.txt
```

Classer chaque occurrence : `ENTREE_ESPACE` (garder `go()`), `SOUS_ECRAN` (→ `push()`),
`POST_AUTH` (garder `go()`). Un commit par conversion, un widget test par conversion.

## Annexe C — Ce qu'il NE FAUT PAS faire (anti-patterns identifiés pendant l'audit)

1. Ajouter un bouton de retour dans les 220 pages qui héritent déjà de `MainLayout.tsx:102-108`.
2. Modifier `applyBranding` pour la rendre « scopée » → créer `applyScopedBranding` à côté (A6).
3. Rendre le choix d'église obligatoire à la connexion, ou lui faire filtrer `LoginRequest` (D5).
4. Envoyer `tenantSlug` au serveur comme preuve d'appartenance depuis le sélecteur (D1/R1).
5. Étendre `/public/churches` (contrat en production) au lieu d'ajouter `/suggest` et `/exists`.
6. Exposer `/pages/:slug` en public pour faire une landing d'église (c'est une route protégée).
7. Convertir les 143 `context.go()` en `push()` d'un coup (D6/R6).
8. Indexer (`og:` oui, `robots` non) les landing par église sans `noindex` tant que le double
   opt-in n'est pas prouvé par test (R2).
9. Traduire dans la seule locale `fr` (R9).
10. « Nettoyer » un doublon de bouton de retour sans ligne de dérogation au §8 (A1).
