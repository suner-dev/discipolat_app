package com.discipolat.modules.authentication.domain;

import com.discipolat.modules.authentication.config.SocialAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sécurité de la connexion par identité externe : ce qui décide si quelqu'un
 * peut entrer dans un compte.
 *
 * <p>Chaque test fixe un <b>credential déjà décodé</b> (le décodeur réel
 * necesita un JWKS réseau) et vérifie la logique de claims : c'est cette logique
 * qui décide de l'audience, de l'émetteur, du tenant et de l'email.
 */
class OidcSocialIdentityVerifierTest {

    private static final String WEB_CLIENT_ID = "web-client.apps.googleusercontent.com";
    private static final String ANDROID_CLIENT_ID = "1234567890-android.apps.googleusercontent.com";
    private static final String IOS_CLIENT_ID = "1234567890-ios.apps.googleusercontent.com";
    private static final String MS_CLIENT_ID = "ms-client-id-uuid";
    private static final String MS_TENANT = "contoso-tenant-id";

    private static SocialAuthProperties properties() {
        SocialAuthProperties properties = new SocialAuthProperties();
        SocialAuthProperties.Google google = new SocialAuthProperties.Google();
        google.setEnabled(true);
        google.setWebClientId(WEB_CLIENT_ID);
        google.setAndroidClientId(ANDROID_CLIENT_ID);
        google.setIosClientId(IOS_CLIENT_ID);
        properties.setGoogle(google);

        SocialAuthProperties.Microsoft microsoft = new SocialAuthProperties.Microsoft();
        microsoft.setEnabled(true);
        microsoft.setClientId(MS_CLIENT_ID);
        microsoft.setTenantId(MS_TENANT);
        properties.setMicrosoft(microsoft);
        return properties;
    }

    /** Décodeur factice : retourne le jeton fourni sans accès réseau. */
    private static SocialIdentityVerifier verifierFor(SocialAuthProperties properties, Jwt jwt) {
        JwtDecoder decoder = token -> jwt;
        Function<SocialProvider, JwtDecoder> factory = provider -> decoder;
        return new OidcSocialIdentityVerifier(properties, factory);
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .subject(String.valueOf(claims.getOrDefault("sub", "subject-1")));
        claims.forEach(builder::claim);
        return builder.build();
    }

    private static Map<String, Object> googleClaims(String... audience) {
        return Map.of(
                "iss", "https://accounts.google.com",
                "aud", List.of(audience),
                "sub", "google-sub-123",
                "email", "Paul@Exemple.com",
                "email_verified", true,
                "name", "Paul Koffi",
                "picture", "https://lh3.googleusercontent.com/a/photo");
    }

    private static Map<String, Object> microsoftClaims(String tenantId, String... audience) {
        return Map.of(
                "iss", "https://login.microsoftonline.com/" + tenantId + "/v2.0",
                "aud", List.of(audience),
                "sub", "ms-sub-456",
                "tid", tenantId,
                "email", "paul@exemple.com",
                "email_verified", true,
                "name", "Paul Koffi");
    }

    // ==================================================================
    @Nested
    @DisplayName("Google")
    class GoogleBehaviour {

        @Test
        @DisplayName("accepte un credential valide et expose une identité vérifiée")
        void acceptsValidCredential() {
            SocialIdentityVerifier verifier =
                    verifierFor(properties(), jwt(googleClaims(WEB_CLIENT_ID)));

            SocialIdentityVerifier.VerifiedIdentity identity =
                    verifier.verify(SocialProvider.GOOGLE, "credential");

            assertEquals(SocialProvider.GOOGLE, identity.provider());
            assertEquals("google-sub-123", identity.subject());
            // L'email est normalisé (trim) mais PAS re-casé : la comparaison se
            // fait en mode insensible à la casse au moment de l'usage.
            assertEquals("Paul@Exemple.com", identity.email());
            assertTrue(identity.emailVerified());
            assertEquals("Paul Koffi", identity.displayName());
            assertEquals("Paul", identity.splitDisplayName()[0]);
            assertEquals("Koffi", identity.splitDisplayName()[1]);
        }

