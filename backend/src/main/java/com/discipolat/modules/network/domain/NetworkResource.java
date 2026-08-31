package com.discipolat.modules.network.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ressource partagée entre églises (templates, guides, bonnes pratiques, formations).
 * Aucune donnée privée n'est exposée : seul le contenu marqué comme partagé est visible.
 *
 * NOTE multi-tenant : PAS de @Filter(tenantFilter) Hibernate ici. Le réseau est
 * volontairement INTER-églises : les listes publiques doivent agréger plusieurs
 * tenants. L'isolation est garantie au niveau service (NetworkService) :
 *  - toute création force tenantId = tenant courant ;
 *  - toute opération sensible vérifie la propriété (anti-IDOR) ;
 *  - les requêtes « mes données » filtrent explicitement par tenantId.
 */
@Entity
@Table(name = "network_resources")
public class NetworkResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category = "BEST_PRACTICE";

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType = "GUIDE";

    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "text[]")
    private String[] tags;

    @Column(name = "shared_with_public", nullable = false)
    private Boolean sharedWithPublic = false;

    @Column(name = "shared_by_user_id")
    private UUID sharedByUserId;

    @Column(nullable = false)
    private Integer downloads = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Category {
        BEST_PRACTICE, TEMPLATE, TRAINING, GUIDE, TOOL, DOCUMENT, FORMATION
    }

    public enum ResourceType {
        GUIDE, TEMPLATE, COURSE, DOCUMENT, VIDEO, AUDIO, LINK
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String[] getTags() { return tags; }
    public void setTags(String[] tags) { this.tags = tags; }
    public Boolean getSharedWithPublic() { return sharedWithPublic; }
    public void setSharedWithPublic(Boolean sharedWithPublic) { this.sharedWithPublic = sharedWithPublic; }
    public UUID getSharedByUserId() { return sharedByUserId; }
    public void setSharedByUserId(UUID sharedByUserId) { this.sharedByUserId = sharedByUserId; }
    public Integer getDownloads() { return downloads; }
    public void setDownloads(Integer downloads) { this.downloads = downloads; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
