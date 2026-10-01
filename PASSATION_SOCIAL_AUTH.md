# PASSATION — Connexion Google + Microsoft (gratuit, illimité)

> **Branche** : `feat/social-auth-google-microsoft` — **1 commit**, `39d45a77`
> **Base** : `main` @ `f2769ab0` (jamais modifiée)
> **État** : code terminé et **vérifié** backend + frontend ; mobile vérifié
> `analyze` + tests mais **build APK non validé**. **Reste : merger + push.**
> `main` n'est **pas protégé** sur GitHub (vérifié via l'API) : le push direct
> fonctionnera.

---

## 1. Ce qui est TERMINÉ et vérifié

| Couche | Vérification | Résultat |
|---|---|---|
| Backend | `mvn test` (JDK 21.0.12, Testcontainers PostgreSQL) | **1771 tests, 0 échec, 0 erreur**, 13 skip |
| Backend | `mvn compile` | BUILD SUCCESS |
| Frontend | `npx tsc --noEmit` + `npx tsc -b` (via build) | 0 erreur |
| Frontend | `npx vitest run` (58 fichiers) | **444 tests verts** |
| Frontend | `npm run build` | `✓ built in 45.98s`, `dist/index.html` produit |
| Mobile | `flutter analyze` (2 fichiers touchés) | `No issues found` |
| Mobile | `flutter test test/features/auth/social/` | **9 tests verts** |
| Config | `render.yaml` parsé par PyYAML | YAML valide |
| Secrets | grep `AIza*` / `-----BEGIN PRIVATE` sur les fichiers du chantier | aucun |

**57 fichiers, +6219 / −269 lignes.** 80 tests nouveaux (57 backend, 14 frontend,
9 mobile).

### Commande de reproductibilité (backend)

```bash
cd backend
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.12+1.1-tem"   # JDK 21 obligatoire
mvn -B -o test
```

> ⚠️ **JDK 25 casse les tests** (`byte-buddy`/Mockito non supporté > 24). Le
> `JAVA_HOME` par défaut de la machine est le 25. **Toujours** passer par le 21.

> ⚠️ La machine est **partagée** (autres agents, ~30 conteneurs Docker). Les
> tests Testcontainers échouent (`Timed out waiting for log output matching
> '.*database system is ready to accept connections'`) quand la charge est
> forte. C'est un faux négatif : relancer quand `uptime` est bas suffit.

---

## 2. RESTE À FAIRE — dans cet ordre

### ☐ 2.1 Merge + push (le seul blocage dur)

```bash
cd /home/arise/discipolat/discipolat_app
git checkout main
git merge --ff-only feat/social-auth-google-microsoft
git push origin feat/social-auth-google-microsoft main
```

Puis **surveiller la CI** (`gh run watch`) car `main` déclenche aussi
`.github/workflows/ci.yml` → *Deploy to Render*. Si l'APK échoue (voir 2.2),
la prod n'est pas redéployée : c'est sans conséquence pour la prod.

