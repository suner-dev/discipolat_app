package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LOT 2 §GR — tests du service de groupes de navigation.
 *
 * <p>Invariants verrouillés :
 * <ol>
 *   <li>l'église écrase le groupe global de même {@code key} (override) ;</li>
 *   <li>un groupe absent des rôles de l'utilisateur n'est jamais renvoyé ;</li>
 *   <li>un groupe d'une autre église est un <b>404</b>, pas un 403 (pas de
 *       confirmation d'existence) ;</li>
 *   <li>un groupe global est en lecture seule pour une église ;</li>
 *   <li>supprimer un groupe père le <b>désactive</b> au lieu de casser la
 *       hiérarchie ;</li>
 *   <li>{@code setItems} est idempotent (réécrit l'affectation, ne cumule pas).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LOT 2 §GR — groupes d'onglets de navigation")
class NavigationGroupServiceTest {

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    @Mock NavigationGroupRepository groupRepository;
    @Mock NavigationGroupItemRepository itemRepository;
    @InjectMocks NavigationGroupService service;

    private static NavigationGroup group(UUID tenantId, String key, String label, Integer order,
                                         boolean enabled, List<String> roles, UUID parentId) {
        return NavigationGroup.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .key(key)
                .label(label)
                .displayOrder(order)
                .enabled(enabled)
                .roles(roles == null ? List.of() : List.copyOf(roles))
                .parentGroupId(parentId)
                .collapsedByDefault(true)
                .build();
    }

    @Nested
    @DisplayName("résolution groupe global + groupe d'église")
    class Resolution {

        @Test
        @DisplayName("l'église écrase le groupe global de même clé")
        void tenantOverridesGlobalOfSameKey() {
            NavigationGroup global = group(null, "pilotage", "Pilotage", 0, true, List.of(), null);
            NavigationGroup override = group(TENANT_A, "pilotage", "Mon pilotage", 0, true, List.of(), null);
            when(groupRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(global, override));
            when(groupRepository.findByTenantId(TENANT_A)).thenReturn(List.of(override));

            List<NavigationGroup> resolved = service.resolveForTenant(TENANT_A, List.of("ADMIN"));

            assertThat(resolved).containsExactly(override);
            assertThat(resolved.get(0).getLabel()).isEqualTo("Mon pilotage");
        }

        @Test
        @DisplayName("une église qui ne surcharge pas voit le groupe global")
        void globalVisibleWhenNotOverridden() {
            NavigationGroup global = group(null, "discipolat", "Discipolat", 1, true, List.of(), null);
            when(groupRepository.findByTenantIdIsNullOrTenantId(TENANT_B)).thenReturn(List.of(global));
            when(groupRepository.findByTenantId(TENANT_B)).thenReturn(List.of());

            assertThat(service.resolveForTenant(TENANT_B, List.of("MEMBRE")))
                    .extracting(NavigationGroup::getLabel).containsExactly("Discipolat");
        }

        @Test
        @DisplayName("un groupe restreint à d'autres rôles n'est jamais renvoyé")
        void roleRestrictedGroupHidden() {
            NavigationGroup pasteurOnly = group(null, "gouvernance", "Gouvernance", 2, true,
                    List.of("PASTEUR"), null);
            when(groupRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(pasteurOnly));

            assertThat(service.resolveForTenant(TENANT_A, List.of("MEMBRE"))).isEmpty();
            assertThat(service.resolveForTenant(TENANT_A, List.of("PASTEUR"))).hasSize(1);
        }

        @Test
        @DisplayName("un groupe désactivé disparaît, même pour un rôle autorisé")
        void disabledGroupHidden() {
            when(groupRepository.findByTenantIdIsNullOrTenantId(TENANT_A))
                    .thenReturn(List.of(group(null, "off", "Éteint", 0, false, List.of(), null)));

            assertThat(service.resolveForTenant(TENANT_A, List.of("ADMIN"))).isEmpty();
        }

        @Test
        @DisplayName("les groupes sont ordonnés par ordre d'affichage puis par libellé")
        void orderedByDisplayOrderThenLabel() {
            when(groupRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    group(null, "c", "Charlie", 2, true, List.of(), null),
                    group(null, "a", "Alpha", 1, true, List.of(), null),
                    group(null, "b", "Bravo", 1, true, List.of(), null)));

            assertThat(service.resolveForTenant(TENANT_A, List.of("ADMIN")))
                    .extracting(NavigationGroup::getLabel).containsExactly("Alpha", "Bravo", "Charlie");
        }
    }

    @Nested
    @DisplayName("isolation multi-tenant")
    class Isolation {

        @Test
        @DisplayName("modifier un groupe d'une autre église renvoie 404 (pas 403)")
        void foreignGroupIsNotFoundOnUpdate() {
            when(groupRepository.findById(any())).thenReturn(Optional.of(group(TENANT_B, "x", "X", 0, true, List.of(), null)));

            assertThatThrownBy(() -> service.update(TENANT_A, UUID.randomUUID(), "pirate", null, null,
                    null, null, null, null, null, null, null))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(groupRepository, never()).save(any());
        }

        @Test
        @DisplayName("supprimer un groupe d'une autre église renvoie 404")
        void foreignGroupIsNotFoundOnDelete() {
            when(groupRepository.findById(any())).thenReturn(Optional.of(group(TENANT_B, "x", "X", 0, true, List.of(), null)));

            assertThatThrownBy(() -> service.delete(TENANT_A, UUID.randomUUID()))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(groupRepository, never()).delete(any());
        }

        @Test
        @DisplayName("un groupe global est en lecture seule pour une église")
        void globalGroupReadOnlyForTenant() {
            when(groupRepository.findById(any())).thenReturn(Optional.of(group(null, "g", "G", 0, true, List.of(), null)));

            assertThatThrownBy(() -> service.delete(TENANT_A, UUID.randomUUID()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("plateforme");
        }

        @Test
        @DisplayName("un parent appartenant à une autre église est refusé")
        void foreignParentRefused() {
            UUID parentId = UUID.randomUUID();
            when(groupRepository.findById(parentId))
                    .thenReturn(Optional.of(group(TENANT_B, "p", "Parent", 0, true, List.of(), null)));

            assertThatThrownBy(() -> service.create(TENANT_A, "k", "K", null, null, parentId,
                    0, List.of(), null, true, true, false))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("autre église");
        }
    }

    @Nested
    @DisplayName("écriture & hiérarchie")
    class Writes {

        @Test
        @DisplayName("supprimer un groupe qui a des enfants le désactive au lieu de le retirer")
        void parentWithChildrenIsDisabledNotDeleted() {
            NavigationGroup parent = group(TENANT_A, "p", "Parent", 0, true, List.of(), null);
            when(groupRepository.findById(parent.getId())).thenReturn(Optional.of(parent));
            when(groupRepository.findByParentGroupId(parent.getId()))
                    .thenReturn(List.of(group(TENANT_A, "c", "Enfant", 0, true, List.of(), parent.getId())));
            when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.delete(TENANT_A, parent.getId());

            assertThat(parent.isEnabled()).isFalse();
            verify(groupRepository, never()).delete(any());
        }

        @Test
        @DisplayName("supprimer une feuille purge ses affectations puis le groupe")
        void leafIsDeletedWithItsAssignments() {
            NavigationGroup leaf = group(TENANT_A, "l", "Feuille", 0, true, List.of(), null);
            when(groupRepository.findById(leaf.getId())).thenReturn(Optional.of(leaf));
            when(groupRepository.findByParentGroupId(leaf.getId())).thenReturn(List.of());

            service.delete(TENANT_A, leaf.getId());

            verify(itemRepository).deleteByGroupId(leaf.getId());
            verify(groupRepository).delete(leaf);
        }

        @Test
        @DisplayName("un groupe ne peut pas être son propre parent")
        void selfParentRefused() {
            NavigationGroup self = group(TENANT_A, "s", "S", 0, true, List.of(), null);
            when(groupRepository.findById(self.getId())).thenReturn(Optional.of(self));

            assertThatThrownBy(() -> service.update(TENANT_A, self.getId(), null, null, null,
                    self.getId(), null, null, null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("propre parent");
        }

        @Test
        @DisplayName("clé et libellé sont obligatoires")
        void keyAndLabelRequired() {
            assertThatThrownBy(() -> service.create(TENANT_A, " ", "Label", null, null, null,
                    0, List.of(), null, true, true, false))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("requis");
        }

        @Test
        @DisplayName("clé en doublon dans la même église refusée")
        void duplicateKeyRefused() {
            when(groupRepository.findByTenantId(TENANT_A))
                    .thenReturn(List.of(group(TENANT_A, "pilotage", "Pilotage", 0, true, List.of(), null)));

            assertThatThrownBy(() -> service.create(TENANT_A, "PILOTAGE", "Autre", null, null, null,
                    0, List.of(), null, true, true, false))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("clé");
        }
    }

    @Nested
    @DisplayName("affectations d'entrées")
    class Assignments {

        @Test
        @DisplayName("setItems réécrit l'affectation au lieu de cumuler")
        void setItemsIsIdempotent() {
            NavigationGroup g = group(TENANT_A, "g", "G", 0, true, List.of(), null);
            when(groupRepository.findById(g.getId())).thenReturn(Optional.of(g));
            when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            List<NavigationGroupService.ItemRef> refs = List.of(
                    new NavigationGroupService.ItemRef("dashboard", "/dashboard", 0),
                    new NavigationGroupService.ItemRef(null, "/sermons", 1),
                    new NavigationGroupService.ItemRef(null, "   ", 2));
            List<NavigationGroupItem> saved = service.setItems(TENANT_A, g.getId(), refs);

            verify(itemRepository, times(1)).deleteByGroupId(g.getId());
            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(NavigationGroupItem::getHref)
                    .containsExactly("/dashboard", "/sermons");

            ArgumentCaptor<NavigationGroupItem> captor = ArgumentCaptor.forClass(NavigationGroupItem.class);
            verify(itemRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).allSatisfy(item ->
                    assertThat(item.getTenantId()).isEqualTo(TENANT_A));
        }

        @Test
        @DisplayName("assignmentsByHref indexe par href, une entrée pouvant être dans plusieurs groupes")
        void assignmentsIndexedByHref() {
            UUID g1 = UUID.randomUUID();
            UUID g2 = UUID.randomUUID();
            NavigationGroup first = NavigationGroup.builder().id(g1).build();
            NavigationGroup second = NavigationGroup.builder().id(g2).build();

            when(groupRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(first, second));
            when(itemRepository.findByGroupIdInOrderByDisplayOrderAsc(List.of(g1, g2))).thenReturn(List.of(
                    NavigationGroupItem.builder().groupId(g1).href("/dashboard").build(),
                    NavigationGroupItem.builder().groupId(g2).href("/dashboard").build(),
                    NavigationGroupItem.builder().groupId(g2).href("/sermons").build()));

            Map<String, List<UUID>> byHref = service.assignmentsByHref(
                    service.resolveForTenant(TENANT_A, List.of("ADMIN")));

            assertThat(byHref).containsOnlyKeys("/dashboard", "/sermons");
            assertThat(byHref.get("/dashboard")).containsExactlyInAnyOrder(g1, g2);
        }

        @Test
        @DisplayName("navigationFor ne renvoie aucun groupe quand il n'y en a aucun")
        void navigationForEmpty() {
            when(groupRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of());

            Map<String, Object> payload = service.navigationFor(TENANT_A, List.of("ADMIN"));

            assertThat(payload.get("groups")).isEqualTo(List.of());
            assertThat(payload.get("assignments")).isEqualTo(Map.of());
        }
    }

    @Test
    @DisplayName("normalize compare sans casse ni accents")
    void normalizeStripsAccentsAndCase() {
        assertThat(NavigationGroupService.normalize("Vie de l'Église ")).isEqualTo("vie de l'eglise");
        assertThat(NavigationGroupService.normalize(null)).isEmpty();
    }
}