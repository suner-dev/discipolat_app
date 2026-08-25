package com.discipolat.modules.devPlan.api;

import com.discipolat.modules.devPlan.domain.DevelopmentPlan;
import com.discipolat.modules.devPlan.domain.DevelopmentPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/development-plans")
@PreAuthorize("isAuthenticated()")
public class DevelopmentPlanController {

    private final DevelopmentPlanService service;
    public DevelopmentPlanController(DevelopmentPlanService service) { this.service = service; }

    @GetMapping("/by-member/{membreId}")
    public List<DevelopmentPlan> listByMember(@PathVariable UUID membreId) {
        return service.listByMember(membreId);
    }

    @GetMapping("/by-department/{deptId}")
    public List<DevelopmentPlan> listByDepartment(@PathVariable UUID deptId) {
        return service.listByDepartment(deptId);
    }

    @GetMapping("/{id}")
    public DevelopmentPlan get(@PathVariable UUID id) { return service.get(id); }

    @PostMapping
    public ResponseEntity<DevelopmentPlan> create(@RequestBody DevelopmentPlan plan) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(plan));
    }

    @PutMapping("/{id}")
    public DevelopmentPlan update(@PathVariable UUID id, @RequestBody DevelopmentPlan updates) {
        return service.update(id, updates);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auto-generate")
    public List<DevelopmentPlan> autoGenerate(@RequestParam UUID membreId, @RequestParam UUID deptId) {
        return service.autoGenerate(membreId, deptId);
    }

    @GetMapping("/stats/{membreId}")
    public Map<String, Object> stats(@PathVariable UUID membreId) {
        return service.getStats(membreId);
    }
}
