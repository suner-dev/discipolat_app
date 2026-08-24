package com.discipolat.modules.onboarding.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OnboardingWizardRepository extends JpaRepository<OnboardingWizardStep, UUID> {
    List<OnboardingWizardStep> findByTenantIdOrderByStepOrderAsc(UUID tenantId);
}
