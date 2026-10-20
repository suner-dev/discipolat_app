-- V163__fulltext_search_pg_trgm.sql
-- Enable pg_trgm extension for trigram-based full-text search
-- Add tsvector columns and GIN indexes for efficient full-text search across key entities

-- Enable pg_trgm extension for trigram similarity search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ============================================================
-- SOULS: Add full-text search vector and indexes
-- ============================================================
ALTER TABLE souls ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Create trigger function to update search_vector on insert/update
CREATE OR REPLACE FUNCTION souls_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('french', COALESCE(NEW.nom, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.prenom, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.email, '')), 'B') ||
        setweight(to_tsvector('french', COALESCE(NEW.telephone, '')), 'B') ||
        setweight(to_tsvector('french', COALESCE(NEW.profession, '')), 'C') ||
        setweight(to_tsvector('french', COALESCE(NEW.adresse, '')), 'C') ||
        setweight(to_tsvector('french', COALESCE(NEW.notes_pasteur, '')), 'D');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS souls_search_vector_trigger ON souls;
CREATE TRIGGER souls_search_vector_trigger
    BEFORE INSERT OR UPDATE ON souls
    FOR EACH ROW EXECUTE FUNCTION souls_search_vector_update();

-- Update existing rows
UPDATE souls SET search_vector = 
    setweight(to_tsvector('french', COALESCE(nom, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(prenom, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(email, '')), 'B') ||
    setweight(to_tsvector('french', COALESCE(telephone, '')), 'B') ||
    setweight(to_tsvector('french', COALESCE(profession, '')), 'C') ||
    setweight(to_tsvector('french', COALESCE(adresse, '')), 'C') ||
    setweight(to_tsvector('french', COALESCE(notes_pasteur, '')), 'D');

-- GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_souls_search_vector ON souls USING GIN(search_vector);
-- Trigram indexes for fuzzy/autocomplete search
CREATE INDEX IF NOT EXISTS idx_souls_nom_trgm ON souls USING GIN(nom gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_souls_prenom_trgm ON souls USING GIN(prenom gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_souls_email_trgm ON souls USING GIN(email gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_souls_telephone_trgm ON souls USING GIN(telephone gin_trgm_ops);

-- ============================================================
-- USERS: Add full-text search vector and indexes
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION users_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('french', COALESCE(NEW.first_name, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.last_name, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.email, '')), 'B') ||
        setweight(to_tsvector('french', COALESCE(NEW.phone, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS users_search_vector_trigger ON users;
CREATE TRIGGER users_search_vector_trigger
    BEFORE INSERT OR UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION users_search_vector_update();

UPDATE users SET search_vector = 
    setweight(to_tsvector('french', COALESCE(first_name, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(last_name, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(email, '')), 'B') ||
    setweight(to_tsvector('french', COALESCE(phone, '')), 'B');

CREATE INDEX IF NOT EXISTS idx_users_search_vector ON users USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_users_first_name_trgm ON users USING GIN(first_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_last_name_trgm ON users USING GIN(last_name gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_users_email_trgm ON users USING GIN(email gin_trgm_ops);

-- ============================================================
-- FAMILIES: Add full-text search vector and indexes
-- ============================================================
ALTER TABLE families ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION families_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('french', COALESCE(NEW.nom, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.zone, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS families_search_vector_trigger ON families;
CREATE TRIGGER families_search_vector_trigger
    BEFORE INSERT OR UPDATE ON families
    FOR EACH ROW EXECUTE FUNCTION families_search_vector_update();

UPDATE families SET search_vector = 
    setweight(to_tsvector('french', COALESCE(nom, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(zone, '')), 'B');

CREATE INDEX IF NOT EXISTS idx_families_search_vector ON families USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_families_nom_trgm ON families USING GIN(nom gin_trgm_ops);

-- ============================================================
-- DEPARTMENTS: Add full-text search vector and indexes
-- ============================================================
ALTER TABLE departments ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION departments_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('french', COALESCE(NEW.nom, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.description, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS departments_search_vector_trigger ON departments;
CREATE TRIGGER departments_search_vector_trigger
    BEFORE INSERT OR UPDATE ON departments
    FOR EACH ROW EXECUTE FUNCTION departments_search_vector_update();

UPDATE departments SET search_vector = 
    setweight(to_tsvector('french', COALESCE(nom, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(description, '')), 'B');

CREATE INDEX IF NOT EXISTS idx_departments_search_vector ON departments USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_departments_nom_trgm ON departments USING GIN(nom gin_trgm_ops);

-- ============================================================
-- EVENT: Add full-text search vector and indexes (singular: event, not events)
-- ============================================================
ALTER TABLE event ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION event_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('french', COALESCE(NEW.title, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.description, '')), 'B') ||
        setweight(to_tsvector('french', COALESCE(NEW.lieu, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS event_search_vector_trigger ON event;
CREATE TRIGGER event_search_vector_trigger
    BEFORE INSERT OR UPDATE ON event
    FOR EACH ROW EXECUTE FUNCTION event_search_vector_update();

UPDATE event SET search_vector = 
    setweight(to_tsvector('french', COALESCE(title, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(description, '')), 'B') ||
    setweight(to_tsvector('french', COALESCE(lieu, '')), 'B');

CREATE INDEX IF NOT EXISTS idx_event_search_vector ON event USING GIN(search_vector);
CREATE INDEX IF NOT EXISTS idx_event_title_trgm ON event USING GIN(title gin_trgm_ops);

-- ============================================================
-- MAKER_REPORTS: Add full-text search vector
-- ============================================================
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION maker_reports_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('french', COALESCE(NEW.absence_commentaire, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.difficultes, '')), 'A') ||
        setweight(to_tsvector('french', COALESCE(NEW.notes_complementaires, '')), 'B');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS maker_reports_search_vector_trigger ON maker_reports;
CREATE TRIGGER maker_reports_search_vector_trigger
    BEFORE INSERT OR UPDATE ON maker_reports
    FOR EACH ROW EXECUTE FUNCTION maker_reports_search_vector_update();

UPDATE maker_reports SET search_vector = 
    setweight(to_tsvector('french', COALESCE(absence_commentaire, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(difficultes, '')), 'A') ||
    setweight(to_tsvector('french', COALESCE(notes_complementaires, '')), 'B');

CREATE INDEX IF NOT EXISTS idx_maker_reports_search_vector ON maker_reports USING GIN(search_vector);

-- ============================================================
-- FAMILY_REPORTS: Add full-text search vector
-- ============================================================
ALTER TABLE family_reports ADD COLUMN IF NOT EXISTS search_vector tsvector;

CREATE OR REPLACE FUNCTION family_reports_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('french', COALESCE(NEW.commentaire_synthese, '')), 'A');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS family_reports_search_vector_trigger ON family_reports;
CREATE TRIGGER family_reports_search_vector_trigger
    BEFORE INSERT OR UPDATE ON family_reports
    FOR EACH ROW EXECUTE FUNCTION family_reports_search_vector_update();

UPDATE family_reports SET search_vector = 
    setweight(to_tsvector('french', COALESCE(commentaire_synthese, '')), 'A');

CREATE INDEX IF NOT EXISTS idx_family_reports_search_vector ON family_reports USING GIN(search_vector);

-- ============================================================
-- SEARCH AUDIT TABLE: Track all search queries for analytics
-- ============================================================
CREATE TABLE IF NOT EXISTS search_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    query_text TEXT NOT NULL,
    entity_types TEXT[], -- e.g., {'SOULS', 'USERS', 'FAMILIES', 'DEPARTMENTS', 'EVENTS'}
    results_count INTEGER NOT NULL DEFAULT 0,
    execution_time_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_audit_tenant ON search_audit(tenant_id);
CREATE INDEX IF NOT EXISTS idx_search_audit_user ON search_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_search_audit_created ON search_audit(created_at);

COMMENT ON TABLE search_audit IS 'Audit trail of all search queries for analytics and security';

-- ============================================================
-- EXPORT AUDIT TABLE: Track all exports (who exported what)
-- ============================================================
CREATE TABLE IF NOT EXISTS export_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    export_type VARCHAR(100) NOT NULL, -- e.g., 'SOULS', 'FAMILIES', 'PAYMENTS', 'REPORTS', etc.
    export_format VARCHAR(20) NOT NULL, -- CSV, EXCEL, PDF, JSON, ZIP
    filters_json JSONB, -- applied filters
    columns_exported TEXT[], -- selected columns
    record_count INTEGER NOT NULL DEFAULT 0,
    file_size_bytes BIGINT,
    execution_time_ms INTEGER,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'FAILED', 'PARTIAL')),
    ip VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_export_audit_tenant ON export_audit(tenant_id);
CREATE INDEX IF NOT EXISTS idx_export_audit_user ON export_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_export_audit_type ON export_audit(export_type);
CREATE INDEX IF NOT EXISTS idx_export_audit_created ON export_audit(created_at);

COMMENT ON TABLE export_audit IS 'Audit trail of all data exports (who exported what, when, how many records)';

-- ============================================================
-- SOFT DELETE AUDIT TABLE: Track all soft deletes with business history integration
-- ============================================================
CREATE TABLE IF NOT EXISTS soft_delete_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    deleted_by UUID NOT NULL,
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason TEXT,
    previous_values_json JSONB, -- snapshot before deletion
    business_history_id BIGINT REFERENCES business_history(id), -- link to business_history
    ip VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX IF NOT EXISTS idx_soft_delete_audit_tenant ON soft_delete_audit(tenant_id);
CREATE INDEX IF NOT EXISTS idx_soft_delete_audit_entity ON soft_delete_audit(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_soft_delete_audit_deleted_by ON soft_delete_audit(deleted_by);
CREATE INDEX IF NOT EXISTS idx_soft_delete_audit_created ON soft_delete_audit(deleted_at);

COMMENT ON TABLE soft_delete_audit IS 'Audit trail of all soft deletes with link to business_history';