# Tenant Security Guide — Discipolat Church OS

**Version**: 1.0  
**Date**: 2026-09-22  
**Status**: ✅ PRODUCTION READY

---

## 1. Security Principles

### 1.1 Zero Trust Architecture
- **Never trust, always verify**: Every request validated at multiple layers
- **Defense in depth**: Database → API → Cache → Real-time → Frontend
- **Least privilege**: Minimum permissions by default

### 1.2 Critical Rules (Non-Negotiable)

> **NEVER** consider as proof of authorization:
> - `tenantId` present in request (body/query/header)
> - `tenantId` present in frontend state
> - Button hidden in UI
> - User knows the resource ID

> **ALWAYS** verify server-side:
> 1. **IDENTITY**: User authenticated (valid JWT)
> 2. **MEMBERSHIP**: User belongs to tenant (active TenantMembership)
> 3. **TENANT**: Server-resolved tenant context
> 4. **PERMISSION**: Role + Permission + Scope authorize action
> 5. **OWNERSHIP**: Resource belongs to tenant/context

---

## 2. Isolation Layers

### 2.1 Database Layer (Primary Defense)

| Mechanism | Implementation | Coverage |
|-----------|---------------|----------|
| Hibernate Filter | `@FilterDef(name="tenantFilter", condition="tenant_id = :tenantId")` | 260+ entities |
| ThreadLocal Context | `TenantContext.CURRENT_TENANT` (UUID) | All request paths |
| Composite Indexes | `idx_{table}_tenant`, `idx_{table}_tenant_{col}` | All tenant-scoped tables |
| Unique Constraints | `(tenant_id, email)`, `(tenant_id, code)` | Identity & config tables |
| RLS (PostgreSQL) | `ALTER TABLE ... ENABLE ROW LEVEL SECURITY` | `audit_event`, `finance_transaction`, `file_entity` |

### 2.2 API Layer

| Mechanism | Implementation |
|-----------|---------------|
| JWT Validation | RS256, 15-min access / 7-day refresh, tenantId claim |
| TenantInterceptor | Extracts tenantId from JWT, validates membership |
| AuthorizationService | Centralized `@PreAuthorize("@authorizationService.can(...)")` |
| Repository Guards | All repositories extend `TenantAwareRepository` → `findByIdAndTenantId()` |
| Rate Limiting | Bucket4j: login 10/min, refresh 20/min, invite 5/min, impersonation 3/min |

### 2.3 Cache Layer (Redis)

| Mechanism | Implementation |
|-----------|---------------|
| Key Prefixing | `TenantAwareRedisManager.buildKey(key)` → `tenant:{tenantId}:{key}` |
| @Cacheable Isolation | `TenantAwareKeyGenerator` auto-prefixes cache keys |
| Template Helper | `TenantAwareRedisTemplateConfig.tenantAwareRedisTemplate()` |

### 2.4 File Storage

| Mechanism | Implementation |
|-----------|---------------|
| Path Isolation | `tenants/{tenantId}/{module}/{uuid}.{ext}` |
| Download Validation | `FileController:/{id}/download` checks tenant ownership |
| Type Validation | MIME + extension allowlist (images, docs, audio, video) |
| Size Limits | Controller (10MB) + nginx (50MB) |
| Path Traversal | UUID filenames + `Path.normalize()` + tenant prefix validation |

### 2.5 Real-time (WebSocket/SSE)

| Mechanism | Implementation |
|-----------|---------------|
| Room Isolation | `tenant:{tenantId}:config`, `tenant:{tenantId}:notifications` |
| Connect Validation | JWT tenant claim verified on handshake |
| SSE Scoping | `/events/entity-changes` filtered by tenant |

---

## 3. Attack Vector Mitigations

### 3.1 IDOR (Insecure Direct Object References)

| Vector | Mitigation | Test |
|--------|------------|------|
| `GET /api/users/{id}` | `UserRepository.findByIdAndTenantId(id, tenantId)` | `members_cross_tenant_idor_*` |
| `GET /api/files/{id}/download` | Ownership check in controller | `file_isolation_test` |
| `PUT /api/events/{id}` | Scope check via `AuthorizationService` | `event_scope_test` |

### 3.2 Privilege Escalation

| Vector | Mitigation | Test |
|--------|------------|------|
| Member → Admin endpoint | `@PreAuthorize("@authz.isTenantAdmin()")` | `member_cannot_access_tenant_admin_endpoints` |
| Dept Admin → Church Admin | Scope hierarchy enforcement | `departmentAdminCannotEscalateToChurchAdmin` |
| Forged JWT role claim | Server-side role validation from DB | `forgedActiveRole_doesNotBypassDatabaseCheck` |
| Impersonation elevation | Token carries target identity only, no elevation | `impersonationToken_carriesTargetIdentityOnly` |

