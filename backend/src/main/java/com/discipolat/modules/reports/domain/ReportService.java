package com.discipolat.modules.reports.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutValidation;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.reports.api.SubmitFamilyReportRequest;
import com.discipolat.modules.reports.api.SubmitMakerReportRequest;
import com.discipolat.modules.files.domain.EntityAttachment;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.souls.domain.WorkspaceScopeService;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.discipolat.common.enums.StatutAme;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ReportService {

    private final MakerReportRepository makerReportRepository;
    private final FamilyReportRepository familyReportRepository;
    private final SecurityUtils securityUtils;
    private final WorkspaceScopeService workspaceScope;
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final ParallelFollowupRepository parallelFollowupRepository;
    private final EntityAttachmentService attachmentService;

    public ReportService(MakerReportRepository makerReportRepository,
                         FamilyReportRepository familyReportRepository,
                         SecurityUtils securityUtils,
                         WorkspaceScopeService workspaceScope,
                         SoulRepository soulRepository,
                         UserRepository userRepository,
                         ParallelFollowupRepository parallelFollowupRepository,
                         EntityAttachmentService attachmentService) {
        this.makerReportRepository = makerReportRepository;
        this.familyReportRepository = familyReportRepository;
        this.securityUtils = securityUtils;
        this.workspaceScope = workspaceScope;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.parallelFollowupRepository = parallelFollowupRepository;
        this.attachmentService = attachmentService;
    }

    /**
     * Submit a maker report (RG-03: once submitted, locked and non-modifiable)
     */
    public MakerReport submitMakerReport(SubmitMakerReportRequest request) {
        verifyCanReportFor(request.faiseurId());
        Optional<MakerReport> existing = makerReportRepository
                .findByFaiseurIdAndAmeIdAndSemaine(request.faiseurId(), request.ameId(), request.semaine());

        MakerReport report;
        if (existing.isPresent()) {
            report = existing.get();
            if (report.isSoumis()) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "REPORT_LOCKED", "Cannot modify a report that has already been submitted");
            }
        } else {
            report = new MakerReport();
            report.setFaiseurId(request.faiseurId());
            report.setAmeId(request.ameId());
            report.setSemaine(request.semaine());
        }

        report.setPresencesParCulte(request.presencesParCulte());
        report.setAbsencesMulti(request.absencesMulti());
        report.setAbsenceRaison(request.absenceRaison());
        report.setAbsenceCommentaire(request.absenceCommentaire());
        report.setDifficultesCategorie(request.difficultesCategorie());
        report.setDifficultes(request.difficultes());
        report.setNbSorties(request.nbSorties() != null ? request.nbSorties() : 0);
        report.setMotifSortie(request.motifSortie());
        report.setNbMaintenus(request.nbMaintenus() != null ? request.nbMaintenus() : 0);
        report.setNbInvitesCulte(request.nbInvitesCulte() != null ? request.nbInvitesCulte() : 0);
        report.setVieFaiseurChallenges(request.vieFaiseurChallenges());
        report.setVieFaiseurDemandesAide(request.vieFaiseurDemandesAide());
        report.setVieFaiseurSuggestions(request.vieFaiseurSuggestions());
        report.setNotesComplementaires(request.notesComplementaires());
        report.setSoumis(true);
        report.setDateSoumission(LocalDateTime.now());

        MakerReport saved = makerReportRepository.save(report);
        attachmentService.replace(EntityAttachment.EntityType.MAKER_REPORT, saved.getId(), request.fichierIds());
        return saved;
    }

    /**
     * US-29: Save a maker report as draft (not submitted)
     */
    public MakerReport saveDraft(SubmitMakerReportRequest request) {
        verifyCanReportFor(request.faiseurId());
        Optional<MakerReport> existing = makerReportRepository
                .findByFaiseurIdAndAmeIdAndSemaine(request.faiseurId(), request.ameId(), request.semaine());

        MakerReport report;
        if (existing.isPresent()) {
            report = existing.get();
            if (report.isSoumis()) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "REPORT_LOCKED", "Cannot modify a submitted report");
            }
        } else {
            report = new MakerReport();
            report.setFaiseurId(request.faiseurId());
            report.setAmeId(request.ameId());
            report.setSemaine(request.semaine());
        }

        report.setPresencesParCulte(request.presencesParCulte());
        report.setAbsencesMulti(request.absencesMulti());
        report.setAbsenceRaison(request.absenceRaison());
        report.setAbsenceCommentaire(request.absenceCommentaire());
        report.setDifficultesCategorie(request.difficultesCategorie());
        report.setDifficultes(request.difficultes());
        report.setNbSorties(request.nbSorties() != null ? request.nbSorties() : 0);
        report.setMotifSortie(request.motifSortie());
        report.setNbMaintenus(request.nbMaintenus() != null ? request.nbMaintenus() : 0);
        report.setNbInvitesCulte(request.nbInvitesCulte() != null ? request.nbInvitesCulte() : 0);
        report.setVieFaiseurChallenges(request.vieFaiseurChallenges());
        report.setVieFaiseurDemandesAide(request.vieFaiseurDemandesAide());
        report.setVieFaiseurSuggestions(request.vieFaiseurSuggestions());
        report.setNotesComplementaires(request.notesComplementaires());
        report.setSoumis(false);

        MakerReport saved = makerReportRepository.save(report);
        attachmentService.replace(EntityAttachment.EntityType.MAKER_REPORT, saved.getId(), request.fichierIds());
        return saved;
    }

    public MakerReport findMakerReportById(UUID id) {
        MakerReport report = makerReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MakerReport", id));
        // Espace métier : un rôle opérationnel ne lit que les rapports de son espace
        // (les siens, ceux des faiseurs de sa famille / de ses départements).
        if (!workspaceScope.isSuperUser()) {
            UUID userId = securityUtils.getCurrentUserId();
            boolean isOwn = report.getFaiseurId() != null && report.getFaiseurId().equals(userId);
            if (!isOwn && !workspaceScope.canAccessFaiseur(report.getFaiseurId())
                    && !workspaceScope.canAccessSoul(report.getAmeId())) {
                throw new AccessDeniedException(
                        "Accès refusé à ce rapport dans l'espace métier courant");
            }
        }
        return report;
    }

    public MakerReport saveMakerReport(MakerReport report) {
        return makerReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public Page<MakerReport> findMakerReports(UUID faiseurId, UUID familleId, UUID ameId,
                                              LocalDate semaine, Pageable pageable) {
        // Super-utilisateurs : filtres DB explicites sur tous les rapports
        if (workspaceScope.isSuperUser()) {
            if (faiseurId != null && semaine != null)
                return makerReportRepository.findByFaiseurId(faiseurId, pageable);
            if (ameId != null) return makerReportRepository.findByAmeId(ameId, pageable);
            if (semaine != null) return makerReportRepository.findBySemaine(semaine, pageable);
            return makerReportRepository.findAll(pageable);
        }
        // Non super-utilisateur : les filtres (faiseurId/ameId) ne sont PAS des
        // ancres de confiance — intersection avec l'espace métier du rôle actif.
        List<MakerReport> candidates;
        if (faiseurId != null) candidates = makerReportRepository.findByFaiseurId(faiseurId, pageable).getContent();
        else if (ameId != null) candidates = makerReportRepository.findByAmeId(ameId, pageable).getContent();
        else if (semaine != null) candidates = makerReportRepository.findBySemaine(semaine, pageable).getContent();
        else candidates = makerReportRepository.findAll(pageable).getContent();

        Set<UUID> visibleFaiseurs = workspaceScope.accessibleFaiseurIds();
        Set<UUID> visibleSouls = workspaceScope.accessibleSoulIds();
        List<MakerReport> scoped = candidates.stream()
                .filter(r -> visibleSouls.contains(r.getAmeId()) || visibleFaiseurs.contains(r.getFaiseurId()))
                .toList();
        return paginate(scoped, pageable);
    }

    public FamilyReport submitFamilyReport(SubmitFamilyReportRequest request) {
        // Espace métier : on ne soumet un rapport que pour une famille visible.
        if (!workspaceScope.isSuperUser() && !workspaceScope.canAccessFamily(request.familleId())) {
            throw new AccessDeniedException(
                    "Accès refusé : cette famille n'appartient pas à votre espace métier");
        }
        // Anti-usurpation : un rôle opérationnel ne peut déclarer que SON propre chef
        // de famille (le chef de la famille gérée est l'utilisateur courant). Les
        // super-utilisateurs conservent le champ fourni (soumission pour le compte du chef).
        if (!workspaceScope.isSuperUser()
                && !request.chefFamilleId().equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException(
                    "Vous ne pouvez soumettre un rapport de famille qu'en votre propre nom");
        }
        List<FamilyReport> existingReports = familyReportRepository.findByFamilleIdAndSemaine(
                request.familleId(), request.semaine());
        FamilyReport report;
        if (!existingReports.isEmpty()) {
            report = existingReports.get(0);
            if (report.getStatutValidation() == StatutValidation.VU_PAR_PASTEUR) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "REPORT_LOCKED", "Cannot modify a report already validated by the Pasteur");
            }
        } else {
            report = FamilyReport.builder()
                    .familleId(request.familleId())
                    .chefFamilleId(request.chefFamilleId())
                    .semaine(request.semaine())
                    .statutValidation(StatutValidation.BROUILLON)
                    .build();
        }

        aggregateFamilyStats(report, request.familleId(), request.semaine());

        if (request.commentaireSynthese() != null) {
            report.setCommentaireSynthese(request.commentaireSynthese());
        }

        report.setStatutValidation(StatutValidation.SOUMIS);
        report.setDateSoumission(LocalDateTime.now());

        FamilyReport saved = familyReportRepository.save(report);
        attachmentService.replace(EntityAttachment.EntityType.FAMILY_REPORT, saved.getId(), request.fichierIds());
        return saved;
    }

    private void aggregateFamilyStats(FamilyReport report, UUID familleId, LocalDate semaine) {
        List<MakerReport> makerReports = findMakerReportsForFamily(familleId, semaine);

        int totalPresents = 0;
        int totalAbsents = 0;
        int totalSorties = 0;
        int totalMaintenus = 0;
        Map<String, Object> motifSorties = new LinkedHashMap<>();
        Map<String, Object> difficultesParFaiseur = new LinkedHashMap<>();

        for (MakerReport mr : makerReports) {
            if (mr.getPresencesParCulte() != null) {
                for (Boolean present : mr.getPresencesParCulte().values()) {
                    if (present) totalPresents++;
                    else totalAbsents++;
                }
            }
            if (mr.getNbSorties() != null) {
                totalSorties += mr.getNbSorties();
                if (mr.getMotifSortie() != null) {
                    motifSorties.merge(mr.getMotifSortie().name(), mr.getNbSorties().longValue(),
                            (a, b) -> (Long) a + (Long) b);
                }
            }
            if (mr.getNbMaintenus() != null) {
                totalMaintenus += mr.getNbMaintenus();
            }
            if (mr.getDifficultes() != null && !mr.getDifficultes().isEmpty()) {
                String faiseurKey = mr.getFaiseurId().toString();
                difficultesParFaiseur.merge(faiseurKey, 1L, (a, b) -> (Long) a + (Long) b);
            }
        }

        long nbSuivisParalleles = parallelFollowupRepository.countByFamilleIdAndStatut(
                familleId, com.discipolat.common.enums.StatutSuiviParallele.EN_COURS);

        Set<UUID> faiseursWithReports = new HashSet<>();
        for (MakerReport mr : makerReports) {
            faiseursWithReports.add(mr.getFaiseurId());
        }

        List<Soul> familySouls = soulRepository.findAllByFamilleId(familleId);
        Set<UUID> allFaiseursInFamily = new HashSet<>();
        for (Soul soul : familySouls) {
            allFaiseursInFamily.add(soul.getFaiseurId());
        }

        List<UUID> faiseursSansRapport = new ArrayList<>();
        for (UUID faiseurId : allFaiseursInFamily) {
            if (!faiseursWithReports.contains(faiseurId)) {
                faiseursSansRapport.add(faiseurId);
            }
        }

        int totalPresencesPossible = totalPresents + totalAbsents;
        BigDecimal presenceMoyenne = totalPresencesPossible > 0
                ? BigDecimal.valueOf(totalPresents)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalPresencesPossible), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> statsAgregees = new LinkedHashMap<>();
        statsAgregees.put("totalPresents", totalPresents);
        statsAgregees.put("totalAbsents", totalAbsents);
        statsAgregees.put("totalRapports", makerReports.size());
        statsAgregees.put("nbFaiseursAvecRapport", faiseursWithReports.size());
        statsAgregees.put("nbFaiseursSansRapport", faiseursSansRapport.size());
        statsAgregees.put("difficultesParFaiseur", difficultesParFaiseur);

        report.setTotalPresents(totalPresents);
        report.setTotalAbsents(totalAbsents);
        report.setTotalSorties(totalSorties);
        report.setTotalMaintenus(totalMaintenus);
        report.setRepartitionSorties(motifSorties);
        report.setNbSuivisParalleles((int) nbSuivisParalleles);
        report.setPresenceMoyenne(presenceMoyenne);
        report.setStatsAgregees(statsAgregees);

        Map<String, Object> faiseursSansRapportDetails = new LinkedHashMap<>();
        for (UUID faiseurId : faiseursSansRapport) {
            try {
                User faiseur = userRepository.findById(faiseurId).orElse(null);
                if (faiseur != null) {
                    faiseursSansRapportDetails.put(faiseurId.toString(),
                            faiseur.getFirstName() + " " + faiseur.getLastName());
                }
            } catch (Exception ignored) {}
        }
        report.setFaiseursSansRapport(faiseursSansRapportDetails);
    }

    private List<MakerReport> findMakerReportsForFamily(UUID familleId, LocalDate semaine) {
        List<Soul> familySouls = soulRepository.findAllByFamilleId(familleId);
        Set<UUID> faiseurIds = new HashSet<>();
        for (Soul soul : familySouls) {
            faiseurIds.add(soul.getFaiseurId());
        }

        List<MakerReport> allReports = new ArrayList<>();
        for (UUID faiseurId : faiseurIds) {
            allReports.addAll(makerReportRepository.findByFaiseurIdAndSemaine(faiseurId, semaine));
        }
        return allReports;
    }

    @Transactional(readOnly = true)
    public Page<FamilyReport> findFamilyReports(UUID familleId, UUID chefFamilleId,
                                                LocalDate semaine, Pageable pageable) {
        // Super-utilisateurs : filtres DB explicites sur tous les rapports
        if (workspaceScope.isSuperUser()) {
            if (familleId != null) return familyReportRepository.findByFamilleId(familleId, pageable);
            if (chefFamilleId != null) return familyReportRepository.findByChefFamilleId(chefFamilleId, pageable);
            if (semaine != null) return familyReportRepository.findBySemaine(semaine, pageable);
            return familyReportRepository.findAll(pageable);
        }
        // Non super-utilisateur : intersection avec les familles de l'espace métier
        Set<UUID> visibleFamilies = workspaceScope.accessibleFamilyIds();
        List<FamilyReport> candidates;
        if (familleId != null) candidates = familyReportRepository.findByFamilleId(familleId, pageable).getContent();
        else if (chefFamilleId != null) candidates = familyReportRepository.findByChefFamilleId(chefFamilleId, pageable).getContent();
        else if (semaine != null) candidates = familyReportRepository.findBySemaine(semaine, pageable).getContent();
        else candidates = familyReportRepository.findAll(pageable).getContent();

        List<FamilyReport> scoped = candidates.stream()
                .filter(r -> visibleFamilies.contains(r.getFamilleId()))
                .toList();
        return paginate(scoped, pageable);
    }

    @Transactional(readOnly = true)
    public List<FamilyReport> findFamilyReportsByFamily(UUID familyId) {
        verifyFamilyReportAccess(familyId);
        return familyReportRepository.findByFamilleIdOrderBySemaineDesc(familyId);
    }

    @Transactional(readOnly = true)
    public List<FamilyReport> findFamilyReportsByFamilyAndWeek(UUID familyId, LocalDate semaine) {
        verifyFamilyReportAccess(familyId);
        return familyReportRepository.findByFamilleIdAndSemaineOrderByCreatedAtDesc(familyId, semaine);
    }

    public FamilyReport validateFamilyReport(UUID reportId, String validationType) {
        FamilyReport report = familyReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("FamilyReport", reportId));
        // Espace métier : on ne valide que les rapports des familles visibles.
        if (!workspaceScope.isSuperUser() && !workspaceScope.canAccessFamily(report.getFamilleId())) {
            throw new AccessDeniedException(
                    "Accès refusé : ce rapport ne concerne pas votre espace métier");
        }

        if ("RESPONSABLE".equalsIgnoreCase(validationType)) {
            report.setStatutValidation(StatutValidation.VU_PAR_RESPONSABLE);
            report.setDateValidationResponsable(LocalDateTime.now());
        } else if ("PASTEUR".equalsIgnoreCase(validationType)) {
            report.setStatutValidation(StatutValidation.VU_PAR_PASTEUR);
            report.setDateValidationPasteur(LocalDateTime.now());
        }

        return familyReportRepository.save(report);
    }

    // ======================== US-26: PRE-FILLED REPORT ========================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPreFilledReport(UUID faiseurId) {
        // Espace métier : on ne pré-remplit que pour soi (faiseur), pour un faiseur
        // de sa famille (chef) ou de ses départements (responsable).
        if (!workspaceScope.isSuperUser()) {
            UUID userId = securityUtils.getCurrentUserId();
            if (!faiseurId.equals(userId) && !workspaceScope.canAccessFaiseur(faiseurId)) {
                throw new AccessDeniedException(
                        "Accès refusé au pré-remplissage de ce faiseur");
            }
        }
        LocalDate currentWeek = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        List<Soul> souls = soulRepository.findAllByFaiseurId(faiseurId).stream()
                .filter(s -> !s.isDeleted())
                .filter(s -> s.getStatut() != StatutAme.DECROCHE)
                .toList();

        return souls.stream().map(soul -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("ameId", soul.getId());
            entry.put("nom", soul.getNomComplet());
            entry.put("statut", soul.getStatut().name());
            entry.put("etatSpirituel", soul.getEtatSpirituel());
            entry.put("semaine", currentWeek.toString());

            // Check if draft already exists
            Optional<MakerReport> existing = makerReportRepository
                    .findByFaiseurIdAndAmeIdAndSemaine(faiseurId, soul.getId(), currentWeek);
            entry.put("brouillonExistant", existing.isPresent());
            entry.put("dejaSoumis", existing.isPresent() && existing.get().isSoumis());

            return entry;
        }).toList();
    }

    // ======================== US-42: URGENT AID REQUESTS ========================

    public void markAidAsTreated(UUID reportId) {
        MakerReport report = findMakerReportById(reportId);
        // Mark as treated by updating the report - we use a simple approach:
        // Mark the vieFaiseurDemandesAide as treated by prefixing with [TRAITE]
        String demande = report.getVieFaiseurDemandesAide();
        if (demande != null && !demande.startsWith("[TRAITE]")) {
            report.setVieFaiseurDemandesAide("[TRAITE] " + demande);
            makerReportRepository.save(report);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUrgentAidRequests(Boolean traite) {
        List<MakerReport> allReports = makerReportRepository.findAll();
        // Espace métier : un responsable ne voit que les demandes d'aide des
        // faiseurs de ses départements ; les super-utilisateurs voient tout.
        Set<UUID> visibleFaiseurs = workspaceScope.isSuperUser() ? null : workspaceScope.accessibleFaiseurIds();

        return allReports.stream()
                .filter(r -> visibleFaiseurs == null || visibleFaiseurs.contains(r.getFaiseurId()))
                .filter(r -> r.getVieFaiseurDemandesAide() != null && !r.getVieFaiseurDemandesAide().isBlank())
                .filter(r -> {
                    if (traite == null) return true; // No filter
                    boolean estTraite = r.getVieFaiseurDemandesAide().startsWith("[TRAITE]");
                    return traite ? estTraite : !estTraite;
                })
                .map(r -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("reportId", r.getId());
                    entry.put("faiseurId", r.getFaiseurId());
                    entry.put("ameId", r.getAmeId());
                    entry.put("semaine", r.getSemaine().toString());
                    String demande = r.getVieFaiseurDemandesAide();
                    boolean estTraite = demande.startsWith("[TRAITE]");
                    entry.put("demande", estTraite ? demande.substring(8).trim() : demande);
                    entry.put("dateSoumission", r.getDateSoumission() != null ? r.getDateSoumission().toString() : null);
                    entry.put("traite", estTraite);
                    return entry;
                })
                .sorted((a, b) -> {
                    String dateA = (String) a.get("dateSoumission");
                    String dateB = (String) b.get("dateSoumission");
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                })
                .toList();
    }

    // ========================================================================
    // Isolation des espaces métiers (rôle actif)
    // ========================================================================

    /**
     * Vérifie que le rôle actif peut saisir un rapport pour le faiseur donné :
     * super-utilisateur, soi-même, ou un faiseur de l'espace métier courant
     * (faiseurs de la famille pour le chef, des départements pour le responsable).
     */
    private void verifyCanReportFor(UUID faiseurId) {
        if (workspaceScope.isSuperUser()) return;
        UUID userId = securityUtils.getCurrentUserId();
        if (faiseurId != null && (faiseurId.equals(userId) || workspaceScope.canAccessFaiseur(faiseurId))) {
            return;
        }
        throw new AccessDeniedException(
                "Vous ne pouvez pas saisir un rapport pour ce faiseur dans l'espace métier courant");
    }

    private void verifyFamilyReportAccess(UUID familleId) {
        if (!workspaceScope.isSuperUser() && !workspaceScope.canAccessFamily(familleId)) {
            throw new AccessDeniedException(
                    "Accès refusé aux rapports de cette famille dans l'espace métier courant");
        }
    }

    /** Pagination en mémoire (résultats déjà filtrés côté service). */
    private <T> Page<T> paginate(List<T> items, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<T> content = start < items.size() ? items.subList(start, end) : List.of();
        return new PageImpl<>(content, pageable, items.size());
    }
}