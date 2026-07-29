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

import java.util.List;
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
}
