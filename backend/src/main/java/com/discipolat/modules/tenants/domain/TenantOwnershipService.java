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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SPEC_ONBOARDING_FLOWS (D6) — propriété de l'église « façon WhatsApp » :
 * le fondateur est {@code TENANT_OWNER} (« le roi »), peut déléguer des
 * {@code TENANT_ADMIN}, transférer la barre à un membre existant, et un admin
 * peut demander un remplacement assisté si l'owner a disparu (arbitrage
 * plateforme, jamais automatique).
 */
@Service
public class TenantOwnershipService {

    private static final Logger log = LoggerFactory.getLogger(TenantOwnershipService.class);
    private static final String OWNER_KEY = "TENANT_OWNER";
    private static final String ADMIN_KEY = "TENANT_ADMIN";
    private static final String MEMBER_KEY = "MEMBRE";

    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final CrossTenantScopeAccess crossTenant;
    private final AuditService auditService;
    private final EmailService emailService;

    public TenantOwnershipService(TenantMembershipRepository membershipRepository,
                                  RoleRepository roleRepository,
                                  UserRepository userRepository,
                                  TenantRepository tenantRepository,
                                  CrossTenantScopeAccess crossTenant,
                                  AuditService auditService,
                                  EmailService emailService) {
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.crossTenant = crossTenant;
        this.auditService = auditService;
        this.emailService = emailService;
    }

    public record OwnershipView(UUID ownerUserId, String ownerEmail, String ownerName,
                                List<Map<String, Object>> admins) {
    }

    /** Lecture sous filtre suspendu : le tenant courant est bien le bon, mais
     *  les memberships portent la FK role en lazy — on stabilise le graphe. */
    @Transactional(readOnly = true)
    public OwnershipView overview(UUID tenantId) {
        return crossTenant.call(() -> {
            TenantMembership owner = findOwnerMembership(tenantId);
            User ownerUser = owner == null ? null
                    : userRepository.findById(owner.getUserId()).orElse(null);
            List<TenantMembership> admins = membershipRepository
                    .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                    .filter(m -> ADMIN_KEY.equals(m.getRoleLegacy()))
                    .toList();
            List<Map<String, Object>> adminViews = admins.stream()
                    .map(m -> {
                        User u = userRepository.findById(m.getUserId()).orElse(null);
                        return u == null ? Map.<String, Object>of() : Map.<String, Object>of(
                                "userId", u.getId(),
                                "email", u.getEmail(),
                                "name", (u.getFirstName() == null ? "" : u.getFirstName() + " ")
                                        + (u.getLastName() == null ? "" : u.getLastName()),
                                "joinedAt", m.getJoinedAt());
                    })
                    .filter(m -> !m.isEmpty())
                    .toList();
            return new OwnershipView(
                    ownerUser == null ? null : ownerUser.getId(),
                    ownerUser == null ? null : ownerUser.getEmail(),
                    ownerUser == null ? null
                            : (ownerUser.getFirstName() + " " + ownerUser.getLastName()),
                    adminViews);
        });
    }

