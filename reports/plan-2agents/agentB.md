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
