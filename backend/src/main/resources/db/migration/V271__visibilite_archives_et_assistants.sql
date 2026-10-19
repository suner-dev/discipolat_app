-- V271__visibilite_archives_et_assistants.sql
-- ============================================================
-- VISIBILITÉ — Archives, Assistant IA, Assistant Vocal.
-- ------------------------------------------------------------
-- Audit client (oct. 2026) : ces trois écrans existent (routes React
-- réelles, endpoint serveur GET /church-events/archives pour les
-- archives) mais n'apparaissaient dans AUCUN menu : la table
-- `menu_entries` — source du menu latéral servi par
-- GET /platform/menus — ne portait aucune ligne pour eux. Un écran
-- sans entrée de menu est invisible pour tous les rôles, y compris
-- pour les églises qui le voudraient activé.
--
-- Correction ADDITIVE (rien n'est supprimé, aucun accès élargi) :
--  1) Clé UI `UI_ARCHIVES_ACCESS` — consultation des archives
--     d'événements. Matrice = rôles qui détiennent déjà la garde
--     serveur `EVENTS_EXECUTE_R6A2` exigée par
--     GET /church-events/archives (ADMIN, PASTEUR, RESPONSABLE,
--     CHEF_DE_FAMILLE) : parité exacte, aucun rôle ne voit un bouton
--     qui lui renverrait 403.
--  2) Trois entrées de menu (section « Engagement & outils » :
--     regroupement automatique par section, cf. V229) :
--      - « Assistant IA » /ai-assistant — rôles tenant la clé servie
--        UI_AI_ASSISTANT_ACCESS de la garde de route (5 rôles, MEMBRE
--        exclu car la route le refuse) ;
--      - « Assistant Vocal » /voice-assistant — même matrice
--        (UI_VOICE_ASSISTANT_ACCESS refuse MEMBRE) ;
--      - « Archives » /archives — matrice de la clé (4 rôles).
--     module_key NULL = menu autonome (non couplé à un module
--     activable), comme le Profil (V36).
--  3) Répercussion des clés globales vers les surcouches locales
--     homonymes (miroir V259 §5 / V261 §3 / V270 §3) : additif, une
--     église garde le pouvoir de RETIRER en local ensuite.
--
-- ⚠ CONTRAINTE OUTILLAGE (import.sql / gen_h2_seed.py) : pas
-- d'apostrophe SQL échappée dans les libellés des tuples catalogue
-- et matrice (cf. en-tête V270).
-- ============================================================

-- 1) Catalogue des permissions UI (globales, système).
INSERT INTO permissions (id, tenant_id, key, label, description, scope, category, system, created_at, updated_at)
SELECT uuid_generate_v4(), NULL, p.key, p.label, p.label, 'TENANT', p.category, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('UI_ARCHIVES_ACCESS', 'Consultation des archives des événements', 'UI')
) AS p(key, label, category)
WHERE NOT EXISTS (SELECT 1 FROM permissions po WHERE po.key = p.key);

-- 2) Matrice par défaut : parité EXACTE des détenteurs de la garde
--    serveur EVENTS_EXECUTE_R6A2 sur GET /church-events/archives.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('ADMIN', 'UI_ARCHIVES_ACCESS'),
    ('PASTEUR', 'UI_ARCHIVES_ACCESS'),
    ('RESPONSABLE', 'UI_ARCHIVES_ACCESS'),
    ('CHEF_DE_FAMILLE', 'UI_ARCHIVES_ACCESS')
) AS m(role_key, perm_key)
JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL
JOIN permissions p ON p.key = m.perm_key
ON CONFLICT DO NOTHING;

-- 3) Répercussion des nouvelles clés globales vers les surcouches
--    locales homonymes. Additif : une église peut retirer en local.
INSERT INTO role_permissions (role_id, permission_id)
SELECT local.id, rp.permission_id
FROM roles g
JOIN roles local ON local.key = g.key AND local.tenant_id IS NOT NULL
JOIN role_permissions rp ON rp.role_id = g.id
WHERE g.tenant_id IS NULL
  AND rp.permission_id IN (SELECT id FROM permissions WHERE key = 'UI_ARCHIVES_ACCESS')
ON CONFLICT DO NOTHING;

-- 4) Entrées de menu (tenant plateforme par défaut, cf. backfill V70 ;
--    le service menusForRoles sert toutes les entrées actives).
INSERT INTO menu_entries (key, label, href, icon, section, ordre, roles, module_key, enabled, tenant_id)
VALUES
    ('assistant-ia', 'Assistant IA', '/ai-assistant', 'Sparkles', 'Engagement & outils', 11,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, NULL, TRUE,
     '00000000-0000-0000-0000-000000000001'),
    ('assistant-vocal', 'Assistant Vocal', '/voice-assistant', 'Mic', 'Engagement & outils', 12,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb, NULL, TRUE,
     '00000000-0000-0000-0000-000000000001'),
    ('archives', 'Archives', '/archives', 'Archive', 'Engagement & outils', 13,
     '["ADMIN","PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE"]'::jsonb, NULL, TRUE,
     '00000000-0000-0000-0000-000000000001')
ON CONFLICT (key) DO NOTHING;
