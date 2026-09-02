package com.discipolat.modules.visits.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.badges.domain.BadgeService;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.visits.api.CreateVisitRequest;
import com.discipolat.modules.visits.api.UpdateVisitRequest;
import com.discipolat.modules.visits.api.VisitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestion des visites pastorales : planification, réalisation, compte rendu,
 * photo, présence, objectifs et suivi. Chaque visite est rattachée à une âme
 * et à un visiteur (faiseur, responsable ou pasteur).
 */
@Service
@Transactional
public class VisitService {

    private final VisitRepository visitRepository;
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final BadgeService badgeService;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public VisitService(VisitRepository visitRepository,
                        SoulRepository soulRepository,
                        UserRepository userRepository,
                        BadgeService badgeService,
                        EntityPropagationPublisher propagationPublisher,
                        SecurityUtils securityUtils) {
        this.visitRepository = visitRepository;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.badgeService = badgeService;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    public VisitResponse create(CreateVisitRequest request) {
        if (!soulRepository.existsById(request.soulId())) {
            throw new EntityNotFoundException("Soul", request.soulId());
        }
        Visit visit = Visit.builder()
                .soulId(request.soulId())
                .visiteurId(securityUtils.getCurrentUserId())
                .datePrevue(request.datePrevue())
                .motif(request.motif())
                .objectif(request.objectif())
                .statut(Visit.StatutVisite.PLANIFIEE)
                .build();
        Visit saved = visitRepository.save(visit);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("VISIT", saved.getId(),
                Map.of("soulId", saved.getSoulId(), "motif", saved.getMotif() != null ? saved.getMotif() : ""),
                "Visite planifiée");
        return toResponse(saved);
    }

    public VisitResponse update(UUID visitId, UpdateVisitRequest request) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visit", visitId));
        boolean becomesRealisee = request.statut() == Visit.StatutVisite.REALISEE
                && visit.getStatut() != Visit.StatutVisite.REALISEE;
        String oldStatut = visit.getStatut().name();
        visit.setStatut(request.statut());
        if (request.statut() == Visit.StatutVisite.REALISEE) {
            visit.setDateRealisee(request.dateRealisee() != null ? request.dateRealisee() : LocalDate.now());
        } else {
            // Une visite non réalisée ne garde pas de date de réalisation obsolète
            visit.setDateRealisee(null);
        }
        if (request.datePrevue() != null) visit.setDatePrevue(request.datePrevue());
        if (request.compteRendu() != null) visit.setCompteRendu(request.compteRendu());
        if (request.photoUrl() != null) visit.setPhotoUrl(request.photoUrl());
        if (request.present() != null) visit.setPresent(request.present());
        VisitResponse response = toResponse(visitRepository.save(visit));
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("VISIT", visitId,
                oldStatut, visit.getStatut().name(),
                "Statut visite: " + oldStatut + " -> " + visit.getStatut());

        // Gamification : le score évolue automatiquement quand une visite est réalisée
        if (becomesRealisee) {
            badgeService.evaluate();
        }
        return response;
    }

    /** Visites d'une âme (fiche 360°). */
    @Transactional(readOnly = true)
    public List<VisitResponse> findBySoul(UUID soulId) {
        return visitRepository.findBySoulIdOrderByDatePrevueDesc(soulId)
                .stream().map(this::toResponse).toList();
    }

    /** Visites planifiées/à venir de l'utilisateur connecté. */
    @Transactional(readOnly = true)
    public List<VisitResponse> myVisits() {
        UUID userId = securityUtils.getCurrentUserId();
        return visitRepository.findByVisiteurIdOrderByDatePrevueDesc(userId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> upcoming() {
        return visitRepository.findByStatutOrderByDatePrevueAsc(Visit.StatutVisite.PLANIFIEE)
                .stream().map(this::toResponse).toList();
    }

    /** Liste paginée de toutes les visites avec filtres optionnels (statut + texte). */
    @Transactional(readOnly = true)
    public Page<VisitResponse> findAll(String statut, String search, Pageable pageable) {
        Visit.StatutVisite st = (statut != null && !statut.isBlank())
                ? Visit.StatutVisite.valueOf(statut) : null;
        String trimmed = (search == null || search.isBlank()) ? null : search.trim();
        return visitRepository.search(st, trimmed, pageable).map(this::toResponse);
    }

    public void delete(UUID visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new EntityNotFoundException("Visit", visitId));
        UUID currentUserId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentUserRole();
        if (!visit.getVisiteurId().equals(currentUserId)
                && !"ADMIN".equals(role) && !"PASTEUR".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne pouvez supprimer que vos propres visites");
        }
        visitRepository.delete(visit);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishDeleted("VISIT", visitId,
                Map.of("soulId", visit.getSoulId()),
                "Visite supprimée");
    }

    private VisitResponse toResponse(Visit v) {
        String soulNom = soulRepository.findById(v.getSoulId())
                .map(s -> (s.getPrenom() != null && !s.getPrenom().isBlank()
                        ? s.getPrenom() + " " : "") + s.getNom())
                .orElse(null);
        String visiteurNom = userRepository.findById(v.getVisiteurId())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        return VisitResponse.from(v, soulNom, visiteurNom);
    }
}
