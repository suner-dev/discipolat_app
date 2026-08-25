package com.discipolat.modules.forms.api;

import com.discipolat.modules.forms.domain.FormResponse;
import com.discipolat.modules.forms.domain.FormService;
import com.discipolat.modules.forms.domain.FormTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/forms")
@PreAuthorize("hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')")
public class FormController {

    private final FormService formService;

    public FormController(FormService formService) {
        this.formService = formService;
    }

    @GetMapping
    public List<FormTemplate> listTemplates() {
        return formService.listTemplates();
    }

    @GetMapping("/published")
    public List<FormTemplate> listPublished() {
        return formService.listPublished();
    }

    @GetMapping("/{id}")
    public FormTemplate getTemplate(@PathVariable UUID id) {
        return formService.getTemplate(id);
    }

    @PostMapping
    public ResponseEntity<FormTemplate> createTemplate(@RequestBody FormTemplate t) {
        return ResponseEntity.status(HttpStatus.CREATED).body(formService.createTemplate(t));
    }

    @PutMapping("/{id}")
    public FormTemplate updateTemplate(@PathVariable UUID id, @RequestBody FormTemplate updates) {
        return formService.updateTemplate(id, updates);
    }

    @PostMapping("/{id}/publish")
    public FormTemplate publishTemplate(@PathVariable UUID id) {
        return formService.publishTemplate(id);
    }

    @PostMapping("/{id}/archive")
    public FormTemplate archiveTemplate(@PathVariable UUID id) {
        return formService.archiveTemplate(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable UUID id) {
        formService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{templateId}/responses")
    public ResponseEntity<FormResponse> submitResponse(
            @PathVariable UUID templateId, @RequestBody FormResponse response) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(formService.submitResponse(templateId, response));
    }

    @GetMapping("/{templateId}/responses")
    public List<FormResponse> getResponses(@PathVariable UUID templateId) {
        return formService.getResponses(templateId);
    }

    @GetMapping("/{templateId}/stats")
    public Map<String, Object> getStats(@PathVariable UUID templateId) {
        return formService.getStats(templateId);
    }
}
