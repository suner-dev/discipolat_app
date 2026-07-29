package com.discipolat.modules.souls.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SoulRetractionRequestRepository extends JpaRepository<SoulRetractionRequest, UUID> {
    List<SoulRetractionRequest> findByAmeIdOrderByCreatedAtDesc(UUID ameId);
    Page<SoulRetractionRequest> findByDemandeurIdOrderByCreatedAtDesc(UUID demandeurId, Pageable pageable);
    Page<SoulRetractionRequest> findByStatutOrderByCreatedAtDesc(String statut, Pageable pageable);
    long countByStatut(String statut);
}
