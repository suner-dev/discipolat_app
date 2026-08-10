-- V32__transfer_workflow.sql
-- ============================================================
-- WORKFLOW INTELLIGENT ET CONFIGURABLE DES TRANSFERTS
-- Moteur métier entièrement piloté par la base :
--   * transfer_workflow_configs  : paramétrage pasteur par type de transfert
--     (rôles initiateurs, mode de validation, nombre requis, délais,
--      notifications automatiques, modèles de messages, règles d'exécution)
--   * transfer_workflow_steps    : étapes du circuit de validation (ordonnées)
--   * transfer_requests          : demandes de transfert (cycle de vie complet)
--   * transfer_decisions         : décisions motivées des validateurs
--   * transfer_history           : historique immuable de chaque demande
--   * transfer_attachments       : pièces jointes (lien vers la table files)
-- Le circuit de validation évolue SANS modification de code : il est
-- entièrement reconfigurable depuis l'administration (pasteur/admin).
-- ============================================================

-- ============================================================
-- 1. TRANSFER_WORKFLOW_CONFIGS : paramétrage par type de transfert
-- ============================================================
CREATE TABLE IF NOT EXISTS transfer_workflow_configs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_type VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    description TEXT,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    roles_initiateurs JSONB NOT NULL DEFAULT '["PASTEUR"]'::jsonb,
    mode_validation VARCHAR(30) NOT NULL DEFAULT 'SEQUENTIEL'
        CHECK (mode_validation IN ('SEQUENTIEL', 'PARALLELE', 'N_VALIDATIONS_REQUISES')),
    nombre_validations_requises INTEGER NOT NULL DEFAULT 1,
    delai_traitement_heures INTEGER NOT NULL DEFAULT 72,
    notifications_auto BOOLEAN NOT NULL DEFAULT TRUE,
    modele_message_demande TEXT,
    modele_message_validation TEXT,
    modele_message_refus TEXT,
    modele_message_execution TEXT,
    regles_execution JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_wf_configs_type ON transfer_workflow_configs(transfer_type);
CREATE INDEX IF NOT EXISTS idx_transfer_wf_configs_actif ON transfer_workflow_configs(actif);

-- ============================================================
-- 2. TRANSFER_WORKFLOW_STEPS : étapes du circuit de validation
-- ============================================================
CREATE TABLE IF NOT EXISTS transfer_workflow_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    workflow_config_id UUID NOT NULL REFERENCES transfer_workflow_configs(id) ON DELETE CASCADE,
    etape_ordre INTEGER NOT NULL DEFAULT 1,
    roles_validateurs JSONB NOT NULL DEFAULT '["PASTEUR"]'::jsonb,
    label VARCHAR(120) NOT NULL,
    description TEXT,
    requis BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_wf_steps_config ON transfer_workflow_steps(workflow_config_id);

-- ============================================================
-- 3. TRANSFER_REQUESTS : demandes de transfert
-- ============================================================
CREATE TABLE IF NOT EXISTS transfer_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type_transfert VARCHAR(50) NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'BROUILLON'
        CHECK (statut IN ('BROUILLON', 'SOUMIS', 'EN_ATTENTE_VALIDATION', 'VALIDATION_PARTIELLE',
                          'VALIDE', 'REFUSE', 'ANNULE', 'EXECUTE', 'ARCHIVE')),
    personne_id UUID NOT NULL,
    personne_type VARCHAR(20) NOT NULL DEFAULT 'SOUL'
        CHECK (personne_type IN ('SOUL', 'USER')),
    ancienne_affectation JSONB,
    nouvelle_affectation JSONB NOT NULL,
    demandeur_id UUID NOT NULL,
    justification TEXT NOT NULL,
    priorite VARCHAR(20) NOT NULL DEFAULT 'MOYENNE'
        CHECK (priorite IN ('BASSE', 'MOYENNE', 'HAUTE', 'URGENTE')),
    commentaires TEXT,
    date_soumission TIMESTAMP,
    date_execution TIMESTAMP,
    delai_limite TIMESTAMP,
    etape_courante INTEGER NOT NULL DEFAULT 0,
    approbations_obtenues INTEGER NOT NULL DEFAULT 0,
    workflow_config_id UUID REFERENCES transfer_workflow_configs(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_requests_statut ON transfer_requests(statut);
CREATE INDEX IF NOT EXISTS idx_transfer_requests_demandeur ON transfer_requests(demandeur_id);
CREATE INDEX IF NOT EXISTS idx_transfer_requests_personne ON transfer_requests(personne_id);
CREATE INDEX IF NOT EXISTS idx_transfer_requests_type ON transfer_requests(type_transfert);

-- ============================================================
-- 4. TRANSFER_DECISIONS : décisions des validateurs
-- ============================================================
CREATE TABLE IF NOT EXISTS transfer_decisions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_request_id UUID NOT NULL REFERENCES transfer_requests(id) ON DELETE CASCADE,
    validateur_id UUID NOT NULL,
    role_validateur VARCHAR(50),
    decision VARCHAR(30) NOT NULL
        CHECK (decision IN ('APPROBATION', 'REFUS', 'DEMANDE_INFORMATIONS', 'RENVOI_CORRECTION')),
    motivation TEXT,
    etape_ordre INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_decisions_request ON transfer_decisions(transfer_request_id);

-- ============================================================
-- 5. TRANSFER_HISTORY : historique immuable (aucune donnée perdue)
-- ============================================================
CREATE TABLE IF NOT EXISTS transfer_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_request_id UUID NOT NULL REFERENCES transfer_requests(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    ancien_statut VARCHAR(30),
    nouveau_statut VARCHAR(30),
    utilisateur_id UUID,
    role_actif VARCHAR(50),
    commentaire TEXT,
    ancienne_valeur JSONB,
    nouvelle_valeur JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_history_request ON transfer_history(transfer_request_id);

-- ============================================================
-- 6. TRANSFER_ATTACHMENTS : pièces jointes (module fichiers existant)
-- ============================================================
CREATE TABLE IF NOT EXISTS transfer_attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_request_id UUID NOT NULL REFERENCES transfer_requests(id) ON DELETE CASCADE,
    file_id UUID NOT NULL REFERENCES files(id),
    uploaded_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_transfer_attachments_request ON transfer_attachments(transfer_request_id);

-- ============================================================
-- 7. SEED : circuits de validation par défaut (modifiables par le pasteur)
-- Les identifiants sont stables pour permettre la reproductibilité.
-- ============================================================

-- 7.1 Départements : transfert / ajout / retrait de membre → responsable → pasteur
INSERT INTO transfer_workflow_configs (id, transfer_type, label, description, roles_initiateurs,
                                       mode_validation, nombre_validations_requises, delai_traitement_heures,
                                       notifications_auto, regles_execution)
VALUES
    ('11000000-0000-0000-0000-000000000001', 'MEMBRE_DEPARTEMENT_TRANSFERT',
     'Transfert de membre entre départements',
     'Déplacement d''un membre d''un département vers un autre.',
     '["PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","MEMBRE"]'::jsonb,
     'SEQUENTIEL', 2, 72, TRUE,
     '{"notifierMembre":true,"notifierAncienDepartement":true,"notifierNouveauDepartement":true}'::jsonb),
    ('11000000-0000-0000-0000-000000000002', 'MEMBRE_DEPARTEMENT_AJOUT',
     'Ajout de membre dans un département',
     'Affectation d''un membre à un nouveau département.',
     '["PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","MEMBRE"]'::jsonb,
     'SEQUENTIEL', 2, 48, TRUE,
     '{"notifierMembre":true,"notifierNouveauDepartement":true}'::jsonb),
    ('11000000-0000-0000-0000-000000000003', 'MEMBRE_DEPARTEMENT_RETRAIT',
     'Retrait de membre d''un département',
     'Retrait d''un membre d''un département (sans le retirer de l''église).',
     '["PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","MEMBRE"]'::jsonb,
     'SEQUENTIEL', 2, 48, TRUE,
     '{"notifierMembre":true,"notifierDepartement":true}'::jsonb),
    -- 7.2 Familles : disciple / faiseur / chef → chef → pasteur
    ('11000000-0000-0000-0000-000000000004', 'DISCIPLE_FAMILLE_TRANSFERT',
     'Transfert de disciple entre familles',
     'Déplacement d''un disciple d''une famille vers une autre.',
     '["PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb,
     'SEQUENTIEL', 2, 72, TRUE,
     '{"notifierDisciple":true,"notifierAncienFaiseur":true,"notifierNouveauFaiseur":true}'::jsonb),
    ('11000000-0000-0000-0000-000000000005', 'FAISEUR_FAMILLE_TRANSFERT',
     'Transfert de faiseur entre familles',
     'Déplacement d''un faiseur de disciple vers une autre famille (avec ou sans ses disciples).',
     '["PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE"]'::jsonb,
     'SEQUENTIEL', 2, 72, TRUE,
     '{"transfererAmes":false,"notifierFaiseur":true,"notifierFamilles":true}'::jsonb),
    ('11000000-0000-0000-0000-000000000006', 'CHEF_FAMILLE_TRANSFERT',
     'Transfert de chef de famille',
     'Changement de chef pour une famille de disciples.',
     '["PASTEUR"]'::jsonb,
     'SEQUENTIEL', 1, 48, TRUE,
     '{"historiserAncienChef":true,"notifierFamille":true}'::jsonb),
    -- 7.3 Affectations
    ('11000000-0000-0000-0000-000000000007', 'FAISEUR_DISCIPLE_CHANGEMENT',
     'Changement du faiseur d''un disciple',
     'Réaffectation du suivi d''un disciple à un autre faiseur.',
     '["PASTEUR","RESPONSABLE","CHEF_DE_FAMILLE","FAISEUR"]'::jsonb,
     'SEQUENTIEL', 2, 48, TRUE,
     '{"notifierAncienFaiseur":true,"notifierNouveauFaiseur":true,"notifierDisciple":true}'::jsonb),
    ('11000000-0000-0000-0000-000000000008', 'RESPONSABLE_DEPARTEMENT_CHANGEMENT',
     'Changement du responsable d''un département',
     'Désignation d''un nouveau responsable principal de département.',
     '["PASTEUR"]'::jsonb,
     'SEQUENTIEL', 1, 48, TRUE,
     '{"notifierResponsable":true,"notifierDepartement":true}'::jsonb),
    ('11000000-0000-0000-0000-000000000009', 'CHEF_ADJOINT_CHANGEMENT',
     'Changement du chef adjoint d''une famille',
     'Désignation d''un nouveau chef adjoint pour une famille.',
     '["PASTEUR","CHEF_DE_FAMILLE"]'::jsonb,
     'SEQUENTIEL', 2, 48, TRUE,
     '{"notifierFamille":true}'::jsonb);

-- Étapes du circuit de validation (ordre = ordre de validation séquentielle)
INSERT INTO transfer_workflow_steps (workflow_config_id, etape_ordre, roles_validateurs, label, description, requis) VALUES
    ('11000000-0000-0000-0000-000000000001', 1, '["RESPONSABLE"]'::jsonb, 'Validation du responsable', 'Le responsable du département concerné valide la demande.', TRUE),
    ('11000000-0000-0000-0000-000000000001', 2, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000002', 1, '["RESPONSABLE"]'::jsonb, 'Validation du responsable', 'Le responsable du département concerné valide la demande.', TRUE),
    ('11000000-0000-0000-0000-000000000002', 2, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000003', 1, '["RESPONSABLE"]'::jsonb, 'Validation du responsable', 'Le responsable du département concerné valide la demande.', TRUE),
    ('11000000-0000-0000-0000-000000000003', 2, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000004', 1, '["CHEF_DE_FAMILLE"]'::jsonb, 'Validation du chef de famille', 'Le chef de famille concerné valide la demande.', TRUE),
    ('11000000-0000-0000-0000-000000000004', 2, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000005', 1, '["CHEF_DE_FAMILLE"]'::jsonb, 'Validation du chef de famille', 'Le chef de famille concerné valide la demande.', TRUE),
    ('11000000-0000-0000-0000-000000000005', 2, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000006', 1, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000007', 1, '["CHEF_DE_FAMILLE"]'::jsonb, 'Validation du chef de famille', 'Le chef de famille concerné valide la demande.', TRUE),
    ('11000000-0000-0000-0000-000000000007', 2, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000008', 1, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE),
    ('11000000-0000-0000-0000-000000000009', 1, '["CHEF_DE_FAMILLE"]'::jsonb, 'Validation du chef de famille', 'Le chef de famille concerné valide la demande.', TRUE),
    ('11000000-0000-0000-0000-000000000009', 2, '["PASTEUR"]'::jsonb, 'Validation du pasteur', 'Validation finale par le pasteur.', TRUE);

COMMENT ON TABLE transfer_workflow_configs IS 'Paramétrage du workflow de transfert par type (pasteur/admin). Circuit entièrement configurable sans code.';
COMMENT ON TABLE transfer_workflow_steps IS 'Étapes du circuit de validation (rôles validateurs, ordre, caractère requis).';
COMMENT ON TABLE transfer_requests IS 'Demandes de transfert avec cycle de vie complet (BROUILLON → SOUMIS → EN_ATTENTE_VALIDATION → VALIDATION_PARTIELLE → VALIDE → EXECUTE / REFUSE / ANNULE / ARCHIVE).';
COMMENT ON TABLE transfer_decisions IS 'Décisions motivées des validateurs (approbation, refus, demande d''informations, renvoi pour correction).';
COMMENT ON TABLE transfer_history IS 'Historique immuable de chaque demande de transfert : aucune donnée perdue.';
COMMENT ON TABLE transfer_attachments IS 'Pièces jointes d''une demande de transfert (lien vers le module fichiers).';
