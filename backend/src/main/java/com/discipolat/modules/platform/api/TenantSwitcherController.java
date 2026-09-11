package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Tenant Switcher API (Sections 33-35 du prompt maître)
 * Permet aux utilisateurs multi-tenants de changer de contexte
 */
@RestController
@RequestMapping("/api/v1/tenant-switcher")
public class TenantSwitcherController {

    private final TenantMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final OrganizationNodeService orgNodeService;

    public TenantSwitcherController(
            TenantMembershipRepository membershipRepository,
            UserRepository userRepository,
            TenantRepository tenantRepository,
            OrganizationNodeService orgNodeService) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.orgNodeService = orgNodeService;
    }

    // ==================== OBTENIR LES TENANTS DE L'UTILISATEUR ====================

    @GetMapping("/my-tenants")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MyTenantInfo>> getMyTenants() {
        UUID userId = TenantContext.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(List.of());
        }

        List<TenantMembership> memberships = membershipRepository
                .findByUserIdAndStatus(userId, MembershipStatus.ACTIVE);

        List<MyTenantInfo> result = new ArrayList<>();
        for (TenantMembership m : memberships) {
            Optional<Tenant> tenant = tenantRepository.findById(m.getTenantId());
            Optional<OrganizationNode> root = orgNodeService.findRootByTenantId(m.getTenantId());
            
            tenant.ifPresent(t -> result.add(new MyTenantInfo(
                    t.getId(),
                    t.getName(),
                    t.getSlug(),
                    root.map(r -> r.getName()).orElse(t.getName()),
                    m.getRole(),
                    m.getJoinedAt(),
                    t.getStatus()
            )));
        }

        return ResponseEntity.ok(result);
    }

    // ==================== CHANGER DE TENANT ====================

    @PostMapping("/switch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SwitchTenantResponse> switchTenant(@RequestBody SwitchTenantRequest request) {
        UUID userId = TenantContext.getCurrentUserId();
        if (userId == null) {
            throw new SecurityException("Non authentifié");
        }

        UUID newTenantId = UUID.fromString(request.tenantId());
        
        // Vérifier que l'utilisateur a accès à ce tenant
        boolean hasAccess = membershipRepository.existsByUserIdAndTenantIdAndStatus(
                userId, newTenantId, MembershipStatus.ACTIVE);
        
        if (!hasAccess) {
            throw new SecurityException("Accès refusé à ce tenant");
        }

        // Mettre à jour le contexte
        TenantContext.setTenantId(newTenantId);

        Optional<Tenant> tenant = tenantRepository.findById(newTenantId);
        Optional<OrganizationNode> root = orgNodeService.findRootByTenantId(newTenantId);
        Optional<TenantMembership> membership = membershipRepository
                .findByUserIdAndTenantIdAndStatus(userId, newTenantId, MembershipStatus.ACTIVE);

        return ResponseEntity.ok(new SwitchTenantResponse(
                tenant.flatMap(t -> Optional.of(t.getName())).orElse("Inconnu"),
                tenant.flatMap(t -> Optional.of(t.getSlug())).orElse(""),
                root.map(r -> r.getName()).orElse(null),
                membership.map(TenantMembership::getRole).orElse(null),
                newTenantId,
                membership.flatMap(m -> Optional.of(m.getJoinedAt())).orElse(null)
        ));
    }

    // ==================== OBTENIR LES ORGANISATIONS ACCESSIBLES ====================

    @GetMapping("/my-organizations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MyOrganizationInfo>> getMyOrganizations() {
        UUID userId = TenantContext.getCurrentUserId();
        UUID currentTenantId = TenantContext.getTenantId();
        
        if (userId == null || currentTenantId == null) {
            return ResponseEntity.ok(List.of());
        }

        // Obtenir les nodes où l'utilisateur est responsable
        List<OrganizationNode> responsibleNodes = orgNodeService.findByResponsibleId(userId);
        
        // Filtrer par tenant courant
        List<OrganizationNode> myNodes = responsibleNodes.stream()
                .filter(n -> n.getTenantId().equals(currentTenantId))
                .toList();

        List<MyOrganizationInfo> result = new ArrayList<>();
        for (OrganizationNode node : myNodes) {
            result.add(new MyOrganizationInfo(
                    node.getId(),
                    node.getName(),
                    node.getType(),
                    node.getParentId(),
                    node.getPath(),
                    node.getStatus()
            ));
        }

        return ResponseEntity.ok(result);
    }

    // ==================== CHANGER D'ORGANISATION ====================

    @PostMapping("/switch-org")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SwitchOrgResponse> switchOrganization(@RequestBody SwitchOrgRequest request) {
        UUID userId = TenantContext.getCurrentUserId();
        UUID currentTenantId = TenantContext.getTenantId();
        
        if (userId == null || currentTenantId == null) {
            throw new SecurityException("Non authentifié");
        }

        UUID nodeId = UUID.fromString(request.organizationId());
        
        Optional<OrganizationNode> node = orgNodeService.findById(nodeId);
        if (node.isEmpty()) {
            throw new RuntimeException("Organisation non trouvée");
        }
        
        if (!node.get().getTenantId().equals(currentTenantId)) {
            throw new SecurityException("Cette organisation ne appartient pas au tenant courant");
        }

        // Vérifier que l'utilisateur a accès à ce node
        boolean hasAccess = orgNodeService.hasUserAccessToNode(userId, nodeId);
        if (!hasAccess) {
            throw new SecurityException("Accès refusé à cette organisation");
        }

        return ResponseEntity.ok(new SwitchOrgResponse(
                node.get().getName(),
                node.get().getType(),
                node.get().getId(),
                node.get().getPath()
        ));
    }

    // ==================== MISES À JOUR CONTEXTE ====================

    @GetMapping("/context")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentContextResponse> getCurrentContext() {
        UUID currentTenantId = TenantContext.getTenantId();
        UUID currentUserId = TenantContext.getCurrentUserId();

        if (currentTenantId == null || currentUserId == null) {
            return ResponseEntity.ok(new CurrentContextResponse(null, null, null, null, null));
        }

        Optional<Tenant> tenant = tenantRepository.findById(currentTenantId);
        Optional<TenantMembership> membership = membershipRepository
                .findByUserIdAndTenantIdAndStatus(currentUserId, currentTenantId, MembershipStatus.ACTIVE);
        Optional<OrganizationNode> root = orgNodeService.findRootByTenantId(currentTenantId);

        return ResponseEntity.ok(new CurrentContextResponse(
                currentTenantId,
                tenant.flatMap(t -> Optional.of(t.getName())).orElse(null),
                membership.map(TenantMembership::getRole).orElse(null),
                root.map(r -> r.getName()).orElse(null),
                membership.flatMap(m -> Optional.of(m.getJoinedAt())).orElse(null)
        ));
    }

    // ==================== RECORDS ====================

    public record MyTenantInfo(
            UUID tenantId, String name, String slug,
            String organizationName, String role,
            Instant joinedAt, TenantStatus status
    ) {}

    public record MyOrganizationInfo(
            UUID id, String name, OrganizationNodeType type,
            UUID parentId, String path, OrganizationNodeStatus status
    ) {}

    public record SwitchTenantRequest(String tenantId) {}
    public record SwitchOrgRequest(String organizationId) {}

    public record SwitchTenantResponse(
            String tenantName, String slug, String organizationName,
            String role, UUID tenantId, Instant joinedAt
    ) {}

    public record SwitchOrgResponse(
            String name, OrganizationNodeType type,
            UUID organizationId, String path
    ) {}

    public record CurrentContextResponse(
            UUID tenantId, String tenantName, String role,
            String organizationName, Instant joinedAt
    ) {}
}
