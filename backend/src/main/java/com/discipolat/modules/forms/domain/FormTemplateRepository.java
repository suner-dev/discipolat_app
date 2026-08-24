package com.discipolat.modules.forms.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FormTemplateRepository extends JpaRepository<FormTemplate, UUID> {
    List<FormTemplate> findByTenantIdOrderByCreeLeDesc(UUID tenantId);
    List<FormTemplate> findByTenantIdAndStatutOrderByCreeLeDesc(UUID tenantId, FormTemplate.Statut statut);
    long countByTenantId(UUID tenantId);
}
