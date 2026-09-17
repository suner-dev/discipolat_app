package com.discipolat.modules.statuses.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G2.7 — Tests du moteur de statuts configurables.
 * DoD : transition interdite refusée, kanban groupé par statuts perso, héritage (G1.7).
 */
@ExtendWith(MockitoExtension.class)
class CustomStatusServiceTest {

    @Mock private CustomStatusRepository repository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private EntityPropagationPublisher entityPropagationPublisher;

    private CustomStatusService service;

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID SPACE = UUID.randomUUID();
    private static final String ENTITY = "TASK";

    @BeforeEach
    void setUp() {
        service = new CustomStatusService(repository, auditService, eventPublisher, entityPropagationPublisher);
    }

    private CustomStatus status(String code, int order, boolean initial, boolean finalStatus, List<String> allowed) {
        return CustomStatus.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .entityType(ENTITY)
                .spaceId(SPACE)
                .code(code)
                .name(code)
                .color("#000000")
                .displayOrder(order)
                .initial(initial)
                .finalStatus(finalStatus)
                .allowedTransitions(allowed)
                .build();
    }

    private List<CustomStatus> defaultSet() {
        return List.of(
                status("PREPROD", 0, true, false, List.of("PROD")),
                status("PROD", 1, false, false, List.of("POSTPROD")),
                status("POSTPROD", 2, false, true, List.of()));
    }

    private void givenSpaceSet(List<CustomStatus> set) {
        when(repository.findByTenantIdAndEntityTypeAndSpaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(
                TENANT, ENTITY, SPACE)).thenReturn(set);
    }

    @Test
    void transitionAutorisee_passe() {
        givenSpaceSet(defaultSet());

        assertDoesNotThrow(() -> service.validateTransition(TENANT, ENTITY, SPACE, "PREPROD", "PROD"));
    }

    @Test
    void transitionInterdite_estRefusee() {
        givenSpaceSet(defaultSet());

        StatusTransitionDeniedException ex = assertThrows(StatusTransitionDeniedException.class,
                () -> service.validateTransition(TENANT, ENTITY, SPACE, "PREPROD", "POSTPROD"));
        assertTrue(ex.getMessage().contains("non autorisée"));
    }

    @Test
    void transitionDepuisStatutFinal_estRefusee() {
        givenSpaceSet(defaultSet());

        assertThrows(StatusTransitionDeniedException.class,
                () -> service.validateTransition(TENANT, ENTITY, SPACE, "POSTPROD", "PROD"));
    }

    @Test
    void transitionVersStatutInconnu_estRefusee() {
        givenSpaceSet(defaultSet());

        assertThrows(StatusTransitionDeniedException.class,
                () -> service.validateTransition(TENANT, ENTITY, SPACE, "PROD", "INEXISTANT"));
    }

    @Test
    void kanban_groupeEtOrdonneParStatutPerso() {
        givenSpaceSet(defaultSet());

        List<Map<String, Object>> board = service.getStatusBoard(TENANT, ENTITY, SPACE);

        assertEquals(3, board.size());
        assertEquals("PREPROD", board.get(0).get("code"));
        assertEquals("PROD", board.get(1).get("code"));
        assertEquals("POSTPROD", board.get(2).get("code"));
        assertEquals(true, board.get(0).get("initial"));
        assertEquals(true, board.get(2).get("final"));
    }

    @Test
    void heritage_espaceSansJeuPropre_retombeSurLeTenant() {
        when(repository.findByTenantIdAndEntityTypeAndSpaceIdAndDeletedAtIsNullOrderByDisplayOrderAsc(
                TENANT, ENTITY, SPACE)).thenReturn(List.of());
        List<CustomStatus> tenantSet = List.of(
                CustomStatus.builder().id(UUID.randomUUID()).tenantId(TENANT).entityType(ENTITY)
                        .spaceId(null).code("A").name("A").displayOrder(0).build());
        when(repository.findByTenantIdAndEntityTypeAndSpaceIdIsNullAndDeletedAtIsNullOrderByDisplayOrderAsc(
                TENANT, ENTITY)).thenReturn(tenantSet);

        List<CustomStatus> resolved = service.resolveStatusSet(TENANT, ENTITY, SPACE);

        assertEquals(1, resolved.size());
        assertEquals("A", resolved.get(0).getCode());
    }

    @Test
    void changeStatus_valideEtPublieEvenement() {
        givenSpaceSet(defaultSet());

        service.changeStatus(TENANT, ENTITY, SPACE, UUID.randomUUID(), "PREPROD", "PROD", UUID.randomUUID());

        verify(auditService).logSimple(eq("STATUS_CHANGED"), eq(ENTITY), any());
        verify(eventPublisher).publishEvent(any(StatusChangedEvent.class));
    }
}
