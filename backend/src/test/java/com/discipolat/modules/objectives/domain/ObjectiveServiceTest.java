package com.discipolat.modules.objectives.domain;

import com.discipolat.common.domain.BusinessRuleException;
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
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
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
    @Mock private RoleRepository roleRepository;

    @InjectMocks private ObjectiveService objectiveService;

    @Test
    void create_shouldPersistObjective() {
        SecurityTestHelper.loginAs(UUID.randomUUID());

        CreateObjectiveRequest request = new CreateObjectiveRequest(
                "FAISEUR", ObjectiveType.VISITES, 6, Objective.Periode.MENSUEL);
        when(objectiveRepository.save(any(Objective.class))).thenAnswer(inv -> inv.getArgument(0));

        Objective created = objectiveService.create(request);

        assertEquals("FAISEUR", created.getRole());
        assertEquals(ObjectiveType.VISITES, created.getType());
        assertEquals(6, created.getCible());
        assertTrue(created.isActif());
    }

    @Test
    void progress_shouldComputeTauxAndAtteint() {
        Objective objective = Objective.builder()
                .id(UUID.randomUUID())
                .role("FAISEUR")
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
                .role("PASTEUR")
                .type(ObjectiveType.NOUVELLES_AMES)
                .cible(5)
                .periode(Objective.Periode.MENSUEL)
                .actif(true)
                .build();
        when(objectiveRepository.findByRoleAndActifTrue("PASTEUR"))
                .thenReturn(List.of(pasteurObj));

        // Pasteur voit toutes les âmes (scope null)
        lenient().when(soulRepository.findAll()).thenReturn(List.of());

        List<ObjectiveProgressResponse> progress = objectiveService.myProgress();

        assertEquals(1, progress.size());
        assertEquals(ObjectiveType.NOUVELLES_AMES, progress.get(0).type());
        assertEquals(0.0, progress.get(0).realise());
    }

    @Test
    void create_shouldAcceptCustomCatalogRoleKey() {
        // P7a/V263 : une clé de rôle custom du catalogue servi du tenant est
        // acceptée en ciblage d'objectif (union enum ∪ catalogue), sans crash.
        SecurityTestHelper.loginAs(UUID.randomUUID());
        UUID tenantId = UUID.randomUUID();
        com.discipolat.common.multitenancy.TenantContext.setTenantId(tenantId);
        try {
            Role diacre = new Role();
            diacre.setKey("DIACRE");
            when(roleRepository.findByTenantIdAndKey(tenantId, "DIACRE")).thenReturn(java.util.Optional.of(diacre));
            when(objectiveRepository.save(any(Objective.class))).thenAnswer(inv -> inv.getArgument(0));

            CreateObjectiveRequest request = new CreateObjectiveRequest(
                    "diacre", ObjectiveType.VISITES, 4, Objective.Periode.MENSUEL);
            Objective created = objectiveService.create(request);

            // Normalisée en majuscules, clé custom conservée.
            assertEquals("DIACRE", created.getRole());
        } finally {
            com.discipolat.common.multitenancy.TenantContext.clear();
        }
    }

    @Test
    void create_shouldRejectUnknownRoleKey() {
        // P7a/V263 : une clé absente à la fois de l'enum socle et du catalogue
        // servi → rejet métier 4xx, jamais un crash Enum.valueOf (500).
        SecurityTestHelper.loginAs(UUID.randomUUID());
        UUID tenantId = UUID.randomUUID();
        com.discipolat.common.multitenancy.TenantContext.setTenantId(tenantId);
        try {
            when(roleRepository.findByTenantIdAndKey(tenantId, "MAGICIEN")).thenReturn(java.util.Optional.empty());
            when(roleRepository.findByTenantIdIsNullAndKey("MAGICIEN")).thenReturn(java.util.Optional.empty());

            CreateObjectiveRequest request = new CreateObjectiveRequest(
                    "MAGICIEN", ObjectiveType.VISITES, 4, Objective.Periode.MENSUEL);
            assertThrows(BusinessRuleException.class, () -> objectiveService.create(request));
        } finally {
            com.discipolat.common.multitenancy.TenantContext.clear();
        }
    }

    @Test
    void myProgress_shouldNotCrashOnCustomActiveRole() {
        // P7a/V263 : un utilisateur dont le rôle actif est une clé custom du
        // catalogue ne doit plus faire crasher UserRole.valueOf (500). La
        // progression retombe sur le périmètre de la clé (vide → objectifs du
        // rôle custom, mesure à 0), sans exception.
        when(securityUtils.getCurrentUserRole()).thenReturn("DIACRE");
        SecurityTestHelper.loginAs(UUID.randomUUID());
        when(objectiveRepository.findByRoleAndActifTrue("DIACRE")).thenReturn(List.of());

        List<ObjectiveProgressResponse> progress = objectiveService.myProgress();
        assertTrue(progress.isEmpty());
    }
}
