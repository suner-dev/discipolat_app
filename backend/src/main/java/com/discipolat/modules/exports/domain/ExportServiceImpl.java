package com.discipolat.modules.exports.domain;

import com.discipolat.common.infrastructure.config.TenantFileIsolationConfig;
import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Transactional
public class ExportServiceImpl implements ExportService {

    private final Path exportDir;
    private final Map<UUID, ExportResult> exportStore = new java.util.concurrent.ConcurrentHashMap<>();

    public ExportServiceImpl(
            @Value("${app.exports.directory:./exports}") String exportDirPath,
            TenantFileIsolationConfig tenantConfig) {
        java.util.UUID tenantUuid = TenantContext.getCurrentTenantId();
        String tenantId = tenantUuid != null ? tenantUuid.toString() : "default";
        this.exportDir = Paths.get(exportDirPath, tenantId).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.exportDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create export directory", e);
        }
    }

    @Override
    public ExportResult createExport(ExportRequest request, UUID userId) {
        UUID exportId = UUID.randomUUID();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = String.format("%s_%s_%s.%s",
                request.getType().name().toLowerCase(),
                timestamp,
                exportId.toString().substring(0, 8),
                getFileExtension(request.getFormat()));

        ExportResult result = ExportResult.builder()
                .exportId(exportId)
                .type(request.getType())
                .format(request.getFormat())
                .status("PENDING")
                .fileName(fileName)
                .createdAt(Instant.now())
                .build();

        exportStore.put(exportId, result);

        // Process asynchronously
        new Thread(() -> processExport(exportId, request, fileName, userId)).start();

        return result;
    }

    private void processExport(UUID exportId, ExportRequest request, String fileName, UUID userId) {
        ExportResult result = exportStore.get(exportId);
        if (result == null) return;

        result.setStatus("PROCESSING");
        try {
            byte[] fileData = generateExportFile(request);
            Path filePath = exportDir.resolve(fileName);
            Files.write(filePath, fileData);

            result.setStatus("COMPLETED");
            result.setTotalRecords((long) (fileData.length / 100)); // approximate
            result.setDownloadUrl("/api/v1/exports/download/" + exportId);
            result.setFileSize((long) fileData.length);
            result.setCompletedAt(Instant.now());
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
            result.setCompletedAt(Instant.now());
        }
    }

    @Override
    public ExportResult getExportStatus(UUID exportId) {
        return exportStore.get(exportId);
    }

    @Override
    public List<ExportResult> getExportHistory(UUID userId) {
        return exportStore.values().stream()
                .sorted(Comparator.comparing(ExportResult::getCreatedAt).reversed())
                .limit(50)
                .toList();
    }

    @Override
    public byte[] generateExportFile(ExportRequest request) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            String format = request.getFormat().toLowerCase();

            if ("csv".equals(format)) {
                return generateCsv(request);
            } else if ("excel".equals(format) || "xlsx".equals(format)) {
                return generateExcel(request);
            } else if ("pdf".equals(format)) {
                return generatePdf(request);
            } else if ("json".equals(format)) {
                return generateJson(request);
            } else if ("zip".equals(format)) {
                return generateZip(request);
            } else {
                return generateCsv(request); // default
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate export file", e);
        }
    }

    private byte[] generateCsv(ExportRequest request) {
        List<Map<String, Object>> data = fetchDataForExport(request);
        if (data.isEmpty()) {
            return "No data found\n".getBytes();
        }

        StringBuilder csv = new StringBuilder();
        boolean includeHeaders = request.getIncludeHeaders() != null ? request.getIncludeHeaders() : true;

        // Headers
        if (includeHeaders && !data.isEmpty()) {
            Set<String> allColumns = new LinkedHashSet<>();
            if (request.getColumns() != null && !request.getColumns().isEmpty()) {
                allColumns.addAll(request.getColumns());
            } else {
                for (Map<String, Object> row : data) {
                    allColumns.addAll(row.keySet());
                }
            }
            csv.append(String.join(",", allColumns)).append("\n");
        }

        // Data rows
        for (Map<String, Object> row : data) {
            List<String> values = new ArrayList<>();
            for (String col : (request.getColumns() != null && !request.getColumns().isEmpty()
                    ? request.getColumns() : row.keySet())) {
                Object val = row.get(col);
                String strVal = val != null ? escapeCsv(val.toString()) : "";
                values.add(strVal);
            }
            csv.append(String.join(",", values)).append("\n");
        }

        return csv.toString().getBytes();
    }

    private byte[] generateExcel(ExportRequest request) {
        // For now, return CSV with .xlsx extension - in production use Apache POI
        byte[] csv = generateCsv(request);
        return ("EXCEL_FORMAT_PLACEHOLDER\n" + new String(csv)).getBytes();
    }

    private byte[] generatePdf(ExportRequest request) {
        // For now, return simple text - in production use iText or similar
        List<Map<String, Object>> data = fetchDataForExport(request);
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        pdf.append("Export: ").append(request.getType()).append("\n");
        pdf.append("Generated: ").append(LocalDateTime.now()).append("\n");
        pdf.append("Records: ").append(data.size()).append("\n\n");
        for (Map<String, Object> row : data) {
            pdf.append(row.toString()).append("\n");
        }
        pdf.append("%%EOF\n");
        return pdf.toString().getBytes();
    }

    private byte[] generateJson(ExportRequest request) {
        List<Map<String, Object>> data = fetchDataForExport(request);
        StringBuilder json = new StringBuilder();
        json.append("{\n  \"type\": \"").append(request.getType()).append("\",\n");
        json.append("  \"generatedAt\": \"").append(LocalDateTime.now()).append("\",\n");
        json.append("  \"count\": ").append(data.size()).append(",\n");
        json.append("  \"data\": [\n");
        for (int i = 0; i < data.size(); i++) {
            json.append("    ").append(mapToJson(data.get(i)));
            if (i < data.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n}");
        return json.toString().getBytes();
    }

    private byte[] generateZip(ExportRequest request) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            List<Map<String, Object>> data = fetchDataForExport(request);

            // Add CSV
            ZipEntry csvEntry = new ZipEntry("export.csv");
            zos.putNextEntry(csvEntry);
            byte[] csv = generateCsv(request);
            zos.write(csv);
            zos.closeEntry();

            // Add JSON
            ZipEntry jsonEntry = new ZipEntry("export.json");
            zos.putNextEntry(jsonEntry);
            byte[] json = generateJson(request);
            zos.write(json);
            zos.closeEntry();

            zos.finish();
            return baos.toByteArray();
        }
    }

    private List<Map<String, Object>> fetchDataForExport(ExportRequest request) {
        // In production, this would query the database based on the export type and filters
        // For now, return mock data
        List<Map<String, Object>> data = new ArrayList<>();

        switch (request.getType()) {
            case SOULS -> {
                for (int i = 1; i <= 10; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("nom", "Nom" + i);
                    row.put("prenom", "Prenom" + i);
                    row.put("telephone", "+33 6 " + String.format("%02d %02d %02d %02d", i, i, i, i));
                    row.put("email", "personne" + i + "@example.com");
                    row.put("dateNaissance", LocalDate.now().minusYears(20 + i).toString());
                    row.put("sexe", i % 2 == 0 ? "F" : "M");
                    row.put("statut", "ACTIF");
                    row.put("dateCreation", LocalDateTime.now().minusDays(i).toString());
                    data.add(row);
                }
            }
            case FAMILIES -> {
                for (int i = 1; i <= 5; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("nom", "Famille " + i);
                    row.put("adresse", i + " Rue de l'Eglise");
                    row.put("telephone", "+33 6 " + String.format("%02d %02d %02d %02d", i, i, i, i));
                    row.put("chefFamille", "Chef " + i);
                    row.put("nombreMembres", 3 + i);
                    row.put("dateCreation", LocalDateTime.now().minusDays(i * 10).toString());
                    data.add(row);
                }
            }
            case USERS -> {
                for (int i = 1; i <= 8; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("email", "user" + i + "@example.com");
                    row.put("firstName", "User");
                    row.put("lastName", "Test" + i);
                    row.put("role", i <= 2 ? "ADMIN" : i <= 4 ? "PASTEUR" : "FAISEUR");
                    row.put("actif", true);
                    row.put("dateCreation", LocalDateTime.now().minusDays(i * 5).toString());
                    data.add(row);
                }
            }
            case PAYMENTS -> {
                for (int i = 1; i <= 15; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("montant", 1000 + i * 500);
                    row.put("devise", "XOF");
                    row.put("type", i % 3 == 0 ? "DIME" : i % 3 == 1 ? "OFFRANDE" : "DON");
                    row.put("statut", i % 4 == 0 ? "EN_ATTENTE" : "CONFIRME");
                    row.put("reference", "REF" + String.format("%06d", i));
                    row.put("datePaiement", LocalDateTime.now().minusHours(i).toString());
                    row.put("donneur", "Donneur " + i);
                    data.add(row);
                }
            }
            case ATTENDANCE -> {
                for (int i = 1; i <= 20; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("personne", "Personne " + i);
                    row.put("evenement", "Culte du " + LocalDate.now().minusWeeks(i).toString());
                    row.put("date", LocalDate.now().minusWeeks(i).toString());
                    row.put("present", i % 3 != 0);
                    row.put("heureArrivee", "10:00");
                    row.put("departement", "Département " + (i % 5 + 1));
                    data.add(row);
                }
            }
            case REPORTS -> {
                for (int i = 1; i <= 10; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("titre", "Rapport semaine " + i);
                    row.put("auteur", "Auteur " + i);
                    row.put("type", i % 2 == 0 ? "HEBDOMADAIRE" : "MENSUEL");
                    row.put("statut", "SOUMIS");
                    row.put("dateCreation", LocalDateTime.now().minusDays(i * 7).toString());
                    data.add(row);
                }
            }
            case ALERTS -> {
                String[] types = {"ABSENCE_48H", "ABSENCE_3_SEMAINES", "RAPPORT_NON_SOUMIS", "ALERTE_ABSENCE"};
                for (int i = 1; i <= 12; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("type", types[i % types.length]);
                    row.put("titre", "Alerte " + i);
                    row.put("message", "Message d'alerte numero " + i);
                    row.put("cible", "PERSONNE");
                    row.put("priorite", i % 3 == 0 ? "URGENTE" : i % 3 == 1 ? "HAUTE" : "MOYENNE");
                    row.put("statut", i % 2 == 0 ? "ACTIVE" : "RESOLUE");
                    row.put("dateDeclenchement", LocalDateTime.now().minusDays(i).toString());
                    data.add(row);
                }
            }
            case NOTIFICATIONS -> {
                String[] types = {"INFORMATION", "ALERTE_ABSENCE", "TRANSFERT_DEMANDE", "PRIERE_EXAUCEE"};
                String[] canaux = {"IN_APP", "EMAIL", "PUSH"};
                for (int i = 1; i <= 15; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("type", types[i % types.length]);
                    row.put("titre", "Notification " + i);
                    row.put("message", "Contenu de la notification " + i);
                    row.put("canal", canaux[i % canaux.length]);
                    row.put("lu", i % 2 == 0);
                    row.put("createdAt", LocalDateTime.now().minusHours(i).toString());
                    data.add(row);
                }
            }
            case TRANSFERS -> {
                String[] types = {"DEPARTEMENT", "FAMILLE", "REGION", "ROLE"};
                String[] statuts = {"BROUILLON", "SOUMIS", "EN_ATTENTE_VALIDATION", "VALIDE", "EXECUTE"};
                for (int i = 1; i <= 10; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("type", types[i % types.length]);
                    row.put("personneNom", "Personne " + i);
                    row.put("ancienneAffectation", "Ancienne " + i);
                    row.put("nouvelleAffectation", "Nouvelle " + i);
                    row.put("statut", statuts[i % statuts.length]);
                    row.put("priorite", i % 3 == 0 ? "URGENTE" : "MOYENNE");
                    row.put("demandeur", "Demandeur " + i);
                    row.put("createdAt", LocalDateTime.now().minusDays(i * 3).toString());
                    data.add(row);
                }
            }
            case VOICE_REPORTS -> {
                for (int i = 1; i <= 8; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("titre", "Rapport vocal " + i);
                    row.put("transcription", "Transcription du rapport " + i);
                    row.put("duree", 60 + i * 30);
                    row.put("statut", i % 2 == 0 ? "TRAITE" : "EN_ATTENTE");
                    row.put("createdAt", LocalDateTime.now().minusDays(i).toString());
                    data.add(row);
                }
            }
            case EVENTS -> {
                for (int i = 1; i <= 10; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("titre", "Evenement " + i);
                    row.put("description", "Description de l'evenement " + i);
                    row.put("dateDebut", LocalDateTime.now().plusDays(i).toString());
                    row.put("dateFin", LocalDateTime.now().plusDays(i + 1).toString());
                    row.put("lieu", "Eglise principale");
                    row.put("type", i % 2 == 0 ? "CULTE" : "REUNION");
                    row.put("statut", "PLANIFIE");
                    data.add(row);
                }
            }
            case PRAYERS -> {
                for (int i = 1; i <= 12; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("titre", "Sujet de priere " + i);
                    row.put("description", "Description " + i);
                    row.put("categorie", i % 3 == 0 ? "SANTE" : i % 3 == 1 ? "FAMILLE" : "TRAVAIL");
                    row.put("statut", i % 4 == 0 ? "EXAUCEE" : "EN_COURS");
                    row.put("demandeur", "Demandeur " + i);
                    row.put("dateCreation", LocalDateTime.now().minusDays(i * 2).toString());
                    data.add(row);
                }
            }
            case COMPLIANCE_GDPR -> {
                for (int i = 1; i <= 5; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", UUID.randomUUID().toString());
                    row.put("userId", "user-" + i);
                    row.put("requestType", i % 2 == 0 ? "EXPORT" : "DELETE");
                    row.put("status", "COMPLETED");
                    row.put("createdAt", LocalDateTime.now().minusDays(i * 10).toString());
                    row.put("processedAt", LocalDateTime.now().minusDays(i * 10 + 1).toString());
                    data.add(row);
                }
            }
        }

        // Apply date filters if provided
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            // In real implementation, filter by date
        }

        return data;
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String mapToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) json.append(", ");
            json.append("\"").append(entry.getKey()).append("\": ");
            Object val = entry.getValue();
            if (val instanceof String) {
                json.append("\"").append(val).append("\"");
            } else if (val instanceof Number || val instanceof Boolean) {
                json.append(val);
            } else {
                json.append("\"").append(val).append("\"");
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    @Override
    public String getMimeType(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> "text/csv";
            case "excel", "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pdf" -> "application/pdf";
            case "json" -> "application/json";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    @Override
    public String getFileExtension(String format) {
        return switch (format.toLowerCase()) {
            case "csv" -> "csv";
            case "excel", "xlsx" -> "xlsx";
            case "pdf" -> "pdf";
            case "json" -> "json";
            case "zip" -> "zip";
            default -> "csv";
        };
    }
}