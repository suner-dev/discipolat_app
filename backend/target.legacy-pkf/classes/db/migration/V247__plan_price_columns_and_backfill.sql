-- V247__plan_price_columns_and_backfill.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.3
-- Colonnes de tracabilite sur saas_plans + BACKFILL du catalogue de prix.
--
-- RÈGLE DE BACKFILL (contrat §2.1 règle 4) : les colonnes legacy
-- price_eur / price_xaf / price_usd / price_monthly / price_yearly sont lues
-- comme des MONTANTS MAJEURS — c'est ce que le frontend affiche aujourd'hui
-- (« 29 € » pour price_eur = 29). On convertit donc :
--     amount_minor = round(prix_majeur * 10 ^ minorUnits(devise))
-- EUR/USD (2 déc.) -> ×100 ; XAF/XOF (0 déc.) -> ×1.
--
-- Ce backfill corrige notamment la faille F1 : Stripe débitait 29 CENTIMES
-- au lieu de 29 EUROS pour le plan GROWTH.
-- ============================================================

ALTER TABLE saas_plans
    ADD COLUMN IF NOT EXISTS stripe_product_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pricing_model    VARCHAR(20) NOT NULL DEFAULT 'PER_CURRENCY',
    ADD COLUMN IF NOT EXISTS max_currencies    INTEGER NOT NULL DEFAULT 3;

COMMENT ON COLUMN saas_plans.stripe_product_id IS
    'Produit Stripe cree une fois par plan ; les Price s''y rattachent.';
COMMENT ON COLUMN saas_plans.pricing_model IS
    'PER_CURRENCY = prix par devise (defaut). PER_REGION = grille regionale prioritaire.';
COMMENT ON COLUMN saas_plans.max_currencies IS
    'Nombre maximum de devises affichees sur la carte de plan (garde-fou de lisibilite).';

-- ── Prix mensuels issus de price_eur / price_xaf / price_usd ────────────────

INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code,
     is_enabled, is_visible, is_selectable, sort_order, payment_channels,
     stripe_price_status, metadata)
SELECT p.key, 'EUR', 'MONTHLY', ROUND(p.price_eur::numeric * 100)::bigint, 'GLOBAL',
       COALESCE(p.is_active, TRUE), COALESCE(p.is_public, TRUE), COALESCE(p.is_public, TRUE),
       COALESCE(p.sort_order, 0) * 10 + 1,
       'CARD', 'NOT_REQUIRED',
       jsonb_build_object('origin', 'V247', 'legacy_column', 'price_eur')
FROM saas_plans p
WHERE p.price_eur IS NOT NULL
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code,
     is_enabled, is_visible, is_selectable, sort_order, payment_channels,
     stripe_price_status, metadata)
SELECT p.key, 'XAF', 'MONTHLY', p.price_xaf::bigint, 'GLOBAL',
       COALESCE(p.is_active, TRUE), COALESCE(p.is_public, TRUE), COALESCE(p.is_public, TRUE),
       COALESCE(p.sort_order, 0) * 10 + 2,
       -- XAF/XOF : devise 0-décimale, Stripe refuse le prix inline.
       -- On propose donc Mobile Money puis débit en devise forte (§2.3).
       'MOMO,STRONG', 'PENDING',
       jsonb_build_object('origin', 'V247', 'legacy_column', 'price_xaf')
FROM saas_plans p
WHERE p.price_xaf IS NOT NULL
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code,
     is_enabled, is_visible, is_selectable, sort_order, payment_channels,
     stripe_price_status, metadata)
SELECT p.key, 'USD', 'MONTHLY', ROUND(p.price_usd::numeric * 100)::bigint, 'GLOBAL',
       COALESCE(p.is_active, TRUE), COALESCE(p.is_public, TRUE), COALESCE(p.is_public, TRUE),
       COALESCE(p.sort_order, 0) * 10 + 3,
       'CARD', 'NOT_REQUIRED',
       jsonb_build_object('origin', 'V247', 'legacy_column', 'price_usd')
FROM saas_plans p
WHERE p.price_usd IS NOT NULL
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

-- ── Prix annuels : price_yearly est exprime dans saas_plans.currency ─────────
-- La remise annuelle est DÉJÀ incorporée dans price_yearly (150 000 vs 15 000×12,
-- soit ≈16,7 % ≈ annual_discount_pct = 17). On respecte le prix saisi.

INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code,
     is_enabled, is_visible, is_selectable, sort_order, payment_channels,
     stripe_price_status, metadata)
SELECT p.key,
       UPPER(p.currency),
       'YEARLY',
       CASE UPPER(p.currency)
            WHEN 'XAF' THEN p.price_yearly::bigint
            WHEN 'XOF' THEN p.price_yearly::bigint
            WHEN 'JPY' THEN p.price_yearly::bigint
            WHEN 'KRW' THEN p.price_yearly::bigint
            ELSE ROUND(p.price_yearly::numeric * 100)::bigint
       END,
       'GLOBAL',
       COALESCE(p.is_active, TRUE), COALESCE(p.is_public, TRUE), COALESCE(p.is_public, TRUE),
       COALESCE(p.sort_order, 0) * 10 + 4,
       CASE WHEN UPPER(p.currency) IN ('XAF','XOF','JPY','KRW')
            THEN 'MOMO,STRONG' ELSE 'CARD' END,
       CASE WHEN UPPER(p.currency) IN ('XAF','XOF','JPY','KRW')
            THEN 'PENDING' ELSE 'NOT_REQUIRED' END,
       jsonb_build_object('origin', 'V247', 'legacy_column', 'price_yearly')
FROM saas_plans p
WHERE p.price_yearly IS NOT NULL AND p.price_yearly > 0
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

-- ── Lignes 0-décimales qui passent par la carte : needs a Stripe Price ──────
UPDATE saas_plan_prices
   SET stripe_price_status = 'PENDING'
 WHERE currency IN ('XAF','XOF','JPY','KRW')
   AND payment_channels LIKE '%CARD%';

-- ── enabled_currencies : partir du catalogue réel plutôt que de la defaut ───
UPDATE saas_pricing_settings s
   SET enabled_currencies = COALESCE(
         (SELECT array_agg(DISTINCT pp.currency ORDER BY pp.currency)
            FROM saas_plan_prices pp),
         s.enabled_currencies)
 WHERE s.id = 1
   AND NOT EXISTS (SELECT 1 FROM saas_plan_prices);

COMMENT ON TABLE saas_plan_prices IS
    'Prix par plan x devise x cycle x région. amount_minor en unités mineures ISO-4217 (contrat §2.1).';