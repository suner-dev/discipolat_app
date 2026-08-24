package com.discipolat.modules.trainings.api;

import com.discipolat.modules.trainings.domain.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trainings")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    // ============================================================
    // Catalogue (tous les rôles)
    // ============================================================

    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<CourseResponse>> findAll() {
        return ResponseEntity.ok(trainingService.findAll());
    }

    @GetMapping("/courses/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<CourseResponse> findById(@PathVariable UUID courseId) {
        return ResponseEntity.ok(trainingService.findById(courseId));
    }

    @GetMapping("/courses/{courseId}/modules")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<CourseModule>> modules(@PathVariable UUID courseId) {
        return ResponseEntity.ok(trainingService.modules(courseId));
    }

    /** Quiz sanitizé : la bonne réponse n'est jamais exposée au client. */
    @GetMapping("/modules/{moduleId}/quiz")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<QuizQuestionResponse>> quiz(@PathVariable UUID moduleId) {
        return ResponseEntity.ok(trainingService.quiz(moduleId));
    }

    // ============================================================
    // Inscription, progression, quiz, certificats
    // ============================================================

    @PostMapping("/courses/{courseId}/enroll")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<CourseEnrollment> enroll(@PathVariable UUID courseId) {
        return ResponseEntity.ok(trainingService.enroll(courseId));
    }

    @GetMapping("/my-enrollments")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<CourseEnrollment>> myEnrollments() {
        return ResponseEntity.ok(trainingService.myEnrollments());
    }

    @PostMapping("/courses/{courseId}/quiz/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<QuizResultResponse> submitQuiz(
            @PathVariable UUID courseId,
            @Valid @RequestBody SubmitQuizRequest request) {
        return ResponseEntity.ok(trainingService.submitQuiz(courseId, request));
    }

    @GetMapping("/my-certificates")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<List<TrainingCertificate>> myCertificates() {
        return ResponseEntity.ok(trainingService.myCertificates());
    }

    // ============================================================
    // Statistiques globales (pasteur / admin)
    // ============================================================

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(trainingService.stats());
    }

    // ============================================================
    // Administration (pasteur / admin)
    // ============================================================

    @PostMapping("/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<Course> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainingService.createCourse(request));
    }

    @PostMapping("/courses/{courseId}/modules")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<CourseModule> addModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody CreateModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingService.addModule(courseId, request));
    }

    @PostMapping("/modules/{moduleId}/questions")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR')")
    public ResponseEntity<QuizQuestion> addQuestion(
            @PathVariable UUID moduleId,
            @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingService.addQuestion(moduleId, request));
    }

    /** Marque un module comme lu (modules sans quiz) et met à jour la progression. */
    @PostMapping("/courses/{courseId}/modules/{moduleId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')")
    public ResponseEntity<CourseEnrollment> completeModuleRead(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId) {
        return ResponseEntity.ok(trainingService.completeModuleRead(courseId, moduleId));
    }
}
