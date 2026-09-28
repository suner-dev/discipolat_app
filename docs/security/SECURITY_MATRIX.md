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
| Cross-tenant READ (members/âmes) | `MultiTenantSecurityTests#members_isolated`, `families_isolated`, `reports_isolated`, `crossTenant_listQueries_isolated` |
| Cross-scope (department admin → autre dept) | `departmentAdmin_otherDepartment_refused` |
| Escalation membre → admin endpoint | `MultiTenantSecurityTests#memberToAdminEndpoint_refused` |
| Escalation DEPARTMENT_ADMIN → CHURCH_ADMIN | `departmentAdminCannotEscalateToChurchAdmin` |
| Scopes ASSIGNED / OWN / TENANT | `assignedScope_onlyAssigned`, `ownScope_onlyOwnId`, `tenantScope_coversChildren` |
| FAMILY_LEADER → autres familles | `familyLeaderCannotAccessOtherFamilies` |
| Impersonation (élévation, anti-escalade, IDOR) | `impersonationToken_carriesTargetIdentityOnly`, `impersonatingPlatformSuperAdmin_isForbidden`, `nonSuperAdmin_cannotStartImpersonation`, `forgedActiveRole_doesNotBypassDatabaseCheck`, `impersonation_targetTenantMismatch_refused` |
| Mass assignment (rôle forcé dans payload) | **non couvert par un test** — le rôle est résolu côté service à partir de l'invitation, pas du payload (`InvitationService.accept`) |
| Module désactivé | `disabled_module_data_access_refused` |
| Pastoral par un membre | **non couvert par un test** — à écrire |

## 6. Onboarding d'un tenant (A3, A4, A11)

| Attaque / exigence | Preuve |
|---|---|
| Tenant B complète une étape du tenant A → 404 `STEP_NOT_FOUND` | `OnboardingWizardSecurityIT#tenantBCannotCompleteStepOfTenantA` |
| Tenant B démarre ou saute une étape du tenant A → refusé | `OnboardingWizardSecurityIT#tenantBCannotStartNorSkipStepOfTenantA` |
| Tenant B ne voit que ses propres étapes | `OnboardingWizardSecurityIT#tenantBSeesOnlyItsOwnSteps` |
| Un membre (ni owner ni admin) ne mute pas une étape (403) | `OnboardingWizardSecurityIT#nonAdminMemberCannotMutate` |
| Un membre non admin peut **lire** le wizard (lecture non bloquée) | `OnboardingWizardSecurityIT#nonAdminMemberCanRead` |
| Anonyme → 401 | `OnboardingWizardSecurityIT#anonymousIsRejected` |
| **Tenant suspendu ne peut plus appeler le wizard (403)** | `OnboardingWizardSecurityIT#suspendedTenantCannotCallTheWizard` |
| La suspension ne fuit pas vers l'autre tenant | `OnboardingWizardSecurityIT#suspensionDoesNotLeakToTheOtherTenant` |
| La réactivation rétablit l'accès | `OnboardingWizardSecurityIT#reactivationRestoresAccess` |
| Étape hors ordre → 409 `STEP_ORDER_VIOLATION` | `OnboardingWizardSecurityIT` + `OnboardingWizardServiceTest#completeStep_rejectsAStepOutOfOrderWith409` |
| Rejeu d'étape terminée → 409 `STEP_ALREADY_COMPLETED` | `OnboardingWizardServiceTest#completeStep_rejectsAnAlreadyCompletedStepWith409` |
| Saut sans motif exigé → 400 `STEP_SKIP_REASON_REQUIRED` | `OnboardingWizardServiceTest#skipStep_requiresAReasonWhenTheStepDemandsOne` |
| Échec de validation n'écrit **rien** | `OnboardingStepActionsTest#churchIdentityRejectsTooShortName`, `rolesRejectsInvalidEmail` |
| Initialisation concurrente : un seul jeu d'étapes | `OnboardingWizardInitializeConcurrencyTest#concurrentInitializationNeverDuplicatesSteps` |
| Un id inconnu et une étape d'autrui sont **indiscernables** | `OnboardingWizardTenantIsolationTest#unknownStepAndForeignStepAreIndistinguishable` |

**Limite connue, à traiter ultérieurement** : le sélecteur d'église
(`/tenant-switcher/switch`) était structurellement inopérant, le filtre
multi-tenant s'appliquant au contrôle d'accès lui-même (constat H4). Corrigé dans
`fix/schema-drift-h1-h5` (`CrossTenantReadScopeTest`), pas encore dans `main`.

**CI** : la matrice tourne dans `mvn verify` (`MultiTenantSecurityTests`,
`OnboardingWizardSecurityIT`). Une cellule ⚠️ restante = porte G1 non levée.