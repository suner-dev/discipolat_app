-- V144__seed_saas_plans.sql
-- ============================================================
-- G1.4 - §30-31 : Plans SaaS — 4 plans Dual-Market
-- DÉCOUVERTE / DÉMARRAGE / CROISSANCE / RÉSEAU & CAMPUS
-- Prix EUR ◈ FCFA ◈ USD, quotas et crédits IA par plan
-- ============================================================

-- Plan 1 : DÉCOUVERTE (gratuit, viral)
INSERT INTO saas_plans (key, name, description, price_monthly, price_yearly, currency,
    limits_json, features_json, is_active, sort_order, is_public, trial_days, annual_discount_pct,
    seats_limit, storage_limit_mb, ai_credits_limit, status)
VALUES ('DISCOVERY', 'Découverte', 'Gratuit pour découvrir Discipolat. Parfait pour une petite église qui débute.',
    0, 0, 'EUR',
    '{"members": 50, "spaces": 3, "storage_mb": 500, "events": 10, "ai_credits": 100}',
    '{"dashboard": true, "members": true, "families": true, "churches": true, "events": true, "notifications": true, "documents": true, "reports": true, "analytics": false, "discipleship": false, "academy": false, "ai": false, "finance": false, "mobile_money": false, "marketplace": false, "community": false, "forms": true, "calendar": true}',
    true, 1, true, 30, 17,
    50, 500, 100, 'ACTIVE')
ON CONFLICT (key) DO NOTHING;

-- Plan 2 : DÉMARRAGE (Afrique - FCFA)
INSERT INTO saas_plans (key, name, description, price_monthly, price_yearly, currency,
    limits_json, features_json, is_active, sort_order, is_public, trial_days, annual_discount_pct,
    seats_limit, storage_limit_mb, ai_credits_limit, status)
VALUES ('STARTUP', 'Démarrage', 'Pour les églises en croissance. Gestion complète avec IA incluse.',
    15000, 150000, 'FCFA',
    '{"members": 200, "spaces": 10, "storage_mb": 5000, "events": 50, "ai_credits": 500}',
    '{"dashboard": true, "members": true, "families": true, "churches": true, "events": true, "notifications": true, "documents": true, "reports": true, "analytics": true, "discipleship": true, "academy": false, "ai": true, "finance": true, "mobile_money": true, "marketplace": false, "community": false, "forms": true, "calendar": true}',
    true, 2, true, 30, 17,
    200, 5000, 500, 'ACTIVE')
ON CONFLICT (key) DO NOTHING;

-- Plan 3 : CROISSANCE (Europe - EUR)
INSERT INTO saas_plans (key, name, description, price_monthly, price_yearly, currency,
    limits_json, features_json, is_active, sort_order, is_public, trial_days, annual_discount_pct,
    seats_limit, storage_limit_mb, ai_credits_limit, status)
VALUES ('GROWTH', 'Croissance', 'Pour les églises en expansion. Fonctionnalités avancées et IA illimitée.',
    29, 290, 'EUR',
    '{"members": 500, "spaces": 25, "storage_mb": 20000, "events": 200, "ai_credits": 2000}',
    '{"dashboard": true, "members": true, "families": true, "churches": true, "events": true, "notifications": true, "documents": true, "reports": true, "analytics": true, "discipleship": true, "academy": true, "ai": true, "finance": true, "mobile_money": false, "marketplace": true, "community": true, "forms": true, "calendar": true}',
    true, 3, true, 30, 17,
    500, 20000, 2000, 'ACTIVE')
ON CONFLICT (key) DO NOTHING;

-- Plan 4 : RÉSEAU & CAMPUS (International - USD)
INSERT INTO saas_plans (key, name, description, price_monthly, price_yearly, currency,
    limits_json, features_json, is_active, sort_order, is_public, trial_days, annual_discount_pct,
    seats_limit, storage_limit_mb, ai_credits_limit, status)
VALUES ('NETWORK', 'Réseau & Campus', 'Pour les réseaux d''églises et campus. Multi-tenant avancé, white-label, support dédié.',
    99, 990, 'USD',
    '{"members": 2000, "spaces": 100, "storage_mb": 100000, "events": 1000, "ai_credits": 5000}',
    '{"dashboard": true, "members": true, "families": true, "churches": true, "events": true, "notifications": true, "documents": true, "reports": true, "analytics": true, "discipleship": true, "academy": true, "ai": true, "finance": true, "mobile_money": true, "marketplace": true, "community": true, "forms": true, "calendar": true, "white_label": true, "api_access": true, "sso": true}',
    true, 4, true, 30, 17,
    2000, 100000, 5000, 'ACTIVE')
ON CONFLICT (key) DO NOTHING;

COMMENT ON TABLE saas_plans IS 'Plans SaaS Dual-Market (G1.4 - §30-31) : DÉCOUVERTE/DÉMARRAGE/CROISSANCE/RÉSEAU & CAMPUS';
