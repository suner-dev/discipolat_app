package com.discipolat.modules.tenants.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.discipolat.modules.ai.domain.AiUsageRepository;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.messages.domain.ConversationMessageRepository;
import com.discipolat.modules.trainings.domain.CourseRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantUsageSnapshotServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileEntityRepository fileRepository;
    @Mock private AiUsageRepository aiUsageRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ConversationMessageRepository messageRepository;
    @Mock private SaasPlanRepository planRepository;
    @Mock private TenantSubscriptionRepository subscriptionRepository;

    private TenantUsageSnapshotService service;
    private final UUID tenantId = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-09-01T12:00:00Z");
    private final LocalDate month = LocalDate.of(2026, 9, 1);
    private final LocalDateTime from = month.atStartOfDay();
    private final LocalDateTime to = month.plusMonths(1).atStartOfDay();

    @BeforeEach
    void setUp() {
        TenantPlanPolicy policy = new TenantPlanPolicy(subscriptionRepository, planRepository, new ObjectMapper());
        service = new TenantUsageSnapshotService(tenantRepository, userRepository, fileRepository,
                aiUsageRepository, courseRepository, messageRepository, policy,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void normalizesBothPlanGenerations() {
        TenantPlanPolicy policy = new TenantPlanPolicy(subscriptionRepository, planRepository, new ObjectMapper());

        assertEquals("DISCOVERY", policy.normalizePlanKey("FREE"));
        assertEquals("DISCOVERY", policy.normalizePlanKey("DISCOVERY"));
        assertEquals("STARTUP", policy.normalizePlanKey("STARTER"));
        assertEquals("STARTUP", policy.normalizePlanKey("STARTUP"));
        assertEquals("GROWTH", policy.normalizePlanKey("PRO"));
        assertEquals("NETWORK", policy.normalizePlanKey("ENTERPRISE"));
    }

    @Test
    void buildsSnapshotOnlyFromPersistedCounters() {
        Tenant tenant = tenant("GROWTH");
        SaasPlan plan = plan("GROWTH", true, 500, 20000, 2000);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.empty());
        when(planRepository.findById("GROWTH")).thenReturn(Optional.of(plan));
        when(userRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(7L);
        when(fileRepository.sumSizeBytesByTenantIdAndDeletedFalse(tenantId)).thenReturn(4096L);
        when(aiUsageRepository.sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
                tenantId, month, month.plusMonths(1))).thenReturn(11L);
        when(courseRepository.countByTenantId(tenantId)).thenReturn(3L);
        when(messageRepository.countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
                tenantId, from, to)).thenReturn(9L);

        TenantUsageSnapshot snapshot = service.getSnapshotForTenant(tenantId);

        assertEquals(7L, snapshot.users().used());
        assertEquals(4096L, snapshot.storageBytes().used());
        assertEquals(11L, snapshot.aiCredits().used());
        assertEquals(3L, snapshot.courses().used());
        assertEquals(9L, snapshot.messages().used());
        assertEquals(500L, snapshot.users().limit());
        assertEquals(20_971_520_000L, snapshot.storageBytes().limit());
        assertEquals(2000L, snapshot.aiCredits().limit());
        assertEquals(TenantUsageSnapshot.MetricSource.PERSISTED_FILES, snapshot.storageBytes().source());
        assertTrue(snapshot.plan().enforcementEnabled());
        assertEquals(Instant.parse("2026-09-01T12:00:00Z"), snapshot.generatedAt());
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), snapshot.aiCredits().periodStart());
        assertEquals(Instant.parse("2026-10-01T00:00:00Z"), snapshot.aiCredits().periodEnd());
    }

    @Test
    void noSubscriptionOrPlanHasNoInventedLimits() {
        Tenant tenant = tenant(null);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.empty());
        when(userRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(2L);
        when(fileRepository.sumSizeBytesByTenantIdAndDeletedFalse(tenantId)).thenReturn(100L);
        when(aiUsageRepository.sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
                tenantId, month, month.plusMonths(1))).thenReturn(1L);
        when(courseRepository.countByTenantId(tenantId)).thenReturn(1L);
        when(messageRepository.countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
                tenantId, from, to)).thenReturn(4L);

        TenantUsageSnapshot snapshot = service.getSnapshotForTenant(tenantId);

        assertNull(snapshot.plan());
        assertNull(snapshot.periodStart());
        assertNull(snapshot.users().limit());
        assertNull(snapshot.storageBytes().limit());
        assertNull(snapshot.aiCredits().limit());
        assertNull(snapshot.courses().limit());
        assertNull(snapshot.messages().limit());
        assertFalse(snapshot.users().serverEnforced());
        assertFalse(snapshot.aiCredits().serverEnforced());
    }

    @Test
    void disabledPlanIsReportedButNotEnforced() {
        Tenant tenant = tenant("NETWORK");
        SaasPlan plan = plan("NETWORK", false, 2000, 100000, 5000);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.empty());
        when(planRepository.findById("NETWORK")).thenReturn(Optional.of(plan));
        when(userRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(1L);
        when(fileRepository.sumSizeBytesByTenantIdAndDeletedFalse(tenantId)).thenReturn(0L);
        when(aiUsageRepository.sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
                tenantId, month, month.plusMonths(1))).thenReturn(0L);
        when(courseRepository.countByTenantId(tenantId)).thenReturn(0L);
        when(messageRepository.countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
                tenantId, from, to)).thenReturn(0L);

        TenantUsageSnapshot snapshot = service.getSnapshotForTenant(tenantId);

        assertEquals("NETWORK", snapshot.plan().canonicalKey());
        assertFalse(snapshot.plan().active());
        assertFalse(snapshot.plan().enforcementEnabled());
        assertEquals(2000L, snapshot.users().limit());
        assertFalse(snapshot.users().serverEnforced());
    }

    @Test
    void keepsCountersScopedToRequestedTenant() {
        UUID otherTenantId = UUID.randomUUID();
        Tenant first = tenant("STARTUP");
        Tenant second = tenant(otherTenantId, "GROWTH");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(first));
        when(tenantRepository.findById(otherTenantId)).thenReturn(Optional.of(second));
        when(subscriptionRepository.findCurrentByTenantId(tenantId)).thenReturn(Optional.empty());
        when(subscriptionRepository.findCurrentByTenantId(otherTenantId)).thenReturn(Optional.empty());
        when(planRepository.findById("STARTUP")).thenReturn(Optional.of(plan("STARTUP", true, 200, 5000, 500)));
        when(planRepository.findById("GROWTH")).thenReturn(Optional.of(plan("GROWTH", true, 500, 20000, 2000)));
        when(userRepository.countByTenantIdAndDeletedFalse(tenantId)).thenReturn(2L);
        when(userRepository.countByTenantIdAndDeletedFalse(otherTenantId)).thenReturn(8L);
        when(fileRepository.sumSizeBytesByTenantIdAndDeletedFalse(tenantId)).thenReturn(20L);
        when(fileRepository.sumSizeBytesByTenantIdAndDeletedFalse(otherTenantId)).thenReturn(80L);
        when(aiUsageRepository.sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
                tenantId, month, month.plusMonths(1))).thenReturn(2L);
        when(aiUsageRepository.sumCreditsConsumedByTenantIdAndUsageDateGreaterThanEqualAndUsageDateLessThan(
                otherTenantId, month, month.plusMonths(1))).thenReturn(8L);
        when(courseRepository.countByTenantId(tenantId)).thenReturn(1L);
        when(courseRepository.countByTenantId(otherTenantId)).thenReturn(4L);
        when(messageRepository.countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
                tenantId, from, to)).thenReturn(3L);
        when(messageRepository.countByTenantIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanAndIsDeletedFalse(
                otherTenantId, from, to)).thenReturn(6L);

        TenantUsageSnapshot firstSnapshot = service.getSnapshotForTenant(tenantId);
        TenantUsageSnapshot secondSnapshot = service.getSnapshotForTenant(otherTenantId);

        assertEquals(2L, firstSnapshot.users().used());
        assertEquals(8L, secondSnapshot.users().used());
        assertEquals(20L, firstSnapshot.storageBytes().used());
        assertEquals(80L, secondSnapshot.storageBytes().used());
        verify(userRepository).countByTenantIdAndDeletedFalse(tenantId);
        verify(userRepository).countByTenantIdAndDeletedFalse(otherTenantId);
    }

    private Tenant tenant(String plan) {
        return tenant(tenantId, plan);
    }

    private Tenant tenant(UUID id, String plan) {
        return Tenant.builder().id(id).name("Tenant").slug("tenant").plan(plan)
                .status(TenantStatus.ACTIVE).build();
    }

    private SaasPlan plan(String key, boolean active, int users, int storageMb, int aiCredits) {
        return SaasPlan.builder().key(key).name(key).currency("XAF").isActive(active)
                .limitsJson("{}").featuresJson("{\"ai\":true}").seatsLimit(users)
                .storageLimitMb(storageMb).aiCreditsLimit(aiCredits).status(active ? "ACTIVE" : "INACTIVE")
                .build();
    }
}
