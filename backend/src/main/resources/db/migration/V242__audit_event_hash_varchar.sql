-- V242 : alignement audit_event.hash — la migration V135 avait créé
-- `hash` et `prev_hash` en CHAR(64) (longueur fixe, padding), alors que
-- l'entité AuditEvent les mappe en `String` (VARCHAR(64)).
--
-- CONSÉQUENCE MESURÉE. Avec `ddl-auto: validate` (profil docker, imposé
-- depuis le commit docs(governance)), Hibernate échoue au démarrage :
--
--   Schema-validation: wrong column type encountered in column [hash] in
--   table [audit_event]; found [bpchar (Types#CHAR)], but expecting
--   [varchar(64) (Types#VARCHAR)]
--
-- (logs discipolat-api, 09/10/2026). L'ancien mode `update` masquait la
-- dérive en réécrivant le schéma au boot — comportement désormais exclu.
--
-- SÉMANTIQUE RETENUE. `hash`/`prev_hash` portent des empreintes hexadécimales
-- de 64 caractères exactement (voir AuditEventService). `CHAR(64)` forcerait un
-- padding d'espaces, ce qui casse les comparaisons et l'export ; `VARCHAR(64)`
-- sans défaut est le type exact de l'entité. La conversion CHAR → VARCHAR est
-- sans perte : les valeurs existantes sont rognées de leur padding droit, ce qui
-- les rend IDENTIQUES à l'usage prévu (hash strict de 64 caractères).
--
-- POTENTIELLEMENT AFFECTÉ : toute base construite par les migrations V135+.
-- Les bases H2 des tests (create-drop, schéma généré) ne sont pas concernées.

ALTER TABLE audit_event
    ALTER COLUMN hash TYPE VARCHAR(64)
    USING rtrim(hash::text);

ALTER TABLE audit_event
    ALTER COLUMN prev_hash TYPE VARCHAR(64)
    USING rtrim(prev_hash::text);

COMMENT ON COLUMN audit_event.hash IS
    'Empreinte hexadécimale de cet enregistrement (chaîne d''intégrité). '
    'Type VARCHAR(64), et non CHAR(64) : pas de padding d''espaces (V242, aligné AuditEvent).';

COMMENT ON COLUMN audit_event.prev_hash IS
    'Hash de l''enregistrement précédent (chaîne d''intégrité). '
    'Type VARCHAR(64), et non CHAR(64) : pas de padding d''espaces (V242, aligné AuditEvent).';
