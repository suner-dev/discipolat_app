package com.discipolat.modules.reports.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.files.domain.EntityAttachment;
import com.discipolat.modules.files.domain.EntityAttachmentService;
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
    private final EntityAttachmentService attachmentService;

    public MakerReportController(ReportService reportService,
                                  ReportCorrectionRepository correctionRepository,
                                  SecurityUtils securityUtils,
                                  EntityAttachmentService attachmentService) {
        this.reportService = reportService;
        this.correctionRepository = correctionRepository;
        this.securityUtils = securityUtils;
        this.attachmentService = attachmentService;
    }

    private MakerReportResponse toResponse(MakerReport report) {
        return MakerReportResponse.from(report,
                attachmentService.itemsFor(EntityAttachment.EntityType.MAKER_REPORT, report.getId()));
    }

    private FamilyReportResponse toFamilyResponse(com.discipolat.modules.reports.domain.FamilyReport report) {
        return FamilyReportResponse.from(report,
                attachmentService.itemsFor(EntityAttachment.EntityType.FAMILY_REPORT, report.getId()));
    }

    @GetMapping("/maker-weekly")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE', 'TENANT', null)")
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
        Page<MakerReportResponse> response = reports.map(this::toResponse);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @PostMapping("/maker-weekly")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_SUBMITMAKERREPORT', 'TENANT', null)")
    public ResponseEntity<MakerReportResponse> submitMakerReport(@Valid @RequestBody SubmitMakerReportRequest request) {
        MakerReport report = reportService.submitMakerReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    /** US-29: Save report as draft */
    @PostMapping("/maker-weekly/draft")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_SAVEDRAFT', 'TENANT', null)")
    public ResponseEntity<MakerReportResponse> saveDraft(@Valid @RequestBody SubmitMakerReportRequest request) {
        MakerReport report = reportService.saveDraft(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(report));
    }

    @GetMapping("/maker-weekly/{id}")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_GETMAKERREPORT', 'TENANT', null)")
    public ResponseEntity<MakerReportResponse> getMakerReport(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(reportService.findMakerReportById(id)));
    }

    // ======================== US-26: PRE-FILLED REPORT ========================

    @GetMapping("/maker-weekly/prefill/{faiseurId}")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE', 'TENANT', null)")
    public ResponseEntity<List<Map<String, Object>>> getPreFilledReport(@PathVariable UUID faiseurId) {
        return ResponseEntity.ok(reportService.getPreFilledReport(faiseurId));
    }

    // ======================== US-42: URGENT AID REQUESTS ========================

    // ======================== US-42: URGENT AID REQUESTS ========================

    @GetMapping("/maker-weekly/urgent-aid")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_GETURGENTAIDREQUESTS', 'TENANT', null)")
    public ResponseEntity<List<Map<String, Object>>> getUrgentAidRequests(@RequestParam(required = false) Boolean traite) {
        return ResponseEntity.ok(reportService.getUrgentAidRequests(traite));
    }

    @PatchMapping("/maker-weekly/urgent-aid/{reportId}/mark-treated")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_MARKAIDASTREATED', 'TENANT', null)")
    public ResponseEntity<Map<String, String>> markAidAsTreated(@PathVariable UUID reportId) {
        reportService.markAidAsTreated(reportId);
        return ResponseEntity.ok(Map.of("message", "Aid request marked as treated"));
    }

    // ======================== US-34: REPORT CORRECTION ========================

    @PostMapping("/correction")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_CORRECTREPORT', 'TENANT', null)")
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
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_GETCORRECTIONS', 'TENANT', null)")
    public ResponseEntity<List<ReportCorrection>> getCorrections(@PathVariable UUID reportId) {
        return ResponseEntity.ok(correctionRepository.findByReportIdOrderByCreatedAtDesc(reportId));
    }

    @GetMapping("/family-weekly")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE', 'TENANT', null)")
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
        Page<FamilyReportResponse> response = reports.map(this::toFamilyResponse);
        return ResponseEntity.ok(PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @PostMapping("/family-weekly")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_SUBMITFAMILYREPORT', 'TENANT', null)")
    public ResponseEntity<FamilyReportResponse> submitFamilyReport(@Valid @RequestBody SubmitFamilyReportRequest request) {
        com.discipolat.modules.reports.domain.FamilyReport report = reportService.submitFamilyReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toFamilyResponse(report));
    }

    @GetMapping("/family-weekly/{familyId}")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE', 'TENANT', null)")
    public ResponseEntity<List<FamilyReportResponse>> getFamilyReportsByFamily(
            @PathVariable UUID familyId,
            @RequestParam(required = false) LocalDate semaine) {
        List<com.discipolat.modules.reports.domain.FamilyReport> reports;
        if (semaine != null) {
            reports = reportService.findFamilyReportsByFamilyAndWeek(familyId, semaine);
        } else {
            reports = reportService.findFamilyReportsByFamily(familyId);
        }
        return ResponseEntity.ok(reports.stream().map(this::toFamilyResponse).toList());
    }

    @PatchMapping("/family-weekly/{id}/validate")
    @PreAuthorize("@authz.can('REPORTS_EXECUTE_VALIDATEFAMILYREPORT', 'TENANT', null)")
    public ResponseEntity<FamilyReportResponse> validateFamilyReport(
            @PathVariable UUID id,
            @RequestBody @Valid ValidateReportRequest request) {
        com.discipolat.modules.reports.domain.FamilyReport report =
                reportService.validateFamilyReport(id, request.validationType());
        return ResponseEntity.ok(toFamilyResponse(report));
    }
}
