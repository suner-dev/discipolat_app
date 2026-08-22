package com.discipolat.modules.surveys.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, UUID> {
    Page<Survey> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
    Page<Survey> findByTenantIdAndStatut(UUID tenantId, Survey.Statut statut, Pageable pageable);
}
