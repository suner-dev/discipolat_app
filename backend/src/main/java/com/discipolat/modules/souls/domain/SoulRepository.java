package com.discipolat.modules.souls.domain;

import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SoulRepository extends JpaRepository<Soul, UUID> {
    Page<Soul> findByFaiseurId(UUID faiseurId, Pageable pageable);
    Page<Soul> findByFamilleId(UUID familleId, Pageable pageable);
    Page<Soul> findByTypeDisciple(TypeDisciple typeDisciple, Pageable pageable);
    Page<Soul> findByStatut(StatutAme statut, Pageable pageable);
    Page<Soul> findByTypeDiscipleAndStatut(TypeDisciple typeDisciple, StatutAme statut, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Soul s WHERE s.deleted = false AND " +
            "(LOWER(s.nom) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.prenom) LIKE LOWER(CONCAT('%', :q, '%')) " +
            "OR LOWER(s.email) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(s.telephone) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<Soul> search(@org.springframework.data.repository.query.Param("q") String q, Pageable pageable);

    List<Soul> findAllByFaiseurId(UUID faiseurId);
    List<Soul> findAllByFamilleId(UUID familleId);
    List<Soul> findByFamilleIdIn(List<UUID> familleIds);
    Page<Soul> findByFamilleIdIn(List<UUID> familleIds, Pageable pageable);
    List<Soul> findByFaiseurIdIn(List<UUID> faiseurIds);
    long countByFaiseurId(UUID faiseurId);
    long countByFamilleId(UUID familleId);
    long countByTypeDisciple(TypeDisciple typeDisciple);
    long countByStatut(StatutAme statut);
    Page<Soul> findByStatutAndEtatSpirituel(StatutAme statut, String etatSpirituel, Pageable pageable);
    Page<Soul> findByEtatSpirituel(String etatSpirituel, Pageable pageable);
    long countByFamilleIdAndStatut(UUID familleId, StatutAme statut);
    List<Soul> findByFaiseurIdAndStatut(UUID faiseurId, StatutAme statut);
    List<Soul> findAllByUserId(UUID userId);
    Page<Soul> findByUserId(UUID userId, Pageable pageable);
    List<Soul> findByUserIdIsNotNull();
    Page<Soul> findByDeletedTrue(Pageable pageable);
}
