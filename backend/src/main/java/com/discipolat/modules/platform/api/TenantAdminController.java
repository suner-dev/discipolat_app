package com.discipolat.modules.platform.api;

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
        membershipRepository.findByTenantId(tenantId).forEach(m ->
            membersByRole.merge(m.getRole(), 1L, Long::sum)
        );
        
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
        dashboard.put("subscription", subscription.map(s -> Map.of(
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
        
        List<TenantMembership> memberships;
        if (role != null) {
            memberships = membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                tenantId, MembershipStatus.ACTIVE, role, pageRequest);
        } else {
            memberships = membershipRepository.findByTenantIdAndStatus(
                tenantId, MembershipStatus.ACTIVE, pageRequest);
        }
        
        List<Map<String, Object>> content = memberships.stream().map(m -> {
            Optional<User> user = userRepository.findById(m.getUserId());
            return Map.<String, Object>of(
                "membershipId", m.getId().toString(),
                "userId", m.getUserId().toString(),
                "email", user.map(User::getEmail).orElse("unknown"),
                "firstName", user.map(User::getFirstName).orElse(""),
                "lastName", user.map(User::getLastName).orElse(""),
                "fullName", (user.map(User::getFirstName).orElse("") + " " + user.map(User::getLastName).orElse("")).trim(),
                "role", m.getRole(),
                "status", m.getStatus().name(),
                "joinedAt", m.getJoinedAt().toString(),
                "photoUrl", user.map(User::getPhotoUrl).orElse(null),
                "phone", user.map(User::getPhone).orElse(null),
                "isActive", m.getStatus() == MembershipStatus.ACTIVE
            );
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
     * Créer une invitation
     */
    @PostMapping("/invitations")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> createInvitation(
            @RequestBody Map<String, Object> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
        String email = (String) request.get("email");
        String role = (String) request.get("role");
        UUID organizationNodeId = request.get("organizationNodeId") != null ?
            UUID.fromString((String) request.get("organizationNodeId")) : null;
        
        if (email == null || role == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "email et role sont requis"
            ));
        }
        
        // Vérifier si l'utilisateur existe déjà
        Optional<User> existingUser = userRepository.findByEmail(email.toLowerCase());
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            
            // Vérifier si l'utilisateur a déjà une membership active
            Optional<TenantMembership> existingMembership = membershipRepository
                .findByUserIdAndTenantIdAndStatus(user.getId(), tenantId, MembershipStatus.ACTIVE);
            
            if (existingMembership.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cet utilisateur appartient déjà à ce tenant",
                    "userId", user.getId().toString()
                ));
            }
            
            // Créer la membership directement
            TenantMembership membership = membershipService.addMembership(
                user.getId(), tenantId, role, currentUserId);
            
            auditService.log(currentUserId, tenantId, "USER_INVITED_EXISTING",
                "USER", user.getId(), "SUCCESS", Map.of(
                "email", email, "role", role
            ));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "invitedUserId", user.getId().toString(),
                "email", email,
                "role", role,
                "message", "Utilisateur ajouté directement"
            ));
        }
        
        // Créer l'invitation pour un nouvel utilisateur
        String token = UUID.randomUUID().toString();
        
        Invitation invitation = Invitation.builder()
            .tenantId(tenantId)
            .email(email.toLowerCase())
            .role(role)
            .invitedBy(currentUserId)
            .invitationToken(token)
            .status(InvitationStatus.PENDING)
            .expiresAt(Instant.now().plusSeconds(7 * 24 * 3600))
            .organizationNodeId(organizationNodeId)
            .build();
        
        invitation = orgNodeService.saveInvitation(invitation);
        
        auditService.log(currentUserId, tenantId, "INVITATION_CREATED",
            "INVITATION", invitation.getId(), "SUCCESS", Map.of(
            "email", email, "role", role
        ));
        
        // Ici, il faudrait envoyer un email réel (via un service d'email)
        // Pour l'instant, on retourne juste les informations
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "success", true,
            "invitationId", invitation.getId().toString(),
            "email", email,
            "role", role,
            "invitationToken", token,
            "expiresAt", invitation.getExpiresAt().toString(),
            "message", "Invitation créée. L'invité recevra un email avec un lien d'invitation."
        ));
    }

    /**
     * Liste des invitations
     */
    @GetMapping("/invitations")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listInvitations() {
        UUID tenantId = TenantContext.requireTenantId();
        
        List<Invitation> invitations = orgNodeService.findInvitationsByTenantId(tenantId);
        
        return ResponseEntity.ok(invitations.stream().map(i -> Map.of(
            "id", i.getId().toString(),
            "email", i.getEmail(),
            "role", i.getRole(),
            "status", i.getStatus().name(),
            "invitedBy", i.getInvitedBy() != null ? i.getInvitedBy().toString() : null,
            "createdAt", i.getCreatedAt().toString(),
            "expiresAt", i.getExpiresAt().toString(),
            "acceptedAt", i.getAcceptedAt() != null ? i.getAcceptedAt().toString() : null
        )).collect(Collectors.toList()));
    }

    /**
     * Annuler une invitation
     */
    @DeleteMapping("/invitations/{id}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Void> cancelInvitation(@PathVariable UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
        Optional<Invitation> invitation = orgNodeService.findInvitationById(id);
        
        if (invitation.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        if (!invitation.get().getTenantId().equals(tenantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        invitation.get().setStatus(InvitationStatus.CANCELED);
        orgNodeService.saveInvitation(invitation.get());
        
        auditService.log(currentUserId, tenantId, "INVITATION_CANCELLED",
            "INVITATION", id, "SUCCESS", Map.of());
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Gérer les rôles
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> listRoles() {
        UUID tenantId = TenantContext.requireTenantId();
        
        List<Role> roles = roleService.getRoles(tenantId);
        
        return ResponseEntity.ok(roles.stream().map(r -> Map.of(
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
     * Créer un rôle personnalisé
     */
    @PostMapping("/roles")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> createRole(
            @RequestBody Map<String, Object> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
        String key = ((String) request.get("key")).toUpperCase();
        String label = (String) request.get("label");
        String description = request.get("description") != null ? 
            (String) request.get("description") : "";
        
        Role role = roleService.createRole(tenantId, key, label, description, currentUserId);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", role.getId().toString(),
            "key", role.getKey(),
            "label", role.getLabel(),
            "description", role.getDescription(),
            "createdAt", role.getCreatedAt().toString()
        ));
    }

    /**
     * Mettre à jour un rôle
     */
    @PutMapping("/roles/{roleId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable UUID roleId,
            @RequestBody Map<String, Object> request) {
        
        UUID currentUserId = TenantContext.getTenantId();
        
        String label = request.get("label") != null ? (String) request.get("label") : null;
        String description = request.get("description") != null ? 
            (String) request.get("description") : null;
        Integer priority = request.get("priority") != null ? 
            (Integer) request.get("priority") : null;
        
        Role role = roleService.updateRole(roleId, label, description, priority, currentUserId);
        
        return ResponseEntity.ok(Map.of(
            "id", role.getId().toString(),
            "key", role.getKey(),
            "label", role.getLabel(),
            "description", role.getDescription(),
            "priority", role.getPriority()
        ));
    }

    /**
     * Supprimer un rôle personnalisé
     */
    @DeleteMapping("/roles/{roleId}")
    @PreAuthorize("hasAnyRole('TENANT_OWNER')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId) {
        UUID currentUserId = TenantContext.getTenantId();
        UUID tenantId = TenantContext.requireTenantId();
        
        roleService.deleteRole(roleId, currentUserId);
        
        auditService.log(currentUserId, tenantId, "ROLE_DELETED",
            "ROLE", roleId, "SUCCESS", Map.of());
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Branding - Obtenir la configuration
     */
    @GetMapping("/branding")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> getBranding() {
        UUID tenantId = TenantContext.requireTenantId();
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Map<String, Object> branding = new HashMap<>();
        if (tenant.getBrandingJson() != null) {
            try {
                branding = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(tenant.getBrandingJson(), Map.class);
            } catch (Exception e) {
                branding = getDefaultBranding();
            }
        } else {
            branding = getDefaultBranding();
        }
        
        branding.put("tenantName", tenant.getName());
        branding.put("tenantId", tenantId.toString());
        
        return ResponseEntity.ok(branding);
    }

    /**
     * Branding - Mettre à jour
     */
    @PutMapping("/branding")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateBranding(
            @RequestBody Map<String, String> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Map<String, Object> branding = new HashMap<>();
        if (tenant.getBrandingJson() != null) {
            try {
                branding = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(tenant.getBrandingJson(), Map.class);
            } catch (Exception e) {
                branding = getDefaultBranding();
            }
        } else {
            branding = getDefaultBranding();
        }
        
        // Mettre à jour les champs
        if (request.containsKey("primaryColor")) branding.put("primaryColor", request.get("primaryColor"));
        if (request.containsKey("secondaryColor")) branding.put("secondaryColor", request.get("secondaryColor"));
        if (request.containsKey("accentColor")) branding.put("accentColor", request.get("accentColor"));
        if (request.containsKey("logoUrl")) branding.put("logoUrl", request.get("logoUrl"));
        if (request.containsKey("faviconUrl")) branding.put("faviconUrl", request.get("faviconUrl"));
        if (request.containsKey("churchName")) branding.put("churchName", request.get("churchName"));
        if (request.containsKey("tagline")) branding.put("tagline", request.get("tagline"));
        if (request.containsKey("address")) branding.put("address", request.get("address"));
        if (request.containsKey("phone")) branding.put("phone", request.get("phone"));
        if (request.containsKey("email")) branding.put("email", request.get("email"));
        if (request.containsKey("website")) branding.put("website", request.get("website"));
        
        try {
            tenant.setBrandingJson(new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(branding));
            tenantRepository.save(tenant);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Erreur lors de la mise à jour du branding"
            ));
        }
        
        auditService.log(currentUserId, tenantId, "BRANDING_UPDATED",
            "TENANT", tenantId, "SUCCESS", Map.of());
        
        return ResponseEntity.ok(branding);
    }

    /**
     * Settings - Obtenir les paramètres
     */
    @GetMapping("/settings")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> getSettings() {
        UUID tenantId = TenantContext.requireTenantId();
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Map<String, Object> settings = new HashMap<>();
        if (tenant.getSettingsJson() != null) {
            try {
                settings = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(tenant.getSettingsJson(), Map.class);
            } catch (Exception e) {
                settings = getDefaultSettings();
            }
        } else {
            settings = getDefaultSettings();
        }
        
        settings.put("country", tenant.getCountry());
        settings.put("currency", tenant.getCurrency());
        settings.put("timezone", tenant.getTimezone());
        settings.put("locale", tenant.getLocale());
        
        return ResponseEntity.ok(settings);
    }

    /**
     * Settings - Mettre à jour
     */
    @PutMapping("/settings")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateSettings(
            @RequestBody Map<String, Object> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));
        
        Map<String, Object> settings = new HashMap<>();
        if (tenant.getSettingsJson() != null) {
            try {
                settings = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(tenant.getSettingsJson(), Map.class);
            } catch (Exception e) {
                settings = getDefaultSettings();
            }
        } else {
            settings = getDefaultSettings();
        }
        
        // Mettre à jour les champs
        if (request.containsKey("language")) settings.put("language", request.get("language"));
        if (request.containsKey("dateFormat")) settings.put("dateFormat", request.get("dateFormat"));
        if (request.containsKey("phoneCountryCode")) settings.put("phoneCountryCode", request.get("phoneCountryCode"));
        if (request.containsKey("email")) settings.put("email", request.get("email"));
        if (request.containsKey("phone")) settings.put("phone", request.get("phone"));
        if (request.containsKey("website")) settings.put("website", request.get("website"));
        if (request.containsKey("openingHours")) settings.put("openingHours", request.get("openingHours"));
        if (request.containsKey("workingDays")) settings.put("workingDays", request.get("workingDays"));
        
        // Mettre à jour les champs du tenant
        if (request.containsKey("country")) tenant.setCountry((String) request.get("country"));
        if (request.containsKey("currency")) tenant.setCurrency((String) request.get("currency"));
        if (request.containsKey("timezone")) tenant.setTimezone((String) request.get("timezone"));
        if (request.containsKey("locale")) tenant.setLocale((String) request.get("locale"));
        
        try {
            tenant.setSettingsJson(new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(settings));
            tenantRepository.save(tenant);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Erreur lors de la mise à jour des paramètres"
            ));
        }
        
        auditService.log(currentUserId, tenantId, "SETTINGS_UPDATED",
            "TENANT", tenantId, "SUCCESS", Map.of());
        
        settings.put("country", tenant.getCountry());
        settings.put("currency", tenant.getCurrency());
        settings.put("timezone", tenant.getTimezone());
        settings.put("locale", tenant.getLocale());
        
        return ResponseEntity.ok(settings);
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
            @RequestBody Map<String, Boolean> request) {
        
        UUID tenantId = TenantContext.requireTenantId();
        UUID currentUserId = TenantContext.getTenantId();
        
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
        ));
        
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
