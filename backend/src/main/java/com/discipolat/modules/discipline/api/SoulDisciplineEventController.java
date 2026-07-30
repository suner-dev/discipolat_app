package com.discipolat.modules.discipline.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.discipline.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/souls/{soulId}/discipline")
public class SoulDisciplineEventController {

    private final SoulDisciplineEventService service;

    public SoulDisciplineEventController(SoulDisciplineEventService service) {
        this.service = service;
    }

    record CreateDisciplineRequest(
            @NotNull CategorieDiscipline categorie,
            @NotBlank String typeEvenement,
            @NotBlank String titre,
            String description,
            GraviteDiscipline gravite,
            LocalDate dateEvenement
    ) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable UUID soulId,
            @Valid @RequestBody CreateDisciplineRequest request) {
        SoulDisciplineEvent event = service.create(
                soulId, request.categorie(), request.typeEvenement(),
                request.titre(), request.description(), request.gravite(),
                request.dateEvenement());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(event));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<Map<String, Object>>> findAll(
            @PathVariable UUID soulId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String categorie) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SoulDisciplineEvent> events;
        if (categorie != null && !categorie.isEmpty()) {
            events = service.findByAmeIdAndCategorie(soulId, CategorieDiscipline.valueOf(categorie), pageable);
        } else {
            events = service.findByAmeId(soulId, pageable);
        }
        Page<Map<String, Object>> response = events.map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> stats(@PathVariable UUID soulId) {
        return ResponseEntity.ok(service.getStats(soulId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<Map<String, Object>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(service.findById(id)));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, Object>> resolve(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String resolution = body != null ? body.get("resolution") : null;
        return ResponseEntity.ok(toResponse(service.resolve(id, resolution)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(SoulDisciplineEvent event) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", event.getId());
        resp.put("ameId", event.getAmeId());
        resp.put("auteurId", event.getAuteurId());
        resp.put("categorie", event.getCategorie().name());
        resp.put("typeEvenement", event.getTypeEvenement());
        resp.put("gravite", event.getGravite() != null ? event.getGravite().name() : null);
        resp.put("titre", event.getTitre());
        resp.put("description", event.getDescription());
        resp.put("dateEvenement", event.getDateEvenement().toString());
        resp.put("resolu", event.isResolu());
        resp.put("dateResolution", event.getDateResolution() != null ? event.getDateResolution().toString() : null);
        resp.put("resoluPar", event.getResoluPar());
        resp.put("createdAt", event.getCreatedAt().toString());
        return resp;
    }
}
