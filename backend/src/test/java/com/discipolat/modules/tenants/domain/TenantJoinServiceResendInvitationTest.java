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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <b>Trou 1 — l'invitation expirée est un cul-de-sac.</b>
 *
 * <p>État mesuré : le token n'est stocké que haché, donc pas rejouable. Le lien
 * expire 7 jours après l'émission. La demande passe {@code APPROVED} — or
 * {@code GET /tenant/join-requests} ne renvoyait que les {@code PENDING}. À J+8,
 * la personne ne pouvait plus rien demander et le responsable n'avait aucun
 * moyen de l'aider. La seule sortie était un effet de bord : repasser par
 * {@code /register} pour créer une NOUVELLE demande.
 *
 * <p>Ces tests verrouillent la relance : elle régénère un jeton RÉELLEMENT
 * valide (pas un lien reconstruit qui ne fonctionnerait pas), elle est refusée
 * quand elle n'a pas de sens, et elle ne laisse jamais l'appelant croire que
 * l'email est parti quand il est parti dans le vide.
 *
 * <p><b>Trou 2 — {@code JOINED} mentait.</b> L'approbation d'une demande sans
 * compte renvoyait {@code JOINED} alors que personne n'avait rejoint : une
 * invitation avait été émise. L'écran affichait « Demande approuvée » sans que
 * quiconque ne soit membre, et sans dire si le mail était parti.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantJoinServiceResendInvitationTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID REQUEST_ID = UUID.randomUUID();
    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID INVITATION_ID = UUID.randomUUID();
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
    private Instant expiry;

    @BeforeEach
    void setUp() {
        TenantJoinService.AuthServiceBridge bridge = (email, rawPassword, firstName, lastName,
                phone, tenantSlug, tenantId) -> User.builder().id(UUID.randomUUID()).build();
        service = new TenantJoinService(joinCodeService, tenantRepository, membershipRepository,
                joinRequestRepository, organizationNodeRepository, roleRepository, userRepository,
                activeTenantService, bridge, invitationService, auditService, crossTenant,
                emailService, peopleService);
        when(crossTenant.call(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(
                Tenant.builder().id(TENANT_ID).slug("bethel").name(CHURCH_NAME)
                        .status(TenantStatus.ACTIVE).build()));
        expiry = Instant.now().plusSeconds(7 * 24 * 3600);
    }

    private TenantJoinRequest givenApprovedRequest(UUID userId, UUID invitationId) {
        TenantJoinRequest request = TenantJoinRequest.builder()
                .id(REQUEST_ID).tenantId(TENANT_ID).userId(userId).email(EMAIL)
                .code("BETHEL-7K2M").status(TenantJoinRequest.Status.APPROVED)
                .invitationId(invitationId).invitationSentAt(Instant.now())
                .build();
        when(joinRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        return request;
    }

    private void givenReissueSucceeds() {
        when(invitationService.reissue(eq(INVITATION_ID), eq(TENANT_ID), eq(ADMIN_ID)))
                .thenReturn(new InvitationService.ReissueResult(
                        INVITATION_ID, EMAIL, expiry, true, "PENDING"));
    }

    // ---------- la relance fonctionne ----------

    @Test
    @DisplayName("Relance : le jeton est RÉGÉNÉRÉ, pas rejoué")
    void resendRegeneratesTheToken() {
        givenApprovedRequest(null, INVITATION_ID);
        givenReissueSucceeds();

        TenantJoinService.ApprovalOutcome outcome =
                service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        // C'est le service d'invitation qui régénère : le token n'étant stocké
        // que haché, un « renvoi » du même lien ne fonctionnerait pas.
        verify(invitationService).reissue(eq(INVITATION_ID), eq(TENANT_ID), eq(ADMIN_ID));
        assertThat(outcome.status()).isEqualTo("INVITATION_SENT");
        assertThat(outcome.applicantEmail()).isEqualTo(EMAIL);
        assertThat(outcome.emailSent()).isTrue();
        assertThat(outcome.invitationExpiresAt()).isEqualTo(expiry);
        assertThat(outcome.hasSession()).isFalse();
    }

    @Test
    @DisplayName("Relance : la demande est journalisée et sa date d’envoi rafraîchie")
    void resendIsAuditedAndStamped() {
        TenantJoinRequest request = givenApprovedRequest(null, INVITATION_ID);
        Instant before = request.getInvitationSentAt();
        givenReissueSucceeds();

        service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        assertThat(request.getInvitationSentAt()).isAfter(before);
        verify(joinRequestRepository).save(request);
        verify(auditService).logSimple("JOIN_REQUEST_INVITATION_RESENT", "TENANT", TENANT_ID);
    }

    @Test
    @DisplayName("Relance : l’approbation n’est PAS touchée, et aucune session n’est émise")
    void resendNeverReapprovesNorIssuesSession() {
        givenApprovedRequest(null, INVITATION_ID);
        givenReissueSucceeds();

        service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        // Une relance n'est pas une nouvelle approbation : si elle changeait le
        // statut, un bug pourrait faire réapparaître la demande « à approuver ».
        assertThat(joinRequestRepository.findById(REQUEST_ID).orElseThrow().getStatus())
                .isEqualTo(TenantJoinRequest.Status.APPROVED);
        verify(activeTenantService, never()).switchTenant(any(), any());
    }

    // ---------- honnêteté du retour ----------

    @Test
    @DisplayName("Email parti dans le vide : le retour dit NON, et l’ancien horodatage est conservé")
    void emailFailureIsReportedAsFailure() {
        TenantJoinRequest request = givenApprovedRequest(null, INVITATION_ID);
        Instant before = request.getInvitationSentAt();
        when(invitationService.reissue(eq(INVITATION_ID), eq(TENANT_ID), eq(ADMIN_ID)))
                .thenReturn(new InvitationService.ReissueResult(
                        INVITATION_ID, EMAIL, expiry, false, "PENDING"));

        TenantJoinService.ApprovalOutcome outcome =
                service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        assertThat(outcome.emailSent()).isFalse();
        // Un envoi raté ne doit PAS rafraîchir « envoyé à », sinon l'écran
        // afficherait un lien comme récent alors qu'il n'est jamais parti.
        assertThat(request.getInvitationSentAt()).isEqualTo(before);
    }

    // ---------- refus : quand une relance n'a pas de sens ----------

    @Test
    @DisplayName("Demande pas encore approuvée : relance refusée")
    void pendingRequestCannotBeResent() {
        TenantJoinRequest request = TenantJoinRequest.builder()
                .id(REQUEST_ID).tenantId(TENANT_ID).email(EMAIL).code("BETHEL-7K2M")
                .status(TenantJoinRequest.Status.PENDING).build();
        when(joinRequestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getCode())
                .isEqualTo("JOIN_REQUEST_NOT_APPROVED");
        verify(invitationService, never()).reissue(any(), any(), any());
    }

    @Test
    @DisplayName("Adhésion déjà effective (compte rattaché) : relance refusée — sinon double entrée")
    void requestWithAccountCannotBeResent() {
        givenApprovedRequest(UUID.randomUUID(), null);

        assertThatThrownBy(() -> service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getCode())
                .isEqualTo("JOIN_REQUEST_NOT_RESENDABLE");
        verify(invitationService, never()).reissue(any(), any(), any());
    }

    @Test
    @DisplayName("Demande d’une autre église : refus, et l’ existence n’est pas révélée")
    void foreignRequestCannotBeResent() {
        givenApprovedRequest(null, INVITATION_ID);

        assertThatThrownBy(() -> service.resendInvitationForRequest(REQUEST_ID, UUID.randomUUID(), ADMIN_ID))
                .isInstanceOf(DomainException.class)
                .hasMessage("Demande introuvable")
                .extracting(e -> ((DomainException) e).getCode())
                .isEqualTo("JOIN_REQUEST_NOT_FOUND");
        verify(invitationService, never()).reissue(any(), any(), any());
    }

    @Test
    @DisplayName("Invitation déjà acceptée : le refus vient du service d’invitation, la demande est intacte")
    void alreadyAcceptedInvitationIsRefused() {
        TenantJoinRequest request = givenApprovedRequest(null, INVITATION_ID);
        when(invitationService.reissue(any(), any(), any()))
                .thenThrow(new DomainException("Cette invitation a déjà été acceptée",
                        org.springframework.http.HttpStatus.CONFLICT, "INVITATION_ALREADY_ACCEPTED"));

        assertThatThrownBy(() -> service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getCode())
                .isEqualTo("INVITATION_ALREADY_ACCEPTED");
        assertThat(request.getStatus()).isEqualTo(TenantJoinRequest.Status.APPROVED);
    }

    // ---------- héritage : demande approuvée avant V253 ----------

    @Test
    @DisplayName("Demande sans invitation liée (donnée antérieure à V253) : on retrouve la sienne par email")
    void legacyRequestWithoutInvitationLinkIsRecovered() {
        givenApprovedRequest(null, null);
        when(invitationService.findInvitableInvitationId(eq(TENANT_ID), eq(EMAIL), eq(InvitationStatus.PENDING)))
                .thenReturn(INVITATION_ID);
        givenReissueSucceeds();

        service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        verify(invitationService).reissue(eq(INVITATION_ID), eq(TENANT_ID), eq(ADMIN_ID));
    }

    @Test
    @DisplayName("Aucune invitation du tout : on en émet une, plutôt que de laisser la demande sans issue")
    void requestWithNoInvitationAtAllGetsOne() {
        TenantJoinRequest request = givenApprovedRequest(null, null);
        when(invitationService.findInvitableInvitationId(any(), any(), any())).thenReturn(null);
        when(invitationService.createInvitation(any(), any(), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new InvitationService.InvitationCreationResult(
                        InvitationService.InvitationCreationKind.INVITATION,
                        INVITATION_ID, null, EMAIL, "MEMBRE", "TENANT", null,
                        "jeton", "https://app/accept-invitation?token=jeton", true, false, false));
        when(invitationService.peekExpiry(eq(INVITATION_ID), eq(TENANT_ID))).thenReturn(Optional.of(expiry));

        TenantJoinService.ApprovalOutcome outcome =
                service.resendInvitationForRequest(REQUEST_ID, TENANT_ID, ADMIN_ID);

        assertThat(outcome.status()).isEqualTo("INVITATION_SENT");
        assertThat(request.getInvitationId()).isEqualTo(INVITATION_ID);
        verify(invitationService).createInvitation(eq(TENANT_ID), eq(ADMIN_ID), eq(EMAIL),
                eq("MEMBRE"), eq(MembershipScopeType.TENANT), isNull(), isNull());
    }

    // ---------- isResendable : une seule règle, partagée par tous les clients ----------

    @Test
    @DisplayName("isResendable : vrai seulement pour une demande approuvée, sans compte, avec email")
    void resendableRule() {
        assertThat(service.isResendable(TenantJoinRequest.builder()
                .status(TenantJoinRequest.Status.APPROVED).email(EMAIL).build())).isTrue();

        assertThat(service.isResendable(TenantJoinRequest.builder()
                .status(TenantJoinRequest.Status.PENDING).email(EMAIL).build())).isFalse();
        assertThat(service.isResendable(TenantJoinRequest.builder()
                .status(TenantJoinRequest.Status.REJECTED).email(EMAIL).build())).isFalse();
        assertThat(service.isResendable(TenantJoinRequest.builder()
                .status(TenantJoinRequest.Status.APPROVED).email("  ").build())).isFalse();
        assertThat(service.isResendable(TenantJoinRequest.builder()
                .status(TenantJoinRequest.Status.APPROVED).email(null).build())).isFalse();
        assertThat(service.isResendable(TenantJoinRequest.builder()
                .status(TenantJoinRequest.Status.APPROVED).email(EMAIL)
                .userId(UUID.randomUUID()).build())).isFalse();
    }

    // ---------- liste : l'approuvée doit redevenir visible ----------

    @Test
    @DisplayName("joinRequests(null) renvoie l’historique complet, PENDING reste le défaut")
    void listFiltersByStatus() {
        when(joinRequestRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                eq(TENANT_ID), eq(TenantJoinRequest.Status.PENDING))).thenReturn(List.of());
        when(joinRequestRepository.findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID))).thenReturn(List.of());

        assertThat(service.joinRequests(TENANT_ID, TenantJoinRequest.Status.PENDING)).isEmpty();
        assertThat(service.joinRequests(TENANT_ID, null)).isEmpty();
        // Le défaut historique reste exactement le même appel qu'avant.
        verify(joinRequestRepository).findByTenantIdAndStatusOrderByCreatedAtDesc(
                eq(TENANT_ID), eq(TenantJoinRequest.Status.PENDING));
        verify(joinRequestRepository).findByTenantIdOrderByCreatedAtDesc(eq(TENANT_ID));
    }
}
