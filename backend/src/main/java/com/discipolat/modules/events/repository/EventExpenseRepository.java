package com.discipolat.modules.events.repository;

import com.discipolat.modules.events.domain.EventExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventExpenseRepository extends JpaRepository<EventExpense, UUID> {

    List<EventExpense> findByChurchEventId(UUID churchEventId);

    List<EventExpense> findByExpenseId(UUID expenseId);
}