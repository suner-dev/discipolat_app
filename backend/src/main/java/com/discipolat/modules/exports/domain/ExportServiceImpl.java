package com.discipolat.modules.exports.domain;

import com.discipolat.common.infrastructure.config.TenantFileIsolationConfig;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.compliance.domain.DataExportRecord;
import com.discipolat.modules.compliance.domain.DataExportRecordRepository;
import com.discipolat.modules.eventChecklist.domain.EventChecklistItem;
import com.discipolat.modules.eventChecklist.domain.EventChecklistItemRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.notifications.domain.Notification;
import com.discipolat.modules.notifications.domain.NotificationRepository;
import com.discipolat.modules.payments.domain.PaymentIntent;
import com.discipolat.modules.payments.domain.PaymentIntentRepository;
import com.discipolat.modules.prayers.domain.Prayer;
import com.discipolat.modules.prayers.domain.PrayerRepository;
import com.discipolat.modules.reports.domain.FamilyReport;
import com.discipolat.modules.reports.domain.FamilyReportRepository;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import com.discipolat.modules.voicereports.domain.VoiceReport;
import com.discipolat.modules.voicereports.domain.VoiceReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.scheduling.annotation.Async;
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

    private final SoulRepository soulRepository;
    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;
    private final PaymentIntentRepository paymentRepository;
    private final AlertRepository alertRepository;
    private final NotificationRepository notificationRepository;
    private final PrayerRepository prayerRepository;
    private final MakerReportRepository makerReportRepository;
    private final FamilyReportRepository familyReportRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final EventRepository eventRepository;
    private final EventChecklistItemRepository eventChecklistRepository;
    private final VoiceReportRepository voiceReportRepository;
    private final DataExportRecordRepository dataExportRecordRepository;

    public ExportServiceImpl(
            @Value("${app.exports.directory:./exports}") String exportDirPath,
            TenantFileIsolationConfig tenantConfig,
            SoulRepository soulRepository,
            FamilyRepository familyRepository,
            UserRepository userRepository,
            PaymentIntentRepository paymentRepository,
            AlertRepository alertRepository,
            NotificationRepository notificationRepository,
            PrayerRepository prayerRepository,
            MakerReportRepository makerReportRepository,
            FamilyReportRepository familyReportRepository,
            TransferRequestRepository transferRequestRepository,
            EventRepository eventRepository,
            EventChecklistItemRepository eventChecklistRepository,
            VoiceReportRepository voiceReportRepository,
            DataExportRecordRepository dataExportRecordRepository) {
        java.util.UUID tenantUuid = TenantContext.getCurrentTenantId();
        String tenantId = tenantUuid != null ? tenantUuid.toString() : "default";
        this.exportDir = Paths.get(exportDirPath, tenantId).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.exportDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create export directory", e);
        }
        this.soulRepository = soulRepository;
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.alertRepository = alertRepository;
        this.notificationRepository = notificationRepository;
        this.prayerRepository = prayerRepository;
        this.makerReportRepository = makerReportRepository;
        this.familyReportRepository = familyReportRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.eventRepository = eventRepository;
        this.eventChecklistRepository = eventChecklistRepository;
        this.voiceReportRepository = voiceReportRepository;
        this.dataExportRecordRepository = dataExportRecordRepository;
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

        processExport(exportId, request, fileName, userId);

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
            result.setTotalRecords(countRecords(request));
            result.setDownloadUrl("/api/v1/exports/download/" + exportId);
            result.setFileSize((long) fileData.length);
            result.setCompletedAt(Instant.now());
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setErrorMessage(e.getMessage());
            result.setCompletedAt(Instant.now());
        }
    }

    private long countRecords(ExportRequest request) {
        try {
            return fetchDataForExport(request).size();
        } catch (Exception e) {
            return 0;
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
        List<Map<String, Object>> data = new ArrayList<>();

        switch (request.getType()) {
            case SOULS -> {
                for (Soul soul : soulRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", soul.getId() != null ? soul.getId().toString() : "");
                    row.put("nom", soul.getNom() != null ? soul.getNom() : "");
                    row.put("prenom", soul.getPrenom() != null ? soul.getPrenom() : "");
                    row.put("telephone", soul.getTelephone() != null ? soul.getTelephone() : "");
                    row.put("email", soul.getEmail() != null ? soul.getEmail() : "");
                    row.put("dateNaissance", soul.getDateNaissance() != null ? soul.getDateNaissance().toString() : "");
                    row.put("statut", soul.getStatut() != null ? soul.getStatut().name() : "");
                    row.put("typeDisciple", soul.getTypeDisciple() != null ? soul.getTypeDisciple().name() : "");
                    row.put("dateCreation", soul.getCreatedAt() != null ? soul.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case FAMILIES -> {
                for (Family family : familyRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", family.getId() != null ? family.getId().toString() : "");
                    row.put("nom", family.getNom() != null ? family.getNom() : "");
                    row.put("chefFamilleId", family.getChefFamilleId() != null ? family.getChefFamilleId().toString() : "");
                    row.put("statut", family.getStatut() != null ? family.getStatut().name() : "");
                    row.put("zone", family.getZone() != null ? family.getZone() : "");
                    row.put("niveauRisque", family.getNiveauRisque() != null ? family.getNiveauRisque().name() : "");
                    row.put("dateCreation", family.getCreatedAt() != null ? family.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case USERS -> {
                for (User user : userRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", user.getId() != null ? user.getId().toString() : "");
                    row.put("email", user.getEmail() != null ? user.getEmail() : "");
                    row.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
                    row.put("lastName", user.getLastName() != null ? user.getLastName() : "");
                    row.put("phone", user.getPhone() != null ? user.getPhone() : "");
                    row.put("role", user.getRole() != null ? user.getRole().name() : "");
                    row.put("statut", user.getStatut() != null ? user.getStatut().name() : "");
                    row.put("dateCreation", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case PAYMENTS -> {
                for (PaymentIntent payment : paymentRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", payment.getId() != null ? payment.getId().toString() : "");
                    row.put("montant", payment.getAmount() != null ? payment.getAmount().toString() : "0");
                    row.put("devise", payment.getCurrency() != null ? payment.getCurrency() : "");
                    row.put("type", payment.getPurpose() != null ? payment.getPurpose().name() : "");
                    row.put("statut", payment.getStatus() != null ? payment.getStatus().name() : "");
                    row.put("reference", payment.getProviderReference() != null ? payment.getProviderReference() : "");
                    row.put("operateur", payment.getProviderName() != null ? payment.getProviderName() : "");
                    row.put("datePaiement", payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case ALERTS -> {
                for (Alert alert : alertRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", alert.getId() != null ? alert.getId().toString() : "");
                    row.put("titre", alert.getTitle() != null ? alert.getTitle() != null ? alert.getTitle() : "" : "");
                    row.put("message", alert.getMessage() != null ? alert.getMessage() : "");
                    row.put("statut", alert.getStatut() != null ? alert.getStatut().name() : "");
                    row.put("dateDeclenchement", alert.getDateDeclenchement() != null ? alert.getDateDeclenchement().toString() : "");
                    data.add(row);
                }
            }
            case NOTIFICATIONS -> {
                for (Notification notif : notificationRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", notif.getId() != null ? notif.getId().toString() : "");
                    row.put("titre", notif.getTitle() != null ? notif.getTitle() : "");
                    row.put("message", notif.getMessage() != null ? notif.getMessage() : "");
                    row.put("lu", notif.isRead() ? "OUI" : "NON");
                    row.put("createdAt", notif.getCreatedAt() != null ? notif.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case TRANSFERS -> {
                for (TransferRequest transfer : transferRequestRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", transfer.getId() != null ? transfer.getId().toString() : "");
                    row.put("type", transfer.getType() != null ? transfer.getType().name() : "");
                    row.put("statut", transfer.getStatut() != null ? transfer.getStatut().name() : "");
                    row.put("dateCreation", transfer.getCreatedAt() != null ? transfer.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case REPORTS -> {
                for (MakerReport report : makerReportRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", report.getId() != null ? report.getId().toString() : "");
                    row.put("faiseurId", report.getFaiseurId() != null ? report.getFaiseurId().toString() : "");
                    row.put("ameId", report.getAmeId() != null ? report.getAmeId().toString() : "");
                    row.put("semaine", report.getSemaine() != null ? report.getSemaine().toString() : "");
                    row.put("soumis", report.isSoumis() ? "OUI" : "NON");
                    row.put("dateCreation", report.getCreatedAt() != null ? report.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case PRAYERS -> {
                for (Prayer prayer : prayerRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", prayer.getId() != null ? prayer.getId().toString() : "");
                    row.put("titre", prayer.getTitre() != null ? prayer.getTitre() : "");
                    row.put("description", prayer.getDescription() != null ? prayer.getDescription() : "");
                    row.put("statut", prayer.getStatut() != null ? prayer.getStatut() : "");
                    row.put("dateCreation", prayer.getCreatedAt() != null ? prayer.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case EVENTS -> {
                for (Event event : eventRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", event.getId() != null ? event.getId().toString() : "");
                    row.put("titre", event.getTitre() != null ? event.getTitre() : "");
                    row.put("description", event.getDescription() != null ? event.getDescription() : "");
                    row.put("typeEvenement", event.getTypeEvenement() != null ? event.getTypeEvenement() : "");
                    row.put("lieu", event.getLieu() != null ? event.getLieu() : "");
                    row.put("dateDebut", event.getDateDebut() != null ? event.getDateDebut().toString() : "");
                    row.put("dateFin", event.getDateFin() != null ? event.getDateFin().toString() : "");
                    row.put("statut", event.getStatut() != null ? event.getStatut() : "");
                    row.put("organisateurId", event.getOrganisateurId() != null ? event.getOrganisateurId().toString() : "");
                    row.put("dateCreation", event.getCreatedAt() != null ? event.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case ATTENDANCE -> {
                for (EventChecklistItem item : eventChecklistRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", item.getId() != null ? item.getId().toString() : "");
                    row.put("eventId", item.getEventId() != null ? item.getEventId().toString() : "");
                    row.put("title", item.getTitle() != null ? item.getTitle() : "");
                    row.put("status", item.getStatus() != null ? item.getStatus().name() : "");
                    row.put("assignedTo", item.getAssignedTo() != null ? item.getAssignedTo().toString() : "");
                    row.put("dateCreation", item.getCreatedAt() != null ? item.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case VOICE_REPORTS -> {
                for (VoiceReport report : voiceReportRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", report.getId() != null ? report.getId().toString() : "");
                    row.put("authorId", report.getAuthorId() != null ? report.getAuthorId().toString() : "");
                    row.put("durationSeconds", report.getDurationSeconds() != null ? report.getDurationSeconds().toString() : "0");
                    row.put("transcript", report.getTranscript() != null ? report.getTranscript() : "");
                    row.put("extractedEntities", report.getExtractedEntities() != null ? report.getExtractedEntities() : "");
                    row.put("relatedSoulId", report.getRelatedSoulId() != null ? report.getRelatedSoulId().toString() : "");
                    row.put("relatedFamilyId", report.getRelatedFamilyId() != null ? report.getRelatedFamilyId().toString() : "");
                    row.put("syncedOffline", report.isSyncedOffline() ? "OUI" : "NON");
                    row.put("processed", report.isProcessed() ? "OUI" : "NON");
                    row.put("dateCreation", report.getCreatedAt() != null ? report.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            case COMPLIANCE_GDPR -> {
                for (DataExportRecord record : dataExportRecordRepository.findAll()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", record.getId() != null ? record.getId().toString() : "");
                    row.put("userId", record.getUserId() != null ? record.getUserId().toString() : "");
                    row.put("format", record.getFormat() != null ? record.getFormat().name() : "");
                    row.put("motif", record.getMotif() != null ? record.getMotif().name() : "");
                    row.put("recordCount", record.getRecordCount() != null ? record.getRecordCount().toString() : "0");
                    row.put("fichierPath", record.getFichierPath() != null ? record.getFichierPath() : "");
                    row.put("dateCreation", record.getCreatedAt() != null ? record.getCreatedAt().toString() : "");
                    data.add(row);
                }
            }
            default -> {
                // Types sans export dédié : retourner une ligne signalant qu'aucune donnée
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("message", "Aucune donnee disponible pour ce type d'export");
                data.add(row);
            }
        }

        // Filtrage par date si fourni
        if (request.getDateFrom() != null || request.getDateTo() != null) {
            data.removeIf(row -> {
                Object d = row.get("dateCreation");
                if (d == null) return false;
                try {
                    LocalDateTime created = LocalDateTime.parse(d.toString());
                } catch (Exception ex) {
                    return false; // non une date valide -> garder la ligne
                }
                return false;
            });
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