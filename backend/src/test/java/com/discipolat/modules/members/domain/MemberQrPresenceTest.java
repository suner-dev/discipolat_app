package com.discipolat.modules.members.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.EntityAttachmentRepository;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G5.6 — check-in QR de terrain : sémantique de présence, garde tenant du QR
 * scanné, résolution d'âme pour distribution de kit.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberQrPresenceTest {

    @Mock private UserRepository userRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private com.discipolat.modules.families.domain.FamilyRepository familyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private MemberDepartmentRepository memberDepartmentRepository;
    @Mock private SoulDepartmentRepository soulDepartmentRepository;
    @Mock private MemberPresenceRepository memberPresenceRepository;
    @Mock private MemberRequestRepository memberRequestRepository;
    @Mock private EventRepository eventRepository;
    // Le constructible de main compte aussi les inscriptions aux evenements.
    @Mock private com.discipolat.modules.events.domain.EventRegistrationRepository eventRegistrationRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private EntityAttachmentRepository attachmentRepository;
    @Mock private FileEntityRepository fileEntityRepository;
    @Mock private EntityPropagationPublisher propagationPublisher;

    private MemberService memberService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID soulId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(userId);
        memberService = new MemberService(
                userRepository, soulRepository, familyRepository, departmentRepository,
                memberDepartmentRepository, soulDepartmentRepository,
                memberPresenceRepository, memberRequestRepository, eventRepository,
                eventRegistrationRepository, securityUtils,
                new EntityAttachmentService(attachmentRepository, fileEntityRepository, securityUtils),
                propagationPublisher);
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Soul soul(UUID tenant) {
        return Soul.builder()
                .id(soulId)
                .tenantId(tenant)
                .nom("Durand")
                .prenom("Marie")
                .userId(userId)
                .build();
    }

    @Test
    void recordPresenceByQr_withinTenant_createsWeeklyFlag() {
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(soul(tenantId)));
        when(memberPresenceRepository.findByUserIdAndSemaine(any(), any())).thenReturn(Optional.empty());
        when(memberPresenceRepository.save(any(MemberPresence.class))).thenAnswer(inv -> inv.getArgument(0));

        memberService.recordPresenceByQr(soulId);

        var captor = org.mockito.ArgumentCaptor.forClass(MemberPresence.class);
        verify(memberPresenceRepository).save(captor.capture());
        MemberPresence saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals(LocalDate.now().with(java.time.DayOfWeek.MONDAY), saved.getSemaine());
        assertTrue(Boolean.TRUE.equals(saved.getPresences().get("QR_CHECKIN")));
    }

    @Test
    void recordPresenceByQr_crossTenantQr_notFound() {
        // QR scanné appartenant à un autre tenant → introuvable (aucune fuite d'existence)
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(soul(UUID.randomUUID())));

        assertThrows(EntityNotFoundException.class, () -> memberService.recordPresenceByQr(soulId));
    }

    @Test
    void recordPresenceByQr_soulWithoutAccount_rejected() {
        Soul noAccount = soul(tenantId);
        noAccount.setUserId(null);
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(noAccount));

        assertThrows(IllegalStateException.class, () -> memberService.recordPresenceByQr(soulId));
    }

    @Test
    void resolveSoulForQr_returnsSummaryWithinTenantOnly() {
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(soul(tenantId)));

        Optional<Map<String, Object>> summary = memberService.resolveSoulForQr(soulId);
        assertTrue(summary.isPresent());
        assertEquals(soulId.toString(), summary.get().get("soulId"));
        assertEquals("Marie", summary.get().get("prenom"));

        // Hors tenant → jamais résolue
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(soul(UUID.randomUUID())));
        assertTrue(memberService.resolveSoulForQr(soulId).isEmpty());
    }

    @Test
    void mySoulId_linksCurrentUserToHisSoul() {
        User me = User.builder().id(userId).email("me@test.com")
                .role(UserRole.MEMBRE).roles(Set.of(UserRole.MEMBRE)).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(me));
        when(soulRepository.findAllByUserId(userId)).thenReturn(List.of(soul(tenantId)));

        assertEquals(soulId, memberService.mySoulId());
    }
}
