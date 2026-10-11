# Guide Super Admin & commercial — §G6.8 (Church OS)

> Console de la **plateforme** (au-dessus des tenants/églises) et procédure de vente de
> bout-en-bout : création d'une église cliente, choix de plan, onboarding, import des
> données, mise en beta. Destinée à l'équipe interne (Super Admin / Sales / Ops).
>
> Rôle : seuls `PLATFORM_SUPER_ADMIN` (contrôlé **en base**, jamais par le rôle porté par le
> client) accèdent aux endpoints `/platform/admin/**`. Toute action sensible est journalisée
> (`audit_event`, chaîne hashée).

---

## 1. Concepts

| Terme | Définition |
|---|---|
| **Tenant** | Une église (ou réseau/campus) = unité d'isolation des données (`tenant_id`). |
| **Plan / abonnement** | Offre commerciale (cf. §3), détermine quotas (stockage, IA, membres, modules). |
| **Super Admin** | Opérateur plateforme : provisionne les tenants, gère plans & quotas, impersonne (journalisé). |
| **Tenant Admin / Pasteur** | Responsable **de l'église** — n'a jamais de vue transverse autres tenants. |

Isolation stricte : le backend résout **toujours** le tenant depuis le JWT (`TenantContext`),
`findById`/requêtes sont filtrés tenant (`TenantAwareSimpleJpaRepository`). Revue :
[`security/SECURITY_MATRIX.md`](./security/SECURITY_MATRIX.md).

---

## 2. Console Super Admin (back-office)

### 2.1 Tenants
- **Lister / rechercher** : `GET /platform/admin/tenants`.
- **Créer** : `POST /platform/admin/tenants` (nom, slug unique, pays, devise, timezone, langue).
- **Fiche tenant** : `GET /platform/admin/tenants/{id}` — membres, abonnement, **usage réel**
  (`GET /admin/saas/plans/usage/{tenantId}`). Lazy à l'ouverture, jamais de chiffre inventé.
- **Cycle de vie** : `POST /platform/admin/tenants/{id}/suspend|reactivate|archive`.
- **Édition plan/params** : `PUT /tenants/{id}`.

### 2.2 Plans & SaaS
- CRUD plans : `POST/PUT/DELETE /admin/saas/plans[/{key}]`, `POST/PUT /platform/admin/plans`.
- **Tarifs & devises** : onglet dédié de `/platform/saas/plans` (§2.5).
- Quotas appliqués serveur (stockage, crédits IA, nb membres, modules) — consommation via
  `GET /api/saas/usage` ; quota atteint → **message clair + file d'attente**, jamais de
  blocage silencieux.

### 2.5 Tarifs, devises et canaux de paiement

> **Tout se décide ici.** Aucun comportement tarifaire n'est figé dans le code :
> chaque interrupteur est un réglage en base (`saas_pricing_settings`), modifiable
> sans redéploiement.

Écran : **`/platform/saas/plans`**, onglet **« Tarification & devises »**.

#### Devises

Cochez les devises que le produit sait servir (XAF, XOF, EUR, USD, CAD, GBP, CHF…).
Puis désignez la **devise de base** et la **devise affichée par défaut**.

> ⚠️ Activer une devise ne crée pas de prix. Tant qu'un plan n'a pas de prix dans
> cette devise, la page publique affiche « tarif non communiqué » et propose un
> contact — c'est volontaire : mieux vaut une absence qu'un montant inventé.
> L'onglet **Devises** de `GET /platform/admin/pricing/currencies` indique le
> `zeroDecimal` de chaque devise, utile pour anticiper les contraintes Stripe.

#### Ce que le visiteur peut choisir

| Réglage | Effet |
|---|---|
| « Le visiteur peut changer de devise » | Le sélecteur apparaît sur `/pricing` et sur la landing. |
| « Le visiteur peut changer de cycle » | La bascule Mensuel / Annuel apparaît. |
| « Le visiteur peut choisir sa région » | Le sélecteur de région apparaît (tarification régionale requise). |
| « Repli sur la devise de base » | Si un plan n'a pas de prix dans la devise demandée, on affiche celui de la devise de base. **Déconseillé** : l'inverse du repli est de ne rien afficher, ce qui est plus honnête. |

