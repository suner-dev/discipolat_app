package com.discipolat.modules.testimonials.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.testimonials.domain.Testimony;
import com.discipolat.modules.testimonials.domain.TestimonyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/testimonies")
public class TestimonyController {

    private final TestimonyService testimonyService;

    public TestimonyController(TestimonyService testimonyService) {
        this.testimonyService = testimonyService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<Testimony>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String categorie) {
        Page<Testimony> result = testimonyService.list(PageRequest.of(page, size), statut, categorie);
        return ResponseEntity.ok(PageResponse.of(result.getContent(), page, size,
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Testimony> get(@PathVariable UUID id) {
        return ResponseEntity.ok(testimonyService.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Testimony> create(@RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Testimony t = testimonyService.create(body.get("titre"), body.get("contenu"),
                body.getOrDefault("categorie", "AUTRE"), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(t);
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Testimony> like(@PathVariable UUID id) {
        return ResponseEntity.ok(testimonyService.like(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<Testimony> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(testimonyService.approve(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Testimony> reject(@PathVariable UUID id) {
        return ResponseEntity.ok(testimonyService.reject(id));
    }
}
