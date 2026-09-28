package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.tenants.api.CreateTenantRequest;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.api.UpdateTenantRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher propagationPublisher;
    @Mock private TenantFeatureService featureService;
    @Mock private TenantPlanPolicy planPolicy;
    @Mock private TenantSubscriptionRepository subscriptionRepository;
    @Mock private SaasPlanService saasPlanService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks private TenantService tenantService;

    private Tenant tenant(UUID id, String slug, String plan) {
        return Tenant.builder()
                .id(id)
                .name("Église Test")
                .slug(slug)
                .status(TenantStatus.ACTIVE)
                .plan(plan)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void create_shouldPersistWithActiveStatusAndDefaultPlan() {
        CreateTenantRequest req = new CreateTenantRequest("Église Nouvelle", "nouvelle-eglise", null, null, null, null, null, null, null, null);
        when(tenantRepository.existsBySlug("nouvelle-eglise")).thenReturn(false);
        // L'identifiant est attribue UNE seule fois : en production il est genere
        // par la base et stable sur tous les `save` de la meme transaction
        // (`create` sauvegarde a nouveau le tenant dans ensureInitialSubscription).
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });
        SaasPlan discovery = SaasPlan.builder().key("DISCOVERY").isActive(true)
                .limitsJson("{\"max_users\":50}").featuresJson("{\"ai\":false}").build();
        when(subscriptionRepository.findCurrentByTenantId(any())).thenReturn(Optional.empty());
        when(planPolicy.resolve(any(Tenant.class))).thenAnswer(inv -> {
            Tenant created = inv.getArgument(0);
            return new TenantPlanPolicy.ResolvedPlan(created.getPlan(), "DISCOVERY", discovery,
                    Map.of("max_users", 50L), false, true, true, null);
        });

        TenantResponse created = tenantService.create(req);

        assertEquals("Église Nouvelle", created.name());
        assertEquals("nouvelle-eglise", created.slug());
        assertEquals(TenantStatus.ACTIVE, created.status());
        assertEquals("DISCOVERY", created.plan());
        verify(subscriptionRepository).save(any(TenantSubscription.class));
        verify(propagationPublisher).publishCreated(eq("TENANT"), eq(created.id()), any(), anyString());
    }

    @Test
    void create_withCustomPlan_shouldKeepIt() {
        CreateTenantRequest req = new CreateTenantRequest("Église A", "eglise-a", "PRO", null, null, null, null, null, null, null);
        when(tenantRepository.existsBySlug("eglise-a")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse created = tenantService.create(req);

        assertEquals("PRO", created.plan());
    }

    @Test
    void create_withDuplicateSlug_shouldThrow() {
        CreateTenantRequest req = new CreateTenantRequest("Église B", "eglise-b", null, null, null, null, null, null, null, null);
        when(tenantRepository.existsBySlug("eglise-b")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> tenantService.create(req));
    }

    @Test
    void update_shouldApplyProvidedFields() {
        UUID id = UUID.randomUUID();
        Tenant existing = tenant(id, "eglise-a", "free");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(existing));
         when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
         when(planPolicy.normalizePlanKey("STARTER")).thenReturn("STARTUP");
         when(saasPlanService.getPlan("STARTUP")).thenReturn(Optional.of(
                 SaasPlan.builder().key("STARTUP").isActive(true).build()));
         when(saasPlanService.subscribe(eq(id), eq("STARTUP"), eq("monthly"), isNull(UUID.class)))
                 .thenAnswer(invocation -> {
                     existing.setPlan("STARTUP");
                     return null;
                 });

         TenantResponse updated = tenantService.update(id,

                new UpdateTenantRequest("Église Renommée", TenantStatus.SUSPENDED, "STARTER", null, null, null, null, null, null, null));

        assertEquals("Église Renommée", updated.name());
        assertEquals(TenantStatus.SUSPENDED, updated.status());
         assertEquals("STARTUP", updated.plan());

        verify(propagationPublisher).publishUpdated(eq("TENANT"), eq(id), any(), any(), anyString());
    }

    @Test
    void get_withUnknownId_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> tenantService.get(id));
    }

    @Test
    void list_shouldReturnAllTenantsOrderedByCreation() {
        Tenant a = tenant(UUID.randomUUID(), "a", "free");
        Tenant b = tenant(UUID.randomUUID(), "b", "free");
        // Horodatages distincts et explicites : tri par date de création.
        a.setCreatedAt(Instant.now().minusSeconds(120));
        b.setCreatedAt(Instant.now());
        when(tenantRepository.findAll()).thenReturn(List.of(b, a));

        List<TenantResponse> tenants = tenantService.list();

        assertEquals(2, tenants.size());
        assertEquals("a", tenants.get(0).slug());
        assertEquals("b", tenants.get(1).slug());
    }

    // ===== Constat M1 : chaque mutation ecrit exactement un evenement d'audit =====

    @Test
    void create_shouldAuditTenantCreatedExactlyOnce() {
        CreateTenantRequest req = new CreateTenantRequest(
                "Eglise Auditee", "eglise-auditee", null, null, null, null, null, null, null, null);
        when(tenantRepository.existsBySlug("eglise-auditee")).thenReturn(false);
        // L'identifiant est attribue UNE seule fois : en production il est genere
        // par la base et stable sur tous les `save` de la meme transaction
        // (`create` sauvegarde a nouveau le tenant dans ensureInitialSubscription).
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            if (t.getId() == null) {
                t.setId(UUID.randomUUID());
            }
            return t;
        });
        SaasPlan discovery = SaasPlan.builder().key("DISCOVERY").isActive(true)
                .limitsJson("{}").featuresJson("{}").build();
        when(subscriptionRepository.findCurrentByTenantId(any())).thenReturn(Optional.empty());
        when(planPolicy.resolve(any(Tenant.class))).thenAnswer(inv -> {
            Tenant created = inv.getArgument(0);
            return new TenantPlanPolicy.ResolvedPlan(created.getPlan(), "DISCOVERY", discovery,
                    Map.of(), false, true, true, null);
        });

        TenantResponse created = tenantService.create(req);

        verify(auditService, times(1)).logSimple("TENANT_CREATED", "TENANT", created.id());
        verifyNoMoreInteractions(auditService);
    }

    @Test
    void update_withoutPlanChange_shouldAuditTenantUpdatedOnlyOnce() {
        UUID id = UUID.randomUUID();
        Tenant existing = tenant(id, "eglise-a", "free");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        tenantService.update(id, new UpdateTenantRequest(
                "Eglise Renommee", null, null, null, null, null, null, null, null, null));

        verify(auditService, times(1)).logSimple("TENANT_UPDATED", "TENANT", id);
        verifyNoMoreInteractions(auditService);
    }

    @Test
    void update_withPlanChange_shouldAuditBothTenantUpdatedAndPlanChanged() {
        UUID id = UUID.randomUUID();
        Tenant existing = tenant(id, "eglise-a", "free");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(planPolicy.normalizePlanKey("STARTER")).thenReturn("STARTUP");
        when(saasPlanService.getPlan("STARTUP")).thenReturn(Optional.of(
                SaasPlan.builder().key("STARTUP").isActive(true).build()));
        when(saasPlanService.subscribe(eq(id), eq("STARTUP"), eq("monthly"), isNull(UUID.class)))
                .thenAnswer(invocation -> {
                    existing.setPlan("STARTUP");
                    return null;
                });

        tenantService.update(id, new UpdateTenantRequest(
                null, null, "STARTER", null, null, null, null, null, null, null));

        verify(auditService, times(1)).logSimple("TENANT_UPDATED", "TENANT", id);
        verify(auditService, times(1)).logSimple("TENANT_PLAN_CHANGED", "TENANT", id);
        verifyNoMoreInteractions(auditService);
    }

    @Test
    void deactivate_shouldAuditTenantSuspendedExactlyOnce() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant(id, "eglise-a", "free")));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        tenantService.deactivate(id);

        verify(auditService, times(1)).logSimple("TENANT_SUSPENDED", "TENANT", id);
        verifyNoMoreInteractions(auditService);
        verify(eventPublisher).publishEvent(any(TenantStatusChangedEvent.class));
    }

    @Test
    void reactivate_shouldAuditTenantReactivatedExactlyOnce() {
        UUID id = UUID.randomUUID();
        Tenant existing = tenant(id, "eglise-a", "free");
        existing.setStatus(TenantStatus.SUSPENDED);
        when(tenantRepository.findById(id)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        tenantService.reactivate(id);

        verify(auditService, times(1)).logSimple("TENANT_REACTIVATED", "TENANT", id);
        verifyNoMoreInteractions(auditService);
        verify(eventPublisher).publishEvent(any(TenantStatusChangedEvent.class));
    }

    @Test
    void reads_shouldNotWriteAnyAuditEvent() {
        UUID id = UUID.randomUUID();
        when(tenantRepository.findById(id)).thenReturn(Optional.of(tenant(id, "eglise-a", "free")));
        when(tenantRepository.findAll()).thenReturn(List.of(tenant(id, "eglise-a", "free")));

        tenantService.list();
        tenantService.get(id);

        verifyNoInteractions(auditService);
    }


    // ===== A4 / D2 : marquage de l'achèvement de l'onboarding =====

    @Test
    void markOnboardingCompleted_shouldSetColumnsAndAuditOnce() {
        UUID id = UUID.randomUUID();
        Tenant existing = tenant(id, "eglise-a", "free");
        com.discipolat.common.multitenancy.TenantContext.setTenantId(id);
        when(tenantRepository.findById(id)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean firstCall = tenantService.markOnboardingCompleted(UUID.randomUUID());

        assertTrue(firstCall);
        assertNotNull(existing.getOnboardingCompletedAt());
        assertNotNull(existing.getOnboardingCompletedBy());
        verify(auditService, times(1)).logSimple("TENANT_ONBOARDING_COMPLETED", "TENANT", id);
        com.discipolat.common.multitenancy.TenantContext.clear();
    }

    @Test
    void markOnboardingCompleted_shouldBeIdempotentAndNeverOverwriteTheOriginalDate() {
        UUID id = UUID.randomUUID();
        UUID originalActor = UUID.randomUUID();
        Tenant existing = tenant(id, "eglise-a", "free");
        existing.setOnboardingCompletedAt(java.time.Instant.parse("2026-09-01T08:00:00Z"));
        existing.setOnboardingCompletedBy(originalActor);
        com.discipolat.common.multitenancy.TenantContext.setTenantId(id);
        when(tenantRepository.findById(id)).thenReturn(Optional.of(existing));

        boolean secondCall = tenantService.markOnboardingCompleted(UUID.randomUUID());

        assertFalse(secondCall);
        assertEquals(java.time.Instant.parse("2026-09-01T08:00:00Z"), existing.getOnboardingCompletedAt());
        assertEquals(originalActor, existing.getOnboardingCompletedBy());
        // Aucune écriture ni audit lors d'un rejeu.
        verify(tenantRepository, never()).save(any());
        verify(auditService, never()).logSimple(eq("TENANT_ONBOARDING_COMPLETED"), any(), any());
        com.discipolat.common.multitenancy.TenantContext.clear();
    }

    @Test
    void markOnboardingCompleted_shouldBeANoOpWithoutTenantContext() {
        assertFalse(tenantService.markOnboardingCompleted(UUID.randomUUID()));
        verify(tenantRepository, never()).save(any());
    }

}
