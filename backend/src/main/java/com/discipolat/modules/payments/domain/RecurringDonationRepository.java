package com.discipolat.modules.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringDonationRepository extends JpaRepository<RecurringDonation, UUID> {

    List<RecurringDonation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT r FROM RecurringDonation r WHERE r.active = true AND r.nextDonationDate <= :date")
    List<RecurringDonation> findDueDonations(@Param("date") LocalDate date);

    List<RecurringDonation> findByTenantIdAndActiveTrue(UUID tenantId);

    long countByTenantIdAndActiveTrue(UUID tenantId);

    @Query("SELECT r.frequency, COALESCE(SUM(r.amount), 0), COUNT(r) FROM RecurringDonation r " +
           "WHERE r.active = true AND r.tenantId = :tenantId GROUP BY r.frequency ORDER BY SUM(r.amount) DESC")
    List<Object[]> sumActiveByFrequency(@Param("tenantId") UUID tenantId);

    @Query("SELECT r.operator, COALESCE(SUM(r.amount), 0), COUNT(r) FROM RecurringDonation r " +
           "WHERE r.active = true AND r.tenantId = :tenantId GROUP BY r.operator ORDER BY SUM(r.amount) DESC")
    List<Object[]> sumActiveByOperator(@Param("tenantId") UUID tenantId);

    @Query("SELECT COALESCE(SUM(r.amount), 0), COALESCE(AVG(r.amount), 0), COALESCE(SUM(r.donationCount), 0), COALESCE(SUM(r.totalDonated), 0) FROM RecurringDonation r " +
           "WHERE r.active = true AND r.tenantId = :tenantId")
    Object[] recurringSummary(@Param("tenantId") UUID tenantId);
}
