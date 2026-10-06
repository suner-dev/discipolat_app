package com.discipolat.modules.discipleship.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests du service Discipleship (V233).
 *
 * <p>Ces tests n'existaient pas : la spécification (§2.4) les exigeait et la
 * section « État d'avancement » les déclarait « Terminé » sans les avoir
 * écrits. C'est précisément l'absence de couverture qui a laissé passer les
 * défauts corrigés ici :
 * <ul>
 *   <li>le filtre {@code isActive} de {@code listJourneys} était inopérant
 *       (les deux branches du ternaire appelaient la même requête) ;</li>
 *   <li>{@code getTopMentors} renvoyait des compteurs en dur
 *       ({@code discipleCount: 1}, {@code meetingCount: 0}) ;</li>
 *   <li>le rapport de parcours ne renvoyait pas les champs {@code required}
 *       du modèle mobile ;</li>
 *   <li>{@code listProgress} ignorait {@code status} dès qu'un autre filtre
 *       était présent, et paginait de façon fausse ;</li>
 *   <li>aucun revérification de tenant sur les utilisateurs référencés (R4).</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiscipleshipServiceTest {

    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");
    private static final UUID MENTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DISCIPLE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID ACTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private DiscipleshipJourneyRepository journeyRepository;
    @Mock private DiscipleshipStageRepository stageRepository;
    @Mock private StageRequirementRepository requirementRepository;
    @Mock private StageRewardRepository rewardRepository;
    @Mock private DiscipleProgressRepository progressRepository;
    @Mock private DiscipleProgressRequirementRepository progressRequirementRepository;
    @Mock private MentorAssignmentRepository assignmentRepository;
    @Mock private MentorMeetingRepository meetingRepository;
    @Mock private UserRepository userRepository;

    private DiscipleshipService service;

    @BeforeEach
    void setUp() {
        service = new DiscipleshipService(journeyRepository, stageRepository, requirementRepository,
                rewardRepository, progressRepository, progressRequirementRepository,
                assignmentRepository, meetingRepository, userRepository);
        SecurityTestHelper.loginAs(ACTOR, "ADMIN");
    }

    private DiscipleshipJourney journey(Long id, UUID tenantId, String name) {
        DiscipleshipJourney j = DiscipleshipJourney.builder()
                .id(id).tenantId(tenantId).name(name)
                .type(DiscipleshipJourney.JourneyType.GROWTH)
                .totalStages(3)
                .startDate(Instant.parse("2026-01-01T00:00:00Z"))
                .isActive(true)
                .build();
        // createdAt est non nul dans les vues : on déclenche le callback JPA réel.
        invokeOnCreate(j);
        return j;
    }

    private DiscipleshipStage stage(Long id, UUID tenantId, Long journeyId, int order, String name) {
        DiscipleshipStage s = DiscipleshipStage.builder()
                .id(id).tenantId(tenantId).journeyId(journeyId).order(order).name(name)
                .isOptional(false)
                .build();
        invokeOnCreate(s);
        return s;
    }

    private DiscipleProgress progress(Long id, UUID tenantId, UUID discipleId, Long journeyId,
                                      int completed, int total) {
        DiscipleProgress p = DiscipleProgress.builder()
                .id(id).tenantId(tenantId).discipleId(discipleId).journeyId(journeyId)
                .completedStages(completed).totalStages(total)
                .completedRequirements(0).totalRequirements(0)
                .status(DiscipleProgress.ProgressStatus.IN_PROGRESS)
                .build();
        invokeOnCreate(p);
        return p;
    }

    /** Déclenche le vrai {@code @PrePersist} : les dates sont pilotées par l'entité. */
    private static void invokeOnCreate(Object entity) {
        try {
            entity.getClass().getDeclaredMethod("onCreate").invoke(entity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("callback @PrePersist introuvable sur " + entity.getClass(), e);
        }
    }

    private void userIn(UUID tenantId, UUID id, String first, String last) {
        User u = new User();
        u.setId(id);
        u.setTenantId(tenantId);
        u.setFirstName(first);
        u.setLastName(last);
        when(userRepository.findByIdWithActiveMembershipInTenant(id, tenantId)).thenReturn(Optional.of(u));
        lenient().when(userRepository.findById(id)).thenReturn(Optional.of(u));
    }

    // ==================== ISOLATION MULTI-TENANT (R4) ====================

    @Nested
    @DisplayName("Isolation multi-tenant : aucun IDOR")
    class TenantIsolation {

        @Test
        @DisplayName("lire le parcours d'un autre tenant lève 404 et ne renvoie rien")
        void getJourney_ofOtherTenant_isNotReadable() {
            Long id = 42L;
            // Le repository, filtré par tenant, ne renvoie rien.
            when(journeyRepository.findByTenantIdAndId(TENANT_A, id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getJourney(TENANT_A, id))
                    .isInstanceOf(EntityNotFoundException.class);
            // On ne doit surtout PAS avoir relu le même id chez un autre tenant.
            verify(journeyRepository, org.mockito.Mockito.never()).findByTenantIdAndId(TENANT_B, id);
        }

        @Test
        @DisplayName("progresser un disciple d'un autre tenant est refusé (R4)")
        void createProgress_withForeignDisciple_isRejected() {
            when(journeyRepository.findByTenantIdAndId(eq(TENANT_A), eq(1L)))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            // Le disciple n'a AUCUNE membership active dans TENANT_A.
            when(userRepository.findByIdWithActiveMembershipInTenant(DISCIPLE, TENANT_A))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createProgress(TENANT_A,
                    Map.of("discipleId", DISCIPLE.toString(), "journeyId", 1L)))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(progressRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("assigner un mentor d'un autre tenant est refusé (R4)")
        void createAssignment_withForeignMentor_isRejected() {
            when(journeyRepository.findByTenantIdAndId(eq(TENANT_A), eq(1L)))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            when(userRepository.findByIdWithActiveMembershipInTenant(MENTOR, TENANT_A))
                    .thenReturn(Optional.empty());
            userIn(TENANT_A, DISCIPLE, "Jean", "Bonheur");

            assertThatThrownBy(() -> service.createAssignment(TENANT_A, Map.of(
                    "mentorId", MENTOR.toString(),
                    "discipleId", DISCIPLE.toString(),
                    "journeyId", 1L)))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(assignmentRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("planifier une réunion sur une assignation d'un autre tenant est refusé (R4)")
        void scheduleMeeting_onForeignAssignment_isRejected() {
            when(assignmentRepository.findByTenantIdAndId(TENANT_A, 7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.scheduleMeeting(TENANT_A, Map.of("assignmentId", 7L)))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(meetingRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("créer une étape sur un parcours d'un autre tenant est refusé")
        void createStage_onForeignJourney_isRejected() {
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createStage(TENANT_A,
                    Map.of("name", "Étape", "journeyId", 9L)))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(stageRepository, org.mockito.Mockito.never()).save(any());
        }
    }

    // ==================== FILTRES ET PAGINATION ====================

    @Nested
    @DisplayName("Filtres et pagination")
    class Filters {

        @Test
        @DisplayName("listJourneys : isActive=false renvoie les parcours ARCHIVÉS")
        void listJourneys_honoursInactiveFilter() {
            List<DiscipleshipJourney> archived = List.of(journey(2L, TENANT_A, "Ancien parcours"));
            when(journeyRepository.findByTenantIdAndIsActiveFalseOrderByNameAsc(TENANT_A))
                    .thenReturn(archived);

            List<Map<String, Object>> rows = service.listJourneys(TENANT_A, false);

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)).containsEntry("name", "Ancien parcours");
            // La requête « actifs » ne doit pas avoir été utilisée.
            verify(journeyRepository, org.mockito.Mockito.never())
                    .findByTenantIdAndIsActiveTrueOrderByNameAsc(TENANT_A);
        }

        @Test
        @DisplayName("listJourneys : isActive=true renvoie les parcours ACTIFS")
        void listJourneys_honoursActiveFilter() {
            when(journeyRepository.findByTenantIdAndIsActiveTrueOrderByNameAsc(TENANT_A))
                    .thenReturn(List.of(journey(1L, TENANT_A, "Croissance")));

            assertThat(service.listJourneys(TENANT_A, true)).hasSize(1);
        }

        @Test
        @DisplayName("listProgress : le statut est combiné aux autres filtres")
        void listProgress_combinesStatusWithOtherFilters() {
            // Régression : le statut était ignoré dès qu'un autre filtre était présent.
            when(progressRepository.findAll(eq(TENANT_A), eq(1L), eq(DISCIPLE),
                    eq(DiscipleProgress.ProgressStatus.COMPLETED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(
                            progress(5L, TENANT_A, DISCIPLE, 1L, 3, 3))));

            List<Map<String, Object>> rows = service.listProgress(
                    TENANT_A, 0, 20, 1L, DISCIPLE, "COMPLETED");

            assertThat(rows).hasSize(1);
            verify(progressRepository).findAll(eq(TENANT_A), eq(1L), eq(DISCIPLE),
                    eq(DiscipleProgress.ProgressStatus.COMPLETED), any(Pageable.class));
        }

        @Test
        @DisplayName("listProgress : un statut inconnu est rejeté en 400, pas silencieusement ignoré")
        void listProgress_rejectsUnknownStatus() {
            assertThatThrownBy(() -> service.listProgress(TENANT_A, 0, 20, null, null, "N_IMPORTE_QUOI"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ProgressStatus");
        }

        @Test
        @DisplayName("la taille de page est bornée côté serveur (R8)")
        void listProgress_clampsPageSize() {
            when(progressRepository.findAll(any(), any(), any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            service.listProgress(TENANT_A, 0, 100_000, null, null, null);

            org.mockito.ArgumentCaptor<Pageable> captor =
                    org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(progressRepository).findAll(any(), any(), any(), any(), captor.capture());
            assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        }
    }

    // ==================== RAPPORTS ====================

    @Nested
    @DisplayName("Rapports")
    class Reports {

        @Test
        @DisplayName("le rapport contient TOUS les champs required du modèle mobile")
        void report_containsEveryFieldRequiredByMobileModel() {
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Grandissement")));
            when(progressRepository.countByTenantIdAndJourneyId(TENANT_A, 1L)).thenReturn(2L);
            when(progressRepository.countByTenantIdAndJourneyIdAndStatus(any(), eq(1L), any()))
                    .thenReturn(1L);
            when(meetingRepository.countByTenantIdAndJourneyId(TENANT_A, 1L)).thenReturn(3L);
            when(meetingRepository.countByTenantIdAndJourneyIdAndStatus(any(), eq(1L), any()))
                    .thenReturn(2L);
            when(progressRepository.findByTenantIdAndJourneyId(eq(TENANT_A), eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(progress(1L, TENANT_A, DISCIPLE, 1L, 3, 3))));
            when(progressRepository.countByCurrentStage(TENANT_A, 1L)).thenReturn(List.of());
            when(stageRepository.findByTenantIdAndJourneyIdOrderByOrderAsc(TENANT_A, 1L))
                    .thenReturn(List.of(stage(1L, TENANT_A, 1L, 1, "Fondations")));
            when(assignmentRepository.findByTenantIdAndJourneyId(any(), eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            Map<String, Object> report = service.getReport(TENANT_A, 1L);

            // Ces clés sont `required` dans DiscipleshipReport.fromJson : sans
            // elles, le client échoue à désérialiser.
            assertThat(report).containsKeys(
                    "journeyId", "journeyName", "totalDisciples", "activeDisciples",
                    "completedDisciples", "stalledDisciples", "averageCompletion",
                    "totalMeetings", "completedMeetings", "stageDistribution",
                    "statusDistribution", "topMentors", "generatedAt");
            assertThat(report).containsEntry("journeyName", "Grandissement");
            assertThat(report).containsEntry("averageCompletion", 100.0d);
            assertThat(report).containsEntry("totalMeetings", 3L);
            assertThat(report).containsEntry("completedMeetings", 2L);
        }

        @Test
        @DisplayName("getTopMentors agrège réellement par mentor au lieu de renvoyer des compteurs en dur")
        void topMentors_aggregatesByMentor() {
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            MentorAssignment a1 = assignment(1L, MENTOR, TENANT_A, 1L, MentorAssignment.AssignmentStatus.ACTIVE);
            MentorAssignment a2 = assignment(2L, MENTOR, TENANT_A, 1L, MentorAssignment.AssignmentStatus.ACTIVE);
            when(assignmentRepository.findByTenantIdAndJourneyId(any(), eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(a1, a2)));
            when(meetingRepository.countByTenantIdAndAssignmentId(TENANT_A, 1L)).thenReturn(4L);
            when(meetingRepository.countByTenantIdAndAssignmentId(TENANT_A, 2L)).thenReturn(1L);
            userIn(TENANT_A, MENTOR, "Pasteur", "Jean");

            List<Map<String, Object>> top = service.getTopMentors(TENANT_A, 1L, 10);

            // Deux assignations du MÊME mentor => UNE ligne, avec le cumul réel.
            assertThat(top).hasSize(1);
            assertThat(top.get(0))
                    .containsEntry("mentorId", MENTOR)
                    .containsEntry("discipleCount", 2)
                    .containsEntry("meetingCount", 5)
                    .containsEntry("mentorName", "Pasteur Jean");
        }

        @Test
        @DisplayName("getTopMentors ignore les assignations terminées dans le décompte des disciples")
        void topMentors_excludesEndedAssignments() {
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            when(assignmentRepository.findByTenantIdAndJourneyId(any(), eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(
                            assignment(1L, MENTOR, TENANT_A, 1L, MentorAssignment.AssignmentStatus.ENDED))));
            userIn(TENANT_A, MENTOR, "Pasteur", "Jean");

            List<Map<String, Object>> top = service.getTopMentors(TENANT_A, 1L, 10);

            assertThat(top).hasSize(1);
            assertThat(top.get(0)).containsEntry("discipleCount", 0);
        }

        @Test
        @DisplayName("getTopMentors borne le limit reçu du client")
        void topMentors_clampsLimit() {
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            when(assignmentRepository.findByTenantIdAndJourneyId(any(), eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            // Un limit absurde ne doit pas produire une requête hors borne.
            service.getTopMentors(TENANT_A, 1L, 100_000);
            verify(assignmentRepository).findByTenantIdAndJourneyId(eq(TENANT_A), eq(1L), any(Pageable.class));

            // Un limit négatif ne doit pas faire planter PageRequest.
            assertThat(service.getTopMentors(TENANT_A, 1L, -5)).isEmpty();
        }
    }

    // ==================== CONTRAT MOBILE (R7) ====================

    @Nested
    @DisplayName("Charge utile conforme au modèle mobile")
    class MobileContract {

        @Test
        @DisplayName("progressView expose les libellés required et une MAP de requirements")
        void progressView_exposesRequiredDisplayFields() {
            DiscipleProgress p = progress(5L, TENANT_A, DISCIPLE, 1L, 1, 3);
            p.setCurrentStageId(10L);
            when(progressRepository.findByTenantIdAndId(TENANT_A, 5L)).thenReturn(Optional.of(p));
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            when(stageRepository.findByTenantIdAndId(TENANT_A, 10L))
                    .thenReturn(Optional.of(stage(10L, TENANT_A, 1L, 2, "Servir")));
            when(progressRequirementRepository.findByTenantIdAndProgressId(TENANT_A, 5L))
                    .thenReturn(List.of());
            userIn(TENANT_A, DISCIPLE, "Jean", "Bonheur");

            Map<String, Object> view = service.getProgress(TENANT_A, 5L);

            assertThat(view).containsKeys("discipleName", "journeyName",
                    "currentStageName", "currentStageOrder");
            assertThat(view).containsEntry("discipleName", "Jean Bonheur");
            assertThat(view).containsEntry("journeyName", "Croissance");
            assertThat(view).containsEntry("currentStageName", "Servir");
            assertThat(view).containsEntry("currentStageOrder", 2);
            // Le modèle mobile attend une Map<int, RequirementProgress>, pas une liste.
            assertThat(view.get("requirementProgress")).isInstanceOf(Map.class);
        }

        @Test
        @DisplayName("progressView renvoie currentStageId à 0 (jamais null) pour un parcours non démarré")
        void progressView_neverReturnsNullStageId() {
            when(progressRepository.findByTenantIdAndId(TENANT_A, 5L))
                    .thenReturn(Optional.of(progress(5L, TENANT_A, DISCIPLE, 1L, 0, 3)));
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            when(progressRequirementRepository.findByTenantIdAndProgressId(TENANT_A, 5L))
                    .thenReturn(List.of());
            userIn(TENANT_A, DISCIPLE, "Jean", "Bonheur");

            Map<String, Object> view = service.getProgress(TENANT_A, 5L);

            // `required int currentStageId` côté mobile : null ferait échouer le fromJson.
            assertThat(view.get("currentStageId")).isEqualTo(0L);
            assertThat(view).containsEntry("currentStageName", "");
        }

        @Test
        @DisplayName("assignmentView et meetingView exposent mentorName/discipleName")
        void views_exposeUserNames() {
            when(assignmentRepository.findByTenantIdAndId(TENANT_A, 1L)).thenReturn(Optional.of(
                    assignment(1L, MENTOR, TENANT_A, 1L, MentorAssignment.AssignmentStatus.ACTIVE)));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            userIn(TENANT_A, MENTOR, "Pasteur", "Jean");
            userIn(TENANT_A, DISCIPLE, "Jean", "Bonheur");

            Map<String, Object> assignmentView = service.endAssignment(TENANT_A, 1L);

            assertThat(assignmentView).containsEntry("mentorName", "Pasteur Jean");
            assertThat(assignmentView).containsEntry("discipleName", "Jean Bonheur");
        }
    }

    // ==================== RÈGLES MÉTIER ====================

    @Nested
    @DisplayName("Règles métier")
    class BusinessRules {

        @Test
        @DisplayName("deux progressions pour le même (disciple, parcours) sont refusées")
        void createProgress_rejectsDuplicate() {
            when(journeyRepository.findByTenantIdAndId(TENANT_A, 1L))
                    .thenReturn(Optional.of(journey(1L, TENANT_A, "Croissance")));
            userIn(TENANT_A, DISCIPLE, "Jean", "Bonheur");
            when(progressRepository.findByTenantIdAndDiscipleIdAndJourneyId(TENANT_A, DISCIPLE, 1L))
                    .thenReturn(Optional.of(progress(3L, TENANT_A, DISCIPLE, 1L, 1, 3)));

            assertThatThrownBy(() -> service.createProgress(TENANT_A,
                    Map.of("discipleId", DISCIPLE.toString(), "journeyId", 1L)))
                    .isInstanceOf(BusinessRuleException.class);
            verify(progressRepository, org.mockito.Mockito.never()).save(any());
        }

        @Test
        @DisplayName("un identifiant d'utilisateur mal formé renvoie 400 et non 500")
        void createProgress_rejectsMalformedUuid() {
            assertThatThrownBy(() -> service.createProgress(TENANT_A,
                    Map.of("discipleId", "pas-un-uuid", "journeyId", 1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Identifiant invalide");
        }

        @Test
        @DisplayName("un nom de parcours vide est refusé")
        void createJourney_rejectsBlankName() {
            assertThatThrownBy(() -> service.createJourney(TENANT_A, Map.of("name", "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("un type de parcours inconnu est refusé au lieu d'être.recordé en CUSTOM")
        void createJourney_rejectsUnknownType() {
            assertThatThrownBy(() -> service.createJourney(TENANT_A,
                    Map.of("name", "Parcours", "type", "INCONNU")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("terminer une assignation passe le statut à ENDED et horodate la fin")
        void endAssignment_marksEnded() {
            MentorAssignment a = assignment(1L, MENTOR, TENANT_A, 1L, MentorAssignment.AssignmentStatus.ACTIVE);
            when(assignmentRepository.findByTenantIdAndId(TENANT_A, 1L)).thenReturn(Optional.of(a));
            when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            userIn(TENANT_A, MENTOR, "Pasteur", "Jean");
            userIn(TENANT_A, DISCIPLE, "Jean", "Bonheur");

            Map<String, Object> view = service.endAssignment(TENANT_A, 1L);

            assertThat(view).containsEntry("status", "ENDED");
            assertThat(view.get("endedAt")).isNotNull();
        }
    }

    private MentorAssignment assignment(Long id, UUID mentorId, UUID tenantId, Long journeyId,
                                        MentorAssignment.AssignmentStatus status) {
        return MentorAssignment.builder()
                .id(id).tenantId(tenantId).mentorId(mentorId)
                .discipleId(DISCIPLE).journeyId(journeyId)
                .assignedAt(Instant.parse("2026-02-01T00:00:00Z"))
                .meetingFrequencyDays(7)
                .status(status)
                .build();
    }
}
