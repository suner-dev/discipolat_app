package com.discipolat.modules.security.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenRevocationServiceTest {

    @Test
    void revokesAndChecksTheHashWithoutRetainingTheRawToken() {
        RevokedTokenRepository repository = mock(RevokedTokenRepository.class);
        when(repository.existsByTokenHashAndExpiresAtAfter(any(), any())).thenReturn(false);
        when(repository.findByTokenHash(any())).thenReturn(java.util.Optional.empty());
        TokenRevocationService service = new TokenRevocationService(repository);
        String token = "raw-token-value";
        Instant expiry = Instant.now().plusSeconds(300);

        service.revoke(token, "refresh", expiry, "rotated");

        org.mockito.ArgumentCaptor<RevokedToken> captor = org.mockito.ArgumentCaptor.forClass(RevokedToken.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(token);
        assertThat(service.isRevoked(token)).isFalse();
    }
}
