package com.discipolat.modules.demo.domain;

import com.discipolat.modules.demo.api.CreateDemoRequestRequest;
import com.discipolat.modules.demo.api.DemoRequestResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Collecte des demandes de démonstration de la landing page.
 * - Soumission publique (rate-limitée côté contrôleur, validation stricte)
 * - Consultation réservée à l'administration (ADMIN, PASTEUR)
 */
@Service
@Transactional
public class DemoRequestService {

    private final DemoRequestRepository repository;

    public DemoRequestService(DemoRequestRepository repository) {
        this.repository = repository;
    }

    /** Création d'une demande depuis la landing (endpoint public). */
    public DemoRequestResponse create(CreateDemoRequestRequest req) {
        DemoRequest demoRequest = DemoRequest.builder()
                .fullName(req.fullName().trim())
                .email(req.email().trim().toLowerCase())
                .churchName(req.churchName().trim())
                .role(req.role() != null && !req.role().isBlank() ? req.role().trim() : null)
                .message(req.message() != null && !req.message().isBlank() ? req.message().trim() : null)
                .status("NOUVEAU")
                .source(req.source() != null && !req.source().isBlank() ? req.source().trim() : "landing")
                .build();
        repository.save(demoRequest);
        return DemoRequestResponse.from(demoRequest);
    }

    /** Liste complète (les plus récentes d'abord) — usage admin. */
    @Transactional(readOnly = true)
    public List<DemoRequestResponse> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(DemoRequestResponse::from)
                .toList();
    }
}