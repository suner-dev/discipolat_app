-- ============================================================================
-- V191 — A3 (M7) : fondations d'échelle — index composés (tenant_id, …) sur
--                  les tables chaudes + politique de partitionnement documentée
-- ----------------------------------------------------------------------------
-- Why : à plusieurs centaines de millions de rows partagées entre tenants,
--       chaque requête est filtrée par tenant_id (Hibernate tenantFilter).
--       Un index mono-colonne tenant_id puis un filtre supplémentaire force
--       PostgreSQL à trier après coup. L'index composé (tenant_id, colonne
--       la plus filtrée) sert directement le plan de requête.
--
-- Méthode (aucun inventaire « au feeling ») :
--   1. Tables chaudes identifiées par le code : entités mappées portant
--      @Filter(tenantFilter) ET fortes cardinalités attendues :
--      users, souls, families, departments, event, finance_transactions,
--      payment_intents, notifications, tenant_memberships, invitations.
--   2. Existence des index vérifiée sur le schéma réel via
--      SELECT indexname, indexdef FROM pg_indexes — cette migration n'ajoute
--      QUE ce qui manque réellement (CREATE INDEX IF NOT EXISTS, idempotent).
--      Les couples déjà indexés (ex. finance (tenant_id, date_transaction),
--      souls (tenant_id, famille_id), payment_intents (tenant_id, status),
--      event (tenant_id, start_at), users (tenant_id, email)) ne sont PAS
--      dupliqués.
--   3. Le plus volumineux est déterminé en exploitation par :
--        SELECT relname, reltuples FROM pg_class
--        WHERE relkind='r' ORDER BY reltuples DESC LIMIT 10;
--      Sur la base de référence (fraîche, max ≈ 300 rows/table), AUCUNE table
--      n'est à l'échelle justifiant une conversion RANGE en ligne : la
--      conversion est livrée comme procédure documentée dans
--      scripts/partition-hot-tables.sql (table partitionnée en parallèle +
--      bascule par renommage), jamais exécutée à l'aveugle en migration.
--      C'est le choix demandé par le prompt A3 : NE PAS risquer une
--      conversion sur une table peuplée, documenter le chemin.
-- ============================================================================

-- souls : la liste du catalogue (tenant + statut d'avancement discipolat)
CREATE INDEX IF NOT EXISTS idx_souls_tenant_statut
    ON souls (tenant_id, statut);

-- families : suivi pastoral filtré par statut / niveau de risque
CREATE INDEX IF NOT EXISTS idx_families_tenant_statut
    ON families (tenant_id, statut);
CREATE INDEX IF NOT EXISTS idx_families_tenant_niveau_risque
    ON families (tenant_id, niveau_risque);

-- departments : annuaire des départements par statut
CREATE INDEX IF NOT EXISTS idx_departments_tenant_statut
    ON departments (tenant_id, statut);
CREATE INDEX IF NOT EXISTS idx_departments_tenant_responsable
    ON departments (tenant_id, responsable_id);

-- notifications : fil « les plus récentes » du destinataire (table destinée à
-- être la plus volumineuse du SaaS : 1 ligne notifiée = 1 ligne stockée)
CREATE INDEX IF NOT EXISTS idx_notifications_tenant_created
    ON notifications (tenant_id, created_at DESC);

-- invitations : relances d'invitation (statut + expiration, scopées tenant)
CREATE INDEX IF NOT EXISTS idx_invitations_tenant_status_expires
    ON invitations (tenant_id, status, expires_at);
