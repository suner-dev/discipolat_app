# TODO — TARIFS & PAIEMENTS INTERNATIONIONALISATION (multi-devises, multi-régions, multi-canaux)

> **STATUT** : À FAIRE (spécification détaillée, non commencée).
> **VERSION** : v1.0 — rédigée le 11/10/2026.
> **PÉRIMÈTRE** : Lots T1 → T9 ci-dessous. Couvre **backend (Spring Boot)**, **frontend (React/Vite)**, **mobile (Flutter)**, **E2E (Playwright)**.
> **PRINCIPE DIRECTEUR** : *ne rien supprimer qui fonctionne*. Toute évolution est **additive** (nouvelles tables, nouvelles colonnes, nouvelles routes). Les colonnes et endpoints legacy restent lisibles et en écriture pendant au moins une version.
> **AUDIENCES** : tout agent (ou humain) reprenant le chantier doit pouvoir exécuter ce document de bout en bout sans redécouvrir le codebase.
>
> **Documents liés** : `docs/SPEC_LOT2_TOUT_PARAMETRABLE.md` (surcharge d'étiquettes, pattern `resolveLabel`),
> `docs/ARCHITECTURE.md`, `docs/RBAC.md`, `docs/GUIDE_SUPER_ADMIN.md`, `docs/KNOWN_ISSUES.md:33`
> (« Stripe / paiements jamais testés en live — Bloquant monétisation »), `docs/DECISIONS.md`.

---

## §0. DÉCISIONS PRODUCTE ARBITRÉES (à ne pas rediscuter sans arbitrage explicite)

| # | Question | Décision retenue |
|---|---|---|
| Q1 | Devises internationales | **XOF** (UEM : CI, SN, ML…) **+ CAD, GBP, CHF** en plus de l'existant **EUR / XAF / USD**. Repofer KES/NZD/AED/INR à l'étape T9 (facultatif). |
| Q2 | Définition des prix | **Prix saisis en dur par devise** (pas de taux de change automatique) **+ grille tarifaire régionale optionnelle**. Les deux sont **activables/désactivables indépendamment** par le super-admin. |
| Q3 | Paiement des devises 0-décimales (XAF/XOF) | **Trois canaux, tous paramétrables par plan × devise** : (a) création de Price Stripe **via l'API** (par défaut), (c) **Mobile Money** (MTN/Orange/M-Pesa), (d) **débitation en devise forte** (affichage XAF, débit EUR/USD). |
| Q4 | Périmètre | Page `/pricing` publique refaite, page admin plans enrichie, **Landing branchée sur l'API** (plus de prix « Sur devis » codés en dur), **contrat d'unités de prix clarifié**, tout rendu paramétrable, **parité mobile**. |
| Q5 | Qui décide | **Le super-admin**. Chaque comportement listé ci-dessous est un interrupteur de `saas_pricing_settings` (table singleton). L'utilisateur final ne peut choisir que ce que le super-admin a autorisé (`allow_user_currency_choice`, `is_selectable` par prix). |

### Devise de base et piège XAF/XOF

**XAF ≠ XOF.** Deux devises distinctes ISO-4217, toutes deux à **0 décimale**, mais des valeurs différentes :
XAF (BEAC : CM, GA, TD, CG, GQ, GA) ≈ 0,0016 USD · XOF (UEM : CI, SN, ML, BF, NE, TG, BJ, GW) ≈ 0,0015 USD.

**Décision** : aucune devise « canonique » imposée. XAF et XOF sont **deux devises indépendantes** avec chacune
leurs prix. La devise de base (`base_currency`) ne sert qu'à :
- l'ordre d'affichage par défaut ;
- le repli quand un plan n'a pas de prix dans la devise demandée (repli **désactivé** par défaut : voir `settings.show_fallback_currency`).

> ⚠️ **BUG EXISTANT À CORRIGER (T2.7)** : le backend résout `XAF` (`SaasPlan:123`, `TenantCurrencyResolver:53`)
> mais les dons envoient `XOF` en dur (`frontend/src/pages/GivingPage.tsx:159,211` et
> `mobile/lib/presentation/screens/giving/giving_screen.dart:132,217`). Corriger dans le cadre de T2 : le choix
> de devise de donation doit venir du tenant (`currency_configs`) et non d'une constante.

---

## §1. CARTOGRAPHIE DE L'ÉTAT ACTUEL (à lire avant de coder)

### 1.1 Stack

| Tier | Stack | Chemin |
|---|---|---|
| Backend | Spring Boot 3.4.7 · Java 21 · Spring Web · Spring Data JPA (Hibernate) · Spring Security · Flyway · PostgreSQL · Maven | `backend/` |
| Frontend | React 19 · Vite 8 · React Router 7 · TypeScript · TailwindCSS · TanStack Query 5 · Axios · lucide-react | `frontend/` |
| Mobile | Flutter / Dart (`dio`, `http`) | `mobile/` |
| E2E | Playwright (`@playwright/test` seul) — `e2/specs/` ne contient **que** un README | `e2/` |

- Base API : `/api/v1` — `frontend/src/lib/api.ts:5-13` ; proxy dev `frontend/vite.config.ts:28-31`.
- Migrations : `backend/src/main/resources/db/migration/` — **dernière = `V244`**. **La première migration de ce chantier est `V245`.**
- Règle dépôt : **aucune migration `V ≤ 244` ne doit être modifiée** (cf. `SPEC_LOT2_TOUT_PARAMETRABLE.md`, D14).
- Multi-tenancy : chaque entité porte `tenant_id` + `@Filter(name = "tenantFilter")`. **Sauf** la catalogue plateforme (plans, réglages plateforme) qui est globale.

### 1.2 Modèle de prix actuel — et ses défauts

Entité : `backend/src/main/java/com/discipolat/modules/tenants/domain/SaasPlan.java` (147 lignes).

| Champ Java | Colonne | Type | Ligne |
|---|---|---|---|
| `key` (PK) | `key` | `varchar(30)` | 26-28 |
| `name` | `name` | `varchar(100)` | 30-31 |
| `description` | `description` | TEXT | 33-34 |
| `priceMonthly` | `price_monthly` | `BIGINT` | 36-37 |
| `priceYearly` | `price_yearly` | `BIGINT` | 39-40 |
| `currency` | `currency` | `varchar(3)` NOT NULL | 42-43 |
| `limitsJson` / `featuresJson` | `limits_json` / `features_json` | JSONB NOT NULL | 45-51 |
| `stripePriceIdMonthly` / `Yearly` | `stripe_price_id_monthly` / `_yearly` | `varchar` | 53-57 |
| `isActive` / `sortOrder` | `is_active` / `sort_order` | | 59-65 |
| `isPublic` | `is_public` | | 75-77 |
| `seatsLimit` / `storageLimitMb` / `aiCreditsLimit` | idem | INTEGER | 79-90 |
| `priceEur` / `priceXaf` / `priceUsd` | `price_eur` / `price_xaf` / `price_usd` | `BIGINT` | 92-99 |
| `billingPeriod` | `billing_period` | `varchar(20)` | 101-102 |
| `trialDays` (=30) / `annualDiscountPct` (=17) | idem | INTEGER | 104-110 |
| `regionsJson` | `regions_json` | JSONB | 112-114 |
| `status` | `status` | `varchar(20)` | 116-117 |

**Défauts identifiés (tous à traiter) :**

1. **Colonnes figées à 3 devises.** Ajouter une 4ᵉ devise (XOF, GBP…) exige une migration + une colonne + un setter
   + un DTO + un champ de formulaire. → Corrigé par **T1** (table `saas_plan_prices`).
2. **`regions_json` existe mais n'est lu nulle part** (aucune résolution de région). → Corrigé par **T1/T4**.
3. **`billing_period` mélange deux notions** : le *cycle de référence du plan* et la *période d'affichage*. Consommé
   comme période d'affichage par `PricingPage.tsx:210`. → Corrigé par **T1** (`billing_cycle` par prix).
4. **`priceMonthly`/`priceYearly` vs `priceEur`/`priceXaf`/`priceUsd` : deux systèmes de prix concurrents** sur la
   même table, jamais synchronisés (V144 renseigne les premiers, V177 les seconds). → Corrigé par **T1/T2**.

### 1.3 Données seedées (source de vérité actuelle)

`V144__seed_saas_plans.sql` puis `V177__complete_canonical_saas_plans.sql`.

| Plan | `price_monthly`/`price_yearly` | `currency` | `price_eur` | `price_xaf` | `price_usd` | `trial_days` | `annual_discount_pct` |
|---|---|---|---|---|---|---|---|
| `DISCOVERY` | 0 / 0 | EUR | 0 | 0 | 0 | 30 | 17 |
| `STARTUP` | 15 000 / 150 000 | XAF | NULL | 15 000 | NULL | 30 | 17 |
| `GROWTH` | 29 / 290 | EUR | 29 | NULL | NULL | 30 | 17 |
| `NETWORK` | 99 / 990 | USD | NULL | NULL | 99 | 30 | 17 |

`regions_json` (V177) : DISCOVERY `[AF,EU,US]` · STARTUP `[AF]` · GROWTH `[AF,EU]` · NETWORK `[AF,EU,US]`.
`limits_json` complet en V177:66-79. `modules_included_json` en V177:2,14,27,40,53.

### 1.4 Endpoints existants (ne pas casser les contrats)

| Méthode | Route | Fichier:ligne | Rôle |
|---|---|---|---|
| GET | `/api/v1/public/plans` | `modules/tenants/api/PublicSaasPlanController.java:26-34` | Catalogue public, `Cache-Control: max-age=300` |
| GET | `/api/v1/public/billing/status` | `modules/payments/stripe/PublicBillingController.java:25-28` | `{stripeEnabled}` |
| POST | `/api/v1/billing/stripe/checkout` | `modules/payments/stripe/StripeBillingController.java:40-47` | `@authz.isTenantAdmin()` |
| POST | `/api/v1/billing/stripe/portal` | idem `:49-53` | Portail client |
| GET | `/api/v1/billing/stripe/status` | idem `:55-61` | |
| GET | `/api/v1/billing/stripe/subscription` | idem `:64-84` | |
| GET/POST/PUT/DELETE | `/api/v1/admin/saas/plans/**` | `modules/admin/api/SuperAdminSaasPlanController.java` | `@authz.isPlatformSuperAdmin()` |
| GET/POST | `/api/v1/platform/admin/plans` | `modules/platform/api/SuperAdminController.java:330, 462` | résumés + upsert |
| GET/POST/PUT | `/api/v1/admin/subscription/**` | `modules/platform/api/SubscriptionController.java:37,43,64,84,103,120` | scope tenant |
| GET | `/api/v1/platform/currencies` | `modules/platform/api/PlatformCurrenciesController.java:31-44` | catalogue ISO-4217 complet |

Sécurité : `GET /api/v1/public/**` est `permitAll()` (`common/infrastructure/config/SecurityConfig.java:92`) ;
`POST /api/v1/payments/webhooks/**` aussi (`:104`, protégé par signature).

### 1.5 Failles à corriger — détail factuel

#### F1 — **Facturation 100× trop basse (CRITIQUE)**
`modules/payments/stripe/StripeBillingService.java:89-107` :
```java
Long amount = "yearly".equals(cycle) ? plan.getPriceYearly() : plan.getPriceMonthly();   // 29
...
.setUnitAmount(amount)   // Stripe interprète en UNITÉS MINEURES → 0,29 €
```
Stripe attend des **centimes**. Avec `GROWTH.price_monthly = 29`, le checkout débite **0,29 €** au lieu de 29 €.
Idem `NETWORK` : 99 cents au lieu de 99 $. STARTUP est bloqué avant (F2).
→ **Corrigé par T2.1 + T1** (unification sur `amount_minor`).

#### F2 — **STARTUP (XAF) non souscriptable**
`StripeBillingService.java:40` :
```java
private static final Set<String> INLINE_PRICE_CURRENCIES = Set.of("eur", "usd");
```
puis `:96-101` lève `STRIPE_PRICE_ID_REQUIRED` pour toute autre devise. XAF/XOF sont **0-décimales** : Stripe refuse
`price_data` inline, il faut un `Price` créé à l'avance. Le plan STARTUP (le plan payé principal en Afrique) est donc
inutilisable en direct. → **Corrigé par T6.**

#### F3 — Le double filtre `PUBLIC_PLAN_KEYS` rend les plans custom invisibles
`PublicSaasPlanController.java:29` **et** `SaasPlanService.java:46` filtrent sur la constante
`PUBLIC_PLAN_KEYS = List.of("DISCOVERY","STARTUP","GROWTH","NETWORK")` (`SaasPlanService.java:24`).
Un plan créé par le super-admin avec `is_public = true` **n'apparaît jamais** sur `/pricing`.
→ **Corrigé par T3.1** (le filtre devient configurable, la liste figée disparaît de la couche service).

#### F4 — Landing déconnectée du backend
`frontend/src/components/landing/SectionPricing.tsx:7-13, 40` : 3 tiers codés en dur
(`Essentiel` / `Pro` / `Église`) avec le prix **« Sur devis »** — alors que `/pricing` affiche 4 plans avec de vrais
prix. Deux vérités contradictoires sur le même produit, et les modifications super-admin ne se reflètent pas.
→ **Corrigé par T7.**

#### F5 — `FCFA` codé en dur dans l'admin
`frontend/src/pages/PlatformPlansPage.tsx:136-137` : `<span>FCFA/mois</span>` et `{plan.priceYearly} FCFA/an`,
quelle que soit la devise du plan.
→ **Corrigé par T5.4.**

#### F6 — Ambigüïté d'unités de prix
`SaasPlan.priceMonthly/priceYearly/priceEur/...` sont des `BIGINT` sans contrat d'unité documenté. Le frontend affiche
`priceEur=29` comme 29 € (unités **majeures**), Stripe consomme `priceMonthly=29` comme 29 centimes (unités
**mineures**). → **Contrat écrit et appliqué : voir §2.1.**

#### F7 — Partial-update incapable de remettre un prix à `NULL`
`SuperAdminSaasPlanController.java:86-103` : `if (request.priceEur() != null) plan.setPriceEur(...)`.
Impossible de retirer un prix. `SuperAdminController.java:341-345` a le même défaut pour `priceMonthly/priceYearly`.
→ **Corrigé par T3.3** (upsert sur table prix avec sémantique explicite `null` = supprimer).

#### F8 — `annual_discount_pct` n'est appliqué nulle part
`17` est stocké mais aucun code ne recalcule `price_yearly`. Les prix annuels seedés (150 000 vs 15 000×12=180 000 ;
290 vs 29×12=348 ; 990 vs 99×12=1 188) intègrent **déjà** la remise de ≈16,7 %. Deux sources de vérité.
→ **Corrigé par T3.6** : le prix annuel est **explicite** ; `annual_discount_pct` devient un simple indicateur
d'affichage (« −17 % ») tant que T3.6 n'a pas choisi la source de vérité. **Décision** : prix annuel **explicite
et prioritaire** ; si absent, le backend calcule `monthly × 12 × (1 − pct/100)` et le signale
(`price_yearly_computed: true`).

#### F9 — `SubscriptionService.handleStripeEvent` = 4 stubs vides
`modules/platform/.../SubscriptionService.java:275-299` — code mort, supplanté par
`StripeBillingWebhookService` (358 lignes). Ne pas y toucher dans ce chantier ; **ne pas** s'appuyer dessus.
Note pour la suite : à supprimer dans une lotie de nettoyage dédiée.

#### F10 — Inscription → jamais de paiement
`PricingPage.tsx:397-407` → `/register?plan=<id>` → `RegisterPage.tsx:56,144` envoie `plan` à `/auth/register`.
Le tenant est créé **sur le plan payant** sans jamais être débité. → **Corrigé par T8.4**
(rediriger vers `/pricing` → checkout, ou orphelin de plan payant sur DISCOVERY).

---

## §2. CONTRATS FONDAMENTAUX (à.implémenter tel quel)

### 2.1 Contrat d'unités de prix — **DÉFINITIF**

> **Toute valeur monétaire stockée, échangée ou envoyée à un prestataire est exprimée en UNITÉS MINEURES ISO-4217.**

| Devise | Décimales | Exemple | Stocké (`amount_minor`) | Affiché |
|---|---|---|---|---|
| XAF, XOF, JPY | 0 | 15 000 FCFA | `15000` | `15 000 FCFA` |
| EUR, USD, CAD, GBP, CHF | 2 | 29,00 € | `2900` | `29,00 €` |

Règles :
1. Le nombre de décimales provient **exclusivement** de
   `modules/currency/domain/Iso4217CurrencyValidator.java:59-61` (`minorUnits(code)`).
   **Jamais** de `* 100` codé en dur (c'est explicitement la raison d'être de cette classe, cf. son javadoc).
2. Conversion : `Iso4217CurrencyValidator.toMinorUnits(code, BigDecimal)` (`:81-84`) et
   `fromMinorUnits(code, long)` (`:87-89`).
3. Toute API de prix renvoie **les deux** : `amountMinor` (entier, autoritaire) **et** `amount`
   (chaîne décimale, pour affichage direct). Le client **ne doit jamais recalculer**.
4. **Conversion du legacy** : les colonnes `saas_plans.price_eur / price_xaf / price_usd / price_monthly /
   price_yearly` sont lues comme des **montants MAJEURS** (confirmé par l'affichage actuel 29 €).
   Migration V245 : `amount_minor = round(price_majeur × 10 ^ minorUnits(devise))`.
5. `price_monthly` / `price_yearly` sans devise propre utilisent `saas_plans.currency`.

### 2.2 Contrat de résolution de région (si `region_pricing_enabled = true`)

Ordre de résolution **strict**, implémenté dans
`modules/platform/domain/PricingRegionResolver.java` :

1. en-tête HTTP de debug/edge : `settings.region_header_name` (défaut `X-Discipolat-Region`) — non lisible par un
   navigateur en cross-origin ⇒ **ignoré sur les appels navigateur** sauf si `settings.trust_region_header = true`
   (défaut `false`) ;
2. `?region=XX` sur l'URL **uniquement si** `settings.allow_region_query_override = true` (défaut `true`) ;
3. localisation du tenant (`tenants.country_code` / `settings_json.region`) ;
4. `settings.default_region` (défaut `AF`).

Un prix régional a `region_code` renseigné ; un prix global a `region_code = 'GLOBAL'`.
Sélection : **prix régional exact > prix GLOBAL de la même devise/cycle**.
Si aucun prix pour la devise demandée et que `show_fallback_currency = true` (défaut `false`) :
repli sur `base_currency`, sinon **« tarif non communiqué »** — jamais un prix inventé.

> ⚠️ **Piège SQL** : `region_code` **ne doit pas être `NULL`** si on veut une contrainte `UNIQUE`
> (en PostgreSQL `NULL <> NULL`, donc deux lignes `NULL` passeraient). Utiliser le sentinelle `'GLOBAL'`.
> Idem pour `currency` : toujours 3 majuscules.

### 2.3 Contrat de canaux de paiement par prix

Chaque ligne `saas_plan_prices` porte `payment_channels VARCHAR(100)` = liste CSV parmi :

| Canal | Constante | Exigence | Cas d'usage |
|---|---|---|---|
| Carte (Stripe) | `CARD` | devise à 2 déc. ⇒ inline possible ; sinon Price ID | EUR, USD, CAD, GBP, CHF |
| Mobile Money | `MOMO` | devise 0-déc. + opérateur configuré + pays couvert | XAF (CM/GA/TD/CG/GQ), XOF (CI/SN/ML/BF/NE/TG) |
| Devise forte | `STRONG` | `strong_currency` configurée ≠ devise d'affichage | XAF/XOF affiché, débit EUR |
| Virement / SEPA | `BANK` | `PayoutProvidersProperties` déjà câblé | EUR zone SEPA |
| Contact | `CONTACT` | aucun | « Sur devis » — **conserve le comportement actuel de la landing** |

- **Ordre d'affichage** si plusieurs canaux : l'ordre du CSV fait foi.
- Le premier canal **disponible** (prestataire activé ET devise supportée) est proposé par défaut.
- Aucun canal disponible ⇒ le CTA devient `CONTACT` (« Nous contacter ») au lieu de planter.

### 2.4 Contrat de figeage du prix (immuabilité)

À la création d'un abonnement, on **fige** le prix :
- `tenant_subscriptions.price_currency`, `.price_minor`, `.price_cycle`,
  `.price_snapshot_json` (copie du plan au moment T),
  `.price_source` (`STRIPE` | `MOMO` | `STRONG` | `MANUAL` | `ADMIN`).
Ainsi, une hausse de tarif par le super-admin **n'affecte pas** les abonnements en cours.
Le backfill (V248) pose `price_source = 'MIGRATION'` pour l'existant.

---

## §3. T1 — MODÈLE DE DONNÉES DES PRIX (backend)

**Objectif** : remplacer les colonnes figées par une table extensible, sans rien supprimer.

### T1.1 Migration `V245__saas_plan_prices.sql`

```sql
-- V245 — Catalogue de prix multi-devises / multi-régions / multi-canaux (T1)
-- ADDITIF : aucune colonne existante n'est supprimée ni modifiée.

CREATE TABLE IF NOT EXISTS saas_plan_prices (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    plan_key              VARCHAR(30) NOT NULL REFERENCES saas_plans(key) ON DELETE CASCADE,
    currency              CHAR(3)     NOT NULL,
    billing_cycle         VARCHAR(20) NOT NULL,          -- MONTHLY | YEARLY
    amount_minor          BIGINT      NOT NULL,          -- CONTRAT §2.1 : unités mineures ISO-4217
    region_code           VARCHAR(10) NOT NULL DEFAULT 'GLOBAL',  -- GLOBAL | AF | EU | NA | US | ...
    checkout_currency     CHAR(3),                      -- canal STRONG : devise réellement débitée
    -- visibilité &choix (paramétrable par le super-admin)
    is_enabled            BOOLEAN NOT NULL DEFAULT TRUE,   -- actif techniquement
    is_visible            BOOLEAN NOT NULL DEFAULT TRUE,   -- affiché sur /pricing et landing
    is_selectable         BOOLEAN NOT NULL DEFAULT TRUE,   -- l'utilisateur PEUT choisir
    sort_order            INTEGER  NOT NULL DEFAULT 0,
    -- canal de paiement : CSV de CARD,MOMO,STRONG,BANK,CONTACT (ordre d'affichage)
    payment_channels      VARCHAR(100) NOT NULL DEFAULT 'CARD',
    -- synchronisation Stripe (canal CARD)
    stripe_price_id       VARCHAR(255),
    stripe_price_status   VARCHAR(30) NOT NULL DEFAULT 'NOT_REQUIRED', -- NOT_REQUIRED|PENDING|SYNCED|FAILED|MANUAL
    stripe_price_synced_at TIMESTAMPTZ,
    stripe_sync_error     TEXT,
    -- traçabilité
    created_by            UUID,
    updated_by            UUID,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata              JSONB NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT ck_plan_price_amount   CHECK (amount_minor >= 0),
    CONSTRAINT ck_plan_price_currency CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_plan_price_cycle    CHECK (billing_cycle IN ('MONTHLY','YEARLY')),
    CONSTRAINT ck_plan_price_channels CHECK (payment_channels <> '')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_plan_price
    ON saas_plan_prices (plan_key, currency, billing_cycle, region_code);

CREATE INDEX IF NOT EXISTS idx_plan_price_lookup
    ON saas_plan_prices (currency, billing_cycle, is_enabled, is_visible);

CREATE INDEX IF NOT EXISTS idx_plan_price_stripe_pending
    ON saas_plan_prices (stripe_price_status)
    WHERE stripe_price_status IN ('PENDING','FAILED');

COMMENT ON TABLE saas_plan_prices IS
  'Prix par plan x devise x cycle x région. amount_minor en unités mineures ISO-4217 (contrat §2.1).';
```

### T1.2 Migration `V246__saas_pricing_settings.sql`

Table **singleton** (1 seule ligne, `id SMALLINT CHECK (id = 1)`) regroupant **tous** les interrupteurs.

```sql
-- V246 — Réglages plateforme de la tarification (singleton, T1)
CREATE TABLE IF NOT EXISTS saas_pricing_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),

    -- ── Devises ────────────────────────────────────────────────────────────
    base_currency            CHAR(3) NOT NULL DEFAULT 'XAF',
    default_display_currency CHAR(3) NOT NULL DEFAULT 'XAF',
    enabled_currencies       CHAR(3)[] NOT NULL DEFAULT '{XAF,XOF,EUR,USD,CAD,GBP,CHF}',
    show_fallback_currency   BOOLEAN NOT NULL DEFAULT FALSE,
    allow_user_currency_choice BOOLEAN NOT NULL DEFAULT TRUE,

    -- ── Modes d'affichage ──────────────────────────────────────────────────
    display_mode             VARCHAR(30) NOT NULL DEFAULT 'SINGLE',
        -- SINGLE     : un prix affiché, sélecteur de devise
        -- SWITCHER   : sélecteur de devise toujours visible
        -- LIST_ALL   : tous les prix du plan empilés (comportement actuel)
        -- AUTO_REGION: prix de la région détectée, repli GLOBAL
    allow_cycle_choice       BOOLEAN NOT NULL DEFAULT TRUE,   -- bascule mensuel/annuel
    default_cycle            VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    allow_monthly            BOOLEAN NOT NULL DEFAULT TRUE,
    allow_yearly             BOOLEAN NOT NULL DEFAULT TRUE,
    allow_region_choice      BOOLEAN NOT NULL DEFAULT FALSE,  -- l'utilisateur choisit sa région

    -- ── Régions ────────────────────────────────────────────────────────────
    region_pricing_enabled   BOOLEAN NOT NULL DEFAULT FALSE,
    region_resolution        VARCHAR(30) NOT NULL DEFAULT 'TENANT',
        -- TENANT | HEADER | QUERY | IP
    region_header_name       VARCHAR(64) NOT NULL DEFAULT 'X-Discipolat-Region',
    trust_region_header      BOOLEAN NOT NULL DEFAULT FALSE,
    allow_region_query_override BOOLEAN NOT NULL DEFAULT TRUE,
    default_region           VARCHAR(10) NOT NULL DEFAULT 'AF',

    -- ── Canaux de paiement ─────────────────────────────────────────────────
    allow_card_channel       BOOLEAN NOT NULL DEFAULT TRUE,
    allow_momo_channel       BOOLEAN NOT NULL DEFAULT FALSE,
    allow_strong_channel     BOOLEAN NOT NULL DEFAULT TRUE,
    allow_bank_channel       BOOLEAN NOT NULL DEFAULT TRUE,
    allow_contact_channel    BOOLEAN NOT NULL DEFAULT TRUE,
    strong_currency          CHAR(3) NOT NULL DEFAULT 'EUR',
    momo_operators           VARCHAR(50)[] NOT NULL DEFAULT '{MTN_MOMO,ORANGE_MONEY,M_PESA}',

    -- ── Stripe ─────────────────────────────────────────────────────────────
    stripe_price_auto_sync   BOOLEAN NOT NULL DEFAULT FALSE,
    stripe_sync_batch_size   INTEGER NOT NULL DEFAULT 20,
    stripe_sync_interval_ms  BIGINT  NOT NULL DEFAULT 300000,

    -- ── Essai / remise ─────────────────────────────────────────────────────
    trial_enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    annual_discount_default  INTEGER NOT NULL DEFAULT 17,
    price_display_precision  INTEGER NOT NULL DEFAULT 0,  -- 0 = décimales ISO-4217

    -- ── Landing ────────────────────────────────────────────────────────────
    landing_pricing_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    landing_pricing_source       VARCHAR(20) NOT NULL DEFAULT 'API',  -- API | STATIC
    landing_pricing_max_plans    INTEGER NOT NULL DEFAULT 4,
    landing_pricing_show_prices  BOOLEAN NOT NULL DEFAULT TRUE,
    landing_pricing_show_toggle  BOOLEAN NOT NULL DEFAULT TRUE,
    landing_pricing_cta_label    VARCHAR(60),

    -- ── Journalisation ─────────────────────────────────────────────────────
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by UUID
);

INSERT INTO saas_pricing_settings (id) VALUES (1) ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE saas_pricing_settings IS
  'Réglages plateforme de la tarification (singleton). Toute la paramétrisation T1-T7 vit ici.';
```

### T1.3 Migration `V247__plan_price_columns_and_backfill.sql`

```sql
-- V247 — Colonnes de traçabilité sur saas_plans + backfill du catalogue de prix (T1)

ALTER TABLE saas_plans
    ADD COLUMN IF NOT EXISTS stripe_product_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pricing_model VARCHAR(20) NOT NULL DEFAULT 'PER_CURRENCY',
        -- PER_CURRENCY | PER_REGION
    ADD COLUMN IF NOT EXISTS max_currencies    INTEGER NOT NULL DEFAULT 3;

-- Backfill : lecture des colonnes legacy comme des MONTANTS MAJEURS (§2.1 règle 4).
-- EUR/USD = 2 décimales → ×100 ; XAF/XOF = 0 décimale → ×1.
INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code, is_enabled, is_visible,
     is_selectable, sort_order, payment_channels, stripe_price_status, metadata)
SELECT p.key, 'EUR', 'MONTHLY', ROUND(p.price_eur::numeric * 100)::bigint, 'GLOBAL',
       p.is_active, p.is_public, p.is_public, p.sort_order * 10 + 1, 'CARD',
       'NOT_REQUIRED', jsonb_build_object('origin', 'V247', 'legacy_column', 'price_eur')
FROM saas_plans p WHERE p.price_eur IS NOT NULL
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code, is_enabled, is_visible,
     is_selectable, sort_order, payment_channels, stripe_price_status, metadata)
SELECT p.key, 'XAF', 'MONTHLY', p.price_xaf::bigint, 'GLOBAL',
       p.is_active, p.is_public, p.is_public, p.sort_order * 10 + 2,
       -- STARTUP = plan XAF : Stripe inline impossible → MOMO prioritaire
       CASE WHEN p.currency = 'XAF' THEN 'MOMO,STRONG' ELSE 'CARD' END,
       CASE WHEN p.currency = 'XAF' THEN 'PENDING' ELSE 'NOT_REQUIRED' END,
       jsonb_build_object('origin', 'V247', 'legacy_column', 'price_xaf')
FROM saas_plans p WHERE p.price_xaf IS NOT NULL
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code, is_enabled, is_visible,
     is_selectable, sort_order, payment_channels, stripe_price_status, metadata)
SELECT p.key, 'USD', 'MONTHLY', ROUND(p.price_usd::numeric * 100)::bigint, 'GLOBAL',
       p.is_active, p.is_public, p.is_public, p.sort_order * 10 + 3, 'CARD',
       'NOT_REQUIRED', jsonb_build_object('origin', 'V247', 'legacy_column', 'price_usd')
FROM saas_plans p WHERE p.price_usd IS NOT NULL
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

-- Annuel : price_yearly est exprimé dans saas_plans.currency.
INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code, is_enabled, is_visible,
     is_selectable, sort_order, payment_channels, stripe_price_status, metadata)
SELECT p.key, p.currency, 'YEARLY',
       CASE p.currency WHEN 'XAF' THEN p.price_yearly::bigint
                       WHEN 'XOF' THEN p.price_yearly::bigint
                       ELSE ROUND(p.price_yearly::numeric * 100)::bigint END,
       'GLOBAL', p.is_active, p.is_public, p.is_public, p.sort_order * 10 + 4,
       CASE WHEN p.currency IN ('XAF','XOF') THEN 'MOMO,STRONG' ELSE 'CARD' END,
       CASE WHEN p.currency IN ('XAF','XOF') THEN 'PENDING' ELSE 'NOT_REQUIRED' END,
       jsonb_build_object('origin', 'V247', 'legacy_column', 'price_yearly')
FROM saas_plans p
WHERE p.price_yearly IS NOT NULL AND p.price_yearly > 0
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

-- Markage des lignes 0-décimales nécessitant un Price Stripe (Q3a).
UPDATE saas_plan_prices
   SET stripe_price_status = 'PENDING'
 WHERE currency IN ('XAF','XOF') AND payment_channels LIKE '%CARD%';

-- Alimentation de enabled_currencies à partir de l'existant.
UPDATE saas_pricing_settings
   SET enabled_currencies = ARRAY(
         SELECT DISTINCT currency FROM saas_plan_prices ORDER BY currency)
 WHERE id = 1 AND cardinality(enabled_currencies) = 0;
```

### T1.4 Migration `V248__subscription_price_snapshot.sql`

```sql
-- V248 — Figeage du prix sur l'abonnement (contrat §2.4)
ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS price_currency   CHAR(3),
    ADD COLUMN IF NOT EXISTS price_minor      BIGINT,
    ADD COLUMN IF NOT EXISTS price_cycle      VARCHAR(20),
    ADD COLUMN IF NOT EXISTS price_source     VARCHAR(20) NOT NULL DEFAULT 'MIGRATION',
    ADD COLUMN IF NOT EXISTS price_snapshot_json JSONB;

UPDATE tenant_subscriptions ts
   SET price_source = 'MIGRATION',
       price_snapshot_json = jsonb_build_object(
           'migrated_at', CURRENT_TIMESTAMP,
           'plan_key', ts.plan_key,
           'note', 'Aucun prix figé avant T1 : le tarif sera figé au prochain renouvellement.')
 WHERE ts.price_source = 'MIGRATION';

COMMENT ON COLUMN tenant_subscriptions.price_minor IS
  'Prix figé en unités mineures ISO-4217 au moment de la souscription (§2.4).';
```

### T1.5 Entités JPA

**`backend/.../modules/tenants/domain/SaasPlanPrice.java`** (nouveau)

```java
package com.discipolat.modules.tenants.domain;

@Entity
@Table(name = "saas_plan_prices",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_plan_price",
           columnNames = {"plan_key", "currency", "billing_cycle", "region_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaasPlanPrice {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id") private UUID id;

    @Column(name = "plan_key", nullable = false, length = 30) private String planKey;

    @Column(name = "currency", nullable = false, length = 3) private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20) private BillingCycle billingCycle;

    /** CONTRAT §2.1 : unités mineures ISO-4217. Ne jamais lire/écrire un montant majeur ici. */
    @Column(name = "amount_minor", nullable = false) private Long amountMinor;

    @Column(name = "region_code", nullable = false, length = 10) private String regionCode;

    @Column(name = "checkout_currency", length = 3) private String checkoutCurrency;

    @Column(name = "is_enabled", nullable = false) @Builder.Default private Boolean isEnabled = true;
    @Column(name = "is_visible", nullable = false) @Builder.Default private Boolean isVisible = true;
    @Column(name = "is_selectable", nullable = false) @Builder.Default private Boolean isSelectable = true;
    @Column(name = "sort_order", nullable = false) @Builder.Default private Integer sortOrder = 0;

    @Column(name = "payment_channels", nullable = false, length = 100) @Builder.Default
    private String paymentChannels = "CARD";

    @Column(name = "stripe_price_id") private String stripePriceId;
    @Enumerated(EnumType.STRING)
    @Column(name = "stripe_price_status", nullable = false, length = 30) @Builder.Default
    private StripePriceStatus stripePriceStatus = StripePriceStatus.NOT_REQUIRED;
    @Column(name = "stripe_price_synced_at") private Instant stripePriceSyncedAt;
    @Column(name = "stripe_sync_error", columnDefinition = "TEXT") private String stripeSyncError;

    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;

    @Column(name = "metadata", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON) @Builder.Default private String metadata = "{}";

    @PrePersist protected void onCreate() { this.createdAt = Instant.now(); this.updatedAt = Instant.now(); }
    @PreUpdate  protected void onUpdate() { this.updatedAt = Instant.now(); }

    public List<String> channels() {
        return Arrays.stream(paymentChannels.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Override public boolean equals(Object o) {
        return o instanceof SaasPlanPrice p && id != null && id.equals(p.id);
    }
    @Override public int hashCode() { return getClass().hashCode(); }
}
```

Énumérations **nouvelles** (`modules/tenants/enums/`) :
- `BillingCycle { MONTHLY, YEARLY }` — helpers `fromString(String)` tolérant (`"yearly"`, `"YEARLY"`, `"annual"` ⇒ `YEARLY`).
- `StripePriceStatus { NOT_REQUIRED, PENDING, SYNCED, FAILED, MANUAL }`.
- `PaymentChannel { CARD, MOMO, STRONG, BANK, CONTACT }` — helpers `csv()` / `parse(String)`.
- `PricingDisplayMode { SINGLE, SWITCHER, LIST_ALL, AUTO_REGION }`.
- `PricingRegionSource { TENANT, HEADER, QUERY, IP }`.

**Modifications `additives` à `SaasPlan.java`** (ne rien retirer) :
- `+ stripeProductId` (colonne `stripe_product_id`)
- `+ pricingModel` (`pricing_model`, défaut `PER_CURRENCY`)
- `+ maxCurrencies` (`max_currencies`, défaut 3)
- `+ @OneToMany(mappedBy = "planKey", ...) List<SaasPlanPrice> prices` — **`fetch = LAZY`, `cascade` AUCUN**
  (la cascade serait dangereuse : la clé du plan est l'ID, un `CascadeType.REMOVE` malencontreux effacerait l'historique de prix).

### T1.6 Repositories

**`SaasPlanPriceRepository`** (nouveau, `modules/tenants/domain/`) :

```java
public interface SaasPlanPriceRepository extends JpaRepository<SaasPlanPrice, UUID> {
    List<SaasPlanPrice> findByPlanKey(String planKey);
    List<SaasPlanPlanPrice> findByPlanKeyIn(Collection<String> planKeys);
    Optional<SaasPlanPrice> findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(
            String planKey, String currency, BillingCycle cycle, String regionCode);
    List<SaasPlanPrice> findByCurrencyAndBillingCycleAndIsEnabledTrueAndIsVisibleTrue(
            String currency, BillingCycle cycle);
    List<SaasPlanPrice> findByStripePriceStatusIn(Collection<StripePriceStatus> statuses);
    List<SaasPlanPrice> findByStripePriceStatus(StripePriceStatus status);
    boolean existsByPlanKey(String planKey);
    void deleteByPlanKey(String planKey);
}
```

**`PricingSettingsRepository`** (nouveau) :
```java
public interface PricingSettingsRepository extends JpaRepository<SaasPlanPricingSettings, Short> {
    Optional<SaasPlanPricingSettings> findTopByOrderByIdAsc();
}
```
+ une méthode `findOrCreate()` dans le service, avec `@Cacheable` invalidé au write (voir T3.4).

**⚠️ Filtrage tenant** : `saas_plan_prices` et `saas_pricing_settings` sont des tables **plateforme globales**
(comme `saas_plans`). **Ne pas** ajouter de `tenant_id` ni de `@Filter` dessus. Documenter dans
`docs/ARCHITECTURE.md`.

### T1.7 Tests backend

- `SaasPlanPriceMigrationTest` (Testcontainers PostgreSQL) : V144→V247 produit bien 8 lignes de prix avec les bons
  `amount_minor` (STARTUP XAF monthly = 15000 ; GROWTH EUR monthly = **2900**, pas 29 ; NETWORK USD = **9900**).
- `Iso4217MinorUnitsTest` : XAF=0, XOF=0, EUR=2, CAD=2, GBP=2, CHF=2, JPY=0.
- `SaasPlanPriceRepositoryTest` : unicité `uq_plan_price`, `region_code='GLOBAL'` par défaut.

---

## §4. T2 — SERVICE TARIFICATION (backend, cœur métier)

**Fichier** : `backend/.../modules/tenants/domain/PricingCatalogService.java` (nouveau).

### T2.1 `resolvePrice(planKey, currency, cycle, region)` — implémentation de §2.2

```java
public Optional<ResolvedPrice> resolvePrice(String planKey, String requestedCurrency,
                                            BillingCycle cycle, String regionCode) {
    PricingSettings s = settings();
    String currency = normalize(requestedCurrency);
    if (!s.getEnabledCurrencies().contains(currency)) return Optional.empty();

    // 1) prix régional exact
    if (s.isRegionPricingEnabled() && regionCode != null && !regionCode.isBlank()) {
        var regional = repo.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(
                planKey, currency, cycle, normalizeRegion(regionCode));
        if (regional.isPresent()) return toResolved(regional.get(), true);
    }
    // 2) prix GLOBAL
    var global = repo.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(
            planKey, currency, cycle, PricingSettings.GLOBAL_REGION);
    if (global.isPresent()) return toResolved(global.get(), false);

    // 3) repli optionnel (désactivé par défaut) — jamais de prix inventé
    if (s.isShowFallbackCurrency()) {
        var base = repo.findByPlanKeyAndCurrencyAndBillingCycleAndRegionCode(
                planKey, s.getBaseCurrency(), cycle, PricingSettings.GLOBAL_REGION);
        if (base.isPresent()) return toResolved(base.get(), false);   // isFallback = true
    }
    return Optional.empty();
}
```

`ResolvedPrice` (record) :
```java
public record ResolvedPrice(
        String planKey, String currency, BillingCycle cycle, String regionCode,
        long amountMinor, String amount,            // "2900" / "29.00" — VÉRIFIÉ ISO-4217
        int decimals,
        boolean isFallback, boolean isRegional,
        boolean selectable, boolean visible, String checkoutCurrency,
        List<PaymentChannel> channels) {
    public BigDecimal amountMajor() { return BigDecimal.valueOf(amountMinor, decimals); }
}
```

### T2.2 Correction du bug F1 — `StripeBillingService`

Dans `createCheckoutSession` (`StripeBillingService.java:63-134`), remplacer la lecture
`plan.getPriceMonthly()/getPriceYearly()` (lignes 81-113) par :
1. `PricingCatalogService.resolvePrice(planKey, currencyDemandée, cycle, region)` ;
2. si aucun prix ⇒ `PLAN_PRICE_MISSING` (code existant, conserver) ;
3. `unit_amount` ← **`amountMinor`** (contrat §2.1) ;
4. si un `stripe_price_id` existe sur la ligne de prix ⇒ `setPrice(priceId)` (prioritaire) ;
5. sinon devise 2-déc. ⇒ `price_data` inline ;
6. sinon ⇒ erreur explicite **détaillée** :
   `STRIPE_PRICE_ID_REQUIRED` enrichi → `"Le prix XAF de STARTUP requiert une synchronisation Stripe (statut: PENDING)"`.

**Garde-fou anti-régression F1** : test unitaire
`createCheckoutSession_usesMinorUnits()` qui vérifie qu'un plan EUR à `29` **majeurs** produit
`unit_amount == 2900`.

### T2.3 `PricingSettingsService` (nouveau, `modules/platform/domain/`)

CRUD + garde-fous :
- `base_currency` et `default_display_currency` doivent être dans `enabled_currencies` et valides ISO-4217
  (`Iso4217CurrencyValidator.require`) ;
- `strong_currency` ne doit pas être une devise 0-décimale (inutile pour le canal STRONG) — *avertissement*, pas refus ;
- `default_cycle` doit être autorisé (`allow_monthly` / `allow_yearly`) ;
- `default_region` doit exister dans le référentiel `RegionCatalog` (§2.2) ;
- **jamais** de suppression de devise encore utilisée par au moins une ligne `saas_plan_prices` ⇒ 409 + liste.

### T2.4 `PricingRegionResolver` (nouveau)

Implémente §2.2. Signe : `Optional<String> resolve(HttpServletRequest req, RegionSource source, String queryRegion, Tenant tenant)`.
Référentiel régions (constante) :
`AF`(Afrique), `NA`(Afrique du Nord), `EU`(Europe), `UK`(Royaume-Uni), `US`(Amérique du Nord), `CA`(Canada),
`LATAM`, `ASIA`, `OC`, `GLOBAL`.
Équivalences pays→région : CM/GA/TD/CG/GQ/BJ ⇒ AF ou NA selon config ; CI/SN/ML/BF/NE/TG ⇒ AF ; FR/DE/ES/IT/… ⇒ EU ;
GB ⇒ UK ; US ⇒ US ; CA ⇒ CA.

### T2.5 Tests backend

- `resolvePrice_globalFallback` / `_regionalPrecedence` / `_noFallbackByDefault` / `_unsupportedCurrency`.
- `resolvePrice_missingPriceYieldsEmpty_notZero` (**jamais de 0€ bidon**).
- `PricingSettingsService` : devise de base hors `enabled_currencies` ⇒ 400.

---

## §5. T3 — API ADMIN & API PUBLIQUE (backend)

### T3.1 `PublicPricingController` (nouveau) — `GET /api/v1/public/pricing/*`

| Route | Réponse | Cache |
|---|---|---|
| `GET /config` | Réglages publics (voir §5.2) | 300 s |
| `GET /plans?currency=XAF&cycle=MONTHLY&region=AF` | Catalogue résolu | 300 s |
| `GET /plans/{key}?…` | Un plan | 300 s |
| `GET /currency` | Devise + région **résolues pour cet appelant** (détection) | `no-store` |

> ⚠️ **Correction F3** : `PublicSaasPlanController.getPublicPlans()` (`:28-31`) doit **déléguer** à
> `PricingCatalogService.publicPlans(...)`. Le filtre `PUBLIC_PLAN_KEYS` (dans
> `PublicSaasPlanController.java:29` **et** `SaasPlanService.java:46`) disparaît de la couche service ; le nouveau
> filtre est `is_public = true AND is_active = true AND status NOT IN ('INACTIVE','DEPRECATED')`
> — **inchangé en l'état actuel** (T3.2), mais la liste figée disparaît.
> `SaasPlanService.PUBLIC_PLAN_KEYS` reste déclaré (constante publique, usages éventuels) mais n'est plus utilisé
> comme filtre.
>
> `GET /api/v1/public/plans` **reste exposée** et reste compatible : on ajoute les nouveaux champs
> (`prices: [...]`, `display`) **sans retirer** `priceMonthly`/`priceEur`/`priceXaf`/`priceUsd`/`currency`.

### T3.2 Contrat `GET /api/v1/public/pricing/config`

```json
{
  "displayMode": "SWITCHER",
  "baseCurrency": "XAF",
  "defaultCurrency": "XAF",
  "resolvedCurrency": "XAF",
  "resolvedRegion": "AF",
  "regionPricingEnabled": false,
  "allowCurrencyChoice": true,
  "allowCycleChoice": true,
  "defaultCycle": "MONTHLY",
  "enabledCurrencies": [
    { "code": "XAF", "symbol": "FCFA", "decimals": 0, "name": "Franc CFA (BEAC)", "hasPrice": true },
    { "code": "XOF", "symbol": "F CFA", "decimals": 0, "name": "Franc CFA (UEM)", "hasPrice": false }
  ],
  "allowRegionChoice": false,
  "regions": [
    { "code": "AF", "labelKey": "pricing.region.AF" },
    { "code": "EU", "labelKey": "pricing.region.EU" }
  ],
  "channels": { "card": true, "momo": false, "strong": true, "bank": true, "contact": true },
  "trialEnabled": true,
  "landing": { "enabled": true, "showPrices": true, "showToggle": true, "ctaLabel": null },
  "stripeEnabled": false,
  "i18nVersion": 7
}
```

> `hasPrice` par devise permet d'afficher « indisponible » proprement plutôt que « tarif non communiqué ».

### T3.3 Contrat `GET /api/v1/public/pricing/plans`

```json
{
  "currency": "XAF",
  "cycle": "MONTHLY",
  "region": "AF",
  "plans": [
    {
      "key": "STARTUP",
      "name": "Démarrage",
      "description": "…",
      "sortOrder": 2,
      "isPublic": true,
      "popular": false,
      "badge": null,
      "trialDays": 30,
      "annualDiscountPct": 17,
      "limits": { "members": 200, "spaces": 10, "storageMb": 5000, "aiCredits": 500, "events": 50 },
      "features": { "analytics": true, "finance": true, "mobile_money": true },
      "modules": ["dashboard","members", "…"],
      "prices": [
        { "currency": "XAF", "cycle": "MONTHLY", "amountMinor": 15000, "amount": "15000",
          "decimals": 0, "region": "GLOBAL", "isRegional": false, "selectable": true,
          "channels": ["MOMO","STRONG"], "checkoutCurrency": "EUR",
          "stripePriceStatus": "PENDING", "fromFallback": false }
      ],
      "display": {
        "available": true,
        "amountMinor": 15000, "amount": "15000", "currency": "XAF", "cycle": "MONTHLY",
        "isFallback": false, "channels": ["MOMO","STRONG"]
      }
    }
  ]
}
```

Garanties : `display.available = false` ⇒ le frontend affiche « tarif non communiqué » / « nous contacter »,
**jamais** `0`.

### T3.4 `SuperAdminPricingController` (nouveau) — `GET/PUT /api/v1/platform/admin/pricing/settings`

`@PreAuthorize("@authz.isPlatformSuperAdmin()")`, `PlatformAdminAuditLog` pour chaque écriture
(action `PRICING_SETTINGS_UPDATED`, diff avant/après dans `details_json`).

PUT : **PUT sémantique complet** (le corps est l'état desired). Toutes les clés présentes sont écrites ;
`PartialUpdate` non supporté volontairement (évite les surprises). Validation via `PricingSettingsService` (§2.3).

### T3.5 `SuperAdminPlanPriceController` (nouveau) — CRUD prix

| Route | Rôle |
|---|---|
| `GET /api/v1/platform/admin/plans/{key}/prices` | Tous les prix du plan (tous statuts) |
| `POST /api/v1/platform/admin/plans/{key}/prices` | Créer (upsert si conflit `uq_plan_price`) |
| `PUT /api/v1/platform/admin/plans/{key}/prices/{priceId}` | Modifier — **accepte `null` explicite** |
| `DELETE /api/v1/platform/admin/plans/{key}/prices/{priceId}` | Supprimer (soft-preféré : `is_enabled=false`) |
| `POST /api/v1/platform/admin/plans/{key}/prices/{priceId}/stripe-sync` | Force la synchro Stripe |
| `POST /api/v1/platform/admin/plans/{key}/prices/bulk-currency` | Remplir N devises en une fois |
| `GET /api/v1/platform/admin/pricing/currencies` | Catalogue ISO-4217 + `hasPrice` par plan |

> **Correction F7** : l'upsert de prix utilise un `Map<String,Object>` avec détection explicite
> `containsKey("amountMinor")` — un `null` explicite **supprime** le champ (ou le prix si `deleteMissing`).
> Les records Java à `Long` ne peuvent pas distinguer « absent » de « null » : c'est la raison du `Map`.

DTO d'écriture :

```java
public record UpsertPlanPriceRequest(
        String currency,              // requis, ISO-4217 majuscules
        String billingCycle,          // MONTHLY | YEARLY
        Long   amountMinor,           // REQUIS — unités mineures. Rejeté si décimales invalides.
        String regionCode,            // défaut GLOBAL
        String checkoutCurrency,      // canal STRONG
        Boolean isEnabled, Boolean isVisible, Boolean isSelectable,
        Integer sortOrder,
        String paymentChannels,       // CSV
        String stripePriceId,         // saisie manuelle possible (canal MANUAL)
        Map<String, Object> metadata
) {}
```

**Validation serveur des montants** : `Iso4217CurrencyValidator.validateAmount(currency,
BigDecimal.valueOf(amountMinor, decimals))` — rejette par exemple `29.5` en XAF.

### T3.6 Prix annuel — règle F8

`resolveYearly()` : si un prix `YEARLY` existe → utilisé tel quel (`computed = false`).
Sinon si `annual_discount_pct > 0` et un prix `MONTHLY` existe :
`amountMinor = round(monthly × 12 × (100 − pct) / 100)` aligné sur les décimales de la devise, `computed = true`.
Réponse : champ `"annualComputed": true` pour que le frontend affiche une mention discrète.

### T3.7 Cache

`PublicSaasPlanController` pose `Cache-Control: max-age=300` (`PublicSaasPlanController.java:32`).
**Invalider côté CDN/proxy** : la nouvelle clé `pricing/config` doit être incluse dans la purge lors d'un
`PUT /pricing/settings` ou d'un upsert de prix. Implémentation simple et suffisante :
`Cache-Control: max-age=60` sur `/pricing/config` (l'admin est rare) et invalider par un
`@Scheduled` qui bascule une version dans le JSON (`i18nVersion` → renommé `catalogVersion`) que le client
interroge au chargement. **Décision** : `catalogVersion` incrémentée à chaque écriture admin, exposée dans
`/pricing/config` et lue par le frontend ; le frontend ne dépend **pas** d'un WebSocket.

### T3.8 Tests

- `PublicPricingControllerIT` : plan custom `is_public=true` **apparaît** (régression F3).
- `UpsertPlanPrice_eraseWithExplicitNull` (régression F7).
- `PublicPricingControllerIT` : XOF demandé, aucun prix XOF, `showFallbackCurrency=false` ⇒ `display.available=false`.
- `Contract`: `PublicPlanResponse` conserve tous les champs legacy (test de non-régression du contrat).

---

## §6. T6 — CANAUX DE PAIEMENT (Stripe · devise forte · Mobile Money)

### T6.1 `StripePriceSyncService` (nouveau) — **Q3 (a)**

`backend/.../modules/payments/stripe/StripePriceSyncService.java`

Responsabilités :
1. **Produit** : `Product.create()` une fois par plan (`product_data[plan_key] = <stripe_product_id>` dans
   `saas_plans.stripe_product_id`, nouvelle colonne V247).
2. **Prix** : `Price.create()` par ligne de prix nécessitant un Price ID.

```java
PriceCreateParams params = PriceCreateParams.builder()
    .setProduct(productId)
    .setCurrency(price.getCurrency().toLowerCase(Locale.ROOT))   // Stripe attend minuscules
    .setUnitAmount(price.getAmountMinor())                        // CONTRAT §2.1
    .setRecurring(PriceCreateParams.Recurring.builder()
            .setInterval(cycle == BillingCycle.YEARLY
                    ? PriceCreateParams.Recurring.Interval.YEAR
                    : PriceCreateParams.Recurring.Interval.MONTH)
            .build())
    .setMetadata(Map.of(
            "plan_key",     price.getPlanKey(),
            "currency",     price.getCurrency(),
            "cycle",        price.getBillingCycle().name(),
            "region_code",  price.getRegionCode()))
    .setActive(true)
    .build();
RequestOptions opts = RequestOptions.builder()
        .setApiKey(props.getSecretKey())
        .setIdempotencyKey("discipolat:" + price.getPlanKey() + ":" + price.getCurrency()
                + ":" + price.getBillingCycle() + ":" + price.getRegionCode() + ":" + price.getAmountMinor())
        .build();
Price created = Price.create(params, opts);
```

3. **Idempotence** : la clé ci-dessus rend la création sûre en cas de retry ; un `stripe_price_id` déjà
   présent **et** de même montant ⇒ pas d'appel.
4. **Changement de montant** : ne pas créer un 2ᵉ Price si `metadata.price_id` existe déjà avec un autre
   montant ⇒ créer le nouveau puis **`PriceUpdateParams` → `active=false`** sur l'ancien
   (`Price.retrieve(oldId).update(PriceUpdateParams.builder().setActive(false).build())`).
   Les abonnements actifs rattachés à l'ancien Price **restent valides** (Stripe ne les casse pas).
5. **Statut** : `SYNCED` + `stripe_price_synced_at` ; en cas d'échec `FAILED` + `stripe_sync_error` (tronqué à 2 000 car.)
6. **Retry** : `@Scheduled(fixedDelayString = "${app.stripe.price-sync-interval-ms:300000}")`
   qui reprend les lignes `PENDING`/`FAILED`, par lots de `stripe_sync_batch_size`.
7. **Fail-closed** : si `!props.isEnabled()` (pas de `STRIPE_SECRET_KEY`) ⇒ statut `PENDING` conservé,
   **aucun appel réseau**, log.warn. Ne jamais faire semblant.
8. **Déclenchement** : automatique si `saas_pricing_settings.stripe_price_auto_sync = true`, sinon manuel via
   `POST .../stripe-sync`, sinon le super-admin saisit l'ID à la main (`stripe_price_status = MANUAL`).

Propriétés à ajouter dans `application.yml` (§ `app.stripe`) :
```yaml
    # Synchronisation automatique des Prices (Q3a). false = le super-admin déclenche
    # ou saisit les Price IDs manuellement.
    price-sync:
      auto: ${STRIPE_PRICE_SYNC_AUTO:false}
      interval-ms: ${STRIPE_PRICE_SYNC_INTERVAL_MS:300000}
      batch-size: ${STRIPE_PRICE_SYNC_BATCH_SIZE:20}
```
+ `app.stripe.api-version` optionnelle (`${STRIPE_API_VERSION:}`) — à laisser vide par défaut (SDK 24.16.0).

### T6.2 Canal devise forte — **Q3 (d)**

Le prix affiché est en XAF ; le débit est en `settings.strong_currency` (EUR par défaut).
- `SaasPlanPrice.checkoutCurrency` porte la devise de débit.
- `amountMinor` de débit : **conversion à la charge du super-admin**, pas de taux automatique (cohérence avec Q2 (a)).
  L'admin saisit un **second prix** en devise forte si le montant diffère (il ne le fait que s'il le souhaite) ;
  sinon le backend applique un **ratio fixe configurable** :
  `saas_pricing_settings.strong_rate_ratio NUMERIC(18,8)` (défaut `NULL` = pas de conversion, et alors
  `STRONG` est refusé sur ce prix avec un message clair).
  > Cette option est volontairement **opt-in** : sans taux de change fiable (cf. §1.4 « aucun service de taux »),
  > convertir automatiquement serait une source d'erreur de facturation.
- Le checkout Stripe utilise alors `currency = checkoutCurrency`, `unit_amount = amountMinor`.
- Affichage : prix en XAF + mention discrète « débit en € » si `checkoutCurrency ≠ currency`.

### T6.3 Canal Mobile Money — **Q3 (c)**

⚠️ **Il n'existe aujourd'hui AUCUN abonnement SaaS par Mobile Money** : `RecurringDonation` sert la dîme.
On **ajoute** un chemin parallèle, on ne modifie pas le chemin dîme.

1. **Nouvel `Purpose`** dans `PaymentIntent.Purpose` (`modules/payments/domain/PaymentIntent.java`) :
   `SAAS_SUBSCRIPTION`. **Migration `V249__payment_intent_saas_subscription.sql`** qui élargit la contrainte
   `CHECK` existante (`DROP CONSTRAINT … ; ADD CONSTRAINT … IN (…, 'SAAS_SUBSCRIPTION')`).
   ⚠️ **reprendre la contrainte exacte** en lisant `V93__growth_engagement_tables.sql:68-88` **et** toute
   migration ultérieure qui l'ait modifiée — chercher `payment_intents` + `CHECK`.
2. **Nouvelle table `V250__saas_momo_subscriptions.sql`** :
   `id UUID PK, tenant_id UUID FK, plan_key, price_id UUID FK, plan_price_minor BIGINT, currency CHAR(3),
   operator VARCHAR(30), phone VARCHAR(32), status VARCHAR(30), next_charge_at TIMESTAMPTZ,
   provider_reference VARCHAR(255), metadata JSONB, created_at, updated_at`,
   index unique partiel `(tenant_id) WHERE status IN ('ACTIVE','PENDING')`.
3. **`MomoSubscriptionService`** : `initiate()` → `MobileMoneyProviderRegistry.find(operator).initiate(...)`
   avec un `PaymentIntent` `Purpose.SAAS_SUBSCRIPTION`, puis activation par le **webhook existant**
   (`PaymentGatewayService.handleWebhook` → statut `CONFIRMED` ⇒ passer la commande en `ACTIVE` +
   `SaasPlanService.subscribe(...)`).
4. **Renouvellement** : `@Scheduled` qui reprend `next_charge_at <= now` **et** dont l'opérateur supporte la
   tokenisation (MTN MoMo : oui ; Orange : selon compte ; M-Pesa : STK Push = non récurrent ⇒
   **exclure M-Pesa du canal récurrent**, `payment_channels` ne doit proposer que des opérateurs compatibles —
   vérifier `MpesaProvider` avant d'autoriser).
5. **Fail-closed** : si `saas_pricing_settings.allow_momo_channel = false`, le canal `MOMO` est retiré de la
   réponse publique **et** le service refuse l'initiation.

### T6.4 Sélection de canal côté serveur

Nouveau `PaymentChannelResolver` : given `(planPrice, settings, HttpServletRequest)` ⇒
`List<AvailableChannel>` en ne listant que les canaux **activés ET techniquement possibles**
(`StripeBillingProperties.isEnabled()`, opérateur MoMo actif, `PayoutProvidersProperties` pour SEPA).
C'est **la seule source de vérité** ; le frontend ne fait que la refléter (pas de logique de disponibilité
dupliquée côté client).

### T6.5 Tests

- `StripePriceSyncServiceTest` (mock `Price.create`) : idempotence, désactivation de l'ancien Price,
  statut `FAILED` + message persisté, **aucun appel** si clé absente (fail-closed).
- `PaymentChannelResolverTest` : XAF sans Price ID + Stripe activé ⇒ `STRONG` proposé si `strong_currency` dispo.
- `MomoSubscriptionServiceTest` : M-Pesa exclu du récurrent.

---

## §7. T5 — ESPACE SUPER-ADMIN : GESTION DES PLANS & TARIFS (frontend)

### T5.1 Route

`frontend/src/App.tsx:1222` — route existante `/platform/saas/plans` (`<ProtectedRoute scope="platform">`).
**Ajouter** `/platform/saas/pricing` (réglages) à côté, et l'entrée de menu dans
`frontend/src/workspaces.ts:210` (« Plans SaaS » → groupe « Offres & quotas », ajouter « Tarification & devises »).

### T5.2 `PlatformPlansPage.tsx` — refonte

**Conserver** : `key` / `name` / `description` / `priceMonthly` / `priceYearly` / `isActive` / `sortOrder`,
le parcours de création/édition, le tri, le bouton power, les libellés d'action i18n
(`admin.planNew` … `fr.ts:239-247`).

**Corriger F5** : remplacer `FCFA/mois` (`:136-137`) par le vrai symbole issu de la devise.

**Ajouter** :
- **Grille multi-devises** dans la carte de chaque plan : une ligne par devise, avec
  montant mensuel / annuel, statut de synchro Stripe (`SYNCED`/`PENDING`/`FAILED`/`MANUAL`), et
  badges de canaux.
- **Éditeur de prix** (drawer plein écran, pas une modale qui déborde) : ajout/édition/suppression d'un prix.
- **Aperçu live** : « rendu visitor » (le même composant que `/pricing`, en lecture seule) — détecte visuellement
  les régressions (devise manquante, 0 accidentel, canal indisponible).
- **Jauge de cohérence** : alerte visuelle si
  - un plan payant n'a **aucun** prix dans `base_currency` ;
  - un prix `is_visible` mais `is_selectable = false` (invisible pour l'utilisateur, dépassement d'un quota) ;
  - un prix 0-décimal sans Price ID et sans canal MOMO/STRONG ⇒ **CTA mort** ;
  - `annualDiscountPct` annoncé mais aucun prix annuel ni prix mensuel coherent.

### T5.3 `PricingSettingsPage.tsx` (nouveau) — `/platform/saas/pricing`

Sections :
1. **Devises** : multi-select sur le catalogue `GET /platform/currencies` ; devise de base ; devise d'affichage par
   défaut ; repli autorisé ou non ; choix utilisateur autorisé ou non.
2. **Affichage** : mode (`SINGLE`/`SWITCHER`/`LIST_ALL`/`AUTO_REGION`), choix du cycle, choix de la région,
   précision d'affichage.
3. **Régions** : activation, source de résolution, région par défaut, en-tête de confiance.
4. **Paiements** : canaux autorisés globalement, devise forte, opérateurs MoMo.
5. **Stripe** : auto-sync ON/OFF, taille de lot, lien dashboard, **bouton « synchroniser tout »** avec
   progression (nb OK / nb en échec) et export CSV des échecs.
6. **Essai & remise** : essai ON/OFF, remise annuelle par défaut.
7. **Landing** : section tarifs visible, source (`API`/`STATIC`), nombre max de plans affichés,
   afficher les prix, afficher le CTA « voir tous les tarifs », libellé du CTA.
8. **Aperçu** : renduembedded de la section landing avec les réglages en cours, pour valider avant save.

Sauvegarde : `PUT /api/v1/platform/admin/pricing/settings`, puis invalidation du cache public (§3.7).

### T5.4 Rendu des montants côté admin

Nouveau helper `frontend/src/lib/money.ts` :

```ts
export interface CurrencyMeta { code: string; symbol: string; decimals: number; name: string; }

/** Formatte un montant MINEUR (contrat §2.1 §backend). Ne jamais passer un montant majeur. */
export function formatMinor(amountMinor: number, meta: CurrencyMeta, locale: Locale): string {
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: meta.code,
    minimumFractionDigits: meta.decimals,
    maximumFractionDigits: meta.decimals,
  }).format(amountMinor / 10 ** meta.decimals);
}

/** Parse une saisie utilisateur (montant MAJEUR) vers des unités mineures, sans flottant. */
export function parseMajorToMinor(input: string, decimals: number): number | null {
  const normalized = input.trim().replace(/ |\s/g, '').replace(',', '.');
  if (!/^-?\d+(\.\d+)?$/.test(normalized)) return null;
  const [int, frac = ''] = normalized.split('.');
  if (frac.length > decimals) return null;
  return Number(int) * 10 ** decimals + Number((frac + '0'.repeat(decimals)).slice(0, decimals));
}
```

⚠️ **Interdiction absolue** de `parseFloat(x) * 100` dans tout le nouveau code (cf. javadoc de
`Iso4217CurrencyValidator`). Les tests doivent couvrir `0.1 + 0.2`.

### T5.5 Tests frontend (vitest)

- `PlatformPlansPage.test.tsx` (nouveau) : affichage du bon symbole par devise (régression F5).
- `PricingSettingsPage.test.tsx` (nouveau) : chaque interrupteur part dans le PUT.
- `money.test.ts` (nouveau) : `formatMinor(2900, EUR)` = `29,00 €` ; `formatMinor(15000, XAF)` = `15 000 FCFA` ;
  `parseMajorToMinor('29,99', 2) === 2999` ; `parseMajorToMinor('15000,5', 0) === null`.

---

## §8. T7 — PAGES PUBLIQUES (`/pricing` et LANDING)

### T7.1 `PricingPage.tsx` — refonte (fichier actuel : 562 lignes)

**Ce qui est conservé (ne rien casser)** :
- la route publique `/pricing` (`App.tsx:984`) ;
- le comportement `?checkout=cancelled` (`:233, 302-306`) ;
- `stripeEnabled` via `/public/billing/status` (`:235-241`) ;
- le CTA « Souscrire en ligne » visible si `stripeEnabled && isAuthenticated && plan.id !== 'discovery'` (`:409-421`) ;
- le garde-fou `isAuthenticated` (test `PricingPage.test.tsx:18-20`) ;
- `tText(plan.name)` (`:349, 352, 405`) — **indispensable**, car les noms de plans sont surchargeables par l'église
  (`resolveLabel`, cf. `SPEC_LOT2_TOUT_PARAMETRABLE` §4) ;
- le repli visuel en cas d'API indisponible (`:279-284, 313-315`) — **conservé**, mais extractible dans
  `frontend/src/features/pricing/pricingFallback.ts` avec un **contrat d'unité explicite** et un commentaire
  « données de secours, jamais une source de vérité » ;
- les blocs « Feature Highlights », « Testimonials », « FAQ » (`:426-558`) — **conservés**, traduits.

**Ce qui change** :
1. **Suppression de la multi-affichage fixe** : `toPlanPrices` (`:177-186`) est remplacé par la réponse serveur
   (`prices[]` + `display`). `LIST_ALL` reste disponible si `displayMode = LIST_ALL` (donc **rien n'est perdu**).
2. **Sélecteur de devise** : rendu **uniquement si** `allowCurrencyChoice` (sinon la devise est imposée et
   aucun sélecteur n'est rendu). Persistance `localStorage['discipolat-display-currency']`,
   synchronisée avec `?currency=XX` si présent.
3. **Bascule mensuel/annuel** : rendue **uniquement si** `allowCycleChoice`.
4. **Sélecteur de région** : rendu **uniquement si** `allowRegionChoice` (défaut `false`).
5. **Canal** : le CTA par défaut = premier canal disponible renvoyé par le serveur
   (cartes / MoMo / devise forte / contact). Un interrupteur par canal si >1.
6. **i18n** : **toutes** les chaînes codées en dur passent en clés `pricing.*` dans les 6 locales
   (`fr.ts, en.ts, pt.ts, es.ts, sw.ts, ar.ts`). Le bloc `subscription.*` existant (`fr.ts:3244-3282`) est
   complété, pas dupliqué. Le fichier `ar.ts` étant RTL, **vérifier l'ordre des libellés** (les `.test` e2e RTL).
7. **Conversion de devise à l'affichage** : si `display.available = false` et `showFallbackCurrency = false` ⇒
   « Tarif non communiqué » + CTA contact. **Jamais** de montant estimé.
8. **Accessibilité** : le sélecteur de devise est un `<select>` natif (ou `role="radiogroup"` complet avec
   flèches) ; `aria-live` sur le changement de devise ; focus visible ; le CTA doit être atteignable au clavier.

### T7.2 `usePricing` hook (nouveau) — `frontend/src/features/pricing/usePricing.ts`

```ts
export interface PricingState {
  config: PricingConfig | null;
  plans: PublicPlanView[];
  currency: string;      // devise affichée (résolue)
  cycle: BillingCycle;
  region: string | null;
  setCurrency: (c: string) => void;
  setCycle: (c: BillingCycle) => void;
  setRegion: (r: string) => void;
  loading: boolean;
  error: string | null;
  usingFallback: boolean;
}
```
- TanStack Query, `queryKey: ['public', 'pricing', currency, cycle, region]`, `staleTime: 60_000`.
- `placeholderData: keepPreviousData` pour éviter le clignotement au changement de devise.
- En cas d'erreur réseau ⇒ repli `pricingFallback` **et** bandeau honnête (comportement actuel conservé).

### T7.3 `SectionPricing.tsx` (landing) — **correction F4**

Fichier actuel : 63 lignes, `TIERS` en dur (`:7-11`), `COMMON` en dur (`:13`), prix « Sur devis » (`:40`).

**Nouvelle comportement** :
- lire `saas_pricing_settings.landing_*` via `/public/pricing/config` ;
- si `landing_pricing_enabled = false` ⇒ **la section est masquée** (plus de 3 tiers codés en dur) ;
- si `landing_pricing_source = STATIC` ⇒ comportement actuel conservé (3 tiers, « Sur devis ») — **c'est le mode
  « rien ne casse »** ;
- si `= API` ⇒ rendre `GET /public/pricing/plans`, limité à `landing_pricing_max_plans`,
  **respectant `landing_pricing_show_prices`** ;
- si un plan n'a pas de prix dans la devise résolue ⇒ afficher « Sur devis » **uniquement pour ce plan**, et non
  pour tous ;
- le CTA devient `landing_pricing_cta_label` s'il est défini, sinon `t('landing.nav.start')` (comportement actuel) ;
- `showToggle` ⇒ bouton « Voir tous les tarifs » vers `/pricing` ;
- garder `Reveal delay={i * 80}` et les classes `glass-strong` / `glass-card` / `ring-primary-500/40` (design system).
- **Supprimer la contradiction** : plus jamais « Sur devis » là où l'API a un prix.

⚠️ `SectionPricing` est rendu par `LandingPage.tsx:52` (`<Pricing />`) — **ne pas** changer la signature du
composant sans mettre à jour `LandingPage.tsx`.

### T7.4 i18n — clés à créer (6 locales)

Bloc `pricing.*` dans `fr.ts` (puis `en/pt/es/sw/ar`), à insérer **à côté** du bloc `subscription.*` (`fr.ts:3244`) :

```
pricing.title, pricing.subtitle, pricing.subtitleAi, pricing.loading, pricing.loadError,
pricing.fallbackNotice, pricing.checkoutCancelled, pricing.checkoutError,
pricing.currency, pricing.currencyAuto, pricing.cycle, pricing.monthly, pricing.yearly,
pricing.perMonth, pricing.perYear, pricing.perYearShort, pricing.region, pricing.regionAuto,
pricing.priceUnavailable, pricing.contactUs, pricing.savePercent, pricing.trialDays,
pricing.channel.card, pricing.channel.momo, pricing.channel.strong, pricing.channel.bank, pricing.channel.contact,
pricing.channel.chargedIn, pricing.region.AF, pricing.region.NA, pricing.region.EU, pricing.region.UK,
pricing.region.US, pricing.region.CA, pricing.region.LATAM, pricing.region.ASIA, pricing.region.OC,
pricing.feature.seats, pricing.feature.storage, pricing.feature.aiCredits, pricing.feature.events,
pricing.highlight.ai.title/.body, pricing.highlight.momo.title/.body, pricing.highlight.security.title/.body,
pricing.faq.changePlan.title/.body, pricing.faq.paymentMethods.title/.body, pricing.faq.trial.title/.body,
pricing.testimonials.1.name/.role/.quote  (×3),
pricing.viewAll, pricing.landing.cta, pricing.landing.surDevis,
pricing.syncing, pricing.syncOk, pricing.syncFailed, pricing.syncManual
```

⚠️ Le serveur renvoie `labelKey` pour les régions (`pricing.region.AF`) : le client fait `t(labelKey)`.
Si `labelKey` est absent ⇒ repli sur le code.

**Vérification** : `cd frontend && npm run i18n:audit` (script `frontend/scripts/i18n-audit.mjs`) et
`npm run i18n:baseline` **seulement si** le format change volontairement. Le rapport doit rester propre :
**aucune clé orpheline dans `fr.ts` sans équivalent dans les 5 autres locales.**

### T7.5 Tests frontend

- `PricingPage.test.tsx` — **adapter** le test existant (105 lignes) :
  - le mock `api.get` doit désormais router par URL (`/public/pricing/config`, `/public/pricing/plans`)
    sinon le test casse. Le test « affiche chaque prix fourni sans supposer une devise unique » (`:34-92`)
    reste **valide** — il vérifie l'indépendance aux devises : le conserver tel quel est une garantie.
  - nouveau : `sélecteur de devise masqué si allowCurrencyChoice=false` ;
  - nouveau : `bascule mensuel/annuel masquée si allowCycleChoice=false` ;
  - nouveau : `LIST_ALL` affiche les 3 prix sur une carte ;
  - nouveau : `display.available=false` ⇒ « Tarif non communiqué », **jamais 0**.
- `SectionPricing.test.tsx` (nouveau) : lit l'API, respecte `landing_pricing_max_plans`, `STATIC` inchangé.
- `money.test.ts` (cf. T5.5).

---

## §9. T8 — BACK-OFFICE TENANT (`/billing`, `/admin/subscription`)

### T8.1 `TenantAdminSubscriptionPage.tsx` (455 lignes)

- Le changement de plan (`:181`) passe par `POST /admin/subscription/change-plan` : **inchangé**.
- `:405,409` affiche `formatPrice(plan.priceMonthly, plan.currency)` — à migrer vers `prices[]` /
  `display`, pour que l'admin voie **le même prix** que le visiteur (une seule vérité).
- Afficher la **région** et le **canal de débit** de l'abonnement en cours (données de `tenant_subscriptions`).
- Si le plan courant a un `price_snapshot_json` (contrat §2.4) et que le tarif catalogue a changé,
  afficher une mention « *tarif catalogué actuel : X, votre tarif reste Y jusqu'au JJ/MM* » — honnête et utile.

### T8.2 `BillingPage.tsx` (186 lignes)

- Inchangée sur le portail Stripe (`:69-79`).
- **Ajouter** : si `stripeEnabled = false` et que le plan est payant ⇒ afficher les **canaux alternatifs**
  (MoMo / devise forte) au lieu d'un écran vide.

### T8.3 `SubscriptionController` (backend) — `GET /admin/subscription/plans`

Doit renvoyer la même forme enrichie (`prices[]`, `display`) que `/public/pricing/plans`, pour éviter que
l'admin et le visiteur voient des prix différents.

### T8.4 Fermeture de la fuite F10 (inscription sans paiement)

`RegisterPage.tsx:56,144` accepte `?plan=` et pose le tenant sur le plan payant sans débit.
**Décision** :
- si le plan demandé est gratuit (`DISCOVERY`) ⇒ comportement actuel (inscription directe) ;
- si le plan demandé est payant ⇒ le tenant est créé **sur `DISCOVERY`**, et l'utilisateur est redirigé vers
  `/pricing?plan=<key>` avec un bandeau « *finalisez votre abonnement pour activer <plan>* ».
- Alternative (si le produit veut garder le flux actuel) : créer la session Stripe **immédiatement après**
  l'inscription. **À arbitrer avec le produit** — documenté ici, pas implémenté par défaut (le plus sûr est
  l'option 1 : ne jamais offrir un plan payant non payé).

---

## §10. T9 — MOBILE (Flutter) — parité fonctionnelle

**Objectif** : même catalogue, même sélecteur de devise, mêmeomenclature de canaux, aucun XOF codé en dur.

### T9.1 Correction de `giving_screen.dart`

Lignes **132** et **217** : `'currency': 'XOF'` en dur.
→ La devise doit venir de `CurrencyCatalogService` / du tenant (`currency_configs`), avec XAF/XOF **choisis**,
pas devinés. Le service existe déjà : `mobile/lib/data/services/currency_catalog_service.dart`
(consomme `GET /platform/currencies`, repli EUR/XAF/USD — **repli à mettre à jour** pour inclure XOF,
puisque le backend l'autorise désormais).

### T9.2 `pricing_screen.dart` (nouveau)

`mobile/lib/presentation/screens/pricing/pricing_screen.dart` :
- charge `GET /public/pricing/config` + `GET /public/pricing/plans` ;
- sélecteur de devise **respectant `allowCurrencyChoice`** ;
- formatage via `NumberFormat.currency(name: symbol, decimalDigits: decimals)` avec `decimals` du serveur
  (**0 pour XAF/XOF** — un `decimalDigits: 2` sur du FCFA est un bug d'affichage classique) ;
- liste des canaux et CTA ;
- **lancement du checkout** : ouvrir l'URL Stripe dans le navigateur (`url_launcher`) si le canal est `CARD`/`STRONG`,
  ou déclencher le flux MoMo natif si `MOMO`. **Attention** : `mobile/pubspec.yaml` ne contient aujourd'hui
  **ni** `url_launcher` **ni** SDK de paiement ⇒ ajout de dépendance à valider (réseau, taille, store policies).

### T9.3 l10n mobile

`mobile/lib/l10n/app_*.arb` — ajouter le bloc `pricing*` dans les 6 langues
(`intl_{fr,en,pt,es,sw,ar}.arb`, puis `app_localizations.dart` régénéré).
Existing payment strings : fr `:2224-2226`, en `:3822`, pt `:5306`, sw `:6839`, es `:8369`, ar `:9899`.
Commande de génération habituelle du projet (`flutter gen-l10n`) — **vérifier `l10n.yaml`** avant de lancer.

### T9.4 Cohérence web/mobile

`docs/WEB_MOBILE_PARITY.md` — ajouter une ligne par écranComparé
(`/pricing` web ↔ `pricing_screen` mobile ; `billing` ↔ écran abonnement).

### T9.5 Tests

`flutter test` : parsing de `PricingConfig` avec `enabledCurrencies` vide, devise inconnue, `display.available=false`.

---

## §11. T10 — E2E, QUALITÉ, DÉPLOIEMENT

### T10.1 E2E Playwright (dossier `e2/specs/` actuellement **vide**)

Créer :

| Fichier | Scénario |
|---|---|
| `e2/specs/pricing-public.spec.ts` | `/pricing` charge 4 plans ; sélecteur de devise ; bascule mensuel/annuel ; mode LIST_ALL ; pas de 0 accidentel |
| `e2/specs/landing-pricing.spec.ts` | Section landing alignée sur `/pricing` ; respects `landing_pricing_max_plans` ; CTA « voir tous les tarifs » |
| `e2/specs/pricing-i18n.spec.ts` | Les 6 locales n'affichent aucune chaîne française résiduelle sur `/pricing` |
| `e2/specs/pricing-rtl.spec.ts` | `dir="rtl"` correct en `ar`, mise en page du sélecteur sane |
| `e2/specs/pricing-currency-units.spec.ts` | EUR ⇒ `29,00 €` (et **jamais** `29 centimes`) ; XAF ⇒ `15 000 FCFA` sans décimale |

### T10.2 Commandes de vérification (à exécuter avant chaque commit)

```bash
# Backend
cd backend && ./mvnw -q verify          # inclut Testcontainers + ArchUnit
cd backend && ./mvnw -q spring-boot:run # smoke manual

# Frontend
cd frontend && npm run lint
cd frontend && npm run test              # vitest
cd frontend && npm run build             # tsc -b && vite build  (OBLIGATOIRE : erreurs de types)
cd frontend && npm run i18n:audit

# E2E
cd e2 && npx playwright test

# Mobile
cd mobile && flutter analyze && flutter test
```

⚠️ `npm run build` est **non négociable** : le projet est TypeScript strict et `tsc -b` échoue vite.

### T10.3 Variables d'environnement à documenter

`docs/ENV_TEMPLATE.md` — ajouter :

```
# Tarification & devises
PRICING_BASE_CURRENCY=XAF
PRICING_DEFAULT_DISPLAY_CURRENCY=XAF
PRICING_REGION_HEADER_NAME=X-Discipolat-Region
STRIPE_PRICE_SYNC_AUTO=false
STRIPE_PRICE_SYNC_INTERVAL_MS=300000
STRIPE_PRICE_SYNC_BATCH_SIZE=20
```

⚠️ Les réglages **super-admin** vivent en base (`saas_pricing_settings`), **pas** en variables d'env.
Les variables ci-dessus ne servent qu'aux **valeurs de premier démarrage** (seed de la ligne singleton)
et aux paramètres d'exécution du batch Stripe. **Ne pas créer deux sources de vérité.**

### T10.4 Documentation à mettre à jour

- `docs/GUIDE_SUPER_ADMIN.md` — chapitre « Tarification & devises ».
- `docs/API.md` + `docs/openapi.json` — nouveaux endpoints.
- `docs/DATABASE.md` — `saas_plan_prices`, `saas_pricing_settings`, `saas_momo_subscriptions`.
- `docs/KNOWN_ISSUES.md:33` — **clôturer** l'entrée « Stripe jamais testé en live » seulement après T10.5.
- `docs/CHANGELOG.md`.
- `docs/ARCHITECTURE.md` — note « catalogue plateforme = tables globales, hors filtre tenant ».

### T10.5 Test Stripe live (T10 obligatoire avant clôture monetization)

1. `STRIPE_SECRET_KEY=sk_test_…` + `STRIPE_WEBHOOK_SECRET=whsec_…`
2. `stripe listen --forward-to localhost:8080/api/v1/payments/webhooks/stripe`
3. Parcourir : checkout GROWTH (EUR inline) → vérifier `unit_amount == 2900` (**le bug F1 est là**)
   → vérifier `tenant_subscriptions.price_minor == 2900` après le webhook.
4. Activer `stripe_price_auto_sync`, créer un prix XAF → vérifier `stripe_price_id` renseigné et
   `stripe_price_status = SYNCED` → checkout STARTUP **réussi** (c'est le bug F2).
5. Tester le **changement de montant** : modifier STARTUP → l'ancien Price est `active=false`, le nouveau est
   utilisé, **l'abonnement existant n'est pas cassé**.

---

## §12. ORDRE D'EXÉCUTION RECOMMANDÉ

```
T1  (V245→V248, entités, repos)          ← socle, bloquant
 └─ T2 (PricingCatalogService, résolution)  ← bloquant pour tout le reste
     ├─ T3 (API publique + admin)             ← débloque le front
     │   └─ T5 (admin front) + T7 (pages publiques) + T8 (back-office)
     │       └─ T10 (E2E + doc)
     ├─ T6 (canaux : Stripe sync → STRONG → MOMO)  ← T6.1 d'abord (débloque F2)
     └─ T9 (mobile)                            ← peut démarrer en parallèle dès T3
```

**Jalon minimal viable (à faire en premier si le temps manque)** : `T1 → T2 → T6.1 → T3.1/T3.3 → T7.1/T7.3`.
Ce chemin corrige à lui seul **F1** (facturation 100× fausse), **F2** (STARTUP non souscriptable),
**F3** (plans custom invisibles), **F4** (landing contradictoire) et **F5** (FCFA codé en dur).

---

## §13. RISQUES & PARADES

| # | Risque | Parade |
|---|---|---|
| R1 | **Régression de facturation** : passer de `priceMonthly` (majeur) à `amountMinor` (mineur) multiplie les prix par 100 si mal fait. | Le seul chemin qui décide est `PricingCatalogService`. `StripeBillingService` n'a **plus le droit** de lire `SaasPlan.priceMonthly`. Test obligatoire `createCheckoutSession_usesMinorUnits`. Revue de code ciblée sur ce point. |
| R2 | Prix **×100 trop bas** chez les clients existants si on oublie le backfill V247. | Vérification post-migration en SQL (requête de contrôle fournie en §14) affichant `legacy` vs `minor`. |
| R3 | Le plan STARTUP devient **souscriptable en XAF** →Appearance d'un produit qui n'a jamais été facturé. | `KNOWN_ISSUES.md:33` préexiste : ce chantier **est** la correction. Communiquer avant activation. |
| R4 | Ajout de `SAAS_SUBSCRIPTION` au `CHECK` de `payment_intents` casse si un `CHECK` a été modifié après V93. | Chercher **toutes** les migrations qui touchent `payment_intents` avant d'écrire V249. Migration `IF EXISTS`-tolérante. |
| R5 | `saas_plan_prices` en FK sur `saas_plans(key)` : supprimer un plan casse le catalogue de prix. | `ON DELETE CASCADE` assumé, **mais** les plans ne se suppriment pas : `SuperAdminSaasPlanController.deactivatePlan` (`:108-116`) fait un soft-delete (`is_active=false`, `status='INACTIVE'`). Ne pas introduire de hard-delete. |
| R6 | Le cache public de 5 min (`PublicSaasPlanController.java:32`) sert des prix obsolètes après une édition admin. | `catalogVersion` (§3.7) + `max-age=60` sur `/pricing/config`. |
| R7 | Rupture de l'existant : la nouvelle API ne renvoie plus `priceEur`… | **Interdit** : `PublicPlanResponse` conserve tous les champs legacy (T3.1). Test de contrat. |
| R8 | Souscription via devise forte sans taux fiable ⇒ marge perdue ou client sur-facturé. | `strong_rate_ratio` **opt-in**, `NULL` par défaut, `STRONG` refusé sur ce prix si le ratio est absent. |
| R9 | Le mobile casse (dépendance `url_launcher` absente). | T9.2 commence par vérifier les dépendances existantes ; si `url_launcher` manque, isoler l'ouverture derrière une abstraction et la documenter comme dépendance à ajouter. |
| R10 | Le chantier grossit trop (3 tiers × 6 canaux × 6 langues × 7 devises). | Priorité donnée au jalon minimal (§12). Chaque étape est indépendamment livrable et réversible. |

---

## §14. CONTRÔLES SQL POST-MIGRATION (à coller dans `docs/RUNBOOK.md`)

```sql
-- 1) Chaque plan payant public a-t-il un prix dans la devise de base ?
SELECT p.key, s.base_currency
  FROM saas_plans p, saas_pricing_settings s
 WHERE s.id = 1 AND p.is_public AND p.is_active
   AND NOT EXISTS (
        SELECT 1 FROM saas_plan_prices pp
         WHERE pp.plan_key = p.key
           AND pp.currency = s.base_currency
           AND pp.billing_cycle = 'MONTHLY'
           AND pp.region_code = 'GLOBAL');

-- 2) Backfill : vérifier la conversion majeur → mineur (GROWTH EUR doit valoir 2900)
SELECT plan_key, currency, billing_cycle, amount_minor, metadata->>'legacy_column' AS origine
  FROM saas_plan_prices
 WHERE metadata ? 'origin'
 ORDER BY plan_key, currency, billing_cycle;

-- 3) Prix 0-décimales sans Price ID et sans canal alternatif ⇒ CTA mort
SELECT id, plan_key, currency, payment_channels, stripe_price_status
  FROM saas_plan_prices
 WHERE currency IN ('XAF','XOF')
   AND payment_channels LIKE '%CARD%'
   AND (stripe_price_id IS NULL OR stripe_price_id = '')
   AND payment_channels NOT LIKE '%MOMO%'
   AND payment_channels NOT LIKE '%STRONG%';

-- 4) Devises activées mais orphelines (aucun prix) ⇒ à afficher « indisponible » ou à retirer
SELECT unnest(s.enabled_currencies) AS devise, count(pp.id) AS nb_prix
  FROM saas_pricing_settings s
  LEFT JOIN saas_plan_prices pp
    ON pp.currency = s.enabled_currencies AND pp.is_enabled AND pp.is_visible
 WHERE s.id = 1
 GROUP BY devise ORDER BY nb_prix ASC;

-- 5) Cohérence remise annuelle annoncée vs prix réels
SELECT plan_key, currency,
       MAX(amount_minor) FILTER (WHERE billing_cycle = 'MONTHLY') AS mensuel,
       MAX(amount_minor) FILTER (WHERE billing_cycle = 'YEARLY')  AS annuel
  FROM saas_plan_prices
 WHERE is_enabled AND region_code = 'GLOBAL'
 GROUP BY plan_key, currency
HAVING COUNT(*) FILTER (WHERE billing_cycle = 'YEARLY') = 0;
```

---

## §15. DÉFINITION DE « TERMINÉ » (DoD)

Une tâche n'est close que si **toutes** ces conditions sont remplies :

- [ ] `cd backend && ./mvnw -q verify` passe (tests + ArchUnit + Testcontainers).
- [ ] `cd frontend && npm run lint && npm run test && npm run build` passe (`tsc -b` inclus).
- [ ] `cd frontend && npm run i18n:audit` ne signale **aucune** nouvelle clé manquante dans les 6 locales.
- [ ] `cd mobile && flutter analyze && flutter test` passe.
- [ ] Aucune chaîne française codée en dur **ajoutée** dans `PricingPage.tsx` / `SectionPricing.tsx` /
      `PlatformPlansPage.tsx` / `TenantAdminSubscriptionPage.tsx` / `BillingPage.tsx`.
- [ ] Les 5 requêtes de contrôle du §14 retournent un résultat **satisfaisant** (0 ligne, ou justifiée).
- [ ] Aucune migration `V ≤ 244` modifiée (vérifiable par `git diff --stat backend/src/main/resources/db/migration`).
- [ ] Aucune colonne/table existante supprimée.
- [ ] Contrat d'API documenté dans `docs/API.md` + `docs/openapi.json`.
- [ ] Si T10.5 n'est pas passé : **ne pas** clôturer `KNOWN_ISSUES.md:33`, et le laisser écrit explicitement.

---

## §16. POINTS ARBITRABLES RESTANTS (à trancher avec le produit)

| # | Question | Recommandation par défaut retenue ici |
|---|---|---|
| A1 | Inscription sur plan payant : créée sur `DISCOVERY` puis redirection checkout (T8.4), ou tenant d'emblée sur le plan payé ? | **`DISCOVERY` + redirection** (on ne doit jamais offrir un plan payant non payé). |
| A2 | Prix annuel : explicite prioritaire, ou toujours recalculé depuis `annual_discount_pct` ? | **Explicite prioritaire**, calcul en repli (T3.6). |
| A3 | Les plans `PUBLIC_PLAN_KEYS` doivent-ils rester une liste fermée côté serveur ? | **Non** — ouvert dès que `is_public = true` (F3). Une liste fermée rend le super-admin incapable d'ajouter une offre. |
| A4 | MoMo récurrent : seul MTN MoMo est réellement tokenisable. M-Pesa (STK Push) ne l'est pas. | **N'exposer que les opérateurs tokenisables** ; M-Pesa reste dispo en paiement ponctuel (dîme), pas en abonnement. |
| A5 | Région par IP (geo) ? | **Non en v1** : géolocalisation = dépendance externe + charge RGPD. `region_resolution = TENANT` par défaut ; la valeur `IP` est prévue dans le modèle mais reste désactivée. |