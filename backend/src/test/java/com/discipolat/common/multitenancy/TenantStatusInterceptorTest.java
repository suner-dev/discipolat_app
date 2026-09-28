package com.discipolat.common.multitenancy;

import com.discipolat.common.exception.DomainException;
import com.discipolat.modules.tenants.domain.TenantStatusGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Constat B1 — {@link TenantStatusInterceptor} : la suspension s'applique à toute
 * requête authentifiée, mais les chemins publics et les appels sans contexte
 * tenant restent joignables.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantStatusInterceptorTest {

    @Mock
    private TenantStatusGuard guard;
    @Mock
    private TenantFilter tenantFilter;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ---------- Cas refusés ----------

    @Test
    void suspendedTenantIsRejectedOnAnyAuthenticatedApiCall() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = apiRequest("/api/v1/dashboard");
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(false);
        doThrowSuspended();

        assertThatThrownBy(() -> interceptor().preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOf(DomainException.class)
                .satisfies(thrown -> {
                    DomainException domain = (DomainException) thrown;
                    assertThat(domain.toProblemDetail().getStatus()).isEqualTo(403);
                    assertThat(domain.toProblemDetail().getTitle()).isEqualTo("TENANT_SUSPENDED");
                });
    }

    @Test
    void guardIsInvokedWithTheCurrentTenantId() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = apiRequest("/api/v1/people");
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(false);

        assertThat(interceptor().preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        verify(guard).assertAccessible(tenantId);
    }

    // ---------- Cas ignorés ----------

    @Test
    void publicPathIsIgnored() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = apiRequest("/api/v1/auth/login");
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(true);

        assertThat(interceptor().preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        verifyNoInteractions(guard);
    }

    @Test
    void invitationAcceptancePathStaysReachableForSuspendedTenant() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = apiRequest("/api/v1/admin/invitations/accept/abc123");
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(true);

        assertThat(interceptor().preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        verifyNoInteractions(guard);
    }

    @Test
    void actuatorHealthStaysReachable() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = apiRequest("/actuator/health");
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(true);

        assertThat(interceptor().preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        verifyNoInteractions(guard);
    }

    @Test
    void requestWithoutTenantContextIsIgnored() {
        TenantContext.clear();
        HttpServletRequest request = apiRequest("/api/v1/platform/admin/tenants");

        assertThat(interceptor().preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        verifyNoInteractions(guard);
        verify(guard, never()).assertAccessible(any());
    }

    @Test
    void degradesGracefullyWhenGuardBeanIsAbsent() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = apiRequest("/api/v1/dashboard");
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(false);

        ObjectProvider<TenantStatusGuard> emptyProvider = emptyProvider();

        assertThatCode(() -> new TenantStatusInterceptor(emptyProvider, provider(tenantFilter))
                .preHandle(request, new MockHttpServletResponse(), new Object()))
                .doesNotThrowAnyException();
    }

    @Test
    void degradesGracefullyWhenTenantFilterBeanIsAbsent() {
        TenantContext.setTenantId(tenantId);
        HttpServletRequest request = apiRequest("/api/v1/dashboard");

        // Sans TenantFilter, aucun chemin n'est considéré public : la garde s'applique.
        assertThat(interceptor().preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();

        verify(guard).assertAccessible(tenantId);
    }

    // ---------- helpers ----------

    private void doThrowSuspended() {
        org.mockito.Mockito.doThrow(new DomainException(
                        "Le service de cette église est suspendu. Contactez le support Discipolat.",
                        HttpStatus.FORBIDDEN, "TENANT_SUSPENDED"))
                .when(guard).assertAccessible(tenantId);
    }

    private TenantStatusInterceptor interceptor() {
        return new TenantStatusInterceptor(provider(guard), provider(tenantFilter));
    }

    private HttpServletRequest apiRequest(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        return request;
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = org.mockito.Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> emptyProvider() {
        ObjectProvider<T> provider = org.mockito.Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
