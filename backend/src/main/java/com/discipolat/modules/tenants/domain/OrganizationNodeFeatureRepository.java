package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §D — repository des modules par nœud.
 */
public interface OrganizationNodeFeatureRepository extends JpaRepository<OrganizationNodeFeature, UUID> {

    List<OrganizationNodeFeature> findByTenantIdAndNodeId(UUID tenantId, UUID nodeId);

    Optional<OrganizationNodeFeature> findByNodeIdAndModuleCode(UUID nodeId, String moduleCode);

    List<OrganizationNodeFeature> findByTenantIdAndModuleCodeAndEnabledTrue(UUID tenantId, String moduleCode);

    void deleteByTenantIdAndNodeId(UUID tenantId, UUID nodeId);
}
