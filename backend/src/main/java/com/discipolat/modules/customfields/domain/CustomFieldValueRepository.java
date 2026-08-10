package com.discipolat.modules.customfields.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomFieldValueRepository extends JpaRepository<CustomFieldValue, UUID> {
    List<CustomFieldValue> findByEntiteTypeAndEntiteId(String entiteType, UUID entiteId);
    Optional<CustomFieldValue> findByEntiteTypeAndEntiteIdAndFieldId(String entiteType, UUID entiteId, UUID fieldId);
    List<CustomFieldValue> findByFieldId(UUID fieldId);
}