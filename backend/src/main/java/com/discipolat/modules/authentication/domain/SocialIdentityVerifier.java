package com.discipolat.modules.authentication.domain;

/**
 * Vérification d'un credential d'identité externe (jeton OIDC émis par Google
 * ou Microsoft).
 *
 * <p>Implémentation : signature RS256 validée contre les clés publiques du
 * fournisseur (JWKS), puis contrôles de {@code iss}, {@code aud},
 * {@code email_verified} et, pour Microsoft, du claim {@code tid}.
 */
public interface SocialIdentityVerifier {

    /**
     * Vérifie un credential et retourne l'identité prouvée.
     *
     * @param provider   fournisseur attendu
     * @param credential jeton opaque reçu du client (jamais journalisé)
     * @return l'identité vérifiée, jeton de session compris
     * @throws SocialCredentialException si le credential est invalide, expiré,
     *         émis pour une autre audience, ou si l'email n'est pas vérifié
     */
    VerifiedIdentity verify(SocialProvider provider, String credential);

    /**
     * Identité prouvée par un credential valide.
     *
     * @param provider      fournisseur émetteur
     * @param subject       claim {@code sub} : identifiant opaque et STABLE du
     *                      fournisseur. Seule clé d'authentification — l'email
     *                      n'est pas renvoyé à chaque connexion par Apple.
     * @param email         email communiqué par le fournisseur (jamais brut :
     *                      toujours issu d'un jeton vérifié)
     * @param emailVerified le fournisseur a-t-il explicitement vérifié l'email
     * @param displayName   nom d'affichage, éventuellement vide
     * @param pictureUrl    URL de l'avatar, éventuellement vide
     */
    record VerifiedIdentity(
            SocialProvider provider,
            String subject,
            String email,
            boolean emailVerified,
            String displayName,
            String pictureUrl) {

        public VerifiedIdentity {
            if (provider == null) {
                throw new IllegalArgumentException("Fournisseur nul");
            }
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException("Subject du fournisseur manquant");
            }
            email = email == null ? "" : email.trim();
            displayName = displayName == null ? "" : displayName.trim();
            pictureUrl = pictureUrl == null ? "" : pictureUrl.trim();
        }

        /** Email attendu d'une invitation : comparaison insensible à la casse. */
        public boolean matchesEmail(String expectedEmail) {
            return expectedEmail != null
                    && !expectedEmail.isBlank()
                    && email.equalsIgnoreCase(expectedEmail.trim());
        }

        /** Prénom et nom déduits du nom d'affichage, pour préremplir un profil. */
        public String[] splitDisplayName() {
            if (displayName.isBlank()) {
                return new String[]{"", ""};
            }
            int space = displayName.indexOf(' ');
            if (space <= 0 || space == displayName.length() - 1) {
                return new String[]{displayName, ""};
            }
            return new String[]{
                    displayName.substring(0, space),
                    displayName.substring(space + 1).trim()
            };
        }
    }

    /**
     * Échec de vérification, avec un code stable et un statut HTTP.
     *
     * <p>Un code distinct par cause évite au client de distinction inutile
     * (aucune fuite d'existence de compte), tout en permettant un message
     * honnête et une décision d'interface correcte.
     */
    class SocialCredentialException extends RuntimeException {

        private final String code;
        private final int httpStatus;

        public SocialCredentialException(String code, int httpStatus, String message) {
            super(message);
            this.code = code;
            this.httpStatus = httpStatus;
        }

        public SocialCredentialException(String code, int httpStatus, String message, Throwable cause) {
            super(message, cause);
            this.code = code;
            this.httpStatus = httpStatus;
        }

        public String code() {
            return code;
        }

        public int httpStatus() {
            return httpStatus;
        }

        /** 400 : le credential lui-même est malformé ou rejeté par le fournisseur. */
        public static SocialCredentialException invalid(String reason) {
            return new SocialCredentialException("SOCIAL_CREDENTIAL_INVALID", 400, reason);
        }

        /** 401 : signature, expiration ou audience incorrectes. */
        public static SocialCredentialException rejected(String reason) {
            return new SocialCredentialException("SOCIAL_CREDENTIAL_REJECTED", 401, reason);
        }

        /** 403 : credential authentique, mais refusée par notre politique (email non vérifié, tenant non autorisé). */
        public static SocialCredentialException forbidden(String code, String reason) {
            return new SocialCredentialException(code, 403, reason);
        }

        /** 503 : fournisseur non configuré sur ce serveur. */
        public static SocialCredentialException notConfigured(String providerName) {
            return new SocialCredentialException("SOCIAL_PROVIDER_NOT_CONFIGURED", 503,
                    "Le fournisseur d'identité " + providerName + " n'est pas configuré sur ce serveur");
        }
    }
}
