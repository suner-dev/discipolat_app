package com.discipolat.modules.souls.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SoulTagRepository extends JpaRepository<SoulTag, UUID> {
    List<SoulTag> findBySoulIdOrderByTagAsc(UUID soulId);
    Optional<SoulTag> findBySoulIdAndTagIgnoreCase(UUID soulId, String tag);
    void deleteBySoulIdAndTagIgnoreCase(UUID soulId, String tag);

    /** Tous les tags distincts utilisés (pour l'autocomplétion). */
    @Query("SELECT DISTINCT st.tag FROM SoulTag st ORDER BY st.tag")
    List<String> findAllTags();
}