> Le merge est un **fast-forward** (`main` n'a pas bougé), donc pas de conflit
> possible.

### ☐ 2.2 Valider le build Android (EN COURS, résultat inconnu)

Un `flutter build apk --debug --no-pub` avait été lancé et **n'a pas terminé**
lors du changement d'agent. Le journal est vide (Flutter bufferise) :
`/tmp/sa-apk-build.log`. À relancer :

```bash
cd mobile && flutter build apk --debug --no-pub 2>&1 | tail -30
```

**Risque identifié, à surveiller** : `android/app/build.gradle.kts` fixe
`jvmTarget = JVM_11` et `sourceCompatibility = VERSION_11`. Les deux nouvelles
dépendances (`google_sign_in` 7.2.0, `flutter_web_auth_2` 5.1.0) sont récentes et
peuvent exiger **JVM 17**. Si le build échoue sur ce point, corriger ainsi :

```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    isCoreLibraryDesugaringEnabled = true
}
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

C'est le **seul** point non vérifié du chantier. `flutter analyze` et
`flutter test` ne couvrent pas la compilation Gradle/manifeste Android.

### ☐ 2.3 Créer les identifiants chez Google et Microsoft (ACTION HUMAINE)

Aucune connexion sociale ne fonctionnera tant que ce n'est pas fait. Le code est
fail-closed : sans identifiant, `GET /auth/social/providers` renvoie une liste
vide et **aucun bouton ne s'affiche** (comportement voulu, pas un bug).

Détail exact des écrans à remplir : **`docs/AUTH_SOCIAL.md` §6**.

- Google : 3 clients OAuth (**Web**, **Android**, **iOS**) — les trois sont
  nécessaires, sinon le mobile ne peut pas se connecter (l'`aud` du jeton est le
  client-id de la plateforme émettrice). Passer l'écran de consentement en
  **Production**. Scopes `email`+`profile` = non sensibles, pas de revue Google.
- Microsoft : 1 enregistrement d'application, permissions `openid`/`email`/
  `profile`, URI de redirection mobile
  `com.discipolat.app://auth/microsoft`. **Aucun secret client** (application
  publique + PKCE). Renseigner `SOCIAL_MICROSOFT_TENANT_ID` (sinon `common`
  accepte toutes les organisations — l'audit de démarrage le signale).

### ☐ 2.4 Variables d'environnement à définir (Render + build)

```bash
# API (prod ET beta — voir render.yaml, déjà déclarés en `sync: false`)
SOCIAL_GOOGLE_ENABLED=true
SOCIAL_GOOGLE_WEB_CLIENT_ID / _ANDROID_ / _IOS_
SOCIAL_MICROSOFT_ENABLED=true
SOCIAL_MICROSOFT_CLIENT_ID / _TENANT_ID
SOCIAL_ALLOW_ACCOUNT_LINKING=true
SOCIAL_REQUIRE_VERIFIED_EMAIL=true

# Frontend (prod ET beta)
VITE_GOOGLE_CLIENT_ID / VITE_MICROSOFT_CLIENT_ID / VITE_MICROSOFT_TENANT_ID
```

> 🚨 **Piège à ne pas réintroduire** : les propriétés backend sont
> `app.auth.social.google.web.client.id` — **sans tiret**. Avec un tiret,
> Spring **ignore silencieusement** la variable d'env et tout répond 503.
> Commenté dans `application.yml` et `SocialAuthProperties`.

### ☐ 2.5 Validation terrain de Microsoft sur mobile

Le flux PKCE passe par le navigateur système : **impossible à tester ici** (pas
d'appareil). À faire sur un vrai Android/iOS avant d'activer le bouton
Microsoft sur mobile. Google n'a pas cette réserve (SDK natif
`google_sign_in`).

`docs/AUTH_SOCIAL.md` §9 déclare déjà cette limite honnêtement.

---

## 3. Architecture livrée (pour ne pas la casser)

**Règle de governance :** un credential externe prouve *qui* la personne est,
**jamais** *ce qu'elle a le droit*.

| Endpoint | Effet compte | Effet rôle/église |
|---|---|---|
| `POST /auth/social/{provider}` | authentifie | aucun |
| `POST /auth/social/link` | rattache au compte connecté | aucun |
| `POST /admin/invitations/accept-identity/{token}` | crée si invitation | **rôle+tenant de l'invitation** |

Il n'existe **aucun** endpoint « créer un compte depuis un credential social » —
c'était le défaut de `SocialAuthController` (créait un `User` sans tenant alors
que `users.tenant_id` est `NOT NULL` depuis `V70`).

### Fichiers clés

