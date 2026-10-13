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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Faille refermée : l'inscription « au nom d'une église » par lien
 * ({@code POST /auth/register} avec {@code tenantSlug} / {@code tenantId})
 * créait le compte via {@code AuthService.registerInChurch}, qui ne consulte
 * jamais le {@link JoinMode} de l'église.
 *
 * <p>Mesuré avant correction : une église en {@code APPROVAL} — celle dont
 * l'interface affiche « Cette église valide chaque demande d'adhésion avant
 * l'entrée » — se voyait franchir par une simple URL, sans code ni invitation.
 * Le compte obtenu était un MEMBRE rattaché, il lisait donc
 * {@code GET /api/v1/souls} : noms et suivi pastoral de l'église, soit des
 * données de conviction (RGPD art. 9). Le slug étant dérivé mécaniquement du
 * nom de l'église, il est devinable — la porte n'était pas une porte.
 *
 * <p>Ces tests verrouillent les DEUX faces du correctif : le mode
 * {@code APPROVAL} est désormais respecté, et le cas {@code OPEN} — le
 * comportement par défaut, sans lequel le produit ne fonctionnerait pas — reste
 * strictement inchangé.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantJoinServiceRegisterWithSlugTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String SLUG = "eglise-bethel-de-cocody";
    private static final String CHURCH_NAME = "Église Bethel de Cocody";

    @Mock private JoinCodeService joinCodeService;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private TenantJoinRequestRepository joinRequestRepository;
    @Mock private OrganizationNodeRepository organizationNodeRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private UserRepository userRepository;
    @Mock private ActiveTenantService activeTenantService;
    @Mock private com.discipolat.modules.tenants.domain.InvitationService invitationService;
    @Mock private AuditService auditService;
    @Mock private CrossTenantScopeAccess crossTenant;
    @Mock private EmailService emailService;
    @Mock private com.discipolat.modules.people.service.PeopleService peopleService;

    /** Pont capturé : c'est par lui que passe la création de compte. */
    private final List<String> createdAccounts = new java.util.ArrayList<>();
    private TenantJoinService service;

    @BeforeEach
    void setUp() {
        TenantJoinService.AuthServiceBridge bridge = (email, rawPassword, firstName, lastName,
                phone, tenantSlug, tenantId) -> {
            createdAccounts.add(email);
            return User.builder().id(USER_ID).email(email)
                    .firstName(firstName).lastName(lastName).build();
        };
        service = new TenantJoinService(joinCodeService, tenantRepository, membershipRepository,
                joinRequestRepository, organizationNodeRepository, roleRepository, userRepository,
                activeTenantService, bridge, invitationService, auditService, crossTenant, emailService,
                peopleService);

        // CrossTenantScopeAccess : on neutralise le filtre, on exécute le bloc.
        when(crossTenant.call(any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
        when(joinRequestRepository.findByTenantIdAndEmailAndCodeAndStatus(
                any(), anyString(), nullable(String.class), any()))
                .thenReturn(Optional.empty());
    }

    private Tenant givenActiveTenant() {
        Tenant tenant = Tenant.builder().id(TENANT_ID).slug(SLUG).name(CHURCH_NAME)
                .status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findBySlug(anyString())).thenReturn(Optional.of(tenant));
        // `requireActiveTenant` (chemin connecté) relit le tenant par id.
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        return tenant;
    }

    private void givenApprovalCode() {
        TenantJoinCode code = new TenantJoinCode();
        code.setTenantId(TENANT_ID);
        code.setCode("BETHEL-7K2M");
        code.setLabel("Code principal");
        code.setJoinMode(JoinMode.APPROVAL);
        when(joinCodeService.findPrimaryActiveCode(TENANT_ID)).thenReturn(Optional.of(code));
    }

    private void givenOpenCode() {
        TenantJoinCode code = new TenantJoinCode();
        code.setTenantId(TENANT_ID);
        code.setCode("BETHEL-OPEN");
        code.setJoinMode(JoinMode.OPEN);
        when(joinCodeService.findPrimaryActiveCode(TENANT_ID)).thenReturn(Optional.of(code));
    }

    private void givenNoCode() {
        when(joinCodeService.findPrimaryActiveCode(TENANT_ID)).thenReturn(Optional.empty());
    }

    // ---------- le défaut refermé ----------

    @Test
    @DisplayName("Église en APPROVAL : AUCUN compte créé, seulement une demande PENDING")
    void approvalChurchCreatesNoAccount() {
        givenActiveTenant();
        givenApprovalCode();

        TenantJoinService.RegisterOutcome outcome = service.registerWithSlug(
                "marie@example.com", "motdepasse123", "Marie", "Kouassi", null, SLUG, null);

        assertThat(outcome.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(outcome.userId()).isNull();
        assertThat(outcome.activationRequired()).isEqualTo("false");
        assertThat(outcome.churchName()).isEqualTo(CHURCH_NAME);

        // Le cœur de la faille : aucun compte MEMBRE ne doit être né.
        assertThat(createdAccounts).isEmpty();
        verify(joinRequestRepository).save(any(TenantJoinRequest.class));
    }

    @Test
    @DisplayName("La demande APPROVAL par lien est tracée pour l'église, email normalisé")
    void approvalRequestIsTraced() {
        givenActiveTenant();
        givenApprovalCode();

        service.registerWithSlug("Marie@Example.COM", "motdepasse123", "Marie", "Kouassi", null, SLUG, null);

        ArgumentCaptor<TenantJoinRequest> captor = ArgumentCaptor.forClass(TenantJoinRequest.class);
        verify(joinRequestRepository).save(captor.capture());
        TenantJoinRequest saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getEmail()).isEqualTo("marie@example.com");
        assertThat(saved.getCode()).isEqualTo("BETHEL-7K2M");
        assertThat(saved.getStatus()).isEqualTo(TenantJoinRequest.Status.PENDING);
        assertThat(saved.getUserId()).isNull();
    }

    @Test
    @DisplayName("Deux demandes identiques n'en créent qu'une (pas de doublon PENDING)")
    void duplicateApprovalRequestIsNotDuplicated() {
        givenActiveTenant();
        givenApprovalCode();
        when(joinRequestRepository.findByTenantIdAndEmailAndCodeAndStatus(any(), anyString(), nullable(String.class), any()))
                .thenReturn(Optional.of(TenantJoinRequest.builder().id(UUID.randomUUID()).build()));

        TenantJoinService.RegisterOutcome outcome = service.registerWithSlug(
                "marie@example.com", "motdepasse123", "Marie", "Kouassi", null, SLUG, null);

        assertThat(outcome.status()).isEqualTo("PENDING_APPROVAL");
        verify(joinRequestRepository, never()).save(any(TenantJoinRequest.class));
    }

    // ---------- les cas qui doivent RESTER identiques ----------

    @Test
    @DisplayName("Église en OPEN : compte créé + email d'activation (comportement historique)")
    void openChurchCreatesAccount() {
        givenActiveTenant();
        givenOpenCode();

        TenantJoinService.RegisterOutcome outcome = service.registerWithSlug(
                "marie@example.com", "motdepasse123", "Marie", "Kouassi", null, SLUG, null);

        assertThat(outcome.status()).isEqualTo("ACTIVATION_EMAIL_SENT");
        assertThat(outcome.activationRequired()).isEqualTo("true");
        assertThat(outcome.churchName()).isEqualTo(CHURCH_NAME);
        assertThat(createdAccounts).containsExactly("marie@example.com");
        verify(joinRequestRepository, never()).save(any(TenantJoinRequest.class));
    }

    @Test
    @DisplayName("Église SANS code d'entrée : adhésion ouverte — un code synthétique ne fabrique pas d'approbation")
    void churchWithoutCodeStaysOpen() {
        givenActiveTenant();
        givenNoCode();

        TenantJoinService.RegisterOutcome outcome = service.registerWithSlug(
                "marie@example.com", "motdepasse123", "Marie", "Kouassi", null, SLUG, null);

        // Une église sans code d'entrée n'a jamais choisi de faire valider ses
        // adhésion : exiger son accord maintenant casserait toutes les liaisons
        // /j/<slug> déjà distribuées, y compris celles des fondateurs.
        assertThat(outcome.status()).isEqualTo("ACTIVATION_EMAIL_SENT");
        assertThat(createdAccounts).hasSize(1);
        verify(joinRequestRepository, never()).save(any(TenantJoinRequest.class));
    }

    @Test
    @DisplayName("Variante mobile (tenantId) : même décision que le slug")
    void tenantIdVariantFollowsSamePolicy() {
        Tenant tenant = Tenant.builder().id(TENANT_ID).slug(SLUG).name(CHURCH_NAME)
                .status(TenantStatus.ACTIVE).build();
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
        givenApprovalCode();

        TenantJoinService.RegisterOutcome outcome = service.registerWithSlug(
                "marie@example.com", "motdepasse123", "Marie", "Kouassi", null, null, TENANT_ID);

        assertThat(outcome.status()).isEqualTo("PENDING_APPROVAL");
        assertThat(createdAccounts).isEmpty();
    }

    // ---------- anti-énumération ----------

    @Test
    @DisplayName("Slug inconnu ET église suspendue : même message, même code — pas d'oracle d'existence")
    void unknownAndSuspendedAreIndistinguishable() {
        when(tenantRepository.findBySlug(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registerWithSlug(
                "a@b.c", "motdepasse123", "A", "B", null, "inconnu", null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Aucune église trouvée pour ce lien")
                .extracting(e -> ((DomainException) e).getCode())
                .isEqualTo("JOIN_SLUG_NOT_FOUND");

        Tenant suspended = Tenant.builder().id(TENANT_ID).slug("suspendue").name("Église X")
                .status(TenantStatus.SUSPENDED).build();
        when(tenantRepository.findBySlug(anyString())).thenReturn(Optional.of(suspended));
        assertThatThrownBy(() -> service.registerWithSlug(
                "a@b.c", "motdepasse123", "A", "B", null, "suspendue", null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Aucune église trouvée pour ce lien")
                .extracting(e -> ((DomainException) e).getCode())
                .isEqualTo("JOIN_SLUG_NOT_FOUND");

        assertThat(createdAccounts).isEmpty();
    }

    @Test
    @DisplayName("Aucune église demandée : refus, jamais d'entrée à l'aveugle")
    void noTenantRequestedIsRejected() {
        assertThatThrownBy(() -> service.registerWithSlug(
                "a@b.c", "motdepasse123", "A", "B", null, null, null))
                .isInstanceOf(DomainException.class)
                .hasMessage("Aucune église trouvée pour ce lien");
        assertThat(createdAccounts).isEmpty();
    }

    // ---------- email aux responsables : ne jamais annoncer « code null » ----------

    @Test
    @DisplayName("Rejoindre par lien une église sans code : l'email admin nomme le canal, pas « null »")
    void adminNotificationNeverMentionsNullCode() {
        givenActiveTenant();
        givenNoCode();

        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(
                User.builder().id(USER_ID).email("marie@example.com")
                        .firstName("Marie").lastName("Kouassi").build()));
        Role memberRole = Role.builder().id(UUID.randomUUID()).tenantId(TENANT_ID).key("MEMBRE").build();
        when(roleRepository.findByTenantIdAndKey(eq(TENANT_ID), eq("MEMBRE"))).thenReturn(Optional.of(memberRole));
        when(membershipRepository.findAllByUserIdAndTenantIdAndStatus(any(), any(), any()))
                .thenReturn(List.of());
        when(activeTenantService.switchTenant(any(), any()))
                .thenReturn(new ActiveTenantService.SwitchOutcome(TENANT_ID, "a1", "r1", "MEMBRE"));

        TenantMembership admin = TenantMembership.builder()
                .tenantId(TENANT_ID).userId(UUID.randomUUID()).roleLegacy("TENANT_OWNER")
                .status(MembershipStatus.ACTIVE).build();
        when(membershipRepository.findByTenantIdAndStatus(eq(TENANT_ID), eq(MembershipStatus.ACTIVE)))
                .thenReturn(List.of(admin));
        when(userRepository.findById(admin.getUserId()))
                .thenReturn(Optional.of(User.builder().email("pasteur@bethel.example").build()));

        TenantJoinService.JoinOutcome outcome = service.joinBySlug(USER_ID, SLUG);
        assertThat(outcome.status()).isEqualTo("JOINED");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).send(anyString(), anyString(), body.capture());
        assertThat(body.getValue())
                .doesNotContain("null")
                .contains("lien d'invitation");
    }
}
