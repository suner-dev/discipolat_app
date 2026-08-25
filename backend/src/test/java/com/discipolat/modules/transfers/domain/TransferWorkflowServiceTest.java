package com.discipolat.modules.transfers.domain;

import com.discipolat.common.enums.*;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.api.CreateTransferRequest;
import com.discipolat.modules.transfers.api.DecideRequest;
import com.discipolat.modules.transfers.api.UpdateTransferRequest;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class TransferWorkflowServiceTest {

    @Mock private TransferRequestRepository requestRepository;
    @Mock private TransferDecisionRepository decisionRepository;
    @Mock private TransferHistoryRepository historyRepository;
    @Mock private TransferAttachmentRepository attachmentRepository;
    @Mock private TransferWorkflowConfigRepository configRepository;
    @Mock private TransferWorkflowStepRepository stepRepository;
    @Mock private TransferExecutor executor;
    @Mock private SoulRepository soulRepository;
    @Mock private UserRepository userRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private SoulDepartmentRepository soulDepartmentRepository;
    @Mock private FileEntityRepository fileEntityRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;

    private TransferWorkflowService service;

    private final UUID demandeurId = UUID.randomUUID();
    private final UUID personneId = UUID.randomUUID();
    private final UUID familleCible = UUID.randomUUID();

    private TransferWorkflowConfig config;
    private List<TransferWorkflowStep> steps;
    private List<TransferDecision> storedDecisions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        lenient().when(securityUtils.isSuperUser()).thenReturn(true);
        SecurityTestHelper.loginAs(demandeurId);
        lenient().when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        lenient().when(requestRepository.save(any(TransferRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(decisionRepository.save(any(TransferDecision.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findAll()).thenReturn(List.of());
        lenient().when(notificationService.create(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        service = new TransferWorkflowService(requestRepository, decisionRepository,
                historyRepository, attachmentRepository, configRepository, stepRepository,
                executor, soulRepository, userRepository, familyRepository,
                departmentRepository, soulDepartmentRepository, fileEntityRepository,
                notificationService, auditService, propagationPublisher, securityUtils);

        config = TransferWorkflowConfig.builder()
                .id(UUID.randomUUID())
                .transferType(TransferType.DISCIPLE_FAMILLE_TRANSFERT)
                .label("Transfert de disciple")
                .actif(true)
                .rolesInitiateurs(List.of("PASTEUR"))
                .modeValidation(ValidationMode.SEQUENTIEL)
                .nombreValidationsRequises(2)
                .delaiTraitementHeures(72)
                .notificationsAuto(false)
                .reglesExecution(Map.of())
                .build();

        steps = new ArrayList<>(List.of(
                TransferWorkflowStep.builder().id(UUID.randomUUID()).workflowConfigId(config.getId())
                        .etapeOrdre(1).rolesValidateurs(List.of("CHEF_DE_FAMILLE")).label("Chef").requis(true).build(),
                TransferWorkflowStep.builder().id(UUID.randomUUID()).workflowConfigId(config.getId())
                        .etapeOrdre(2).rolesValidateurs(List.of("PASTEUR")).label("Pasteur").requis(true).build()
        ));

        Soul soul = Soul.builder().id(personneId).nom("Dupont").prenom("Marie")
                .faiseurId(UUID.randomUUID()).familleId(UUID.randomUUID())
                .build();
        lenient().when(soulRepository.findById(personneId)).thenReturn(Optional.of(soul));
    }

    private void stubConfig(boolean withSteps) {
        lenient().when(configRepository.findByTransferType(TransferType.DISCIPLE_FAMILLE_TRANSFERT))
                .thenReturn(Optional.of(config));
        lenient().when(configRepository.findById(config.getId())).thenReturn(Optional.of(config));
        lenient().when(stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(config.getId()))
                .thenReturn(withSteps ? steps : List.of());
    }

    private TransferRequest newRequest(TransferStatus statut) {
        return TransferRequest.builder()
                .id(UUID.randomUUID())
                .type(TransferType.DISCIPLE_FAMILLE_TRANSFERT)
                .statut(statut)
                .personneId(personneId)
                .personneType("SOUL")
                .ancienneAffectation(Map.of("type", "FAMILLE", "id", UUID.randomUUID()))
                .nouvelleAffectation(Map.of("type", "FAMILLE", "id", familleCible))
                .demandeurId(demandeurId)
                .justification("Suivi pastoral")
                .workflowConfigId(config.getId())
                .etapeCourante(0)
                .approbationsObtenues(0)
                .build();
    }

    private void stubDecisions() {
        lenient().when(decisionRepository.findByTransferRequestIdOrderByCreatedAtDesc(any(UUID.class)))
                .thenAnswer(inv -> new ArrayList<>(storedDecisions));
    }

    // ========================================================================
    // CRÉATION
    // ========================================================================

    @Test
    void create_ShouldReturnBrouillonAndHistoriser() {
        stubConfig(true);
        when(familyRepository.findById(familleCible))
                .thenReturn(Optional.of(Family.builder().id(familleCible).nom("Famille B").build()));
        when(soulRepository.findById(personneId))
                .thenReturn(Optional.of(Soul.builder().id(personneId).nom("Dupont").prenom("Marie")
                        .faiseurId(UUID.randomUUID()).familleId(UUID.randomUUID()).build()));

        CreateTransferRequest request = new CreateTransferRequest(
                TransferType.DISCIPLE_FAMILLE_TRANSFERT, personneId, "SOUL", null,
                Map.of("type", "FAMILLE", "id", familleCible),
                "Transfert pour suivi", PrioriteTransfert.HAUTE, "Commentaire", null, null);

        TransferRequest result = service.create(request);

        assertEquals(TransferStatus.BROUILLON, result.getStatut());
        assertEquals(demandeurId, result.getDemandeurId());
        assertNotNull(result.getAncienneAffectation()); // calculée côté serveur
        verify(historyRepository).save(any(TransferHistory.class));
        verify(propagationPublisher).publishCreated(eq("TRANSFER_REQUEST"), any(), any(), anyString());
    }

    @Test
    void create_ByRoleNonAutorise_ShouldThrowAccessDenied() {
        stubConfig(true);
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserRole()).thenReturn("MEMBRE");

        CreateTransferRequest request = new CreateTransferRequest(
                TransferType.DISCIPLE_FAMILLE_TRANSFERT, personneId, "SOUL", null,
                Map.of("type", "FAMILLE", "id", familleCible),
                "Transfert pour suivi", null, null, null, null);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.create(request));
    }

    // ========================================================================
    // MISE À JOUR (brouillon)
    // ========================================================================

    @Test
    void update_Brouillon_AvecFichierIds_ShouldRemplacerPiecesJointes() {
        TransferRequest req = newRequest(TransferStatus.BROUILLON);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));
        UUID fichier1 = UUID.randomUUID();
        UUID fichier2 = UUID.randomUUID();
        when(fileEntityRepository.existsById(fichier1)).thenReturn(true);
        when(fileEntityRepository.existsById(fichier2)).thenReturn(true);

        TransferRequest result = service.update(req.getId(),
                new UpdateTransferRequest(null, null, null, null, List.of(fichier1, fichier2)));

        // Remplacement complet : suppression des anciennes liaisons puis relink.
        verify(attachmentRepository).deleteByTransferRequestId(req.getId());
        ArgumentCaptor<TransferAttachment> captor = ArgumentCaptor.forClass(TransferAttachment.class);
        verify(attachmentRepository, times(2)).save(captor.capture());
        assertEquals(List.of(fichier1, fichier2),
                captor.getAllValues().stream().map(TransferAttachment::getFileId).toList());
        assertNotNull(result);
        verify(historyRepository).save(any(TransferHistory.class));
    }

    @Test
    void update_NonBrouillon_ShouldThrowBusinessRule() {
        TransferRequest req = newRequest(TransferStatus.EN_ATTENTE_VALIDATION);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        assertThrows(com.discipolat.common.domain.BusinessRuleException.class,
                () -> service.update(req.getId(),
                        new UpdateTransferRequest(null, null, null, null, List.of(UUID.randomUUID()))));
        verify(attachmentRepository, never()).deleteByTransferRequestId(any());
    }

    // ========================================================================
    // SOUMISSION
    // ========================================================================

    @Test
    void submit_SansEtape_ShouldExecuterImmediatement() {
        stubConfig(false);
        TransferRequest req = newRequest(TransferStatus.BROUILLON);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        TransferRequest result = service.submit(req.getId());

        assertEquals(TransferStatus.EXECUTE, result.getStatut());
        assertNotNull(result.getDateExecution());
        verify(executor).execute(eq(req), any());
        verify(historyRepository, atLeast(2)).save(any(TransferHistory.class));
    }

    @Test
    void submit_SansEtape_AvecReglesDemande_ShouldFusionnerRegles() {
        stubConfig(false);
        TransferRequest req = newRequest(TransferStatus.BROUILLON);
        req.setReglesExecution(Map.of("transfererAmes", true));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        service.submit(req.getId());

        // La demande est sans circuit : les règles de la demande doivent être
        // transmises à l'exécuteur (la config est absente).
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> rulesCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);
        verify(executor).execute(eq(req), rulesCaptor.capture());
        assertEquals(Boolean.TRUE, rulesCaptor.getValue().get("transfererAmes"));
    }

    @Test
    void submit_AvecReglesDemande_ShouldEcraserCellulesDeLaConfig() {
        stubConfig(true);
        config.setReglesExecution(Map.of("transfererAmes", false, "notifierFaiseur", true));
        TransferRequest req = newRequest(TransferStatus.BROUILLON);
        req.setReglesExecution(Map.of("transfererAmes", true));
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        // Circuit vide pour forcer l'exécution immédiate (config active sans étapes)
        when(stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(config.getId()))
                .thenReturn(List.of());

        service.submit(req.getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> rulesCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);
        verify(executor).execute(eq(req), rulesCaptor.capture());
        // La demande écrase la config pour la clé qu'elle fournit…
        assertEquals(Boolean.TRUE, rulesCaptor.getValue().get("transfererAmes"));
        // …et la config reste la source pour les autres clés.
        assertEquals(Boolean.TRUE, rulesCaptor.getValue().get("notifierFaiseur"));
    }

    @Test
    void submit_AvecCircuit_ShouldPasserEnAttenteValidation() {
        stubConfig(true);
        TransferRequest req = newRequest(TransferStatus.BROUILLON);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        TransferRequest result = service.submit(req.getId());

        assertEquals(TransferStatus.EN_ATTENTE_VALIDATION, result.getStatut());
        assertEquals(0, result.getEtapeCourante());
        assertNotNull(result.getDateSoumission());
        assertNotNull(result.getDelaiLimite());
        verify(executor, never()).execute(any(), any());
    }

    // ========================================================================
    // DÉCISIONS — circuit séquentiel
    // ========================================================================

    @Test
    void decide_ApprobationEtape1_ShouldPasserValidationPartielle() {
        stubConfig(true);
        stubDecisions();
        TransferRequest req = newRequest(TransferStatus.EN_ATTENTE_VALIDATION);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        TransferRequest result = service.decide(req.getId(),
                new DecideRequest(DecisionType.APPROBATION, "OK"));

        assertEquals(TransferStatus.VALIDATION_PARTIELLE, result.getStatut());
        assertEquals(1, result.getEtapeCourante());
        verify(executor, never()).execute(any(), any());
    }

    @Test
    void decide_ApprobationFinale_ShouldValiderPuisExecuter() {
        stubConfig(true);
        TransferRequest req = newRequest(TransferStatus.VALIDATION_PARTIELLE);
        req.setEtapeCourante(1);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        // La 1re étape (ordre 1) a déjà été approuvée
        storedDecisions.add(TransferDecision.builder().id(UUID.randomUUID())
                .transferRequestId(req.getId()).validateurId(UUID.randomUUID())
                .decision(DecisionType.APPROBATION).etapeOrdre(1).build());
        stubDecisions();
        when(decisionRepository.findByTransferRequestIdOrderByCreatedAtDesc(req.getId()))
                .thenReturn(new ArrayList<>(storedDecisions));

        TransferRequest result = service.decide(req.getId(),
                new DecideRequest(DecisionType.APPROBATION, "Validé par le pasteur"));

        assertEquals(TransferStatus.EXECUTE, result.getStatut());
        assertNotNull(result.getDateExecution());
        verify(executor).execute(eq(req), any());
        verify(propagationPublisher, atLeast(1)).publishStatusChanged(eq("TRANSFER_REQUEST"), any(), anyString(), anyString(), anyString());
    }

    @Test
    void decide_Refus_ShouldPasserRefuse() {
        stubConfig(true);
        stubDecisions();
        TransferRequest req = newRequest(TransferStatus.EN_ATTENTE_VALIDATION);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        TransferRequest result = service.decide(req.getId(),
                new DecideRequest(DecisionType.REFUS, "Demande non justifiée"));

        assertEquals(TransferStatus.REFUSE, result.getStatut());
        verify(executor, never()).execute(any(), any());
        verify(notificationService, atLeastOnce()).create(eq(demandeurId), eq(TypeNotification.TRANSFERT_REFUSEE),
                any(), any(), any(), any(), any());
    }

    @Test
    void decide_RenvoiCorrection_ShouldRenvoyerSoumis() {
        stubConfig(true);
        stubDecisions();
        TransferRequest req = newRequest(TransferStatus.EN_ATTENTE_VALIDATION);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        TransferRequest result = service.decide(req.getId(),
                new DecideRequest(DecisionType.RENVOI_CORRECTION, "Famille cible incorrecte"));

        assertEquals(TransferStatus.SOUMIS, result.getStatut());
        // Une nouvelle soumission reprend à la même étape
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));
        TransferRequest resubmitted = service.submit(req.getId());
        assertEquals(TransferStatus.EN_ATTENTE_VALIDATION, resubmitted.getStatut());
    }

    @Test
    void decide_RoleNonValideur_ShouldThrowAccessDenied() {
        stubConfig(true);
        stubDecisions();
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserRole()).thenReturn("MEMBRE");
        TransferRequest req = newRequest(TransferStatus.EN_ATTENTE_VALIDATION);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.decide(req.getId(), new DecideRequest(DecisionType.APPROBATION, "OK")));
    }

    @Test
    void decide_DejaValide_ShouldThrowBusinessRule() {
        stubConfig(true);
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserRole()).thenReturn("CHEF_DE_FAMILLE");
        TransferRequest req = newRequest(TransferStatus.EN_ATTENTE_VALIDATION);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));
        when(decisionRepository.findFirstByTransferRequestIdAndValidateurIdOrderByCreatedAtDesc(
                req.getId(), demandeurId))
                .thenReturn(Optional.of(TransferDecision.builder().id(UUID.randomUUID())
                        .decision(DecisionType.APPROBATION).build()));

        assertThrows(com.discipolat.common.domain.BusinessRuleException.class,
                () -> service.decide(req.getId(), new DecideRequest(DecisionType.APPROBATION, "OK")));
    }

    // ========================================================================
    // ANNULATION / ARCHIVAGE
    // ========================================================================

    @Test
    void cancel_Brouillon_ShouldPasserAnnule() {
        stubConfig(false);
        TransferRequest req = newRequest(TransferStatus.BROUILLON);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        TransferRequest result = service.cancel(req.getId());

        assertEquals(TransferStatus.ANNULE, result.getStatut());
    }

    @Test
    void cancel_Execute_ShouldThrowBusinessRule() {
        TransferRequest req = newRequest(TransferStatus.EXECUTE);
        when(requestRepository.findById(req.getId())).thenReturn(Optional.of(req));

        assertThrows(com.discipolat.common.domain.BusinessRuleException.class,
                () -> service.cancel(req.getId()));
    }
}
