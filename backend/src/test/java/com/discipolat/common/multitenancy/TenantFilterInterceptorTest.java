package com.discipolat.common.multitenancy;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantFilterInterceptorTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void bypassesInvitationValidationWhenAnotherTenantTokenIsIncidentallyPresent() {
        TenantFilter tenantFilter = mock(TenantFilter.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<TenantFilter> provider = mock(ObjectProvider.class);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/admin/invitations/validate/secret-token");
        when(provider.getIfAvailable()).thenReturn(tenantFilter);
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(true);
        TenantContext.setTenantId(java.util.UUID.randomUUID());

        boolean allowed = new TenantFilterInterceptor(provider)
                .preHandle(request, mock(HttpServletResponse.class), new Object());

        org.assertj.core.api.Assertions.assertThat(allowed).isTrue();
        verify(tenantFilter, never()).enableFilter(request);
    }

    @Test
    void keepsTenantFilteringForPrivateInvitationManagement() {
        TenantFilter tenantFilter = mock(TenantFilter.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<TenantFilter> provider = mock(ObjectProvider.class);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/admin/invitations");
        when(provider.getIfAvailable()).thenReturn(tenantFilter);
        when(tenantFilter.shouldBypassFilter(request)).thenReturn(false);
        TenantContext.setTenantId(java.util.UUID.randomUUID());

        new TenantFilterInterceptor(provider)
                .preHandle(request, mock(HttpServletResponse.class), new Object());

        verify(tenantFilter).enableFilter(request);
    }
}
