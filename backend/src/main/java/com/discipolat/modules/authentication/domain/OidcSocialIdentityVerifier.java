package com.discipolat.modules.authentication.domain;

import com.discipolat.modules.authentication.config.SocialAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Vérifie un {@code id_token} Google ou Microsoft par ses clés publiques (JWKS).
 *
 * <h2>Pourquoi ce remplacement est important</h2>
 * L'endpoint {@code oauth2.googleapis.com/tokeninfo} utilisé auparavant :
 * <ol>
 *   <li>est <b>non documenté pour la production</b> et sans SLA ;</li>
 *   <li>effectue un appel réseau à chaque connexion (latence + point de
 *       défaillance externe) ;</li>
 *   <li>n'est mis en cache par aucun mécanisme ;</li>
 *   <li>transforme chaque réponse en oracle « token valide / invalide ».</li>
 * </ol>
 * Ici la signature est vérifiée <b>localement</b> contre le JWKS du
 * fournisseur : aucun appel par demande, aucun quota, et c'est exactement ce
 * qui rend le service gratuit <b>et</b> illimité. Les clés publiques sont mises
 * en cache par le {@code RemoteJWKSet} de Nimbus, qui gère la rotation.
 *
 * <h2>Contrôles appliqués, dans l'ordre</h2>
 * <ol>
 *   <li>fournisseur configuré (fail-closed, 503 sinon) ;</li>
 *   <li>signature RS256 + {@code exp}/{@code nbf} (par le décodeur) ;</li>
 *   <li>{@code iss} dans la liste du fournisseur ;</li>
 *   <li>{@code aud} = l'un des clients enregistrés (web, Android <b>et</b> iOS —
 *       sans quoi le mobile ne pourrait jamais se connecter) ;</li>
 *   <li>pour Microsoft, cohérence {@code iss} ↔ {@code tid} et liste blanche de
 *       tenants si configurée ;</li>
 *   <li>email présent, et vérifié selon la politique du fournisseur.</li>
 * </ol>
 *
 * <h2>Politique de vérification d'email, par fournisseur</h2>
 * <ul>
 *   <li><b>Google</b> : {@code email_verified} doit valoir {@code true}. Google
 *       l'émet systématiquement ; c'est la preuve que l'adresse appartient à la
 *       personne qui se connecte.</li>
 *   <li><b>Microsoft</b> : le claim est <b>absent</b> sur plusieurs politiques
 *       Entra (comptes personnels notamment). Une valeur explicitement
 *       {@code false} est donc refusée, une valeur absente est acceptée — et le
 *       jeton ne peut venir que du tenant qui a émis la signature. Si
 *       {@code email} est absent, {@code preferred_username} n'est utilisé que
 *       lorsqu'il a la forme d'un email <b>et</b> que {@code email_verified}
 *       n'est pas explicitement faux.</li>
 * </ul>
 */
@Component
public class OidcSocialIdentityVerifier implements SocialIdentityVerifier {

    private static final Logger log = LoggerFactory.getLogger(OidcSocialIdentityVerifier.class);

    /** JWKS Google : clés de signature des {@code id_token}. */
    static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    /** Émetteurs acceptés pour un jeton Google. */
    static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private static final String MICROSOFT_ISSUER_PREFIX = "https://login.microsoftonline.com/";

    private final SocialAuthProperties properties;
    private final Function<SocialProvider, JwtDecoder> decoderFactory;
    private final ConcurrentMap<SocialProvider, JwtDecoder> decoders = new ConcurrentHashMap<>();

    /**
     * Constructeur utilisé par Spring.
     *
     * <p>{@code @Autowired} est <b>obligatoire</b> : la classe a deux
     * constructeurs (l'autre sert aux tests), et sans cette annotation Spring
     * cherche un constructeur par défaut, échoue au démarrage du contexte, et
     * fait échouer en cascade tous les tests qui chargent l'application.
     */
    @Autowired
    public OidcSocialIdentityVerifier(SocialAuthProperties properties) {
        this(properties, null);
    }

    /**
     * Constructeur d'injectabilité : les tests fournissent un décodeur de
     * substitution pour valider la logique de claims <b>sans réseau</b>.
     *
     * @param properties    configuration
     * @param decoderFactory fabrique de décodeur, ou {@code null} pour la
     *                      fabrique réelle (JWKS)
     */
    OidcSocialIdentityVerifier(SocialAuthProperties properties,
                               Function<SocialProvider, JwtDecoder> decoderFactory) {
        this.properties = properties;
        this.decoderFactory = decoderFactory == null ? this::buildRemoteDecoder : decoderFactory;
    }

