package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.security.domain.TokenRevocationService;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * §G1.9 — Impersonation Super Admin : workflow complet avec journalisation
 * et SANS élévation de privilège.
 *
 * <p>Règles d'intégrité :
 * <ul>
 *   <li>Seul un {@code PLATFORM_SUPER_ADMIN} réel (vérifié en base via
 *       {@link AuthorizationService#isPlatformSuperAdmin}, jamais via le seul rôle
 *       actif du JWT) peut démarrer une impersonation.</li>
 *   <li>Il est interdit d'impersonner un utilisateur porteur du rôle
 *       {@code PLATFORM_SUPER_ADMIN} (anti-escalade, vérification en base par
 *       rôle — pas par email).</li>
 *   <li>Le token émis est un JWT court (30 min) portant l'identité de la
 *       CIBLE : l'impersonateur n'obtient que les permissions de la cible.</li>
 *   <li>Toute la session est journalisée (qui/quoi/quand/IP/durée).</li>
 * </ul>
 */
@Service
public class ImpersonationService {

    private static final Logger log = LoggerFactory.getLogger(ImpersonationService.class);

    /** TTL court de la session d'impersonation (minutes). */
    public static final long IMPERSONATION_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantMembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final AuthorizationService authorizationService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuditService auditService;
    private final TokenRevocationService tokenRevocationService;

    public ImpersonationService(UserRepository userRepository,
                                RoleRepository roleRepository,
                                TenantMembershipRepository membershipRepository,
                                TenantRepository tenantRepository,
                                AuthorizationService authorizationService,
                                JwtTokenProvider jwtTokenProvider,
                                AuditService auditService,
                                TokenRevocationService tokenRevocationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.authorizationService = authorizationService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.auditService = auditService;
        this.tokenRevocationService = tokenRevocationService;
    }

    /**
     * Démarre une session d'impersonation de {@code targetUserEmail} et renvoie
     * un JWT d'impersonation (identité cible, TTL court) + les métadonnées de session.
     */
    @Transactional
    public ImpersonationSession start(UUID realAdminId, UUID tenantId, String targetUserEmail,
                                      String reason, String ipAddress, String userAgent) {
        if (targetUserEmail == null || targetUserEmail.isBlank()) {
            throw new BusinessRuleException("L'email de l'utilisateur cible est requis", "IMPERSONATION_TARGET_REQUIRED");
        }
        if (tenantId == null) {
            throw new BusinessRuleException("Le tenant cible est requis", "IMPERSONATION_TENANT_REQUIRED");
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Un motif est requis pour démarrer une impersonation", "IMPERSONATION_REASON_REQUIRED");
        }
        // Garde-fou 1 : l'appelant doit être un super admin plateforme RÉEL
        // (vérification en base, pas seulement le rôle actif du JWT).
        if (!authorizationService.isPlatformSuperAdmin(realAdminId)) {
            throw new BusinessRuleException("Seul un super admin plateforme peut impersoner",
                    "IMPERSONATION_NOT_SUPER_ADMIN");
        }
        User target = userRepository.findByTenantIdAndEmail(tenantId, targetUserEmail.trim().toLowerCase())
                .orElseThrow(() -> new BusinessRuleException("Utilisateur cible introuvable",
                        "IMPERSONATION_TARGET_NOT_FOUND"));
        // Garde-fou 2 (anti-escalade) : vérification PAR RÔLE en base — l'utilisateur
        // cible ne doit pas porter le rôle global PLATFORM_SUPER_ADMIN.
        if (isPlatformSuperAdminUser(target.getId())) {
            throw new BusinessRuleException("Impossible d'impersoner un super admin plateforme",
                    "SUPER_ADMIN_IMPERSONATION_FORBIDDEN");
        }
        // Garde-fou 3 : la cible doit appartenir au tenant ciblé.
        UUID effectiveTenantId = target.getTenantId() != null ? target.getTenantId() : tenantId;
        if (effectiveTenantId == null) {
            throw new BusinessRuleException("Utilisateur cible sans tenant — impersonation impossible",
                    "IMPERSONATION_TARGET_NO_TENANT");
        }
        if (tenantId != null && !tenantId.equals(effectiveTenantId)) {
            throw new BusinessRuleException("L'utilisateur cible n'appartient pas à ce tenant",
                    "IMPERSONATION_TENANT_MISMATCH");
        }
        Tenant tenant = tenantRepository.findById(effectiveTenantId)
                .orElseThrow(() -> new BusinessRuleException("Tenant non trouvé", "IMPERSONATION_TENANT_NOT_FOUND"));

        // Le token porte l'identité de la CIBLE : aucune élévation possible.
        String activeRole = target.getActiveRole() != null
                ? target.getActiveRole().name()
                : target.getRole().name();
        java.util.Set<String> roles = target.getRoles() != null && !target.getRoles().isEmpty()
                ? target.getRoles().stream().map(Enum::name).collect(Collectors.toSet())
                : java.util.Set.of(activeRole);

        Instant startTime = Instant.now();
        String token = jwtTokenProvider.generateImpersonationToken(
                target.getId(), target.getEmail(), activeRole, roles,
                effectiveTenantId, realAdminId, IMPERSONATION_TTL_MINUTES);

        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("targetUserId", target.getId().toString());
        details.put("targetUserEmail", target.getEmail());
        details.put("targetRole", activeRole);
        details.put("impersonationTokenType", "JWT");
        auditService.log(realAdminId, effectiveTenantId, "IMPERSONATION_START", "TENANT",
                effectiveTenantId, "SUCCESS", details, ipAddress, userAgent, null);

        log.info("IMPERSONATION_START admin={} target={} tenant={} ip={}",
                realAdminId, target.getEmail(), effectiveTenantId, ipAddress);

        return new ImpersonationSession(token, target, effectiveTenantId, tenant.getName(),
                activeRole, startTime, startTime.plusSeconds(IMPERSONATION_TTL_MINUTES * 60));
    }

    /**
     * Termine la session d'impersonation et journalise la durée totale.
     * L'appelant présente le token d'impersonation : le claim `imp` identifie
     * le super admin réel sans lui restituer de pouvoir.
     */
    @Transactional
    public void stop(String impersonationToken, String ipAddress, String userAgent) {
        UUID realAdminId;
        UUID targetUserId;
        UUID tenantId;
        Instant issuedAt;
        try {
            var claims = jwtTokenProvider.getClaims(impersonationToken);
            realAdminId = UUID.fromString(claims.get("imp", String.class));
            targetUserId = UUID.fromString(claims.getSubject());
            String tid = claims.get("tenantId", String.class);
            tenantId = tid != null ? UUID.fromString(tid) : null;
            issuedAt = claims.getIssuedAt().toInstant();
        } catch (Exception e) {
            throw new BusinessRuleException("Token d'impersonation invalide", "IMPERSONATION_TOKEN_INVALID");
        }
        if (!"impersonation".equals(jwtTokenProvider.getClaims(impersonationToken).get("type", String.class))) {
            throw new BusinessRuleException("Token d'impersonation invalide", "IMPERSONATION_TOKEN_INVALID");
        }
        tokenRevocationService.revoke(impersonationToken, "impersonation",
                jwtTokenProvider.getTokenExpiration(impersonationToken), "stopped");
        long durationMinutes = java.time.Duration.between(issuedAt, Instant.now()).toMinutes();
        Map<String, Object> details = new HashMap<>();
        details.put("targetUserId", targetUserId.toString());
        details.put("durationMinutes", durationMinutes);
        auditService.log(realAdminId, tenantId, "IMPERSONATION_END", "TENANT",
                tenantId, "SUCCESS", details, ipAddress, userAgent, null);
        log.info("IMPERSONATION_END admin={} target={} durationMinutes={}", realAdminId, targetUserId, durationMinutes);
    }

    /** Vérifie en base que l'utilisateur porte le rôle global PLATFORM_SUPER_ADMIN (actif). */
    private boolean isPlatformSuperAdminUser(UUID userId) {
        return roleRepository.findByTenantIdIsNullAndKey("PLATFORM_SUPER_ADMIN")
                .map(role -> membershipRepository.existsByUserIdAndRoleIdAndStatus(
                        userId, role.getId(), MembershipStatus.ACTIVE))
                .orElse(false);
    }

    /** Session d'impersonation retournée au client (web). */
    public record ImpersonationSession(String token, UUID targetUserId, UUID tenantId,
                                       String tenantName, String targetRole,
                                       Instant startTime, Instant expiresAt) {
        public ImpersonationSession(String token, User target, UUID tenantId, String tenantName,
                                    String targetRole, Instant startTime, Instant expiresAt) {
            this(token, target.getId(), tenantId, tenantName, targetRole, startTime, expiresAt);
        }
    }
}