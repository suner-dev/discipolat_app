package com.discipolat.modules.network.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Événement inter-églises (conférences, journées portes ouvertes, formations).
 * Visible uniquement si shared_with_public = true.
 *
 * NOTE multi-tenant : PAS de @Filter(tenantFilter) Hibernate ici — le réseau est
 * volontairement INTER-églises (voir NetworkResource). Isolation garantie dans
 * NetworkService (création forcée au tenant courant + vérifications de propriété).
 */
@Entity
@Table(name = "network_events")
public class NetworkEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType = "CONFERENCE";

    @Column(length = 500)
    private String location;

    @Column(length = 200)
    private String city;

    @Column(length = 100)
    private String country;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "current_participants", nullable = false)
    private Integer currentParticipants = 0;

    @Column(name = "is_virtual", nullable = false)
    private Boolean isVirtual = false;

    @Column(name = "virtual_link", length = 1000)
    private String virtualLink;

    @Column(name = "shared_with_public", nullable = false)
    private Boolean sharedWithPublic = false;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum EventType {
        CONFERENCE, WORKSHOP, RETREAT, MEETING, TRAINING, OPEN_DAY, PRAYER
    }

    /**
     * Champ dérivé (non persisté) : l'utilisateur courant est-il inscrit ?
     * Calculé par NetworkService à partir de network_event_participants.
     */
    @Transient
    private Boolean joinedByMe = false;

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
    public Boolean getJoinedByMe() { return joinedByMe; }
    public void setJoinedByMe(Boolean joinedByMe) { this.joinedByMe = joinedByMe; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    public Integer getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(Integer currentParticipants) { this.currentParticipants = currentParticipants; }
    public Boolean getIsVirtual() { return isVirtual; }
    public void setIsVirtual(Boolean isVirtual) { this.isVirtual = isVirtual; }
    public String getVirtualLink() { return virtualLink; }
    public void setVirtualLink(String virtualLink) { this.virtualLink = virtualLink; }
    public Boolean getSharedWithPublic() { return sharedWithPublic; }
    public void setSharedWithPublic(Boolean sharedWithPublic) { this.sharedWithPublic = sharedWithPublic; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId) { this.createdByUserId = createdByUserId; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
