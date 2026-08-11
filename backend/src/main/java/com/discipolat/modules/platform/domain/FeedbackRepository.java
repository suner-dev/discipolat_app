package com.discipolat.modules.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    List<Feedback> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);

    @Query("SELECT f.category AS category, COUNT(f) AS count FROM Feedback f GROUP BY f.category ORDER BY f.category")
    List<Map<String, Object>> countByCategory();

    @Query("SELECT f.status AS status, COUNT(f) AS count FROM Feedback f GROUP BY f.status")
    List<Map<String, Object>> countByStatusGrouped();
}
