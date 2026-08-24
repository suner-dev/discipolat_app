package com.discipolat.modules.departmentKpi.api;

import com.discipolat.common.multitenancy.TenantContext;
import com.discipolat.modules.departmentKpi.domain.DepartmentKpi;
import com.discipolat.modules.departmentKpi.domain.DepartmentKpiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/department-kpis")
public class DepartmentKpiController {

    private final DepartmentKpiService service;

    public DepartmentKpiController(DepartmentKpiService service) {
        this.service = service;
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<DepartmentKpi>> listByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(service.listByDepartment(departmentId));
    }

    @GetMapping
    public ResponseEntity<List<DepartmentKpi>> listByTenant(@RequestParam Long tenantId) {
        return ResponseEntity.ok(service.listByTenant(tenantId));
    }

    @PostMapping
    public ResponseEntity<DepartmentKpi> create(@RequestBody DepartmentKpi kpi) {
        return ResponseEntity.ok(service.create(kpi));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentKpi> update(@PathVariable Long id, @RequestBody DepartmentKpi kpi) {
        return ResponseEntity.ok(service.update(id, kpi));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * KPIs calculés automatiquement : taux remplissage, réalisation, satisfaction, alertes.
     */
    @GetMapping("/department/{departmentId}/computed")
    public ResponseEntity<Map<String, Object>> getComputedKpis(@PathVariable Long departmentId) {
        return ResponseEntity.ok(service.getComputedKpis(departmentId, departmentId));
    }
}
