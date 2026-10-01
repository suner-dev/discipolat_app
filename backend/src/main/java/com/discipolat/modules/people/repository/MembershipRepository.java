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

    /**
     * PORT Develop1 (§moteur de migration legacy) : une personne peut être membre
     * de plusieurs unités dans un même tenant ; le rollback d'une migration doit
     * supprimer TOUTES ses adhésions, d'où le retour en liste (la méthode
     * findByPersonId ci-dessus, en Optional, refuserait les doublons).
     */
    List<Membership> findByTenantIdAndPersonId(UUID tenantId, UUID personId);

    List<Membership> findByTenantIdAndMembershipStatus(UUID tenantId, String status);

    long countByTenantId(UUID tenantId);
}