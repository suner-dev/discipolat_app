package com.discipolat.modules.families.repository;

import com.discipolat.modules.families.domain.PastorateAppointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastorateAppointmentRepository extends JpaRepository<PastorateAppointment, UUID> {

    List<PastorateAppointment> findByTenantIdAndDeletedAtIsNullOrderByStartDateDesc(UUID tenantId);

    List<PastorateAppointment> findByTenantIdAndPastorIdAndDeletedAtIsNull(UUID tenantId, UUID pastorId);

    List<PastorateAppointment> findByTenantIdAndOrganizationUnitIdAndDeletedAtIsNull(UUID tenantId, UUID orgUnitId);

    @Query("SELECT pa FROM PastorateAppointment pa WHERE pa.tenantId = :tenantId AND pa.deletedAt IS NULL AND pa.startDate BETWEEN :from AND :to ORDER BY pa.startDate ASC")
    List<PastorateAppointment> findByTenantIdAndStartDateBetween(@Param("tenantId") UUID tenantId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<PastorateAppointment> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);
}