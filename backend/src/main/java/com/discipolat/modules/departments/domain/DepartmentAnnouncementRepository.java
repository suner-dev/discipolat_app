package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepartmentAnnouncementRepository extends JpaRepository<DepartmentAnnouncement, UUID> {

    List<DepartmentAnnouncement> findByDepartmentIdAndDeletedFalseOrderByCreatedAtDesc(UUID departmentId);

    List<DepartmentAnnouncement> findByDepartmentIdAndDeletedFalseAndCibleIn(UUID departmentId,
                                                                            List<DepartmentAnnouncement.Cible> cibles);
}
