package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.PrioriteTransfert;
import com.discipolat.common.enums.TransferType;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.transfers.api.CreateTransferRequest;
import com.discipolat.modules.transfers.api.TransferResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * PONT D'INTÉGRATION : fait passer les anciens transferts directs
 * (transferFaiseur, reassignChef, reassign d'âme) par le MOTEUR DE WORKFLOW
 * configurable. Plus aucune mutation directe : chaque opération devient une
 * demande de transfert soumise au circuit de validation paramétré par le
 * pasteur. Si le circuit est vide, l'exécution est automatique et immédiate ;
 * sinon, la demande attend les validations puis s'exécute automatiquement.
 *
 * SCALPING : le pont réapplique le scoping par rôle ACTIF (anti-IDOR) que les
 * anciennes méthodes directes assuraient via leurs findById scopés — un rôle
 * opérationnel ne peut pas cibler une âme, une famille ou un faiseur hors de
 * son espace métier, même s'il a le droit d'initier ce type de transfert.
 */
@Service
public class TransferBridgeService {

    private final TransferWorkflowService workflowService;
    private final WorkspaceScopeService workspaceScopeService;

    public TransferBridgeService(TransferWorkflowService workflowService,
                                 WorkspaceScopeService workspaceScopeService) {
        this.workflowService = workflowService;
        this.workspaceScopeService = workspaceScopeService;
    }

    /** US-13 : transfert d'un faiseur vers une autre famille (avec ou sans ses âmes). */
    public TransferResponse transferFaiseur(UUID faiseurId, UUID nouvelleFamilleId, boolean transfererAmes) {
        return createAndSubmit(new CreateTransferRequest(
                TransferType.FAISEUR_FAMILLE_TRANSFERT,
                faiseurId, "USER", null,
                Map.of("type", "FAMILLE", "id", nouvelleFamilleId),
                "Transfert de faiseur de disciples vers une autre famille",
                PrioriteTransfert.MOYENNE, null, null,
                Map.of("transfererAmes", transfererAmes)));
    }

    /** US-07 : changement du chef d'une famille de disciples. */
    public TransferResponse reassignChef(UUID familyId, UUID newChefId) {
        // Anti-IDOR : un rôle opérationnel ne peut changer le chef que d'une
        // famille de SON espace métier (l'ancien findById scopé le garantissait).
        if (!workspaceScopeService.isSuperUser() && !workspaceScopeService.canAccessFamily(familyId)) {
            throw new AccessDeniedException("Accès refusé : cette famille n'appartient pas à votre espace métier");
        }
        return createAndSubmit(new CreateTransferRequest(
                TransferType.CHEF_FAMILLE_TRANSFERT,
                newChefId, "USER", null,
                Map.of("type", "FAMILLE", "id", familyId),
                "Changement du chef de famille",
                PrioriteTransfert.MOYENNE, null, null, null));
    }

    /** US-21 : réaffectation du suivi d'une âme à un autre faiseur. */
    public TransferResponse reassignSoul(UUID soulId, UUID newFaiseurId) {
        // Anti-IDOR : un rôle opérationnel ne peut réaffecter qu'une âme de SON
        // espace, vers un faiseur de SON espace (l'ancien assertAccessible le
        // garantissait pour l'âme ; le faiseur cible est contrôlé également).
        if (!workspaceScopeService.isSuperUser()
                && (!workspaceScopeService.canAccessSoul(soulId)
                || !workspaceScopeService.canAccessFaiseur(newFaiseurId))) {
            throw new AccessDeniedException(
                    "Accès refusé : cette âme ou ce faiseur n'appartient pas à votre espace métier");
        }
        return createAndSubmit(new CreateTransferRequest(
                TransferType.FAISEUR_DISCIPLE_CHANGEMENT,
                soulId, "SOUL", null,
                Map.of("type", "FAISEUR", "id", newFaiseurId),
                "Réaffectation du suivi d'un disciple à un autre faiseur",
                PrioriteTransfert.MOYENNE, null, null, null));
    }

    /** Crée puis soumet la demande ; le moteur décide (validation ou exécution immédiate). */
    private TransferResponse createAndSubmit(CreateTransferRequest request) {
        TransferRequest created = workflowService.create(request);
        return workflowService.toResponse(workflowService.submit(created.getId()));
    }
}
