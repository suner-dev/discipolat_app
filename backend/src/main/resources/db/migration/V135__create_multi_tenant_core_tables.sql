-- V135__create_multi_tenant_core_tables.sql
-- ============================================================
-- MULTI-TENANT CORE: Membership, RBAC, Organization Hierarchy, Plans, Invitations, Audit
-- ============================================================

-- 1. Enable ltree extension for organization hierarchy paths
CREATE EXTENSION IF NOT EXISTS ltree;

-- 2. TENANT MEMBERSHIPS (User <-> Tenant many-to-many with role)
CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'PENDING', 'SUSPENDED', 'REVOKED')),
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invited_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_membership_user_tenant UNIQUE (user_id, tenant_id)
);

CREATE INDEX idx_tenant_membership_tenant ON tenant_memberships(tenant_id);
CREATE INDEX idx_tenant_membership_user ON tenant_memberships(user_id);
CREATE INDEX idx_tenant_membership_status ON tenant_memberships(status);

-- 3. ROLES (customizable per tenant + system roles)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    key VARCHAR(50) NOT NULL,
    label VARCHAR(100) NOT NULL,
    description TEXT,
    system BOOLEAN NOT NULL DEFAULT FALSE,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_tenant_key UNIQUE (tenant_id, key)
);

CREATE INDEX idx_role_tenant ON roles(tenant_id);
CREATE INDEX idx_role_system ON roles(system);

-- 4. PERMISSIONS (global catalog with scope)
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE,
    key VARCHAR(100) NOT NULL UNIQUE,
    label VARCHAR(150) NOT NULL,
    description TEXT,
    scope VARCHAR(20) NOT NULL CHECK (scope IN ('GLOBAL', 'TENANT', 'CHURCH', 'SUB_CHURCH', 'DEPARTMENT', 'FAMILY', 'OWN')),
    category VARCHAR(50),
    system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permission_tenant ON permissions(tenant_id);
CREATE INDEX idx_permission_scope ON permissions(scope);
CREATE INDEX idx_permission_category ON permissions(category);
CREATE INDEX idx_permission_system ON permissions(system);

-- 5. ROLE_PERMISSIONS (many-to-many)
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- 6. ORGANIZATION NODES (hierarchical: church, campus, sub-church, department, group)
CREATE TABLE organization_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES organization_nodes(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('ROOT_CHURCH', 'CAMPUS', 'SUB_CHURCH', 'ASSEMBLY', 'REGION', 'DISTRICT', 'DEPARTMENT', 'GROUP')),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    path LTREE NOT NULL,
    level INTEGER NOT NULL DEFAULT 0,
    timezone VARCHAR(64),
    country VARCHAR(2),
    city VARCHAR(100),
    metadata_json JSONB,
    responsible_id UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_org_node_tenant ON organization_nodes(tenant_id);
CREATE INDEX idx_org_node_parent ON organization_nodes(parent_id);
CREATE INDEX idx_org_node_type ON organization_nodes(type);
CREATE INDEX idx_org_node_path ON organization_nodes USING GIST (path);
CREATE INDEX idx_org_node_tenant_type ON organization_nodes(tenant_id, type);
CREATE INDEX idx_org_node_responsible ON organization_nodes(responsible_id);

