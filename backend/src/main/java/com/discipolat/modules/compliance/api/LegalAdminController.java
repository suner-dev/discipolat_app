package com.discipolat.modules.compliance.api;

import com.discipolat.modules.compliance.domain.LegalDocument;
import com.discipolat.modules.compliance.domain.LegalDocumentRepository;
import com.discipolat.modules.compliance.domain.LegalDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Administration des documents légaux (plateforme). Les versions publiées
 * sont immuables : seule la publication d'une version supérieure est possible.
 */
@RestController
@RequestMapping("/api/v1/platform/admin/legal")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
public class LegalAdminController {

    private final LegalDocumentService service;
    private final LegalDocumentRepository repository;

    public LegalAdminController(LegalDocumentService service, LegalDocumentRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    public record PublishRequest(
            @NotBlank(message = "code is required") String code,
            @NotBlank(message = "title is required") @Size(max = 255) String title,
            @NotBlank(message = "content is required") String content,
            String language) {
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> publish(@Valid @RequestBody PublishRequest request) {
        LegalDocument doc = service.publish(request.code(), request.title(), request.content(), request.language());
        return ResponseEntity.ok(Map.of(
                "code", doc.getCode(),
                "version", doc.getVersion(),
                "language", doc.getLanguage(),
                "message", "Nouvelle version publiée — les consentements futurs référenceront cette version"));
    }

    @GetMapping("/{code}/versions")
    public ResponseEntity<List<Map<String, Object>>> versions(@PathVariable String code,
                                                              @RequestParam(defaultValue = "fr") String language) {
        List<Map<String, Object>> rows = repository
                .findByCodeAndLanguageOrderByVersionDesc(code.toUpperCase(), language).stream()
                .map(d -> Map.<String, Object>of(
                        "version", d.getVersion(),
                        "title", d.getTitle(),
                        "published", d.getPublished(),
                        "publishedAt", d.getPublishedAt().toString()))
                .toList();
        return ResponseEntity.ok(rows);
    }
}
