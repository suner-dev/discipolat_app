package com.discipolat.modules.members.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MemberCompetenceRepository extends JpaRepository<MemberCompetence, UUID> {

    List<MemberCompetence> findByUserIdOrderByCompetenceNameAsc(UUID userId);

    List<MemberCompetence> findByCategoryOrderByLevelDesc(String category);

    List<MemberCompetence> findByCompetenceNameContainingIgnoreCaseOrderByLevelDesc(String name);

    long countByUserId(UUID userId);
}
