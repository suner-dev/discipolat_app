package com.discipolat.modules.payments.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, UUID> {

    List<PaymentIntent> findTop50ByOrderByCreatedAtDesc();

    List<PaymentIntent> findByStatusOrderByCreatedAtDesc(PaymentIntent.Status status);

    Optional<PaymentIntent> findByProviderReference(String providerReference);

    long countByStatus(PaymentIntent.Status status);

    @Query("SELECT p.operator, COALESCE(SUM(p.amount), 0), COUNT(p) FROM PaymentIntent p " +
           "WHERE p.status = 'CONFIRMED' GROUP BY p.operator ORDER BY SUM(p.amount) DESC")
    List<Object[]> sumConfirmedByOperator();

    @Query("SELECT FUNCTION('to_char', p.createdAt, 'YYYY-MM'), COALESCE(SUM(p.amount), 0) FROM PaymentIntent p " +
           "WHERE p.status = 'CONFIRMED' GROUP BY FUNCTION('to_char', p.createdAt, 'YYYY-MM') ORDER BY 1 DESC")
    List<Object[]> monthlyTrend();
}
