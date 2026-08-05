package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, UUID> {
    Optional<CourseEnrollment> findByCourseIdAndUserId(UUID courseId, UUID userId);
    List<CourseEnrollment> findByUserIdOrderByDateInscriptionDesc(UUID userId);
    List<CourseEnrollment> findByCourseId(UUID courseId);
    long countByCourseId(UUID courseId);
}
