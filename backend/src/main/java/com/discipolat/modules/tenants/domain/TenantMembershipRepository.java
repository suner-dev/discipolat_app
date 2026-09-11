package com.discipolat.modules.tenants.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    Optional<TenantMembership> findByUserIdAndTenantId(UUID userId, UUID tenantId);

    List<TenantMembership> findByUserIdAndStatus(UUID userId, MembershipStatus status);

    List<TenantMembership> findByTenantIdAndStatus(UUID tenantId, MembershipStatus status);

    Page<TenantMembership> findByTenantIdAndStatus(UUID tenantId, MembershipStatus status, Pageable pageable);

    List<TenantMembership> findByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, MembershipStatus status);

    boolean existsByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, MembershipStatus status);

    List<TenantMembership> findByTenantIdAndStatusAndRoleContaining(UUID tenantId, MembershipStatus status, String role);

    Page<TenantMembership> findByTenantIdAndStatusAndRoleContaining(
            UUID tenantId, MembershipStatus status, String role, Pageable pageable);

    Optional<TenantMembership> findByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, MembershipStatus status);

    List<TenantMembership> findByTenantIdAndStatusIn(UUID tenantId, List<MembershipStatus> statuses);

    Optional<TenantMembership> findByInvitedBy(UUID invitedBy);
}
