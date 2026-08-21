package com.discipolat.modules.evaluations.domain;

import com.discipolat.common.domain.BusinessRuleException;
import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.domain.UserRole;
import com.discipolat.common.infrastructure.propagation.EntityPropagationListener;
import com.discipolat.common.infrastructure.propagation.EntityPropagationPublisher;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentRepository;
import com.discipolat.modules.families.domain.Family;
import com.discipolat.modules.families.domain.FamilyRepository;
import com.discipolat.modules.souls.domain.Soul;
import com.discipolat.modules.souls.domain.SoulDepartmentRepository;
import com.discipolat.modules.souls.domain.SoulRepository;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock private EvaluationRepository evaluationRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private FamilyRepository familyRepository;
    @Mock private SoulRepository soulRepository;
    @Mock private SoulDepartmentRepository soulDepartmentRepository;
    @Mock private com.discipolat.modules.audit.domain.AuditService auditService;
    @Mock private com.discipolat.modules.notifications.domain.NotificationService notificationService;
    @Mock private EntityPropagationPublisher propagationPublisher;
    @Mock private EntityPropagationListener propagationListener;

    private EvaluationService evaluationService;

    private final UUID currentUserId = UUID.randomUUID();
    private final UUID evalueId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        evaluationService = new EvaluationService(evaluationRepository, securityUtils,
                userRepository, departmentRepository, familyRepository, soulRepository,
                soulDepartmentRepository, auditService,
                propagationPublisher, propagationListener, notificationService);
    }

    private User user(UUID id, UserRole role) {
        return User.builder().id(id).email(id + "@test.com")
                .firstName("Test").lastName("Test").role(role).build();
    }

    @Test
    void submitOrUpdate_createQuandAbsente() {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(evalueId)).thenReturn(Optional.of(user(evalueId, UserRole.FAISEUR)));
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(evaluationRepository.findByEvaluateurIdAndEvalueIdAndCategorie(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evaluation saved = evaluationService.submitOrUpdate(evalueId, CategorieEvaluation.FAISEUR, 4, "Bon suivi");

        assertNotNull(saved);
        assertEquals(CategorieEvaluation.FAISEUR, saved.getCategorie());
        assertEquals(4, saved.getNote());
        assertEquals("Bon suivi", saved.getCommentaire());
        verify(evaluationRepository, never()).delete(any());
    }

    @Test
    void submitOrUpdate_modifieQuandExistante() {
        UUID existingId = UUID.randomUUID();
        Evaluation existing = Evaluation.builder().id(existingId).evalueId(evalueId)
                .evaluateurId(currentUserId).categorie(CategorieEvaluation.FAISEUR)
                .note(2).commentaire("Ancien avis").build();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(evalueId)).thenReturn(Optional.of(user(evalueId, UserRole.FAISEUR)));
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(evaluationRepository.findByEvaluateurIdAndEvalueIdAndCategorie(eq(currentUserId), eq(evalueId),
                eq(CategorieEvaluation.FAISEUR))).thenReturn(Optional.of(existing));
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evaluation updated = evaluationService.submitOrUpdate(evalueId, CategorieEvaluation.FAISEUR, 5, "Nouvel avis");

        assertEquals(existingId, updated.getId());
        assertEquals(5, updated.getNote());
        assertEquals("Nouvel avis", updated.getCommentaire());
    }

    @Test
    void submitOrUpdate_categorieDeriveeDuRole() {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(evalueId)).thenReturn(Optional.of(user(evalueId, UserRole.MEMBRE)));
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(evaluationRepository.findByEvaluateurIdAndEvalueIdAndCategorie(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evaluation saved = evaluationService.submitOrUpdate(evalueId, null, 3, null);

        assertEquals(CategorieEvaluation.MEMBRE, saved.getCategorie());
    }

    @Test
    void autoEvaluationRefusee() {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        assertThrows(BusinessRuleException.class,
                () -> evaluationService.submitOrUpdate(currentUserId, CategorieEvaluation.FAISEUR, 4, null));
    }

    @Test
    void noteHorsBornesRefusee() {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        assertThrows(BusinessRuleException.class,
                () -> evaluationService.submitOrUpdate(evalueId, CategorieEvaluation.FAISEUR, 0, null));
        assertThrows(BusinessRuleException.class,
                () -> evaluationService.submitOrUpdate(evalueId, CategorieEvaluation.FAISEUR, 6, null));
    }

    @Test
    void utilisateurInconnuRefuse() {
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(evalueId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> evaluationService.submitOrUpdate(evalueId, CategorieEvaluation.FAISEUR, 4, null));
    }

    @Test
    void responsablePeutEvaluerMembreDeSonDepartement() {
        UUID deptId = UUID.randomUUID();
        UUID soulId = UUID.randomUUID();
        Soul soul = Soul.builder().id(soulId).nom("Disciple").userId(evalueId).faiseurId(UUID.randomUUID()).build();
        Department dept = Department.builder().id(deptId).nom("Jeunesse").responsableId(currentUserId).build();

        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(evalueId)).thenReturn(Optional.of(user(evalueId, UserRole.MEMBRE)));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(true);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("FAISEUR")).thenReturn(false);
        // Bottom-up : non lié (pas de soul pour le courant)
        when(soulRepository.findAllByUserId(currentUserId)).thenReturn(List.of());
        // Top-down : l'âme de l'évalué est membre de mon département
        when(soulRepository.findAllByUserId(evalueId)).thenReturn(List.of(soul));
        when(departmentRepository.findByResponsableId(currentUserId)).thenReturn(List.of(dept));
        when(soulDepartmentRepository.existsBySoulIdAndDepartmentIdAndActifTrue(soulId, deptId)).thenReturn(true);
        when(evaluationRepository.findByEvaluateurIdAndEvalueIdAndCategorie(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(evaluationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Evaluation saved = evaluationService.submitOrUpdate(evalueId, CategorieEvaluation.MEMBRE, 4, "Membre engagé");

        assertNotNull(saved);
        assertEquals(4, saved.getNote());
    }

    @Test
    void nonAutoriseRefuse() {
        Soul soul = Soul.builder().id(UUID.randomUUID()).nom("Disciple")
                .userId(evalueId).faiseurId(UUID.randomUUID()).build();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(evalueId)).thenReturn(Optional.of(user(evalueId, UserRole.FAISEUR)));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("FAISEUR")).thenReturn(false);
        when(soulRepository.findAllByUserId(currentUserId)).thenReturn(List.of());
        when(soulRepository.findAllByUserId(evalueId)).thenReturn(List.of(soul));

        assertThrows(BusinessRuleException.class,
                () -> evaluationService.submitOrUpdate(evalueId, CategorieEvaluation.FAISEUR, 4, null));
    }

    @Test
    void getEvaluationsForUser_avecMesPropresEvaluations_nonSuperuser_autorise() {
        // « /evaluations/me » : voir SES propres stats (anonymisées) ne doit pas
        // déclencher la vérification d'auto-évaluation (régression 422).
        User me = user(currentUserId, UserRole.RESPONSABLE);
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(me));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(evaluationRepository.findByEvalueIdAndCategorie(eq(currentUserId), any()))
                .thenReturn(List.of());

        java.util.Map<String, Object> result = evaluationService.getEvaluationsForUser(currentUserId);

        assertNotNull(result);
        assertEquals(currentUserId, result.get("userId"));
        verify(soulRepository, never()).findAllByUserId(currentUserId); // pas de validation de droit
    }

    @Test
    void getEvaluationsForUser_autreUtilisateur_sansDroit_refuse() {
        UUID other = UUID.randomUUID();
        Soul soul = Soul.builder().id(UUID.randomUUID()).nom("Disciple")
                .userId(other).faiseurId(UUID.randomUUID()).build();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(other)).thenReturn(Optional.of(user(other, UserRole.FAISEUR)));
        when(securityUtils.isSuperUser()).thenReturn(false);
        when(securityUtils.hasActiveRole("RESPONSABLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("CHEF_DE_FAMILLE")).thenReturn(false);
        when(securityUtils.hasActiveRole("FAISEUR")).thenReturn(false);
        when(soulRepository.findAllByUserId(currentUserId)).thenReturn(List.of());
        when(soulRepository.findAllByUserId(other)).thenReturn(List.of(soul));

        assertThrows(BusinessRuleException.class,
                () -> evaluationService.getEvaluationsForUser(other));
    }

    @Test
    void getMyEvaluationsFor_retourneMesEvaluations() {
        Evaluation e = Evaluation.builder().id(UUID.randomUUID()).evalueId(evalueId)
                .evaluateurId(currentUserId).categorie(CategorieEvaluation.FAISEUR)
                .note(4).commentaire("Bien").createdAt(java.time.LocalDateTime.now()).build();
        when(securityUtils.getCurrentUserId()).thenReturn(currentUserId);
        when(userRepository.findById(evalueId)).thenReturn(Optional.of(user(evalueId, UserRole.FAISEUR)));
        when(securityUtils.isSuperUser()).thenReturn(true);
        when(evaluationRepository.findByEvaluateurIdAndEvalueId(currentUserId, evalueId))
                .thenReturn(List.of(e));

        List<java.util.Map<String, Object>> mine = evaluationService.getMyEvaluationsFor(evalueId);

        assertEquals(1, mine.size());
        assertEquals("FAISEUR", mine.get(0).get("categorie"));
        assertEquals(4, mine.get(0).get("note"));
    }
}
