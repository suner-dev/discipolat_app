package com.discipolat.modules.programs.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.programs.api.ProgramTypeRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.discipolat.common.infrastructure.security.SecurityTestHelper;

@ExtendWith(MockitoExtension.class)
class ProgramServiceTest {

    @Mock
    private ProgramTypeRepository programTypeRepository;
    @Mock
    private ProgramSubTypeRepository programSubTypeRepository;
    @Mock
    private SecurityUtils securityUtils;

    private ProgramService programService;
    private UUID typeId;
    private ProgramType programType;

    @BeforeEach
    void setUp() {
        SecurityTestHelper.loginAs(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        programService = new ProgramService(programTypeRepository, programSubTypeRepository, securityUtils);
        typeId = UUID.randomUUID();
        programType = ProgramType.builder()
                .id(typeId)
                .code("DIMANCHE")
                .label("Dimanche")
                .aSousProgrammes(true)
                .actif(true)
                .ordre(1)
                .build();
    }

    @Test
    void create_ShouldPersistTypeAndSubTypes() {
        when(programTypeRepository.findByCode("DIMANCHE")).thenReturn(Optional.empty());
        when(programTypeRepository.save(any(ProgramType.class))).thenReturn(programType);
        when(programTypeRepository.findById(typeId)).thenReturn(Optional.of(programType));
        when(programSubTypeRepository.findByProgramTypeIdOrderByOrdreAsc(typeId)).thenReturn(List.of());

        ProgramTypeRequest request = new ProgramTypeRequest(
                "DIMANCHE", "Dimanche", "Culte du dimanche", true, "#8b5cf6",
                true, 1, List.of(
                        new ProgramTypeRequest.SubTypeRequest(null, "Premier culte", "08:00", null, true, 1),
                        new ProgramTypeRequest.SubTypeRequest(null, "Deuxième culte", "10:00", null, true, 2)
                ));

        var result = programService.create(request);

        assertEquals("DIMANCHE", result.code());
        verify(programTypeRepository).save(any(ProgramType.class));
        verify(programSubTypeRepository, times(2)).save(any(ProgramSubType.class));
    }

    @Test
    void create_WithDuplicateCode_ShouldThrow() {
        when(programTypeRepository.findByCode("DIMANCHE")).thenReturn(Optional.of(programType));

        ProgramTypeRequest request = new ProgramTypeRequest(
                "DIMANCHE", "Dimanche", null, false, null, true, 0, List.of());

        assertThrows(BusinessRuleException.class, () -> programService.create(request));
    }

    @Test
    void update_ShouldUpdateFields() {
        when(programTypeRepository.findById(typeId)).thenReturn(Optional.of(programType));
        when(programTypeRepository.save(any(ProgramType.class))).thenReturn(programType);
        when(programTypeRepository.findById(typeId)).thenReturn(Optional.of(programType));
        when(programSubTypeRepository.findByProgramTypeIdOrderByOrdreAsc(typeId)).thenReturn(List.of());

        ProgramTypeRequest request = new ProgramTypeRequest(
                "DIMANCHE", "Dimanche (nouveau)", null, false, null, true, 2, List.of());

        var result = programService.update(typeId, request);

        assertEquals("Dimanche (nouveau)", result.label());
    }

    @Test
    void delete_ShouldRemoveTypeAndSubTypes() {
        when(programTypeRepository.findById(typeId)).thenReturn(Optional.of(programType));

        programService.delete(typeId);

        verify(programSubTypeRepository).deleteByProgramTypeId(typeId);
        verify(programTypeRepository).deleteById(typeId);
    }

    @Test
    void findById_Unknown_ShouldThrow() {
        when(programTypeRepository.findById(typeId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> programService.findById(typeId));
    }
}
