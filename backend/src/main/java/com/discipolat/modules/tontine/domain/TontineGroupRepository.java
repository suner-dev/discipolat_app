package com.discipolat.modules.tontine.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TontineGroupRepository extends JpaRepository<TontineGroup, UUID> {
    List<TontineGroup> findByStatutOrderByCreatedAtDesc(TontineGroup.Statut statut);

    long countByStatut(TontineGroup.Statut statut);

    @Query("SELECT COALESCE(SUM(c.montant), 0) FROM TontineContribution c " +
           "WHERE c.groupId = :groupId AND c.paye = true")
    java.math.BigDecimal totalCollecte(@Param("groupId") UUID groupId);
}
