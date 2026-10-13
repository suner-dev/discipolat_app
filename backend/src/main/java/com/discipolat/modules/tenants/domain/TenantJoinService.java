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
 *   <li>{@link #registerWithSlug} — inscription « au nom d'une église » par
 *     lien ({@code /register?tenant=<slug>} web, {@code tenantId} mobile) :
 *     elle appelle le MÊME noyau que {@link #registerWithCode}, donc elle
 *     respecte le même {@link JoinMode#APPROVAL}. Elle existait auparavant sous
 *     la forme d'un appel direct à {@code AuthService.registerInChurch}, qui
 *     ignorait le mode de l'église — voir le détail sur la méthode.</li>
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
    private final InvitationService invitationService;
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
                             InvitationService invitationService,
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
        this.invitationService = invitationService;
        this.auditService = auditService;
        this.crossTenant = crossTenant;
        this.emailService = emailService;
        this.peopleService = peopleService;
    }

    /**
     * Issue d'une rejointure.
     *
     * <p><b>SPEC_ORGANISATION_DENOMINATION_V2 §7.0 / T-B0bis (faille F7).</b>
     * Les deux derniers champs sont la <b>correction du défaut qui rendait la
     * promesse « le code ne se saisit qu'une fois » fausse</b> :
     * {@code markActiveTenant} persistait {@code users.active_tenant_id} sans
     * réémettre de jeton, alors que le claim {@code tenantId} du access token
     * courant — relu par le {@code TenantInterceptor} à chaque requête —
     * continuait de désigner l'ancienne organisation. Le client atterrissait
     * donc dans l'église précédente (web) ou croyait être dans la nouvelle
     * alors que toutes ses requêtes partaient vers l'ancienne (mobile).
     *
     * <p>Ils valent {@code null} quand aucune bascule n'est survenue : adhésion
     * en attente d'approbation, ou rejeu sur une église dont l'on est déjà
     * membre.
     */
    public record JoinOutcome(String status, UUID tenantId, String tenantName, String orgNodeLabel,
                              String accessToken, String refreshToken) {
        static JoinOutcome joined(UUID tenantId, String name, String label) {
            return new JoinOutcome("JOINED", tenantId, name, label, null, null);
        }

        static JoinOutcome joinedWithSession(UUID tenantId, String name, String label,
                                             String accessToken, String refreshToken) {
            return new JoinOutcome("JOINED", tenantId, name, label, accessToken, refreshToken);
        }

        static JoinOutcome requested(UUID tenantId, String name, String label) {
            return new JoinOutcome("PENDING_APPROVAL", tenantId, name, label, null, null);
        }

        /** L'adhésion existe déjà dans cette organisation : pas de bascule. */
        public boolean hasSession() {
            return accessToken != null && !accessToken.isBlank();
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
        Tenant tenant = resolveActiveTenantForSlug(slug, null);
        TenantJoinCode primary = joinCodeService.findPrimaryActiveCode(tenant.getId())
                .orElseGet(() -> syntheticOpenCode(tenant.getId()));
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
        //
        // T-B0bis (F7) : `markActiveTenant` seul ne suffit PAS. Le claim
        // `tenantId` du access token courant reste sur l'ancienne organisation
        // et le TenantInterceptor le relit à chaque requête : sans réémission,
        // le client atterrit dans l'église précédente. On persistance donc le
        // choix ET on réémet la paire de jetons qui le porte.
        activeTenantService.markActiveTenant(user, tenant.getId());
        ActiveTenantService.SwitchOutcome session =
                activeTenantService.switchTenant(userId, tenant.getId());
        registerInDirectoryQuietly(user, tenant);
        notifyAdminsQuietly(tenant, user, code);
        auditService.logSimple("MEMBER_JOINED_BY_CODE", "TENANT", tenant.getId());
        return JoinOutcome.joinedWithSession(tenant.getId(), tenant.getName(), code.getLabel(),
                session.accessToken(), session.refreshToken());
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
        return registerAgainstResolvedCode(email, rawPassword, firstName, lastName, phone,
                code, tenant, "JOIN_REQUEST_CREATED_BY_CODE");
    }

    /**
     * <p>Inscription « au nom d'une église » — lien web {@code /register?tenant=<slug>}
     * et variante mobile {@code tenantId}. Elle passe par EXACTEMENT le même chemin
     * que {@link #registerWithCode}, donc elle respecte le même
     * {@link JoinMode#APPROVAL} que l'inscription par code.
     *
     * <p><b>Faille refermée (D1/R1).</b> Ce chemin appelait directement
     * {@code AuthService.registerInChurch}, qui résout le tenant puis crée le
     * compte sans jamais consulter le {@code JoinMode} de l'église. Conséquence
     * mesurée : une église en {@code APPROVAL} — celle dont l'interface affiche
     * « Cette église valide chaque demande d'adhésion avant l'entrée » — se
     * voyait franchir quand même par une simple URL, sans invitation ni code, et
     * le compte MEMBRE obtenu lisait {@code GET /api/v1/souls} (données de
     * conviction, RGPD art. 9). Le slug étant dérivé mécaniquement du nom de
     * l'église ({@code SelfServiceChurchService#uniqueSlug}), il est devinable :
     * la porte n'était pas une porte.
     *
     * <p>Aucune intention produit nouvelle : une église en {@code OPEN} — le cas
     * par défaut, sans code configuré — se comporte exactement comme avant.
     *
     * <p><b>Anti-énumération.</b> Slug inconnu et église suspendue répondent
     * le MÊME message et le MÊME code ({@code JOIN_SLUG_NOT_FOUND}), comme le fait
     * déjà {@link #joinBySlug} : distinguer les deux revenait à offrir un oracle
     * permettant de sonder quelles églises existent.

     */
    @Transactional
    public RegisterOutcome registerWithSlug(String email, String rawPassword, String firstName,
                                            String lastName, String phone, String rawSlug,
                                            UUID tenantId) {
        Tenant tenant = resolveActiveTenantForSlug(rawSlug, tenantId);
        // Même résolution que joinBySlug : le code principal actif porte le
        // JoinMode de la politique d'adhésion ; à défaut de code, l'adhésion est
        // ouverte (mode OPEN historique, jamais APPROVAL sur un code synthétique).
        TenantJoinCode primary = joinCodeService.findPrimaryActiveCode(tenant.getId())
                .orElseGet(() -> syntheticOpenCode(tenant.getId()));
        return registerAgainstResolvedCode(email, rawPassword, firstName, lastName, phone,
                primary, tenant, "JOIN_REQUEST_CREATED_BY_LINK");
    }

    /**
     * Noyau commun aux inscriptions par code et par lien : c'est ICI que la
     * politique {@link JoinMode} de l'église est appliquée, une seule fois pour
     * les deux portes. Les extraire était indispensable — c'est précisément
     * parce que la branche « par lien » avait sa propre implémentation qu'elle
     * a pu diverger et ne plus rien vérifier.
     *
     * @param auditEvent libellé d'audit propre à la porte d'entrée, pour qu'un
     *                  journal permette encore de distinguer les deux après coup.
     */
    private RegisterOutcome registerAgainstResolvedCode(String email, String rawPassword,
                                                        String firstName, String lastName, String phone,
                                                        TenantJoinCode code, Tenant tenant,
                                                        String auditEvent) {
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
                auditService.logSimple(auditEvent, "TENANT", tenant.getId());
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

    /** Code d'entrée OPEN de substitution : aucune église n'a de code, l'adhésion
     *  reste ouverte. Jamais APPROVAL — un code synthétique ne doit pas pouvoir
     *  fabriquer une politique d'approbation qui n'a jamais été choisie. */
    private TenantJoinCode syntheticOpenCode(UUID tenantId) {
        TenantJoinCode synthetic = new TenantJoinCode();
        synthetic.setTenantId(tenantId);
        synthetic.setJoinMode(JoinMode.OPEN);
        return synthetic;
    }

    /**
     * Résolution « église à qui on veut s'inscrire » par slug (web) ou par id
     * (mobile). Inconnue, suspendue ou non-ACTIVE : réponse strictement
     * identique — sinon l'endpoint devenait un oracle d'existence.
     */
    private Tenant resolveActiveTenantForSlug(String rawSlug, UUID tenantId) {
        Tenant tenant = null;
        if (tenantId != null) {
            tenant = tenantRepository.findById(tenantId).orElse(null);
        } else if (rawSlug != null && !rawSlug.isBlank()) {
            String normalized = rawSlug.trim();
            tenant = tenantRepository.findBySlug(normalized.toLowerCase())
                    .or(() -> tenantRepository.findBySlug(normalized))
                    .orElse(null);
        }
        if (tenant == null || tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new DomainException("Aucune église trouvée pour ce lien",
                    HttpStatus.NOT_FOUND, "JOIN_SLUG_NOT_FOUND");
        }
        return tenant;
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
        if (request.getUserId() == null) {
            // Pré-inscription par email : AUCUN compte n'existe. C'est le cas
            // des deux portes APPROVAL — inscription par code ET inscription par
            // lien, désormais protégée elle aussi par le JoinMode.
            issueActivationInvitationQuietly(request, tenant, handledBy);
        } else {
            sendEmailQuietly(request.getEmail(), "Votre adhésion à " + tenant.getName() + " est validée",
                    "Bonjour,\n\nVotre demande d'adhésion à « " + tenant.getName()
                            + " » a été approuvée. Connectez-vous, vous y êtes maintenant membre.\n\n"
                            + "L'équipe Discipolat");
        }
        auditService.logSimple("JOIN_REQUEST_APPROVED", "TENANT", tenant.getId());
        // T-B0bis (F7) : même correction que joinWithCode — l'approbation peut
        // concerner un compte déjà connecté (request.userId renseigné), qui doit
        // donc repartir avec un jeton portant la nouvelle organisation plutôt
        // qu'avec son jeton périmé. Sans compte connecté, aucune session n'est à
        // émettre : la personne n'a rien à ouvrir tant qu'elle n'a pas fixé son
        // mot de passe, ce que le lien d'activation lui permet de faire.
        if (request.getUserId() != null) {
            ActiveTenantService.SwitchOutcome session =
                    activeTenantService.switchTenant(request.getUserId(), tenant.getId());
            return JoinOutcome.joinedWithSession(tenant.getId(), tenant.getName(), null,
                    session.accessToken(), session.refreshToken());
        }
        return JoinOutcome.joined(tenant.getId(), tenant.getName(), null);
    }

    /**
     * Émet une VRAIE invitation à l'approbation d'une demande sans compte.
     *
     * <p><b>Le trou refermé.</b> L'approbation d'une pré-inscription envoyait
     * « Votre demande a été approuvée. <b>Connectez-vous</b>, vous y êtes
     * maintenant membre » — alors qu'aucun compte n'avait jamais été créé. Le
     * chemin APPROVAL ne crée volontairement aucun compte avant la décision de
     * l'église (c'est tout l'intérêt du mode) ; il n'en créait pas davantage
     * après. La personne est donc invitée à « se connecter », n'a pas de compte,
     * et l'approbation devient une impasse : l'église a validé, et rien ne
     * s'ouvre.
     *
     * <p>On réutilise le mécanisme DÉJÀ livré par le produit
     * ({@link InvitationService}) : jeton à usage unique, hashé en base, valable
     * 7 jours, email contenant {@code /accept-invitation?token=…}. Aucune
     * invitation parallèle, aucun second écran d'activation, aucun mot de passe
     * posé par le serveur : la personne le choisit elle-même en ouvrant le lien,
     * comme pour toute invitation du produit.
     *
     * <p><b>Jamais bloquant pour l'approbation.</b> L'église a décidé, la
     * décision est enregistrée et auditée quoi qu'il arrive. Si l'invitation ne
     * peut pas être émise, on ne remet PAS l'approbation en cause (l'admin
     * reverrait sa demande repasser en attente) — on prévient la personne de ce
     * qui s'est réellement passé, pour ne pas retomber dans le mensonge
     * d'origine.
     */
    private void issueActivationInvitationQuietly(TenantJoinRequest request, Tenant tenant,
                                                  UUID handledBy) {
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            // Ni compte ni adresse : il n'y a personne à convoquer. On le journalise
            // au lieu d'envoyer un email à une adresse vide.
            log.warn("Demande d'adhésion {} approuvée sans email ni compte : "
                    + "aucune invitation émise (tenant {})", request.getId(), tenant.getId());
            return;
        }
        UUID orgNodeId = request.getOrgNodeId();
        MembershipScopeType scopeType = scopeTypeForJoinRequest(orgNodeId);
        try {
            InvitationService.InvitationCreationResult result = invitationService.createInvitation(
                    tenant.getId(), handledBy, email, MEMBER_ROLE_KEY, scopeType, orgNodeId, orgNodeId);
            auditService.logSimple("JOIN_REQUEST_APPROVED_INVITATION_ISSUED", "TENANT", tenant.getId());
            log.info("Adhésion approuvée pour {} (tenant {}) : invitation {} ({}, email envoyé={})",
                    email, tenant.getId(), result.invitationId(), result.kind(), result.emailSent());
        } catch (RuntimeException failure) {
            // INVITATION_ALREADY_PENDING, INVITATION_NODE_INVALID, rôle absent…
            // L'approbation reste valide : on le dit à la personne, sans promettre
            // un lien qui n'a pas pu être produit.
            log.warn("Adhésion approuvée pour {} (tenant {}) mais invitation impossible : {}",
                    email, tenant.getId(), failure.getMessage());
            sendEmailQuietly(email, "Votre adhésion à " + tenant.getName() + " est validée",
                    "Bonjour,\n\nVotre demande d'adhésion à « " + tenant.getName()
                            + " » a été approuvée. Un responsable doit vous renvoyer votre lien "
                            + "d'accès par e-mail : le lien reçu plus tôt a peut-être expiré.\n\n"
                            + "L'équipe Discipolat");
        }
    }

    /**
     * Portée de l'invitation émise à l'approbation : l'église entière, ou le
     * nœud qui était visé par la demande. Même lecture que
     * {@link #grantMembership}, pour que la demande et l'invitation ne
     * repartent pas sur deux périmètres différents.
     */
    private MembershipScopeType scopeTypeForJoinRequest(UUID orgNodeId) {
        if (orgNodeId == null) {
            return MembershipScopeType.TENANT;
        }
        return organizationNodeRepository.findById(orgNodeId)
                .map(node -> scopeFor(node.getType()))
                .orElse(MembershipScopeType.SUB_CHURCH);
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
     *
     * <p><b>Lecture cross-tenant sous filtre suspendu (H4)</b> : l'existing-check
     * doit voir les adhésions de l'AUTRE église, sinon la contrainte d'unicité
     * de {@code tenant_memberships} exploserait en 500.
     *
     * <p><b>F17 — un SEUL accès par (utilisateur, organisation).</b> La base
     * porte {@code UNIQUE (user_id, tenant_id)} (V135) : une seconde ligne pour
     * la même organisation — même avec une autre portée de nœud — est
     * <b>impossible</b>. La version précédente insérait une seconde ligne et
     * s'appuyait sur {@code existsExactActiveMembership} : la base rejetait
     * l'insert, et les requêtes {@code Optional} sur
     * {@code (user_id, tenant_id, status)} levaient ensuite
     * {@code IncorrectResultSizeDataAccessException} dans le transfert de
     * propriété, la gestion des rôles et les permissions IA.
     *
     * <p>La portée du nœud est donc appliquée à l'appartenance <b>existante</b>
     * quand elle est plus restrictive (spéc §1.3, mode LÉGER) : un membre qui
     * rejoint une sous-église voit son périmètre préciser, pas une seconde
     * appartenance créée. Une adhesion déjà large (TENANT) n'est jamais
     * rétrécie — cela reviendrait à révoquer l'accès d'un membre à l'ensemble
     * de l'église pour l'avoir inscrit dans un campus.
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

            // Toutes les appartenances ACTIVE de cet utilisateur dans cette
            // organisation — et non un seul Optional : il peut y en avoir
            // plusieurs (périmètres différents) sur des données héritées.
            List<TenantMembership> existing = membershipRepository
                    .findAllByUserIdAndTenantIdAndStatus(user.getId(), code.getTenantId(),
                            MembershipStatus.ACTIVE);

            if (existing.isEmpty()) {
                membershipRepository.save(TenantMembership.builder()
                        .tenantId(code.getTenantId())
                        .userId(user.getId())
                        .role(memberRole)
                        .roleLegacy(memberRole.getKey())
                        .scopeType(scopeType)
                        .scopeId(scopeId)
                        .status(MembershipStatus.ACTIVE)
                        .build());
                return null;
            }

            // Adhésion déjà existante : on ne crée pas de seconde ligne (F17).
            // On ne fait que VALIDER que le code n'a pas servi à ouvrir une
            // église fermée à l'adhésion.
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
            // Une église sans code actif reçoit une adhésion par son lien : le code
            // est alors INEXISTANT (code de substitution). Écrire « avec le code
            // null » dans un email destiné à un responsable était faux et
            // injouable — on nomme donc le canal réellement utilisé.
            boolean viaCode = code != null && code.getCode() != null && !code.getCode().isBlank();
            String how = viaCode
                    ? "avec le code " + code.getCode()
                    : "via le lien d'invitation de l'église";
            for (TenantMembership admin : admins) {
                userRepository.findById(admin.getUserId())
                        .ifPresent(adminUser -> sendEmailQuietly(adminUser.getEmail(),
                                "Demande de rejoindre " + tenant.getName(),
                                "Bonjour,\n\n" + who + " souhaite rejoindre « " + tenant.getName()
                                        + " » " + how
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
