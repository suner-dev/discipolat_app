-- V215__legacy_userrole_catalog_bridge.sql (PORTÉ de Develop1 V173)
-- ============================================================
-- §G6.4 — E2E CP8 (rôles vivantes) : l'enum historique UserRole
-- (ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR, MEMBRE) portée
-- par users.role / JWT n'avait AUCUNE ligne dans le catalogue multi-tenant
-- « roles » (clés canoniques TENANT_ADMIN, CHURCH_ADMIN, …). Conséquences
-- réelles : seedDefaultMemberships() du DataInitializer laissait 100 % des
-- comptes sans tenant_membership (« No role found … »), donc /me/permissions
-- et le contexte tenant restaient vides — la chaîne G4.4 « rôle modifié →
-- permissions recalculées » ne pouvait pas se propager.
--
-- Correctif : créer les six rôles GLOBAUX (tenant_id NULL, idempotent) en
-- miroir exact des droits du rôle canonique correspondant — le pont ne
-- INVENTE aucune permission, il expose les sets déjà audités (rôles du
-- catalogue V135+). Le frontend et @PreAuthorize continuent de parler
-- UserRole ; PermissionResolver résout enfin des clés.
-- ============================================================

DO $$
DECLARE
    m record;
    v_role_id uuid;
BEGIN
    FOR m IN
        SELECT * FROM (VALUES
            ('ADMIN',           'Administrateur',      'TENANT_ADMIN',     800),
            ('PASTEUR',         'Pasteur',             'CHURCH_ADMIN',     600),
            ('RESPONSABLE',     'Responsable',         'DEPARTMENT_ADMIN', 400),
            ('CHEF_DE_FAMILLE', 'Chef de famille',     'FAMILY_LEADER',    300),
            ('FAISEUR',         'Faiseur de disciples','DISCIPLE_MAKER',   200),
            ('MEMBRE',          'Membre',              'MEMBER',           100)
        ) AS t(key, label, twin, prio)
    LOOP
        IF NOT EXISTS (SELECT 1 FROM public.roles WHERE key = m.key AND tenant_id IS NULL) THEN
            v_role_id := uuid_generate_v4();
            INSERT INTO public.roles (id, tenant_id, key, label, description, system, priority)
            VALUES (v_role_id, NULL, m.key, m.label,
                    'Rôle historique (enum UserRole) — pont vers le catalogue multi-tenant, miroir de ' || m.twin,
                    true, m.prio);
            INSERT INTO public.role_permissions (role_id, permission_id)
            SELECT v_role_id, rp.permission_id
            FROM public.role_permissions rp
            JOIN public.roles r ON r.id = rp.role_id AND r.tenant_id IS NULL
            WHERE r.key = m.twin
            ON CONFLICT DO NOTHING;
        END IF;
    END LOOP;
END $$;
