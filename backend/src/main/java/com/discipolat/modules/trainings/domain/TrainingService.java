package com.discipolat.modules.trainings.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.trainings.api.*;
import com.discipolat.modules.users.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Plateforme de formation : cours structurés en modules, quiz par module,
 * suivi de progression et délivrance de certificats numériques uniques.
 */
@Service
@Transactional
public class TrainingService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final QuizQuestionRepository quizRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final TrainingCertificateRepository certificateRepository;
    private final ModuleCompletionRepository completionRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    public TrainingService(CourseRepository courseRepository,
                           CourseModuleRepository moduleRepository,
                           QuizQuestionRepository quizRepository,
                           CourseEnrollmentRepository enrollmentRepository,                            TrainingCertificateRepository certificateRepository,
                           ModuleCompletionRepository completionRepository,
                           UserRepository userRepository,
                           SecurityUtils securityUtils) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.quizRepository = quizRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.certificateRepository = certificateRepository;
        this.completionRepository = completionRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
    }

    // ============================================================
    // Catalogue & détail
    // ============================================================

    @Transactional(readOnly = true)
    public List<CourseResponse> findAll() {
        return courseRepository.findByActifTrueOrderByTitreAsc().stream()
                .map(c -> toResponse(c, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse findById(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course", courseId));
        return toResponse(course, false);
    }

    @Transactional(readOnly = true)
    public List<CourseModule> modules(UUID courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course", courseId);
        }
        return moduleRepository.findByCourseIdOrderByOrdreAsc(courseId);
    }

    /**
     * Quiz d'un module, sanitizé : la bonne réponse n'est jamais exposée au client.
     */
    @Transactional(readOnly = true)
    public List<QuizQuestionResponse> quiz(UUID moduleId) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new EntityNotFoundException("CourseModule", moduleId);
        }
        return quizRepository.findByModuleIdOrderByOrdreAsc(moduleId).stream()
                .map(QuizQuestionResponse::from)
                .toList();
    }

    /**
     * Ajout d'une question de quiz (réservé au pasteur / admin).
     */
    public QuizQuestion addQuestion(UUID moduleId, CreateQuestionRequest request) {
        if (!moduleRepository.existsById(moduleId)) {
            throw new EntityNotFoundException("CourseModule", moduleId);
        }
        return quizRepository.save(QuizQuestion.builder()
                .moduleId(moduleId)
                .question(request.question())
                .propositions(request.propositions())
                .reponseIndex(request.reponseIndex())
                .ordre(request.ordre())
                .build());
    }

    /**
     * Marque un module comme lu/complété (pour les modules sans quiz),
     * mettant à jour la progression individuelle.
     */
    public CourseEnrollment completeModuleRead(UUID courseId, UUID moduleId) {
        return completeModule(courseId, moduleId);
    }

    // ============================================================
    // Inscription & progression
    // ============================================================

    public CourseEnrollment enroll(UUID courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course", courseId);
        }
        UUID userId = securityUtils.getCurrentUserId();
        return enrollmentRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseGet(() -> enrollmentRepository.save(CourseEnrollment.builder()
                        .courseId(courseId)
                        .userId(userId)
                        .statut(CourseEnrollment.Statut.INSCRIT)
                        .build()));
    }

    /**
     * Marque un module comme terminé (après validation du quiz) et recalcule
     * la progression individuelle : modules complétés / total de modules.
     */
    public CourseEnrollment completeModule(UUID courseId, UUID moduleId) {
        UUID userId = securityUtils.getCurrentUserId();
        CourseEnrollment enrollment = enrollmentRepository.findByCourseIdAndUserId(courseId, userId)
                .orElseGet(() -> enroll(courseId));

        CourseModule module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new EntityNotFoundException("CourseModule", moduleId));
        if (!module.getCourseId().equals(courseId)) {
            throw new com.discipolat.common.exception.BadRequestException(
                    "Ce module n'appartient pas à ce cours");
        }
        if (completionRepository.findByEnrollmentIdAndModuleId(enrollment.getId(), moduleId).isEmpty()) {
            completionRepository.save(ModuleCompletion.builder()
                    .enrollmentId(enrollment.getId())
                    .moduleId(moduleId)
                    .build());
        }

        long totalModules = moduleRepository.countByCourseId(courseId);
        long completed = completionRepository.countByEnrollmentId(enrollment.getId());
        int progression = totalModules > 0
                ? (int) Math.round((completed * 100.0) / totalModules)
                : 0;
        enrollment.setProgression(Math.max(enrollment.getProgression(), progression));
        if (enrollment.getProgression() >= 100) {
            enrollment.setStatut(CourseEnrollment.Statut.TERMINE);
            enrollment.setDateTerminaison(LocalDateTime.now());
        } else if (enrollment.getStatut() == CourseEnrollment.Statut.INSCRIT) {
            enrollment.setStatut(CourseEnrollment.Statut.EN_COURS);
        }
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<CourseEnrollment> myEnrollments() {
        return enrollmentRepository.findByUserIdOrderByDateInscriptionDesc(securityUtils.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public List<TrainingCertificate> myCertificates() {
        return certificateRepository.findByUserIdOrderByDelivreLeDesc(securityUtils.getCurrentUserId());
    }

    // ============================================================
    // Quiz & certificat
    // ============================================================

    /** Soumet un quiz : calcule le score, met à jour la progression, délivre un certificat si 100%. */
    public QuizResultResponse submitQuiz(UUID courseId, SubmitQuizRequest request) {
        UUID userId = securityUtils.getCurrentUserId();
        List<QuizQuestion> questions = quizRepository.findByModuleIdOrderByOrdreAsc(request.moduleId());
        if (questions.isEmpty()) {
            throw new EntityNotFoundException("QuizQuestion", request.moduleId());
        }

        int bonnes = 0;
        for (QuizQuestion q : questions) {
            Integer choix = request.reponses().get(q.getId());
            if (choix != null && choix == q.getReponseIndex()) {
                bonnes++;
            }
        }
        int score = Math.round((bonnes * 100.0f) / questions.size());
        boolean reussi = score >= 70;

        // Met à jour le meilleur score et marque le module complété
        CourseEnrollment enrollment = enrollmentRepository
                .findByCourseIdAndUserId(courseId, userId)
                .orElseGet(() -> enroll(courseId));
        if (enrollment.getScoreQuiz() == null || score > enrollment.getScoreQuiz()) {
            enrollment.setScoreQuiz(score);
        }
        enrollmentRepository.save(enrollment);

        boolean certificat = false;
        if (reussi) {
            CourseEnrollment updated = completeModule(courseId, request.moduleId());
            if (updated.getProgression() >= 100) {
                certificat = issueCertificate(updated, score);
            }
        }
        return new QuizResultResponse(score, bonnes, questions.size(), reussi, certificat);
    }

    private boolean issueCertificate(CourseEnrollment enrollment, int score) {
        if (certificateRepository.findByEnrollmentId(enrollment.getId()).isPresent()) {
            return false; // déjà délivré
        }
        if (!enrollment.getStatut().equals(CourseEnrollment.Statut.TERMINE)) {
            enrollment.setStatut(CourseEnrollment.Statut.TERMINE);
            enrollment.setDateTerminaison(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        }
        String numero = "CERT-" + enrollment.getUserId().toString().substring(0, 8).toUpperCase()
                + "-" + enrollment.getCourseId().toString().substring(0, 8).toUpperCase();
        certificateRepository.save(TrainingCertificate.builder()
                .enrollmentId(enrollment.getId())
                .numero(numero)
                .userId(enrollment.getUserId())
                .courseId(enrollment.getCourseId())
                .scoreFinal(score)
                .build());
        return true;
    }

    // ============================================================
    // Statistiques globales (pasteur / admin)
    // ============================================================

    /**
     * Statistiques de la plateforme de formation calculées sur les données
     * réelles : cours actifs, inscriptions, certificats délivrés, progression
     * moyenne, répartition par catégorie et par statut d'inscription.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> stats() {
        List<Course> courses = courseRepository.findByActifTrueOrderByTitreAsc();
        List<CourseEnrollment> enrollments = enrollmentRepository.findAll();
        long certificats = certificateRepository.count();

        double moyenne = enrollments.stream()
                .mapToInt(CourseEnrollment::getProgression)
                .average().orElse(0.0);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("nbCours", courses.size());
        result.put("nbInscrits", enrollments.size());
        result.put("nbCertificats", certificats);
        result.put("progressionMoyenne", Math.round(moyenne));
        result.put("parCategorie", courses.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCategorie() == null || c.getCategorie().isBlank()
                                ? "DISCIPOLAT" : c.getCategorie(),
                        Collectors.counting())));
        result.put("parStatut", enrollments.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getStatut() != null ? e.getStatut().name() : "INSCRIT",
                        Collectors.counting())));
        return result;
    }

    // ============================================================
    // Administration (pasteur / admin)
    // ============================================================

    public Course createCourse(CreateCourseRequest request) {
        Course course = Course.builder()
                .titre(request.titre())
                .description(request.description())
                .categorie(request.categorie() != null ? request.categorie() : "DISCIPOLAT")
                .niveau(request.niveau())
                .dureeMinutes(request.dureeMinutes())
                .formateurId(request.formateurId())
                .imageUrl(request.imageUrl())
                .actif(true)
                .build();
        return courseRepository.save(course);
    }

    public CourseModule addModule(UUID courseId, CreateModuleRequest request) {
        if (!courseRepository.existsById(courseId)) {
            throw new EntityNotFoundException("Course", courseId);
        }
        return moduleRepository.save(CourseModule.builder()
                .courseId(courseId)
                .titre(request.titre())
                .contenu(request.contenu())
                .videoUrl(request.videoUrl())
                .ordre(request.ordre())
                .build());
    }

    // ============================================================
    // Helpers
    // ============================================================

    private CourseResponse toResponse(Course c, boolean includeDetails) {
        String formateurNom = c.getFormateurId() != null
                ? userRepository.findById(c.getFormateurId())
                        .map(u -> u.getFirstName() + " " + u.getLastName()).orElse(null)
                : null;
        long nbModules = moduleRepository.countByCourseId(c.getId());
        long nbInscrits = enrollmentRepository.countByCourseId(c.getId());
        return CourseResponse.from(c, formateurNom, nbModules, nbInscrits);
    }
}
