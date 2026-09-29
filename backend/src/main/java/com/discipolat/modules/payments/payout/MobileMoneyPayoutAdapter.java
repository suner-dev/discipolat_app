package com.discipolat.modules.payments.payout;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.payments.domain.MobileMoneyProvider;
import com.discipolat.modules.payments.domain.PaymentIntent;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * A3 (M8) — Adaptation des fournisseurs Mobile Money existants (MTN, Orange,
 * M-Pesa) vers le SPI de décaissement.
 *
 * <p>Contexte vérifié dans le code : {@link MobileMoneyProvider} implémente la
 * <b>collecte</b> (demande de paiement au fidèle) — aucun des trois contrats
 * opérateurs intégrés n'implémente l'API de <b>décaissement</b>
 * (disbursement/transfer), qui est un produit distinct chez chaque opérateur.
 * Cet adaptateur est donc volontairement restrictif et honnête :</p>
 * <ul>
 *   <li>il expose l'existence et l'état de configuration de l'opérateur ;</li>
 *   <li>il refuse toute initiation avec un 503 {@code PAYOUT_DISBURSEMENT_NOT_INTEGRATED}
 *       explicite — jamais un faux succès construit sur l'API de collecte ;</li>
 *   <li>il réutilise la vérification de paiement de l'opérateur pour les statuts,
 *       seule capacité réellement intégrée à ce jour.</li>
 * </ul>
 *
 * <p>Ce n'est pas une limitation technique cachée : c'est la frontière exacte
 * entre ce qui est prouvé par le code et ce qui ne l'est pas. L'intégration
 * réelle des API de décaissement opérateurs reste à conduire (comptes
 * merchant dédiés), et chaque instance renverra ce même refus documenté
 * jusqu'alors.</p>
 */
class MobileMoneyPayoutAdapter implements PayoutProvider {

    private final String key;
    private final PaymentIntent.Operator operator;
    private final MobileMoneyProvider delegate;
    private final PayoutProvidersProperties.Provider config;

    MobileMoneyPayoutAdapter(String key,
                             PaymentIntent.Operator operator,
                             MobileMoneyProvider delegate,
                             PayoutProvidersProperties.Provider config) {
        this.key = key;
        this.operator = operator;
        this.delegate = delegate;
        this.config = config;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public boolean isEnabled() {
        // Actif seulement si le flag payout est posé ET que la collecte est
        // réellement configurée : sans credentials opérateur, il n'y a rien.
        return config.isEnabled() && delegate != null && delegate.isEnabled();
    }

    @Override
    public String disabledReason() {
        if (!config.isEnabled()) {
            return "drapeau app.payments.providers." + key + ".enabled=false";
        }
        if (delegate == null || !delegate.isEnabled()) {
            return "credentials opérateur " + operator.getLabel() + " absents";
        }
        return null;
    }

    @Override
    public PayoutResult initiate(PayoutRequest request) {
        throw new DomainException(
                "L'API de décaissement " + operator.getLabel() + " n'est pas intégrée : ce canal ne sait que collecter. "
                        + "Ouvrir un compte disbursement auprès de l'opérateur puis brancher l'implémentation dédiée.",
                HttpStatus.SERVICE_UNAVAILABLE, "PAYOUT_DISBURSEMENT_NOT_INTEGRATED",
                Map.of("operator", operator.name()));
    }

    @Override
    public PayoutStatus queryStatus(String providerReference) {
        if (!isEnabled() || providerReference == null || providerReference.isBlank()) {
            return PayoutStatus.UNKNOWN;
        }
        try {
            MobileMoneyProvider.Verification verification = delegate.verify(providerReference);
            if (verification == null) {
                return PayoutStatus.UNKNOWN;
            }
            return verification.paid() ? PayoutStatus.SETTLED : PayoutStatus.PENDING;
        } catch (RuntimeException providerUnreachable) {
            return PayoutStatus.UNKNOWN;
        }
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> headers, String rawBody) {
        // La vérification signature des webhooks opérateurs vit dans
        // WebhookSignatureVerifier (module payments) : cet adaptateur ne la
        // duplique pas et n'accepte rien par lui-même (fail-closed).
        return false;
    }
}
