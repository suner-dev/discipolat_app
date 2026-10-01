# Connexion par identité externe — Google & Microsoft

> Statut : implémenté sur `feat/social-auth-google-microsoft`.
> Fournisseurs retenus : **Google**, **Microsoft** et **Facebook** — tous
> **gratuits et sans plafond côté serveur** (vérification locale du JWT).
> Apple et le téléphone sont volontairement hors périmètre (payants), voir §7.
> LinkedIn, Telegram et VK ont été **évalués et écartés**, raisons en §7.1.

---

## 1. Le principe qui gouverne tout

**Un credential externe prouve *qui* la personne est. Il n'accorde jamais un rôle
ni une église.**

Concrètement, il n'existe que trois entrées, et la création libre de compte en
est absente par construction :

| Entrée | Effet sur le compte | Effet sur le rôle / l'église |
|---|---|---|
| `POST /auth/social/{provider}` | authentifie un compte existant | aucun |
| `POST /auth/social/link` | rattache une identité au compte **connecté** | aucun |
| `POST /admin/invitations/accept-identity/{token}` | crée le compte si l'invitation l'exige | **rôle et tenant de l'invitation**, jamais du fournisseur |

> **Pourquoi cette règle.** Le modèle produit est « demande d'église →
> approbation Super Admin » (`POST /auth/register` ne crée rien). Une inscription
> sociale libre aurait créé un compte actif sans église — impossible ici
> (`users.tenant_id` est `NOT NULL`, `V70`) et dangereux (contournement de
> l'approbation). Le code précédent essayait pourtant exactement cela : il construisait
> un `User` sans tenant, donc échouait sur la contrainte SQL pour tout nouvel email.

---

## 2. Pourquoi c'est gratuit **et** illimité

La vérification se fait **localement** : la signature de l'`id_token` est
contrôlée avec les clés publiques du fournisseur (JWKS), via
`NimbusJwtDecoder` (`spring-security-oauth2-jose`).

- **0 €** : aucun appel à un service de vérification payant.
- **Illimité** : aucun appel réseau par connexion. Le cache de `RemoteJWKSet`
  (Nimbus) gère la rotation des clés.
- L'ancien code appelait `oauth2.googleapis.com/tokeninfo` : endpoint **non
  documenté pour la production**, sans SLA, et **non mis en cache**. C'est
  c'est précisément lui qui rendait le service ni fiable ni illimité.

> Firebase Authentication facturerait par MAU (0 $ jusqu'à 50 000 MAU, puis
> payant). Nous n'en avons pas besoin : `firebase-admin` ne sert ici qu'au push.

---

## 3. Contrôles appliqués à chaque credential

1. Fournisseur configuré — sinon **503** (fail-closed, jamais de contournement).
2. Signature **RS256** + `exp`/`nbf` (par le décodeur).
3. `alg` forcé à RS256 : refuse `none` et toute bascule symétrique.
4. `iss` dans la liste du fournisseur.
5. `aud` ∈ {web, Android, iOS} — **les trois** : sans quoi le mobile ne peut
   jamais se connecter, l'audience étant le client-id de la plateforme émettrice.
6. Microsoft : cohérence `iss` ↔ `tid`, puis tenant principal **ou** liste blanche
   (la liste s'**ajoute**, elle ne remplace pas).
7. Email présent, et vérifié selon la politique du fournisseur.

### Politique de vérification d'email

| Fournisseur | `email_verified` absent | `email_verified` = `false` |
|---|---|---|
| Google | **refusé** (403 `SOCIAL_EMAIL_NOT_VERIFIED`) | **refusé** |
| Microsoft | accepté (absent de plusieurs politiques Entra) | **refusé** |

Microsoft sans claim `email` : repli sur `preferred_username` **uniquement** si
la forme est un email et que le fournisseur n'a pas dit « non vérifié ».

### 3 bis. Facebook — deux différences à connaître

**1) Facebook n'émet PAS de claim `email_verified`.**

Il n'est pas dans le document de découverte OIDC de Facebook (contrairement à
Google et Microsoft). Appliquer la règle « `email_verified` obligatoire »
rendrait **toute** connexion Facebook impossible. La politique appliquée est
donc, explicitement :

