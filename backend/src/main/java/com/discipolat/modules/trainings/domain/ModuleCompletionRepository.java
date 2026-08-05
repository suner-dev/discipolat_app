package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModuleCompletionRepository extends JpaRepository<ModuleCompletion, UUID> {
    long countByEnrollmentId(UUID enrollmentId);
    Optional<ModuleCompletion> findByEnrollmentIdAndModuleId(UUID enrollmentId, UUID moduleId);
}
