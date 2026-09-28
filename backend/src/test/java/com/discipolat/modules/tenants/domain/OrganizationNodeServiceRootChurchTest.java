package com.discipolat.modules.tenants.domain;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat H3 (second volet) — la création d'une ÉGLISE RACINE levait une NPE.
 *
 * <p>Dans {@code createNode}, la charge utile d'audit était construite avec
 * {@code Map.of("type", ..., "name", ..., "parentId", effectiveParentId)} et
 * {@code Map.of} <b>interdit les valeurs nulles</b>. Or {@code parentId} vaut
 * systématiquement {@code null} pour une église racine — le cas le plus courant,
 * celui du provisionnement atomique et de l'étape CHURCH_IDENTITY du wizard.
 * Résultat : {@code NullPointerException} et HTTP 500 à la création de toute
 * première église d'un tenant.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganizationNodeServiceRootChurchTest {

    @Mock
    private OrganizationNodeRepository nodeRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private EntityPropagationPublisher propagationPublisher;
    @Mock
    private QuotaService quotaService;
    @Mock
    private InvitationRepository invitationRepository;

    @InjectMocks
    private OrganizationNodeService service;

    @Test
    @DisplayName("H3 — créer une église racine (parentId nul) ne lève plus de NPE")
    void creatingARootChurchWithNullParentDoesNotThrow() {
        UUID tenantId = UUID.randomUUID();
        when(nodeRepository.findRootByTenantId(tenantId)).thenReturn(Optional.empty());
        when(nodeRepository.findByTenantIdAndCode(eq(tenantId), isNull())).thenReturn(Optional.empty());
        when(nodeRepository.save(any(OrganizationNode.class))).thenAnswer(call -> {
            OrganizationNode saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        assertThatCode(() -> service.createRootChurch(tenantId, "Eglise de Douala", null, null))
                .doesNotThrowAnyException();

        verify(nodeRepository).save(any(OrganizationNode.class));
    }

    @Test
    @DisplayName("H3 — la charge utile d'audit porte bien parentId=null, sans échouer")
    void auditPayloadAcceptsANullParentId() {
        UUID tenantId = UUID.randomUUID();
        when(nodeRepository.findRootByTenantId(tenantId)).thenReturn(Optional.empty());
        when(nodeRepository.findByTenantIdAndCode(eq(tenantId), isNull())).thenReturn(Optional.empty());
        when(nodeRepository.save(any(OrganizationNode.class))).thenAnswer(call -> {
            OrganizationNode saved = call.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        service.createRootChurch(tenantId, "Eglise de Douala", null, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        // Signature : actorId, tenantId, action, type, cible, statut, detail,
        // anciennes valeurs, nouvelles valeurs, requete.
        verify(auditService).log(isNull(), eq(tenantId), eq("ORG_NODE_CREATED"),
                eq("ORGANIZATION_NODE"), any(), eq("SUCCESS"), payload.capture(),
                isNull(), isNull(), isNull());

        // Le noeud racine n'a pas de parent : la charge utile doit le dire
        // explicitement plutôt que de ne pas exister.
        assertThat(payload.getValue())
                .containsEntry("name", "Eglise de Douala")
                .containsEntry("type", OrganizationNodeType.ROOT_CHURCH.name())
                .containsEntry("parentId", null);
    }
}
