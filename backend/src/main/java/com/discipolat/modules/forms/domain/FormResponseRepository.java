package com.discipolat.modules.forms.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FormResponseRepository extends JpaRepository<FormResponse, UUID> {
    List<FormResponse> findByFormTemplateIdOrderBySubmittedAtDesc(UUID formTemplateId);
    long countByFormTemplateId(UUID formTemplateId);
}
