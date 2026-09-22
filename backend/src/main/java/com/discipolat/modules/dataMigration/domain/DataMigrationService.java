package com.discipolat.modules.dataMigration.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.imports.domain.ImportResult;
import com.discipolat.modules.imports.domain.ImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P3 #101 — Assistant de migration de données avec mapping intelligent des champs.
 */
@Service
@Transactional
public class DataMigrationService {

        private final DataMigrationJobRepository repository;
    private final ImportService importService;

    public DataMigrationService(DataMigrationJobRepository repository, ImportService importService) {
        this.repository = repository;
        this.importService = importService;
    }

    /** Champs cibles connus par type d'import (champ -> synonymes normalisés). */
    private static final Map<String, Map<String, List<String>>> TARGET_FIELDS = Map.of(
            "SOULS", Map.of(
                    "nom", List.of("nom", "nomdefamille", "lastname", "surname", "famille"),
                    "prenom", List.of("prenom", "firstname", "givenname", "postnom"),
                    "email", List.of("email", "mail", "adresseemail", "courriel"),
                    "telephone", List.of("telephone", "tel", "phone", "mobile", "portable"),
                    "adresse", List.of("adresse", "address", "residence", "quartier", "zone"),
                    "dateNaissance", List.of("datenaissance", "birthday", "birthdate"),
                    "profession", List.of("profession", "metier", "occupation", "emploi"),
                    "situationFamiliale", List.of("situationfamiliale", "statutmarital")),
            "MEMBERS", Map.of(
                    "nom", List.of("nom", "nomdefamille", "lastname", "surname"),
                    "prenom", List.of("prenom", "firstname", "givenname"),
                    "email", List.of("email", "mail", "courriel"),
                    "telephone", List.of("telephone", "tel", "phone", "mobile"),
                    "departement", List.of("departement", "department", "service", "ministere"),
                    "role", List.of("role", "fonction", "responsabilite", "poste")),
            "FAMILIES", Map.of(
                    "nom", List.of("nom", "nomfamille", "familyname", "noms"),
                    "chefFamille", List.of("cheffamille", "chef", "leader", "responsable"),
                    "zone", List.of("zone", "quartier", "secteur", "localisation"),
                    "telephone", List.of("telephone", "tel", "phone")),
            "EVENTS", Map.of(
                    "titre", List.of("titre", "title", "nomevenement", "libelle"),
                    "typeEvenement", List.of("type", "categorie", "category"),
                    "lieu", List.of("lieu", "location", "endroit", "venue"),
                    "dateDebut", List.of("datedebut", "startdate", "debut", "date"),
                    "description", List.of("description", "details", "notes")),
            "FINANCES", Map.of(
                    "montant", List.of("montant", "amount", "somme", "valeur"),
                    "devise", List.of("devise", "currency", "monnaie"),
                    "typeTransaction", List.of("type", "categorie", "nature"),
                    "dateTransaction", List.of("date", "datetransaction")));

