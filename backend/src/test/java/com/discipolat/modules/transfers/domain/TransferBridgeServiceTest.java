package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.PrioriteTransfert;
import com.discipolat.common.enums.TransferStatus;
import com.discipolat.common.enums.TransferType;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.transfers.api.CreateTransferRequest;
import com.discipolat.modules.transfers.api.TransferResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests du PONT : les anciens transferts directs (transferFaiseur, reassignChef,
 * reassign d'âme) doivent créer ET soumettre une demande de workflow avec le bon
 * type, la bonne personne et les bonnes règles d'exécution — tout en conservant
 * le scoping par espace métier (anti-IDOR) des anciennes méthodes directes.
 */
@ExtendWith(MockitoExtension.class)
class TransferBridgeServiceTest {

    @Mock private TransferWorkflowService workflowService;
    @Mock private WorkspaceScopeService workspaceScopeService;

    private TransferBridgeService bridge;

    @BeforeEach
    void setUp() {
        bridge = new TransferBridgeService(workflowService, workspaceScopeService);
        lenient().when(workspaceScopeService.isSuperUser()).thenReturn(true);
        lenient().when(workflowService.create(any(CreateTransferRequest.class)))
                .thenAnswer(inv -> {
                    CreateTransferRequest req = inv.getArgument(0);
                    return TransferRequest.builder()
                            .id(UUID.randomUUID())
                            .type(req.type())
                            .statut(TransferStatus.BROUILLON)
                            .personneId(req.personneId())
                            .personneType(req.personneType())
                            .nouvelleAffectation(req.nouvelleAffectation())
                            .justification(req.justification())
                            .reglesExecution(req.reglesExecution())
                            .build();
                });
        lenient().when(workflowService.submit(any(UUID.class)))
                .thenAnswer(inv -> TransferRequest.builder()
                        .id(inv.getArgument(0))
                        .type(TransferType.FAISEUR_FAMILLE_TRANSFERT)
                        .statut(TransferStatus.EXECUTE)
                        .personneId(UUID.randomUUID())
                        .personneType("USER")
                        .build());
        lenient().when(workflowService.toResponse(any(TransferRequest.class)))
                .thenAnswer(inv -> {
                    TransferRequest r = inv.getArgument(0);
                    return new TransferResponse(
                            r.getId(), r.getType(), r.getStatut(), r.getPersonneId(),
                            r.getPersonneType(), "Nom", null, null, r.getDemandeurId(),
                            "Demandeur", r.getJustification(),
                            PrioriteTransfert.MOYENNE,
                            null, null, null, null, 0, 0, 0, null);
                });
    }

    @Test
    void transferFaiseur_ShouldCreateSoumettreDemandeFaiseurFamille() {
        UUID faiseurId = UUID.randomUUID();
        UUID nouvelleFamilleId = UUID.randomUUID();

        bridge.transferFaiseur(faiseurId, nouvelleFamilleId, true);

        ArgumentCaptor<CreateTransferRequest> captor = ArgumentCaptor.forClass(CreateTransferRequest.class);
        verify(workflowService).create(captor.capture());
        CreateTransferRequest req = captor.getValue();

        assertEquals(TransferType.FAISEUR_FAMILLE_TRANSFERT, req.type());
        assertEquals(faiseurId, req.personneId());
        assertEquals("USER", req.personneType());
        assertEquals(nouvelleFamilleId, req.nouvelleAffectation().get("id"));
        assertEquals(Boolean.TRUE, req.reglesExecution().get("transfererAmes"));
        verify(workflowService).submit(any(UUID.class));
    }

    @Test
    void transferFaiseur_TransfererAmesFalse_ShouldPasserFalse() {
        UUID faiseurId = UUID.randomUUID();
        UUID nouvelleFamilleId = UUID.randomUUID();

        bridge.transferFaiseur(faiseurId, nouvelleFamilleId, false);

        ArgumentCaptor<CreateTransferRequest> captor = ArgumentCaptor.forClass(CreateTransferRequest.class);
        verify(workflowService).create(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().reglesExecution().get("transfererAmes"));
    }

    @Test
    void reassignChef_ShouldCreateDemandeChefFamille() {
        UUID familyId = UUID.randomUUID();
        UUID newChefId = UUID.randomUUID();

        bridge.reassignChef(familyId, newChefId);

        ArgumentCaptor<CreateTransferRequest> captor = ArgumentCaptor.forClass(CreateTransferRequest.class);
        verify(workflowService).create(captor.capture());
        CreateTransferRequest req = captor.getValue();

        assertEquals(TransferType.CHEF_FAMILLE_TRANSFERT, req.type());
        assertEquals(newChefId, req.personneId());
        assertEquals("USER", req.personneType());
        assertEquals(familyId, req.nouvelleAffectation().get("id"));
        assertNull(req.reglesExecution());
        verify(workflowService).submit(any(UUID.class));
    }

    @Test
    void reassignSoul_ShouldCreateDemandeChangementFaiseur() {
        UUID soulId = UUID.randomUUID();
        UUID newFaiseurId = UUID.randomUUID();

        bridge.reassignSoul(soulId, newFaiseurId);

        ArgumentCaptor<CreateTransferRequest> captor = ArgumentCaptor.forClass(CreateTransferRequest.class);
        verify(workflowService).create(captor.capture());
        CreateTransferRequest req = captor.getValue();

        assertEquals(TransferType.FAISEUR_DISCIPLE_CHANGEMENT, req.type());
        assertEquals(soulId, req.personneId());
        assertEquals("SOUL", req.personneType());
        assertEquals(newFaiseurId, req.nouvelleAffectation().get("id"));
        verify(workflowService).submit(any(UUID.class));
    }

    @Test
    void chaqueAppel_ShouldSoumettreExactementUneFois() {
        bridge.transferFaiseur(UUID.randomUUID(), UUID.randomUUID(), false);
        bridge.reassignChef(UUID.randomUUID(), UUID.randomUUID());
        bridge.reassignSoul(UUID.randomUUID(), UUID.randomUUID());

        verify(workflowService, times(3)).create(any(CreateTransferRequest.class));
        verify(workflowService, times(3)).submit(any(UUID.class));
    }

    // ========================================================================
    // ANTI-IDOR : scoping par espace métier (rôle actif)
    // ========================================================================

    @Test
    void reassignChef_HorsEspace_ShouldThrowAccessDenied() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessFamily(any(UUID.class))).thenReturn(false);
        UUID familyId = UUID.randomUUID();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> bridge.reassignChef(familyId, UUID.randomUUID()));
        verify(workflowService, never()).create(any(CreateTransferRequest.class));
    }

    @Test
    void reassignChef_DansEspace_ShouldPasser() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessFamily(any(UUID.class))).thenReturn(true);
        UUID familyId = UUID.randomUUID();

        bridge.reassignChef(familyId, UUID.randomUUID());

        verify(workflowService).create(any(CreateTransferRequest.class));
        verify(workflowService).submit(any(UUID.class));
    }

    @Test
    void reassignSoul_AmeHorsEspace_ShouldThrowAccessDenied() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessSoul(any(UUID.class))).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> bridge.reassignSoul(UUID.randomUUID(), UUID.randomUUID()));
        verify(workflowService, never()).create(any(CreateTransferRequest.class));
    }

    @Test
    void reassignSoul_FaiseurCibleHorsEspace_ShouldThrowAccessDenied() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessSoul(any(UUID.class))).thenReturn(true);
        when(workspaceScopeService.canAccessFaiseur(any(UUID.class))).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> bridge.reassignSoul(UUID.randomUUID(), UUID.randomUUID()));
        verify(workflowService, never()).create(any(CreateTransferRequest.class));
    }

    @Test
    void reassignSoul_DansEspace_ShouldPasser() {
        when(workspaceScopeService.isSuperUser()).thenReturn(false);
        when(workspaceScopeService.canAccessSoul(any(UUID.class))).thenReturn(true);
        when(workspaceScopeService.canAccessFaiseur(any(UUID.class))).thenReturn(true);

        bridge.reassignSoul(UUID.randomUUID(), UUID.randomUUID());

        verify(workflowService).create(any(CreateTransferRequest.class));
        verify(workflowService).submit(any(UUID.class));
    }
}
