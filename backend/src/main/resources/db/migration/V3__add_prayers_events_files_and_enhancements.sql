-- V3__add_prayers_events_files_and_enhancements.sql
-- New modules: Prayers, Events, Files, Soul Notes
-- Entity enhancements per cahier des charges v2.0

-- ============================================================
-- ENHANCE USERS - Add date_naissance, photo_url, situation_familiale
-- ============================================================
ALTER TABLE users ADD COLUMN IF NOT EXISTS date_naissance DATE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);
ALTER TABLE USers ADD COLUMN IF NOT EXISTS situation_familiale VARCHAR(50) CHECK (situation_familiale IN ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'AUTRE'));
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMP;

-- ============================================================
-- ENHANCE SOULS - Add situation_familiale, etat_spirituel, niveau_croissance
-- ============================================================
ALTER TABLE souls ADD COLUMN IF NOT EXISTS situation_familiale VARCHAR(50) CHECK (situation_familiale IN ('CELIBATAIRE', 'MARIE', 'DIVORCE', 'VEUF', 'AUTRE'));
ALTER TABLE souls ADD COLUMN IF NOT EXISTS etat_spirituel VARCHAR(50) NOT NULL DEFAULT 'NOUVEAU_CONVERTI' CHECK (etat_spirituel IN ('NOUVEAU_CONVERTI', 'EN_CROISSANCE', 'MATURE', 'EN_DIFFICULTE'));
ALTER TABLE souls ADD COLUMN IF NOT EXISTS niveau_croissance INTEGER NOT NULL DEFAULT 1 CHECK (niveau_croissance >= 1 AND niveau_croissance <= 5);

-- ============================================================
-- ENHANCE MAKER REPORTS - Add evangelism and vie du faiseur fields
-- ============================================================
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS nb_invites_culte INTEGER DEFAULT 0;
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS vie_faiseur_challenges TEXT;
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS vie_faiseur_demandes_aide TEXT;
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS vie_faiseur_suggestions TEXT;
ALTER TABLE maker_reports ADD COLUMN IF NOT EXISTS date_derniere_modification TIMESTAMP;

