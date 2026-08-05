package com.discipolat.modules.trainings.domain;

import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.trainings.api.CreateCourseRequest;
import com.discipolat.modules.trainings.api.CreateModuleRequest;
import com.discipolat.modules.trainings.api.QuizResultResponse;
import com.discipolat.modules.trainings.api.SubmitQuizRequest;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository moduleRepository;
    @Mock private QuizQuestionRepository quizRepository;
    @Mock private CourseEnrollmentRepository enrollmentRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private ModuleCompletionRepository completionRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private TrainingService trainingService;

    private QuizQuestion question(int index) {
        return QuizQuestion.builder()
                .id(UUID.randomUUID())
                .moduleId(UUID.randomUUID())
                .question("Q?")
                .propositions("[\"a\",\"b\",\"c\"]")
                .reponseIndex(index)
                .ordre(1)
                .build();
    }

    private CourseEnrollment enrollment(UUID courseId, UUID userId) {
        CourseEnrollment e = CourseEnrollment.builder()
                .id(UUID.randomUUID())
                .courseId(courseId)
                .userId(userId)
                .statut(CourseEnrollment.Statut.INSCRIT)
                .progression(0)
                .build();
        return e;
    }

    @Test
    void createCourse_shouldPersist() {
        CreateCourseRequest req = new CreateCourseRequest(
                "Test", "desc", "DISCIPOLAT", Course.Niveau.DEBUTANT, 30, null, null);
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        Course created = trainingService.createCourse(req);

        assertEquals("Test", created.getTitre());
        assertEquals(Course.Niveau.DEBUTANT, created.getNiveau());
        assertTrue(created.isActif());
    }

    @Test
    void addModule_shouldAttachToCourse() {
        UUID courseId = UUID.randomUUID();
        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(moduleRepository.save(any(CourseModule.class))).thenAnswer(inv -> inv.getArgument(0));

        CourseModule module = trainingService.addModule(courseId,
                new CreateModuleRequest("Module 1", "contenu", null, 1));

        assertEquals(courseId, module.getCourseId());
        assertEquals("Module 1", module.getTitre());
        assertEquals(1, module.getOrdre());
    }

    @Test
    void submitQuiz_perfectScore_shouldCompleteAndIssueCertificate() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        QuizQuestion q = question(1);
        lenient().when(quizRepository.findByModuleIdOrderByOrdreAsc(moduleId)).thenReturn(List.of(q));
        lenient().when(courseRepository.existsById(courseId)).thenReturn(true);
        lenient().when(moduleRepository.existsById(moduleId)).thenReturn(true);

        CourseEnrollment e = enrollment(courseId, userId);
        lenient().when(enrollmentRepository.findByCourseIdAndUserId(courseId, userId))
                .thenReturn(Optional.of(e));
        lenient().when(enrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(moduleRepository.findById(moduleId)).thenReturn(Optional.of(CourseModule.builder()
                .id(moduleId).courseId(courseId).titre("M").ordre(1).build()));
        when(moduleRepository.countByCourseId(courseId)).thenReturn(1L);
        when(completionRepository.findByEnrollmentIdAndModuleId(any(), any()))
                .thenReturn(Optional.empty());
        when(completionRepository.save(any(ModuleCompletion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(completionRepository.countByEnrollmentId(any())).thenReturn(1L);
        when(certificateRepository.findByEnrollmentId(any())).thenReturn(Optional.empty());
        when(certificateRepository.save(any(Certificate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        QuizResultResponse result = trainingService.submitQuiz(courseId,
                new SubmitQuizRequest(moduleId, java.util.Map.of(q.getId(), 1)));

        assertEquals(100, result.score());
        assertTrue(result.reussi());
        assertTrue(result.certificat());
    }

    @Test
    void submitQuiz_failingScore_shouldNotIssueCertificate() {
        UUID userId = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        UUID moduleId = UUID.randomUUID();
        when(securityUtils.getCurrentUserId()).thenReturn(userId);

        QuizQuestion q = question(1);
        lenient().when(quizRepository.findByModuleIdOrderByOrdreAsc(moduleId)).thenReturn(List.of(q));
        lenient().when(courseRepository.existsById(courseId)).thenReturn(true);

        CourseEnrollment e = enrollment(courseId, userId);
        lenient().when(enrollmentRepository.findByCourseIdAndUserId(courseId, userId))
                .thenReturn(Optional.of(e));
        lenient().when(enrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        QuizResultResponse result = trainingService.submitQuiz(courseId,
                new SubmitQuizRequest(moduleId, java.util.Map.of(q.getId(), 0)));

        assertEquals(0, result.score());
        assertFalse(result.reussi());
        assertFalse(result.certificat());
    }
}