### 3.3 Cross-Tenant Access

| Vector | Mitigation | Test |
|--------|------------|------|
| Tenant A → Tenant B data | Hibernate filter + membership validation | `souls_isolated`, `members_cross_tenant_idor_*` |
| WebSocket cross-tenant | Room prefix `tenant:{id}:` + connect validation | `tenant_isolated_ws` |
| Cache key collision | `tenant:{id}:` prefix on all keys | `redis_tenant_isolation_test` |
| File access cross-tenant | Path prefix + download validation | `file_cross_tenant_test` |

### 3.4 Mass Assignment

| Vector | Mitigation | Test |
|--------|------------|------|
| Role in invitation payload | Server ignores client-provided roles | `invitationAccept_massAssignment_roleIgnored` |
| Permission in user create | Permissions derived only from DB | `permissions_not_grantable_via_client_attributes` |

### 3.5 Disabled Module Access

| Vector | Mitigation | Test |
|--------|------------|------|
| Access disabled module data | `ModuleRouter` interceptor → 403 | `disabled_module_data_access_refused` |
| Frontend route access | `RequireFeature` guard hides routes | E2E test |

---

## 4. Secrets Management

### 4.1 Repository Scan
```bash
# Scanned patterns: SECRET, PASSWORD, API_KEY, TOKEN, JWT_PRIVATE_KEY
# Result: ✅ No hardcoded secrets in source code
```

### 4.2 Configuration
| Secret | Storage | Rotation |
|--------|---------|----------|
| JWT Private/Public Key | `/app/keys/` (Docker volume) / Secret Manager | Generated via `setup-keys.sh` (RSA 2048) |
| Database Password | Docker secrets / Render env vars | Manual |
| Redis Password | Docker secrets / Render env vars | Manual |
| Mailgun/SMTP Credentials | Render env vars (sync: false) | Manual |
| Webhook HMAC Secrets | Per-provider config (WhatsApp, Mobile Money) | Manual |
| Encryption AES Key | `ENCRYPTION_AES_KEY` env var | Manual |

### 4.3 Key Rotation Policy
- **JWT**: RS256, 15-min access / 7-day refresh (rotation on use)
- **Impersonation**: 30-min TTL, single-use
- **Webhook HMAC**: Configurable per provider
- **Recommendation**: Rotate JWT keys quarterly

---

## 5. Dependency Security

### 5.1 Backend (Maven)
```bash
# OWASP Dependency-Check (requires NVD API key)
# CISA Known Exploited Vulnerabilities: ✅ Checked
# Manual review: ✅ No critical CVEs in direct dependencies
```
**Action Required**: Configure `NVD_API_KEY` in CI for automated scanning

### 5.2 Frontend (npm audit)
| Severity | Count | Location | Status |
|----------|-------|----------|--------|
| Critical | 0 | - | ✅ |
| High | 4 | Dev deps (esbuild, browserslist, brace-expansion) | ⚠️ Dev only |
| Moderate | 6 | Dev/test deps (@vitest/mocker, baseline-browser-mapping) | ⚠️ Dev only |
| Low | 0 | - | ✅ |

**Assessment**: No production runtime dependencies affected. Fix via `npm update` in next sprint.

### 5.3 Mobile (flutter pub outdated)
| Package | Current | Latest | Risk |
|---------|---------|--------|------|
| `cached_network_image` | 3.4.1 | 4.0.0 | Major - test required |
| `connectivity_plus` | 6.1.5 | 7.3.1 | Breaking API |
| `firebase_core` | 3.15.2 | 4.15.0 | Major |
| `flutter_local_notifications` | 18.0.1 | 22.3.1 | Major |
| `flutter_riverpod` | 2.6.1 | 3.4.3 | Major |
| `go_router` | 14.8.1 | 18.0.1 | Major |
| `drift` | 2.28.2 | 2.35.0 | Minor |

**Discontinued packages**: `flutter_secure_storage_macos`, `js`, `build_resolvers`, `build_runner_core` — migration planned for v1.1.

---

## 6. Security Headers & Transport

### 6.1 Response Headers (Verified via `SecurityHeadersTest`)
| Header | Value |
|--------|-------|
| `Strict-Transport-Security` | `max-age=31536000; includeSubDomains; preload` |
| `X-Frame-Options` | `DENY` |
| `X-Content-Type-Options` | `nosniff` |
| `Content-Security-Policy` | Restrictive, nonce-based for scripts |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | `camera=(), microphone=(), geolocation=(), payment=()` |

