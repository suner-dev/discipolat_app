-- V254 : aligne saas_momo_subscriptions.currency sur la mapping JPA de
-- SaasMomoSubscription.currency (String length=3 → VARCHAR).
--
-- V251 déclarait `currency CHAR(3)` ; en PostgreSQL `CHAR(n)` = `bpchar` (Types#CHAR),
-- alors que l'entité attend `varchar` (Types#VARCHAR). Sous `ddl-auto: validate`
-- (profil docker), Hibernate refusait tout démarrage :
--   Schema-validation: wrong column type ... column [currency] ...
--   found [bpchar (Types#CHAR)], but expecting [varchar(3)].
--
-- On ne modifie pas V251 (déjà appliquée → checksum). On altère le type ici :
-- idempotent et sans perte de données (3 caractères, padding irrelevant pour un
-- code ISO-4217). Fonctionne aussi bien sur une base existante (volume, beta)
-- que sur une base fraîche (CI) où V251 aura créé le bpchar avant cette ALTER.
ALTER TABLE saas_momo_subscriptions
    ALTER COLUMN currency TYPE VARCHAR(3);