| Fichier | Rôle |
|---|---|
| `authentication/domain/SocialIdentityVerifier.java` | contrat + `VerifiedIdentity` + `SocialCredentialException` |
| `authentication/domain/OidcSocialIdentityVerifier.java` | vérification JWKS (Google + Microsoft) |
| `authentication/domain/SocialIdentityService.java` | **unique** point de décision (résolution, linking) |
| `authentication/domain/SocialInvitationAcceptanceService.java` | acceptation d'invitation par identité |
| `authentication/domain/SocialProvider.java` | enum fermée (GOOGLE, MICROSOFT) |
| `authentication/config/SocialAuthProperties.java` | config bindable + règles de cohérence |
| `users/domain/UserIdentity.java` | identité, **sans** `tenant_id`, **sans** filtre |
| `api/AuthResponseFactory.java` | charge utile unique (social = login) |
| `db/migration/V206__user_identities.sql` | clé `(provider, subject)`, pas l'email |
| `frontend/src/features/auth/social/` | module web (types, providers, google, microsoft, composant) |
| `mobile/lib/features/auth/social/` | module mobile (contrat + sources Google/Microsoft) |

### Contraintes à respecter pour toute évolution

1. **Audience Google** : les 3 client-id doivent rester acceptés ensemble.
2. **Liste blanche Microsoft** : elle s'**ajoute** au tenant principal, elle ne
   le remplace pas (bug réel corrigé, verrouillé par
   `allowListDoesNotReplacePrimaryTenant`).
3. **`OidcSocialIdentityVerifier` a 2 constructeurs** : le public porte
   `@Autowired`. Sans lui → `No default constructor found` → **cascade de
   269 erreurs** sur tous les tests Spring. Bug rencontré et corrigé.
4. **`SocialAuthProperties` ne doit jamais recevoir de tiret** dans ses clés.
5. **CSP** (`render.yaml` + `frontend/nginx.conf`) doit garder
   `accounts.google.com` et `login.microsoftonline.com`, sinon ça casse
   silencieusement en prod.

---

## 4. Ajouts ultérieurs possibles (hors périmètre, documentés)

| Option | Coût | Note |
|---|---|---|
| Apple | 99 $/an | ajouter à `SocialProvider` + properties + verifier ; prévoir la restitution partielle de l'email (d'où la clé `subject`). Obligatoire App Store (règle 4.8) dès qu'un autre login est proposé dans l'app iOS. |
| Téléphone (OTP) | SMS facturé | `SmsGateway` (Twilio) existe mais sert aux notifications. Reste à écrire : code 6 chiffres **haché**, TTL 5 min, 3 tentatives, quotas par IP **et** par numéro, normalisation E.164. |
| Déporter les jetons hors `localStorage` | — | Pré-existant, hors de ce chantier. Cookies `httpOnly` = **server-side**. |

---

## 5. Fichiers modifiés (liste pour relecture)

**Ajoutés (18)** : migration `V206`, `SocialProvider`, `SocialIdentityVerifier`,
`OidcSocialIdentityVerifier`, `SocialIdentityService`,
`SocialInvitationAcceptanceService`, `SocialAuthProperties`,
`SocialAuthConfiguration`, `SocialAuthStartupAudit`, `AuthResponseFactory`,
`UserIdentity`, `UserIdentityRepository`, 3 tests backend, `docs/AUTH_SOCIAL.md`,
module web `features/auth/social/` (5 fichiers), test frontend
`SocialLoginButtons.test.tsx`, module mobile `features/auth/social/` (3 fichiers),
test mobile, widget mobile `social_login_buttons.dart`.

**Modifiés (39)** : `pom.xml` (+`spring-security-oauth2-jose`), `application.yml`,
`SecurityConfig`, `PerIpRateLimiter`, `AuthService`, `AuthController`,
`SocialAuthController`, `InvitationController`, `DomainException`
(+`getCode()`), `render.yaml`, `.env.example`, `frontend/nginx.conf`,
`LoginPage`, `AcceptInvitationPage`, 6 fichiers i18n (15 clés chacun),
`pubspec.yaml`, `login_screen.dart`, 4 tests existants ajustés.

> **Rien n'a été supprimé.** `POST /auth/google` est conservé avec son ancien
> format de réponse ; les boutons Apple et Facebook qui n'affichaient que
> « bientôt disponible » ont été **remplacés** par Microsoft (fonctionnel) — seul
> écart à « ne rien supprimer », assumé : un bouton mort est une promesse
> d'interface non tenue.
