package com.discipolat.modules.transfers.api;

import com.discipolat.common.enums.TransferType;
import com.discipolat.common.enums.ValidationMode;

import java.util.List;
import java.util.Map;

/** Paramétrage d'un workflow de transfert par le pasteur (config + étapes). */
public record WorkflowConfigRequest(
        TransferType transferType,
        String label,
        String description,
        Boolean actif,
        List<String> rolesInitiateurs,
        ValidationMode modeValidation,
        Integer nombreValidationsRequises,
        Integer delaiTraitementHeures,
        Boolean notificationsAuto,
        String modeleMessageDemande,
        String modeleMessageValidation,
        String modeleMessageRefus,
        String modeleMessageExecution,
        Map<String, Object> reglesExecution,
        List<StepRequest> steps
) {
    public record StepRequest(
            Integer etapeOrdre,
            List<String> rolesValidateurs,
            String label,
            String description,
            Boolean requis
    ) {}
}
