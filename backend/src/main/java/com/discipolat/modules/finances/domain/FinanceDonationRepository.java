package com.discipolat.modules.finances.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FinanceDonationRepository extends JpaRepository<FinanceDonation, UUID> {

    List<FinanceDonation> findByTenantIdOrderByDonationDateDesc(UUID tenantId);
}
