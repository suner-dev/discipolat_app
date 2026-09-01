package com.discipolat.modules.payments.domain;

/**
 * SPI d’un fournisseur de paiement Mobile Money (MTN MoMo, Orange Money,
 * M-Pesa, …) — Tithe &amp; Offering 2.0.
 *
 * <p>Chaque implémentation contacte l’API REST <strong>réelle</strong> du
 * opérateur documenté (pas de simulation). Un provider n’est activé QUE si
 * ses credentials sont renseignés dans la configuration. En l’absence de
 * provider configuré, {@link PaymentGatewayService} conserve la génération
 * locale de référence (fallback dev) et la confirmation passera par le
 * webhook opérateur signé.</p>
 *
 * <p>Conformément à la règle du master agent (§41) : l’intégration est réelle
 * (contrat, configuration, persistance, gestion d’erreurs, tests) mais le
 * fonctionnement live exige un compte opérateur configuré.</p>
 */
public interface MobileMoneyProvider {

    /** Opérateur auquel ce provider est rattaché. */
    PaymentIntent.Operator operator();

    /** Ce provider est-il opérationnel (credentials + URL de base configurés) ? */
    boolean isEnabled();

    /**
     * Envoie une demande de collecte au opérateur.
     *
     * @return la référence opérateur (remplace la référence locale), l’URL de
     *         redirection/checkout éventuelle et des instructions à afficher.
     */
    Result initiate(PaymentIntent intent);

    /** Vérifie auprès de l’opérateur si une référence est réellement payée. */
    Verification verify(String providerReference);

    record Result(String providerReference,
                  String checkoutUrl,
                  String instructions,
                  boolean externalRedirect) {}

    record Verification(boolean paid,
                          String operatorStatus,
                          String failureReason) {}
}
