package com.discipolat.modules.onboarding.domain;

import com.discipolat.common.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OnboardingWizardService {

    private final OnboardingWizardRepository wizardRepo;

    private static final List<OnboardingWizardStep.StepType> DEFAULT_STEPS = List.of(
        OnboardingWizardStep.StepType.CHURCH_IDENTITY,
        OnboardingWizardStep.StepType.MEMBER_IMPORT,
        OnboardingWizardStep.StepType.STRUCTURE,
        OnboardingWizardStep.StepType.ROLES,
        OnboardingWizardStep.StepType.BRANDING,
        OnboardingWizardStep.StepType.MODULES,
        OnboardingWizardStep.StepType.FIRST_EVENT
    );

    public OnboardingWizardService(OnboardingWizardRepository wizardRepo) { this.wizardRepo = wizardRepo; }

    public List<OnboardingWizardStep> getSteps() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        var steps = wizardRepo.findByTenantIdOrderByStepOrderAsc(tenantId);
        if (steps.isEmpty()) {
            return initializeSteps();
        }
        return steps;
    }

    public List<OnboardingWizardStep> initializeSteps() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<OnboardingWizardStep> steps = new ArrayList<>();
        int order = 0;
        for (OnboardingWizardStep.StepType stepType : DEFAULT_STEPS) {
            OnboardingWizardStep step = new OnboardingWizardStep();
            step.setTenantId(tenantId);
            step.setStepType(stepType);
            step.setStepOrder(order++);
            step.setStatus(OnboardingWizardStep.Status.PENDING);
            steps.add(wizardRepo.save(step));
        }
        return steps;
    }

    public OnboardingWizardStep startStep(UUID stepId) {
        OnboardingWizardStep step = wizardRepo.findById(stepId).orElseThrow();
        step.setStatus(OnboardingWizardStep.Status.IN_PROGRESS);
        step.setStartedAt(LocalDateTime.now());
        return wizardRepo.save(step);
    }

    public OnboardingWizardStep completeStep(UUID stepId, String data) {
        OnboardingWizardStep step = wizardRepo.findById(stepId).orElseThrow();
        step.setStatus(OnboardingWizardStep.Status.COMPLETED);
        step.setCompletedData(data);
        step.setCompletedAt(LocalDateTime.now());
        return wizardRepo.save(step);
    }

    public OnboardingWizardStep skipStep(UUID stepId) {
        OnboardingWizardStep step = wizardRepo.findById(stepId).orElseThrow();
        step.setStatus(OnboardingWizardStep.Status.SKIPPED);
        return wizardRepo.save(step);
    }

    public Map<String, Object> getProgress() {
        List<OnboardingWizardStep> steps = getSteps();
        long completed = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.COMPLETED).count();
        long skipped = steps.stream().filter(s -> s.getStatus() == OnboardingWizardStep.Status.SKIPPED).count();
        Map<String, Object> progress = new HashMap<>();
        progress.put("totalSteps", steps.size());
        progress.put("completedSteps", completed);
        progress.put("skippedSteps", skipped);
        progress.put("percentage", steps.isEmpty() ? 0 : Math.round((completed + skipped) * 100.0 / steps.size()));
        progress.put("isComplete", completed + skipped >= steps.size());
        progress.put("steps", steps);
        return progress;
    }
}
