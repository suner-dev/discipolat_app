package com.discipolat.modules.discipleship.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByTenantIdAndAssignmentId(UUID tenantId, Long assignmentId);

    Page<MentorMeeting> findByTenantIdAndScheduledAtBetweenOrderByScheduledAtDesc(
            UUID tenantId, Instant from, Instant to, Pageable pageable);

    /**
     * Le nombre de réunions d'un PARCOURS.
     *
     * <p>{@code MentorMeeting} ne porte pas de colonne {@code journey_id} : le
     * parcours est porté par l'assignation. Une requête dérivée
     * {@code countByTenantIdAndJourneyId} lèverait donc
     * {@code PropertyReferenceException: No property 'journeyId' found for type
     * 'MentorMeeting'} — et, comme le repository est instancié au démarrage du
     * contexte, cela empêchait l'APPLICATION ENTIÈRE de démarrer (278 tests en
     * erreur). D'où la jointure explicite.
     */
    @Query("SELECT COUNT(m) FROM MentorMeeting m, MentorAssignment a "
            + "WHERE m.assignmentId = a.id AND m.tenantId = :tenantId "
            + "AND a.tenantId = :tenantId AND a.journeyId = :journeyId")
    long countByTenantIdAndJourneyId(@Param("tenantId") UUID tenantId,
                                     @Param("journeyId") Long journeyId);

    /** Idem, restreint à un statut — utilisé par le rapport de parcours. */
    @Query("SELECT COUNT(m) FROM MentorMeeting m, MentorAssignment a "
            + "WHERE m.assignmentId = a.id AND m.tenantId = :tenantId "
            + "AND a.tenantId = :tenantId AND a.journeyId = :journeyId "
            + "AND m.status = :status")
    long countByTenantIdAndJourneyIdAndStatus(@Param("tenantId") UUID tenantId,
                                              @Param("journeyId") Long journeyId,
                                              @Param("status") MentorMeeting.MeetingStatus status);
}
