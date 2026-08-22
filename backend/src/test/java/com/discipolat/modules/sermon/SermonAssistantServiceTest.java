package com.discipolat.modules.sermon;

import com.discipolat.modules.sermon.domain.SermonAssistantService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SermonAssistantServiceTest {

    private final SermonAssistantService service = new SermonAssistantService();

    @Test
    void genereTroisStructuresParDefaut() {
        Map<String, Object> result = service.generateOutlines("Psaume 23", "la provision de Dieu",
                "FAMILLES", null);
        List<Map<String, Object>> outlines = (List<Map<String, Object>>) result.get("outlines");
        assertThat(outlines).hasSize(3);
        assertThat(result.get("theme")).isEqualTo("la provision de Dieu");
        // Chaque outline a accroche + points + applications + appel
        for (Map<String, Object> o : outlines) {
            assertThat(o).containsKeys("type", "title", "accroche", "points", "applications", "appel");
            assertThat((List<?>) o.get("points")).isNotEmpty();
        }
    }

    @Test
    void dureeLongue_genereCinqStructures() {
        Map<String, Object> result = service.generateOutlines("Actes 2", "Pentecôte", null, "45");
        assertThat((List<?>) result.get("outlines")).hasSize(5);
    }

    @Test
    void passageObligatoire() {
        assertThatThrownBy(() -> service.generateOutlines(null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passage");
    }

    @Test
    void audienceFaiseurs_applicationsDediees() {
        Map<String, Object> result = service.generateOutlines("Matthieu 28", "mission", "FAISEURS", null);
        List<Map<String, Object>> outlines = (List<Map<String, Object>>) result.get("outlines");
        assertThat(outlines.get(0).get("applications").toString()).contains("âme à visiter");
    }

    @Test
    void versetsSuggeres_selonLeTheme() {
        Map<String, Object> result = service.generateOutlines("Romains 15", "espérance", null, null);
        List<Map<String, String>> verses = (List<Map<String, String>>) result.get("versetsSuggeres");
        assertThat(verses.stream().anyMatch(v -> v.get("ref").contains("Jérémie 29:11"))).isTrue();
    }
}
