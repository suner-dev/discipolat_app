package com.discipolat.modules.authentication.domain;

/**
 * Fournisseurs d'identité externe supportés.
 *
 * <p><b>Volontairement restreint à deux fournisseurs, tous deux gratuits et
 * sans plafond</b> (aucun coût par utilisateur, aucune vérification de jeton
 * facturée) :
 * <ul>
 *   <li>{@link #GOOGLE} — Google Identity Services : validation d'{@code id_token}
 *       par les clés publiques (JWKS), donc <b>aucun appel réseau et aucun quota
 *       Google</b> côté serveur.</li>
 *   <li>{@link #MICROSOFT} — Microsoft Entra ID : même mécanisme OIDC/JWKS.</li>
 * </ul>
 *
 * <p><b>Apple et le téléphone ne sont pas Volontairement absents par oubli,
 * mais par décision :</b> Apple exige un compte développeur payant (99 $/an) et
 * l'authentification par SMS est facturée par message. Ils pourront être ajoutés
 * plus tard dans cette même enum sans changement d'architecture — la
 * vérification passe par {@code SocialIdentityVerifier}, qui est déjà
 * piloté par fournisseur.
 *
 * <p>La valeur {@code enum} est stockée en base (contrainte
 * {@code ck_user_identities_provider}) : ajouter un fournisseur exige donc une
 * migration qui étend cette contrainte.
 */
public enum SocialProvider {

    /** Google (scopes {@code email} + {@code profile}, non sensibles). */
    GOOGLE("google"),

    /** Microsoft Entra ID (tenant de travail ou compte personnel). */
    MICROSOFT("microsoft"),

    /**
     * Facebook (Facebook Login, OpenID Connect).
     *
     * <p>Gratuit et sans plafond comme les deux autres, mais avec une
     * <b>contrainte de déploiement propre</b> : une application Meta récente
     * n'obtient {@code email} et {@code public_profile} qu'en
     * <b>Standard Access</b>, c'est-à-dire pour les personnes qui ont un rôle
     * sur l'application (développeurs, testeurs). Pour servir de vrais membres,
     * il faut un <b>Advanced Access</b> : Business Verification + revue Meta
     * (captures d'écran, et au moins un appel API réussi dans les 30 jours).
     *
     * <p>Cette contrainte est <b>documentée, pas contournée</b> : le code est
     *.fail-closed, donc tant que la revue n'est pas passée, seuls les comptes
     * ayant un rôle sur l'application peuvent se connecter — et l'interface
     * affiche un message honnête plutôt qu'un échec opaque.
     */
    FACEBOOK("facebook");

    private final String wireName;

    SocialProvider(String wireName) {
        this.wireName = wireName;
    }

    /** Nom utilisé dans les URL et les corps JSON de l'API. */
    public String wireName() {
        return wireName;
    }

    /**
     * Résout un nom transmis par le client (URL ou JSON) vers un fournisseur.
     *
     * @param value nom du fournisseur, insensible à la casse
     * @return le fournisseur correspondant
     * @throws IllegalArgumentException si le fournisseur n'existe pas — évite
     *         qu'un appel en base ait lieu pour une valeur inventée
     */
    public static SocialProvider fromWireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fournisseur d'identité manquant");
        }
        String candidate = value.trim().toLowerCase();
        for (SocialProvider provider : values()) {
            if (provider.wireName.equals(candidate) || provider.name().toLowerCase().equals(candidate)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Fournisseur d'identité non supporté : " + value);
    }
}
