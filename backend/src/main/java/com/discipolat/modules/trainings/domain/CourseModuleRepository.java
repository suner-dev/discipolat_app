package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseModuleRepository extends JpaRepository<CourseModule, UUID> {
    List<CourseModule> findByCourseIdOrderByOrdreAsc(UUID courseId);
    long countByCourseId(UUID courseId);
}
