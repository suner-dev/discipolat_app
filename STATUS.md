# STATUS — état factuel de la plateforme (28 septembre 2026)

Ce document est un constat, pas une promesse. Chaque ligne est vérifiable dans
le dépôt ou par les commandes citées.

## Périmètre livré dans ce cycle (RGPD — correction des limites réelles)

### Purge de rétention : exécution réelle (au lieu d'un simple comptage)
- `ComplianceManagerService.purgePolicy(...)` : en mode **dur**, export JSON
  systématique (`DataExportRecord`, motif `AVANT_PURGE`) puis `deleteAll` réel
  des `ConsentLog`/`AuditLog` expirés ; en mode **souple**, anonymisation des
  traceurs (IP, user-agent, détails, valeurs d'audit) — la preuve d'art. 7 est
  conservée, seuls les identifiants disparaissent.
- Bouton « Exécuter » du tableau de bord conformité : `POST
  /compliance/retention-policies/{id}/execute` exécute réellement la politique
  ciblée (`executeRetentionPolicy`), vérifie l'appartenance au tenant, et
  renvoie le nombre d'enregistrements traités (plus de réponse factice
  `status: "executed"`).
- Job planifié `0 30 3 * * *` (`scheduledPurgeAllTenants`) : appel effectif
  `executeAutomatedPurge()` pour chaque tenant via `runAsTenant` (auparavant
  il se contentait de logger).

### Portabilité RGPD art. 20 : export réel + anti-IDOR
- `GET /compliance/portability/{userId}` renvoie l'export réel
  (`exportUserData` : consentements + demandes RGPD + journal d'export)
  au lieu d'un `Map.of()` vide.
- Accès restreint à **l'intéressé lui-même** ou à un administrateur du tenant
  (`ROLE_ADMIN`/`ROLE_PASTEUR`) — un compte connecté ne peut plus exporter le
  compte d'un autre utilisateur (faille IDOR corrigée).

### Self-service RGPD côté utilisateur (profil web)
- Nouvelle section « Mes données (RGPD) » dans `ProfilePage` :
  - « Exporter mes données (art. 20) » → téléchargement JSON du compte courant ;
  - « Demander la suppression (art. 17) » → crée une demande `SUPPRESSION` via
    `POST /compliance/gdpr` (transmise aux administrateurs du tenant, non
    exécutée immédiatement — aucune suppression automatique de compte).
- Aucune suppression de fonctionnalités existantes ; sections ajoutées en
  complément du dashboard conformité admin.

## Vérifications effectuées (evidence)
- `mvn -f backend/pom.xml test` (JDK 23) : **1 253 tests, 0 échec, 0 erreur**
  (13 skip préexistants) — suite verte **après** les corrections de purge.
  Note : le Mockito/Byte Buddy embarqué ne supporte pas officiellement les
  JDK > 24 — lancer les tests sous JDK 21–23.
- `tsc -b` + `vite build` (frontend) : 0 erreur, build réussi.
- eslint sur les fichiers modifiés : 0 erreur.

## Limites connues (factuelles, mise à jour)
- Le self-service RGPD mobile (export/suppression depuis l'app Flutter) n'est
  pas implémenté — seuls le web et l'admin sont couverts ; le SDK Flutter est
  absent de l'environnement de dev.
- La suppression art. 17 reste un flux **demande → traitement admin**
  (`PATCH /compliance/gdpr/{id}/process`) : aucun effacement automatique du
  compte utilisateur lui-même (volontaire, pour éviter les suppressions
  irréversibles sans validation humaine).
- Les autres limites du cycle précédent (Stripe non testé en live, documents
  légaux = gabarits, mesure d'usage à chaud, hiérarchie multi-envs non chargée)
  restent valables — voir plus bas.

## Périmètre livré au cycle précédent

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

## Vérifications effectuées (evidence) — cycle précédent
- Mobile : SDK Flutter absent de l'environnement de dev — `flutter analyze`
  n'a pas pu être exécuté ; le changement (`register_screen.dart`,
  consentement art. 7/9) est limité à ce fichier et relecture manuelle OK.
- Hygiène : `app-debug.apk` et `token.tmp` ne sont plus suivis par git ;
  `.gitignore` couvre `*.log`, apk et tokens.

## Limites connues (factuelles) — cycle précédent
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
