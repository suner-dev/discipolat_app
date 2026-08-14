package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.CanalNotification;
import com.discipolat.common.enums.StatutAlerte;
import com.discipolat.common.enums.TypeNotification;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.alerts.domain.Alert;
import com.discipolat.modules.alerts.domain.AlertRepository;
import com.discipolat.modules.departments.api.DepartmentAnnouncementRequest;
import com.discipolat.modules.departments.api.DepartmentNoteRequest;
import com.discipolat.modules.discipline.domain.SoulDisciplineEvent;
import com.discipolat.modules.discipline.domain.SoulDisciplineEventRepository;
import com.discipolat.modules.evaluations.domain.Evaluation;
import com.discipolat.modules.evaluations.domain.EvaluationRepository;
import com.discipolat.modules.events.domain.Event;
import com.discipolat.modules.events.domain.EventRegistration;
import com.discipolat.modules.events.domain.EventRegistrationRepository;
import com.discipolat.modules.events.domain.EventRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.EntityAttachmentService;
import com.discipolat.modules.members.domain.MemberPresence;
import com.discipolat.modules.members.domain.MemberPresenceRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.reports.domain.MakerReport;
import com.discipolat.modules.reports.domain.MakerReportRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulNote;
import com.discipolat.modules.souls.domain.SoulNoteRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.domain.TransferRequest;
import com.discipolat.modules.transfers.domain.TransferRequestRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dossier départemental & analytics du Department Management System.
 * <p>
 * Fournit le dossier complet d'un membre (profil, appartenance, affectations,
 * tâches, présences, discipline, rapports, évaluations, événements, notes,
 * documents, alertes, transferts, activité), les statistiques réelles du
 * département, l'export/import CSV des membres, les notes et annonces du
 * département, et les alertes intelligentes (absence répétée, tâche en retard).
 * <p>
 * Tous les accès passent par {@link DepartmentService#findById} : un responsable
 * ne consulte que SES départements.
 */
@Service
@Transactional
public class DepartmentDossierService {

    private final DepartmentService departmentService;
    private final DepartmentTeamRepository teamRepository;
    private final DepartmentPositionRepository positionRepository;
    private final DepartmentAssignmentRepository assignmentRepository;
    private final DepartmentTaskRepository taskRepository;
    private final DepartmentActivityRepository activityRepository;
    private final DepartmentMemberNoteRepository noteRepository;
    private final DepartmentAnnouncementRepository announcementRepository;
    private final DepartmentMemberObjectiveRepository objectiveRepository;
    private final DepartmentMemberReportRepository memberReportRepository;
    private final SoulRepository soulRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final SoulNoteRepository soulNoteRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final SecurityUtils securityUtils;
    private final MemberPresenceRepository presenceRepository;
    private final SoulDisciplineEventRepository disciplineRepository;
    private final MakerReportRepository makerReportRepository;
    private final EvaluationRepository evaluationRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventRepository eventRepository;
    private final AlertRepository alertRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final EntityAttachmentService attachmentService;
    private final NotificationService notificationService;
    private final DepartmentSettingsService settingsService;

    public DepartmentDossierService(DepartmentService departmentService,
                                    DepartmentTeamRepository teamRepository,
                                    DepartmentPositionRepository positionRepository,
                                    DepartmentAssignmentRepository assignmentRepository,
                                    DepartmentTaskRepository taskRepository,
                                    DepartmentActivityRepository activityRepository,
                                    DepartmentMemberNoteRepository noteRepository,
                                    DepartmentAnnouncementRepository announcementRepository,
                                    DepartmentMemberObjectiveRepository objectiveRepository,
                                    DepartmentMemberReportRepository memberReportRepository,
                                    SoulRepository soulRepository,
                                    SoulDepartmentRepository soulDepartmentRepository,
                                    SoulNoteRepository soulNoteRepository,
                                    UserRepository userRepository,
                                    FamilyRepository familyRepository,
                                    SecurityUtils securityUtils,
                                    MemberPresenceRepository presenceRepository,
                                    SoulDisciplineEventRepository disciplineRepository,
                                    MakerReportRepository makerReportRepository,
                                    EvaluationRepository evaluationRepository,
                                    EventRegistrationRepository eventRegistrationRepository,
                                    EventRepository eventRepository,
                                    AlertRepository alertRepository,
                                    TransferRequestRepository transferRequestRepository,
                                    EntityAttachmentService attachmentService,
                                    NotificationService notificationService,
                                    DepartmentSettingsService settingsService) {
        this.departmentService = departmentService;
        this.teamRepository = teamRepository;
        this.positionRepository = positionRepository;
        this.assignmentRepository = assignmentRepository;
        this.taskRepository = taskRepository;
        this.activityRepository = activityRepository;
        this.noteRepository = noteRepository;
        this.announcementRepository = announcementRepository;
        this.objectiveRepository = objectiveRepository;
        this.memberReportRepository = memberReportRepository;
        this.soulRepository = soulRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.soulNoteRepository = soulNoteRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.securityUtils = securityUtils;
        this.presenceRepository = presenceRepository;
        this.disciplineRepository = disciplineRepository;
        this.makerReportRepository = makerReportRepository;
        this.evaluationRepository = evaluationRepository;
        this.eventRegistrationRepository = eventRegistrationRepository;
        this.eventRepository = eventRepository;
        this.alertRepository = alertRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.attachmentService = attachmentService;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
    }

    /** Vérifie que le département existe et appartient à l'espace métier de l'utilisateur. */
    private void assertCanManage(UUID departmentId) {
        departmentService.findById(departmentId);
    }

    /** Vérifie qu'un membre appartient au département (lecture de dossier). */
    private void assertMemberInDepartment(UUID departmentId, UUID memberId) {
        if (!soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, departmentId)
                && !securityUtils.isSuperUser()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : ce membre ne fait pas partie de votre département");
        }
    }

    private String userName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null);
    }

    private String soulName(UUID soulId) {
        if (soulId == null) return null;
        return soulRepository.findById(soulId).map(Soul::getNomComplet).orElse(null);
    }

    // ========================================================================
    // DOSSIER COMPLET D'UN MEMBRE
    // ========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getMemberDossier(UUID departmentId, UUID memberId) {
        assertCanManage(departmentId);
        assertMemberInDepartment(departmentId, memberId);
        Soul soul = soulRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", memberId));

        Map<String, Object> dossier = new LinkedHashMap<>();
        dossier.put("departmentId", departmentId);
        dossier.put("memberId", memberId);
        dossier.put("profil", profil(soul, departmentId));
        dossier.put("appartenance", appartenance(soul.getId()));
        dossier.put("affectations", affectations(departmentId, memberId));
        dossier.put("taches", taches(departmentId, memberId));
        dossier.put("presences", presences(memberId));
        dossier.put("discipline", discipline(memberId));
        dossier.put("rapports", rapports(memberId));
        dossier.put("evaluations", evaluations(soul.getUserId()));
        dossier.put("evenements", evenements(soul.getUserId()));
        dossier.put("documents", documents(memberId));
        dossier.put("notes", notes(departmentId, memberId));
        dossier.put("notesDisciple", notesDisciple(memberId));
        dossier.put("objectifs", objectifs(departmentId, memberId));
        dossier.put("rapportsResponsable", rapportsResponsable(departmentId, memberId));
        dossier.put("alertes", alertes(memberId));
        dossier.put("transferts", transferts(memberId));
        dossier.put("activite", activite(departmentId, memberId));
        dossier.put("annonces", annoncesPourMembre(departmentId, memberId));
        return dossier;
    }

    private Map<String, Object> profil(Soul soul, UUID departmentId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", soul.getId());
        m.put("nom", soul.getNom());
        m.put("prenom", soul.getPrenom());
        m.put("nomComplet", soul.getNomComplet());
        m.put("email", soul.getEmail());
        m.put("telephone", soul.getTelephone());
        m.put("adresse", soul.getAdresse());
        m.put("dateNaissance", soul.getDateNaissance() != null ? soul.getDateNaissance().toString() : null);
        m.put("profession", soul.getProfession());
        m.put("niveauEtude", soul.getNiveauEtude());
        m.put("situationFamiliale", soul.getSituationFamiliale());
        m.put("statut", soul.getStatut() != null ? soul.getStatut().name() : null);
        m.put("typeDisciple", soul.getTypeDisciple() != null ? soul.getTypeDisciple().name() : null);
        m.put("etatSpirituel", soul.getEtatSpirituel());
        m.put("niveauCroissance", soul.getNiveauCroissance());
        m.put("dateIntegration", soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : null);
        m.put("dateConversion", soul.getDateConversion() != null ? soul.getDateConversion().toString() : null);
        m.put("dateDernierContact", soul.getDateDernierContact() != null ? soul.getDateDernierContact().toString() : null);
        m.put("faiseurId", soul.getFaiseurId());
        m.put("faiseurNom", userName(soul.getFaiseurId()));
        m.put("familleId", soul.getFamilleId());
        m.put("familleNom", soul.getFamilleId() != null
                ? familyRepository.findById(soul.getFamilleId()).map(Family::getNom).orElse(null) : null);
        m.put("userId", soul.getUserId());
        m.put("userNom", soul.getUserId() != null ? userName(soul.getUserId()) : null);
        m.put("ville", soul.getAdresse() != null ? soul.getAdresse().split(",")[0].trim() : null);
        // Lien au département (traçabilité du rattachement)
        soulDepartmentRepository.findBySoulIdAndDepartmentId(soul.getId(), departmentId).stream()
                .findFirst()
                .ifPresent(link -> {
                    m.put("dateAffectation", link.getDateAffectation() != null ? link.getDateAffectation().toString() : null);
                    m.put("dateDesaffectation", link.getDateDesaffectation() != null ? link.getDateDesaffectation().toString() : null);
                    m.put("membreActif", link.isActif());
                    m.put("ajoutePar", link.getCreatedBy() != null ? userName(link.getCreatedBy()) : null);
                    m.put("ajouteParId", link.getCreatedBy());
                    m.put("origine", link.getOrigine());
                });
        return m;
    }

    /** Tous les départements du membre (traçabilité des mouvements). */
    private List<Map<String, Object>> appartenance(UUID memberId) {
        List<UUID> deptIds = soulDepartmentRepository.findBySoulId(memberId).stream()
                .map(SoulDepartment::getDepartmentId).distinct().toList();
        Map<UUID, String> deptNames = new HashMap<>();
        if (!deptIds.isEmpty()) {
            departmentService.findAllIn(deptIds).forEach(d -> deptNames.put(d.getId(), d.getNom()));
        }
        return soulDepartmentRepository.findBySoulId(memberId).stream()
                .sorted(Comparator.comparing(SoulDepartment::getDateAffectation).reversed())
                .map(link -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("departmentId", link.getDepartmentId());
                    m.put("departmentNom", deptNames.get(link.getDepartmentId()));
                    m.put("actif", link.isActif());
                    m.put("dateAffectation", link.getDateAffectation() != null ? link.getDateAffectation().toString() : null);
                    m.put("dateDesaffectation", link.getDateDesaffectation() != null ? link.getDateDesaffectation().toString() : null);
                    m.put("ajoutePar", link.getCreatedBy() != null ? userName(link.getCreatedBy()) : null);
                    m.put("origine", link.getOrigine());
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> affectations(UUID departmentId, UUID memberId) {
        Map<UUID, String> teamNames = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentTeam::getId, DepartmentTeam::getNom));
        Map<UUID, String> positionNames = positionRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentPosition::getId, DepartmentPosition::getNom));
        return assignmentRepository.findByDepartmentIdAndMemberId(departmentId, memberId).stream()
                .sorted(Comparator.comparing(DepartmentAssignment::getDateDebut, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("teamId", a.getTeamId());
                    m.put("teamNom", a.getTeamId() != null ? teamNames.get(a.getTeamId()) : null);
                    m.put("positionId", a.getPositionId());
                    m.put("positionNom", a.getPositionId() != null ? positionNames.get(a.getPositionId()) : null);
                    m.put("role", a.getRole().name());
                    m.put("dateDebut", a.getDateDebut() != null ? a.getDateDebut().toString() : null);
                    m.put("dateFin", a.getDateFin() != null ? a.getDateFin().toString() : null);
                    m.put("actif", a.isActif());
                    return m;
                })
                .toList();
    }

    private Map<String, Object> taches(UUID departmentId, UUID memberId) {
        List<DepartmentTask> tasks = taskRepository.findByDepartmentIdOrderByEcheanceAsc(departmentId).stream()
                .filter(t -> memberId.equals(t.getAssignedTo()))
                .toList();
        long ouvertes = tasks.stream().filter(DepartmentTask::isOpen).count();
        long enRetard = tasks.stream().filter(DepartmentTask::isOverdue).count();
        long terminees = tasks.stream()
                .filter(t -> t.getStatut() == DepartmentTask.TaskStatus.TERMINEE
                        || t.getStatut() == DepartmentTask.TaskStatus.VALIDEE).count();
        Map<UUID, String> teamNames = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentTeam::getId, DepartmentTeam::getNom));
        List<Map<String, Object>> liste = tasks.stream()
                .sorted(Comparator.comparing(DepartmentTask::getEcheance, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("titre", t.getTitre());
                    m.put("description", t.getDescription());
                    m.put("teamNom", t.getTeamId() != null ? teamNames.get(t.getTeamId()) : null);
                    m.put("priorite", t.getPriorite().name());
                    m.put("statut", t.getStatut().name());
                    m.put("echeance", t.getEcheance() != null ? t.getEcheance().toString() : null);
                    m.put("avancement", t.getAvancement());
                    m.put("enRetard", t.isOverdue());
                    return m;
                })
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liste", liste);
        result.put("total", (long) tasks.size());
        result.put("ouvertes", ouvertes);
        result.put("enRetard", enRetard);
        result.put("terminees", terminees);
        return result;
    }

    private Map<String, Object> presences(UUID memberId) {
        List<MemberPresence> records = presenceRepository.findBySoulIdInOrderBySemaineDesc(List.of(memberId));
        long presents = records.stream().filter(r -> Boolean.TRUE.equals(r.getPresent())).count();
        long total = records.size();
        double taux = total > 0 ? Math.round(presents * 1000.0 / total) / 10.0 : 0.0;
        List<Map<String, Object>> liste = records.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("semaine", r.getSemaine().toString());
                    m.put("present", r.getPresent());
                    m.put("typeProgramme", r.getTypeProgramme());
                    m.put("sousProgramme", r.getSousProgramme());
                    m.put("notes", r.getNotes());
                    m.put("date", r.getCreatedAt().toLocalDate().toString());
                    return m;
                })
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liste", liste);
        result.put("total", total);
        result.put("presents", presents);
        result.put("absents", total - presents);
        result.put("tauxPresence", taux);
        return result;
    }

    private Map<String, Object> discipline(UUID memberId) {
        List<SoulDisciplineEvent> events = disciplineRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(memberId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liste", events.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("titre", e.getTitre());
            m.put("description", e.getDescription());
            m.put("categorie", e.getCategorie() != null ? e.getCategorie().name() : null);
            m.put("typeEvenement", e.getTypeEvenement());
            m.put("gravite", e.getGravite() != null ? e.getGravite().name() : null);
            m.put("dateEvenement", e.getDateEvenement() != null ? e.getDateEvenement().toString() : null);
            m.put("resolu", e.isResolu());
            return m;
        }).toList());
        result.put("total", (long) events.size());
        result.put("nonResolus", events.stream().filter(e -> !e.isResolu()).count());
        return result;
    }

    private Map<String, Object> rapports(UUID memberId) {
        List<MakerReport> reports = makerReportRepository.findByAmeId(memberId, PageRequest.of(0, 30)).getContent();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liste", reports.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("semaine", r.getSemaine().toString());
            m.put("soumis", r.isSoumis());
            m.put("difficultes", r.getDifficultes());
            m.put("presencesParCulte", r.getPresencesParCulte());
            m.put("dateSoumission", r.getDateSoumission() != null ? r.getDateSoumission().toString() : null);
            return m;
        }).toList());
        result.put("total", (long) reports.size());
        result.put("soumis", reports.stream().filter(MakerReport::isSoumis).count());
        return result;
    }

    private Map<String, Object> evaluations(UUID userId) {
        if (userId == null) return Map.of("liste", List.of(), "total", 0L);
        List<Evaluation> evals = evaluationRepository.findByEvalueId(userId, PageRequest.of(0, 30)).getContent();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liste", evals.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("categorie", e.getCategorie() != null ? e.getCategorie().name() : null);
            m.put("note", e.getNote());
            m.put("commentaire", e.getCommentaire());
            m.put("date", e.getCreatedAt().toLocalDate().toString());
            return m;
        }).toList());
        result.put("total", (long) evals.size());
        return result;
    }

    private Map<String, Object> evenements(UUID userId) {
        if (userId == null) return Map.of("liste", List.of(), "total", 0L);
        List<EventRegistration> regs = eventRegistrationRepository.findByUtilisateurId(userId);
        List<Map<String, Object>> liste = regs.stream()
                .map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("eventId", r.getEventId());
                    m.put("statut", r.getStatutInscription());
                    m.put("dateInscription", r.getDateInscription() != null ? r.getDateInscription().toString() : null);
                    eventRepository.findById(r.getEventId()).ifPresent(ev -> {
                        m.put("titre", ev.getTitre());
                        m.put("dateDebut", ev.getDateDebut() != null ? ev.getDateDebut().toString() : null);
                        m.put("lieu", ev.getLieu());
                        m.put("statutEvenement", ev.getStatut());
                    });
                    return m;
                })
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("liste", liste);
        result.put("total", (long) liste.size());
        return result;
    }

    private List<Map<String, Object>> documents(UUID memberId) {
        return attachmentService.itemsFor(
                com.discipolat.modules.files.domain.EntityAttachment.EntityType.DEPARTMENT_MEMBER, memberId).stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.id());
                    m.put("fileId", a.fileId());
                    m.put("nom", a.nom());
                    m.put("url", a.url());
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> notes(UUID departmentId, UUID memberId) {
        Map<UUID, String> auteurs = new HashMap<>();
        List<DepartmentMemberNote> notes = noteRepository
                .findByDepartmentIdAndMemberIdAndDeletedFalseOrderByCreatedAtDesc(departmentId, memberId);
        notes.stream().map(DepartmentMemberNote::getAuteurId).distinct()
                .forEach(id -> auteurs.put(id, userName(id)));
        return notes.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("contenu", n.getContenu());
            m.put("auteurNom", auteurs.get(n.getAuteurId()));
            m.put("auteurId", n.getAuteurId());
            m.put("createdAt", ts(n.getCreatedAt()));
            return m;
        }).toList();
    }

    private List<Map<String, Object>> notesDisciple(UUID memberId) {
        return soulNoteRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(memberId).stream()
                .map(n -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", n.getId());
                    m.put("contenu", n.getContenu());
                    m.put("auteurNom", userName(n.getAuteurId()));
                    m.put("createdAt", ts(n.getCreatedAt()));
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> objectifs(UUID departmentId, UUID memberId) {
        return objectiveRepository.findByMemberIdAndDepartmentIdOrderByEcheanceAscCreatedAtDesc(memberId, departmentId)
                .stream().map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getId());
                    m.put("titre", o.getTitre());
                    m.put("description", o.getDescription());
                    m.put("echeance", o.getEcheance() != null ? o.getEcheance().toString() : null);
                    m.put("avancement", o.getAvancement());
                    m.put("statut", o.getStatut() != null ? o.getStatut().name() : null);
                    m.put("creeParNom", userName(o.getCreePar()));
                    m.put("createdAt", ts(o.getCreatedAt()));
                    m.put("enRetard", o.getStatut() != null
                            && (o.getStatut() == DepartmentMemberObjective.ObjectiveStatus.A_FAIRE
                                || o.getStatut() == DepartmentMemberObjective.ObjectiveStatus.EN_COURS)
                            && o.getEcheance() != null && o.getEcheance().isBefore(java.time.LocalDate.now()));
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> rapportsResponsable(UUID departmentId, UUID memberId) {
        return memberReportRepository.findByMemberIdAndDepartmentIdOrderByCreatedAtDesc(memberId, departmentId)
                .stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("type", r.getType() != null ? r.getType().name() : null);
                    m.put("contenu", r.getContenu());
                    m.put("auteurNom", userName(r.getAuteurId()));
                    m.put("createdAt", ts(r.getCreatedAt()));
                    return m;
                })
                .toList();
    }

    // ========================================================================
    // RAPPORTS DU RESPONSABLE SUR UN MEMBRE
    // ========================================================================

    public List<Map<String, Object>> listMemberReports(UUID departmentId, UUID memberId) {
        assertCanManage(departmentId);
        return rapportsResponsable(departmentId, memberId);
    }

    public Map<String, Object> createMemberReport(UUID departmentId, UUID memberId,
                                                  com.discipolat.modules.departments.api.DepartmentMemberReportRequest request) {
        assertCanManage(departmentId);
        soulRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Soul", memberId));
        DepartmentMemberReport report = DepartmentMemberReport.builder()
                .departmentId(departmentId)
                .memberId(memberId)
                .auteurId(securityUtils.getCurrentUserId())
                .type(request.type())
                .contenu(request.contenu().trim())
                .build();
        DepartmentMemberReport saved = memberReportRepository.save(report);
        activityRepository.save(DepartmentActivity.builder()
                .departmentId(departmentId)
                .actorId(securityUtils.getCurrentUserId())
                .actorNom(userName(securityUtils.getCurrentUserId()))
                .action("MEMBER_REPORT_ADDED")
                .entityType("MEMBER")
                .entityId(memberId)
                .details("Rapport (" + request.type() + ") ajouté au dossier du membre")
                .build());
        return rapportsResponsable(departmentId, memberId).stream()
                .filter(r -> saved.getId().equals(((java.util.UUID) r.get("id"))))
                .findFirst().orElseGet(Map::of);
    }

    public void deleteMemberReport(UUID departmentId, UUID reportId) {
        assertCanManage(departmentId);
        DepartmentMemberReport report = memberReportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentMemberReport", reportId));
        if (!report.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé : rapport hors de votre espace métier");
        }
        memberReportRepository.delete(report);
    }

    private List<Map<String, Object>> alertes(UUID memberId) {
        return alertRepository.findByAmeIdAndStatut(memberId, StatutAlerte.ACTIVE).stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("typeAlerte", a.getTypeAlerte());
                    m.put("titre", a.getTitre());
                    m.put("message", a.getMessage());
                    m.put("priorite", a.getPriorite());
                    m.put("dateDeclenchement", ts(a.getDateDeclenchement()));
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> transferts(UUID memberId) {
        return transferRequestRepository.findByPersonneId(memberId).stream()
                .sorted(Comparator.comparing(TransferRequest::getCreatedAt).reversed())
                .limit(20)
                .map(t -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", t.getId());
                    m.put("type", t.getType() != null ? t.getType().name() : null);
                    m.put("statut", t.getStatut() != null ? t.getStatut().name() : null);
                    m.put("justification", t.getJustification());
                    m.put("demandeurNom", userName(t.getDemandeurId()));
                    m.put("ancienneAffectation", t.getAncienneAffectation());
                    m.put("nouvelleAffectation", t.getNouvelleAffectation());
                    m.put("dateSoumission", t.getDateSoumission() != null ? t.getDateSoumission().toString() : null);
                    m.put("createdAt", ts(t.getCreatedAt()));
                    return m;
                })
                .toList();
    }

    private List<Map<String, Object>> activite(UUID departmentId, UUID memberId) {
        return activityRepository.findTop50ByDepartmentIdOrderByCreatedAtDesc(departmentId).stream()
                .filter(a -> memberId.equals(a.getEntityId())
                        || ("SOUL".equals(a.getEntityType()) && memberId.equals(a.getEntityId())))
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("action", a.getAction());
                    m.put("details", a.getDetails());
                    m.put("actorNom", a.getActorNom());
                    m.put("createdAt", ts(a.getCreatedAt()));
                    return m;
                })
                .toList();
    }

    /** Annonces du département destinées à ce membre (toutes, équipe ou poste). */
    private List<Map<String, Object>> annoncesPourMembre(UUID departmentId, UUID memberId) {
        List<DepartmentAnnouncement> annonces = announcementRepository
                .findByDepartmentIdAndDeletedFalseOrderByCreatedAtDesc(departmentId);
        List<UUID> teamIds = assignmentRepository.findByDepartmentIdAndMemberIdAndActifTrue(departmentId, memberId).stream()
                .map(DepartmentAssignment::getTeamId).filter(Objects::nonNull).toList();
        List<UUID> positionIds = assignmentRepository.findByDepartmentIdAndMemberIdAndActifTrue(departmentId, memberId).stream()
                .map(DepartmentAssignment::getPositionId).filter(Objects::nonNull).toList();
        return annonces.stream()
                .filter(a -> a.getCible() == DepartmentAnnouncement.Cible.TOUS
                        || (a.getCible() == DepartmentAnnouncement.Cible.EQUIPE && a.getTeamId() != null && teamIds.contains(a.getTeamId()))
                        || (a.getCible() == DepartmentAnnouncement.Cible.POSTE && a.getPositionId() != null && positionIds.contains(a.getPositionId()))
                        || (a.getCible() == DepartmentAnnouncement.Cible.MEMBRES
                                && a.getMemberIds() != null && a.getMemberIds().contains(memberId)))
                .map(a -> toAnnouncementMap(a))
                .toList();
    }

    private Map<String, Object> toAnnouncementMap(DepartmentAnnouncement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("titre", a.getTitre());
        m.put("message", a.getMessage());
        m.put("cible", a.getCible().name());
        m.put("teamId", a.getTeamId());
        m.put("teamNom", a.getTeamId() != null ? teamRepository.findById(a.getTeamId()).map(DepartmentTeam::getNom).orElse(null) : null);
        m.put("positionId", a.getPositionId());
        m.put("positionNom", a.getPositionId() != null ? positionRepository.findById(a.getPositionId()).map(DepartmentPosition::getNom).orElse(null) : null);
        m.put("memberIds", a.getMemberIds() != null ? new ArrayList<>(a.getMemberIds()) : List.of());
        m.put("nbMembres", a.getMemberIds() != null ? a.getMemberIds().size() : 0);
        m.put("auteurNom", userName(a.getAuteurId()));
        m.put("createdAt", ts(a.getCreatedAt()));
        return m;
    }

    private String ts(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    // ========================================================================
    // NOTES DU DOSSIER (membre)
    // ========================================================================

    public List<Map<String, Object>> listMemberNotes(UUID departmentId, UUID memberId) {
        assertCanManage(departmentId);
        assertMemberInDepartment(departmentId, memberId);
        return notes(departmentId, memberId);
    }

    public Map<String, Object> addMemberNote(UUID departmentId, UUID memberId, DepartmentNoteRequest request) {
        assertCanManage(departmentId);
        assertMemberInDepartment(departmentId, memberId);
        DepartmentMemberNote note = DepartmentMemberNote.builder()
                .departmentId(departmentId)
                .memberId(memberId)
                .auteurId(securityUtils.getCurrentUserId())
                .contenu(request.contenu().trim())
                .build();
        note = noteRepository.save(note);
        activityRepository.save(DepartmentActivity.builder()
                .departmentId(departmentId)
                .actorId(securityUtils.getCurrentUserId())
                .actorNom(userName(securityUtils.getCurrentUserId()))
                .action("NOTE_ADDED")
                .entityType("SOUL")
                .entityId(memberId)
                .details("Note ajoutée au dossier de " + soulName(memberId))
                .build());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", note.getId());
        m.put("contenu", note.getContenu());
        m.put("auteurNom", userName(note.getAuteurId()));
        m.put("createdAt", ts(note.getCreatedAt()));
        return m;
    }

    public void deleteMemberNote(UUID departmentId, UUID noteId) {
        assertCanManage(departmentId);
        DepartmentMemberNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentMemberNote", noteId));
        if (!note.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : note hors de votre espace métier");
        }
        note.setDeleted(true);
        noteRepository.save(note);
    }

    // ========================================================================
    // ANNONCES DU DÉPARTEMENT (communication interne)
    // ========================================================================

    public List<Map<String, Object>> listAnnouncements(UUID departmentId) {
        assertCanManage(departmentId);
        return announcementRepository.findByDepartmentIdAndDeletedFalseOrderByCreatedAtDesc(departmentId).stream()
                .map(this::toAnnouncementMap)
                .toList();
    }

    public Map<String, Object> createAnnouncement(UUID departmentId, DepartmentAnnouncementRequest request) {
        assertCanManage(departmentId);
        DepartmentAnnouncement.Cible cible = request.cible() != null ? request.cible() : DepartmentAnnouncement.Cible.TOUS;
        if (cible == DepartmentAnnouncement.Cible.EQUIPE && request.teamId() != null) {
            DepartmentTeam team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new EntityNotFoundException("DepartmentTeam", request.teamId()));
            if (!team.getDepartmentId().equals(departmentId)) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "L'équipe doit appartenir au département", "TEAM_DEPARTMENT_MISMATCH");
            }
        }
        if (cible == DepartmentAnnouncement.Cible.POSTE && request.positionId() != null) {
            DepartmentPosition position = positionRepository.findById(request.positionId())
                    .orElseThrow(() -> new EntityNotFoundException("DepartmentPosition", request.positionId()));
            if (!position.getDepartmentId().equals(departmentId)) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "Le poste doit appartenir au département", "POSITION_DEPARTMENT_MISMATCH");
            }
        }
        Set<UUID> memberIds = new java.util.HashSet<>();
        if (cible == DepartmentAnnouncement.Cible.MEMBRES) {
            if (request.memberIds() == null || request.memberIds().isEmpty()) {
                throw new com.discipolat.common.domain.BusinessRuleException(
                        "Cible MEMBRES : au moins un membre est requis", "ANNOUNCEMENT_MEMBERS_REQUIRED");
            }
            for (UUID memberId : request.memberIds()) {
                if (!soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(memberId, departmentId)) {
                    throw new com.discipolat.common.domain.BusinessRuleException(
                            "Le membre ciblé n'appartient pas au département", "ANNOUNCEMENT_MEMBER_NOT_IN_DEPARTMENT");
                }
            }
            memberIds.addAll(request.memberIds());
        }
        DepartmentAnnouncement announcement = DepartmentAnnouncement.builder()
                .departmentId(departmentId)
                .auteurId(securityUtils.getCurrentUserId())
                .titre(request.titre().trim())
                .message(request.message().trim())
                .cible(cible)
                .teamId(cible == DepartmentAnnouncement.Cible.EQUIPE ? request.teamId() : null)
                .positionId(cible == DepartmentAnnouncement.Cible.POSTE ? request.positionId() : null)
                .memberIds(memberIds)
                .build();
        announcement = announcementRepository.save(announcement);
        activityRepository.save(DepartmentActivity.builder()
                .departmentId(departmentId)
                .actorId(securityUtils.getCurrentUserId())
                .actorNom(userName(securityUtils.getCurrentUserId()))
                .action("ANNOUNCEMENT_CREATED")
                .entityType("ANNOUNCEMENT")
                .entityId(announcement.getId())
                .details("Annonce « " + announcement.getTitre() + " » publiée")
                .build());
        return toAnnouncementMap(announcement);
    }

    public void deleteAnnouncement(UUID departmentId, UUID announcementId) {
        assertCanManage(departmentId);
        DepartmentAnnouncement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new EntityNotFoundException("DepartmentAnnouncement", announcementId));
        if (!announcement.getDepartmentId().equals(departmentId)) {
            throw new org.springframework.security.access.AccessDeniedException("Accès refusé : annonce hors de votre espace métier");
        }
        announcement.setDeleted(true);
        announcementRepository.save(announcement);
    }

    // ========================================================================
    // STATISTIQUES DU DÉPARTEMENT (données réelles)
    // ========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentStats(UUID departmentId) {
        assertCanManage(departmentId);
        List<SoulDepartment> links = soulDepartmentRepository.findByDepartmentIdAndActifTrue(departmentId);
        List<Soul> souls = links.isEmpty() ? List.of()
                : soulRepository.findAllById(links.stream().map(SoulDepartment::getSoulId).toList()).stream()
                        .filter(s -> !s.isDeleted()).toList();
        Map<UUID, Soul> soulsById = souls.stream().collect(Collectors.toMap(Soul::getId, s -> s));

        // ---- Effectif ----
        Map<String, Object> effectif = new LinkedHashMap<>();
        effectif.put("total", (long) souls.size());
        effectif.put("actifs", souls.stream().filter(s -> s.getStatut() == com.discipolat.common.enums.StatutAme.ACTIF).count());
        effectif.put("enIntegration", souls.stream().filter(s -> s.getStatut() == com.discipolat.common.enums.StatutAme.EN_INTEGRATION).count());
        effectif.put("enVeille", souls.stream().filter(s -> s.getStatut() == com.discipolat.common.enums.StatutAme.EN_VEILLE).count());
        effectif.put("decroches", souls.stream().filter(s -> s.getStatut() == com.discipolat.common.enums.StatutAme.DECROCHE).count());
        effectif.put("nouveaux30j", souls.stream()
                .filter(s -> s.getDateIntegration() != null && s.getDateIntegration().isAfter(LocalDate.now().minusDays(30)))
                .count());

        // ---- Évolution mensuelle de l'effectif (12 derniers mois) ----
        List<Map<String, Object>> evolutionEffectif = new ArrayList<>();
        Map<UUID, SoulDepartment> linkBySoul = links.stream()
                .collect(Collectors.toMap(SoulDepartment::getSoulId, l -> l, (a, b) -> a));
        List<SoulDepartment> allLinks = soulDepartmentRepository.findByDepartmentId(departmentId);
        YearMonth now = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            long ajoutes = allLinks.stream()
                    .filter(l -> l.getDateAffectation() != null && YearMonth.from(l.getDateAffectation()).equals(ym))
                    .count();
            long sortis = allLinks.stream()
                    .filter(l -> l.getDateDesaffectation() != null && YearMonth.from(l.getDateDesaffectation()).equals(ym))
                    .count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mois", ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + ym.getYear() % 100);
            m.put("ajoutes", ajoutes);
            m.put("sortis", sortis);
            m.put("solde", ajoutes - sortis);
            evolutionEffectif.add(m);
        }

        // ---- Présence (fiches de présence du département) ----
        List<MemberPresence> presences = souls.isEmpty() ? List.of()
                : presenceRepository.findBySoulIdInOrderBySemaineDesc(souls.stream().map(Soul::getId).toList());
        long presents = presences.stream().filter(r -> Boolean.TRUE.equals(r.getPresent())).count();
        Map<String, Object> presence = new LinkedHashMap<>();
        presence.put("total", (long) presences.size());
        presence.put("presents", presents);
        presence.put("absents", presences.size() - presents);
        presence.put("taux", presences.isEmpty() ? 0.0
                : Math.round(presents * 1000.0 / presences.size()) / 10.0);
        List<Map<String, Object>> evolutionPresence = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            List<MemberPresence> monthRecords = presences.stream()
                    .filter(r -> YearMonth.from(r.getSemaine()).equals(ym))
                    .toList();
            long p = monthRecords.stream().filter(r -> Boolean.TRUE.equals(r.getPresent())).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mois", ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + ym.getYear() % 100);
            m.put("presents", p);
            m.put("absents", monthRecords.size() - p);
            m.put("taux", monthRecords.isEmpty() ? 0.0 : Math.round(p * 1000.0 / monthRecords.size()) / 10.0);
            evolutionPresence.add(m);
        }

        // ---- Tâches ----
        List<DepartmentTask> tasks = taskRepository.findByDepartmentIdOrderByEcheanceAsc(departmentId);
        Map<String, Object> taches = new LinkedHashMap<>();
        taches.put("total", (long) tasks.size());
        taches.put("ouvertes", tasks.stream().filter(DepartmentTask::isOpen).count());
        taches.put("enRetard", tasks.stream().filter(DepartmentTask::isOverdue).count());
        taches.put("terminees", tasks.stream().filter(t -> t.getStatut() == DepartmentTask.TaskStatus.TERMINEE
                || t.getStatut() == DepartmentTask.TaskStatus.VALIDEE).count());
        Map<String, Long> parStatut = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getStatut().name(), Collectors.counting()));
        taches.put("parStatut", parStatut);
        List<Map<String, Object>> evolutionTaches = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            long creees = tasks.stream()
                    .filter(t -> t.getCreatedAt() != null && YearMonth.from(t.getCreatedAt().toLocalDate()).equals(ym))
                    .count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("mois", ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + ym.getYear() % 100);
            m.put("creees", creees);
            evolutionTaches.add(m);
        }

        // ---- Discipline ----
        Map<String, Long> disciplineParCategorie = new LinkedHashMap<>();
        for (Soul s : souls) {
            disciplineRepository.findByAmeIdAndDeletedFalseOrderByCreatedAtDesc(s.getId()).stream()
                    .filter(e -> e.getCategorie() != null)
                    .collect(Collectors.groupingBy(e -> e.getCategorie().name(), Collectors.counting()))
                    .forEach((k, v) -> disciplineParCategorie.merge(k, v, Long::sum));
        }

        // ---- Équipes / postes / affectations ----
        long equipesActives = teamRepository.countByDepartmentIdAndStatut(departmentId, DepartmentTeam.TeamStatus.ACTIVE);
        long equipesArchivees = teamRepository.countByDepartmentIdAndStatut(departmentId, DepartmentTeam.TeamStatus.ARCHIVED);
        Map<String, Long> equipesParType = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.groupingBy(t -> t.getType().name(), Collectors.counting()));
        long postesActifs = positionRepository.countByDepartmentIdAndStatut(departmentId, DepartmentPosition.PositionStatus.ACTIVE);
        List<DepartmentAssignment> activeAssignments = assignmentRepository.findByDepartmentIdAndActifTrue(departmentId);
        long membresAffectes = activeAssignments.stream().map(DepartmentAssignment::getMemberId).distinct().count();

        // ---- Charge de travail ----
        Map<UUID, Long> openByMember = tasks.stream()
                .filter(DepartmentTask::isOpen)
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(DepartmentTask::getAssignedTo, Collectors.counting()));
        Map<UUID, Long> overdueByMember = tasks.stream()
                .filter(DepartmentTask::isOverdue)
                .filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(DepartmentTask::getAssignedTo, Collectors.counting()));
        List<Map<String, Object>> chargeParMembre = openByMember.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("memberId", e.getKey());
                    m.put("memberNom", soulName(e.getKey()));
                    m.put("tachesOuvertes", e.getValue());
                    m.put("enRetard", overdueByMember.getOrDefault(e.getKey(), 0L));
                    return m;
                })
                .sorted((a, b) -> Long.compare((long) b.get("tachesOuvertes"), (long) a.get("tachesOuvertes")))
                .toList();

        // ---- Événements à venir liés au département ----
        List<Map<String, Object>> evenements = evenementsDuDepartement(departmentId, soulsById);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("departmentId", departmentId);
        stats.put("effectif", effectif);
        stats.put("evolutionEffectif", evolutionEffectif);
        stats.put("presence", presence);
        stats.put("evolutionPresence", evolutionPresence);
        stats.put("taches", taches);
        stats.put("evolutionTaches", evolutionTaches);
        stats.put("disciplineParCategorie", disciplineParCategorie);
        stats.put("equipes", Map.of("actives", equipesActives, "archivees", equipesArchivees, "parType", equipesParType));
        stats.put("postesActifs", postesActifs);
        stats.put("affectations", Map.of(
                "actives", (long) activeAssignments.size(),
                "membresAffectes", membresAffectes,
                "tauxAffectation", souls.isEmpty() ? 0.0
                        : Math.round(membresAffectes * 1000.0 / souls.size()) / 10.0));
        stats.put("chargeParMembre", chargeParMembre);
        stats.put("evenements", evenements);
        return stats;
    }

    private List<Map<String, Object>> evenementsDuDepartement(UUID departmentId, Map<UUID, Soul> soulsById) {
        // Événements à venir (30 jours) où un membre du département est inscrit
        // (via son compte utilisateur) ou dont le responsable est l'organisateur.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(30);
        List<Event> events = eventRepository.findByDateDebutBetweenAndDeletedFalse(now, horizon);
        if (events.isEmpty()) return List.of();
        Set<UUID> memberUserIds = soulsById.values().stream()
                .map(Soul::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        UUID responsableId = departmentService.findById(departmentId).getResponsableId();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Event ev : events) {
            boolean concerne = (responsableId != null && responsableId.equals(ev.getOrganisateurId()))
                    || eventRegistrationRepository.findByEventId(ev.getId()).stream()
                            .anyMatch(r -> memberUserIds.contains(r.getUtilisateurId()));
            if (!concerne) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ev.getId());
            m.put("titre", ev.getTitre());
            m.put("lieu", ev.getLieu());
            m.put("dateDebut", ev.getDateDebut() != null ? ev.getDateDebut().toString() : null);
            m.put("statut", ev.getStatut());
            result.add(m);
        }
        return result;
    }

    // ========================================================================
    // ALERTES INTELLIGENTES (règles, dédupliquées)
    // ========================================================================

    /**
     * Génère les alertes automatiques du département (absence répétée, inactivité,
     * tâche en retard) puis retourne les alertes actives du département. Les
     * seuils sont lus depuis le paramétrage du département ({@code settings})
     * — jamais de constantes hardcodées. Les alertes sont dédupliquées par
     * (type, âme, département) : on ne crée jamais deux alertes identiques
     * tant que la précédente n'a pas été résolue.
     */
    public List<Map<String, Object>> getIntelligentAlerts(UUID departmentId) {
        assertCanManage(departmentId);
        com.discipolat.modules.departments.domain.DepartmentSetting settings =
                settingsService.effectiveSettings(departmentId);
        List<SoulDepartment> links = soulDepartmentRepository.findByDepartmentIdAndActifTrue(departmentId);
        if (links.isEmpty()) return List.of();
        List<Soul> souls = soulRepository.findAllById(links.stream().map(SoulDepartment::getSoulId).toList());

        // Règle 1 : absence répétée (seuil + période configurables)
        for (Soul soul : souls) {
            List<MemberPresence> records = presenceRepository.findBySoulIdInOrderBySemaineDesc(List.of(soul.getId()));
            if (records.size() >= settings.getAbsencePeriode()) {
                long absences = records.stream().limit(settings.getAbsencePeriode())
                        .filter(r -> !Boolean.TRUE.equals(r.getPresent())).count();
                if (absences >= settings.getAbsenceSeuil()
                        && !alertRepository.existsByDepartmentIdAndAmeIdAndTypeAlerteAndStatut(
                                departmentId, soul.getId(), "ABSENCE_REPETEE", StatutAlerte.ACTIVE)) {
                    alertRepository.save(Alert.builder()
                            .departmentId(departmentId)
                            .ameId(soul.getId())
                            .cible("PERSONNE")
                            .priorite("HAUTE")
                            .typeAlerte("ABSENCE_REPETEE")
                            .titre("Absences répétées — " + soul.getNomComplet())
                            .message(soul.getNomComplet() + " est absent(e) sur " + absences
                                    + " des " + settings.getAbsencePeriode()
                                    + " dernières semaines. Un suivi est recommandé.")
                            .dateDeclenchement(LocalDateTime.now())
                            .statut(StatutAlerte.ACTIVE)
                            .build());
                }
            }
        }

        // Règle 2 : inactivité (aucune fiche de présence depuis N mois, 0 = désactivée)
        if (settings.getInactiviteMois() > 0) {
            LocalDate cutoff = LocalDate.now().minusMonths(settings.getInactiviteMois());
            Map<UUID, LocalDate> joinDate = links.stream().collect(Collectors.toMap(
                    SoulDepartment::getSoulId,
                    l -> l.getDateAffectation() != null ? l.getDateAffectation().toLocalDate() : LocalDate.now()));
            for (Soul soul : souls) {
                List<MemberPresence> records = presenceRepository.findBySoulIdInOrderBySemaineDesc(List.of(soul.getId()));
                boolean inactive = records.isEmpty()
                        ? joinDate.getOrDefault(soul.getId(), LocalDate.now()).isBefore(cutoff)
                        : records.get(0).getSemaine().isBefore(cutoff);
                if (inactive
                        && !alertRepository.existsByDepartmentIdAndAmeIdAndTypeAlerteAndStatut(
                                departmentId, soul.getId(), "INACTIVITE", StatutAlerte.ACTIVE)) {
                    alertRepository.save(Alert.builder()
                            .departmentId(departmentId)
                            .ameId(soul.getId())
                            .cible("PERSONNE")
                            .priorite("MOYENNE")
                            .typeAlerte("INACTIVITE")
                            .titre("Membre inactif — " + soul.getNomComplet())
                            .message(soul.getNomComplet() + " n'a aucune fiche de présence depuis "
                                    + settings.getInactiviteMois() + " mois. Un accompagnement est recommandé.")
                            .dateDeclenchement(LocalDateTime.now())
                            .statut(StatutAlerte.ACTIVE)
                            .build());
                }
            }
        }

        // Règle 3 : tâches en retard (activable/désactivable)
        List<DepartmentTask> tasks = taskRepository.findByDepartmentIdOrderByEcheanceAsc(departmentId);
        for (DepartmentTask task : tasks) {
            if (settings.isTacheRetardAlerte() && task.isOverdue() && task.getAssignedTo() != null
                    && !alertRepository.existsByDepartmentIdAndAmeIdAndTypeAlerteAndStatut(
                            departmentId, task.getAssignedTo(), "TACHE_EN_RETARD", StatutAlerte.ACTIVE)) {
                alertRepository.save(Alert.builder()
                        .departmentId(departmentId)
                        .ameId(task.getAssignedTo())
                        .cible("PERSONNE")
                        .priorite("MOYENNE")
                        .typeAlerte("TACHE_EN_RETARD")
                        .titre("Tâche en retard — " + task.getTitre())
                        .message("La tâche « " + task.getTitre() + " » est en retard (échéance : "
                                + task.getEcheance() + ").")
                        .dateDeclenchement(LocalDateTime.now())
                        .statut(StatutAlerte.ACTIVE)
                        .build());
            }
        }

        // Retour des alertes actives du département
        List<Alert> active = alertRepository.findByDepartmentIdAndStatut(departmentId, StatutAlerte.ACTIVE).stream()
                .sorted(Comparator.comparing(Alert::getDateDeclenchement).reversed())
                .toList();
        return active.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("typeAlerte", a.getTypeAlerte());
            m.put("titre", a.getTitre());
            m.put("message", a.getMessage());
            m.put("priorite", a.getPriorite());
            m.put("ameId", a.getAmeId());
            m.put("ameNom", a.getAmeId() != null ? soulName(a.getAmeId()) : null);
            m.put("dateDeclenchement", ts(a.getDateDeclenchement()));
            return m;
        }).toList();
    }

    // ========================================================================
    // EXPORT CSV DES MEMBRES
    // ========================================================================

    @Transactional(readOnly = true)
    public String exportMembersCsv(UUID departmentId) {
        assertCanManage(departmentId);
        List<SoulDepartment> links = soulDepartmentRepository.findByDepartmentIdAndActifTrue(departmentId);
        if (links.isEmpty()) return bom() + "nom;prenom;telephone;email;profession;statut;typeDisciple;dateIntegration;famille;faiseur;ville;equipes;postes\n";
        List<Soul> souls = soulRepository.findAllById(links.stream().map(SoulDepartment::getSoulId).toList());
        Map<UUID, Soul> byId = souls.stream().collect(Collectors.toMap(Soul::getId, s -> s));
        Map<UUID, String> teamNames = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentTeam::getId, DepartmentTeam::getNom));
        Map<UUID, String> positionNames = positionRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .collect(Collectors.toMap(DepartmentPosition::getId, DepartmentPosition::getNom));
        List<DepartmentAssignment> active = assignmentRepository.findByDepartmentIdAndActifTrue(departmentId);
        Map<UUID, String> familleNoms = new HashMap<>();
        Map<UUID, String> faiseurNoms = new HashMap<>();

        StringBuilder sb = new StringBuilder(bom());
        sb.append("nom;prenom;telephone;email;profession;statut;typeDisciple;dateIntegration;famille;faiseur;ville;equipes;postes\n");
        for (SoulDepartment link : links) {
            Soul soul = byId.get(link.getSoulId());
            if (soul == null) continue;
            List<String> teams = new ArrayList<>();
            List<String> positions = new ArrayList<>();
            for (DepartmentAssignment a : active) {
                if (!a.getMemberId().equals(soul.getId())) continue;
                if (a.getTeamId() != null && teamNames.containsKey(a.getTeamId())) teams.add(teamNames.get(a.getTeamId()));
                if (a.getPositionId() != null && positionNames.containsKey(a.getPositionId())) positions.add(positionNames.get(a.getPositionId()));
            }
            String familleNom = soul.getFamilleId() != null
                    ? familleNoms.computeIfAbsent(soul.getFamilleId(), id -> familyRepository.findById(id).map(Family::getNom).orElse(""))
                    : "";
            String faiseurNom = soul.getFaiseurId() != null
                    ? faiseurNoms.computeIfAbsent(soul.getFaiseurId(), id -> userName(id) == null ? "" : userName(id))
                    : "";
            String ville = soul.getAdresse() != null && soul.getAdresse().contains(",")
                    ? soul.getAdresse().split(",")[0].trim() : (soul.getAdresse() != null ? soul.getAdresse() : "");
            sb.append(csv(soul.getNom())).append(';')
                    .append(csv(soul.getPrenom())).append(';')
                    .append(csv(soul.getTelephone())).append(';')
                    .append(csv(soul.getEmail())).append(';')
                    .append(csv(soul.getProfession())).append(';')
                    .append(soul.getStatut() != null ? soul.getStatut().name() : "").append(';')
                    .append(soul.getTypeDisciple() != null ? soul.getTypeDisciple().name() : "").append(';')
                    .append(soul.getDateIntegration() != null ? soul.getDateIntegration().toString() : "").append(';')
                    .append(csv(familleNom)).append(';')
                    .append(csv(faiseurNom)).append(';')
                    .append(csv(ville)).append(';')
                    .append(csv(String.join(" | ", teams))).append(';')
                    .append(csv(String.join(" | ", positions)))
                    .append('\n');
        }
        return sb.toString();
    }

    private String bom() {
        return "\uFEFF";
    }

    private String csv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    // ========================================================================
    // IMPORT CSV DES MEMBRES (prévisualisation + import)
    // ========================================================================

    /**
     * Import de membres depuis des lignes CSV (nom obligatoire). Détection des
     * doublons par email ou téléphone. En mode preview, aucune écriture : chaque
     * ligne est classée CREER / DOUBLON / ERREUR. En mode import, les nouvelles
     * âmes sont créées, rattachées au département (origine MANUEL) et affectées
     * aux équipes/postes existants (par nom).
     */
    public Map<String, Object> importMembers(UUID departmentId, List<Map<String, Object>> rows, boolean preview) {
        assertCanManage(departmentId);
        Map<String, UUID> teamByName = teamRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .filter(t -> t.getStatut() == DepartmentTeam.TeamStatus.ACTIVE)
                .collect(Collectors.toMap(t -> t.getNom().trim().toLowerCase(), DepartmentTeam::getId, (a, b) -> a));
        Map<String, UUID> positionByName = positionRepository.findByDepartmentIdOrderByNomAsc(departmentId).stream()
                .filter(p -> p.getStatut() == DepartmentPosition.PositionStatus.ACTIVE)
                .collect(Collectors.toMap(p -> p.getNom().trim().toLowerCase(), DepartmentPosition::getId, (a, b) -> a));

        int cree = 0, doublon = 0, erreur = 0;
        List<Map<String, Object>> resultats = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> resultat = new LinkedHashMap<>();
            String nom = str(row.get("nom"));
            String prenom = str(row.get("prenom"));
            String email = str(row.get("email"));
            String telephone = str(row.get("telephone"));
            try {
                if (nom.isBlank()) {
                    throw new com.discipolat.common.domain.BusinessRuleException("Nom manquant", "IMPORT_NOM_MANQUANT");
                }
                // Détection des doublons (email ou téléphone existants sur une âme non supprimée)
                Soul existing = null;
                if (!email.isBlank()) {
                    existing = findSoulByEmail(email);
                }
                if (existing == null && !telephone.isBlank()) {
                    existing = findSoulByTelephone(telephone);
                }
                if (existing != null) {
                    doublon++;
                    resultat.put("statut", "DOUBLON");
                    resultat.put("message", "Déjà inscrit : " + existing.getNomComplet());
                    resultat.put("doublonId", existing.getId());
                    resultat.put("nom", nom);
                    resultat.put("prenom", prenom);
                    resultat.put("email", email);
                    resultat.put("telephone", telephone);
                    resultats.add(resultat);
                    continue;
                }
                UUID teamId = null;
                String equipeNom = str(row.get("equipe"));
                if (!equipeNom.isBlank()) {
                    teamId = teamByName.get(equipeNom.trim().toLowerCase());
                    if (teamId == null) {
                        throw new com.discipolat.common.domain.BusinessRuleException(
                                "Équipe introuvable : " + equipeNom, "IMPORT_EQUIPE_INCONNUE");
                    }
                }
                UUID positionId = null;
                String posteNom = str(row.get("poste"));
                if (!posteNom.isBlank()) {
                    positionId = positionByName.get(posteNom.trim().toLowerCase());
                    if (positionId == null) {
                        throw new com.discipolat.common.domain.BusinessRuleException(
                                "Poste introuvable : " + posteNom, "IMPORT_POSTE_INCONNU");
                    }
                }
                resultat.put("statut", "CREER");
                resultat.put("nom", nom);
                resultat.put("prenom", prenom);
                resultat.put("email", email);
                resultat.put("telephone", telephone);
                resultat.put("equipe", equipeNom);
                resultat.put("poste", posteNom);
                if (!preview) {
                    Soul soul = Soul.builder()
                            .nom(nom.trim())
                            .prenom(prenom.isBlank() ? null : prenom.trim())
                            .email(email.isBlank() ? null : email.trim())
                            .telephone(telephone.isBlank() ? null : telephone.trim())
                            .profession(str(row.get("profession")).isBlank() ? null : str(row.get("profession")).trim())
                            .adresse(str(row.get("ville")).isBlank() ? null : str(row.get("ville")).trim())
                            .typeDisciple(com.discipolat.common.enums.TypeDisciple.NOUVEL_ARRIVANT)
                            .dateIntegration(LocalDate.now())
                            .statut(com.discipolat.common.enums.StatutAme.EN_INTEGRATION)
                            .etatSpirituel("NOUVEAU_CONVERTI")
                            .niveauCroissance(1)
                            .faiseurId(securityUtils.getCurrentUserId())
                            .build();
                    soul = soulRepository.save(soul);
                    soulDepartmentRepository.save(SoulDepartment.builder()
                            .soulId(soul.getId())
                            .departmentId(departmentId)
                            .actif(true)
                            .createdBy(securityUtils.getCurrentUserId())
                            .origine("MANUEL")
                            .build());
                    if (teamId != null) {
                        assignmentRepository.save(DepartmentAssignment.builder()
                                .departmentId(departmentId)
                                .memberId(soul.getId())
                                .teamId(teamId)
                                .role(DepartmentAssignment.AssignmentRole.MEMBRE)
                                .dateDebut(LocalDate.now())
                                .actif(true)
                                .createdBy(securityUtils.getCurrentUserId())
                                .build());
                    }
                    if (positionId != null) {
                        assignmentRepository.save(DepartmentAssignment.builder()
                                .departmentId(departmentId)
                                .memberId(soul.getId())
                                .positionId(positionId)
                                .role(DepartmentAssignment.AssignmentRole.MEMBRE)
                                .dateDebut(LocalDate.now())
                                .actif(true)
                                .createdBy(securityUtils.getCurrentUserId())
                                .build());
                    }
                    activityRepository.save(DepartmentActivity.builder()
                            .departmentId(departmentId)
                            .actorId(securityUtils.getCurrentUserId())
                            .actorNom(userName(securityUtils.getCurrentUserId()))
                            .action("IMPORT_MEMBER_CREATED")
                            .entityType("SOUL")
                            .entityId(soul.getId())
                            .details("Membre importé : " + soul.getNomComplet())
                            .build());
                    resultat.put("id", soul.getId());
                    cree++;
                }
                resultats.add(resultat);
            } catch (Exception e) {
                erreur++;
                resultat.put("statut", "ERREUR");
                resultat.put("nom", nom);
                resultat.put("prenom", prenom);
                resultat.put("message", e.getMessage());
                resultats.add(resultat);
            }
        }
        if (preview) {
            cree = (int) resultats.stream().filter(r -> "CREER".equals(r.get("statut"))).count();
            doublon = (int) resultats.stream().filter(r -> "DOUBLON".equals(r.get("statut"))).count();
            erreur = (int) resultats.stream().filter(r -> "ERREUR".equals(r.get("statut"))).count();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("preview", preview);
        result.put("cree", cree);
        result.put("doublon", doublon);
        result.put("erreur", erreur);
        result.put("total", rows.size());
        result.put("resultats", resultats);
        return result;
    }

    private Soul findSoulByEmail(String email) {
        return soulRepository.findByEmailIgnoreCaseAndDeletedFalse(email).orElse(null);
    }

    private Soul findSoulByTelephone(String telephone) {
        return soulRepository.findByTelephoneAndDeletedFalse(telephone).orElse(null);
    }

    private String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }
}
