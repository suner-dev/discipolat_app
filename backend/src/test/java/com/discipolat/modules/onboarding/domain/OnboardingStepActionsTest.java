package com.discipolat.modules.onboarding.domain;

import com.discipolat.common.exception.DomainException;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentService;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventService;
import com.discipolat.modules.families.api.CreateFamilyRequest;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyService;
import com.discipolat.modules.tenants.domain.InvitationService;
import com.discipolat.modules.tenants.domain.MembershipScopeType;
import com.discipolat.modules.tenants.domain.MembershipStatus;
import com.discipolat.modules.tenants.domain.ModuleCatalogService;
import com.discipolat.modules.tenants.domain.ModuleDefinition;
import com.discipolat.modules.tenants.domain.OrganizationNode;
import com.discipolat.modules.tenants.domain.OrganizationNodeService;
import com.discipolat.modules.tenants.domain.TenantFeature;
import com.discipolat.modules.tenants.domain.TenantFeatureService;
import com.discipolat.modules.tenants.domain.TenantMembership;
import com.discipolat.modules.tenants.domain.TenantMembershipRepository;
import com.discipolat.modules.tenants.service.TenantSettingsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Constat B2 / A3.4 — les 7 actions métier du wizard appellent un service RÉEL
 * et produisent un effet vérifiable.
 *
 * <p>Avant ce correctif, `completeStep` ne faisait que `setStatus(COMPLETED)` :
 * aucune de ces actions n'existait. Ce test verrouille le fait que chacune
 * delegate bien au service métier correspondant, écrit son audit, et refuse toute
 * donnée invalide <b>avant</b> toute écriture.
 */
@ExtendWith(MockitoExtension.class)
class OnboardingStepActionsTest {

    @Mock private OrganizationNodeService organizationNodeService;
    @Mock private TenantSettingsService tenantSettingsService;
    @Mock private DepartmentService departmentService;
    @Mock private FamilyService familyService;
    @Mock private TenantFeatureService tenantFeatureService;
    @Mock private ModuleCatalogService moduleCatalogService;
    @Mock private EventService eventService;
    @Mock private InvitationService invitationService;
    @Mock private TenantMembershipRepository membershipRepository;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.security.SecurityUtils securityUtils;

