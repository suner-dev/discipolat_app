package com.discipolat.modules.payments.payout;

import java.util.Map;

/**
 * A3 (M8, M9) — Demande de décaissement universelle.
 *
 * <p>Le montant est porté dans l'unité mineure de la devise (centimes,
 * kopecks, ou francs entiers pour les devises à 0 décimale comme le XOF ou
 * le JPY) : c'est la représentation <b>auditable</b> exigée par M9 — un
 * {@code double} de montant n'est jamais exact et rend la réconciliation
 * multi-devises impossible.</p>
 *
 * <p>La devise est un code ISO-4217 à trois lettres, validée ici ; la
 * conversion en unités mineures relève de
 * {@link com.discipolat.modules.currency.domain.Iso4217CurrencyValidator}.</p>
 */
public record PayoutRequest(
        String idempotencyKey,
        String recipient,
        long amountMinor,
        String currency,
        String reference,
        Map<String, String> metadata) {

    public PayoutRequest {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("PayoutRequest.recipient requis");
        }
        if (amountMinor <= 0) {
            throw new IllegalArgumentException("PayoutRequest.amountMinor doit être > 0");
        }
        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("PayoutRequest.currency doit être un code ISO-4217 (3 lettres majuscules)");
        }
        if (reference == null || reference.isBlank()) {
            throw new IllegalArgumentException("PayoutRequest.reference (référence métier) requise");
        }
    }
}
