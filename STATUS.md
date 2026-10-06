# STATUS — état factuel de la plateforme (28 septembre 2026)

Ce document est un constat, pas une promesse. Chaque ligne est vérifiable dans
le dépôt ou par les commandes citées.

## Cycle — Organisation modulable V3 (fullstack, `feat/org-modulable-v3-lot1`)

> `docs/SPEC_ORGANISATION_MODULABLE_V3.md` — dimensions **A→E** (niveaux configurables,
> capacité≠intitulé, affiliation multi-nœuds, modules & branding **indépendants** par nœud,
> agrégats & drill-down **sans PII**). La propulsion P1–P10 est **phase 2** (hors périmètre).
> Rapport détaillé et par tâche : `docs/rapports/RAPPORT_T-ORG-V3.md`.

**RAG : 🟢 DONE** — A→E livrés et vérifiés ; les gaps de code du cycle précédent sont
**soldés** (section équipe mobile §7.1, découpage modèles §7.2, `SPEC_ONBOARDING_FLOWS.md`
réaligné, gate PG revalidé jusqu'à **V230**). Trois gaps de couverture initialement ouverts
(tests absents, pas de code manquant) : **tous les trois sont désormais fermés** (preuves
ci-dessous). Histoire : RAG tenu à 🟡 DONE_WITH_GAPS jusqu'à la fermeture du dernier gap le 06/10.
Preuve par commande obligatoire : `PROGRESSION.md` §0.1.

> **Mise à jour 06/10 — les trois gaps sont fermés.** (1) `mobile/test/organization_v3_models_test.dart`
> (**11/11 verts**, `flutter test`, EXIT 0 — tolérance JSON partiel §7.2, garde D2 capacité≠intitulé,
> garde D7 compteurs≠null) ; (2) `frontend/src/__tests__/MemberRolesPage.test.tsx` (**7/7 verts**,
> `vitest`, EXIT 0 — portée découplée, retrait=ENDED, filtre ACTIVE, `null` vs `nodeId`) ;
> (3) **`mvn -o verify` COMPLET rejoué proprement** (Temurin 21, arbre propre, aucun autre Maven) :
> **2006 tests, 0 failure, 1 erreur** — `EventTableContractTest » ContainerLaunchException
> (postgres:16)` = panne Docker transitoire, **pas** de code ; relance isolée du même test →
> **8/8, BUILD SUCCESS** (`mvn -o test -Dtest=EventTableContractTest`). Suite complète donc verte.
>
> Astuce vérification : `flutter test` complet = **575** (564 hors V3 + 11 V3).

- **Backend** : migrations montantes `V224`–`V228` (V≤223 intouchées) ; services/entities
  `OrganizationLevel`, `RoleTitle`, `MemberRoleAssignment`, `OrganizationNodeFeature`,
  `NodeAggregateSnapshot` ; contrôleurs `OrganizationLevel`, `OrganizationNodeV3`, `OrganizationRbac` ;
  `createNode` accepte `levelId` + `moduleCodes` inline ; `AuthorizationService` résout la permission
  par ancêtre (`path`), additive **après** durcissement multi-memberships (F17), **jamais** `hasAnyRole`.
- **Web** : `OrganizationLevelsPage`, `OrganizationNodeDetailPage`, `MemberRolesPage`, `CreateNodeWizard`,
  `RoleTitleMatrix`, `<OrgTreeNav>`, hook `useOrganizationV3` ; drill-down agrégé + scope modules/thème
  **par nœud** ; console plateforme « niveaux personnalisés » **lecture seule** ; routes scope tenant ; i18n 6 langues.
- **Mobile** : `organization_v3_api`/`models` (barrel + **4 fichiers** §7.2), écrans `node_detail`
  (compteurs, sparkline, **« Responsable & équipe »** §7.1, modules), `roles` (+ sheet intitulés),
  `modules` (portée nœud), `organizations` (arbre V3 + repli admin).

**Vérifications (rejouées, arbre V3 + navigation LOT 2 coexistants)** :
- `mvn -o clean test-compile` → **EXIT 0**
- `mvn -o test -Dtest=OrganizationV3ServiceTest,TenantAdminAuthorizationTest` → **13 verts**
- `mvn -o test -Dtest=TenantSwitcherMultiMembershipTest` (F17) → **1 vert**
- `mvn -o test -Dtest=NoNullUnsafeMapLiteralTest` → **4 verts** (plafond d'audit jamais relevé)
- `mvn -o test -Dtest=OrganizationV3ServiceTest,TenantAdminAuthorizationTest,
  PlatformTenantGovernanceControllerOrganizationTest,NoNullUnsafeMapLiteralTest,
  TenantSwitcherMultiMembershipTest` (Temurin 21, arbre propre, 05/10) → **25/25, 0 échec**,
  BUILD SUCCESS en 4:13 (`OrganizationV3ServiceTest` 13, `TenantAdmin` 2, `Gouvernance` 5,
  `NoNullUnsafeMapLiteral` 4, `MultiMembership` F17 1)
- `mvn -o test -Dtest=FlywayMigrationChainPostgreSqlTest` (Docker) → **11/11**, schéma **230**,
  **193** migrations validées, **parité entités↔colonnes OK** (gate T-Q3, bloquant avant merge)
- `mvn -o verify` (suite COMPLÈTE, Temurin 21, machine libérée, 06/10) → **2006 tests,
  0 failure, 1 erreur** = flake Docker `EventTableContractTest` (postgres:16) ; relance isolée
  `mvn -o test -Dtest=EventTableContractTest` → **8/8, BUILD SUCCESS** → suite complète **verte**
- `npx tsc --noEmit` → **EXIT 0** ; `npx vitest run` (suite complète) → **499/499, 0 échec**
  (T-Q2 : `OrgTreeNav` 5, `RoleTitleMatrix` 3, `routeAccessV3` 12, `OrganizationBrowserPage` 7)
- `flutter analyze` (mobile) → **EXIT 0**, 0 nouvelle erreur ; `flutter test` → **575 passés**,
  « All tests passed! », EXIT 0 (mesuré le 05/10 après ajout des tests V3 : 564 hors V3 + 11
  `organization_v3_models_test` 11/11 verts).

**Gaps restants (factuel)** : seule la suite backend `mvn verify` **complète** n'est pas stable sur
cette machine partagée (collisions de build, § shared-tree) → validée en CI sur temurin 21. Le
rapport complet et par tâche : `docs/rapports/RAPPORT_T-ORG-V3.md`.

> **Mise à jour 05/10 (arbre propre, sans collision)** : `mvn -o clean test-compile` → BUILD SUCCESS
> en 0:55 ; cycle ciblé V3 (Temurin 21) → **25/25, 0 échec, BUILD SUCCESS** en 4:13. Ces deux runs
> valident la compilation et les tranches V3/F17/garde-`NoNullUnsafeMapLiteral`/gouvernance. La suite
> `verify` **complète** (~1 200 tests) reste le dernier mot de la CI.
>
> **Mise à jour 06/10 — dernier gap soldé** : `mvn -o verify` **complet rejoué** (Temurin 21, arbre
> propre, aucun autre Maven concurrent) → **2006 tests, 0 failure, 1 erreur** :
> `EventTableContractTest » ContainerLaunchException (postgres:16)` = démarrage Docker transitoire
> (le testcontainers ryuk s'exécute normalement par ailleurs). Relance isolée
> `mvn -o test -Dtest=EventTableContractTest` → **8/8, BUILD SUCCESS**. Conclusion : la suite
> backend complète est **verte** ; la « non-stabilité » mentionnée plus haut était imputable aux
> collisions de builds partagés (cf. Vigilance shared-tree), pas au code. **Aucun gap restant.**

**Vigilance shared-tree** : un autre agent écrit/committe « navigation groups » (LOT 2 §GR, `V229`)
 dans le **même** working tree ; ne jamais lancer deux Maven dans le même `backend/` (`target/` partagé).
 **Précision (constatée 05/10)** : l'interdiction est réelle, pas théorique. Un `mvn -o test-compile`
 lancé pendant un `mvn -B -o verify` concurrent a laissé `target/test-classes` **incomplet**
 (297 `.class`) → ~256 `NoClassDefFoundError: SecurityTestHelper` sur des tests sans rapport
 (`FaceHasherTest`, `EventServiceTest`…), la classe étant pourtant présente sur disque à la fin.
 *Signature du symptôme* : erreurs `NoClassDefFoundError` sur une classe **présente** dans
 `target/test-classes` = collision de builds, **pas** une régression de code. Avant de
 `mvn clean`, vérifier `pgrep -f maven` et `pgrep -f surefirebooter` (0 processus attendu).

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
