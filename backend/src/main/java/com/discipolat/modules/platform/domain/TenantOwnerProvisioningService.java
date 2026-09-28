package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Constat B3 — tout tenant provisionné doit avoir un **propriétaire** (owner),
 * sinon personne ne peut administrer l'église.
 *
 * <p>Avant ce correctif, {@code POST /api/v1/platform/admin/provisioning}
 * créait l'église, le département et la famille… mais **aucun compte
 * administrateur**. Le tenant était créé, immédiatement vide, et le super admin
 * devait aller creer un compte « à la main » — chemin que rien n'automatisait ni
 * ne documentait.
 *
 * <p><b>Fail-closed</b> : un tenant sans owner ne doit pas exister. La validation
 * de la présence de l'owner est faite <b>en tête</b> de
 * {@link PlatformProvisioningService#provision}, donc <b>avant toute écriture</b>.
 *
 * <p><b>Sécurité du mot de passe</b> : le compte owner est créé en
 * {@code PENDING_ACTIVATION} avec un mot de passe aléatoire de 32 caractères
 * (BCrypt, {@link SecureRandom}) qui n'est <b>jamais</b> communiqué ni stocké en
 * clair. Le propriétaire définit son propre mot de passe via le lien
 * d'activation, ce qui évite d'avoir à transporter un mot de passe par un canal
 * non maîtrisé.
 */
@Service
public class TenantOwnerProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(TenantOwnerProvisioningService.class);

    /** Longueur du mot de passe aléatoire initial (non communiqué). */
    static final int INITIAL_PASSWORD_LENGTH = 32;

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public TenantOwnerProvisioningService(UserRepository userRepository,
                                          TenantMembershipRepository membershipRepository,
                                          AuthService authService,
                                          PasswordEncoder passwordEncoder,
                                          AuditService auditService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    /**
     * Résultat du provisionnement du propriétaire.
     *
     * @param userId              identifiant du compte owner
     * @param email               email du owner
     * @param activationEmailSent l'email d'activation a-t-il pu être envoyé ?
     *                           (peut valoir {@code false} si SMTP n'est pas
     *                           configuré : l'admin devra transmettre le lien)
     * @param alreadyMember       le compte existait-il déjà comme membre de ce tenant ?
     */
    public record OwnerProvisioningResult(
            UUID userId,
            String email,
            boolean activationEmailSent,
            boolean alreadyMember) {
    }

    /**
     * Crée (ou rattache) le propriétaire d'un tenant.
     *
     * @throws DomainException 409 {@code OWNER_EMAIL_ALREADY_USED} si l'email est
     *                         déjà porté par un compte d'un **autre** tenant
     *                         (l'unicité email est globale depuis V185)
     */
    @Transactional
    public OwnerProvisioningResult provisionOwner(UUID tenantId,
                                                 String email,
                                                 String firstName,
                                                 String lastName,
                                                 UUID actorId) {
        if (tenantId == null) {
            throw new DomainException("tenantId est requis", HttpStatus.BAD_REQUEST, "TENANT_REQUIRED");
        }
        if (email == null || email.isBlank()) {
            throw new DomainException(
                    "L'email du propriétaire de l'église est requis",
                    HttpStatus.BAD_REQUEST, "OWNER_REQUIRED");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Optional<User> existing = userRepository.findGlobalByEmailIgnoreCase(normalizedEmail);
        if (existing.isPresent()) {
            User user = existing.get();
            boolean sameTenant = tenantId.equals(user.getTenantId());
            if (!sameTenant) {
                // L'unicité email est GLOBALE (V185) : on ne peut pas créer un
                // second compte pour cette adresse dans une autre église. Refus
                // explicite AVANT toute écriture, sans création partielle.
                throw new DomainException(
                        "Cet email est déjà utilisé par un compte d'une autre église. "
                                + "Invitez cette personne à votre église au lieu de la provisionner comme owner.",
                        HttpStatus.CONFLICT, "OWNER_EMAIL_ALREADY_USED",
                        Map.of("email", normalizedEmail));
            }
            boolean alreadyMember = ensureOwnerMembership(tenantId, user.getId());
            boolean activationEmailSent = sendActivationEmailQuietly(user.getId());
            return new OwnerProvisioningResult(user.getId(), user.getEmail(), activationEmailSent, alreadyMember);
        }

        String firstNameNormalized = firstName == null ? null : firstName.trim();
        String lastNameNormalized = lastName == null ? null : lastName.trim();

        User owner = User.builder()
                .tenantId(tenantId)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(randomInitialPassword()))
                .firstName(firstNameNormalized)
                .lastName(lastNameNormalized)
                .role(UserRole.PASTEUR)
                .roles(java.util.Set.of(UserRole.PASTEUR))
                .activeRole(UserRole.PASTEUR)
                // PENDING_ACTIVATION : le compte n'est utilisable qu'après avoir
                // défini son propre mot de passe via le lien reçu.
                .statut(UserStatus.PENDING_ACTIVATION)
                .build();
        owner = userRepository.save(owner);

        ensureOwnerMembership(tenantId, owner.getId());
        auditService.log(actorId, tenantId, "TENANT_OWNER_PROVISIONED", "USER", owner.getId(),
                "SUCCESS", Map.of("email", owner.getEmail()), null, null, null);

        // Réutilise le flux d'activation EXISTANT (AuthService.sendActivationEmail)
        // au lieu de le réimplémenter : un seul générateur de token, une seule
        // durée de validité, une seule source de vérité.
        boolean activationEmailSent = sendActivationEmailQuietly(owner.getId());
        return new OwnerProvisioningResult(owner.getId(), owner.getEmail(), activationEmailSent, false);
    }

    /**
     * Envoie l'email d'activation sans jamais faire échouer le provisionnement
     * (décision D10) : un SMTP absent donne {@code activationEmailSent = false},
     * que le client sait afficher (« transmettez le lien manuellement »).
     */
    private boolean sendActivationEmailQuietly(UUID userId) {
        try {
            authService.sendActivationEmail(userId);
            return true;
        } catch (RuntimeException failure) {
            log.warn("Email d'activation du propriétaire non envoyé (userId={}) : {}", userId, failure.getMessage());
            return false;
        }
    }

    private boolean ensureOwnerMembership(UUID tenantId, UUID userId) {
        boolean alreadyMember = membershipRepository
                .findAllByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE)
                .stream()
                .anyMatch(membership -> membership.getScopeType() == MembershipScopeType.TENANT);
        if (alreadyMember) {
            return true;
        }
        membershipRepository.save(TenantMembership.builder()
                .tenantId(tenantId)
                .userId(userId)
                .roleLegacy("TENANT_OWNER")
                .scopeType(MembershipScopeType.TENANT)
                .status(MembershipStatus.ACTIVE)
                .build());
        return false;
    }

    /**
     * Mot de passe initial aléatoire et fort. Il n'est jamais communiqué : le
     * propriétaire le définit via le lien d'activation.
     */
    String randomInitialPassword() {
        StringBuilder password = new StringBuilder(INITIAL_PASSWORD_LENGTH);
        for (int i = 0; i < INITIAL_PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(secureRandom.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }
}
