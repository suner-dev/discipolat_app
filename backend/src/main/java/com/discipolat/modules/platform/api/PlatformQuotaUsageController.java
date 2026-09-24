package com.discipolat.modules.platform.api;

import com.discipolat.common.infrastructure.api.PageResponse;
import com.discipolat.modules.tenants.domain.TenantUsageSnapshot;
import com.discipolat.modules.tenants.domain.TenantUsageSnapshotService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/admin/quota-usage")
@PreAuthorize("@authz.isPlatformSuperAdmin()")
@Validated
public class PlatformQuotaUsageController {

    private final TenantUsageSnapshotService usageSnapshotService;

    public PlatformQuotaUsageController(TenantUsageSnapshotService usageSnapshotService) {
        this.usageSnapshotService = usageSnapshotService;
    }

    @GetMapping("/tenants/{tenantId}")
    public ResponseEntity<TenantUsageSnapshot> getTenantSnapshot(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(usageSnapshotService.getSnapshotForTenant(tenantId));
    }

    @GetMapping("/tenants")
    public ResponseEntity<PageResponse<TenantUsageSnapshotService.TenantUsageOverview>> listTenantUsage(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(PageResponse.from(usageSnapshotService.getTenantOverviews(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))));
    }
}
