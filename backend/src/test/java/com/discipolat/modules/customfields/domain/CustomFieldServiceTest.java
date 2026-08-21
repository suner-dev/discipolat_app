package com.discipolat.modules.customfields.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomFieldServiceTest {

    @Mock private CustomFieldDefinitionRepository definitionRepository;
    @Mock private CustomFieldValueRepository valueRepository;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;

    private CustomFieldService service;

    private final UUID fieldId = UUID.randomUUID();
    private final UUID entiteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CustomFieldService(definitionRepository, valueRepository, auditService, propagationPublisher, securityUtils);
    }

    private CustomFieldDefinition def(List<String> rolesEcriture) {
        return CustomFieldDefinition.builder()
                .id(fieldId).entiteType("SOUL").code("TEST")
                .label("Test").type("TEXTE").rolesEcriture(rolesEcriture).build();
    }

    @Test
    void getDefinitions_filtersByRoleLecture() {
        when(securityUtils.getCurrentUserRole()).thenReturn("FAISEUR");
        CustomFieldDefinition visible = def(null);
        visible.setRolesLecture(List.of("PASTEUR", "FAISEUR"));
        CustomFieldDefinition restricted = def(null);
        restricted.setRolesLecture(List.of("ADMIN"));
        when(definitionRepository.findByEntiteTypeAndActifTrueOrderByOrdreAsc("SOUL"))
                .thenReturn(List.of(visible, restricted));

        var result = service.getDefinitions("SOUL");

        assertThat(result).containsExactly(visible);
    }

    @Test
    void getDefinitions_emptyRolesLectureMeansVisibleToAll() {
        when(securityUtils.getCurrentUserRole()).thenReturn("MEMBRE");
        CustomFieldDefinition open = def(null); // roles_lecture vide
        when(definitionRepository.findByEntiteTypeAndActifTrueOrderByOrdreAsc("SOUL"))
                .thenReturn(List.of(open));

        assertThat(service.getDefinitions("SOUL")).containsExactly(open);
    }

    @Test
    void saveValues_ignoresFieldNotWritableByCurrentRole() {
        when(securityUtils.getCurrentUserRole()).thenReturn("FAISEUR");
        when(definitionRepository.findAllById(any())).thenReturn(List.of(def(List.of("PASTEUR"))));

        service.saveValues("SOUL", entiteId, Map.of(fieldId.toString(), "valeur interdite"));

        verify(valueRepository, never()).save(any(CustomFieldValue.class));
        verify(valueRepository, never()).findByEntiteTypeAndEntiteIdAndFieldId(any(), any(), any());
        verify(auditService).logSimple("CUSTOM_FIELD_VALUES_SAVED", "SOUL", entiteId);
    }

    @Test
    void saveValues_emptyRolesEcritureAllowsAnyRole() {
        when(securityUtils.getCurrentUserRole()).thenReturn("FAISEUR");
        when(definitionRepository.findAllById(any())).thenReturn(List.of(def(List.of())));
        when(valueRepository.findByEntiteTypeAndEntiteIdAndFieldId("SOUL", entiteId, fieldId))
                .thenReturn(Optional.empty());

        service.saveValues("SOUL", entiteId, Map.of(fieldId.toString(), "ok"));

        verify(valueRepository).save(argThat(v -> "ok".equals(v.getValue())
                && entiteId.equals(v.getEntiteId())
                && fieldId.equals(v.getFieldId())));
    }

    @Test
    void saveValues_updatesExistingValueWhenRoleAllowed() {
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        when(definitionRepository.findAllById(any())).thenReturn(List.of(def(List.of("PASTEUR"))));
        CustomFieldValue existing = CustomFieldValue.builder()
                .id(UUID.randomUUID()).entiteType("SOUL").entiteId(entiteId)
                .fieldId(fieldId).value("ancien").build();
        when(valueRepository.findByEntiteTypeAndEntiteIdAndFieldId("SOUL", entiteId, fieldId))
                .thenReturn(Optional.of(existing));

        service.saveValues("SOUL", entiteId, Map.of(fieldId.toString(), "nouveau"));

        assertThat(existing.getValue()).isEqualTo("nouveau");
        verify(valueRepository).save(existing);
    }

    @Test
    void saveValues_skipsUnknownFieldDefinitionWithoutAbortingBatch() {
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        UUID unknownId = UUID.randomUUID();
        when(definitionRepository.findAllById(any())).thenReturn(List.of());

        service.saveValues("SOUL", entiteId, Map.of(unknownId.toString(), "x"));

        verify(valueRepository, never()).save(any(CustomFieldValue.class));
        verify(auditService).logSimple("CUSTOM_FIELD_VALUES_SAVED", "SOUL", entiteId);
    }

    @Test
    void saveValues_skipsInvalidUuidKeys() {
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        when(definitionRepository.findAllById(any())).thenReturn(List.of());

        service.saveValues("SOUL", entiteId, Map.of("pas-un-uuid", "x"));

        verify(valueRepository, never()).save(any(CustomFieldValue.class));
    }

    @Test
    void saveValues_skipsDefinitionOfAnotherEntityType() {
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        CustomFieldDefinition userDef = def(List.of());
        userDef.setEntiteType("USER");
        when(definitionRepository.findAllById(any())).thenReturn(List.of(userDef));

        service.saveValues("SOUL", entiteId, Map.of(fieldId.toString(), "x"));

        verify(valueRepository, never()).save(any(CustomFieldValue.class));
    }

    @Test
    void saveValues_skipsInactiveField() {
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");
        CustomFieldDefinition inactive = def(List.of());
        inactive.setActif(false);
        when(definitionRepository.findAllById(any())).thenReturn(List.of(inactive));

        service.saveValues("SOUL", entiteId, Map.of(fieldId.toString(), "x"));

        verify(valueRepository, never()).save(any(CustomFieldValue.class));
    }

    @Test
    void saveValues_skipsFieldNotReadableByCurrentRole() {
        when(securityUtils.getCurrentUserRole()).thenReturn("MEMBRE");
        CustomFieldDefinition restricted = def(List.of());
        restricted.setRolesLecture(List.of("PASTEUR", "FAISEUR")); // écriture vide mais lecture restreinte
        when(definitionRepository.findAllById(any())).thenReturn(List.of(restricted));

        service.saveValues("SOUL", entiteId, Map.of(fieldId.toString(), "x"));

        verify(valueRepository, never()).save(any(CustomFieldValue.class));
    }

    @Test
    void saveValues_emptyValuesStillTracesAudit() {
        when(securityUtils.getCurrentUserRole()).thenReturn("PASTEUR");

        service.saveValues("SOUL", entiteId, Map.of());

        verify(definitionRepository, never()).findAllById(any());
        verify(auditService).logSimple("CUSTOM_FIELD_VALUES_SAVED", "SOUL", entiteId);
    }
}