    /**
     * Transfert de propriété : réservé au {@code TENANT_OWNER} courant ; la
     * cible doit déjà être membre ACTIVE de l'église (sinon 404 — on n'ajoute
     * pas un inconnu au passage). L'ancien owner rétrograde {@code TENANT_ADMIN}
     * (jamais simple membre : il garde la main pour la récupération).
     */
    @Transactional
    public OwnershipView transfer(UUID tenantId, UUID actorUserId, UUID toUserId) {
        return crossTenant.call(() -> {
            TenantMembership currentOwner = requireOwnerMembership(tenantId, actorUserId);
            TenantMembership target = membershipRepository
                    .findByUserIdAndTenantIdAndStatus(toUserId, tenantId, MembershipStatus.ACTIVE)
                    .orElseThrow(() -> new DomainException(
                            "La cible n'est pas membre de cette église",
                            HttpStatus.NOT_FOUND, "TRANSFER_TARGET_NOT_MEMBER"));
            if (target.getUserId().equals(actorUserId)) {
                throw new DomainException("Vous êtes déjà propriétaire",
                        HttpStatus.BAD_REQUEST, "ALREADY_OWNER");
            }
            Role ownerRole = globalRole(OWNER_KEY);
            Role adminRole = globalRole(ADMIN_KEY);

            target.setRole(ownerRole);
            target.setRoleLegacy(OWNER_KEY);
            membershipRepository.save(target);

            currentOwner.setRole(adminRole);
            currentOwner.setRoleLegacy(ADMIN_KEY);
            membershipRepository.save(currentOwner);

            auditService.logSimple("TENANT_OWNERSHIP_TRANSFERRED", "TENANT", tenantId);
            sendNoticeQuietly(tenantId, target.getUserId(), currentOwner.getUserId(), "transférée");
            return overview(tenantId);
        });
    }

    /** Délégation WhatsApp : un owner promeut un membre TENANT_ADMIN. */
    @Transactional
    public void promoteAdmin(UUID tenantId, UUID actorUserId, UUID targetUserId) {
        requireOwnerMembership(tenantId, actorUserId);
        crossTenant.call(() -> {
            TenantMembership target = membershipRepository
                    .findByUserIdAndTenantIdAndStatus(targetUserId, tenantId, MembershipStatus.ACTIVE)
                    .orElseThrow(() -> new DomainException("Cible non membre",
                            HttpStatus.NOT_FOUND, "TARGET_NOT_MEMBER"));
            if (OWNER_KEY.equals(target.getRoleLegacy())) {
                throw new DomainException("Cible déjà propriétaire",
                        HttpStatus.BAD_REQUEST, "ALREADY_OWNER");
            }
            Role adminRole = globalRole(ADMIN_KEY);
            target.setRole(adminRole);
            target.setRoleLegacy(ADMIN_KEY);
            membershipRepository.save(target);
            auditService.logSimple("TENANT_ADMIN_PROMOTED", "USER", targetUserId);
            return null;
        });
    }

    /** Révocation de délégation : TENANT_ADMIN → MEMBRE (l'owner uniquement). */
    @Transactional
    public void demoteAdmin(UUID tenantId, UUID actorUserId, UUID targetUserId) {
        requireOwnerMembership(tenantId, actorUserId);
        crossTenant.call(() -> {
            TenantMembership target = membershipRepository
                    .findByUserIdAndTenantIdAndStatus(targetUserId, tenantId, MembershipStatus.ACTIVE)
                    .orElseThrow(() -> new DomainException("Cible non membre",
                            HttpStatus.NOT_FOUND, "TARGET_NOT_MEMBER"));
            if (!ADMIN_KEY.equals(target.getRoleLegacy())) {
                throw new DomainException("Cible n'est pas administrateur délégué",
                        HttpStatus.BAD_REQUEST, "NOT_A_DELEGATED_ADMIN");
            }
            Role memberRole = roleRepository.findByTenantIdAndKey(tenantId, MEMBER_KEY)
                    .or(() -> roleRepository.findGlobalByKey(MEMBER_KEY))
                    .orElseThrow(() -> new DomainException("Rôle membre indisponible",
                            HttpStatus.CONFLICT, "MEMBER_ROLE_MISSING"));
            target.setRole(memberRole);
            target.setRoleLegacy(memberRole.getKey());
            membershipRepository.save(target);
            auditService.logSimple("TENANT_ADMIN_DEMOTED", "USER", targetUserId);
            return null;
        });
    }

