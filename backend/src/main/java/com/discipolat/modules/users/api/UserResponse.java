package com.discipolat.modules.users.api;

import com.discipolat.common.domain.UserRole;
import com.discipolat.modules.users.domain.User;
import com.discipolat.modules.users.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        UserRole role,
        boolean estChefDeFamille,
        UUID familleGereeId,
        UserStatus statut,
        java.time.LocalDate dateNaissance,
        String photoUrl,
        String situationFamiliale,
        boolean twoFactorEnabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.isEstChefDeFamille(),
                user.getFamilleGereeId(),
                user.getStatut(),
                user.getDateNaissance(),
                user.getPhotoUrl(),
                user.getSituationFamiliale(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