-- 7. SAAS PLANS (global catalog)
CREATE TABLE saas_plans (
    key VARCHAR(30) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price_monthly BIGINT NOT NULL,
    price_yearly BIGINT,
    currency VARCHAR(3) NOT NULL DEFAULT 'XAF',
    limits_json JSONB NOT NULL,
    features_json JSONB NOT NULL,
    stripe_price_id_monthly VARCHAR(100),
    stripe_price_id_yearly VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. TENANT SUBSCRIPTIONS
CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_key VARCHAR(30) NOT NULL REFERENCES saas_plans(key),
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL' CHECK (status IN ('TRIAL', 'ACTIVE', 'PAST_DUE', 'CANCELED', 'PAUSED', 'EXPIRED')),
    billing_cycle VARCHAR(10) NOT NULL DEFAULT 'monthly' CHECK (billing_cycle IN ('monthly', 'yearly')),
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMP,
    trial_ends_at TIMESTAMP,
    stripe_subscription_id VARCHAR(100),
    stripe_customer_id VARCHAR(100),
    quotas_json JSONB,
    metadata_json JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subscription_tenant ON tenant_subscriptions(tenant_id);
CREATE INDEX idx_subscription_status ON tenant_subscriptions(status);
CREATE INDEX idx_subscription_stripe_sub ON tenant_subscriptions(stripe_subscription_id);
CREATE INDEX idx_subscription_stripe_cus ON tenant_subscriptions(stripe_customer_id);

-- 9. INVITATIONS
CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    scope_type VARCHAR(30),
    scope_id UUID,
    inviter_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED', 'CANCELED')),
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invitation_tenant ON invitations(tenant_id);
CREATE INDEX idx_invitation_email ON invitations(email);
CREATE INDEX idx_invitation_status ON invitations(status);
CREATE INDEX idx_invitation_expires ON invitations(expires_at);

-- 10. AUDIT LOGS (tenant-aware, high volume)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    actor_id UUID,
    actor_type VARCHAR(30),
    action VARCHAR(100) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id UUID,
    result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' CHECK (result IN ('SUCCESS', 'FAILURE', 'ERROR', 'DENIED')),
    metadata_json JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_ms BIGINT
);

CREATE INDEX idx_audit_tenant ON audit_logs(tenant_id);
CREATE INDEX idx_audit_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_resource ON audit_logs(resource, resource_id);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_tenant_timestamp ON audit_logs(tenant_id, timestamp);

-- 11. Add tenant_id to existing roles table if not exists (from V70 platform_roles)
-- Note: V70 created platform_roles with tenant_id. We'll keep both for migration period.

