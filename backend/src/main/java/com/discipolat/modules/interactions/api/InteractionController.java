package com.discipolat.modules.interactions.api;

import com.discipolat.modules.interactions.domain.InteractionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping("/souls/{soulId}/interactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<InteractionResponse>> findBySoul(@PathVariable UUID soulId) {
        return ResponseEntity.ok(interactionService.findBySoul(soulId));
    }

    @PostMapping("/souls/{soulId}/interactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<InteractionResponse> create(
            @PathVariable UUID soulId,
            @Valid @RequestBody CreateInteractionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interactionService.create(soulId, request));
    }

    @DeleteMapping("/interactions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        interactionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Rappels et actions assignées à l'utilisateur connecté (CRM). */
    @GetMapping("/interactions/my-reminders")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<List<InteractionResponse>> myReminders() {
        return ResponseEntity.ok(interactionService.myReminders());
    }
}
