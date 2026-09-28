package com.discipolat.modules.onboarding.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.onboarding.api.OnboardingProgressResponse;
import com.discipolat.modules.onboarding.api.OnboardingStatusResponse;
import com.discipolat.modules.onboarding.api.OnboardingStepResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat B2 — contrat figé §3.1, ordre, idempotence, skip, complétion globale et
 * appel d'action métier réel.
 */
@ExtendWith(MockitoExtension.class)
class OnboardingWizardServiceTest {

    @Mock
    private OnboardingWizardRepository wizardRepo;
    @Mock
    private OnboardingStepActions stepActions;
    @Mock
    private TenantOnboardingStatusPort statusPort;
    @Mock
    private com.discipolat.modules.tenants.domain.TenantService tenantService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OnboardingWizardService service;
    private UUID tenantId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        // `SecurityUtils.getCurrentUserId()` est STATIQUE : un mock Mockito ne
        // l'intercepte pas (cf. SecurityTestHelper : « Remplace le mock de
        // SecurityUtils qui ne fonctionne pas pour les méthodes statiques »).
        SecurityTestHelper.loginAs(actorId);
        service = new OnboardingWizardService(wizardRepo, stepActions, objectMapper,
                portProvider(statusPort), portProvider(tenantService),
                new com.discipolat.common.infrastructure.security.SecurityUtils(null));
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityTestHelper.logout();
    }

    // ==================================================================
    // Contrat DTO (§3.1)
    // ==================================================================

    @Test
    void getSteps_returnsTheSevenCanonicalStepsWithExactContractFields() {
        givenPersistedSteps();

        List<OnboardingStepResponse> steps = service.getSteps();

        assertThat(steps).hasSize(7);
        assertThat(steps).extracting(OnboardingStepResponse::stepType).containsExactly(
                "CHURCH_IDENTITY", "MEMBER_IMPORT", "STRUCTURE", "ROLES",
                "BRANDING", "MODULES", "FIRST_EVENT");
        assertThat(steps).extracting(OnboardingStepResponse::stepOrder)
                .containsExactly(0, 1, 2, 3, 4, 5, 6);
        assertThat(steps).extracting(OnboardingStepResponse::title).containsExactly(
                "Identité de l'église",
                "Import des membres",
                "Familles et départements",
                "Inviter les responsables",
                "Identité visuelle",
                "Modules activés",
                "Premier événement");

        // Exactement les champs du contrat, et rien de plus.
        assertThat(steps.get(0).id()).isNotNull();
        assertThat(steps.get(0).status()).isEqualTo("PENDING");
        assertThat(steps.get(0).isCompleted()).isFalse();
        assertThat(steps.get(0).isSkippable()).isFalse();
        assertThat(steps.get(0).skipRequiresReason()).isFalse();
        assertThat(steps.get(0).startedAt()).isNull();
        assertThat(steps.get(0).completedAt()).isNull();
        assertThat(steps.get(0).completedData()).isNull();
        assertThat(steps.get(0).description()).isNotBlank();

        // MEMBER_IMPORT est skippable AVEC motif ; BRANDING skippable SANS motif.
        assertThat(steps.get(1).isSkippable()).isTrue();
        assertThat(steps.get(1).skipRequiresReason()).isTrue();
        assertThat(steps.get(4).isSkippable()).isTrue();
        assertThat(steps.get(4).skipRequiresReason()).isFalse();
    }

    @Test
    void getSteps_deserializesCompletedDataIntoAnObjectNotAString() {
        OnboardingWizardStep step = step(OnboardingWizardStep.StepType.BRANDING, 4,
                OnboardingWizardStep.Status.COMPLETED);
        step.setCompletedData("{\"primaryColor\":\"#112233\",\"createdIds\":[\"a1\"]}");
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(step));

        OnboardingStepResponse response = service.getSteps().get(0);

        assertThat(response.completedData()).isInstanceOf(Map.class);
        assertThat(response.completedData()).containsEntry("primaryColor", "#112233");
        assertThat(response.isCompleted()).isTrue();
    }

    @Test
    void getSteps_neverLeaksTheLegacyConfigColumn() {
        OnboardingWizardStep step = step(OnboardingWizardStep.StepType.BRANDING, 4,
                OnboardingWizardStep.Status.PENDING);
        step.setConfig("{\"secret\":\"legacy\"}");
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(step));

        assertThat(service.getSteps().get(0).completedData()).isNull();
    }

    // ==================================================================
    // Ordre et idempotence
    // ==================================================================

    @Test
    void completeStep_rejectsAStepOutOfOrderWith409() {
        OnboardingWizardStep pending = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.PENDING);
        OnboardingWizardStep roles = step(OnboardingWizardStep.StepType.ROLES, 3,
                OnboardingWizardStep.Status.PENDING);
        when(wizardRepo.findById(roles.getId())).thenReturn(java.util.Optional.of(roles));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(pending, roles));

        assertThatThrownBy(() -> service.completeStep(roles.getId(), Map.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    DomainException domain = (DomainException) thrown;
                    ProblemDetail problem = domain.toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(problem.getTitle()).isEqualTo("STEP_ORDER_VIOLATION");
                });

        verify(stepActions, never()).execute(any(), any(), any());
        verify(wizardRepo, never()).save(any());
    }

    @Test
    void completeStep_rejectsAnAlreadyCompletedStepWith409() {
        OnboardingWizardStep completed = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.COMPLETED);
        when(wizardRepo.findById(completed.getId())).thenReturn(java.util.Optional.of(completed));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of(completed));

        assertThatThrownBy(() -> service.completeStep(completed.getId(), Map.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_ALREADY_COMPLETED"));

        verify(stepActions, never()).execute(any(), any(), any());
    }

    @Test
    void completeStep_acceptsAnAlreadySkippedStepPath() {
        // Une étape SKIPPED n'est pas « complétée » : on ne la rejoue pas.
        OnboardingWizardStep skipped = step(OnboardingWizardStep.StepType.MEMBER_IMPORT, 1,
                OnboardingWizardStep.Status.SKIPPED);
        OnboardingWizardStep first = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.COMPLETED);
        when(wizardRepo.findById(skipped.getId())).thenReturn(java.util.Optional.of(skipped));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(first, skipped));

        assertThatThrownBy(() -> service.completeStep(skipped.getId(), Map.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_ALREADY_COMPLETED"));
    }

    // ==================================================================
    // Action métier réelle
    // ==================================================================

    @Test
    void completeStep_delegatesToTheRealBusinessActionAndStoresItsOutcome() {
        OnboardingWizardStep branding = step(OnboardingWizardStep.StepType.BRANDING, 4,
                OnboardingWizardStep.Status.IN_PROGRESS);
        when(wizardRepo.findById(branding.getId())).thenReturn(java.util.Optional.of(branding));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of(allSettledBefore(4), branding));
        Map<String, Object> outcome = Map.of("primaryColor", "#AABBCC", "createdIds", List.of());
        when(stepActions.execute(OnboardingWizardStep.StepType.BRANDING, tenantId, Map.of("primaryColor", "#AABBCC")))
                .thenReturn(outcome);
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingStepResponse response = service.completeStep(
                branding.getId(), Map.of("primaryColor", "#AABBCC"));

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.isCompleted()).isTrue();
        assertThat(response.completedData()).containsEntry("primaryColor", "#AABBCC");
        assertThat(response.completedAt()).isNotNull();
        verify(stepActions, times(1)).execute(
                OnboardingWizardStep.StepType.BRANDING, tenantId, Map.of("primaryColor", "#AABBCC"));
    }

    @Test
    void completeStep_withNoBodyAtAllSucceedsForAStepThatNeedsNoData() {
        OnboardingWizardStep churchIdentity = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.PENDING);
        when(wizardRepo.findById(churchIdentity.getId())).thenReturn(java.util.Optional.of(churchIdentity));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of(churchIdentity));
        when(stepActions.execute(OnboardingWizardStep.StepType.CHURCH_IDENTITY, tenantId, Map.of()))
                .thenReturn(Map.of("churchName", "Église Bethel"));
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Décision D7 : corps absent ≡ corps vide.
        OnboardingStepResponse response = service.completeStep(churchIdentity.getId(), Map.of());

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedData()).containsEntry("churchName", "Église Bethel");
    }

    @Test
    void completeStep_propagatesStepDataInvalidWithoutSavingAnything() {
        OnboardingWizardStep branding = step(OnboardingWizardStep.StepType.BRANDING, 4,
                OnboardingWizardStep.Status.PENDING);
        when(wizardRepo.findById(branding.getId())).thenReturn(java.util.Optional.of(branding));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of(allSettledBefore(4), branding));
        when(stepActions.execute(any(), any(), any())).thenThrow(new DomainException(
                "Données d'étape invalides", HttpStatus.BAD_REQUEST, "STEP_DATA_INVALID",
                Map.of("primaryColor", "format #RRGGBB attendu")));

        assertThatThrownBy(() -> service.completeStep(branding.getId(), Map.of("primaryColor", "rouge")))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_DATA_INVALID"));

        // Aucune écriture : l'étape n'est pas marquée complétée à tort.
        verify(wizardRepo, never()).save(any());
    }

    // ==================================================================
    // Skip
    // ==================================================================

    @Test
    void skipStep_requiresAReasonWhenTheStepDemandsOne() {
        OnboardingWizardStep import_ = step(OnboardingWizardStep.StepType.MEMBER_IMPORT, 1,
                OnboardingWizardStep.Status.PENDING);
        OnboardingWizardStep first = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.COMPLETED);
        when(wizardRepo.findById(import_.getId())).thenReturn(java.util.Optional.of(import_));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(first, import_));

        assertThatThrownBy(() -> service.skipStep(import_.getId(), null))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    ProblemDetail problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(problem.getTitle()).isEqualTo("STEP_SKIP_REASON_REQUIRED");
                });
        verify(wizardRepo, never()).save(any());
    }

    @Test
    void skipStep_rejectsANonSkippableStepWith409() {
        OnboardingWizardStep churchIdentity = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.PENDING);
        when(wizardRepo.findById(churchIdentity.getId())).thenReturn(java.util.Optional.of(churchIdentity));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(churchIdentity));

        assertThatThrownBy(() -> service.skipStep(churchIdentity.getId(), "je ne veux pas"))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_NOT_SKIPPABLE"));
        verify(wizardRepo, never()).save(any());
    }

    @Test
    void skipStep_storesTheReasonAndMarksTheStepSkipped() {
        OnboardingWizardStep import_ = step(OnboardingWizardStep.StepType.MEMBER_IMPORT, 1,
                OnboardingWizardStep.Status.PENDING);
        OnboardingWizardStep first = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.COMPLETED);
        when(wizardRepo.findById(import_.getId())).thenReturn(java.util.Optional.of(import_));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(first, import_));
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingStepResponse response = service.skipStep(import_.getId(), "  Import hors ligne  ");

        assertThat(response.status()).isEqualTo("SKIPPED");
        assertThat(response.isCompleted()).isTrue();
        assertThat(response.completedData()).containsEntry("skipReason", "Import hors ligne");
    }

    @Test
    void skipStep_allowsSkippingWithoutReasonWhenTheStepDoesNotRequireIt() {
        OnboardingWizardStep branding = step(OnboardingWizardStep.StepType.BRANDING, 4,
                OnboardingWizardStep.Status.PENDING);
        when(wizardRepo.findById(branding.getId())).thenReturn(java.util.Optional.of(branding));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of(allSettledBefore(4), branding));
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.skipStep(branding.getId(), null)).doesNotThrowAnyException();
    }

    // ==================================================================
    // Start
    // ==================================================================

    @Test
    void startStep_movesPendingToInProgress() {
        OnboardingWizardStep churchIdentity = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.PENDING);
        when(wizardRepo.findById(churchIdentity.getId())).thenReturn(java.util.Optional.of(churchIdentity));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(churchIdentity));
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OnboardingStepResponse response = service.startStep(churchIdentity.getId());

        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.startedAt()).isNotNull();
    }

    @Test
    void startStep_rejectsAStartedStepOutOfOrder() {
        OnboardingWizardStep pending = step(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0,
                OnboardingWizardStep.Status.PENDING);
        OnboardingWizardStep structure = step(OnboardingWizardStep.StepType.STRUCTURE, 2,
                OnboardingWizardStep.Status.PENDING);
        when(wizardRepo.findById(structure.getId())).thenReturn(java.util.Optional.of(structure));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of(pending, structure));

        assertThatThrownBy(() -> service.startStep(structure.getId()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_ORDER_VIOLATION"));
    }

    // ==================================================================
    // Progression & statut
    // ==================================================================

    @Test
    void getProgress_isCompleteOnlyWhenEveryStepIsCompletedOrSkipped() {
        List<OnboardingWizardStep> steps = new ArrayList<>();
        steps.add(completedStep(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0));
        steps.add(skippedStep(OnboardingWizardStep.StepType.MEMBER_IMPORT, 1));
        steps.add(completedStep(OnboardingWizardStep.StepType.STRUCTURE, 2));
        steps.add(completedStep(OnboardingWizardStep.StepType.ROLES, 3));
        steps.add(completedStep(OnboardingWizardStep.StepType.BRANDING, 4));
        steps.add(completedStep(OnboardingWizardStep.StepType.MODULES, 5));
        steps.add(completedStep(OnboardingWizardStep.StepType.FIRST_EVENT, 6));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(steps);

        OnboardingProgressResponse progress = service.getProgress();

        assertThat(progress.totalSteps()).isEqualTo(7);
        // `completedSteps` ne compte QUE les étapes COMPLETED : une étape SKIPPED
        // est comptée dans `skippedSteps`, jamais dans `completedSteps`
        // (contrat §3.1 : completedSteps=2, skippedSteps=1, percentage=43).
        assertThat(progress.completedSteps()).isEqualTo(6);
        assertThat(progress.skippedSteps()).isEqualTo(1);
        assertThat(progress.percentage()).isEqualTo(100);
        assertThat(progress.isComplete()).isTrue();
        assertThat(progress.steps()).hasSize(7);
    }

    @Test
    void getProgress_percentageIsRoundedAndCountsSkippedSteps() {
        // 1 complétée + 1 sautée sur 7 = 2/7 = 28.57 % -> 29 (arrondi du contrat).
        // C'est exactement l'exemple du contrat §3.1 : 2 + 1 sur 7 -> 43 %.
        List<OnboardingWizardStep> steps = new ArrayList<>();
        steps.add(completedStep(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0));
        steps.add(completedStep(OnboardingWizardStep.StepType.MEMBER_IMPORT, 1));
        steps.add(skippedStep(OnboardingWizardStep.StepType.STRUCTURE, 2));
        steps.add(pendingStep(OnboardingWizardStep.StepType.ROLES, 3));
        steps.add(pendingStep(OnboardingWizardStep.StepType.BRANDING, 4));
        steps.add(pendingStep(OnboardingWizardStep.StepType.MODULES, 5));
        steps.add(pendingStep(OnboardingWizardStep.StepType.FIRST_EVENT, 6));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(steps);

        OnboardingProgressResponse progress = service.getProgress();

        assertThat(progress.completedSteps()).isEqualTo(2);
        assertThat(progress.skippedSteps()).isEqualTo(1);
        assertThat(progress.percentage()).isEqualTo(43);
        assertThat(progress.isComplete()).isFalse();
    }

    @Test
    void getProgress_isNotCompleteWhileAStepRemains() {
        List<OnboardingWizardStep> steps = new ArrayList<>();
        steps.add(completedStep(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0));
        steps.add(pendingStep(OnboardingWizardStep.StepType.MEMBER_IMPORT, 1));
        steps.add(pendingStep(OnboardingWizardStep.StepType.STRUCTURE, 2));
        steps.add(pendingStep(OnboardingWizardStep.StepType.ROLES, 3));
        steps.add(pendingStep(OnboardingWizardStep.StepType.BRANDING, 4));
        steps.add(pendingStep(OnboardingWizardStep.StepType.MODULES, 5));
        steps.add(pendingStep(OnboardingWizardStep.StepType.FIRST_EVENT, 6));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(steps);

        OnboardingProgressResponse progress = service.getProgress();

        assertThat(progress.isComplete()).isFalse();
        assertThat(progress.percentage()).isEqualTo(14);
    }

    @Test
    void getStatus_reportsNotCompletedWhenTheTenantColumnIsAbsent() {
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(allSevenPending());
        when(statusPort.completedAtOf(tenantId)).thenReturn(null);
        when(statusPort.completedByOf(tenantId)).thenReturn(null);

        OnboardingStatusResponse status = service.getStatus();

        assertThat(status.completed()).isFalse();
        assertThat(status.completedAt()).isNull();
        assertThat(status.completedBy()).isNull();
        assertThat(status.totalSteps()).isEqualTo(7);
        assertThat(status.percentage()).isZero();
    }

    @Test
    void getStatus_reportsCompletionWithActorWhenColumnsAreSet() {
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(allSevenSettled());
        UUID actor = UUID.randomUUID();
        java.time.Instant finishedAt = java.time.Instant.parse("2026-09-27T12:00:00Z");
        when(statusPort.completedAtOf(tenantId)).thenReturn(finishedAt);
        when(statusPort.completedByOf(tenantId)).thenReturn(actor);

        OnboardingStatusResponse status = service.getStatus();

        assertThat(status.completed()).isTrue();
        assertThat(status.completedAt()).isEqualTo(finishedAt);
        assertThat(status.completedBy()).isEqualTo(actor.toString());
        assertThat(status.percentage()).isEqualTo(100);
    }

    // ==================================================================
    // IDOR : aucun orElseThrow nu
    // ==================================================================

    @Test
    void everyUnknownStepIdIsRejectedWith404StepNotFound() {
        UUID unknown = UUID.randomUUID();
        when(wizardRepo.findById(unknown)).thenReturn(java.util.Optional.empty());

        for (var call : List.<Runnable>of(
                () -> service.startStep(unknown),
                () -> service.completeStep(unknown, Map.of()),
                () -> service.skipStep(unknown, "raison"))) {
            assertThatThrownBy(call::run)
                    .isInstanceOf(DomainException.class)
                    .satisfies(thrown -> {
                        ProblemDetail problem = ((DomainException) thrown).toProblemDetail();
                        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                        assertThat(problem.getTitle()).isEqualTo("STEP_NOT_FOUND");
                    });
        }
        verify(stepActions, never()).execute(any(), any(), any());
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    @Test
    void initializeSteps_isIdempotentAndDoesNotDuplicate() {
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of())
                .thenReturn(allSevenPending());
        when(wizardRepo.save(any(OnboardingWizardStep.class))).thenAnswer(invocation -> {
            OnboardingWizardStep step = invocation.getArgument(0);
            step.setId(UUID.randomUUID());
            return step;
        });

        List<OnboardingStepResponse> first = service.initializeSteps();
        List<OnboardingStepResponse> second = service.initializeSteps();

        assertThat(first).hasSize(7);
        assertThat(second).hasSize(7);
        // Le second appel relit l'existant : aucune sauvegarde supplémentaire.
        verify(wizardRepo, times(7)).save(any(OnboardingWizardStep.class));
    }

    @Test
    void initializeSteps_rereadsInsteadOfFailingOnConcurrentInsert() {
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of())
                .thenReturn(allSevenPending());
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "uk_onboarding_step_tenant_type"));

        List<OnboardingStepResponse> steps = service.initializeSteps();

        // Pas de 500 : l'index unique arbitre la course, on relit.
        assertThat(steps).hasSize(7);
    }

    // ==================================================================
    // roleTemplate (contrat §3.1 : endpoint inchangé)
    // ==================================================================

    @Test
    void roleTemplate_stillReturnsTheChecklistForEachRole() {
        assertThat(service.roleTemplate("PASTEUR")).containsEntry("role", "PASTEUR");
        assertThat(service.roleTemplate("CHEF_DE_FAMILLE")).containsEntry("totalSteps", 4);
        assertThat(service.roleTemplate("FAISEUR")).containsEntry("totalSteps", 4);
        assertThat(service.roleTemplate("MEMBRE")).containsEntry("totalSteps", 5);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private void givenPersistedSteps() {
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(List.of());
        when(wizardRepo.save(any(OnboardingWizardStep.class))).thenAnswer(invocation -> {
            OnboardingWizardStep created = invocation.getArgument(0);
            created.setId(UUID.randomUUID());
            return created;
        });
    }

    private OnboardingWizardStep allSettledBefore(int order) {
        OnboardingWizardStep settled = completedStep(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0);
        settled.setStepOrder(order - 1);
        return settled;
    }

    private OnboardingWizardStep step(OnboardingWizardStep.StepType type, int order, OnboardingWizardStep.Status status) {
        OnboardingWizardStep step = new OnboardingWizardStep();
        step.setId(UUID.randomUUID());
        step.setTenantId(tenantId);
        step.setStepType(type);
        step.setStepOrder(order);
        step.setStatus(status);
        return step;
    }

    private OnboardingWizardStep completedStep(OnboardingWizardStep.StepType type, int order) {
        return step(type, order, OnboardingWizardStep.Status.COMPLETED);
    }

    private OnboardingWizardStep skippedStep(OnboardingWizardStep.StepType type, int order) {
        return step(type, order, OnboardingWizardStep.Status.SKIPPED);
    }

    private OnboardingWizardStep pendingStep(OnboardingWizardStep.StepType type, int order) {
        return step(type, order, OnboardingWizardStep.Status.PENDING);
    }

    private List<OnboardingWizardStep> allSevenPending() {
        List<OnboardingWizardStep> steps = new ArrayList<>();
        int order = 0;
        for (OnboardingStepDefinition definition : OnboardingStepDefinition.CANONICAL_ORDER) {
            steps.add(pendingStep(definition.stepType(), order++));
        }
        return steps;
    }

    private List<OnboardingWizardStep> allSevenSettled() {
        List<OnboardingWizardStep> steps = new ArrayList<>();
        int order = 0;
        for (OnboardingStepDefinition definition : OnboardingStepDefinition.CANONICAL_ORDER) {
            steps.add(completedStep(definition.stepType(), order++));
        }
        return steps;
    }

    /**
     * Implémentation réelle de {@link ObjectProvider} : évite un mock qui
     *ne serait jamais consommé et que Mockito signalerait comme
     * stubbing inutile dans les tests qui n'appellent pas {@code /status}.
     */
    private static <T> ObjectProvider<T> portProvider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public java.util.Iterator<T> iterator() {
                return value == null
                        ? java.util.Collections.<T>emptyIterator()
                        : java.util.Collections.singleton(value).iterator();
            }
        };
    }

    // ==================================================================
    // A4 / D2 : l'achèvement global n'est posé qu'à la TOUTE fin
    // ==================================================================

    @Test
    @DisplayName("L'achèvement du tenant n'est marqué que lorsque les 7 étapes sont réglées")
    void globalCompletionIsMarkedOnlyWhenEveryStepIsSettled() {
        OnboardingWizardStep last = step(OnboardingWizardStep.StepType.FIRST_EVENT, 6,
                OnboardingWizardStep.Status.IN_PROGRESS);
        List<OnboardingWizardStep> allSettledButLast = allSixSettled();
        allSettledButLast.add(last);
        when(wizardRepo.findById(last.getId())).thenReturn(java.util.Optional.of(last));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(allSettledButLast);
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stepActions.execute(any(), any(), any())).thenReturn(Map.of("ok", true));

        service.completeStep(last.getId(), Map.of());

        verify(tenantService, times(1)).markOnboardingCompleted(actorId);
    }

    @Test
    @DisplayName("Une étape intermédiaire NE marque PAS l'achèvement du tenant")
    void globalCompletionIsNotMarkedWhileAStepRemains() {
        OnboardingWizardStep structure = step(OnboardingWizardStep.StepType.STRUCTURE, 2,
                OnboardingWizardStep.Status.IN_PROGRESS);
        List<OnboardingWizardStep> steps = new ArrayList<>();
        steps.add(completedStep(OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0));
        steps.add(skippedStep(OnboardingWizardStep.StepType.MEMBER_IMPORT, 1));
        steps.add(structure);
        steps.add(pendingStep(OnboardingWizardStep.StepType.ROLES, 3));
        steps.add(pendingStep(OnboardingWizardStep.StepType.BRANDING, 4));
        steps.add(pendingStep(OnboardingWizardStep.StepType.MODULES, 5));
        steps.add(pendingStep(OnboardingWizardStep.StepType.FIRST_EVENT, 6));
        when(wizardRepo.findById(structure.getId())).thenReturn(java.util.Optional.of(structure));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenReturn(steps);
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stepActions.execute(any(), any(), any())).thenReturn(Map.of("ok", true));

        service.completeStep(structure.getId(), Map.of());

        verify(tenantService, never()).markOnboardingCompleted(any());
    }

    private List<OnboardingWizardStep> allSixSettled() {
        List<OnboardingWizardStep> steps = new ArrayList<>();
        for (int order = 0; order <= 5; order++) {
            steps.add(completedStep(
                    OnboardingStepDefinition.CANONICAL_ORDER.get(order).stepType(), order));
        }
        return steps;
    }

}
