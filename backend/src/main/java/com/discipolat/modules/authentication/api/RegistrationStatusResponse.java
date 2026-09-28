package com.discipolat.modules.authentication.api;

import java.time.Instant;

/**
 * Contrat figé §3.3 — réponse de {@code POST /api/v1/auth/registration-status}.
 *
 * <p><b>Aucune fuite d'information</b> : ni le mot de passe, ni le nom de
 * l'organisation, ni l'existence d'un compte ne sont exposés. Le seul
 * indicateur est le statut de la <i>demande d'inscription</i>.
 *
 * @param status     {@code PENDING_APPROVAL | APPROVED | REJECTED | NONE}
 * @param decidedAt  date de la décision, {@code null} si en attente ou inconnue
 * @param reason     motif du rejet, {@code null} sauf si {@code status = REJECTED}
 * @param canLogin   l'utilisateur peut-il se connecter ? (= {@code status == APPROVED})
 */
public record RegistrationStatusResponse(
        String status,
        Instant decidedAt,
        String reason,
        boolean canLogin) {
}
