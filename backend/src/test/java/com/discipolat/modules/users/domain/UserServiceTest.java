package com.discipolat.modules.users.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.souls.domain.SoulExitRepository;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, securityUtils,
                soulRepository, soulExitRepository, soulHistoryRepository, auditService);
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
}
