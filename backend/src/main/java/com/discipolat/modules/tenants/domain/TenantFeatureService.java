package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service pour la gestion des modules par tenant (G1.3 - §29)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TenantFeatureService {

    private final TenantFeatureRepository repository;
    private final TenantRepository tenantRepository;

    public List<TenantFeature> getFeaturesByTenant(UUID tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public Optional<TenantFeature> getFeature(UUID tenantId, String moduleCode) {
        return repository.findByTenantIdAndModuleCode(tenantId, moduleCode);
    }

    public TenantFeature enableFeature(UUID tenantId, String moduleCode, Map<String, Object> configuration) {
        return upsertFeature(tenantId, moduleCode, true, configuration, null);
    }

    public TenantFeature disableFeature(UUID tenantId, String moduleCode) {
        return upsertFeature(tenantId, moduleCode, false, null, null);
    }

    public TenantFeature updateConfiguration(UUID tenantId, String moduleCode, Map<String, Object> configuration) {
        TenantFeature feature = repository.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .orElseThrow(() -> new EntityNotFoundException("TenantFeature", "moduleCode", moduleCode));
        feature.setConfigurationJson(configuration);
        return repository.save(feature);
    }

    public List<TenantFeature> getEnabledFeatures(UUID tenantId) {
        return repository.findEnabledByTenantId(tenantId);
    }

    /**
     * G5.4 (§55-2 RequireFeature) — État d'activation canonique de tous les
     * modules du tenant (code → enabled), source unique pour le drapeau
     * {@code features} du contexte résolu. Les modules sans ligne explicite
     * ne figurent pas dans la carte : le frontend traite une absence comme
     * « non géré » (jamais un refus arbitraire), une valeur false comme un
     * refus — miroir du 403 backend.
     */
    public Map<String, Boolean> getFeatureFlags(UUID tenantId) {
        Map<String, Boolean> flags = new java.util.LinkedHashMap<>();
        for (TenantFeature f : repository.findByTenantId(tenantId)) {
            if (f.getModuleCode() != null) {
                flags.put(f.getModuleCode().toUpperCase(), Boolean.TRUE.equals(f.getEnabled()));
            }
        }
        return flags;
    }

    private TenantFeature upsertFeature(UUID tenantId, String moduleCode, boolean enabled,
                                         Map<String, Object> configuration, Map<String, Object> limits) {
        TenantFeature feature = repository.findByTenantIdAndModuleCode(tenantId, moduleCode).orElse(null);
        if (feature == null) {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Tenant", "id", tenantId.toString()));
            feature = TenantFeature.builder()
                    .tenant(tenant)
                    .moduleCode(moduleCode)
                    .enabled(enabled)
                    .configurationJson(configuration != null ? configuration : Map.of())
                    .limitsJson(limits != null ? limits : Map.of())
                    .build();
        } else {
            feature.setEnabled(enabled);
            if (configuration != null) {
                feature.setConfigurationJson(configuration);
            }
            if (limits != null) {
                feature.setLimitsJson(limits);
            }
        }
        return repository.save(feature);
    }
}
