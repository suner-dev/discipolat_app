# STATUS — état factuel de la plateforme (27 septembre 2026)

Ce document est un constat, pas une promesse. Chaque ligne est vérifiable dans
le dépôt ou par les commandes citées.

## Périmètre livré dans ce cycle

### RGPD / conformité (backend + web + mobile)
- Migrations Flyway `V179`–`V182` : documents légaux, preuves de consentement,
  agrégats d'usage quotidien, événements webhooks de paiement.
- `ComplianceService.logConsent(...)` : journal horodaté (type, version du
  document, IP, user-agent) — preuve art. 7.
- Documents légaux versionnés (CGU, politique de confidentialité, DPA) avec
  API publique `GET /api/v1/public/legal` et administration réservée à la
  plateforme (`LegalAdminController`).
- Inscription web (`RegisterPage`) et mobile (`register_screen.dart`) : trois
  consentements explicites **non pré-cochés** (CGU, confidentialité, données
  religieuses art. 9) + version des documents transmise ; la soumission sans
  consentements est refusée (`CONSENT_REQUIRED`).
- Demande de création de tenant (`TenantRegistrationService`) : consentements
  capturés à la souscription, matérialisés dans le journal RGPD à l'approbation.
- Pages publiques `/legal` et `/legal/:code` (hors shell authentifié).

### Mesure d'usage des endpoints
- Intercepteur `EndpointUsageInterceptor` + service d'agrégation (compteurs
  mémoire, vidange vers `endpoint_usage_daily`).
- Rapport plateforme `GET /api/v1/platform/admin/usage/endpoints?days=30`
  (super-admin uniquement) : routes mappées, volumes d'appels, candidats à la
  retraite. Base factuelle pour toute future suppression de code.
- Désactivable : l'intercepteur est absent des contextes réduits (`@WebMvcTest`)
  via `ObjectProvider`, aucun impact sur les tranches de test.

### Facturation Stripe (code prêt, désactivé par défaut)
- `stripe-java 24.16.0` ; configuration `StripeProperties` conditionnée par
  `STRIPE_SECRET_KEY` : **sans clé, rien n'est activé** (endpoints checkout/
  portail renvoient 503, UI cache les CTA via `/public/billing/status`).
- Checkout abonnement (`/billing/stripe/checkout`), portail client
  (`/billing/stripe/portal`), vue abonnement (`GET /billing/stripe/subscription`).
- Webhooks signés `POST /api/v1/payments/webhooks/stripe` : vérification de
  signature sur le corps brut, idempotence par `event.id`, fail-closed (503 si
  secret absent) ; dunning (paiement échoué → statut `PAST_DUE`, avis de
  résiliation à la période suivante).
- Frontend : CTA « Souscrire en ligne » sur la tarification (sessions
  authentifiées uniquement) + page `/billing` (statut, portail, bandeau de
  retour de checkout).
- Correction SecurityConfig : `POST /api/v1/payments/webhooks/**` en accès
  public (les webhooks MoMo précédemment bloqués 401 sont débloqués).

## Vérifications effectuées (evidence)
- `mvn -f backend/pom.xml test` (JDK 23) : **1 253 tests, 0 échec, 0 erreur**
  (13 skip préexistants). Note : le Mockito/Byte Buddy embarqué ne supporte
  pas officiellement les JDK > 24 — lancer les tests sous JDK 21–23.
- `npm run build` (frontend) : `tsc -b` sans erreur + build Vite réussi.
- `npx vitest run` : **47/47 fichiers de tests verts, 339 tests**.
- Mobile : SDK Flutter absent de l'environnement de dev — `flutter analyze`
  n'a pas pu être exécuté ; le changement (`register_screen.dart`,
  consentement art. 7/9) est limité à ce fichier et relecture manuelle OK.
- Hygiène : `app-debug.apk` et `token.tmp` ne sont plus suivis par git ;
  `.gitignore` couvre `*.log`, apk et tokens.

## Limites connues (factuelles)
- Stripe n'a **jamais été testé en live** (pas de clés test dans ce dépôt) :
  montants, prix récurrents et messages d'abonnement doivent être validés en
  mode test avant toute commercialisation.
- Les documents légaux seedés (`V179`) sont des **gabarits** : ils doivent
  être relus par un juriste avant mise en production.
- La mesure d'usage démarre à zéro : le rapport « code mort » n'est exploitable
  qu'après plusieurs semaines de production.
- Hiérarchie d'organisation : la propagation inter-tenant reste couverte par
  des tests d'intégration, pas encore par des charges réelles multi-environnements.

## Pour activer Stripe (quand décidé)
1. Fournir `STRIPE_SECRET_KEY` et `STRIPE_WEBHOOK_SECRET` (sk_test d'abord).
2. Créer les produits/prix récurrents correspondants aux plans du catalogue.
3. Exposer l'URL publique des webhooks vers `/api/v1/payments/webhooks/stripe`.
4. Tester le cycle complet : checkout → webhook `checkout.session.completed` →
   abonnement actif → portail → échec de paiement → dunning.
