package com.discipolat.modules.departments.domain;

import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.departments.api.DepartmentAnnouncementRequest;
import com.discipolat.modules.departments.api.DepartmentMemberReportRequest;
import com.discipolat.modules.departments.api.DepartmentNoteRequest;
import com.discipolat.modules.discipline.domain.SoulDisciplineEventRepository;
import com.discipolat.modules.evaluations.domain.EvaluationRepository;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulNoteRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class DepartmentDossierServiceTest {

    @Mock
    private DepartmentService departmentService;
    @Mock
    private DepartmentTeamRepository teamRepository;
    @Mock
    private DepartmentPositionRepository positionRepository;
    @Mock
    private DepartmentAssignmentRepository assignmentRepository;
    @Mock
    private DepartmentTaskRepository taskRepository;
    @Mock
    private DepartmentActivityRepository activityRepository;
    @Mock
    private DepartmentMemberNoteRepository noteRepository;
    @Mock
    private DepartmentAnnouncementRepository announcementRepository;
    @Mock
    private DepartmentMemberObjectiveRepository objectiveRepository;
    @Mock
    private DepartmentMemberReportRepository memberReportRepository;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private SoulDepartmentRepository soulDepartmentRepository;
    @Mock
    private SoulNoteRepository soulNoteRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MemberPresenceRepository presenceRepository;
    @Mock
    private SoulDisciplineEventRepository disciplineRepository;
    @Mock
    private MakerReportRepository makerReportRepository;
    @Mock
    private EvaluationRepository evaluationRepository;
    @Mock
    private EventRegistrationRepository eventRegistrationRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private TransferRequestRepository transferRequestRepository;
    @Mock
    private EntityAttachmentService attachmentService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private DepartmentSettingsService settingsService;

    private DepartmentDossierService service;
    private final UUID deptId = UUID.randomUUID();
    private final UUID memberId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DepartmentDossierService(departmentService, teamRepository, positionRepository,
                assignmentRepository, taskRepository, activityRepository, noteRepository, announcementRepository,
                objectiveRepository, memberReportRepository,
                soulRepository, soulDepartmentRepository, soulNoteRepository, userRepository, familyRepository,
                securityUtils, presenceRepository, disciplineRepository, makerReportRepository, evaluationRepository,
                eventRegistrationRepository, eventRepository, alertRepository, transferRequestRepository,
                attachmentService, notificationService, settingsService);
        lenient().when(settingsService.effectiveSettings(deptId)).thenReturn(
                DepartmentSetting.builder().departmentId(deptId).build());
        lenient().when(departmentService.findById(deptId)).thenReturn(new Department());
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        lenient().when(userRepository.findById(any(UUID.class))).thenReturn(Optional.empty());
        lenient().when(securityUtils.isSuperUser()).thenReturn(true);
    }

    private Soul soul(UUID id) {
        return Soul.builder().id(id).nom("Dupont").prenom("Jean")
                .typeDisciple(TypeDisciple.NOUVEAU_CONVERTI).statut(StatutAme.ACTIF)
                .dateIntegration(LocalDate.now().minusDays(10)).build();
    }

    // ======================= DOSSIER =======================

    @Test
    void getMemberDossier_returnsProfilWithTraçabilityAndSections() {
        Soul soul = soul(memberId);
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul));
        when(soulDepartmentRepository.findBySoulIdAndDepartmentId(memberId, deptId)).thenReturn(List.of(
                SoulDepartment.builder().soulId(memberId).departmentId(deptId)
                        .actif(true).dateAffectation(LocalDateTime.now().minusDays(5))
                        .createdBy(UUID.randomUUID()).origine("MANUEL").build()));
        when(soulDepartmentRepository.findBySoulId(memberId)).thenReturn(List.of());
        when(assignmentRepository.findByDepartmentIdAndMemberId(deptId, memberId)).thenReturn(List.of());
        when(taskRepository.findByDepartmentIdOrderByEcheanceAsc(deptId)).thenReturn(List.of());
        when(presenceRepository.findBySoulIdInOrderBySemaineDesc(List.of(memberId))).thenReturn(List.of());
        when(disciplineRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(memberId)).thenReturn(List.of());
        when(makerReportRepository.findByAmeId(eq(memberId), any())).thenReturn(org.springframework.data.domain.Page.empty());
        when(noteRepository.findByDepartmentIdAndMemberIdAndDeletedFalseOrderByCreatedAtDesc(deptId, memberId)).thenReturn(List.of());
        when(soulNoteRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(memberId)).thenReturn(List.of());
        when(alertRepository.findByAmeIdAndStatut(memberId, StatutAlerte.ACTIVE)).thenReturn(List.of());
        when(transferRequestRepository.findByPersonneId(memberId)).thenReturn(List.of());
        when(activityRepository.findTop50ByDepartmentIdOrderByCreatedAtDesc(deptId)).thenReturn(List.of());
        when(announcementRepository.findByDepartmentIdAndDeletedFalseOrderByCreatedAtDesc(deptId)).thenReturn(List.of());

        Map<String, Object> dossier = service.getMemberDossier(deptId, memberId);

        assertThat(dossier.get("memberId")).isEqualTo(memberId);
        Map<?, ?> profil = (Map<?, ?>) dossier.get("profil");
        assertThat(profil.get("nomComplet")).isEqualTo("Jean Dupont");
        assertThat(profil.get("origine")).isEqualTo("MANUEL");
        assertThat(profil.get("membreActif")).isEqualTo(true);
        assertThat(dossier).containsKeys("appartenance", "affectations", "taches", "presences",
                "discipline", "rapports", "evaluations", "evenements", "documents", "notes",
                "notesDisciple", "alertes", "transferts", "activite", "annonces");
    }

    @Test
    void getMemberDossier_rejectsMemberOutsideDepartment() {
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, deptId)).thenReturn(false);
        lenient().when(securityUtils.isSuperUser()).thenReturn(false);

        try {
            service.getMemberDossier(deptId, memberId);
            org.assertj.core.api.Assertions.fail("Doit refuser l'accès");
        } catch (org.springframework.security.access.AccessDeniedException e) {
            assertThat(e.getMessage()).contains("ne fait pas partie");
        }
        verify(soulRepository, never()).findById(any());
    }

    // ======================= STATISTIQUES =======================

    @Test
    void getDepartmentStats_computesEffectifPresenceAndTasks() {
        Soul actif = soul(memberId);
        Soul veille = Soul.builder().id(UUID.randomUUID()).nom("Martin").prenom("Paul")
                .typeDisciple(TypeDisciple.NOUVEL_ARRIVANT).statut(StatutAme.EN_VEILLE)
                .dateIntegration(LocalDate.now().minusYears(1)).build();
        SoulDepartment link1 = SoulDepartment.builder().soulId(memberId).departmentId(deptId)
                .actif(true).dateAffectation(LocalDateTime.now().minusMonths(2)).build();
        SoulDepartment link2 = SoulDepartment.builder().soulId(veille.getId()).departmentId(deptId)
                .actif(true).dateAffectation(LocalDateTime.now().minusMonths(1)).build();

        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of(link1, link2));
        when(soulDepartmentRepository.findByDepartmentId(deptId)).thenReturn(List.of(link1, link2));
        when(soulRepository.findAllById(anyList())).thenReturn(List.of(actif, veille));

        MemberPresence p1 = MemberPresence.builder().soulId(memberId).semaine(LocalDate.now()).present(true).build();
        MemberPresence p2 = MemberPresence.builder().soulId(memberId).semaine(LocalDate.now().minusWeeks(1)).present(false).build();
        when(presenceRepository.findBySoulIdInOrderBySemaineDesc(anyList())).thenReturn(List.of(p1, p2));

        DepartmentTask task = DepartmentTask.builder().id(UUID.randomUUID()).departmentId(deptId)
                .titre("Montage").statut(DepartmentTask.TaskStatus.EN_COURS)
                .echeance(LocalDate.now().minusDays(1)).assignedTo(memberId)
                .createdAt(LocalDateTime.now().minusDays(2)).build();
        when(taskRepository.findByDepartmentIdOrderByEcheanceAsc(deptId)).thenReturn(List.of(task));
        when(teamRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());
        when(disciplineRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(any(UUID.class))).thenReturn(List.of());
        when(assignmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of());
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(List.of());
        when(departmentService.findById(deptId)).thenReturn(Department.builder().id(deptId).responsableId(UUID.randomUUID()).build());

        Map<String, Object> stats = service.getDepartmentStats(deptId);

        Map<?, ?> effectif = (Map<?, ?>) stats.get("effectif");
        assertThat(effectif.get("total")).isEqualTo(2L);
        assertThat(effectif.get("actifs")).isEqualTo(1L);
        assertThat(effectif.get("enVeille")).isEqualTo(1L);

        Map<?, ?> presence = (Map<?, ?>) stats.get("presence");
        assertThat(presence.get("presents")).isEqualTo(1L);
        assertThat(presence.get("absents")).isEqualTo(1L);
        assertThat(presence.get("taux")).isEqualTo(50.0);

        Map<?, ?> taches = (Map<?, ?>) stats.get("taches");
        assertThat(taches.get("enRetard")).isEqualTo(1L);
        assertThat(stats).containsKeys("evolutionEffectif", "evolutionPresence", "evolutionTaches",
                "disciplineParCategorie", "equipes", "affectations", "chargeParMembre", "evenements");
    }

    @Test
    void getDepartmentStats_avecPeriode_filtrePresenceEtTaches() {
        Soul actif = soul(memberId);
        SoulDepartment link1 = SoulDepartment.builder().soulId(memberId).departmentId(deptId)
                .actif(true).dateAffectation(LocalDateTime.now().minusMonths(2)).build();
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of(link1));
        when(soulDepartmentRepository.findByDepartmentId(deptId)).thenReturn(List.of(link1));
        when(soulRepository.findAllById(anyList())).thenReturn(List.of(actif));

        LocalDate limite = LocalDate.now().minusMonths(2);
        MemberPresence ancienne = MemberPresence.builder().soulId(memberId).semaine(limite.minusMonths(3)).present(true).build();
        MemberPresence recente = MemberPresence.builder().soulId(memberId).semaine(LocalDate.now().minusWeeks(1)).present(false).build();
        when(presenceRepository.findBySoulIdInOrderBySemaineDesc(anyList()))
                .thenReturn(List.of(recente, ancienne));

        DepartmentTask ancienneTache = DepartmentTask.builder().id(UUID.randomUUID()).departmentId(deptId)
                .titre("Ancienne").statut(DepartmentTask.TaskStatus.TERMINEE)
                .echeance(LocalDate.now().minusMonths(4))
                .createdAt(LocalDateTime.now().minusMonths(4)).build();
        DepartmentTask tacheRecente = DepartmentTask.builder().id(UUID.randomUUID()).departmentId(deptId)
                .titre("Récente").statut(DepartmentTask.TaskStatus.EN_COURS)
                .echeance(LocalDate.now().minusDays(1)).assignedTo(memberId)
                .createdAt(LocalDateTime.now().minusDays(2)).build();
        when(taskRepository.findByDepartmentIdOrderByEcheanceAsc(deptId))
                .thenReturn(List.of(ancienneTache, tacheRecente));
        when(teamRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());
        when(disciplineRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(any(UUID.class))).thenReturn(List.of());
        when(assignmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of());
        when(eventRepository.findByDateDebutBetweenAndDeletedFalse(any(), any())).thenReturn(List.of());
        when(departmentService.findById(deptId)).thenReturn(Department.builder().id(deptId).responsableId(UUID.randomUUID()).build());

        // Période : dernier mois — l'ancienne présence et l'ancienne tâche sortent du périmètre
        Map<String, Object> stats = service.getDepartmentStats(deptId, "MOIS", null, null);

        Map<?, ?> presence = (Map<?, ?>) stats.get("presence");
        assertThat(presence.get("total")).isEqualTo(1L); // seulement la fiche récente
        assertThat(presence.get("absents")).isEqualTo(1L);

        Map<?, ?> taches = (Map<?, ?>) stats.get("taches");
        assertThat(taches.get("total")).isEqualTo(1L); // seulement la tâche récente

        Map<?, ?> periode = (Map<?, ?>) stats.get("periode");
        assertThat(periode.get("code")).isEqualTo("MOIS");
    }

    // ======================= IMPORT / EXPORT =======================

    @Test
    void importMembers_previewClassifiesDuplicateAndCreatable() {
        Soul existing = Soul.builder().id(UUID.randomUUID()).nom("Martin").prenom("Paul")
                .email("paul@mail.com").typeDisciple(TypeDisciple.NOUVEL_ARRIVANT).statut(StatutAme.ACTIF).build();
        when(soulRepository.findByEmailIgnoreCaseAndDeletedFalse("paul@mail.com")).thenReturn(Optional.of(existing));
        when(teamRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of(
                DepartmentTeam.builder().id(UUID.randomUUID()).departmentId(deptId).nom("Son")
                        .statut(DepartmentTeam.TeamStatus.ACTIVE).build()));
        when(positionRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());

        Map<String, Object> result = service.importMembers(deptId, List.of(
                Map.of("nom", "Martin", "prenom", "Paul", "email", "paul@mail.com"),
                Map.of("nom", "Kouassi", "prenom", "Aya", "email", "aya@mail.com", "equipe", "Son")
        ), true);

        assertThat(result.get("preview")).isEqualTo(true);
        assertThat(result.get("doublon")).isEqualTo(1);
        assertThat(result.get("cree")).isEqualTo(1);
        assertThat(result.get("erreur")).isEqualTo(0);
        verify(soulRepository, never()).save(any(Soul.class));
    }

    @Test
    void importMembers_commitCreatesSoulsAndAssignments() {
        when(soulRepository.findByEmailIgnoreCaseAndDeletedFalse(any())).thenReturn(Optional.empty());
        UUID teamId = UUID.randomUUID();
        when(teamRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of(
                DepartmentTeam.builder().id(teamId).departmentId(deptId).nom("Son")
                        .statut(DepartmentTeam.TeamStatus.ACTIVE).build()));
        when(positionRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());
        when(soulRepository.save(any(Soul.class))).thenAnswer(inv -> {
            Soul s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        Map<String, Object> result = service.importMembers(deptId, List.of(
                Map.of("nom", "Kouassi", "prenom", "Aya", "email", "aya@mail.com", "equipe", "Son")
        ), false);

        assertThat(result.get("preview")).isEqualTo(false);
        assertThat(result.get("cree")).isEqualTo(1);
        verify(soulDepartmentRepository).save(any(SoulDepartment.class));
        verify(assignmentRepository).save(argThat(a -> teamId.equals(a.getTeamId())));
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    @Test
    void importMembers_errorsOnUnknownTeam() {
        when(teamRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());
        when(positionRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());

        Map<String, Object> result = service.importMembers(deptId, List.of(
                Map.of("nom", "Kouassi", "equipe", "Son")
        ), true);

        assertThat(result.get("erreur")).isEqualTo(1);
        assertThat(result.get("cree")).isEqualTo(0);
    }

    @Test
    void exportMembersCsv_returnsHeaderAndRows() {
        Soul soul = soul(memberId);
        SoulDepartment link = SoulDepartment.builder().soulId(memberId).departmentId(deptId).actif(true).build();
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of(link));
        when(soulRepository.findAllById(List.of(memberId))).thenReturn(List.of(soul));
        when(teamRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());
        when(positionRepository.findByDepartmentIdOrderByNomAsc(deptId)).thenReturn(List.of());
        when(assignmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of());

        String csv = service.exportMembersCsv(deptId);

        assertThat(csv).startsWith("\uFEFF");
        assertThat(csv).contains("nom;prenom;telephone;email");
        assertThat(csv).contains("Dupont;Jean");
    }

    // ======================= NOTES =======================

    @Test
    void addMemberNote_savesAndRecordsActivity() {
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, deptId)).thenReturn(true);
        when(noteRepository.save(any(DepartmentMemberNote.class))).thenAnswer(inv -> {
            DepartmentMemberNote n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul(memberId)));

        Map<String, Object> note = service.addMemberNote(deptId, memberId, new DepartmentNoteRequest("À suivre"));

        assertThat(note.get("contenu")).isEqualTo("À suivre");
        verify(noteRepository).save(argThat(n -> n.getDepartmentId().equals(deptId) && n.getMemberId().equals(memberId)));
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    // ======================= ANNONCES =======================

    @Test
    void createAnnouncement_rejectsTeamFromAnotherDepartment() {
        UUID otherDept = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(
                DepartmentTeam.builder().id(teamId).departmentId(otherDept).nom("Autre").build()));

        DepartmentAnnouncementRequest request = new DepartmentAnnouncementRequest(
                "Répétition", "Présence à 8h", DepartmentAnnouncement.Cible.EQUIPE, teamId, null, null);

        try {
            service.createAnnouncement(deptId, request);
            org.assertj.core.api.Assertions.fail("Doit refuser");
        } catch (com.discipolat.common.domain.BusinessRuleException e) {
            assertThat(e.getMessage()).contains("appartenir au département");
        }
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void createAnnouncement_cibleMembres_avecMembreHorsDepartement_refuse() {
        UUID otherMember = UUID.randomUUID();
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(otherMember, deptId)).thenReturn(false);

        DepartmentAnnouncementRequest request = new DepartmentAnnouncementRequest(
                "Rappel", "Répétition samedi", DepartmentAnnouncement.Cible.MEMBRES, null, null,
                List.of(otherMember));

        assertThatThrownBy(() -> service.createAnnouncement(deptId, request))
                .isInstanceOf(com.discipolat.common.domain.BusinessRuleException.class);
        verify(announcementRepository, never()).save(any());
    }

    @Test
    void createAnnouncement_cibleMembres_valideEtSauvegarde() {
        UUID memberA = UUID.randomUUID();
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberA, deptId)).thenReturn(true);
        when(announcementRepository.save(any(DepartmentAnnouncement.class))).thenAnswer(inv -> {
            DepartmentAnnouncement a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(LocalDateTime.now());
            return a;
        });

        DepartmentAnnouncementRequest request = new DepartmentAnnouncementRequest(
                "Rappel", "Répétition samedi", DepartmentAnnouncement.Cible.MEMBRES, null, null,
                List.of(memberA));

        Map<String, Object> annonce = service.createAnnouncement(deptId, request);

        assertThat(annonce.get("cible")).isEqualTo("MEMBRES");
        ArgumentCaptor<DepartmentAnnouncement> captor = ArgumentCaptor.forClass(DepartmentAnnouncement.class);
        verify(announcementRepository).save(captor.capture());
        assertThat(captor.getValue().getMemberIds()).contains(memberA);
    }

    @Test
    void createAnnouncement_savesForAllMembers() {
        DepartmentAnnouncementRequest request = new DepartmentAnnouncementRequest(
                "Répétition", "Présence à 8h", DepartmentAnnouncement.Cible.TOUS, null, null, null);
        when(announcementRepository.save(any(DepartmentAnnouncement.class))).thenAnswer(inv -> {
            DepartmentAnnouncement a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setCreatedAt(LocalDateTime.now());
            return a;
        });

        Map<String, Object> annonce = service.createAnnouncement(deptId, request);

        assertThat(annonce.get("titre")).isEqualTo("Répétition");
        assertThat(annonce.get("cible")).isEqualTo("TOUS");
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    // ======================= ALERTES INTELLIGENTES =======================

    @Test
    void getIntelligentAlerts_createsAbsenceAlertAndDeduplicates() {
        Soul soul = soul(memberId);
        SoulDepartment link = SoulDepartment.builder().soulId(memberId).departmentId(deptId).actif(true).build();
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of(link));
        when(soulRepository.findAllById(List.of(memberId))).thenReturn(List.of(soul));

        MemberPresence abs1 = MemberPresence.builder().soulId(memberId).semaine(LocalDate.now()).present(false).build();
        MemberPresence abs2 = MemberPresence.builder().soulId(memberId).semaine(LocalDate.now().minusWeeks(1)).present(false).build();
        MemberPresence pre = MemberPresence.builder().soulId(memberId).semaine(LocalDate.now().minusWeeks(2)).present(true).build();
        when(presenceRepository.findBySoulIdInOrderBySemaineDesc(List.of(memberId))).thenReturn(List.of(abs1, abs2, pre));
        when(taskRepository.findByDepartmentIdOrderByEcheanceAsc(deptId)).thenReturn(List.of());
        when(alertRepository.existsByDepartmentIdAndAmeIdAndTypeAlerteAndStatut(deptId, memberId, "ABSENCE_REPETEE", StatutAlerte.ACTIVE))
                .thenReturn(false);
        when(alertRepository.findByDepartmentIdAndStatut(deptId, StatutAlerte.ACTIVE)).thenReturn(List.of(
                Alert.builder().id(UUID.randomUUID()).departmentId(deptId).ameId(memberId)
                        .typeAlerte("ABSENCE_REPETEE").titre("Absences répétées").message("x")
                        .dateDeclenchement(LocalDateTime.now()).statut(StatutAlerte.ACTIVE).build()));

        List<Map<String, Object>> alerts = service.getIntelligentAlerts(deptId);

        verify(alertRepository).save(argThat(a -> "ABSENCE_REPETEE".equals(a.getTypeAlerte())
                && a.getDepartmentId().equals(deptId) && a.getAmeId().equals(memberId)));
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).get("typeAlerte")).isEqualTo("ABSENCE_REPETEE");
    }

    // ======================= RAPPORTS DU RESPONSABLE SUR UN MEMBRE =======================

    @Test
    void createMemberReport_savesReportAndActivity() {
        when(soulRepository.findById(memberId)).thenReturn(Optional.of(soul(memberId)));
        UUID savedId = UUID.randomUUID();
        when(memberReportRepository.save(any(DepartmentMemberReport.class))).thenAnswer(inv -> {
            DepartmentMemberReport r = inv.getArgument(0);
            r.setId(savedId);
            r.setCreatedAt(LocalDateTime.now());
            return r;
        });
        when(memberReportRepository.findByMemberIdAndDepartmentIdOrderByCreatedAtDesc(memberId, deptId))
                .thenReturn(List.of(DepartmentMemberReport.builder()
                        .id(savedId).departmentId(deptId).memberId(memberId)
                        .auteurId(UUID.randomUUID()).type(DepartmentMemberReport.ReportType.PROGRESSION)
                        .contenu("Bonne progression").createdAt(LocalDateTime.now()).build()));

        Map<String, Object> report = service.createMemberReport(deptId, memberId,
                new DepartmentMemberReportRequest(DepartmentMemberReport.ReportType.PROGRESSION, "Bonne progression"));

        assertThat(report.get("type")).isEqualTo("PROGRESSION");
        assertThat(report.get("contenu")).isEqualTo("Bonne progression");
        verify(memberReportRepository).save(any(DepartmentMemberReport.class));
        verify(activityRepository).save(any(DepartmentActivity.class));
    }

    @Test
    void deleteMemberReport_rejectsReportFromAnotherDepartment() {
        UUID reportId = UUID.randomUUID();
        when(memberReportRepository.findById(reportId)).thenReturn(Optional.of(
                DepartmentMemberReport.builder().id(reportId).departmentId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> service.deleteMemberReport(deptId, reportId))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        verify(memberReportRepository, never()).delete(any());
    }
}
