# Tarifs & devises — API du catalogue de prix

> **STATUT** : IMPLÉMENTÉ (11/10/2026) — chantier `TODO_TARIFS_PAIEMENTS_INTERNATIONAL.md`.
> Base : `/api/v1`. Tous les endpoints publics sont `permitAll` ; les endpoints
> `/platform/admin/**` exigent `@authz.isPlatformSuperAdmin()`.

---

## 1. Le contrat le plus important : les unités

> **Tout montant stocké, échangé ou envoyé à un prestataire est en unités
> MINEURES ISO-4217.**

| Devise | Décimales | Exemple | Stocké / échangé | Affiché |
|---|---|---|---|---|
| XAF, XOF, JPY, KRW | 0 | 15 000 FCFA | `15000` | `15 000 FCFA` |
| EUR, USD, CAD, GBP, CHF | 2 | 29,00 € | `2900` | `29,00 €` |

Le nombre de décimales provient **exclusivement** de
`modules/currency/domain/Iso4217CurrencyValidator#minorUnits(String)`.
Aucun code — backend, web ou mobile — ne multiplie par 100 : le catalogue stocke
déjà la valeur exacte.

Conséquence pratique : **un montant absent ne vaut pas zéro.** Les réponses
exposent `available: false` et `amountMinor: null` ; l'interface affiche
« tarif non communiqué » plutôt qu'un 0 qui se lirait comme un plan gratuit.

---

## 2. API publique

### `GET /api/v1/public/pricing/config`

Ce que le visiteur a le **droit** de faire. Tout y est décidé par le super-admin
(`saas_pricing_settings`) ; le client ne fait que refléter.

Query : `currency` (optionnel), `region` (optionnel).
Cache : `max-age=60, public`.

```json
{
  "catalogVersion": 7,
  "displayMode": "SWITCHER",
  "baseCurrency": "XAF",
  "defaultCurrency": "XAF",
  "resolvedCurrency": "XAF",
  "resolvedRegion": "AF",
  "regionPricingEnabled": false,
  "allowCurrencyChoice": true,
  "allowCycleChoice": true,
  "allowRegionChoice": false,
  "allowMonthly": true,
  "allowYearly": true,
  "defaultCycle": "MONTHLY",
  "trialEnabled": true,
  "stripeEnabled": false,
  "enabledCurrencies": [
    { "code": "XAF", "symbol": "FCFA", "name": "Franc CFA (BEAC)", "decimals": 0, "selected": true },
    { "code": "EUR", "symbol": "€", "name": "Euro", "decimals": 2, "selected": false }
  ],
  "regions": [],
  "channels": { "CARD": true, "MOMO": false, "STRONG": true, "BANK": false, "CONTACT": true },
  "landing": { "enabled": true, "source": "API", "maxPlans": 4,
               "showPrices": true, "showToggle": true, "ctaLabel": null }
}
```

### `GET /api/v1/public/pricing/plans`

Query : `currency`, `cycle` (`MONTHLY` | `YEARLY`), `region`.
Cache : `max-age=60, public`.

```json
{
  "catalogVersion": 7,
  "currency": "XAF",
  "cycle": "MONTHLY",
  "region": "AF",
  "plans": [
    {
      "key": "STARTUP",
      "name": "Démarrage",
      "description": "Pour les églises en croissance",
      "trialDays": 30,
      "annualDiscountPct": 17,
      "seatsLimit": 200,
      "storageLimitMb": 5000,
      "aiCreditsLimit": 500,
      "limits": { "members": 200 },
      "features": { "finance": true },
      "modules": ["dashboard", "members"],
      "prices": [
        { "currency": "XAF", "cycle": "MONTHLY", "amountMinor": 15000, "amount": "15000",
          "decimals": 0, "region": "GLOBAL", "isRegional": false, "fromFallback": false,
          "selectable": true, "checkoutCurrency": null,
          "channels": ["MOMO", "STRONG"], "stripePriceStatus": "SYNCED" }
      ],
      "display": {
        "available": true,
        "amountMinor": 15000, "amount": "15000",
        "currency": "XAF", "decimals": 0, "cycle": "MONTHLY",
        "selectable": true,
        "checkoutCurrency": null,
        "chargedInDifferentCurrency": false,
        "channels": ["MOMO", "STRONG"],
        "reason": null
      }
    }
  ]
}
```

