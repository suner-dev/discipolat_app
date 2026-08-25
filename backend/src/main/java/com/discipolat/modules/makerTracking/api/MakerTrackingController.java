package com.discipolat.modules.makerTracking.api;

import com.discipolat.modules.makerTracking.domain.MakerTracking;
import com.discipolat.modules.makerTracking.domain.MakerTrackingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/maker-tracking")
@PreAuthorize("isAuthenticated()")
public class MakerTrackingController {

    private final MakerTrackingService service;
    public MakerTrackingController(MakerTrackingService service) { this.service = service; }

    @GetMapping("/by-faiseur/{faiseurId}")
    public List<MakerTracking> list(@PathVariable UUID faiseurId) { return service.listByFaiseur(faiseurId); }

    @GetMapping("/{id}")
    public MakerTracking get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<MakerTracking> create(@RequestBody MakerTracking t) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(t));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resume/{faiseurId}")
    public Map<String, Object> resume(@PathVariable UUID faiseurId) { return service.getResume(faiseurId); }
}
