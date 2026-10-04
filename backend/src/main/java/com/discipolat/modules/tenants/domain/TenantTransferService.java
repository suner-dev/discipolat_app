package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.Payloads;
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
 * <b>TRANSFERT de membre</b> — SPEC_ORGANISATION_DENOMINATION_V2 §4.4 / §7.1 / T-B5.
 *
 * <p>C'est le besoin central énoncé par le client : <b>« un membre qui change
 * d'église ne doit pas se réinscrire »</b>. Il est déjà connu de la
 * plateforme — un compte, un mot de passe, une identité. Ce qui change, c'est
 * son <b>appartenance</b>.
 *
 * <h2>Le détecteur</h2>
 * <p>Deux organisations appartiennent au même réseau si leurs
 * {@code rootTenantId} coïncident (V222, D3). C'est le seul critère :
 * comparer des slugs ou des noms serait fragile (deux églises peuvent porter le
 * même nom dans deux réseaux différents), et comparer des identifiants
 * ferait échouer le cas le plus fréquent — une église et son campus, qui sont
 * deux organisations distinctes du même réseau.
 *
 * <h2>Deux issues, jamais une confusion</h2>
 * <ul>
 *   <li><b>Même racine</b> → TRANSFERT : l'appartenance source est marquée
 *       {@code REVOKED} avec sa trace (V223), l'appartenance cible est créée,
 *       l'organisation active bascule et les jetons sont réémis.</li>
 *   <li><b>Racine différente</b> → ce n'est PAS un transfert : le membre est
 *       simplement <b>admis</b> dans la nouvelle organisation, et son
 *       appartenance d'origine est <b>intacte</b>. Une personne peut être
 *       membre de deux réseaux distincts ; le dafür est normal.</li>
 * </ul>
 *
 * <p><b>D5 — l'historique est préservé.</b> Le transfert change une
 * appartenance, pas une identité : les âmes, les familles, les présences et
 * les années de service restent rattachés au même dossier pastoral.
 */
@Service
public class TenantTransferService {

    private static final Logger log = LoggerFactory.getLogger(TenantTransferService.class);

    private static final String MEMBER_ROLE_KEY = "MEMBRE";
    private static final String AUDIT_TRANSFER = "TENANT_TRANSFER";
    private static final String AUDIT_CROSS_ROOT = "TENANT_JOIN_CROSS_ROOT";

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final OrganizationNodeRepository organizationNodeRepository;
    private final UserRepository userRepository;
    private final JoinCodeService joinCodeService;
    private final ActiveTenantService activeTenantService;
    private final CrossTenantScopeAccess crossTenant;
    private final AuditService auditService;
    private final EmailService emailService;

