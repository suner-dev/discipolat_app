package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.people.domain.Person;
import com.discipolat.modules.people.repository.PersonRepository;
import com.discipolat.modules.people.service.PeopleService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvitationService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InvitationService.class);

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final OrganizationNodeRepository organizationNodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final TenantRepository tenantRepository;
    private final EmailService emailService;
    private final PeopleService peopleService;
    private final PersonRepository personRepository;
    private final String frontendUrl;

    public InvitationService(InvitationRepository invitationRepository,
                             UserRepository userRepository,
                             TenantMembershipRepository membershipRepository,
                             RoleRepository roleRepository,
                             OrganizationNodeRepository organizationNodeRepository,
                             PasswordEncoder passwordEncoder,
                             AuditService auditService,
                             TenantRepository tenantRepository,
                             EmailService emailService,
                             PeopleService peopleService,
                             PersonRepository personRepository,
                             @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.organizationNodeRepository = organizationNodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.tenantRepository = tenantRepository;
        this.emailService = emailService;
        this.peopleService = peopleService;
        this.personRepository = personRepository;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Constat M4 — l'acceptation d'une inscription inscrivant la personne au
     * <b>répertoire</b> de l'église.
     *
     * <p>Avant ce correctif, un membre invité n'apparaissait jamais dans le
     * répertoire : l'église avait un compte sans fiches personne, donc les
     * statistiques de suivi, les listes et les fiches étaient faux.
     *
     * <p><b>Jamais de doublon</b> : la recherche par {@code email_normalized}
     * précède systématiquement l'écriture. Si une fiche existe déjà, elle est
     * simplement réutilisée, sans appel à {@code PeopleService.registerPerson}
     * (qui leverait une {@code PersonAlreadyExistsException}).
     */
    private void registerInDirectory(Invitation invitation, User user, String firstName, String lastName) {
        String emailNormalized = user.getEmail() == null ? null : user.getEmail().trim().toLowerCase(Locale.ROOT);
        if (emailNormalized == null || emailNormalized.isBlank()) {
            return;
        }
        if (personRepository
                .findByTenantIdAndEmailNormalizedAndDeletedAtIsNull(invitation.getTenantId(), emailNormalized)
                .isPresent()) {
            return;
        }
        String resolvedFirstName = resolveFirstName(firstName, user, emailNormalized);
        String resolvedLastName = resolveLastName(lastName, user);
        Person person = Person.builder()
                .firstName(resolvedFirstName)
                .lastName(resolvedLastName)
                .emailNormalized(emailNormalized)
                .build();
        try {
            peopleService.registerPerson(invitation.getTenantId(), person, "INVITATION", invitation.getInviterId());
        } catch (RuntimeException directoryFailure) {
            // Un échec du répertoire ne doit pas faire échouer l'acceptation :
            // le compte et la membership sont déjà créés et valides.
            log.warn("Enregistrement au répertoire impossible pour {} : {}",
                    emailNormalized, directoryFailure.getMessage());
        }
    }

    private String resolveFirstName(String fromInvitation, User user, String emailNormalized) {
        if (fromInvitation != null && !fromInvitation.isBlank()) {
            return fromInvitation.trim();
        }
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName().trim();
        }
        int at = emailNormalized.indexOf('@');
        String localPart = at > 0 ? emailNormalized.substring(0, at) : emailNormalized;
        return localPart.isBlank() ? "Membre" : localPart;
    }

    private String resolveLastName(String fromInvitation, User user) {
        if (fromInvitation != null && !fromInvitation.isBlank()) {
            return fromInvitation.trim();
        }
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            return user.getLastName().trim();
        }
        return null;
    }

    // ==================================================================
    // Creation d'invitation — logique extraite de InvitationController (constat B2)
    // ==================================================================

    /**
     * Nature de l'opération réellement effectuée par {@link #createInvitation}.
     * Le contrôleur HTTP s'en sert pour répondre exactement comme avant
     * (aucun changement de comportement observable), et l'action d'onboarding
     * {@code ROLES} s'en sert pour journaliser ce qui a été fait.
     */
    public enum InvitationCreationKind {
        /** Le compte existe déjà dans ce tenant : une membership a été ajoutée. */
        DIRECT_MEMBERSHIP,
        /** Le compte existe dans une AUTRE église : invitation classique, l'utilisateur choisira son organisation. */
        CROSS_TENANT_INVITATION,
        /** Aucun compte : une invitation classique a été créée. */
        INVITATION
    }

    /** Résultat d'une création d'invitation, sans aucune dépendance à la couche HTTP. */
    public record InvitationCreationResult(
            InvitationCreationKind kind,
            UUID invitationId,
            UUID invitedUserId,
            String email,
            String role,
            String scopeType,
            UUID scopeId,
            String invitationToken,
            String invitationLink,
            boolean emailSent,
            boolean crossTenantIdentity,
            boolean requiresTenantSwitch) {
    }

    /**
     * Crée une invitation (ou rattache directement un compte existant).
     *
     * <p>Extraite du contrôleur pour être réutilisée à l'identique par le wizard
     * d'onboarding (étape {@code ROLES}) : une seule implémentation, donc aucune
     * divergence de comportement entre l'écran d'invitations et le parcours
     * d'onboarding.
     *
     * @throws DomainException 400 {@code INVITATION_ROLE_INVALID},
     *                         {@code INVITATION_SCOPE_INVALID},
     *                         {@code INVITATION_NODE_INVALID},
     *                         {@code INVITATION_ALREADY_MEMBER},
     *                         {@code INVITATION_ALREADY_PENDING}
     */
    @Transactional
    public InvitationCreationResult createInvitation(UUID tenantId,
                                                      UUID inviterId,
                                                      String email,
                                                      String roleKey,
                                                      MembershipScopeType scopeType,
                                                      UUID scopeId,
                                                      UUID organizationNodeId) {
        if (email == null || email.isBlank() || roleKey == null || roleKey.isBlank()) {
            throw new DomainException("email et role sont requis", HttpStatus.BAD_REQUEST, "INVITATION_INPUT_INVALID");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String normalizedRole = roleKey.trim().toUpperCase(Locale.ROOT);
        MembershipScopeType effectiveScopeType = scopeType == null ? MembershipScopeType.TENANT : scopeType;

        Role role = resolveCreatableRole(tenantId, normalizedRole);
        UUID effectiveScopeId = scopeId != null ? scopeId : organizationNodeId;

        if (effectiveScopeType == MembershipScopeType.TENANT && effectiveScopeId != null) {
            throw new DomainException(
                    "Un scope tenant ne doit pas contenir de ressource",
                    HttpStatus.BAD_REQUEST, "INVITATION_SCOPE_INVALID");
        }
        if (effectiveScopeType != MembershipScopeType.TENANT
                && (organizationNodeId == null || !organizationNodeId.equals(effectiveScopeId))) {
            throw new DomainException(
                    "Le scope de l'invitation est invalide",
                    HttpStatus.BAD_REQUEST, "INVITATION_SCOPE_INVALID");
        }
        if (organizationNodeId != null) {
            OrganizationNode node = organizationNodeRepository.findById(organizationNodeId)
                    .filter(candidate -> candidate.getTenantId().equals(tenantId))
                    .orElseThrow(() -> new DomainException(
                            "Nœud organisationnel invalide",
                            HttpStatus.BAD_REQUEST, "INVITATION_NODE_INVALID"));
            if (node.getId() == null) {
                throw new DomainException(
                        "Nœud organisationnel invalide",
                        HttpStatus.BAD_REQUEST, "INVITATION_NODE_INVALID");
            }
        }

        // B4 : l'email est une identite GLOBALE. Meme tenant d'abord, puis tous tenants.
        Optional<User> existingUser = userRepository.findByTenantIdAndEmailIgnoreCase(tenantId, normalizedEmail);
        boolean foreignIdentity = false;

        if (existingUser.isEmpty()) {
            Optional<User> foreignUser = userRepository.findGlobalByEmailIgnoreCase(normalizedEmail);
            if (foreignUser.isPresent()) {
                // Decision D3 : invitation classique, aucune membership cross-tenant,
                // aucun doublon `users`. L'utilisateur choisira son organisation.
                foreignIdentity = true;
                return createPendingInvitation(tenantId, inviterId, normalizedEmail, normalizedRole,
                        effectiveScopeType, effectiveScopeId, organizationNodeId,
                        InvitationCreationKind.CROSS_TENANT_INVITATION,
                        "INVITATION_CREATED_CROSS_TENANT");
            }
        } else {
            User user = existingUser.get();
            foreignIdentity = user.getTenantId() == null || !user.getTenantId().equals(tenantId);
            if (membershipRepository.existsByUserIdAndTenantIdAndStatus(
                    user.getId(), tenantId, MembershipStatus.ACTIVE)) {
                throw new DomainException(
                        "Cet utilisateur appartient déjà à ce tenant",
                        HttpStatus.BAD_REQUEST, "INVITATION_ALREADY_MEMBER",
                        Map.of("userId", user.getId().toString()));
            }
            membershipRepository.save(TenantMembership.builder()
                    .tenantId(tenantId)
                    .userId(user.getId())
                    .role(role)
                    .roleLegacy(role.getKey())
                    .scopeType(effectiveScopeType)
                    .scopeId(effectiveScopeId)
                    .status(MembershipStatus.ACTIVE)
                    .invitedBy(inviterId)
                    .build());
            auditService.logSimple("USER_INVITED_EXISTING", "USER", user.getId());
            return new InvitationCreationResult(
                    InvitationCreationKind.DIRECT_MEMBERSHIP,
                    null, user.getId(), normalizedEmail, normalizedRole,
                    effectiveScopeType.name(), effectiveScopeId,
                    null, null, false, foreignIdentity, false);
        }

        return createPendingInvitation(tenantId, inviterId, normalizedEmail, normalizedRole,
                effectiveScopeType, effectiveScopeId, organizationNodeId,
                InvitationCreationKind.INVITATION, "INVITATION_CREATED");
    }

    private Role resolveCreatableRole(UUID tenantId, String normalizedRole) {
        Optional<Role> role = roleRepository.findByTenantIdAndKey(tenantId, normalizedRole);
        if (role.isEmpty()) {
            role = roleRepository.findByTenantIdIsNullAndKey(normalizedRole);
        }
        if (role.isEmpty()) {
            throw new DomainException(
                    "Rôle invalide: " + normalizedRole,
                    HttpStatus.BAD_REQUEST, "INVITATION_ROLE_INVALID");
        }
        if (role.get().getTenantId() == null && role.get().getKey().startsWith("PLATFORM_")) {
            throw new DomainException(
                    "Un rôle plateforme ne peut pas être attribué à une invitation tenant",
                    HttpStatus.BAD_REQUEST, "INVITATION_PLATFORM_ROLE_FORBIDDEN");
        }
        return role.get();
    }

    private InvitationCreationResult createPendingInvitation(UUID tenantId,
                                                             UUID inviterId,
                                                             String normalizedEmail,
                                                             String normalizedRole,
                                                             MembershipScopeType scopeType,
                                                             UUID scopeId,
                                                             UUID organizationNodeId,
                                                             InvitationCreationKind kind,
                                                             String auditAction) {
        Optional<Invitation> pending = invitationRepository
                .findByTenantIdAndEmailAndStatus(tenantId, normalizedEmail, InvitationStatus.PENDING);
        if (pending.isPresent()) {
            throw new DomainException(
                    "Une invitation en attente existe déjà pour cet email",
                    HttpStatus.BAD_REQUEST, "INVITATION_ALREADY_PENDING",
                    Map.of("invitationId", pending.get().getId().toString()));
        }

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        Invitation invitation = Invitation.builder()
                .tenantId(tenantId)
                .email(normalizedEmail)
                .role(normalizedRole)
                .scopeType(scopeType == null ? null : scopeType.name())
                .scopeId(scopeId)
                .inviterId(inviterId)
                .tokenHash(InvitationTokenHasher.hash(token))
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .organizationNodeId(organizationNodeId)
                .build();
        invitationRepository.save(invitation);
        auditService.logSimple(auditAction, "INVITATION", invitation.getId());

        String link = frontendUrl + "/accept-invitation?token=" + token;
        boolean emailSent = sendInvitationEmail(normalizedEmail, normalizedRole, tenantId, link);

        return new InvitationCreationResult(
                kind,
                invitation.getId(), null, normalizedEmail, normalizedRole,
                scopeType == null ? null : scopeType.name(), scopeId,
                token, link, emailSent, false, kind == InvitationCreationKind.CROSS_TENANT_INVITATION);
    }

    /**
     * Envoi de l'email d'invitation. Jamais bloquant (décision D10) : un SMTP non
     * configuré doit produire {@code emailSent = false}, pas une exception.
     */
    private boolean sendInvitationEmail(String email, String roleKey, UUID tenantId, String invitationLink) {
        try {
            String tenantName = tenantRepository.findById(tenantId)
                    .map(Tenant::getName)
                    .orElse("Discipolat");
            emailService.send(
                    email,
                    "Vous êtes invité(e) à rejoindre " + tenantName,
                    "Bonjour,\n\n"
                            + "Vous avez été invité(e) à rejoindre "
                            + tenantName
                            + " avec le rôle " + roleKey + ".\n\n"
                            + "Pour accepter l'invitation et créer votre compte, ouvrez le lien suivant :\n"
                            + invitationLink + "\n\n"
                            + "Ce lien expire dans 7 jours.\n\n"
                            + "Cordialement,\nL'équipe Discipolat"
            );
            return true;
        } catch (RuntimeException failure) {
            return false;
        }
    }

    @Transactional(noRollbackFor = DomainException.class)
    public Invitation validate(String token) {
        Invitation invitation = findPending(token, false);
        resolveRole(invitation);
        validateScope(invitation);
        return invitation;
    }

    @Transactional(noRollbackFor = DomainException.class)
    public AcceptanceResult accept(String token, String password, String firstName, String lastName) {
        Invitation invitation = findPending(token, true);
        Role role = resolveRole(invitation);
        validateScope(invitation);

        Optional<User> existingUser = userRepository.findGlobalByEmailIgnoreCase(invitation.getEmail());

        User user;
        boolean crossTenantIdentity = false;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            crossTenantIdentity = user.getTenantId() == null
                    || !user.getTenantId().equals(invitation.getTenantId());
        } else {
            if (password == null || password.isBlank()) {
                throw new DomainException(
                        "Mot de passe requis pour nouveau compte",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_PASSWORD_REQUIRED"
                );
            }
            if (password.length() < 8) {
                throw new DomainException(
                        "Le mot de passe doit contenir au moins 8 caractères",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_PASSWORD_TOO_SHORT"
                );
            }
            if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new DomainException(
                        "Le mot de passe est trop long",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_PASSWORD_TOO_LONG"
                );
            }
            String normalizedFirstName = firstName != null ? firstName.trim() : "";
            String normalizedLastName = lastName != null ? lastName.trim() : "";
            if (normalizedFirstName.length() > 100 || normalizedLastName.length() > 100) {
                throw new DomainException(
                        "Le prénom ou le nom est trop long",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_NAME_INVALID"
                );
            }
            user = User.builder()
                    .tenantId(invitation.getTenantId())
                    .email(invitation.getEmail())
                    .passwordHash(passwordEncoder.encode(password))
                    .firstName(normalizedFirstName)
                    .lastName(normalizedLastName)
                    .role(UserRole.MEMBRE)
                    .statut(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
        }

        MembershipScopeType membershipScopeType = scopeType(invitation);
        boolean alreadyMember = membershipRepository.existsExactActiveMembership(
                user.getId(),
                invitation.getTenantId(),
                role.getId(),
                MembershipStatus.ACTIVE,
                membershipScopeType,
                invitation.getScopeId()
        );
        if (!alreadyMember) {
            membershipRepository.save(TenantMembership.builder()
                    .tenantId(invitation.getTenantId())
                    .userId(user.getId())
                    .role(role)
                    .roleLegacy(role.getKey())
                    .scopeType(membershipScopeType)
                    .scopeId(invitation.getScopeId())
                    .status(MembershipStatus.ACTIVE)
                    .invitedBy(invitation.getInviterId())
                    .build());
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);
        registerInDirectory(invitation, user, firstName, lastName);
        auditService.logSimple("INVITATION_ACCEPTED", "INVITATION", invitation.getId());
        if (crossTenantIdentity) {
            auditService.logSimple("INVITATION_ACCEPTED_CROSS_TENANT", "USER", user.getId());
        }

        return new AcceptanceResult(
                user.getId(),
                user.getEmail(),
                invitation.getTenantId(),
                alreadyMember,
                crossTenantIdentity);
    }

    private Invitation findPending(String token, boolean lock) {
        if (token == null || token.isBlank()) {
            throw new DomainException("Invitation invalide", HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND");
        }

        String tokenHash = InvitationTokenHasher.hash(token);
        Optional<Invitation> found = lock
                ? invitationRepository.findByTokenHashForUpdate(tokenHash)
                : invitationRepository.findByTokenHash(tokenHash);
        if (found.isEmpty()) {
            throw new DomainException("Invitation invalide", HttpStatus.NOT_FOUND, "INVITATION_NOT_FOUND");
        }

        Invitation invitation = found.get();
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new DomainException(
                    "Invitation expirée ou déjà utilisée",
                    HttpStatus.GONE,
                    "INVITATION_NOT_PENDING",
                    Map.of("status", invitation.getStatus().name())
            );
        }

        if (!invitation.getExpiresAt().isAfter(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new DomainException("Invitation expirée", HttpStatus.GONE, "INVITATION_EXPIRED");
        }
        return invitation;
    }

    private Role resolveRole(Invitation invitation) {
        String roleKey = invitation.getRole().toUpperCase();
        Role role = roleRepository.findByTenantIdAndKey(invitation.getTenantId(), roleKey)
                .or(() -> roleRepository.findGlobalByKey(roleKey))
                .orElseThrow(() -> new DomainException(
                        "Le rôle de l'invitation n'existe plus",
                        HttpStatus.GONE,
                        "INVITATION_ROLE_UNAVAILABLE"
                ));
        if (role.getTenantId() == null && role.getKey().startsWith("PLATFORM_")) {
            throw new DomainException(
                    "Un rôle plateforme ne peut pas être attribué par une invitation tenant",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_PLATFORM_ROLE_FORBIDDEN"
            );
        }
        return role;
    }

    private void validateScope(Invitation invitation) {
        MembershipScopeType type = scopeType(invitation);
        if (type == MembershipScopeType.TENANT) {
            if (invitation.getScopeId() != null || invitation.getOrganizationNodeId() != null) {
                throw new DomainException(
                        "Un scope tenant ne doit pas contenir de ressource",
                        HttpStatus.BAD_REQUEST,
                        "INVITATION_SCOPE_INVALID"
                );
            }
            return;
        }

        if (invitation.getOrganizationNodeId() == null
                || invitation.getScopeId() == null
                || !invitation.getOrganizationNodeId().equals(invitation.getScopeId())) {
            throw new DomainException(
                    "Le scope de l'invitation est invalide",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_SCOPE_INVALID"
                );
        }

        OrganizationNode node = organizationNodeRepository.findById(invitation.getOrganizationNodeId())
                .orElseThrow(() -> new DomainException(
                        "Le nœud organisationnel de l'invitation n'existe pas",
                        HttpStatus.GONE,
                        "INVITATION_SCOPE_UNAVAILABLE"
                ));
        if (!invitation.getTenantId().equals(node.getTenantId())) {
            throw new DomainException(
                    "Le scope de l'invitation appartient à un autre tenant",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_SCOPE_INVALID"
            );
        }
    }

    private MembershipScopeType scopeType(Invitation invitation) {
        if (invitation.getScopeType() == null || invitation.getScopeType().isBlank()) {
            return MembershipScopeType.TENANT;
        }
        try {
            return MembershipScopeType.valueOf(invitation.getScopeType());
        } catch (IllegalArgumentException exception) {
            throw new DomainException(
                    "Le type de scope de l'invitation est invalide",
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_SCOPE_INVALID"
            );
        }
    }

    /**
     * Resultat d'acceptation d'une invitation.
     *
     * @param userId             identifiant du compte associe a l'invitation
     * @param email              email du compte
     * @param tenantId           tenant invite
     * @param alreadyMember      le compte etait deja membre de ce tenant/role/scope
     * @param crossTenantIdentity le compte existait deja dans une AUTRE eglise ;
     *                           aucun utilisateur n'a alors ete cree, seule une
     *                           {@code TenantMembership} a ete ajoutee (decision D3)
     */
    public record AcceptanceResult(
            UUID userId,
            String email,
            UUID tenantId,
            boolean alreadyMember,
            boolean crossTenantIdentity) {

        @Override
        public String toString() {
            return "AcceptanceResult[userId=" + userId + ", tenantId=" + tenantId
                    + ", alreadyMember=" + alreadyMember
                    + ", crossTenantIdentity=" + crossTenantIdentity + "]";
        }

        /** Constructeur de compatibilite : avant V185/B4, {@code crossTenantIdentity} n'existait pas. */
        public AcceptanceResult(UUID userId, String email, UUID tenantId, boolean alreadyMember) {
            this(userId, email, tenantId, alreadyMember, false);
        }
    }
}
