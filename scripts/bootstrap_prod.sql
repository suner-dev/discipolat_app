-- Bootstrap prod réel : purge des données de test seedées par Flyway (V2/V4/V5..),
-- puis insertion d'un tenant applicatif + d'un admin réel.
BEGIN;

-- Ordre children -> parents pour respecter les FK.
TRUNCATE TABLE
    event_registrations,
    soul_notes,
    soul_departments,
    user_departments,
    member_departments,
    member_presences,
    family_reports,
    maker_reports,
    prayers,
    events,
    "event",
    legacy_events,
    objectives,
    badges,
    alerts,
    courses,
    custom_field_values,
    families,
    family_department_history,
    departments,
    souls,
    user_roles,
    users
    CASCADE;

-- Admin réel (bcrypt 12 du mot de passe Discipolat@2026).
WITH new_user AS (
    INSERT INTO users (
        email, password_hash, first_name, last_name,
        role, active_role, statut, tenant_id, est_chef_de_famille, deleted, failed_login_attempts
    ) VALUES (
        'arise@discipolat.io',
        '$2b$12$yd.OfgASf8klsSuyVKpTKuqpPf1Ouaymkn0rVwZSJiT..o1.sjTXq',
        'Arise', 'KABULO',
        'ADMIN', 'ADMIN', 'ACTIVE',
        '00000000-0000-0000-0000-000000000001',
        false, false, 0
    )
    RETURNING id
)
INSERT INTO user_roles (user_id, role, tenant_id)
SELECT id, 'ADMIN', '00000000-0000-0000-0000-000000000001' FROM new_user;

INSERT INTO tenant_settings (tenant_id, business_name, slogan)
SELECT '00000000-0000-0000-0000-000000000001', 'Discipolat', 'Église locale'
WHERE NOT EXISTS (
    SELECT 1 FROM tenant_settings WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
);

UPDATE tenants SET name = 'Discipolat', slug = 'discipolat'
WHERE id = '00000000-0000-0000-0000-000000000001';

COMMIT;

SELECT 'users' t, count(*) FROM users
UNION ALL SELECT 'user_roles', count(*) FROM user_roles
UNION ALL SELECT 'souls', count(*) FROM souls
UNION ALL SELECT 'families', count(*) FROM families
UNION ALL SELECT 'departments', count(*) FROM departments
UNION ALL SELECT 'prayers', count(*) FROM prayers;
