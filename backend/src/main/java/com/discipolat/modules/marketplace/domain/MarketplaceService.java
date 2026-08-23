package com.discipolat.modules.marketplace.domain;

import org.springframework.stereotype.Service;

import java.util.List;

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
        MarketplaceListing listing = repository.findById(id).orElseThrow();
        listing.setIsActive(false);
        repository.save(listing);
    }
}
