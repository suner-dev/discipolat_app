-- ============================================================================
-- V241 — LOT 2 §GLISE-D'ABORD (T2.3) : atterrissage public par église.
-- ----------------------------------------------------------------------------
-- Why : chaque église pourra publier SA page d'accueil blanche (/e/:slug),
--       personnalisée via les réglages déjà présents (TenantSettings). Il
--       manquait simplement deux interrupteurs de consentement, pas de données.
-- Règle : 100 % ADDITIF (A5). Deux colonnes seulement, numérotées à la SUITE
--       de la chaîne (V240 → V241), jamais insérées au milieu. Aucune colonne
--       existante touchée, aucun index supprimé, aucune contrainte modifiée.
-- Sécurité (R2, RGPD art. 9) : défaut {@code false} = dark launch naturel —
--       aucune page publique n'apparaît pour une église existante tant
--       qu'elle ne l'a pas explicitement demandé. Double consentement exigé à
--       la lecture : {@code isListed} (annuaire, déjà là) ET {@code landing_enabled}.
-- Parité : H2/tests génère le schéma depuis l'entité (Flyway désactivé) ; ce
--       fichier cible PostgreSQL, validé par le gate PG (`ddl-auto: validate`).
-- Rollback (A5, réversible sans perte) : DROP des deux colonnes ajoutées.
-- ============================================================================

ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS landing_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS landing_sections JSONB;

COMMENT ON COLUMN tenant_settings.landing_enabled IS 'Opt-in page publique /e/:slug (V241). Défaut false = dark launch ; second verrou après isListed.';
COMMENT ON COLUMN tenant_settings.landing_sections IS 'Composition de la landing (JSON, nullable). Absent = sections par défaut ; jamais de donnée membre.';
