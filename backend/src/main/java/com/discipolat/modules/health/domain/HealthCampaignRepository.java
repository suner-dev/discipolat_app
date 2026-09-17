package com.discipolat.modules.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HealthCampaignRepository extends JpaRepository<HealthCampaign, UUID> {
    Page<HealthCampaign> findByFamilyIdAndTenantId(UUID familyId, UUID tenantId, Pageable pageable);
    List<HealthCampaign> findByResponsibleIdAndTenantId(UUID responsibleId, UUID tenantId);
    List<HealthCampaign> findByCampaignTypeAndTenantIdAndDeletedFalse(HealthCampaign.CampaignType type, UUID tenantId);
    List<HealthCampaign> findByStatusAndTenantIdAndDeletedFalse(HealthCampaign.CampaignStatus status, UUID tenantId);
    Page<HealthCampaign> findByStatusAndTenantIdAndDeletedFalse(UUID tenantId, HealthCampaign.CampaignStatus status, Pageable pageable);
    Page<HealthCampaign> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);
    @Query("SELECT hc FROM HealthCampaign hc WHERE hc.tenantId = :tenantId AND hc.deleted = false AND hc.startDate <= :end AND hc.endDate >= :start")
    List<HealthCampaign> findActiveBetween(UUID tenantId, LocalDate start, LocalDate end);
    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, HealthCampaign.CampaignStatus status);
    long countByTenantIdAndDeletedFalse(UUID tenantId);
}