    /**
     * Analyse les en-têtes du fichier et propose un mapping intelligent.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> analyze(String targetType, List<String> headers, List<Map<String, String>> sampleRows) {
        Map<String, List<String>> targets = TARGET_FIELDS.getOrDefault(
                targetType == null ? "SOULS" : targetType.toUpperCase(), TARGET_FIELDS.get("SOULS"));

        List<Map<String, Object>> suggestions = new ArrayList<>();
        Set<String> mappedTargets = new HashSet<>();

        for (String header : headers) {
            String normalized = normalize(header);
            String bestTarget = null;
            double bestScore = 0.0;

            for (Map.Entry<String, List<String>> e : targets.entrySet()) {
                if (mappedTargets.contains(e.getKey())) continue;
                for (String synonym : e.getValue()) {
                    double score = similarity(normalized, synonym);
                    if (score > bestScore) {
                        bestScore = score;
                        bestTarget = e.getKey();
                    }
                }
            }

            Map<String, Object> s = new LinkedHashMap<>();
            s.put("sourceColumn", header);
            s.put("suggestedTarget", bestScore >= 0.5 ? bestTarget : null);
            s.put("confidence", Math.round(bestScore * 100) / 100.0);
            if (bestScore >= 0.5) mappedTargets.add(bestTarget);

            if (sampleRows != null && !sampleRows.isEmpty()) {
                LinkedHashSet<String> preview = new LinkedHashSet<>();
                for (Map<String, String> row : sampleRows) {
                    String v = row.get(header);
                    if (v != null && !v.isBlank()) preview.add(v.trim());
                    if (preview.size() >= 3) break;
                }
                s.put("sampleValues", new ArrayList<>(preview));
            }
            suggestions.add(s);
        }

        long autoMapped = suggestions.stream().filter(s -> s.get("suggestedTarget") != null).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("targetType", targetType);
        result.put("suggestions", suggestions);
        result.put("totalColumns", headers.size());
        result.put("autoMappedColumns", autoMapped);
        result.put("unmappedColumns", headers.size() - autoMapped);
        result.put("availableTargets", targets.keySet());
        return result;
    }

    public DataMigrationJob create(DataMigrationJob job) {
        if (job.getTenantId() == null) {
            job.setTenantId(com.discipolat.common.multitenancy.TenantContext.getTenantId());
        }
        job.setCreatedAt(java.time.LocalDateTime.now());
        return repository.save(job);
    }

    public DataMigrationJob execute(UUID id, String mapping, int totalRows, int importedRows, String errorsLog) {
        DataMigrationJob job = getById(id);
        job.setFieldMapping(mapping);
        job.setTotalRows(totalRows);
        job.setImportedRows(importedRows);
        job.setErrorRows(Math.max(0, totalRows - importedRows));
        job.setErrorsLog(errorsLog);
        int threshold = totalRows <= 0 ? 0 : Math.max(1, totalRows / 10); // tolérance 10 %
        job.setStatus(importedRows > 0 && importedRows >= totalRows - threshold
                ? DataMigrationJob.Status.COMPLETED : DataMigrationJob.Status.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        return repository.save(job);
    }

    public void cancel(UUID id) {
        DataMigrationJob job = getById(id);
        job.setStatus(DataMigrationJob.Status.CANCELLED);
        job.setCompletedAt(LocalDateTime.now());
        repository.save(job);
    }

        @Transactional(readOnly = true)
    public Map<String, Object> analyze(UUID jobId, List<String[]> rows) {
        DataMigrationJob job = getById(jobId);
        if (rows == null || rows.isEmpty()) {
            throw new BusinessRuleException("Le fichier est vide.");
        }
        List<String> headers = Arrays.stream(rows.get(0)).map(DataMigrationService::cleanCsvCell).toList();
        List<Map<String, String>> preview = rows.subList(1, rows.size()).stream()
                .limit(10)
                .map(line -> toMap(headers, line))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("jobId", jobId);
        result.put("targetType", job.getTargetType());
        result.put("totalRows", Math.max(0, rows.size() - 1));
        result.put("headers", headers);
        result.put("preview", preview);
        result.put("analysis", analyzeHeaders(job.getTargetType(), headers,
                preview.stream().limit(5).toList()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<DataMigrationJob> list() {
        return repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.getCurrentTenantId());
    }

        /** G4.6 — execute réellement l'import via ImportService + traçabilité pour rollback. */
    public DataMigrationResult execute(UUID jobId, List<String[]> rows, boolean dryRun) {
        DataMigrationJob job = getById(jobId);
        if (rows == null || rows.isEmpty()) {
            throw new BusinessRuleException("Le fichier est vide.");
        }
        List<String> headers = Arrays.stream(rows.get(0)).map(DataMigrationService::cleanCsvCell).toList();
        List<Map<String, String>> data = rows.subList(1, rows.size()).stream()
                .map(line -> toMap(headers, line))
                .toList();
        int totalRows = data.size();

        String target = job.getTargetType() == null ? "" : job.getTargetType().toUpperCase();
        ImportResult importResult;
        if (dryRun) {
            importResult = ImportResult.builder()
                    .imported(0).skipped(totalRows)
                    .errors(List.of("Mode simulation — aucune écriture effectuée."))
                    .build();
        } else {
            importResult = switch (target) {
                case "SOULS", "MEMBERS" -> importService.importSouls(data);
                case "FAMILIES" -> importService.importFamilies(data);
                case "USERS" -> importService.importUsers(data);
                default -> throw new BusinessRuleException("Type cible d'import non supporté: " + target);
            };
        }

        String createdIds = importResult.getImportedIds() == null ? ""
                : importResult.getImportedIds().stream().filter(Objects::nonNull)
                    .map(UUID::toString).collect(Collectors.joining(","));
        job.setCreatedEntityIds(createdIds);
        job.setLastRunDry(dryRun);
        job.setTotalRows(totalRows);
        job.setImportedRows(importResult.getImported());
        job.setErrorRows(importResult.getSkipped());
        job.setErrorsLog(importResult.getErrors() == null ? "" : String.join("\n", importResult.getErrors()));
        job.setStatus(dryRun ? DataMigrationJob.Status.COMPLETED
                : (importResult.getImported() > 0 ? DataMigrationJob.Status.COMPLETED
                                                   : DataMigrationJob.Status.FAILED));
        job.setCompletedAt(LocalDateTime.now());
        repository.save(job);

        return DataMigrationResult.builder()
                .jobId(jobId).status(job.getStatus().name()).dryRun(dryRun)
                .totalRows(totalRows).importedRows(importResult.getImported())
                .errorRows(importResult.getSkipped()).errorsLog(importResult.getErrors())
                .analysis(analyzeHeaders(target, headers, data.subList(0, Math.min(5, data.size()))))
                .build();
    }

