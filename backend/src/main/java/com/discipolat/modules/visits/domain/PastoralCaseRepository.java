package com.discipolat.modules.visits.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PastoralCaseRepository extends JpaRepository<PastoralCase, UUID> {

    List<PastoralCase> findByTenantId(UUID tenantId);

    List<PastoralCase> findByTenantIdAndAssignedTo(UUID tenantId, UUID assignedTo);

    List<PastoralCase> findByTenantIdAndPersonId(UUID tenantId, UUID personId);

    List<PastoralCase> findByTenantIdAndStatus(UUID tenantId, PastoralCase.Status status);

    Optional<PastoralCase> findByTenantIdAndId(UUID tenantId, UUID id);

    long countByTenantIdAndStatus(UUID tenantId, PastoralCase.Status status);
}
