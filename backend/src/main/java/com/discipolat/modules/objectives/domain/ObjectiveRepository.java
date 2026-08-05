package com.discipolat.modules.objectives.domain;

import com.discipolat.common.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ObjectiveRepository extends JpaRepository<Objective, UUID> {
    List<Objective> findByActifTrueOrderByRoleAscTypeAsc();
    List<Objective> findByRoleAndActifTrue(UserRole role);
    List<Objective> findByActifTrue();
}
