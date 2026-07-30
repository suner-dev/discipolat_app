package com.discipolat.modules.departments.api;

import com.discipolat.modules.departments.domain.Department;
import com.discipolat.modules.departments.domain.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public ResponseEntity<com.discipolat.common.infrastructure.api.PageResponse<DepartmentResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50),
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending());
        Page<Department> departments = departmentService.findAll(pageable);
        Page<DepartmentResponse> response = departments.map(DepartmentResponse::from);
        return ResponseEntity.ok(com.discipolat.common.infrastructure.api.PageResponse.of(
                response.getContent(), response.getNumber(), response.getSize(),
                response.getTotalElements(), response.getTotalPages()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> findById(@PathVariable UUID id) {
        Department department = departmentService.findById(id);
        return ResponseEntity.ok(DepartmentResponse.from(department));
    }

    @PostMapping
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody CreateDepartmentRequest request) {
        Department department = Department.builder()
                .nom(request.nom())
                .description(request.description())
                .responsableId(request.responsableId())
                .build();
        department = departmentService.create(department);
        return ResponseEntity.status(HttpStatus.CREATED).body(DepartmentResponse.from(department));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<DepartmentResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody CreateDepartmentRequest request) {
        Department department = departmentService.findById(id);
        department.setNom(request.nom());
        department.setDescription(request.description());
        department.setResponsableId(request.responsableId());
        department = departmentService.update(department);
        return ResponseEntity.ok(DepartmentResponse.from(department));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PASTEUR')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-responsable/{responsableId}")
    public ResponseEntity<List<DepartmentResponse>> findByResponsable(@PathVariable UUID responsableId) {
        List<Department> departments = departmentService.findByResponsableId(responsableId);
        return ResponseEntity.ok(departments.stream().map(DepartmentResponse::from).toList());
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getDetail(id));
    }

    @GetMapping("/{id}/kpi")
    public ResponseEntity<Map<String, Object>> kpi(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getDepartmentKpi(id));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<com.discipolat.common.infrastructure.api.PageResponse<Map<String, Object>>> members(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, Math.min(size, 100));
        var result = departmentService.getDepartmentMembers(id, pageable);
        return ResponseEntity.ok(com.discipolat.common.infrastructure.api.PageResponse.of(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()));
    }

    @GetMapping("/{id}/unassigned")
    public ResponseEntity<List<Map<String, Object>>> unassigned(@PathVariable UUID id) {
        return ResponseEntity.ok(departmentService.getUnassignedMembers(id));
    }

    @GetMapping("/{id}/report")
    public ResponseEntity<Map<String, Object>> report(
            @PathVariable UUID id,
            @RequestParam(required = false) String semaine) {
        LocalDate week = semaine != null ? LocalDate.parse(semaine) : LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        return ResponseEntity.ok(departmentService.getDepartmentReport(id, week));
    }
}
