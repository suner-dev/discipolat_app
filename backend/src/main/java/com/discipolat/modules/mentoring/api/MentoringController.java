package com.discipolat.modules.mentoring.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.mentoring.domain.MentorSuggestion;
import com.discipolat.modules.mentoring.domain.MentoringService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mentoring")
public class MentoringController {

    private final MentoringService service;

    public MentoringController(MentoringService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<PageResponse<MentorSuggestion>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID chefId) {
        UUID userId = chefId != null ? chefId : SecurityUtils.getCurrentUserId();
        Page<MentorSuggestion> suggestions = service.listSuggestions(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(PageResponse.of(suggestions.getContent(), page, size,
                suggestions.getTotalElements(), suggestions.getTotalPages()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<List<MentorSuggestion>> listAll(@RequestParam(required = false) UUID chefId) {
        UUID userId = chefId != null ? chefId : SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.listAllSuggestions(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<MentorSuggestion> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    @SuppressWarnings("unchecked")
    public ResponseEntity<List<MentorSuggestion>> generate(@RequestBody Map<String, Object> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> faiseursData = (List<Map<String, Object>>) body.getOrDefault("faiseurs", List.of());
        return ResponseEntity.ok(service.generateSuggestions(userId, faiseursData));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        service.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        service.archive(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<Map<String, Object>> stats(@RequestParam(required = false) UUID chefId) {
        UUID userId = chefId != null ? chefId : SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.getStats(userId));
    }
}
