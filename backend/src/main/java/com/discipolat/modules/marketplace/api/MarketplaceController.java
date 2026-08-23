package com.discipolat.modules.marketplace.api;

import com.discipolat.modules.marketplace.domain.MarketplaceListing;
import com.discipolat.modules.marketplace.domain.MarketplaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/marketplace")
public class MarketplaceController {

    private final MarketplaceService service;

    public MarketplaceController(MarketplaceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MarketplaceListing>> list(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.list(tenantId));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<MarketplaceListing>> listByCategory(
            @RequestParam Long tenantId,
            @PathVariable String category) {
        return ResponseEntity.ok(service.listByCategory(tenantId, category));
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
}
