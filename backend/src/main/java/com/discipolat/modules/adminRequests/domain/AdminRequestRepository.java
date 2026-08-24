package com.discipolat.modules.adminRequests.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AdminRequestRepository extends JpaRepository<AdminRequest, UUID> {
    List<AdminRequest> findByTenantIdOrderBySoumiseLeDesc(UUID tenantId);
    List<AdminRequest> findByDemandeurIdOrderBySoumiseLeDesc(UUID demandeurId);
    List<AdminRequest> findByStatutOrderBySoumiseLeDesc(AdminRequest.Statut statut);
    long countByTenantIdAndStatut(UUID tenantId, AdminRequest.Statut statut);
}
