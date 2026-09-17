package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.WorkflowTransition;
import com.discipolat.modules.config.domain.WorkflowTransitionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, WorkflowTransitionId> {

    List<WorkflowTransition> findByFromStepId(UUID fromStepId);

    List<WorkflowTransition> findByWorkflowId(UUID workflowId);
}