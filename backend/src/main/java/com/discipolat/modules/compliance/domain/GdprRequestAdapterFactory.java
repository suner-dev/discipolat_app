package com.discipolat.modules.compliance.domain;

import com.discipolat.modules.gdpr.domain.GdprRequestRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Pont entre le ComplianceManager et le module gdpr (GdprRequest). */
public class GdprRequestAdapterFactory {

    @org.springframework.stereotype.Component
    public static class Adapter implements ComplianceManagerService.GdprRequestAdapter {

        private final GdprRequestRepository repository;

        public Adapter(GdprRequestRepository repository) {
            this.repository = repository;
        }

        @Override
        public List<GdprRequestSnapshot> findByTenant(java.util.UUID tenantId) {
            return repository.findByTenantIdOrderByRequestedAtDesc(tenantId).stream()
                    .map(r -> new GdprRequestSnapshot(r.getId(), r.getRequestedAt()))
                    .toList();
        }
    }
}
