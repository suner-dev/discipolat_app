package com.discipolat.modules.people.repository;

import com.discipolat.modules.people.domain.Membership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByTenantId(UUID tenantId);

    Optional<Membership> findByPersonId(UUID personId);

    List<Membership> findByTenantIdAndMembershipStatus(UUID tenantId, String status);

    long countByTenantId(UUID tenantId);
}