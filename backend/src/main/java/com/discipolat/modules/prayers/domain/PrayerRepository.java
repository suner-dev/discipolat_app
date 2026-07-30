package com.discipolat.modules.prayers.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PrayerRepository extends JpaRepository<Prayer, UUID> {
    Page<Prayer> findByAuteurIdAndDeletedFalse(UUID auteurId, Pageable pageable);
    Page<Prayer> findByFamilleIdAndDeletedFalse(UUID familleId, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Prayer p WHERE p.familleId = ?1 AND p.deleted = false ORDER BY " +
            "CASE p.priorite WHEN 'HAUTE' THEN 0 WHEN 'MOYENNE' THEN 1 WHEN 'BASSE' THEN 2 ELSE 3 END, p.createdAt DESC")
    Page<Prayer> findByFamilleIdAndDeletedFalseOrderByPrioriteDateDesc(UUID familleId, Pageable pageable);
    Page<Prayer> findByFamilleIdAndStatutAndDeletedFalse(UUID familleId, String statut, Pageable pageable);
    Page<Prayer> findByFamilleIdAndCategorieAndDeletedFalse(UUID familleId, String categorie, Pageable pageable);
    Page<Prayer> findByAuteurIdAndStatutAndDeletedFalse(UUID auteurId, String statut, Pageable pageable);
    List<Prayer> findByAmeIdAndDeletedFalse(UUID ameId);
    long countByFamilleIdAndStatutAndDeletedFalse(UUID familleId, String statut);
    long countByFamilleIdAndDeletedFalse(UUID familleId);
    long countByAuteurIdAndDeletedFalse(UUID auteurId);
    Page<Prayer> findByVisibiliteInAndDeletedFalse(List<String> visibilites, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Prayer p WHERE (p.auteurId = ?1 OR p.visibilite IN ?2) AND p.deleted = false")
    Page<Prayer> findByAuteurIdOrVisibiliteIn(UUID auteurId, List<String> visibilites, Pageable pageable);
    List<Prayer> findByStatutAndDeletedFalseOrderByDateExauceeDesc(String statut);
    List<Prayer> findByFamilleIdAndStatutAndDeletedFalseOrderByDateExauceeDesc(UUID familleId, String statut);
}
