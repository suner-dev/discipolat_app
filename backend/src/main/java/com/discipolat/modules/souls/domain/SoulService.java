package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.api.CreateSoulRequest;
import com.discipolat.modules.souls.api.SoulHistoryResponse;
import com.discipolat.modules.souls.api.UpdateSoulRequest;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SoulService {

    private static final Logger log = LoggerFactory.getLogger(SoulService.class);

    private final SoulRepository soulRepository;
    private final SoulHistoryRepository soulHistoryRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SoulService(SoulRepository soulRepository, SoulHistoryRepository soulHistoryRepository,
                       SecurityUtils securityUtils, UserRepository userRepository,
                       NotificationService notificationService) {
        this.soulRepository = soulRepository;
        this.soulHistoryRepository = soulHistoryRepository;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Soul create(CreateSoulRequest request) {
        // US-18: Auto-set faiseurId to current user if not provided
        UUID faiseurId = request.faiseurId() != null ? request.faiseurId() : securityUtils.getCurrentUserId();

        Soul soul = Soul.builder()
                .nom(request.nom())
                .prenom(request.prenom())
                .email(request.email())
                .telephone(request.telephone())
                .adresse(request.adresse())
                .dateNaissance(request.dateNaissance())
                .profession(request.profession())
                .typeDisciple(request.typeDisciple())
                .dateIntegration(request.dateIntegration() != null ? request.dateIntegration() : LocalDate.now())
                .dateConversion(request.dateConversion())
                .statut(StatutAme.EN_INTEGRATION)
                .faiseurId(faiseurId)
                .familleId(request.familleId())
                .situationFamiliale(request.situationFamiliale())
                .etatSpirituel(request.etatSpirituel() != null ? request.etatSpirituel() : "NOUVEAU_CONVERTI")
                .niveauCroissance(request.niveauCroissance() != null ? request.niveauCroissance() : 1)
                .build();
        soul = soulRepository.save(soul);
        logHistory(soul.getId(), "CREATION", "Âme créée", null, soul.getStatut().name(), null, request.faiseurId());
        return soul;
    }

    @Transactional(readOnly = true)
    public Soul findById(UUID id) {
        return soulRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Soul", id));
    }

    @Transactional(readOnly = true)
    public Page<Soul> findAll(UUID faiseurId, UUID familleId, TypeDisciple typeDisciple,
                              StatutAme statut, Pageable pageable) {
        if (faiseurId != null) return soulRepository.findByFaiseurId(faiseurId, pageable);
        if (familleId != null) return soulRepository.findByFamilleId(familleId, pageable);
        if (typeDisciple != null && statut != null)
            return soulRepository.findByTypeDiscipleAndStatut(typeDisciple, statut, pageable);
        if (typeDisciple != null) return soulRepository.findByTypeDisciple(typeDisciple, pageable);
        if (statut != null) return soulRepository.findByStatut(statut, pageable);
        return soulRepository.findAll(pageable);
    }

    public Soul update(UUID id, UpdateSoulRequest request) {
        Soul soul = findById(id);
        String oldStatut = soul.getStatut().name();
        UUID oldFaiseurId = soul.getFaiseurId();

        if (request.nom() != null) soul.setNom(request.nom());
        if (request.prenom() != null) soul.setPrenom(request.prenom());
        if (request.email() != null) soul.setEmail(request.email());
        if (request.telephone() != null) soul.setTelephone(request.telephone());
        if (request.adresse() != null) soul.setAdresse(request.adresse());
        if (request.dateNaissance() != null) soul.setDateNaissance(request.dateNaissance());
        if (request.profession() != null) soul.setProfession(request.profession());
        if (request.typeDisciple() != null) soul.setTypeDisciple(request.typeDisciple());
        if (request.dateIntegration() != null) soul.setDateIntegration(request.dateIntegration());
        if (request.dateConversion() != null) soul.setDateConversion(request.dateConversion());
        if (request.notesPasteur() != null) soul.setNotesPasteur(request.notesPasteur());
        if (request.situationFamiliale() != null) soul.setSituationFamiliale(request.situationFamiliale());
        // US-19: Log spiritual state changes
        if (request.etatSpirituel() != null && !request.etatSpirituel().equals(soul.getEtatSpirituel())) {
            String oldEtat = soul.getEtatSpirituel();
            soul.setEtatSpirituel(request.etatSpirituel());
            logHistory(soul.getId(), "CHANGEMENT_ETAT_SPIRITUEL",
                    "État spirituel: " + oldEtat + " -> " + request.etatSpirituel(),
                    oldEtat, request.etatSpirituel(), null, null);
        } else if (request.etatSpirituel() != null) {
            soul.setEtatSpirituel(request.etatSpirituel());
        }

        if (request.niveauCroissance() != null && !request.niveauCroissance().equals(soul.getNiveauCroissance())) {
            Integer oldNiveau = soul.getNiveauCroissance();
            soul.setNiveauCroissance(request.niveauCroissance());
            logHistory(soul.getId(), "CHANGEMENT_NIVEAU_CROISSANCE",
                    "Niveau de croissance: " + oldNiveau + " -> " + request.niveauCroissance(),
                    null, null, null, null);
        } else if (request.niveauCroissance() != null) {
            soul.setNiveauCroissance(request.niveauCroissance());
        }

        if (request.statut() != null) {
            soul.setStatut(request.statut());
            logHistory(soul.getId(), "CHANGEMENT_STATUT",
                    "Statut changé: " + oldStatut + " -> " + request.statut(),
                    oldStatut, request.statut().name(), null, null);
        }

        if (request.faiseurId() != null && !request.faiseurId().equals(oldFaiseurId)) {
            soul.setFaiseurId(request.faiseurId());
            soul.setFamilleId(request.familleId());
            logHistory(soul.getId(), "REAFFECTATION",
                    "Réaffecté du faiseur " + oldFaiseurId + " au faiseur " + request.faiseurId(),
                    null, null, oldFaiseurId, request.faiseurId());
        }

        return soulRepository.save(soul);
    }

    public void delete(UUID id) {
        Soul soul = findById(id);
        soul.setDeleted(true);
        soulRepository.save(soul);
    }

    // ======================== US-60: RESTORE SOUL ========================

    public Soul restore(UUID id) {
        Soul soul = soulRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Soul", id));
        soul.setDeleted(false);
        soul.setStatut(StatutAme.ACTIF);
        return soulRepository.save(soul);
    }

    public Soul reassign(UUID soulId, UUID newFaiseurId) {
        Soul soul = findById(soulId);
        UUID oldFaiseurId = soul.getFaiseurId();
        soul.setFaiseurId(newFaiseurId);
        soul = soulRepository.save(soul);
        logHistory(soulId, "REAFFECTATION",
                "Âme réaffectée du faiseur " + oldFaiseurId + " au faiseur " + newFaiseurId,
                null, null, oldFaiseurId, newFaiseurId);

        // US-21: Notify both faiseurs
        try {
            String soulName = soul.getNomComplet();
            notificationService.create(
                    newFaiseurId, TypeNotification.INFORMATION, CanalNotification.EMAIL,
                    "Âme réaffectée",
                    "L'âme " + soulName + " vous a été réaffectée pour suivi.",
                    soulId, "SOUL");
            if (oldFaiseurId != null && !oldFaiseurId.equals(newFaiseurId)) {
                notificationService.create(
                        oldFaiseurId, TypeNotification.INFORMATION, CanalNotification.EMAIL,
                        "Âme retirée de votre suivi",
                        "L'âme " + soulName + " a été réaffectée à un autre faiseur.",
                        soulId, "SOUL");
            }
        } catch (Exception e) {
            log.warn("Failed to send reassignment notification: {}", e.getMessage());
        }

        return soul;
    }

    @Transactional(readOnly = true)
    public List<Soul> findByFaiseurId(UUID faiseurId) {
        return soulRepository.findAllByFaiseurId(faiseurId);
    }

    @Transactional(readOnly = true)
    public List<Soul> findByFamilleId(UUID familleId) {
        return soulRepository.findAllByFamilleId(familleId);
    }

    @Transactional(readOnly = true)
    public List<SoulHistoryResponse> getHistory(UUID soulId) {
        return soulHistoryRepository.findByAmeIdOrderByCreatedAtDesc(soulId)
                .stream()
                .map(h -> new SoulHistoryResponse(
                        h.getId(), h.getAmeId(), h.getTypeEvenement(),
                        h.getDescription(), h.getAncienStatut(), h.getNouveauStatut(),
                        h.getUtilisateurId(), h.getCreatedAt()))
                .toList();
    }

    /**
     * US-23: Filter souls by spiritual state, status, faiseur, family
     */
    @Transactional(readOnly = true)
    public Page<Soul> filterSouls(String etatSpirituel, String statut, UUID faiseurId,
                                   UUID familleId, Pageable pageable) {
        if (faiseurId != null) return soulRepository.findByFaiseurId(faiseurId, pageable);
        if (familleId != null) return soulRepository.findByFamilleId(familleId, pageable);
        if (statut != null && etatSpirituel != null) {
            return soulRepository.findByStatutAndEtatSpirituel(StatutAme.valueOf(statut), etatSpirituel, pageable);
        }
        if (statut != null) return soulRepository.findByStatut(StatutAme.valueOf(statut), pageable);
        if (etatSpirituel != null) return soulRepository.findByEtatSpirituel(etatSpirituel, pageable);
        return soulRepository.findAll(pageable);
    }

    /**
     * US-15: Auto-suggest the least loaded faiseur in a family for a new soul
     */
    @Transactional(readOnly = true)
    public UUID suggestLeastLoadedFaiseur(UUID familleId) {
        List<Soul> familySouls = soulRepository.findAllByFamilleId(familleId);
        java.util.Map<UUID, Long> loadByFaiseur = new java.util.HashMap<>();
        for (Soul soul : familySouls) {
            loadByFaiseur.merge(soul.getFaiseurId(), 1L, Long::sum);
        }
        return loadByFaiseur.entrySet().stream()
                .min(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * US-24: Find all souls "en difficulté" across all families (for Pasteur)
     */
    @Transactional(readOnly = true)
    public List<Soul> findAllEnDifficulte() {
        return soulRepository.findAll().stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> "EN_DIFFICULTE".equals(s.getEtatSpirituel())
                        || StatutAme.DECROCHE.equals(s.getStatut())
                        || StatutAme.EN_VEILLE.equals(s.getStatut()))
                .toList();
    }

    private void logHistory(UUID ameId, String typeEvenement, String description,
                            String ancienStatut, String nouveauStatut,
                            UUID ancienFaiseurId, UUID nouveauFaiseurId) {
        SoulHistory history = new SoulHistory();
        history.setAmeId(ameId);
        history.setTypeEvenement(typeEvenement);
        history.setDescription(description);
        history.setAncienStatut(ancienStatut);
        history.setNouveauStatut(nouveauStatut);
        history.setAncienFaiseurId(ancienFaiseurId);
        history.setNouveauFaiseurId(nouveauFaiseurId);
        try {
            history.setUtilisateurId(securityUtils.getCurrentUserId());
        } catch (Exception e) {
            // System operations may not have a user context
        }
        soulHistoryRepository.save(history);
    }
}
