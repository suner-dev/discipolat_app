package com.discipolat.modules.security.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenSessionServiceTest {

    @Mock
    private RefreshTokenSessionRepository repository;

    @Test
    void registerStoresOnlyTheTokenHash() {
        String token = "raw-refresh-token";
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(repository.save(org.mockito.ArgumentMatchers.any(RefreshTokenSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        new RefreshTokenSessionService(repository).register(token, userId, familyId, expiresAt);

        ArgumentCaptor<RefreshTokenSession> captor = ArgumentCaptor.forClass(RefreshTokenSession.class);
        verify(repository).save(captor.capture());
        RefreshTokenSession session = captor.getValue();
        assertThat(session.getTokenHash()).hasSize(64).isNotEqualTo(token);
        assertThat(session.getUserId()).isEqualTo(userId);
        assertThat(session.getFamilyId()).isEqualTo(familyId);
    }

    @Test
    void consumeMarksTheCurrentSessionAsUsed() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshTokenSession session = session(userId, familyId, null, null, Instant.now().plusSeconds(3600));
        when(repository.findByTokenHashForUpdate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(session));

        RefreshTokenSessionService.ConsumptionResult result = new RefreshTokenSessionService(repository)
                .consume("raw-refresh-token", userId, familyId);

        assertThat(result).isEqualTo(RefreshTokenSessionService.ConsumptionResult.ROTATED);
        assertThat(session.getUsedAt()).isNotNull();
    }

    @Test
    void reuseRevokesTheWholeTokenFamily() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshTokenSession used = session(userId, familyId, Instant.now().minusSeconds(60), null, Instant.now().plusSeconds(3600));
        RefreshTokenSession replacement = session(userId, familyId, null, null, Instant.now().plusSeconds(3600));
        when(repository.findByTokenHashForUpdate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(used));
        when(repository.findByFamilyId(familyId)).thenReturn(List.of(used, replacement));

        RefreshTokenSessionService.ConsumptionResult result = new RefreshTokenSessionService(repository)
                .consume("raw-refresh-token", userId, familyId);

        assertThat(result).isEqualTo(RefreshTokenSessionService.ConsumptionResult.REUSE);
        assertThat(used.getRevokedAt()).isNotNull();
        assertThat(replacement.getRevokedAt()).isNotNull();
    }

    @Test
    void expiredSessionIsRevokedAndRejected() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshTokenSession session = session(userId, familyId, null, null, Instant.now().minusSeconds(1));
        when(repository.findByTokenHashForUpdate(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Optional.of(session));

        RefreshTokenSessionService.ConsumptionResult result = new RefreshTokenSessionService(repository)
                .consume("raw-refresh-token", userId, familyId);

        assertThat(result).isEqualTo(RefreshTokenSessionService.ConsumptionResult.EXPIRED);
        assertThat(session.getRevokedAt()).isNotNull();
    }

    private RefreshTokenSession session(UUID userId, UUID familyId, Instant usedAt, Instant revokedAt, Instant expiresAt) {
        return RefreshTokenSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .familyId(familyId)
                .tokenHash("hash")
                .usedAt(usedAt)
                .revokedAt(revokedAt)
                .expiresAt(expiresAt)
                .build();
    }
}
