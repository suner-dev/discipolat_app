-- AdminRequest — demandes administratives (baptême, dédicace, accueil nouveau, …).
-- (P1 #57) Table absente des migrations : le schéma était auparavant créé via JPA
-- (ddl-auto=create en dev), ce qui cassait ddl-auto=validate/none en prod.

CREATE TABLE IF NOT EXISTS admin_requests (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                 UUID NOT NULL,
    demandeur_id              UUID NOT NULL,
    type_demande              VARCHAR(40) NOT NULL,
    motif                     VARCHAR(500) NOT NULL,
    details                   TEXT,
    statut                    VARCHAR(30) NOT NULL,
    traite_par                UUID,
    traite_le                 TIMESTAMP,
    commentaire_traitement    TEXT,
    soumise_le                TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_requests_tenant
    ON admin_requests (tenant_id);
CREATE INDEX IF NOT EXISTS idx_admin_requests_demandeur
    ON admin_requests (tenant_id, demandeur_id);
CREATE INDEX IF NOT EXISTS idx_admin_requests_statut
    ON admin_requests (tenant_id, statut);