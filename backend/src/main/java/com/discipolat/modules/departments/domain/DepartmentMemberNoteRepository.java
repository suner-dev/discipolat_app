package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentMemberNoteRepository extends JpaRepository<DepartmentMemberNote, UUID> {

    List<DepartmentMemberNote> findByDepartmentIdAndMemberIdAndDeletedFalseOrderByCreatedAtDesc(UUID departmentId, UUID memberId);

    List<DepartmentMemberNote> findByDepartmentIdAndDeletedFalseOrderByCreatedAtDesc(UUID departmentId);
}