Le mode d'affichage (`SINGLE`, `SWITCHER`, `LIST_ALL`, `AUTO_REGION`) décide si la
page montre un prix, un sélecteur, tous les prix empilés, ou le prix de la région
détectée.

#### Prix d'un plan

Sur la carte d'un plan, « Gérer les tarifs de ce plan » ouvre l'éditeur :

- **Montant** : saisissez-le en valeur **majeure** (« 15 000 », « 29.00 »).
  L'application applique les décimales de la devise. Pour XAF/XOF, une décimale
  est refusée — c'est l'exactitude comptable.
- **Région** : `GLOBAL` pour un tarif mondial, sinon `AF`, `EU`, `NA`, `CA`…
- **Canaux de paiement** : cochez ceux que ce prix accepte. L'ordre compte —
  c'est lui qui détermine le bouton proposé en premier.
- **Stripe Price ID** : facultatif. Renseigné, il passe le prix en `MANUAL` et la
  synchronisation automatique ne l'écrasera pas.

Un bandeau **« Points à corriger »** signale ce qui empêcherait une vente —
typiquement un prix en XAF proposé par carte sans Price Stripe et sans autre
canal : le bouton de paiement échouerait.

#### Canaux de paiement

| Canal | Quand | Prérequis |
|---|---|---|
| `CARD` | Carte bancaire (Stripe) | Devise à 2 décimales, **ou** un Price Stripe créé |
| `MOMO` | Mobile Money | Opérateur configuré (`MOBILEMONEY_ENABLED` + identifiants) |
| `STRONG` | Affichage local, débit en devise forte | `strong_currency` définie ; le débit réel est porté par `checkoutCurrency` |
| `BANK` | Virement / SEPA | Canal correspondant activé côté serveur |
| `CONTACT` | Pas de paiement en ligne | Toujours disponible : garantit qu'aucun client n'arrive sur une impasse |

> **Fail-closed assumé.** Un canal déclaré mais dont le prestataire n'est pas
> configuré n'est **pas** proposé. C'est volontaire : afficher « Carte bancaire »
> vers un échecserver est le meilleur moyen de perdre une vente.

#### Synchronisation Stripe

Les devises **sans unité mineure** (XAF, XOF, JPY, KRW) ne peuvent pas être
facturées en prix « inline » : Stripe exige qu'un Price existe au préalable.

- **Création automatique** activée → le backend crée les Products et les Prices
  via l'API, avec une clé d'idempotence (donc sans doublon en cas de rejeu).
- Désactivée → le bouton « Synchroniser Stripe » d'un plan, ou la saisie manuelle
  du Price ID.
- Le **rattrapage** des prix en attente ou en échec tourne toujours, même si la
  création automatique est désactivée : « ne pas créer tout seul » ne veut pas
  dire « ne jamais réessayer ».

Un changement de montant crée un nouveau Price et **désactive** l'ancien plutôt
que de le supprimer : les abonnements déjà souscrits continuent d'être facturés
au tarif auquel ils se sont engagés.

#### Prix figé à la souscription

Le prix est figé sur l'abonnement au moment où il est souscrit
(`tenant_subscriptions.price_minor` + `price_snapshot_json`). Une hausse de
tarif **ne modifie pas** la facture d'un abonnement en cours. C'est ce qui
permet de répondre factuellement à une contestation dans deux ans.

#### Landing

- **Source** `API` : la section lit le catalogue (une seule vérité).
- **Source** `STATIC` : les trois tiers « Sur devis » historiques. Utile si vous
  voulez une page d'accueil sans prix.
- `landing_pricing_max_plans` borne le nombre de plans affichés.

---


### 2.3 Impersonation (§43 — dépannage / support)
- `POST /platform/admin/impersonation` (motif **obligatoire**, TTL 30 min, cible = identité
  portée uniquement, sans élévation). **Interdit** d'impersonner un `PLATFORM_SUPER_ADMIN`.
