package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DepartmentChecklistItemRepository extends JpaRepository<DepartmentChecklistItem, UUID> {
    List<DepartmentChecklistItem> findByChecklistIdOrderByOrdreAsc(UUID checklistId);
    void deleteByChecklistId(UUID checklistId);
}
