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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher propagationPublisher;

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
        CreateTenantRequest req = new CreateTenantRequest("Église Nouvelle", "nouvelle-eglise", null);
        when(tenantRepository.existsBySlug("nouvelle-eglise")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TenantResponse created = tenantService.create(req);

        assertEquals("Église Nouvelle", created.name());
        assertEquals("nouvelle-eglise", created.slug());
        assertEquals(TenantStatus.ACTIVE, created.status());
        assertEquals("free", created.plan());
        verify(propagationPublisher).publishCreated(eq("TENANT"), eq(created.id()), any(), anyString());
    }

    @Test
    void create_withCustomPlan_shouldKeepIt() {
        CreateTenantRequest req = new CreateTenantRequest("Église A", "eglise-a", "PRO");
        when(tenantRepository.existsBySlug("eglise-a")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse created = tenantService.create(req);

        assertEquals("PRO", created.plan());
    }

    @Test
    void create_withDuplicateSlug_shouldThrow() {
        CreateTenantRequest req = new CreateTenantRequest("Église B", "eglise-b", null);
        when(tenantRepository.existsBySlug("eglise-b")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> tenantService.create(req));
    }

    @Test
    void update_shouldApplyProvidedFields() {
        UUID id = UUID.randomUUID();
        Tenant existing = tenant(id, "eglise-a", "free");
        when(tenantRepository.findById(id)).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse updated = tenantService.update(id,
                new UpdateTenantRequest("Église Renommée", TenantStatus.SUSPENDED, "STARTER"));

        assertEquals("Église Renommée", updated.name());
        assertEquals(TenantStatus.SUSPENDED, updated.status());
        assertEquals("STARTER", updated.plan());
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
        when(tenantRepository.findAll()).thenReturn(List.of(b, a));

        List<TenantResponse> tenants = tenantService.list();

        assertEquals(2, tenants.size());
        assertEquals("a", tenants.get(0).slug());
        assertEquals("b", tenants.get(1).slug());
    }
}
