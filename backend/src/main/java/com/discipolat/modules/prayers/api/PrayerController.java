package com.discipolat.modules.prayers.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.prayers.domain.Prayer;
import com.discipolat.modules.prayers.domain.PrayerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prayers")
public class PrayerController {

    private final PrayerService prayerService;

    public PrayerController(PrayerService prayerService) {
        this.prayerService = prayerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PrayerResponse> create(@Valid @RequestBody CreatePrayerRequest request) {
        Prayer prayer = Prayer.builder()
                .titre(request.titre())
                .description(request.description())
                .familleId(request.familleId())
                .ameId(request.ameId())
                .categorie(request.categorie())
                .priorite(request.priorite() != null ? request.priorite() : "MOYENNE")
                .visibilite(request.visibilite() != null ? request.visibilite() : "PARTAGEE")
                .build();
        prayer = prayerService.create(prayer);
        return ResponseEntity.status(HttpStatus.CREATED).body(PrayerResponse.from(prayer));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PrayerResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(PrayerResponse.from(prayerService.findById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<PrayerResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) UUID auteurId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String visibilite) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Prayer> prayers;
        if (familleId != null && statut != null) {
            prayers = prayerService.findByFamilleIdAndStatut(familleId, statut, pageable);
        } else if (familleId != null && categorie != null) {
            prayers = prayerService.findByFamilleIdAndCategorie(familleId, categorie, pageable);
        } else if (familleId != null) {
            prayers = prayerService.findByFamilleId(familleId, pageable);
        } else if (auteurId != null) {
            prayers = prayerService.findByAuteurId(auteurId, pageable);
        } else if (visibilite != null) {
            prayers = prayerService.findByVisibilite(visibilite, pageable);
        } else {
            prayers = prayerService.findAll(pageable);
        }
        Page<PrayerResponse> response = prayers.map(PrayerResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/by-ame/{ameId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<List<PrayerResponse>> findByAmeId(@PathVariable UUID ameId) {
        return ResponseEntity.ok(prayerService.findByAmeId(ameId)
                .stream().map(PrayerResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PrayerResponse> update(@PathVariable UUID id,
                                                 @Valid @RequestBody UpdatePrayerRequest request) {
        Prayer prayer = Prayer.builder()
                .titre(request.titre())
                .description(request.description())
                .categorie(request.categorie())
                .priorite(request.priorite())
                .visibilite(request.visibilite())
                .build();
        return ResponseEntity.ok(PrayerResponse.from(prayerService.update(id, prayer)));
    }

    @PatchMapping("/{id}/answer")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PrayerResponse> markAsAnswered(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(PrayerResponse.from(
                prayerService.markAsAnswered(id, body.getOrDefault("temoignage", ""))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        prayerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ======================== US-48: ACTIONS DE GRÂCE ========================

    @GetMapping("/actions-de-grace")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<List<PrayerResponse>> getActionsDeGrace(
            @RequestParam(required = false) UUID familleId) {
        List<Prayer> answered;
        if (familleId != null) {
            answered = prayerService.findAnsweredByFamille(familleId);
        } else {
            // Phase 9: filter by role-based visibility
            answered = prayerService.findAllAnsweredByVisibility();
        }
        return ResponseEntity.ok(answered.stream().map(PrayerResponse::from).toList());
    }
}
