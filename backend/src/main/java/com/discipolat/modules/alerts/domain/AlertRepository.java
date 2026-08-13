package com.discipolat.modules.alerts.domain;

import com.discipolat.common.enums.StatutAlerte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
