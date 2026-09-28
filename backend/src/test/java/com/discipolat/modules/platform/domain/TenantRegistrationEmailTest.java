package com.discipolat.modules.platform.domain;

import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat M2 — l'inscription d'une église était **100 % silencieuse** : aucune
 * notification, à aucun moment du parcours (dépôt, approbation, rejet).
 *
 * <p>Ces tests prouvent que les trois emails existent, qu'ils sont tentés, et
 * qu'un échec SMTP ne casse jamais la transaction métier (décision D10).
 */
@ExtendWith(MockitoExtension.class)
class TenantRegistrationEmailTest {

    private static final String FRONTEND_URL = "https://app.example.com";

    @Mock private TenantRegistrationRequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantService tenantService;
    @Mock private OrganizationNodeService organizationNodeService;
    @Mock private RoleRepository roleRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private AuditService auditService;
    @Mock private EmailService emailService;
    // Le service exige aussi la preuve de consentement RGPD (art. 9) introduite
    // sur la branche distante : sans ces deux dependances, la soumission echouerait
    // en CONSENT_REQUIRED avant d'atteindre l'envoi de l'email.
    @Mock private com.discipolat.modules.compliance.domain.ComplianceService complianceService;
    @Mock private com.discipolat.modules.compliance.domain.LegalDocumentService legalDocumentService;

    private TenantRegistrationService service;

    /**
     * Preuve de consentement RGPD : les trois consentements sont obligatoires
     * (art. 9 pour les données religieuses) et sont verifies AVANT toute
     * ecriture. Une soumission sans consentement ne doit donc jamais produire
     * d'email de recu — c'est le comportement attendu, pas une regression.
     */
    private static TenantRegistrationService.ConsentInfo consentComplet() {
        return new TenantRegistrationService.ConsentInfo(
                true, true, true, "2026-01-01", "203.0.113.7", "JUnit");
    }

    @BeforeEach
    void setUp() {
        service = new TenantRegistrationService(requestRepository, userRepository, passwordEncoder,
                tenantService, organizationNodeService, roleRepository, membershipRepository,
                auditService, complianceService, legalDocumentService,
                emailService, FRONTEND_URL);
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.logout();
    }

    // ---------- 1 — dépôt de la demande ----------

    @Test
    @DisplayName("Soumission : email de réception envoyé")
    void submitSendsRegistrationReceived() {
        when(requestRepository.findByEmailIgnoreCase("demandeur@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("MotDePasse1")).thenReturn("hash");
        when(requestRepository.save(any(TenantRegistrationRequest.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.submit("Demandeur@Example.com", "MotDePasse1", "Jean", "Dupont", null,
                null, consentComplet());

        verify(emailService).sendRegistrationReceived("demandeur@example.com", "Jean");
    }

    @Test
    @DisplayName("Soumission : échec SMTP (retour false) n'affecte pas la demande enregistrée")
    void submitSurvivesSmtpFailure() {
        when(requestRepository.findByEmailIgnoreCase("demandeur@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("MotDePasse1")).thenReturn("hash");
        when(requestRepository.save(any(TenantRegistrationRequest.class)))
                .thenAnswer(i -> i.getArgument(0));
        // Décision D10 : `EmailService` renvoie `false` et ne lève jamais.
        when(emailService.sendRegistrationReceived(anyString(), anyString())).thenReturn(false);

        TenantRegistrationRequest saved = service.submit(
                "demandeur@example.com", "MotDePasse1", "Jean", "Dupont", null,
                null, consentComplet());

        // La demande est bien enregistrée, avec son statut PENDING_APPROVAL,
        // malgré l'échec d'envoi de l'email.
        assertThat(saved.getStatus()).isEqualTo(TenantRegistrationStatus.PENDING_APPROVAL);
        assertThat(saved.getEmail()).isEqualTo("demandeur@example.com");
        verify(requestRepository).save(any(TenantRegistrationRequest.class));
        verify(emailService).sendRegistrationReceived("demandeur@example.com", "Jean");
    }

    // ---------- 2 — approbation ----------

    @Test
    @DisplayName("Approbation : email d'approbation avec le lien de connexion")
    void approveSendsRegistrationApproved() {
        SecurityTestHelper.loginAs(UUID.randomUUID());
        UUID requestId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        TenantRegistrationRequest request = pendingRequest("demandeur@example.com");
        request.setId(requestId);
        TenantResponse tenant = new TenantResponse(tenantId, "Demandeur — Église", "demandeur-eglise",
                com.discipolat.modules.tenants.domain.TenantStatus.ACTIVE, "DISCOVERY", "CM", "XAF",
                "Africa/Douala", "fr", null, null, null, null, Instant.now(), Instant.now());
        OrganizationNode church = OrganizationNode.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .name("Demandeur — Église").type(OrganizationNodeType.ROOT_CHURCH).code("ROOT").build();
        User owner = User.builder().id(UUID.randomUUID()).tenantId(tenantId)
                .email("demandeur@example.com").build();
        Role ownerRole = Role.builder().id(UUID.randomUUID()).tenantId(null).key("TENANT_OWNER").build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(tenantService.create(any())).thenReturn(tenant);
        when(organizationNodeService.createRootChurch(any(), any(), any(), any())).thenReturn(church);
        when(userRepository.findGlobalByEmail("demandeur@example.com")).thenReturn(Optional.of(owner));
        when(roleRepository.findGlobalByKey("TENANT_OWNER")).thenReturn(Optional.of(ownerRole));
        when(requestRepository.save(any(TenantRegistrationRequest.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.approve(requestId, "Dossier complet");

        verify(emailService).sendRegistrationApproved(
                eq("demandeur@example.com"), eq("Jean"), eq(FRONTEND_URL + "/login"));
        assertThat(request.getStatus()).isEqualTo(TenantRegistrationStatus.APPROVED);
    }

    // ---------- 3 — rejet ----------

    @Test
    @DisplayName("Rejet : email de rejet AVEC le motif communiqué")
    void rejectSendsRegistrationRejected() {
        SecurityTestHelper.loginAs(UUID.randomUUID());
        UUID requestId = UUID.randomUUID();
        TenantRegistrationRequest request = pendingRequest("demandeur@example.com");
        request.setId(requestId);
        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(TenantRegistrationRequest.class)))
                .thenAnswer(i -> i.getArgument(0));

        service.reject(requestId, "Informations complémentaires nécessaires");

        verify(emailService).sendRegistrationRejected(
                eq("demandeur@example.com"), eq("Jean"), eq("Informations complémentaires nécessaires"));
        assertThat(request.getStatus()).isEqualTo(TenantRegistrationStatus.REJECTED);
        assertThat(request.getDecisionReason()).isEqualTo("Informations complémentaires nécessaires");
    }

    // ---------- helper ----------

    private TenantRegistrationRequest pendingRequest(String email) {
        TenantRegistrationRequest request = TenantRegistrationRequest.builder()
                .email(email)
                .firstName("Jean")
                .lastName("Dupont")
                .status(TenantRegistrationStatus.PENDING_APPROVAL)
                .build();
        request.setPasswordHash("hash");
        request.setOrganizationName("Demandeur — Église");
        request.setSlug("demandeur-eglise");
        request.setPlan("DISCOVERY");
        return request;
    }
}
