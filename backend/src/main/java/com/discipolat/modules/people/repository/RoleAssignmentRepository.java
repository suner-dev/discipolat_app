package com.discipolat.modules.people.repository;

import com.discipolat.modules.people.domain.RoleAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, UUID> {

    List<RoleAssignment> findByTenantIdAndPersonIdAndStatus(UUID tenantId, UUID personId, String status);

    List<RoleAssignment> findByTenantIdAndOrganizationUnitIdAndStatus(UUID tenantId, UUID orgUnitId, String status);

    List<RoleAssignment> findByTenantIdAndSpaceIdAndStatus(UUID tenantId, UUID spaceId, String status);

    List<RoleAssignment> findByRoleIdAndStatus(UUID roleId, String status);

    @Query("SELECT ra FROM RoleAssignment ra WHERE ra.tenantId = :tenantId AND ra.personId = :personId AND ra.status = 'ACTIVE' AND (ra.endedAt IS NULL OR ra.endedAt >= CURRENT_DATE)")
    List<RoleAssignment> findActiveByPersonId(@Param("tenantId") UUID tenantId, @Param("personId") UUID personId);

    Page<RoleAssignment> findByTenantIdOrderByStartedAtDesc(UUID tenantId, Pageable pageable);

    long countByTenantId(UUID tenantId);
}