**`display.available = false`** ⇒ `amountMinor: null`, `channels: []`,
`reason: "NO_PRICE_IN_CURRENCY"`. Le client affiche « tarif non communiqué » et
propose un contact. **C'est une réponse de premier ordre, pas une erreur.**

`display.chargedInDifferentCurrency = true` ⇒ l'affichage est en XAF/XOF mais le
débit se fera en `checkoutCurrency`. L'interface doit l'annoncer : une surprise
à l'écran de carte est un motif d'abandon.

### `GET /api/v1/public/pricing/plans/{key}`

Un plan isolé (lien profond). Même forme, sous `plan`.

### `GET /api/v1/public/pricing/currency`

Devise et région effectivement retenues. `Cache-Control: no-store`.
Utile à l'indicateur d'une app mobile ou d'un bandeau.

```json
{ "currency": "XAF", "requested": "JPY", "honored": false, "region": "AF", "decimals": 0 }
```

### `GET /api/v1/public/plans` — *conservé, non cassé*

Réponse historique, **inchangée** : `priceMonthly`, `priceYearly`, `priceEur`,
`priceXaf`, `priceUsd`, `currency`… avec leur sémantique de montants
**majeurs**. Le client mobile et les intégrations tierces continuent de
fonctionner.

Une correction y a été appliquée : le filtre sur la liste fermée des quatre plans
canoniques a disparu (faille **F3**). Un plan créé par le super-admin avec
`is_public = true` **apparaît désormais**. La liste canonique ne sert plus
qu'à l'ordre d'affichage ; les plans ajoutés viennent après, triés par
`sort_order`.

---

## 3. Administration plateforme — `@authz.isPlatformSuperAdmin()`

Base : `/api/v1/platform/admin/pricing`.

| Méthode | Route | Rôle |
|---|---|---|
| `GET` | `/settings` | Réglages courants |
| `PUT` | `/settings` | Mise à jour (PUT sémantique complet), incrémente `catalogVersion` |
| `GET` | `/currencies` | Catalogue ISO-4217 + `enabled` + `zeroDecimal` par devise |
| `GET` | `/plans/{key}/prices` | Tous les prix du plan (tous statuts) |
| `POST` | `/plans/{key}/prices` | Upsert sur (plan, devise, cycle, région) |
| `PUT` | `/plans/{key}/prices/{id}` | Mise à jour partielle |
| `DELETE` | `/plans/{key}/prices/{id}` | Retrait du catalogue |
| `POST` | `/plans/{key}/prices/bulk` | Remplissage de N devises en une fois |
| `POST` | `/plans/{key}/prices/stripe-sync` | Synchronisation Stripe du plan |
| `POST` | `/prices/stripe-sync` | Synchronisation de tout le catalogue |
| `GET` | `/plans/{key}/diagnostics` | Points bloquants et avertissements |

### Écriture d'un prix

```json
{
  "currency": "XAF",
  "billingCycle": "MONTHLY",
  "amountMinor": 15000,
  "regionCode": "GLOBAL",
  "checkoutCurrency": null,
  "isEnabled": true,
  "isVisible": true,
  "isSelectable": true,
  "paymentChannels": "MOMO,STRONG",
  "stripePriceId": null
}
```

Règles serveur :

- `amountMinor` est **obligatoire** et validé par `Iso4217CurrencyValidator` :
  `15000` pour du XAF, `"15000.50"` est **refusé** (`CURRENCY_DECIMALS_INVALID`).
- `currency` doit être ISO-4217 connu, en majuscules.
- `regionCode` : `'GLOBAL'` ou un code région. **Jamais `null`** (PostgreSQL ne
  déduplique pas les `NULL` dans un `UNIQUE`).
- `paymentChannels` : CSV ordonné parmi `CARD,MOMO,STRONG,BANK,CONTACT`.
  L'ordre du CSV fait loi pour l'ordre d'affichage des boutons.
- `stripePriceId` renseigné ⇒ statut `MANUAL` : la synchronisation automatique
  n'écrasera pas une saisie du super-admin.
