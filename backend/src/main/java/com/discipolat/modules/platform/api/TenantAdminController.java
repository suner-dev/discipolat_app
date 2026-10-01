package com.discipolat.modules.platform.api;

import com.discipolat.common.domain.Payloads;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tenant Admin Controller - Gestion de l'organisation par le tenant admin
 * Rôles requis : TENANT_OWNER, TENANT_ADMIN
 */
@RestController
@RequestMapping("/api/v1/admin")
public class TenantAdminController {

    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TenantMembershipService membershipService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuditService auditService;
    private final OrganizationNodeService orgNodeService;
    private final TenantService tenantService;

    public TenantAdminController(TenantRepository tenantRepository,
                                 TenantMembershipRepository membershipRepository,
                                 UserRepository userRepository,
                                 TenantMembershipService membershipService,
                                 RoleService roleService,
                                 PermissionService permissionService,
                                 AuditService auditService,
                                 OrganizationNodeService orgNodeService,
                                 TenantService tenantService) {
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.auditService = auditService;
        this.orgNodeService = orgNodeService;
        this.tenantService = tenantService;
    }

    /**
     * Dashboard du Tenant Admin
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        UUID tenantId = TenantContext.requireTenantId();
        
        long totalUsers = userRepository.countByTenantId(tenantId);
        long activeUsers = userRepository.countByTenantIdAndStatut(tenantId, UserStatus.ACTIVE);
        long totalMemberships = membershipRepository.countByTenantId(tenantId);
        
        Map<String, Long> membersByRole = new HashMap<>();
        membershipRepository.findByTenantId(tenantId).forEach(m -> {
            String roleKey = m.getRole() != null ? m.getRole().getKey() : "UNKNOWN";
            membersByRole.merge(roleKey, 1L, Long::sum);
        });
        
        long churchCount = orgNodeService.countByType(tenantId, OrganizationNodeType.ROOT_CHURCH);
        long departmentCount = orgNodeService.countByType(tenantId, OrganizationNodeType.DEPARTMENT);
        long subChurchCount = orgNodeService.countByType(tenantId, OrganizationNodeType.SUB_CHURCH);
        long campusCount = orgNodeService.countByType(tenantId, OrganizationNodeType.CAMPUS);
        long groupCount = orgNodeService.countByType(tenantId, OrganizationNodeType.GROUP);
        
        Optional<TenantSubscription> subscription = 
            orgNodeService.getTenantSubscription(tenantId); // Utilise le service existant
        
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("tenantId", tenantId.toString());
        dashboard.put("tenantName", tenantRepository.findById(tenantId).get().getName());
        dashboard.put("totalUsers", totalUsers);
        dashboard.put("activeUsers", activeUsers);
        dashboard.put("totalMemberships", totalMemberships);
        dashboard.put("membersByRole", membersByRole);
        dashboard.put("churchCount", churchCount);
        dashboard.put("departmentCount", departmentCount);
        dashboard.put("subChurchCount", subChurchCount);
        dashboard.put("campusCount", campusCount);
        dashboard.put("groupCount", groupCount);
        dashboard.put("subscription", subscription.map(s -> Payloads.of(
            "planKey", s.getPlanKey(),
            "status", s.getStatus().name(),
            "currentPeriodEnd", s.getCurrentPeriodEnd()
        )).orElse(null));
        dashboard.put("generatedAt", Instant.now());
        
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Liste des membres du tenant
     */
    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<PageResponse<Map<String, Object>>> listMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        
        UUID tenantId = TenantContext.requireTenantId();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<TenantMembership> membershipPage;
        if (role != null) {
            membershipPage = membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                tenantId, MembershipStatus.ACTIVE, role, pageRequest);
        } else {
            membershipPage = membershipRepository.findByTenantIdAndStatus(
                tenantId, MembershipStatus.ACTIVE, pageRequest);
        }
        
        List<TenantMembership> memberships = membershipPage.getContent();
        
        List<Map<String, Object>> content = memberships.stream().map(m -> {
            Optional<User> user = userRepository.findById(m.getUserId());
            String roleKey = m.getRole() != null ? m.getRole().getKey() : "UNKNOWN";
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("membershipId", m.getId().toString());
            map.put("userId", m.getUserId().toString());
            map.put("email", user.map(User::getEmail).orElse("unknown"));
            map.put("firstName", user.map(User::getFirstName).orElse(""));
            map.put("lastName", user.map(User::getLastName).orElse(""));
            map.put("fullName", (user.map(User::getFirstName).orElse("") + " " + user.map(User::getLastName).orElse("")).trim());
            map.put("role", roleKey);
            map.put("status", m.getStatus().name());
            map.put("joinedAt", m.getJoinedAt().toString());
            map.put("photoUrl", user.map(User::getPhotoUrl).orElse(null));
            map.put("phone", user.map(User::getPhone).orElse(null));
            map.put("isActive", m.getStatus() == MembershipStatus.ACTIVE);
            return map;
        }).collect(Collectors.toList());
        
        // Filtrer par recherche si fourni
        if (search != null && !search.isBlank()) {
            String lowerSearch = search.toLowerCase();
            content = content.stream()
                .filter(m -> m.get("fullName").toString().toLowerCase().contains(lowerSearch) ||
                             m.get("email").toString().toLowerCase().contains(lowerSearch))
                .collect(Collectors.toList());
        }
        
        return ResponseEntity.ok(PageResponse.of(content, page, size, content.size(), 1));
    }


    /**
     * §58 / §G3.2 — Affectation : changer le rôle d'un membre dans CE tenant.
     * La membership doit appartenir au tenant courant (isolation), le rôle
     * cible doit exister dans le tenant, et le dernier propriétaire actif ne
     * peut pas être rétrogradé. Action auditée.
     */
    @PutMapping("/members/{membershipId}/role")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateMemberRole(
            @PathVariable UUID membershipId,
            @RequestBody Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        UUID tenantId = TenantContext.requireTenantId();
        TenantMembership membership = tenantScopedMembership(membershipId, tenantId);

        String roleKey = body.get("roleKey");
        if (roleKey == null || roleKey.isBlank()) {
            throw new IllegalArgumentException("roleKey requis");
        }
        Role newRole = roleService.getRoles(tenantId).stream()
            .filter(r -> r.getKey().equalsIgnoreCase(roleKey.trim()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Rôle inconnu dans ce tenant : " + roleKey));

        Role currentRole = membership.getRole();
        if (currentRole != null && isOwnerRoleKey(currentRole.getKey())
                && !currentRole.getId().equals(newRole.getId())) {
            guardNotLastOwner(tenantId, currentRole.getId());
        }

        UUID previousRoleId = currentRole != null ? currentRole.getId() : null;
        membership.setRole(newRole);
        membership.setRoleLegacy(null);
        membership.setUpdatedAt(Instant.now());
        membershipRepository.save(membership);

        auditService.log(TenantContext.getCurrentUserId(), tenantId, "MEMBER_ROLE_CHANGED",
            "TENANT_MEMBERSHIP", membershipId, "SUCCESS", Payloads.of(
            "previousRoleId", String.valueOf(previousRoleId),
            "newRoleKey", newRole.getKey()
        ), null, null, httpRequest);

        return ResponseEntity.ok(Payloads.of(
            "membershipId", membershipId.toString(),
            "userId", membership.getUserId().toString(),
            "roleKey", newRole.getKey(),
            "success", true
        ));
    }

    /**
     * §58 — Révoquer l'adhésion d'un membre de CE tenant (soft : statut REVOKED,
     * aucune suppression de données). Impossible de se révoquer soi-même ni de
     * révoquer le dernier propriétaire. Action auditée.
     */
    @DeleteMapping("/members/{membershipId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> revokeMember(
            @PathVariable UUID membershipId,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        UUID tenantId = TenantContext.requireTenantId();
        TenantMembership membership = tenantScopedMembership(membershipId, tenantId);

        UUID currentUserId = TenantContext.getCurrentUserId();
        if (membership.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("Vous ne pouvez pas vous retirer vous-même de l'organisation.");
        }
        Role currentRole = membership.getRole();
        if (currentRole != null && isOwnerRoleKey(currentRole.getKey())) {
            guardNotLastOwner(tenantId, currentRole.getId());
        }

        membership.setStatus(MembershipStatus.REVOKED);
        membership.setUpdatedAt(Instant.now());
        membershipRepository.save(membership);

        auditService.log(currentUserId, tenantId, "MEMBER_REVOKED",
            "TENANT_MEMBERSHIP", membershipId, "SUCCESS", Payloads.of(
            "userId", membership.getUserId().toString()
        ), null, null, httpRequest);

        return ResponseEntity.ok(Payloads.of("membershipId", membershipId.toString(), "status", "REVOKED"));
    }

    /** Isolation : une membership d'un autre tenant est introuvable (404), jamais 403 fuitable. */
    private TenantMembership tenantScopedMembership(UUID membershipId, UUID tenantId) {
        TenantMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Adhésion introuvable"));
        if (!membership.getTenantId().equals(tenantId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.NOT_FOUND, "Adhésion introuvable");
        }
        return membership;
    }

    private static boolean isOwnerRoleKey(String key) {
        return "TENANT_OWNER".equalsIgnoreCase(key) || "OWNER".equalsIgnoreCase(key);
    }

    private void guardNotLastOwner(UUID tenantId, UUID ownerRoleId) {
        long activeOwners = membershipRepository.findByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE).stream()
            .filter(m -> m.getRole() != null && m.getRole().getId().equals(ownerRoleId))
            .count();
        if (activeOwners <= 1) {
            throw new IllegalArgumentException(
                "Impossible de modifier le rôle du dernier propriétaire de l'organisation.");
        }
    }

    /**
     * Gérer les rôles
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listRoles() {
        UUID tenantId = TenantContext.requireTenantId();
        
        List<Role> roles = roleService.getRoles(tenantId);
        
        return ResponseEntity.ok(roles.stream().map(r -> Payloads.of(
            "id", r.getId().toString(),
            "key", r.getKey(),
            "label", r.getLabel(),
            "description", r.getDescription(),
            "priority", r.getPriority(),
            "isSystem", r.getSystem(),
            "permissions", r.getPermissions() != null ? 
                r.getPermissions().stream().map(p -> p.getKey()).collect(Collectors.toList()) : List.of()
        )).collect(Collectors.toList()));
    }




    /**
     * Modules / Feature Flags
     */
    @GetMapping("/modules")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> getModules() {
        UUID tenantId = TenantContext.requireTenantId();
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Map<String, Object> features = new HashMap<>();
        if (tenant.getFeaturesJson() != null) {
            try {
                features = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(tenant.getFeaturesJson(), Map.class);
            } catch (Exception e) {
                features = getDefaultFeatures();
            }
        } else {
            features = getDefaultFeatures();
        }
        
        return ResponseEntity.ok(features);
    }

    /**
     * Modules - Activer/Désactiver
     */
    @PutMapping("/modules/{moduleKey}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> toggleModule(
            @PathVariable String moduleKey,
            @RequestBody Map<String, Boolean> request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        UUID tenantId = TenantContext.requireTenantId();
        // FIX G5.5 : l'auteur de l'audit est l'utilisateur courant, pas le tenant.
        UUID currentUserId = TenantContext.getCurrentUserId();
        
        boolean enabled = request.get("enabled");
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Map<String, Object> features = new HashMap<>();
        if (tenant.getFeaturesJson() != null) {
            try {
                features = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(tenant.getFeaturesJson(), Map.class);
            } catch (Exception e) {
                features = getDefaultFeatures();
            }
        } else {
            features = getDefaultFeatures();
        }
        
        features.put(moduleKey, enabled);
        
        try {
            tenant.setFeaturesJson(new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(features));
            tenantRepository.save(tenant);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Erreur lors de la mise à jour du module"
            ));
        }
        
        auditService.log(currentUserId, tenantId, "MODULE_TOGGLED",
            "TENANT", tenantId, "SUCCESS", Map.of(
            "module", moduleKey, "enabled", enabled
        ), null, null, httpRequest);
        
        return ResponseEntity.ok(Map.of(
            "module", moduleKey,
            "enabled", enabled,
            "message", enabled ? "Module activé" : "Module désactivé"
        ));
    }

    private Map<String, Object> getDefaultBranding() {
        Map<String, Object> branding = new HashMap<>();
        branding.put("primaryColor", "#6366F1");
        branding.put("secondaryColor", "#8B5CF6");
        branding.put("accentColor", "#EC4899");
        branding.put("logoUrl", "");
        branding.put("faviconUrl", "");
        branding.put("churchName", "");
        branding.put("tagline", "");
        branding.put("address", "");
        branding.put("phone", "");
        branding.put("email", "");
        branding.put("website", "");
        return branding;
    }

    private Map<String, Object> getDefaultSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("language", "fr");
        settings.put("dateFormat", "dd/MM/yyyy");
        settings.put("phoneCountryCode", "+237");
        settings.put("email", "");
        settings.put("phone", "");
        settings.put("website", "");
        settings.put("openingHours", Map.of());
        settings.put("workingDays", List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
        return settings;
    }

    private Map<String, Object> getDefaultFeatures() {
        Map<String, Object> features = new HashMap<>();
        features.put("dashboard", true);
        features.put("members", true);
        features.put("families", true);
        features.put("departments", true);
        features.put("events", true);
        features.put("notifications", true);
        features.put("communications", true);
        features.put("documents", true);
        features.put("reports", true);
        features.put("analytics", true);
        features.put("discipleship", true);
        features.put("academy", true);
        features.put("ai", true);
        features.put("finance", true);
        features.put("giving", true);
        features.put("mobileMoney", true);
        features.put("marketplace", true);
        features.put("whatsapp", true);
        features.put("community", true);
        features.put("forms", true);
        features.put("calendar", true);
        return features;
    }
}
