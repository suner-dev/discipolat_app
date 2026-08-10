package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.TransferType;
import com.discipolat.common.enums.ValidationMode;
import com.discipolat.modules.transfers.domain.TransferWorkflowConfig;
import com.discipolat.modules.transfers.domain.TransferWorkflowStep;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Vue complète d'une configuration de workflow (administration). */
public record WorkflowConfigResponse(
        UUID id,
        TransferType transferType,
        String label,
        String description,
        boolean actif,
        List<String> rolesInitiateurs,
        ValidationMode modeValidation,
        Integer nombreValidationsRequises,
        Integer delaiTraitementHeures,
        boolean notificationsAuto,
        String modeleMessageDemande,
        String modeleMessageValidation,
        String modeleMessageRefus,
        String modeleMessageExecution,
        Map<String, Object> reglesExecution,
        List<StepResponse> steps
) {
    public record StepResponse(
            UUID id,
            Integer etapeOrdre,
            List<String> rolesValidateurs,
            String label,
            String description,
            boolean requis
    ) {}

    public static WorkflowConfigResponse from(TransferWorkflowConfig c, List<TransferWorkflowStep> loadedSteps) {
        List<StepResponse> steps = loadedSteps == null ? List.of()
                : loadedSteps.stream()
                        .sorted(java.util.Comparator.comparing(TransferWorkflowStep::getEtapeOrdre))
                        .map(s -> new StepResponse(s.getId(), s.getEtapeOrdre(), s.getRolesValidateurs(),
                                s.getLabel(), s.getDescription(), s.isRequis()))
                        .toList();
        return new WorkflowConfigResponse(c.getId(), c.getTransferType(), c.getLabel(), c.getDescription(),
                c.isActif(), c.getRolesInitiateurs(), c.getModeValidation(), c.getNombreValidationsRequises(),
                c.getDelaiTraitementHeures(), c.isNotificationsAuto(), c.getModeleMessageDemande(),
                c.getModeleMessageValidation(), c.getModeleMessageRefus(), c.getModeleMessageExecution(),
                c.getReglesExecution(), steps);
    }
}
