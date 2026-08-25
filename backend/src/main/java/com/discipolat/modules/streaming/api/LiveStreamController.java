package com.discipolat.modules.streaming.api;

import com.discipolat.modules.streaming.domain.LiveStream;
import com.discipolat.modules.streaming.domain.LiveStreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/streams")
public class LiveStreamController {

    private final LiveStreamService service;

    public LiveStreamController(LiveStreamService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<LiveStream>> list(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.listByTenant(tenantId));
    }

    @GetMapping("/live")
    public ResponseEntity<List<LiveStream>> live(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.listLive(tenantId));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping
    public ResponseEntity<LiveStream> create(@RequestBody LiveStream stream) {
        return ResponseEntity.ok(service.create(stream));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping("/{id}/go-live")
    public ResponseEntity<LiveStream> goLive(@PathVariable Long id) {
        return ResponseEntity.ok(service.goLive(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    @PostMapping("/{id}/end")
    public ResponseEntity<LiveStream> endStream(@PathVariable Long id) {
        return ResponseEntity.ok(service.endStream(id));
    }

    @PostMapping("/{id}/viewer")
    public ResponseEntity<LiveStream> addViewer(@PathVariable Long id) {
        return ResponseEntity.ok(service.incrementViewers(id));
    }
}
