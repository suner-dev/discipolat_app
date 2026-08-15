package com.discipolat.modules.departments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentEventAttendanceRepository extends JpaRepository<DepartmentEventAttendance, UUID> {

    List<DepartmentEventAttendance> findByEventId(UUID eventId);

    Optional<DepartmentEventAttendance> findByDepartmentIdAndEventIdAndSoulId(UUID departmentId, UUID eventId, UUID soulId);

    long countByEventIdAndPresent(UUID eventId, boolean present);
}
