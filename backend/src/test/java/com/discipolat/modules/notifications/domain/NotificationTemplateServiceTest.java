package com.discipolat.modules.notifications.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.notifications.api.NotificationTemplateRequest;
import com.discipolat.modules.notifications.api.NotificationTemplateResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;
    @Mock
    private AuditService auditService;

    private NotificationTemplateService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service = new NotificationTemplateService(templateRepository, auditService);
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void create_ShouldPersistTemplateForTenant() {
        NotificationTemplateRequest req = new NotificationTemplateRequest(
                TypeNotification.TRANSFERT_VALIDEE, "Titre", "Message",
                List.of(CanalNotification.IN_APP), List.of("PASTEUR"), true);
        NotificationTemplate saved = NotificationTemplate.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).event(TypeNotification.TRANSFERT_VALIDEE)
                .titre("Titre").message("Message")
                .canaux(new java.util.ArrayList<>(List.of(CanalNotification.IN_APP)))
                .rolesDestinataires(new java.util.ArrayList<>(List.of("PASTEUR")))
                .actif(true).build();
        when(templateRepository.findByTenantIdAndEvent(tenantId, TypeNotification.TRANSFERT_VALIDEE))
                .thenReturn(Optional.empty());
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(saved);

        NotificationTemplateResponse response = service.create(req);

        assertEquals(tenantId, saved.getTenantId());
        assertEquals(TypeNotification.TRANSFERT_VALIDEE, response.event());
        verify(templateRepository).save(any(NotificationTemplate.class));
        verify(auditService).logSimple("NOTIFICATION_TEMPLATE_CREATED", "NOTIFICATION_TEMPLATE", saved.getId());
    }

    @Test
    void create_DuplicateEvent_ShouldThrowBusinessRule() {
        NotificationTemplateRequest req = new NotificationTemplateRequest(
                TypeNotification.INFORMATION, "Titre", "Message",
                List.of(CanalNotification.IN_APP), List.of(), true);
        NotificationTemplate existing = NotificationTemplate.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).event(TypeNotification.INFORMATION).build();
        when(templateRepository.findByTenantIdAndEvent(tenantId, TypeNotification.INFORMATION))
                .thenReturn(Optional.of(existing));

        assertThrows(BusinessRuleException.class, () -> service.create(req));
        verify(templateRepository, never()).save(any());
    }

    @Test
    void create_WithoutTenantContext_ShouldThrow() {
        TenantContext.clear();
        NotificationTemplateRequest req = new NotificationTemplateRequest(
                TypeNotification.INFORMATION, "Titre", "Message",
                List.of(CanalNotification.IN_APP), List.of(), true);
        assertThrows(BusinessRuleException.class, () -> service.create(req));
    }

    @Test
    void toggle_ShouldChangeActifAndAudit() {
        NotificationTemplate template = NotificationTemplate.builder()
                .id(UUID.randomUUID()).tenantId(tenantId).event(TypeNotification.INFORMATION)
                .titre("Titre").actif(true).build();
        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));
        when(templateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NotificationTemplateResponse response = service.toggle(template.getId(), false);

        assertFalse(response.actif());
        assertFalse(template.isActif());
        verify(auditService).logSimple("NOTIFICATION_TEMPLATE_DISABLED", "NOTIFICATION_TEMPLATE", template.getId());
    }

    @Test
    void find_OtherTenantTemplate_ShouldNotBeAccessible() {
        NotificationTemplate otherTenant = NotificationTemplate.builder()
                .id(UUID.randomUUID()).tenantId(UUID.randomUUID()).event(TypeNotification.INFORMATION).build();
        when(templateRepository.findById(otherTenant.getId())).thenReturn(Optional.of(otherTenant));

        assertThrows(com.discipolat.common.domain.EntityNotFoundException.class,
                () -> service.toggle(otherTenant.getId(), false));
    }

    @Test
    void render_ShouldSubstituteKnownVariables() {
        String rendered = NotificationTemplateService.render(
                "{{type}} — entité {{entiteType}}", TypeNotification.ALERTE_ABSENCE, "SOUL");
        assertEquals("ALERTE_ABSENCE — entité SOUL", rendered);
    }

    @Test
    void render_UnknownVariable_ShouldBeKept() {
        String rendered = NotificationTemplateService.render(
                "Bonjour {{nom}} ({{type}})", TypeNotification.INFORMATION, "USER");
        assertEquals("Bonjour {{nom}} (INFORMATION)", rendered);
    }

    @Test
    void render_NullPattern_ShouldReturnNull() {
        assertNull(NotificationTemplateService.render(null, TypeNotification.INFORMATION, "USER"));
    }

    @Test
    void preferredCanal_FallbackInList_ShouldKeepFallback() {
        assertEquals(CanalNotification.EMAIL,
                NotificationTemplateService.preferredCanal(
                        List.of(CanalNotification.IN_APP, CanalNotification.EMAIL), CanalNotification.EMAIL));
    }

    @Test
    void preferredCanal_NotInList_ShouldPreferInApp() {
        assertEquals(CanalNotification.IN_APP,
                NotificationTemplateService.preferredCanal(
                        List.of(CanalNotification.PUSH, CanalNotification.IN_APP), CanalNotification.EMAIL));
    }

    @Test
    void eventCatalog_ShouldContainEveryTypeNotification() {
        List<com.discipolat.modules.notifications.api.NotificationEventInfo> catalog = service.eventCatalog();
        assertEquals(TypeNotification.values().length, catalog.size());
    }
}
