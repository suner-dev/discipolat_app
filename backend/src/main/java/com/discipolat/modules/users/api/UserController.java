package com.discipolat.modules.users.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.evaluations.domain.EvaluationService;
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

    public UserController(UserService userService, AuthService authService, SecurityUtils securityUtils,
                          EvaluationService evaluationService,
                          com.discipolat.modules.souls.domain.WorkspaceScopeService workspaceScopeService) {
        this.userService = userService;
        this.authService = authService;
        this.securityUtils = securityUtils;
        this.evaluationService = evaluationService;
        this.workspaceScopeService = workspaceScopeService;
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE') or #id == authentication.principal")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Self-update: any authenticated user can update their own profile */
    @PutMapping("/me")
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
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.findById(id);
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
            @RequestBody ChefPromotionRequest request) {
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
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<Map<String, Object>>> getFaiseurWorkload(
            @RequestParam(required = false) UUID familleId) {
        return ResponseEntity.ok(userService.getFaiseurWorkload(familleId));
    }

    // ======================== US-16: FAISEUR HISTORY ========================

    @GetMapping("/{id}/faiseur-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> getFaiseurHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getFaiseurHistory(id));
    }

    // ======================== US-13: TRANSFER FAISEUR ========================

    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> transferFaiseur(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        UUID nouvelleFamilleId = UUID.fromString((String) body.get("nouvelleFamilleId"));
        boolean transfererAmes = body.containsKey("transfererAmes") && (Boolean) body.get("transfererAmes");
        return ResponseEntity.ok(UserResponse.from(
                userService.transferFaiseur(id, nouvelleFamilleId, transfererAmes)));
    }

    // ======================== US-17: DEMOTE FAISEUR ========================

    @PatchMapping("/{id}/demote")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> demote(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String newRole = body.get("newRole");
        UserRole role = newRole != null ? UserRole.valueOf(newRole) : null;
        return ResponseEntity.ok(UserResponse.from(userService.demoteFaiseur(id, role)));
    }

    // ======================== US-57: RGPD HARD DELETE ========================

    @DeleteMapping("/{id}/hard-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> hardDelete(@PathVariable UUID id) {
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
            @RequestBody Map<String, String> body) {
        UserRole role = UserRole.valueOf(body.get("role"));
        User user = userService.addRole(id, role);
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
            @RequestBody Map<String, Set<String>> body) {
        Set<String> roleStrings = body.get("roles");
        Set<UserRole> roles = roleStrings.stream().map(UserRole::valueOf).collect(java.util.stream.Collectors.toSet());
        User user = userService.replaceRoles(id, roles);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Set the active role for a user */
    @PatchMapping("/{id}/active-role")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<UserResponse> setActiveRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        UserRole activeRole = UserRole.valueOf(body.get("activeRole"));
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
