package com.discipolat.modules.tenants.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Canaux de paiement proposés pour un prix de catalogue (contrat §2.3).
 *
 * <p>Un même prix peut en proposer plusieurs ; l'ordre du CSV stocké en base
 * fait foi pour l'ordre d'affichage des boutons. Le premier canal techniquement
 * <em>disponible</em> est proposé par défaut — la disponibilité étant décidée
 * côté serveur ({@code PaymentChannelResolver}), jamais devinée par le client.</p>
 */
public enum PaymentChannel {

    /** Carte bancaire via Stripe Checkout. Exige 2 décimales, ou un Price pré-créé. */
    CARD,
    /** Mobile Money (MTN MoMo, Orange Money, M-Pesa) — devisedes pays sans carte. */
    MOMO,
    /** Affichage en devise locale (XAF/XOF), débit en devise forte (EUR/USD). */
    STRONG,
    /** Virement bancaire / SEPA. */
    BANK,
    /** Pas de paiement en ligne : contact commercial (« Sur devis »). */
    CONTACT;

    public static PaymentChannel parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    /** Parse un CSV stocké, en ignorant silencieusement les valeurs inconnues. */
    public static List<PaymentChannel> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<PaymentChannel> channels = new ArrayList<>();
        for (String part : csv.split(",")) {
            PaymentChannel channel = parse(part);
            if (channel != null && !channels.contains(channel)) {
                channels.add(channel);
            }
        }
        return List.copyOf(channels);
    }

    public static String toCsv(List<PaymentChannel> channels) {
        return channels.stream().map(Enum::name).reduce((a, b) -> a + "," + b).orElse("CARD");
    }
}