package com.discipolat.modules.objectives.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.evangelism.domain.EvangelismTrackRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.interactions.domain.InteractionRepository;
import com.discipolat.modules.members.domain.MemberDepartmentRepository;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.objectives.api.CreateObjectiveRequest;
import com.discipolat.modules.objectives.api.ObjectiveProgressResponse;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectiveServiceTest {

    @Mock private ObjectiveRepository objectiveRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private InteractionRepository interactionRepository;
    @Mock private EvangelismTrackRepository evangelismTrackRepository;
    @Mock private MemberPresenceRepository memberPresenceRepository;
    @Mock private MemberDepartmentRepository memberDepartmentRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private EntityPropagationPublisher propagationPublisher;

    @InjectMocks private ObjectiveService objectiveService;

    @Test
    void create_shouldPersistObjective() {
        SecurityTestHelper.loginAs(UUID.randomUUID());

        CreateObjectiveRequest request = new CreateObjectiveRequest(
                UserRole.FAISEUR, ObjectiveType.VISITES, 6, Objective.Periode.MENSUEL);
        when(objectiveRepository.save(any(Objective.class))).thenAnswer(inv -> inv.getArgument(0));

        Objective created = objectiveService.create(request);

        assertEquals(UserRole.FAISEUR, created.getRole());
        assertEquals(ObjectiveType.VISITES, created.getType());
        assertEquals(6, created.getCible());
        assertTrue(created.isActif());
    }

    @Test
    void progress_shouldComputeTauxAndAtteint() {
        Objective objective = Objective.builder()
                .id(UUID.randomUUID())
                .role(UserRole.FAISEUR)
                .type(ObjectiveType.VISITES)
                .cible(10)
                .periode(Objective.Periode.MENSUEL)
                .actif(true)
                .build();

        ObjectiveProgressResponse underTarget = ObjectiveProgressResponse.of(objective, 5);
        assertEquals(50.0, underTarget.taux());
        assertFalse(underTarget.atteint());

        ObjectiveProgressResponse overTarget = ObjectiveProgressResponse.of(objective, 12);
        assertEquals(100.0, overTarget.taux()); // plafonné à 100
        assertTrue(overTarget.atteint());
    }

    @Test
    void myProgress_shouldOnlyReturnObjectivesOfActiveRole() {
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        SecurityTestHelper.loginAs(UUID.randomUUID());

        Objective pasteurObj = Objective.builder()
                .id(UUID.randomUUID())
                .role(UserRole.PASTEUR)
                .type(ObjectiveType.NOUVELLES_AMES)
                .cible(5)
                .periode(Objective.Periode.MENSUEL)
                .actif(true)
                .build();
        when(objectiveRepository.findByRoleAndActifTrue(UserRole.PASTEUR))
                .thenReturn(List.of(pasteurObj));

        // Pasteur voit toutes les âmes (scope null)
        lenient().when(soulRepository.findAll()).thenReturn(List.of());

        List<ObjectiveProgressResponse> progress = objectiveService.myProgress();

        assertEquals(1, progress.size());
        assertEquals(ObjectiveType.NOUVELLES_AMES, progress.get(0).type());
        assertEquals(0.0, progress.get(0).realise());
    }
}
