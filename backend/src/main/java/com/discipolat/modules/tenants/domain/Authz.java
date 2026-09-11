package com.discipolat.modules.tenants.domain;

import java.lang.annotation.*;

/**
 * Annotation for declarative authorization checks.
 * Can be used on methods or classes to enforce permissions.
 *
 * Usage examples:
 * 
 * @Authz(permission = "MEMBER_READ", scopeType = "DEPARTMENT", scopeId = "#deptId")
 * public Member getMember(UUID deptId, UUID memberId) { ... }
 * 
 * @Authz(permission = "EVENT_CREATE", scopeType = "CHURCH", scopeId = "#churchId")
 * @PostMapping
 * public Event createEvent(UUID churchId, EventRequest request) { ... }
 * 
 * @Authz(permission = "TENANT_SETTINGS_UPDATE")
 * @PutMapping("/settings")
 * public TenantSettings updateSettings(TenantSettingsRequest request) { ... }
 * 
 * The scopeId supports SpEL expressions like #paramName or #root.args[0]
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Authz {

    /**
     * The permission key to check (e.g., "MEMBER_READ", "EVENT_CREATE", "REPORT_UPDATE").
     */
    String permission();

    /**
     * The scope type for the permission check.
     * Values: "TENANT", "REGION", "CHURCH", "SUB_CHURCH", "CAMPUS", "DEPARTMENT", "FAMILY", "ASSIGNED", "OWN"
     */
    String scopeType() default "TENANT";

    /**
     * The scope ID (organization node ID).
     * Supports SpEL expressions like #deptId, #churchId, #paramName.
     * If empty, will be inferred from the first UUID parameter.
     */
    String scopeId() default "";

    /**
     * If true, throws AccessDeniedException on failure.
     * If false, returns false/empty (for conditional logic).
     */
    boolean enforce() default true;
}