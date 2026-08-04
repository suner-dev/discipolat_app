package com.discipolat.modules.souls.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.common.enums.TypeDisciple;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.modules.evaluations.domain.EvaluationService;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.reports.domain.MakerReportRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SoulService {

    private static final Logger log = LoggerFactory.getLogger(SoulService.class);

    private final SoulRepository soulRepository;
    private final SoulHistoryRepository soulHistoryRepository;
    private final SoulNoteRepository soulNoteRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final MakerReportRepository makerReportRepository;
    private final EvaluationService evaluationService;
    private final NotificationService notificationService;

    public SoulService(SoulRepository soulRepository, SoulHistoryRepository soulHistoryRepository,
                       SoulNoteRepository soulNoteRepository,
                       SecurityUtils securityUtils, UserRepository userRepository,
                       FamilyRepository familyRepository, MakerReportRepository makerReportRepository,
                       EvaluationService evaluationService,
                       NotificationService notificationService) {
        this.soulRepository = soulRepository;
        this.soulHistoryRepository = soulHistoryRepository;
        this.soulNoteRepository = soulNoteRepository;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.makerReportRepository = makerReportRepository;
        this.evaluationService = evaluationService;
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
                              StatutAme statut, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) return soulRepository.search(search.trim(), pageable);
        if (faiseurId != null) return soulRepository.findByFaiseurId(faiseurId, pageable);
        if (familleId != null) return soulRepository.findByFamilleId(familleId, pageable);
        if (typeDisciple != null && statut != null)
            return soulRepository.findByTypeDiscipleAndStatut(typeDisciple, statut, pageable);
        if (typeDisciple != null) return soulRepository.findByTypeDisciple(typeDisciple, pageable);
        if (statut != null) return soulRepository.findByStatut(statut, pageable);
        return soulRepository.findAll(pageable);
    }

    /** Corbeille : toutes les âmes soft-deleted, pour restauration. */
    @Transactional(readOnly = true)
    public Page<Soul> findTrash(Pageable pageable) {
        return soulRepository.findByDeletedTrue(pageable);
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

    // ========================================================================
    // PHASE 3: DOSSIER PASTORAL 360°
    // ========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getPastoral360(UUID soulId) {
        Soul soul = findById(soulId);
        Map<String, Object> dossier = new java.util.LinkedHashMap<>();

        // 1. INFORMATIONS PERSONNELLES
        Map<String, Object> infos = new java.util.LinkedHashMap<>();
        infos.put("id", soul.getId());
        infos.put("nom", soul.getNom());
        infos.put("prenom", soul.getPrenom());
        infos.put("email", soul.getEmail());
        infos.put("telephone", soul.getTelephone());
        infos.put("adresse", soul.getAdresse());
        infos.put("dateNaissance", soul.getDateNaissance() != null ? soul.getDateNaissance().toString() : null);
        infos.put("profession", soul.getProfession());
        infos.put("situationFamiliale", soul.getSituationFamiliale());
        infos.put("photoUrl", null);
        dossier.put("informations", infos);

        // 2. PARCOURS SPIRITUEL
        Map<String, Object> spirituel = new java.util.LinkedHashMap<>();
        spirituel.put("typeDisciple", soul.getTypeDisciple().name());
        spirituel.put("statut", soul.getStatut().name());
        spirituel.put("etatSpirituel", soul.getEtatSpirituel());
        spirituel.put("niveauCroissance", soul.getNiveauCroissance());
        spirituel.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
        spirituel.put("dateConversion", soul.getDateConversion() != null ? soul.getDateConversion().toString() : null);
        spirituel.put("dateDernierContact", soul.getDateDernierContact() != null ? soul.getDateDernierContact().toString() : null);

        // 3. INDICES INTELLIGENTS
        Map<String, Object> indices = new java.util.LinkedHashMap<>();
        // Indice de santé spirituelle (basé sur etatSpirituel, niveauCroissance, statut)
        int sante = calculateSanteIndex(soul);
        indices.put("santeSpirituelle", sante);
        // Indice de fidélité (basé sur présences aux rapports)
        int fidelite = calculateFideliteIndex(soul);
        indices.put("fidelite", fidelite);
        // Indice d'engagement (basé sur participation, type disciple, durée)
        int engagement = calculateEngagementIndex(soul);
        indices.put("engagement", engagement);
        // Indice de participation (basé sur rapports soumis)
        int participation = calculateParticipationIndex(soul);
        indices.put("participation", participation);
        // Indice global
        int global = Math.round((float)(sante + fidelite + engagement + participation) / 4);
        indices.put("global", global);
        dossier.put("indices", indices);

        // Alertes automatiques
        List<Map<String, Object>> alertesAuto = new java.util.ArrayList<>();
        if (soul.getStatut() == StatutAme.DECROCHE) {
            alertesAuto.add(Map.of("type", "INACTIF", "message", "Membre inactif (décroché)", "priorite", "HAUTE"));
        } else if (soul.getStatut() == StatutAme.EN_VEILLE) {
            alertesAuto.add(Map.of("type", "VEILLE", "message", "Membre en veille", "priorite", "MOYENNE"));
        }
        if (soul.getDateDernierContact() != null
                && soul.getDateDernierContact().plusDays(30).isBefore(java.time.LocalDateTime.now())) {
            alertesAuto.add(Map.of("type", "ABSENCE_CONTACT", "message", "Aucun contact depuis plus de 30 jours", "priorite", "MOYENNE"));
        }
        if ("EN_DIFFICULTE".equals(soul.getEtatSpirituel())) {
            alertesAuto.add(Map.of("type", "DIFFICULTE", "message", "Membre en difficulté spirituelle", "priorite", "HAUTE"));
        }
        dossier.put("alertesAutomatiques", alertesAuto);

        // 4. ENCADREMENT
        Map<String, Object> encadrement = new java.util.LinkedHashMap<>();
        encadrement.put("faiseurId", soul.getFaiseurId());
        encadrement.put("familleId", soul.getFamilleId());
        userRepository.findById(soul.getFaiseurId()).ifPresent(f ->
            encadrement.put("faiseurNom", f.getFirstName() + " " + f.getLastName()));
        dossier.put("encadrement", encadrement);

        // 5. HISTORIQUE COMPLET (timeline)
        List<SoulHistory> allHistory = soulHistoryRepository.findByAmeIdOrderByCreatedAtDesc(soulId);
        List<Map<String, Object>> timeline = new java.util.ArrayList<>();
        for (SoulHistory h : allHistory) {
            Map<String, Object> entry = new java.util.LinkedHashMap<>();
            entry.put("id", h.getId());
            entry.put("type", h.getTypeEvenement());
            entry.put("description", h.getDescription());
            entry.put("ancienStatut", h.getAncienStatut());
            entry.put("nouveauStatut", h.getNouveauStatut());
            entry.put("utilisateurId", h.getUtilisateurId());
            entry.put("date", h.getCreatedAt().toString());
            timeline.add(entry);
        }
        dossier.put("timeline", timeline);

        // 6. ÉVALUATIONS du faiseur
        if (soul.getFaiseurId() != null) {
            try {
                Map<String, Object> evalScores = evaluationService.getUserEvalScores(soul.getFaiseurId());
                dossier.put("evaluations", evalScores);
            } catch (Exception e) {
                dossier.put("evaluations", Map.of());
            }
        }

        // 7. NOTES PRIVÉES
        List<SoulNote> notes = soulNoteRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(soulId);
        dossier.put("notes", notes.stream().map(n -> Map.<String, Object>of(
            "id", n.getId(), "contenu", n.getContenu(),
            "auteurId", n.getAuteurId(), "date", n.getCreatedAt().toString()
        )).toList());

        spirituel.put("indices", indices);
        dossier.put("spirituel", spirituel);

        return dossier;
    }

    private int calculateSanteIndex(Soul soul) {
        int score = 50; // base
        if (soul.getStatut() == StatutAme.ACTIF) score += 30;
        else if (soul.getStatut() == StatutAme.EN_INTEGRATION) score += 15;
        else if (soul.getStatut() == StatutAme.EN_VEILLE) score -= 10;
        else if (soul.getStatut() == StatutAme.DECROCHE) score -= 30;

        if ("MATURE".equals(soul.getEtatSpirituel())) score += 20;
        else if ("CROISSANCE".equals(soul.getEtatSpirituel())) score += 10;
        else if ("EN_DIFFICULTE".equals(soul.getEtatSpirituel())) score -= 20;

        if (soul.getNiveauCroissance() >= 4) score += 10;
        else if (soul.getNiveauCroissance() >= 2) score += 5;

        return Math.max(0, Math.min(100, score));
    }

    private int calculateFideliteIndex(Soul soul) {
        int score = 50;
        // Check attendances in recent reports
        var currentWeek = java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        var reports = makerReportRepository.findByAmeIdAndSemaine(soul.getId(), currentWeek);
        if (!reports.isEmpty()) {
            for (var r : reports) {
                if (r.getPresencesParCulte() != null) {
                    for (var p : r.getPresencesParCulte().values()) {
                        if (p) score += 10;
                    }
                }
            }
        }
        if (soul.getDateDernierContact() != null
                && soul.getDateDernierContact().plusDays(7).isAfter(java.time.LocalDateTime.now())) {
            score += 15; // contacted recently
        }
        return Math.max(0, Math.min(100, score));
    }

    private int calculateEngagementIndex(Soul soul) {
        int score = 50;
        if (soul.getNiveauCroissance() >= 3) score += 20;
        else if (soul.getNiveauCroissance() >= 2) score += 10;

        var reports = makerReportRepository.findByAmeIdAndSemaine(
                soul.getId(), java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY));
        if (!reports.isEmpty() && reports.get(0).isSoumis()) score += 15;

        return Math.max(0, Math.min(100, score));
    }

    private int calculateParticipationIndex(Soul soul) {
        int score = 50;
        var allReports = makerReportRepository.findByAmeIdAndSemaine(
                soul.getId(), java.time.LocalDate.now().with(java.time.DayOfWeek.MONDAY));
        if (!allReports.isEmpty()) {
            if (allReports.get(0).isSoumis()) score += 25;
            if (allReports.get(0).getPresencesParCulte() != null) {
                long presents = allReports.get(0).getPresencesParCulte().values().stream().filter(b -> b).count();
                long total = allReports.get(0).getPresencesParCulte().size();
                if (total > 0) score += (int) (presents * 25 / total);
            }
        }
        return Math.max(0, Math.min(100, score));
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
