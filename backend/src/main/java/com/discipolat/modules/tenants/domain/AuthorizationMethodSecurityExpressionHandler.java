package com.discipolat.modules.tenants.domain;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private AuthorizationService authzService;

    @Autowired
    public void setAuthzService(AuthorizationService authzService) {
        this.authzService = authzService;
    }

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(
            Authentication authentication,
            java.lang.reflect.Method method) {
        return new AuthorizationExpressionRoot(authentication, authzService);
    }
}