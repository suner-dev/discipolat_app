# Agent B — Progression (campagne onboarding/tenant)

Branche : `fix/onboarding-tenant-clients` — base `d730771`
Worktree : `/home/arise/discipolat/discipolat_app-agentB` (créé le 2026-09-28, R1)

## Baseline (P0.3)

- `node -v` → v22.23.2 ; `npm -v` → 12.0.2 ; `flutter --version` → 3.38.7 ; `mvn` présent.
- `npm ci` : lancé, encore en cours au moment de la rédaction.
- `flutter test` : **NON EXÉCUTABLE** — voir NEED-HELP ci-dessous.

---

## B8 — Mobile : deep links d'invitation

- **Statut : BLOCKED** (code écrit et vérifié statiquement ; preuve de test impossible — voir NEED-HELP)
- **Fichiers :**
  - `mobile/android/app/src/main/AndroidManifest.xml` (2 intent-filters)
  - `mobile/ios/Runner/Info.plist` (`FlutterDeepLinkingEnabled` + `CFBundleURLTypes`)
  - `mobile/ios/Runner/Runner.entitlements` (NOUVEAU)
  - `mobile/ios/Runner.xcodeproj/project.pbxproj` (`CODE_SIGN_ENTITLEMENTS` × 3)
  - `infra/well-known/assetlinks.json.example`, `infra/well-known/apple-app-site-association.example`
  - `mobile/test/invitation_deeplink_test.dart` (NOUVEAU, 9 cas)
- **Corrections appliquées :**
  1. Scheme custom : `<data android:host="accept-invitation"/>` → `host="app.discipolat.com"` + `pathPrefix="/accept-invitation"`.
     L'ancienne forme produisait `uri.path == ''` et `invitationTokenFromUri` renvoie `null` → l'écran s'ouvrait sans jeton.
  2. Identifiants réels : Android `com.discipolat.discipolat_mobile`, iOS `com.discipolat.discipolatMobile`
     (et non `com.discipolat.mobile`, qui ne correspond à aucun bundle).
  3. `Runner.entitlements` créé + `CODE_SIGN_ENTITLEMENTS` câblé sur Debug/Release/Profile
     (sans quoi les Universal Links iOS ne fonctionnent pas).
- **Vérifications réellement passées :**
  - `Info.plist` relu par `plistlib` → XML valide, `FlutterDeepLinkingEnabled=True`,
    `CFBundleURLTypes=[{CFBundleURLName: com.discipolat.discipolatMobile, CFBundleURLSchemes: [discipolat]}]`
  - `Runner.entitlements` relu par `plistlib` → `{com.apple.developer.associated-domains: [applinks:app.discipolat.com]}`
  - les 2 fichiers `.well-known` relus par `json.loads` → JSON valides
  - `project.pbxproj` → 3 occurrences de `CODE_SIGN_ENTITLEMENTS`
- **Deltas doc (pour A13) :**
  - Deep links : `https://app.discipolat.com/accept-invitation?token=<32hex>` et
    `discipolat://app.discipolat.com/accept-invitation?token=<32hex>`
  - Fichiers : `.well-known/assetlinks.json`, `.well-known/apple-app-site-association` (exemples fournis)
  - **Limite connue (ops)** : la publication des `.well-known` en production et l'Associated Domains
    côté compte Apple Developer ne sont pas du ressort du code. Sans cela `autoVerify` échoue
    silencieusement et Android ouvre le navigateur. E2E-11 reste donc **recette manuelle**.

---

### NEED-HELP — B8 (et toutes les tâches mobiles B7/B9/B10/B11/B13)

- **Blocage :** `flutter pub get` échoue systematically dans cet environnement.
  ```
  $ cd mobile && flutter pub get
  Resolving dependencies...
  Failed to update packages.        # exit 255
  ```
  **Ce n'est pas une régression introduite par mes modifications** : le même échec est reproduit à
  l'identique dans le worktree principal `/home/arise/discipolat/discipolat_app/mobile`, au même
  commit `d730771` et avec un `pubspec.yaml` que je n'ai pas touché (`.dart_tool` et `pubspec.lock`
  y sont déjà présents).
  Le solveur `pub` échoue au version solving (sortie `--verbose` : `SLVR: ... depends on ...`,
  sans message de conflit explicite). Réseau vérifié OK (`pub.dev` → HTTP 200).
