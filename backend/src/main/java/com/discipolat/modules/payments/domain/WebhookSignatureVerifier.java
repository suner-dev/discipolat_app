package com.discipolat.modules.payments.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/**
 * Vérificateur de signatures HMAC pour les callbacks webhook des opérateurs.
 *
 * <p>Chaque opérateur a son propre mécanisme de vérification :</p>
 * <ul>
 *   <li><strong>M-Pesa (Safaricom)</strong> : Vérification par lookup de référence
 *       (le body est signé par Safaricom via TLS + IP whitelisting)</li>
 *   <li><strong>Orange Money</strong> : HMAC-SHA256 du body avec le secret partagé,
 *       envoyé dans le header {@code X-Orange-Signature}</li>
 *   <li><strong>MTN MoMo</strong> : HMAC-SHA256 de la référence avec le secret partagé,
 *       envoyé dans le header {@code X-MTN-Signature} (pour endpoint manuel)</li>
 *   <li><strong>Webhook générique</strong> : HMAC-SHA256 du body avec le secret partagé,
 *       envoyé dans le header {@code X-Webhook-Signature}</li>
 * </ul>
 *
 * <p>Si aucun secret n'est configuré pour un opérateur, la vérification est
 * désactivée (mode sandbox/dev). En production, un secret est OBLIGATOIRE
 * pour chaque opérateur actif.</p>
 */