- `amountMinor` = 0 est accepté (plan gratuit), mais la diagonale `/diagnostics`
  le signale pour les plans payants.

### Codes d'erreur

| Code | Signification |
|---|---|
| `CURRENCY_INVALID` | Code absent ou mal formé |
| `CURRENCY_UNSUPPORTED` | Devise inconnue du référentiel ISO-4217 |
| `CURRENCY_DECIMALS_INVALID` | Décimales interdites pour cette devise |
| `PRICE_AMOUNT_REQUIRED` / `PRICE_AMOUNT_INVALID` | Montant absent ou négatif |
| `PRICE_NO_CHANNEL` | Aucun canal de paiement sélectionné |
| `PRICE_NOT_FOUND` | `priceId` inconnu ou hors du plan |
| `PLAN_NOT_FOUND` | Plan inconnu |
| `PRICING_NO_CURRENCY_ENABLED` | Aucune devise activée |
| `PRICING_NO_CYCLE_ENABLED` | Ni mensuel ni annuel autorisé |
| `PRICING_CURRENCY_NOT_ENABLED` | Devise de base hors de la liste activée |
| `PRICING_NO_CHANNEL_ENABLED` | Aucun canal de paiement ouvert globalement |
| `PRICING_DISCOUNT_OUT_OF_RANGE` | Remise annuelle hors 0–90 % |

### Diagnostics d'un plan

`GET /plans/{key}/diagnostics` liste ce qui empêcherait une vente :

- `PRICE_UNREACHABLE` — devise sans centimes + canal carte + **ni Price Stripe ni
  canal alternatif** : le bouton de paiement échouerait. **À corriger avant vente.**
- `NO_MONTHLY_PRICE` — le cycle par défaut n'a rien à afficher.
- `NO_PRICE` — aucun prix : le plan s'affichera sans montant.
- `STRIPE_SYNC_FAILED` — la création du Price a échoué.
- `PRICE_DISPLAY_ONLY` (INFO) — prix visible mais non sélectionnable.

---

## 4. Checkout Stripe

### `POST /api/v1/billing/stripe/checkout`

```json
{ "planKey": "GROWTH", "billingCycle": "monthly", "currency": "EUR", "region": "AF" }
```

`currency` et `region` sont **optionnels** : sans devise, le serveur tente le
plan dans sa devise, puis dans la devise de base du catalogue.

`STRIPE_PRICE_ID_REQUIRED` — la devise de débit n'a pas d'unité mineure
(XAF, XOF, JPY, KRW) et aucun Price Stripe n'a été créé. Le message indique
explicitement quoi faire. Causes possibles : synchronisation automatique
désactivée et Price non saisi, ou clé Stripe absente.

`STRIPE_DISABLED` — aucune `STRIPE_SECRET_KEY`. Fail-closed, aucun appel réseau.

---

## 5. Changements de comportement importants

| Faille | Avant | Après |
|---|---|---|
| **F1** facturation 100× trop basse | `unit_amount` recevait un montant **majeur** : GROWTH débité 0,29 € au lieu de 29 € | Unités mineures, sans exception |
| **F2** STARTUP non souscriptable | `STRIPE_PRICE_ID_REQUIRED` pour toute devise hors EUR/USD | `StripePriceSyncService` crée les Prices par l'API, avec clé d'idempotence |
| **F3** plans custom invisibles | Liste blanche des 4 plans canoniques, appliquée 2 fois | Seuls « publié + actif » comptent |
| **F4** landing contradictoire | 3 tiers « Sur devis » codés en dur pendant que `/pricing` montrait 4 prix réels | La landing lit le catalogue ; le mode statique devient un réglage explicite |
| **F5** `FCFA` codé en dur | Tout plan affiché en « FCFA/mois », même en EUR ou USD | Symbole issu de la devise du prix |

---

## 6. Vérification avant mise en production

Les requêtes de contrôle sont dans
[`TODO_TARIFS_PAIEMENTS_INTERNATIONAL.md`](TODO_TARIFS_PAIEMENTS_INTERNATIONAL.md) §14.
À exécuter après migration, avant d'ouvrir la vente. Le critère n° 3 (prix
0-décimale sans Price et sans alternative) doit retourner **zéro ligne**.