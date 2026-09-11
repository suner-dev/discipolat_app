-- V136__add_membership_scope_and_role_fk.sql
-- Add scope_type, scope_id and role_id FK to tenant_memberships for scoped RBAC

-- 1. Add new columns to tenant_memberships
ALTER TABLE tenant_memberships
    ADD COLUMN IF NOT EXISTS role_id UUID REFERENCES roles(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS scope_type VARCHAR(30) CHECK (scope_type IN ('TENANT', 'REGION', 'CHURCH', 'SUB_CHURCH', 'CAMPUS', 'DEPARTMENT', 'FAMILY', 'ASSIGNED', 'OWN')),
    ADD COLUMN IF NOT EXISTS scope_id UUID;

-- 2. Create index for scoped membership queries
CREATE INDEX IF NOT EXISTS idx_tenant_membership_scope ON tenant_memberships(tenant_id, scope_type, scope_id);
CREATE INDEX IF NOT EXISTS idx_tenant_membership_role ON tenant_memberships(role_id);

-- 3. Backfill: for existing memberships, set scope_type = 'TENANT' and find matching role
UPDATE tenant_memberships tm
SET scope_type = 'TENANT',
    role_id = (
        SELECT r.id FROM roles r
        WHERE r.tenant_id = tm.tenant_id AND r.key = tm.role
        LIMIT 1
    )
WHERE tm.scope_type IS NULL;

-- 4. For memberships without matching role, assign TENANT_ADMIN role
UPDATE tenant_memberships tm
SET role_id = (
    SELECT r.id FROM roles r
    WHERE r.tenant_id = tm.tenant_id AND r.key = 'TENANT_ADMIN'
    LIMIT 1
)
WHERE tm.role_id IS NULL;

-- 5. Make role_id NOT NULL after backfill
ALTER TABLE tenant_memberships ALTER COLUMN role_id SET NOT NULL;
ALTER TABLE tenant_memberships ALTER COLUMN scope_type SET NOT NULL;
ALTER TABLE tenant_memberships ALTER COLUMN scope_type SET DEFAULT 'TENANT';

-- 6. Drop old role column (string) after migration
-- ALTER TABLE tenant_memberships DROP COLUMN role; -- Keep for now for rollback safety

-- 7. Add unique constraint: one membership per user per tenant per scope
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_membership_user_tenant_scope
    ON tenant_memberships(user_id, tenant_id, scope_type, scope_id);

-- 8. Add missing atomic permissions for full RBAC coverage
INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system, created_at, updated_at) VALUES
    -- EVENT permissions
    (uuid_generate_v4(), NULL, 'EVENT_READ', 'Lire événements', 'Voir les événements', 'TENANT', 'EVENTS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'EVENT_CREATE', 'Créer événements', 'Créer de nouveaux événements', 'TENANT', 'EVENTS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'EVENT_UPDATE', 'Modifier événements', 'Modifier les événements', 'TENANT', 'EVENTS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'EVENT_DELETE', 'Supprimer événements', 'Supprimer/archiver les événements', 'TENANT', 'EVENTS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'EVENT_REGISTER', 'S\'inscrire aux événements', 'S\'inscrire aux événements', 'TENANT', 'EVENTS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- NOTIFICATION permissions
    (uuid_generate_v4(), NULL, 'NOTIFICATION_READ', 'Lire notifications', 'Voir les notifications', 'TENANT', 'NOTIFICATIONS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'NOTIFICATION_CREATE', 'Créer notifications', 'Envoyer des notifications', 'TENANT', 'NOTIFICATIONS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'NOTIFICATION_MANAGE', 'Gérer notifications', 'Gérer toutes les notifications', 'TENANT', 'NOTIFICATIONS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- MESSAGE/CHAT permissions
    (uuid_generate_v4(), NULL, 'MESSAGE_READ', 'Lire messages', 'Voir les conversations', 'TENANT', 'MESSAGES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MESSAGE_CREATE', 'Envoyer messages', 'Envoyer des messages', 'TENANT', 'MESSAGES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MESSAGE_MANAGE', 'Gérer messages', 'Modérer les conversations', 'TENANT', 'MESSAGES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- FILE permissions
    (uuid_generate_v4(), NULL, 'FILE_READ', 'Lire fichiers', 'Télécharger/voir les fichiers', 'TENANT', 'FILES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'FILE_UPLOAD', 'Uploader fichiers', 'Télécharger des fichiers', 'TENANT', 'FILES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'FILE_MANAGE', 'Gérer fichiers', 'Gérer tous les fichiers', 'TENANT', 'FILES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- COURSE/ACADEMY permissions
    (uuid_generate_v4(), NULL, 'COURSE_READ', 'Lire formations', 'Voir les cours et modules', 'TENANT', 'ACADEMY', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'COURSE_ENROLL', 'S\'inscrire formations', 'S\'inscrire aux cours', 'TENANT', 'ACADEMY', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'COURSE_CREATE', 'Créer formations', 'Créer des cours et modules', 'TENANT', 'ACADEMY', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'COURSE_MANAGE', 'Gérer formations', 'Gérer toutes les formations', 'TENANT', 'ACADEMY', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- AI permissions
    (uuid_generate_v4(), NULL, 'AI_USE', 'Utiliser IA', 'Utiliser les fonctionnalités IA', 'TENANT', 'AI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'AI_ADMIN', 'Admin IA', 'Configurer et entraîner l\'IA', 'TENANT', 'AI', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- SETTINGS/BRANDING/MODULES permissions
    (uuid_generate_v4(), NULL, 'BRANDING_READ', 'Lire branding', 'Voir le branding', 'TENANT', 'SETTINGS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'BRANDING_UPDATE', 'Modifier branding', 'Modifier le branding', 'TENANT', 'SETTINGS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MODULES_READ', 'Lire modules', 'Voir les modules activés', 'TENANT', 'SETTINGS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MODULES_TOGGLE', 'Activer/Désactiver modules', 'Activer ou désactiver des modules', 'TENANT', 'SETTINGS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- AUDIT permissions
    (uuid_generate_v4(), NULL, 'AUDIT_READ', 'Lire audit', 'Voir les logs d\'audit', 'TENANT', 'AUDIT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'AUDIT_EXPORT', 'Exporter audit', 'Exporter les logs d\'audit', 'TENANT', 'AUDIT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- EXPORT permissions
    (uuid_generate_v4(), NULL, 'EXPORT_DATA', 'Exporter données', 'Exporter des données du tenant', 'TENANT', 'EXPORT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- ROLE/PERMISSION management
    (uuid_generate_v4(), NULL, 'ROLE_READ', 'Lire rôles', 'Voir les rôles', 'TENANT', 'RBAC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'ROLE_CREATE', 'Créer rôles', 'Créer des rôles personnalisés', 'TENANT', 'RBAC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'ROLE_UPDATE', 'Modifier rôles', 'Modifier les rôles', 'TENANT', 'RBAC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'ROLE_DELETE', 'Supprimer rôles', 'Supprimer des rôles personnalisés', 'TENANT', 'RBAC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'PERMISSION_READ', 'Lire permissions', 'Voir le catalogue de permissions', 'TENANT', 'RBAC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'PERMISSION_ASSIGN', 'Assigner permissions', 'Assigner permissions aux rôles', 'TENANT', 'RBAC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- ORGANIZATION NODE permissions
    (uuid_generate_v4(), NULL, 'ORG_NODE_READ', 'Lire structure', 'Voir la hiérarchie organisationnelle', 'TENANT', 'ORGANIZATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'ORG_NODE_CREATE', 'Créer nœuds', 'Créer églises/campus/départements', 'TENANT', 'ORGANIZATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'ORG_NODE_UPDATE', 'Modifier nœuds', 'Modifier la structure', 'TENANT', 'ORGANIZATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'ORG_NODE_DELETE', 'Supprimer nœuds', 'Supprimer des nœuds', 'TENANT', 'ORGANIZATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'ORG_NODE_MOVE', 'Déplacer nœuds', 'Déplacer dans la hiérarchie', 'TENANT', 'ORGANIZATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- SUBSCRIPTION/BILLING permissions
    (uuid_generate_v4(), NULL, 'SUBSCRIPTION_READ', 'Lire abonnement', 'Voir l\'abonnement du tenant', 'TENANT', 'BILLING', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'SUBSCRIPTION_MANAGE', 'Gérer abonnement', 'Modifier l\'abonnement', 'TENANT', 'BILLING', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    
    -- SUPPORT/IMPERSONATION permissions
    (uuid_generate_v4(), NULL, 'IMPERSONATE', 'Impersonner', 'Impersonner un utilisateur', 'PLATFORM', 'SUPPORT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'SUPPORT_ACCESS', 'Accès support', 'Accès aux outils de support', 'PLATFORM', 'SUPPORT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (key) DO NOTHING;

-- 9. Link new permissions to system roles
-- TENANT_OWNER: all TENANT scope permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE AND p.scope = 'TENANT'
WHERE r.system = TRUE AND r.key = 'TENANT_OWNER'
ON CONFLICT DO NOTHING;

-- TENANT_ADMIN: most TENANT scope permissions (except billing, role management)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE AND p.scope = 'TENANT' 
    AND p.key NOT IN ('SUBSCRIPTION_MANAGE', 'ROLE_CREATE', 'ROLE_DELETE', 'ROLE_UPDATE', 'PERMISSION_ASSIGN', 'IMPERSONATE')
WHERE r.system = TRUE AND r.key = 'TENANT_ADMIN'
ON CONFLICT DO NOTHING;

-- CHURCH_ADMIN: CHURCH scope + some TENANT scope
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE 
    AND (p.scope = 'CHURCH' OR p.key IN ('MEMBER_READ', 'MEMBER_CREATE', 'MEMBER_UPDATE', 'FAMILY_READ', 'FAMILY_CREATE', 'FAMILY_UPDATE', 'REPORT_READ', 'REPORT_CREATE', 'EVENT_READ', 'EVENT_CREATE', 'EVENT_UPDATE', 'NOTIFICATION_READ', 'NOTIFICATION_CREATE', 'COURSE_READ', 'COURSE_ENROLL'))
WHERE r.system = TRUE AND r.key = 'CHURCH_ADMIN'
ON CONFLICT DO NOTHING;

-- CHURCH_LEADER: subset
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE 
    AND p.key IN ('MEMBER_READ', 'FAMILY_READ', 'REPORT_READ', 'REPORT_CREATE', 'EVENT_READ', 'NOTIFICATION_READ', 'COURSE_READ', 'COURSE_ENROLL')
WHERE r.system = TRUE AND r.key = 'CHURCH_LEADER'
ON CONFLICT DO NOTHING;

-- DEPARTMENT_ADMIN: DEPARTMENT scope
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE 
    AND (p.scope = 'DEPARTMENT' OR p.key IN ('MEMBER_READ', 'MEMBER_CREATE', 'MEMBER_UPDATE', 'FAMILY_READ', 'FAMILY_CREATE', 'FAMILY_UPDATE', 'REPORT_READ', 'REPORT_CREATE', 'EVENT_READ', 'EVENT_CREATE', 'NOTIFICATION_READ', 'NOTIFICATION_CREATE'))
WHERE r.system = TRUE AND r.key = 'DEPARTMENT_ADMIN'
ON CONFLICT DO NOTHING;

-- DEPARTMENT_LEADER: subset
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE 
    AND p.key IN ('MEMBER_READ', 'REPORT_CREATE', 'FAMILY_READ', 'NOTIFICATION_READ')
WHERE r.system = TRUE AND r.key = 'DEPARTMENT_LEADER'
ON CONFLICT DO NOTHING;

-- DISCIPLE_MAKER: ASSIGNED scope
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE 
    AND p.key IN ('MEMBER_READ', 'REPORT_CREATE', 'COURSE_READ', 'COURSE_ENROLL')
WHERE r.system = TRUE AND r.key = 'DISCIPLE_MAKER'
ON CONFLICT DO NOTHING;

-- FAMILY_LEADER: FAMILY scope
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE 
    AND p.key IN ('MEMBER_READ', 'REPORT_CREATE', 'FAMILY_READ', 'FAMILY_UPDATE')
WHERE r.system = TRUE AND r.key = 'FAMILY_LEADER'
ON CONFLICT DO NOTHING;

-- MEMBER: OWN scope
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.system = TRUE 
    AND p.key IN ('MEMBER_READ', 'REPORT_CREATE', 'COURSE_READ', 'COURSE_ENROLL', 'EVENT_READ', 'EVENT_REGISTER')
WHERE r.system = TRUE AND r.key = 'MEMBER'
ON CONFLICT DO NOTHING;

-- 10. Update unique constraint on permissions to allow tenant-specific permissions
-- (tenant_id can be NULL for global, or set for tenant-specific)
-- The existing uk_permission_key on key alone is fine since key is globally unique
-- For tenant-specific permissions, we'd need a composite key - keeping simple for now