package com.discipolat.modules.tenants.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LOT 2 §LB — tests du paramétrage fin de l'interface.
 *
 * <p>Invariants verrouillés :
 * <ol>
 *   <li>résolution <b>déterministe</b> : nœud &gt; église &gt; global, quel que
 *       soit l'ordre de restitution de la base (une previous version dépendait
 *       de cet ordre — le nom affiché changeait au hasard) ;</li>
 *   <li>langue exacte avant « toutes langues » ;</li>
 *   <li>une surcharge désactivée est ignorée, pas supprimée ;</li>
 *   <li>une clé absente ne produit <b>aucune</b> entrée : le frontend retombe
 *       alors sur son dictionnaire, jamais sur un blanc ;</li>
 *   <li>une fonctionnalité absente est <b>visible</b> (défaut ouvert) ;</li>
 *   <li>isolation multi-tenant : un réglage d'une autre église est un 404.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LOT 2 §LB — paramétrage fin de l'interface")
class UiCustomizationServiceTest {

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();
    private static final UUID NODE_A = UUID.randomUUID();

    @Mock UiLabelOverrideRepository labelRepository;
    @Mock UiPageFeatureRepository featureRepository;
    @InjectMocks UiCustomizationService service;

    private static UiLabelOverride label(UUID tenantId, UUID nodeId, String key, String locale,
                                         String value, boolean enabled) {
        return UiLabelOverride.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .nodeId(nodeId)
                .labelKey(key)
                .locale(locale)
                .value(value)
                .enabled(enabled)
                .build();
    }

    private static UiPageFeature feature(UUID tenantId, UUID nodeId, String pageKey,
                                         String featureKey, boolean enabled) {
        return UiPageFeature.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .nodeId(nodeId)
                .pageKey(pageKey)
                .featureKey(featureKey)
                .enabled(enabled)
                .build();
    }

    @Nested
    @DisplayName("hiérarchie des surcharges")
    class Hierarchy {

