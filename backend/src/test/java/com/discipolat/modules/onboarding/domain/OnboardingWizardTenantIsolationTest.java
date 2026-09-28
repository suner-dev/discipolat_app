package com.discipolat.modules.onboarding.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.multitenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat B2 — isolation inter-tenant du wizard.
 *
 * <p>Prouve qu'un tenant ne peut <b>ni lire, ni démarrer, ni compléter, ni
 * sauter</b> une étape d'un autre tenant, et qu'il ne peut pas écrire dans le
 * wizard d'autrui.
 *
 * <p>Avant le correctif, `wizardRepo.findById(stepId).orElseThrow()` ne
 * vérifiait <b>pas</b> le {@code tenantId} : un admin du tenant B pouvait
 * compléter une étape du tenant A (IDOR). Le test reproduit exactement ce
 * scénario et exige un 404 identique à celui d'un id inexistant, pour ne pas
 * révéler l'existence de l'étape d'autrui.
 */
@ExtendWith(MockitoExtension.class)
class OnboardingWizardTenantIsolationTest {

    @Mock
    private OnboardingWizardRepository wizardRepo;
    @Mock
    private OnboardingStepActions stepActions;
    @Mock
    private TenantOnboardingStatusPort statusPort;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OnboardingWizardService service;
    private UUID tenantA;
    private UUID tenantB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        tenantB = UUID.randomUUID();
        // `SecurityUtils.getCurrentUserId()` est statique : on passe par le
        // SecurityContext (cf. SecurityTestHelper), pas par un mock.
        SecurityTestHelper.loginAs(UUID.randomUUID());
        service = new OnboardingWizardService(wizardRepo, stepActions, objectMapper,
                portProvider(statusPort),
                portProvider(org.mockito.Mockito.mock(com.discipolat.modules.tenants.domain.TenantService.class)),
                new com.discipolat.common.infrastructure.security.SecurityUtils(null));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityTestHelper.logout();
    }

    @Test
    @DisplayName("Le tenant B ne peut PAS compléter une étape du tenant A (404 STEP_NOT_FOUND)")
    void tenantBCannotCompleteStepOfTenantA() {
        OnboardingWizardStep stepOfA = newStep(tenantA, OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0);
        asTenant(tenantB);
        when(wizardRepo.findById(stepOfA.getId())).thenReturn(Optional.of(stepOfA));

        assertThatThrownBy(() -> service.completeStep(stepOfA.getId(), Map.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                    assertThat(problem.getTitle()).isEqualTo("STEP_NOT_FOUND");
                });

        verify(stepActions, never()).execute(any(), any(), any());
        verify(wizardRepo, never()).save(any());
    }

    @Test
    @DisplayName("Le tenant B ne peut PAS démarrer une étape du tenant A")
    void tenantBCannotStartStepOfTenantA() {
        OnboardingWizardStep stepOfA = newStep(tenantA, OnboardingWizardStep.StepType.ROLES, 3);
        asTenant(tenantB);
        when(wizardRepo.findById(stepOfA.getId())).thenReturn(Optional.of(stepOfA));

        assertThatThrownBy(() -> service.startStep(stepOfA.getId()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_NOT_FOUND"));
        verify(wizardRepo, never()).save(any());
    }

    @Test
    @DisplayName("Le tenant B ne peut PAS sauter une étape du tenant A")
    void tenantBCannotSkipStepOfTenantA() {
        OnboardingWizardStep stepOfA = newStep(tenantA, OnboardingWizardStep.StepType.MEMBER_IMPORT, 1);
        asTenant(tenantB);
        when(wizardRepo.findById(stepOfA.getId())).thenReturn(Optional.of(stepOfA));

        assertThatThrownBy(() -> service.skipStep(stepOfA.getId(), "raison"))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_NOT_FOUND"));
        verify(wizardRepo, never()).save(any());
    }

    @Test
    @DisplayName("Un id inexistant et une étape d'autrui produisent EXACTEMENT le même refus")
    void unknownStepAndForeignStepAreIndistinguishable() {
        OnboardingWizardStep foreignStep = newStep(tenantA, OnboardingWizardStep.StepType.BRANDING, 4);
        UUID unknownStep = UUID.randomUUID();
        asTenant(tenantB);
        when(wizardRepo.findById(foreignStep.getId())).thenReturn(Optional.of(foreignStep));
        when(wizardRepo.findById(unknownStep)).thenReturn(Optional.empty());

        String foreignCode = null;
        String unknownCode = null;
        int foreignStatus = 0;
        int unknownStatus = 0;
        try {
            service.completeStep(foreignStep.getId(), Map.of());
        } catch (DomainException thrown) {
            foreignCode = thrown.toProblemDetail().getTitle();
            foreignStatus = thrown.toProblemDetail().getStatus();
        }
        try {
            service.completeStep(unknownStep, Map.of());
        } catch (DomainException thrown) {
            unknownCode = thrown.toProblemDetail().getTitle();
            unknownStatus = thrown.toProblemDetail().getStatus();
        }

        assertThat(foreignCode).isEqualTo(unknownCode).isEqualTo("STEP_NOT_FOUND");
        assertThat(foreignStatus).isEqualTo(unknownStatus).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Le tenant B ne voit QUE ses propres étapes (lecture)")
    void tenantBSeesOnlyItsOwnSteps() {
        asTenant(tenantB);
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantB))
                .thenReturn(List.of(newStep(tenantB, OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0)));

        var visible = service.getSteps();

        assertThat(visible).hasSize(1);
        assertThat(visible.get(0).stepType()).isEqualTo("CHURCH_IDENTITY");
        // La requête est systématiquement bornée au tenant courant.
        verify(wizardRepo, never()).findByTenantIdOrderByStepOrderAsc(tenantA);
    }

    @Test
    @DisplayName("L'action métier reçoit le tenant du contexte, jamais celui de l'étape")
    void businessActionAlwaysReceivesTheCurrentTenant() {
        OnboardingWizardStep stepOfB = newStep(tenantB, OnboardingWizardStep.StepType.BRANDING, 4);
        asTenant(tenantB);
        when(wizardRepo.findById(stepOfB.getId())).thenReturn(Optional.of(stepOfB));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantB)).thenReturn(List.of(stepOfB));
        when(wizardRepo.save(any(OnboardingWizardStep.class))).thenAnswer(i -> i.getArgument(0));
        when(stepActions.execute(any(), any(), any())).thenReturn(Map.of("ok", true));

        service.completeStep(stepOfB.getId(), Map.of());

        verify(stepActions).execute(eq(OnboardingWizardStep.StepType.BRANDING), eq(tenantB), any());
    }

    @Test
    @DisplayName("Le contrôle d'ordre ne consulte QUE les étapes du tenant courant")
    void orderCheckOnlyConsultsTheCurrentTenantSteps() {
        // Les 4 premières étapes du tenant A sont PENDING : si le contrôle
        // d'ordre les prenait en compte, l'étape ROLES du tenant B serait bloquée.
        List<OnboardingWizardStep> foreignSteps = new ArrayList<>();
        foreignSteps.add(newStep(tenantA, OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0));
        foreignSteps.add(newStep(tenantA, OnboardingWizardStep.StepType.MEMBER_IMPORT, 1));
        foreignSteps.add(newStep(tenantA, OnboardingWizardStep.StepType.STRUCTURE, 2));

        OnboardingWizardStep rolesOfB = newStep(tenantB, OnboardingWizardStep.StepType.ROLES, 3);
        List<OnboardingWizardStep> ownSteps = new ArrayList<>();
        ownSteps.add(newStep(tenantB, OnboardingWizardStep.StepType.CHURCH_IDENTITY, 0));
        ownSteps.add(newStep(tenantB, OnboardingWizardStep.StepType.MEMBER_IMPORT, 1));
        ownSteps.add(newStep(tenantB, OnboardingWizardStep.StepType.STRUCTURE, 2));
        ownSteps.get(0).setStatus(OnboardingWizardStep.Status.COMPLETED);
        ownSteps.get(1).setStatus(OnboardingWizardStep.Status.SKIPPED);
        ownSteps.get(2).setStatus(OnboardingWizardStep.Status.COMPLETED);
        ownSteps.add(rolesOfB);

        asTenant(tenantB);
        when(wizardRepo.findById(rolesOfB.getId())).thenReturn(Optional.of(rolesOfB));
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantB)).thenReturn(ownSteps);
        when(wizardRepo.save(any(OnboardingWizardStep.class))).thenAnswer(i -> i.getArgument(0));
        when(stepActions.execute(any(), any(), any())).thenReturn(Map.of("ok", true));

        assertThat(service.completeStep(rolesOfB.getId(), Map.of()).status()).isEqualTo("COMPLETED");

        // Aucune requête sur les étapes de l'autre tenant, et les siennes ne sont
        // pas modifiées.
        verify(wizardRepo, never()).findByTenantIdOrderByStepOrderAsc(tenantA);
        for (OnboardingWizardStep foreign : foreignSteps) {
            verify(wizardRepo, never()).findById(foreign.getId());
        }
    }

    // ---------- helpers ----------

    private void asTenant(UUID tenantId) {
        TenantContext.setTenantId(tenantId);
    }

    /** Crée une étape APPARTENANT au tenant `owner` et renvoie son identifiant. */
    private UUID stepOf(UUID owner, OnboardingWizardStep.StepType type, int order) {
        return newStep(owner, type, order).getId();
    }

    private OnboardingWizardStep newStep(UUID owner, OnboardingWizardStep.StepType type, int order) {
        OnboardingWizardStep step = new OnboardingWizardStep();
        step.setId(UUID.randomUUID());
        step.setTenantId(owner);
        step.setStepType(type);
        step.setStepOrder(order);
        step.setStatus(OnboardingWizardStep.Status.PENDING);
        return step;
    }

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
}
