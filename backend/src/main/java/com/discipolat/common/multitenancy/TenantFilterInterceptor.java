package com.discipolat.common.multitenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Enables the Hibernate tenant filter at the start of each request
 * and disables it at the end.
 *
 * Ordered to run AFTER TenantInterceptor (which sets the tenant ID)
 * and BEFORE the controller.
 *
 * The filter is optional: in sliced test contexts (e.g. @WebMvcTest) the
 * TenantFilter bean is not present, so the interceptor degrades gracefully
 * instead of failing afterCompletion on every request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantFilterInterceptor implements HandlerInterceptor {

    private final ObjectProvider<TenantFilter> tenantFilterProvider;

    public TenantFilterInterceptor(ObjectProvider<TenantFilter> tenantFilterProvider) {
        this.tenantFilterProvider = tenantFilterProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (TenantContext.getTenantId() != null) {
            TenantFilter tenantFilter = tenantFilterProvider.getIfAvailable();
            if (tenantFilter != null) {
                tenantFilter.enableFilter();
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantFilter tenantFilter = tenantFilterProvider.getIfAvailable();
        if (tenantFilter != null) {
            tenantFilter.disableFilter();
        }
    }
}
