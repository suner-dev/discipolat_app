package com.discipolat.modules.authentication.api;

/**
 * Contrat figé §3.3 — corps de {@code POST /api/v1/auth/registration-status}.
 *
 * @param email adresse email dont l'on veut suivre la demande d'inscription
 */
public record RegistrationStatusRequest(String email) {
}
