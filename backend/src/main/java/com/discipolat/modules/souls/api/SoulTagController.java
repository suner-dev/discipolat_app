package com.discipolat.modules.souls.api;

import com.discipolat.modules.souls.domain.SoulTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/soul-tags")
public class SoulTagController {

    private final SoulTagService soulTagService;

    public SoulTagController(SoulTagService soulTagService) {
        this.soulTagService = soulTagService;
    }

    @GetMapping("/{soulId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> getTags(@PathVariable UUID soulId) {
        return ResponseEntity.ok(soulTagService.getTags(soulId));
    }

    @PostMapping("/{soulId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> addTag(@PathVariable UUID soulId,
                                               @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(soulTagService.addTag(soulId, body.getOrDefault("tag", "")));
    }

    @DeleteMapping("/{soulId}/{tag}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> removeTag(@PathVariable UUID soulId, @PathVariable String tag) {
        return ResponseEntity.ok(soulTagService.removeTag(soulId, tag));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<String>> allTags() {
        return ResponseEntity.ok(soulTagService.listAllTags());
    }
}
