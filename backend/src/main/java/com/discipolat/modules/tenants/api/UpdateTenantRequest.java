package com.discipolat.modules.tenants.api;

import com.discipolat.modules.tenants.domain.TenantStatus;
import jakarta.validation.constraints.Size;

public record UpdateTenantRequest(
        @Size(max = 255) String name,
        TenantStatus status,
        String plan
) {}
