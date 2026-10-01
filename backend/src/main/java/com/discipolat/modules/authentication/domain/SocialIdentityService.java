package com.discipolat.modules.authentication.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserIdentity;
import com.discipolat.modules.users.domain.UserStatus;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.repository.UserIdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Point de decision unique de l'authentification par identite externe.
 *
 * <h2>Regle invariante du produit</h2>
 * Un credential externe prouve <b>qui</b> la personne est. Il n'accorde
 * <b>jamais</b> un role ni une eglise. Concretement, ces trois entrees sont
 * possibles et la troisieme est interdite :
 * <ol>
 *   <li><b>Authentifier</b> un compte existant ({@link #login}) ;</li>
 *   <li><b>Rattacher</b> une identite a un compte deja connecte
 *       ({@link #linkToCurrentUser}) ;</li>
 *   <li><b>Accepter une invitation</b> — le role ET le tenant proviennent alors
 *       de l'invitation, jamais du fournisseur ({@code InvitationService}).</li>
 * </ol>
 * Il n'existe volontairement <b>aucun</b> endpoint « creer un compte a partir
 * d'un credential social » : c'est exactement le defaut de l'ancien
 * {@code SocialAuthController}, qui creait un utilisateur ACTIF sans tenant
 * (impossible : {@code users.tenant_id} est NOT NULL) et sans passer par
 * l'approbation Super Admin.
 *
 * <h2>Resolution d'un compte, dans cet ordre</h2>
 * <ol>
 *   <li><b>Identite deja liee</b> ({@code provider + subject}) : c'est la voie
 *       normale et la seule qui survive a Apple, qui ne redonne pas l'email.</li>
 *   <li><b>Email connu</b> et identique a un compte existant : premiere
 *       connexion — on rattache automatiquement, puis on journalise.</li>
 *   <li><b>Aucun compte</b> : refus 403. Le message invite a utiliser le lien
 *       d'invitation. Aucun compte n'est cree hors invitation.</li>
 * </ol>
 *
 * <p>La resolution par email (etape 2) est volontairement restreinte au cas
 * « aucune identite du tout pour ce compte » : si un compte a deja une identite
 * Google, un credential Google portant le meme email mais un {@code subject}
 * different ne peut pas s'y greffer — cela ouvrirait une prise de contrôle via
 * un compte Google homonyme reutilise.
 */
@Service
public class SocialIdentityService {

    private static final Logger log = LoggerFactory.getLogger(SocialIdentityService.class);

    private final SocialIdentityVerifier verifier;
    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;
    private final AuthService authService;

    public SocialIdentityService(SocialIdentityVerifier verifier,
                                UserRepository userRepository,
                                UserIdentityRepository identityRepository,
                                AuthService authService) {
        this.verifier = verifier;
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.authService = authService;
    }

    /**
     * Connexion d'un compte deja connu, par identite externe.
     *
     * @throws SocialIdentityVerifier.SocialCredentialException credential invalide
     *         ou compte inexistant (403 {@code SOCIAL_ACCOUNT_NOT_LINKED})
     */
    @Transactional
    public SocialLoginResult login(SocialProvider provider, String credential) {
        SocialIdentityVerifier.VerifiedIdentity verified = verifier.verify(provider, credential);
        User user = resolveAccount(verified);

        identityRepository.findByProviderAndSubject(provider, verified.subject())
                .ifPresent(identity -> {
                    identity.touchLogin();
                    identityRepository.save(identity);
                });

        AuthService.AuthResult session = authService.issueSession(user);
        log.info("Connexion {} réussie pour {} (userId={})",
                provider.wireName(), user.getEmail(), user.getId());

        return new SocialLoginResult(session, provider);
    }

    /**
     * Rattache une identite externe au compte de l'utilisateur CONNECTE.
     *
     * <p>Trois conditions cumulatives, toutes vérifiées cote serveur :
     * <ol>
     *   <li>session authentifiee (implicite : le endpoint est protege) ;</li>
     *   <li>credential externe re-verifie par {@link #verifier} ;</li>
     *   <li><b>email verifie identique</b> a celui du compte connecte — sans ce
     *       controle, un compte Google tiers pourrait revendiquer le compte
     *       Discipolat d'autrui ;</li>
     * </ol>
     * Le Super Admin (cree par bootstrap, sans mot de passe connu) passe par
     * ce point d'entree pour pouvoir se connecter par Google.
     */
    @Transactional
    public SocialLinkResult linkToCurrentUser(UUID currentUserId,
                                              SocialProvider provider,
                                              String credential,
                                              boolean linkingAllowed) {
        if (!linkingAllowed) {
            throw new DomainException(
                    "Le rattachement d'identite externe est desactive sur ce serveur",
                    HttpStatus.FORBIDDEN, "SOCIAL_LINKING_DISABLED");
        }

        SocialIdentityVerifier.VerifiedIdentity verified = verifier.verify(provider, credential);
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new DomainException(
                        "Compte introuvable", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (!verified.matchesEmail(user.getEmail())) {
            // Aucun détail sur l'email attendu : un attaquant ne doit pas pouvoir
            // tester une adresse en lisant la reponse.
            log.warn("Rattachement {} refuse : email du credential different du compte", provider.wireName());
            throw new DomainException(
                    "L'adresse de ce compte externe ne correspond pas a votre compte Discipolat",
                    HttpStatus.FORBIDDEN, "SOCIAL_EMAIL_MISMATCH");
        }

        Optional<UserIdentity> existingBySubject =
                identityRepository.findByProviderAndSubject(provider, verified.subject());
        if (existingBySubject.isPresent()) {
            UUID ownerId = existingBySubject.get().getUserId();
            if (!ownerId.equals(user.getId())) {
                log.warn("Rattachement {} refuse : identite deja liee a un autre compte", provider.wireName());
                throw new DomainException(
                        "Cette identite est deja rattachee a un autre compte",
                        HttpStatus.CONFLICT, "SOCIAL_IDENTITY_ALREADY_LINKED");
            }
            existingBySubject.get().touchLogin();
            identityRepository.save(existingBySubject.get());
            return new SocialLinkResult(provider, false);
        }

        UserIdentity identity = UserIdentity.builder()
                .userId(user.getId())
                .provider(provider)
                .subject(verified.subject())
                .emailAtLink(verified.email())
                .pictureUrl(verified.pictureUrl())
                .build();
        identityRepository.save(identity);
        log.info("Identite {} rattachee au compte {}", provider.wireName(), user.getId());

        return new SocialLinkResult(provider, true);
    }

    /**
     * Verifie un credential destined a l'acceptation d'une invitation.
     *
     * <p>Le controle decisif est ici : l'email verifie par le fournisseur doit
     * etre <b>exactement</b> celui de l'invitation. Sans lui, un tiers pourrait
     * accepter une invitation envoyee a quelqu'un d'autre en utilisant SON
     * propre compte (et-verrou sur l'adresse de la victime).
     *
     * @param expectedEmail email porte par l'invitation
     * @return l'identite verifiee, prete pour la creation du compte
     */
    public SocialIdentityVerifier.VerifiedIdentity verifyForInvitation(SocialProvider provider,
                                               String credential,
                                               String expectedEmail) {
        SocialIdentityVerifier.VerifiedIdentity verified = verifier.verify(provider, credential);
        if (!verified.matchesEmail(expectedEmail)) {
            log.warn("Acceptation d'invitation refusee : email du credential different de celui de l'invitation");
            throw new DomainException(
                    "L'identite verifiee ne correspond pas a l'adresse invit\u00e9e",
                    HttpStatus.FORBIDDEN, "SOCIAL_EMAIL_MISMATCH");
        }
        return verified;
    }

    /**
     * Resolution d'un compte a partir d'une identite verifiee, sans creation.
     */
    private User resolveAccount(SocialIdentityVerifier.VerifiedIdentity verified) {
        Optional<UserIdentity> known = identityRepository
                .findByProviderAndSubject(verified.provider(), verified.subject());
        if (known.isPresent()) {
            return userRepository.findById(known.get().getUserId())
                    .orElseThrow(() -> new DomainException(
                            "Identite rattachee a un compte supprime",
                            HttpStatus.GONE, "SOCIAL_IDENTITY_ORPHANED"));
        }

        Optional<User> byEmail = userRepository.findGlobalByEmailIgnoreCase(verified.email());
        if (byEmail.isEmpty()) {
            // 403 et non 404 : la distinction "compte inconnu" / "credential
            // invalide" est deja visible par le code HTTP, on ne la ferme pas.
            throw new DomainException(
                    "Aucun compte Discipolat pour cette adresse. Utilisez le lien d'invitation recu de votre eglise.",
                    HttpStatus.FORBIDDEN, "SOCIAL_ACCOUNT_NOT_LINKED");
        }

        User user = byEmail.get();
        if (user.getStatut() == UserStatus.PENDING_ACTIVATION) {
            throw new DomainException(
                    "Compte en attente d'activation. Verifiez vos emails.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVATED");
        }
        if (user.getStatut() == UserStatus.INACTIVE) {
            throw new DomainException(
                    "Compte desactive. Contactez l'administrateur de votre eglise.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_INACTIVE");
        }
        if (user.isAccountLocked()) {
            throw new DomainException(
                    "Compte temporairement verrouille apres plusieurs tentatives echouees.",
                    HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED");
        }

        boolean alreadyHasThisProvider =
                identityRepository.existsByUserIdAndProvider(user.getId(), verified.provider());
        if (alreadyHasThisProvider) {
            // Le compte est deja rattache a une AUTRE identite du meme
            // fournisseur : on refuse plutot que de rattacher a l'aveugle.
            log.warn("Connexion {} refusee : le compte a deja une identite de ce fournisseur (email {})",
                    verified.provider().wireName(), user.getEmail());
            throw new DomainException(
                    "Ce compte est deja rattache a une autre adresse "
                            + verified.provider().wireName(),
                    HttpStatus.CONFLICT, "SOCIAL_PROVIDER_ALREADY_LINKED");
        }

        // Premiere connexion de cette identite sur un compte existant : rattachement
        // automatique, possible uniquement parce que l'email verifie correspond.
        UserIdentity identity = UserIdentity.builder()
                .userId(user.getId())
                .provider(verified.provider())
                .subject(verified.subject())
                .emailAtLink(verified.email())
                .pictureUrl(verified.pictureUrl())
                .build();
        identityRepository.save(identity);
        log.info("Identite {} rattachee automatiquement a un compte existant (userId={})",
                verified.provider().wireName(), user.getId());

        return user;
    }

    /** Identites rattachees au compte : pour l'ecran « mon compte ». */
    @Transactional(readOnly = true)
    public List<UserIdentity> identitiesOf(UUID userId) {
        return identityRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    /** Resultat d'une connexion sociale. */
    public record SocialLoginResult(AuthService.AuthResult session, SocialProvider provider) {
    }

    /** Resultat d'un rattachement. */
    public record SocialLinkResult(SocialProvider provider, boolean created) {
    }
}
