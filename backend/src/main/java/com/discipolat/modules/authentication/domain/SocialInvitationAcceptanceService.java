package com.discipolat.modules.authentication.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.tenants.domain.Invitation;
import com.discipolat.modules.tenants.domain.InvitationService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserIdentity;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.repository.UserIdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Acceptation d'une invitation en s'identifiant avec Google ou Microsoft.
 *
 * <p><b>Pourquoi ce service existe séparément de {@link InvitationService}</b> :
 * la création du compte, de l'adhésion, de l'inscription au répertoire et de
 * l'audit est <b>déjà correcte et testée</b> dans {@code accept(token, password…)}.
 * Plutôt que de dupliquer cette logique (donc de la faire dériver), on
 * <b>réutilise ce chemin tel quel</b> et on n'ajoute que ce qui est propre à
 * l'identité externe :
 * <ol>
 *   <li>vérifier le credential (JWKS) ;</li>
 *   <li>imposer que l'email vérifié soit celui de l'invitation ;</li>
 *   <li>fournir un mot de passe aléatoire <b>inconnu et jamais retourné</b> —
 *       la colonne {@code password_hash} est NOT NULL, et un hash BCrypt d'un
 *       secret aléatoire de 256 bits rend la connexion par mot de passe
 *       impossible sans être un changement de comportement pour le reste du
 *       produit ;</li>
 *   <li>rattacher l'identité au compte créé.</li>
 * </ol>
 *
 * <p><b>Le rôle et l'église proviennent TOUJOURS de l'invitation</b>, jamais du
 * fournisseur : un credential Google ne peut donner ni rôle, ni tenant, ni
 * accès à une église.
 *
 * <p><b>Mot de passe aléatoire :</b> si l'utilisateur souhaite à nouveau utiliser
 * un mot de passe, il passe par « mot de passe oublié » — le flux existant. Aucun
 * mot de passe n'est stocké en clair, journalisé ni transmis.
 */
@Service
public class SocialInvitationAcceptanceService {

    private static final Logger log = LoggerFactory.getLogger(SocialInvitationAcceptanceService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InvitationService invitationService;
    private final SocialIdentityService socialIdentityService;
    private final UserIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public SocialInvitationAcceptanceService(InvitationService invitationService,
                                             SocialIdentityService socialIdentityService,
                                             UserIdentityRepository identityRepository,
                                             UserRepository userRepository,
                                             AuthService authService) {
        this.invitationService = invitationService;
        this.socialIdentityService = socialIdentityService;
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * Accepte l'invitation en prouvant son identité par un fournisseur externe,
     * puis ouvre une session.
     *
     * @param token       token d'invitation (le même que celui du lien reçu par email)
     * @param provider    fournisseur ayant vérifié le credential
     * @param credential  jeton OIDC du client
     * @param firstName   prénom saisi (facultatif : sinon déduit du fournisseur)
     * @param lastName    nom saisi (facultatif)
     * @return la session ouverte et le résumé de l'adhésion
     */
    @Transactional
    public AcceptanceOutcome acceptWithIdentity(String token,
                                                SocialProvider provider,
                                                String credential,
                                                String firstName,
                                                String lastName) {

        // 1) L'invitation est relue pour connaître l'adresse attendue.
        //    `accept()` la revalidera sous verrou : cette lecture ne fait que
        //    vérifier qu'une invitation existe et n'est pas expirée.
        Invitation invitation = invitationService.validate(token);

        // 2) Vérification du credential + contrôle décisif : l'email vérifié par le
        //    fournisseur doit être celui de l'invitation.
        SocialIdentityVerifier.VerifiedIdentity verified =
                socialIdentityService.verifyForInvitation(provider, credential, invitation.getEmail());

        // 3) Garde-fou : si cette identité est déjà rattachée à un compte, ce
        //    compte doit être celui de l'adresse invitée. Sans ce contrôle, un
        //    compte Google antérieur pourrait « consommer » une invitation
        //    adressée à quelqu'un d'autre.
        Optional<UserIdentity> knownIdentity =
                identityRepository.findByProviderAndSubject(provider, verified.subject());
        if (knownIdentity.isPresent()) {
            UUID ownerId = knownIdentity.get().getUserId();
            Optional<User> invitedOwner = userRepository.findGlobalByEmailIgnoreCase(invitation.getEmail());
            if (invitedOwner.isEmpty() || !invitedOwner.get().getId().equals(ownerId)) {
                log.warn("Acceptation d'invitation refusee : identite {} deja liee a un autre compte",
                        provider.wireName());
                throw new DomainException(
                        "Cette identite est deja rattachee a un autre compte",
                        HttpStatus.CONFLICT, "SOCIAL_IDENTITY_ALREADY_LINKED");
            }
        }

        // 4) Prénom / nom : saisie prioritaire, sinon déduits du fournisseur.
        String[] deduced = verified.splitDisplayName();
        String resolvedFirstName = pick(firstName, deduced[0]);
        String resolvedLastName = pick(lastName, deduced[1]);

        // 5) Chemin d'acceptation EXISTANT : compte, adhésion, répertoire, audit,
        //    email de bienvenue. Un mot de passe aléatoire remplace la saisie.
        InvitationService.AcceptanceResult acceptance = invitationService.accept(
                token, generateUnusablePassword(), resolvedFirstName, resolvedLastName);

        // 6) Rattachement de l'identité au compte obtenu.
        boolean created = false;
        if (knownIdentity.isEmpty()) {
            identityRepository.save(UserIdentity.builder()
                    .userId(acceptance.userId())
                    .provider(provider)
                    .subject(verified.subject())
                    .emailAtLink(verified.email())
                    .pictureUrl(verified.pictureUrl())
                    .build());
            created = true;
        }

        User user = userRepository.findById(acceptance.userId())
                .orElseThrow(() -> new DomainException(
                        "Compte introuvable apres acceptation", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        AuthService.AuthResult session = authService.issueSession(user);
        log.info("Invitation acceptee via {} (userId={}, tenantId={}, nouveauCompte={})",
                provider.wireName(), acceptance.userId(), acceptance.tenantId(), created);

        return new AcceptanceOutcome(session, provider, created, acceptance);
    }

    /**
     * Secret aléatoire de 256 bits, haché en BCrypt puis jeté.
     *
     * <p>Un hash BCrypt d'un secret aléatoire est irrésoluble en pratique
     * (l'espace de recherche est le nombre de mots de passe possibles, pas
     * l'algorithme) : le compte ne peut pas être ouvert par mot de passe, et le
     * hash reste une valeur parfaitement normale pour le reste du code.
     */
    private String generateUnusablePassword() {
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
    }

    private String pick(String provided, String deduced) {
        if (provided != null && !provided.isBlank()) {
            return provided.trim();
        }
        return deduced == null ? "" : deduced;
    }

    /**
     * Resultat de l'acceptation par identite externe.
     *
     * @param session      session ouverte (access + refresh token)
     * @param provider     fournisseur utilisé
     * @param identityCreated l'identité a-t-elle été créée (false = déjà liée)
     * @param acceptance   résumé de l'adhésion, pour le message affiché
     */
    public record AcceptanceOutcome(AuthService.AuthResult session,
                                    SocialProvider provider,
                                    boolean identityCreated,
                                    InvitationService.AcceptanceResult acceptance) {
    }
}
