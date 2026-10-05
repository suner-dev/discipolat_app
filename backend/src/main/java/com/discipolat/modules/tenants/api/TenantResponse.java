package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantKind;
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
        UUID onboardingCompletedBy,
        // SPEC_ORGANISATION_DENOMINATION_V2 (T-B2, D1/D2/D3) — modèle
        // d'organisation. Placés EN FIN comme les champs V183 pour ne pas
        // décaler l'index des champs existants côté désérialisation.
        // `rootTenantId` est celui de la racine RÉSOLU : une organisation
        // isolée vaut sa propre racine (cf. Tenant.effectiveRootTenantId()).
        TenantKind kind,
        UUID parentTenantId,
        UUID rootTenantId
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
                trialEndsAt, createdAt, updatedAt, null, null,
                TenantKind.CHURCH, null, null);
    }

    /**
     * Constructeur V183 + modèle organisation : garde l'API à 2 arguments pour
     * les appelants qui n'ont pas modifié leur construction (aucune valeur
     * d'organisation affirmée à tort).
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
            Instant updatedAt,
            Instant onboardingCompletedAt,
            UUID onboardingCompletedBy) {
        this(id, name, slug, status, plan, country, currency, timezone, locale,
                brandingJson, featuresJson, settingsJson,
                trialEndsAt, createdAt, updatedAt, onboardingCompletedAt, onboardingCompletedBy,
                TenantKind.CHURCH, null, null);
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
                tenant.getOnboardingCompletedBy(),
                // Racine RÉSOLUE : jamais null (une tenant isolée est sa propre
                // racine) — c'est ce que la console et le transfert consomment.
                tenant.getKind() == null ? TenantKind.CHURCH : tenant.getKind(),
                tenant.getParentTenantId(),
                tenant.effectiveRootTenantId()
        );
    }
}
