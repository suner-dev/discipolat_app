package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.spaces.domain.SpaceRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat M3 — quotas « espaces » et « événements » (les deux ressources qui
 * n'étaient bornées par AUCUN contrôle à la création) et l'alerte administrateur.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuotaServiceSpacesEventsTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationNodeRepository orgNodeRepository;
    @Mock private com.discipolat.modules.files.domain.FileEntityRepository fileRepository;
    @Mock private com.discipolat.modules.trainings.domain.CourseRepository courseRepository;
    @Mock private com.discipolat.modules.messages.domain.ConversationMessageRepository messageRepository;
    @Mock private com.discipolat.modules.ai.domain.AiUsageRepository aiUsageRepository;
    @Mock private TenantPlanPolicy planPolicy;
    @Mock private TenantUsageSnapshotService usageSnapshotService;
    @Mock private SpaceRepository spaceRepository;
    @Mock private EventRepository eventRepository;
    @Mock private QuotaAlertService quotaAlertService;

    private QuotaService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service = new QuotaService(tenantRepository, userRepository, orgNodeRepository,
                fileRepository, courseRepository, messageRepository, aiUsageRepository,
                planPolicy, usageSnapshotService, spaceRepository, eventRepository, quotaAlertService,
                new com.fasterxml.jackson.databind.ObjectMapper());
        tenantId = UUID.randomUUID();
        when(tenantRepository.findByIdForUpdate(tenantId))
                .thenReturn(Optional.of(Tenant.builder().id(tenantId).plan("GROWTH").build()));
        // `lockPlan` valide l'abonnement via `TenantPlanPolicy` (mock) : sans ce
        // stub, `isEnabledSubscription` renvoie `false` et tout est refusé.
        when(planPolicy.isEnabledSubscription(any())).thenReturn(true);
    }

    /** Plan GROWTH tel que seedé par V177 (spaces 25, events 200, max_churches 10…). */
    private void givenPlan(Map<String, Object> limits) {
        SaasPlan plan = SaasPlan.builder().key("GROWTH").isActive(true).status("ACTIVE").build();
        // `QuotaService.lockPlan` exige un abonnement ACTIF : c'est la garantie
        // de production (V177 seede les plans, `TenantService.create` cree
        // l'abonnement initial). Un tenant sans abonnement est refuse
        // fail-closed, et c'est voulu.
        TenantSubscription subscription = TenantSubscription.builder()
                .tenantId(tenantId)
                .planKey("GROWTH")
                .status(com.discipolat.modules.tenants.enums.SubscriptionStatus.ACTIVE)
                .build();
        when(planPolicy.resolve(any())).thenReturn(new TenantPlanPolicy.ResolvedPlan(
                "GROWTH", "GROWTH", plan, limits, true, true, true, subscription));
    }

    private Map<String, Object> growLimits() {
        return Map.of("spaces", 25, "events", 200,
                "max_churches", 10, "max_departments", 25, "max_campuses", 10, "max_groups", 10);
    }

    // ---------- ESPACES ----------

    @Test
    @DisplayName("Espaces : sous la limite, la création est autorisée")
    void spaceBelowLimitIsAllowed() {
        givenPlan(growLimits());
        when(spaceRepository.countByTenantIdAndDeletedAtIsNull(tenantId)).thenReturn(4L);

        assertThatCode(() -> service.checkCanCreateSpace(tenantId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Espaces : à la limite, refus 403 QUOTA_EXCEEDED_SPACES + alerte")
    void spaceAtLimitIsRefusedWithAlert() {
        givenPlan(growLimits());
        when(spaceRepository.countByTenantIdAndDeletedAtIsNull(tenantId)).thenReturn(25L);

        assertThatThrownBy(() -> service.checkCanCreateSpace(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> {
                    BusinessRuleException quota = (BusinessRuleException) thrown;
                    assertThat(quota.getCode()).isEqualTo("QUOTA_EXCEEDED_SPACES");
                    // Les quotas au préfixe QUOTA_ sont mappés en 403 par
                    // GlobalExceptionHandler.
                    assertThat(quota.getCode().startsWith("QUOTA_")).isTrue();
                });

        verify(quotaAlertService).alertQuotaExceeded(tenantId, "spaces", 25L, 25L);
    }

    @Test
    @DisplayName("Espaces : limite absente du plan => fail-closed QUOTA_CONFIGURATION_INVALID")
    void spaceWithoutLimitFailsClosed() {
        givenPlan(Map.of("events", 200)); // pas de clé `spaces`

        assertThatThrownBy(() -> service.checkCanCreateSpace(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> {
                    BusinessRuleException quota = (BusinessRuleException) thrown;
                    assertThat(quota.getCode()).isEqualTo("QUOTA_CONFIGURATION_INVALID");
                    // 403 : préfixe QUOTA_ => mappé Forbidden par GlobalExceptionHandler.
                    assertThat(HttpStatus.FORBIDDEN).isNotNull();
                });
        // Aucune lecture de consommation : la configuration est refusée d'emblée.
        verify(spaceRepository, org.mockito.Mockito.never())
                .countByTenantIdAndDeletedAtIsNull(tenantId);
    }

    // ---------- EVENEMENTS ----------

    @Test
    @DisplayName("Événements : sous la limite, la création est autorisée")
    void eventBelowLimitIsAllowed() {
        givenPlan(growLimits());
        when(eventRepository.countByTenantIdAndStatutNotInAndDeletedFalse(
                any(), any())).thenReturn(12L);

        assertThatCode(() -> service.checkCanCreateEvent(tenantId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Événements : à la limite, refus + alerte")
    void eventAtLimitIsRefusedWithAlert() {
        givenPlan(growLimits());
        when(eventRepository.countByTenantIdAndStatutNotInAndDeletedFalse(any(), any()))
                .thenReturn(200L);

        assertThatThrownBy(() -> service.checkCanCreateEvent(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getCode())
                        .isEqualTo("QUOTA_EXCEEDED_EVENTS"));

        verify(quotaAlertService).alertQuotaExceeded(tenantId, "events", 200L, 200L);
    }

    @Test
    @DisplayName("Événements : seuls les statuts clos sont exclus du décompte")
    void eventCountingExcludesClosedStatuses() {
        givenPlan(growLimits());
        when(eventRepository.countByTenantIdAndStatutNotInAndDeletedFalse(any(), any()))
                .thenReturn(0L);

        service.checkCanCreateEvent(tenantId);

        verify(eventRepository).countByTenantIdAndStatutNotInAndDeletedFalse(
                eq(tenantId), eq(java.util.List.of("TERMINE", "ANNULE")));
    }

    @Test
    @DisplayName("Événements : limite absente => fail-closed")
    void eventWithoutLimitFailsClosed() {
        givenPlan(Map.of("spaces", 25));

        assertThatThrownBy(() -> service.checkCanCreateEvent(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getCode())
                        .isEqualTo("QUOTA_CONFIGURATION_INVALID"));
    }

    // ---------- EGLISES / CAMPUS ----------

    @Test
    @DisplayName("Églises : quota appliqué à la création (constat M3)")
    void churchQuotaIsEnforced() {
        givenPlan(growLimits());
        when(orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.ROOT_CHURCH))
                .thenReturn(1L);
        when(orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH))
                .thenReturn(0L);

        assertThatCode(() -> service.checkCanCreateChurch(tenantId, OrganizationNodeType.SUB_CHURCH))
                .doesNotThrowAnyException();

        when(orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.SUB_CHURCH))
                .thenReturn(9L);
        assertThatThrownBy(() -> service.checkCanCreateChurch(tenantId, OrganizationNodeType.SUB_CHURCH))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getCode())
                        .isEqualTo("QUOTA_EXCEEDED_CHURCHES"));
    }

    @Test
    @DisplayName("Campus : quota appliqué")
    void campusQuotaIsEnforced() {
        givenPlan(growLimits());
        when(orgNodeRepository.countByTenantIdAndType(tenantId, OrganizationNodeType.CAMPUS))
                .thenReturn(10L);

        assertThatThrownBy(() -> service.checkCanCreateCampus(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getCode())
                        .isEqualTo("QUOTA_EXCEEDED_CAMPUSES"));
    }

    // ---------- alerte ----------

    @Test
    @DisplayName("Le dépassement déclenche bien une alerte, et une panne d'alerte ne bloque rien")
    void alertIsTriggeredAndNeverBlocksTheRefusal() {
        givenPlan(growLimits());
        when(spaceRepository.countByTenantIdAndDeletedAtIsNull(tenantId)).thenReturn(99L);
        org.mockito.Mockito.doThrow(new IllegalStateException("notifications indisponibles"))
                .when(quotaAlertService).alertQuotaExceeded(any(), anyString(), anyLong(), anyLong());

        // Le refus de quota doit être levé malgré l'échec de la notification.
        assertThatThrownBy(() -> service.checkCanCreateSpace(tenantId))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(thrown -> assertThat(((BusinessRuleException) thrown).getCode())
                        .isEqualTo("QUOTA_EXCEEDED_SPACES"));
    }

    @Test
    @DisplayName("Aucune alerte n'est émise quand la création est autorisée")
    void noAlertWhenWithinLimit() {
        givenPlan(growLimits());
        when(eventRepository.countByTenantIdAndStatutNotInAndDeletedFalse(any(), any()))
                .thenReturn(1L);

        service.checkCanCreateEvent(tenantId);

        verify(quotaAlertService, org.mockito.Mockito.never())
                .alertQuotaExceeded(any(), anyString(), anyLong(), anyLong());
    }
}
