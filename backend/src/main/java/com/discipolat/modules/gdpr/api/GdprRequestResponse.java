package com.discipolat.modules.gdpr.api;

import com.discipolat.modules.gdpr.domain.GdprRequest;
import com.discipolat.modules.gdpr.domain.GdprRequestStatus;
import com.discipolat.modules.gdpr.domain.GdprRequestType;

import java.time.LocalDateTime;
import java.util.UUID;

public record GdprRequestResponse(
        UUID id,
        UUID tenantId,
        UUID requesterUserId,
        GdprRequestType requestType,
        GdprRequestStatus status,
        LocalDateTime requestedAt,
        LocalDateTime processedAt,
        UUID processedBy,
        String notes,
        boolean hasExportData
) {
    public static GdprRequestResponse from(GdprRequest r) {
        return new GdprRequestResponse(
                r.getId(),
                r.getTenantId(),
                r.getRequesterUserId(),
                r.getRequestType(),
                r.getStatus(),
                r.getRequestedAt(),
                r.getProcessedAt(),
                r.getProcessedBy(),
                r.getNotes(),
                r.getExportData() != null);
    }
}
