package com.discipolat.modules.transfers.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.*;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.audit.domain.AuditService;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.files.domain.FileEntity;
import com.discipolat.modules.files.domain.FileEntityRepository;
import com.discipolat.modules.notifications.domain.NotificationService;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartment;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.transfers.api.*;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MOTEUR DE WORKFLOW INTELLIGENT ET CONFIGURABLE DES TRANSFERTS.
 *
 * Le circuit de validation n'est PAS codé en dur : il est lu depuis la base
 * (TransferWorkflowConfig + TransferWorkflowStep), configurable par le pasteur.
 *
 * Cycle de vie : BROUILLON → SOUMIS → EN_ATTENTE_VALIDATION →
 * VALIDATION_PARTIELLE → VALIDE → EXECUTE (ou REFUSE / ANNULE), puis ARCHIVE.
 *
 * Une fois toutes les validations obtenues, l'exécution est AUTOMATIQUE :
 * relations mises à jour, statistiques/tableaux de bord (calculés à la volée),
 * historiques métier, permissions et notifications — aucune étape manuelle.
 * Chaque transition est historisée (TransferHistory) et journalisée (audit).
 */
@Service
@Transactional
public class TransferWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(TransferWorkflowService.class);

    private final TransferRequestRepository requestRepository;
    private final TransferDecisionRepository decisionRepository;
    private final TransferHistoryRepository historyRepository;
    private final TransferAttachmentRepository attachmentRepository;
    private final TransferWorkflowConfigRepository configRepository;
    private final TransferWorkflowStepRepository stepRepository;
    private final TransferExecutor executor;
    private final SoulRepository soulRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final DepartmentRepository departmentRepository;
    private final SoulDepartmentRepository soulDepartmentRepository;
    private final FileEntityRepository fileEntityRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final EntityPropagationPublisher propagationPublisher;
    private final SecurityUtils securityUtils;

    public TransferWorkflowService(TransferRequestRepository requestRepository,
                                   TransferDecisionRepository decisionRepository,
                                   TransferHistoryRepository historyRepository,
                                   TransferAttachmentRepository attachmentRepository,
                                   TransferWorkflowConfigRepository configRepository,
                                   TransferWorkflowStepRepository stepRepository,
                                   TransferExecutor executor,
                                   SoulRepository soulRepository,
                                   UserRepository userRepository,
                                   FamilyRepository familyRepository,
                                   DepartmentRepository departmentRepository,
                                   SoulDepartmentRepository soulDepartmentRepository,
                                   FileEntityRepository fileEntityRepository,
                                   NotificationService notificationService,
                                   AuditService auditService,
                                   EntityPropagationPublisher propagationPublisher,
                                   SecurityUtils securityUtils) {
        this.requestRepository = requestRepository;
        this.decisionRepository = decisionRepository;
        this.historyRepository = historyRepository;
        this.attachmentRepository = attachmentRepository;
        this.configRepository = configRepository;
        this.stepRepository = stepRepository;
        this.executor = executor;
        this.soulRepository = soulRepository;
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
        this.departmentRepository = departmentRepository;
        this.soulDepartmentRepository = soulDepartmentRepository;
        this.fileEntityRepository = fileEntityRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.propagationPublisher = propagationPublisher;
        this.securityUtils = securityUtils;
    }

    // ========================================================================
    // CRÉATION / MISE À JOUR / LECTURE
    // ========================================================================

    /** Crée une demande de transfert (statut BROUILLON). */
    public TransferRequest create(CreateTransferRequest request) {
        TransferWorkflowConfig config = activeConfig(request.type());
        if (!securityUtils.isSuperUser() && !config.getRolesInitiateurs().contains(securityUtils.getCurrentUserRole())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Votre rôle actif ne permet pas d'initier ce type de transfert");
        }

        String personneType = request.personneType() != null ? request.personneType()
                : (isUserTargetType(request.type()) ? "USER" : "SOUL");
        validatePerson(request.type(), request.personneId(), personneType, request.nouvelleAffectation());

        Map<String, Object> ancienne = request.ancienneAffectation() != null && !request.ancienneAffectation().isEmpty()
                ? request.ancienneAffectation()
                : computeAncienneAffectation(request.type(), request.personneId(), request.nouvelleAffectation());

        TransferRequest req = TransferRequest.builder()
                .type(request.type())
                .statut(TransferStatus.BROUILLON)
                .personneId(request.personneId())
                .personneType(personneType)
                .ancienneAffectation(ancienne)
                .nouvelleAffectation(request.nouvelleAffectation())
                .demandeurId(securityUtils.getCurrentUserId())
                .justification(request.justification())
                .priorite(request.priorite() != null ? request.priorite() : PrioriteTransfert.MOYENNE)
                .commentaires(request.commentaires())
                .reglesExecution(request.reglesExecution())
                .build();
        req = requestRepository.save(req);

        if (request.fichierIds() != null) {
            linkFiles(req.getId(), request.fichierIds());
        }

        history(req, "CREATION", null, TransferStatus.BROUILLON, null, null,
                Map.of("type", req.getType().name(), "personneId", req.getPersonneId()));
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishCreated("TRANSFER_REQUEST", req.getId(),
                Map.of("type", req.getType().name(), "personneId", req.getPersonneId(),
                        "demandeurId", req.getDemandeurId()),
                "Transfert créé: " + req.getType().name());
        return req;
    }

    /** Met à jour une demande en brouillon. */
    public TransferRequest update(UUID id, UpdateTransferRequest request) {
        TransferRequest req = findById(id);
        if (req.getStatut() != TransferStatus.BROUILLON) {
            throw new BusinessRuleException("Seule une demande en brouillon peut être modifiée");
        }
        if (request.nouvelleAffectation() != null && !request.nouvelleAffectation().isEmpty()) {
            req.setNouvelleAffectation(request.nouvelleAffectation());
        }
        if (request.justification() != null && !request.justification().isBlank()) {
            req.setJustification(request.justification());
        }
        if (request.priorite() != null) req.setPriorite(request.priorite());
        if (request.commentaires() != null) req.setCommentaires(request.commentaires());
        if (request.fichierIds() != null) {
            attachmentRepository.deleteByTransferRequestId(id);
            linkFiles(id, request.fichierIds());
        }
        req = requestRepository.save(req);
        history(req, "MODIFICATION", req.getStatut(), req.getStatut(), "Demande modifiée par le demandeur", null, null);
        return req;
    }

    @Transactional(readOnly = true)
    public TransferRequest findById(UUID id) {
        TransferRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TransferRequest", id));
        if (!canView(req)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Accès refusé à cette demande de transfert");
        }
        return req;
    }

    /** Liste paginée, scopée par le rôle actif (demandeur, personne concernée ou validateur potentiel). */
    @Transactional(readOnly = true)
    public Page<TransferRequest> findAll(TransferStatus statut, TransferType type, Pageable pageable) {
        if (securityUtils.isSuperUser()) {
            if (statut != null && type != null) return requestRepository.findByStatutAndTypeOrderByCreatedAtDesc(statut, type, pageable);
            if (statut != null) return requestRepository.findByStatutOrderByCreatedAtDesc(statut, pageable);
            if (type != null) return requestRepository.findByTypeOrderByCreatedAtDesc(type, pageable);
            return requestRepository.findAll(pageable);
        }
        List<TransferRequest> visible = requestRepository.findAll().stream()
                .filter(this::canView)
                .filter(r -> statut == null || r.getStatut() == statut)
                .filter(r -> type == null || r.getType() == type)
                .sorted(Comparator.comparing(TransferRequest::getCreatedAt).reversed())
                .toList();
        return paginate(visible, pageable);
    }

    // ========================================================================
    // CYCLE DE VIE
    // ========================================================================

    /** Soumission : démarre le circuit de validation (ou exécution immédiate si aucune étape). */
    public TransferRequest submit(UUID id) {
        TransferRequest req = findById(id);
        if (!securityUtils.isSuperUser() && !req.getDemandeurId().equals(securityUtils.getCurrentUserId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Seul le demandeur peut soumettre cette demande");
        }
        if (req.getStatut() == TransferStatus.EXECUTE || req.getStatut() == TransferStatus.VALIDE
                || req.getStatut() == TransferStatus.REFUSE || req.getStatut() == TransferStatus.ANNULE
                || req.getStatut() == TransferStatus.ARCHIVE) {
            throw new BusinessRuleException("Cette demande ne peut plus être soumise (statut : " + req.getStatut() + ")");
        }

        TransferWorkflowConfig config = configRepository.findByTransferType(req.getType())
                .filter(TransferWorkflowConfig::isActif)
                .orElse(null);
        List<TransferWorkflowStep> steps = config != null
                ? stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(config.getId())
                : List.of();

        if (config == null || steps.isEmpty()) {
            // Aucun circuit : exécution automatique immédiate.
            req.setStatut(TransferStatus.VALIDE);
            req.setWorkflowConfigId(config != null ? config.getId() : null);
            req.setDateSoumission(LocalDateTime.now());
            requestRepository.save(req);
            history(req, "SOUMISSION", TransferStatus.BROUILLON, TransferStatus.VALIDE,
                    "Soumission sans circuit de validation — exécution automatique", null, null);
            // ===== PROPAGATION CENTRALISÉE =====
            propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                    TransferStatus.BROUILLON.name(), TransferStatus.VALIDE.name(),
                    "Transfert soumis et exécuté (aucun circuit)");
            completeAndExecute(req, config);
            return req;
        }

        boolean firstSubmit = req.getStatut() == TransferStatus.BROUILLON;
        req.setWorkflowConfigId(config.getId());
        req.setDateSoumission(LocalDateTime.now());
        req.setDelaiLimite(LocalDateTime.now().plusHours(config.getDelaiTraitementHeures()));
        if (firstSubmit) {
            req.setEtapeCourante(0);
            req.setApprobationsObtenues(0);
        }
        TransferStatus ancien = req.getStatut();
        req.setStatut(req.getApprobationsObtenues() > 0 ? TransferStatus.VALIDATION_PARTIELLE
                : TransferStatus.EN_ATTENTE_VALIDATION);
        requestRepository.save(req);
        history(req, "SOUMISSION", ancien, req.getStatut(), "Demande soumise au circuit de validation", null, null);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                ancien.name(), req.getStatut().name(),
                "Transfert soumis au circuit de validation");

        notifyValidators(req, config, steps);
        return req;
    }

    /**
     * Décision d'un validateur : approbation, refus, demande d'informations
     * ou renvoi pour correction. Toutes les décisions sont motivées et historisées.
     */
    public TransferRequest decide(UUID id, DecideRequest request) {
        TransferRequest req = findById(id);
        if (req.getStatut() != TransferStatus.EN_ATTENTE_VALIDATION
                && req.getStatut() != TransferStatus.VALIDATION_PARTIELLE) {
            throw new BusinessRuleException("Cette demande n'est pas en attente de validation (statut : " + req.getStatut() + ")");
        }

        TransferWorkflowConfig config = configRepository.findById(req.getWorkflowConfigId()).orElse(null);
        if (config == null) {
            throw new BusinessRuleException("La configuration du workflow n'existe plus — contactez l'administrateur");
        }
        List<TransferWorkflowStep> steps = stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(config.getId());
        if (steps.isEmpty()) {
            throw new BusinessRuleException("Le circuit de validation est vide — contactez l'administrateur");
        }

        UUID validatorId = securityUtils.getCurrentUserId();
        String activeRole = securityUtils.getCurrentUserRole();
        boolean superUser = securityUtils.isSuperUser();

        // En mode SÉQUENTIEL, seule l'étape COURANTE est validable : un validateur
        // d'une étape ultérieure ne peut pas devancer le circuit.
        List<TransferWorkflowStep> pendingSteps = pendingSteps(req, steps);
        if (pendingSteps.isEmpty()) {
            throw new BusinessRuleException("Toutes les validations ont déjà été obtenues");
        }
        boolean sequentiel = config.getModeValidation() == ValidationMode.SEQUENTIEL;
        List<TransferWorkflowStep> validatable = sequentiel ? pendingSteps.subList(0, 1) : pendingSteps;

        if (!superUser) {
            boolean allowed = validatable.stream()
                    .anyMatch(s -> s.getRolesValidateurs().contains(activeRole));
            if (!allowed) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Votre rôle actif (« " + activeRole + " ») ne peut pas valider cette étape");
            }
            boolean already = decisionRepository
                    .findFirstByTransferRequestIdAndValidateurIdOrderByCreatedAtDesc(req.getId(), validatorId)
                    .map(d -> d.getDecision() == DecisionType.APPROBATION)
                    .orElse(false);
            if (already) {
                throw new BusinessRuleException("Vous avez déjà validé cette demande");
            }
        }

        // Étape visée : la courante en mode séquentiel, sinon la première étape requise restante
        // que le validateur est habilité à valider.
        TransferWorkflowStep step = sequentiel
                ? validatable.get(0)
                : validatable.stream()
                        .filter(s -> superUser || s.getRolesValidateurs().contains(activeRole))
                        .findFirst()
                        .orElse(validatable.get(0));

        decisionRepository.save(TransferDecision.builder()
                .transferRequestId(req.getId())
                .validateurId(validatorId)
                .roleValidateur(activeRole)
                .decision(request.decision())
                .motivation(request.motivation())
                .etapeOrdre(step.getEtapeOrdre())
                .build());

        TransferStatus ancien = req.getStatut();
        switch (request.decision()) {
            case APPROBATION -> handleApproval(req, config, steps, validatorId);
            case REFUS -> {
                req.setStatut(TransferStatus.REFUSE);
                requestRepository.save(req);
                history(req, "REFUS", ancien, TransferStatus.REFUSE,
                        request.motivation() != null ? request.motivation() : "Demande refusée", null, null);
                // ===== PROPAGATION CENTRALISÉE =====
                propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                        ancien.name(), TransferStatus.REFUSE.name(),
                        "Transfert refusé");
                notifyUser(req.getDemandeurId(), transfertLabel(req) + " a été refusée. " + orEmpty(request.motivation()),
                        TypeNotification.TRANSFERT_REFUSEE, req.getId());
                notifyConcerned(req, transfertLabel(req) + " a été refusée.", TypeNotification.TRANSFERT_REFUSEE);
            }
            case DEMANDE_INFORMATIONS -> {
                req.setStatut(TransferStatus.SOUMIS);
                requestRepository.save(req);
                history(req, "DEMANDE_INFORMATIONS", ancien, TransferStatus.SOUMIS,
                        request.motivation() != null ? request.motivation() : "Informations complémentaires demandées", null, null);
                // ===== PROPAGATION CENTRALISÉE =====
                propagationPublisher.publishUpdated("TRANSFER_REQUEST", req.getId(),
                        Map.of("statut", ancien.name()),
                        Map.of("statut", TransferStatus.SOUMIS.name()),
                        "Informations complémentaires demandées");
                notifyUser(req.getDemandeurId(), "Des informations complémentaires sont demandées pour " + transfertLabel(req)
                        + ". " + orEmpty(request.motivation()), TypeNotification.TRANSFERT_INFOS_DEMANDEES, req.getId());
            }
            case RENVOI_CORRECTION -> {
                req.setStatut(TransferStatus.SOUMIS);
                requestRepository.save(req);
                history(req, "RENVOI_CORRECTION", ancien, TransferStatus.SOUMIS,
                        request.motivation() != null ? request.motivation() : "Renvoi pour correction", null, null);
                // ===== PROPAGATION CENTRALISÉE =====
                propagationPublisher.publishUpdated("TRANSFER_REQUEST", req.getId(),
                        Map.of("statut", ancien.name()),
                        Map.of("statut", TransferStatus.SOUMIS.name()),
                        "Renvoi pour correction");
                notifyUser(req.getDemandeurId(), "Votre demande (" + transfertLabel(req) + ") a été renvoyée pour correction. "
                        + orEmpty(request.motivation()), TypeNotification.TRANSFERT_CORRECTION, req.getId());
            }
        }
        return req;
    }

    /** Annulation par le demandeur (ou un super-utilisateur). */
    public TransferRequest cancel(UUID id) {
        TransferRequest req = findById(id);
        if (!securityUtils.isSuperUser() && !req.getDemandeurId().equals(securityUtils.getCurrentUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Seul le demandeur peut annuler cette demande");
        }
        if (req.getStatut() == TransferStatus.EXECUTE || req.getStatut() == TransferStatus.VALIDE
                || req.getStatut() == TransferStatus.REFUSE || req.getStatut() == TransferStatus.ARCHIVE) {
            throw new BusinessRuleException("Cette demande ne peut plus être annulée (statut : " + req.getStatut() + ")");
        }
        TransferStatus ancien = req.getStatut();
        req.setStatut(TransferStatus.ANNULE);
        requestRepository.save(req);
        history(req, "ANNULATION", ancien, TransferStatus.ANNULE, "Demande annulée", null, null);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                ancien.name(), TransferStatus.ANNULE.name(),
                "Transfert annulé");
        notifyConcerned(req, transfertLabel(req) + " a été annulée.", TypeNotification.TRANSFERT_ANNULEE);
        return req;
    }

    /** Archivage (super-utilisateurs uniquement). */
    public TransferRequest archive(UUID id) {
        if (!securityUtils.isSuperUser()) {
            throw new org.springframework.security.access.AccessDeniedException("Seul le pasteur ou l'admin peut archiver une demande");
        }
        TransferRequest req = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("TransferRequest", id));
        TransferStatus ancien = req.getStatut();
        req.setStatut(TransferStatus.ARCHIVE);
        requestRepository.save(req);
        history(req, "ARCHIVAGE", ancien, TransferStatus.ARCHIVE, "Demande archivée", null, null);
        // ===== PROPAGATION CENTRALISÉE =====
        propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                ancien.name(), TransferStatus.ARCHIVE.name(),
                "Transfert archivé");
        return req;
    }

    // ========================================================================
    // LECTURES COMPLÉMENTAIRES
    // ========================================================================

    @Transactional(readOnly = true)
    public List<TransferHistoryResponse> getHistory(UUID id) {
        findById(id);
        return historyRepository.findByTransferRequestIdOrderByCreatedAtAsc(id).stream()
                .map(h -> new TransferHistoryResponse(h.getId(), h.getAction(),
                        h.getAncienStatut() != null ? h.getAncienStatut().name() : null,
                        h.getNouveauStatut() != null ? h.getNouveauStatut().name() : null,
                        h.getUtilisateurId(), userName(h.getUtilisateurId()), h.getRoleActif(),
                        h.getCommentaire(), h.getAncienneValeur(), h.getNouvelleValeur(), h.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransferDecision> getDecisions(UUID id) {
        findById(id);
        return decisionRepository.findByTransferRequestIdOrderByCreatedAtDesc(id);
    }

    @Transactional(readOnly = true)
    public TransferDetailResponse getDetail(UUID id) {
        TransferRequest req = findById(id);
        TransferResponse base = toResponse(req);

        List<TransferWorkflowStep> steps = req.getWorkflowConfigId() != null
                ? stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(req.getWorkflowConfigId())
                : List.of();
        Set<Integer> approvedStepOrders = approvedStepOrders(req);
        List<TransferDetailResponse.EtapeValidation> etapes = steps.stream()
                .map(s -> new TransferDetailResponse.EtapeValidation(s.getId(), s.getEtapeOrdre(),
                        s.getRolesValidateurs(), s.getLabel(), s.getDescription(), s.isRequis(),
                        approvedStepOrders.contains(s.getEtapeOrdre())))
                .toList();

        List<TransferDetailResponse.DecisionItem> decisions = decisionRepository
                .findByTransferRequestIdOrderByCreatedAtDesc(req.getId()).stream()
                .map(d -> new TransferDetailResponse.DecisionItem(d.getId(), d.getValidateurId(),
                        userName(d.getValidateurId()), d.getRoleValidateur(), d.getDecision().name(),
                        d.getMotivation(), d.getEtapeOrdre(), d.getCreatedAt()))
                .toList();

        List<TransferDetailResponse.AttachmentItem> piecesJointes = attachmentRepository
                .findByTransferRequestIdOrderByCreatedAtAsc(req.getId()).stream()
                .map(a -> new TransferDetailResponse.AttachmentItem(a.getId(), a.getFileId(),
                        fileEntityRepository.findById(a.getFileId()).map(FileEntity::getNom).orElse(null),
                        fileEntityRepository.findById(a.getFileId()).map(FileEntity::getChemin).orElse(null),
                        a.getCreatedAt()))
                .toList();

        String modeValidation = req.getWorkflowConfigId() != null
                ? configRepository.findById(req.getWorkflowConfigId()).map(c -> c.getModeValidation().name()).orElse(null)
                : null;

        return new TransferDetailResponse(base, etapes, decisions, piecesJointes,
                canValidate(req), securityUtils.getCurrentUserRole(), modeValidation);
    }

    @Transactional(readOnly = true)
    public List<TransferConfigurationResponse> getConfigurations() {
        String activeRole = securityUtils.getCurrentUserRole();
        return configRepository.findByActifTrueOrderByTransferTypeAsc().stream()
                .map(c -> new TransferConfigurationResponse(c.getId(), c.getTransferType(), c.getLabel(),
                        c.getDescription(), c.isActif(), c.getRolesInitiateurs(),
                        securityUtils.isSuperUser() || c.getRolesInitiateurs().contains(activeRole),
                        stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(c.getId()).stream()
                                .map(s -> s.getLabel() + " (" + String.join(", ", s.getRolesValidateurs()) + ")")
                                .toList()))
                .toList();
    }

    // ========================================================================
    // VALIDATION / EXÉCUTION
    // ========================================================================

    private void handleApproval(TransferRequest req, TransferWorkflowConfig config,
                                List<TransferWorkflowStep> steps, UUID validatorId) {
        TransferStatus ancien = req.getStatut();
        boolean complete;
        if (config.getModeValidation() == ValidationMode.SEQUENTIEL) {
            req.setEtapeCourante(Math.min(req.getEtapeCourante() + 1, steps.size()));
            complete = req.getEtapeCourante() >= steps.size();
        } else {
            req.setApprobationsObtenues(req.getApprobationsObtenues() + 1);
            int requis = config.getModeValidation() == ValidationMode.N_VALIDATIONS_REQUISES
                    ? config.getNombreValidationsRequises()
                    : (int) steps.stream().filter(TransferWorkflowStep::isRequis).count();
            complete = req.getApprobationsObtenues() >= requis;
        }

        if (complete) {
            req.setStatut(TransferStatus.VALIDE);
            requestRepository.save(req);
            history(req, "VALIDATION", ancien, TransferStatus.VALIDE,
                    "Toutes les validations requises ont été obtenues", null, null);
            // ===== PROPAGATION CENTRALISÉE =====
            propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                    ancien.name(), TransferStatus.VALIDE.name(),
                    "Transfert validé — exécution automatique");
            completeAndExecute(req, config);
        } else {
            req.setStatut(req.getEtapeCourante() > 0 || req.getApprobationsObtenues() > 0
                    ? TransferStatus.VALIDATION_PARTIELLE : TransferStatus.EN_ATTENTE_VALIDATION);
            requestRepository.save(req);
            history(req, "VALIDATION", ancien, req.getStatut(),
                    "Validation enregistrée — le circuit se poursuit", null, null);
            // ===== PROPAGATION CENTRALISÉE =====
            propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                    ancien.name(), req.getStatut().name(),
                    "Validation partielle enregistrée");
            notifyValidators(req, config, steps);
            notifyUser(req.getDemandeurId(), "Une validation a été enregistrée pour " + transfertLabel(req),
                    TypeNotification.TRANSFERT_VALIDATION, req.getId());
        }
    }

    /** Finalise : exécution automatique du transfert puis passage en EXECUTE. */
    private void completeAndExecute(TransferRequest req, TransferWorkflowConfig config) {
        // Fusion : les règles de la config servent de socle, la demande écrase
        // les clés qu'elle fournit explicitement (ex : transfererAmes).
        Map<String, Object> regles = new HashMap<>();
        if (config != null && config.getReglesExecution() != null) {
            regles.putAll(config.getReglesExecution());
        }
        if (req.getReglesExecution() != null) {
            regles.putAll(req.getReglesExecution());
        }
        try {
            executor.execute(req, regles);
            req.setStatut(TransferStatus.EXECUTE);
            req.setDateExecution(LocalDateTime.now());
            requestRepository.save(req);
            history(req, "EXECUTION", TransferStatus.VALIDE, TransferStatus.EXECUTE,
                    "Transfert exécuté automatiquement", req.getAncienneAffectation(), req.getNouvelleAffectation());
            // ===== PROPAGATION CENTRALISÉE =====
            propagationPublisher.publishStatusChanged("TRANSFER_REQUEST", req.getId(),
                    TransferStatus.VALIDE.name(), TransferStatus.EXECUTE.name(),
                    "Transfert exécuté");
            notifyUser(req.getDemandeurId(), transfertLabel(req) + " a été exécuté avec succès",
                    TypeNotification.TRANSFERT_EXECUTEE, req.getId());
            notifyConcerned(req, transfertLabel(req) + " a été exécuté.", TypeNotification.TRANSFERT_EXECUTEE);
        } catch (Exception e) {
            // ATOMICITÉ : l'exception remonte hors de la transaction → l'ensemble de la
            // validation finale et de l'exécution est annulé (aucune écriture partielle,
            // aucune donnée corrompue). La demande redevient VALIDATION_PARTIELLE et le
            // validateur peut corriger puis valider à nouveau.
            log.error("Échec de l'exécution du transfert {} : {}", req.getId(), e.getMessage(), e);
            throw new BusinessRuleException("L'exécution du transfert a échoué : " + e.getMessage());
        }
    }

    // ========================================================================
    // PERMISSIONS & VISIBILITÉ
    // ========================================================================

    private boolean canView(TransferRequest req) {
        if (securityUtils.isSuperUser()) return true;
        UUID me = securityUtils.getCurrentUserId();
        if (req.getDemandeurId().equals(me) || req.getPersonneId().equals(me)) return true;
        return isPotentialValidator(req);
    }

    private boolean canValidate(TransferRequest req) {
        if (req.getStatut() != TransferStatus.EN_ATTENTE_VALIDATION
                && req.getStatut() != TransferStatus.VALIDATION_PARTIELLE) return false;
        if (securityUtils.isSuperUser()) return true;
        return isPotentialValidator(req);
    }

    private boolean isPotentialValidator(TransferRequest req) {
        if (req.getWorkflowConfigId() == null) return false;
        String activeRole = securityUtils.getCurrentUserRole();
        return stepRepository.findByWorkflowConfigIdOrderByEtapeOrdreAsc(req.getWorkflowConfigId()).stream()
                .anyMatch(s -> s.getRolesValidateurs().contains(activeRole));
    }

    private List<TransferWorkflowStep> pendingSteps(TransferRequest req, List<TransferWorkflowStep> steps) {
        if (steps.isEmpty()) return List.of();
        // N_VALIDATIONS_REQUISES : chaque validateur (distinct) compte, sur n'importe
        // quelle étape requise de son rôle. La liste reste donc complète.
        if (modeOf(req) == ValidationMode.N_VALIDATIONS_REQUISES) {
            return steps.stream().filter(TransferWorkflowStep::isRequis).toList();
        }
        // SEQUENTIEL : l'étape courante (etapeCourante). PARALLELE : les étapes restantes.
        int idx = Math.min(req.getEtapeCourante(), steps.size() - 1);
        return steps.subList(idx, steps.size()).stream()
                .filter(s -> !approvedStepOrders(req).contains(s.getEtapeOrdre()))
                .toList();
    }

    private ValidationMode modeOf(TransferRequest req) {
        if (req.getWorkflowConfigId() == null) return ValidationMode.SEQUENTIEL;
        return configRepository.findById(req.getWorkflowConfigId())
                .map(TransferWorkflowConfig::getModeValidation)
                .orElse(ValidationMode.SEQUENTIEL);
    }

    /** Ordres des étapes déjà approuvées (par n'importe quel validateur). */
    private Set<Integer> approvedStepOrders(TransferRequest req) {
        return decisionRepository.findByTransferRequestIdOrderByCreatedAtDesc(req.getId()).stream()
                .filter(d -> d.getDecision() == DecisionType.APPROBATION)
                .map(TransferDecision::getEtapeOrdre)
                .collect(Collectors.toSet());
    }

    private void notifyValidators(TransferRequest req, TransferWorkflowConfig config, List<TransferWorkflowStep> steps) {
        if (!config.isNotificationsAuto()) return;
        List<TransferWorkflowStep> pending = pendingSteps(req, steps);
        if (pending.isEmpty()) return;
        Set<String> roles = pending.stream()
                .flatMap(s -> s.getRolesValidateurs().stream())
                .collect(Collectors.toSet());
        String titre = "Action requise : " + transfertLabel(req);
        String message = "Une demande de transfert (" + transfertLabel(req) + ") attend votre validation. Priorité : "
                + req.getPriorite() + ".";
        userRepository.findAll().stream()
                .filter(u -> !u.isDeleted() && u.getActiveRole() != null && roles.contains(u.getActiveRole().name()))
                .forEach(u -> notifyUser(u.getId(), titre, TypeNotification.TRANSFERT_VALIDATION,
                        req.getId(), message));
    }

    private void notifyConcerned(TransferRequest req, String message, TypeNotification type) {
        // La personne concernée peut être une âme (SOUL) : son compte utilisateur
        // est résolu via Soul.userId — jamais l'UUID brut de l'âme (FK users).
        UUID cible = req.getPersonneId();
        if ("SOUL".equals(req.getPersonneType())) {
            cible = soulRepository.findById(req.getPersonneId())
                    .map(Soul::getUserId)
                    .orElse(null);
        }
        if (cible != null) {
            notifyUser(cible, message, type, req.getId());
        }
    }

    private void notifyUser(UUID userId, String titre, TypeNotification type, UUID entiteId) {
        notifyUser(userId, titre, type, entiteId, null);
    }

    private void notifyUser(UUID userId, String titre, TypeNotification type, UUID entiteId, String message) {
        try {
            notificationService.create(userId, type, CanalNotification.IN_APP,
                    titre, message != null ? message : titre, entiteId, "TRANSFER");
        } catch (Exception e) {
            log.warn("Notification de transfert non envoyée à {} : {}", userId, e.getMessage());
        }
    }

    // ========================================================================
    // HELPERS MÉTIER
    // ========================================================================

    private void validatePerson(TransferType type, UUID personneId, String personneType,
                                Map<String, Object> nouvelleAffectation) {
        UUID targetId = idOf(nouvelleAffectation);
        if (targetId == null) {
            throw new BusinessRuleException("La nouvelle affectation doit contenir un identifiant (id)");
        }
        if ("USER".equals(personneType) || isUserTargetType(type)) {
            user(personneId);
        } else {
            soul(personneId);
        }
        switch (type) {
            case MEMBRE_DEPARTEMENT_TRANSFERT, MEMBRE_DEPARTEMENT_AJOUT, MEMBRE_DEPARTEMENT_RETRAIT ->
                    department(targetId);
            case DISCIPLE_FAMILLE_TRANSFERT, FAISEUR_FAMILLE_TRANSFERT, CHEF_FAMILLE_TRANSFERT, CHEF_ADJOINT_CHANGEMENT ->
                    family(targetId);
            case FAISEUR_DISCIPLE_CHANGEMENT -> user(targetId);
            case RESPONSABLE_DEPARTEMENT_CHANGEMENT -> department(targetId);
        }
    }

    private boolean isUserTargetType(TransferType type) {
        return type == TransferType.CHEF_FAMILLE_TRANSFERT
                || type == TransferType.CHEF_ADJOINT_CHANGEMENT
                || type == TransferType.FAISEUR_FAMILLE_TRANSFERT
                || type == TransferType.RESPONSABLE_DEPARTEMENT_CHANGEMENT;
    }

    /** Calcule l'affectation actuelle à partir de l'état courant des données. */
    private Map<String, Object> computeAncienneAffectation(TransferType type, UUID personneId,
                                                           Map<String, Object> nouvelleAffectation) {
        UUID targetId = idOf(nouvelleAffectation);
        return switch (type) {
            case DISCIPLE_FAMILLE_TRANSFERT -> soul(personneId).getFamilleId() != null
                    ? Map.of("type", "FAMILLE", "id", soul(personneId).getFamilleId(),
                            "nom", familyName(soul(personneId).getFamilleId()))
                    : null;
            case FAISEUR_FAMILLE_TRANSFERT -> user(personneId).getFamilleGereeId() != null
                    ? Map.of("type", "FAMILLE", "id", user(personneId).getFamilleGereeId(),
                            "nom", familyName(user(personneId).getFamilleGereeId()))
                    : null;
            case FAISEUR_DISCIPLE_CHANGEMENT -> Map.of("type", "FAISEUR", "id", soul(personneId).getFaiseurId(),
                    "nom", userName(soul(personneId).getFaiseurId()));
            case MEMBRE_DEPARTEMENT_TRANSFERT -> firstActiveDept(personneId);
            case RESPONSABLE_DEPARTEMENT_CHANGEMENT -> department(targetId).getResponsableId() != null
                    ? Map.of("type", "UTILISATEUR", "id", department(targetId).getResponsableId(),
                            "nom", userName(department(targetId).getResponsableId()))
                    : null;
            case CHEF_FAMILLE_TRANSFERT -> family(targetId).getChefFamilleId() != null
                    ? Map.of("type", "UTILISATEUR", "id", family(targetId).getChefFamilleId(),
                            "nom", userName(family(targetId).getChefFamilleId()))
                    : null;
            case CHEF_ADJOINT_CHANGEMENT -> family(targetId).getChefAdjointId() != null
                    ? Map.of("type", "UTILISATEUR", "id", family(targetId).getChefAdjointId(),
                            "nom", userName(family(targetId).getChefAdjointId()))
                    : null;
            default -> null; // AJOUT / RETRAIT département : pas d'affectation antérieure pertinente
        };
    }

    private Map<String, Object> firstActiveDept(UUID soulId) {
        List<SoulDepartment> depts = soulDepartmentRepository.findBySoulIdAndActifTrue(soulId);
        if (depts.isEmpty()) return null;
        UUID deptId = depts.get(0).getDepartmentId();
        return Map.of("type", "DEPARTEMENT", "id", deptId, "nom", departmentName(deptId));
    }

    private void linkFiles(UUID requestId, List<UUID> fileIds) {
        for (UUID fileId : fileIds) {
            if (!fileEntityRepository.existsById(fileId)) {
                throw new BusinessRuleException("Pièce jointe introuvable : " + fileId);
            }
            attachmentRepository.save(TransferAttachment.builder()
                    .transferRequestId(requestId)
                    .fileId(fileId)
                    .uploadedBy(securityUtils.getCurrentUserId())
                    .build());
        }
    }

    private void history(TransferRequest req, String action, TransferStatus ancien, TransferStatus nouveau,
                         String commentaire, Map<String, Object> ancienneValeur, Map<String, Object> nouvelleValeur) {
        try {
            historyRepository.save(TransferHistory.builder()
                    .transferRequestId(req.getId())
                    .action(action)
                    .ancienStatut(ancien)
                    .nouveauStatut(nouveau)
                    .utilisateurId(securityUtils.getCurrentUserId())
                    .roleActif(securityUtils.getCurrentUserRole())
                    .commentaire(commentaire)
                    .ancienneValeur(ancienneValeur)
                    .nouvelleValeur(nouvelleValeur)
                    .build());
        } catch (Exception e) {
            log.warn("Historique de transfert non enregistré : {}", e.getMessage());
        }
    }

    private void audit(String action, TransferRequest req, Map<String, Object> ancien, Map<String, Object> nouveau) {
        try {
            auditService.log(action, "TRANSFER_REQUEST", req.getId(), ancien, nouveau, null);
        } catch (Exception e) {
            log.warn("Entrée d'audit non enregistrée : {}", e.getMessage());
        }
    }

    /** Mapper léger pour la liste : 3 requêtes par ligne (personne, demandeur, nb d'étapes). */
    public TransferResponse toResponse(TransferRequest req) {
        return new TransferResponse(req.getId(), req.getType(), req.getStatut(),
                req.getPersonneId(), req.getPersonneType(), personneNom(req),
                req.getAncienneAffectation(), req.getNouvelleAffectation(),
                req.getDemandeurId(), userName(req.getDemandeurId()),
                req.getJustification(), req.getPriorite(), req.getCommentaires(),
                req.getDateSoumission(), req.getDateExecution(), req.getDelaiLimite(),
                req.getEtapeCourante(), req.getApprobationsObtenues(),
                totalEtapes(req), req.getCreatedAt());
    }

    private Integer totalEtapes(TransferRequest req) {
        if (req.getWorkflowConfigId() == null) return 0;
        return (int) stepRepository.countByWorkflowConfigId(req.getWorkflowConfigId());
    }

    private String personneNom(TransferRequest req) {
        if ("USER".equals(req.getPersonneType())) return userName(req.getPersonneId());
        return soulRepository.findById(req.getPersonneId())
                .map(Soul::getNomComplet).orElse("—");
    }

    private String transfertLabel(TransferRequest req) {
        return "La demande de transfert (" + req.getType().name() + ")";
    }

    private UUID idOf(Map<String, Object> map) {
        if (map == null) return null;
        Object id = map.get("id");
        if (id == null) return null;
        if (id instanceof UUID uuid) return uuid;
        return UUID.fromString(id.toString());
    }

    private String userName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> u.getFirstName() + " " + u.getLastName())
                .orElse("—");
    }

    private String familyName(UUID familyId) {
        if (familyId == null) return null;
        return familyRepository.findById(familyId).map(Family::getNom).orElse("—");
    }

    private String departmentName(UUID deptId) {
        if (deptId == null) return null;
        return departmentRepository.findById(deptId).map(Department::getNom).orElse("—");
    }

    private User user(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));
    }

    private Soul soul(UUID id) {
        return soulRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Soul", id));
    }

    private Family family(UUID id) {
        return familyRepository.findById(id)
                .filter(f -> !f.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Family", id));
    }

    private Department department(UUID id) {
        return departmentRepository.findById(id)
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new EntityNotFoundException("Department", id));
    }

    private TransferWorkflowConfig activeConfig(TransferType type) {
        return configRepository.findByTransferType(type)
                .filter(TransferWorkflowConfig::isActif)
                .orElseThrow(() -> new BusinessRuleException(
                        "Ce type de transfert n'est pas configuré : " + type));
    }

    private String orEmpty(String s) {
        return s != null ? s : "";
    }

    private Page<TransferRequest> paginate(List<TransferRequest> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<TransferRequest> content = start < list.size() ? list.subList(start, end) : List.of();
        return new PageImpl<>(content, pageable, list.size());
    }
}
