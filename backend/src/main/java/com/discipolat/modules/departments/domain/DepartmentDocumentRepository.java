package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentDocumentRepository extends JpaRepository<DepartmentDocument, UUID> {
    List<DepartmentDocument> findByDepartmentIdOrderByCreatedAtDesc(UUID departmentId);
    long countByDepartmentIdAndStatut(UUID departmentId, DepartmentDocument.DocumentStatus statut);
}
