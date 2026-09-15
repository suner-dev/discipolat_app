# Security Matrix — §44-45 (G1.10)

> Matrice de sécurité vérifiable : **ressource × action × rôle × scope**, chaque cellule
> `AUTORISÉ`/`REFUSÉ` adossée à un test automatisé (`MultiTenantSecurityTests`, `mvn verify`).
> Une cellule ⚠️ (non prouvée) bloque la porte G1 (cf. checklist §G1.12).

## Légende
- ✅ AUTORISÉ (prouvé par test)
- ❌ REFUSÉ (prouvé par test — refus attendu et testé)
- ⚠️ Non prouvé (à couvrir avant commercial GO)

## Scopes (rappel §45)
`TENANT > REGION > CHURCH > SUB_CHURCH/CAMPUS > DEPARTMENT > FAMILY > ASSIGNED/OWN` —
un scope parent couvre ses enfants (testé : `tenantScope_coversChildren`, `assignedScope_onlyAssigned`).

## 1. Members (âmes/membres)

| Ressource / Action | PLATFORM_SUPER_ADMIN | TENANT_OWNER | TENANT_ADMIN | REGION_ADMIN | CHURCH_ADMIN | DEPARTMENT_ADMIN | FAMILY_LEADER | DISCIPLE_MAKER | MEMBER |
|---|---|---|---|---|---|---|---|---|---|
| members READ (tenant) | ✅ (tous tenants) | ✅ | ✅ | ✅ (sa région) | ✅ (son église) | ✅ (son département) | ✅ (sa famille) | ✅ (ses disciples) | ✅ (self) |
| members READ (autre tenant) | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR |
| members CREATE / UPDATE / DELETE | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

## 2. Departments

| Ressource / Action | PLATFORM_SUPER_ADMIN | TENANT_OWNER | TENANT_ADMIN | REGION_ADMIN | CHURCH_ADMIN | DEPARTMENT_ADMIN | Autres rôles |
|---|---|---|---|---|---|---|---|
| departments READ (son département) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| departments READ (autre département même tenant) | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ cross-scope | ❌ |
| departments MANAGE (postes, affectations, tâches) | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ (SES départements) | ❌ |

## 3. Settings / Branding / Modules (§27-29)

| Ressource / Action | PLATFORM_SUPER_ADMIN | TENANT_OWNER | TENANT_ADMIN | Autres rôles |
|---|---|---|---|---|
| tenant settings / branding / modules WRITE | ✅ | ✅ | ✅ | ❌ |
| module désactivé → accès données module | ✅ bypass | ❌ | ❌ | ❌ |

## 4. Invitations (§52 / G1.6)

| Ressource / Action | PLATFORM_SUPER_ADMIN | TENANT_OWNER | TENANT_ADMIN | Autres rôles | Invité (public) |
|---|---|---|---|---|---|
| invitations CREATE / RESEND / REVOKE | ✅ | ✅ | ✅ | ❌ | ❌ |
| invitations READ (liste tenant) | ✅* | ✅ | ✅ | ❌ | ❌ |
| invitations READ (autre tenant) | ✅* | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ |
| invitations VALIDATE / ACCEPT (token public) | ✅ | ✅ | ✅ | ✅ | ✅ (permitAll + rate-limit 5/min/IP + expiration + anti-rejeu statut PENDING) |
| invitation cross-tenant (IDOR sur scope) | n/a | ❌ | ❌ | ❌ | ❌ (membership créé dans le tenant de l'invitation uniquement) |

\* PLATFORM_SUPER_ADMIN : lecture transverse de diagnostic uniquement, journalisée.

## 5. Impersonation (§43 / G1.9)

| Ressource / Action | PLATFORM_SUPER_ADMIN réel | Falsifiant le rôle actif | Tenant admin / member | Cible super admin |
|---|---|---|---|---|
| POST /platform/admin/impersonation | ✅ (motif requis, journalisé) | ❌ (vérif en base) | ❌ | n/a |
| Impersonner une cible PLATFORM_SUPER_ADMIN | ❌ SUPER_ADMIN_IMPERSONATION_FORBIDDEN | ❌ | ❌ | n/a |
| Impersonner un membre d'un autre tenant (IDOR) | ✅ (cible conforme uniquement) | ❌ | ❌ | n/a |
| Token d'impersonation = identité cible (sans élévation, TTL 30 min) | ✅ | — | — | — |
| IMPERSONATION_END journalisé (durée, IP, UA) | ✅ | — | — | — |

## 6. Pastoral / confidentialité (§46)

| Ressource / Action | PLATFORM_SUPER_ADMIN | TENANT_OWNER | TENANT_ADMIN | CHURCH_ADMIN | DEPARTMENT_ADMIN | MEMBER |
|---|---|---|---|---|---|---|
| notes pastorales READ | ✅ | ✅ | ✅ | ✅ | ✅ (son département) | ❌ |
| notes pastorales READ (autre tenant) | ❌ IDOR* | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR | ❌ IDOR |
| cas pastoral WRITE | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |

\* La traversée n'est possible que VIA impersonation journalisée (§43).

## 7. Finance / Assets / Events / Workflow / Custom fields / Dress code

| Ressource / Action | PLATFORM_SUPER_ADMIN | TENANT_OWNER | TENANT_ADMIN | DEPARTMENT_ADMIN | FAMILY_LEADER | MEMBER |
|---|---|---|---|---|---|---|
| assets READ/WRITE (GLOBAL / son unité / autre unité) | ✅ | ✅ | ✅ | ✅ / ✅ (son unité) / ❌ | ❌ | ❌ |
| finance READ/WRITE | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| events WRITE | ✅ | ✅ | ✅ | ✅ (son département) | ❌ | ❌ |
| workflow configs / custom fields / dress code WRITE | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |

## Couverture de tests (base `MultiTenantSecurityTests`)

| Cellule / risque | Test |
|---|---|
| Cross-tenant READ (members/âmes) | `souls_isolated` + `members_cross_tenant_idor_*` |
| Cross-scope (department admin → autre dept) | `departmentAdmin_otherDepartment_refused` |
| Escalation membre → admin endpoint | `member_cannot_access_tenant_admin_endpoints` |
| Escalation DEPARTMENT_ADMIN → CHURCH_ADMIN | `departmentAdminCannotEscalateToChurchAdmin` |
| Scopes ASSIGNED / OWN / TENANT | `assignedScope_onlyAssigned`, `ownScope_onlyOwnId`, `tenantScope_coversChildren` |
| FAMILY_LEADER → autres familles | `familyLeaderCannotAccessOtherFamilies` |
| Impersonation (élévation, anti-escalade, IDOR) | `impersonationToken_carriesTargetIdentityOnly`, `impersonatingPlatformSuperAdmin_isForbidden`, `nonSuperAdmin_cannotStartImpersonation`, `forgedActiveRole_doesNotBypassDatabaseCheck`, `impersonation_targetTenantMismatch_refused` |
| Mass assignment (rôle forcé dans payload) | `invitationAccept_massAssignment_roleIgnored` |
| Module désactivé | `disabled_module_data_access_refused` |
| Pastoral par un membre | `member_cannot_read_pastoral_notes` |

**CI** : la matrice tourne dans `mvn verify` (tests `MultiTenantSecurityTests`). Une cellule ⚠️ restante = porte G1 non levée.