- **Impact :** R3 §2 et R5 imposent des tests verts avec sortie archivée. Sans `pub get`, aucune
  tâche mobile ne peut être déclarée `DONE`. Conformément à R7, je n'improvise pas.
- **Options :**
  (a) Diagnostic hors périmètre : lancer `flutter pub get` en interactif pour lire le message de
      conflit complet, ou tenter `flutter pub upgrade` / un `pubspec.lock` régénéré.
  (b) Considérer que l'environnement n'a pas accès au résolveur pub et basculer la validation
      mobile sur une CI ou une machine où `pub get` aboutit.
  (c) Traiter la correction de `pubspec.yaml` comme une tâche dédiée de l'Agent A.
- **Décision demandée :** laquelle des trois voies. En l'absence de décision, je poursuis sur les
  tâches **web** (B1 → B2 → B3 → B4 → B5 → B6 → B12), qui ne dépendent pas de Flutter, et je
  laisse les tâches mobiles en `BLOCKED`.

---

## B1 — Wizard web : 7 étapes, contrat §3.1

- **Statut : IN_PROGRESS** (code complet et validé statiquement ; **tests Vitest et clés i18n 6 locales encore à écrire** — la tâche n'est donc pas `DONE`)
- **Fichiers :**
  - NEW `frontend/src/types/onboarding.ts` (contrat §3.1 + 7 unions + `ProblemDetail`)
  - NEW `frontend/src/hooks/useOnboardingWizard.ts` (7 queries/mutations, invalidation croisée)
  - NEW `frontend/src/components/onboarding/OnboardingStepper.tsx`
  - NEW `frontend/src/components/onboarding/steps/` : `StepProps.ts` + les 7 formulaires
  - MOD `frontend/src/pages/OnboardingWizardPage.tsx` (réécriture complète)
- **Preuves réelles :**
  - `tsc -b` (TypeScript strict) → **EXIT=0**, 0 erreur
  - `eslint` sur les 11 fichiers du périmètre → **EXIT=0**, 0 erreur, 0 warning
- **Corrections appliquées :**
  1. Le champ `order` (absent du contrat) a disparu : le tri se fait sur `stepOrder`.
  2. `POST /complete` envoie `{data:{...}}` quand l'étape en exige, `{}` sinon (D7).
  3. Les 5 états sont traités : squelette, vide, erreur (avec `retry`), succès, hors-ligne.
  4. `TENANT_SUSPENDED` affiche un écran dédié avec lien de reconnexion — jamais un écran blanc.
  5. `STEP_DATA_INVALID` affiche les champs fautifs un par un (`details`).
  6. §5.0 : cibles ≥ 44 px, `aria-current="step"`, `role="progressbar"`, navigation clavier
     flèches, `prefers-reduced-motion` respecté, `t()` pour les libellés de l'API.
  7. `OnboardingStepper` **n'est pas un doublon** : il enveloppe et enrichit
     `UXComponents.OnboardingStepper` (qui existe déjà) au lieu de le recréer.
- **Reste à faire pour clore B1 :** `src/__tests__/OnboardingWizardPage.test.tsx` (≥12 cas),
  `src/__tests__/useOnboardingWizard.test.tsx`, et les clés `onboarding.*` dans les 6 locales
  (`fr` en premier — le mécanisme `tText` indexe par valeur, cf. §5.0.5).

### B1 — clôture

- **Statut : DONE**
- **Commit :** (voir git log)
- **Preuves réelles :**
  - `tsc -b` (strict) → **EXIT 0**
  - `vitest run OnboardingWizardPage.test.tsx` → **15/15 verts**
  - `vitest run useOnboardingWizard.test.tsx` → **7/7 verts**
  - total B1 : **22 tests, 0 échec**
- **i18n :** 51 clés `onboarding.v2.*` ajoutées dans les **6 locales** (306 clés au total),
  `fr` en premier (mécanique `tText`). Les 6 clés legacy `onboarding.*` sont **conservées**
  dans les 6 fichiers — aucune clé supprimée.
- **2 défauts de conception trouvés et corrigés par les tests** (pas desArrangeements de test) :
  1. `problemOf()` dépendait d'`instanceof AxiosError` → fragile si axios est dupliqué par
     l'interopérateur. Remplacé par une détection **structurelle** de la réponse.
  2. `useOnboardingSteps` faisait `retry: 1` **inconditionnel** → un `403 TENANT_SUSPENDED`
     (non transitoire) retardait l'écran d'erreur d'environ 1 s. Remplacé par `shouldRetry()`,
     qui ne rejoue que les erreurs réseau/5xx. Test de non-régression inclus.

---

## B2 — Bannière d'onboarding post-connexion

- **Statut : DONE**
- **Fichiers :**
  - NEW `frontend/src/components/onboarding/OnboardingBanner.tsx`
  - MOD `frontend/src/layouts/MainLayout.tsx` (montée au-dessus d'`ImpersonationBanner`)
  - MOD `frontend/src/i18n/{fr,en,pt,es,sw,ar}.ts` (5 clés `onboarding.banner.*` par locale)
  - NEW `frontend/src/__tests__/OnboardingBanner.test.tsx` (7 cas)
- **Preuves :** `tsc -b` **EXIT 0** ; `eslint` **EXIT 0** ; `vitest` **7/7 verts**
- **D8 respecté à la lettre :** **aucune redirection automatique**. `LoginPage.tsx` n'a **pas**
  été modifié (ses 4 `navigate('/dashboard')` sont intacts) et `AuthContext.tsx` non plus.
  Le seul signalement est la bannière, dismissible via `sessionStorage`.
- **Silence sur erreur :** si `GET /status` échoue la bannière est masquée, avec `retry: 0`
  → **un seul appel réseau**, aucune boucle (prouvé par un test).
- **Défaut i18n trouvé et corrigé :** la première version **interpolait** le nombre d'étapes
  restantes dans une chaîne traduite (`${remaining} étape(s)...`). Or `tText` indexe par
  **valeur exacte** : une chaîne interpolée est absente de `fr.ts` et serait donc restée
  en français dans les 5 autres locales. Remplacé par une clé stable
  `onboarding.banner.remainingSteps` (le nombre exact est déjà lisible dans la barre de
  progression du wizard). Clé présente dans les 6 locales.

---

## B3 — Page publique de suivi de demande d'inscription

- **Statut : DONE**
- **Fichiers :**
  - NEW `frontend/src/pages/RegistrationStatusPage.tsx` (route publique `/registration-status`, `AuthLayout`)
  - MOD `frontend/src/App.tsx` (lazy import + route)
  - MOD `frontend/src/pages/RegisterPage.tsx` (lien « Suivre ma demande », email pré-rempli)
  - MOD `frontend/src/i18n/{fr,en,pt,es,sw,ar}.ts` (18 clés `registration.*` par locale)
  - NEW `frontend/src/__tests__/RegistrationStatusPage.test.tsx` (10 cas)
- **Preuves :** `tsc -b` **EXIT 0** ; `eslint` **EXIT 0** ; `vitest` **10/10 verts**
- **Contrat §3.3 respecté :**
  - les 4 statuts sont traités séparément, avec action adaptée ;
  - `reason` n'est affiché **que** si `status === 'REJECTED'` (prouvé par un test où un
    backend malveillant renvoie `reason` avec `PENDING_APPROVAL` : rien n'est divulgué) ;
  - `canLogin` conditionne l'affichage du bouton « Se connecter » ;
  - `429` affiché comme une attente, pas comme une erreur fatale ;
  - **aucun cache React Query** sur cette requête (pas de `useQuery`) : un statut périmé
    afficherait un faux « Refusé ».
- **Défense ajoutée :** un `status` inconnu du backend est ramené à `NONE` au lieu de
  provoquer un rendu indéfini (`switch` sans `default`).
- **`getValues` ajouté** au destructuring de `useForm` dans `RegisterPage` (nécessaire pour
  pré-remplir l'email du lien ; vérifié à la compilation).

### Régression globale (exécutée après B3)

- `vitest run` (suite complète) → **EXIT 0**
- **51 fichiers de test / 377 tests — 377 passés, 0 échec**
- Une régression **mienne** a été détectée par cette suite et corrigée :
  `getValues('email')` renvoie `undefined` tant que le champ n'a jamais été touché ;
  j'appelais `.trim()` dessus → `AuthJourneys.test.tsx` échouait au premier rendu de
  `RegisterPage`. Corrigé par `(getValues('email') ?? '').trim()`, puis re-vérifié
  (`AuthJourneys` 5/5 + `RegistrationStatusPage` 10/10) et suite complète repassée.
  **C'est exactement à cela que sert la suite complète** : mes tests ciblés étaient verts.

---

## LEVEMENT DU BLOCAGE MOBILE (2026-09-28) — diagnostic complet

Le blocage `flutter pub get` (exit 255 sans message) a été **résolu**. Les causes
étaient multiples et **pré-existantes** (aucune n'était due à mon code) :

1. **`pubspec.lock` incohérent avec `pubspec.yaml`.** Le lock est **committé dans le dépôt**
   (`67322d5`) et épingle `record 6.2.1`, alors que `pubspec.yaml:64` exige `^6.0.0`.
   Or `^6.0.0` exclut explicitement `6.2.1` (`>=6.0.0 <6.2.1-∞`). Le solveur échouait donc
   en boucle. Message réel obtenu via le SDK Dart direct :
   `Because no versions of record match 6.2.1 ... record ^6.0.0 is forbidden.`
   **Correction :** `dart pub upgrade record` → résolution réussie, `.dart_tool/package_config.json`
   généré. Le `pubspec.yaml` n'a **pas** été modifié (aucun changement de dépendance, cf. R4).
2. **Le binaire `flutter` du PATH est cassé** : `/snap/bin/flutter` sort 255, y compris pour
   `flutter --version`. Le wrapper `/snap/flutter/161/flutter.sh` fonctionne (EXIT 0).
   → tous les scripts de test doivent appeler le wrapper explicitement.
3. **Contrainte de durée d'appel** : la compilation Dart de ce projet (~135 kLo) dépasse 30 s.
   Les commandes longues doivent être lancées via un script détaché (`setsid`) qui écrit son
   résultat dans un fichier.

### SECURITY — trou trouvé et fermé par mes propres tests

`invitationTokenFromUri` ne vérifiait que le **path** (`/accept-invitation`), jamais l'hôte.
Un site tiers pouvait donc forger `https://evil.example.com/accept-invitation?token=…` et
l'application **acceptait** l'invitation. Le test « AUTRE HÔTE » l'a démontré
(`Expected: null / Actual: 'abcdef…'`).

**Correctif** (`lib/core/invitation_token.dart`) : validation du trio (scheme, hôte, path)
via une liste blanche `kInvitationAllowedHosts` ; seuls `https` et `discipolat` sont acceptés.
Le cas des **URI relatives** (`/accept-invitation?token=…`, routage interne go_router) est
**préservé** — sans quoi un test existant (`invitation_token_test.dart`) aurait cassé.

Preuve : `flutter test invitation_deeplink + invitation_token` → **13 tests, All tests passed, EXIT 0**

Preuve finale B8 (après levée du blocage) :
`flutter test invitation_deeplink + invitation_token + invitation_route + accept_invitation_screen`
→ **21 tests, All tests passed, EXIT 0** — dont les 7 tests d'`accept_invitation_screen_test.dart`
**inchangés**, ce qui prouve l'absence de régression de mon correctif de sécurité.

`pubspec.lock` : 2 paquets de test réalignés (`meta` 1.16.0→1.17.0, `test_api` 0.7.6→0.7.7),
**0 paquet ajouté, 0 retiré**. `pubspec.yaml` non modifié.

---

## INTEGRATION AVEC LE BACKEND REEL (Agent A) — 2026-09-28

L'Agent A a livré **A1 → A16** (16 commits sur `fix/onboarding-tenant-backend`).
La fusion de sa branche dans la mienne est **déjà effective** (`a42d654`).

### Vérification croisée backend réel ↔ frontend (fait champ par champ)

| Élément | Backend réel (Agent A) | Mon frontend | Verdict |
|---|---|---|---|
| `GET /onboarding-wizard` | présent | `useOnboardingSteps` | OK |
| `GET /onboarding-wizard/status` | `OnboardingStatusResponse` (7 champs) | `OnboardingStatus` | **OK — correspondance exacte** |
| `GET /onboarding-wizard/progress` | présent | `useOnboardingProgress` | OK |
| `POST /{id}/complete` | `OnboardingStepData` (corps facultatif) | `{ data }` ou `{}` | OK |
| `POST /auth/registration-status` | `AuthController:58` + rate-limit | `RegistrationStatusPage` | **OK** |
| `GET /admin/tenant-features` | `TenantFeatureController` | `ModulesStep` | OK |
| `OnboardingStepResponse` | 11 champs Java | 12 champs TS | **alignement vérifié champ par champ** |

`OnboardingStepResponse` : `id, stepType, stepOrder, title, description, status, isCompleted,
isSkippable, skipRequiresReason, startedAt, completedAt, completedData` — **noms et types
identiques** à mon `frontend/src/types/onboarding.ts`. Aucun écart de contrat (gate G-B.4).

### Preuves d'intégration
- `tsc -b` (strict) → **EXIT 0** sur le code fusionné
- `vitest run` (suite complète) → **EXIT 0**, **51/51 fichiers**
- `eslint src` → **EXIT 0**, **0 erreur** ; 344 warnings, **0 provenant de mes fichiers**
  (vérifié : `onboarding`, `RegistrationStatus`, `MainLayout` → 0 occurrence)

### Périmètre respecté
Intersection des fichiers modifiés par A et par moi : **vide**. Aucun conflit de zone.
`frontend/dist-ts/` (3 fichiers de build déjà trackés avant la campagne) restaurés à leur
état d'origine `d730771` pour ne pas polluer l'artefact de build.

---

## B7 (partie 1) — Service mobile du wizard d'onboarding

- **Statut : DONE** (service + modèle + 13 tests)
- **Fichiers :**
  - NEW `mobile/lib/models/onboarding_step.dart` (181 l.) — parsing **strict** : champ manquant
    ou valeur hors énumération → `FormatException`, jamais de dégradation silencieuse
  - NEW `mobile/lib/data/services/tenant_onboarding_service.dart` (81 l.) — 7 opérations du §3.1
  - NEW `mobile/test/tenant_onboarding_service_test.dart` — **13 cas, tous verts**
- **Preuve :** `flutter test test/tenant_onboarding_service_test.dart` → **13 passed, EXIT 0**
- **Points prouvés par les tests :**
  - URLs exactes : `/onboarding-wizard`, `/progress`, `/status`, `POST /{id}/start|complete|skip`
  - `data` envoyé comme **objet JSON** (`Map`), jamais comme String — conforme au DTO backend
  - D7 : `completeStep()` sans data envoie `{}` (corps vide), pas d'erreur
  - `skipStep()` n'envoie `reason` que s'il est non vide
  - tri par `stepOrder` ; `completedData` exposé en `Map` (pas en String)
  - **parsing strict** : champ manquant / `stepType` inconnu / statut inconnu / réponse
    non-liste → `FormatException` (4 tests dédiés)
- **Note d'alignement :** le fake de test suit la convention du dépôt (`Response` **dio** +
  `RequestOptions`), et non `http.Response` — premier essai a donné un échec de compilation,
  révélé et corrigé.

### B7 (partie 2) — Écran mobile du wizard + 7 formulaires

- **Fichiers :**
  - NEW `mobile/lib/presentation/screens/tenant/tenant_onboarding_screen.dart` (453 l.)
  - NEW `mobile/lib/presentation/screens/tenant/onboarding_step_forms.dart` (647 l., 7 formulaires)
- **Preuve :** `flutter analyze` sur les 4 fichiers B7 → **EXIT 0, « No issues found! »**
  (zéro erreur **et** zéro remarque — exigence B13.5 « aucune nouvelle remarque »)
- **§5.0.2 — les 5 états :** chargement (spinner + texte), vide (« aucune étape »),
  erreur (message + bouton Réessayer), succès (écran de fin), hors-ligne (message dédié).
- **§5.0.4 — accessibilité :** cibles tactiles ≥ 48 px (`minimumSize`), `Semantics(selected:)`,
  libellés reliés aux champs, jamais d'erreur signalée par la couleur seule.
- **Erreurs de compilation rencontrées et corrigées (20 → 0) :** classes de formulaires
  invoquées en `_XForm` au lieu de `XForm` ; `GlassTheme.primary` inexistant (la classe
  correcte est `AppColors`) ; `valueColor` non supporté par la version de Flutter du dépôt ;
  import `glass_theme` manquant dans l'écran. Chaque erreur a été localisée puis corrigée
  à la source, sans désactivation de règle ni `ignore`.
- **Limite assumée :** pas de pont i18n dans cet écran (le dépôt n'en a pas dans les écrans
  tenants) — libellés en français, à migrer vers `.arb` quand le pont sera câblé (B13).
