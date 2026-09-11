package com.discipolat.modules.tenants.domain;

import com.discipolat.common.exception.ForbiddenException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.UUID;

/**
 * Aspect that enforces @Authz annotations using AuthorizationService.
 * Supports SpEL expressions in scopeId parameter.
 */
@Aspect
@Component
public class AuthzAspect {

    private final AuthorizationService authzService;
    private final ExpressionParser parser = new SpelExpressionParser();

    public AuthzAspect(AuthorizationService authzService) {
        this.authzService = authzService;
    }

    @Around("@annotation(authz)")
    public Object enforceAuthz(ProceedingJoinPoint joinPoint, Authz authz) throws Throwable {
        UUID userId = getCurrentUserId();
        UUID tenantId = getCurrentTenantId();

        if (userId == null || tenantId == null) {
            if (authz.enforce()) {
                throw new ForbiddenException("Authentication required");
            }
            return proceedOrNull(joinPoint);
        }

        // Resolve scope ID from SpEL expression or parameters
        UUID scopeId = resolveScopeId(joinPoint, authz);

        try {
            MembershipScopeType scopeType = MembershipScopeType.valueOf(authz.scopeType().toUpperCase());
            boolean authorized = authzService.can(userId, tenantId, authz.permission(), scopeType, scopeId);

            if (!authorized) {
                if (authz.enforce()) {
                    throw new ForbiddenException("Permission denied: " + authz.permission() + 
                            " on " + authz.scopeType() + (scopeId != null ? ":" + scopeId : ""));
                }
                return proceedOrNull(joinPoint);
            }
        } catch (IllegalArgumentException e) {
            if (authz.enforce()) {
                throw new ForbiddenException("Invalid scope type: " + authz.scopeType());
            }
            return proceedOrNull(joinPoint);
        }

        return joinPoint.proceed();
    }

    private UUID resolveScopeId(ProceedingJoinPoint joinPoint, Authz authz) {
        // 1. Try SpEL expression first
        if (authz.scopeId() != null && !authz.scopeId().isBlank()) {
            try {
                StandardEvaluationContext context = new StandardEvaluationContext();
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Parameter[] parameters = signature.getMethod().getParameters();
                Object[] args = joinPoint.getArgs();

                for (int i = 0; i < parameters.length; i++) {
                    context.setVariable(parameters[i].getName(), args[i]);
                }

                String expression = authz.scopeId();
                Object result = parser.parseExpression(expression).getValue(context);
                if (result instanceof UUID) {
                    return (UUID) result;
                }
                if (result instanceof String) {
                    return UUID.fromString((String) result);
                }
            } catch (Exception e) {
                // Fall through to parameter inference
            }
        }

        // 2. Infer from first UUID parameter
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UUID) {
                return (UUID) arg;
            }
        }

        return null;
    }

    private UUID getCurrentUserId() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UUID) return (UUID) principal;
        if (principal instanceof String) {
            try { return UUID.fromString((String) principal); } catch (Exception e) { return null; }
        }
        return null;
    }

    private UUID getCurrentTenantId() {
        return com.discipolat.common.multitenancy.TenantContext.getTenantId();
    }

    private Object proceedOrNull(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return null;
        }
        if (returnType.isPrimitive()) {
            return java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(returnType, 1), 0);
        }
        return null;
    }
}