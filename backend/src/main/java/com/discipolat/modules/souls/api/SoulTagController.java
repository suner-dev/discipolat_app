package com.discipolat.modules.souls.api;

import com.discipolat.modules.souls.domain.SoulTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/souls")
public class SoulTagController {

    private final SoulTagService soulTagService;

    public SoulTagController(SoulTagService soulTagService) {
        this.soulTagService = soulTagService;
    }

    @GetMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> getTags(@PathVariable UUID id) {
        return ResponseEntity.ok(soulTagService.getTags(id));
    }

    @PostMapping("/{id}/tags")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> addTag(@PathVariable UUID id,
                                               @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(soulTagService.addTag(id, body.getOrDefault("tag", "")));
    }

    @DeleteMapping("/{id}/tags/{tag}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> removeTag(@PathVariable UUID id, @PathVariable String tag) {
        return ResponseEntity.ok(soulTagService.removeTag(id, tag));
    }

    @GetMapping("/tags/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> allTags() {
        return ResponseEntity.ok(soulTagService.listAllTags());
    }
}
