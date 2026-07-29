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
    List<Soul> findAllByFaiseurId(UUID faiseurId);
    List<Soul> findAllByFamilleId(UUID familleId);
    long countByFaiseurId(UUID faiseurId);
    long countByFamilleId(UUID familleId);
    long countByTypeDisciple(TypeDisciple typeDisciple);
    long countByStatut(StatutAme statut);
    Page<Soul> findByStatutAndEtatSpirituel(StatutAme statut, String etatSpirituel, Pageable pageable);
    Page<Soul> findByEtatSpirituel(String etatSpirituel, Pageable pageable);
}
