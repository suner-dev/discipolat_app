package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (BE-3) — rejointure par code, côté membre.
 *
 * <p>Deux chemins :
 * <ol>
 *   <li>{@link #registerWithCode} — un NOUVEAU compte s'inscrit avec le code :
 *     mode OPEN → reprise du flux {@code AuthService.registerInChurch}
 *     (email d'activation + répertoire SELF_SIGNUP à l'activation) ; si le
 *     code cible une sous-église, le membership scopé est posé immédiatement ;
 *     mode APPROVAL → {@code tenant_join_requests} PENDING, rien d'autre.</li>
 *   <li>{@link #join} — un compte DÉJÀ connecté rejoint une autre église :
 *     membership MEMBRE créé (scope du nœud si code de sous-église), tenant
 *     actif basculé (D7 : le code ne sera plus jamais redemandé), mode
 *     APPROVAL → demande PENDING + email non bloquant aux admins.</li>
 * </ol>
 *
 * <p>Toutes les lectures/écritures de {@code tenant_memberships} et
 * {@code organization_nodes} qui traversent le tenant du porteur de la requête
 * passent par {@link CrossTenantScopeAccess#call} (pattern H4 — sans cela, le
 * filtre Hibernate masque les lignes de l'autre église et le contrôle
 * d'unicité deviendrait aveugle).</p>
 */
@Service
public class TenantJoinService {

    private static final Logger log = LoggerFactory.getLogger(TenantJoinService.class);
    private static final String MEMBER_ROLE_KEY = "MEMBRE";

    private final JoinCodeService joinCodeService;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantJoinRequestRepository joinRequestRepository;
    private final OrganizationNodeRepository organizationNodeRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ActiveTenantService activeTenantService;
    private final AuthServiceBridge authServiceBridge;
    private final AuditService auditService;
    private final CrossTenantScopeAccess crossTenant;
    private final EmailService emailService;
    private final com.discipolat.modules.people.service.PeopleService peopleService;

    public TenantJoinService(JoinCodeService joinCodeService,
                             TenantRepository tenantRepository,
                             TenantMembershipRepository membershipRepository,
                             TenantJoinRequestRepository joinRequestRepository,
                             OrganizationNodeRepository organizationNodeRepository,
                             RoleRepository roleRepository,
                             UserRepository userRepository,
                             ActiveTenantService activeTenantService,
                             AuthServiceBridge authServiceBridge,
                             AuditService auditService,
                             CrossTenantScopeAccess crossTenant,
                             EmailService emailService,
                             com.discipolat.modules.people.service.PeopleService peopleService) {
        this.joinCodeService = joinCodeService;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.organizationNodeRepository = organizationNodeRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.activeTenantService = activeTenantService;
        this.authServiceBridge = authServiceBridge;
        this.auditService = auditService;
        this.crossTenant = crossTenant;
        this.emailService = emailService;
        this.peopleService = peopleService;
    }

    public record JoinOutcome(String status, UUID tenantId, String tenantName, String orgNodeLabel) {
        static JoinOutcome joined(UUID tenantId, String name, String label) {
            return new JoinOutcome("JOINED", tenantId, name, label);
        }

        static JoinOutcome requested(UUID tenantId, String name, String label) {
            return new JoinOutcome("PENDING_APPROVAL", tenantId, name, label);
        }
    }

    /** Résultat de l'inscription par code d'un nouveau compte. */
    public record RegisterOutcome(String status, UUID userId, String churchName, String activationRequired) {
    }

    /**
     * Rejointure authentifiée (POST /api/v1/tenant/join). Le compte courant
     * devient MEMBRE de l'église du code — ou demande une approbation si le
     * code est en mode APPROVAL.
     */
    @Transactional
    public JoinOutcome join(UUID userId, String rawCode) {
        TenantJoinCode code = resolveActiveCode(rawCode);
        return joinWithCode(userId, code);
    }

    /**
     * Rejointure authentifiée via le lien vanity /j/&lt;slug&gt; : on résout le
     * code principal actif de l'église (jamais exposé au client) puis on suit
     * le même chemin que par code. À défaut de code configuré, l'adhésion se
     * fait directement au tenant racine (mode OPEN historique).
     */
    @Transactional
    public JoinOutcome joinBySlug(UUID userId, String slug) {
        Tenant tenant = tenantRepository.findBySlug(slug == null ? "" : slug.trim().toLowerCase())
                .orElseThrow(() -> new DomainException("Aucune église trouvée pour ce lien",
                        HttpStatus.NOT_FOUND, "JOIN_SLUG_NOT_FOUND"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new DomainException("Aucune église trouvée pour ce lien",
                    HttpStatus.NOT_FOUND, "JOIN_SLUG_NOT_FOUND");
        }
        TenantJoinCode primary = joinCodeService.findPrimaryActiveCode(tenant.getId())
                .orElseGet(() -> {
                    TenantJoinCode synthetic = new TenantJoinCode();
                    synthetic.setTenantId(tenant.getId());
                    synthetic.setJoinMode(JoinMode.OPEN);
                    return synthetic;
                });
        return joinWithCode(userId, primary);
    }

    private JoinOutcome joinWithCode(UUID userId, TenantJoinCode code) {
        Tenant tenant = requireActiveTenant(code.getTenantId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Compte introuvable",
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (code.getJoinMode() == JoinMode.APPROVAL) {
            return requestApproval(code, tenant, user);
        }

        grantMembership(code, user);
        // D7 : le tenant rejoint devient le tenant actif — aux connexions
        // suivantes l'utilisateur retombe dedans sans jamais ressaisir le code.
        activeTenantService.markActiveTenant(user, tenant.getId());
        registerInDirectoryQuietly(user, tenant);
        notifyAdminsQuietly(tenant, user, code);
        auditService.logSimple("MEMBER_JOINED_BY_CODE", "TENANT", tenant.getId());
        return JoinOutcome.joined(tenant.getId(), tenant.getName(), code.getLabel());
    }

    /**
     * Inscription d'un NOUVEAU compte avec un code (POST /auth/register,
     * champ joinCode). Mode OPEN : compte MEMBRE en attente d'activation
     * (flux §G3.1 inchangé) + membership scopé si sous-église. Mode
     * APPROVAL : simple demande tracée par email, aucun compte créé.
     */
    @Transactional
    public RegisterOutcome registerWithCode(String email, String rawPassword, String firstName,
                                            String lastName, String phone, String rawCode) {
        TenantJoinCode code = resolveActiveCode(rawCode);
        Tenant tenant = requireActiveTenant(code.getTenantId());
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);

        if (code.getJoinMode() == JoinMode.APPROVAL) {
            if (joinRequestRepository.findByTenantIdAndEmailAndCodeAndStatus(
                    tenant.getId(), normalizedEmail, code.getCode(),
                    TenantJoinRequest.Status.PENDING).isEmpty()) {
                joinRequestRepository.save(TenantJoinRequest.builder()
                        .tenantId(tenant.getId())
                        .orgNodeId(code.getOrgNodeId())
                        .email(normalizedEmail)
                        .code(code.getCode())
                        .build());
                notifyAdminsQuietly(tenant, null, code);
                auditService.logSimple("JOIN_REQUEST_CREATED_BY_CODE", "TENANT", tenant.getId());
            }
            return new RegisterOutcome("PENDING_APPROVAL", null, tenant.getName(), "false");
        }

        User user = authServiceBridge.registerInChurch(
                email, rawPassword, firstName, lastName, phone, null, tenant.getId());
        if (code.getOrgNodeId() != null) {
            grantMembership(code, user);
        }
        return new RegisterOutcome("ACTIVATION_EMAIL_SENT", user.getId(), tenant.getName(), "true");
    }

    /** Validation par un admin tenant d'une demande PENDING (mode APPROVAL). */
    @Transactional
    public JoinOutcome approveRequest(UUID requestId, UUID currentTenantId, UUID handledBy) {
        TenantJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new DomainException("Demande introuvable",
                        HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND"));
        if (!request.getTenantId().equals(currentTenantId)) {
            throw new DomainException("Demande introuvable", HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND");
        }
        if (request.getStatus() != TenantJoinRequest.Status.PENDING) {
            throw new DomainException("Demande déjà traitée", HttpStatus.CONFLICT, "JOIN_REQUEST_HANDLED");
        }
        Tenant tenant = requireActiveTenant(request.getTenantId());
        if (request.getUserId() != null) {
            User user = userRepository.findById(request.getUserId()).orElse(null);
            if (user != null) {
                TenantJoinCode synthetic = new TenantJoinCode();
                synthetic.setTenantId(tenant.getId());
                synthetic.setOrgNodeId(request.getOrgNodeId());
                synthetic.setCode(request.getCode());
                synthetic.setJoinMode(JoinMode.OPEN);
                grantMembership(synthetic, user);
                activeTenantService.markActiveTenant(user, tenant.getId());
                registerInDirectoryQuietly(user, tenant);
            }
        }
        request.setStatus(TenantJoinRequest.Status.APPROVED);
        request.setHandledAt(Instant.now());
        request.setHandledBy(handledBy);
        joinRequestRepository.save(request);
        sendEmailQuietly(request.getEmail(), "Votre adhésion à " + tenant.getName() + " est validée",
                "Bonjour,\n\nVotre demande d'adhésion à « " + tenant.getName()
                        + " » a été approuvée. Connectez-vous, vous y êtes maintenant membre.\n\n"
                        + "L'équipe Discipolat");
        auditService.logSimple("JOIN_REQUEST_APPROVED", "TENANT", tenant.getId());
        return JoinOutcome.joined(tenant.getId(), tenant.getName(), null);
    }

    @Transactional
    public void rejectRequest(UUID requestId, UUID currentTenantId, UUID handledBy) {
        TenantJoinRequest request = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new DomainException("Demande introuvable",
                        HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND"));
        if (!request.getTenantId().equals(currentTenantId)) {
            throw new DomainException("Demande introuvable", HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND");
        }
        request.setStatus(TenantJoinRequest.Status.REJECTED);
        request.setHandledAt(Instant.now());
        request.setHandledBy(handledBy);
        joinRequestRepository.save(request);
        sendEmailQuietly(request.getEmail(), "Demande d'adhésion refusée",
                "Bonjour,\n\nVotre demande d'adhésion a été refusée. "
                        + "Rapprochez-vous de votre église pour en connaître la raison.\n\nL'équipe Discipolat");
        auditService.logSimple("JOIN_REQUEST_REJECTED", "TENANT", request.getTenantId());
    }

    @Transactional(readOnly = true)
    public List<TenantJoinRequest> pendingRequests(UUID tenantId) {
        return joinRequestRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                tenantId, TenantJoinRequest.Status.PENDING);
    }

    // ======================== INTERNAL ========================

    private TenantJoinCode resolveActiveCode(String rawCode) {
        return joinCodeService.findActiveByRawInput(rawCode)
                .orElseThrow(() -> new DomainException("Code d'église introuvable ou expiré",
                        HttpStatus.NOT_FOUND, "JOIN_CODE_NOT_FOUND"));
    }

    private Tenant requireActiveTenant(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new DomainException("Église introuvable",
                        HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND"));
        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new DomainException("Cette église n'accepte pas d'adhésions actuellement",
                    HttpStatus.GONE, "TENANT_NOT_ACCEPTING_JOIN");
        }
        return tenant;
    }

    private JoinOutcome requestApproval(TenantJoinCode code, Tenant tenant, User user) {
        Optional<TenantJoinRequest> existing = joinRequestRepository
                .findByTenantIdAndUserIdAndCodeAndStatus(tenant.getId(), user.getId(),
                        code.getCode(), TenantJoinRequest.Status.PENDING);
        if (existing.isEmpty()) {
            joinRequestRepository.save(TenantJoinRequest.builder()
                    .tenantId(tenant.getId())
                    .orgNodeId(code.getOrgNodeId())
                    .userId(user.getId())
                    .email(user.getEmail())
                    .code(code.getCode())
                    .build());
            notifyAdminsQuietly(tenant, user, code);
            auditService.logSimple("JOIN_REQUEST_CREATED_BY_MEMBER", "TENANT", tenant.getId());
        }
        return JoinOutcome.requested(tenant.getId(), tenant.getName(), code.getLabel());
    }

    /**
     * Membership MEMBRE, scopé sur la sous-église du code si présent (D3).
     * Lecture cross-tenant sous filtre suspendu (H4) : l'existing-check doit
     * voir les adhésions de l'AUTRE église, sinon la contrainte d'unicité
     * (user_id, tenant_id) exploserait en 500.
     */
    private void grantMembership(TenantJoinCode code, User user) {
        crossTenant.call(() -> {
            Role memberRole = roleRepository.findByTenantIdAndKey(code.getTenantId(), MEMBER_ROLE_KEY)
                    .or(() -> roleRepository.findGlobalByKey(MEMBER_ROLE_KEY))
                    .orElseThrow(() -> new DomainException("Rôle membre indisponible dans cette église",
                            HttpStatus.CONFLICT, "MEMBER_ROLE_MISSING"));
            MembershipScopeType scopeType = MembershipScopeType.TENANT;
            UUID scopeId = null;
            if (code.getOrgNodeId() != null) {
                scopeType = organizationNodeRepository.findById(code.getOrgNodeId())
                        .map(node -> scopeFor(node.getType()))
                        .orElse(MembershipScopeType.CHURCH);
                scopeId = code.getOrgNodeId();
            }
            boolean already = membershipRepository.existsExactActiveMembership(
                    user.getId(), code.getTenantId(), memberRole.getId(),
                    MembershipStatus.ACTIVE, scopeType, scopeId);
            if (!already) {
                membershipRepository.save(TenantMembership.builder()
                        .tenantId(code.getTenantId())
                        .userId(user.getId())
                        .role(memberRole)
                        .roleLegacy(memberRole.getKey())
                        .scopeType(scopeType)
                        .scopeId(scopeId)
                        .status(MembershipStatus.ACTIVE)
                        .build());
            }
            return null;
        });
    }

    private MembershipScopeType scopeFor(OrganizationNodeType type) {
        return switch (type) {
            case SUB_CHURCH -> MembershipScopeType.SUB_CHURCH;
            case CAMPUS -> MembershipScopeType.CAMPUS;
            case REGION -> MembershipScopeType.REGION;
            case DISTRICT -> MembershipScopeType.REGION;
            case DEPARTMENT -> MembershipScopeType.DEPARTMENT;
            default -> MembershipScopeType.CHURCH;
        };
    }

    /** Inscription au répertoire de l'église — jamais bloquante (pattern §G3.1). */
    private void registerInDirectoryQuietly(User user, Tenant tenant) {
        try {
            com.discipolat.modules.people.domain.Person person =
                    com.discipolat.modules.people.domain.Person.builder()
                            .firstName(user.getFirstName() != null && !user.getFirstName().isBlank()
                                    ? user.getFirstName() : "Membre")
                            .lastName(user.getLastName() != null && !user.getLastName().isBlank()
                                    ? user.getLastName() : "")
                            .emailNormalized(user.getEmail())
                            .phoneNormalized(user.getPhone())
                            .build();
            peopleService.registerPerson(tenant.getId(), person, "JOIN_CODE", user.getId());
        } catch (com.discipolat.modules.people.service.PeopleService.PersonAlreadyExistsException exists) {
            // Déjà au répertoire : pas de fiche en double.
        } catch (RuntimeException directoryIssue) {
            log.warn("Inscription répertoire impossible pour {} (tenant {}) : {}",
                    user.getEmail(), tenant.getId(), directoryIssue.getMessage());
        }
    }

    /** Email aux admins du tenant quand une demande APPROVAL arrive — non bloquant. */
    private void notifyAdminsQuietly(Tenant tenant, User user, TenantJoinCode code) {
        try {
            List<TenantMembership> admins = crossTenant.call(() ->
                    membershipRepository.findByTenantIdAndStatus(tenant.getId(), MembershipStatus.ACTIVE))
                    .stream()
                    .filter(m -> "TENANT_OWNER".equals(m.getRoleLegacy()) || "TENANT_ADMIN".equals(m.getRoleLegacy()))
                    .toList();
            String who = user != null
                    ? (user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ")")
                    : (code != null && code.getLabel() != null ? "Une personne (" + code.getLabel() + ")" : "Une personne");
            for (TenantMembership admin : admins) {
                userRepository.findById(admin.getUserId())
                        .ifPresent(adminUser -> sendEmailQuietly(adminUser.getEmail(),
                                "Demande de rejoindre " + tenant.getName(),
                                "Bonjour,\n\n" + who + " souhaite rejoindre « " + tenant.getName()
                                        + " » avec le code " + code.getCode()
                                        + ". Validez ou refusez cette demande dans votre espace admin.\n\n"
                                        + "L'équipe Discipolat"));
            }
        } catch (RuntimeException failure) {
            log.warn("Notification admins échouée pour tenant {}: {}", tenant.getId(), failure.getMessage());
        }
    }

    private void sendEmailQuietly(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            emailService.send(to, subject, body);
        } catch (RuntimeException failure) {
            log.warn("Email non envoyé à {} : {}", to, failure.getMessage());
        }
    }

    /**
     * Petit pont vers {@code AuthService.registerInChurch} : évite que la
     * classe du domaine auth apparaisse dans le graphe de dépendances déclaré
     * de ce service (AuthService → TenantJoinService n'existera jamais, mais
     * le pont rend le sens du graphe lisible et testable avec un mock).
     */
    @FunctionalInterface
    public interface AuthServiceBridge {
        User registerInChurch(String email, String rawPassword, String firstName, String lastName,
                              String phone, String tenantSlug, UUID tenantId);
    }
}
