package com.discipolat.modules.tasks.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskTimeEntryRepository extends JpaRepository<TaskTimeEntry, Long> {

    List<TaskTimeEntry> findByTenantIdAndTaskIdOrderByStartTimeAsc(UUID tenantId, Long taskId);

    Optional<TaskTimeEntry> findByTenantIdAndId(UUID tenantId, Long id);

    /**
     * Somme des durées d'une tâche.
     *
     * <p>Écrit en {@code @Query} et NON en nom dérivé : Spring Data ne sait pas
     * dériver {@code sumDurationMinutes...} (il l'interprétait comme une
     * propriété {@code sumDurationMinutesByTenantId} et levait
     * {@code PropertyReferenceException}, empêchant le démarrage du contexte —
     * donc de toute l'application).
     */
    @Query("SELECT COALESCE(SUM(e.durationMinutes), 0) FROM TaskTimeEntry e "
            + "WHERE e.tenantId = :tenantId AND e.taskId = :taskId")
    Integer sumDurationMinutes(@Param("tenantId") UUID tenantId, @Param("taskId") Long taskId);
}
