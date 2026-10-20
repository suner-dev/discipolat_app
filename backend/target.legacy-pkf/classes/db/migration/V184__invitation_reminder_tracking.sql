-- V184 : suivi des relances d'invitation (constat M4).
--
-- ADDITIF STRICT : aucun DROP, aucune modification de migration existante.
--
-- 1) `reminded_at` : horodatage de la DERNIÈRE relance envoyée. Sans cette
--    colonne, impossible de savoir si une relance est déjà partie : le scheduler
--    enverrait un rappel à chaque exécution quotidienne à toute invitation en
--    attente, ce qui est un harcèlement postal et un risque de réputation.
-- 2) Index (status, expires_at) : le scheduler interroge les invitations
--    PENDING par fenêtre d'expiration ; sans cet index, le balayage est un
--    scan séquentiel de `invitations` à chaque exécution.

ALTER TABLE invitations ADD COLUMN IF NOT EXISTS reminded_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_invitations_status_expires ON invitations (status, expires_at);
