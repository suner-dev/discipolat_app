package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentSettingsServiceTest {

    @Mock
    private DepartmentService departmentService;
    @Mock
    private DepartmentSettingRepository settingRepository;

    private DepartmentSettingsService service;
    private final UUID deptId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DepartmentSettingsService(departmentService, settingRepository);
        lenient().when(departmentService.findById(deptId)).thenReturn(new Department());
    }

    @Test
    void getSettings_creeLaLigneParDefautSiAbsente() {
        when(settingRepository.findById(deptId)).thenReturn(Optional.empty());
        when(settingRepository.save(any(DepartmentSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.getSettings(deptId);

        assertThat(result.get("absenceSeuil")).isEqualTo(2);
        assertThat(result.get("absencePeriode")).isEqualTo(3);
        assertThat(result.get("inactiviteMois")).isEqualTo(3);
        assertThat(result.get("tacheRetardAlerte")).isEqualTo(true);
        verify(settingRepository).save(any(DepartmentSetting.class));
    }

    @Test
    void updateSettings_modifieSeulementLesChampsFournis() {
        DepartmentSetting existing = DepartmentSetting.builder()
                .departmentId(deptId).absenceSeuil(2).absencePeriode(3).inactiviteMois(3)
                .tacheRetardAlerte(true).build();
        when(settingRepository.findById(deptId)).thenReturn(Optional.of(existing));

        Map<String, Object> result = service.updateSettings(deptId, Map.of(
                "absenceSeuil", 5,
                "tacheRetardAlerte", false));

        assertThat(result.get("absenceSeuil")).isEqualTo(5);
        assertThat(result.get("absencePeriode")).isEqualTo(3); // inchangé
        assertThat(result.get("tacheRetardAlerte")).isEqualTo(false);
        verify(settingRepository).save(existing);
    }

    @Test
    void updateSettings_seuilHorsBornes_refuse() {
        when(settingRepository.findById(deptId)).thenReturn(Optional.empty());
        when(settingRepository.save(any(DepartmentSetting.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.updateSettings(deptId, Map.of("absenceSeuil", 50)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.updateSettings(deptId, Map.of("inactiviteMois", -1)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void effectiveSettings_retourneLaLigneExistante() {
        DepartmentSetting existing = DepartmentSetting.builder()
                .departmentId(deptId).absenceSeuil(4).build();
        when(settingRepository.findById(deptId)).thenReturn(Optional.of(existing));

        DepartmentSetting result = service.effectiveSettings(deptId);

        assertThat(result.getAbsenceSeuil()).isEqualTo(4);
        verify(settingRepository, never()).save(any(DepartmentSetting.class));
    }
}
