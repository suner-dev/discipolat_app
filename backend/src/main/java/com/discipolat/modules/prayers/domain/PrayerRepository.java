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

    // Recherche combinée sans critère obligatoire : le contrôleur doit pouvoir
    // honorer statut/catégorie/visibilité seuls (avant, ces paramètres étaient
    // ignorés hors contexte « famille » — les filtres de la page Prières
    // étaient décoratifs). CAST : typage explicite requis sur PostgreSQL.
    @org.springframework.data.jpa.repository.Query(
            "SELECT p FROM Prayer p WHERE p.deleted = false " +
            "AND (CAST(:statut AS string) IS NULL OR p.statut = :statut) " +
            "AND (CAST(:categorie AS string) IS NULL OR p.categorie = :categorie) " +
            "AND (CAST(:visibilite AS string) IS NULL OR p.visibilite = :visibilite) " +
            "AND (CAST(:auteurId AS uuid) IS NULL OR p.auteurId = :auteurId) " +
            "AND (CAST(:familleId AS uuid) IS NULL OR p.familleId = :familleId)")
    Page<Prayer> searchFiltered(@org.springframework.data.repository.query.Param("statut") String statut,
                                @org.springframework.data.repository.query.Param("categorie") String categorie,
                                @org.springframework.data.repository.query.Param("visibilite") String visibilite,
                                @org.springframework.data.repository.query.Param("auteurId") UUID auteurId,
                                @org.springframework.data.repository.query.Param("familleId") UUID familleId,
                                Pageable pageable);

    // Totaux agrégés (piloton des cartes KPI /prayers) : le front ne doit PAS
    // décompter la seule page courante — périmètre = tenant via @Filter tenantFilter.
    @org.springframework.data.jpa.repository.Query("SELECT p.statut, COUNT(p) FROM Prayer p WHERE p.deleted = false GROUP BY p.statut")
    List<Object[]> countGroupByStatut();

    @org.springframework.data.jpa.repository.Query("SELECT p.categorie, COUNT(p) FROM Prayer p WHERE p.deleted = false GROUP BY p.categorie")
    List<Object[]> countGroupByCategorie();

    @org.springframework.data.jpa.repository.Query("SELECT p.priorite, COUNT(p) FROM Prayer p WHERE p.deleted = false GROUP BY p.priorite")
    List<Object[]> countGroupByPriorite();
}
