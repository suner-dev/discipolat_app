package com.discipolat.modules.search.domain;

import com.discipolat.common.domain.UserRole;
import com.discipolat.common.enums.StatutAme;
import com.discipolat.modules.discipline.domain.SoulDisciplineEventRepository;
import com.discipolat.modules.evaluations.domain.EvaluationService;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.StatutSuiviParallele;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowup;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.prayers.domain.Prayer;
import com.discipolat.modules.prayers.domain.PrayerRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulExitRepository;
import com.discipolat.modules.souls.domain.SoulHistoryRepository;
import com.discipolat.modules.souls.domain.SoulNote;
import com.discipolat.modules.souls.domain.SoulNoteRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SearchService {

    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final MakerReportRepository makerReportRepository;
    private final PrayerRepository prayerRepository;
    private final SoulNoteRepository soulNoteRepository;
    private final SoulHistoryRepository soulHistoryRepository;
    private final SoulExitRepository soulExitRepository;
    private final AlertRepository alertRepository;
    private final ParallelFollowupRepository parallelFollowupRepository;
    private final EvaluationService evaluationService;
    private final SoulDisciplineEventRepository disciplineEventRepository;
    private final SecurityUtils securityUtils;

    public SearchService(SoulRepository soulRepository, UserRepository userRepository,
                         FamilyRepository familyRepository, DepartmentRepository departmentRepository,
                         MakerReportRepository makerReportRepository, PrayerRepository prayerRepository,
                         SoulNoteRepository soulNoteRepository, SoulHistoryRepository soulHistoryRepository,
                         SoulExitRepository soulExitRepository, AlertRepository alertRepository,
                         ParallelFollowupRepository parallelFollowupRepository,
                         EvaluationService evaluationService,
                         SoulDisciplineEventRepository disciplineEventRepository,
                         SecurityUtils securityUtils) {
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.makerReportRepository = makerReportRepository;
        this.prayerRepository = prayerRepository;
        this.soulNoteRepository = soulNoteRepository;
        this.soulHistoryRepository = soulHistoryRepository;
        this.soulExitRepository = soulExitRepository;
        this.alertRepository = alertRepository;
        this.parallelFollowupRepository = parallelFollowupRepository;
        this.evaluationService = evaluationService;
        this.disciplineEventRepository = disciplineEventRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Search for souls across the system with role-based filtering.
     * - PASTEUR/ADMIN: sees all souls
     * - RESPONSABLE: sees souls in their departments
     * - Chef de famille (FAISEUR with estChefDeFamille): sees souls in their family
     * - FAISEUR: sees only their assigned souls
     */
    public Page<Map<String, Object>> search(String query, Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Set<UUID> accessibleSoulIds = getAccessibleSoulIds(currentUser);
        List<Soul> allSouls;

        if (accessibleSoulIds != null) {
            // Load only accessible souls and filter by query
            allSouls = soulRepository.findAllById(accessibleSoulIds).stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesQuery(s, query))
                    .sorted((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()))
                    .toList();
        } else {
            // PASTEUR/ADMIN: search all souls
            allSouls = soulRepository.findAll().stream()
                    .filter(s -> !s.isDeleted())
                    .filter(s -> matchesQuery(s, query))
                    .sorted((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()))
                    .toList();
        }

        // Also search users (faiseurs, chefs, responsables) if pasteur
        List<Map<String, Object>> userResults = new ArrayList<>();
        if (currentUser.getRoles().contains(UserRole.PASTEUR) || currentUser.getRoles().contains(UserRole.ADMIN)) {
            userRepository.findAll().stream()
                    .filter(u -> !u.isDeleted())
                    .filter(u -> matchesUserQuery(u, query))
                    .forEach(u -> {
                        Map<String, Object> userEntry = new LinkedHashMap<>();
                        userEntry.put("type", "UTILISATEUR");
                        userEntry.put("id", u.getId());
                        userEntry.put("nom", u.getFirstName() + " " + u.getLastName());
                        userEntry.put("email", u.getEmail());
                        userEntry.put("role", u.getActiveRole() != null ? u.getActiveRole().name() : u.getRole().name());
                        userEntry.put("estChefDeFamille", u.isEstChefDeFamille());
                        userEntry.put("familleGereeId", u.getFamilleGereeId());
                        userResults.add(userEntry);
                    });
        }

        // Convert soul results
        List<Map<String, Object>> soulResults = allSouls.stream()
                .map(this::soulToSearchResult)
                .toList();

        // Combine results - souls first, then users
        List<Map<String, Object>> combined = new ArrayList<>();
        combined.addAll(soulResults);
        combined.addAll(userResults);

        // Manual pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combined.size());
        List<Map<String, Object>> pageContent = start < combined.size() ? combined.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, combined.size());
    }

    /**
     * Get the complete profile for a specific soul (all related data in one view).
     * Role-based: user can only access souls they have permission to see.
     */
    public Map<String, Object> getCompleteProfile(UUID soulId) {
        Soul soul = soulRepository.findById(soulId)
                .orElseThrow(() -> new RuntimeException("Soul not found: " + soulId));

        // Check access
        UUID currentUserId = securityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        checkSoulAccess(soul, currentUser);

        Map<String, Object> profile = new LinkedHashMap<>();

        // ===== 1. Personal information =====
        Map<String, Object> personalInfo = new LinkedHashMap<>();
        personalInfo.put("id", soul.getId());
        personalInfo.put("nom", soul.getNom());
        personalInfo.put("prenom", soul.getPrenom());
        personalInfo.put("nomComplet", soul.getNomComplet());
        personalInfo.put("email", soul.getEmail());
        personalInfo.put("telephone", soul.getTelephone());
        personalInfo.put("adresse", soul.getAdresse());
        personalInfo.put("dateNaissance", soul.getDateNaissance());
        if (soul.getDateNaissance() != null) {
            personalInfo.put("age", Period.between(soul.getDateNaissance(), LocalDate.now()).getYears());
        }
        personalInfo.put("profession", soul.getProfession());
        personalInfo.put("situationFamiliale", soul.getSituationFamiliale());
        profile.put("informationsPersonnelles", personalInfo);

        // ===== 2. Church information =====
        Map<String, Object> churchInfo = new LinkedHashMap<>();
        churchInfo.put("dateIntegration", soul.getDateIntegration());
        churchInfo.put("dateConversion", soul.getDateConversion());
        churchInfo.put("typeDisciple", soul.getTypeDisciple().name());
        churchInfo.put("statut", soul.getStatut().name());
        churchInfo.put("etatSpirituel", soul.getEtatSpirituel());
        churchInfo.put("niveauCroissance", soul.getNiveauCroissance());
        churchInfo.put("dateDernierContact", soul.getDateDernierContact());
        churchInfo.put("anneesDansEglise", soul.getDateIntegration() != null
                ? Period.between(soul.getDateIntegration(), LocalDate.now()).getYears() : 0);
        profile.put("informationsEcclesiales", churchInfo);

        // ===== 3. Assignment chain =====
        Map<String, Object> assignments = new LinkedHashMap<>();
        assignments.put("faiseurId", soul.getFaiseurId());
        userRepository.findById(soul.getFaiseurId()).ifPresent(f -> {
            assignments.put("faiseurNom", f.getFirstName() + " " + f.getLastName());
            assignments.put("faiseurEmail", f.getEmail());
        });
        assignments.put("familleId", soul.getFamilleId());
        if (soul.getFamilleId() != null) {
            familyRepository.findById(soul.getFamilleId()).ifPresent(fam -> {
                assignments.put("familleNom", fam.getNom());
                // Get chef de famille
                userRepository.findById(fam.getChefFamilleId()).ifPresent(chef -> {
                    assignments.put("chefFamilleId", chef.getId());
                    assignments.put("chefFamilleNom", chef.getFirstName() + " " + chef.getLastName());
                });
                // Get department(s) via soul_departments
            });
        }
        // Disciples suivis by this soul (if they are a faiseur)
        List<Soul> disciplesSuivis = soulRepository.findAllByFaiseurId(soul.getId());
        if (!disciplesSuivis.isEmpty()) {
            assignments.put("disciplesSuivis", disciplesSuivis.stream()
                    .filter(d -> !d.isDeleted())
                    .map(d -> Map.of("id", d.getId(), "nom", d.getNomComplet(), "statut", d.getStatut().name()))
                    .toList());
            assignments.put("nombreDisciplesSuivis", disciplesSuivis.size());
        }
        profile.put("assignations", assignments);

        // ===== 4. Presence history (from maker reports) =====
        List<Map<String, Object>> presenceHistory = new ArrayList<>();
        Page<MakerReport> reports = makerReportRepository.findByAmeId(soul.getId(), PageRequest.of(0, 52));
        for (MakerReport report : reports.getContent()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", report.getId());
            entry.put("semaine", report.getSemaine());
            entry.put("presencesParCulte", report.getPresencesParCulte());
            entry.put("absenceRaison", report.getAbsenceRaison() != null ? report.getAbsenceRaison().name() : null);
            entry.put("absenceCommentaire", report.getAbsenceCommentaire());
            entry.put("soumis", report.isSoumis());
            entry.put("dateSoumission", report.getDateSoumission());
            presenceHistory.add(entry);
        }

        // Compute stats
        long totalReports = presenceHistory.size();
        long soumis = presenceHistory.stream().filter(r -> (boolean) r.get("soumis")).count();
        int totalPresents = 0;
        int totalPossible = 0;
        long absencesWithReason = 0;
        for (Map<String, Object> r : presenceHistory) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> presences = (Map<String, Boolean>) r.get("presencesParCulte");
            if (presences != null) {
                for (Boolean p : presences.values()) {
                    totalPossible++;
                    if (p) totalPresents++;
                }
            }
            if (r.get("absenceRaison") != null) absencesWithReason++;
        }

        Map<String, Object> presenceStats = new LinkedHashMap<>();
        presenceStats.put("totalRapports", totalReports);
        presenceStats.put("rapportsSoumis", soumis);
        presenceStats.put("tauxSoumission", totalReports > 0 ? Math.round((double) soumis / totalReports * 1000.0) / 10.0 : 0.0);
        presenceStats.put("totalPresences", totalPresents);
        presenceStats.put("totalAbsences", totalPossible - totalPresents);
        presenceStats.put("tauxPresence", totalPossible > 0 ? Math.round((double) totalPresents / totalPossible * 1000.0) / 10.0 : 0.0);
        presenceStats.put("absencesJustifiees", absencesWithReason);
        presenceStats.put("historique", presenceHistory);
        profile.put("presences", presenceStats);

        // ===== 5. Full history (soul_history) =====
        List<Map<String, Object>> fullHistory = soulHistoryRepository.findByAmeIdOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(h -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", h.getId());
                    entry.put("typeEvenement", h.getTypeEvenement());
                    entry.put("description", h.getDescription());
                    entry.put("ancienStatut", h.getAncienStatut());
                    entry.put("nouveauStatut", h.getNouveauStatut());
                    entry.put("ancienFaiseurId", h.getAncienFaiseurId());
                    entry.put("nouveauFaiseurId", h.getNouveauFaiseurId());
                    entry.put("utilisateurId", h.getUtilisateurId());
                    entry.put("date", h.getCreatedAt());
                    return entry;
                })
                .toList();
        profile.put("historiqueComplet", fullHistory);

        // ===== 6. Notes =====
        List<Map<String, Object>> notes = soulNoteRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(n -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", n.getId());
                    entry.put("contenu", n.getContenu());
                    entry.put("auteurId", n.getAuteurId());
                    entry.put("date", n.getCreatedAt());
                    return entry;
                })
                .toList();
        profile.put("notes", notes);

        // ===== 7. Prayer requests =====
        List<Map<String, Object>> prayers = prayerRepository.findByAmeIdAndDeletedFalse(soul.getId())
                .stream()
                .map(p -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", p.getId());
                    entry.put("titre", p.getTitre());
                    entry.put("description", p.getDescription());
                    entry.put("categorie", p.getCategorie());
                    entry.put("priorite", p.getPriorite());
                    entry.put("statut", p.getStatut());
                    entry.put("temoignage", p.getTemoignage());
                    entry.put("dateCreation", p.getCreatedAt());
                    entry.put("dateExaucee", p.getDateExaucee());
                    return entry;
                })
                .toList();
        profile.put("demandesPriere", prayers);

        // ===== 8. Parallel followups =====
        List<Map<String, Object>> suivisParalleles = parallelFollowupRepository.findByAmeId(soul.getId())
                .stream()
                .map(sp -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", sp.getId());
                    entry.put("raison", sp.getRaison().name());
                    entry.put("raisonDetail", sp.getRaisonDetail());
                    entry.put("dateDebut", sp.getDateDebut());
                    entry.put("dateFin", sp.getDateFin());
                    entry.put("statut", sp.getStatut().name());
                    entry.put("initiateurId", sp.getInitiateurId());
                    return entry;
                })
                .toList();
        profile.put("suivisParalleles", suivisParalleles);

        // ===== 9. Alerts =====
        List<Map<String, Object>> alerts = alertRepository.findByAmeIdAndStatut(soul.getId(), StatutAlerte.ACTIVE)
                .stream()
                .map(a -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", a.getId());
                    entry.put("typeAlerte", a.getTypeAlerte());
                    entry.put("message", a.getMessage());
                    entry.put("dateDeclenchement", a.getDateDeclenchement());
                    entry.put("statut", a.getStatut().name());
                    return entry;
                })
                .toList();
        profile.put("alertes", alerts);

        // ===== 10. Exits history =====
        List<Map<String, Object>> exits = soulExitRepository.findByAmeIdOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(e -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", e.getId());
                    entry.put("motif", e.getMotif());
                    entry.put("motifDetail", e.getMotifDetail());
                    entry.put("dateSortie", e.getDateSortie());
                    entry.put("peutReintegrer", e.isPeutReintegrer());
                    return entry;
                })
                .toList();
        profile.put("sorties", exits);

        // ===== 11. Evaluation scores for the assignment chain =====
        Map<String, Object> evalScores = new LinkedHashMap<>();
        // Faiseur evaluation
        if (soul.getFaiseurId() != null) {
            Map<String, Object> faiseurEval = buildUserEvalScores(soul.getFaiseurId());
            if (!faiseurEval.isEmpty()) evalScores.put("faiseur", faiseurEval);
        }
        // Chef de famille evaluation
        if (soul.getFamilleId() != null) {
            familyRepository.findById(soul.getFamilleId()).ifPresent(fam -> {
                Map<String, Object> chefEval = buildUserEvalScores(fam.getChefFamilleId());
                if (!chefEval.isEmpty()) evalScores.put("chefFamille", chefEval);
                // Responsable evaluation (skipped - departments independent from families)
            });
        }
        if (!evalScores.isEmpty()) profile.put("evaluations", evalScores);

        // ===== 12. Discipline events (Phase 7) =====
        List<Map<String, Object>> disciplineEvents = disciplineEventRepository
                .findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(soul.getId())
                .stream()
                .map(d -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", d.getId());
                    entry.put("categorie", d.getCategorie().name());
                    entry.put("typeEvenement", d.getTypeEvenement());
                    entry.put("gravite", d.getGravite() != null ? d.getGravite().name() : null);
                    entry.put("titre", d.getTitre());
                    entry.put("description", d.getDescription());
                    entry.put("dateEvenement", d.getDateEvenement().toString());
                    entry.put("resolu", d.isResolu());
                    entry.put("dateResolution", d.getDateResolution() != null ? d.getDateResolution().toString() : null);
                    entry.put("auteurId", d.getAuteurId());
                    entry.put("createdAt", d.getCreatedAt().toString());
                    return entry;
                })
                .toList();
        profile.put("evenementsDisciplinaires", disciplineEvents);

        // Discipline stats
        long nonResolus = disciplineEvents.stream().filter(e -> !(boolean) e.get("resolu")).count();
        Map<String, Object> disciplineStats = new LinkedHashMap<>();
        disciplineStats.put("total", (long) disciplineEvents.size());
        disciplineStats.put("nonResolus", nonResolus);
        profile.put("statistiquesDisciplinaires", disciplineStats);

        return profile;
    }

    /** Build evaluation score summary for a user across all categories (delegates to cached EvaluationService) */
    private Map<String, Object> buildUserEvalScores(UUID userId) {
        return evaluationService.getUserEvalScores(userId);
    }

    // ======================== HELPERS ========================

    private boolean matchesQuery(Soul soul, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        return (soul.getNom() != null && soul.getNom().toLowerCase().contains(q))
                || (soul.getPrenom() != null && soul.getPrenom().toLowerCase().contains(q))
                || (soul.getEmail() != null && soul.getEmail().toLowerCase().contains(q))
                || (soul.getTelephone() != null && soul.getTelephone().contains(q))
                || (soul.getProfession() != null && soul.getProfession().toLowerCase().contains(q))
                || (soul.getAdresse() != null && soul.getAdresse().toLowerCase().contains(q))
                || (soul.getNomComplet() != null && soul.getNomComplet().toLowerCase().contains(q));
    }

    private boolean matchesUserQuery(User user, String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.toLowerCase().trim();
        String fullName = (user.getFirstName() != null ? user.getFirstName() : "")
                + " " + (user.getLastName() != null ? user.getLastName() : "");
        return (user.getEmail() != null && user.getEmail().toLowerCase().contains(q))
                || (user.getFirstName() != null && user.getFirstName().toLowerCase().contains(q))
                || (user.getLastName() != null && user.getLastName().toLowerCase().contains(q))
                || (fullName.toLowerCase().contains(q));
    }

    private Map<String, Object> soulToSearchResult(Soul soul) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "AME");
        result.put("id", soul.getId());
        result.put("nom", soul.getNom());
        result.put("prenom", soul.getPrenom());
        result.put("nomComplet", soul.getNomComplet());
        result.put("email", soul.getEmail());
        result.put("telephone", soul.getTelephone());
        result.put("statut", soul.getStatut().name());
        result.put("typeDisciple", soul.getTypeDisciple().name());
        result.put("etatSpirituel", soul.getEtatSpirituel());

        // Get faiseur name
        userRepository.findById(soul.getFaiseurId()).ifPresent(f ->
                result.put("faiseurNom", f.getFirstName() + " " + f.getLastName()));

        // Get family name
        if (soul.getFamilleId() != null) {
            familyRepository.findById(soul.getFamilleId()).ifPresent(fam ->
                    result.put("familleNom", fam.getNom()));
        }

        result.put("dateIntegration", soul.getDateIntegration());
        result.put("dateDernierContact", soul.getDateDernierContact());

        // Years in church
        if (soul.getDateIntegration() != null) {
            result.put("anneesDansEglise", Period.between(soul.getDateIntegration(), LocalDate.now()).getYears());
        }

        return result;
    }

    /**
     * Returns the set of soul IDs accessible to the current user, or null if all souls are accessible.
     */
    private Set<UUID> getAccessibleSoulIds(User currentUser) {
        if (currentUser.getRoles().contains(UserRole.PASTEUR) || currentUser.getRoles().contains(UserRole.ADMIN)) {
            return null; // All souls
        }

        Set<UUID> accessibleIds = new HashSet<>();

        if (currentUser.getRoles().contains(UserRole.RESPONSABLE)) {
            // Get all departments managed by this responsable
            List<Department> departments = departmentRepository.findByResponsableId(currentUser.getId());
            for (Department dept : departments) {
                List<Family> families = familyRepository.findAll();
                for (Family fam : families) {
                    List<Soul> souls = soulRepository.findAllByFamilleId(fam.getId());
                    souls.stream().map(Soul::getId).forEach(accessibleIds::add);
                }
            }
        }

        // Chef de famille: their family's souls
        if (currentUser.isEstChefDeFamille() && currentUser.getFamilleGereeId() != null) {
            List<Soul> familySouls = soulRepository.findAllByFamilleId(currentUser.getFamilleGereeId());
            familySouls.stream().map(Soul::getId).forEach(accessibleIds::add);
        }

        // FAISEUR: their own souls (if they have the FAISEUR role)
        if (currentUser.getRoles().contains(UserRole.FAISEUR)) {
            List<Soul> mySouls = soulRepository.findAllByFaiseurId(currentUser.getId());
            mySouls.stream().map(Soul::getId).forEach(accessibleIds::add);
        }

        return accessibleIds;
    }

    private void checkSoulAccess(Soul soul, User currentUser) {
        Set<UUID> accessibleIds = getAccessibleSoulIds(currentUser);
        if (accessibleIds != null && !accessibleIds.contains(soul.getId())) {
            throw new RuntimeException("Access denied to soul: " + soul.getId());
        }
    }
}
