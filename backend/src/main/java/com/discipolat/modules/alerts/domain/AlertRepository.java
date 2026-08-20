package com.discipolat.modules.alerts.domain;

import com.discipolat.common.enums.StatutAlerte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {
    Page<Alert> findByStatut(StatutAlerte statut, Pageable pageable);
    Page<Alert> findByFamilleId(UUID familleId, Pageable pageable);
    List<Alert> findByAmeIdAndStatut(UUID ameId, StatutAlerte statut);
    long countByStatut(StatutAlerte statut);

    /** Alertes actives d'un département (alertes intelligentes). */
    List<Alert> findByDepartmentIdAndStatut(UUID departmentId, StatutAlerte statut);

    /** Déduplication des alertes automatiques (type + âme + département). */
    boolean existsByDepartmentIdAndAmeIdAndTypeAlerteAndStatut(UUID departmentId, UUID ameId, String typeAlerte, StatutAlerte statut);

    /** Source du Page Builder : alertes actives récentes. */
    List<Alert> findTop10ByStatutOrderByDateDeclenchementDesc(StatutAlerte statut);

    /** Sources du Page Builder scopées : alertes actives sur des âmes données. */
    long countByStatutAndAmeIdIn(StatutAlerte statut, Collection<UUID> ameIds);

    List<Alert> findTop10ByStatutAndAmeIdInOrderByDateDeclenchementDesc(StatutAlerte statut, Collection<UUID> ameIds);

    /** Source du Page Builder GRAPHIQUE : toutes les alertes actives (répartition par type). */
    List<Alert> findByStatut(StatutAlerte statut);

    /** Source du Page Builder GRAPHIQUE scopée : actives sur des âmes données. */
    List<Alert> findByStatutAndAmeIdIn(StatutAlerte statut, Collection<UUID> ameIds);

    /** Smart Alerts: find by type and tenant for deduplication. */
    List<Alert> findByTypeAlerteAndTenantId(String typeAlerte, UUID tenantId);

    /** Smart Alerts: count by statut and priorite for anomaly summary. */
    long countByStatutAndPriorite(StatutAlerte statut, String priorite);
}
