package com.discipolat.modules.onboarding.domain;

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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Constat B2 / A10.2 — deux initialisations <b>concurrentes</b> du wizard ne
 * doivent jamais créer de doublon d'étape.
 *
 * <p>La garantie vient de l'index unique {@code uk_onboarding_step_tenant_type}
 * (migration V183) : quand deux requêtes se disputent la création, l'une gagne et
 * l'autre reçoit une {@code DataIntegrityViolationException}. Le service doit
 * alors RELIRE les étapes existantes au lieu de renvoyer une erreur 500.
 *
 * <p>Ce test simule la course avec deux vrais threads et une barrière, et vérifie
 * qu'un seul jeu de 7 étapes existe au total.
 */
@ExtendWith(MockitoExtension.class)
class OnboardingWizardInitializeConcurrencyTest {

    @Mock private OnboardingWizardRepository wizardRepo;
    @Mock private OnboardingStepActions stepActions;
    @Mock private TenantOnboardingStatusPort statusPort;
    @Mock private com.discipolat.modules.tenants.domain.TenantService tenantService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Deux initialisations concurrentes ne créent qu'un seul jeu d'étapes")
    void concurrentInitializationNeverDuplicatesSteps() throws Exception {
        // Stock partagé simulant la base : les identifiants sont uniques et un
        // identifiant déjà pris déclenche la violation de l'index unique.
        List<OnboardingWizardStep> stored = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger insertAttempts = new AtomicInteger();

        // Pour FORCER la course, les deux premières lectures (une par thread)
        // renvoient « aucun étape » : c'est exactement la fenêtre Real entre le
        // SELECT et le INSERT des deux requêtes concurrentes.
        AtomicInteger reads = new AtomicInteger();
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId)).thenAnswer(invocation -> {
            if (reads.incrementAndGet() <= 2) {
                return List.of();
            }
            synchronized (stored) {
                return new ArrayList<>(stored);
            }
        });
        when(wizardRepo.save(any(OnboardingWizardStep.class))).thenAnswer(invocation -> {
            OnboardingWizardStep step = invocation.getArgument(0);
            insertAttempts.incrementAndGet();
            synchronized (stored) {
                boolean duplicate = stored.stream().anyMatch(existing ->
                        existing.getStepType() == step.getStepType());
                if (duplicate) {
                    // C'est exactement ce que produit l'index unique V183.
                    throw new DataIntegrityViolationException(
                            "uk_onboarding_step_tenant_type violated");
                }
                step.setId(UUID.randomUUID());
                stored.add(step);
            }
            return step;
        });

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Void> task = () -> {
                // Chaque thread a son propre TenantContext : le vrai contrôleur
                // applique le contexte par requête HTTP.
                TenantContext.setTenantId(tenantId);
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                    OnboardingWizardService service = new OnboardingWizardService(
                            wizardRepo, stepActions, objectMapper,
                            emptyProvider(), emptyProvider(),
                            new com.discipolat.common.infrastructure.security.SecurityUtils(null));
                    service.initializeSteps();
                } finally {
                    TenantContext.clear();
                }
                return null;
            };

            Future<Void> first = executor.submit(task);
            Future<Void> second = executor.submit(task);
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(stored)
                .as("un seul jeu de 7 étapes doit exister, malgré la course")
                .hasSize(OnboardingStepDefinition.TOTAL_STEPS);
        assertThat(stored).extracting(OnboardingWizardStep::getStepType)
                .containsExactlyInAnyOrderElementsOf(
                        OnboardingStepDefinition.CANONICAL_ORDER.stream()
                                .map(OnboardingStepDefinition::stepType)
                                .toList());
        assertThat(insertAttempts.get())
                .as("les 2 threads ont bien tente la creation ; le perdant a echoue "
                        + "sur l'index unique puis a relu")
                .isEqualTo(7 + 1);
    }

    @Test
    @DisplayName("Une violation d'index unique ne remonte jamais en 500")
    void integrityViolationIsAbsorbedAndReread() {
        when(wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId))
                .thenReturn(List.of())
                .thenReturn(existingSevenSteps());
        when(wizardRepo.save(any(OnboardingWizardStep.class)))
                .thenThrow(new DataIntegrityViolationException("uk_onboarding_step_tenant_type"));

        OnboardingWizardService service = new OnboardingWizardService(
                wizardRepo, stepActions, objectMapper,
                emptyProvider(), emptyProvider(),
                new com.discipolat.common.infrastructure.security.SecurityUtils(null));

        // Aucune exception : les 7 étapes déjà présentes sont relues.
        assertThat(service.initializeSteps()).hasSize(OnboardingStepDefinition.TOTAL_STEPS);
    }

    private List<OnboardingWizardStep> existingSevenSteps() {
        List<OnboardingWizardStep> steps = new ArrayList<>();
        int order = 0;
        for (OnboardingStepDefinition definition : OnboardingStepDefinition.CANONICAL_ORDER) {
            OnboardingWizardStep step = new OnboardingWizardStep();
            step.setId(UUID.randomUUID());
            step.setTenantId(tenantId);
            step.setStepType(definition.stepType());
            step.setStepOrder(order++);
            step.setStatus(OnboardingWizardStep.Status.PENDING);
            steps.add(step);
        }
        return steps;
    }

    private static <T> ObjectProvider<T> emptyProvider() {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return null;
            }

            @Override
            public T getIfAvailable() {
                return null;
            }

            @Override
            public T getIfUnique() {
                return null;
            }

            @Override
            public Iterator<T> iterator() {
                return Collections.<T>emptyIterator();
            }
        };
    }
}
