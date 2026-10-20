ALTER TABLE saas_plans
    ADD COLUMN IF NOT EXISTS modules_included_json JSONB,
    ADD COLUMN IF NOT EXISTS price_eur BIGINT,
    ADD COLUMN IF NOT EXISTS price_xaf BIGINT,
    ADD COLUMN IF NOT EXISTS price_usd BIGINT,
    ADD COLUMN IF NOT EXISTS billing_period VARCHAR(20),
    ADD COLUMN IF NOT EXISTS regions_json JSONB;

UPDATE saas_plans
SET is_public = false
WHERE key NOT IN ('DISCOVERY', 'STARTUP', 'GROWTH', 'NETWORK');

UPDATE saas_plans
SET modules_included_json = '["dashboard", "members", "families", "churches", "events", "notifications", "documents", "reports", "forms", "calendar"]'::jsonb,
    price_eur = 0,
    price_xaf = 0,
    price_usd = 0,
    billing_period = 'monthly',
    regions_json = '["AF", "EU", "US"]'::jsonb,
    is_public = true,
    is_active = true,
    status = 'ACTIVE',
    sort_order = 1
WHERE key = 'DISCOVERY';

UPDATE saas_plans
SET modules_included_json = '["dashboard", "members", "families", "churches", "events", "notifications", "documents", "reports", "finance", "mobile_money", "forms", "calendar"]'::jsonb,
    price_eur = NULL,
    price_xaf = 15000,
    price_usd = NULL,
    billing_period = 'monthly',
    regions_json = '["AF"]'::jsonb,
    is_public = true,
    is_active = true,
    status = 'ACTIVE',
    sort_order = 2
WHERE key = 'STARTUP';

UPDATE saas_plans
SET modules_included_json = '["dashboard", "members", "families", "churches", "events", "notifications", "documents", "reports", "analytics", "discipleship", "academy", "ai", "finance", "marketplace", "community", "forms", "calendar"]'::jsonb,
    price_eur = 29,
    price_xaf = NULL,
    price_usd = NULL,
    billing_period = 'monthly',
    regions_json = '["AF", "EU"]'::jsonb,
    is_public = true,
    is_active = true,
    status = 'ACTIVE',
    sort_order = 3
WHERE key = 'GROWTH';

UPDATE saas_plans
SET modules_included_json = '["dashboard", "members", "families", "churches", "events", "notifications", "documents", "reports", "analytics", "discipleship", "academy", "ai", "finance", "mobile_money", "marketplace", "community", "white_label", "api_access", "sso", "forms", "calendar"]'::jsonb,
    price_eur = NULL,
    price_xaf = NULL,
    price_usd = 99,
    billing_period = 'monthly',
    regions_json = '["AF", "EU", "US"]'::jsonb,
    is_public = true,
    is_active = true,
    status = 'ACTIVE',
    sort_order = 4
WHERE key = 'NETWORK';

UPDATE saas_plans
SET limits_json = '{"members": 50, "max_users": 50, "max_churches": 1, "max_departments": 3, "max_campuses": 1, "max_groups": 1, "spaces": 3, "storage_mb": 500, "max_storage_mb": 500, "events": 10, "ai_credits": 100, "max_ai_requests_month": 100, "max_courses": 5, "max_messages_month": 1000}'::jsonb
WHERE key = 'DISCOVERY';

UPDATE saas_plans
SET limits_json = '{"members": 200, "max_users": 200, "max_churches": 3, "max_departments": 10, "max_campuses": 3, "max_groups": 5, "spaces": 10, "storage_mb": 5000, "max_storage_mb": 5000, "events": 50, "ai_credits": 500, "max_ai_requests_month": 500, "max_courses": 20, "max_messages_month": 10000}'::jsonb
WHERE key = 'STARTUP';

UPDATE saas_plans
SET limits_json = '{"members": 500, "max_users": 500, "max_churches": 10, "max_departments": 25, "max_campuses": 10, "max_groups": 10, "spaces": 25, "storage_mb": 20000, "max_storage_mb": 20000, "events": 200, "ai_credits": 2000, "max_ai_requests_month": 2000, "max_courses": 100, "max_messages_month": 100000}'::jsonb
WHERE key = 'GROWTH';

UPDATE saas_plans
SET limits_json = '{"members": 2000, "max_users": 2000, "max_churches": 100, "max_departments": 100, "max_campuses": 50, "max_groups": 50, "spaces": 100, "storage_mb": 100000, "max_storage_mb": 100000, "events": 1000, "ai_credits": 5000, "max_ai_requests_month": 5000, "max_courses": 1000, "max_messages_month": 1000000}'::jsonb
WHERE key = 'NETWORK';

UPDATE tenants
SET plan = CASE LOWER(plan)
    WHEN 'free' THEN 'DISCOVERY'
    WHEN 'starter' THEN 'STARTUP'
    WHEN 'pro' THEN 'GROWTH'
    WHEN 'enterprise' THEN 'NETWORK'
    WHEN 'discovery' THEN 'DISCOVERY'
    WHEN 'startup' THEN 'STARTUP'
    WHEN 'growth' THEN 'GROWTH'
    WHEN 'network' THEN 'NETWORK'
    ELSE 'DISCOVERY'
END;

UPDATE tenants t
SET features_json = p.features_json
FROM saas_plans p
WHERE p.key = t.plan;

INSERT INTO tenant_subscriptions (
    id,
    tenant_id,
    plan_key,
    status,
    billing_cycle,
    current_period_start,
    current_period_end,
    cancel_at_period_end,
    quotas_json,
    metadata_json,
    created_at,
    updated_at
)
SELECT
    uuid_generate_v4(),
    t.id,
    t.plan,
    'ACTIVE',
    'monthly',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    FALSE,
    p.limits_json,
    '{}'::jsonb,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM tenants t
JOIN saas_plans p ON p.key = t.plan AND p.is_active = TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_subscriptions ts
    WHERE ts.tenant_id = t.id
);

CREATE INDEX IF NOT EXISTS idx_subscription_due_changes
    ON tenant_subscriptions(status, current_period_start)
    WHERE status = 'PENDING_CHANGE';
