-- ============================================================
-- PORT Develop1 → main (lot « restauration des capacités absentes »).
-- Source: Develop1 V165__active_tenant_and_access_request.sql, renumérotée.
--
-- G5.4 (§55-56) :
--  1) tenant actif PERSISTANT par utilisateur : la bascule
--     /tenant-switcher/switch doit survivre aux requêtes suivantes.
--  2) notifications.type : miroir EXHAUSTIF de l'enum Java. La liste est
--     l'union de V51 (main) et des 4 types apportés par les services portés
--     (DEMANDE_ACCES, OFFLINE_CONFLIT, SUIVI_RAPPEL, PASTORAT_NOMINATION) :
--     sur-ensemble strict, aucune valeur existante retirée.
--  3) table access_request : traçabilité réelle des « Demander l'accès ».
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS active_tenant_id UUID;

CREATE INDEX IF NOT EXISTS idx_users_active_tenant ON users(active_tenant_id);

ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;

ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (
    type IN (
        'RAPPORT_NON_SOUMIS',
        'ABSENCE_48H',
        'RAPPORT_FAMILLE_NON_SOUMIS',
        'ALERTE_ABSENCE',
        'INFORMATION',
        'PRIERE_EXAUCEE',
        'TRANSFERT_DEMANDE',
        'TRANSFERT_VALIDATION',
        'TRANSFERT_VALIDEE',
        'TRANSFERT_REFUSEE',
        'TRANSFERT_INFOS_DEMANDEES',
        'TRANSFERT_CORRECTION',
        'TRANSFERT_EXECUTEE',
        'TRANSFERT_ANNULEE',
        'TRANSFERT_DELAI_DEPASSE',
        'MEMBRE_AJOUTE',
        'MEMBRE_RETIRE',
        'TACHE_ASSIGNEE',
        'TACHE_EN_RETARD',
        'MEMBRE_AFFECTE',
        'EVENEMENT_RAPPEL',
        'DEMANDE_ACCES',
        'OFFLINE_CONFLIT',
        'SUIVI_RAPPEL',
        'PASTORAT_NOMINATION'
    )
);

CREATE TABLE IF NOT EXISTS access_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    resource_label VARCHAR(200),
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'NOTIFIED'
        CHECK (status IN ('NOTIFIED', 'HANDLED', 'REJECTED')),
    notified_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    handled_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_access_request_user
    ON access_request(tenant_id, user_id, permission_key, created_at);

COMMENT ON TABLE access_request IS
    'G5.4 : demandes d acces emises depuis les gardes frontend (Acces refuse -> Demander l acces)';