    private OnboardingStepActions actions;
    private UUID tenantId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        SecurityTestHelper.loginAs(actorId);
        actions = new OnboardingStepActions(organizationNodeService, tenantSettingsService,
                departmentService, familyService, tenantFeatureService, moduleCatalogService,
                eventService, invitationService, membershipRepository, auditService, securityUtils);
    }

    @AfterEach
    void tearDown() {
        SecurityTestHelper.logout();
    }

    // ---------- 0 — Identité de l'église ----------

    @Test
    @DisplayName("CHURCH_IDENTITY : l'église racine est créée puisauditée")
    void churchIdentityCreatesRootChurch() {
        when(organizationNodeService.getRoot(tenantId)).thenReturn(Optional.empty());
        OrganizationNode created = new OrganizationNode();
        created.setId(UUID.randomUUID());
        when(organizationNodeService.createRootChurch(eq(tenantId), eq("Église Bethel"), any(), eq(actorId)))
                .thenReturn(created);

        Map<String, Object> result = actions.execute(
                OnboardingWizardStep.StepType.CHURCH_IDENTITY, tenantId,
                Map.of("churchName", "Église Bethel"));

        assertThat(result).containsEntry("churchName", "Église Bethel");
        assertThat((List<?>) result.get("createdIds")).hasSize(1);
        verify(organizationNodeService).createRootChurch(tenantId, "Église Bethel", null, actorId);
        verify(auditService).logSimple("TENANT_ONBOARDING_CHURCH_IDENTITY", "TENANT", tenantId);
    }

    @Test
    @DisplayName("CHURCH_IDENTITY : si la racine existe, elle est RENOMMÉE (pas dupliquée)")
    void churchIdentityRenamesTheExistingRoot() {
        OrganizationNode root = new OrganizationNode();
        root.setId(UUID.randomUUID());
        root.setName("Ancien nom");
        when(organizationNodeService.getRoot(tenantId)).thenReturn(Optional.of(root));
        OrganizationNode renamed = new OrganizationNode();
        renamed.setId(root.getId());
        when(organizationNodeService.updateNode(eq(root.getId()), eq("Nouveau nom"), any(), any(), any(), eq(actorId)))
                .thenReturn(renamed);

        actions.execute(OnboardingWizardStep.StepType.CHURCH_IDENTITY, tenantId,
                Map.of("churchName", "Nouveau nom"));

        verify(organizationNodeService).updateNode(root.getId(), "Nouveau nom", null, null, null, actorId);
        verify(organizationNodeService, never()).createRootChurch(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("CHURCH_IDENTITY : un nom trop court est refusé SANS aucune écriture")
    void churchIdentityRejectsTooShortName() {
        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.CHURCH_IDENTITY, tenantId, Map.of("churchName", "A")))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
                    assertThat(problem.getTitle()).isEqualTo("STEP_DATA_INVALID");
                });

        verifyNoInteractions(organizationNodeService);
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("CHURCH_IDENTITY : les contacts sont écrits dans les paramètres du tenant")
    void churchIdentityPersistsContacts() {
        OrganizationNode root = new OrganizationNode();
        root.setId(UUID.randomUUID());
        root.setName("Église");
        when(organizationNodeService.getRoot(tenantId)).thenReturn(Optional.of(root));

        actions.execute(OnboardingWizardStep.StepType.CHURCH_IDENTITY, tenantId, Map.of(
                "churchName", "Église", "businessName", "Association Bethel",
                "city", "Douala", "phone", "+237600000000",
                "email", "contact@eglise.cm", "timezone", "Africa/Douala", "currency", "XAF"));

        ArgumentCaptor<TenantSettingsService.TenantSettingsRequest> captor =
                ArgumentCaptor.forClass(TenantSettingsService.TenantSettingsRequest.class);
        verify(tenantSettingsService).updateSettings(eq(tenantId), captor.capture(), eq(actorId));
        TenantSettingsService.TenantSettingsRequest request = captor.getValue();
        assertThat(request.businessName()).isEqualTo("Association Bethel");
        assertThat(request.city()).isEqualTo("Douala");
        assertThat(request.phone()).isEqualTo("+237600000000");
        assertThat(request.email()).isEqualTo("contact@eglise.cm");
        assertThat(request.timezone()).isEqualTo("Africa/Douala");
        assertThat(request.currency()).isEqualTo("XAF");
        // Seuls les champs fournis sont déclarés comme modifiés.
        assertThat(request.updatedFields()).containsExactlyInAnyOrder(
                "businessName", "city", "phone", "email", "timezone", "currency");
    }

    // ---------- 1 — Import des membres (D4) ----------

    @Test
    @DisplayName("MEMBER_IMPORT : declareOnly=true et audit (honneteté D4)")
    void memberImportIsDeclaredButVerified() {
        Map<String, Object> result = actions.execute(
                OnboardingWizardStep.StepType.MEMBER_IMPORT, tenantId, Map.of("importedCount", 120));

        assertThat(result).containsEntry("importedCount", 120).containsEntry("declaredOnly", true);
        verify(auditService).logSimple("TENANT_MEMBERS_IMPORTED", "TENANT", tenantId);
        // Aucune fausse automatisation : l'import réel reste le module /imports.
        verifyNoInteractions(userCreationTargets());
    }

    private Object[] userCreationTargets() {
        return new Object[]{departmentService, familyService, eventService, tenantFeatureService};
    }

    @Test
    @DisplayName("MEMBER_IMPORT : 0 membre déclaré est refusé")
    void memberImportRejectsZero() {
        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.MEMBER_IMPORT, tenantId, Map.of("importedCount", 0)))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_DATA_INVALID"));
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("MEMBER_IMPORT : un nombre non entier est refusé")
    void memberImportRejectsNonNumeric() {
        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.MEMBER_IMPORT, tenantId, Map.of("importedCount", "beaucoup")))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(auditService);
    }

    // ---------- 2 — Structure ----------

    @Test
    @DisplayName("STRUCTURE : départements et familles sont RÉELLEMENT créés")
    void structureCreatesDepartmentsAndFamilies() {
        Department department = new Department();
        department.setId(UUID.randomUUID());
        when(departmentService.create(org.mockito.ArgumentMatchers.<Department>any()))
                .thenReturn(department);
        Family family = new Family();
        family.setId(UUID.randomUUID());
        when(familyService.create(any(CreateFamilyRequest.class))).thenReturn(family);

        Map<String, Object> result = actions.execute(OnboardingWizardStep.StepType.STRUCTURE, tenantId,
                Map.of("departments", List.of("Intercession", "Louange"),
                        "families", List.of("Famille Pierre")));

        ArgumentCaptor<Department> departmentCaptor = ArgumentCaptor.forClass(Department.class);
        verify(departmentService, times(2)).create(departmentCaptor.capture());
        assertThat(departmentCaptor.getAllValues()).extracting(Department::getNom)
                .containsExactly("Intercession", "Louange");
        assertThat(departmentCaptor.getAllValues()).allSatisfy(d -> {
            assertThat(d.getTenantId()).isEqualTo(tenantId);
            assertThat(d.getResponsableId()).isEqualTo(actorId);
        });

        verify(familyService).create(any(CreateFamilyRequest.class));
        ArgumentCaptor<CreateFamilyRequest> familyCaptor = ArgumentCaptor.forClass(CreateFamilyRequest.class);
        verify(familyService).create(familyCaptor.capture());
        assertThat(familyCaptor.getValue().nom()).isEqualTo("Famille Pierre");
        assertThat((List<?>) result.get("createdIds")).hasSize(3);
        verify(auditService).logSimple("TENANT_STRUCTURE_CREATED", "TENANT", tenantId);
    }

    @Test
    @DisplayName("STRUCTURE : sans liste et sans structure existante => 409 STEP_PRECONDITION_FAILED")
    void structureRefusesWhenNothingExistsYet() {
        when(departmentService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(familyService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.STRUCTURE, tenantId, Map.of()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
                    assertThat(problem.getTitle()).isEqualTo("STEP_PRECONDITION_FAILED");
                });
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("STRUCTURE : sans liste mais avec un département ET une famille => accepté")
    void structureAcceptsWhenAlreadyStructured() {
        when(departmentService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Department())));
        when(familyService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Family())));

        Map<String, Object> result = actions.execute(
                OnboardingWizardStep.StepType.STRUCTURE, tenantId, Map.of());

        assertThat(result).containsEntry("preconditionAlreadyMet", true);
        verify(departmentService, never()).create(org.mockito.ArgumentMatchers.<Department>any());
        verify(familyService, never()).create(org.mockito.ArgumentMatchers.<CreateFamilyRequest>any());
    }

    // ---------- 3 — Rôles / invitations ----------

    @Test
    @DisplayName("ROLES : une invitation par entrée, via le service partagé")
    void rolesInvitesEveryEntry() {
        UUID invitationId = UUID.randomUUID();
        when(invitationService.createInvitation(any(), eq(actorId), anyString(), anyString(), any(), any(), any()))
                .thenReturn(new InvitationService.InvitationCreationResult(
                        InvitationService.InvitationCreationKind.INVITATION,
                        invitationId, null, "a@eglise.cm", "PASTEUR", "TENANT", null,
                        "token", "https://app/accept-invitation?token=token", true, false, false));

        Map<String, Object> result = actions.execute(OnboardingWizardStep.StepType.ROLES, tenantId,
                Map.of("invitations", List.of(
                        Map.of("email", "A@Eglise.cm", "role", "pasteur"),
                        Map.of("email", "b@eglise.cm", "role", "RESPONSABLE"))));

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> roleCaptor = ArgumentCaptor.forClass(String.class);
        verify(invitationService, times(2)).createInvitation(
                eq(tenantId), eq(actorId), emailCaptor.capture(), roleCaptor.capture(),
                eq(MembershipScopeType.TENANT), any(), any());
        // Emails normalisés en minuscules, rôles en majuscules.
        assertThat(emailCaptor.getAllValues()).containsExactly("a@eglise.cm", "b@eglise.cm");
        assertThat(roleCaptor.getAllValues()).containsExactly("PASTEUR", "RESPONSABLE");
        // 2 invitations => 2 identifiants dans `createdIds`.
        assertThat((List<UUID>) result.get("createdIds")).hasSize(2).allMatch(invitationId::equals);
        verify(auditService).logSimple("TENANT_ROLES_INVITED", "TENANT", tenantId);
    }

    @Test
    @DisplayName("ROLES : liste vide alors que le PROPRIETAIRE est le seul membre => 409")
    void rolesRefusesEmptyListWithoutTeam() {
        // 1 membre actif, et c'est le propriétaire : il reste 0 membre non-owner.
        TenantMembership ownerMembership = new TenantMembership();
        when(membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                tenantId, MembershipStatus.ACTIVE, "OWNER"))
                .thenReturn(List.of(ownerMembership));
        when(membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.ROLES, tenantId, Map.of("invitations", List.of())))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_PRECONDITION_FAILED"));
        verifyNoInteractions(invitationService);
    }

    @Test
    @DisplayName("ROLES : liste vide avec un membre non-owner => accepté")
    void rolesAcceptsEmptyListWhenTeamExists() {
        TenantMembership ownerMembership = new TenantMembership();
        when(membershipRepository.findByTenantIdAndStatusAndRoleContaining(
                tenantId, MembershipStatus.ACTIVE, "OWNER"))
                .thenReturn(List.of(ownerMembership));
        when(membershipRepository.countByTenantIdAndStatus(tenantId, MembershipStatus.ACTIVE))
                .thenReturn(3L);

        Map<String, Object> result = actions.execute(
                OnboardingWizardStep.StepType.ROLES, tenantId, Map.of("invitations", List.of()));

        assertThat(result).containsEntry("preconditionAlreadyMet", true);
        verifyNoInteractions(invitationService);
    }

    @Test
    @DisplayName("ROLES : un email invalide est refusé SANS créer d'invitation")
    void rolesRejectsInvalidEmail() {
        assertThatThrownBy(() -> actions.execute(OnboardingWizardStep.StepType.ROLES, tenantId,
                Map.of("invitations", List.of(Map.of("email", "pas-un-email", "role", "PASTEUR")))))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_DATA_INVALID"));
        verifyNoInteractions(invitationService);
    }

    // ---------- 4 — Branding ----------

    @Test
    @DisplayName("BRANDING : la couleur est validée puis appliquée")
    void brandingAppliesColor() {
        Map<String, Object> result = actions.execute(OnboardingWizardStep.StepType.BRANDING, tenantId,
                Map.of("primaryColor", "#1A2B3C", "allowDarkMode", true));

        ArgumentCaptor<TenantSettingsService.BrandingRequest> captor =
                ArgumentCaptor.forClass(TenantSettingsService.BrandingRequest.class);
        verify(tenantSettingsService).updateBranding(eq(tenantId), captor.capture(), eq(actorId));
        assertThat(captor.getValue().primaryColor()).isEqualTo("#1A2B3C");
        assertThat(captor.getValue().updatedFields()).contains("primaryColor");
        assertThat(result).containsEntry("primaryColor", "#1A2B3C").containsEntry("allowDarkMode", true);
        verify(auditService).logSimple("TENANT_ONBOARDING_BRANDING_UPDATED", "TENANT", tenantId);
    }

    @Test
    @DisplayName("BRANDING : une couleur non hexadécimale est refusée")
    void brandingRejectsNonHexColor() {
        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.BRANDING, tenantId, Map.of("primaryColor", "rouge")))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_DATA_INVALID"));
        verifyNoInteractions(tenantSettingsService);
    }

    @Test
    @DisplayName("BRANDING : aucun champ fourni est refusé")
    void brandingRejectsEmptyPayload() {
        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.BRANDING, tenantId, Map.of()))
                .isInstanceOf(DomainException.class);
        verifyNoInteractions(tenantSettingsService);
    }

    // ---------- 5 — Modules ----------

    @Test
    @DisplayName("MODULES : chaque code est validé contre le catalogue puis activé")
    void modulesValidatesAgainstCatalogAndEnables() {
        // resolveCanonicalCode est l'API utilisée par la production (cf.
        // commentaire dans OnboardingStepActions) : c'est elle qu'on stubbe.
        // « PEOPLE » (3e élément) sera dédupliqué via le code canonique déjà vu,
        // donc resolveCanonicalCode n'est appelé que 2 fois.
        when(moduleCatalogService.resolveCanonicalCode("people")).thenReturn(Optional.of("PEOPLE"));
        when(moduleCatalogService.resolveCanonicalCode("events")).thenReturn(Optional.of("EVENTS"));
        TenantFeature feature = new TenantFeature();
        feature.setId(UUID.randomUUID());
        when(tenantFeatureService.enableFeature(eq(tenantId), anyString(), any())).thenReturn(feature);

        Map<String, Object> result = actions.execute(OnboardingWizardStep.StepType.MODULES, tenantId,
                Map.of("modules", List.of("people", "events", "PEOPLE")));

        // Le doublon (casse différente) n'est activé qu'une fois, et c'est le
        // code CANONIQUE qui est persisté (exigence du catalogue).
        verify(tenantFeatureService, times(2)).enableFeature(eq(tenantId), anyString(), any());
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(tenantFeatureService, times(2)).enableFeature(eq(tenantId), captor.capture(), any());
        assertThat(captor.getAllValues()).containsExactly("PEOPLE", "EVENTS");
        assertThat((List<String>) result.get("modules")).containsExactly("PEOPLE", "EVENTS");
        verify(auditService).logSimple("TENANT_ONBOARDING_MODULES_ENABLED", "TENANT", tenantId);
    }

    @Test
    @DisplayName("MODULES : un code inconnu est refusé SANS activer quoi que ce soit")
    void modulesRejectsUnknownCode() {
        when(moduleCatalogService.resolveCanonicalCode("inexistant")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.MODULES, tenantId, Map.of("modules", List.of("inexistant"))))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(problem.getTitle()).isEqualTo("STEP_DATA_INVALID");
                    // Les détails des champs fautifs sont dans la propriété
                    // `details` (ProblemDetail RFC 7807, cf. DomainException).
                    assertThat(((Map<?, ?>) problem.getProperties().get("details")).get("modules"))
                            .isNotNull();
                });
        verifyNoInteractions(tenantFeatureService);
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("MODULES : liste vide ou absente est refusée")
    void modulesRejectsEmptyList() {
        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.MODULES, tenantId, Map.of("modules", List.of())))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> actions.execute(
                OnboardingWizardStep.StepType.MODULES, tenantId, Map.of()))
                .isInstanceOf(DomainException.class);
    }

    // ---------- 6 — Premier événement ----------

    @Test
    @DisplayName("FIRST_EVENT : l'événement est réellement créé au statut PLANIFIE")
    void firstEventCreatesEvent() {
        Event saved = new Event();
        saved.setId(UUID.randomUUID());
        when(eventService.create(any(Event.class), eq(List.of()))).thenReturn(saved);
        String startAt = Instant.now().plusSeconds(7 * 24 * 3600).toString();

        Map<String, Object> result = actions.execute(OnboardingWizardStep.StepType.FIRST_EVENT, tenantId,
                Map.of("title", "Grande réunion de lancement", "startAt", startAt, "location", "Grande salle"));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventService).create(captor.capture(), eq(List.of()));
        Event event = captor.getValue();
        assertThat(event.getTenantId()).isEqualTo(tenantId);
        assertThat(event.getTitre()).isEqualTo("Grande réunion de lancement");
        assertThat(event.getLieu()).isEqualTo("Grande salle");
        assertThat(event.getStatut()).isEqualTo("PLANIFIE");
        assertThat(event.getDateDebut()).isNotNull();
        assertThat(result).containsEntry("statut", "PLANIFIE")
                .containsEntry("title", "Grande réunion de lancement");
        verify(auditService).logSimple("TENANT_ONBOARDING_FIRST_EVENT_CREATED", "TENANT", tenantId);
    }

    @Test
    @DisplayName("FIRST_EVENT : une date passée est refusée")
    void firstEventRejectsPastDate() {
        assertThatThrownBy(() -> actions.execute(OnboardingWizardStep.StepType.FIRST_EVENT, tenantId,
                Map.of("title", "Rétrospective", "startAt", "2020-01-01T10:00:00Z")))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("STEP_DATA_INVALID"));
        verifyNoInteractions(eventService);
    }

    @Test
    @DisplayName("FIRST_EVENT : un titre absent est refusé")
    void firstEventRequiresTitle() {
        assertThatThrownBy(() -> actions.execute(OnboardingWizardStep.StepType.FIRST_EVENT, tenantId,
                Map.of("startAt", Instant.now().plusSeconds(3600).toString())))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    var problem = ((DomainException) thrown).toProblemDetail();
                    assertThat(((Map<?, ?>) problem.getProperties().get("details")).get("title"))
                            .isNotNull();
                });
        verifyNoInteractions(eventService);
    }
}
