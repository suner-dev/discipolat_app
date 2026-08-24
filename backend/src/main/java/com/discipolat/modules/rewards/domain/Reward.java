package com.discipolat.modules.rewards.domain;

import java.util.UUID;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rewards")
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "points_required", nullable = false)
    private Integer pointsRequired;

    @Column(name = "reward_type")
    @Enumerated(EnumType.STRING)
    private RewardType rewardType = RewardType.BADGE;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public enum RewardType {
        BADGE, CERTIFICATE, PRIVILEGE, PHYSICAL, VIRTUAL
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public Integer getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(Integer pointsRequired) { this.pointsRequired = pointsRequired; }
    public RewardType getRewardType() { return rewardType; }
    public void setRewardType(RewardType rewardType) { this.rewardType = rewardType; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
