package com.discipolat.modules.churchComparison.api;

import com.discipolat.modules.churchComparison.domain.ChurchComparison;
import com.discipolat.modules.churchComparison.domain.ChurchComparisonService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/church-comparisons")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
public class ChurchComparisonController {

    private final ChurchComparisonService service;
    public ChurchComparisonController(ChurchComparisonService service) { this.service = service; }

    @GetMapping
    public List<ChurchComparison> list() { return service.listAll(); }

    @GetMapping("/by-category/{cat}")
    public List<ChurchComparison> listByCategory(@PathVariable String cat) {
        return service.listByCategory(cat);
    }

    @GetMapping("/by-country/{pays}")
    public List<ChurchComparison> listByCountry(@PathVariable String pays) {
        return service.listByCountry(pays);
    }

    @GetMapping("/{id}")
    public ChurchComparison get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<ChurchComparison> create(@RequestBody ChurchComparison c) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(c));
    }

    @PutMapping("/{id}")
    public ChurchComparison update(@PathVariable UUID id, @RequestBody ChurchComparison c) {
        c.setId(id);
        return service.save(c);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/benchmark")
    public Map<String, Object> benchmark(@PathVariable UUID id) {
        return service.benchmark(id);
    }

    // P3 #107 — Benchmark amélioré avec clustering anonyme
    @GetMapping("/clusters")
    public Map<String, Object> clusters(@RequestParam(required = false) UUID ourId) {
        return service.clusters(ourId);
    }
}

