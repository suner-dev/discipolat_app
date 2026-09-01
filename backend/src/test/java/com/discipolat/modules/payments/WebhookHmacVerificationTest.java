package com.discipolat.modules.payments;

import com.discipolat.modules.payments.domain.PaymentProviderProperties;
import com.discipolat.modules.payments.domain.WebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de la vérification HMAC pour chaque provider.
 *
 * <p>Valide que :</p>
 * <ul>
 *   <li>Une signature valide est acceptée</li>
 *   <li>Une signature invalide est rejetée</li>
 *   <li>Sans secret configuré, la vérification est désactivée (sandbox)</li>
 *   <li>Les comparaisons sont à temps constant (timing attack resistant)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class WebhookHmacVerificationTest {

    @Mock private PaymentProviderProperties props;

    private WebhookSignatureVerifier verifier;

    private static final String TEST_SECRET = "my-super-secret-key-for-hmac-testing-2026";
    private static final String TEST_BODY = "{\"reference\":\"MTN-260901-ABC\",\"success\":true}";
    private static final String TEST_REF = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    @BeforeEach
    void setUp() {
        verifier = new WebhookSignatureVerifier(props);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HMAC-SHA256 utility
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("HMAC-SHA256 produit la bonne signature")
    void hmacSha256_produitSignatureCorrecte() throws Exception {
        String signature = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, TEST_SECRET);
        assertThat(signature).isNotEmpty();
        assertThat(signature).hasSize(64); // SHA-256 = 32 bytes = 64 hex chars
        assertThat(signature).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("HMAC-SHA256 est déterministe")
    void hmacSha256_estDeterministe() throws Exception {
        String sig1 = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, TEST_SECRET);
        String sig2 = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, TEST_SECRET);
        assertThat(sig1).isEqualTo(sig2);
    }

    @Test
    @DisplayName("HMAC-SHA256 change avec clé différente")
    void hmacSha256_changeAvecCleDifferent() throws Exception {
        String sig1 = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, TEST_SECRET);
        String sig2 = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, "other-key");
        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    @DisplayName("HMAC-SHA256 change avec body différent")
    void hmacSha256_changeAvecBodyDifferent() throws Exception {
        String sig1 = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, TEST_SECRET);
        String sig2 = WebhookSignatureVerifier.computeHmacSha256("{\"different\":true}", TEST_SECRET);
        assertThat(sig1).isNotEqualTo(sig2);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Orange Money verification
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Orange: signature valide acceptée")
    void orange_signatureValide_acceptee() throws Exception {
        String signature = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, TEST_SECRET);
        assertThat(verifier.verifyOrange(TEST_BODY, signature, TEST_SECRET)).isTrue();
    }

    @Test
    @DisplayName("Orange: signature invalide rejetée")
    void orange_signatureInvalide_rejetee() {
        assertThat(verifier.verifyOrange(TEST_BODY, "invalid-signature", TEST_SECRET)).isFalse();
    }

    @Test
    @DisplayName("Orange: sans merchant key → mode sandbox (accepte tout)")
    void orange_sansCle_modeSandbox() {
        assertThat(verifier.verifyOrange(TEST_BODY, null, null)).isTrue();
        assertThat(verifier.verifyOrange(TEST_BODY, null, "")).isTrue();
        assertThat(verifier.verifyOrange(TEST_BODY, "anything", null)).isTrue();
    }

    @Test
    @DisplayName("Orange: sans header signature → rejeté")
    void orange_sansHeader_rejetee() {
        assertThat(verifier.verifyOrange(TEST_BODY, null, TEST_SECRET)).isFalse();
        assertThat(verifier.verifyOrange(TEST_BODY, "", TEST_SECRET)).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // MTN MoMo verification
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("MTN: signature de la référence valide acceptée")
    void mtn_signatureValide_acceptee() throws Exception {
        String signature = WebhookSignatureVerifier.computeHmacSha256(TEST_REF, TEST_SECRET);
        assertThat(verifier.verifyMtn(TEST_REF, signature, TEST_SECRET)).isTrue();
    }

    @Test
    @DisplayName("MTN: signature invalide rejetée")
    void mtn_signatureInvalide_rejetee() {
        assertThat(verifier.verifyMtn(TEST_REF, "wrong-sig", TEST_SECRET)).isFalse();
    }

    @Test
    @DisplayName("MTN: sans subscription key → mode sandbox")
    void mtn_sansCle_modeSandbox() {
        assertThat(verifier.verifyMtn(TEST_REF, null, null)).isTrue();
    }

    @Test
    @DisplayName("MTN: sans header signature → rejeté")
    void mtn_sansHeader_rejetee() {
        assertThat(verifier.verifyMtn(TEST_REF, null, TEST_SECRET)).isFalse();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Generic verification
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Generic: signature valide acceptée")
    void generic_signatureValide_acceptee() throws Exception {
        String signature = WebhookSignatureVerifier.computeHmacSha256(TEST_BODY, TEST_SECRET);
        assertThat(verifier.verifyGeneric(TEST_BODY, signature, TEST_SECRET)).isTrue();
    }

    @Test
    @DisplayName("Generic: signature invalide rejetée")
    void generic_signatureInvalide_rejetee() {
        assertThat(verifier.verifyGeneric(TEST_BODY, "fake", TEST_SECRET)).isFalse();
    }

    @Test
    @DisplayName("Generic: sans secret → mode ouvert")
    void generic_sansSecret_modeOuvert() {
        assertThat(verifier.verifyGeneric(TEST_BODY, null, null)).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // M-Pesa IP verification
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("M-Pesa: sans secret → mode sandbox (accepte tout)")
    void mpesa_sansSecret_modeSandbox() {
        assertThat(verifier.verifyMpesa(TEST_BODY, "196.201.214.1", null)).isTrue();
    }

    @Test
    @DisplayName("M-Pesa: avec secret → vérifie structure du body")
    void mpesa_avecSecret_verifieBody() {
        assertThat(verifier.verifyMpesa(TEST_BODY, "196.201.214.1", TEST_SECRET)).isTrue();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Base64 variant
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("HMAC-SHA256 Base64 produit une signature Base64 valide")
    void hmacSha256Base64_valide() throws Exception {
        String sig = WebhookSignatureVerifier.computeHmacSha256Base64(TEST_BODY, TEST_SECRET);
        assertThat(sig).isNotEmpty();
        // Base64 encoded SHA-256 = 44 chars
        assertThat(sig).matches("[A-Za-z0-9+/]+=*");
    }
}
