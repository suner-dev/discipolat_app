package com.discipolat.modules.payments.domain;

import java.util.Set;

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

    /**
     * Operateurs que ce provider sait réellement servir.
     *
     * <p>Par defaut un provider ne sert que son {@link #operator()}. Un
     * <strong>agregateur</strong> (ex. CinetPay) etend cet ensemble pour couvrir
     * plusieurs operateurs (Orange Money, MTN MoMo, Wave, carte) derriere une
     * seule integration. Methode <em>additive</em> : les providers existants
     * (Orange, MTN, M-Pesa) heritent ce comportement sans aucune modification.
     */
    default Set<PaymentIntent.Operator> supportedOperators() {
        return Set.of(operator());
    }

    /**
     * Un agregateur prend la priorite sur les APIs operateurs directes quand il
     * est active (un seul contrat, des frais generalement plus bas) ; les
     * providers operateurs directs restent le repli. Faux par defaut.
     */
    default boolean aggregator() {
        return false;
    }

    /** Ce provider est-il operationnel (credentials + URL de base configures) ? */
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
