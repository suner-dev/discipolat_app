package com.discipolat.modules.workflow.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WorkflowAutomationService {

    private final AutomationRepository repository;

    public WorkflowAutomationService(AutomationRepository repository) {
        this.repository = repository;
    }

    public List<Automation> list() {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

    public Automation getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("Automation", id));
    }

    public Automation create(String nom, String description, String triggerType, String triggerConfig,
                             String actionType, String actionConfig, UUID userId) {
        Automation a = new Automation();
        a.setTenantId(TenantContext.getCurrentTenantId());
        a.setNom(nom);
        a.setDescription(description);
        a.setTriggerType(Automation.TriggerType.valueOf(triggerType));
        a.setTriggerConfig(triggerConfig);
        a.setActionType(Automation.ActionType.valueOf(actionType));
        a.setActionConfig(actionConfig);
        a.setCreePar(userId);
        return repository.save(a);
    }

    public Automation toggleStatut(UUID id) {
        Automation a = getById(id);
        a.setStatut(a.getStatut() == Automation.Statut.ACTIVE ? Automation.Statut.PAUSEE : Automation.Statut.ACTIVE);
        return repository.save(a);
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
