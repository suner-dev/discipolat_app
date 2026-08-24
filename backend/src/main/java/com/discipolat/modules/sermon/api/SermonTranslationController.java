package com.discipolat.modules.sermon.api;

import com.discipolat.modules.sermon.domain.SermonTranslation;
import com.discipolat.modules.sermon.domain.SermonTranslationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sermons/translations")
public class SermonTranslationController {

    private final SermonTranslationService service;

    public SermonTranslationController(SermonTranslationService service) {
        this.service = service;
    }

    @GetMapping
    public List<SermonTranslation> listAll() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public SermonTranslation get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/by-sermon/{sermonId}")
    public List<SermonTranslation> listBySermon(@PathVariable UUID sermonId) {
        return service.listBySermon(sermonId);
    }

    @PostMapping
    public ResponseEntity<SermonTranslation> requestTranslation(
            @RequestBody Map<String, String> body) {
        UUID sermonId = UUID.fromString(body.get("sermonId"));
        SermonTranslation.Langue langue = SermonTranslation.Langue.valueOf(
                body.getOrDefault("langue", "EN"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.requestTranslation(sermonId, langue));
    }

    @PostMapping("/{id}/complete")
    public SermonTranslation completeTranslation(@PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return service.completeTranslation(id, body.get("text"), body.get("subtitles"));
    }
}
