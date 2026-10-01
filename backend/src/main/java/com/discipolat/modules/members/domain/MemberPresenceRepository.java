package com.discipolat.modules.members.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberPresenceRepository extends JpaRepository<MemberPresence, UUID> {

    List<MemberPresence> findByUserIdOrderBySemaineDesc(UUID userId);

    /** Toutes les présences (vue pasteur / admin), semaine la plus récente d'abord. */
    List<MemberPresence> findAllByOrderBySemaineDesc();

    Optional<MemberPresence> findByUserIdAndSemaine(UUID userId, LocalDate semaine);

    /** Présence d'une âme pour une semaine (saisie par le responsable). */
    Optional<MemberPresence> findBySoulIdAndSemaine(UUID soulId, LocalDate semaine);

    /**
     * PORT Develop1 (§G7 synchronisation hors ligne) : test de conflit LWW — une
     * présence du tenant a-t-elle été modifiée APRÈS la saisie terrain ? Scopé
     * tenant : une écriture d'un autre tenant ne doit jamais faire croire à un
     * conflit local.
     */
    List<MemberPresence> findByTenantIdAndUpdatedAtAfter(UUID tenantId, java.time.LocalDateTime after);

    /** Présences des membres d'un groupe d'âmes (famille ou département), semaine la plus récente d'abord. */
    List<MemberPresence> findBySoulIdInOrderBySemaineDesc(List<UUID> soulIds);

    // AI module methods
    long countByTenantIdAndCreatedAtAfter(UUID tenantId, java.time.LocalDateTime date);
    @Query("SELECT (COUNT(DISTINCT CASE WHEN mp.present = true THEN mp.soulId END) * 1.0d) / NULLIF(COUNT(DISTINCT mp.soulId), 0) FROM MemberPresence mp WHERE mp.tenantId = :tenantId AND mp.semaine BETWEEN :start AND :end")
    double calculatePresenceRate(@Param("tenantId") UUID tenantId, @Param("start") java.time.LocalDate start, @Param("end") java.time.LocalDate end);
}
