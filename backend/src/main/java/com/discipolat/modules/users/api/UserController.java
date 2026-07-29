package com.discipolat.modules.users.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.authentication.domain.AuthService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
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
            users = userService.findByRole(UserRole.valueOf(role), pageable);
        } else {
            users = userService.findAll(pageable);
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
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE') or #id == authentication.principal")
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
        User user = User.builder()
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .passwordHash(request.password())
                .role(request.role())
                .build();
        user = userService.create(user, request.password());
        // US-02: Send welcome email with activation link
        authService.sendActivationEmail(user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        User user = userService.findById(id);
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(request.role());
        user = userService.update(user);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        userService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/promote-chef")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> promoteToChefDeFamille(
            @PathVariable UUID id,
            @RequestBody ChefPromotionRequest request) {
        User user = userService.promoteToChefDeFamille(id, request.familleId());
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @PatchMapping("/{id}/demote-chef")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> demoteFromChefDeFamille(@PathVariable UUID id) {
        User user = userService.demoteFromChefDeFamille(id);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @GetMapping("/by-role/{role}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<UserResponse>> findByRole(@PathVariable UserRole role) {
        List<User> users = userService.findByRole(role);
        return ResponseEntity.ok(users.stream().map(UserResponse::from).toList());
    }

    @GetMapping("/by-famille/{familleId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<UserResponse>> findByFamille(@PathVariable UUID familleId) {
        List<User> users = userService.findByFamilleGereeId(familleId);
        return ResponseEntity.ok(users.stream().map(UserResponse::from).toList());
    }

    // ======================== US-12: PROMOTE TO FAISEUR ========================

    @PatchMapping("/{id}/promote-faiseur")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<UserResponse> promoteToFaiseur(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(userService.promoteToFaiseur(id)));
    }

    // ======================== US-14: FAISEUR WORKLOAD ========================

    @GetMapping("/faiseur-workload")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<Map<String, Object>>> getFaiseurWorkload(
            @RequestParam(required = false) UUID familleId) {
        return ResponseEntity.ok(userService.getFaiseurWorkload(familleId));
    }

    // ======================== US-16: FAISEUR HISTORY ========================

    @GetMapping("/{id}/faiseur-history")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> getFaiseurHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getFaiseurHistory(id));
    }

    // ======================== US-13: TRANSFER FAISEUR ========================

    @PatchMapping("/{id}/transfer")
    @PreAuthorize("hasRole('PASTEUR')")
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
    @PreAuthorize("hasRole('PASTEUR')")
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
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<UserResponse> restore(@PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(userService.restoreUser(id)));
    }
}
