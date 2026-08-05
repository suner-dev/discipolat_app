-- ============================================================
-- V24 : Plateforme de formations (cours, modules, quiz, certificats)
-- ============================================================

-- ------------------------------------------------------------
-- Cours : parcours de formation
-- ------------------------------------------------------------
CREATE TABLE courses (
    id            UUID PRIMARY KEY,
    titre         VARCHAR(150) NOT NULL,
    description   TEXT,
    categorie     VARCHAR(40) NOT NULL DEFAULT 'DISCIPOLAT',
    niveau        VARCHAR(20) NOT NULL DEFAULT 'DEBUTANT',   -- DEBUTANT | INTERMEDIAIRE | AVANCE
    duree_minutes INTEGER,
    formateur_id  UUID,
    image_url     TEXT,
    actif         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_course_niveau CHECK (niveau IN ('DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'))
);

-- ------------------------------------------------------------
-- Modules : chapitres d'un cours
-- ------------------------------------------------------------
CREATE TABLE course_modules (
    id            UUID PRIMARY KEY,
    course_id     UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    titre         VARCHAR(150) NOT NULL,
    contenu       TEXT,
    video_url     TEXT,
    ordre         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_modules_course ON course_modules (course_id);

-- ------------------------------------------------------------
-- Questions de quiz (rattachées à un module)
-- ------------------------------------------------------------
CREATE TABLE quiz_questions (
    id            UUID PRIMARY KEY,
    module_id     UUID NOT NULL REFERENCES course_modules (id) ON DELETE CASCADE,
    question      TEXT NOT NULL,
    propositions  TEXT NOT NULL,      -- JSON array de réponses possibles
    reponse_index INTEGER NOT NULL,   -- index de la bonne réponse
    ordre         INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_quiz_module ON quiz_questions (module_id);

-- ------------------------------------------------------------
-- Inscriptions / progression des apprenants
-- ------------------------------------------------------------
CREATE TABLE course_enrollments (
    id            UUID PRIMARY KEY,
    course_id     UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    statut        VARCHAR(20) NOT NULL DEFAULT 'INSCRIT',   -- INSCRIT | EN_COURS | TERMINE
    progression   INTEGER NOT NULL DEFAULT 0,               -- 0-100
    score_quiz    INTEGER,                                  -- meilleur score (0-100)
    date_inscription TIMESTAMP NOT NULL DEFAULT now(),
    date_terminaison TIMESTAMP,
    CONSTRAINT uq_enrollment UNIQUE (course_id, user_id)
);

CREATE INDEX idx_enrollments_user ON course_enrollments (user_id);
CREATE INDEX idx_enrollments_course ON course_enrollments (course_id);

-- ------------------------------------------------------------
-- Modules complétés par un apprenant (progression individuelle)
-- ------------------------------------------------------------
CREATE TABLE module_completions (
    id            UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL REFERENCES course_enrollments (id) ON DELETE CASCADE,
    module_id     UUID NOT NULL REFERENCES course_modules (id) ON DELETE CASCADE,
    completed_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_module_completion UNIQUE (enrollment_id, module_id)
);

CREATE INDEX idx_completions_enrollment ON module_completions (enrollment_id);

-- ------------------------------------------------------------
-- Certificats délivrés
-- ------------------------------------------------------------
CREATE TABLE certificates (
    id            UUID PRIMARY KEY,
    enrollment_id UUID NOT NULL REFERENCES course_enrollments (id) ON DELETE CASCADE,
    numero        VARCHAR(40) NOT NULL UNIQUE,
    user_id       UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    course_id     UUID NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    score_final   INTEGER NOT NULL,
    delivre_le    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_certificates_user ON certificates (user_id);

-- ------------------------------------------------------------
-- Cours de référence (données de base)
-- ------------------------------------------------------------
INSERT INTO courses (id, titre, description, categorie, niveau, duree_minutes) VALUES
  (gen_random_uuid(), 'Fondements du Discipolat', 'Les bases bibliques et pratiques du discipolat : faire des disciples qui en font d''autres.', 'DISCIPOLAT', 'DEBUTANT', 120),
  (gen_random_uuid(), 'Vie de Prière', 'Développer une vie de prière régulière et structurée au quotidien.', 'SPIRITUEL', 'DEBUTANT', 90),
  (gen_random_uuid(), 'Conduire une Visite Pastorale', 'Savoir préparer, conduire et rendre compte d''une visite de suivi.', 'MINISTERE', 'INTERMEDIAIRE', 75),
  (gen_random_uuid(), 'Former des Leaders', 'Identifier, équiper et déléguer pour multiplier les leaders.', 'LEADERSHIP', 'AVANCE', 150);
