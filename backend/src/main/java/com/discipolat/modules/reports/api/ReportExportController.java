package com.discipolat.modules.reports.api;

import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.MakerReport;
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

    public ReportExportController(ReportService reportService) {
        this.reportService = reportService;
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

    // ======================== US-43: PDF EXPORT ========================

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

        // Generate HTML report that can be printed/saved as PDF
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\">");
        html.append("<title>Rapport Consolidé - Discipolat</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 40px; color: #333; }");
        html.append("h1 { color: #1a365d; border-bottom: 3px solid #2b6cb0; padding-bottom: 10px; }");
        html.append("h2 { color: #2d3748; margin-top: 30px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th { background-color: #2b6cb0; color: white; padding: 12px; text-align: left; }");
        html.append("td { padding: 10px; border-bottom: 1px solid #e2e8f0; }");
        html.append("tr:nth-child(even) { background-color: #f7fafc; }");
        html.append(".summary { background-color: #ebf8ff; padding: 15px; border-radius: 8px; margin: 20px 0; }");
        html.append(".kpi { display: inline-block; margin: 10px 20px 10px 0; padding: 10px 20px; background: #fff; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }");
        html.append(".kpi-value { font-size: 24px; font-weight: bold; color: #2b6cb0; }");
        html.append(".kpi-label { font-size: 12px; color: #718096; }");
        html.append("@media print { body { margin: 20px; } }");
        html.append("</style></head><body>");

        html.append("<h1>📊 Rapport Consolidé Discipolat</h1>");
        html.append("<p>Période : semaine du <strong>").append(semaine).append("</strong></p>");
        html.append("<p>Généré le : ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</p>");

        int totalFamilles = reports.size();
        int totalPresents = reports.stream().mapToInt(r -> r.getTotalPresents() != null ? r.getTotalPresents() : 0).sum();
        int totalAbsents = reports.stream().mapToInt(r -> r.getTotalAbsents() != null ? r.getTotalAbsents() : 0).sum();
        int totalSorties = reports.stream().mapToInt(r -> r.getTotalSorties() != null ? r.getTotalSorties() : 0).sum();
        int totalMaintenus = reports.stream().mapToInt(r -> r.getTotalMaintenus() != null ? r.getTotalMaintenus() : 0).sum();

        html.append("<div class=\"summary\">");
        html.append("<h2>Synthèse Globale</h2>");
        html.append("<div class=\"kpi\"><div class=\"kpi-value\">").append(totalFamilles).append("</div><div class=\"kpi-label\">Familles</div></div>");
        html.append("<div class=\"kpi\"><div class=\"kpi-value\">").append(totalPresents).append("</div><div class=\"kpi-label\">Présents</div></div>");
        html.append("<div class=\"kpi\"><div class=\"kpi-value\">").append(totalAbsents).append("</div><div class=\"kpi-label\">Absents</div></div>");
        html.append("<div class=\"kpi\"><div class=\"kpi-value\">").append(totalSorties).append("</div><div class=\"kpi-label\">Sorties</div></div>");
        html.append("<div class=\"kpi\"><div class=\"kpi-value\">").append(totalMaintenus).append("</div><div class=\"kpi-label\">Maintenus</div></div>");
        html.append("</div>");

        html.append("<h2>Détail par Famille</h2>");
        html.append("<table><thead><tr>");
        html.append("<th>Famille</th><th>Présence</th><th>Présents</th><th>Absents</th><th>Sorties</th><th>Maintenus</th><th>Suivis Parallèles</th><th>Statut</th>");
        html.append("</tr></thead><tbody>");

        for (FamilyReport report : reports) {
            html.append("<tr>");
            html.append("<td>").append(report.getFamilleId()).append("</td>");
            html.append("<td>").append(report.getPresenceMoyenne() != null ? report.getPresenceMoyenne() + "%" : "-").append("</td>");
            html.append("<td>").append(report.getTotalPresents() != null ? report.getTotalPresents() : 0).append("</td>");
            html.append("<td>").append(report.getTotalAbsents() != null ? report.getTotalAbsents() : 0).append("</td>");
            html.append("<td>").append(report.getTotalSorties() != null ? report.getTotalSorties() : 0).append("</td>");
            html.append("<td>").append(report.getTotalMaintenus() != null ? report.getTotalMaintenus() : 0).append("</td>");
            html.append("<td>").append(report.getNbSuivisParalleles() != null ? report.getNbSuivisParalleles() : 0).append("</td>");
            html.append("<td>").append(report.getStatutValidation()).append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody></table>");
        html.append("<p style=\"margin-top: 40px; color: #a0aec0; font-size: 12px; text-align: center;\">");
        html.append("Rapport généré automatiquement par Discipolat © ").append(LocalDate.now().getYear());
        html.append("</p>");
        html.append("</body></html>");

        String filename = "rapport-consolide-" + semaine.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".html";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_HTML)
                .body(html.toString().getBytes());
    }
}
