package com.discipolat.modules.authentication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Configuration de l'authentification par identité externe (Google, Microsoft).
 *
 * <p><b>Noms de propriétés volontairement sans tirets.</b> Spring lie
 * {@code app.auth.social.google.web-client-id} à la variable
 * {@code APP_AUTH_SOCIAL_GOOGLE_WEB_CLIENT_ID}, mais <b>ne lie pas</b>
 * {@code app.auth.social.google.webClientId}… et surtout pas la variante à
 * tirets {@code app.auth.social.google.web-client-id} depuis
 * {@code APP_AUTH_SOCIAL_GOOGLE_WEB_CLIENT_ID} : le binding « relaché »
 * (−/\_ uniformisé) ne rapproche pas la forme segmentée de la forme à tirets.
 * Concrètement, un tiret dans une propriété rend la variable d'environnement
 * <b>silencieusement ignorée</b> — ce qui se traduit par un bouton de
 * connexion qui ne fonctionne jamais. D'où la règle : ici, uniquement des
 * segments en {@code lowercase.snake}.
 *
 * <p><b>Désactivé par défaut et fail-closed.</b> Un fournisseur n'est actif que
 * si {@code enabled=true} ET qu'au moins un identifiant client est fourni. Un
 * endpoint de fournisseur inactif répond 503 plutôt que d'accepter ou de créer
 * un compte : mieux vaut une erreur visible qu'une authentification qui dérive.
 *
 * <p><b>Aucun secret ici.</b> Ces identifiants clients sont publics par nature
 * (ils sont embarqués dans le JavaScript du navigateur) ; ce ne sont pas des
 * secrets. La confiance ne repose pas dessus : elle repose sur la validation de
 * la signature du jeton par les clés publiques du fournisseur. Un secret client
 * Microsoft ou la clé privée Apple n'ont donc AUCUNE raison d'être ici.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.auth.social")
public class SocialAuthProperties {

    /** Google Identity Services (gratuit, sans plafond). */
    private Google google = new Google();

    /** Microsoft Entra ID (gratuit jusqu'à ~50 000 utilisateurs/mois). */
    private Microsoft microsoft = new Microsoft();

    /** Facebook Login (OIDC) — gratuit, mais revue Meta requise (cf. SocialProvider). */
    private Facebook facebook = new Facebook();

    /**
     * Permet de rattacher une identité externe à un compte DÉJÀ connecté
     * ({@code POST /api/v1/auth/social/link}).
     *
     * <p>Sans ce point d'entrée, un compte créé par mot de passe ne pourrait
     * jamais utiliser « Se connecter avec Google » — et le Super Admin, dont le
     * compte naît d'un bootstrap par variable d'environnement, n'y aurait aucun
     * accès. Le rattachement est soumis à trois conditions cumulatives :
     * session authentifiée, credential re-vérifié côté serveur, et email vérifié
     * identique à celui du compte connecté.
     */
    private boolean allowAccountLinking = true;

    /**
     * Refuse toute identité dont l'email n'est pas marqué vérifié par le
     * fournisseur. Par défaut {@code true} : sans cela, une adresse non vérifiée
     * pourrait être utilisée pour s'attribuer une invitation.
     *
     * <p>Note Microsoft : Microsoft Entra peut renvoyer {@code email_verified}
     * absent pour certains tenants (comptes personnels). Ce refus est alors
     * explicite et le client reçoit un 403 documenté plutôt qu'un échec muet —
     * c'est un choix de sécurité assumé, pas un bug.
     */
    private boolean requireVerifiedEmail = true;

    @Getter
    @Setter
    public static class Google {

        /** Autorise explicitement le fournisseur même si aucun client-id n'est fourni. */
        private boolean enabled = false;

        /** Client ID OAuth de type « Web » (frontend / PWA). */
        private String webClientId = "";

        /** Client ID OAuth de type « Android » (application mobile Flutter). */
        private String androidClientId = "";

        /** Client ID OAuth de type « iOS » (application mobile Flutter). */
        private String iosClientId = "";

        /**
         * Le client ID est dans le claim {@code aud} du jeton, et il est
         * DIFFÉRENT selon la plateforme. Un backend configuré pour le web refuse
         * donc le jeton émis pour l'app Android : c'est la raison principale pour
         * laquelle les trois identifiants sont acceptés ensemble.
         */
        public Set<String> acceptedAudiences() {
            Set<String> audiences = new LinkedHashSet<>();
            addIfPresent(audiences, webClientId);
            addIfPresent(audiences, androidClientId);
            addIfPresent(audiences, iosClientId);
            return audiences;
        }

        /** Le fournisseur est-il réellement utilisable ? */
        public boolean isConfigured() {
            return enabled && !acceptedAudiences().isEmpty();
        }

        /** Actif mais sans identifiant : configuration incomplète à corriger. */
        public boolean isMisconfigured() {
            return enabled && acceptedAudiences().isEmpty();
        }
    }

    @Getter
    @Setter
    public static class Microsoft {

        private boolean enabled = false;

        /** Client ID de l'enregistrement d'application Entra (portail Azure). */
        private String clientId = "";

        /**
         * Tenant attendu. Valeur par défaut {@code common} = l'application
         * accepte n'importe quel tenant Microsoft.
         *
         * <p><b>Conséquence de sécurité à assumer :</b> avec {@code common},
         * une organisation tierce — y compris extérieure à votre plateforme —
         * peut connecter ses utilisateurs. Renseignez l'ID de votre tenant pour
         * restreindre, et/ou utilisez {@link #allowedTenantIds}.
         */
        private String tenantId = "common";

        /**
         * Liste blanche de tenants (claim {@code tid}). Vide = pas de
         * restriction supplémentaire. Utile pour accepter votre tenant ET des
         * organisations partenaires identifiées.
         */
        private List<String> allowedTenantIds = new ArrayList<>();

        public boolean isConfigured() {
            return enabled && clientId != null && !clientId.isBlank()
                    && tenantId != null && !tenantId.isBlank();
        }

        public boolean isMisconfigured() {
            return enabled && (clientId == null || clientId.isBlank());
        }
    }

    /** Google est-il prêt à servir du trafic ? */
    public boolean isGoogleActive() {
        return google.isConfigured();
    }

    /** Microsoft est-il prêt à servir du trafic ? */
    public boolean isMicrosoftActive() {
        return microsoft.isConfigured();
    }

    /** Facebook est-il prêt à servir du trafic ? */
    public boolean isFacebookActive() {
        return facebook.isConfigured();
    }

    @Getter
    @Setter
    public static class Facebook {

        private boolean enabled = false;

        /**
         * Identifiant de l'application Meta (App ID), qui joue le rôle de
         * {@code client_id} ET d'audience du {@code id_token}.
         *
         * <p>Unlike Google, il n'y a <b>qu'un seul</b> identifiant : la même
         * application Meta sert le web et le mobile, donc une seule audience.
         */
        private String appId = "";

        /** Version de l'API Graph utilisée dans les URL d'autorisation. */
        private String apiVersion = "v21.0";

        public boolean isConfigured() {
            return enabled && appId != null && !appId.isBlank();
        }

        public boolean isMisconfigured() {
            return enabled && (appId == null || appId.isBlank());
        }
    }

    private static void addIfPresent(Set<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }
}
