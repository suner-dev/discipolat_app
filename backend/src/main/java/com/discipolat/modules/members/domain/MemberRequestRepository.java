package com.discipolat.modules.members.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberRequestRepository extends JpaRepository<MemberRequest, UUID> {

    /** Demandes envoyées par le membre connecté. */
    List<MemberRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Boîte de réception du pasteur / admin : toutes les demandes. */
    List<MemberRequest> findAllByOrderByCreatedAtDesc();

    /** Boîte de réception du responsable : demandes adressées aux responsables de ses départements. */
    List<MemberRequest> findByCibleAndDepartmentIdInOrderByCreatedAtDesc(
            MemberRequest.Cible cible, List<UUID> departmentIds);

    /** Boîte de réception du chef de famille : demandes adressées aux chefs de ses familles. */
    List<MemberRequest> findByCibleAndFamilyIdInOrderByCreatedAtDesc(
            MemberRequest.Cible cible, List<UUID> familyIds);
}
