package com.discipolat.modules.reports.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutValidation;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.reports.api.SubmitFamilyReportRequest;
import com.discipolat.modules.reports.api.SubmitMakerReportRequest;
import com.discipolat.modules.parallelfollowups.domain.ParallelFollowupRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final ParallelFollowupRepository parallelFollowupRepository;

    public ReportService(MakerReportRepository makerReportRepository,
                         FamilyReportRepository familyReportRepository,
                         SecurityUtils securityUtils,
                         SoulRepository soulRepository,
                         UserRepository userRepository,
                         ParallelFollowupRepository parallelFollowupRepository) {
        this.makerReportRepository = makerReportRepository;
        this.familyReportRepository = familyReportRepository;
        this.securityUtils = securityUtils;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.parallelFollowupRepository = parallelFollowupRepository;
    }

    /**
     * Submit a maker report (RG-03: once submitted, locked and non-modifiable)
     */
    public MakerReport submitMakerReport(SubmitMakerReportRequest request) {
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

        return makerReportRepository.save(report);
    }

    /**
     * US-29: Save a maker report as draft (not submitted)
     */
    public MakerReport saveDraft(SubmitMakerReportRequest request) {
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

        return makerReportRepository.save(report);
    }

    public MakerReport findMakerReportById(UUID id) {
        return makerReportRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MakerReport", id));
    }

    public MakerReport saveMakerReport(MakerReport report) {
        return makerReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public Page<MakerReport> findMakerReports(UUID faiseurId, UUID familleId, UUID ameId,
                                              LocalDate semaine, Pageable pageable) {
        if (faiseurId != null && semaine != null)
            return makerReportRepository.findByFaiseurId(faiseurId, pageable);
        if (ameId != null) return makerReportRepository.findByAmeId(ameId, pageable);
        if (semaine != null) return makerReportRepository.findBySemaine(semaine, pageable);
        return makerReportRepository.findAll(pageable);
    }

    public FamilyReport submitFamilyReport(SubmitFamilyReportRequest request) {
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

        return familyReportRepository.save(report);
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
        if (familleId != null) return familyReportRepository.findByFamilleId(familleId, pageable);
        if (chefFamilleId != null) return familyReportRepository.findByChefFamilleId(chefFamilleId, pageable);
        if (semaine != null) return familyReportRepository.findBySemaine(semaine, pageable);
        return familyReportRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<FamilyReport> findFamilyReportsByFamily(UUID familyId) {
        return familyReportRepository.findByFamilleIdOrderBySemaineDesc(familyId);
    }

    @Transactional(readOnly = true)
    public List<FamilyReport> findFamilyReportsByFamilyAndWeek(UUID familyId, LocalDate semaine) {
        return familyReportRepository.findByFamilleIdAndSemaineOrderByCreatedAtDesc(familyId, semaine);
    }

    public FamilyReport validateFamilyReport(UUID reportId, String validationType) {
        FamilyReport report = familyReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("FamilyReport", reportId));

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

        return allReports.stream()
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
}