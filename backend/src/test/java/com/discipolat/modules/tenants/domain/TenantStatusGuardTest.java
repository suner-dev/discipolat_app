package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Constat B1 — {@link TenantStatusGuard} : suspension/annulation effective,
 * cache TTL 30 s, invalidation par événement, et comportement fail-closed.
 */
@ExtendWith(MockitoExtension.class)
class TenantStatusGuardTest {

    @Mock
    private TenantRepository tenantRepository;

    private TenantStatusGuard guard;
    private UUID tenantId;
    private Instant now;

    @BeforeEach
    void setUp() {
        guard = new TenantStatusGuard(tenantRepository);
        now = Instant.parse("2026-09-27T10:00:00Z");
        guard.setClock(Clock.fixed(now, ZoneOffset.UTC));
        tenantId = UUID.randomUUID();
    }

    // ---------- États accessibles ----------

    @Test
    void allowsActiveTenant() {
        givenStatus(TenantStatus.ACTIVE);

        assertThatCode(() -> guard.assertAccessible(tenantId)).doesNotThrowAnyException();
        verify(tenantRepository, times(1)).findById(tenantId);
    }

    @Test
    void allowsTenantPendingSetup() {
        givenStatus(TenantStatus.PENDING_SETUP);

        assertThatCode(() -> guard.assertAccessible(tenantId)).doesNotThrowAnyException();
    }

    @Test
    void isTransparentWithoutTenantId() {
        assertThatCode(() -> guard.assertAccessible(null)).doesNotThrowAnyException();

        verify(tenantRepository, never()).findById(any());
    }

    // ---------- États refusés ----------

    @Test
    void refusesSuspendedTenantWith403AndContractCode() {
        givenStatus(TenantStatus.SUSPENDED);

        assertThatThrownBy(() -> guard.assertAccessible(tenantId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    DomainException domain = (DomainException) thrown;
                    assertThat(domain.toProblemDetail().getStatus()).isEqualTo(403);
                    assertThat(domain.toProblemDetail().getTitle()).isEqualTo("TENANT_SUSPENDED");
                    assertThat(domain.getMessage())
                            .isEqualTo("Le service de cette église est suspendu. Contactez le support Discipolat.");
                });
    }

    @Test
    void refusesCancelledTenantWith403AndContractCode() {
        givenStatus(TenantStatus.CANCELLED);

        assertThatThrownBy(() -> guard.assertAccessible(tenantId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    DomainException domain = (DomainException) thrown;
                    assertThat(domain.toProblemDetail().getStatus()).isEqualTo(403);
                    assertThat(domain.toProblemDetail().getTitle()).isEqualTo("TENANT_CANCELLED");
                });
    }

    // ---------- Cache TTL ----------

    @Test
    void servesStatusFromCacheWithinTtl() {
        givenStatus(TenantStatus.ACTIVE);

        guard.assertAccessible(tenantId);
        guard.assertAccessible(tenantId);
        guard.assertAccessible(tenantId);

        // Une seule lecture base pour trois appels : aucune lecture supplémentaire
        // par requête en régime normal (décision D1).
        verify(tenantRepository, times(1)).findById(tenantId);
    }

    @Test
    void rereadsStatusOnceTtlExpired() {
        givenStatus(TenantStatus.ACTIVE);
        guard.assertAccessible(tenantId);

        // Juste avant l'échéance du TTL (30 s) : toujours en cache.
        guard.setClock(Clock.fixed(now.plusMillis(TenantStatusGuard.CACHE_TTL_MILLIS - 1), ZoneOffset.UTC));
        guard.assertAccessible(tenantId);
        verify(tenantRepository, times(1)).findById(tenantId);

        // TTL dépassé : relecture obligatoire.
        guard.setClock(Clock.fixed(now.plusMillis(TenantStatusGuard.CACHE_TTL_MILLIS + 1), ZoneOffset.UTC));
        guard.assertAccessible(tenantId);
        verify(tenantRepository, times(2)).findById(tenantId);
    }

