package com.discipolat.modules.messages.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateGroupRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @Size(max = 50) String groupType,
        List<UUID> memberIds
) {}
