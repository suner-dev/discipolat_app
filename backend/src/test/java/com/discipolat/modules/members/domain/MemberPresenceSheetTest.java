package com.discipolat.modules.members.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.BadRequestException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.EntityAttachmentRepository;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.members.api.SubmitDepartmentPresenceItem;
import com.discipolat.modules.members.api.SubmitDepartmentPresenceRequest;
import com.discipolat.modules.members.domain.MemberDepartment;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberPresenceSheetTest {

    @Mock private UserRepository userRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private MemberDepartmentRepository memberDepartmentRepository;
    @Mock private SoulDepartmentRepository soulDepartmentRepository;
    @Mock private MemberPresenceRepository memberPresenceRepository;
    @Mock private MemberRequestRepository memberRequestRepository;
    @Mock private com.discipolat.modules.events.domain.EventRepository eventRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private EntityAttachmentRepository attachmentRepository;
    @Mock private FileEntityRepository fileEntityRepository;
    @Mock private EntityPropagationPublisher propagationPublisher;

    private MemberService memberService;
    private UUID responsableId;
    private UUID deptId;
    private UUID soulId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
                userRepository, soulRepository, familyRepository, departmentRepository,
                memberDepartmentRepository, soulDepartmentRepository,
                memberPresenceRepository, memberRequestRepository, eventRepository, securityUtils,
                new EntityAttachmentService(attachmentRepository, fileEntityRepository, securityUtils),
                propagationPublisher);
        responsableId = UUID.randomUUID();
        deptId = UUID.randomUUID();
        soulId = UUID.randomUUID();
        userId = UUID.randomUUID();

        User responsable = User.builder()
                .id(responsableId)
                .email("resp@test.com")
                .role(UserRole.RESPONSABLE)
                .roles(Set.of(UserRole.RESPONSABLE))
                .activeRole(UserRole.RESPONSABLE)
                .build();
        when(userRepository.findById(responsableId)).thenReturn(Optional.of(responsable));
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).firstName("Jean").lastName("Test")
                        .role(UserRole.MEMBRE).roles(Set.of(UserRole.MEMBRE)).build()));

        Department dept = Department.builder().id(deptId).nom("Chorale").responsableId(responsableId).build();
        when(departmentRepository.findByResponsableId(responsableId)).thenReturn(List.of(dept));
        // Rôle actif RESPONSABLE (l'utilisateur agit en tant que responsable)
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
    }

    private Soul soul() {
        return Soul.builder()
                .id(soulId)
                .nom("Test")
                .prenom("Jean")
                .userId(userId)
                .dateIntegration(LocalDate.now().minusMonths(2))
                .build();
    }

    @Test
    void getDepartmentPresenceSheet_ShouldListMembers() {
        when(securityUtils.getCurrentUserId()).thenReturn(responsableId);
        Soul s = soul();
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId))
                .thenReturn(List.of(SoulDepartment.builder().soulId(soulId).departmentId(deptId).dateAffectation(LocalDateTime.now()).actif(true).build()));
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(s));
        when(memberPresenceRepository.findByUserIdAndSemaine(any(), any())).thenReturn(Optional.empty());

        var sheet = memberService.getDepartmentPresenceSheet(deptId, LocalDate.now());

        assertEquals(1, sheet.size());
        assertEquals("Jean Test", sheet.get(0).nom());
        assertFalse(sheet.get(0).presenceSaisie());
    }

    @Test
    void getDepartmentPresenceSheet_Unauthorized_ShouldThrow() {
        when(securityUtils.getCurrentUserId()).thenReturn(responsableId);
        UUID otherDept = UUID.randomUUID();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> memberService.getDepartmentPresenceSheet(otherDept, LocalDate.now()));
    }

    @Test
    void submitDepartmentPresences_ShouldSavePresence() {
        when(securityUtils.getCurrentUserId()).thenReturn(responsableId);
        Soul s = soul();
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId))
                .thenReturn(List.of(SoulDepartment.builder().soulId(soulId).departmentId(deptId).dateAffectation(LocalDateTime.now()).actif(true).build()));
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(s));
        when(memberPresenceRepository.findByUserIdAndSemaine(any(), any())).thenReturn(Optional.empty());
        when(memberPresenceRepository.save(any(MemberPresence.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitDepartmentPresenceRequest request = new SubmitDepartmentPresenceRequest(
                LocalDate.now(), "DIMANCHE", "Premier culte",
                List.of(new SubmitDepartmentPresenceItem(soulId, true, null, "Présent")));

        var results = memberService.submitDepartmentPresences(deptId, request);

        assertEquals(1, results.size());
        assertTrue(results.get(0).present());
        verify(memberPresenceRepository).save(any(MemberPresence.class));
    }

    @Test
    void submitDepartmentPresences_MemberWithoutAccount_ShouldSaveBySoulId() {
        when(securityUtils.getCurrentUserId()).thenReturn(responsableId);
        // Âme sans compte utilisateur lié
        Soul s = Soul.builder()
                .id(soulId)
                .nom("Durand")
                .prenom("Claire")
                .dateIntegration(LocalDate.now().minusMonths(1))
                .build();
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId))
                .thenReturn(List.of(SoulDepartment.builder().soulId(soulId).departmentId(deptId).dateAffectation(LocalDateTime.now()).actif(true).build()));
        when(soulRepository.findById(soulId)).thenReturn(Optional.of(s));
        when(memberPresenceRepository.findBySoulIdAndSemaine(any(), any())).thenReturn(Optional.empty());
        when(memberPresenceRepository.save(any(MemberPresence.class))).thenAnswer(inv -> inv.getArgument(0));

        SubmitDepartmentPresenceRequest request = new SubmitDepartmentPresenceRequest(
                LocalDate.now(), "DIMANCHE", null,
                List.of(new SubmitDepartmentPresenceItem(soulId, true, null, "Présent au culte")));

        var results = memberService.submitDepartmentPresences(deptId, request);

        assertEquals(1, results.size());
        assertTrue(results.get(0).present());
        verify(memberPresenceRepository).save(any(MemberPresence.class));
    }

    @Test
    void submitDepartmentPresences_MemberNotInDept_ShouldThrow() {
        when(securityUtils.getCurrentUserId()).thenReturn(responsableId);
        when(soulDepartmentRepository.findByDepartmentIdAndActifTrue(deptId)).thenReturn(List.of());

        SubmitDepartmentPresenceRequest request = new SubmitDepartmentPresenceRequest(
                LocalDate.now(), "DIMANCHE", null,
                List.of(new SubmitDepartmentPresenceItem(soulId, true, null, null)));

        assertThrows(BadRequestException.class,
                () -> memberService.submitDepartmentPresences(deptId, request));
    }

    @Test
    void getDepartmentPresenceSheet_WrongActiveRole_ShouldBeDenied() {
        when(securityUtils.getCurrentUserId()).thenReturn(responsableId);
        // L'utilisateur possède RESPONSABLE mais agit en tant qu'autre rôle → refusé
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> memberService.getDepartmentPresenceSheet(deptId, LocalDate.now()));
    }

    @Test
    void getDepartmentPresenceSheet_UnknownUser_ShouldBeDenied() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        // Utilisateur inconnu : aucun département administré → refusé directement,
        // sans fetch préalable (aucune fuite d'existence du compte)
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> memberService.getDepartmentPresenceSheet(deptId, LocalDate.now()));
    }

    @Test
    void getMyUpcomingEvents_ShouldReturnFamilyAndDepartmentEvents() {
        UUID familleId = UUID.randomUUID();
        UUID autreDept = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        Soul s = Soul.builder()
                .id(soulId)
                .nom("Test")
                .prenom("Jean")
                .userId(userId)
                .familleId(familleId)
                .build();
        when(soulRepository.findAllByUserId(userId)).thenReturn(List.of(s));
        when(memberDepartmentRepository.findBySoulId(soulId))
                .thenReturn(List.of(MemberDepartment.builder().soulId(soulId).departmentId(autreDept).build()));

        Event evtFamille = Event.builder().id(UUID.randomUUID()).titre("Culte famille")
                .familleId(familleId).dateDebut(LocalDateTime.now().plusDays(2)).statut("PLANIFIE").build();
        Event evtDept = Event.builder().id(UUID.randomUUID()).titre("Répétition chorale")
                .departmentId(autreDept).dateDebut(LocalDateTime.now().plusDays(5)).statut("PLANIFIE").build();
        Event evtPasse = Event.builder().id(UUID.randomUUID()).titre("Culte passé")
                .familleId(familleId).dateDebut(LocalDateTime.now().minusDays(1)).statut("PLANIFIE").build();

        when(eventRepository.findByFamilleIdAndStatutAndDeletedFalse(familleId, "PLANIFIE"))
                .thenReturn(List.of(evtFamille, evtPasse));
        when(eventRepository.findByDepartmentIdInAndDeletedFalse(List.of(autreDept)))
                .thenReturn(List.of(evtDept));

        var events = memberService.getMyUpcomingEvents();

        // L'événement passé est exclu ; le doublon éventuel est dédupliqué.
        assertEquals(2, events.size());
        assertEquals("Culte famille", events.get(0).get("titre"));
        assertEquals("Répétition chorale", events.get(1).get("titre"));
        assertEquals("PLANIFIE", events.get(0).get("statut"));
    }

    @Test
    void getMyUpcomingEvents_NoSoul_ShouldReturnEmpty() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        when(soulRepository.findAllByUserId(userId)).thenReturn(List.of());

        assertTrue(memberService.getMyUpcomingEvents().isEmpty());
    }
}
