-- V260__parametrisation_admin_events_read.sql
-- ============================================================
-- PARAMÉTRISATION PAR ÉGLISE — correction de cohérence du rôle ADMIN.
--
-- Le seed V258 accorde à ADMIN le *_EXECUTE générique de la plupart des
-- modules (SOULS/FAMILIES/DEPARTMENTS/PAYMENTS_EXECUTE…) mais a omis
-- EVENTS_EXECUTE. Or EVENTS_EXECUTE garde la LECTURE et la LISTE des
-- événements (EventController GET /{id}, GET, GET /department/{id}, …).
-- Résultat : ADMIN pouvait créer/modifier/archiver/supprimer un événement
-- (il détient EVENTS_EXECUTE_CREATEEVENT/UPDATEEVENT/ARCHIVEEVENT/DELETEEVENT)
-- mais ne pouvait ni le consulter ni le lister — rôle incohérent, et
-- cassure du test d'isolation multi-tenant (un ADMIN doit franchir la garde
-- RBAC pour atteindre la frontière de tenant, qui répond alors 404, pas 403).
--
-- Purement ADDITIF : une seule capacité LECTURE ajoutée à ADMIN ; aucun
-- autre rôle touché, aucun droit retiré, aucune ligne modifiée.
--   1) couple globale (ADMIN, EVENTS_EXECUTE) ;
--   2) re-héritage global -> surcouche locale homonyme (piège #7 : une
--      capacité globale ajoutée après V259 §5 ne descend pas d'elle-même
--      dans les rôles locaux d'église) — additif, ON CONFLICT DO NOTHING.
-- Cible PostgreSQL (Flyway désactivé sous H2/tests ; seed H2 = import.sql).
-- ============================================================

-- 1) ADMIN détient la lecture/liste des événements comme ses autres modules.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM (VALUES
    ('ADMIN', 'EVENTS_EXECUTE')
) AS m(role_key, perm_key)
JOIN roles r ON r.key = m.role_key AND r.tenant_id IS NULL
JOIN permissions p ON p.key = m.perm_key
ON CONFLICT DO NOTHING;

-- 2) Répercussion de la nouvelle capacité globale vers les surcouches locales
--    homonymes (miroir exact du backfill V259 §5, rejoué pour les clés ajoutées
--    depuis). Additif : une église garde le pouvoir de RETIRER en local ensuite.
INSERT INTO role_permissions (role_id, permission_id)
SELECT local.id, rp.permission_id
FROM roles g
JOIN roles local ON local.key = g.key AND local.tenant_id IS NOT NULL
JOIN role_permissions rp ON rp.role_id = g.id
WHERE g.tenant_id IS NULL
ON CONFLICT DO NOTHING;