    @Test
    void cacheDoesNotHideSuspensionForThirtySeconds() {
        givenStatus(TenantStatus.ACTIVE);
        guard.assertAccessible(tenantId);

        // La base passe en suspendu : l'invalidation par événement doit suffire.
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(TenantStatus.SUSPENDED)));
        guard.onTenantStatusChanged(new TenantStatusChangedEvent(
                tenantId, TenantStatus.ACTIVE, TenantStatus.SUSPENDED));

        assertThatThrownBy(() -> guard.assertAccessible(tenantId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle()).isEqualTo("TENANT_SUSPENDED"));
    }

    @Test
    void eventInvalidationEmptiesCache() {
        givenStatus(TenantStatus.SUSPENDED);
        assertThatThrownBy(() -> guard.assertAccessible(tenantId))
                .isInstanceOf(DomainException.class);
        // Le statut est bien mémorisé (interrogation répétée sans relire la base).
        verify(tenantRepository, times(1)).findById(tenantId);
        assertThat(guard.cachedTenantCount()).isEqualTo(1);

        guard.onTenantStatusChanged(new TenantStatusChangedEvent(
                tenantId, TenantStatus.SUSPENDED, TenantStatus.ACTIVE));

        assertThat(guard.cachedTenantCount()).isZero();
    }

    @Test
    void reactivationEventRestoresAccessImmediately() {
        givenStatus(TenantStatus.SUSPENDED);
        assertThatThrownBy(() -> guard.assertAccessible(tenantId)).isInstanceOf(DomainException.class);

        org.mockito.Mockito.doReturn(Optional.of(tenant(TenantStatus.ACTIVE)))
                .when(tenantRepository).findById(tenantId);
        guard.onTenantStatusChanged(new TenantStatusChangedEvent(
                tenantId, TenantStatus.SUSPENDED, TenantStatus.ACTIVE));

        assertThatCode(() -> guard.assertAccessible(tenantId)).doesNotThrowAnyException();
    }

    // ---------- Fail-closed ----------

    @Test
    void failsClosedWhenStatusCannotBeRead() {
        when(tenantRepository.findById(tenantId)).thenThrow(new RuntimeException("base injoignable"));

        assertThatThrownBy(() -> guard.assertAccessible(tenantId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    DomainException domain = (DomainException) thrown;
                    assertThat(domain.toProblemDetail().getStatus()).isEqualTo(403);
                    assertThat(domain.toProblemDetail().getTitle()).isEqualTo("TENANT_STATUS_UNAVAILABLE");
                });
    }

    @Test
    void failsClosedWhenTenantNotFound() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.assertAccessible(tenantId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("TENANT_STATUS_UNAVAILABLE"));
    }

    @Test
    void failsClosedWhenStatusIsNull() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(null)));

        assertThatThrownBy(() -> guard.assertAccessible(tenantId))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> assertThat(((DomainException) thrown).toProblemDetail().getTitle())
                        .isEqualTo("TENANT_STATUS_UNAVAILABLE"));
    }

    @Test
    void readFailureIsNeverCached() {
        when(tenantRepository.findById(tenantId)).thenThrow(new RuntimeException("panne 1"));
        assertThatThrownBy(() -> guard.assertAccessible(tenantId)).isInstanceOf(DomainException.class);
        assertThat(guard.cachedTenantCount()).isZero();

        // La panne est transitoire : la tentative suivante doit relire la base.
        // `doReturn` et non `when` : reecrire le stub sur une methode deja configuree
        // pour lever une exception ferait lever l'exception au moment du `when(...)`.
        org.mockito.Mockito.doReturn(Optional.of(tenant(TenantStatus.ACTIVE)))
                .when(tenantRepository).findById(tenantId);
        assertThatCode(() -> guard.assertAccessible(tenantId)).doesNotThrowAnyException();
    }

    private void givenStatus(TenantStatus status) {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant(status)));
    }

    private Tenant tenant(TenantStatus status) {
        Tenant tenant = Tenant.builder().id(tenantId).name("Église").slug("eglise").build();
        tenant.setStatus(status);
        return tenant;
    }
}
