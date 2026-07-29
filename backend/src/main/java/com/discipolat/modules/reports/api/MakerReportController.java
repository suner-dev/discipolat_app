package com.discipolat.modules.reports.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.ReportService;
import com.discipolat.modules.reports.domain.ReportCorrection;
import com.discipolat.modules.reports.domain.ReportCorrectionRepository;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/reports")
public class MakerReportController {

    private final ReportService reportService;
    private final ReportCorrectionRepository correctionRepository;
    private final SecurityUtils securityUtils;

    public MakerReportController(ReportService reportService,
                                  ReportCorrectionRepository correctionRepository,
                                  SecurityUtils securityUtils) {
        this.reportService = reportService;
        this.correctionRepository = correctionRepository;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/maker-weekly")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<MakerReportResponse>> getMakerReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID faiseurId,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) UUID ameId,
            @RequestParam(required = false) LocalDate semaine) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "semaine").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<MakerReport> reports = reportService.findMakerReports(faiseurId, familleId, ameId, semaine, pageable);
        Page<MakerReportResponse> response = reports.map(MakerReportResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @PostMapping("/maker-weekly")
    @PreAuthorize("hasAnyRole('PASTEUR', 'FAISEUR')")
    public ResponseEntity<MakerReportResponse> submitMakerReport(@Valid @RequestBody SubmitMakerReportRequest request) {
        MakerReport report = reportService.submitMakerReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MakerReportResponse.from(report));
    }

    /** US-29: Save report as draft */
    @PostMapping("/maker-weekly/draft")
    @PreAuthorize("hasAnyRole('PASTEUR', 'FAISEUR')")
    public ResponseEntity<MakerReportResponse> saveDraft(@Valid @RequestBody SubmitMakerReportRequest request) {
        MakerReport report = reportService.saveDraft(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(MakerReportResponse.from(report));
    }

    @GetMapping("/maker-weekly/{id}")
    public ResponseEntity<MakerReportResponse> getMakerReport(@PathVariable UUID id) {
        return ResponseEntity.ok(MakerReportResponse.from(reportService.findMakerReportById(id)));
    }

    // ======================== US-26: PRE-FILLED REPORT ========================

    @GetMapping("/maker-weekly/prefill/{faiseurId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<List<Map<String, Object>>> getPreFilledReport(@PathVariable UUID faiseurId) {
        return ResponseEntity.ok(reportService.getPreFilledReport(faiseurId));
    }

    // ======================== US-42: URGENT AID REQUESTS ========================

    // ======================== US-42: URGENT AID REQUESTS ========================

    @GetMapping("/maker-weekly/urgent-aid")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<List<Map<String, Object>>> getUrgentAidRequests(@RequestParam(required = false) Boolean traite) {
        return ResponseEntity.ok(reportService.getUrgentAidRequests(traite));
    }

    @PatchMapping("/maker-weekly/urgent-aid/{reportId}/mark-treated")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<Map<String, String>> markAidAsTreated(@PathVariable UUID reportId) {
        reportService.markAidAsTreated(reportId);
        return ResponseEntity.ok(Map.of("message", "Aid request marked as treated"));
    }

    // ======================== US-34: REPORT CORRECTION ========================

    @PostMapping("/correction")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> correctReport(
            @Valid @RequestBody ReportCorrectionRequest request) {
        MakerReport report = reportService.findMakerReportById(request.reportId());
        ReportCorrection correction = ReportCorrection.builder()
                .reportId(request.reportId())
                .correctedBy(securityUtils.getCurrentUserId())
                .ancienneValeur(request.ancienneValeur())
                .nouvelleValeur(request.nouvelleValeur())
                .raison(request.raison())
                .build();
        correctionRepository.save(correction);

        // Apply corrections to report
        if (request.nouvelleValeur().containsKey("presencesParCulte")) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> presences = (Map<String, Boolean>) request.nouvelleValeur().get("presencesParCulte");
            report.setPresencesParCulte(presences);
        }
        if (request.nouvelleValeur().containsKey("notesComplementaires")) {
            report.setNotesComplementaires((String) request.nouvelleValeur().get("notesComplementaires"));
        }
        reportService.saveMakerReport(report);

        return ResponseEntity.ok(Map.of("message", "Report corrected successfully"));
    }

    @GetMapping("/correction/{reportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<List<ReportCorrection>> getCorrections(@PathVariable UUID reportId) {
        return ResponseEntity.ok(correctionRepository.findByReportIdOrderByCreatedAtDesc(reportId));
    }

    @GetMapping("/family-weekly")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<PageResponse<FamilyReportResponse>> getFamilyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) UUID chefFamilleId,
            @RequestParam(required = false) LocalDate semaine) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "semaine").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<com.discipolat.modules.reports.domain.FamilyReport> reports =
                reportService.findFamilyReports(familleId, chefFamilleId, semaine, pageable);
        Page<FamilyReportResponse> response = reports.map(FamilyReportResponse::from);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @PostMapping("/family-weekly")
    @PreAuthorize("hasAnyRole('PASTEUR', 'FAISEUR')")
    public ResponseEntity<FamilyReportResponse> submitFamilyReport(@Valid @RequestBody SubmitFamilyReportRequest request) {
        com.discipolat.modules.reports.domain.FamilyReport report = reportService.submitFamilyReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(FamilyReportResponse.from(report));
    }

    @GetMapping("/family-weekly/{familyId}")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE', 'FAISEUR')")
    public ResponseEntity<List<FamilyReportResponse>> getFamilyReportsByFamily(
            @PathVariable UUID familyId,
            @RequestParam(required = false) LocalDate semaine) {
        List<com.discipolat.modules.reports.domain.FamilyReport> reports;
        if (semaine != null) {
            reports = reportService.findFamilyReportsByFamilyAndWeek(familyId, semaine);
        } else {
            reports = reportService.findFamilyReportsByFamily(familyId);
        }
        return ResponseEntity.ok(reports.stream().map(FamilyReportResponse::from).toList());
    }

    @PatchMapping("/family-weekly/{id}/validate")
    @PreAuthorize("hasAnyRole('PASTEUR', 'RESPONSABLE')")
    public ResponseEntity<FamilyReportResponse> validateFamilyReport(
            @PathVariable UUID id,
            @RequestBody @Valid ValidateReportRequest request) {
        com.discipolat.modules.reports.domain.FamilyReport report =
                reportService.validateFamilyReport(id, request.validationType());
        return ResponseEntity.ok(FamilyReportResponse.from(report));
    }
}
