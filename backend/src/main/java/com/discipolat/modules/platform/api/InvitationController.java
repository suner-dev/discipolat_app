package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/invitations")
public class InvitationController {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantMembershipRepository membershipRepository;
    private final RoleRepository roleRepository;
    private final OrganizationNodeRepository orgNodeRepository;
    private final TenantRepository tenantRepository;
    private final AuthorizationService authzService;
    private final AuditService auditService;

    public InvitationController(InvitationRepository invitationRepository,
                                UserRepository userRepository,
                                TenantMembershipRepository membershipRepository,
                                RoleRepository roleRepository,
                                OrganizationNodeRepository orgNodeRepository,
                                TenantRepository tenantRepository,
                                AuthorizationService authzService,
                                AuditService auditService) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.orgNodeRepository = orgNodeRepository;
        this.tenantRepository = tenantRepository;
        this.authzService = authzService;
        this.auditService = auditService;
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

        // Validate scope if provided
        if (organizationNodeId != null) {
            Optional<OrganizationNode> node = orgNodeRepository.findById(organizationNodeId);
            if (node.isEmpty() || !node.get().getTenantId().equals(tenantId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Nœud organisationnel invalide"));
            }
        }

        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(email.toLowerCase());
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
                    .scopeType(scopeType)
                    .scopeId(scopeId != null ? scopeId : (organizationNodeId != null ? organizationNodeId : null))
                    .status(MembershipStatus.ACTIVE)
                    .invitedBy(currentUserId)
                    .build();
            membershipRepository.save(membership);

            auditService.log(currentUserId, tenantId, "USER_INVITED_EXISTING",
                    "USER", user.getId(), "SUCCESS", Map.of("email", email, "role", roleKey));

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
                .scopeType(scopeType)
                .scopeId(scopeId != null ? scopeId : (organizationNodeId != null ? organizationNodeId : null))
                .inviterId(currentUserId)
                .invitationToken(token)
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600)) // 7 days
                .organizationNodeId(organizationNodeId)
                .build();
        invitationRepository.save(invitation);

        auditService.log(currentUserId, tenantId, "INVITATION_CREATED",
                "INVITATION", invitation.getId(), "SUCCESS", Map.of("email", email, "role", roleKey));

        // TODO: Send email with invitation link
        String invitationLink = "/auth/accept-invitation?token=" + token;

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "invitationId", invitation.getId().toString(),
                "email", email,
                "role", roleKey,
                "scopeType", scopeType.name(),
                "scopeId", scopeId != null ? scopeId.toString() : null,
                "invitationToken", token,
                "invitationLink", invitationLink,
                "expiresAt", invitation.getExpiresAt().toString(),
                "message", "Invitation créée. Envoyez le lien à l'utilisateur : " + invitationLink
        ));
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
    public ResponseEntity<Void> cancelInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Optional<Invitation> invitation = invitationRepository.findById(id);
        if (invitation.isEmpty() || !invitation.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.notFound().build();
        }

        Invitation inv = invitation.get();
        if (inv.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Seules les invitations en attente peuvent être annulées"));
        }

        inv.setStatus(InvitationStatus.CANCELED);
        invitationRepository.save(inv);

        auditService.log(currentUserId, tenantId, "INVITATION_CANCELLED",
                "INVITATION", id, "SUCCESS", Map.of("email", inv.getEmail()));

        return ResponseEntity.noContent().build();
    }

    // ==================== RESEND INVITATION ====================

    @PostMapping("/{id}/resend")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> resendInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Optional<Invitation> invitation = invitationRepository.findById(id);
        if (invitation.isEmpty() || !invitation.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.notFound().build();
        }

        Invitation inv = invitation.get();
        if (inv.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.badRequest().body(Map.of("error", "Seules les invitations en attente peuvent être renvoyées"));
        }

        // Generate new token and extend expiry
        String newToken = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        inv.setInvitationToken(newToken);
        inv.setExpiresAt(Instant.now().plusSeconds(7 * 24 * 3600));
        invitationRepository.save(inv);

        auditService.log(currentUserId, tenantId, "INVITATION_RESENT",
                "INVITATION", id, "SUCCESS", Map.of("email", inv.getEmail()));

        String invitationLink = "/auth/accept-invitation?token=" + newToken;

        return ResponseEntity.ok(Map.of(
                "success", true,
                "invitationToken", newToken,
                "invitationLink", invitationLink,
                "expiresAt", inv.getExpiresAt().toString(),
                "message", "Invitation renvoyée"
        ));
    }

    // ==================== PUBLIC: ACCEPT INVITATION ====================

    @GetMapping("/validate/{token}")
    public ResponseEntity<Map<String, Object>> validateInvitation(@PathVariable String token) {
        Optional<Invitation> invitation = invitationRepository.findByInvitationToken(token);

        if (invitation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Invitation invalide"));
        }

        Invitation inv = invitation.get();

        if (inv.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Invitation expirée ou déjà utilisée", "status", inv.getStatus().name()));
        }

        if (inv.getExpiresAt().isBefore(Instant.now())) {
            inv.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(inv);
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Invitation expirée"));
        }

        Optional<Tenant> tenant = tenantRepository.findById(inv.getTenantId());
        Optional<OrganizationNode> orgNode = inv.getOrganizationNodeId() != null ?
                orgNodeRepository.findById(inv.getOrganizationNodeId()) : Optional.empty();

        return ResponseEntity.ok(Map.of(
                "valid", true,
                "email", inv.getEmail(),
                "role", inv.getRole(),
                "scopeType", inv.getScopeType() != null ? inv.getScopeType().name() : "TENANT",
                "tenantName", tenant.map(Tenant::getName).orElse("Inconnu"),
                "organizationName", orgNode.map(OrganizationNode::getName).orElse(null),
                "expiresAt", inv.getExpiresAt().toString()
        ));
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<Map<String, Object>> acceptInvitation(@PathVariable String token,
                                                                 @RequestBody(required = false) Map<String, String> request) {
        Optional<Invitation> invitation = invitationRepository.findByInvitationToken(token);

        if (invitation.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Invitation invalide"));
        }

        Invitation inv = invitation.get();

        if (inv.getStatus() != InvitationStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Invitation expirée ou déjà utilisée", "status", inv.getStatus().name()));
        }

        if (inv.getExpiresAt().isBefore(Instant.now())) {
            inv.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(inv);
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("error", "Invitation expirée"));
        }

        String password = request != null ? request.get("password") : null;
        String firstName = request != null ? request.get("firstName") : null;
        String lastName = request != null ? request.get("lastName") : null;

        // Create or find user
        User user;
        Optional<User> existingUser = userRepository.findByEmail(inv.getEmail());
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            if (password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe requis pour nouveau compte"));
            }
            user = User.builder()
                    .tenantId(inv.getTenantId())
                    .email(inv.getEmail())
                    .passwordHash(password) // Will be encoded by service
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .statut(com.discipolat.modules.users.domain.UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
        }

        // Create membership
        Optional<Role> role = roleRepository.findByTenantIdAndKey(inv.getTenantId(), inv.getRole());
        if (role.isEmpty()) {
            role = roleRepository.findByTenantIdIsNullAndKey(inv.getRole());
        }

        TenantMembership membership = TenantMembership.builder()
                .tenantId(inv.getTenantId())
                .userId(user.getId())
                .role(role.orElseThrow())
                .scopeType(inv.getScopeType() != null ? inv.getScopeType() : MembershipScopeType.TENANT)
                .scopeId(inv.getScopeId())
                .status(MembershipStatus.ACTIVE)
                .invitedBy(inv.getInviterId())
                .build();
        membershipRepository.save(membership);

        // Update invitation
        inv.setStatus(InvitationStatus.ACCEPTED);
        inv.setAcceptedAt(Instant.now());
        invitationRepository.save(inv);

        auditService.log(inv.getInviterId(), inv.getTenantId(), "INVITATION_ACCEPTED",
                "INVITATION", inv.getId(), "SUCCESS", Map.of("email", inv.getEmail(), "userId", user.getId().toString()));

        return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "tenantId", inv.getTenantId().toString(),
                "message", "Invitation acceptée avec succès"
        ));
    }

    private Map<String, Object> toMap(Invitation inv) {
        Optional<Tenant> tenant = tenantRepository.findById(inv.getTenantId());
        Optional<OrganizationNode> orgNode = inv.getOrganizationNodeId() != null ?
                orgNodeRepository.findById(inv.getOrganizationNodeId()) : Optional.empty();

        return Map.of(
                "id", inv.getId().toString(),
                "email", inv.getEmail(),
                "role", inv.getRole(),
                "scopeType", inv.getScopeType() != null ? inv.getScopeType().name() : "TENANT",
                "scopeId", inv.getScopeId() != null ? inv.getScopeId().toString() : null,
                "organizationNodeId", inv.getOrganizationNodeId() != null ? inv.getOrganizationNodeId().toString() : null,
                "organizationNodeName", orgNode.map(OrganizationNode::getName).orElse(null),
                "status", inv.getStatus().name(),
                "invitedBy", inv.getInviterId() != null ? inv.getInviterId().toString() : null,
                "createdAt", inv.getCreatedAt().toString(),
                "expiresAt", inv.getExpiresAt().toString(),
                "acceptedAt", inv.getAcceptedAt() != null ? inv.getAcceptedAt().toString() : null,
                "tenantName", tenant.map(Tenant::getName).orElse("Inconnu")
        );
    }
}