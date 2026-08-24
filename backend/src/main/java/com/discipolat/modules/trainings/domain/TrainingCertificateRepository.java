package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingCertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findByUserIdOrderByDelivreLeDesc(UUID userId);
    Optional<Certificate> findByEnrollmentId(UUID enrollmentId);
}
