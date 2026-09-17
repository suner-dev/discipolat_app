package com.discipolat.modules.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, UUID> {

    List<WorkflowTransition> findByWorkflowId(UUID workflowId);

    List<WorkflowTransition> findByFromStepId(UUID fromStepId);

    Optional<WorkflowTransition> findFirstByFromStepIdAndOnEvent(UUID fromStepId, String onEvent);
}
