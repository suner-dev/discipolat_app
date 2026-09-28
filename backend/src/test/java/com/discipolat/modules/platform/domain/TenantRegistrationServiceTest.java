package com.discipolat.modules.platform.domain;

import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.compliance.domain.ComplianceService;
import com.discipolat.modules.compliance.domain.LegalDocumentService;
import com.discipolat.modules.tenants.api.TenantResponse;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.OrganizationNodeStatus;
import com.discipolat.modules.tenants.domain.OrganizationNodeType;
import com.discipolat.modules.tenants.domain.Role;
import com.discipolat.modules.tenants.domain.RoleRepository;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.domain.TenantService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRegistrationServiceTest {

    @Mock
    private TenantRegistrationRequestRepository requestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TenantService tenantService;
    @Mock
    private OrganizationNodeService organizationNodeService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TenantMembershipRepository membershipRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private ComplianceService complianceService;
    @Mock
    private LegalDocumentService legalDocumentService;
    @Mock
    private com.discipolat.modules.authentication.domain.EmailService emailService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static TenantRegistrationService.ConsentInfo consentOk() {
        return new TenantRegistrationService.ConsentInfo(true, true, true, "2026-09-01-v1", "127.0.0.1", "JUnit");
    }

    @Test
    void publicSubmissionDoesNotCreateTenantOrUser() {
        when(requestRepository.findByEmailIgnoreCase("demandeur@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(requestRepository.save(any(TenantRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TenantRegistrationService service = service();

        TenantRegistrationRequest request = service.submit(
                "demandeur@example.com", "password123", "Jean", "Test", "0700000000", null, consentOk());

        assertThat(request.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_APPROVAL);
        assertThat(request.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(request.getPasswordHash()).isNotEqualTo("password123");
        // Preuve RGPD art. 7 capturée dès la demande
        assertThat(request.isConsentCgu()).isTrue();
        assertThat(request.isConsentPrivacy()).isTrue();
        assertThat(request.isConsentArt9()).isTrue();
        assertThat(request.getConsentTermsVersion()).isEqualTo("2026-09-01-v1");
        verify(tenantService, never()).create(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void submissionWithoutConsentsIsRejected() {
        // `findByEmailIgnoreCase` et non `findByEmail` : l'email est une identite
        // globale unique insensible a la casse (constat B4 / V185).
        when(requestRepository.findByEmailIgnoreCase("sans-consentement@example.com"))
                .thenReturn(Optional.empty());
        TenantRegistrationService service = service();

        assertThatThrownBy(() -> service.submit(
                "sans-consentement@example.com", "password123", "Jean", "Test", null, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("consentements");
        // Aucune ecriture en base : une demande sans consentement ne doit pas
        // laisser de trace, et son email de recu ne doit donc pas partir non plus.
        verify(requestRepository, never()).save(any(TenantRegistrationRequest.class));
    }

    @Test
    void publicSubmissionNormalizesTheSelectedPublicPlan() {
        when(requestRepository.findByEmailIgnoreCase("growth@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(requestRepository.save(any(TenantRegistrationRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TenantRegistrationRequest request = service().submit(
                "growth@example.com", "password123", "Jean", "Test", null, "growth", consentOk());

        assertThat(request.getPlan()).isEqualTo("GROWTH");
    }

    @Test
    void approvalCreatesOwnerAndTenantMembership() {
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SecurityTestHelper.loginAs(UUID.randomUUID());
        TenantRegistrationRequest request = TenantRegistrationRequest.builder()
                .id(requestId).email("owner@example.com").passwordHash("hashed-password")
                .firstName("Owner").lastName("Test").organizationName("Owner Test")
                .slug("owner-test").plan("free").country("CM").currency("XAF")
                .timezone("Africa/Douala").locale("fr")
                .status(TenantRegistrationStatus.PENDING_APPROVAL).build();
        TenantResponse tenant = new TenantResponse(tenantId, "Owner Test", "owner-test",
                com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE, "free", "CM", "XAF",
                "Africa/Douala", "fr", null, null, null, null, Instant.now(), Instant.now());
        OrganizationNode church = OrganizationNode.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .name("Owner Test").type(OrganizationNodeType.ROOT_CHURCH).code("ROOT")
                .path("ROOT").level(0).status(OrganizationNodeStatus.ACTIVE).build();
        User owner = User.builder().id(userId).email("owner@example.com").tenantId(tenantId).build();
        Role ownerRole = Role.builder().id(UUID.randomUUID()).key("TENANT_OWNER").build();
        request.setConsentCgu(true);
        request.setConsentPrivacy(true);
        request.setConsentArt9(true);
        request.setConsentTermsVersion("2026-09-01-v1");
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(tenantService.create(any())).thenReturn(tenant);
        when(organizationNodeService.createRootChurch(any(), any(), any(), any())).thenReturn(church);
        when(userRepository.findGlobalByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(roleRepository.findGlobalByKey("TENANT_OWNER")).thenReturn(Optional.of(ownerRole));

        TenantRegistrationService.ApprovalResult result = service().approve(requestId, "approved");

        assertThat(result.tenant()).isEqualTo(tenant);
        assertThat(result.owner()).isEqualTo(owner);
        assertThat(request.getStatus()).isEqualTo(TenantRegistrationStatus.APPROVED);
        verify(membershipRepository).save(any());
        verify(requestRepository).save(request);
        // Les consentements doivent être matérialisés dans le journal RGPD du tenant
        // (3e paramètre : boolean primitif → anyBoolean, jamais any()).
        verify(complianceService, times(3)).logConsent(any(), any(), anyBoolean(), any(), any(), any(), any());
        verify(complianceService).logConsent(any(), eq("CGU"), eq(true), any(), any(), any(), any());
        verify(complianceService).logConsent(any(), eq("PRIVACY"), eq(true), any(), any(), any(), any());
        verify(complianceService).logConsent(any(), eq("CONSENT_ART9"), eq(true), any(), any(), any(), any());
        verify(auditService).log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    private TenantRegistrationService service() {
        return new TenantRegistrationService(requestRepository, userRepository, passwordEncoder,
                tenantService, organizationNodeService, roleRepository, membershipRepository, auditService,
                complianceService, legalDocumentService,
                emailService, "https://app.example.com");
    }
}
