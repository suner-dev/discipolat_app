package com.discipolat.modules.marketplace.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MarketplaceService {

    private final MarketplaceListingRepository repository;

    public MarketplaceService(MarketplaceListingRepository repository) {
        this.repository = repository;
    }

    public List<MarketplaceListing> list(Long tenantId) {
        return repository.findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(tenantId);
    }

    public List<MarketplaceListing> listByCategory(Long tenantId, String category) {
        return repository.findByTenantIdAndCategoryAndIsActiveTrue(tenantId, category);
    }

    public MarketplaceListing create(MarketplaceListing listing) {
        return repository.save(listing);
    }

    public MarketplaceListing update(Long id, MarketplaceListing updated) {
        MarketplaceListing listing = repository.findById(id).orElseThrow();
        listing.setTitle(updated.getTitle());
        listing.setDescription(updated.getDescription());
        listing.setPriceCents(updated.getPriceCents());
        listing.setCategory(updated.getCategory());
        return repository.save(listing);
    }

    public void deactivate(Long id) {
        MarketplaceListing l = repository.findById(id).orElseThrow();
        l.setIsActive(false);
        repository.save(l);
    }

    // ======================== P3 #105 — MARKETPLACE DE TEMPLATES ========================

    /**
     * Installe un template dans une église : valide la disponibilité et renvoie
     * la définition complète (structure à importer) + instructions.
     */
    @Transactional
    public Map<String, Object> install(Long id, Long tenantId) {
        MarketplaceListing listing = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MarketplaceListing", "id", String.valueOf(id)));
        if (!Boolean.TRUE.equals(listing.getIsActive())) {
            throw new IllegalStateException("Ce template n'est plus disponible.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("templateId", listing.getId());
        result.put("title", listing.getTitle());
        result.put("category", listing.getCategory());
        result.put("description", listing.getDescription());
        result.put("installedForTenant", tenantId);
        result.put("installedAt", java.time.LocalDateTime.now());
        result.put("nextSteps", List.of(
                "Vérifiez les éléments proposés par le template.",
                "Adaptez les noms et responsabilités à votre contexte.",
                "Activez le template depuis la page correspondante."));
        return result;
    }
}
