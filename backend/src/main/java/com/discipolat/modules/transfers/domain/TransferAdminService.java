package com.discipolat.modules.transfers.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.transfers.api.WorkflowConfigRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * PARAMÉTRAGE PAR LE PASTEUR (ou l'admin) du workflow de transfert :
 * types autorisés, rôles initiateurs, rôles validateurs, nombre de validations
 * requises, mode de validation, délais de traitement, notifications
 * automatiques, modèles de messages et règles d'exécution.
 * Le circuit évolue SANS modification de code.
 */
@Service
@Transactional
public class TransferAdminService {

    private final TransferWorkflowConfigRepository configRepository;
    private final TransferWorkflowStepRepository stepRepository;
    private final TransferRequestRepository requestRepository;

    public TransferAdminService(TransferWorkflowConfigRepository configRepository,
                                TransferWorkflowStepRepository stepRepository,
                                TransferRequestRepository requestRepository) {
        this.configRepository = configRepository;
        this.stepRepository = stepRepository;
        this.requestRepository = requestRepository;
    }

    public List<TransferWorkflowConfig> findAll() {
        return configRepository.findAllByOrderByTransferTypeAsc();
    }

    public TransferWorkflowConfig findById(UUID id) {
        return configRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TransferWorkflowConfig", id));
    }

    /** Crée une configuration avec ses étapes. */
    public TransferWorkflowConfig create(WorkflowConfigRequest request) {
        if (request.transferType() == null) {
            throw new BusinessRuleException("Le type de transfert est obligatoire");
        }
        if (configRepository.findByTransferType(request.transferType()).isPresent()) {
            throw new BusinessRuleException("Une configuration existe déjà pour ce type de transfert : "
                    + request.transferType());
        }
        TransferWorkflowConfig config = TransferWorkflowConfig.builder()
                .transferType(request.transferType())
                .label(request.label())
                .description(request.description())
                .actif(request.actif() == null || request.actif())
                .rolesInitiateurs(request.rolesInitiateurs() != null
                        ? new ArrayList<>(request.rolesInitiateurs()) : new ArrayList<>(List.of("PASTEUR")))
                .modeValidation(request.modeValidation())
                .nombreValidationsRequises(request.nombreValidationsRequises() != null
                        ? request.nombreValidationsRequises() : 1)
                .delaiTraitementHeures(request.delaiTraitementHeures() != null
                        ? request.delaiTraitementHeures() : 72)
                .notificationsAuto(request.notificationsAuto() == null || request.notificationsAuto())
                .modeleMessageDemande(request.modeleMessageDemande())
                .modeleMessageValidation(request.modeleMessageValidation())
                .modeleMessageRefus(request.modeleMessageRefus())
                .modeleMessageExecution(request.modeleMessageExecution())
                .reglesExecution(request.reglesExecution())
                .build();
        config = configRepository.save(config);
        saveSteps(config, request);
        return config;
    }

    /** Met à jour une configuration (les étapes sont remplacées). */
    public TransferWorkflowConfig update(UUID id, WorkflowConfigRequest request) {
        TransferWorkflowConfig config = findById(id);
        if (request.label() != null) config.setLabel(request.label());
        if (request.description() != null) config.setDescription(request.description());
        if (request.actif() != null) config.setActif(request.actif());
        if (request.rolesInitiateurs() != null) config.setRolesInitiateurs(new ArrayList<>(request.rolesInitiateurs()));
        if (request.modeValidation() != null) config.setModeValidation(request.modeValidation());
        if (request.nombreValidationsRequises() != null) config.setNombreValidationsRequises(request.nombreValidationsRequises());
        if (request.delaiTraitementHeures() != null) config.setDelaiTraitementHeures(request.delaiTraitementHeures());
        if (request.notificationsAuto() != null) config.setNotificationsAuto(request.notificationsAuto());
        if (request.modeleMessageDemande() != null) config.setModeleMessageDemande(request.modeleMessageDemande());
        if (request.modeleMessageValidation() != null) config.setModeleMessageValidation(request.modeleMessageValidation());
        if (request.modeleMessageRefus() != null) config.setModeleMessageRefus(request.modeleMessageRefus());
        if (request.modeleMessageExecution() != null) config.setModeleMessageExecution(request.modeleMessageExecution());
        if (request.reglesExecution() != null) config.setReglesExecution(request.reglesExecution());

        if (request.steps() != null) {
            stepRepository.deleteByWorkflowConfigId(id);
            saveSteps(config, request);
        }
        return configRepository.save(config);
    }

    /** Active / désactive une configuration. */
    public TransferWorkflowConfig toggle(UUID id, boolean actif) {
        TransferWorkflowConfig config = findById(id);
        config.setActif(actif);
        return configRepository.save(config);
    }

    /** Supprime une configuration (refusée si des demandes s'y réfèrent). */
    public void delete(UUID id) {
        TransferWorkflowConfig config = findById(id);
        long referenced = requestRepository.countByWorkflowConfigId(id);
        if (referenced > 0) {
            throw new BusinessRuleException(
                    "Impossible de supprimer : " + referenced + " demande(s) de transfert utilisent cette configuration. "
                            + "Désactivez-la à la place.");
        }
        stepRepository.deleteByWorkflowConfigId(id);
        configRepository.delete(config);
    }

    private void saveSteps(TransferWorkflowConfig config, WorkflowConfigRequest request) {
        if (request.steps() == null || request.steps().isEmpty()) return;
        List<TransferWorkflowStep> steps = new ArrayList<>();
        int ordre = 1;
        for (WorkflowConfigRequest.StepRequest s : request.steps()) {
            steps.add(TransferWorkflowStep.builder()
                    .workflowConfigId(config.getId())
                    .etapeOrdre(s.etapeOrdre() != null ? s.etapeOrdre() : ordre++)
                    .rolesValidateurs(s.rolesValidateurs() != null
                            ? new ArrayList<>(s.rolesValidateurs()) : new ArrayList<>(List.of("PASTEUR")))
                    .label(s.label() != null ? s.label() : "Validation étape " + ordre)
                    .description(s.description())
                    .requis(s.requis() == null || s.requis())
                    .build());
        }
        stepRepository.saveAll(steps);
    }
}
