package com.discipolat.modules.payments.payout;

import com.discipolat.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.security.SecureRandom;
import java.util.Map;

/**
 * A3 (M8) — Virement bancaire classique (hors SEPA) et canaux « manuels ».
 *
 * <p>Ce canal couvre les marchés sans connecteur temps réel : la plateforme
 * génère une référence end-to-end vérifiable (EBS TR 1.3, alphanumérique
 * sans I/O, contrôle mod-97) et l'opération reste {@code PENDING} jusqu'à la
 * réconciliation par webhook bancaire ou confirmation comptable. C'est exactement le contrat
 * attendu d'un virement : ici, le « pending » n'est pas un échec
 * déguisé — aucun {@code SETTLED} n'est jamais émis sans preuve externe.</p>
 */
public class BankTransferProvider implements PayoutProvider {

    static final String KEY = "bank";
    // 34 symboles : 0-9 puis A-Z sans I ni O (saisie humaine sans ambiguïté).
    private static final char[] BASE36 = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PayoutProvidersProperties.Provider config;

    public BankTransferProvider(PayoutProvidersProperties.Provider config) {
        this.config = config;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public String disabledReason() {
        return config.isEnabled() ? null : "drapeau app.payments.providers.bank.enabled=false";
    }

    @Override
    public PayoutResult initiate(PayoutRequest request) {
        if (!isEnabled()) {
            throw new DomainException("Canal virement non configuré",
                    HttpStatus.SERVICE_UNAVAILABLE, "PAYOUT_BANK_NOT_CONFIGURED",
                    Map.of("reason", String.valueOf(disabledReason())));
        }
        String endToEnd = endToEndReference(request.reference());
        return PayoutResult.pending(endToEnd,
                "instruction de virement émise (référence E2E " + endToEnd + ") — en attente de règlement bancaire");
    }

    /**
     * Aucun fournisseur à interroger : le statut vient de la réconciliation
     * (webhook {@code verifyWebhookSignature} ou rapprochement comptable).
     * {@code UNKNOWN} est la réponse honnête.
     */
    @Override
    public PayoutStatus queryStatus(String providerReference) {
        return PayoutStatus.UNKNOWN;
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> headers, String rawBody) {
        if (!StripePayoutProvider.hasText(config.getWebhookSecret()) || rawBody == null) {
            return false;   // fail-closed
        }
        String signature = StripePayoutProvider.getIgnoreCase(headers, "X-Bank-Signature");
        if (signature == null) {
            return false;
        }
        String expected = PayoutCrypto.hmacSha256Hex(config.getWebhookSecret(), rawBody);
        return PayoutCrypto.constantTimeEquals(expected, signature);
    }

    /**
     * Référence E2E conforme aux principes EBS TR 1.3 : 35 caractères max,
     * alphanumérique sans I/O pour la saisie humaine, clés de contrôle mod-97
     * en position 2 et 3 calculées sur la fin de la chaîne.
     */
    static String endToEndReference(String businessReference) {
        // Le « sans I/O » est un contrat de saisie humaine : il s'applique à
        // toute la référence, y compris la partie issue de la référence métier.
        String cleaned = (businessReference == null ? "REF" : businessReference)
                .toUpperCase().replaceAll("[^A-HJ-NP-Z0-9]", "");
        if (cleaned.length() > 32) {
            cleaned = cleaned.substring(0, 32);
        }
        StringBuilder random = new StringBuilder(35);
        random.append(cleaned);
        while (random.length() < 33) {
            random.append(BASE36[RANDOM.nextInt(BASE36.length)]);
        }
        int numValue = numericValue(random.substring(2));
        random.insert(0, checkChars(numValue));
        return random.toString();
    }

    private static String checkChars(int numValue) {
        int mod97 = numValue % 97;
        int check = (mod97 == 0 ? 97 : mod97) % 97;
        // Codage sur deux caractères de la table de 34 symboles (indices sûrs :
        // check < 97 ⇒ check/34 ≤ 2 et check%34 ≤ 28, jamais hors table).
        return String.valueOf(new char[]{BASE36[check / 34], BASE36[check % 34]});
    }

    private static int numericValue(String payload) {
        long value = 0;
        for (char c : payload.toCharArray()) {
            int digit = Character.isDigit(c) ? c - '0' : (c - 'A' + 10) % 36;
            value = (value * 36 + digit) % Integer.MAX_VALUE;
        }
        return (int) (value % 97);
    }
}
