package com.discipolat.modules.sermon.api;

import com.discipolat.modules.sermon.domain.SermonAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sermon-assistant")
public class SermonAssistantController {

    private final SermonAssistantService service;

    public SermonAssistantController(SermonAssistantService service) {
        this.service = service;
    }

    @PostMapping("/outlines")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Map<String, Object>> outlines(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.generateOutlines(
                body.get("passage"),
                body.get("theme"),
                body.get("audience"),
                body.get("durationMinutes")));
    }
}