-- ============================================================
-- PRAYERS (Sujets de prière) - Module G, US-44 to US-49
-- ============================================================
CREATE TABLE IF NOT EXISTS prayers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    auteur_id UUID NOT NULL REFERENCES users(id),
    famille_id UUID REFERENCES families(id),
    ame_id UUID REFERENCES souls(id),
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    categorie VARCHAR(50) NOT NULL CHECK (categorie IN ('SANTE', 'FAMILLE', 'TRAVAIL', 'SPIRITUEL', 'AUTRE')),
    priorite VARCHAR(20) NOT NULL DEFAULT 'MOYENNE' CHECK (priorite IN ('BASSE', 'MOYENNE', 'HAUTE')),
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_COURS' CHECK (statut IN ('EN_COURS', 'EXAUCE')),
    temoignage TEXT,
    date_exaucee TIMESTAMP,
    visibilite VARCHAR(30) NOT NULL DEFAULT 'PARTAGEE' CHECK (visibilite IN ('PRIVEE', 'PARTAGEE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_prayers_auteur ON prayers(auteur_id);
CREATE INDEX IF NOT EXISTS idx_prayers_famille ON prayers(famille_id);
CREATE INDEX IF NOT EXISTS idx_prayers_categorie ON prayers(categorie);
CREATE INDEX IF NOT EXISTS idx_prayers_statut ON prayers(statut);
CREATE INDEX IF NOT EXISTS idx_prayers_visibilite ON prayers(visibilite);
CREATE INDEX IF NOT EXISTS idx_prayers_deleted ON prayers(deleted);

-- ============================================================
-- EVENTS (Événements) - Module H, US-50 to US-55
-- ============================================================
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisateur_id UUID NOT NULL REFERENCES users(id),
    famille_id UUID REFERENCES families(id),
    type_evenement VARCHAR(50) NOT NULL CHECK (type_evenement IN ('SORTIE', 'RETRAITE', 'EVANGELISATION', 'REUNION', 'VISITE', 'CONFERENCE', 'FORMATION', 'ANNIVERSAIRE', 'AUTRE')),
    titre VARCHAR(255) NOT NULL,
    description TEXT,
    lieu VARCHAR(255),
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP,
    limite_places INTEGER,
    nb_inscrits INTEGER NOT NULL DEFAULT 0,
    statut VARCHAR(30) NOT NULL DEFAULT 'PLANIFIE' CHECK (statut IN ('PLANIFIE', 'EN_COURS', 'TERMINE', 'ANNULE')),
    compte_rendu TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX IF NOT EXISTS idx_events_organisateur ON events(organisateur_id);
CREATE INDEX IF NOT EXISTS idx_events_famille ON events(famille_id);
CREATE INDEX IF NOT EXISTS idx_events_type ON events(type_evenement);
CREATE INDEX IF NOT EXISTS idx_events_date ON events(date_debut);
CREATE INDEX IF NOT EXISTS idx_events_statut ON events(statut);
CREATE INDEX IF NOT EXISTS idx_events_deleted ON events(deleted);

-- ============================================================
-- EVENT REGISTRATIONS (Inscriptions aux événements)
-- ============================================================
CREATE TABLE IF NOT EXISTS event_registrations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL REFERENCES events(id),
    utilisateur_id UUID NOT NULL REFERENCES users(id),
    statut_inscription VARCHAR(30) NOT NULL DEFAULT 'INSCRIT' CHECK (statut_inscription IN ('INSCRIT', 'EN_ATTENTE', 'PRESENT', 'ABSENT')),
    date_inscription TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_emargement TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(event_id, utilisateur_id)
);

CREATE INDEX IF NOT EXISTS idx_event_reg_event ON event_registrations(event_id);
CREATE INDEX IF NOT EXISTS idx_event_reg_user ON event_registrations(utilisateur_id);

-- ============================================================
-- FILES (Gestion documentaire) - Section 21
-- ============================================================
CREATE TABLE IF NOT EXISTS files (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    nom VARCHAR(255) NOT NULL,
    type_fichier VARCHAR(100) NOT NULL,
    taille BIGINT NOT NULL,
    chemin VARCHAR(500) NOT NULL,
    description TEXT,
    famille_id UUID REFERENCES families(id),
    evenement_id UUID REFERENCES events(id),
    auteur_id UUID NOT NULL REFERENCES users(id),
    categorie VARCHAR(50) NOT NULL DEFAULT 'DOCUMENT' CHECK (categorie IN ('DOCUMENT', 'PHOTO', 'COMpte_RENDU', 'FORMATION', 'RAPPORT', 'AUTRE')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_files_famille ON files(famille_id);
CREATE INDEX IF NOT EXISTS idx_files_evenement ON files(evenement_id);
CREATE INDEX IF NOT EXISTS idx_files_auteur ON files(auteur_id);
CREATE INDEX IF NOT EXISTS idx_files_categorie ON files(categorie);
CREATE INDEX IF NOT EXISTS idx_files_deleted ON files(deleted);

-- ============================================================
-- SOUL NOTES (Notes libres sur âme) - US-25
-- ============================================================
CREATE TABLE IF NOT EXISTS soul_notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ame_id UUID NOT NULL REFERENCES souls(id),
    auteur_id UUID NOT NULL REFERENCES users(id),
    contenu TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_soul_notes_ame ON soul_notes(ame_id);
CREATE INDEX IF NOT EXISTS idx_soul_notes_auteur ON soul_notes(auteur_id);
CREATE INDEX IF NOT EXISTS idx_soul_notes_deleted ON soul_notes(deleted);

-- ============================================================
-- SOUL RETRACTION REQUESTS (Demandes de retrait d'âme) - US-04/32
-- ============================================================
CREATE TABLE IF NOT EXISTS soul_retraction_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ame_id UUID NOT NULL REFERENCES souls(id),
    demandeur_id UUID NOT NULL REFERENCES users(id),
    justification TEXT NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (statut IN ('EN_ATTENTE', 'APPROUVEE', 'REJETEE')),
    traite_par UUID REFERENCES users(id),
    date_traitement TIMESTAMP,
    commentaire_reponse TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_retraction_ame ON soul_retraction_requests(ame_id);
CREATE INDEX IF NOT EXISTS idx_retraction_demandeur ON soul_retraction_requests(demandeur_id);
CREATE INDEX IF NOT EXISTS idx_retraction_statut ON soul_retraction_requests(statut);