    @Override
    public VerifiedIdentity verify(SocialProvider provider, String credential) {
        if (provider == null) {
            throw SocialCredentialException.invalid("Fournisseur d'identité manquant");
        }
        if (!isConfigured(provider)) {
            throw SocialCredentialException.notConfigured(provider.wireName());
        }
        if (credential == null || credential.isBlank()) {
            throw SocialCredentialException.invalid("Credential absent");
        }
        if (credential.length() > 8192) {
            // Garde-fou de volume : un « credential » légitime fait ~1-2 Ko.
            throw SocialCredentialException.invalid("Credential trop volumineux");
        }

        Jwt jwt = decode(provider, credential);
        return provider == SocialProvider.GOOGLE
                ? toGoogleIdentity(jwt)
                : toMicrosoftIdentity(jwt);
    }

    private Jwt decode(SocialProvider provider, String credential) {
        try {
            JwtDecoder decoder = decoders.computeIfAbsent(provider, decoderFactory);
            return decoder.decode(credential);
        } catch (JwtException | IllegalArgumentException failure) {
            // Le message du fournisseur peut contenir des détails sur la clé :
            // on journalise le type d'erreur, jamais le credential.
            log.warn("Credential {} rejeté : {}", provider.wireName(), failure.getClass().getSimpleName());
            throw SocialCredentialException.rejected("Credential rejeté par le fournisseur");
        }
    }

    // ------------------------------------------------------------------
    // Google
    // ------------------------------------------------------------------

    private VerifiedIdentity toGoogleIdentity(Jwt jwt) {
        requireIssuer(jwt, GOOGLE_ISSUERS);
        requireAudience(jwt, properties.getGoogle().acceptedAudiences());

        String email = value(jwt, "email");
        Boolean emailVerified = claimAsBoolean(jwt, "email_verified");
        if (properties.isRequireVerifiedEmail() && !Boolean.TRUE.equals(emailVerified)) {
            throw SocialCredentialException.forbidden("SOCIAL_EMAIL_NOT_VERIFIED",
                    "Google n'a pas confirmé que cette adresse est vérifiée");
        }
        if (email.isBlank()) {
            throw SocialCredentialException.forbidden("SOCIAL_EMAIL_MISSING",
                    "Le compte Google ne fournit pas d'adresse email");
        }

        return new VerifiedIdentity(
                SocialProvider.GOOGLE,
                jwt.getSubject(),
                email,
                Boolean.TRUE.equals(emailVerified),
                value(jwt, "name"),
                value(jwt, "picture"));
    }

    // ------------------------------------------------------------------
    // Microsoft
    // ------------------------------------------------------------------

    private VerifiedIdentity toMicrosoftIdentity(Jwt jwt) {
        SocialAuthProperties.Microsoft config = properties.getMicrosoft();
        String tid = value(jwt, "tid");
        String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();

        // L'émetteur DOIT porter le tenant du jeton. Sans cette cohérence, un
        // jeton émis par un tenant A pourrait être accepté alors qu'on n'attend
        // que le tenant B (défaut classique des configurations `common`).
        if (tid.isBlank()) {
            throw SocialCredentialException.rejected("Jeton Microsoft sans claim tid");
        }
        if (!issuer.equals(MICROSOFT_ISSUER_PREFIX + tid + "/v2.0")) {
            log.warn("Jeton Microsoft : émetteur {} incohérent avec tid {}", issuer, tid);
            throw SocialCredentialException.rejected("Émetteur Microsoft incohérent");
        }

        // Tenants acceptés = tenant principal + liste blanche (qui s'y AJOUTE,
        // elle ne le remplace pas : sinon renseigner un partenaire désactiverait
        // silencieusement l'accès à son propre tenant).
        List<String> allowed = config.getAllowedTenantIds();
        boolean matchesPrimaryTenant = !"common".equalsIgnoreCase(config.getTenantId())
                && tid.equalsIgnoreCase(config.getTenantId().trim());
        boolean matchesAllowList = allowed != null && !allowed.isEmpty()
                && allowed.stream().anyMatch(candidate -> candidate != null && candidate.equalsIgnoreCase(tid));

        if (!matchesPrimaryTenant && !matchesAllowList) {
            if ("common".equalsIgnoreCase(config.getTenantId()) && (allowed == null || allowed.isEmpty())) {
                // `common` sans liste blanche = tous les tenants acceptés : choix
                // explicite du produit, signalé en jaune par l'audit de démarrage.
                log.debug("Jeton Microsoft : tenant {} accepté (configuration common)", tid);
            } else {
                throw SocialCredentialException.forbidden("SOCIAL_TENANT_NOT_ALLOWED",
                        "Votre organisation Microsoft n'est pas autorisée sur cette plateforme");
            }
        }

        requireAudience(jwt, Set.of(config.getClientId()));

        Boolean emailVerified = claimAsBoolean(jwt, "email_verified");
        if (Boolean.FALSE.equals(emailVerified)) {
            throw SocialCredentialException.forbidden("SOCIAL_EMAIL_NOT_VERIFIED",
                    "Microsoft signale cette adresse comme non vérifiée");
        }

        String email = value(jwt, "email");
        if (email.isBlank()) {
            // Absent sur certains comptes personnels : seule source restante.
            // Autorisée UNIQUEMENT si le fournisseur n'a pas dit "non vérifié".
            String preferred = value(jwt, "preferred_username");
            if (preferred.contains("@")) {
                email = preferred;
            }
        }
        if (email.isBlank()) {
            throw SocialCredentialException.forbidden("SOCIAL_EMAIL_MISSING",
                    "Le compte Microsoft ne fournit pas d'adresse email");
        }

        return new VerifiedIdentity(
                SocialProvider.MICROSOFT,
                jwt.getSubject(),
                email,
                !Boolean.FALSE.equals(emailVerified),
                firstNonBlank(value(jwt, "name"), value(jwt, "preferred_username")),
                value(jwt, "picture"));
    }

