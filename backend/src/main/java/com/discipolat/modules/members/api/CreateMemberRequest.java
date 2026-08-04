package com.discipolat.modules.members.api;

import com.discipolat.modules.members.domain.MemberRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotNull MemberRequest.Type type,
        @NotNull MemberRequest.Cible cible,
        @NotBlank @Size(max = 2000) String message
) {}
