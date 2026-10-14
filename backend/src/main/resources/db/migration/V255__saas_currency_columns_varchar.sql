-- V255 : termine l'alignement CHAR(3) → VARCHAR(3) entre le schéma et les entités JPA.
--
-- Les entités (SaasPlanPrice, SaasPlanPricingSettings, TenantSubscription) déclarent
-- leurs colonnes devise en `@Column(length = 3)` sur un `String` → Hibernate attend
-- `varchar` (Types#VARCHAR). Les migrations d'origine avaient créé ces colonnes en
-- `CHAR(3)` = `bpchar` (Types#CHAR) ; sous `ddl-auto: validate` le démarrage était
-- bloqué (même famille d'erreur que V254, colonne [currency] de saas_momo_subscriptions).
--
-- ALTER TYPE vers varchar est sans perte ici (codes ISO-4217, 3 caractères) et
-- idempotent. V254 n'est pas modifié (déjà appliquée → checksum) : on complète ici.
ALTER TABLE saas_plan_prices
    ALTER COLUMN checkout_currency TYPE VARCHAR(3),
    ALTER COLUMN currency          TYPE VARCHAR(3);

ALTER TABLE saas_pricing_settings
    ALTER COLUMN base_currency             TYPE VARCHAR(3),
    ALTER COLUMN default_display_currency  TYPE VARCHAR(3),
    ALTER COLUMN strong_currency           TYPE VARCHAR(3);

ALTER TABLE tenant_subscriptions
    ALTER COLUMN price_currency TYPE VARCHAR(3);