| Claim | Décision | Justification |
|---|---|---|
| `email_verified: false` | **refusé** (403) | le fournisseur a dit ne pas garantir l'adresse |
| `email_verified` absent | **accepté** | Facebook a vérifié l'adresse à l'inscription du compte |
| `email` absent | **refusé** (403 `SOCIAL_EMAIL_MISSING`) | cause réelle la plus fréquente : permission `email` non accordée, ou profil sans adresse. Notre modèle indexe les comptes par email global : une identité sans email n'a rien où se rattacher. |

L'utilisateur qui refuse l'accès à son email voit donc un message actionnable
(`auth.social.facebookNoEmail`), pas une erreur opaque.

**2) Le SDK JavaScript de Facebook ne renvoie PAS d'`id_token`.**

`FB.login()` renvoie un *access token* — donc non vérifiable localement, ce qui
nous ramènerait au modèle « un appel réseau par connexion » que cette
architecture a précisément écarté. L'unique moyen d'obtenir un **JWT signé par
Facebook** est le flux OIDC implicite (`response_type=id_token`).

Ce flux est **acceptable ici** (et seulement ici) parce que :
- notre backend émet la session Discipolat → **aucun refresh token
  fournisseur** n'est nécessaire ;
- le jeton est court-lived et lié à notre App ID (audience vérifiée) ;
- la signature est validée **côté serveur**, jamais dans le navigateur.

Implémentation : **redirection de page entière** avec relais par `sessionStorage`
(et non popup), car `postMessage` casse avec
`Cross-Origin-Opener-Policy: same-origin` et Meta impose son propre
`X-Frame-Options`. La route `/auth/social/callback` :
1. vérifie le relais — `state` aléatoire de 32 octets + **fenêtre de 5 minutes** ;
2. refuse tout `state` non concordant (protection CSRF) ;
3. consomme le relais et **nettoie le fragment** de l'URL, pour qu'aucun
   `id_token` ne reste dans l'historique du navigateur.

> **Mobile** : le SDK Flutter `flutter_facebook_auth` est **abandonné** (non
> maintenu depuis 2023). Utiliser un SDK non maintenu sur un flux
> d'authentification est un risque de sécurité. Le même flux OIDC par
> redirection est donc utilisé sur mobile, via `flutter_web_auth_2` (officiel,
> maintenu), sans dépendance Meta.

### 3 ter. Ce qu'il faut obtenir de Meta (⚠️ le point bloquant réel)

Facebook est gratuit, mais **ce n'est pas le même genre de gratuité que Google
ou Microsoft** : il y a une **revue** à passer.

| Étape | Conséquence |
|---|---|
| Créer l'application Meta (Consumer) | `email` + `public_profile` sontGranted en **Standard Access** |
| **Standard Access** | fonctionne **uniquement pour les personnes ayant un rôle sur l'application** (développeurs, testeurs). Pas un membre de l'église. |
| **Advanced Access** | Business Verification + **App Review** Meta : cas d'usage, captures d'écran et **au moins un appel API réussi dans les 30 jours** ; app en mode **Live** |
| **Data Use Checkup** | annuel, obligatoire pour conserver l'Advanced Access |

**Conséquence à assumer :** tant que la revue n'est pas passée, Facebook
fonctionne en *test* mais **pas pour de vrais membres**. Ce n'est pas contournable
— et le code ne le contourne pas : il reste fail-closed, l'API répond 503 tant
que l'App ID n'est pas configuré, et la documentation est explicite.

---

## 4. Modèle de données

Migration **`V217__user_identities.sql`** (additive stricte ; renumérotée depuis
V206 — collision avec le port Develop1 qui occupe V206..V216 sur main).

```
user_identities(id, user_id → users, provider, subject, email_at_link,
                picture_url, created_at, last_login_at)
  UNIQUE (provider, subject)
```

Trois décisions structurantes :

1. **Clé = `(provider, subject)`, jamais l'email.** Apple ne restitue l'email
   qu'à la première autorisation ; une table clé-email casserait au 2ᵉ login.
