package com.discipolat.modules.trainings.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingCertificateRepository extends JpaRepository<TrainingCertificate, UUID> {
    List<TrainingCertificate> findByUserIdOrderByDelivreLeDesc(UUID userId);
    Optional<TrainingCertificate> findByEnrollmentId(UUID enrollmentId);
}
