package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.CustomFieldValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, UUID> {

    List<CustomFieldValue> findByEntityId(UUID entityId);

    List<CustomFieldValue> findByFieldId(UUID fieldId);

    Optional<CustomFieldValue> findByFieldIdAndEntityId(UUID fieldId, UUID entityId);

    void deleteByFieldId(UUID fieldId);

    void deleteByEntityId(UUID entityId);

    @Modifying
    @Query("DELETE FROM CustomFieldValue c WHERE c.fieldId = :fieldId AND c.entityId = :entityId")
    void deleteByFieldIdAndEntityId(@Param("fieldId") UUID fieldId, @Param("entityId") UUID entityId);

    long countByTenantId(UUID tenantId);
}