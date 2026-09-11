package com.discipolat.modules.tenants.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status;

    @Column(name = "plan", nullable = false)
    private String plan;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "timezone", length = 64)
    private String timezone;

    @Column(name = "locale", length = 10)
    private String locale;

    @Column(name = "branding_json", columnDefinition = "jsonb")
    private String brandingJson;

    @Column(name = "features_json", columnDefinition = "jsonb")
    private String featuresJson;

    @Column(name = "settings_json", columnDefinition = "jsonb")
    private String settingsJson;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.plan == null) this.plan = "free";
        if (this.status == null) this.status = TenantStatus.PENDING_SETUP;
        if (this.country == null) this.country = "CM";
        if (this.currency == null) this.currency = "XAF";
        if (this.timezone == null) this.timezone = "Africa/Douala";
        if (this.locale == null) this.locale = "fr";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return id != null && id.equals(tenant.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
