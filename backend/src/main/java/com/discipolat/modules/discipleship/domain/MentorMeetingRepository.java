package com.discipolat.modules.discipleship.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorMeetingRepository extends JpaRepository<MentorMeeting, Long> {

    Page<MentorMeeting> findByTenantId(UUID tenantId, Pageable pageable);

    Page<MentorMeeting> findByTenantIdAndAssignmentId(UUID tenantId, Long assignmentId, Pageable pageable);

    Page<MentorMeeting> findByTenantIdAndMentorId(UUID tenantId, UUID mentorId, Pageable pageable);

    Page<MentorMeeting> findByTenantIdAndStatus(UUID tenantId, MentorMeeting.MeetingStatus status, Pageable pageable);

    Page<MentorMeeting> findByTenantIdAndScheduledAtBetween(UUID tenantId, Instant from, Instant to, Pageable pageable);

    Optional<MentorMeeting> findByTenantIdAndId(UUID tenantId, Long id);

    long countByTenantIdAndJourneyId(UUID tenantId, Long journeyId);
}
