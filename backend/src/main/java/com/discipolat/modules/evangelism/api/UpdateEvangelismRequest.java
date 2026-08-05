package com.discipolat.modules.evangelism.api;

import com.discipolat.modules.evangelism.domain.EvangelismEtape;
import jakarta.validation.constraints.NotNull;

public record UpdateEvangelismRequest(
        @NotNull EvangelismEtape etape,
        String note
) {
}
