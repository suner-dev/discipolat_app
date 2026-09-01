package com.discipolat.modules.aid.api;

import com.discipolat.modules.aid.domain.EmergencyAidRequest;
import com.discipolat.modules.aid.domain.EmergencyAidService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/aid")
public class EmergencyAidController {

    private final EmergencyAidService service;

    public EmergencyAidController(EmergencyAidService service) {
        this.service = service;
    }

    /** Bouton « Urgence Pastorale » : ouvre une demande + plan de secours automatisé. */
    @PostMapping("/emergency")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<EmergencyAidRequest> open(@RequestBody EmergencyAidRequest request) {
        return ResponseEntity.ok(service.open(request));
    }

    @GetMapping("/emergency")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<EmergencyAidRequest>> recent() {
        return ResponseEntity.ok(service.recent());
    }

    @GetMapping("/emergency/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<EmergencyAidRequest>> openRequests() {
        return ResponseEntity.ok(service.openRequests());
    }

    @PostMapping("/emergency/{id}/collect")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<EmergencyAidRequest> collect(@PathVariable java.util.UUID id,
                                                       @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(service.addCollected(id, amount));
    }

    @PostMapping("/emergency/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<EmergencyAidRequest> resolve(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(service.resolve(id));
    }

    /* ------------------------- Bureau de change ------------------------- */

    /** Conversion dons diaspora (ex: 100 USD → XOF). */
    @GetMapping("/exchange")
    public ResponseEntity<Map<String, Object>> convert(@RequestParam BigDecimal amount,
                                                       @RequestParam(defaultValue = "XOF") String from,
                                                       @RequestParam(defaultValue = "USD") String to) {
        return ResponseEntity.ok(service.convert(amount, from, to));
    }

    @GetMapping("/exchange/rates")
    public ResponseEntity<Map<String, Double>> rates() {
        return ResponseEntity.ok(service.rates());
    }
}
