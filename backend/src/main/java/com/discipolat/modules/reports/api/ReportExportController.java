package com.discipolat.modules.reports.api;

import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.ReportPdfService;
import com.discipolat.modules.reports.domain.ReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/export")
public class ReportExportController {

    private final ReportService reportService;
    private final ReportPdfService reportPdfService;

    public ReportExportController(ReportService reportService, ReportPdfService reportPdfService) {
        this.reportService = reportService;
        this.reportPdfService = reportPdfService;
    }

    @GetMapping("/maker-weekly")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<byte[]> exportMakerReports(
            @RequestParam(required = false) UUID faiseurId,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) LocalDate semaine) {

        Page<MakerReport> reportsPage = reportService.findMakerReports(faiseurId, familleId, null, semaine,
                PageRequest.of(0, 10000));
        List<MakerReport> reports = reportsPage.getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Faiseur,Âme,Semaine,Présences,Difficultés,Sorties,Maintenus,Soumis\n");

        for (MakerReport report : reports) {
            csv.append(String.format("%s,%s,%s,%s,\"%s\",\"%s\",%d,%d,%s\n",
                    report.getId(),
                    report.getFaiseurId(),
                    report.getAmeId(),
                    report.getSemaine(),
                    report.getPresencesParCulte() != null ? report.getPresencesParCulte().toString() : "",
                    report.getDifficultes() != null ? report.getDifficultes().replace("\"", "\"\"") : "",
                    report.getNbSorties() != null ? report.getNbSorties() : 0,
                    report.getNbMaintenus() != null ? report.getNbMaintenus() : 0,
                    report.isSoumis() ? "Oui" : "Non"
            ));
        }

        String filename = "rapports-faiseur-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv.toString().getBytes());
    }

    @GetMapping("/family-weekly")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<byte[]> exportFamilyReports(
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) LocalDate semaine) {

        Page<FamilyReport> reportsPage = reportService.findFamilyReports(familleId, null, semaine,
                PageRequest.of(0, 10000));
        List<FamilyReport> reports = reportsPage.getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Famille,Chef,Semaine,Présence Moyenne,Total Présents,Total Absents,Sorties,Maintenus,Suivis Parallèles,Statut\n");

        for (FamilyReport report : reports) {
            csv.append(String.format("%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%s\n",
                    report.getId(),
                    report.getFamilleId(),
                    report.getChefFamilleId(),
                    report.getSemaine(),
                    report.getPresenceMoyenne() != null ? report.getPresenceMoyenne().toString() : "0",
                    report.getTotalPresents() != null ? report.getTotalPresents() : 0,
                    report.getTotalAbsents() != null ? report.getTotalAbsents() : 0,
                    report.getTotalSorties() != null ? report.getTotalSorties() : 0,
                    report.getTotalMaintenus() != null ? report.getTotalMaintenus() : 0,
                    report.getNbSuivisParalleles() != null ? report.getNbSuivisParalleles() : 0,
                    report.getStatutValidation()
            ));
        }

        String filename = "rapports-famille-" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(csv.toString().getBytes());
    }

    // ======================== PDF EXPORT ========================

    @GetMapping("/consolidated-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')")
    public ResponseEntity<byte[]> exportConsolidatedPdf(
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) LocalDate semaine) {

        if (semaine == null) {
            semaine = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        }

        Page<FamilyReport> reportsPage = reportService.findFamilyReports(familleId, null, semaine,
                PageRequest.of(0, 10000));
        List<FamilyReport> reports = reportsPage.getContent();

        byte[] pdfBytes = reportPdfService.generateFamilyReportPdf(reports, semaine);

        String filename = "rapport-consolide-" + semaine.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/maker-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')")
    public ResponseEntity<byte[]> exportMakerPdf(
            @RequestParam(required = false) UUID faiseurId,
            @RequestParam(required = false) UUID familleId,
            @RequestParam(required = false) LocalDate semaine) {

        if (semaine == null) {
            semana = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        }

        Page<MakerReport> reportsPage = reportService.findMakerReports(faiseurId, familleId, null, semana,
                PageRequest.of(0, 10000));
        List<MakerReport> reports = reportsPage.getContent();

        byte[] pdfBytes = reportPdfService.generateMakerReportPdf(reports, semana);

        String filename = "rapport-faiseur-" + semana.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
