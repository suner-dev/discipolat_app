package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.multitenancy.CrossTenantScopeAccess;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.authentication.domain.EmailService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'approbation d'une demande d'adhésion SANS COMPTE doit ouvrir quelque chose.
 *
 * <p>État mesuré avant ce correctif : le chemin {@code APPROVAL} ne crée
 * volontairement aucun compte tant que l'église n'a pas décidé — et n'en
 * créait pas davantage après sa décision. L'approbation envoyait pourtant
 * « Votre demande a été approuvée. <b>Connectez-vous</b>, vous y êtes
 * maintenant membre ». La personne n'avait pas de compte : l'approbation était
 * une impasse, et l'église ne le savait pas.
 *
 * <p>Le correctif émet, à l'approbation, une VRAIE invitation — celle que le
 * produit sait déjà produire et envoyer ({@link InvitationService}), donc avec
 * son jeton unique hashé, son expiration à 7 jours et son lien
 * {@code /accept-invitation?token=…}. Ces tests verrouillent trois choses :
 * <ol>
 *   <li>le cas sans compte émet bien l'invitation, et n'émet plus le message
 *       qui disait « connectez-vous » ;</li>
 *   <li>le cas AVEC compte ne change pas (le compte existe, le message tient) ;</li>
 *   <li>l'approbation n'est JAMAIS remise en cause si l'invitation échoue —
 *       l'admin ne voit pas sa décision disparaître — et la personne n'est
 *       jamais laissée avec une promesse muette.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantJoinServiceApproveRequestTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final String EMAIL = "marie@example.com";
    private static final String CHURCH_NAME = "Église Bethel de Cocody";

    @Mock private JoinCodeService joinCodeService;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private TenantJoinRequestRepository joinRequestRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private ActiveTenantService activeTenantService;
    @Mock private InvitationService invitationService;
    @Mock private AuditService auditService;
    @Mock private CrossTenantScopeAccess crossTenant;
    @Mock private EmailService emailService;
    @Mock private com.discipolat.modules.people.service.PeopleService peopleService;

    private TenantJoinService service;

    @BeforeEach
    void setUp() {
        TenantJoinService.AuthServiceBridge bridge = (email, rawPassword, firstName, lastName,
                phone, tenantSlug, tenantId) -> User.builder().id(UUID.randomUUID()).build();
        service = new TenantJoinService(joinCodeService, tenantRepository, membershipRepository,
                joinRequestRepository, organizationNodeRepository, roleRepository, userRepository,
                activeTenantService, bridge, invitationService, auditService, crossTenant,
                emailService, peopleService);
        when(crossTenant.call(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());

        Tenant tenant = Tenant.builder().id(TENANT_ID).slug("eglise-bethel-de-cocody").name(CHURCH_NAME)
                .status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
    }

    private TenantJoinRequest givenPendingRequest(UUID userId, UUID orgNodeId) {
        TenantJoinRequest request = TenantJoinRequest.builder()
                .id(REQUEST_ID)
                .tenantId(TENANT_ID)
                .userId(userId)
                .email(EMAIL)
                .code("BETHEL-7K2M")
                .orgNodeId(orgNodeId)
                .status(TenantJoinRequest.Status.PENDING)
                .build();
        when(joinRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        return request;
    }

    private void givenInvitationIssued() {
        when(invitationService.createInvitation(any(), any(), anyString(), anyString(),
                any(), any(), any()))
                .thenReturn(new InvitationService.InvitationCreationResult(
                        InvitationService.InvitationCreationKind.INVITATION,
                        UUID.randomUUID(), null, EMAIL, "MEMBRE", "TENANT", null,
                        "jeton", "https://app/accept-invitation?token=jeton", true, false, false));
    }

    // ---------- le trou refermé ----------

    @Test
    @DisplayName("Approbation sans compte : une VRAIE invitation est émise")
    void approvalWithoutAccountIssuesInvitation() {
        givenPendingRequest(null, null);
        givenInvitationIssued();

        service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        verify(invitationService).createInvitation(eq(TENANT_ID), eq(ADMIN_ID), eq(EMAIL),
                eq("MEMBRE"), eq(MembershipScopeType.TENANT), isNull(), isNull());
    }

    @Test
    @DisplayName("Approbation sans compte : PLUS AUCUN email « connectez-vous »")
    void approvalWithoutAccountNeverPromisesToLogIn() {
        givenPendingRequest(null, null);
        givenInvitationIssued();

        service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        // L'invitation elle-même porte l'email : c'est InvitationService qui
        // l'envoie, avec le lien. Ce service ne doit donc plus rien promettre.
        verify(emailService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Approbation sans compte : la demande reste APPROVED et journalisée")
    void approvalWithoutAccountStillMarksRequestApproved() {
        TenantJoinRequest request = givenPendingRequest(null, null);
        givenInvitationIssued();

        service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        assertThat(request.getStatus()).isEqualTo(TenantJoinRequest.Status.APPROVED);
        assertThat(request.getHandledBy()).isEqualTo(ADMIN_ID);
        verify(joinRequestRepository).save(request);
        verify(auditService).logSimple("JOIN_REQUEST_APPROVED", "TENANT", TENANT_ID);
        verify(auditService).logSimple("JOIN_REQUEST_APPROVED_INVITATION_ISSUED", "TENANT", TENANT_ID);
    }

    @Test
    @DisplayName("Approbation SANS compte ne renvoie aucune session : il n'y a rien à ouvrir")
    void approvalWithoutAccountIssuesNoSession() {
        givenPendingRequest(null, null);
        givenInvitationIssued();

        TenantJoinService.ApprovalOutcome outcome = service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        assertThat(outcome.status()).isEqualTo("INVITATION_SENT");
        assertThat(outcome.hasSession()).isFalse();
        verify(activeTenantService, never()).switchTenant(any(), any());
    }

    @Test
    @DisplayName("Approbation sans compte : le lien est POSÉ sur la demande et l’envoi est horodaté")
    void approvalStampsTheInvitationLinkOnTheRequest() {
        TenantJoinRequest request = givenPendingRequest(null, null);
        givenInvitationIssued();

        TenantJoinService.ApprovalOutcome outcome = service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        // V253 : sans ce lien, la demande approuvée reste introuvable et sa
        // relance impossible — c'est exactement le cul-de-sac qu'on ferme.
        assertThat(request.getInvitationId()).isNotNull();
        assertThat(request.getInvitationSentAt()).isNotNull();
        assertThat(outcome.applicantEmail()).isEqualTo(EMAIL);
        assertThat(outcome.emailSent()).isTrue();
    }

    @Test
    @DisplayName("Approbation sans compte, email parti dans le vide : le retour dit NON")
    void approvalReportsEmailFailure() {
        TenantJoinRequest request = givenPendingRequest(null, null);
        when(invitationService.createInvitation(any(), any(), anyString(), anyString(),
                any(), any(), any()))
                .thenReturn(new InvitationService.InvitationCreationResult(
                        InvitationService.InvitationCreationKind.INVITATION,
                        UUID.randomUUID(), null, EMAIL, "MEMBRE", "TENANT", null,
                        "jeton", "https://app/accept-invitation?token=jeton", false, false, false));

        TenantJoinService.ApprovalOutcome outcome = service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        // L'invitation EXISTE (donc relance possible) mais personne ne l'a reçue :
        // c'est au responsable de le savoir, sinon il croira avoir welcoming
        // quelqu'un alors que la personne est encore outside.
        assertThat(outcome.emailSent()).isFalse();
        assertThat(request.getInvitationId()).isNotNull();
        assertThat(request.getInvitationSentAt()).isNull();
    }

    // ---------- la sous-église ----------

    @Test
    @DisplayName("Demande visant un nœud : l'invitation est scopée sur ce nœud, pas sur l'église entière")
    void approvalScopesInvitationToTheRequestedNode() {
        UUID nodeId = UUID.randomUUID();
        givenPendingRequest(null, nodeId);
        givenInvitationIssued();
        when(organizationNodeRepository.findById(nodeId)).thenReturn(Optional.of(
                OrganizationNode.builder().id(nodeId).tenantId(TENANT_ID)
                        .type(OrganizationNodeType.SUB_CHURCH).build()));

        service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        // La demande et l'invitation doivent porter le MÊME périmètre : sinon la
        // personne entre dans l'église entière alors qu'elle avait demandé un
        // campus précis.
        verify(invitationService).createInvitation(eq(TENANT_ID), eq(ADMIN_ID), eq(EMAIL),
                eq("MEMBRE"), eq(MembershipScopeType.SUB_CHURCH), eq(nodeId), eq(nodeId));
    }

    // ---------- non-régression du cas AVEC compte ----------

    @Test
    @DisplayName("Approbation AVEC compte : aucune invitation, le message « connectez-vous » tient")
    void approvalWithExistingAccountIsUnchanged() {
        UUID userId = UUID.randomUUID();
        givenPendingRequest(userId, null);
        when(userRepository.findById(userId)).thenReturn(Optional.of(
                User.builder().id(userId).email(EMAIL).firstName("Marie").lastName("Kouassi").build()));
        Role memberRole = Role.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).key("MEMBRE").build();
        when(roleRepository.findByTenantIdAndKey(eq(TENANT_ID), eq("MEMBRE")))
                .thenReturn(Optional.of(memberRole));
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(activeTenantService.switchTenant(any(), any()))
                .thenReturn(new ActiveTenantService.SwitchOutcome(TENANT_ID, "a1", "r1", "MEMBRE"));

        TenantJoinService.ApprovalOutcome outcome = service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        // Ici le compte EXISTE : « connectez-vous » est vrai, et l'invitation
        // serait inutile.
        verify(invitationService, never()).createInvitation(any(), any(), any(), any(), any(), any(), any());
        verify(emailService).send(anyString(), anyString(), anyString());
        assertThat(outcome.hasSession()).isTrue();
    }

    // ---------- l'approbation ne doit JAMAIS être remise en cause ----------

    @Test
    @DisplayName("Invitation impossible : la demande reste approuvée, et la personne est prévenue")
    void invitationFailureNeverUndoesTheApproval() {
        TenantJoinRequest request = givenPendingRequest(null, null);
        when(invitationService.createInvitation(any(), any(), anyString(), anyString(),
                any(), any(), any()))
                .thenThrow(new DomainException("Une invitation en attente existe déjà",
                        org.springframework.http.HttpStatus.BAD_REQUEST, "INVITATION_ALREADY_PENDING"));

        // Ne doit PAS lever : l'admin reverrait sa demande repasser en attente.
        TenantJoinService.ApprovalOutcome outcome = service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        assertThat(outcome.status()).isEqualTo("INVITATION_SENT");
        // Aucune invitation émise → aucune échéance : l'écran reçoit `null` et
        // doit en faire un avertissement, pas un silence.
        assertThat(outcome.invitationExpiresAt()).isNull();
        assertThat(outcome.emailSent()).isFalse();
        assertThat(request.getStatus()).isEqualTo(TenantJoinRequest.Status.APPROVED);

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(anyString(), anyString(), body.capture());
        // Le message ne promet ni connexion immédiate, ni lien déjà envoyé.
        assertThat(body.getValue())
                .doesNotContain("Connectez-vous")
                .contains("doit vous renvoyer votre lien");
    }

    @Test
    @DisplayName("Demande sans email ET sans compte : ni invitation, ni email à une adresse vide")
    void requestWithoutEmailAndAccountInvitesNobody() {
        TenantJoinRequest request = TenantJoinRequest.builder()
                .id(REQUEST_ID).tenantId(TENANT_ID).email("   ")
                .code("BETHEL-7K2M").status(TenantJoinRequest.Status.PENDING).build();
        when(joinRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        verify(invitationService, never()).createInvitation(any(), any(), any(), any(), any(), any(), any());
        verify(emailService, never()).send(anyString(), anyString(), anyString());
        assertThat(request.getStatus()).isEqualTo(TenantJoinRequest.Status.APPROVED);
    }

    // ---------- garde-fous existants, inchangés ----------

    @Test
    @DisplayName("Demande déjà traitée : refus, aucune invitation")
    void alreadyHandledRequestIsRejected() {
        TenantJoinRequest request = givenPendingRequest(null, null);
        request.setStatus(TenantJoinRequest.Status.REJECTED);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.approveRequest(REQUEST_ID, TENANT_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Demande déjà traitée");

        verify(invitationService, never()).createInvitation(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Demande d’une autre église : refus, aucune invitation")
    void foreignRequestIsRejected() {
        givenPendingRequest(null, null);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.approveRequest(REQUEST_ID, UUID.randomUUID(), ADMIN_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Demande introuvable");

        verify(invitationService, never()).createInvitation(any(), any(), any(), any(), any(), any(), any());
    }
}
