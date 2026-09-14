package com.discipolat.modules.dresscode.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Request/Response records for Dress Code API.
 */
public class DressCodeRequests {

    public record DressCodeResponse(
            UUID id, UUID spaceId, UUID eventId, String serviceName,
            String title, Instant beginsAt, Instant endsAt,
            String status, boolean archived
    ) {}

    public record DressCodeDetailResponse(
            UUID id, UUID spaceId, UUID eventId, String serviceName,
            String title, Instant beginsAt, Instant endsAt,
            String status, boolean archived,
            Instant createdAt, Instant updatedAt,
            List<DressCodeRuleResponse> rules
    ) {}

    public record DressCodeRuleResponse(
            UUID id, String groupName, String description, String imageUrl
    ) {}

    public record DressCodeRequest(
            String title, String serviceName, UUID spaceId, UUID eventId,
            Instant beginsAt, Instant endsAt, String status,
            List<DressCodeRuleRequest> rules
    ) {}

    public record DressCodeRuleRequest(
            String groupName, String description, String imageUrl
    ) {}
}
