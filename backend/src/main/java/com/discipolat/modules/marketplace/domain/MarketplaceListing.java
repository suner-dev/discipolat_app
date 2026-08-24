package com.discipolat.modules.marketplace.domain;

import java.util.UUID;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_listings")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class MarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "listing_type")
    @Enumerated(EnumType.STRING)
    private ListingType listingType = ListingType.OFFER;

    private String category;

    @Column(name = "price_cents")
    private Long priceCents;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "contact_info")
    private String contactInfo;

    public enum ListingType {
        OFFER, REQUEST, SERVICE, FREE
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ListingType getListingType() { return listingType; }
    public void setListingType(ListingType listingType) { this.listingType = listingType; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Long getPriceCents() { return priceCents; }
    public void setPriceCents(Long priceCents) { this.priceCents = priceCents; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
