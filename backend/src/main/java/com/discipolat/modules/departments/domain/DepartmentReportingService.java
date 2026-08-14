package com.discipolat.modules.departments.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.api.DepartmentChecklistItemRequest;
import com.discipolat.modules.departments.api.DepartmentChecklistRequest;
import com.discipolat.modules.departments.api.DepartmentDocumentRequest;
import com.discipolat.modules.departments.api.DepartmentEquipmentRequest;
import com.discipolat.modules.departments.api.DepartmentReportRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reporting & outils de gestion du département :
 * <ul>
 *   <li><b>Rapports de département</b> : génération de synthèses sur les
 *       données réelles (effectif, présence, tâches, discipline, équipes,
 *       événements), sauvegarde, consultation, export CSV.</li>
 *   <li><b>Checklists</b> : listes de contrôle attachées à une cible
 *       (tâche, événement, équipe, membre) ou générales.</li>
 *   <li><b>Inventaire matériel</b> : équipements du département, état,
 *       responsable, affectation, localisation.</li>
 * </ul>
 * L'accès est validé par {@link DepartmentService#findById} (un responsable
 * ne gère que ses départements).
 */
@Service
public class DepartmentReportingService {

    private final DepartmentService departmentService;
    private final DepartmentDossierService dossierService;
    private final DepartmentReportRepository reportRepository;
    private final DepartmentChecklistRepository checklistRepository;
    private final DepartmentChecklistItemRepository checklistItemRepository;
    private final DepartmentEquipmentRepository equipmentRepository;
    private final DepartmentDocumentRepository documentRepository;
    private final DepartmentMemberObjectiveRepository objectiveRepository;
    private final SecurityUtils securityUtils;

    public DepartmentReportingService(DepartmentService departmentService,
                                      DepartmentDossierService dossierService,
                                      DepartmentReportRepository reportRepository,
                                      DepartmentChecklistRepository checklistRepository,
                                      DepartmentChecklistItemRepository checklistItemRepository,
                                      DepartmentEquipmentRepository equipmentRepository,
                                      DepartmentDocumentRepository documentRepository,
                                      DepartmentMemberObjectiveRepository objectiveRepository,
                                      SecurityUtils securityUtils) {
        this.departmentService = departmentService;
        this.dossierService = dossierService;
        this.reportRepository = reportRepository;
        this.checklistRepository = checklistRepository;
        this.checklistItemRepository = checklistItemRepository;
        this.equipmentRepository = equipmentRepository;
        this.documentRepository = documentRepository;
        this.objectiveRepository = objectiveRepository;
        this.securityUtils = securityUtils;
    }

    private Department requireDepartment(UUID departmentId) {
        return departmentService.findById(departmentId);
    }

    // ========================================================================
    // RAPPORTS DE DÉPARTEMENT
    // ========================================================================

    /** Génère une synthèse du département sur les données réelles et la sauvegarde. */
    @Transactional
    public Map<String, Object> generateReport(UUID departmentId, DepartmentReportRequest request) {
        requireDepartment(departmentId);
        DepartmentReport.ReportType type = parseType(request.type());
        LocalDate[] period = periodFor(type, request.periodeDebut(), request.periodeFin());
        LocalDate debut = period[0];
        LocalDate fin = period[1];

        Map<String, Object> stats = dossierService.getDepartmentStats(departmentId);
        String contenu = buildSynthesis(departmentId, type, debut, fin, stats);

        String titre = request.titre() != null && !request.titre().isBlank()
                ? request.titre()
                : titreAuto(type, debut, fin);

        DepartmentReport.ReportStatus statut = "SOUMIS".equalsIgnoreCase(request.statut())
                ? DepartmentReport.ReportStatus.SOUMIS
                : DepartmentReport.ReportStatus.BROUILLON;

        DepartmentReport report = DepartmentReport.builder()
                .departmentId(departmentId)
                .auteurId(securityUtils.getCurrentUserId())
                .type(type)
                .titre(titre)
                .periodeDebut(debut)
                .periodeFin(fin)
                .contenu(contenu)
                .statut(statut)
                .build();
        reportRepository.save(report);

        return toReportMap(report);
    }

