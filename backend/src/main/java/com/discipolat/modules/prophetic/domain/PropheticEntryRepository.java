package com.discipolat.modules.prophetic.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropheticEntryRepository extends JpaRepository<PropheticEntry, UUID> {

    List<PropheticEntry> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    List<PropheticEntry> findByTypeOrderByCreatedAtDesc(PropheticEntry.EntryType type);

    List<PropheticEntry> findByRelatedSoulIdOrderByCreatedAtDesc(UUID soulId);

    List<PropheticEntry> findByRelatedFamilyIdOrderByCreatedAtDesc(UUID familyId);

    List<PropheticEntry> findByRelatedDepartmentIdOrderByCreatedAtDesc(UUID departmentId);

    List<PropheticEntry> findByIsPublicTrueOrderByCreatedAtDesc();

    List<PropheticEntry> findByTagsContainingIgnoreCaseOrderByCreatedAtDesc(String tag);
}
