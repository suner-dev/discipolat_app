package com.discipolat.modules.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringDonationRepository extends JpaRepository<RecurringDonation, UUID> {

    List<RecurringDonation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<RecurringDonation> findByActiveTrueAndNextDonationDateLessThanOrEqual(LocalDate date);

    List<RecurringDonation> findByTenantIdAndActiveTrue(UUID tenantId);

    long countByTenantIdAndActiveTrue(UUID tenantId);
}