    /** Sauvegarde un rapport rédigé manuellement. */
    @Transactional
    public Map<String, Object> saveManualReport(UUID departmentId, DepartmentReportRequest request) {
        requireDepartment(departmentId);
        if (request.contenu() == null || request.contenu().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le contenu du rapport est requis");
        }
        DepartmentReport.ReportType type = parseType(request.type());
        DepartmentReport.ReportStatus statut = "SOUMIS".equalsIgnoreCase(request.statut())
                ? DepartmentReport.ReportStatus.SOUMIS
                : DepartmentReport.ReportStatus.BROUILLON;

        DepartmentReport report = DepartmentReport.builder()
                .departmentId(departmentId)
                .auteurId(securityUtils.getCurrentUserId())
                .type(type)
                .titre(request.titre() != null ? request.titre() : "Rapport " + type.name().toLowerCase(Locale.ROOT))
                .periodeDebut(request.periodeDebut())
                .periodeFin(request.periodeFin())
                .contenu(request.contenu())
                .statut(statut)
                .build();
        reportRepository.save(report);
        return toReportMap(report);
    }

    /** Modifie un rapport sauvegardé (contenu, statut, période, titre). */
    @Transactional
    public Map<String, Object> updateReport(UUID departmentId, UUID reportId, DepartmentReportRequest request) {
        requireDepartment(departmentId);
        DepartmentReport report = reportRepository.findById(reportId)
                .filter(r -> r.getDepartmentId().equals(departmentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rapport introuvable"));
        if (request.titre() != null && !request.titre().isBlank()) report.setTitre(request.titre().trim());
        if (request.contenu() != null && !request.contenu().isBlank()) report.setContenu(request.contenu());
        if (request.periodeDebut() != null) report.setPeriodeDebut(request.periodeDebut());
        if (request.periodeFin() != null) report.setPeriodeFin(request.periodeFin());
        if (request.statut() != null) {
            report.setStatut("SOUMIS".equalsIgnoreCase(request.statut())
                    ? DepartmentReport.ReportStatus.SOUMIS
                    : "ARCHIVE".equalsIgnoreCase(request.statut())
                            ? DepartmentReport.ReportStatus.ARCHIVE
                            : DepartmentReport.ReportStatus.BROUILLON);
        }
        reportRepository.save(report);
        return toReportMap(report);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listReports(UUID departmentId) {
        requireDepartment(departmentId);
        return reportRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId).stream()
                .map(this::toReportMap)
                .toList();
    }

    @Transactional
    public void deleteReport(UUID departmentId, UUID reportId) {
        requireDepartment(departmentId);
        DepartmentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rapport introuvable"));
        if (!report.getDepartmentId().equals(departmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rapport introuvable");
        }
        reportRepository.delete(report);
    }

    /** Export CSV du rapport (synthèse + période). */
    @Transactional(readOnly = true)
    public String exportReportCsv(UUID departmentId, UUID reportId) {
        requireDepartment(departmentId);
        DepartmentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rapport introuvable"));
        if (!report.getDepartmentId().equals(departmentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rapport introuvable");
        }
        String departmentNom = requireDepartment(departmentId).getNom();
        StringBuilder sb = new StringBuilder();
        sb.append("Rapport;").append(escapeCsv(report.getTitre())).append('\n');
        sb.append("Département;").append(escapeCsv(departmentNom)).append('\n');
        sb.append("Type;").append(report.getType().name()).append('\n');
        sb.append("Période;").append(report.getPeriodeDebut()).append(" -> ").append(report.getPeriodeFin()).append('\n');
        sb.append("Statut;").append(report.getStatut().name()).append('\n');
        sb.append("Généré le;").append(report.getCreatedAt()).append('\n');
        sb.append('\n');
        sb.append("Contenu\n");
        for (String line : report.getContenu().split("\n")) {
            sb.append(escapeCsv(line)).append('\n');
        }
        return sb.toString();
    }

    // ========================================================================
    // CHECKLISTS
    // ========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listChecklists(UUID departmentId, String cibleType, UUID cibleId) {
        requireDepartment(departmentId);
        List<DepartmentChecklist> checklists;
        if (cibleType != null && cibleId != null) {
            checklists = checklistRepository.findByCibleTypeAndCibleIdOrderByCreatedAtDesc(
                    DepartmentChecklist.CibleType.valueOf(cibleType), cibleId);
        } else {
            checklists = checklistRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
        }
        return checklists.stream()
                .map(c -> toChecklistMap(c, checklistItemRepository.findByChecklistIdOrderByOrdreAsc(c.getId())))
                .toList();
    }

    @Transactional
    public Map<String, Object> createChecklist(UUID departmentId, DepartmentChecklistRequest request) {
        requireDepartment(departmentId);
        DepartmentChecklist checklist = DepartmentChecklist.builder()
                .departmentId(departmentId)
                .titre(request.titre())
                .cibleType(parseCibleType(request.cibleType()))
                .cibleId(request.cibleId())
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        checklist = checklistRepository.save(checklist);

        List<DepartmentChecklistItem> items = new ArrayList<>();
        if (request.items() != null) {
            int ordre = 0;
            for (String libelle : request.items()) {
                if (libelle == null || libelle.isBlank()) continue;
                items.add(checklistItemRepository.save(DepartmentChecklistItem.builder()
                        .checklistId(checklist.getId())
                        .libelle(libelle)
                        .ordre(ordre++)
                        .build()));
            }
        }
        return toChecklistMap(checklist, items);
    }

    @Transactional
    public Map<String, Object> updateChecklist(UUID departmentId, UUID checklistId, DepartmentChecklistRequest request) {
        requireDepartment(departmentId);
        DepartmentChecklist checklist = checklistRepository.findByIdAndDepartmentId(checklistId, departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist introuvable"));
        if (request.titre() != null && !request.titre().isBlank()) checklist.setTitre(request.titre());
        if (request.statut() != null && !request.statut().isBlank()) {
            checklist.setStatut(DepartmentChecklist.ChecklistStatus.valueOf(request.statut()));
        }
        checklistRepository.save(checklist);
        return toChecklistMap(checklist, checklistItemRepository.findByChecklistIdOrderByOrdreAsc(checklistId));
    }

    @Transactional
    public Map<String, Object> addChecklistItem(UUID departmentId, UUID checklistId, DepartmentChecklistItemRequest request) {
        requireDepartment(departmentId);
        if (request.libelle() == null || request.libelle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le libellé de l'élément est requis");
        }
        DepartmentChecklist checklist = checklistRepository.findByIdAndDepartmentId(checklistId, departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist introuvable"));
        if (checklist.getStatut() == DepartmentChecklist.ChecklistStatus.TERMINEE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Checklist terminée : ajout impossible");
        }
        List<DepartmentChecklistItem> items = checklistItemRepository.findByChecklistIdOrderByOrdreAsc(checklistId);
        int ordre = items.isEmpty() ? 0 : items.get(items.size() - 1).getOrdre() + 1;
        DepartmentChecklistItem item = checklistItemRepository.save(DepartmentChecklistItem.builder()
                .checklistId(checklistId)
                .libelle(request.libelle())
                .ordre(ordre)
                .build());
        items.add(item);
        return toChecklistMap(checklist, items);
    }

    @Transactional
    public Map<String, Object> toggleChecklistItem(UUID departmentId, UUID checklistId, UUID itemId, DepartmentChecklistItemRequest request) {
        requireDepartment(departmentId);
        DepartmentChecklist checklist = checklistRepository.findByIdAndDepartmentId(checklistId, departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist introuvable"));
        DepartmentChecklistItem item = checklistItemRepository.findById(itemId)
                .filter(i -> i.getChecklistId().equals(checklistId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément introuvable"));
        if (request.libelle() != null && !request.libelle().isBlank()) item.setLibelle(request.libelle());
        if (request.fait() != null) item.setFait(request.fait());
        checklistItemRepository.save(item);

        // Termine automatiquement la checklist quand tous les items sont cochés.
        List<DepartmentChecklistItem> items = checklistItemRepository.findByChecklistIdOrderByOrdreAsc(checklistId);
        if (!items.isEmpty() && items.stream().allMatch(DepartmentChecklistItem::isFait)
                && checklist.getStatut() != DepartmentChecklist.ChecklistStatus.TERMINEE) {
            checklist.setStatut(DepartmentChecklist.ChecklistStatus.TERMINEE);
            checklistRepository.save(checklist);
        }
        return toChecklistMap(checklist, items);
    }

    @Transactional
    public void deleteChecklistItem(UUID departmentId, UUID checklistId, UUID itemId) {
        requireDepartment(departmentId);
        DepartmentChecklistItem item = checklistItemRepository.findById(itemId)
                .filter(i -> i.getChecklistId().equals(checklistId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément introuvable"));
        checklistItemRepository.delete(item);
    }

    @Transactional
    public void deleteChecklist(UUID departmentId, UUID checklistId) {
        requireDepartment(departmentId);
        DepartmentChecklist checklist = checklistRepository.findByIdAndDepartmentId(checklistId, departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Checklist introuvable"));
        checklistItemRepository.deleteByChecklistId(checklistId);
        checklistRepository.delete(checklist);
    }

    // ========================================================================
    // INVENTAIRE MATÉRIEL
    // ========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listEquipment(UUID departmentId) {
        requireDepartment(departmentId);
        return equipmentRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .map(this::toEquipmentMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> createEquipment(UUID departmentId, DepartmentEquipmentRequest request) {
        requireDepartment(departmentId);
        DepartmentEquipment equipment = DepartmentEquipment.builder()
                .departmentId(departmentId)
                .nom(request.nom())
                .description(request.description())
                .quantite(request.quantite() != null ? request.quantite() : 1)
                .etat(request.etat() != null ? DepartmentEquipment.Etat.valueOf(request.etat()) : DepartmentEquipment.Etat.BON)
                .responsableId(request.responsableId())
                .affecteAId(request.affecteAId())
                .localisation(request.localisation())
                .dateAcquisition(request.dateAcquisition())
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        equipmentRepository.save(equipment);
        return toEquipmentMap(equipment);
    }

    @Transactional
    public Map<String, Object> updateEquipment(UUID departmentId, UUID equipmentId, DepartmentEquipmentRequest request) {
        requireDepartment(departmentId);
        DepartmentEquipment equipment = equipmentRepository.findByIdAndDepartmentId(equipmentId, departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipement introuvable"));
        equipment.setNom(request.nom());
        equipment.setDescription(request.description());
        if (request.quantite() != null) equipment.setQuantite(request.quantite());
        if (request.etat() != null) equipment.setEtat(DepartmentEquipment.Etat.valueOf(request.etat()));
        equipment.setResponsableId(request.responsableId());
        equipment.setAffecteAId(request.affecteAId());
        equipment.setLocalisation(request.localisation());
        equipment.setDateAcquisition(request.dateAcquisition());
        equipmentRepository.save(equipment);
        return toEquipmentMap(equipment);
    }

    @Transactional
    public void deleteEquipment(UUID departmentId, UUID equipmentId) {
        requireDepartment(departmentId);
        DepartmentEquipment equipment = equipmentRepository.findByIdAndDepartmentId(equipmentId, departmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipement introuvable"));
        equipmentRepository.delete(equipment);
    }

    // ========================================================================
    // DOCUMENTATION DU DÉPARTEMENT (procédures, guides, formulaires…)
    // ========================================================================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDocuments(UUID departmentId) {
        requireDepartment(departmentId);
        return documentRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId).stream()
                .map(this::toDocumentMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> documentStats(UUID departmentId) {
        requireDepartment(departmentId);
        List<DepartmentDocument> actifs = documentRepository.findByDepartmentIdOrderByCreatedAtDesc(departmentId).stream()
                .filter(d -> d.getStatut() == DepartmentDocument.DocumentStatus.ACTIF)
                .toList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", (long) actifs.size());
        for (DepartmentDocument.DocumentType type : DepartmentDocument.DocumentType.values()) {
            m.put(type.name(), actifs.stream().filter(d -> d.getType() == type).count());
        }
        return m;
    }

    @Transactional
    public Map<String, Object> createDocument(UUID departmentId, DepartmentDocumentRequest request) {
        requireDepartment(departmentId);
        DepartmentDocument doc = DepartmentDocument.builder()
                .departmentId(departmentId)
                .titre(request.titre().trim())
                .type(request.type() != null ? request.type() : DepartmentDocument.DocumentType.DOCUMENT)
                .description(request.description())
                .url(request.url())
                .statut(DepartmentDocument.DocumentStatus.ACTIF)
                .createdBy(securityUtils.getCurrentUserId())
                .build();
        documentRepository.save(doc);
        return toDocumentMap(doc);
    }

    @Transactional
    public Map<String, Object> updateDocument(UUID departmentId, UUID documentId, DepartmentDocumentRequest request) {
        requireDepartment(departmentId);
        DepartmentDocument doc = documentRepository.findById(documentId)
                .filter(d -> d.getDepartmentId().equals(departmentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable"));
        doc.setTitre(request.titre().trim());
        if (request.type() != null) doc.setType(request.type());
        doc.setDescription(request.description());
        doc.setUrl(request.url());
        if (request.statut() != null) {
            doc.setStatut(DepartmentDocument.DocumentStatus.valueOf(request.statut()));
        }
        documentRepository.save(doc);
        return toDocumentMap(doc);
    }

    @Transactional
    public void deleteDocument(UUID departmentId, UUID documentId) {
        requireDepartment(departmentId);
        DepartmentDocument doc = documentRepository.findById(documentId)
                .filter(d -> d.getDepartmentId().equals(departmentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document introuvable"));
        documentRepository.delete(doc);
    }

    private Map<String, Object> toDocumentMap(DepartmentDocument d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("departmentId", d.getDepartmentId());
        m.put("titre", d.getTitre());
        m.put("type", d.getType().name());
        m.put("description", d.getDescription());
        m.put("url", d.getUrl());
        m.put("statut", d.getStatut().name());
        m.put("createdBy", d.getCreatedBy());
        m.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
        return m;
    }

    // ========================================================================
    // SYNTHÈSE — données réelles uniquement
    // ========================================================================

    private String buildSynthesis(UUID departmentId, DepartmentReport.ReportType type,
                                  LocalDate debut, LocalDate fin, Map<String, Object> stats) {
        String deptNom = requireDepartment(departmentId).getNom();
        StringBuilder sb = new StringBuilder();

        @SuppressWarnings("unchecked")
        Map<String, Object> effectif = (Map<String, Object>) stats.get("effectif");
        @SuppressWarnings("unchecked")
        Map<String, Object> presence = (Map<String, Object>) stats.get("presence");
        @SuppressWarnings("unchecked")
        Map<String, Object> taches = (Map<String, Object>) stats.get("taches");
        @SuppressWarnings("unchecked")
        Map<String, Object> equipes = (Map<String, Object>) stats.get("equipes");

        sb.append("SYNTHÈSE DU DÉPARTEMENT « ").append(deptNom).append(" »\n");
        sb.append("Période : du ").append(debut).append(" au ").append(fin).append(" (")
                .append(type.name().toLowerCase(Locale.ROOT)).append(")\n\n");

        // Effectif
        long total = ((Number) effectif.getOrDefault("total", 0L)).longValue();
        long actifs = ((Number) effectif.getOrDefault("actifs", 0L)).longValue();
        long nouveaux30j = ((Number) effectif.getOrDefault("nouveaux30j", 0L)).longValue();
        long enIntegration = ((Number) effectif.getOrDefault("enIntegration", 0L)).longValue();
        long decroches = ((Number) effectif.getOrDefault("decroches", 0L)).longValue();
        sb.append("EFFECTIF\n");
        sb.append("- ").append(total).append(" membre").append(total > 1 ? "s" : "").append(" au total\n");
        sb.append("- ").append(actifs).append(" actif").append(actifs > 1 ? "s" : "").append('\n');
        sb.append("- ").append(nouveaux30j).append(" nouveau").append(nouveaux30j > 1 ? "x" : "")
                .append(" intégré").append(nouveaux30j > 1 ? "s" : "").append(" sur les 30 derniers jours\n");
        sb.append("- ").append(enIntegration).append(" en intégration, ").append(decroches).append(" décroché")
                .append(decroches > 1 ? "s" : "").append('\n');

        // Présence
        long presencesTotal = ((Number) presence.getOrDefault("total", 0L)).longValue();
        long presents = ((Number) presence.getOrDefault("presents", 0L)).longValue();
        long absents = ((Number) presence.getOrDefault("absents", 0L)).longValue();
        double tauxPresence = ((Number) presence.getOrDefault("taux", 0.0)).doubleValue();
        sb.append("\nASSIDUITÉ\n");
        sb.append("- ").append(presencesTotal).append(" fiche").append(presencesTotal > 1 ? "s" : "")
                .append(" de présence, ").append(presents).append(" présent").append(presents > 1 ? "s" : "")
                .append(", ").append(absents).append(" absent").append(absents > 1 ? "s" : "").append('\n');
        sb.append("- Taux de présence : ").append(tauxPresence).append(" %\n");

        // Tâches
        long tachesTotal = ((Number) taches.getOrDefault("total", 0L)).longValue();
        long tachesOuvertes = ((Number) taches.getOrDefault("ouvertes", 0L)).longValue();
        long tachesEnRetard = ((Number) taches.getOrDefault("enRetard", 0L)).longValue();
        long tachesTerminees = ((Number) taches.getOrDefault("terminees", 0L)).longValue();
        sb.append("\nTÂCHES\n");
        sb.append("- ").append(tachesTotal).append(" tâche").append(tachesTotal > 1 ? "s" : "")
                .append(" : ").append(tachesOuvertes).append(" ouverte").append(tachesOuvertes > 1 ? "s" : "")
                .append(", ").append(tachesTerminees).append(" terminée").append(tachesTerminees > 1 ? "s" : "").append('\n');
        sb.append("- ").append(tachesEnRetard).append(" tâche").append(tachesEnRetard > 1 ? "s" : "")
                .append(" en retard\n");

        // Objectifs
        List<DepartmentMemberObjective> objectives = objectiveRepository.findByDepartmentId(departmentId);
        long objectifsAtteints = objectives.stream()
                .filter(o -> o.getStatut() == DepartmentMemberObjective.ObjectiveStatus.ATTEINT).count();
        long objectifsEnCours = objectives.stream()
                .filter(o -> o.getStatut() == DepartmentMemberObjective.ObjectiveStatus.EN_COURS
                        || o.getStatut() == DepartmentMemberObjective.ObjectiveStatus.A_FAIRE).count();
        if (!objectives.isEmpty()) {
            sb.append("\nPROGRESSION\n");
            sb.append("- ").append(objectives.size()).append(" objectif").append(objectives.size() > 1 ? "s" : "")
                    .append(" de progression : ").append(objectifsAtteints).append(" atteint")
                    .append(objectifsAtteints > 1 ? "s" : "").append(", ").append(objectifsEnCours)
                    .append(" en cours\n");
        }

        // Discipline
        @SuppressWarnings("unchecked")
        Map<String, Long> discipline = (Map<String, Long>) stats.get("disciplineParCategorie");
        if (discipline != null && !discipline.isEmpty()) {
            long totalDiscipline = discipline.values().stream().mapToLong(Long::longValue).sum();
            sb.append("\nDISCIPLINE\n");
            sb.append("- ").append(totalDiscipline).append(" dossier").append(totalDiscipline > 1 ? "s" : "")
                    .append(" disciplinaire").append(totalDiscipline > 1 ? "s" : "").append(" (")
                    .append(discipline.entrySet().stream()
                            .map(e -> e.getKey() + " : " + e.getValue())
                            .collect(Collectors.joining(", ")))
                    .append(")\n");
        }

        // Équipes
        if (equipes != null) {
            long equipesActives = ((Number) equipes.getOrDefault("actives", 0L)).longValue();
            long equipesArchivees = ((Number) equipes.getOrDefault("archivees", 0L)).longValue();
            sb.append("\nÉQUIPES & SOUS-DÉPARTEMENTS\n");
            sb.append("- ").append(equipesActives).append(" équipe").append(equipesActives > 1 ? "s" : "")
                    .append(" active").append(equipesActives > 1 ? "s" : "").append(", ")
                    .append(equipesArchivees).append(" archivée").append(equipesArchivees > 1 ? "s" : "").append('\n');
        }

        // Événements
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> evenements = (List<Map<String, Object>>) stats.get("evenements");
        if (evenements != null && !evenements.isEmpty()) {
            sb.append("\nÉVÉNEMENTS À VENIR\n");
            for (Map<String, Object> e : evenements) {
                sb.append("- ").append(e.get("titre"));
                if (e.get("date") != null) sb.append(" (").append(e.get("date")).append(")");
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    private DepartmentReport.ReportType parseType(String type) {
        try {
            return DepartmentReport.ReportType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de rapport invalide : " + type + " (attendu : " + Arrays.toString(DepartmentReport.ReportType.values()) + ")");
        }
    }

    private DepartmentChecklist.CibleType parseCibleType(String type) {
        try {
            return DepartmentChecklist.CibleType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Type de cible invalide : " + type + " (attendu : GENERAL, TACHE, EVENEMENT, EQUIPE, MEMBRE)");
        }
    }

    private LocalDate[] periodFor(DepartmentReport.ReportType type, LocalDate debut, LocalDate fin) {
        LocalDate today = LocalDate.now();
        if (debut != null && fin != null) return new LocalDate[]{debut, fin};
        return switch (type) {
            case HEBDOMADAIRE -> new LocalDate[]{
                    today.with(java.time.DayOfWeek.MONDAY),
                    today.with(java.time.DayOfWeek.SUNDAY)};
            case MENSUEL -> {
                YearMonth ym = YearMonth.from(today);
                yield new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
            }
            case TRIMESTRIEL -> {
                YearMonth ym = YearMonth.from(today);
                int startMonth = ((ym.getMonthValue() - 1) / 3) * 3 + 1;
                YearMonth start = YearMonth.of(ym.getYear(), startMonth);
                yield new LocalDate[]{start.atDay(1), start.plusMonths(2).atEndOfMonth()};
            }
            case ANNUEL -> new LocalDate[]{today.withDayOfYear(1), today.withMonth(12).withDayOfMonth(31)};
            default -> new LocalDate[]{today.minusDays(29), today};
        };
    }

    private String titreAuto(DepartmentReport.ReportType type, LocalDate debut, LocalDate fin) {
        String label = switch (type) {
            case HEBDOMADAIRE -> "Rapport hebdomadaire";
            case MENSUEL -> "Rapport mensuel";
            case TRIMESTRIEL -> "Rapport trimestriel";
            case ANNUEL -> "Rapport annuel";
            default -> "Rapport " + type.name().toLowerCase(Locale.ROOT);
        };
        return label + " — du " + debut + " au " + fin;
    }

    private Map<String, Object> toReportMap(DepartmentReport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("departmentId", r.getDepartmentId());
        m.put("auteurId", r.getAuteurId());
        m.put("type", r.getType().name());
        m.put("titre", r.getTitre());
        m.put("periodeDebut", r.getPeriodeDebut() != null ? r.getPeriodeDebut().toString() : null);
        m.put("periodeFin", r.getPeriodeFin() != null ? r.getPeriodeFin().toString() : null);
        m.put("contenu", r.getContenu());
        m.put("statut", r.getStatut().name());
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> toChecklistMap(DepartmentChecklist c, List<DepartmentChecklistItem> items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("departmentId", c.getDepartmentId());
        m.put("titre", c.getTitre());
        m.put("cibleType", c.getCibleType().name());
        m.put("cibleId", c.getCibleId());
        m.put("statut", c.getStatut().name());
        m.put("createdBy", c.getCreatedBy());
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        long fait = items.stream().filter(DepartmentChecklistItem::isFait).count();
        m.put("progression", items.isEmpty() ? 0 : Math.round(fait * 100.0 / items.size()));
        m.put("items", items.stream().map(i -> {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("id", i.getId());
            im.put("libelle", i.getLibelle());
            im.put("fait", i.isFait());
            im.put("ordre", i.getOrdre());
            return im;
        }).toList());
        return m;
    }

    private Map<String, Object> toEquipmentMap(DepartmentEquipment e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("departmentId", e.getDepartmentId());
        m.put("nom", e.getNom());
        m.put("description", e.getDescription());
        m.put("quantite", e.getQuantite());
        m.put("etat", e.getEtat().name());
        m.put("responsableId", e.getResponsableId());
        m.put("affecteAId", e.getAffecteAId());
        m.put("localisation", e.getLocalisation());
        m.put("dateAcquisition", e.getDateAcquisition() != null ? e.getDateAcquisition().toString() : null);
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        return m;
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        return v.contains(";") || v.contains("\n") || v.contains("\"") ? "\"" + v + "\"" : v;
    }
}
