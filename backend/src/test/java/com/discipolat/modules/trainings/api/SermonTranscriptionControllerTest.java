package com.discipolat.modules.trainings.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.trainings.domain.SermonTranscription;
import com.discipolat.modules.trainings.domain.SermonTranscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SermonTranscriptionControllerTest {

    @Mock
    private SermonTranscriptionRepository repository;

    private SermonTranscriptionController controller;
    private UUID tenantId;
    private UUID sermonId;

    @BeforeEach
    void setUp() {
        controller = new SermonTranscriptionController(repository);
        tenantId = UUID.randomUUID();
        sermonId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void list_WithoutSearch_ReturnsAll() {
        Page<SermonTranscription> page = new PageImpl<>(List.of(createSermon()));
        when(repository.findByTenantIdOrderByRecordedAtDesc(eq(tenantId), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<SermonTranscription>> response = controller.list(null, Pageable.unpaged());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().getContent().size());
    }

    @Test
    void list_WithSearch_ReturnsFiltered() {
        Page<SermonTranscription> page = new PageImpl<>(List.of(createSermon()));
        when(repository.search(eq(tenantId), eq("grâce"), any(Pageable.class))).thenReturn(page);

        ResponseEntity<Page<SermonTranscription>> response = controller.list("grâce", Pageable.unpaged());

        assertEquals(200, response.getStatusCode().value());
        verify(repository).search(eq(tenantId), eq("grâce"), any(Pageable.class));
    }

    @Test
    void get_ExistingId_ReturnsSermon() {
        SermonTranscription sermon = createSermon();
        when(repository.findById(sermonId)).thenReturn(Optional.of(sermon));

        ResponseEntity<SermonTranscription> response = controller.get(sermonId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("La grâce suffit", response.getBody().getTitle());
    }

    @Test
    void get_NonExistingId_Returns404() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        ResponseEntity<SermonTranscription> response = controller.get(sermonId);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void create_SetsTenantAndStatus() {
        SermonTranscription input = new SermonTranscription();
        input.setTitle("Nouveau sermon");
        input.setSpeaker("Pasteur Jean");

        when(repository.save(any(SermonTranscription.class))).thenAnswer(inv -> {
            SermonTranscription s = inv.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });

        ResponseEntity<SermonTranscription> response = controller.create(input);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(tenantId, response.getBody().getTenantId());
        assertEquals("PENDING", response.getBody().getTranscriptionStatus());
    }

    @Test
    void update_ExistingId_UpdatesFields() {
        SermonTranscription existing = createSermon();
        when(repository.findById(sermonId)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SermonTranscription update = new SermonTranscription();
        update.setTitle("Titre mis à jour");
        update.setSpeaker("Nouveau pasteure");

        ResponseEntity<SermonTranscription> response = controller.update(sermonId, update);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Titre mis à jour", response.getBody().getTitle());
    }

    @Test
    void delete_ExistingId_ReturnsNoContent() {
        doNothing().when(repository).deleteById(sermonId);

        ResponseEntity<Void> response = controller.delete(sermonId);

        assertEquals(204, response.getStatusCode().value());
    }

    @Test
    void triggerTranscription_SetsProcessingStatus() {
        SermonTranscription sermon = createSermon();
        when(repository.findById(sermonId)).thenReturn(Optional.of(sermon));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<Map<String, String>> response = controller.triggerTranscription(sermonId);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PROCESSING", response.getBody().get("status"));
    }

    @Test
    void stats_ReturnsCount() {
        when(repository.countByTenantId(tenantId)).thenReturn(42L);

        ResponseEntity<Map<String, Object>> response = controller.stats();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(42L, response.getBody().get("totalSermons"));
    }

    private SermonTranscription createSermon() {
        SermonTranscription s = new SermonTranscription();
        s.setId(sermonId);
        s.setTenantId(tenantId);
        s.setTitle("La grâce suffit");
        s.setSpeaker("Pasteur Jean");
        s.setTheme("Grâce");
        s.setFullText("Le texte complet du sermon...");
        s.setSummary("Résumé du sermon");
        s.setTranscriptionStatus("COMPLETED");
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }
}
