package com.discipolat.modules.tenants.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SPEC_ORGANISATION_MODULABLE_V3 §A — repository du référentiel de niveaux.
 * Les niveaux sont scopés par <b>racine</b> (dénomination), pas par tenant :
 * on ne passe donc pas par le {@code tenantFilter} Hibernate, mais par
 * {@code rootTenantId} explicite.
 */
public interface OrganizationLevelRepository extends JpaRepository<OrganizationLevel, UUID> {

    List<OrganizationLevel> findByRootTenantIdOrderByDepthOrderAsc(UUID rootTenantId);

    List<OrganizationLevel> findByRootTenantIdAndActiveTrueOrderByDepthOrderAsc(UUID rootTenantId);

    Optional<OrganizationLevel> findByRootTenantIdAndDepthOrder(UUID rootTenantId, Integer depthOrder);

    List<OrganizationLevel> findByRootTenantIdAndParentLevelId(UUID rootTenantId, UUID parentLevelId);

    long countByRootTenantIdAndParentLevelId(UUID rootTenantId, UUID parentLevelId);

    /** §6.2 console plateforme — simple COUNT des niveaux d'une racine (D7 : un nombre, jamais du nominatif). */
    long countByRootTenantId(UUID rootTenantId);
}
