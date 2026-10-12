# PLAN_VENTE_90JOURS — mettre Discipolat en état de vente

> **Objet** : passer de « plateforme auditable sans acheteur » (0–15 k€) à
> « business cessionnable » (25–50 k€ à 10–20 tenants payants, 100–250 k€ à
> 30 k€ d'ARR). Chaque étape est **datée, mesurée et reliée au registre
> `KNOWN_ISSUES.md`** ; aucune n'est décorative.
>
> **Thèse** : la valeur ne se crée plus dans le code (terminé, gate CI vert,
> 0 échec mesuré le 2026-10-12). Elle se crée dans : **URL publique →
> checkout prouvé → premiers payeurs → dossier de vente**.
>
> **Dernière mise à jour** : 2026-10-12 · mesures du jour consignées ci-dessous.

---

## 0. État mesuré au départ (2026-10-12)

| Fait | Preuve |
|---|---|
| `discipolat.com` **résout et répond 200**, mais sert une page **Squarespace « Prochainement disponible »** — pas l'application | `curl -s https://discipolat.com` → `<title>Prochainement disponible</title>` + `assets.squarespace.com` |
| L'API publique ne sert rien | `curl https://discipolat-api.onrender.com/actuator/health` → `000` |
| La chaîne de déploiement est **prête à l'emploi** : `render.yaml` = Blueprint 4 services (db free, redis free, api docker free, front statique + en-têtes de sécurité) ; `deploy-gate` se rouvre **seule** dès que les 2 secrets GitHub existent | `ci.yml:327-376` |
| Garde E3 active : le profil beta **refuse de démarrer** exposé si `DEMO_PASSWORD` absent/`password123` | `application.yml` (fail-closed) + `render.yaml` (`DEMO_PASSWORD sync:false`) |
| Keep-alive toutes les 10 min (cron), warning sinon rouge | `keep-alive.yml:44` |
| E4 reste **OUVERT** : aucun canal public ne sert le produit | ligne E4 du registre |

---

## Jour 0 — fermer E4 (aujourd'hui, ≈ 2 h + 30 min de clics)

### À faire dans le dashboard Render (30 min, account `jeslight43@gmail.com`)
1. **New → Blueprint** → connecter `suner-dev/discipolat_app`. Le Blueprint
   crée les 4 services (`discipolat-beta-db`, `discipolat-redis`,
   `discipolat-beta-api`, `discipolat-beta` statique). Vérifier que la
   création de la base **free** passe (si Render exige une carte pour la db
   free, prendre la starter à 7 €/mois : ce n'est pas une dépense, c'est le
   premier jour de l'actif).
2. Définir dans le dashboard les variables `sync: false` (valeurs dans le
   fichier local **hors git** `RENDER_BETA_SECRETS.local.md`, jamais dans une
   PR) :
   - `DEMO_PASSWORD` (déjà choisi) — sinon la garde E3 refuse le démarrage ;
   - `JWT_PRIVATE_KEY` / `JWT_PUBLIC_KEY` : `bash setup-keys.sh` (RSA 2048,
     base64, cf. `.env` généré) ;
   - `ENCRYPTION_AES_KEY` : `openssl rand -base64 32` ;
   - `PAYMENTS_WEBHOOK_SECRET` (généré plus tard, à l'étape 3 de la semaine 1) ;
   - optionnel au jour 0 : `MAIL_*`, `SOCIAL_*` (désactivés par défaut).
3. **Custom domain** sur le service statique `discipolat-beta` : ajouter
   `discipolat.com` et `www.discipolat.com`, puis chez le registrar remplacer
   le parking Squarespace par les en-têtes DNS donnés par Render
   (`A 216.24.57.1` / `CNAME`) — le parking actuel **ampute le produit de son
   propre nom**.
4. L'API démarre → vérifier : `curl https://discipolat.onrender.com` →
   la vraie page ; `/actuator/health` → `UP`.

### À faire dans GitHub (2 min, dès que la clé API Render existe)
```
Render → Account Settings → API Keys → créer une clé
Service API → URL du service → extraire srv-xxxxxxxxxxxx (l'ID)
gh secret set RENDER_API_KEY            # coller la clé
gh secret set RENDER_API_SERVICE_ID     # coller srv-…
```
`deploy-gate` repasse `render_provisioned=true` tout seul ; le job
`Deploy to Render` (gris aujourd'hui) redevient **actif à chaque push sur
main**. C'est la fin d'E4 côté CI.

### Contrainte assumée du plan gratuit
Web free = spin-down après 15 min d'inactivité ; le keep-alive `*/10` le
maintient **chaud**, mais le quota mensuel (~750 h) est atteignable à la
fin du mois. **Règle : dès le 3e rendez-vous prospect prévu, passer l'API en
starter (7–19 €/mois)** — une démo qui meurt en plein appel vend moins bien
qu'une instance à 19 €.

---

## Semaine 1 — rendre la valeur *visible* (J3 + démo autonome)

| # | Action | Preuve de sortie |
|---|---|---|
| S1.1 | Créer un **tenant de démonstration** (une église modèle complète : branding, catalogue, `/e/bethel` publié) via le parcours super-admin (script existant `scripts/e2e-superadmin-provisioning.js` adaptable en recette) | URL publique de démo cliquable en 5 min chrono |
| S1.2 | **Stripe mode test** : un checkout EUR + un XAF complets (J3), webhook configuré, facture lue | enregistrement vidéo 3 min + capture dashboard Stripe |
| S1.3 | MoMo : vérifier au moins le parcours initiation en sandbox (si MTN sandbox non accessible, l'écrire KNOWN_ISSUES — ne pas vendre du non-prouvé) | ligne J3 mise à jour |
| S1.4 | Mettre E4/J3 à jour dans `KNOWN_ISSUES.md` (statut + preuve run) | commit docs |

## Semaine 2–4 — coque légale + 2 pilotes (J1/J2, T1)

- **J1/J2** : entité (auto-entrepreneur/SARL selon juridiction), CGU +
  confidentialité validées (défaut : gabarits), `LICENSE` pointée sur
  l'entité. Budget : 300–800 € de conseil juridique — amorti dès le premier
  acheteur car **sans ça, la due diligence s'arrête avant de commencer**.
- **2 églises pilotes** signées (même à 0 € avec **lettre d'intention**
  datée : nom, plan visé, date d'entrée payante). Le MoM est la marchandise,
  pas le code.
- **But commercial réel à J30** : 10 appels produits, 2 pilotes, 0 € de
  CA mais un pipeline écrit.

## Jour 30–90 — premiers euros, puis dossier de vente

- Convertir les pilotes → objectif **5 puis 10 payants** (plans 10–30 €/mois ;
  en XAF, mobiles d'abord). KPIs : MRR, churn < 10 %/mois, temps d'activation
  < 1 jour.
- À 300–800 € MRR : fourchette de cession **25–50 k€** (règle des 10–35× MRR
  sur micro-marchés) ; à 2,5 k€ MRR avec rétention : **100–250 k€**.
- **Dossier de vente** (data room) : `docs/VALORISATION.md` (à créer),
  KNOWN_ISSUES tel quel (c'est un **actif de confiance**, ne rien retirer),
  enregistrement démo 5 min, dashboard Stripe, contrats pilotes, passes CI
  vertes.

---

## Grille de « ce qui débloque quoi »

| Verrou | Lever | Valeur libérée |
|---|---|---|
| E4 (canal public) | Jour 0 ci-dessus | 0 → 15–30 k€ ; sans lui rien d'autre ne se vend |
| J3 (checkout) | Semaine 1 | due diligence « monétisable » devient crédible |
| J1/J2 (légal) | Semaine 2–4 | rend le dossier **achetable** tout court |
| T1 (traction) | J30–90 | 25–50 k€ réels, ensuite scaling ARR |

## Ce que le plan ne promet pas

- Pas de chiffre d'affaires garanti : le marché (églises AF, paiement mobile,
  cycle de confiance) est réel mais lent ; le plan achète de la **preuve**,
  pas du revenu forcé.
- Pas de « zéro échec CI » perdu : chaque étape CI-visible (deploy, health)
  doit rester verte ; une porte qui rougit se documente dans KNOWN_ISSUES,
  jamais ne se sable.
