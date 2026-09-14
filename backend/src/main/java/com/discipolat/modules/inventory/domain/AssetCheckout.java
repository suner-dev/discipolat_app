package com.discipolat.modules.inventory.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Asset Checkout — tracks the checkout and return history of inventory items.
 * Enables full audit trail of who had what asset and when.
 */
@Entity
@Table(name = "asset_checkout", indexes = {
        @Index(name = "idx_checkout_item", columnList = "item_id"),
        @Index(name = "idx_checkout_tenant", columnList = "tenant_id"),
        @Index(name = "idx_checkout_member", columnList = "member_id"),
        @Index(name = "idx_checkout_status", columnList = "status")
})
public class AssetCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "space_id")
    private UUID spaceId;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "checked_out_at", nullable = false)
    private LocalDateTime checkedOutAt;

    @Column(name = "due_back_at")
    private LocalDateTime dueBackAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "status", nullable = false)
    private String status; // CHECKED_OUT, RETURNED, OVERDUE, DAMAGED, LOST

    @Column(name = "condition_on_checkout", length = 50)
    private String conditionOnCheckout;

    @Column(name = "condition_on_return", length = 50)
    private String conditionOnReturn;

    @Column(name = "checked_out_by")
    private UUID checkedOutBy;

    @Column(name = "returned_by")
    private UUID returnedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "CHECKED_OUT";
        if (checkedOutAt == null) checkedOutAt = LocalDateTime.now();
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }
    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }
    public UUID getSpaceId() { return spaceId; }
    public void setSpaceId(UUID spaceId) { this.spaceId = spaceId; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public LocalDateTime getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(LocalDateTime checkedOutAt) { this.checkedOutAt = checkedOutAt; }
    public LocalDateTime getDueBackAt() { return dueBackAt; }
    public void setDueBackAt(LocalDateTime dueBackAt) { this.dueBackAt = dueBackAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getConditionOnCheckout() { return conditionOnCheckout; }
    public void setConditionOnCheckout(String conditionOnCheckout) { this.conditionOnCheckout = conditionOnCheckout; }
    public String getConditionOnReturn() { return conditionOnReturn; }
    public void setConditionOnReturn(String conditionOnReturn) { this.conditionOnReturn = conditionOnReturn; }
    public UUID getCheckedOutBy() { return checkedOutBy; }
    public void setCheckedOutBy(UUID checkedOutBy) { this.checkedOutBy = checkedOutBy; }
    public UUID getReturnedBy() { return returnedBy; }
    public void setReturnedBy(UUID returnedBy) { this.returnedBy = returnedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
