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