    public TenantTransferService(TenantRepository tenantRepository,
                                 TenantMembershipRepository membershipRepository,
                                 RoleRepository roleRepository,
                                 OrganizationNodeRepository organizationNodeRepository,
                                 UserRepository userRepository,
                                 JoinCodeService joinCodeService,
                                 ActiveTenantService activeTenantService,
                                 CrossTenantScopeAccess crossTenant,
                                 AuditService auditService,
                                 EmailService emailService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.organizationNodeRepository = organizationNodeRepository;
        this.userRepository = userRepository;
        this.joinCodeService = joinCodeService;
        this.activeTenantService = activeTenantService;
        this.crossTenant = crossTenant;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    /**
     * Ce qu'il faut savoir AVANT de confirmer — l'IHM ne doit jamais
     * annoncer « vous rejoignez » quand la réalité est « vous êtes
     * transféré ».
     *
     * @param sameNetwork    même {@code rootTenantId} → transfert (§4.4 cas 1)
     * @param alreadyMember  déjà actif dans l'organisation cible
     * @param activeInOther  membre d'une AUTRE organisation → un transfert
     *                      laisserait cette appartenance intacte
     */
    public record TransferPreview(boolean sameNetwork, boolean alreadyMember,
                                  boolean activeInOther,
                                  String fromChurch, String toChurch, String toKind) {
    }

    /**
     * @param status {@code TRANSFERRED}, {@code JOINED} ou {@code ALREADY_MEMBER}
     */
    public record TransferOutcome(String status, UUID fromTenantId, UUID toTenantId,
                                  String toChurch, boolean tokenSwitched,
                                  String accessToken, String refreshToken) {
    }

    // ============================================================
    // PREVIEW
    // ============================================================

    /**
     * Analyse la situation d'un membre face à un code, sans rien écrire.
     *
     * <p>Sert à afficher « Bienvenue, vous êtes déjà membre de la Dénomination
     * X. Vous rejoignez l'Église B » (§4.4) au lieu d'un libellé générique
     * qui ne dit rien de ce qui va réellement se passer à l'historique du
     * membre.
     */
    @Transactional(readOnly = true)
    public TransferPreview preview(UUID userId, String rawCode) {
        TenantJoinCode code = resolveCode(rawCode);
        Tenant target = requireActiveTenant(code.getTenantId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Compte introuvable",
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        List<TenantMembership> mine = activeMembershipsOf(userId);

        Optional<TenantMembership> alreadyThere = mine.stream()
                .filter(m -> m.getTenantId().equals(target.getId()))
                .findFirst();
        if (alreadyThere.isPresent()) {
            return new TransferPreview(true, true, false,
                    alreadyThere.get().getTenantId().equals(target.getId()) ? target.getName() : null,
                    target.getName(), kindOf(target));
        }

        UUID currentActive = user.getTenantId();
        String fromName = currentActive == null ? null
                : tenantRepository.findById(currentActive).map(Tenant::getName).orElse(null);

        boolean sameNetwork = currentActive != null
                && sharesNetwork(currentActive, target);

        return new TransferPreview(sameNetwork, false, currentActive != null,
                fromName, target.getName(), kindOf(target));
    }

    // ============================================================
    // TRANSFERT
    // ============================================================

    /**
     * Effectue le transfert — ou l'adhésion, selon la racine (§4.4).
     *
     * <p><b>Atomique</b> : soit l'appartenance source est tracée ET la cible
     * créée, soit rien. Pas d'adhésion orpheline, pas de membre sans
     * organisation (spéc §8.4).
     */
    @Transactional
    public TransferOutcome transfer(UUID userId, String rawCode, String reason) {
        TenantJoinCode code = resolveCode(rawCode);
        Tenant target = requireActiveTenant(code.getTenantId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("Compte introuvable",
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        List<TenantMembership> mine = activeMembershipsOf(userId);

        // Déjà membre de l'organisation cible : rejeu idempotent, pas de doublon.
        Optional<TenantMembership> alreadyThere = mine.stream()
                .filter(m -> m.getTenantId().equals(target.getId()))
                .findFirst();
        if (alreadyThere.isPresent()) {
            auditService.logSimple(AUDIT_TRANSFER + "_NOOP", "TENANT", target.getId());
            return new TransferOutcome("ALREADY_MEMBER", alreadyThere.get().getTenantId(),
                    target.getId(), target.getName(), false, null, null);
        }

        UUID source = user.getTenantId();
        boolean sameNetwork = source != null && sharesNetwork(source, target);

        if (!sameNetwork) {
            // Racines différentes : ce n'est PAS un transfert (§4.4 cas 2).
            // L'appartenance d'origine reste intacte — on l'inscrit dans le
            // réseau d'accueil sans toucher à l'autre.
            createMembership(userId, target, code.getOrgNodeId());
            auditService.log(userId, target.getId(), AUDIT_CROSS_ROOT, "TENANT", target.getId(),
                    "SUCCESS",
                    Payloads.of("fromTenantId", source, "toTenantId", target.getId(),
                            "reason", reason),
                    null, null, null);
            ActiveTenantService.SwitchOutcome session =
                    activeTenantService.switchTenant(userId, target.getId());
            notifyQuietly(user, target, false, reason);
            return new TransferOutcome("JOINED", source, target.getId(), target.getName(),
                    true, session.accessToken(), session.refreshToken());
        }

        // ---- Même réseau : TRANSFERT (D4/D5) ----
        Tenant sourceTenant = tenantRepository.findById(source)
                .orElseThrow(() -> new DomainException("Organisation d'origine introuvable",
                        HttpStatus.NOT_FOUND, "SOURCE_TENANT_NOT_FOUND"));

        // 1) L'appartenance source est TRACÉE, pas supprimée (D5).
        TenantMembership transferred = crossTenant.call(() -> mine.stream()
                .filter(m -> m.getTenantId().equals(source))
                .min(java.util.Comparator.comparing(TenantMembership::getJoinedAt))
                .orElseThrow(() -> new DomainException("Aucune appartenance active à transférer",
                        HttpStatus.CONFLICT, "NO_ACTIVE_MEMBERSHIP_TO_TRANSFER")));
        transferred.markTransferred(target.getId(), userId, reason);
        membershipRepository.save(transferred);

        // 2) L'appartenance cible est créée.
        createMembership(userId, target, code.getOrgNodeId());

        // 3) Organisation active + JETONS (T-B0bis). Sans réémission, le claim
        //    tenantId du token courant continue de désigner l'ancienne église :
        //    le membre y resterait, en contradiction avec le message affiché.
        ActiveTenantService.SwitchOutcome session =
                activeTenantService.switchTenant(userId, target.getId());

        auditService.log(userId, target.getId(), AUDIT_TRANSFER, "TENANT", target.getId(),
                "SUCCESS",
                Payloads.of("fromTenantId", source, "toTenantId", target.getId(),
                        "fromChurch", sourceTenant.getName(),
                        "toChurch", target.getName(),
                        "reason", reason),
                null, null, null);
        notifyQuietly(user, target, true, reason);

        return new TransferOutcome("TRANSFERRED", source, target.getId(), target.getName(),
                true, session.accessToken(), session.refreshToken());
    }

    // ============================================================
    // INTERNAL
    // ============================================================

    /**
     * Deux organisations sont-elles du même réseau ?
     *
     * <p>Le {@code rootTenantId} est comparé <b>après chargement des deux
     * entités</b> : comparer des slugs serait fragile (deux églises peuvent
     * porter le même nom dans deux réseaux différents), et la valeur portée
     * par le code est le <i>tenant</i> cible, pas sa racine — il faut donc
     * remonter d'un cran.
     */
    private boolean sharesNetwork(UUID tenantAId, Tenant target) {
        Tenant a = tenantRepository.findById(tenantAId).orElse(null);
        if (a == null) {
            return false;
        }
        UUID rootA = a.effectiveRootTenantId();
        UUID rootB = target.effectiveRootTenantId();
        return rootA != null && rootA.equals(rootB);
    }

    /**
     * Appartenances ACTIVE du membre, lues hors filtre tenant.
     *
     * <p>La lecture doit traverser le filtre Hibernate : le membre peut
     * appartenir à une autre organisation que celle du jeton, et c'est
     * précisément ce qu'on cherche à savoir (pattern H4).
     *
     * <p>Liste et non {@code Optional} (F17) : plusieurs périmètres ACTIVE
     * sont possibles dans la même organisation.
     */
    private List<TenantMembership> activeMembershipsOf(UUID userId) {
        return crossTenant.call(() ->
                membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE));
    }

    private void createMembership(UUID userId, Tenant target, UUID orgNodeId) {
        crossTenant.call(() -> {
            Role memberRole = roleRepository.findByTenantIdAndKey(target.getId(), MEMBER_ROLE_KEY)
                    .or(() -> roleRepository.findGlobalByKey(MEMBER_ROLE_KEY))
                    .orElseThrow(() -> new DomainException(
                            "Rôle membre indisponible dans cette organisation",
                            HttpStatus.CONFLICT, "MEMBER_ROLE_MISSING"));
            MembershipScopeType scopeType = MembershipScopeType.TENANT;
            if (orgNodeId != null) {
                scopeType = organizationNodeScope(orgNodeId);
            }
            // F17 : la base impose UNIQUE(user_id, tenant_id) — on ne crée donc
            // une ligne que s'il n'existe aucune appartenance pour ce couple.
            boolean exists = !membershipRepository
                    .findAllByUserIdAndTenantIdAndStatus(userId, target.getId(),
                            MembershipStatus.ACTIVE).isEmpty();
            if (!exists) {
                membershipRepository.save(TenantMembership.builder()
                        .tenantId(target.getId())
                        .userId(userId)
                        .role(memberRole)
                        .roleLegacy(memberRole.getKey())
                        .scopeType(scopeType)
                        .scopeId(orgNodeId)
                        .status(MembershipStatus.ACTIVE)
                        .build());
            }
            return null;
        });
    }

    /**
     * Portée correspondant au nœud visé par le code (D3).
     *
     * <p>Un code de sous-eglise rattache le membre à ce campus plutôt qu'à
     * l'ensemble de l'organisation : c'est le mode LÉGER de la §1.3, et la
     * raison pour laquelle l'appartenance porte un {@code scopeId}.
     */
    private MembershipScopeType organizationNodeScope(UUID orgNodeId) {
        return organizationNodeRepository.findById(orgNodeId)
                .map(node -> switch (node.getType()) {
                    case SUB_CHURCH -> MembershipScopeType.SUB_CHURCH;
                    case CAMPUS -> MembershipScopeType.CAMPUS;
                    case REGION, DISTRICT -> MembershipScopeType.REGION;
                    case DEPARTMENT -> MembershipScopeType.DEPARTMENT;
                    default -> MembershipScopeType.CHURCH;
                })
                .orElse(MembershipScopeType.CHURCH);
    }

    private TenantJoinCode resolveCode(String rawCode) {
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

    private String kindOf(Tenant tenant) {
        return tenant.getKind() == null ? TenantKind.CHURCH.name() : tenant.getKind().name();
    }

    /**
     * Confirmation par email — <b>jamais bloquante</b> (spéc §8.3).
     *
     * <p>Un transfert est un changement d'appartenance majeur : le membre doit
     * pouvoir le vérifier par un canal indépendant. Mais un échec SMTP ne doit
     * pas faire échouer l'opération déjà validée en base.
     */
    private void notifyQuietly(User user, Tenant target, boolean transferred, String reason) {
        String subject = transferred
                ? "Votre transfert vers " + target.getName() + " est effectif"
                : "Vous avez rejoint " + target.getName();
        String body = transferred
                ? "Bonjour " + displayName(user) + ",\n\n"
                    + "Vous avez été transféré vers « " + target.getName() + " ».\n"
                    + "Votre parcours pastoral est conservé : vous restez la même personne.\n\n"
                    + "L'équipe Discipolat"
                : "Bonjour " + displayName(user) + ",\n\n"
                    + "Vous êtes désormais membre de « " + target.getName() + " ».\n\n"
                    + "L'équipe Discipolat";
        try {
            emailService.send(user.getEmail(), subject, body);
        } catch (RuntimeException failure) {
            log.warn("Email de transfert non envoyé à {} : {}", user.getEmail(), failure.getMessage());
        }
    }

    private String displayName(User user) {
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName();
        }
        return user.getEmail();
    }
}