        @Test
        @DisplayName("accepte les trois plateformes : web, Android et iOS ont des client id distincts")
        void acceptsEveryPlatformAudience() {
            // Sans ce test, le mobile ne pourrait JAMAIS se connecter : l'audience
            // d'un jeton Android n'est pas celle du client web.
            for (String clientId : List.of(WEB_CLIENT_ID, ANDROID_CLIENT_ID, IOS_CLIENT_ID)) {
                SocialIdentityVerifier verifier =
                        verifierFor(properties(), jwt(googleClaims(clientId)));
                assertEquals("google-sub-123",
                        verifier.verify(SocialProvider.GOOGLE, "credential").subject(),
                        "client id refusé : " + clientId);
            }
        }

        @Test
        @DisplayName("refuse un jeton émis pour une autre application (audience inconnue)")
        void rejectsForeignAudience() {
            SocialIdentityVerifier verifier =
                    verifierFor(properties(), jwt(googleClaims("another-app.apps.googleusercontent.com")));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.GOOGLE, "credential"));

            assertEquals(401, failure.httpStatus());
            assertEquals("SOCIAL_CREDENTIAL_REJECTED", failure.code());
        }

        @Test
        @DisplayName("refuse un émetteur qui n'est pas Google")
        void rejectsForeignIssuer() {
            Map<String, Object> claims = new java.util.HashMap<>(googleClaims(WEB_CLIENT_ID));
            claims.put("iss", "https://accounts.evil.example");
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.GOOGLE, "credential"));

            assertEquals("SOCIAL_CREDENTIAL_REJECTED", failure.code());
        }

        @Test
        @DisplayName("refuse un email non vérifié par Google (usurpation d'adresse)")
        void rejectsUnverifiedEmail() {
            Map<String, Object> claims = new java.util.HashMap<>(googleClaims(WEB_CLIENT_ID));
            claims.put("email_verified", false);
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.GOOGLE, "credential"));

            assertEquals(403, failure.httpStatus());
            assertEquals("SOCIAL_EMAIL_NOT_VERIFIED", failure.code());
        }

        @Test
        @DisplayName("refuse un jeton sans email")
        void rejectsMissingEmail() {
            Map<String, Object> claims = new java.util.HashMap<>(googleClaims(WEB_CLIENT_ID));
            claims.remove("email");
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.GOOGLE, "credential"));

            assertEquals("SOCIAL_EMAIL_MISSING", failure.code());
        }

        @Test
        @DisplayName("503 si Google est activé mais sans client id : fail-closed, jamais de contournement")
        void failsClosedWhenNotConfigured() {
            SocialAuthProperties properties = new SocialAuthProperties();
            SocialAuthProperties.Google google = new SocialAuthProperties.Google();
            google.setEnabled(true);           // activé…
            google.setWebClientId("");         // …mais sans identifiant
            properties.setGoogle(google);

            SocialIdentityVerifier verifier = verifierFor(properties, jwt(googleClaims(WEB_CLIENT_ID)));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.GOOGLE, "credential"));

            assertEquals(503, failure.httpStatus());
            assertEquals("SOCIAL_PROVIDER_NOT_CONFIGURED", failure.code());
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("Microsoft")
    class MicrosoftBehaviour {

        @Test
        @DisplayName("accepte un credential du tenant attendu")
        void acceptsValidCredential() {
            SocialIdentityVerifier verifier =
                    verifierFor(properties(), jwt(microsoftClaims(MS_TENANT, MS_CLIENT_ID)));

            SocialIdentityVerifier.VerifiedIdentity identity =
                    verifier.verify(SocialProvider.MICROSOFT, "credential");

            assertEquals(SocialProvider.MICROSOFT, identity.provider());
            assertEquals("ms-sub-456", identity.subject());
            assertEquals("paul@exemple.com", identity.email());
        }

        @Test
        @DisplayName("refuse un jeton dont l'émetteur ne correspond pas au claim tid")
        void rejectsIssuerTenantMismatch() {
            // C'est le piège des configurations `common` : un jeton valide d'un
            // tenant A ne doit pas être accepté quand on attend le tenant B.
            Map<String, Object> claims = new java.util.HashMap<>(microsoftClaims(MS_TENANT, MS_CLIENT_ID));
            claims.put("iss", "https://login.microsoftonline.com/autre-tenant/v2.0");
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.MICROSOFT, "credential"));

            assertEquals("SOCIAL_CREDENTIAL_REJECTED", failure.code());
        }

        @Test
        @DisplayName("refuse un tenant hors liste blanche")
        void rejectsTenantNotAllowed() {
            SocialAuthProperties properties = properties();
            properties.getMicrosoft().setTenantId("common");
            properties.getMicrosoft().setAllowedTenantIds(List.of("tenant-autorise"));

            SocialIdentityVerifier verifier =
                    verifierFor(properties, jwt(microsoftClaims("tenant-intrus", MS_CLIENT_ID)));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.MICROSOFT, "credential"));

            assertEquals(403, failure.httpStatus());
            assertEquals("SOCIAL_TENANT_NOT_ALLOWED", failure.code());
        }

        @Test
        @DisplayName("accepte un tenant de la liste blanche en plus du tenant principal")
        void acceptsAdditionalAllowedTenant() {
            SocialAuthProperties properties = properties();
            properties.getMicrosoft().setAllowedTenantIds(List.of("partenaire-1"));

            SocialIdentityVerifier verifier =
                    verifierFor(properties, jwt(microsoftClaims("partenaire-1", MS_CLIENT_ID)));

            assertEquals("ms-sub-456", verifier.verify(SocialProvider.MICROSOFT, "credential").subject());
        }

        @Test
        @DisplayName("la liste blanche s'ajoute au tenant principal sans le remplacer")
        void allowListDoesNotReplacePrimaryTenant() {
            // Régression : une implémentation naïve (liste blanche au lieu de OU)
            // rendait le tenant principal INACCESSIBLE dès qu'un partenaire était
            // déclaré — l'utilisateur de sa propre organisation ne pouvait plus se
            // connecter.
            SocialAuthProperties properties = properties();
            properties.getMicrosoft().setAllowedTenantIds(List.of("partenaire-1"));

            SocialIdentityVerifier verifier =
                    verifierFor(properties, jwt(microsoftClaims(MS_TENANT, MS_CLIENT_ID)));

            assertEquals("ms-sub-456",
                    verifier.verify(SocialProvider.MICROSOFT, "credential").subject());
        }

        @Test
        @DisplayName("refuse une audience inconnue (application cliente différente)")
        void rejectsForeignAudience() {
            SocialIdentityVerifier verifier =
                    verifierFor(properties(), jwt(microsoftClaims(MS_TENANT, "autre-application")));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.MICROSOFT, "credential"));

            assertEquals("SOCIAL_CREDENTIAL_REJECTED", failure.code());
        }

        @Test
        @DisplayName("refuse un email explicitement non vérifié")
        void rejectsExplicitlyUnverifiedEmail() {
            Map<String, Object> claims = new java.util.HashMap<>(microsoftClaims(MS_TENANT, MS_CLIENT_ID));
            claims.put("email_verified", false);
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.MICROSOFT, "credential"));

            assertEquals("SOCIAL_EMAIL_NOT_VERIFIED", failure.code());
        }

        @Test
        @DisplayName("accepte l'absence de email_verified (absent de plusieurs politiques Entra)")
        void acceptsAbsentVerificationClaim() {
            Map<String, Object> claims = new java.util.HashMap<>(microsoftClaims(MS_TENANT, MS_CLIENT_ID));
            claims.remove("email_verified");
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            SocialIdentityVerifier.VerifiedIdentity identity =
                    verifier.verify(SocialProvider.MICROSOFT, "credential");

            assertTrue(identity.emailVerified());
        }

        @Test
        @DisplayName("repli sur preferred_username quand le claim email est absent")
        void fallsBackToPreferredUsername() {
            Map<String, Object> claims = new java.util.HashMap<>(microsoftClaims(MS_TENANT, MS_CLIENT_ID));
            claims.remove("email");
            claims.put("preferred_username", "paul@perso.example");
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            assertEquals("paul@perso.example",
                    verifier.verify(SocialProvider.MICROSOFT, "credential").email());
        }

        @Test
        @DisplayName("refuse un jeton sans aucun email exploitable")
        void rejectsMissingEmail() {
            Map<String, Object> claims = new java.util.HashMap<>(microsoftClaims(MS_TENANT, MS_CLIENT_ID));
            claims.remove("email");
            claims.put("preferred_username", "pas-un-email");
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.MICROSOFT, "credential"));

            assertEquals("SOCIAL_EMAIL_MISSING", failure.code());
        }

        @Test
        @DisplayName("refuse un jeton sans claim tid")
        void rejectsMissingTenantClaim() {
            Map<String, Object> claims = new java.util.HashMap<>(microsoftClaims(MS_TENANT, MS_CLIENT_ID));
            claims.remove("tid");
            SocialIdentityVerifier verifier = verifierFor(properties(), jwt(claims));

            assertEquals("SOCIAL_CREDENTIAL_REJECTED",
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.MICROSOFT, "credential")).code());
        }
    }

    // ==================================================================
    @Nested
    @DisplayName("Garde-fous communs")
    class CommonGuards {

        @Test
        @DisplayName("refuse un credential vide ou démesuré")
        void rejectsMalformedCredential() {
            SocialIdentityVerifier verifier =
                    verifierFor(properties(), jwt(googleClaims(WEB_CLIENT_ID)));

            assertEquals(400, assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                    () -> verifier.verify(SocialProvider.GOOGLE, "  ")).httpStatus());
            assertEquals(400, assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                    () -> verifier.verify(SocialProvider.GOOGLE, "x".repeat(8193))).httpStatus());
        }

        @Test
        @DisplayName("502/401 : une exception du décodeur devient un refus, jamais une fuite")
        void convertsDecoderFailure() {
            SocialAuthProperties properties = properties();
            JwtDecoder exploding = token -> {
                throw new org.springframework.security.oauth2.jwt.JwtValidationException(
                        "signature invalide", List.of());
            };
            SocialIdentityVerifier verifier =
                    new OidcSocialIdentityVerifier(properties, provider -> exploding);

            SocialIdentityVerifier.SocialCredentialException failure =
                    assertThrows(SocialIdentityVerifier.SocialCredentialException.class,
                            () -> verifier.verify(SocialProvider.GOOGLE, "credential"));

            assertEquals(401, failure.httpStatus());
        }

        @Test
        @DisplayName("un fournisseur inconnu est rejeté sans révéler la liste des fournisseurs")
        void rejectsUnknownProviderName() {
            assertThrows(IllegalArgumentException.class, () -> SocialProvider.fromWireName("apple"));
            assertThrows(IllegalArgumentException.class, () -> SocialProvider.fromWireName(null));
            assertThrows(IllegalArgumentException.class, () -> SocialProvider.fromWireName(""));
            assertEquals(SocialProvider.GOOGLE, SocialProvider.fromWireName("GOOGLE"));
            assertEquals(SocialProvider.MICROSOFT, SocialProvider.fromWireName(" Microsoft "));
        }

        @Test
        @DisplayName("les deux fournisseurs exposés restent ceux gratuits et sans plafond")
        void exposesOnlyFreeProviders() {
            assertEquals(Set.of(SocialProvider.GOOGLE, SocialProvider.MICROSOFT),
                    Set.of(SocialProvider.values()));
        }

        @Test
        @DisplayName("le JWKS Microsoft dépend du tenant : common n'est pas une URL par défaut hasardeuse")
        void buildsMicrosoftJwksUri() {
            assertEquals("https://login.microsoftonline.com/common/v2.0/keys",
                    OidcSocialIdentityVerifier.microsoftJwksUri("common"));
            assertEquals("https://login.microsoftonline.com/common/v2.0/keys",
                    OidcSocialIdentityVerifier.microsoftJwksUri(null));
            assertEquals("https://login.microsoftonline.com/" + MS_TENANT + "/discovery/v2.0/keys",
                    OidcSocialIdentityVerifier.microsoftJwksUri(MS_TENANT));
            assertEquals(OidcSocialIdentityVerifier.GOOGLE_JWKS_URI,
                    "https://www.googleapis.com/oauth2/v3/certs");
        }

        @Test
        @DisplayName("correspondance d'email insensible à la casse, avec espaces")
        void matchesEmailIgnoringCaseAndSpaces() {
            SocialIdentityVerifier.VerifiedIdentity identity = new SocialIdentityVerifier.VerifiedIdentity(
                    SocialProvider.GOOGLE, "sub", "Paul@Exemple.com", true, "Paul", "");

            assertTrue(identity.matchesEmail("paul@exemple.com"));
            assertTrue(identity.matchesEmail("PAUL@EXEMPLE.COM"));
            assertTrue(identity.matchesEmail("  paul@exemple.com  "));
            assertFalse(identity.matchesEmail("autre@exemple.com"));
            assertFalse(identity.matchesEmail(""));
            assertFalse(identity.matchesEmail(null));
        }
    }
}
