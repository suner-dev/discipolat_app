-- V244 : alignement des quatre colonnes DECIMAL mappées `Double` — le mode
-- `validate` (profil docker) révèle les dérives de type héritage DECIMAL :
--
--   - patient_records.poids_kg DECIMAL(5,2)   → attendu FLOAT (Double)
--   - patient_records.taille_cm DECIMAL(5,1)  → attendu FLOAT (Double)
--   - pharmacy_items.prix_achat DECIMAL(10,2) → attendu FLOAT (Double)
--   - pharmacy_stock.prix_unitaire DECIMAL(10,2) → attendu FLOAT (Double)
--
-- NOM DE TABLE VÉRIFIÉ. La table est `pharmacy_stock` (SINGULIER), créée par
-- V141 ligne 106 et mappée par PharmacyStock `@Table(name = "pharmacy_stock")`
-- ligne 12. Une première version visait `pharmacy_stocks` (pluriel) : le gate
-- PostgreSQL réel l'a refusée (`ERROR: relation "pharmacy_stocks" does not
-- exist`, SQL State 42P01) — donc elle n'a jamais atteint une base migrée.
--
-- (audit_event + invitations + refresh_token_sessions + finance_transactions
-- ont été traités par V242/V243. Le gate PostgreSQL réel vérifie désormais
-- l'absence de dérive, voir V205 pour le précédent NUMERIC→float8.)
--
-- SÉMANTIQUE RETENUE. Ces champs sont des mesures/estimations à précision
-- flottante (`poids_kg`, `taille_cm`) ou des prix unitaires (`prix_achat`,
-- `prix_unitaire`) manipulés en `double` dans le code (calculs de stock,
-- conversions, exports). L'entité les déclare `Double` → Postgres
-- `double precision` (float8). La conversion `DECIMAL(p,s) → float8` via
-- `USING ...::double precision` est EXACTE pour ces ordres de grandeur
-- (valeurs < 10^4 avec ≤ 2 décimales : représentables sans perte en IEEE 754
-- double, 15-17 chiffres significatifs). On ne touche PAS aux colonnes
-- monétaires mappées `BigDecimal` (montants, tontines, dons : exactitude
-- décimale exigée).
--
-- POTENTIELLEMENT AFFECTÉ : toute base construite par les migrations V141+.
-- Les bases H2 des tests (create-drop, schéma généré) ne sont pas concernées.

ALTER TABLE patient_records
    ALTER COLUMN poids_kg TYPE double precision
    USING poids_kg::double precision;

ALTER TABLE patient_records
    ALTER COLUMN taille_cm TYPE double precision
    USING taille_cm::double precision;

ALTER TABLE pharmacy_items
    ALTER COLUMN prix_achat TYPE double precision
    USING prix_achat::double precision;

ALTER TABLE pharmacy_stock
    ALTER COLUMN prix_unitaire TYPE double precision
    USING prix_unitaire::double precision;

COMMENT ON COLUMN patient_records.poids_kg IS
    'Poids en kg (mesure flottante). Type float8, et non DECIMAL(5,2) : aligné '
    'PatientRecord.poidsKg (V244).';

COMMENT ON COLUMN patient_records.taille_cm IS
    'Taille en cm (mesure flottante). Type float8, et non DECIMAL(5,1) : aligné '
    'PatientRecord.tailleCm (V244).';

COMMENT ON COLUMN pharmacy_items.prix_achat IS
    'Prix d''achat (estimation flottante). Type float8, et non DECIMAL(10,2) : '
    'aligné PharmacyItem.prixAchat (V244).';

COMMENT ON COLUMN pharmacy_stock.prix_unitaire IS
    'Prix unitaire (estimation flottante). Type float8, et non DECIMAL(10,2) : '
    'aligné PharmacyStock.prixUnitaire (V244).';