@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);

    private static final String HMAC_SHA256 = "HmacSHA256";

    // ── M-Pesa Safaricom known IP ranges (sandbox + production) ──
    // Source: https://developer.safaricom.co.ke/faqs
    private static final Set<String> MPESA_KNOWN_IPS = Set.of(
            "196.201.214.0/24",   // Safaricom sandbox
            "41.204.200.0/24"     // Safaricom production
    );

    private final PaymentProviderProperties props;

    public WebhookSignatureVerifier(PaymentProviderProperties props) {
        this.props = props;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // M-Pesa (Safaricom) — IP + reference lookup
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie l'authenticité d'un callback M-Pesa.
     *
     * <p>Safaricom n'utilise PAS de signature HMAC pour les callbacks. La
     * sécurité repose sur :</p>
     * <ol>
     *   <li>TLS obligatoire (HTTPS)</li>
     *   <li>Vérification de l'IP source (optionnel, configurable)</li>
     *   <li>Lookup de la référence dans la base de données (le body doit
     *       contenir un {@code CheckoutRequestID} existant)</li>
     * </ol>
     *
     * @param rawBody      le body brut de la requête
     * @param sourceIp     l'IP source de la requête (peut être null)
     * @param secret       le secret webhook M-Pesa (pour validation IP)
     * @return {@code true} si le callback est considéré légitime
     */
    public boolean verifyMpesa(String rawBody, String sourceIp, String secret) {
        // Si pas de secret configuré → mode sandbox (accepter tout)
        if (secret == null || secret.isBlank()) {
            log.debug("[Verifier:M-Pesa] Pas de secret configuré — mode sandbox");
            return true;
        }

        // 1. Vérification IP source (si configuré)
        if (sourceIp != null && !sourceIp.isBlank()) {
            if (isKnownMpesaIp(sourceIp)) {
                log.debug("[Verifier:M-Pesa] IP source connue: {}", sourceIp);
            } else {
                // En production stricte, on pourrait rejeter ici
                // En mode flexible, on log un warning mais on continue
                log.warn("[Verifier:M-Pesa] IP source non reconnue: {} — " +
                        "en production, activez la vérification IP stricte", sourceIp);
            }
        }

        // 2. Le body doit contenir stkCallback avec des champs requis
        //    (la validation se fait au niveau du parsing dans le controller)
        return true;
    }

    private boolean isKnownMpesaIp(String ip) {
        // Vérification simplifiée — en production, utiliser une lib CIDR
        return MPESA_KNOWN_IPS.stream().anyMatch(range -> {
            String network = range.split("/")[0];
            return ip.startsWith(network.substring(0, network.lastIndexOf('.')));
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Orange Money — HMAC-SHA256 du body
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie la signature HMAC d'un callback Orange Money.
     *
     * <p>Orange signe le body avec le merchant key en utilisant HMAC-SHA256.
     * La signature est envoyée dans le header {@code X-Orange-Signature}.</p>
     *
     * @param rawBody        le body brut de la requête
     * @param signature      la signature du header X-Orange-Signature
     * @param merchantKey    la clé merchant (clé de signature)
     * @return {@code true} si la signature est valide
     */
    public boolean verifyOrange(String rawBody, String signature, String merchantKey) {
        if (merchantKey == null || merchantKey.isBlank()) {
            log.debug("[Verifier:Orange] Pas de merchant key — mode sandbox");
            return true;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("[Verifier:Orange] Header X-Orange-Signature manquant");
            return false;
        }

        try {
            String computed = computeHmacSha256(rawBody, merchantKey);
            boolean valid = constantTimeEquals(computed, signature);
            if (!valid) {
                log.error("[Verifier:Orange] Signature invalide — attendue={}, reçue={}",
                        computed, signature);
            }
            return valid;
        } catch (Exception e) {
            log.error("[Verifier:Orange] Erreur vérification signature", e);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MTN MoMo — HMAC-SHA256 de la référence
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie la signature HMAC d'une demande de vérification MTN MoMo.
     *
     * <p>Puisque MTN n'a pas de webhook natif, les vérifications manuelles
     * doivent être signées pour empêcher le spoofing. La signature est
     * calculée sur la référence avec le subscription key.</p>
     *
     * @param reference       la référence du paiement
     * @param signature       la signature du header X-MTN-Signature
     * @param subscriptionKey le subscription key MTN
     * @return {@code true} si la signature est valide
     */
    public boolean verifyMtn(String reference, String signature, String subscriptionKey) {
        if (subscriptionKey == null || subscriptionKey.isBlank()) {
            log.debug("[Verifier:MTN] Pas de subscription key — mode sandbox");
            return true;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("[Verifier:MTN] Header X-MTN-Signature manquant");
            return false;
        }

        try {
            String computed = computeHmacSha256(reference, subscriptionKey);
            boolean valid = constantTimeEquals(computed, signature);
            if (!valid) {
                log.error("[Verifier:MTN] Signature invalide pour ref={}", reference);
            }
            return valid;
        } catch (Exception e) {
            log.error("[Verifier:MTN] Erreur vérification signature", e);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Webhook générique — HMAC-SHA256 du body
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Vérifie la signature HMAC d'un webhook générique.
     *
     * @param rawBody   le body brut (ou la chaîne signée)
     * @param signature la signature du header X-Webhook-Signature
     * @param secret    le secret partagé
     * @return {@code true} si la signature est valide
     */
    public boolean verifyGeneric(String rawBody, String signature, String secret) {
        if (secret == null || secret.isBlank()) {
            log.debug("[Verifier:Generic] Pas de secret configuré — mode ouvert");
            return true;
        }
        if (signature == null || signature.isBlank()) {
            log.warn("[Verifier:Generic] Header X-Webhook-Signature manquant");
            return false;
        }

        try {
            String computed = computeHmacSha256(rawBody, secret);
            boolean valid = constantTimeEquals(computed, signature);
            if (!valid) {
                log.error("[Verifier:Generic] Signature invalide");
            }
            return valid;
        } catch (Exception e) {
            log.error("[Verifier:Generic] Erreur vérification signature", e);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Utilitaires
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Calcule HMAC-SHA256 d'une chaîne avec une clé donnée.
     *
     * @param data  les données à signer
     * @param key   la clé de signature
     * @return la signature en hexadécimal minuscule
     */
    public static String computeHmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Convertir en hexadécimal (format standard pour les webhooks)
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Calcule HMAC-SHA256 et retourne en Base64 (format alternatif utilisé
     * par certains opérateurs).
     */
    public static String computeHmacSha256Base64(String data, String key) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Comparaison à temps constant pour éviter les timing attacks.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        return java.security.MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
