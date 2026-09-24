package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/invitations")
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final TenantRepository tenantRepository;
    private final AuthorizationService authzService;
    private final AuditService auditService;
    private final com.discipolat.modules.authentication.domain.EmailService emailService;
    private final com.discipolat.common.infrastructure.config.PerIpRateLimiter rateLimiter;
    private final String frontendUrl;

    public InvitationController(InvitationRepository invitationRepository,
                                InvitationService invitationService,
                                UserRepository userRepository,
                                TenantMembershipRepository membershipRepository,
                                RoleRepository roleRepository,
                                OrganizationNodeRepository orgNodeRepository,
                                TenantRepository tenantRepository,
                                AuthorizationService authzService,
                                 AuditService auditService,
                                 com.discipolat.modules.authentication.domain.EmailService emailService,
                                 com.discipolat.common.infrastructure.config.PerIpRateLimiter rateLimiter,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.invitationRepository = invitationRepository;
        this.invitationService = invitationService;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.tenantRepository = tenantRepository;
        this.authzService = authzService;
        this.auditService = auditService;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.frontendUrl = frontendUrl;
    }

    // ==================== CREATE INVITATION ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> createInvitation(@RequestBody Map<String, Object> request) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        String email = (String) request.get("email");
        String roleKey = (String) request.get("role");
        UUID organizationNodeId = request.get("organizationNodeId") != null ?
                UUID.fromString((String) request.get("organizationNodeId")) : null;
        MembershipScopeType scopeType = request.get("scopeType") != null ?
                MembershipScopeType.valueOf((String) request.get("scopeType")) : MembershipScopeType.TENANT;
        UUID scopeId = request.get("scopeId") != null ?
                UUID.fromString((String) request.get("scopeId")) : null;

        if (email == null || email.isBlank() || roleKey == null || roleKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email et role sont requis"));
        }

        // Validate role exists
        Optional<Role> role = roleRepository.findByTenantIdAndKey(tenantId, roleKey.toUpperCase());
        if (role.isEmpty()) {
            role = roleRepository.findByTenantIdIsNullAndKey(roleKey.toUpperCase());
        }
        if (role.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rôle invalide: " + roleKey));
        }
        if (role.get().getTenantId() == null && role.get().getKey().startsWith("PLATFORM_")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Un rôle plateforme ne peut pas être attribué à une invitation tenant"));
        }

        if (organizationNodeId != null) {
            Optional<OrganizationNode> node = orgNodeRepository.findById(organizationNodeId);
            if (node.isEmpty() || !node.get().getTenantId().equals(tenantId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nœud organisationnel invalide"));
            }
        }

        UUID effectiveScopeId = scopeId != null ? scopeId : organizationNodeId;
        if (scopeType == MembershipScopeType.TENANT && effectiveScopeId != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Un scope tenant ne doit pas contenir de ressource"));
        }
        if (scopeType != MembershipScopeType.TENANT
                && (organizationNodeId == null || !organizationNodeId.equals(effectiveScopeId))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le scope de l'invitation est invalide"));
        }

        // Check if user already exists
        Optional<User> existingUser = userRepository.findByTenantIdAndEmail(
                tenantId, email.toLowerCase());
        if (existingUser.isPresent()) {
            User user = existingUser.get();

            // Check if already member of this tenant
            Optional<TenantMembership> existingMembership = membershipRepository
                    .findByUserIdAndTenantIdAndStatus(user.getId(), tenantId, MembershipStatus.ACTIVE);

            if (existingMembership.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Cet utilisateur appartient déjà à ce tenant",
                        "userId", user.getId().toString()
                ));
            }

            // Create membership directly
            TenantMembership membership = TenantMembership.builder()
                    .tenantId(tenantId)
                    .userId(user.getId())
                    .role(role.get())
                    .roleLegacy(role.get().getKey())
                    .scopeType(scopeType)
                    .scopeId(scopeId != null ? scopeId : (organizationNodeId != null ? organizationNodeId : null))
                    .status(MembershipStatus.ACTIVE)
                    .invitedBy(currentUserId)
                    .build();
            membershipRepository.save(membership);

            auditService.logSimple("USER_INVITED_EXISTING", "USER", user.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "invitedUserId", user.getId().toString(),
                    "email", email,
                    "role", roleKey,
                    "message", "Utilisateur ajouté directement (compte existant)"
            ));
        }

        // Check if invitation already exists for this email
        Optional<Invitation> existingInvitation = invitationRepository
                .findByTenantIdAndEmailAndStatus(tenantId, email.toLowerCase(), InvitationStatus.PENDING);
        if (existingInvitation.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Une invitation en attente existe déjà pour cet email",
                    "invitationId", existingInvitation.get().getId().toString()
            ));
        }

        // Create invitation
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        Invitation invitation = Invitation.builder()
                .tenantId(tenantId)
                .email(email.toLowerCase())
                .role(roleKey.toUpperCase())
                .scopeType(scopeType != null ? scopeType.name() : null)
                .scopeId(scopeId != null ? scopeId : (organizationNodeId != null ? organizationNodeId : null))
                .inviterId(currentUserId)
                .tokenHash(InvitationTokenHasher.hash(token))
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600)) // 7 days
                .organizationNodeId(organizationNodeId)
                .build();
        invitationRepository.save(invitation);

        auditService.logSimple("INVITATION_CREATED", "INVITATION", invitation.getId());

        String invitationLink = invitationLink(token);
        boolean emailSent = sendInvitationEmail(
                email.toLowerCase(), roleKey.toUpperCase(), tenantId, invitationLink);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("invitationId", invitation.getId().toString());
        response.put("email", email);
        response.put("role", roleKey);
        response.put("scopeType", scopeType != null ? scopeType.name() : null);
        response.put("scopeId", scopeId != null ? scopeId.toString() : null);
        response.put("invitationToken", token);
        response.put("invitationLink", invitationLink);
        response.put("emailSent", emailSent);
        response.put("expiresAt", invitation.getExpiresAt().toString());
        response.put("message", "Invitation créée. Envoyez le lien à l'utilisateur : " + invitationLink);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== LIST INVITATIONS ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listInvitations() {
        UUID tenantId = TenantContext.requireTenantId();
        List<Invitation> invitations = invitationRepository.findByTenantIdAndStatusIn(
                tenantId, List.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED, InvitationStatus.EXPIRED));

        return ResponseEntity.ok(invitations.stream().map(this::toMap).toList());
    }

    // ==================== GET INVITATION DETAILS ====================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        Optional<Invitation> invitation = invitationRepository.findById(id);

        if (invitation.isEmpty() || !invitation.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toMap(invitation.get()));
    }

    // ==================== CANCEL INVITATION ====================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    @Transactional
    public ResponseEntity<?> cancelInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Optional<Invitation> invitation = invitationRepository.findByIdForUpdate(id);
        if (invitation.isEmpty() || !invitation.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.notFound().build();
        }

        Invitation inv = invitation.get();
        if (inv.getStatus() != InvitationStatus.PENDING) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Seules les invitations en attente peuvent être annulées");
            return (ResponseEntity<Map<String, Object>>) ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
        }

        inv.setStatus(InvitationStatus.CANCELED);
        invitationRepository.save(inv);

        auditService.logSimple("INVITATION_CANCELLED", "INVITATION", id);

        return ResponseEntity.noContent().build();
    }

    // ==================== RESEND INVITATION ====================

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    @Transactional
    public ResponseEntity<Map<String, Object>> resendInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Optional<Invitation> invitation = invitationRepository.findByIdForUpdate(id);
        if (invitation.isEmpty() || !invitation.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.notFound().build();
        }

        Invitation inv = invitation.get();
        if (inv.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Seules les invitations en attente peuvent être renvoyées"));
        }

        // Generate new token and extend expiry
        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        inv.setTokenHash(InvitationTokenHasher.hash(newToken));
        inv.setExpiresAt(Instant.now().plusSeconds(7 * 24 * 3600));
        invitationRepository.save(inv);

        auditService.logSimple("INVITATION_RESENT", "INVITATION", id);

        String invitationLink = invitationLink(newToken);
        boolean emailSent = sendInvitationEmail(
                inv.getEmail(), inv.getRole(), tenantId, invitationLink);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "invitationToken", newToken,
                "invitationLink", invitationLink,
                "emailSent", emailSent,
                "expiresAt", inv.getExpiresAt().toString(),
                "message", "Invitation renvoyée"
        ));
    }

    // ==================== PUBLIC: ACCEPT INVITATION ====================

    @GetMapping("/validate/{token}")
    public ResponseEntity<Map<String, Object>> validateInvitation(@PathVariable String token) {
        Invitation inv = invitationService.validate(token);
        Optional<Tenant> tenant = tenantRepository.findById(inv.getTenantId());
        Optional<OrganizationNode> orgNode = inv.getOrganizationNodeId() != null ?
                orgNodeRepository.findById(inv.getOrganizationNodeId()) : Optional.empty();

        Map<String, Object> response = new HashMap<>();
        response.put("valid", true);
        response.put("email", inv.getEmail());
        response.put("role", inv.getRole());
        response.put("scopeType", inv.getScopeType() != null ? inv.getScopeType() : "TENANT");
        response.put("tenantName", tenant.map(Tenant::getName).orElse("Inconnu"));
        response.put("organizationName", orgNode.map(OrganizationNode::getName).orElse(null));
        response.put("expiresAt", inv.getExpiresAt().toString());
        response.put("accountExists", userRepository.findByTenantIdAndEmail(
                inv.getTenantId(), inv.getEmail()).isPresent());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<Map<String, Object>> acceptInvitation(@PathVariable String token,
                                                                 @RequestBody(required = false) Map<String, String> request,
                                                                 jakarta.servlet.http.HttpServletRequest httpRequest) {
        var ip = httpRequest != null ? httpRequest.getRemoteAddr() : "unknown";
        var rl = rateLimiter.tryConsumeInvitationAccept(ip);
        if (!rl.allowed()) {
            return ResponseEntity.status(429)
                    .cacheControl(CacheControl.noStore())
                    .headers(h -> {
                        h.set("Retry-After", String.valueOf(rl.retryAfterSeconds()));
                        h.set("X-RateLimit-Remaining", "0");
                    })
                    .body(Map.of("error", "Trop de tentatives, réessayez plus tard"));
        }

        String password = request != null ? request.get("password") : null;
        String firstName = request != null ? request.get("firstName") : null;
        String lastName = request != null ? request.get("lastName") : null;
        InvitationService.AcceptanceResult result = invitationService.accept(token, password, firstName, lastName);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "success", true,
                        "userId", result.userId().toString(),
                        "email", result.email(),
                        "tenantId", result.tenantId().toString(),
                        "alreadyMember", result.alreadyMember(),
                        "message", "Invitation acceptée avec succès"
                ));
    }

    private String invitationLink(String token) {
        return frontendUrl + "/accept-invitation?token=" + token;
    }

    private boolean sendInvitationEmail(
            String email,
            String roleKey,
            UUID tenantId,
            String invitationLink) {
        try {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            String tenantName = tenant != null ? tenant.getName() : "Discipolat";
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
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Map<String, Object> toMap(Invitation inv) {
        Optional<Tenant> tenant = tenantRepository.findById(inv.getTenantId());
        Optional<OrganizationNode> orgNode = inv.getOrganizationNodeId() != null ?
                orgNodeRepository.findById(inv.getOrganizationNodeId()) : Optional.empty();

        Map<String, Object> map = new HashMap<>();
        map.put("id", inv.getId().toString());
        map.put("email", inv.getEmail());
        map.put("role", inv.getRole());
        map.put("scopeType", inv.getScopeType() != null ? inv.getScopeType() : "TENANT");
        map.put("scopeId", inv.getScopeId() != null ? inv.getScopeId().toString() : null);
        map.put("organizationNodeId", inv.getOrganizationNodeId() != null ? inv.getOrganizationNodeId().toString() : null);
        map.put("organizationNodeName", orgNode.map(OrganizationNode::getName).orElse(null));
        map.put("status", inv.getStatus().name());
        map.put("invitedBy", inv.getInviterId() != null ? inv.getInviterId().toString() : null);
        map.put("createdAt", inv.getCreatedAt().toString());
        map.put("expiresAt", inv.getExpiresAt().toString());
        map.put("acceptedAt", inv.getAcceptedAt() != null ? inv.getAcceptedAt().toString() : null);
        map.put("tenantName", tenant.map(Tenant::getName).orElse("Inconnu"));
        return map;
    }
}