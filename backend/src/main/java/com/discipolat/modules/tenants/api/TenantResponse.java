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
        Instant updatedAt,
        // Additifs V183 (décision D2) — placés EN FIN de record pour ne pas
        // décaler l'index des champs existants côté désérialisation.
        Instant onboardingCompletedAt,
        UUID onboardingCompletedBy
) {
    /**
     * Constructeur de compatibilité : avant V183, l'enregistrement exposait
     * 15 champs. Le conserver évite de casser les appelants existants
     * (PlatformProvisioningServiceTest, TenantRegistrationServiceTest…) et rend
     * l'ajout réellement <b>additif</b>, conformément au plan.
     */
    public TenantResponse(
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
            Instant updatedAt) {
        this(id, name, slug, status, plan, country, currency, timezone, locale,
                brandingJson, featuresJson, settingsJson,
                trialEndsAt, createdAt, updatedAt, null, null);
    }

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
                tenant.getUpdatedAt(),
                tenant.getOnboardingCompletedAt(),
                tenant.getOnboardingCompletedBy()
        );
    }
}