### 6.2 TLS Configuration
- **Production**: TLS 1.2+ enforced (nginx)
- **Outbound**: Certificate validation on all connections
- **HSTS**: Preload ready

---

## 7. Audit Trail & Compliance

| Table | Purpose | Integrity | Retention |
|-------|---------|-----------|-----------|
| `audit_event` | All mutations | Hash chain (prev_hash/hash) | 95 days |
| `business_history` | Business-level traceability | Immutable append-only | 7 years |
| `export_audit` | Data exports (who/what/when) | Immutable | 3 years |
| `impersonation_audit` | Super admin sessions (IP, UA, duration, reason) | Immutable | 3 years |
| `soft_delete_audit` | Soft deletes with business context | Immutable | 7 years |

### 7.1 GDPR Endpoints
| Endpoint | Purpose |
|----------|---------|
| `POST /api/compliance/export` | Full data export (JSON) |
| `POST /api/compliance/delete` | Right to erasure |
| `GET /api/compliance/audit` | Personal audit trail |

---

## 8. Security Testing

### 8.1 Automated Test Suite
```bash
# Backend security tests
mvn test -Dtest=MultiTenantSecurityTests
# Result: 36 tests, 0 failures

# Full backend test suite
mvn test
# Result: 1188 tests, 0 failures

# Frontend lint + tests
npm run lint && npm run test
# Result: 0 errors, 320 tests pass

# Mobile analyze + tests
flutter analyze && flutter test
# Result: 0 errors, 331 tests pass
```

### 8.2 Penetration Testing
- **Internal**: Completed (G6.6) — No critical/high findings in production code
- **External**: Scheduled pre-public-launch with 3rd party auditor

---

## 9. Incident Response

### 9.1 Security Event Classification

| Severity | Examples | Response Time | Escalation |
|----------|----------|---------------|------------|
| **Critical** | Active breach, data exfiltration | < 1 hour | Platform Super Admin + CTO |
| **High** | Failed IDOR attempts spike, brute force | < 4 hours | Tenant Admin + Security |
| **Medium** | Suspicious login patterns, config drift | < 24 hours | Tenant Admin |
| **Low** | Scan attempts, deprecated API usage | < 7 days | Automated |

### 9.2 Key Runbooks
| Scenario | Runbook |
|----------|---------|
| Suspected cross-tenant breach | `docs/runbooks/CROSS_TENANT_BREACH.md` |
| Compromised JWT key | `docs/runbooks/JWT_KEY_COMPROMISE.md` |
| Database credential leak | `docs/runbooks/DB_CREDENTIAL_LEAK.md` |
| Ransomware / data encryption | `docs/runbooks/RANSOMWARE.md` |

---

## 10. Compliance Checklist

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Tenant isolation (DB) | ✅ | Hibernate filter + 36 security tests |
| Tenant isolation (API) | ✅ | AuthorizationService + @PreAuthorize |
| Tenant isolation (Cache) | ✅ | TenantAwareRedisManager + tests |
| Tenant isolation (Files) | ✅ | Path prefix + download validation |
| Tenant isolation (Realtime) | ✅ | Room prefix + connect validation |
| IDOR prevention | ✅ | Repository guards + 10 IDOR tests |
| Privilege escalation prevention | ✅ | Scope hierarchy + 6 escalation tests |
| Mass assignment protection | ✅ | Server-side derivation + test |
| Disabled module enforcement | ✅ | ModuleRouter + RequireFeature + test |
| Secrets management | ✅ | No hardcoded secrets, externalized |
| Dependency scanning | ⚠️ | NVD API key needed for automation |
| Security headers | ✅ | All headers verified in tests |
| TLS enforcement | ✅ | nginx config + HSTS preload |
| Audit trail integrity | ✅ | Hash chain verified periodically |
| GDPR compliance | ✅ | Export/Delete endpoints implemented |
| Security matrix coverage | ✅ | 320/320 cells proven by tests |

---

## 11. References

- `docs/security/SECURITY_MATRIX.md` — 320-cell permission matrix
- `reports/SECURITY_AUDIT_REPORT.md` — Final security audit (G6.6)
- `docs/MULTI_TENANT_ARCHITECTURE.md` — Architecture overview
- `docs/RBAC.md` — Role-based access control
- `backend/src/main/java/com/discipolat/common/infrastructure/security/AuthorizationService.java`
- `backend/src/test/java/com/discipolat/modules/tenants/MultiTenantSecurityTests.java`