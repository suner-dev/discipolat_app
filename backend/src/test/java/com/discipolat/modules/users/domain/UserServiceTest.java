package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulExitRepository;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private SoulRepository soulRepository;
    @Mock
    private SoulExitRepository soulExitRepository;
    @Mock
    private SoulHistoryRepository soulHistoryRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private WorkspaceScopeService workspaceScopeService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, securityUtils,
                soulRepository, soulExitRepository, soulHistoryRepository, auditService,
                workspaceScopeService);
    }

    private User userWithRole(UserRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .email("utilisateur@discipolat.com")
                .firstName("Test")
                .lastName("Test")
                .role(role)
                .roles(new HashSet<>(Set.of(role)))
                .build();
    }

    @Test
    void create_PasteurAssigningAdmin_ShouldThrowAccessDenied() {
        when(securityUtils.hasActiveRole("ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> userService.create(userWithRole(UserRole.ADMIN), "password123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_ResponsableAssigningPasteur_ShouldThrowAccessDenied() {
        when(securityUtils.hasActiveRole("ADMIN")).thenReturn(false);
        when(securityUtils.hasActiveRole("PASTEUR")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> userService.create(userWithRole(UserRole.PASTEUR), "password123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_ResponsableAssigningResponsable_ShouldThrowAccessDenied() {
        when(securityUtils.hasActiveRole("ADMIN")).thenReturn(false);
        when(securityUtils.hasActiveRole("PASTEUR")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> userService.create(userWithRole(UserRole.RESPONSABLE), "password123"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void create_AdminAssigningAdmin_ShouldSucceed() {
        when(securityUtils.hasActiveRole("ADMIN")).thenReturn(true);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.create(userWithRole(UserRole.ADMIN), "password123");

        assertEquals(UserStatus.PENDING_ACTIVATION, created.getStatut());
        verify(userRepository).save(created);
    }

    @Test
    void create_PasteurAssigningResponsable_ShouldSucceed() {
        when(securityUtils.hasActiveRole("ADMIN")).thenReturn(false);
        when(securityUtils.hasActiveRole("PASTEUR")).thenReturn(true);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = userService.create(userWithRole(UserRole.RESPONSABLE), "password123");

        assertEquals(UserRole.RESPONSABLE, created.getRole());
        verify(userRepository).save(created);
    }

    @Test
    void addRole_ResponsableAddingPasteurRole_ShouldThrowAccessDenied() {
        when(securityUtils.hasActiveRole("ADMIN")).thenReturn(false);
        when(securityUtils.hasActiveRole("PASTEUR")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> userService.addRole(UUID.randomUUID(), UserRole.PASTEUR));

        verify(userRepository, never()).save(any());
    }

    @Test
    void replaceRoles_AdminAddingAdminRole_ShouldSucceed() {
        when(securityUtils.hasActiveRole("ADMIN")).thenReturn(true);
        User target = userWithRole(UserRole.MEMBRE);
        when(userRepository.findById(target.getId())).thenReturn(java.util.Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.replaceRoles(target.getId(), Set.of(UserRole.MEMBRE, UserRole.ADMIN));

        assertTrue(result.getRoles().contains(UserRole.ADMIN));
    }

    // ======================== US-14: WORKLOAD SCOPING ========================

    @Test
    void getFaiseurWorkload_SuperUser_ShouldReturnAllFaiseurs() {
        UUID faiseurId = UUID.randomUUID();
        UUID faiseurId2 = UUID.randomUUID();
        User faiseur1 = userWithRole(UserRole.FAISEUR);
        faiseur1.setId(faiseurId);
        User faiseur2 = userWithRole(UserRole.FAISEUR);
        faiseur2.setId(faiseurId2);
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(userRepository.findByRole(UserRole.FAISEUR)).thenReturn(java.util.List.of(faiseur1, faiseur2));
        when(userRepository.findAllById(any())).thenReturn(java.util.List.of(faiseur1, faiseur2));
        when(soulRepository.countByFaiseurId(any())).thenReturn(5L);

        var result = userService.getFaiseurWorkload(null);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(0).get("soulCount"));
        assertNotNull(result.get(0).get("faiseurName"));
    }

    @Test
    void getFaiseurWorkload_SuperUserWithFamille_ShouldFilterByFamille() {
        UUID familleId = UUID.randomUUID();
        UUID faiseurId = UUID.randomUUID();
        Soul soul = Soul.builder().id(UUID.randomUUID()).faiseurId(faiseurId).build();
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(soulRepository.findAllByFamilleId(familleId)).thenReturn(java.util.List.of(soul));
        User faiseur = userWithRole(UserRole.FAISEUR);
        faiseur.setId(faiseurId);
        when(userRepository.findAllById(any())).thenReturn(java.util.List.of(faiseur));
        when(soulRepository.countByFaiseurId(faiseurId)).thenReturn(3L);

        var result = userService.getFaiseurWorkload(familleId);

        assertEquals(1, result.size());
        assertEquals(faiseurId, result.get(0).get("faiseurId"));
        assertEquals(familleId, result.get(0).get("familleId"));
    }

    @Test
    void getFaiseurWorkload_ChefDeFamille_ShouldOnlySeeHisFamily() {
        UUID userId = UUID.randomUUID();
        UUID familleId = UUID.randomUUID();
        UUID faiseurId = UUID.randomUUID();
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
        User chef = userWithRole(UserRole.CHEF_DE_FAMILLE);
        chef.setId(userId);
        chef.setFamilleGereeId(familleId);
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(chef));
        Soul soul = Soul.builder().id(UUID.randomUUID()).faiseurId(faiseurId).build();
        when(soulRepository.findAllByFamilleId(familleId)).thenReturn(java.util.List.of(soul));
        User faiseur = userWithRole(UserRole.FAISEUR);
        faiseur.setId(faiseurId);
        when(userRepository.findAllById(any())).thenReturn(java.util.List.of(faiseur));
        when(soulRepository.countByFaiseurId(faiseurId)).thenReturn(2L);

        var result = userService.getFaiseurWorkload(familleId);

        assertEquals(1, result.size());
        assertEquals(faiseurId, result.get(0).get("faiseurId"));
    }

    @Test
    void getFaiseurWorkload_Responsable_ShouldOnlySeeHisDepartments() {
        UUID userId = UUID.randomUUID();
        UUID faiseurId = UUID.randomUUID();
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
        when(workspaceScopeService.accessibleFaiseurIds()).thenReturn(java.util.Set.of(faiseurId));
        User faiseur = userWithRole(UserRole.FAISEUR);
        faiseur.setId(faiseurId);
        when(userRepository.findAllById(any())).thenReturn(java.util.List.of(faiseur));
        when(soulRepository.countByFaiseurId(faiseurId)).thenReturn(9L);

        var result = userService.getFaiseurWorkload(null);

        assertEquals(1, result.size());
        assertEquals("SURCHARGE", result.get(0).get("charge"));
        assertEquals(9L, result.get(0).get("soulCount"));
    }

    @Test
    void getFaiseurWorkload_OtherRole_ShouldReturnEmpty() {
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(false);

        var result = userService.getFaiseurWorkload(null);

        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByRole(UserRole.FAISEUR);
    }

    @Test
    void getFaiseurWorkload_ResponsableWithoutAccessibleFaiseurs_ShouldReturnEmpty() {
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
        when(workspaceScopeService.accessibleFaiseurIds()).thenReturn(java.util.Set.of());

        var result = userService.getFaiseurWorkload(null);

        assertTrue(result.isEmpty());
    }
}
