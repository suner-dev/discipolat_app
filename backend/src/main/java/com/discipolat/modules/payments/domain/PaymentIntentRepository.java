package com.discipolat.modules.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {

    List<PaymentIntent> findTop50ByOrderByCreatedAtDesc();

    List<PaymentIntent> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);

    List<PaymentIntent> findByStatusOrderByCreatedAtDesc(PaymentIntent.Status status);

    /** Intentions toujours en attente, créées avant une date donnée (pour la confirmation automatique simulée). */
    @Query("SELECT p FROM PaymentIntent p WHERE p.status = 'PENDING' AND p.createdAt <= :before ORDER BY p.createdAt ASC")
    List<PaymentIntent> findPendingOlderThan(@Param("before") java.time.LocalDateTime before);

    Optional<PaymentIntent> findByProviderReference(String providerReference);

    long countByStatus(PaymentIntent.Status status);

    @Query("SELECT p.operator, COALESCE(SUM(p.amount), 0), COUNT(p) FROM PaymentIntent p " +
           "WHERE p.status = 'CONFIRMED' GROUP BY p.operator ORDER BY SUM(p.amount) DESC")
    List<Object[]> sumConfirmedByOperator();

    @Query("SELECT FUNCTION('to_char', p.createdAt, 'YYYY-MM'), COALESCE(SUM(p.amount), 0) FROM PaymentIntent p " +
           "WHERE p.status = 'CONFIRMED' GROUP BY FUNCTION('to_char', p.createdAt, 'YYYY-MM') ORDER BY 1 DESC")
    List<Object[]> monthlyTrend();

    @Query("SELECT p.purpose, COALESCE(SUM(p.amount), 0), COUNT(p) FROM PaymentIntent p " +
           "WHERE p.status = 'CONFIRMED' GROUP BY p.purpose ORDER BY SUM(p.amount) DESC")
    List<Object[]> sumConfirmedByPurpose();

    @Query("SELECT COALESCE(AVG(p.amount), 0), COALESCE(MAX(p.amount), 0), COALESCE(MIN(p.amount), 0) " +
           "FROM PaymentIntent p WHERE p.status = 'CONFIRMED'")
    Object[] amountStats();

    @Query("SELECT FUNCTION('to_char', p.createdAt, 'YYYY-MM-DD'), COALESCE(SUM(p.amount), 0), COUNT(p) " +
           "FROM PaymentIntent p WHERE p.status = 'CONFIRMED' " +
           "AND p.createdAt >= :since GROUP BY FUNCTION('to_char', p.createdAt, 'YYYY-MM-DD') ORDER BY 1 DESC")
    List<Object[]> dailyTrend(@Param("since") java.time.LocalDateTime since);

    @Query("SELECT p.providerName, COALESCE(SUM(p.amount), 0), COUNT(p) FROM PaymentIntent p " +
           "WHERE p.status = 'CONFIRMED' AND p.providerName IS NOT NULL " +
           "GROUP BY p.providerName ORDER BY SUM(p.amount) DESC")
    List<Object[]> confirmedByProvider();
}
