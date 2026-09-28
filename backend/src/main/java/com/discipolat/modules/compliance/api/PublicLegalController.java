package com.discipolat.modules.compliance.api;

import com.discipolat.modules.compliance.domain.LegalDocument;
import com.discipolat.modules.compliance.domain.LegalDocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture publique des documents légaux (landing, inscription, paramètres).
 * Chemin sous /api/v1/public/** → permitAll (cf. SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/public/legal")
public class PublicLegalController {

    private final LegalDocumentService service;

    public PublicLegalController(LegalDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(service.listLatest().stream().map(service::toSummary).toList());
    }

    /**
     * Contenu d'un document. Par défaut : dernière version publiée.
     * ?version=N permet de récupérer un texte historique (preuve de consentement).
     */
    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String code,
                                                   @RequestParam(required = false) Integer version,
                                                   @RequestParam(defaultValue = "fr") String language) {
        LegalDocument doc = version != null
                ? service.getVersion(code.toUpperCase(), version, language)
                : service.getLatest(code.toUpperCase(), language);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", doc.getCode());
        body.put("version", doc.getVersion());
        body.put("language", doc.getLanguage());
        body.put("title", doc.getTitle());
        body.put("content", doc.getContent());
        body.put("publishedAt", doc.getPublishedAt().toString());
        return ResponseEntity.ok(body);
    }
}
