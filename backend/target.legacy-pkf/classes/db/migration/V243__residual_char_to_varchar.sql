-- V243 : alignement des dernières colonnes CHAR — le mode `validate` (profil
-- docker) révèle, une par une, les dérives de type héritage CHAR :
--
--   - V173 : invitations.token_hash CHAR(64)  → attendu VARCHAR(64)
--   - V174 : refresh_token_sessions.token_hash CHAR(64) → attendu VARCHAR(64)
--   - V190 : finance_transactions.devise CHAR(3) → attendu VARCHAR(3)
--
-- (audit_event.hash/prev_hash ont été traités par V242.)
--
-- SÉMANTIQUE RETENUE. Ces colonnes portent des empreintes hexadécimales
-- (`token_hash`, 64 caractères) ou des codes ISO-4217 (`devise`, 3 lettres).
-- L'entité les mappe en `String` (VARCHAR) : `CHAR(n)` forcerait un padding
-- d'espaces qui casse les comparaisons d'égalité (unicité de `token_hash`,
-- filtrage par devise). La conversion CHAR → VARCHAR via `rtrim` est sans
-- perte : les valeurs existantes sont rognées de leur padding droit, ce qui
-- les rend IDENTIQUES à l'usage prévu.
--
-- POTENTIELLEMENT AFFECTÉ : toute base construite par les migrations V173+.
-- Les bases H2 des tests (create-drop, schéma généré) ne sont pas concernées.

ALTER TABLE invitations
    ALTER COLUMN token_hash TYPE VARCHAR(64)
    USING rtrim(token_hash::text);

ALTER TABLE refresh_token_sessions
    ALTER COLUMN token_hash TYPE VARCHAR(64)
    USING rtrim(token_hash::text);

ALTER TABLE finance_transactions
    ALTER COLUMN devise TYPE VARCHAR(3)
    USING rtrim(devise::text);

COMMENT ON COLUMN invitations.token_hash IS
    'Empreinte du token d''invitation. Type VARCHAR(64), et non CHAR(64) : pas '
    'de padding d''espaces (V243, aligné Invitation).';

COMMENT ON COLUMN refresh_token_sessions.token_hash IS
    'Empreinte de la session refresh. Type VARCHAR(64), et non CHAR(64) : pas '
    'de padding d''espaces (V243, aligné RefreshTokenSession).';

COMMENT ON COLUMN finance_transactions.devise IS
    'Code devise ISO-4217. Type VARCHAR(3), et non CHAR(3) : pas de padding '
    'd''espaces (V243, aligné FinanceTransaction).';
