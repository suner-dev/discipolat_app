package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.CustomPage;
import com.discipolat.modules.platform.domain.PageBuilderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API du Page Builder (pages personnalisées).
 *
 * GET    /api/v1/pages            → toutes les pages (ADMIN)
 * POST   /api/v1/pages            → créer une page (ADMIN)
 * PUT    /api/v1/pages/{id}       → modifier une page (ADMIN)
 * DELETE /api/v1/pages/{id}       → supprimer une page (ADMIN)
 * POST   /api/v1/pages/{id}/publish → publier/dépublier (ADMIN)
 * GET    /api/v1/pages/{slug}     → rendu public résolu (authentifié + rôles)
 * GET    /api/v1/pages/sources    → catalogue des sources de données (ADMIN)
 */
@RestController
@RequestMapping("/api/v1/pages")
public class PageBuilderController {

    private final PageBuilderService pageBuilderService;

    public PageBuilderController(PageBuilderService pageBuilderService) {
        this.pageBuilderService = pageBuilderService;
    }

    /* ----------------------------- Admin ----------------------------- */

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<CustomPage>> listPages() {
        return ResponseEntity.ok(pageBuilderService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<CustomPage> createPage(@Valid @RequestBody CustomPage page) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pageBuilderService.create(page));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<CustomPage> updatePage(@PathVariable UUID id, @Valid @RequestBody CustomPage page) {
        return ResponseEntity.ok(pageBuilderService.update(id, page));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> deletePage(@PathVariable UUID id) {
        pageBuilderService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<CustomPage> setPublished(@PathVariable UUID id,
                                                   @RequestBody Map<String, Boolean> body) {
        boolean published = body.getOrDefault("published", true);
        return ResponseEntity.ok(pageBuilderService.setPublished(id, published));
    }

    /** Aperçu (résolu) d'une page, y compris non publiée. */
    @GetMapping("/preview/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<ResolvedPage> preview(@PathVariable UUID id) {
        return ResponseEntity.ok(pageBuilderService.resolvePreview(id));
    }

    @GetMapping("/sources")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<PageDataSource>> sources() {
        return ResponseEntity.ok(pageBuilderService.sources());
    }

    @GetMapping("/options")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> options() {
        return ResponseEntity.ok(pageBuilderService.roleOptions());
    }

    /* ----------------------------- Public ---------------------------- */

    /** Rendu public d'une page personnalisée (résolue sur données réelles). */
    @GetMapping("/{slug}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResolvedPage> render(@PathVariable String slug) {
        return ResponseEntity.ok(pageBuilderService.resolve(slug));
    }
}
