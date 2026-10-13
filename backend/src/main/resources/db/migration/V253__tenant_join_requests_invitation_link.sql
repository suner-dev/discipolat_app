-- V253__tenant_join_requests_invitation_link.sql
-- ============================================================
-- SPEC_ORGANISATION_DENOMINATION_V2 §7.3 — l'adhésion approuvée doit rester
-- rattrapable.
--
-- Pourquoi : à l'approbation d'une demande SANS compte, le serveur émet une
-- invitation (`InvitationService`) et la personne n'a plus qu'à ouvrir le lien.
-- Mais le lien n'était relié à AUCUNE ligne : `tenant_join_requests` passait à
-- APPROVED, `GET /tenant/join-requests` ne renvoyait que les PENDING, et
-- l'invitation expirait 7 jours plus tard. À ce moment-là la demande était
-- invisible pour le responsable ET l'invitation morte : la personne ne pouvait
-- plus rien demander, et l'église n'avait aucun moyen de l'aider.
--
-- Ces deux colonnes rendent le lien explicite et donc la relance possible :
--   invitation_id     → l'invitation émise pour CETTE demande (une relance
--                       régénère le jeton sur cette ligne-là, pas sur une autre) ;
--   invitation_sent_at→ quand le lien a été envoyé, pour que l'écran puisse
--                       distinguer « jamais parti » de « parti il y a 3 jours ».
--
-- NULLables et sans contrainte de clé étrangère : les demandes approuvées AVANT
-- cette migration n'ont pas d'invitation (l'approbation n'en émettait aucune),
-- et une invitation supprimée ne doit pas empêcher de lire l'historique.
-- ============================================================

ALTER TABLE tenant_join_requests
    ADD COLUMN IF NOT EXISTS invitation_id UUID,
    ADD COLUMN IF NOT EXISTS invitation_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_tenant_join_requests_invitation
    ON tenant_join_requests(invitation_id);

COMMENT ON COLUMN tenant_join_requests.invitation_id IS
    'Invitation émise à l''approbation pour une demande sans compte (V253). NULL pour une demande traitée avant la migration.';

COMMENT ON COLUMN tenant_join_requests.invitation_sent_at IS
    'Dernier envoi du lien d''activation. Permet à l''écran de distinguer un envoi jamais parti d''un envoi ancien.';
