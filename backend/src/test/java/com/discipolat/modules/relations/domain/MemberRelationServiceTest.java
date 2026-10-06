package com.discipolat.modules.relations.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.platform.domain.DictionaryEntry;
import com.discipolat.modules.platform.domain.DictionaryEntryRepository;
import com.discipolat.modules.relations.api.dto.MemberRelationView;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.users.domain.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

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
import static org.mockito.Mockito.when;

/**
 * V231 — « Mon encadrement » : invariants du service de relations
 * personnelles (déclaration autonome du membre, notification automatique
 * chez le supérieur, liste « ses membres » paginée), sans base — mocks de
 * repos, même discipline que OrganizationV3ServiceTest (T-Q1).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberRelationServiceTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final UUID OTHER_TENANT = UUID.randomUUID();
    private static final UUID MEMBRE = UUID.randomUUID();
    private static final UUID PASTEUR = UUID.randomUUID();

    @Mock private MemberRelationRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private DictionaryEntryRepository dictionaryRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private EntityPropagationPublisher propagationPublisher;

    private MemberRelationService svc() {
        return new MemberRelationService(repository, userRepository, dictionaryRepository,
                notificationService, auditService, propagationPublisher);
    }

    private static User user(UUID id, UUID tenantId, String prenom, String email) {
        return User.builder()
                .id(id).tenantId(tenantId).firstName(prenom).lastName("Diallo")
                .email(email).statut(UserStatus.ACTIVE).build();
    }

    /** Contexte d'authentification : un membre ordinaire (pas modérateur). */
    private static void asMember() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                MEMBRE, null, List.of(new SimpleGrantedAuthority("ROLE_MEMBRE"))));
    }

    private static void asModerator(UUID id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_PASTEUR"))));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void givenUsers() {
        when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(
                user(MEMBRE, TENANT, "Awa", "awa@eglise.org")));
        when(userRepository.findById(PASTEUR)).thenReturn(Optional.of(
                user(PASTEUR, TENANT, "Jean", "jean@eglise.org")));
        when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                .thenReturn(List.of());
        when(repository.findByTenantIdAndFromUserIdAndToUserIdAndRelationTypeAndStatut(
                any(), any(), any(), anyString(), any())).thenReturn(Optional.empty());
        when(repository.countByTenantIdAndFromUserIdAndStatut(eq(TENANT), eq(MEMBRE),
                eq(MemberRelation.RelationStatus.ACTIVE))).thenReturn(0L);
        when(repository.countByTenantIdAndToUserIdAndStatut(eq(TENANT), any(),
                eq(MemberRelation.RelationStatus.ACTIVE))).thenReturn(0L);
        when(repository.save(any(MemberRelation.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ==================== DÉCLARATION ====================

    @Test
    @DisplayName("déclarer son pasteur : ACTIVE + notification in-app automatique chez le supérieur")
    void declareActivatesAndNotifiesSuperior() {
        givenUsers();
        asMember();
        MemberRelation saved = svc().declare(TENANT, MEMBRE, PASTEUR, null, "PASTEUR", "note", MEMBRE);

        assertThat(saved.getStatut()).isEqualTo(MemberRelation.RelationStatus.ACTIVE);
        assertThat(saved.getFromUserId()).isEqualTo(MEMBRE);
        assertThat(saved.getToUserId()).isEqualTo(PASTEUR);
        assertThat(saved.getRelationType()).isEqualTo("PASTEUR");
        assertThat(saved.getDeclaredBy()).isEqualTo(MEMBRE);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(notificationService).create(eq(TENANT), eq(PASTEUR), eq(TypeNotification.RELATION_DECLAREE),
                eq(CanalNotification.IN_APP), anyString(), message.capture(), any(), eq("MEMBER_RELATION"));
        assertThat(message.getValue()).contains("Awa").contains("Mon pasteur");
    }

    @Test
    @DisplayName("le destinataire doit être ENREGISTRÉ (email inconnu du tenant → refus)")
    void declareRejectsUnregisteredTargetByEmail() {
        givenUsers();
        asMember();
        when(userRepository.findByTenantIdAndEmailIgnoreCase(TENANT, "inconnu@x.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, null, "inconnu@x.com", "PASTEUR", null, MEMBRE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("enregistré");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("isolation stricte : un compte d'une autre église est invisible (404)")
    void declareIsTenantScoped() {
        givenUsers();
        asMember();
        when(userRepository.findById(PASTEUR)).thenReturn(Optional.of(
                user(PASTEUR, OTHER_TENANT, "Pasteur", "p@autre.org")));

        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "PASTEUR", null, MEMBRE))
                .isInstanceOf(EntityNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("le DÉCLARANT d'une autre église est invisible lui aussi")
    void declarantIsTenantScoped() {
        asMember();
        when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                .thenReturn(List.of());
        when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(
                user(MEMBRE, OTHER_TENANT, "Awa", "awa@autre.org")));

        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "PASTEUR", null, MEMBRE))
                .isInstanceOf(EntityNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("pas d'auto-rattachement et pas de doublon ACTIVE (from,to,type)")
    void selfAndDuplicateRejected() {
        givenUsers();
        asMember();
        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, MEMBRE, null, "MENTOR", null, MEMBRE))
                .isInstanceOf(BusinessRuleException.class);

        when(repository.findByTenantIdAndFromUserIdAndToUserIdAndRelationTypeAndStatut(
                TENANT, MEMBRE, PASTEUR, "PASTEUR", MemberRelation.RelationStatus.ACTIVE))
                .thenReturn(Optional.of(MemberRelation.builder().build()));
        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "pasteur", null, MEMBRE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("déjà actif");
    }

    // ==================== PARAMÉTRAGE PAR ÉGLISE ====================

    @Nested
    @DisplayName("Paramétrage par église (dictionnaire MEMBER_RELATION_TYPE)")
    class Parametrage {

        @Test
        @DisplayName("type hors paramétrage église refusé ; le dictionnaire tenant complète les types")
        void typesComeFromDictionary() {
            givenUsers();
            asMember();
            assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "DJOSSMAN", null, MEMBRE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("inconnu");

            when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                    .thenReturn(List.of(DictionaryEntry.builder().tenantId(TENANT)
                            .dictKey(MemberRelationService.DICT_KEY).code("ANCIEN")
                            .label("Mon ancien").actif(true).ordre(6).build()));
            Map<String, String> types = svc().availableTypes(TENANT);
            assertThat(types).containsKeys("PASTEUR", "ANCIEN");
            assertThat(types.get("ANCIEN")).isEqualTo("Mon ancien");
        }

        @Test
        @DisplayName("BOGUE CORRIGÉ : un type DÉSACTIVÉ par l'église est refusé, pas seulement masqué")
        void desactiveEstRejeteEtNonMasque() {
            givenUsers();
            asMember();
            // L'église a désactivé MENTOR…
            when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                    .thenReturn(List.of(
                            DictionaryEntry.builder().tenantId(TENANT)
                                    .dictKey(MemberRelationService.DICT_KEY).code("MENTOR")
                                    .label("Mon mentor").actif(false).ordre(4).build()));

            // …il ne doit plus être proposé…
            assertThat(svc().availableTypes(TENANT)).doesNotContainKey("MENTOR");
            assertThat(svc().availableTypes(TENANT)).containsKey("PASTEUR");

            // …et sa déclaration doit être REJETÉE (et non acceptée en silence,
            //    ce que le défaut « DEFAULT_TYPES en point de départ » faisait).
            assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "MENTOR", null, MEMBRE))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("désactivé");
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("la ligne du TENANT prime sur la ligne globale et sur le défaut technique")
        void precedenceDuTenant() {
            givenUsers();
            when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                    .thenReturn(List.of(
                            DictionaryEntry.builder().tenantId(null)
                                    .dictKey(MemberRelationService.DICT_KEY).code("PASTEUR")
                                    .label("Pasteur (plateforme)").actif(true).ordre(1).build(),
                            DictionaryEntry.builder().tenantId(TENANT)
                                    .dictKey(MemberRelationService.DICT_KEY).code("PASTEUR")
                                    .label("Mon pasteur").actif(true).ordre(1).build(),
                            // Ligne d'un AUTRE tenant : totalement ignorée.
                            DictionaryEntry.builder().tenantId(OTHER_TENANT)
                                    .dictKey(MemberRelationService.DICT_KEY).code("MENTOR")
                                    .label("Fuyons").actif(true).ordre(2).build()));

            // PASTEUR prend le libellé de la ligne du TENANT, pas celui de la
            // ligne globale « plateforme ».
            assertThat(svc().availableTypes(TENANT)).containsEntry("PASTEUR", "Mon pasteur");
            // La ligne de l'AUTRE tenant ne fuit jamais : MENTOR retombe sur
            // le libellé technique de la plateforme, jamais sur « Fuyons ».
            assertThat(svc().availableTypes(TENANT)).containsEntry("MENTOR", "Mon mentor");
            assertThat(svc().availableTypes(TENANT)).doesNotContainValue("Fuyons");
        }

        @Test
        @DisplayName("l'ordre d'affichage suit l'ordre du dictionnaire de l'église")
        void ordreDuDic() {
            givenUsers();
            when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                    .thenReturn(List.of(
                            DictionaryEntry.builder().tenantId(TENANT)
                                    .dictKey(MemberRelationService.DICT_KEY).code("Z_LATE")
                                    .label("Z").actif(true).ordre(1).build(),
                            DictionaryEntry.builder().tenantId(TENANT)
                                    .dictKey(MemberRelationService.DICT_KEY).code("A_SECOND")
                                    .label("A").actif(true).ordre(2).build()));
            // L'ordre de l'église d'abord ; les types techniques (ordre 999)
            // ensuite, triés par code.
            assertThat(svc().availableTypes(TENANT).keySet())
                    .containsExactly("Z_LATE", "A_SECOND", "MENTOR", "PARRAIN",
                            "PASTEUR", "RESPONSABLE", "SUPERIEUR");
        }
    }

    // ==================== RÉVOCATION ====================

    @Test
    @DisplayName("révoquer : REVOKED (jamais de purge) + notif chez l'encadrant ; tiers non autorisé refusé")
    void revokeIsSoftAndNotifies() {
        MemberRelation relation = MemberRelation.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).fromUserId(MEMBRE).toUserId(PASTEUR)
                .relationType("PASTEUR").statut(MemberRelation.RelationStatus.ACTIVE).build();
        when(repository.findByTenantIdAndId(TENANT, relation.getId())).thenReturn(Optional.of(relation));
        when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                .thenReturn(List.of());
        when(userRepository.findById(MEMBRE)).thenReturn(Optional.of(
                user(MEMBRE, TENANT, "Awa", "awa@eglise.org")));
        when(userRepository.findById(PASTEUR)).thenReturn(Optional.of(
                user(PASTEUR, TENANT, "Jean", "jean@eglise.org")));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        asMember();
        assertThatThrownBy(() -> svc().revoke(TENANT, relation.getId(), UUID.randomUUID()))
                .isInstanceOf(BusinessRuleException.class);

        svc().revoke(TENANT, relation.getId(), MEMBRE);
        assertThat(relation.getStatut()).isEqualTo(MemberRelation.RelationStatus.REVOKED);
        assertThat(relation.getEndedAt()).isNotNull();
        verify(repository, never()).delete(any());
        verify(notificationService).create(eq(TENANT), eq(PASTEUR), eq(TypeNotification.RELATION_REVOQUEE),
                eq(CanalNotification.IN_APP), anyString(), anyString(), any(), eq("MEMBER_RELATION"));
    }

    @Test
    @DisplayName("l'ENCADRANT peut détacher un membre mal rattaché (cas d'usage principal)")
    void encadrantPeutDetacher() {
        MemberRelation relation = MemberRelation.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).fromUserId(MEMBRE).toUserId(PASTEUR)
                .relationType("PASTEUR").statut(MemberRelation.RelationStatus.ACTIVE).build();
        when(repository.findByTenantIdAndId(TENANT, relation.getId())).thenReturn(Optional.of(relation));
        when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                .thenReturn(List.of());
        when(userRepository.findById(any())).thenReturn(Optional.of(
                user(PASTEUR, TENANT, "Jean", "jean@eglise.org")));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        asMember();

        svc().revoke(TENANT, relation.getId(), PASTEUR);
        assertThat(relation.getStatut()).isEqualTo(MemberRelation.RelationStatus.REVOKED);
    }

    @Test
    @DisplayName("un modérateur (PASTEUR/ADMIN) peut retirer le rattachement d'un membre")
    void moderateurPeutRetirer() {
        MemberRelation relation = MemberRelation.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).fromUserId(MEMBRE).toUserId(PASTEUR)
                .relationType("PASTEUR").statut(MemberRelation.RelationStatus.ACTIVE).build();
        when(repository.findByTenantIdAndId(TENANT, relation.getId())).thenReturn(Optional.of(relation));
        when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                .thenReturn(List.of());
        when(userRepository.findById(any())).thenReturn(Optional.of(
                user(PASTEUR, TENANT, "Jean", "jean@eglise.org")));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        asModerator(UUID.randomUUID());

        svc().revoke(TENANT, relation.getId(), UUID.randomUUID());
        assertThat(relation.getStatut()).isEqualTo(MemberRelation.RelationStatus.REVOKED);
    }

    @Test
    @DisplayName("révoquer deux fois est idempotent (pas de double notification)")
    void revokeIdempotent() {
        MemberRelation relation = MemberRelation.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).fromUserId(MEMBRE).toUserId(PASTEUR)
                .relationType("PASTEUR").statut(MemberRelation.RelationStatus.REVOKED).build();
        when(repository.findByTenantIdAndId(TENANT, relation.getId())).thenReturn(Optional.of(relation));
        asMember();

        svc().revoke(TENANT, relation.getId(), MEMBRE);
        verify(notificationService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("une relation d'un autre tenant est invisible à la révocation")
    void revokeIsTenantScoped() {
        when(repository.findByTenantIdAndId(TENANT, UUID.randomUUID())).thenReturn(Optional.empty());
        asMember();
        assertThatThrownBy(() -> svc().revoke(TENANT, UUID.randomUUID(), MEMBRE))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ==================== LECTURES ====================

    @Test
    @DisplayName("« ses membres » : paginé, noms résolus, libellé de type")
    void listMembersOfIsPagedAndResolvesNames() {
        MemberRelation relation = MemberRelation.builder()
                .id(UUID.randomUUID()).tenantId(TENANT).fromUserId(MEMBRE).toUserId(PASTEUR)
                .relationType("PASTEUR").statut(MemberRelation.RelationStatus.ACTIVE).build();
        when(repository.findByTenantIdAndToUserIdAndStatut(eq(TENANT), eq(PASTEUR),
                eq(MemberRelation.RelationStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(relation), org.springframework.data.domain.PageRequest.of(0, 50), 1));
        when(userRepository.findAllById(any())).thenReturn(List.of(
                user(MEMBRE, TENANT, "Awa", "awa@eglise.org"),
                user(PASTEUR, TENANT, "Jean", "jean@eglise.org")));
        when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                .thenReturn(List.of());
        asMember();

        Page<MemberRelationView> page = svc().listMembersOf(TENANT, PASTEUR, 0, 50);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        MemberRelationView v = page.getContent().get(0);
        assertThat(v.otherUserId()).isEqualTo(MEMBRE);
        assertThat(v.otherNom()).isEqualTo("Awa Diallo");
        assertThat(v.typeLabel()).isEqualTo("Mon pasteur");
        assertThat(v.fromNom()).isEqualTo("Awa Diallo");
        assertThat(v.toNom()).isEqualTo("Jean Diallo");
        // Le déclarant (Awa) peut la retirer ; elle est donc marquée revocable.
        assertThat(v.revocable()).isTrue();
    }

    @Test
    @DisplayName("« ses membres » borne la taille de page (garde-fou DOS)")
    void pageSizeClampee() {
        when(repository.findByTenantIdAndToUserIdAndStatut(eq(TENANT), eq(PASTEUR), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        asMember();

        svc().listMembersOf(TENANT, PASTEUR, 0, 100_000);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByTenantIdAndToUserIdAndStatut(eq(TENANT), eq(PASTEUR), any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(MemberRelationService.MAX_PAGE_SIZE);
    }

    @Test
    @DisplayName("PAS DE N+1 : les noms de toute la page sont résolus en un seul findAllById")
    void pasDeNPlusUnSurLesNoms() {
        List<MemberRelation> relations = new java.util.ArrayList<>();
        for (int i = 0; i < 12; i++) {
            relations.add(MemberRelation.builder()
                    .id(UUID.randomUUID()).tenantId(TENANT).fromUserId(UUID.randomUUID())
                    .toUserId(PASTEUR).relationType("MENTOR")
                    .statut(MemberRelation.RelationStatus.ACTIVE).build());
        }
        when(repository.findByTenantIdAndFromUserIdAndStatut(TENANT, MEMBRE,
                MemberRelation.RelationStatus.ACTIVE)).thenReturn(relations);
        when(userRepository.findAllById(any())).thenReturn(List.of(
                user(MEMBRE, TENANT, "Awa", "awa@eglise.org"),
                user(PASTEUR, TENANT, "Jean", "jean@eglise.org")));
        when(dictionaryRepository.findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY))
                .thenReturn(List.of());
        asMember();

        Map<String, Object> summary = svc().summary(TENANT, MEMBRE);
        assertThat((List<?>) summary.get("sortantes")).hasSize(12);
        verify(userRepository, times(1)).findAllById(any());
        // Le catalogue des types n'est chargé qu'UNE fois par vue.
        verify(dictionaryRepository, times(1))
                .findByDictKeyOrderByOrdreAsc(MemberRelationService.DICT_KEY);
    }

    // ==================== PLAFONDS ====================

    @Test
    @DisplayName("garde-fou : plafond des déclarations SORTANTES, refus sans save")
    void outgoingQuotaEnforced() {
        givenUsers();
        asMember();
        when(repository.countByTenantIdAndFromUserIdAndStatut(eq(TENANT), eq(MEMBRE),
                eq(MemberRelation.RelationStatus.ACTIVE)))
                .thenReturn((long) MemberRelationService.MAX_OUTGOING_PER_MEMBER);

        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "MENTOR", null, MEMBRE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("maximum");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("garde-fou : plafond des déclarations ENTRANTES (l'encadrant sature)")
    void incomingQuotaEnforced() {
        givenUsers();
        asMember();
        when(repository.countByTenantIdAndToUserIdAndStatut(eq(TENANT), eq(PASTEUR),
                eq(MemberRelation.RelationStatus.ACTIVE)))
                .thenReturn((long) MemberRelationService.MAX_INCOMING_PER_MEMBER);

        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "MENTOR", null, MEMBRE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limite");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("déclaration pour le compte d'un membre : réservée aux modérateurs")
    void declareForReserveAuxModerateurs() {
        givenUsers();
        asMember();
        assertThatThrownBy(() -> svc().declare(TENANT, MEMBRE, PASTEUR, null, "PASTEUR", null, PASTEUR))
                .isInstanceOf(BusinessRuleException.class);
        verify(repository, never()).save(any());
    }
}