- Chaque session = lignes `IMPERSONATION_START`/`IMPERSONATION_END` (durée, IP, UA).
- Usage : reproduire un ticket, valider une config, **puis quitter** — l'autorité reste serveur.

### 2.4 Journaux & intégrité
- `GET /audit/events` (recherche par tenant/acteur/entité) ; `GET /audit/events/verify-chain`
  (contrôle d'intégrité de la chaîne hashée) ; `GET /audit/history/{type}/{id}` (history métier).

---

## 3. Plans commerciaux (annexe F)

| Plan | Cible | Points clés |
|---|---|---|
| **DÉCOUVERTE** | petites communautés | essentiel : registre, espaces, événements, dress code ; badges 🤖 IA incluse (quota d'entrée). |
| **DÉMARRAGE** | église structurée | + workflow/approbations, finance & dons Mobile Money, plus de crédits IA. |
| **CROISSANCE** | multi-campus naissant | + pasturat/multi-unités, offline terrain, low-band (WhatsApp/USSD), analytics avancés. |
| **RÉSEAU & CAMPUS** | réseaux/dénominations | + hiérarchie REGION/CHURCH/CAMPUS illimitée, héritage de config, quotas élevés, support prioritaire. |

- **Essai** : 30 jours. **Annuel** : 2 mois offerts. **Géolocalisation tarifaire** : par devise
  (XAF/XOF, EUR, USD, CAD, GBP, CHF) et, si vous l'activez, par région — le tout
  paramétrable depuis `/platform/saas/plans` (§2.5).
- **Annuaire public** « Églises sur Discipolat » : opt-in par tenant (`public_directory_enabled`)
  → vitrine + lien de souscription.

---

## 4. Procédure de vente — de la signature à la beta

1. **Prospection / démo** — page vitrine `/pricing` (CTA onboarding). Recueillir : nom église,
   pays, taille, besoins (multi-campus ? finance ? low-band ?).
2. **Provisionner le tenant** — console §2.1 : créer le tenant, choisir le plan §3, devise,
   langue, timezone. Vérifier l'émission du slug unique.
3. **Onboarding (§G1.5)** — envoyer l'invitation au responsable : `POST /admin/invitations`
   → e-mail réel + page d'acceptation (token, expiration, anti-rejeu) → le client définit son
   mot de passe et atterrit sur le wizard (branding, espaces depuis templates).
4. **Héritage de configuration (§G1.7/G1.8)** — choisir `DEFAULT/INHERITED/OVERRIDDEN` par
   unité ; ressources `GLOBAL`/`LOCAL`.
5. **Import des données existantes (§G4.6)** — activer `legacy_migration_enabled` puis
   **dry-run** : `POST /legacy-migration/modules/{code}/dry-run` (0 écriture, rapport par table).
   Valider → **replay** : `POST …/migrate`. Rollback possible ≤ 30 j (`…/jobs/{id}/rollback`).
   **Aucune donnée source n'est effacée avant validation.**
6. **Mise en beta** — suivre la check-list de lancement du
   [`RUNBOOK_OPS.md`](./RUNBOOK_OPS.md) §7 (backup post-import, monitoring, smoke check).
7. **Facturation / cycle de vie** — suivi consommation & renouvellement via usage SaaS ;
   suspension/réactivation/archivage selon paiement.

---

## 5. Garde-fous

- **Principe du moindre privilège** : un Super Admin ne consulte les données transverse que
  pour le support, **journalisé**. Aucune donnée sensible client (pastoral, finance) exportée
  hors procédure documentée.
- **RGPD / confidentialité** : mentions et conditions de traitement (cf. [`SECURITY.md`](./SECURITY.md)).
- **Double confirmation** pour toute suppression physique (réservée Super Admin) + audit.
- **Anti-escalade** : `RESPONSABLE`/rôles tenant ne peuvent jamais atteindre les endpoints
  plateforme ; testés en CI (`MultiTenantSecurityTests`).
