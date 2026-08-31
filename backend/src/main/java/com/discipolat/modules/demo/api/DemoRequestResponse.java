package com.discipolat.modules.demo.api;

import com.discipolat.modules.demo.domain.DemoRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/** Réponse d'une demande de démonstration (création ou consultation admin). */
public record DemoRequestResponse(
        UUID id,
        String fullName,
        String email,
        String churchName,
        String role,
        String message,
        String status,
        String source,
        LocalDateTime createdAt
) {
    public static DemoRequestResponse from(DemoRequest d) {
        return new DemoRequestResponse(
                d.getId(), d.getFullName(), d.getEmail(), d.getChurchName(),
                d.getRole(), d.getMessage(), d.getStatus(), d.getSource(), d.getCreatedAt());
    }
}