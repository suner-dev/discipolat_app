package com.discipolat.modules.departments.domain;

import com.discipolat.common.domain.EntityNotFoundException;
import com.discipolat.common.enums.StatutEntite;
import com.discipolat.common.infrastructure.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final SecurityUtils securityUtils;

    public DepartmentService(DepartmentRepository departmentRepository, SecurityUtils securityUtils) {
        this.departmentRepository = departmentRepository;
        this.securityUtils = securityUtils;
    }

    public Department create(Department department) {
        department.setStatut(StatutEntite.ACTIVE);
        return departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public Department findById(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department", id));
    }

    @Transactional(readOnly = true)
    public Page<Department> findAll(Pageable pageable) {
        return departmentRepository.findAll(pageable);
    }

    public Department update(Department updated) {
        Department existing = findById(updated.getId());
        existing.setNom(updated.getNom());
        existing.setDescription(updated.getDescription());
        existing.setResponsableId(updated.getResponsableId());
        return departmentRepository.save(existing);
    }

    public void delete(UUID id) {
        Department department = findById(id);
        department.setStatut(StatutEntite.ARCHIVED);
        departmentRepository.save(department);
    }

    @Transactional(readOnly = true)
    public List<Department> findByResponsableId(UUID responsableId) {
        return departmentRepository.findByResponsableId(responsableId);
    }
}
