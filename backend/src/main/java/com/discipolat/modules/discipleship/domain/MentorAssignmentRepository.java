package com.discipolat.modules.discipleship.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorAssignmentRepository extends JpaRepository<MentorAssignment, Long> {

    Page<MentorAssignment> findByTenantId(UUID tenantId, Pageable pageable);

    Page<MentorAssignment> findByTenantIdAndMentorId(UUID tenantId, UUID mentorId, Pageable pageable);

    Page<MentorAssignment> findByTenantIdAndDiscipleId(UUID tenantId, UUID discipleId, Pageable pageable);

    Page<MentorAssignment> findByTenantIdAndJourneyId(UUID tenantId, Long journeyId, Pageable pageable);

    Page<MentorAssignment> findByTenantIdAndStatus(UUID tenantId, MentorAssignment.AssignmentStatus status, Pageable pageable);

    Optional<MentorAssignment> findByTenantIdAndId(UUID tenantId, Long id);
}
