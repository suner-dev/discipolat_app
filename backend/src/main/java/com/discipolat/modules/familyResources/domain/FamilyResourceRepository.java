package com.discipolat.modules.familyResources.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyResourceRepository extends JpaRepository<FamilyResource, UUID> {
    List<FamilyResource> findByFamilleIdOrderByCreatedAtDesc(UUID familleId);

    Page<FamilyResource> findByFamilleIdOrderByCreatedAtDesc(UUID familleId, Pageable pageable);
}
