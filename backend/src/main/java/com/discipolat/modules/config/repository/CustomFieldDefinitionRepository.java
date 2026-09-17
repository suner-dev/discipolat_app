package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.CustomFieldDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {

    List<CustomFieldDefinition> findByTenantIdAndEntityTypeAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID tenantId, String entityType);

    List<CustomFieldDefinition> findByTenantIdAndEntityTypeAndSpaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID tenantId, String entityType, UUID spaceId);

    Optional<CustomFieldDefinition> findByTenantIdAndEntityTypeAndSpaceIdAndFieldCodeAndDeletedAtIsNull(UUID tenantId, String entityType, UUID spaceId, String fieldCode);

    Optional<CustomFieldDefinition> findByTenantIdAndEntityTypeAndSpaceIdIsNullAndFieldCodeAndDeletedAtIsNull(UUID tenantId, String entityType, String fieldCode);

    boolean existsByTenantIdAndEntityTypeAndSpaceIdAndFieldCode(UUID tenantId, String entityType, UUID spaceId, String fieldCode);

    @Query("SELECT c FROM CustomFieldDefinition c WHERE c.tenantId = :tenantId AND c.entityType = :entityType AND (c.spaceId = :spaceId OR c.spaceId IS NULL) AND c.deletedAt IS NULL ORDER BY c.displayOrder ASC")
    List<CustomFieldDefinition> findResolvedDefinitions(@Param("tenantId") UUID tenantId, @Param("entityType") String entityType, @Param("spaceId") UUID spaceId);

    long countByTenantId(UUID tenantId);
}