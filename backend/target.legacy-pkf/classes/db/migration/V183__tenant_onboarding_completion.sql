-- V183 : complétion de l'onboarding au niveau du TENANT (constat B2, décision D2).
--
-- ADDITIF STRICT : aucun DROP, aucune modification de migration existante,
-- aucun nouvel état dans l'enum TenantStatus.
--
-- Décision D2 (justification) : l'achèvement de l'onboarding est porté par DEUX
-- colonnes additives sur `tenants` et non par un nouvel état `ONBOARDING` dans
-- `TenantStatus`. Ajouter un état aurait cassé : l'enum Java, les seeds
-- (V70, V177), les tableaux de bord et une dizaine de tests existants.
--
-- 1) `onboarding_completed_at` / `onboarding_completed_by` : quand le wizard a été
--    terminé (7 étapes COMPLETED ou SKIPPED) et par qui.
-- 2) `onboarding_wizard_steps.skip_reason` : le motif d'un saut était demandé par
--    le contrat (skipRequiresReason) mais n'était stocké nulle part.
-- 3) Index unique (tenant_id, step_type) : rend impossible la création de doublons
--    d'étape, y compris lors de deux `POST /initialize` concurrents.

ALTER TABLE tenants ADD COLUMN IF NOT EXISTS onboarding_completed_at TIMESTAMPTZ;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS onboarding_completed_by UUID;

ALTER TABLE onboarding_wizard_steps ADD COLUMN IF NOT EXISTS skip_reason TEXT;

-- Fail-closed : des doublons d'étape empêcheraient l'index unique ci-dessous.
DO $$
DECLARE dup_count int;
BEGIN
  SELECT COUNT(*) INTO dup_count FROM (
    SELECT tenant_id, step_type FROM onboarding_wizard_steps GROUP BY tenant_id, step_type HAVING COUNT(*) > 1
  ) d;
  IF dup_count > 0 THEN
    RAISE EXCEPTION 'V183: % doublon(s) etape onboarding - nettoyage manuel requis', dup_count;
  END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_onboarding_step_tenant_type ON onboarding_wizard_steps (tenant_id, step_type);