2. **Aucun `tenant_id`, aucun filtre `tenantFilter`.** L'identité appartient au
   *compte*. Un compte peut être membre de plusieurs églises
   (`tenant_membership`) : un filtre multi-tenant la rendrait invisible depuis
   une autre église et ferait échouer le reconnectement. Toutes les requêtes
   partent d'un `user_id` déjà résolu — aucune fuite inter-tenant.
3. **Aucun secret stocké.** Uniquement l'identifiant opaque du fournisseur ;
   `email_at_link` est de la traçabilité RGPD, jamais une clé.

### Mots de passe des comptes « identité seule »

`users.password_hash` est `NOT NULL`. Un compte créé par identité reçoit le hash
BCrypt d'un secret **aléatoire de 256 bits, jamais transmis ni journalisé** : la
connexion par mot de passe est donc impossible sans le mot de passe oublié. Aucun
comportement n'est modifié pour le reste du produit.

---

## 5. API

### Public

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/api/v1/auth/social/providers` | fournisseurs **réellement servis** |
| `POST` | `/api/v1/auth/social/{provider}` | connexion (rate-limit 10/min/IP) |
| `POST` | `/api/v1/admin/invitations/accept-identity/{token}` | accepter une invitation par identité (rate-limit 5/min/IP) |
| `POST` | `/api/v1/auth/google` | **conservé** (compatibilité), delegates to the same service |
| `POST` | `/api/v1/auth/magic-link` | inchangé |
| `GET` | `/api/v1/auth/magic-link/verify` | inchangé (bascule sur `issueSession()`) |

### Authentifié

| Méthode | Route | Rôle |
|---|---|---|
| `POST` | `/api/v1/auth/social/link` | rattache une identité au compte connecté (5/5min/IP) |
| `GET` | `/api/v1/auth/social/identities` | identités du compte |

> Ces deux routes sont explicitement `authenticated()` **avant** le
> `permitAll()` de `/api/v1/auth/**` — sans cela, n'importe qui rattacherait une
> identité à un compte.

### Charge utile

`POST /auth/social/{provider}` renvoie **exactement** la même structure que
`/auth/login` (`AuthResponse`), construite par `AuthResponseFactory` : un seul
constructeur, donc aucun chemin ne peut afficher « Super Admin » sur un chemin et
pas l'autre.

`POST /accept-identity/{token}` renvoie cette structure **plus**
`tenantId`, `alreadyMember`, `crossTenantIdentity`, `welcomeEmailSent` : la
session est ouverte, l'invité n'a rien à ressaisir.

### Erreurs (RFC 7807, `title` = code)

| Code | HTTP | Signification pour l'interface |
|---|---|---|
| `SOCIAL_ACCOUNT_NOT_LINKED` | 403 | proposer le lien d'invitation |
| `SOCIAL_EMAIL_MISMATCH` | 403 | l'identité ne correspond pas à l'invité |
| `SOCIAL_PROVIDER_NOT_CONFIGURED` | 503 | **masquer le bouton** |
| `SOCIAL_TENANT_NOT_ALLOWED` | 403 | organisation non autorisée |
| `SOCIAL_IDENTITY_ALREADY_LINKED` | 409 | déjà rattaché à un autre compte |
| `RATE_LIMITED` | 429 | patience |

Aucun message ne révèle l'existence d'un compte ou l'adresse attendue.

---

## 6. Configuration

### Variables d'environnement

```bash
# Google
SOCIAL_GOOGLE_ENABLED=true
SOCIAL_GOOGLE_WEB_CLIENT_ID=…apps.googleusercontent.com
SOCIAL_GOOGLE_ANDROID_CLIENT_ID=…apps.googleusercontent.com
SOCIAL_GOOGLE_IOS_CLIENT_ID=…apps.googleusercontent.com

# Microsoft
SOCIAL_MICROSOFT_ENABLED=true
SOCIAL_MICROSOFT_CLIENT_ID=<uuid>
SOCIAL_MICROSOFT_TENANT_ID=common          # ou l'ID de votre tenant
SOCIAL_MICROSOFT_ALLOWED_TENANT_IDS=       # optionnel : partenaires

SOCIAL_ALLOW_ACCOUNT_LINKING=true
SOCIAL_REQUIRE_VERIFIED_EMAIL=true
```

> ⚠️ **Piège Spring à connaître.** Les propriétés sont
> `app.auth.social.google.web.client.id` — **sans tiret**. Le binding « relaché »
> ne rapproche pas la forme segmentée d'une propriété à tirets : avec
> `web-client-id`, `SOCIAL_GOOGLE_WEB_CLIENT_ID` serait **silencieusement
> ignorée**, et les endpoints répondraient 503.

### Build frontend

```bash
VITE_GOOGLE_CLIENT_ID=…            # client WEB (aussi utilisé comme serverClientId mobile)
VITE_MICROSOFT_CLIENT_ID=<uuid>
VITE_MICROSOFT_TENANT_ID=common
```

### Build mobile

```bash
--dart-define=GOOGLE_WEB_CLIENT_ID=…
--dart-define=MICROSOFT_CLIENT_ID=…
--dart-define=MICROSOFT_TENANT_ID=common
```

### Console Google Cloud

1. **APIs et services → Identifiants** → Créer → **ID client OAuth 2.0 → Application Web**
   → origines JavaScript autorisées :
   `https://discipolat.onrender.com`, `https://discipolat-beta.onrender.com`,
   `http://localhost:5173`.
2. Répéter pour **Android** (`VITE` : SHA-1 de l'app) et **iOS**.
3. **Écran de consentement OAuth** : passer le statut de publication à
   **En production**. Les scopes `email` + `profile` sont **non sensibles** : pas
   de revue de vérification Google requise.
   - En mode *Test*, 100 testeurs maximum et refresh tokens expirant à 7 jours —
     sans effet ici, car aucun refresh token Google n'est conservé.

### Portail Microsoft Entra

1. **Inscriptions d'applications** → nouvelle registration.
2. **Authentification** → *Application monoplateforme* :
   - Plateforme web : URI de redirection `https://discipolat.onrender.com`
     (non utilisée : le flux retenu est popup/redirect côté client).
   - Plateforme Android + iOS pour le mobile, avec l'URI de redirection
     **exactement** égale à celle du code :
     `com.discipolat.app://auth/microsoft`.
3. **Certificats et secrets** : **rien à créer**. Application publique + PKCE :
   aucun secret client. Le backend ne stocke que le client-id, qui est public.
4. **Permissions API** : `openid`, `email`, `profile` (déléguées, pas d'admin).
5. **Tenant** : renseigner `SOCIAL_MICROSOFT_TENANT_ID` pour restreindre. Avec
   `common`, l'audit de démarrage émet un avertissement explicite : une
   organisation tierce peut alors connecter ses utilisateurs.

### CSP

La CSP interdit par défaut `accounts.google.com`. Autorisée dans
`render.yaml` **et** `frontend/nginx.conf` :

```
script-src 'self' 'unsafe-inline' https://accounts.google.com;
connect-src 'self' https://*.onrender.com https://accounts.google.com
           https://login.microsoftonline.com;
frame-src  'self' https://accounts.google.com;
```

**Sans cette modification, la connexion Google échoue silencieusement en prod.**

---

## 7. Pourquoi Apple et le téléphone ne sont pas là

| Option | Coût | Décision |
|---|---|---|
| Google | 0 €, sans plafond | ✅ retenu |
| Microsoft | 0 € jusqu'à ~50 000 MAU | ✅ retenu |
| Apple | **99 $/an** (compte développeur) | ⏸ plus tard |
| Téléphone (SMS) | **facturé par SMS** (Firebase : Blaze obligatoire, ~0,01–0,31 $ selon le pays) | ⏸ plus tard |

Les deux sont ajoutables **sans changement d'architecture** : il suffit d'étendre
`SocialProvider`, `SocialAuthProperties` et `SocialIdentityVerifier`, qui sont
déjà pilotés par fournisseur.

### 7.1 Réseaux sociaux écartés — et pourquoi (décisions assumées)

| Réseau | Verdict | Raison concrète |
|---|---|---|
| **Telegram** | écarté | **Gratuit, sans revue, OIDC standard** (JWKS `https://oauth.telegram.org/.well-known/jwks.json`, RS256, issuer `https://oauth.telegram.org`). Techniquement idéal pour l'audience francophone africaine… mais son `id_token` **ne contient aucun claim `email`** (claims : `sub`, `id`, `name`, `preferred_username`, `picture`, `phone_number`). Or notre modèle indexe les comptes par **email global unique** (`V185`) : une identité Telegram n'a donc **rien où se rattacher**. Il faudrait introduire une identité liée au **numéro de téléphone**, sans contrainte d'unicité sur `users.phone` → risque qu'un numéro partagé fasse entrer quelqu'un chez un autre. Risque de sécurité supérieur au bénéfice, donc **non fait**. |
| **LinkedIn** | écarté | son document de découverte annonce `response_types_supported: ["code"]` **uniquement** : pas d'`id_token` implicite. Il faudrait échanger le `code` côté serveur, donc **stocker un `client_secret`** dans l'API — ce que nous avons délibérément évité (un secret dans le backend, c'est un secret à faire tourner). Revendique par ailleurs une revue d'app. |
| **WhatsApp** | écarté | Ce n'est **pas un fournisseur d'identité** : c'est de la messagerie (Cloud API). Aucune authentification de compte utilisateur. |
| **TikTok** | écarté | OAuth existant mais **claim email absent** de son flux public, et usage très faible pour une église. Même blocage que Telegram, sans l'avantage du OIDC propre. |
| **VK** | écarté | Très présent en Afrique francophone, mais OAuth **non-OIDC** : échange du code avec un secret serveur, donc même objection que LinkedIn. | Pour le téléphone, `SmsGateway` (Twilio) existe
déjà mais sert aux notifications ; l'authentification OTP reste à écrire
(code 6 chiffres haché, TTL 5 min, 3 tentatives, quotas par IP **et** par
numéro).

> Apple est de toute façon obligatoire à la revue App Store dès qu'un autre
> login social est proposé dans l'app iOS (règle 4.8) : c'est une raison de plus
> de ne pas l'implémenter à moitié.

---

## 8. Vérifications effectuées

| Vérification | Résultat |
|---|---|
| `mvn test` (JDK 21, Testcontainers PostgreSQL) | voir §9 |
| `OidcSocialIdentityVerifierTest` | 24 tests — audiences, `iss`, `tid`, emails, fail-closed |
| `SocialIdentityServiceTest` | 14 tests — résolution, refus, idempotence, verrouillage |
| `SocialInvitationAcceptanceServiceTest` | 8 tests — email concordant, secret aléatoire, pas de doublon |
| `SocialAuthControllerTest` | 11 tests — contrat HTTP, 429, 403, 503, compatibilité `/auth/google` |
| `vitest` (web) | boutons conditionnés, erreurs, annulation, a11y |
| `flutter analyze` + `flutter test` | 9 tests — orchestration, filtrage des fournisseurs |

> Un test a révélé un **défaut réel** pendant l'écriture : la liste blanche
> Microsoft **remplaçait** le tenant principal au lieu de s'y ajouter, ce qui
> aurait rendu l'accès impossible dès qu'un partenaire était déclaré. Le test de
> non-régression est dans `allowListDoesNotReplacePrimaryTenant`.

---

## 9. Limites honestes

- **La connexion ne fonctionne qu'après vos inscriptions dans les consoles**
  Google et Microsoft. Le code est complet et testé ; sans client-id, le
  fail-closed renvoie 503 et **aucun bouton ne s'affiche** — c'est voulu.
- **Microsoft mobile n'a pas pu être testé sur appareil** ici : le flux PKCE
  passe par le navigateur système, ce qui exige un appareil Android/iOS. Le code
  est unifié derrière `SocialCredentialSource` et couvert par l'orchestration,
  mais **la validation terrain reste à faire** avant d'activer le bouton Microsoft
  sur mobile.
- **Apple** : même plateforme technique, mais l'UX (bouton Apple, restitution
  partielle de l'email) demande un travail propre — pas un ajout de enum.
- Les jetons restent dans `localStorage` (web) : c'est un **pré**-existant hors
  périmètre de ce chantier ; la migration vers des cookies `httpOnly` est
  server-side.
