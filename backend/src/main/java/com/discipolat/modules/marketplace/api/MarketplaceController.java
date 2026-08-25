package com.discipolat.modules.marketplace.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.marketplace.domain.MarketplaceListing;
import com.discipolat.modules.marketplace.domain.MarketplaceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/marketplace")
@PreAuthorize("isAuthenticated()")
public class MarketplaceController {

    private final MarketplaceService service;

    public MarketplaceController(MarketplaceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MarketplaceListing>> list() {
        return ResponseEntity.ok(service.list(TenantContext.getTenantId().getLeastSignificantBits()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<MarketplaceListing>> listByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(service.listByCategory(TenantContext.getTenantId().getLeastSignificantBits(), category));
    }

    @PostMapping
    public ResponseEntity<MarketplaceListing> create(@RequestBody MarketplaceListing listing) {
        return ResponseEntity.ok(service.create(listing));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MarketplaceListing> update(
            @PathVariable Long id,
            @RequestBody MarketplaceListing listing) {
        return ResponseEntity.ok(service.update(id, listing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== P3 #105 — MARKETPLACE DE TEMPLATES ========================

    /** Publie un template de département/famille/rapport partageable entre églises. */
    @PostMapping("/templates/publish")
    public ResponseEntity<MarketplaceListing> publishTemplate(@RequestBody MarketplaceListing listing) {
        listing.setId(null);
        listing.setCategory(listing.getCategory() == null ? "TEMPLATE" : listing.getCategory());
        listing.setIsActive(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(listing));
    }

    /** Installe un template : renvoie sa définition complète pour import dans l'église. */
    @PostMapping("/{id}/install")
    public ResponseEntity<Map<String, Object>> install(@PathVariable Long id) {
        return ResponseEntity.ok(service.install(id, TenantContext.getTenantId().getLeastSignificantBits()));
    }
}

