package com.discipolat.modules.reports.domain;

import com.discipolat.common.enums.StatutValidation;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.files.domain.EntityAttachment;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.files.domain.EntityAttachmentRepository;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.reports.api.SubmitFamilyReportRequest;
import com.discipolat.modules.reports.api.SubmitMakerReportRequest;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Isolation des espaces métiers : les rapports faiseur / famille sont scopés par
 * rôle actif (un faiseur ne voit que ses rapports, un chef ceux de sa famille,
 * un responsable ceux de ses départements). Les filtres explicites ne sont pas
 * des ancres de confiance (anti-IDOR).
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private MakerReportRepository makerReportRepository;
    @Mock
    private FamilyReportRepository familyReportRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private WorkspaceScopeService workspaceScope;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ParallelFollowupRepository parallelFollowupRepository;
    @Mock
    private EntityAttachmentRepository attachmentRepository;
    @Mock
    private FileEntityRepository fileEntityRepository;

    private ReportService reportService;
    private EntityAttachmentService attachmentService;

    private final UUID userId = UUID.randomUUID();
    private final UUID faiseurId = UUID.randomUUID();
    private final UUID autreFaiseurId = UUID.randomUUID();
    private final UUID ameId = UUID.randomUUID();
    private final UUID autreAmeId = UUID.randomUUID();
    private final UUID familleId = UUID.randomUUID();
    private final UUID autreFamilleId = UUID.randomUUID();
    private final LocalDate semaine = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

    private MakerReport rapportMoi;
    private MakerReport rapportAutre;
    private FamilyReport rapportFamille;
    private FamilyReport rapportFamilleAutre;

    @BeforeEach
    void setUp() {
        attachmentService = new EntityAttachmentService(attachmentRepository, fileEntityRepository, securityUtils);
        reportService = new ReportService(makerReportRepository, familyReportRepository, securityUtils,
                workspaceScope, soulRepository, userRepository, parallelFollowupRepository, attachmentService);

        rapportMoi = new MakerReport();
        rapportMoi.setId(UUID.randomUUID());
        rapportMoi.setFaiseurId(faiseurId);
        rapportMoi.setAmeId(ameId);
        rapportMoi.setSemaine(semaine);
        rapportMoi.setSoumis(true);
        rapportMoi.setVieFaiseurDemandesAide("Demande d'aide");

        rapportAutre = new MakerReport();
        rapportAutre.setId(UUID.randomUUID());
        rapportAutre.setFaiseurId(autreFaiseurId);
        rapportAutre.setAmeId(autreAmeId);
        rapportAutre.setSemaine(semaine);
        rapportAutre.setVieFaiseurDemandesAide("Autre demande hors espace");

        rapportFamille = FamilyReport.builder()
                .id(UUID.randomUUID()).familleId(familleId)
                .chefFamilleId(UUID.randomUUID()).semaine(semaine)
                .statutValidation(StatutValidation.SOUMIS)
                .build();
        rapportFamilleAutre = FamilyReport.builder()
                .id(UUID.randomUUID()).familleId(autreFamilleId)
                .chefFamilleId(UUID.randomUUID()).semaine(semaine)
                .statutValidation(StatutValidation.SOUMIS)
                .build();
    }

    @Test
    void findMakerReports_superUser_voitTout() {
        when(workspaceScope.isSuperUser()).thenReturn(true);
        when(makerReportRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rapportMoi, rapportAutre)));

        Page<MakerReport> result = reportService.findMakerReports(null, null, null, null, PageRequest.of(0, 20));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findMakerReports_faiseurActif_filtreSurSonEspace() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(makerReportRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rapportMoi, rapportAutre)));
        when(workspaceScope.accessibleSoulIds()).thenReturn(Set.of(ameId));
        when(workspaceScope.accessibleFaiseurIds()).thenReturn(Set.of(faiseurId));

        Page<MakerReport> result = reportService.findMakerReports(null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(rapportMoi.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findMakerReports_filtreAutreFaiseurId_neElargitPasLAccess() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(makerReportRepository.findByFaiseurId(autreFaiseurId, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(rapportAutre)));
        when(workspaceScope.accessibleSoulIds()).thenReturn(Set.of(ameId));
        when(workspaceScope.accessibleFaiseurIds()).thenReturn(Set.of(faiseurId));

        Page<MakerReport> result = reportService.findMakerReports(autreFaiseurId, null, null, null, PageRequest.of(0, 20));

        assertEquals(0, result.getTotalElements());
    }

    @Test
    void findMakerReportById_rapportHorsEspace_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(workspaceScope.canAccessFaiseur(rapportAutre.getFaiseurId())).thenReturn(false);
        when(workspaceScope.canAccessSoul(rapportAutre.getAmeId())).thenReturn(false);
        when(makerReportRepository.findById(rapportAutre.getId())).thenReturn(Optional.of(rapportAutre));

        assertThrows(AccessDeniedException.class, () -> reportService.findMakerReportById(rapportAutre.getId()));
    }

    @Test
    void findMakerReportById_sonPropreRapport_autorise() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(faiseurId);
        when(makerReportRepository.findById(rapportMoi.getId())).thenReturn(Optional.of(rapportMoi));

        assertEquals(rapportMoi.getId(), reportService.findMakerReportById(rapportMoi.getId()).getId());
    }

    @Test
    void findMakerReportById_ameAccessibleMaisPasFaiseur_autorise() {
        // Un chef peut lire un rapport d'une âme de SA famille même si le faiseur
        // n'est pas dans son espace (ex : faiseur parti de la famille).
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(workspaceScope.canAccessFaiseur(rapportMoi.getFaiseurId())).thenReturn(false);
        when(workspaceScope.canAccessSoul(rapportMoi.getAmeId())).thenReturn(true);
        when(makerReportRepository.findById(rapportMoi.getId())).thenReturn(Optional.of(rapportMoi));

        assertEquals(rapportMoi.getId(), reportService.findMakerReportById(rapportMoi.getId()).getId());
    }

    @Test
    void submitFamilyReport_chefFamilleIdUsurpe_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(familleId)).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        SubmitFamilyReportRequest request = new SubmitFamilyReportRequest(
                familleId, UUID.randomUUID(), semaine, null, null, null, null, null, null, "Synthèse", null);

        assertThrows(AccessDeniedException.class, () -> reportService.submitFamilyReport(request));
        verify(familyReportRepository, never()).save(any(FamilyReport.class));
    }

    @Test
    void findFamilyReports_nonSuper_filtreSurLesFamillesVisibles() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.accessibleFamilyIds()).thenReturn(Set.of(familleId));
        when(familyReportRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(rapportFamille, rapportFamilleAutre)));

        Page<FamilyReport> result = reportService.findFamilyReports(null, null, null, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(rapportFamille.getId(), result.getContent().get(0).getId());
    }

    @Test
    void findFamilyReportsByFamily_horsEspace_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(autreFamilleId)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> reportService.findFamilyReportsByFamily(autreFamilleId));
    }

    @Test
    void submitMakerReport_faiseurNonAutorise_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(workspaceScope.canAccessFaiseur(autreFaiseurId)).thenReturn(false);

        SubmitMakerReportRequest request = new SubmitMakerReportRequest(
                autreFaiseurId, ameId, semaine, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        assertThrows(AccessDeniedException.class, () -> reportService.submitMakerReport(request));
    }

    @Test
    void getUrgentAidRequests_responsable_scopesParDepartement() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.accessibleFaiseurIds()).thenReturn(Set.of(faiseurId));
        when(makerReportRepository.findAll()).thenReturn(List.of(rapportMoi, rapportAutre));

        List<Map<String, Object>> result = reportService.getUrgentAidRequests(null);

        assertEquals(1, result.size());
        assertEquals(rapportMoi.getId(), result.get(0).get("reportId"));
    }

    @Test
    void validateFamilyReport_horsEspace_refuse() {
        when(workspaceScope.isSuperUser()).thenReturn(false);
        when(workspaceScope.canAccessFamily(autreFamilleId)).thenReturn(false);
        when(familyReportRepository.findById(rapportFamilleAutre.getId())).thenReturn(Optional.of(rapportFamilleAutre));

        assertThrows(AccessDeniedException.class,
                () -> reportService.validateFamilyReport(rapportFamilleAutre.getId(), "RESPONSABLE"));
    }
}
