package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.security.JwtTokenProvider;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * G5.4 (§55) — La bascule de tenant doit être RÉELLE : validation serveur de
 * l'adhésion, persistance du choix et tokens réémis portant le nouveau claim
 * tenantId (sinon le TenantInterceptor rejoue l'ancien tenant).
 */
@ExtendWith(MockitoExtension.class)
class ActiveTenantServiceTest {

    @Mock
    private TenantMembershipRepository membershipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private ActiveTenantService service;

    private UUID userId;
    private UUID homeTenant;
    private UUID otherTenant;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ActiveTenantService(membershipRepository, userRepository, jwtTokenProvider);
        userId = UUID.randomUUID();
        homeTenant = UUID.randomUUID();
        otherTenant = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("marie@eglise.org")
                .passwordHash("x")
                .firstName("Marie")
                .lastName("Dupont")
                .role(UserRole.RESPONSABLE)
                .roles(Set.of(UserRole.RESPONSABLE))
                .activeRole(UserRole.RESPONSABLE)
                .tenantId(homeTenant)
                .estChefDeFamille(false)
                .build();
    }

    @Test
    void switchTenantPersistsChoiceAndReissuesTokensWithNewTenantClaim() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, otherTenant, MembershipStatus.ACTIVE))
                .thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(eq(userId), anyString(), anyString(), anySet(), anyBoolean(), eq(otherTenant)))
                .thenReturn("new-access");
        when(jwtTokenProvider.generateRefreshToken(eq(userId), anyString(), anyString(), anySet(), eq(otherTenant)))
                .thenReturn("new-refresh");

        ActiveTenantService.SwitchOutcome outcome = service.switchTenant(userId, otherTenant);

        assertEquals("new-access", outcome.accessToken());
        assertEquals("new-refresh", outcome.refreshToken());
        assertEquals(otherTenant, outcome.tenantId());
        assertEquals(otherTenant, user.getActiveTenantId(), "le choix doit être persisté sur l'utilisateur");
        verify(userRepository).save(user);
    }

    @Test
    void switchTenantRefusedWithoutActiveMembership() {
        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(user));
        when(membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, otherTenant, MembershipStatus.ACTIVE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.switchTenant(userId, otherTenant));
        assertNull(user.getActiveTenantId());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveTokenTenantKeepsValidActiveTenant() {
        user.setActiveTenantId(otherTenant);
        when(membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, otherTenant, MembershipStatus.ACTIVE))
                .thenReturn(true);

        assertEquals(otherTenant, service.resolveTokenTenantId(user));
    }

    @Test
    void resolveTokenTenantFallsBackToHomeWhenMembershipNoLongerActive() {
        user.setActiveTenantId(otherTenant);
        when(membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, otherTenant, MembershipStatus.ACTIVE))
                .thenReturn(false);

        assertEquals(homeTenant, service.resolveTokenTenantId(user));
        assertNull(user.getActiveTenantId(), "le choix obsolète doit être nettoyé");
        verify(userRepository).save(user);
    }

    @Test
    void resolveTokenTenantWithoutSelectionIsHomeTenant() {
        assertEquals(homeTenant, service.resolveTokenTenantId(user));
        verify(membershipRepository, never()).existsByUserIdAndTenantIdAndStatus(any(), any(), any());
    }
}
