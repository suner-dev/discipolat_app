-- V246__saas_pricing_settings.sql
-- ============================================================
-- TODO_TARIFS_PAIEMENTS_INTERNATIONAL — T1.2
-- Réglages plateforme de la tarification (table SINGLETON, 1 seule ligne).
--
-- Toute la paramétrisation du chapitre « Decisions produit arbitrees » vit ici :
-- le super-admin decide de TOUT (devises, modes d'affichage, regions, canaux de
-- paiement, synchronisation Stripe, essai/remise, section landing).
-- L'utilisateur final ne peut choisir que ce que le super-admin a autorise.
--
-- Volontairement hors du filtre multi-tenant : c'est une table de la PLATEFORME,
-- comme saas_plans.
-- ============================================================

CREATE TABLE IF NOT EXISTS saas_pricing_settings (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),

    -- ── Devises ──────────────────────────────────────────────────────────────
    base_currency                 CHAR(3) NOT NULL DEFAULT 'XAF',
    default_display_currency      CHAR(3) NOT NULL DEFAULT 'XAF',
    enabled_currencies            CHAR(3)[] NOT NULL DEFAULT '{XAF,XOF,EUR,USD,CAD,GBP,CHF}',
    -- Repli sur base_currency quand la devise demandee n'a pas de prix.
    -- false par defaut : mieux vaut « tarif non communique » qu'un prix invente.
    show_fallback_currency        BOOLEAN NOT NULL DEFAULT FALSE,
    allow_user_currency_choice    BOOLEAN NOT NULL DEFAULT TRUE,

    -- ── Modes d'affichage ────────────────────────────────────────────────────
    -- SINGLE      : un prix affiche, avec selecteur de devise
    -- SWITCHER    : selecteur de devise toujours visible
    -- LIST_ALL    : tous les prix du plan empiles (comportement historique)
    -- AUTO_REGION : prix de la region detectee, repli GLOBAL
    display_mode                  VARCHAR(30) NOT NULL DEFAULT 'SINGLE',
    allow_cycle_choice            BOOLEAN NOT NULL DEFAULT TRUE,
    default_cycle                 VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    allow_monthly                 BOOLEAN NOT NULL DEFAULT TRUE,
    allow_yearly                  BOOLEAN NOT NULL DEFAULT TRUE,
    allow_region_choice           BOOLEAN NOT NULL DEFAULT FALSE,

    -- ── Regions ──────────────────────────────────────────────────────────────
    region_pricing_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    -- TENANT | HEADER | QUERY | IP
    region_resolution             VARCHAR(30) NOT NULL DEFAULT 'TENANT',
    region_header_name            VARCHAR(64) NOT NULL DEFAULT 'X-Discipolat-Region',
    -- false par defaut : un navigateur ne peut pas forger un en-tete arbitraire
    -- en cross-origin, l'accepter par defaut laisserait un acheteur choisir sa
    -- region (et donc son tarif) librement.
    trust_region_header           BOOLEAN NOT NULL DEFAULT FALSE,
    allow_region_query_override   BOOLEAN NOT NULL DEFAULT TRUE,
    default_region                VARCHAR(10) NOT NULL DEFAULT 'AF',

    -- ── Canaux de paiement ───────────────────────────────────────────────────
    allow_card_channel            BOOLEAN NOT NULL DEFAULT TRUE,
    allow_momo_channel            BOOLEAN NOT NULL DEFAULT FALSE,
    allow_strong_channel          BOOLEAN NOT NULL DEFAULT TRUE,
    allow_bank_channel            BOOLEAN NOT NULL DEFAULT TRUE,
    allow_contact_channel         BOOLEAN NOT NULL DEFAULT TRUE,
    -- Devise dans laquelle on debite quand le catalogue est en XAF/XOF.
    strong_currency               CHAR(3) NOT NULL DEFAULT 'EUR',
    momo_operators                VARCHAR(50)[] NOT NULL DEFAULT '{MTN_MOMO,ORANGE_MONEY,M_PESA}',

    -- ── Stripe ───────────────────────────────────────────────────────────────
    stripe_price_auto_sync        BOOLEAN NOT NULL DEFAULT FALSE,
    stripe_sync_batch_size        INTEGER NOT NULL DEFAULT 20,
    stripe_sync_interval_ms       BIGINT  NOT NULL DEFAULT 300000,

    -- ── Essai et remise ──────────────────────────────────────────────────────
    trial_enabled                 BOOLEAN NOT NULL DEFAULT TRUE,
    annual_discount_default       INTEGER NOT NULL DEFAULT 17,

    -- ── Landing ──────────────────────────────────────────────────────────────
    landing_pricing_enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    -- API   : la section landing lit le catalogue (source de verite unique)
    -- STATIC: 3 tiers « Sur devis » codees en dur (comportement historique)
    landing_pricing_source        VARCHAR(20) NOT NULL DEFAULT 'API',
    landing_pricing_max_plans     INTEGER NOT NULL DEFAULT 4,
    landing_pricing_show_prices   BOOLEAN NOT NULL DEFAULT TRUE,
    landing_pricing_show_toggle   BOOLEAN NOT NULL DEFAULT TRUE,
    landing_pricing_cta_label     VARCHAR(60),

    -- ── Journalisation ───────────────────────────────────────────────────────
    -- Incrémenté à chaque écriture : le client public l'interroge au chargement
    -- pour savoir s'il doit rafraîchir son cache (pas de WebSocket nécessaire).
    catalog_version               INTEGER NOT NULL DEFAULT 1,
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by                    UUID,

    CONSTRAINT ck_pricing_settings_display_mode CHECK (
        display_mode IN ('SINGLE','SWITCHER','LIST_ALL','AUTO_REGION')),
    CONSTRAINT ck_pricing_settings_source CHECK (
        landing_pricing_source IN ('API','STATIC')),
    CONSTRAINT ck_pricing_settings_resolution CHECK (
        region_resolution IN ('TENANT','HEADER','QUERY','IP')),
    CONSTRAINT ck_pricing_settings_cycle CHECK (
        default_cycle IN ('MONTHLY','YEARLY'))
);

INSERT INTO saas_pricing_settings (id) VALUES (1) ON CONFLICT (id) DO NOTHING;

COMMENT ON TABLE  saas_pricing_settings IS
    'Réglages plateforme de la tarification (singleton, id=1). Source unique de paramétrage.';
COMMENT ON COLUMN saas_pricing_settings.show_fallback_currency IS
    'false = jamais de prix estimé. Un plan sans prix dans la devise demandée reste « non communiqué ».';
COMMENT ON COLUMN saas_pricing_settings.trust_region_header IS
    'false par défaut : un en-tête de région venant d''un navigateur est ignoré (l''acheteur pourrait choisir son tarif).';
COMMENT ON COLUMN saas_pricing_settings.landing_pricing_source IS
    'API = catalogue dynamique. STATIC = 3 tiers « Sur devis » historiques. Évite deux vérités contradictoires.';