package com.discipolat.modules.map.api;

import com.discipolat.modules.map.domain.MapService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/map")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    /** Points cartographiques visibles selon le rôle courant. */
    @GetMapping("/points")
    public ResponseEntity<List<MapPointResponse>> mapPoints() {
        return ResponseEntity.ok(mapService.getMapPoints());
    }

    /** Mise à jour de la position GPS d'un disciple (pasteur/admin/responsable). */
    @PatchMapping("/souls/{soulId}/coordinates")
    public ResponseEntity<MapPointResponse> updateCoordinates(
            @PathVariable UUID soulId,
            @Valid @RequestBody UpdateCoordinatesRequest request) {
        return ResponseEntity.ok(mapService.updateSoulCoordinates(soulId, request));
    }
}
