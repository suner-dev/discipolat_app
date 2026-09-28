package com.discipolat.modules.platform.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.AuthService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Constat B3 — le propriétaire (owner) est OBLIGATOIRE à la création d'un tenant.
 */
@ExtendWith(MockitoExtension.class)
class TenantOwnerProvisioningServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private AuthService authService;
    @Mock private AuditService auditService;

    private PasswordEncoder passwordEncoder;
    private TenantOwnerProvisioningService service;
    private UUID tenantId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        service = new TenantOwnerProvisioningService(userRepository, membershipRepository,
                authService, passwordEncoder, auditService);
        tenantId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    // ---------- création ----------

    @Test
    @DisplayName("Création : utilisateur PENDING_ACTIVATION + membership TENANT_OWNER + email")
    void createsOwnerUserAndMembership() {
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());

        TenantOwnerProvisioningService.OwnerProvisioningResult result =
                service.provisionOwner(tenantId, "Pasteur@Eglise.com", "Jean", "Dupont", actorId);

        assertThat(result.userId()).isNotNull();
        assertThat(result.email()).isEqualTo("pasteur@eglise.com");
        assertThat(result.activationEmailSent()).isTrue();
        assertThat(result.alreadyMember()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User owner = userCaptor.getValue();
        assertThat(owner.getTenantId()).isEqualTo(tenantId);
        assertThat(owner.getEmail()).isEqualTo("pasteur@eglise.com");
        assertThat(owner.getRole()).isEqualTo(UserRole.PASTEUR);
        assertThat(owner.getRoles()).containsExactlyInAnyOrder(UserRole.PASTEUR);
        assertThat(owner.getActiveRole()).isEqualTo(UserRole.PASTEUR);
        assertThat(owner.getStatut()).isEqualTo(UserStatus.PENDING_ACTIVATION);
        assertThat(owner.getFirstName()).isEqualTo("Jean");
        assertThat(owner.getLastName()).isEqualTo("Dupont");

        ArgumentCaptor<TenantMembership> membershipCaptor = ArgumentCaptor.forClass(TenantMembership.class);
        verify(membershipRepository).save(membershipCaptor.capture());
        assertThat(membershipCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(membershipCaptor.getValue().getRoleLegacy()).isEqualTo("TENANT_OWNER");
        assertThat(membershipCaptor.getValue().getScopeType()).isEqualTo(MembershipScopeType.TENANT);
        assertThat(membershipCaptor.getValue().getStatus()).isEqualTo(MembershipStatus.ACTIVE);

        verify(authService).sendActivationEmail(result.userId());
        verify(auditService).log(eq(actorId), eq(tenantId), eq("TENANT_OWNER_PROVISIONED"),
                eq("USER"), eq(result.userId()), eq("SUCCESS"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Le mot de passe initial est aléatoire, long et JAMAIS communiqué")
    void initialPasswordIsRandomStrongAndNeverLeaked() {
        when(userRepository.findGlobalByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());

        service.provisionOwner(tenantId, "a@eglise.com", "A", "B", actorId);
        service.provisionOwner(tenantId, "b@eglise.com", "A", "B", actorId);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        List<String> hashes = captor.getAllValues().stream().map(User::getPasswordHash).toList();
        assertThat(hashes).hasSize(2).doesNotHaveDuplicates();
        // BCrypt d'un mot de passe aléatoire de 32 caractères.
        assertThat(hashes).allSatisfy(hash -> {
            assertThat(hash).startsWith("$2");
            assertThat(passwordEncoder.matches("password123", hash)).isFalse();
        });
        // Le mot de passe en clair n'apparaît nulle part dans la sortie.
        assertThat(service.randomInitialPassword()).hasSize(TenantOwnerProvisioningService.INITIAL_PASSWORD_LENGTH);
        assertThat(service.randomInitialPassword())
                .isNotEqualTo(service.randomInitialPassword());
    }

    // ---------- email déjà utilisé ----------

    @Test
    @DisplayName("Email déjà porté par un compte d'un AUTRE tenant : 409, AUCUNE écriture")
    void refusesEmailAlreadyUsedInAnotherTenant() {
        User foreign = User.builder().id(UUID.randomUUID()).tenantId(UUID.randomUUID())
                .email("pasteur@eglise.com").build();
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com"))
                .thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.provisionOwner(
                tenantId, "pasteur@eglise.com", "Jean", "Dupont", actorId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(problem.getTitle()).isEqualTo("OWNER_EMAIL_ALREADY_USED");
                });

        // Aucune création partielle : ni utilisateur, ni membership, ni email.
        verify(userRepository, never()).save(any(User.class));
        verify(membershipRepository, never()).save(any(TenantMembership.class));
        verifyNoInteractions(authService);
    }

    // ---------- idempotence ----------

    @Test
    @DisplayName("Owner déjà membre de ce tenant : aucun doublon de membership")
    void doesNotDuplicateMembershipWhenOwnerAlreadyMember() {
        User existing = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .email("pasteur@eglise.com").build();
        TenantMembership membership = TenantMembership.builder()
                .userId(existing.getId()).tenantId(tenantId)
                .scopeType(MembershipScopeType.TENANT).status(MembershipStatus.ACTIVE).build();
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com"))
                .thenReturn(Optional.of(existing));
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(
                existing.getId(), tenantId, MembershipStatus.ACTIVE)).thenReturn(List.of(membership));

        TenantOwnerProvisioningService.OwnerProvisioningResult result =
                service.provisionOwner(tenantId, "pasteur@eglise.com", "Jean", "Dupont", actorId);

        assertThat(result.alreadyMember()).isTrue();
        assertThat(result.userId()).isEqualTo(existing.getId());
        verify(membershipRepository, never()).save(any(TenantMembership.class));
        verify(userRepository, never()).save(any(User.class));
        // L'email d'activation est tout de même renvoyé : le compte existe peut-être
        // mais n'a jamais été activé.
        verify(authService).sendActivationEmail(existing.getId());
    }

    // ---------- robustesse email (D10) ----------

    @Test
    @DisplayName("SMTP cassé : le tenant est provisionné, activationEmailSent=false, aucune exception")
    void smtpFailureNeverBreaksProvisioning() {
        when(userRepository.findGlobalByEmailIgnoreCase("pasteur@eglise.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("SMTP indisponible"))
                .when(authService).sendActivationEmail(any());

        TenantOwnerProvisioningService.OwnerProvisioningResult result =
                service.provisionOwner(tenantId, "pasteur@eglise.com", "Jean", "Dupont", actorId);

        assertThat(result.activationEmailSent()).isFalse();
        assertThat(result.userId()).isNotNull();
        verify(membershipRepository).save(any(TenantMembership.class));
    }

    // ---------- entrées invalides ----------

    @Test
    @DisplayName("Email absent : 400 OWNER_REQUIRED")
    void refusesBlankEmail() {
        assertThatThrownBy(() -> service.provisionOwner(tenantId, "   ", "Jean", "Dupont", actorId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(problem.getTitle()).isEqualTo("OWNER_REQUIRED");
                });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("tenantId absent : 400 TENANT_REQUIRED")
    void refusesMissingTenant() {
        assertThatThrownBy(() -> service.provisionOwner(null, "a@eglise.com", "A", "B", actorId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("TENANT_REQUIRED"));
    }
}
