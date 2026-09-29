-- ============================================================================
-- V190 — A3 (M9) : multi-devises ISO-4217 auditable + mentions du reçu fiscal
-- ----------------------------------------------------------------------------
-- Why : le produit doit fonctionner sur toute la planète. Chaque transaction
--       doit porter SA devise exacte (code ISO-4217 + unités mineures entières)
--       et non un BigDecimal ambigu, pour permettre l'audit multi-devises et
--       éviter les erreurs de virgule × 100. Le reçu fiscal doit être
--       configurable par tenant (numéro fiscal, mentions légales) : aucune
--       valeur globale unique.
-- Règle : 100 % ADDITIF. Les colonnes existantes (montant NUMERIC(14,2)) ne
--       sont ni supprimées ni modifiées — les nouvelles colonnes viennent
--       s'y ajouter et le backfill pose taux_vers_base = 1 pour ne casser
--       aucune donnée existante (la base de chaque transaction existante est
--       elle-même, taux 1).
-- ============================================================================

-- 1) finance_transactions : comptabilité exacte par devise --------------------
ALTER TABLE finance_transactions ADD COLUMN IF NOT EXISTS devise CHAR(3);
ALTER TABLE finance_transactions ADD COLUMN IF NOT EXISTS montant_minor BIGINT;
ALTER TABLE finance_transactions ADD COLUMN IF NOT EXISTS taux_vers_base NUMERIC(18,8) NOT NULL DEFAULT 1;
ALTER TABLE finance_transactions ADD COLUMN IF NOT EXISTS montant_base NUMERIC(14,2);

COMMENT ON COLUMN finance_transactions.devise IS 'Code ISO-4217 de la devise de saisie (XAF, EUR, USD, KES…) ; autorité serveur = java.util.Currency.';
COMMENT ON COLUMN finance_transactions.montant_minor IS 'Montant dans l''unité mineure de la devise (entier exact : centimes pour EUR, francs entiers pour XAF/JPY).';
COMMENT ON COLUMN finance_transactions.taux_vers_base IS 'Taux appliqué vers la devise de base du tenant au moment de l''écriture ; 1 = devise de base (backfill historique).';
COMMENT ON COLUMN finance_transactions.montant_base IS 'Contre-valeur dans la devise de base du tenant (audit multi-devises).';

-- Backfill devise : devise primaire déclarée du tenant, sinon tenant_settings,
-- sinon XAF (défaut historique du produit). Deterministe et non destructif.
UPDATE finance_transactions ft
SET devise = COALESCE(
        (SELECT cc.currency_code
           FROM currency_configs cc
          WHERE cc.tenant_id = ft.tenant_id
            AND cc.is_primary = TRUE
            AND cc.is_active = TRUE
          LIMIT 1),
        (SELECT ts.currency
           FROM tenant_settings ts
          WHERE ts.tenant_id = ft.tenant_id
          LIMIT 1),
        'XAF')
WHERE ft.devise IS NULL;

-- Backfill unités mineures : la règle des décimales suit le référentiel
-- ISO-4217 (liste 0-décimale alignée sur java.util.Currency.getDefaultFractionDigits()).
UPDATE finance_transactions
SET montant_minor = CASE
        WHEN devise IN ('XAF','XOF','BIF','CLP','DJF','GNF','ISK','JPY','KMF','KRW','PYG','RWF','UGX','VND','VUV')
            THEN ROUND(montant)
        ELSE ROUND(montant * 100)
    END
WHERE montant_minor IS NULL;

-- Backfill base : historically every tenant booked in its own primary currency
-- at rate 1 → base == saisie. Aucun taux inventé.
UPDATE finance_transactions
SET montant_base = montant,
    taux_vers_base = 1
WHERE montant_base IS NULL;

-- Contraintes : le backfill ayant couvert 100 % des lignes, les colonnes
-- deviennent NOT NULL (fail-closed pour toute écriture future).
ALTER TABLE finance_transactions ALTER COLUMN devise SET NOT NULL;
ALTER TABLE finance_transactions ALTER COLUMN montant_minor SET NOT NULL;

-- Index d'audit multi-devises : agrégations par devise dans un tenant.
CREATE INDEX IF NOT EXISTS idx_finance_transactions_tenant_devise
    ON finance_transactions (tenant_id, devise);

-- 2) tenant_settings : reçu fiscal configurable par tenant ---------------------
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS receipt_tax_number VARCHAR(100);
ALTER TABLE tenant_settings ADD COLUMN IF NOT EXISTS receipt_legal_mentions TEXT;

COMMENT ON COLUMN tenant_settings.receipt_tax_number IS 'Numéro fiscal / identifiant légal de l''organisation, imprimé sur les reçus (configurable par tenant, jamais global).';
COMMENT ON COLUMN tenant_settings.receipt_legal_mentions IS 'Mentions légales libres imprimées en pied de reçu fiscal (configurable par tenant).';