        @Test
        @DisplayName("l'église écrase le global, quel que soit l'ordre de la base")
        void tenantBeatsGlobalRegardlessOfOrder() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, null, "souls.title", "*", "Mes âmes", true),
                    label(null, null, "souls.title", "*", "Âmes", true)));

            assertThat(service.resolvedLabels(TENANT_A, "fr", null))
                    .containsEntry("souls.title", "Mes âmes");
        }

        @Test
        @DisplayName("résolution stable même si le global arrive en premier")
        void resolutionIsOrderIndependent() {
            // Ordre inverse : le global d'abord, l'église ensuite.
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(null, null, "souls.title", "*", "Âmes", true),
                    label(TENANT_A, null, "souls.title", "*", "Mes âmes", true)));

            assertThat(service.resolvedLabels(TENANT_A, "fr", null))
                    .containsEntry("souls.title", "Mes âmes");
        }

        @Test
        @DisplayName("le nœud écrase l'église")
        void nodeBeatsTenant() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, NODE_A, "souls.title", "*", "Âmes du campus", true),
                    label(TENANT_A, null, "souls.title", "*", "Mes âmes", true)));

            assertThat(service.resolvedLabels(TENANT_A, "fr", NODE_A))
                    .containsEntry("souls.title", "Âmes du campus");
        }

        @Test
        @DisplayName("le réglage d'un AUTRE nœud ne s'applique pas")
        void otherNodeSettingIgnored() {
            UUID otherNode = UUID.randomUUID();
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, otherNode, "souls.title", "*", "Ames du nord", true)));

            assertThat(service.resolvedLabels(TENANT_A, "fr", NODE_A)).isEmpty();
        }

        @Test
        @DisplayName("une église qui ne surcharge pas garde le libellé global")
        void globalSurvivesWhenNotOverridden() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_B)).thenReturn(List.of(
                    label(null, null, "souls.title", "*", "Âmes", true)));

            assertThat(service.resolvedLabels(TENANT_B, "fr", null))
                    .containsEntry("souls.title", "Âmes");
        }
    }

    @Nested
    @DisplayName("langues")
    class Locales {

        @Test
        @DisplayName("la langue exacte l'emporte sur « toutes langues »")
        void exactLocaleBeatsWildcard() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, null, "nav.back", "en", "Go back", true),
                    label(TENANT_A, null, "nav.back", "*", "Retour", true)));

            assertThat(service.resolvedLabels(TENANT_A, "en", null)).containsEntry("nav.back", "Go back");
            assertThat(service.resolvedLabels(TENANT_A, "fr", null)).containsEntry("nav.back", "Retour");
        }

        @Test
        @DisplayName("une surcharge « fr » couvre « fr-FR »")
        void baseLocaleCoversRegionalVariant() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, null, "nav.back", "fr", "Revenir", true)));

            assertThat(service.resolvedLabels(TENANT_A, "fr-FR", null)).containsEntry("nav.back", "Revenir");
        }

        @Test
        @DisplayName("une surcharge d'une autre langue est ignorée")
        void otherLocaleIgnored() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, null, "nav.back", "sw", "Rudi", true)));

            assertThat(service.resolvedLabels(TENANT_A, "fr", null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("désactivation & valeurs par défaut")
    class Defaults {

        @Test
        @DisplayName("une surcharge désactivée est ignorée sans être perdue")
        void disabledOverrideIgnored() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, null, "souls.title", "*", "Mes âmes", false)));

            assertThat(service.resolvedLabels(TENANT_A, "fr", null)).isEmpty();
        }

        @Test
        @DisplayName("une clé absente ne produit aucune entrée (repli sur i18n)")
        void absentKeyProducesNoEntry() {
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of());

            assertThat(service.resolvedLabels(TENANT_A, "fr", null)).isEmpty();
        }

        @Test
        @DisplayName("une fonctionnalité absente est visible : le défaut est ouvert")
        void absentFeatureStaysVisible() {
            when(featureRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of());

            // Aucune entrée ne.disable() rien : le frontend garde son défaut.
            assertThat(service.resolvedFeatures(TENANT_A, null)).isEmpty();
        }

        @Test
        @DisplayName("une fonctionnalité explicitement désactivée est bien retirée")
        void disabledFeatureIsRemoved() {
            when(featureRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    feature(TENANT_A, null, "/souls", "export", false)));

            Map<String, UiPageFeature> resolved = service.resolvedFeatures(TENANT_A, null);

            assertThat(resolved).containsKey("/souls:export");
            assertThat(resolved.get("/souls:export").isEnabled()).isFalse();
        }

        @Test
        @DisplayName("l'église écrase le réglage global d'une fonctionnalité")
        void tenantFeatureBeatsGlobal() {
            when(featureRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    feature(null, null, "/souls", "export", true),
                    feature(TENANT_A, null, "/souls", "export", false)));

            assertThat(service.resolvedFeatures(TENANT_A, null).get("/souls:export").isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("charge utile frontend")
    class Payload {

        @Test
        @DisplayName("la charge utile expose libellés et fonctionnalités à plat")
        void payloadShape() {
            UiPageFeature export = feature(TENANT_A, null, "/souls", "export", false);
            export.setLabelOverride("Exporter la liste");
            export.setDisplayOrder(2);
            when(labelRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(
                    label(TENANT_A, null, "souls.title", "*", "Mes âmes", true)));
            when(featureRepository.findByTenantIdIsNullOrTenantId(TENANT_A)).thenReturn(List.of(export));

            Map<String, Object> payload = service.customizationFor(TENANT_A, "fr", null);

            assertThat(payload.get("labels")).isEqualTo(Map.of("souls.title", "Mes âmes"));
            Map<?, ?> features = (Map<?, ?>) payload.get("features");
            Map<?, ?> soulsExport = (Map<?, ?>) features.get("/souls:export");
            assertThat(soulsExport.get("enabled")).isEqualTo(false);
            assertThat(soulsExport.get("label")).isEqualTo("Exporter la liste");
            assertThat(soulsExport.get("displayOrder")).isEqualTo(2);
        }

        @Test
        @DisplayName("la clé composite est stable")
        void compositeKey() {
            assertThat(UiCustomizationService.featureKey("/souls", "export")).isEqualTo("/souls:export");
        }
    }

    @Nested
    @DisplayName("écriture & isolation")
    class Writes {

        @Test
        @DisplayName("clé et valeur sont obligatoires")
        void keyAndValueRequired() {
            assertThatThrownBy(() -> service.upsertLabel(TENANT_A, null, " ", "fr", "x", null, null))
                    .isInstanceOf(BusinessRuleException.class);
            assertThatThrownBy(() -> service.upsertLabel(TENANT_A, null, "k", "fr", "  ", null, null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("écran et fonctionnalité sont obligatoires")
        void featureKeysRequired() {
            assertThatThrownBy(() -> service.upsertFeature(TENANT_A, null, "", "export",
                    null, null, null, null, null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("upsert crée puis met à jour sans dupliquer")
        void upsertIsIdempotent() {
            UiLabelOverride existing = label(TENANT_A, null, "souls.title", "fr", "Mes âmes", true);
            when(labelRepository.findByTenantIdAndLabelKey(TENANT_A, "souls.title"))
                    .thenReturn(List.of(existing));
            when(labelRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            var saved = service.upsertLabel(TENANT_A, null, "souls.title", "fr", "Ãmes de l'église",
                    null, null);

            assertThat(saved.getValue()).isEqualTo("Ãmes de l'église");
            assertThat(saved.getId()).isEqualTo(existing.getId());
            verify(labelRepository).save(existing);
        }

        @Test
        @DisplayName("un réglage d'une autre église renvoie 404")
        void foreignLabelIsNotFound() {
            UiLabelOverride foreign = label(TENANT_B, null, "k", "fr", "v", true);
            when(labelRepository.findById(any())).thenReturn(java.util.Optional.of(foreign));

            assertThatThrownBy(() -> service.deleteLabel(TENANT_A, foreign.getId()))
                    .isInstanceOf(EntityNotFoundException.class);
            verify(labelRepository, never()).delete(any());
        }

        @Test
        @DisplayName("un réglage global est en lecture seule pour une église")
        void globalSettingReadOnly() {
            UiPageFeature global = feature(null, null, "/souls", "export", true);
            when(featureRepository.findById(any())).thenReturn(java.util.Optional.of(global));

            assertThatThrownBy(() -> service.deleteFeature(TENANT_A, global.getId()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("plateforme");
        }

        @Test
        @DisplayName("adminView ne renvoie que les réglages de l'église")
        void adminViewScopedToTenant() {
            when(labelRepository.findByTenantId(eq(TENANT_A))).thenReturn(List.of(
                    label(TENANT_A, null, "k", "fr", "v", true)));
            when(featureRepository.findByTenantId(eq(TENANT_A))).thenReturn(List.of());

            Map<String, Object> view = service.adminView(TENANT_A);

            assertThat((List<?>) view.get("labels")).hasSize(1);
            assertThat((List<?>) view.get("features")).isEmpty();
        }
    }
}