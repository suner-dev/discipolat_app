package com.discipolat.modules.members.domain;

import org.springframework.data.jpa.repository.JpaRepository;
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

    /** Présences des membres d'un groupe d'âmes (famille ou département), semaine la plus récente d'abord. */
    List<MemberPresence> findBySoulIdInOrderBySemaineDesc(List<UUID> soulIds);
}
