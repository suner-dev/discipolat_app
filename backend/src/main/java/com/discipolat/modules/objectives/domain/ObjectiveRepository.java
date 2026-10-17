package com.discipolat.modules.objectives.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObjectiveRepository extends JpaRepository<Objective, UUID> {
    List<Objective> findByActifTrueOrderByRoleAscTypeAsc();
    List<Objective> findByRoleAndActifTrue(String role);
    List<Objective> findByActifTrue();
}
