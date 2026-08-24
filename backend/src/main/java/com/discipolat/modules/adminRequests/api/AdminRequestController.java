package com.discipolat.modules.adminRequests.api;

import com.discipolat.modules.adminRequests.domain.AdminRequest;
import com.discipolat.modules.adminRequests.domain.AdminRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin-requests")
public class AdminRequestController {

    private final AdminRequestService service;
    public AdminRequestController(AdminRequestService service) { this.service = service; }

    @GetMapping
    public List<AdminRequest> list() { return service.listAll(); }

    @GetMapping("/by-member/{membreId}")
    public List<AdminRequest> listByMember(@PathVariable UUID membreId) {
        return service.listByMember(membreId);
    }

    @GetMapping("/{id}")
    public AdminRequest get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<AdminRequest> create(@RequestBody AdminRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PostMapping("/{id}/process")
    public AdminRequest process(@PathVariable UUID id,
            @RequestParam AdminRequest.Statut decision,
            @RequestParam UUID traiteurId,
            @RequestParam(required = false) String commentaire) {
        return service.process(id, decision, traiteurId, commentaire);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() { return service.getStats(); }
}
