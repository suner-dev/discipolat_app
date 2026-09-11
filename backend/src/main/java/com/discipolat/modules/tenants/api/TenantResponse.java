package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantStatus;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        String slug,
        TenantStatus status,
        String plan,
        String country,
        String currency,
        String timezone,
        String locale,
        String brandingJson,
        String featuresJson,
        String settingsJson,
        Instant trialEndsAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getStatus(),
                tenant.getPlan(),
                tenant.getCountry(),
                tenant.getCurrency(),
                tenant.getTimezone(),
                tenant.getLocale(),
                tenant.getBrandingJson(),
                tenant.getFeaturesJson(),
                tenant.getSettingsJson(),
                tenant.getTrialEndsAt(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
