package com.discipolat.modules.platform.domain;

import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.platform.api.UpdateChurchSettingsRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChurchSettingsServiceTest {

    @Mock private ChurchSettingsRepository repository;
    @Mock private AuditService auditService;
    @Mock private com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher propagationPublisher;

    private ChurchSettingsService service;

    @BeforeEach
    void setUp() {
        service = new ChurchSettingsService(repository, auditService, propagationPublisher);
    }

    @Test
    void getSettings_createsDefaultWhenAbsent() {
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.empty());
        when(repository.save(any(ChurchSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        ChurchSettings settings = service.getSettings();

        assertThat(settings.getChurchName()).isEqualTo("Discipolat");
        assertThat(settings.getPlatformName()).isEqualTo("Discipolat");
        assertThat(settings.getPrimaryColor()).isEqualTo("#16a34a");
        assertThat(settings.isAllowDarkMode()).isTrue();
        verify(repository).save(any(ChurchSettings.class));
    }

    @Test
    void getSettings_returnsExistingRow() {
        ChurchSettings existing = ChurchSettings.builder()
                .id(UUID.randomUUID()).churchName("Église de la Grâce").build();
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        assertThat(service.getSettings().getChurchName()).isEqualTo("Église de la Grâce");
        verify(repository, never()).save(any());
    }

    @Test
    void update_appliesOnlyNonNullFields() {
        ChurchSettings existing = ChurchSettings.builder()
                .id(UUID.randomUUID()).churchName("Discipolat").platformName("Discipolat")
                .primaryColor("#16a34a").accentColor("#f59e0b").buttonColor("#16a34a")
                .fontFamily("Inter").allowDarkMode(true).build();
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        service.update(new UpdateChurchSettingsRequest(
                "Église de la Grâce", null, null, null, null, null, null,
                "#0f766e", null, null, null, null, null, null, null, null, null, null));

        assertThat(existing.getChurchName()).isEqualTo("Église de la Grâce");
        assertThat(existing.getPlatformName()).isEqualTo("Discipolat"); // inchangé
        assertThat(existing.getPrimaryColor()).isEqualTo("#0f766e");
        assertThat(existing.getAccentColor()).isEqualTo("#f59e0b"); // inchangé
        verify(repository).save(existing);
    }

    @Test
    void update_ignoresInvalidHexColors() {
        ChurchSettings existing = ChurchSettings.builder()
                .id(UUID.randomUUID()).churchName("Discipolat").platformName("Discipolat")
                .primaryColor("#16a34a").accentColor("#f59e0b").buttonColor("#16a34a")
                .fontFamily("Inter").allowDarkMode(true).build();
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        service.update(new UpdateChurchSettingsRequest(
                null, null, null, null, null, null, null,
                "pas-une-couleur", "red", "#12", null, null, null, null, null, null, null, null));

        assertThat(existing.getPrimaryColor()).isEqualTo("#16a34a");
        assertThat(existing.getAccentColor()).isEqualTo("#f59e0b");
        assertThat(existing.getButtonColor()).isEqualTo("#16a34a");
    }

    @Test
    void update_tracesAuditLog() {
        ChurchSettings existing = ChurchSettings.builder()
                .id(UUID.randomUUID()).churchName("Discipolat").platformName("Discipolat")
                .primaryColor("#16a34a").accentColor("#f59e0b").buttonColor("#16a34a")
                .fontFamily("Inter").allowDarkMode(true).build();
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        service.update(new UpdateChurchSettingsRequest(
                "Nouveau Nom", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null));

        verify(propagationPublisher).publishUpdated(eq("CHURCH_SETTINGS"), eq(existing.getId()), any(), any(), anyString());
        assertThat(existing.getChurchName()).isEqualTo("Nouveau Nom");
    }

    @Test
    void resetToDefaults_restoresDefaultValues() {
        ChurchSettings existing = ChurchSettings.builder()
                .id(UUID.randomUUID()).churchName("Église custom").platformName("Custom")
                .slogan("Slogan custom").primaryColor("#0f766e").accentColor("#b91c1c")
                .buttonColor("#0f766e").fontFamily("Poppins").allowDarkMode(false).build();
        when(repository.findFirstByOrderByCreatedAtAsc()).thenReturn(Optional.of(existing));

        service.resetToDefaults();

        assertThat(existing.getChurchName()).isEqualTo("Discipolat");
        assertThat(existing.getPrimaryColor()).isEqualTo("#16a34a");
        assertThat(existing.getAccentColor()).isEqualTo("#f59e0b");
        assertThat(existing.getFontFamily()).isEqualTo("Inter");
        assertThat(existing.isAllowDarkMode()).isTrue();
        assertThat(existing.getSlogan()).isNull();
        verify(propagationPublisher).publishStatusChanged(eq("CHURCH_SETTINGS"), eq(existing.getId()), anyString(), anyString(), anyString());
    }
}
