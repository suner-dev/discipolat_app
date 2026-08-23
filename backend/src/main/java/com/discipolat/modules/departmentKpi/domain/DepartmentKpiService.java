package com.discipolat.modules.departmentKpi.domain;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentKpiService {

    private final DepartmentKpiRepository repository;

    public DepartmentKpiService(DepartmentKpiRepository repository) {
        this.repository = repository;
    }

    public List<DepartmentKpi> listByDepartment(Long departmentId) {
        return repository.findByDepartmentIdOrderByCreatedAtDesc(departmentId);
    }

    public List<DepartmentKpi> listByTenant(Long tenantId) {
        return repository.findByTenantId(tenantId);
    }

    public DepartmentKpi create(DepartmentKpi kpi) {
        return repository.save(kpi);
    }

    public DepartmentKpi update(Long id, DepartmentKpi updated) {
        DepartmentKpi kpi = repository.findById(id).orElseThrow();
        kpi.setCurrentValue(updated.getCurrentValue());
        kpi.setTargetValue(updated.getTargetValue());
        return repository.save(kpi);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
