package com.discipolat.modules.tickets.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.tickets.domain.Ticket;
import com.discipolat.modules.tickets.domain.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@PreAuthorize("isAuthenticated()")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<PageResponse<Ticket>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String categorie) {
        Page<Ticket> tickets = ticketService.list(PageRequest.of(page, size), statut, categorie);
        return ResponseEntity.ok(PageResponse.from(tickets));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    // TODO: add ownership check — verify the ticket belongs to the authenticated user or user has admin role
    public ResponseEntity<Ticket> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Ticket> create(@Valid @RequestBody CreateTicketRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Ticket ticket = ticketService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Ticket> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Ticket ticket = ticketService.updateStatus(id, body.get("statut"));
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Ticket> addMessage(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        UUID userId = SecurityUtils.getCurrentUserId();
        Ticket ticket = ticketService.addMessage(id, body.get("contenu"), userId);
        return ResponseEntity.ok(ticket);
    }
}
