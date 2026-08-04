package com.discipolat.modules.members.api;

import com.discipolat.modules.members.domain.MemberRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequestStatus(
        @NotNull MemberRequest.Statut statut,
        @Size(max = 2000) String reponse
) {}
