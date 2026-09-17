package com.discipolat.modules.config.repository;

import com.discipolat.modules.config.domain.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, UUID> {

    List<WorkflowStep> findByWorkflowIdOrderByStepOrderAsc(UUID workflowId);
}