    // ------------------------------------------------------------------
    // Contrôles de claims communs
    // ------------------------------------------------------------------

    private void requireIssuer(Jwt jwt, Set<String> accepted) {
        String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
        if (!accepted.contains(issuer)) {
            log.warn("Émetteur inattendu : {}", issuer);
            throw SocialCredentialException.rejected("Émetteur inattendu");
        }
    }

    /**
     * Le claim {@code aud} doit contenir l'un des clients enregistrés.
     *
     * <p>Point non négociable : le client ID est DIFFÉRENT entre le web, Android
     * et iOS. N'accepter qu'un seul identifiant reviendrait à interdire le
     * reconnectement depuis l'application mobile.
     */
    private void requireAudience(Jwt jwt, Set<String> acceptedAudiences) {
        if (acceptedAudiences.isEmpty()) {
            throw SocialCredentialException.notConfigured("client");
        }
        List<String> audience = jwt.getAudience();
        if (audience == null || audience.isEmpty()
                || audience.stream().noneMatch(acceptedAudiences::contains)) {
            throw SocialCredentialException.rejected("Audience inattendue");
        }
    }

    private String value(Jwt jwt, String claim) {
        Object raw = jwt.getClaims().get(claim);
        return raw == null ? "" : String.valueOf(raw).trim();
    }

    private Boolean claimAsBoolean(Jwt jwt, String claim) {
        Object raw = jwt.getClaims().get(claim);
        if (raw instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (raw instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first.isBlank() ? second : first;
    }

    private boolean isConfigured(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> properties.isGoogleActive();
            case MICROSOFT -> properties.isMicrosoftActive();
        };
    }

    /**
     * Construit un décodeur branché sur le JWKS du fournisseur.
     *
     * <p>Le cache de {@code RemoteJWKSet} (Nimbus) gère la rotation des clés
     * et évite un appel réseau par connexion : c'est ce qui rend la vérification
     * locale possible et donc illimitée.
     */
    private JwtDecoder buildRemoteDecoder(SocialProvider provider) {
        String jwksUri = switch (provider) {
            case GOOGLE -> GOOGLE_JWKS_URI;
            case MICROSOFT -> microsoftJwksUri(properties.getMicrosoft().getTenantId());
        };
        log.info("Vérification {} : JWKS {}", provider.wireName(), jwksUri);
        return NimbusJwtDecoder.withJwkSetUri(jwksUri)
                // RS256 uniquement : refuse un `alg: none` ou une bascule vers
                // un algorithme symétrique, qui serait une trappe classique.
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    static String microsoftJwksUri(String tenantId) {
        String tenant = tenantId == null || tenantId.isBlank() ? "common" : tenantId.trim();
        if ("common".equalsIgnoreCase(tenant)) {
            return MICROSOFT_ISSUER_PREFIX + "common/v2.0/keys";
        }
        return MICROSOFT_ISSUER_PREFIX + tenant + "/discovery/v2.0/keys";
    }

    /** Exposé pour les tests : configuration effective par fournisseur. */
    Map<SocialProvider, Boolean> activeProviders() {
        return Map.of(
                SocialProvider.GOOGLE, properties.isGoogleActive(),
                SocialProvider.MICROSOFT, properties.isMicrosoftActive());
    }
}
