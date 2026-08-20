package com.discipolat.modules.users.api;

import com.discipolat.modules.users.api.dto.*;
import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.evaluations.domain.EvaluationService;
import com.discipolat.modules.transfers.api.TransferResponse;
import com.discipolat.modules.transfers.domain.TransferBridgeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final SecurityUtils securityUtils;
    private final EvaluationService evaluationService;
    private final com.discipolat.modules.souls.domain.WorkspaceScopeService workspaceScopeService;
    private final TransferBridgeService transferBridgeService;
    private final com.discipolat.modules.audit.domain.AuditService auditService;

    public UserController(UserService userService, AuthService authService, SecurityUtils securityUtils,
                          EvaluationService evaluationService,
                          com.discipolat.modules.souls.domain.WorkspaceScopeService workspaceScopeService,
                          TransferBridgeService transferBridgeService,
                          com.discipolat.modules.audit.domain.AuditService auditService) {
        this.userService = userService;
        this.authService = authService;
        this.securityUtils = securityUtils;
        this.evaluationService = evaluationService;
        this.workspaceScopeService = workspaceScopeService;
        this.transferBridgeService = transferBridgeService;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String statut,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());

        Page<User> users;
        if (role != null) {
            // Use multi-role query: find users whose roles CONTAIN the given role
            users = userService.findByRolesContaining(UserRole.valueOf(role), pageable);
        } else {
            users = userService.findAll(pageable);
        }

        // FAISEUR: only see themselves and other FAISEURs (check active role)
        String currentRole = securityUtils.getCurrentUserRole();
        boolean isFaiseurActive = "FAISEUR".equals(currentRole);
        boolean isOnlyFaiseur = userService.isOnlyRole(currentRole, "FAISEUR");
        if (isFaiseurActive && (role == null || !role.equals("FAISEUR"))) {
            users = userService.findByRolesContaining(UserRole.FAISEUR, pageable);
        }

        // Non super-utilisateur : restreindre aux faiseurs de l'espace métier + soi-même
        if (!securityUtils.isSuperUser()) {
            java.util.Set<UUID> accessible = new java.util.HashSet<>(workspaceScopeService.accessibleFaiseurIds());
            accessible.add(securityUtils.getCurrentUserId());
            java.util.List<User> scoped = users.getContent().stream()
                    .filter(u -> accessible.contains(u.getId()))
                    .toList();
            users = new org.springframework.data.domain.PageImpl<>(scoped, pageable, scoped.size());
        }

        Page<UserResponse> response = users.map(UserResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(),
                response.getNumber(),
                response.getSize(),
                response.getTotalElements(),
                response.getTotalPages()
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> me() {
        User user = userService.findById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE') or #id == authentication.principal")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Self-update: any authenticated user can update their own profile */
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.updateMyProfile(
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.dateNaissance(),
                request.situationFamiliale()
        );
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               jakarta.servlet.http.HttpServletRequest httpRequest) {
        User.UserBuilder builder = User.builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .passwordHash(request.password())
                .role(request.role());

        // Set multi-role fields if provided
        if (request.roles() != null && !request.roles().isEmpty()) {
            builder.roles(request.roles());
        }
        if (request.activeRole() != null) {
            builder.activeRole(request.activeRole());
        }

        User user = builder.build();
        user = userService.create(user, request.password());
        // US-02: Send welcome email with activation link
        authService.sendActivationEmail(user.getId());
        // Audit log
        auditService.log("CREATE", "USER", user.getId(), null,
                Map.of("email", user.getEmail(), "firstName", user.getFirstName(),
                       "lastName", user.getLastName(), "role", String.valueOf(user.getRole())),
                httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request,
                                               jakarta.servlet.http.HttpServletRequest httpRequest) {
        User user = userService.findById(id);
        Map<String, Object> oldValues = Map.of(
                "email", user.getEmail(), "firstName", user.getFirstName(),
                "lastName", user.getLastName(), "role", String.valueOf(user.getRole()));
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());
        // Set multi-role fields if provided
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(request.roles());
        }
        if (request.activeRole() != null) {
            user.setActiveRole(request.activeRole());
        }
        user = userService.update(user);
        // Audit log
        Map<String, Object> newValues = Map.of(
                "email", user.getEmail(), "firstName", user.getFirstName(),
                "lastName", user.getLastName(), "role", String.valueOf(user.getRole()));
        auditService.log("UPDATE", "USER", user.getId(), oldValues, newValues, httpRequest);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        userService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/promote-chef")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> promoteToChefDeFamille(
            @PathVariable UUID id,
            @Valid @RequestBody ChefPromotionRequest request) {
        User user = userService.promoteToChefDeFamille(id, request.familleId());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/{id}/demote-chef")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> demoteFromChefDeFamille(@PathVariable UUID id) {
        User user = userService.demoteFromChefDeFamille(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<UserResponse>> findByRole(@PathVariable UserRole role) {
        List<User> users = userService.findByRole(role);
        return ResponseEntity.ok(users.stream().map(UserResponse::from).toList());
    }

    @GetMapping("/by-famille/{familleId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<UserResponse>> findByFamille(@PathVariable UUID familleId) {
        List<User> users = userService.findByFamilleGereeId(familleId);
        return ResponseEntity.ok(users.stream().map(UserResponse::from).toList());
    }

    // ======================== US-12: PROMOTE TO FAISEUR ========================

    @PatchMapping("/{id}/promote-faiseur")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> promoteToFaiseur(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(userService.promoteToFaiseur(id)));
    }

    // ======================== US-14: FAISEUR WORKLOAD ========================

    @GetMapping("/faiseur-workload")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<Map<String, Object>>> getFaiseurWorkload(
            @RequestParam(required = false) UUID familleId) {
        return ResponseEntity.ok(userService.getFaiseurWorkload(familleId));
    }

    // ======================== FICHE UTILISATEUR COMPLÈTE ========================

    /**
     * Fiche complète d'un utilisateur pour l'encadrement : identité, âme liée,
     * âmes suivies (faiseur), départements + membres (responsable), famille
     * gérée (chef), évaluations reçues et MES évaluations (donner/modifier).
     */
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> getUserDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserDetail(id));
    }

    // ======================== US-16: FAISEUR HISTORY ========================

    @GetMapping("/{id}/faiseur-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> getFaiseurHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getFaiseurHistory(id));
    }

    // ======================== US-13: TRANSFER FAISEUR ========================

    /**
     * US-13 : transfert d'un faiseur vers une autre famille.
     * Passe désormais par le MOTEUR DE WORKFLOW : la demande est soumise au
     * circuit de validation configuré par le pasteur (exécution immédiate si
     * le circuit est vide). Retourne la demande de transfert.
     */
    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<TransferResponse> transferFaiseur(
            @PathVariable UUID id,
            @Valid @RequestBody TransferFaiseurRequest body) {
        return ResponseEntity.ok(transferBridgeService.transferFaiseur(id, body.nouvelleFamilleId(), body.transfererAmes()));
    }

    // ======================== US-17: DEMOTE FAISEUR ========================

    @PatchMapping("/{id}/demote")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> demote(@PathVariable UUID id, @Valid @RequestBody DemoteRequest body) {
        UserRole role = body.newRole() != null ? UserRole.valueOf(body.newRole()) : null;
        return ResponseEntity.ok(UserResponse.from(userService.demoteFaiseur(id, role)));
    }

    // ======================== US-57: RGPD HARD DELETE ========================

    @DeleteMapping("/{id}/hard-delete")
    @PreAuthorize("hasRole('ADMIN') && @perm.has('USER','DELETE')")
    public ResponseEntity<Map<String, String>> hardDelete(@PathVariable UUID id,
                                                          jakarta.servlet.http.HttpServletRequest httpRequest) {
        auditService.log("DELETE", "USER", id, null, null, httpRequest);
        userService.hardDeleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User permanently deleted"));
    }

    // ======================== US-60: RESTORE USER ========================

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(userService.restoreUser(id)));
    }

    // ======================== MULTI-ROLE MANAGEMENT (Admin) ========================

    /** Add a role to a user */
    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> addRole(
            @PathVariable UUID id,
            @Valid @RequestBody AddRoleRequest body,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        UserRole role = UserRole.valueOf(body.role());
        User user = userService.addRole(id, role);
        auditService.log("ADD_ROLE", "USER", id, null, Map.of("role", body.role()), httpRequest);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Remove a role from a user */
    @DeleteMapping("/{id}/roles/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> removeRole(
            @PathVariable UUID id,
            @PathVariable UserRole role) {
        User user = userService.removeRole(id, role);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Replace all roles for a user */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> replaceRoles(
            @PathVariable UUID id,
            @Valid @RequestBody ReplaceRolesRequest body) {
        Set<UserRole> roles = body.roles().stream().map(UserRole::valueOf).collect(Collectors.toSet());
        User user = userService.replaceRoles(id, roles);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Set the active role for a user */
    @PatchMapping("/{id}/active-role")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> setActiveRole(
            @PathVariable UUID id,
            @Valid @RequestBody SetActiveRoleRequest body) {
        UserRole activeRole = UserRole.valueOf(body.activeRole());
        User user = userService.setActiveRole(id, activeRole);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    // ======================== EVALUATION SCORES ========================

    @GetMapping("/evaluation-scores")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Map<UUID, Map<String, Object>>> getEvaluationScores(
            @RequestParam List<UUID> userIds) {
        Map<UUID, Map<String, Object>> result = new LinkedHashMap<>();
        for (UUID userId : userIds) {
            Map<String, Object> scores = evaluationService.getUserEvalScores(userId);
            if (!scores.isEmpty()) {
                result.put(userId, scores);
            }
        }
        return ResponseEntity.ok(result);
    }
}
