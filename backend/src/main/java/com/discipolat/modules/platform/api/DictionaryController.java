package com.discipolat.modules.platform.api;

import com.discipolat.modules.platform.domain.DictionaryEntry;
import com.discipolat.modules.platform.domain.DictionaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API des dictionnaires de la plateforme (référentiels configurables).
 *
 * GET  /api/v1/dictionaries           → entrées ACTIVES groupées par clé (authentifié)
 * GET  /api/v1/dictionaries/{key}     → entrées actives d'un dictionnaire
 * GET  /api/v1/admin/dictionaries     → toutes les entrées (ADMIN)
 * POST /api/v1/admin/dictionaries/{key} → créer une entrée (ADMIN)
 * PUT  /api/v1/admin/dictionaries/{id}  → modifier une entrée (ADMIN)
 * DELETE /api/v1/admin/dictionaries/{id} → supprimer une entrée (ADMIN)
 * POST /api/v1/admin/dictionaries/reset → restaurer les défauts (ADMIN)
 */
@RestController
@RequestMapping("/api/v1")
public class DictionaryController {

    private final DictionaryService dictionaryService;

    public DictionaryController(DictionaryService dictionaryService) {
        this.dictionaryService = dictionaryService;
    }

    /* ------------------------- Lecture (application) ------------------------- */

    @GetMapping("/dictionaries")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, List<DictionaryEntry>>> activeDictionaries() {
        return ResponseEntity.ok(dictionaryService.activeGrouped());
    }

    @GetMapping("/dictionaries/{key}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DictionaryEntry>> activeByKey(@PathVariable String key) {
        return ResponseEntity.ok(dictionaryService.activeByKey(key));
    }

    /* ---------------------------- Administration ---------------------------- */

    @GetMapping("/admin/dictionaries")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, List<DictionaryEntry>>> allDictionaries() {
        return ResponseEntity.ok(dictionaryService.allGrouped());
    }

    @PostMapping("/admin/dictionaries/{key}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<DictionaryEntry> create(@PathVariable String key,
                                                  @Valid @RequestBody DictionaryEntry entry) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dictionaryService.create(key, entry));
    }

    @PutMapping("/admin/dictionaries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<DictionaryEntry> update(@PathVariable UUID id,
                                                  @Valid @RequestBody DictionaryEntry entry) {
        return ResponseEntity.ok(dictionaryService.update(id, entry));
    }

    @DeleteMapping("/admin/dictionaries/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dictionaryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/dictionaries/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Void> reset() {
        dictionaryService.resetDefaults();
        return ResponseEntity.noContent().build();
    }
}
