package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API de gestion des tenants (églises) — réservée aux super-utilisateurs.
 *
 * <p>Les tenants sont l'ossature multi-tenant V70 : chaque église possède son
 * propre tenant_id porté par le JWT de ses utilisateurs et isolé par le filtre
 * Hibernate ({@code @Filter}) + la base de repository tenant-aware
 * ({@code TenantAwareSimpleJpaRepository}). Cette API permet de provisionner
 * de nouvelles églises depuis le back-office.
 */
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TenantResponse>> list() {
        return ResponseEntity.ok(tenantService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody CreateTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TenantResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdateTenantRequest request) {
        return ResponseEntity.ok(tenantService.update(id, request));
    }
}
