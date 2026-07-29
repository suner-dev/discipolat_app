-- V1__initial_schema.sql
-- Initial database schema for Discipolat application

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL CHECK (role IN ('PASTEUR', 'RESPONSABLE', 'FAISEUR')),
    est_chef_de_famille BOOLEAN NOT NULL DEFAULT FALSE,
    famille_geree_id UUID,
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (statut IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_famille_geree ON users(famille_geree_id);
CREATE INDEX idx_users_statut ON users(statut);
CREATE INDEX idx_users_deleted ON users(deleted);

-- ============================================================
-- DEPARTMENTS
-- ============================================================
CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    responsable_id UUID NOT NULL REFERENCES users(id),
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (statut IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_departments_responsable ON departments(responsable_id);
CREATE INDEX idx_departments_statut ON departments(statut);
CREATE INDEX idx_departments_deleted ON departments(deleted);

-- ============================================================
-- FAMILIES
-- ============================================================
CREATE TABLE families (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nom VARCHAR(255) NOT NULL,
    departement_id UUID NOT NULL REFERENCES departments(id),
    chef_famille_id UUID NOT NULL REFERENCES users(id),
    date_creation DATE NOT NULL DEFAULT CURRENT_DATE,
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (statut IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_families_departement ON families(departement_id);
CREATE INDEX idx_families_chef ON families(chef_famille_id);
CREATE INDEX idx_families_statut ON families(statut);
CREATE INDEX idx_families_deleted ON families(deleted);

-- ============================================================
-- SOULS (AME/DISCIPLE)
-- ============================================================
CREATE TABLE souls (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nom VARCHAR(255) NOT NULL,
    prenom VARCHAR(100),
    email VARCHAR(255),
    telephone VARCHAR(50),
    adresse TEXT,
    date_naissance DATE,
    profession VARCHAR(255),
    type_disciple VARCHAR(50) NOT NULL CHECK (type_disciple IN ('NOUVEL_ARRIVANT', 'NOUVEAU_CONVERTI')),
    date_integration DATE NOT NULL DEFAULT CURRENT_DATE,
    date_conversion DATE,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_INTEGRATION' CHECK (statut IN ('NOUVEAU_CONVERTI', 'NOUVEL_ARRIVANT', 'EN_INTEGRATION', 'ACTIF', 'EN_VEILLE', 'DECROCHE')),
    faiseur_id UUID NOT NULL REFERENCES users(id),
    famille_id UUID REFERENCES families(id),
    notes_pasteur TEXT,
    date_dernier_contact TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_souls_faiseur ON souls(faiseur_id);
CREATE INDEX idx_souls_famille ON souls(famille_id);
CREATE INDEX idx_souls_type ON souls(type_disciple);
CREATE INDEX idx_souls_statut ON souls(statut);
CREATE INDEX idx_souls_deleted ON souls(deleted);

-- ============================================================
-- MAKER REPORTS (Rapport hebdomadaire du faiseur par disciple)
-- ============================================================
CREATE TABLE maker_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    faiseur_id UUID NOT NULL REFERENCES users(id),
    ame_id UUID NOT NULL REFERENCES souls(id),
    semaine DATE NOT NULL,
    presences_par_culte JSONB,
    absence_raison VARCHAR(50) CHECK (absence_raison IN ('MALADIE', 'VOYAGE', 'INDISPONIBILITE', 'INJOIGNABLE', 'NON_RENSEIGNE', 'AUTRE')),
    absence_commentaire TEXT,
    difficultes_categorie VARCHAR(50),
    difficultes TEXT,
    nb_sorties INTEGER DEFAULT 0,
    motif_sortie VARCHAR(50) CHECK (motif_sortie IN ('INTEGRE_AUTONOME', 'TRANSFERT', 'ABANDON', 'INJOIGNABLE_DURABLE', 'DECES', 'AUTRE')),
    nb_maintenus INTEGER DEFAULT 0,
    notes_complementaires TEXT,
    soumis BOOLEAN NOT NULL DEFAULT FALSE,
    date_soumission TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_maker_reports_faiseur ON maker_reports(faiseur_id);
CREATE INDEX idx_maker_reports_ame ON maker_reports(ame_id);
CREATE INDEX idx_maker_reports_semaine ON maker_reports(semaine);
CREATE INDEX idx_maker_reports_soumis ON maker_reports(soumis);
CREATE INDEX idx_maker_reports_deleted ON maker_reports(deleted);

-- ============================================================
-- FAMILY REPORTS (Rapport hebdomadaire consolidé de famille)
-- ============================================================
CREATE TABLE family_reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    famille_id UUID NOT NULL REFERENCES families(id),
    chef_famille_id UUID NOT NULL REFERENCES users(id),
    semaine DATE NOT NULL,
    stats_agregees JSONB,
    presence_moyenne DECIMAL(5,2),
    total_presents INTEGER DEFAULT 0,
    total_absents INTEGER DEFAULT 0,
    total_sorties INTEGER DEFAULT 0,
    repartition_sorties JSONB,
    total_maintenus INTEGER DEFAULT 0,
    nb_suivis_paralleles INTEGER DEFAULT 0,
    suivis_paralleles_details JSONB,
    faiseurs_sans_rapport JSONB,
    commentaire_synthese TEXT,
    statut_validation VARCHAR(50) NOT NULL DEFAULT 'BROUILLON' CHECK (statut_validation IN ('BROUILLON', 'SOUMIS', 'VU_PAR_RESPONSABLE', 'VU_PAR_PASTEUR')),
    date_soumission TIMESTAMP,
    date_validation_responsable TIMESTAMP,
    date_validation_pasteur TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_family_reports_famille ON family_reports(famille_id);
CREATE INDEX idx_family_reports_semaine ON family_reports(semaine);
CREATE INDEX idx_family_reports_validation ON family_reports(statut_validation);
CREATE INDEX idx_family_reports_deleted ON family_reports(deleted);

-- ============================================================
-- PARALLEL FOLLOWUPS (Suivis parallèles)
-- ============================================================
CREATE TABLE parallel_followups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ame_id UUID NOT NULL REFERENCES souls(id),
    initiateur_id UUID NOT NULL REFERENCES users(id),
    famille_id UUID REFERENCES families(id),
    raison VARCHAR(50) NOT NULL CHECK (raison IN ('TRANSFERT_EN_COURS', 'RENFORT', 'VISITE', 'REPRISE_CONTACT', 'AUTRE')),
    raison_detail TEXT,
    date_debut DATE NOT NULL DEFAULT CURRENT_DATE,
    date_fin DATE,
    statut VARCHAR(50) NOT NULL DEFAULT 'EN_COURS' CHECK (statut IN ('EN_COURS', 'CLOTURE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_parallel_followups_ame ON parallel_followups(ame_id);
CREATE INDEX idx_parallel_followups_initiateur ON parallel_followups(initiateur_id);
CREATE INDEX idx_parallel_followups_statut ON parallel_followups(statut);
CREATE INDEX idx_parallel_followups_deleted ON parallel_followups(deleted);

-- ============================================================
-- ALERTS
-- ============================================================
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ame_id UUID NOT NULL REFERENCES souls(id),
    faiseur_id UUID NOT NULL REFERENCES users(id),
    famille_id UUID REFERENCES families(id),
    type_alerte VARCHAR(50) NOT NULL DEFAULT 'ABSENCE_48H' CHECK (type_alerte IN ('ABSENCE_48H', 'RAPPORT_NON_SOUMIS', 'RAPPORT_FAMILLE_NON_SOUMIS')),
    message TEXT,
    date_declenchement TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (statut IN ('ACTIVE', 'TRAITEE', 'RESOLUE')),
    date_resolution TIMESTAMP,
    resolu_par UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_ame ON alerts(ame_id);
CREATE INDEX idx_alerts_faiseur ON alerts(faiseur_id);
CREATE INDEX idx_alerts_statut ON alerts(statut);
CREATE INDEX idx_alerts_type ON alerts(type_alerte);

-- ============================================================
-- NOTIFICATIONS
-- ============================================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    destinataire_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL CHECK (type IN ('RAPPORT_NON_SOUMIS', 'ABSENCE_48H', 'RAPPORT_FAMILLE_NON_SOUMIS', 'ALERTE_ABSENCE', 'INFORMATION')),
    canal VARCHAR(50) NOT NULL CHECK (canal IN ('PUSH', 'EMAIL', 'IN_APP')),
    titre VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    date_lecture TIMESTAMP,
    entite_reference_id UUID,
    entite_reference_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_destinataire ON notifications(destinataire_id);
CREATE INDEX idx_notifications_lu ON notifications(lu);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created ON notifications(created_at);

-- ============================================================
-- AUDIT LOGS
-- ============================================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    utilisateur_id UUID REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    entite_type VARCHAR(100) NOT NULL,
    entite_id UUID,
    ancien_valeur JSONB,
    nouvelle_valeur JSONB,
    adresse_ip VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_utilisateur ON audit_logs(utilisateur_id);
CREATE INDEX idx_audit_logs_entite ON audit_logs(entite_type, entite_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- ============================================================
-- SOUL HISTORY (Timeline des interactions avec une âme)
-- ============================================================
CREATE TABLE soul_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ame_id UUID NOT NULL REFERENCES souls(id),
    type_evenement VARCHAR(50) NOT NULL,
    description TEXT,
    ancien_statut VARCHAR(50),
    nouveau_statut VARCHAR(50),
    ancien_faiseur_id UUID,
    nouveau_faiseur_id UUID,
    utilisateur_id UUID REFERENCES users(id),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_soul_history_ame ON soul_history(ame_id);
CREATE INDEX idx_soul_history_created ON soul_history(created_at);

-- ============================================================
-- CULTE CONFIGURATION (Cultes configurables par l'église)
-- ============================================================
CREATE TABLE culte_config (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nom VARCHAR(100) NOT NULL,
    jour_semaine VARCHAR(20),
    ordre INTEGER NOT NULL DEFAULT 0,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO culte_config (nom, jour_semaine, ordre) VALUES
    ('Dimanche Matin', 'DIMANCHE', 1),
    ('Mercredi Soir', 'MERCREDI', 2),
    ('Vendredi Soir', 'VENDREDI', 3),
    ('Veillée', 'SAMEDI', 4);

-- ============================================================
-- DASHBOARD METRICS CACHE (Cache des KPI pré-calculés)
-- ============================================================
CREATE TABLE dashboard_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type_metrique VARCHAR(100) NOT NULL,
    entite_id UUID,
    entite_type VARCHAR(50),
    periode DATE NOT NULL,
    valeur JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dashboard_metrics_type ON dashboard_metrics(type_metrique);
CREATE INDEX idx_dashboard_metrics_periode ON dashboard_metrics(periode);
CREATE INDEX idx_dashboard_metrics_entite ON dashboard_metrics(entite_type, entite_id);
