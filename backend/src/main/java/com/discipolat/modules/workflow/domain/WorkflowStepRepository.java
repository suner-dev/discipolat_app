package com.discipolat.modules.workflow.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {

    List<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(UUID workflowId);

    Optional<WorkflowStep> findFirstByWorkflowIdOrderByStepOrderAsc(UUID workflowId);

    Optional<WorkflowStep> findFirstByWorkflowIdAndStepOrderGreaterThanOrderByStepOrderAsc(UUID workflowId, Integer stepOrder);
}
