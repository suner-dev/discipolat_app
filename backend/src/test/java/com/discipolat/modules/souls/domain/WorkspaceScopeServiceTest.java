package com.discipolat.modules.souls.domain;

import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Périmètre de l'espace métier courant : chaque rôle actif ne voit que ses données
 * (faiseur → ses disciples, chef → sa famille, responsable → ses départements).
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceScopeServiceTest {

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private SoulDepartmentRepository soulDepartmentRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private FamilyRepository familyRepository;
    @Mock
    private UserRepository userRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID familleId = UUID.randomUUID();
    private final UUID deptId = UUID.randomUUID();
    private final UUID soulId = UUID.randomUUID();

    private WorkspaceScopeService service() {
        return new WorkspaceScopeService(securityUtils, soulRepository, soulDepartmentRepository,
                departmentRepository, familyRepository, userRepository);
    }

    /** Stube le rôle ACTIF (les autres rôles restent non actifs). */
    private void stubActiveRole(String activeRole) {
        when(securityUtils.hasActiveRole("FAISEUR")).thenReturn("FAISEUR".equals(activeRole));
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn("CHEF_DE_FAMILLE".equals(activeRole));
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn("RESPONSABLE".equals(activeRole));
    }

    @Test
    void accessibleSoulIds_faiseurActif_seulementSesDisciples() {
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("FAISEUR");
        Soul s = Soul.builder().id(soulId).faiseurId(userId).build();
        when(soulRepository.findAllByFaiseurId(userId)).thenReturn(List.of(s));

        assertEquals(Set.of(soulId), service().accessibleSoulIds());
    }

    @Test
    void accessibleSoulIds_chefActif_amesDeSaFamille() {
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("CHEF_DE_FAMILLE");
        User u = User.builder().id(userId).familleGereeId(familleId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        Soul s = Soul.builder().id(soulId).familleId(familleId).build();
        when(soulRepository.findAllByFamilleId(familleId)).thenReturn(List.of(s));

        assertEquals(Set.of(soulId), service().accessibleSoulIds());
    }

    @Test
    void accessibleSoulIds_responsableActif_membresDeSesDepartements() {
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("RESPONSABLE");
        Department dept = Department.builder().id(deptId).nom("Chorale").build();
        when(departmentRepository.findByResponsableId(userId)).thenReturn(List.of(dept));
        SoulDepartment sd = SoulDepartment.builder().soulId(soulId).departmentId(deptId).build();
        when(soulDepartmentRepository.findByDepartmentIdIn(List.of(deptId))).thenReturn(List.of(sd));

        assertEquals(Set.of(soulId), service().accessibleSoulIds());
    }

    @Test
    void accessibleFamilyIds_chefActif_saFamilleGeree() {
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("CHEF_DE_FAMILLE");
        User u = User.builder().id(userId).familleGereeId(familleId).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));

        assertEquals(Set.of(familleId), service().accessibleFamilyIds());
    }

    @Test
    void accessibleFamilyIds_faiseurActif_famillesDeSesDisciples() {
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("FAISEUR");
        Soul s = Soul.builder().id(soulId).faiseurId(userId).familleId(familleId).build();
        when(soulRepository.findAllByFaiseurId(userId)).thenReturn(List.of(s));

        assertEquals(Set.of(familleId), service().accessibleFamilyIds());
    }

    @Test
    void accessibleFaiseurIds_faiseurActif_soiMeme() {
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("FAISEUR");

        assertEquals(Set.of(userId), service().accessibleFaiseurIds());
    }

    @Test
    void accessibleFaiseurIds_responsableActif_faiseursDesMembres() {
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("RESPONSABLE");
        Department dept = Department.builder().id(deptId).nom("Chorale").build();
        when(departmentRepository.findByResponsableId(userId)).thenReturn(List.of(dept));
        SoulDepartment sd = SoulDepartment.builder().soulId(soulId).departmentId(deptId).build();
        when(soulDepartmentRepository.findByDepartmentIdIn(List.of(deptId))).thenReturn(List.of(sd));
        UUID faiseurMembre = UUID.randomUUID();
        Soul s = Soul.builder().id(soulId).faiseurId(faiseurMembre).build();
        when(soulRepository.findAllById(Set.of(soulId))).thenReturn(List.of(s));

        assertEquals(Set.of(faiseurMembre), service().accessibleFaiseurIds());
    }

    @Test
    void multiRoles_utiliseSeulementLeRoleActif() {
        // L'utilisateur possède FAISEUR et RESPONSABLE, mais l'espace courant est FAISEUR :
        // il ne voit que ses disciples, jamais les membres de ses départements.
        SecurityTestHelper.loginAs(userId);
        stubActiveRole("FAISEUR");
        Soul s = Soul.builder().id(soulId).faiseurId(userId).build();
        when(soulRepository.findAllByFaiseurId(userId)).thenReturn(List.of(s));

        assertEquals(Set.of(soulId), service().accessibleSoulIds());
    }
}
