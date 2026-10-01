package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.config.PerIpRateLimiter;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.tenants.domain.AuthorizationService;
import com.discipolat.modules.tenants.domain.Invitation;
import com.discipolat.modules.tenants.domain.InvitationRepository;
import com.discipolat.modules.tenants.domain.InvitationService;
import com.discipolat.modules.tenants.domain.InvitationStatus;
import com.discipolat.modules.tenants.domain.InvitationTokenHasher;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.OrganizationNodeRepository;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.Tenant;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationControllerTest {

    @Mock InvitationRepository invitationRepository;
    @Mock InvitationService invitationService;
    @Mock UserRepository userRepository;
    @Mock TenantMembershipRepository membershipRepository;
    @Mock RoleRepository roleRepository;
    @Mock OrganizationNodeRepository organizationNodeRepository;
    @Mock TenantRepository tenantRepository;
    @Mock AuthorizationService authorizationService;
    @Mock AuditService auditService;
    @Mock EmailService emailService;
    @Mock PerIpRateLimiter rateLimiter;
    @Mock com.discipolat.modules.authentication.domain.SocialInvitationAcceptanceService socialInvitationAcceptanceService;
    @Mock com.discipolat.modules.authentication.api.AuthResponseFactory authResponseFactory;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        UUID.randomUUID(), null, List.of()));
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void validationIsTenantScopedAndNeverCacheable() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("invitee@example.com")
                .tokenHash("hash")
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        Tenant tenant = Tenant.builder().id(tenantId).name("Église Bethel").build();
        User user = User.builder().id(userId).tenantId(tenantId).email(invitation.getEmail()).build();
        when(invitationService.validate("secret-token")).thenReturn(invitation);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findGlobalByEmailIgnoreCase(invitation.getEmail()))
                .thenReturn(Optional.of(user));

        var response = controller().validateInvitation("secret-token");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL)).isEqualTo("no-store");
        assertThat(response.getBody()).containsEntry("accountExists", true);
        assertThat(response.getBody()).containsEntry("tenantName", "Église Bethel");
        assertThat(response.getBody()).doesNotContainKey("token");
        assertThat(response.getBody()).doesNotContainKey("tokenHash");
    }

    @Test
    void resendRotatesOnlyTheHashAndAttemptsEmailDelivery() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = pendingInvitation(tenantId);
        String previousHash = invitation.getTokenHash();
        Tenant tenant = Tenant.builder().id(tenantId).name("Église Bethel").build();
        when(invitationRepository.findByIdForUpdate(invitation.getId())).thenReturn(Optional.of(invitation));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        TenantContext.setTenantId(tenantId);

        var response = controller().resendInvitation(invitation.getId());

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        verify(emailService).send(anyString(), anyString(), anyString());
        assertThat(response.getBody()).containsEntry("emailSent", true);
        assertThat(response.getBody().get("invitationToken")).asString().hasSize(32);
        assertThat(captor.getValue().getTokenHash()).hasSize(64);
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(previousHash);
        assertThat(captor.getValue().getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void cancelUsesTheLockedInvitationAndPersistsCanceledState() {
        UUID tenantId = UUID.randomUUID();
        Invitation invitation = pendingInvitation(tenantId);
        when(invitationRepository.findByIdForUpdate(invitation.getId())).thenReturn(Optional.of(invitation));
        TenantContext.setTenantId(tenantId);

        var response = controller().cancelInvitation(invitation.getId());

        verify(invitationRepository).save(invitation);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.CANCELED);
    }

    private Invitation pendingInvitation(UUID tenantId) {
        return Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email("invitee@example.com")
                .tokenHash("old-hash")
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private InvitationController controller() {
        return new InvitationController(
                invitationRepository,
                invitationService,
                userRepository,
                membershipRepository,
                roleRepository,
                organizationNodeRepository,
                tenantRepository,
                authorizationService,
                auditService,
                emailService,
                rateLimiter,
                // PORT Develop1 — l'acceptation d'invitation enregistre aussi la personne.
                mock(com.discipolat.modules.people.service.PeopleService.class),
                socialInvitationAcceptanceService,
                authResponseFactory,
                "https://app.example.com"
        );
    }

    // ===== A10.1 — pagination et filtres de la liste des invitations =====

    @Test
    @DisplayName("Sans le paramètre `page`, la liste complète est renvoyée (rétro-compatible)")
    void listWithoutPageKeepsTheLegacyFullList() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        when(invitationRepository.findByTenantIdAndStatusIn(any(), any()))
                .thenReturn(List.of(pendingInvitation(tenantId, "a@eglise.com"),
                        pendingInvitation(tenantId, "b@eglise.com")));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        var response = controller().listInvitations(null, null, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat((List<?>) response.getBody()).hasSize(2);
        // La voie paginée n'est pas empruntée.
        verify(invitationRepository, org.mockito.Mockito.never())
                .searchForAdmin(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Avec `page`, la réponse est une PageResponse")
    void listWithPageReturnsPageResponse() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        // `PageImpl(content, pageable, total)` : sans le Pageable explicite,
        // Spring Data prend la taille du contenu (1) au lieu de la taille
        // demandée — ce qui ne correspond pas à ce que renvoie un vrai
        // repository.
        when(invitationRepository.searchForAdmin(any(), any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(
                        List.of(pendingInvitation(tenantId, "a@eglise.com")),
                        org.springframework.data.domain.PageRequest.of(0, 50),
                        1L));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        var response = controller().listInvitations(0, 50, null, null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        // Le contrôleur renvoie bien le DTO `PageResponse` (convention des autres
        // endpoints listés) ; Jackson le sérialiserait en objet JSON.
        assertThat(response.getBody())
                .isInstanceOf(com.discipolat.common.infrastructure.api.PageResponse.class);
        com.discipolat.common.infrastructure.api.PageResponse<?> body =
                (com.discipolat.common.infrastructure.api.PageResponse<?>) response.getBody();
        assertThat(body.content()).hasSize(1);
        assertThat(body.page()).isZero();
        assertThat(body.size()).isEqualTo(50);
        assertThat(body.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Le filtre `status` est transmis en majuscules")
    void listFiltersByStatus() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        when(invitationRepository.searchForAdmin(any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        controller().listInvitations(0, 20, "pending", null);

        verify(invitationRepository).searchForAdmin(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.eq("PENDING"),
                org.mockito.ArgumentMatchers.isNull(),
                any());
    }

    @Test
    @DisplayName("Un statut inconnu est ignoré plutôt que de renvoyer une erreur")
    void unknownStatusFilterIsIgnored() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        when(invitationRepository.searchForAdmin(any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        var response = controller().listInvitations(0, 20, "PEUT-ETRE", null);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(invitationRepository).searchForAdmin(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any());
    }

    @Test
    @DisplayName("La recherche `q` est transmise, et trop courte est ignorée")
    void listSearchQueryIsForwarded() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        when(invitationRepository.searchForAdmin(any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        controller().listInvitations(0, 20, null, "  pasteur@eglise.com  ");
        verify(invitationRepository).searchForAdmin(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("pasteur@eglise.com"),
                any());

        // Une seule lettre ne lancerait pas de recherche couse et renverrait tout.
        controller().listInvitations(0, 20, null, "p");
        verify(invitationRepository).searchForAdmin(
                org.mockito.ArgumentMatchers.eq(tenantId),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any());
    }

    @Test
    @DisplayName("La taille de page est bornée à 200")
    void listPageSizeIsBounded() {
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
        when(invitationRepository.searchForAdmin(any(), any(), any(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        controller().listInvitations(0, 5000, null, null);

        ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(invitationRepository).searchForAdmin(any(), any(), any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(200);
    }

    private Invitation pendingInvitation(UUID tenantId, String email) {
        Invitation invitation = Invitation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .email(email)
                .tokenHash(InvitationTokenHasher.hash(UUID.randomUUID().toString()))
                .role("MEMBER")
                .scopeType(MembershipScopeType.TENANT.name())
                .inviterId(UUID.randomUUID())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        invitation.setCreatedAt(Instant.now());
        return invitation;
    }

}
