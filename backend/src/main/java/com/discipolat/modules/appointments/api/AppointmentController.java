package com.discipolat.modules.appointments.api;

import com.discipolat.modules.appointments.domain.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /** Prise de rendez-vous par l'utilisateur connecté. */
    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    /** Rendez-vous demandés par l'utilisateur connecté. */
    @GetMapping("/my")
    public ResponseEntity<List<AppointmentResponse>> myRequests() {
        return ResponseEntity.ok(appointmentService.myRequests());
    }

    /** Rendez-vous reçus (boîte de réception du récepteur). */
    @GetMapping("/inbox")
    public ResponseEntity<List<AppointmentResponse>> myInbox() {
        return ResponseEntity.ok(appointmentService.myInbox());
    }

    /** Validation du rendez-vous par le récepteur ou annulation par le demandeur. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentStatusRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request));
    }
}
