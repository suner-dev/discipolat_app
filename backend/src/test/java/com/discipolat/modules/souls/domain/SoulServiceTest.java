package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.evaluations.domain.EvaluationService;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.EntityAttachment;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.api.CreateSoulRequest;
import com.discipolat.modules.souls.api.UpdateSoulRequest;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoulServiceTest {

    @Mock
    private SoulRepository soulRepository;
    @Mock
    private SoulHistoryRepository soulHistoryRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SoulNoteRepository soulNoteRepository;
    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private SoulDepartmentRepository soulDepartmentRepository;
    @Mock
    private MakerReportRepository makerReportRepository;
    @Mock
    private EvaluationService evaluationService;
    @Mock
    private EntityAttachmentService attachmentService;

    private SoulService soulService;

    private UUID faiseurId;
    private UUID familleId;
    private Soul testSoul;

    @BeforeEach
    void setUp() {
        lenient().when(securityUtils.isSuperUser()).thenReturn(true);
        soulService = new SoulService(soulRepository, soulHistoryRepository,
                soulNoteRepository, securityUtils, userRepository,
                familyRepository, departmentRepository, soulDepartmentRepository,
                makerReportRepository, evaluationService, attachmentService);

        faiseurId = UUID.randomUUID();
        familleId = UUID.randomUUID();
        testSoul = Soul.builder()
                .id(UUID.randomUUID())
                .nom("Dupont")
                .prenom("Marie")
                .email("marie@email.com")
                .telephone("0123456789")
                .typeDisciple(TypeDisciple.NOUVEAU_CONVERTI)
                .statut(StatutAme.ACTIF)
                .dateIntegration(LocalDate.now())
                .faiseurId(faiseurId)
                .familleId(familleId)
                .etatSpirituel("NOUVEAU_CONVERTI")
                .niveauCroissance(1)
                .build();
    }

    @Test
    void createSoul_WithValidRequest_ShouldReturnSavedSoul() {
        CreateSoulRequest request = new CreateSoulRequest(
                "Dupont", "Marie", "marie@email.com", "0123456789",
                null, null, null, TypeDisciple.NOUVEAU_CONVERTI,
                LocalDate.now(), null, faiseurId, familleId,
                null, null, null
        );

        when(soulRepository.save(any(Soul.class))).thenReturn(testSoul);

        Soul result = soulService.create(request);

        assertNotNull(result);
        assertEquals("Dupont", result.getNom());
        assertEquals("Marie", result.getPrenom());
        assertEquals(TypeDisciple.NOUVEAU_CONVERTI, result.getTypeDisciple());
        verify(soulRepository).save(any(Soul.class));
        verify(soulHistoryRepository).save(any(SoulHistory.class));
    }

    @Test
    void findById_WithExistingId_ShouldReturnSoul() {
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));

        Soul result = soulService.findById(testSoul.getId());

        assertNotNull(result);
        assertEquals(testSoul.getId(), result.getId());
        assertEquals("Dupont", result.getNom());
    }

    @Test
    void findById_WithNonExistingId_ShouldThrowEntityNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(soulRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                soulService.findById(nonExistentId)
        );
    }

    @Test
    void findById_NonSuperUser_OutOfScope_ShouldThrowAccessDenied() {
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(securityUtils.hasActiveRole("FAISEUR")).thenReturn(false);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> soulService.findById(testSoul.getId()));
    }

    @Test
    void updateSoul_ShouldModifyFields() {
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));
        when(soulRepository.save(any(Soul.class))).thenReturn(testSoul);

        UpdateSoulRequest request = new UpdateSoulRequest(
                "Dupont2", null, null, null, null, null, null,
                TypeDisciple.NOUVEL_ARRIVANT, null, null, StatutAme.EN_VEILLE,
                null, null, null, null, null, "Notes du pasteur"
        );

        Soul result = soulService.update(testSoul.getId(), request);

        assertNotNull(result);
        verify(soulRepository).save(any(Soul.class));
        verify(soulHistoryRepository).save(any(SoulHistory.class));
    }

    @Test
    void findAll_WithoutFilters_SuperUser_ShouldReturnAllSouls() {
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(soulRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testSoul)));

        Page<Soul> result = soulService.findAll(null, null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        verify(soulRepository).findAll(any(Pageable.class));
        verify(soulRepository, never()).findAllByIdIn(anyList(), any(Pageable.class));
    }

    @Test
    void findAll_WithoutFilters_FaiseurActif_ShouldScopeToOwnSouls() {
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("FAISEUR")).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(faiseurId);
        when(soulRepository.findAllByFaiseurId(faiseurId)).thenReturn(List.of(testSoul));
        when(soulRepository.findAllByIdIn(List.of(testSoul.getId()), PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(testSoul)));

        Page<Soul> result = soulService.findAll(null, null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(testSoul.getId(), result.getContent().get(0).getId());
        verify(soulRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void findAll_WithExplicitFaiseurId_NonSuperUser_ShouldNotExpandScope() {
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("FAISEUR")).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(faiseurId);
        when(soulRepository.findAllByFaiseurId(faiseurId)).thenReturn(List.of(testSoul));
        when(soulRepository.findAllById(any())).thenReturn(List.of(testSoul));

        // Paramètre faiseurId = un AUTRE faiseur : ne doit PAS élargir l'accès
        Page<Soul> result = soulService.findAll(UUID.randomUUID(), null, null, null, null, PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
        verify(soulRepository, never()).findByFaiseurId(any(UUID.class), any(Pageable.class));
    }

    @Test
    void search_SansAcces_ShouldReturnEmpty() {
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(faiseurId);
        // Aucun rôle actif reconnu → aucune âme accessible

        Page<Soul> result = soulService.findAll(null, null, null, null, "Marie", PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
        verify(soulRepository, never()).search(anyString(), any(Pageable.class));
    }

    @Test
    void getPastoral360_ShouldAggregateAttachmentsFromMakerReports() {
        UUID reportId = UUID.randomUUID();
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(userRepository.findById(faiseurId)).thenReturn(Optional.empty());
        when(soulHistoryRepository.findByAmeIdOrderByCreatedAtDesc(testSoul.getId())).thenReturn(List.of());
        when(soulNoteRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(testSoul.getId())).thenReturn(List.of());
        when(makerReportRepository.findByAmeIdAndSemaine(any(), any())).thenReturn(List.of());
        when(makerReportRepository.findAllByAmeIdAndSoumisTrueOrderBySemaineDesc(testSoul.getId()))
                .thenReturn(List.of(MakerReport.builder().id(reportId).semaine(LocalDate.now()).soumis(true).build()));
        when(attachmentService.itemsFor(EntityAttachment.EntityType.MAKER_REPORT, reportId))
                .thenReturn(List.of(new EntityAttachmentService.AttachmentItem(
                        UUID.randomUUID(), UUID.randomUUID(), "Suivi.pdf", "https://drive/2")));

        Map<String, Object> dossier = soulService.getPastoral360(testSoul.getId());

        assertTrue(dossier.containsKey("piecesJointes"));
        List<?> pieces = (List<?>) dossier.get("piecesJointes");
        assertEquals(1, pieces.size());
        assertEquals("Suivi.pdf", ((Map<?, ?>) pieces.get(0)).get("nom"));
        assertEquals("https://drive/2", ((Map<?, ?>) pieces.get(0)).get("url"));
        assertNotNull(((Map<?, ?>) pieces.get(0)).get("source"));
        verify(attachmentService).itemsFor(EntityAttachment.EntityType.MAKER_REPORT, reportId);
    }

    @Test
    void deleteSoul_ShouldSetDeletedFlag() {
        when(soulRepository.findById(testSoul.getId())).thenReturn(Optional.of(testSoul));
        when(soulRepository.save(any(Soul.class))).thenReturn(testSoul);

        soulService.delete(testSoul.getId());

        assertTrue(testSoul.isDeleted());
        verify(soulRepository).save(any(Soul.class));
    }
}
