# Distribution APK aux testeurs (Diawi) — Discipolat Mobile

> **Ne jamais envoyer un APK debug** : en mode debug l'app pointe l'API locale
> (`http://10.0.2.2:8080`) — voir `mobile/lib/data/services/api_config.dart`.
> Seul l'APK construit par `scripts/build-apk-distribution.sh` est distribuable.

## 1. Avant l'upload (responsable de campagne)

1. Vérifier l'**environnement cible** : la prod Render dors en cold-start (~1 min).
   Tester `GET /api/v1/public/legal` doit répondre **HTTP 200**.
2. Construire : `./scripts/build-apk-distribution.sh` (sans argument → force
   confirmation PROD, avec `--api-url …` pour cibler explicitement).
   Le script incrémente le build number, builde `--release`, et affiche le SHA256.
3. Uploader sur https://www.diawi.com avec **obligatoirement** :
   - un **mot de passe** (lien jamais public — l'APK contient l'URL d'API et la logique d'auth) ;
   - une **date d'expiration** (≈ durée de la campagne + 7 j) ;
   - la limite d'installations si l'offre le permet.

## 2. Message à envoyer aux testeurs (modèle)

> Bonjour,
>
> Pour tester l'app Discipolat v___ (build ___) :
> 1. Ouvrez le lien Diawi : `___` (mot de passe : `___`).
> 2. Installez l'APK : Android demandera « Installer des applis inconnues » —
>    autorisez **uniquement pour cette installation** (Chrome/navigateur),
>    puis retirez l'autorisation après install.
> 3. Connectez-vous avec le compte de test fourni (`___` / `___`).
>    N'utilisez **pas** votre vrai compte paroissial.
> 4. Remontez les bugs avec : version de l'app + modèle Android + capture d'écran.
>
> L'app pointe l'environnement `___` (pas vos vraies données).
> SHA256 de l'APK (anti-fraude) : `___`.

## 3. Pendant la campagne

- Chaque nouveau build = **+1 build number** (script automatique) + **nouveau lien**.
- Journaliser : version ↔ lien ↔ liste des testeurs (qui a quoi).
- Android 14+ : l'installation depuis le navigateur est bloquée par défaut —
  la procédure §2 couvre le cas.

## 4. Après la campagne

1. **Révoquer** le lien Diawi (ou attendre l'expiration).
2. **Purger le tenant de test** (données de testeurs + comptes créés) —
   jamais les tenants réels.
3. Si la campagne dépasse ~5 testeurs ou se répète : migrer vers
   **Firebase App Distribution** (gratuit, versions, crash reports) ou
   **Play Console – Testing interne** (100 testeurs).

## 5. Rappel RGPD (données de test)

Les testeurs manipulent des données membres/religieuses (art. 9) :
comptes de test **dédiés**, tenant **isolé**, **aucune donnée réelle** exigée
pour tester, purge documentée en fin de campagne.
