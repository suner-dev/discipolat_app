-- V141: Santé / Infirmerie — module G3.11
-- =====================================================================
-- patient_records, medical_consultations, prescriptions,
-- pharmacy_items, pharmacy_stock, pharmacy_movements,
-- health_campaigns
-- =====================================================================

-- Patient Records (dossiers médicaux confidentiels)
CREATE TABLE IF NOT EXISTS patient_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    person_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID REFERENCES families(id) ON DELETE SET NULL,
    groupe_sanguin VARCHAR(10),
    allergies TEXT,
    antecedents TEXT,
    medecin_traitant VARCHAR(255),
    medecin_tel VARCHAR(50),
    mesures TEXT,
    notes_sensibles TEXT,
    numero_assurance VARCHAR(100),
    poids_kg DECIMAL(5,2),
    taille_cm DECIMAL(5,1),
    tension_arterielle VARCHAR(50),
    glycemie VARCHAR(50),
    pack_year VARCHAR(50),
    abouchement VARCHAR(50),
    confidentiality_level VARCHAR(20) NOT NULL DEFAULT 'STRICT' CHECK (confidentiality_level IN ('STRICT','INTERNAL','GENERAL')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_patient_record_person ON patient_records(person_id);
CREATE INDEX IF NOT EXISTS idx_patient_record_family ON patient_records(family_id);
CREATE INDEX IF NOT EXISTS idx_patient_record_tenant ON patient_records(tenant_id);

-- Medical Consultations
CREATE TABLE IF NOT EXISTS medical_consultations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID REFERENCES families(id) ON DELETE SET NULL,
    practitioner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    consultation_date DATE NOT NULL,
    type_consultation VARCHAR(50) NOT NULL CHECK (type_consultation IN ('TRIAGE','CONSULTATION','SUIVI')),
    motif TEXT,
    constantes TEXT,
    diagnostic TEXT,
    traitement TEXT,
    resultat TEXT,
    orientation VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_med_consult_patient ON medical_consultations(patient_id);
CREATE INDEX IF NOT EXISTS idx_med_consult_family ON medical_consultations(family_id);
CREATE INDEX IF NOT EXISTS idx_med_consult_practitioner ON medical_consultations(practitioner_id);
CREATE INDEX IF NOT EXISTS idx_med_consult_date ON medical_consultations(consultation_date);
CREATE INDEX IF NOT EXISTS idx_med_consult_tenant ON medical_consultations(tenant_id);

-- Prescriptions
CREATE TABLE IF NOT EXISTS prescriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    consultation_id UUID NOT NULL REFERENCES medical_consultations(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    medicament VARCHAR(255) NOT NULL,
    dosage VARCHAR(255),
    posologie TEXT,
    duree VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED')),
    notes TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prescription_consultation ON prescriptions(consultation_id);
CREATE INDEX IF NOT EXISTS idx_prescription_patient ON prescriptions(patient_id);
CREATE INDEX IF NOT EXISTS idx_prescription_tenant ON prescriptions(tenant_id);

-- Pharmacy Items (catalogue médicaments/materiels)
CREATE TABLE IF NOT EXISTS pharmacy_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    code VARCHAR(100) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    description TEXT,
    categorie VARCHAR(50),
    unite VARCHAR(50),
    fournisseur VARCHAR(255),
    prix_achat DECIMAL(10,2),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_pharm_item_code_tenant ON pharmacy_items(tenant_id, code) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pharm_item_tenant ON pharmacy_items(tenant_id);

-- Pharmacy Stock (stocks par lot)
CREATE TABLE IF NOT EXISTS pharmacy_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    pharmacy_item_id UUID NOT NULL REFERENCES pharmacy_items(id) ON DELETE CASCADE,
    lot_number VARCHAR(100) NOT NULL,
    quantite INTEGER NOT NULL DEFAULT 0 CHECK (quantite >= 0),
    seuil_alerte INTEGER NOT NULL DEFAULT 10 CHECK (seuil_alerte >= 0),
    date_expiration DATE NOT NULL,
    prix_unitaire DECIMAL(10,2),
    status VARCHAR(50) NOT NULL DEFAULT 'EN_STOCK' CHECK (status IN ('EN_STOCK','STOCK_FAIBLE','EXPIRANT','EXPIRÉ','ÉPUISÉ')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pharm_stock_item ON pharmacy_stock(pharmacy_item_id);
CREATE INDEX IF NOT EXISTS idx_pharm_stock_lot ON pharmacy_stock(pharmacy_item_id, lot_number) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pharm_stock_expiration ON pharmacy_stock(date_expiration) WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_pharm_stock_tenant ON pharmacy_stock(tenant_id);

-- Pharmacy Movements (traçabilité complète)
CREATE TABLE IF NOT EXISTS pharmacy_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    pharmacy_stock_id UUID NOT NULL REFERENCES pharmacy_stock(id) ON DELETE CASCADE,
    prescription_id UUID REFERENCES prescriptions(id) ON DELETE SET NULL,
    patient_id UUID REFERENCES users(id) ON DELETE SET NULL,
    movement_type VARCHAR(50) NOT NULL CHECK (movement_type IN ('ENTREE','SORTIE','DISTRIBUTION','DON','PERTE')),
    quantite INTEGER NOT NULL CHECK (quantite > 0),
    motif TEXT,
    responsible_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    preuve_reception TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pharm_mouv_stock ON pharmacy_movements(pharmacy_stock_id);
CREATE INDEX IF NOT EXISTS idx_pharm_mouv_prescription ON pharmacy_movements(prescription_id);
CREATE INDEX IF NOT EXISTS idx_pharm_mouv_patient ON pharmacy_movements(patient_id);
CREATE INDEX IF NOT EXISTS idx_pharm_mouv_tenant ON pharmacy_movements(tenant_id);
CREATE INDEX IF NOT EXISTS idx_pharm_mouv_created ON pharmacy_movements(created_at);

-- Health Campaigns
CREATE TABLE IF NOT EXISTS health_campaigns (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    title VARCHAR(255) NOT NULL,
    description TEXT,
    campaign_type VARCHAR(50) NOT NULL CHECK (campaign_type IN ('VACCINATION','DEPISTAGE','SENSIBILISATION','DON_SANG','ATELIER','AUTRE')),
    start_date DATE NOT NULL,
    end_date DATE,
    lieu VARCHAR(255) NOT NULL,
    responsible_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id UUID REFERENCES families(id) ON DELETE SET NULL,
    objectif TEXT,
    cibles TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_health_camp_type ON health_campaigns(campaign_type);
CREATE INDEX IF NOT EXISTS idx_health_camp_responsible ON health_campaigns(responsible_id);
CREATE INDEX IF NOT EXISTS idx_health_camp_family ON health_campaigns(family_id);
CREATE INDEX IF NOT EXISTS idx_health_camp_dates ON health_campaigns(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_health_camp_status ON health_campaigns(status);
CREATE INDEX IF NOT EXISTS idx_health_camp_tenant ON health_campaigns(tenant_id);

-- Comments
COMMENT ON TABLE patient_records IS 'Dossiers patients médicaux — CONFIDENTIEL (G3.11 Santé/Infirmerie)';
COMMENT ON TABLE medical_consultations IS 'Consultations médicales — triage, consultation, suivi (G3.11)';
COMMENT ON TABLE prescriptions IS 'Prescriptions médicamenteuses liées aux consultations (G3.11)';
COMMENT ON TABLE pharmacy_items IS 'Catalogue des médicaments, vaccins, matériels (G3.11)';
COMMENT ON TABLE pharmacy_stock IS 'Stocks par lot avec dates d''expiration (G3.11)';
COMMENT ON TABLE pharmacy_movements IS 'Traçabilité complète des mouvements de stock (G3.11)';
COMMENT ON TABLE health_campaigns IS 'Campagnes médicales : vaccination, dépistage, sensibilisation, dons de sang (G3.11)';
