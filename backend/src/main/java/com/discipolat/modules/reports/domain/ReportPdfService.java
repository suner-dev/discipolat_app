package com.discipolat.modules.reports.domain;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportPdfService {

    private static final Color BRAND_PRIMARY = new Color(43, 108, 176);
    private static final Color BRAND_LIGHT = new Color(235, 248, 255);
    private static final Color BRAND_DARK = new Color(26, 54, 93);
    private static final Color TEXT_COLOR = new Color(45, 55, 72);
    private static final Color LIGHT_GRAY = new Color(247, 250, 252);
    private static final Color BORDER_COLOR = new Color(226, 232, 240);

    public byte[] generateFamilyReportPdf(List<FamilyReport> reports, LocalDate semaine) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeader(document, "Rapport Consolidé Discipolat", semaine);
            addSummaryKpis(document, reports);
            addFamilyTable(document, reports);
            addFooter(document);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }

        return baos.toByteArray();
    }

/** P22 — Rapport exécutif synthétique : remontée des indicateurs clés sur la période. */
    public byte[] generateExecutiveReportPdf(String titre, java.time.LocalDate from, java.time.LocalDate to,
                                             long totalReports, long totalSorties, long totalMaintenus,
                                             long totalFamilles, long totalMembres) {
        try {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeader(document, titre, from);

            document.add(new Paragraph("Période : " + from + " → " + to,
                    new Font(Font.HELVETICA, 11)));
            document.add(new Paragraph(" "));

            PdfPTable kpi = new PdfPTable(2);
            kpi.setWidthPercentage(100);
            kpi.setSpacingAfter(20);
            addKpiCell(kpi, String.valueOf(totalMembres), "Membres");
            addKpiCell(kpi, String.valueOf(totalFamilles), "Familles");
            addKpiCell(kpi, String.valueOf(totalReports), "Rapports soumis");
            addKpiCell(kpi, String.valueOf(totalSorties), "Sorties pastorales");
            addKpiCell(kpi, String.valueOf(totalMaintenus), "Âmes maintenues");
            document.add(kpi);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate executive PDF report", e);
        }
    }
    public byte[] generateMakerReportPdf(List<MakerReport> reports, LocalDate semaine) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeader(document, "Rapport Hebdomadaire des Faiseurs", semaine);

            // Summary
            int totalReports = reports.size();
            long totalSorties = reports.stream().mapToLong(r -> r.getNbSorties() != null ? r.getNbSorties() : 0).sum();
            long totalMaintenus = reports.stream().mapToLong(r -> r.getNbMaintenus() != null ? r.getNbMaintenus() : 0).sum();

            PdfPTable summaryTable = new PdfPTable(3);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(20);
            addKpiCell(summaryTable, String.valueOf(totalReports), "Rapports");
            addKpiCell(summaryTable, String.valueOf(totalSorties), "Sorties");
            addKpiCell(summaryTable, String.valueOf(totalMaintenus), "Maintenus");
            document.add(summaryTable);

            // Table
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 2f, 2f, 1.5f, 1.5f, 1.5f, 1.5f});

            String[] headers = {"Faiseur", "Ame", "Semaine", "Presences", "Difficultes", "Sorties", "Maintenus"};
            addTableHeaders(table, headers);

            boolean alternate = false;
            for (MakerReport r : reports) {
                Color bg = alternate ? LIGHT_GRAY : Color.WHITE;
                addTableCell(table, String.valueOf(r.getFaiseurId()), bg);
                addTableCell(table, String.valueOf(r.getAmeId()), bg);
                addTableCell(table, String.valueOf(r.getSemaine()), bg);
                addTableCell(table, r.getPresencesParCulte() != null ? r.getPresencesParCulte().toString() : "-", bg);
                addTableCell(table, r.getDifficultes() != null ? truncate(r.getDifficultes(), 30) : "-", bg);
                addTableCell(table, String.valueOf(r.getNbSorties() != null ? r.getNbSorties() : 0), bg);
                addTableCell(table, String.valueOf(r.getNbMaintenus() != null ? r.getNbMaintenus() : 0), bg);
                alternate = !alternate;
            }

            document.add(table);
            addFooter(document);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate maker PDF report", e);
        }

        return baos.toByteArray();
    }

    private void addHeader(Document document, String titleText, LocalDate semana) throws DocumentException {
        Paragraph title = new Paragraph(titleText,
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 22, com.lowagie.text.Font.BOLD, BRAND_DARK));
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph period = new Paragraph(
                "Semaine du " + formatWeek(semana),
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.NORMAL, TEXT_COLOR));
        period.setAlignment(Element.ALIGN_CENTER);
        period.setSpacingAfter(5);
        document.add(period);

        Paragraph genDate = new Paragraph(
                "Genere le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.ITALIC, Color.GRAY));
        genDate.setAlignment(Element.ALIGN_CENTER);
        genDate.setSpacingAfter(20);
        document.add(genDate);
    }

    private void addSummaryKpis(Document document, List<FamilyReport> reports) throws DocumentException {
        int totalFamilles = reports.size();
        int totalPresents = reports.stream().mapToInt(r -> r.getTotalPresents() != null ? r.getTotalPresents() : 0).sum();
        int totalAbsents = reports.stream().mapToInt(r -> r.getTotalAbsents() != null ? r.getTotalAbsents() : 0).sum();
        int totalSorties = reports.stream().mapToInt(r -> r.getTotalSorties() != null ? r.getTotalSorties() : 0).sum();
        int totalMaintenus = reports.stream().mapToInt(r -> r.getTotalMaintenus() != null ? r.getTotalMaintenus() : 0).sum();

        PdfPTable kpiTable = new PdfPTable(5);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(20);
        addKpiCell(kpiTable, String.valueOf(totalFamilles), "Familles");
        addKpiCell(kpiTable, String.valueOf(totalPresents), "Presents");
        addKpiCell(kpiTable, String.valueOf(totalAbsents), "Absents");
        addKpiCell(kpiTable, String.valueOf(totalSorties), "Sorties");
        addKpiCell(kpiTable, String.valueOf(totalMaintenus), "Maintenus");
        document.add(kpiTable);
    }

    private void addFamilyTable(Document document, List<FamilyReport> reports) throws DocumentException {
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f, 2f});

        String[] headers = {"Famille", "Presence", "Presents", "Absents", "Sorties", "Maintenus", "Paralleles", "Statut"};
        addTableHeaders(table, headers);

        boolean alternate = false;
        for (FamilyReport r : reports) {
            Color bg = alternate ? LIGHT_GRAY : Color.WHITE;
            addTableCell(table, String.valueOf(r.getFamilleId()), bg);
            addTableCell(table, r.getPresenceMoyenne() != null ? r.getPresenceMoyenne() + "%" : "-", bg);
            addTableCell(table, String.valueOf(r.getTotalPresents() != null ? r.getTotalPresents() : 0), bg);
            addTableCell(table, String.valueOf(r.getTotalAbsents() != null ? r.getTotalAbsents() : 0), bg);
            addTableCell(table, String.valueOf(r.getTotalSorties() != null ? r.getTotalSorties() : 0), bg);
            addTableCell(table, String.valueOf(r.getTotalMaintenus() != null ? r.getTotalMaintenus() : 0), bg);
            addTableCell(table, String.valueOf(r.getNbSuivisParalleles() != null ? r.getNbSuivisParalleles() : 0), bg);
            addTableCell(table, String.valueOf(r.getStatutValidation()), bg);
            alternate = !alternate;
        }

        document.add(table);
    }

    private void addTableHeaders(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h,
                    new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, Color.WHITE)));
            cell.setBackgroundColor(BRAND_PRIMARY);
            cell.setPadding(8);
            cell.setBorderWidth(0);
            table.addCell(cell);
        }
    }

    private void addKpiCell(PdfPTable table, String value, String label) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(BRAND_LIGHT);
        cell.setPadding(12);
        cell.setBorderWidth(1);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph p = new Paragraph();
        p.add(new Chunk(value,
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 24, com.lowagie.text.Font.BOLD, BRAND_PRIMARY)));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk(label,
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, Color.GRAY)));
        cell.setPhrase(p);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text,
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, TEXT_COLOR)));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6);
        cell.setBorderWidth(0);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private void addFooter(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        Paragraph footer = new Paragraph(
                "Rapport genere automatiquement par Discipolat (c) " + LocalDate.now().getYear(),
                new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC, Color.GRAY));
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private String formatWeek(LocalDate date) {
        LocalDate monday = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return monday.format(fmt) + " - " + sunday.format(fmt);
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
