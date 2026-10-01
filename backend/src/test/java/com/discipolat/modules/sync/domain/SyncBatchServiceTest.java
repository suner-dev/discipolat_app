package com.discipolat.modules.sync.domain;

import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.multitenancy.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.discipolat.modules.gantt.api.TeamTaskController;
import com.discipolat.modules.inventory.api.AssetController;
import com.discipolat.modules.members.api.MemberController;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.members.domain.MemberService;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.SoulService;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * §G5.7 — Idempotence du batch offline, toggle tenant offline_mode,
 * rejeu sans doublon, conflit LWW tracé + notifié, photo base64.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncBatchServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    @Mock SyncOperationRepository operationRepository;
    @Mock SyncConflictRepository conflictRepository;
    @Mock TenantSettingsService tenantSettingsService;
    @Mock TenantMembershipRepository membershipRepository;
    @Mock MemberController memberController;
    @Mock AssetController assetController;
    @Mock TeamTaskController teamTaskController;
    @Mock SoulService soulService;
    @Mock MemberService memberService;
    @Mock MemberPresenceRepository presenceRepository;
    @Mock NotificationService notificationService;

    SyncBatchService service;

    @BeforeEach
    void setUp() {
        service = new SyncBatchService(operationRepository, conflictRepository,
                tenantSettingsService, membershipRepository, memberController, assetController,
                teamTaskController, soulService, memberService, presenceRepository,
                notificationService,
                new ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.module.paramnames.ParameterNamesModule())
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()));
        TenantContext.setTenantId(TENANT);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                ACTOR, "credentials",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_RESPONSABLE"))));
        TenantSettings settings = new TenantSettings();
        settings.setOfflineMode("FULL");
        when(tenantSettingsService.getSettings(TENANT)).thenReturn(settings);
        when(operationRepository.save(any(SyncOperation.class))).thenAnswer(inv -> {
            SyncOperation s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(memberController.qrCheckin(anyMap())).thenReturn(
                ResponseEntity.ok(Map.of("success", true)));
        when(memberController.submitDepartmentPresences(any(), any())).thenReturn(
                ResponseEntity.ok(List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private SyncBatchService.ClientOp op(String uuid, String type, Map<String, Object> payload) {
        return new SyncBatchService.ClientOp(uuid, type, payload, LocalDateTime.now(), null, null, null);
    }

    @Test
    void batchAppliesDistinctOperations() {
        var result = service.applyBatch(List.of(
                op("c-1", "QR_CHECKIN", Map.of("code", "discipolat:soul:" + UUID.randomUUID())),
                op("c-2", "TASK_STATUS", Map.of("taskId", UUID.randomUUID().toString(), "statut", "FAITE"))),
                SecurityContextHolder.getContext().getAuthentication());

        assertEquals(2, result.applied());
        assertEquals(0, result.failed());
        assertEquals(2, result.results().stream()
                .filter(r -> "APPLIED".equals(r.get("status"))).count());
    }

    @Test
    void replayOfSameClientUuidIsSkippedWithoutReexecution() {
        when(operationRepository.findByTenantIdAndClientUuid(TENANT, "c-dup"))
                .thenReturn(Optional.of(new SyncOperation()));

        var result = service.applyBatch(
                List.of(op("c-dup", "QR_CHECKIN", Map.of("code", "x"))),
                SecurityContextHolder.getContext().getAuthentication());

        assertEquals(1, result.duplicates());
        assertEquals(0, result.applied());
        assertEquals("SKIPPED_DUPLICATE", result.results().get(0).get("status"));
        verify(memberController, never()).qrCheckin(anyMap());
    }

    @Test
    void lectureModeRejectsEveryWrite() {
        TenantSettings settings = new TenantSettings();
        settings.setOfflineMode("LECTURE");
        when(tenantSettingsService.getSettings(TENANT)).thenReturn(settings);

        var result = service.applyBatch(
                List.of(op("c-l", "QR_CHECKIN", Map.of("code", "x"))),
                SecurityContextHolder.getContext().getAuthentication());

        assertEquals(1, result.failed());
        assertEquals("REJECTED_OFFLINE_MODE", result.results().get(0).get("status"));
        verify(memberController, never()).qrCheckin(anyMap());
    }

    @Test
    void fieldOpsModeAllowsCriticalOnly() {
        TenantSettings settings = new TenantSettings();
        settings.setOfflineMode("FIELD_OPS");
        when(tenantSettingsService.getSettings(TENANT)).thenReturn(settings);

        var result = service.applyBatch(List.of(
                op("c-f1", "ASSET_RETURN", Map.of("itemId", UUID.randomUUID().toString(), "condition", "GOOD")),
                op("c-f2", "TASK_STATUS", Map.of("taskId", UUID.randomUUID().toString(), "statut", "FAITE"))),
                SecurityContextHolder.getContext().getAuthentication());

        assertEquals(1, result.applied());
        assertEquals(1, result.failed());
        assertEquals("REJECTED_OFFLINE_MODE", result.results().get(1).get("status"));
    }

    @Test
    void presenceUpdatedAfterFieldCaptureFlagsLwwConflictAndNotifies() {
        when(presenceRepository.findByTenantIdAndUpdatedAtAfter(eq(TENANT), any(LocalDateTime.class)))
                .thenReturn(List.of(new MemberPresence()));
        com.discipolat.modules.tenants.domain.Role adminRole = new com.discipolat.modules.tenants.domain.Role();
        adminRole.setKey("ADMIN");
        com.discipolat.modules.tenants.domain.TenantMembership membership =
                com.discipolat.modules.tenants.domain.TenantMembership.builder()
                        .userId(UUID.randomUUID()).role(adminRole).build();
        when(membershipRepository.findByTenantIdAndStatus(TENANT,
                com.discipolat.modules.tenants.domain.MembershipStatus.ACTIVE))
                .thenReturn(List.of(membership));

        var result = service.applyBatch(List.of(new SyncBatchService.ClientOp(
                "c-k", "PRESENCE_SUBMIT",
                Map.of("departmentId", UUID.randomUUID().toString(),
                        "request", Map.of("semaine", "2026-09-21", "presences", List.of())),
                LocalDateTime.now().minusDays(1), null, null, null)),
                SecurityContextHolder.getContext().getAuthentication());

                assertEquals(1, result.conflicts());
        assertEquals("CONFLICT_LWW", result.results().get(0).get("status"));
        verify(conflictRepository).save(any(SyncConflict.class));
        verify(notificationService).create(any(UUID.class), eq(TypeNotification.OFFLINE_CONFLIT),
                eq(CanalNotification.IN_APP), anyString(), anyString(), any(), anyString());
    }

    @Test
    void damagePhotoFromBase64ReachesRealUpload() throws Exception {
        byte[] jpeg = new byte[] {1, 2, 3, 4};
        UUID itemId = UUID.randomUUID();
        when(assetController.uploadDamagePhoto(eq(itemId), any(MultipartFile.class)))
                .thenReturn(ResponseEntity.status(201).body(Map.of("photoPath", "p")));

        var result = service.applyBatch(List.of(new SyncBatchService.ClientOp(
                "c-p", "ASSET_DAMAGE_PHOTO", Map.of("itemId", itemId.toString()),
                LocalDateTime.now(), Base64.getEncoder().encodeToString(jpeg), "image/jpeg", "d.jpg")),
                SecurityContextHolder.getContext().getAuthentication());

        assertEquals(1, result.applied());
        var captor = org.mockito.ArgumentCaptor.forClass(MultipartFile.class);
        verify(assetController).uploadDamagePhoto(eq(itemId), captor.capture());
        assertEquals(4, captor.getValue().getBytes().length);
        assertTrue(captor.getValue().getContentType().startsWith("image/"));
    }

    @Test
    void unknownTypeFailsWithoutBreakingTheBatch() {
        var result = service.applyBatch(List.of(
                op("c-u", "MAGIC_OP", Map.of()),
                op("c-v", "QR_CHECKIN", Map.of("code", "y"))),
                SecurityContextHolder.getContext().getAuthentication());

        assertEquals(1, result.failed());
        assertEquals(1, result.applied());
        assertEquals("FAILED", result.results().get(0).get("status"));
    }
}
