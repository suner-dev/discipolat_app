-- V72__enhanced_permissions.sql
-- Permissions granulaires (lecture/écriture/suppression) + scope configurable

-- 1. Ajouter les colonnes R/W/D et scope à role_permissions
ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS can_read  BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS can_write BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS can_delete BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE role_permissions ADD COLUMN IF NOT EXISTS scope VARCHAR(30) NOT NULL DEFAULT 'GLOBAL';

-- 2. Migrer les données existantes : enabled=true → R+W, enabled=false → tout faux
UPDATE role_permissions SET can_read = enabled, can_write = enabled, can_delete = FALSE WHERE enabled = TRUE;
UPDATE role_permissions SET can_read = FALSE, can_write = FALSE, can_delete = FALSE WHERE enabled = FALSE;

-- 3. La colonne enabled reste pour rétrocompatibilité (PermissionGuard historique)
-- Elle est maintenue synchronisée par le service.

-- 4. Ajouter un index pour les requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_role_perm_rwd ON role_permissions(role, permission, can_read, can_write, can_delete);

-- 5. Ajouter scope au permission_catalog pour définir le scope par défaut
ALTER TABLE permission_catalog ADD COLUMN IF NOT EXISTS default_scope VARCHAR(30) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE permission_catalog ADD COLUMN IF NOT EXISTS scope_description TEXT;

-- 6. Mettre à jour le catalog avec des descriptions de scope
UPDATE permission_catalog SET default_scope = 'DEPARTMENT', scope_description = 'Limité aux départements du responsable'
  WHERE module = 'DEPARTMENTS' AND default_scope = 'GLOBAL';
UPDATE permission_catalog SET default_scope = 'FAMILY', scope_description = 'Limité à la famille du chef'
  WHERE module = 'FAMILIES' AND default_scope = 'GLOBAL';
UPDATE permission_catalog SET default_scope = 'DEPARTMENT', scope_description = 'Limité aux départements assignés'
  WHERE module = 'REPORTS' AND key LIKE '%MAKER%' AND default_scope = 'GLOBAL';
