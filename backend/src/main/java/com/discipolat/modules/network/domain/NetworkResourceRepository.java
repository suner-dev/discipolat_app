package com.discipolat.modules.network.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NetworkResourceRepository extends JpaRepository<NetworkResource, UUID> {

    /** Ressources partagées par une église (visible par toutes les autres). */
    List<NetworkResource> findBySharedWithPublicTrueAndIsActiveTrueOrderByCreatedAtDesc();

    /** Ressources d'une église spécifique (pour gérer ses propres partages). */
    List<NetworkResource> findByTenantIdAndIsActiveTrueOrderByCreatedAtDesc(UUID tenantId);

    /** Recherche par catégorie. */
    List<NetworkResource> findBySharedWithPublicTrueAndCategoryAndIsActiveTrueOrderByCreatedAtDesc(String category);

    /** Recherche par texte dans le titre ou la description. */
    @Query("SELECT r FROM NetworkResource r WHERE r.sharedWithPublic = TRUE AND r.isActive = TRUE " +
           "AND (LOWER(r.title) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(r.description) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<NetworkResource> search(@Param("q") String query);

    /** Statistiques agrégées. */
    @Query("SELECT r.category, COUNT(r) FROM NetworkResource r WHERE r.sharedWithPublic = TRUE AND r.isActive = TRUE GROUP BY r.category")
    List<Object[]> countByCategory();

    long countByTenantIdAndIsActiveTrue(UUID tenantId);
}
