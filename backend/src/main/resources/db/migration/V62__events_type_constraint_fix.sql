-- ============================================================
-- FIX: la contrainte CHECK sur events.type_evenement (V3) ne
-- permettait que 9 types alors que le dictionnaire EVENT_TYPE
-- (V42) en configure 13 (CULTE, ETUDE_BIBLIQUE, VEILLEE, PRIERE
-- manquaient) → création d'un événement de ces types : 500.
-- On réaligne la contrainte sur la liste complète du dictionnaire.
-- ============================================================
ALTER TABLE events DROP CONSTRAINT IF EXISTS events_type_evenement_check;

ALTER TABLE events ADD CONSTRAINT events_type_evenement_check
    CHECK (type_evenement IN (
        'SORTIE', 'RETRAITE', 'EVANGELISATION', 'REUNION', 'VISITE',
        'CONFERENCE', 'FORMATION', 'ANNIVERSAIRE',
        'CULTE', 'ETUDE_BIBLIQUE', 'VEILLEE', 'PRIERE', 'AUTRE'
    ));
