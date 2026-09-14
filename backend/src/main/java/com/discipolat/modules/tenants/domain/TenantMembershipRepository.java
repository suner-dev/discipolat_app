package com.discipolat.modules.tenants.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT tm FROM TenantMembership tm JOIN tm.role r WHERE tm.tenantId = :tenantId AND tm.status = :status AND LOWER(r.key) LIKE LOWER(CONCAT('%', :role, '%'))")
    List<TenantMembership> findByTenantIdAndStatusAndRoleContaining(
            @Param("tenantId") UUID tenantId,
            @Param("status") MembershipStatus status,
            @Param("role") String role);

    @Query("SELECT tm FROM TenantMembership tm JOIN tm.role r WHERE tm.tenantId = :tenantId AND tm.status = :status AND LOWER(r.key) LIKE LOWER(CONCAT('%', :role, '%'))")
    Page<TenantMembership> findByTenantIdAndStatusAndRoleContaining(
            @Param("tenantId") UUID tenantId,
            @Param("status") MembershipStatus status,
            @Param("role") String role,
            Pageable pageable);

    Optional<TenantMembership> findByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, MembershipStatus status);

    List<TenantMembership> findAllByUserIdAndTenantIdAndStatus(UUID userId, UUID tenantId, MembershipStatus status);

    List<TenantMembership> findByUserId(UUID userId);

    List<TenantMembership> findByTenantIdAndStatusIn(UUID tenantId, List<MembershipStatus> statuses);

    Optional<TenantMembership> findByInvitedBy(UUID invitedBy);

    @Query("SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TenantMembership tm WHERE tm.userId = :userId AND tm.role.id = :roleId AND tm.status = :status")
    boolean existsByUserIdAndRoleIdAndStatus(@Param("userId") UUID userId, @Param("roleId") UUID roleId, @Param("status") MembershipStatus status);

    @Query("SELECT COUNT(tm) FROM TenantMembership tm WHERE tm.role.id = :roleId")
    long countByRoleId(@Param("roleId") UUID roleId);

    long countByTenantId(UUID tenantId);

    long countByStatus(MembershipStatus status);
}
