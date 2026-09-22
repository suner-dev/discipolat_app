-- ============================================================
-- V64 : versionnage des configurations plateforme (revisions).
-- Journal append-only (jamais modifie apres ecriture) des changements
-- apportes aux modules / menus / parametres de la plateforme, afin
-- de pouvoir auditer « qui a change quoi et quand » et, le cas echeant,
-- revenir en arriere. Adosse au module platform (mission « Plateforme
-- modulaire → versionnage »).
-- ============================================================
CREATE TABLE config_revisions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50)  NOT NULL,             -- PLATFORM_MODULE / PLATFORM_MENU / CHURCH_SETTINGS / ...
    entity_key  VARCHAR(100),                      -- cle du module ou du menu (si applicable)
    action      VARCHAR(60)  NOT NULL,             -- MODULE_ENABLED, MENU_UPDATED, ...
    payload     JSONB,                             -- etat avant/apres (applique a la modification)
    user_id     UUID REFERENCES users(id),         -- auteur (nullable pour les operations systeme)
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_config_revisions_type_date ON config_revisions (entity_type, created_at DESC);
CREATE INDEX idx_config_revisions_user ON config_revisions (user_id);
