package com.discipolat.modules.facerec.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaceTemplateRepository extends JpaRepository<FaceTemplate, UUID> {

    Optional<FaceTemplate> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    List<FaceTemplate> findByTenantIdAndActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    long countByTenantIdAndActiveTrue(UUID tenantId);

    /** Recherche par nom d'affichage (admin). */
    @Query("select f from FaceTemplate f where f.tenantId = :tenantId and f.active = true " +
            "and lower(f.displayName) like lower(concat('%', :q, '%')) order by f.createdAt desc")
    List<FaceTemplate> searchByDisplayName(@Param("tenantId") UUID tenantId, @Param("q") String q);
}
