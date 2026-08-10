package com.discipolat.modules.transfers.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransferWorkflowStepRepository extends JpaRepository<TransferWorkflowStep, UUID> {

    List<TransferWorkflowStep> findByWorkflowConfigIdOrderByEtapeOrdreAsc(UUID workflowConfigId);

    long countByWorkflowConfigId(UUID workflowConfigId);

    void deleteByWorkflowConfigId(UUID workflowConfigId);
}
