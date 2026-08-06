package com.discipolat.common.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Isolation des espaces métiers : les contrôles d'accès reposent sur le rôle
 * ACTIF (espace courant), et non sur l'ensemble des rôles possédés.
 */
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private SecurityUtils buildUtils() {
        return new SecurityUtils(jwtTokenProvider);
    }

    /** Simule un utilisateur authentifié dont le JWT (stocké en credentials) porte le rôle actif. */
    private void authenticateAs(String activeRole, String token) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID(), token, List.of())
        );
    }

    @Test
    void getCurrentUserRole_readsActiveRoleFromJwt() {
        authenticateAs("RESPONSABLE", "jwt-token");
        when(jwtTokenProvider.extractActiveRole("jwt-token")).thenReturn("RESPONSABLE");

        assertEquals("RESPONSABLE", buildUtils().getCurrentUserRole());
    }

    @Test
    void hasActiveRole_matchesOnlyTheActiveRole_notAllOwnedRoles() {
        // L'utilisateur possède RESPONSABLE et CHEF_DE_FAMILLE, mais son espace
        // métier courant est FAISEUR : seuls les contrôles FAISEUR passent.
        authenticateAs("FAISEUR", "jwt-token");
        when(jwtTokenProvider.extractActiveRole("jwt-token")).thenReturn("FAISEUR");
        SecurityUtils utils = buildUtils();

        assertTrue(utils.hasActiveRole("FAISEUR"));
        assertFalse(utils.hasActiveRole("RESPONSABLE"));
        assertFalse(utils.hasActiveRole("RESPONSABLE", "CHEF_DE_FAMILLE"));
    }

    @Test
    void isSuperUser_returnsTrueForActiveAdminAndPasteur() {
        authenticateAs("ADMIN", "t1");
        when(jwtTokenProvider.extractActiveRole("t1")).thenReturn("ADMIN");
        assertTrue(buildUtils().isSuperUser());

        authenticateAs("PASTEUR", "t2");
        when(jwtTokenProvider.extractActiveRole("t2")).thenReturn("PASTEUR");
        assertTrue(buildUtils().isSuperUser());
    }

    @Test
    void isSuperUser_returnsFalseForOperationalActiveRole() {
        authenticateAs("RESPONSABLE", "t");
        when(jwtTokenProvider.extractActiveRole("t")).thenReturn("RESPONSABLE");
        assertFalse(buildUtils().isSuperUser());

        authenticateAs("CHEF_DE_FAMILLE", "t2");
        when(jwtTokenProvider.extractActiveRole("t2")).thenReturn("CHEF_DE_FAMILLE");
        assertFalse(buildUtils().isSuperUser());
    }

    @Test
    void hasActiveRole_returnsFalseWhenNoAuthentication() {
        SecurityContextHolder.clearContext();
        assertFalse(buildUtils().hasActiveRole("ADMIN", "PASTEUR"));
        assertFalse(buildUtils().isSuperUser());
        assertNull(buildUtils().getCurrentUserRole());
    }

    @Test
    void getAllUserRoles_readsAllRolesClaim() {
        authenticateAs("FAISEUR", "t");
        when(jwtTokenProvider.extractRoles("t")).thenReturn(List.of("FAISEUR", "CHEF_DE_FAMILLE"));
        assertEquals(List.of("FAISEUR", "CHEF_DE_FAMILLE"), buildUtils().getAllUserRoles());
    }
}
