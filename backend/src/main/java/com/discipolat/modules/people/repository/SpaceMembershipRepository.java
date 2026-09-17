package com.discipolat.modules.people.repository;

import com.discipolat.modules.people.domain.SpaceMembership;
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
public interface SpaceMembershipRepository extends JpaRepository<SpaceMembership, UUID> {

    List<SpaceMembership> findByTenantIdAndSpaceIdAndStatus(UUID tenantId, UUID spaceId, String status);

    List<SpaceMembership> findByPersonIdAndStatus(UUID personId, String status);

    List<SpaceMembership> findBySpaceIdAndStatus(UUID spaceId, String status);

    Optional<SpaceMembership> findByPersonIdAndSpaceIdAndLeftAtIsNull(UUID personId, UUID spaceId);

    Page<SpaceMembership> findByTenantIdAndSpaceIdOrderByJoinedAtDesc(UUID tenantId, UUID spaceId, Pageable pageable);

    @Query("SELECT sm FROM SpaceMembership sm WHERE sm.tenantId = :tenantId AND sm.personId = :personId AND sm.leftAt IS NULL")
    List<SpaceMembership> findActiveByPersonId(@Param("tenantId") UUID tenantId, @Param("personId") UUID personId);

    long countBySpaceIdAndStatus(UUID spaceId, String status);
}