package com.discipolat.modules.volunteers.api;

import com.discipolat.modules.volunteers.domain.Volunteer;
import com.discipolat.modules.volunteers.domain.VolunteerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/volunteers")
public class VolunteerController {

    private final VolunteerService service;
    public VolunteerController(VolunteerService service) { this.service = service; }

    @GetMapping
    public List<Volunteer> list() { return service.listActive(); }

    @GetMapping("/{id}")
    public Volunteer get(@PathVariable UUID id) { return service.get(id); }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping
    public ResponseEntity<Volunteer> create(@RequestBody Volunteer v) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(v));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PutMapping("/{id}")
    public Volunteer update(@PathVariable UUID id, @RequestBody Volunteer updates) {
        return service.update(id, updates);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/match")
    public List<Volunteer> match(@RequestParam(required = false) String skill,
            @RequestParam(required = false) Volunteer.Disponibilite disponibilite) {
        return service.matchForEvent(skill, disponibilite);
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return service.getStats(); }
}