    /** G4.6 — relit le fichier et ré‑exécute l'import (utile après correction de mapping). */
    public DataMigrationResult replay(UUID jobId, List<String[]> rows) {
        return execute(jobId, rows, false);
    }

    /** G4.6 — rollback des entités créées par le job précédemment exécuté. */
    public DataMigrationResult rollback(UUID jobId) {
        DataMigrationJob job = getById(jobId);
        int deleted = importService.rollback(job);
        repository.save(job);
        return DataMigrationResult.builder()
                .jobId(jobId).status(job.getStatus().name()).dryRun(false)
                .deleted(deleted)
                .errorsLog(List.of("Rollback effectué: " + deleted + " entité(s) désactivée(s)."))
                .build();
    }

    private List<Map<String, String>> analyzeHeaders(String targetType, List<String> headers,
                                                      List<Map<String, String>> sample) {
        return headers.stream().map(h -> {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("header", h);
            entry.put("suggestedTarget", matchField(targetType, h));
            return entry;
        }).collect(Collectors.toList());
    }

    private String matchField(String targetType, String header) {
        Map<String, List<String>> targets = TARGET_FIELDS.get(targetType == null ? "" : targetType.toUpperCase());
        if (targets == null) return null;
        String norm = normalize(header);
        double best = 0.0;
        String match = null;
        for (Map.Entry<String, List<String>> e : targets.entrySet()) {
            for (String syn : e.getValue()) {
                double sim = similarity(norm, normalize(syn));
                if (sim > best) { best = sim; match = e.getKey(); }
            }
        }
        return best > 0.55 ? match : null;
    }

    @Transactional(readOnly = true)
    public DataMigrationJob getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new EntityNotFoundException("DataMigrationJob", id));
    }

        /** Convertit une ligne CSV (sans headers) en map {header -> valeur}. */
    private static Map<String, String> toMap(List<String> headers, String[] line) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.size() && i < line.length; i++) {
            map.put(headers.get(i), cleanCsvCell(line[i]));
        }
        return map;
    }

    private static String cleanCsvCell(String cell) {
        if (cell == null) return "";
        String c = cell.trim();
        if (c.startsWith("\"") && c.endsWith("\"") && c.length() >= 2) {
            c = c.substring(1, c.length() - 1);
        }
        return c.trim();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.toLowerCase().trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-z0-9]", "");
    }

    /** Score combiné : égalité exacte, préfixe/contenu, distance de Levenshtein. */
    private static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        if (a.startsWith(b) || b.startsWith(a)) return 0.9;
        if (a.contains(b) || b.contains(a)) return 0.8;
        int dist = levenshtein(a, b);
        return Math.max(0, 1.0 - (double) dist / Math.max(a.length(), b.length()));
    }

    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j] + 1,
                        Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost));
            }
        }
        return dp[a.length()][b.length()];
    }
}
