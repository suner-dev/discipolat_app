package com.discipolat.modules.interactions.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.interactions.api.CreateInteractionRequest;
import com.discipolat.modules.interactions.api.InteractionResponse;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * CRM Interactions : chaque contact (appel, SMS, WhatsApp, email, visite,
 * réunion, conseil, prière, suivi) avec une âme est historisé.
 */
@Service
@Transactional
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public InteractionService(InteractionRepository interactionRepository,
                              SoulRepository soulRepository,
                              UserRepository userRepository,
                              SecurityUtils securityUtils) {
        this.interactionRepository = interactionRepository;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    public InteractionResponse create(UUID soulId, CreateInteractionRequest request) {
        if (!soulRepository.existsById(soulId)) {
            throw new EntityNotFoundException("Soul", soulId);
        }
        UUID auteurId = securityUtils.getCurrentUserId();

        Interaction interaction = Interaction.builder()
                .soulId(soulId)
                .auteurId(auteurId)
                .type(request.type())
                .canal(request.canal())
                .objet(request.objet())
                .contenu(request.contenu())
                .dateInteraction(request.dateInteraction() != null ? request.dateInteraction() : LocalDateTime.now())
                .aFairePar(request.aFairePar())
                .rappelLe(request.rappelLe())
                .build();

        Interaction saved = interactionRepository.save(interaction);
        // Met à jour la date de dernier contact de l'âme
        final LocalDateTime contactDate = saved.getDateInteraction();
        soulRepository.findById(soulId).ifPresent(soul -> {
            soul.setDateDernierContact(contactDate);
            soulRepository.save(soul);
        });

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<InteractionResponse> findBySoul(UUID soulId) {
        return interactionRepository.findBySoulIdOrderByDateInteractionDesc(soulId)
                .stream().map(this::toResponse).toList();
    }

    /** Rappels et actions assignées à l'utilisateur connecté (CRM). */
    @Transactional(readOnly = true)
    public List<InteractionResponse> myReminders() {
        UUID userId = securityUtils.getCurrentUserId();
        return interactionRepository.findReminders(userId)
                .stream().map(this::toResponse).toList();
    }

    public void delete(UUID interactionId) {
        Interaction interaction = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new EntityNotFoundException("Interaction", interactionId));
        // Seul l'auteur ou un admin/pasteur peut supprimer
        UUID currentUserId = securityUtils.getCurrentUserId();
        String role = securityUtils.getCurrentUserRole();
        if (!interaction.getAuteurId().equals(currentUserId)
                && !"ADMIN".equals(role) && !"PASTEUR".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Vous ne pouvez supprimer que vos propres interactions");
        }
        interactionRepository.delete(interaction);
    }

    private InteractionResponse toResponse(Interaction i) {
        String auteurNom = userRepository.findById(i.getAuteurId())
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
        String aFaireParNom = i.getAFairePar() != null
                ? userRepository.findById(i.getAFairePar())
                        .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null)
                : null;
        return InteractionResponse.from(i, auteurNom, aFaireParNom);
    }
}
