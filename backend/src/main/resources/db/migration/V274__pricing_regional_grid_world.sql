-- V274__pricing_regional_grid_world.sql
-- ============================================================
-- Grille tarifaire régionale MONDIALE + activation de la tarification
-- régionale et des canaux Mobile Money / carte pour les ABONNEMENTS SaaS.
--
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — continuité (T2 tarification régionale).
--
-- ADDITIF ET NON-DESTRUCTIF :
--   * on N'ÉCRASE RIEN : toutes les INSERT sont en ON CONFLICT DO NOTHING ;
--     les lignes GLOBAL déjà seedées par V247 restent l'unique filet de
--     repli (cf. correction de PricingCatalogService.publicCatalog qui garde
--     GLOBAL comme candidat). Aucune ligne n'est supprimée ni modifiée.
--   * le contrat d'unités est respecté : amount_minor en unités mineures
--     ISO-4217. Ici toutes les devises seedées (USD, EUR) ont 2 décimales,
--     donc amount_minor = montant_majeur × 100.
--
-- DEVISES PAR RÉGION (volonté produit) :
--   Afrique (centrale/ouest AF, australe AF_S, de l'est AF_E, du Nord NA),
--   Asie (ASIA), Amériques (Nord AM_N, Sud AM_S) et Océanie (OC) : USD.
--   Europe zone euro (EU) : EUR. Europe hors zone euro (EU_D) : USD.
--
-- CANAUX :
--   AF/AF_S/AF_E/NA/ASIA/AM_S : 'MOMO,CARD'  (Orange Money, MTN MoMo, Wave, Visa/MC
--                                             via l'agrégateur CinetPay, plus carte).
--   AM_N/OC : 'CARD' ; EU : 'CARD,BANK' ; EU_D : 'CARD,BANK' (Visa + virement/SEPA).
--
-- REMISE ANNUELLE : 17 % (annual_discount_default). Prix annuel dérivé du mensuel
--   yearly_major = round(monthly_major × 12 × 0.83).
--
-- Les tarifs (mensuel, devise majeure) sont calibrés par pouvoir d'achat :
--   plan        AF  AF_S AF_E NA  ASIA AM_S AM_N EU(EUR) EU_D OC
--   STARTUP     12  14   13   14  10   15   25   19      21   24
--   GROWTH      25  29   27   29  22   32   49   39      42   45
--   NETWORK     75  85   79   85  69   89   149  119     129  139
-- (DISCOVERY reste gratuit : aucune ligne régionale nécessaire, le GLOBAL 0 suffit.)
-- ============================================================

-- ── Grille régionale : mensuel + annuel, générés depuis une seule table VALUES ──
WITH seed(plan_key, region_code, currency, monthly_major, channels, sort_order) AS (
    VALUES
    -- STARTUP
    ('STARTUP', 'AF',   'USD', 12, 'MOMO,CARD', 101),
    ('STARTUP', 'AF_S', 'USD', 14, 'MOMO,CARD', 102),
    ('STARTUP', 'AF_E', 'USD', 13, 'MOMO,CARD', 103),
    ('STARTUP', 'NA',   'USD', 14, 'MOMO,CARD', 104),
    ('STARTUP', 'ASIA', 'USD', 10, 'MOMO,CARD', 105),
    ('STARTUP', 'AM_S', 'USD', 15, 'MOMO,CARD', 106),
    ('STARTUP', 'AM_N', 'USD', 25, 'CARD',      107),
    ('STARTUP', 'EU',   'EUR', 19, 'CARD,BANK', 108),
    ('STARTUP', 'EU_D', 'USD', 21, 'CARD,BANK', 109),
    ('STARTUP', 'OC',   'USD', 24, 'CARD',      110),
    -- GROWTH
    ('GROWTH',  'AF',   'USD', 25, 'MOMO,CARD', 201),
    ('GROWTH',  'AF_S', 'USD', 29, 'MOMO,CARD', 202),
    ('GROWTH',  'AF_E', 'USD', 27, 'MOMO,CARD', 203),
    ('GROWTH',  'NA',   'USD', 29, 'MOMO,CARD', 204),
    ('GROWTH',  'ASIA', 'USD', 22, 'MOMO,CARD', 205),
    ('GROWTH',  'AM_S', 'USD', 32, 'MOMO,CARD', 206),
    ('GROWTH',  'AM_N', 'USD', 49, 'CARD',      207),
    ('GROWTH',  'EU',   'EUR', 39, 'CARD,BANK', 208),
    ('GROWTH',  'EU_D', 'USD', 42, 'CARD,BANK', 209),
    ('GROWTH',  'OC',   'USD', 45, 'CARD',      210),
    -- NETWORK
    ('NETWORK', 'AF',   'USD',  75, 'MOMO,CARD', 301),
    ('NETWORK', 'AF_S', 'USD',  85, 'MOMO,CARD', 302),
    ('NETWORK', 'AF_E', 'USD',  79, 'MOMO,CARD', 303),
    ('NETWORK', 'NA',   'USD',  85, 'MOMO,CARD', 304),
    ('NETWORK', 'ASIA', 'USD',  69, 'MOMO,CARD', 305),
    ('NETWORK', 'AM_S', 'USD',  89, 'MOMO,CARD', 306),
    ('NETWORK', 'AM_N', 'USD', 149, 'CARD',      307),
    ('NETWORK', 'EU',   'EUR', 119, 'CARD,BANK', 308),
    ('NETWORK', 'EU_D', 'USD', 129, 'CARD,BANK', 309),
    ('NETWORK', 'OC',   'USD', 139, 'CARD',      310)
),
cycles(cycle, factor) AS (
    VALUES ('MONTHLY', 1.0), ('YEARLY', 12 * (100 - 17) / 100.0)
)
INSERT INTO saas_plan_prices
    (plan_key, currency, billing_cycle, amount_minor, region_code,
     is_enabled, is_visible, is_selectable, sort_order, payment_channels,
     stripe_price_status, metadata)
SELECT
    s.plan_key,
    s.currency,
    c.cycle,
    -- USD/EUR : 2 décimales → unités mineures = majeur × 100.
    (ROUND(s.monthly_major * c.factor * 100))::bigint,
    s.region_code,
    TRUE, TRUE, TRUE,
    s.sort_order + CASE WHEN c.cycle = 'YEARLY' THEN 5 ELSE 0 END,
    s.channels,
    'NOT_REQUIRED',
    jsonb_build_object('origin', 'V274', 'regional', TRUE, 'monthly_major', s.monthly_major)
FROM seed s
CROSS JOIN cycles c
ON CONFLICT (plan_key, currency, billing_cycle, region_code) DO NOTHING;

-- ── Activation plateforme de la tarification régionale + canaux ────────────────
-- region_pricing_enabled : la grille ci-dessus prend effet (sinon seul GLOBAL vit).
-- allow_momo_channel     : OM / MoMo / Wave deviennent des CTA proposables.
-- default_display_currency = USD : devise de couverture maximale (toutes régions
--   sauf EU possèdent une ligne USD ; EU retombe sur GLOBAL-USD).
UPDATE saas_pricing_settings
   SET region_pricing_enabled   = TRUE,
       allow_momo_channel       = TRUE,
       allow_card_channel       = TRUE,
       default_display_currency = 'USD',
       enabled_currencies       = ARRAY['XAF','XOF','EUR','USD','CAD','GBP','CHF']::char(3)[],
       catalog_version          = COALESCE(catalog_version, 0) + 1,
       updated_at               = CURRENT_TIMESTAMP
 WHERE id = 1;

COMMENT ON TABLE saas_plan_prices IS
    'Prix par plan x devise x cycle x région. amount_minor en unités mineures ISO-4217 (contrat §2.1). Grille régionale monde seedée en V274.';