    /**
     * Demande de remplacement quand l'owner est injoignable (D6) : posée par un
     * {@code TENANT_ADMIN}, JAMAIS automatique — la réponse suggère le membre
     * le plus ancien pour l'arbitrage de la plateforme, l'audit garde la trace.
     */
    @Transactional
    public Map<String, Object> requestReplacement(UUID tenantId, UUID actorUserId, String reason) {
        TenantMembership actor = membershipRepository
                .findByUserIdAndTenantIdAndStatus(actorUserId, tenantId, MembershipStatus.ACTIVE)
                .orElseThrow(() -> new DomainException("Vous n'êtes pas membre",
                        HttpStatus.NOT_FOUND, "NOT_A_MEMBER"));
        if (!ADMIN_KEY.equals(actor.getRoleLegacy())) {
            throw new DomainException("Seul un administrateur délégué peut demander un remplacement",
                    HttpStatus.FORBIDDEN, "REPLACEMENT_ADMIN_ONLY");
        }
        String suggested = crossTenant.call(() -> membershipRepository
                .findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
                .filter(m -> ADMIN_KEY.equals(m.getRoleLegacy()) && !m.getUserId().equals(actorUserId))
                .min(Comparator.comparing(TenantMembership::getJoinedAt))
                .map(m -> m.getUserId().toString())
                .orElse(null));
        auditService.log(actorUserId, tenantId, "OWNERSHIP_REPLACEMENT_REQUESTED", "TENANT", tenantId,
                "SUCCESS", Map.of("reason", reason == null ? "" : reason,
                        "suggestedCandidate", suggested == null ? "" : suggested), null, null, null);
        java.util.Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("status", "REQUESTED");
        response.put("message", "Demande enregistrée — la plateforme (Super Admin) arbitrera.");
        response.put("suggestedCandidate", suggested);
        return response;
    }

    // ======================== INTERNAL ========================

    private TenantMembership findOwnerMembership(UUID tenantId) {
        return membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                        tenantId, MembershipStatus.ACTIVE, OWNER_KEY).stream()
                .filter(m -> OWNER_KEY.equals(m.getRoleLegacy()))
                .findFirst()
                .orElse(null);
    }

    private TenantMembership requireOwnerMembership(UUID tenantId, UUID userId) {
        TenantMembership membership = crossTenant.call(() -> membershipRepository
                .findByUserIdAndTenantIdAndStatus(userId, tenantId, MembershipStatus.ACTIVE)
                .orElse(null));
        if (membership == null || !OWNER_KEY.equals(membership.getRoleLegacy())) {
            throw new DomainException("Action réservée au propriétaire de l'église",
                    HttpStatus.FORBIDDEN, "OWNER_REQUIRED");
        }
        return membership;
    }

    private Role globalRole(String key) {
        return roleRepository.findGlobalByKey(key)
                .orElseThrow(() -> new DomainException("Rôle global indisponible : " + key,
                        HttpStatus.CONFLICT, "ROLE_MISSING"));
    }

    private void sendNoticeQuietly(UUID tenantId, UUID newOwnerUserId, UUID oldOwnerUserId, String verb) {
        try {
            String tenantName = tenantRepository.findById(tenantId).map(Tenant::getName).orElse("votre église");
            for (UUID uid : List.of(newOwnerUserId, oldOwnerUserId)) {
                userRepository.findById(uid).ifPresent(u -> {
                    try {
                        emailService.send(u.getEmail(),
                                "Propriété de " + tenantName + " " + verb,
                                "Bonjour " + (u.getFirstName() == null ? "" : u.getFirstName()) + ",\n\n"
                                        + "La propriété de l'église « " + tenantName + " » a été " + verb
                                        + " sur Discipolat. Si vous n'êtes pas à l'origine de ce changement, "
                                        + "contactez immédiatement la plateforme.\n\nL'équipe Discipolat");
                    } catch (RuntimeException ignored) {
                        log.debug("Email de transfert non envoyé à {}", u.getEmail());
                    }
                });
            }
        } catch (RuntimeException failure) {
            log.warn("Notification de transfert impossible (tenant {}) : {}", tenantId, failure.getMessage());
        }
    }
}
