package com.discipolat.modules.voicereports;

import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.voicereports.domain.VoiceReport;
import com.discipolat.modules.voicereports.domain.VoiceReportRepository;
import com.discipolat.modules.voicereports.domain.VoiceReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoiceReportServiceTest {

    @Mock private VoiceReportRepository repository;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private SecurityUtils securityUtils;

    private VoiceReportService service;

    @BeforeEach
    void setUp() {
        service = new VoiceReportService(repository, propagationPublisher, securityUtils);
        lenient().when(securityUtils.getCurrentTenantId()).thenReturn(UUID.randomUUID());
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        lenient().when(repository.save(any(VoiceReport.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void extraction_personnesEtActionsDepuisTranscription() {
        String json = service.extractEntities(
                "J'ai vu Jean ce matin chez Marie. Il est découragé. " +
                "Il faut organiser une visite demain et appeler le pasteur.");

        assertThat(json).contains("\"personnes\":[\"Jean\",\"Marie\"]");
        assertThat(json).contains("\"humeur\":\"DECOURAGE\"");
        assertThat(json).contains("\"actions\":[");
        assertThat(json).contains("visite");
    }

    @Test
    void extraction_besoinPriereDetecte() {
        String json = service.extractEntities("Merci de prier pour la santé de sa mère.");
        assertThat(json).contains("\"besoinPriere\":true");
    }

    @Test
    void extraction_vide_siTranscriptionNulle() {
        String json = service.extractEntities(null);
        assertThat(json).contains("\"personnes\":[]").contains("\"besoinPriere\":false");
    }

    @Test
    void create_extraitEntitesEtMarqueProcessed() {
        VoiceReport report = service.create(VoiceReport.builder()
                .durationSeconds(95)
                .transcript("J'ai vu Grace. Elle est triste.")
                .syncedOffline(true)
                .build());

        assertThat(report.isProcessed()).isTrue();
        assertThat(report.getExtractedEntities()).contains("Grace");
        verify(propagationPublisher).publishCreated(
                eq("VOICE_REPORT"), any(UUID.class), any(Map.class), anyString());
    }
}
