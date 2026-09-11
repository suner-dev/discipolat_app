package com.discipolat.modules.platform.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.audit.domain.AuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Tenant Settings Controller (Section 27, 37 du prompt)
 * Configuration complète du tenant
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
public class TenantSettingsController {

    private final TenantRepository tenantRepository;
    private final AuditService auditService;

    public TenantSettingsController(TenantRepository tenantRepository, AuditService auditService) {
        this.tenantRepository = tenantRepository;
        this.auditService = auditService;
    }

    private UUID getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new SecurityException("Aucun tenant");
        return tenantId;
    }

    private UUID getCurrentUserId() {
        return TenantContext.getCurrentUserId();
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantSettingsResponse> getSettings() {
        UUID tenantId = getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        Map<String, Object> settings = parseJson(tenant.getSettingsJson());

        return ResponseEntity.ok(new TenantSettingsResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getDescription(),
                tenant.getCountry(),
                tenant.getCurrency(),
                tenant.getTimezone(),
                tenant.getLocale(),
                settings.getOrDefault("dateFormat", "dd/MM/yyyy"),
                settings.getOrDefault("phoneCountryCode", "+237"),
                settings.getOrDefault("email", ""),
                settings.getOrDefault("phone", ""),
                settings.getOrDefault("website", ""),
                settings.getOrDefault("openingHours", Map.of()),
                settings.getOrDefault("workingDays", List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")),
                tenant.getStatus(),
                tenant.getTrialEndsAt()
        ));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
    public ResponseEntity<TenantSettingsResponse> updateSettings(@RequestBody TenantSettingsRequest request) {
        UUID tenantId = getCurrentTenantId();
        UUID currentUserId = getCurrentUserId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant non trouvé"));

        // Mettre à jour les champs de base
        if (request.name() != null) tenant.setName(request.name());
        if (request.description() != null) tenant.setDescription(request.description());
        if (request.country() != null) tenant.setCountry(request.country());
        if (request.currency() != null) tenant.setCurrency(request.currency());
        if (request.timezone() != null) tenant.setTimezone(request.timezone());
        if (request.locale() != null) tenant.setLocale(request.locale());

        // Mettre à jour les settings JSON
        Map<String, Object> settings = parseJson(tenant.getSettingsJson());
        if (request.dateFormat() != null) settings.put("dateFormat", request.dateFormat());
        if (request.phoneCountryCode() != null) settings.put("phoneCountryCode", request.phoneCountryCode());
        if (request.email() != null) settings.put("email", request.email());
        if (request.phone() != null) settings.put("phone", request.phone());
        if (request.website() != null) settings.put("website", request.website());
        if (request.openingHours() != null) settings.put("openingHours", request.openingHours());
        if (request.workingDays() != null) settings.put("workingDays", request.workingDays());
        
        tenant.setSettingsJson(toJson(settings));
        tenantRepository.save(tenant);

        auditService.log(currentUserId, tenantId, "SETTINGS_UPDATED", "TENANT",
                tenantId, "SUCCESS",
                Map.of("fields", request.fieldsUpdated()),
                null, null, null);

        return ResponseEntity.ok(toResponse(tenant, settings));
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return new java.util.HashMap<>();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            return new java.util.HashMap<>();
        }
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private TenantSettingsResponse toResponse(Tenant tenant, Map<String, Object> settings) {
        return new TenantSettingsResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getDescription(),
                tenant.getCountry(),
                tenant.getCurrency(),
                tenant.getTimezone(),
                tenant.getLocale(),
                settings.getOrDefault("dateFormat", "dd/MM/yyyy"),
                settings.getOrDefault("phoneCountryCode", "+237"),
                settings.getOrDefault("email", ""),
                settings.getOrDefault("phone", ""),
                settings.getOrDefault("website", ""),
                settings.getOrDefault("openingHours", Map.of()),
                settings.getOrDefault("workingDays", List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")),
                tenant.getStatus(),
                tenant.getTrialEndsAt()
        );
    }

    public record TenantSettingsResponse(
            UUID id, String name, String slug, String description,
            String country, String currency, String timezone, String locale,
            String dateFormat, String phoneCountryCode,
            String email, String phone, String website,
            Map<String, Object> openingHours, java.util.List<String> workingDays,
            com.discipolat.modules.tenants.domain.TenantStatus status,
            java.time.Instant trialEndsAt
    ) {}

    public record TenantSettingsRequest(
            String name, String description, String country, String currency,
            String timezone, String locale, String dateFormat, String phoneCountryCode,
            String email, String phone, String website,
            Map<String, Object> openingHours, java.util.List<String> workingDays,
            java.util.List<String> fieldsUpdated
    ) {}
}