-- 12. Insert default system roles (tenant_id = NULL = global)
INSERT INTO roles (id, tenant_id, key, label, description, system, priority, created_at, updated_at) VALUES
    (uuid_generate_v4(), NULL, 'PLATFORM_SUPER_ADMIN', 'Super Admin Plateforme', 'Accès complet à la plateforme Discipolat', TRUE, 1000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'TENANT_OWNER', 'Propriétaire Tenant', 'Propriétaire de l\'organisation', TRUE, 900, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'TENANT_ADMIN', 'Admin Tenant', 'Administrateur de l\'organisation', TRUE, 800, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'CHURCH_ADMIN', 'Admin Église', 'Administrateur d\'église/campus', TRUE, 700, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'CHURCH_LEADER', 'Leader Église', 'Responsable d\'église/campus', TRUE, 600, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'DEPARTMENT_ADMIN', 'Admin Département', 'Administrateur de département', TRUE, 500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'DEPARTMENT_LEADER', 'Leader Département', 'Responsable de département', TRUE, 400, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'FAMILY_LEADER', 'Chef de Famille', 'Responsable de famille/groupe', TRUE, 300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'DISCIPLE_MAKER', 'Faiseur de Disciples', 'Accompagnateur spirituel', TRUE, 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MEMBER', 'Membre', 'Membre standard', TRUE, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'GUEST', 'Invité', 'Accès lecture seule', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (tenant_id, key) DO NOTHING;

-- 13. Insert default permissions (system, global)
INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system, created_at, updated_at) VALUES
    -- MEMBER permissions
    (uuid_generate_v4(), NULL, 'MEMBER_READ', 'Lire membres', 'Voir la liste et détails des membres', 'TENANT', 'MEMBERS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MEMBER_CREATE', 'Créer membres', 'Ajouter de nouveaux membres', 'TENANT', 'MEMBERS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MEMBER_UPDATE', 'Modifier membres', 'Modifier les informations des membres', 'TENANT', 'MEMBERS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'MEMBER_DELETE', 'Supprimer membres', 'Supprimer/archiver des membres', 'TENANT', 'MEMBERS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- FAMILY permissions
    (uuid_generate_v4(), NULL, 'FAMILY_READ', 'Lire familles', 'Voir les familles', 'TENANT', 'FAMILIES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'FAMILY_CREATE', 'Créer familles', 'Créer de nouvelles familles', 'TENANT', 'FAMILIES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'FAMILY_UPDATE', 'Modifier familles', 'Modifier les familles', 'TENANT', 'FAMILIES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'FAMILY_DELETE', 'Supprimer familles', 'Supprimer/archiver des familles', 'TENANT', 'FAMILIES', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- REPORT permissions
    (uuid_generate_v4(), NULL, 'REPORT_READ', 'Lire rapports', 'Voir les rapports de suivi', 'TENANT', 'REPORTS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'REPORT_CREATE', 'Créer rapports', 'Soumettre des rapports', 'TENANT', 'REPORTS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- FINANCE permissions
    (uuid_generate_v4(), NULL, 'FINANCE_READ', 'Lire finances', 'Voir transactions et budgets', 'TENANT', 'FINANCE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'FINANCE_MANAGE', 'Gérer finances', 'Créer/modifier transactions et budgets', 'TENANT', 'FINANCE', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- COURSE permissions
    (uuid_generate_v4(), NULL, 'COURSE_CREATE', 'Créer formations', 'Créer des cours et modules', 'TENANT', 'ACADEMY', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'COURSE_MANAGE', 'Gérer formations', 'Gérer toutes les formations', 'TENANT', 'ACADEMY', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- TENANT SETTINGS permissions
    (uuid_generate_v4(), NULL, 'TENANT_SETTINGS_READ', 'Lire config tenant', 'Voir la configuration du tenant', 'TENANT', 'SETTINGS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'TENANT_SETTINGS_UPDATE', 'Modifier config tenant', 'Modifier la configuration du tenant', 'TENANT', 'SETTINGS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- CHURCH permissions
    (uuid_generate_v4(), NULL, 'CHURCH_CREATE', 'Créer églises', 'Créer églises/campus/sous-églises', 'TENANT', 'ORGANIZATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'CHURCH_MANAGE', 'Gérer églises', 'Gérer la hiérarchie des églises', 'TENANT', 'ORGANIZATION', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- USER permissions
    (uuid_generate_v4(), NULL, 'USER_INVITE', 'Inviter utilisateurs', 'Envoyer des invitations', 'TENANT', 'USERS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (uuid_generate_v4(), NULL, 'USER_MANAGE', 'Gérer utilisateurs', 'Gérer rôles et appartenances', 'TENANT', 'USERS', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (key) DO NOTHING;

-- 14. Link system permissions to system roles (admin gets all)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.system = TRUE AND p.system = TRUE
  AND r.key IN ('PLATFORM_SUPER_ADMIN', 'TENANT_OWNER', 'TENANT_ADMIN')
ON CONFLICT DO NOTHING;

-- 15. Link specific permissions to specific roles
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.key IN (
    'MEMBER_READ', 'FAMILY_READ', 'REPORT_READ', 'REPORT_CREATE'
)
WHERE r.system = TRUE AND r.key = 'CHURCH_LEADER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.key IN (
    'MEMBER_READ', 'MEMBER_CREATE', 'MEMBER_UPDATE',
    'FAMILY_READ', 'FAMILY_CREATE', 'FAMILY_UPDATE',
    'REPORT_READ', 'REPORT_CREATE'
)
WHERE r.system = TRUE AND r.key = 'DEPARTMENT_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.key IN (
    'MEMBER_READ', 'REPORT_CREATE'
)
WHERE r.system = TRUE AND r.key = 'DISCIPLE_MAKER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.key IN (
    'MEMBER_READ'
)
WHERE r.system = TRUE AND r.key = 'FAMILY_LEADER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.key IN (
    'MEMBER_READ', 'REPORT_CREATE'
)
WHERE r.system = TRUE AND r.key = 'MEMBER'
ON CONFLICT DO NOTHING;

-- 16. Insert default SaaS plans
INSERT INTO saas_plans (key, name, description, price_monthly, price_yearly, currency, limits_json, features_json, sort_order) VALUES
    ('FREE', 'Gratuit', 'Plan gratuit pour petites équipes', 0, 0, 'XAF',
     '{"max_users": 50, "max_churches": 1, "max_storage_mb": 100, "max_admin_users": 2, "max_ai_requests_month": 100, "max_courses": 5, "max_messages_month": 1000}'::jsonb,
     '{"discipleship": true, "academy": false, "finance": false, "marketplace": false, "api": false, "ai_copilot": false, "support": "community"}'::jsonb,
     0),
    ('STARTER', 'Démarrage', 'Pour églises en croissance', 15000, 150000, 'XAF',
     '{"max_users": 200, "max_churches": 3, "max_storage_mb": 1000, "max_admin_users": 5, "max_ai_requests_month": 1000, "max_courses": 20, "max_messages_month": 10000}'::jsonb,
     '{"discipleship": true, "academy": true, "finance": false, "marketplace": false, "api": false, "ai_copilot": true, "support": "email"}'::jsonb,
     1),
    ('PRO', 'Professionnel', 'Pour réseaux d\'églises', 50000, 500000, 'XAF',
     '{"max_users": 1000, "max_churches": 10, "max_storage_mb": 10000, "max_admin_users": 20, "max_ai_requests_month": 10000, "max_courses": 100, "max_messages_month": 100000}'::jsonb,
     '{"discipleship": true, "academy": true, "finance": true, "marketplace": true, "api": true, "ai_copilot": true, "support": "priority"}'::jsonb,
     2),
    ('ENTERPRISE', 'Entreprise', 'Pour grandes dénominations', 200000, 2000000, 'XAF',
     '{"max_users": 10000, "max_churches": 100, "max_storage_mb": 100000, "max_admin_users": 100, "max_ai_requests_month": 100000, "max_courses": 1000, "max_messages_month": 1000000}'::jsonb,
     '{"discipleship": true, "academy": true, "finance": true, "marketplace": true, "api": true, "ai_copilot": true, "support": "dedicated"}'::jsonb,
     3)
ON CONFLICT (key) DO NOTHING;

-- 17. Create trigger for updated_at on new tables
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_tenant_memberships_updated_at ON tenant_memberships;
CREATE TRIGGER update_tenant_memberships_updated_at BEFORE UPDATE ON tenant_memberships FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_roles_updated_at ON roles;
CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON roles FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_permissions_updated_at ON permissions;
CREATE TRIGGER update_permissions_updated_at BEFORE UPDATE ON permissions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_organization_nodes_updated_at ON organization_nodes;
CREATE TRIGGER update_organization_nodes_updated_at BEFORE UPDATE ON organization_nodes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_saas_plans_updated_at ON saas_plans;
CREATE TRIGGER update_saas_plans_updated_at BEFORE UPDATE ON saas_plans FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_tenant_subscriptions_updated_at ON tenant_subscriptions;
CREATE TRIGGER update_tenant_subscriptions_updated_at BEFORE UPDATE ON tenant_subscriptions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_invitations_updated_at ON invitations;
CREATE TRIGGER update_invitations_updated_at BEFORE UPDATE ON invitations FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 18. Add ltree GIST index for path queries (already created above)

-- 19. Add composite unique index for organization node code per tenant
CREATE UNIQUE INDEX uk_org_node_tenant_code ON organization_nodes(tenant_id, code) WHERE code IS NOT NULL;

-- 20. Grant permissions (adjust for your DB user)
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO discipolat_app;