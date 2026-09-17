package com.discipolat.modules.workflow.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * G2.5 — CRUD des définitions de workflow (ce que l'éditeur visuel manipule).
 *
 * Un workflow est créé/modifié par configuration : aucun processus métier
 * n'est codé en dur (§0.3 n°1).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowTransitionRepository transitionRepository;
    private final AuditService auditService;

    // ==================== Définitions ====================

    @Transactional(readOnly = true)
    public List<WorkflowDefinition> list(UUID tenantId) {
        requireTenant(tenantId);
        return definitionRepository.findByTenantIdAndDeletedAtIsNull(tenantId);
    }

    @Transactional(readOnly = true)
    public WorkflowDefinition get(UUID tenantId, UUID workflowId) {
        requireTenant(tenantId);
        WorkflowDefinition definition = definitionRepository.findByIdAndTenantId(workflowId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("WorkflowDefinition", workflowId));
        if (definition.isDeleted()) {
            throw new EntityNotFoundException("WorkflowDefinition", workflowId);
        }
        return definition;
    }

    public WorkflowDefinition create(UUID tenantId, DefinitionCommand command) {
        requireTenant(tenantId);
        if (command.code() == null || command.code().isBlank()) {
            throw new IllegalArgumentException("code est obligatoire");
        }
        if (command.entityType() == null || command.entityType().isBlank()) {
            throw new IllegalArgumentException("entityType est obligatoire");
        }
        WorkflowDefinition definition = WorkflowDefinition.builder()
                .tenantId(tenantId)
                .entityType(command.entityType())
                .code(command.code())
                .name(command.name() != null ? command.name() : command.code())
                .spaceId(command.spaceId())
                .enabled(command.enabled() == null || command.enabled())
                .version(1)
                .build();
        WorkflowDefinition saved = definitionRepository.save(definition);
        auditService.logSimple("WORKFLOW_DEFINITION_CREATED", "WORKFLOW_DEFINITION", saved.getId());
        return saved;
    }

    /** Met à jour la définition et incrémente sa version (historisation). */
    public WorkflowDefinition update(UUID tenantId, UUID workflowId, DefinitionCommand command) {
        WorkflowDefinition definition = get(tenantId, workflowId);
        if (command.name() != null) definition.setName(command.name());
        if (command.enabled() != null) definition.setEnabled(command.enabled());
        if (command.entityType() != null) definition.setEntityType(command.entityType());
        definition.setVersion(definition.getVersion() + 1);
        WorkflowDefinition saved = definitionRepository.save(definition);
        auditService.logSimple("WORKFLOW_DEFINITION_UPDATED", "WORKFLOW_DEFINITION", saved.getId());
        return saved;
    }

    public void delete(UUID tenantId, UUID workflowId) {
        WorkflowDefinition definition = get(tenantId, workflowId);
        definition.setDeletedAt(Instant.now());
        definition.setEnabled(false);
        definitionRepository.save(definition);
        auditService.logSimple("WORKFLOW_DEFINITION_DELETED", "WORKFLOW_DEFINITION", workflowId);
    }

    private void requireTenant(UUID tenantId) {
        if (tenantId == null) throw new SecurityException("Aucun tenant dans le contexte");
    }

    public record DefinitionCommand(
            String entityType,
            String code,
            String name,
            UUID spaceId,
            Boolean enabled
    ) {}
}