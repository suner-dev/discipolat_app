# Discipolat Backend — Complete Module Inventory

> **425 Java files | 39 modules | Multi-tenant SaaS (PostgreSQL + Spring Boot)**

---

## Architecture Overview

### Core Infrastructure

| File | Role |
|---|---|
| `common/domain/UserRole.java` | Role enum: `ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR, MEMBRE` |
| `common/domain/BusinessRuleException.java` | Business validation error |
| `common/domain/EntityNotFoundException.java` | Standard entity 404 |
| `common/infrastructure/security/SecurityUtils.java` | JWT security context: `getCurrentUserId()`, `isSuperUser()`, `hasActiveRole()` |
| `common/infrastructure/api/PageResponse.java` | Generic paginated response wrapper |
| `common/infrastructure/api/TenantContext.java` | ThreadLocal tenant ID holder |
| `common/infrastructure/persistence/TenantAwareRepository.java` | Base repository with tenant filter support |
| `common/infrastructure/persistence/MultiTenantInterceptor.java` | JPA interceptor for `tenant_id` auto-fill |

### Tenant Isolation

All entities except `Tenant` itself carry `@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")` and a `tenant_id UUID NOT NULL` column.

### Permission System

- **Spring Security annotations**: `@PreAuthorize("hasAnyRole('ADMIN','PASTEUR')")` on controllers
- **Database permission matrix**: `role_permissions`, `permission_catalog`, `platform_roles` tables (raw SQL in `PermissionService`)
- **Guard bean**: `PermissionGuard` as SpEL bean `@perm.has('DOMAIN','ACTION')` for fine-grained checks
- **Workspace scoping**: `WorkspaceScopeService` restricts data by active role — changing role = changing entire data perimeter

---

## Module-by-Module Inventory

---

### 1. `admin` — 1 file

| File | Description |
|---|---|
| `AdminCacheController.java` | Cache statistics endpoint |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/admin/cache-stats` | ADMIN | Cache statistics |

---

### 2. `ai` — 2 files

| File | Description |
|---|---|
| `AiAssistantController.java` | AI assistant endpoints |
| `AiAssistantService.java` | AI service logic |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/ai/analyze/{soulId}` | ADMIN, PASTEUR, RESPONSABLE | Soul spiritual analysis |
| GET | `/api/v1/ai/resume/{soulId}` | ADMIN, PASTEUR, RESPONSABLE | Soul resume/summary |
| GET | `/api/v1/ai/encouragement/{soulId}` | ADMIN, PASTEUR, RESPONSABLE | Generate encouragement text |

---

### 3. `alerts` — 6 files

| File | Description |
|---|---|
| `Alert.java` | Entity — table `alerts` |
| `AlertController.java` | CRUD controller |
| `AlertService.java` | Alert business logic |
| `AlertRepository.java` | Data access |
| `AlertResponse.java` | Response DTO |
| `CreateAlertRequest.java` | Request DTO |

**Entity: `Alert`** — table `alerts`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, auto-generated |
| tenant_id | UUID | NOT NULL, multi-tenant filter |
| titre | String | NOT NULL |
| message | String | |
| type | String | Alert type |
| priorite | String | LOW / MOYENNE / HAUTE |
| statut | StatutAlerte | ACTIVE / LUE / TRAITEE / ANNULEE |
| cible_role | String | Target role |
| cible_user_id | UUID | Target user (nullable) |
| famile_id | UUID | Family scope |
| date_echeance | LocalDate | Deadline |
| resolu_par | UUID | Resolver |
| created_at | LocalDateTime | Auto-set |
| updated_at | LocalDateTime | Auto-updated |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/alerts` | ALL | List my alerts |
| POST | `/api/v1/alerts` | ADMIN, PASTEUR | Create alert |
| PUT | `/api/v1/alerts/{id}` | ADMIN, PASTEUR | Update alert |
| DELETE | `/api/v1/alerts/{id}` | ADMIN, PASTEUR | Delete alert |
| PATCH | `/api/v1/alerts/{id}/read` | ALL | Mark as read |
| PATCH | `/api/v1/alerts/{id}/resolve` | ALL | Mark as resolved |
| GET | `/api/v1/alerts/my` | ALL | Current user's alerts |
| GET | `/api/v1/alerts/count/unread` | ALL | Unread alert count |

---

### 4. `appointments` — 7 files

| File | Description |
|---|---|
| `Appointment.java` | Entity — table `appointments` |
| `AppointmentController.java` | Controller |
| `AppointmentService.java` | Service |
| `AppointmentRepository.java` | Repository |
| `AppointmentResponse.java` | Response DTO |
| `CreateAppointmentRequest.java` | Request DTO |
| `UpdateAppointmentStatusRequest.java` | Status update DTO |

**Entity: `Appointment`** — table `appointments`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| titre | String | NOT NULL |
| description | String | |
| date_heure | LocalDateTime | NOT NULL |
| duree_minutes | Integer | Default 30 |
| lieu | String | |
| type_rdv | String | RENCONTRE, SUIVI, CONSEIL, PASTORAL |
| statut | String | PLANIFIE, CONFIRME, ANNULE, TERMINE |
| pasteur_id | UUID | Assigned pastor |
| membre_id | UUID | Concerned member |
| famille_id | UUID | Family scope |
| notes | String | |
| created_by | UUID | |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/appointments` | ALL | List appointments |
| POST | `/api/v1/appointments` | ADMIN, PASTEUR, RESPONSABLE | Create |
| PUT | `/api/v1/appointments/{id}` | ADMIN, PASTEUR, RESPONSABLE | Update |
| PATCH | `/api/v1/appointments/{id}/status` | ALL | Change status |
| GET | `/api/v1/appointments/{id}` | ALL | Get one |
| DELETE | `/api/v1/appointments/{id}` | ADMIN, PASTEUR | Delete |
| GET | `/api/v1/appointments/upcoming` | ALL | Upcoming appointments |

---

### 5. `audit` — 7 files

| File | Description |
|---|---|
| `AuditLog.java` | Entity — table `audit_logs` |
| `AuditController.java` | Audit log query controller |
| `PermissionController.java` | Permission matrix API |
| `PermissionService.java` | Permission service (raw SQL) |
| `PermissionGuard.java` | SpEL guard bean `@perm.has()` |
| `AuditLogRepository.java` | Repository |
| `AuditService.java` | Audit service |

**Entity: `AuditLog`** — table `audit_logs`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| user_id | UUID | Actor |
| action | String | Action type |
| entity_type | String | Entity class name |
| entity_id | UUID | Entity ID |
| details | String | JSON change details |
| ip_address | String | |
| user_agent | String | |
| created_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/audit` | ADMIN, PASTEUR | Query audit logs |
| GET | `/api/v1/audit/entity/{type}/{id}` | ADMIN, PASTEUR | Audit trail for entity |
| GET | `/api/v1/permissions` | ADMIN | Get permission matrix |
| GET | `/api/v1/permissions/{role}` | ADMIN | Get permissions for role |
| PUT | `/api/v1/permissions/{role}` | ADMIN | Update role permissions |
| GET | `/api/v1/permissions/catalog` | ADMIN | Permission catalog |
| POST | `/api/v1/permissions/catalog` | ADMIN | Add permission to catalog |

---

### 6. `authentication` — 13 files

| File | Description |
|---|---|
| `AuthController.java` | Login, register, refresh |
| `TwoFactorController.java` | 2FA enable/disable/verify |
| `AuthService.java` | Auth logic |
| `TwoFactorService.java` | TOTP 2FA service |
| `EmailService.java` | Email sending |
| `ActivationToken.java` | Entity — table `activation_tokens` |
| `ActivationTokenRepository.java` | Repository |
| `PasswordResetToken.java` | Entity — table `password_reset_tokens` |
| `PasswordResetTokenRepository.java` | Repository |
| `AuthResponse.java` | Response DTO |
| `LoginRequest.java` | Request DTO |
| `RefreshRequest.java` | Request DTO |
| `TwoFactorSetupResponse.java` | 2FA setup DTO |

**Entities:**
- `ActivationToken` — table `activation_tokens` (id, tenantId, userId, token, expiresAt, used, createdAt)
- `PasswordResetToken` — table `password_reset_tokens` (id, tenantId, userId, token, expiresAt, used, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | PUBLIC | Login, returns JWT |
| POST | `/api/v1/auth/register` | PUBLIC | Register (church admin only) |
| POST | `/api/v1/auth/refresh` | PUBLIC | Refresh JWT |
| GET | `/api/v1/auth/me` | ALL | Current user profile |
| POST | `/api/v1/auth/demo-login` | PUBLIC | Demo user login |
| POST | `/api/v1/auth/2fa/enable` | ALL | Enable 2FA |
| POST | `/api/v1/auth/2fa/disable` | ALL | Disable 2FA |
| POST | `/api/v1/auth/2fa/verify` | ALL | Verify 2FA code |
| GET | `/api/v1/auth/2fa/setup-status` | ALL | 2FA setup status |

---

### 7. `badges` — 6 files

| File | Description |
|---|---|
| `Badge.java` | Entity — table `badges` |
| `UserBadge.java` | Entity — table `user_badges` |
| `BadgeController.java` | Controller |
| `BadgeService.java` | Service |
| `BadgeRepository.java` | Repository |
| `UserBadgeRepository.java` | Repository |

**Entities:**
- `Badge` — table `badges` (id, tenantId, nom, description, icone, categorie, critere, actif, createdAt)
- `UserBadge` — table `user_badges` (id, tenantId, userId, badgeId, dateObtention, evaluatedBy, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/badges` | ALL | List all active badges |
| GET | `/api/v1/badges/my` | ALL | My earned badges |
| GET | `/api/v1/badges/users/{userId}` | ALL | User's earned badges |
| GET | `/api/v1/badges/leaderboard` | ALL | Badge leaderboard |
| POST | `/api/v1/badges/evaluate` | ADMIN, PASTEUR, RESPONSABLE | Evaluate and award badge |

---

### 8. `communications` — 5 files

| File | Description |
|---|---|
| `Communication.java` | Entity — table `communications` |
| `CommunicationController.java` | Controller |
| `CommunicationService.java` | Service |
| `CommunicationRepository.java` | Repository |
| `CommunicationRequest.java` | Request DTO |

**Entity: `Communication`** — table `communications`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| titre | String | NOT NULL |
| contenu | String | NOT NULL, rich text |
| type | String | INFO, URGENT, EVENEMENT, PASTORAL |
| statut | String | BROUILLON, ENVOYE, PLANIFIE |
| cible_role | String | Target role |
| cible_famille_id | UUID | Target family |
| cible_user_id | UUID | Target user |
| sent_by | UUID | Sender |
| date_envoi | LocalDateTime | |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/communications` | ALL | List communications |
| POST | `/api/v1/communications` | ADMIN, PASTEUR | Create |
| PUT | `/api/v1/communications/{id}` | ADMIN, PASTEUR | Update |
| DELETE | `/api/v1/communications/{id}` | ADMIN, PASTEUR | Delete |
| PATCH | `/api/v1/communications/{id}/send` | ADMIN, PASTEUR | Send communication |

---

### 9. `customfields` — 6 files

| File | Description |
|---|---|
| `CustomFieldDefinition.java` | Entity — table `custom_field_definitions` |
| `CustomFieldValue.java` | Entity — table `custom_field_values` |
| `CustomFieldController.java` | Controller |
| `CustomFieldService.java` | Service |
| `CustomFieldDefinitionRepository.java` | Repository |
| `CustomFieldValueRepository.java` | Repository |

**Entities:**
- `CustomFieldDefinition` — table `custom_field_definitions` (id, tenantId, nom, type, options, required, entityTarget, actif, createdAt, updatedAt)
- `CustomFieldValue` — table `custom_field_values` (id, tenantId, definitionId, entityId, entityTarget, valeur, createdAt, updatedAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/custom-fields` | ALL | List definitions |
| POST | `/api/v1/custom-fields` | ADMIN | Create definition |
| PUT | `/api/v1/custom-fields/{id}` | ADMIN | Update definition |
| DELETE | `/api/v1/custom-fields/{id}` | ADMIN | Delete definition |
| GET | `/api/v1/custom-fields/{entityTarget}/{entityId}` | ALL | Get values for entity |
| PUT | `/api/v1/custom-fields/{entityTarget}/{entityId}` | ALL | Set values for entity |

---

### 10. `dashboard` — 2 files

| File | Description |
|---|---|
| `DashboardController.java` | Dashboard KPIs |
| `DashboardService.java` | Aggregation logic |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/dashboard/kpi` | ALL | Dashboard KPIs (counts, trends) |
| GET | `/api/v1/dashboard/presence-trend` | ALL | Weekly presence trend |
| GET | `/api/v1/dashboard/family-risk` | ALL | Family risk overview |
| GET | `/api/v1/dashboard/soul-risk` | ALL | Soul at-risk overview |

---

### 11. `departments` — 61 files (largest module)

| File | Description |
|---|---|
| `Department.java` | Entity — table `departments` |
| `DepartmentController.java` | Basic CRUD |
| `DepartmentManagementController.java` | Advanced management |
| `DepartmentEventAttendanceController.java` | Attendance sub-resource |
| `DepartmentService.java` | Core service |
| `DepartmentManagementService.java` | Advanced service |
| `DepartmentSettingsService.java` | Settings |
| `DepartmentDossierService.java` | Dossier/gestion |
| `DepartmentReportingService.java` | Reporting |
| `DepartmentRepository.java` | Repository |
| 20+ entity files | Sub-entities for tasks, equipment, documents, etc. |
| 15+ request/response DTOs | Request/response objects |

**Core Entity: `Department`** — table `departments`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| nom | String | NOT NULL |
| description | String | |
| responsable_id | UUID | Department head |
| parent_id | UUID | Self-referencing hierarchy |
| type | String | MINISTERE, EQUIPE, COMMISSION |
| actif | boolean | Default true |
| date_creation | LocalDate | |
| created_by | UUID | |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Sub-Entities (all with `tenant_id` filter):**
- `DepartmentTask` — table `department_tasks` (tasks within department)
- `DepartmentEquipment` — table `department_equipments` (equipment tracking)
- `DepartmentDocument` — table `department_documents` (shared documents)
- `DepartmentMemberNote` — table `department_member_notes` (member notes)
- `DepartmentMemberObjective` — table `department_member_objectives` (member objectives)
- `DepartmentMemberReport` — table `department_member_reports` (member reports)
- `DepartmentAnnouncement` — table `department_announcements` (announcements)
- `DepartmentActivity` — table `department_activities` (activity log)
- `DepartmentAssignment` — table `department_assignments` (task assignments)
- `DepartmentChecklist` — table `department_checklists` (checklists)
- `DepartmentChecklistItem` — table `department_checklist_items` (checklist items)
- `DepartmentEventAttendance` — table `department_event_attendances`
- `DepartmentPosition` — table `department_positions` (positions/titles)
- `DepartmentReport` — table `department_reports` (department reports)
- `DepartmentSetting` — table `department_settings` (key-value settings)
- `DepartmentTeam` — table `department_teams` (sub-teams)

**Endpoints (main):**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/departments` | ALL | List departments |
| POST | `/api/v1/departments` | ADMIN, PASTEUR, RESPONSABLE | Create |
| PUT | `/api/v1/departments/{id}` | ADMIN, PASTEUR, RESPONSABLE | Update |
| DELETE | `/api/v1/departments/{id}` | ADMIN, PASTEUR | Delete |
| GET | `/api/v1/departments/{id}` | ALL | Get department |
| GET | `/api/v1/departments/{id}/members` | ALL | List members |
| POST | `/api/v1/departments/{id}/members` | RESPONSABLE | Add member |
| DELETE | `/api/v1/departments/{id}/members/{soulId}` | RESPONSABLE | Remove member |
| GET | `/api/v1/departments/{id}/tasks` | ALL | List tasks |
| POST | `/api/v1/departments/{id}/tasks` | RESPONSABLE | Create task |
| PUT | `/api/v1/departments/{id}/tasks/{taskId}` | RESPONSABLE | Update task |
| GET | `/api/v1/departments/{id}/equipment` | ALL | List equipment |
| POST | `/api/v1/departments/{id}/equipment` | RESPONSABLE | Add equipment |
| GET | `/api/v1/departments/{id}/documents` | ALL | List documents |
| POST | `/api/v1/departments/{id}/documents` | RESPONSABLE | Add document |
| GET | `/api/v1/departments/{id}/notes` | ALL | Member notes |
| POST | `/api/v1/departments/{id}/notes` | RESPONSABLE | Add note |
| GET | `/api/v1/departments/{id}/objectives` | ALL | List objectives |
| POST | `/api/v1/departments/{id}/objectives` | RESPONSABLE | Add objective |
| GET | `/api/v1/departments/{id}/checklists` | ALL | List checklists |
| POST | `/api/v1/departments/{id}/checklists` | RESPONSABLE | Add checklist |
| GET | `/api/v1/departments/{id}/reports` | ALL | Department reports |
| POST | `/api/v1/departments/{id}/reports` | RESPONSABLE | Submit report |
| POST | `/api/v1/departments/import` | ADMIN, PASTEUR | Bulk import departments |
| GET | `/api/v1/departments/{id}/attendance` | ALL | Event attendance |
| POST | `/api/v1/departments/{id}/attendance` | RESPONSABLE | Record attendance |

---

### 12. `discipline` — 6 files

| File | Description |
|---|---|
| `SoulDisciplineEvent.java` | Entity — table `soul_discipline_events` |
| `SoulDisciplineEventController.java` | Controller |
| `SoulDisciplineEventService.java` | Service |
| `SoulDisciplineEventRepository.java` | Repository |
| `CategorieDiscipline.java` | Enum: MORALE, SPIRITUELLE, FINANCIERE, DISCIPLINAIRE, ACCUEIL |
| `GraviteDiscipline.java` | Enum: FAIBLE, MOYENNE, FORTE, CRITIQUE |

**Entity: `SoulDisciplineEvent`** — table `soul_discipline_events`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| ame_id | UUID | Target soul |
| auteur_id | UUID | Author |
| categorie | CategorieDiscipline | NOT NULL |
| type_evenement | String | Event type |
| gravite | GraviteDiscipline | |
| titre | String | NOT NULL |
| description | String | |
| date_evenement | LocalDate | NOT NULL |
| resolu | boolean | Default false |
| date_resolution | LocalDate | |
| resolu_par | UUID | |
| created_at / updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/souls/{soulId}/discipline` | ALL | List discipline events for soul |
| POST | `/api/v1/souls/{soulId}/discipline` | PASTEUR, RESPONSABLE | Create event |
| PUT | `/api/v1/souls/{soulId}/discipline/{id}` | PASTEUR, RESPONSABLE | Update event |
| PATCH | `/api/v1/souls/{soulId}/discipline/{id}/resolve` | PASTEUR, RESPONSABLE | Mark resolved |

---

### 13. `evaluations` — 5 files

| File | Description |
|---|---|
| `Evaluation.java` | Entity — table `evaluations` |
| `EvaluationController.java` | Controller |
| `EvaluationService.java` | Service |
| `EvaluationRepository.java` | Repository |
| `CategorieEvaluation.java` | Enum: PASTORALE, SPIRITUELLE, ENGAGEMENT, ATTENDANCE, LEADERSHIP |

**Entity: `Evaluation`** — table `evaluations`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| evaluateur_id | UUID | Evaluator |
| evalue_id | UUID | Person evaluated |
| categorie | CategorieEvaluation | NOT NULL |
| score | Integer | 1–10 |
| commentaire | String | |
| date_evaluation | LocalDate | |
| visible_par_evalue | boolean | Default false |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/evaluations` | ALL | List my evaluations |
| POST | `/api/v1/evaluations` | PASTEUR, RESPONSABLE | Create evaluation |
| GET | `/api/v1/evaluations/{id}` | ALL | Get evaluation |
| PUT | `/api/v1/evaluations/{id}` | PASTEUR, RESPONSABLE | Update evaluation |
| GET | `/api/v1/evaluations/user/{userId}` | ALL | User's evaluations |

---

### 14. `evangelism` — 10 files

| File | Description |
|---|---|
| `EvangelismTrack.java` | Entity — table `evangelism_track` |
| `EvangelismStageHistory.java` | Entity — table `evangelism_stage_history` |
| `EvangelismController.java` | Controller |
| `EvangelismService.java` | Service |
| `EvangelismTrackRepository.java` | Repository |
| `EvangelismStageHistoryRepository.java` | Repository |
| `EvangelismEtape.java` | Enum: NON_CONTACTE, PREMIER_CONTACT, SUVI, INVITE_CULLES, PREMIER_CULLES, BAPTEME |
| `EvangelismStatsResponse.java` | Stats DTO |
| `EvangelismTrackResponse.java` | Track DTO |
| `UpdateEvangelismRequest.java` | Request DTO |

**Entities:**
- `EvangelismTrack` — table `evangelism_track` (id, tenantId, ameId, contactsId, etape, dateEtape, notes, created_at, updated_at)
- `EvangelismStageHistory` — table `evangelism_stage_history` (id, tenantId, trackId, fromEtape, toEtape, changedBy, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/evangelism/tracks` | ALL | List evangelism tracks |
| POST | `/api/v1/evangelism/tracks` | PASTEUR, RESPONSABLE, FAISEUR | Create track |
| PUT | `/api/v1/evangelism/tracks/{id}` | PASTEUR, RESPONSABLE, FAISEUR | Update track |
| GET | `/api/v1/evangelism/tracks/soul/{soulId}` | ALL | Track for a soul |
| PATCH | `/api/v1/evangelism/tracks/{id}/advance` | PASTEUR, RESPONSABLE, FAISEUR | Advance stage |
| GET | `/api/v1/evangelism/stats` | ALL | Evangelism statistics |

---

### 15. `events` — 12 files

| File | Description |
|---|---|
| `Event.java` | Entity — table `events` |
| `EventRegistration.java` | Entity — table `event_registrations` |
| `WeeklyProgramTemplate.java` | Entity — table `weekly_program_templates` |
| `EventController.java` | Controller |
| `EventService.java` | Service |
| `EventRepository.java` | Repository |
| `EventRegistrationRepository.java` | Repository |
| `WeeklyProgramTemplateRepository.java` | Repository |
| `CreateEventRequest.java` | Request DTO |
| `UpdateEventRequest.java` | Request DTO |
| `EventResponse.java` | Response DTO |
| `EventRegistrationResponse.java` | Response DTO |

**Entities:**
- `Event` — table `events` (id, tenantId, titre, description, dateDebut, dateFin, lieu, type, capacite, recurrente, recurrenceJour, recurrenceHeure, statut, creePar, createdAt, updatedAt)
- `EventRegistration` — table `event_registrations` (id, tenantId, eventId, utilisateurId, statutInscription, dateInscription, createdAt)
- `WeeklyProgramTemplate` — table `weekly_program_templates` (id, tenantId, jour, heureDebut, heureFin, titre, description, lieu, type, actif, createdAt, updatedAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/events` | ALL | List events |
| POST | `/api/v1/events` | ADMIN, PASTEUR, RESPONSABLE | Create event |
| PUT | `/api/v1/events/{id}` | ADMIN, PASTEUR, RESPONSABLE | Update event |
| DELETE | `/api/v1/events/{id}` | ADMIN, PASTEUR | Delete event |
| GET | `/api/v1/events/{id}` | ALL | Get event |
| POST | `/api/v1/events/{id}/register` | ALL | Register for event |
| DELETE | `/api/v1/events/{id}/unregister` | ALL | Unregister |
| GET | `/api/v1/events/{id}/registrations` | ADMIN, PASTEUR, RESPONSABLE | List registrations |
| GET | `/api/v1/events/my-registrations` | ALL | My registrations |
| GET | `/api/v1/events/weekly-program` | ALL | Weekly program template |
| POST | `/api/v1/events/weekly-program` | ADMIN, PASTEUR | Create template |
| PUT | `/api/v1/events/weekly-program/{id}` | ADMIN, PASTEUR | Update template |

---

### 16. `families` — 12 files

| File | Description |
|---|---|
| `Family.java` | Entity — table `families` |
| `FamilyChiefHistory.java` | Entity — table `family_chief_history` |
| `FamilyRiskHistory.java` | Entity — table `family_risk_history` |
| `FamilyController.java` | Controller |
| `FamilyService.java` | Service |
| `FamilyRiskService.java` | Risk calculation |
| `FamilyRepository.java` | Repository |
| `FamilyChiefHistoryRepository.java` | Repository |
| `FamilyRiskHistoryRepository.java` | Repository |
| `CreateFamilyRequest.java` | Request DTO |
| `FamilyResponse.java` | Response DTO |
| `ReassignChiefRequest.java` | Request DTO |

**Entities:**
- `Family` — table `families` (id, tenantId, nom, description, chefFamilleId, risque, nbMembres, statut, dateCreation, createdAt, updatedAt, deleted)
- `FamilyChiefHistory` — table `family_chief_history` (id, tenantId, familleId, ancienChefId, nouveauChefId, dateChangement, changedBy, createdAt)
- `FamilyRiskHistory` — table `family_risk_history` (id, tenantId, familleId, ancienRisque, nouveauRisque, dateChangement, changedBy, raison, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/families` | ALL | List families |
| POST | `/api/v1/families` | ADMIN, PASTEUR, RESPONSABLE | Create |
| PUT | `/api/v1/families/{id}` | ADMIN, PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE | Update |
| DELETE | `/api/v1/families/{id}` | ADMIN, PASTEUR | Delete |
| GET | `/api/v1/families/{id}` | ALL | Get family |
| GET | `/api/v1/families/{id}/souls` | ALL | Souls in family |
| PATCH | `/api/v1/families/{id}/reassign-chief` | ADMIN, PASTEUR | Reassign chief |
| GET | `/api/v1/families/{id}/risk-history` | ALL | Risk change history |
| PATCH | `/api/v1/families/{id}/risk` | ADMIN, PASTEUR, RESPONSABLE | Update risk level |
| GET | `/api/v1/families/my` | CHEF_DE_FAMILLE | My managed family |
| GET | `/api/v1/families/transfers` | ALL | Family transfers |

---

### 17. `favorites` — 5 files

| File | Description |
|---|---|
| `Favorite.java` | Entity — table `favorites` |
| `FavoriteController.java` | Controller |
| `FavoriteService.java` | Service |
| `FavoriteRepository.java` | Repository |
| `FavoriteEntityType.java` | Enum: SOUL, FAMILY, DEPARTMENT, EVENT |

**Entity: `Favorite`** — table `favorites`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| user_id | UUID | Owner |
| entity_id | UUID | Favorited entity |
| entity_type | FavoriteEntityType | Entity kind |
| created_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/api/v1/favorites/toggle` | ALL | Toggle favorite |
| GET | `/api/v1/favorites/check` | ALL | Is entity favorited? |
| GET | `/api/v1/favorites` | ALL | List my favorites |
| GET | `/api/v1/favorites/souls` | ALL | My favorite souls |

---

### 18. `files` — 12 files

| File | Description |
|---|---|
| `FileEntity.java` | Entity — table `files` |
| `EntityAttachment.java` | Entity — table `entity_attachments` |
| `FileController.java` | File CRUD + download |
| `BulkImportController.java` | CSV import |
| `FileService.java` | File service |
| `EntityAttachmentService.java` | Attachment service |
| `BulkImportService.java` | Import logic |
| `FileEntityRepository.java` | Repository |
| `EntityAttachmentRepository.java` | Repository |
| `CreateFileRequest.java` | Request DTO |
| `UpdateFileRequest.java` | Request DTO |
| `FileResponse.java` | Response DTO |

**Entities:**
- `FileEntity` — table `files` (id, tenantId, nom, typeMime, taille, url, uploadedBy, createdAt)
- `EntityAttachment` — table `entity_attachments` (id, tenantId, entityType, entityId, fileId, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| POST | `/api/v1/files` | ALL | Upload file |
| GET | `/api/v1/files/{id}` | ALL | Get file info |
| GET | `/api/v1/files/{id}/download` | ALL | Download file |
| DELETE | `/api/v1/files/{id}` | ALL | Delete file |
| POST | `/api/v1/files/{entityType}/{entityId}/attach` | ALL | Attach file to entity |
| GET | `/api/v1/files/{entityType}/{entityId}` | ALL | List attachments |
| DELETE | `/api/v1/files/attachments/{attachmentId}` | ALL | Remove attachment |
| POST | `/api/v1/import/families` | ADMIN, PASTEUR | Bulk import families CSV |
| POST | `/api/v1/import/users` | ADMIN | Bulk import users CSV |
| POST | `/api/v1/import/souls` | ADMIN, PASTEUR | Bulk import souls CSV |

---

### 19. `finances` — 8 files

| File | Description |
|---|---|
| `FinanceTransaction.java` | Entity — table `finance_transactions` |
| `FinanceBudget.java` | Entity — table `finance_budgets` |
| `FinanceController.java` | Controller |
| `FinanceService.java` | Service |
| `FinanceTransactionRepository.java` | Repository |
| `FinanceBudgetRepository.java` | Repository |
| `FinanceTransactionRequest.java` | Request DTO |
| `FinanceBudgetRequest.java` | Request DTO |

**Entities:**
- `FinanceTransaction` — table `finance_transactions` (id, tenantId, type, categorie, montant, description, date, familleId, userId, statut, modePaiement, reference, creePar, createdAt, updatedAt)
- `FinanceBudget` — table `finance_budgets` (id, tenantId, categorie, montantAnnuel, montantUtilise, annee, createdAt, updatedAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/finances` | ADMIN, PASTEUR, RESPONSABLE | List transactions |
| POST | `/api/v1/finances` | ADMIN, PASTEUR, RESPONSABLE | Create transaction |
| PUT | `/api/v1/finances/{id}` | ADMIN, PASTEUR, RESPONSABLE | Update transaction |
| DELETE | `/api/v1/finances/{id}` | ADMIN, PASTEUR | Delete transaction |
| GET | `/api/v1/finances/stats` | ADMIN, PASTEUR | Financial statistics |
| GET | `/api/v1/finances/budgets` | ADMIN, PASTEUR | List budgets |
| POST | `/api/v1/finances/budgets` | ADMIN | Create budget |
| PUT | `/api/v1/finances/budgets/{id}` | ADMIN | Update budget |

---

### 20. `interactions` — 7 files

| File | Description |
|---|---|
| `Interaction.java` | Entity — table `soul_interactions` |
| `InteractionController.java` | Controller |
| `InteractionService.java` | Service |
| `InteractionRepository.java` | Repository |
| `InteractionType.java` | Enum: APPEL, VISITE, RENCONTRE, MESSAGE, EMAIL |
| `CreateInteractionRequest.java` | Request DTO |
| `InteractionResponse.java` | Response DTO |

**Entity: `Interaction`** — table `soul_interactions`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| ame_id | UUID | Target soul |
| utilisateur_id | UUID | Actor |
| type | InteractionType | NOT NULL |
| description | String | |
| date_interaction | LocalDateTime | NOT result, actual datetime |
| duree_minutes | Integer | |
| resultat | String | |
| created_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/souls/{soulId}/interactions` | ALL | List interactions |
| POST | `/api/v1/souls/{soulId}/interactions` | FAISEUR, CHEF_DE_FAMILLE, RESPONSABLE | Create interaction |
| PUT | `/api/v1/souls/{soulId}/interactions/{id}` | FAISEUR, CHEF_DE_FAMILLE, RESPONSABLE | Update |
| DELETE | `/api/v1/souls/{soulId}/interactions/{id}` | FAISEUR, CHEF_DE_FAMILLE, RESPONSABLE | Delete |
| GET | `/api/v1/souls/{soulId}/interactions/recent` | ALL | Recent interactions |

---

### 21. `map` — 4 files

| File | Description |
|---|---|
| `MapController.java` | Map data endpoints |
| `MapService.java` | Service |
| `MapPointResponse.java` | Response DTO |
| `UpdateCoordinatesRequest.java` | Request DTO |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/map/points` | ALL | Get map points (souls, families) |
| PUT | `/api/v1/map/coordinates` | ALL | Update user coordinates |
| GET | `/api/v1/map/souls` | ALL | Souls with coordinates |

---

### 22. `members` — 18 files

| File | Description |
|---|---|
| `MemberDepartment.java` | Entity — table `member_departments` |
| `MemberPresence.java` | Entity — table `member_presences` |
| `MemberRequest.java` | Entity — table `member_requests` |
| `MemberController.java` | Controller |
| `MemberService.java` | Service |
| `MemberDepartmentRepository.java` | Repository |
| `MemberPresenceRepository.java` | Repository |
| `MemberRequestRepository.java` | Repository |
| 8 request/response DTOs | Various DTOs |

**Entities:**
- `MemberDepartment` — table `member_departments` (id, tenantId, memberId, departmentId, role, dateAffectation, actif, createdAt)
- `MemberPresence` — table `member_presences` (id, tenantId, memberId, date, present, culte, raisonAbsence, notedBy, createdAt)
- `MemberRequest` — table `member_requests` (id, tenantId, memberId, type, description, statut, traitePar, dateTraitement, commentaire, createdAt, updatedAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/members/dashboard` | ALL | Member dashboard |
| GET | `/api/v1/members/{id}/profile` | ALL | Member profile |
| PUT | `/api/v1/members/{id}/profile` | ALL | Update profile |
| GET | `/api/v1/members/presences` | ALL | List presences |
| POST | `/api/v1/members/presences` | RESPONSABLE | Record presence |
| GET | `/api/v1/members/presences/my` | ALL | My presences |
| GET | `/api/v1/members/requests` | ALL | List member requests |
| POST | `/api/v1/members/requests` | ALL | Submit request |
| PATCH | `/api/v1/members/requests/{id}/status` | RESPONSABLE | Process request |

---

### 23. `messages` — 10 files

| File | Description |
|---|---|
| `Conversation.java` | Entity — table `conversations` |
| `ConversationMessage.java` | Entity — table `conversation_messages` |
| `MessageController.java` | Controller |
| `MessageService.java` | Service |
| `ConversationRepository.java` | Repository |
| `ConversationMessageRepository.java` | Repository |
| `ConversationResponse.java` | Response DTO |
| `MessageResponse.java` | Response DTO |
| `SendMessageRequest.java` | Request DTO |
| `StartConversationRequest.java` | Request DTO |

**Entities:**
- `Conversation` — table `conversations` (id, tenantId, participant1Id, participant2Id, lastMessageAt, createdAt)
- `ConversationMessage` — table `conversation_messages` (id, tenantId, conversationId, senderId, content, readAt, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/messages/conversations` | ALL | My conversations |
| POST | `/api/v1/messages/conversations` | ALL | Start conversation |
| GET | `/api/v1/messages/conversations/{id}` | ALL | Conversation details |
| GET | `/api/v1/messages/conversations/{id}/messages` | ALL | Messages in conversation |
| POST | `/api/v1/messages/conversations/{id}/messages` | ALL | Send message |
| PATCH | `/api/v1/messages/conversations/{id}/read` | ALL | Mark as read |
| GET | `/api/v1/messages/unread/count` | ALL | Unread message count |

---

### 24. `notifications` — 5 files

| File | Description |
|---|---|
| `Notification.java` | Entity — table `notifications` |
| `NotificationController.java` | Controller |
| `NotificationService.java` | Service |
| `NotificationRepository.java` | Repository |
| `NotificationResponse.java` | Response DTO |

**Entity: `Notification`** — table `notifications`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| user_id | UUID | Recipient |
| titre | String | NOT NULL |
| message | String | NOT NULL |
| type | TypeNotification | ALERTE, RAPPEL, MISE_A_JOUR, SYSTEME |
| lu | boolean | Default false |
| lien | String | Deep link |
| created_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/notifications` | ALL | List my notifications |
| GET | `/api/v1/notifications/unread/count` | ALL | Unread count |
| PATCH | `/api/v1/notifications/{id}/read` | ALL | Mark as read |
| PATCH | `/api/v1/notifications/read-all` | ALL | Mark all as read |
| DELETE | `/api/v1/notifications/{id}` | ALL | Delete notification |

---

### 25. `objectives` — 7 files

| File | Description |
|---|---|
| `Objective.java` | Entity — table `objectives` |
| `ObjectiveController.java` | Controller |
| `ObjectiveService.java` | Service |
| `ObjectiveRepository.java` | Repository |
| `ObjectiveType.java` | Enum: SPIRITUEL, PASTORAL, FINANCIER, EVANGELISME, FORMATION, DISCIPLINE |
| `CreateObjectiveRequest.java` | Request DTO |
| `ObjectiveProgressResponse.java` | Response DTO |

**Entity: `Objective`** — table `objectives`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| titre | String | NOT NULL |
| description | String | |
| type | ObjectiveType | NOT NULL |
| cible_id | UUID | Target soul |
| cible_type | String | SOUL / FAMILY / DEPARTMENT |
| echeance | LocalDate | Deadline |
| statut | String | EN_COURS, ATTEINT, NON_ATTEINT, ABANDONNE |
| progression | Integer | 0–100 |
| cree_par | UUID | Creator |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/objectives` | ALL | List objectives |
| POST | `/api/v1/objectives` | PASTEUR, RESPONSABLE | Create objective |
| PUT | `/api/v1/objectives/{id}` | PASTEUR, RESPONSABLE | Update objective |
| DELETE | `/api/v1/objectives/{id}` | PASTEUR, RESPONSABLE | Delete objective |
| PATCH | `/api/v1/objectives/{id}/progress` | ALL | Update progress |
| GET | `/api/v1/objectives/my-progress` | ALL | My objectives progress |

---

### 26. `parallelfollowups` — 6 files

| File | Description |
|---|---|
| `ParallelFollowup.java` | Entity — table `parallel_followups` |
| `ParallelFollowupController.java` | Controller |
| `ParallelFollowupService.java` | Service |
| `ParallelFollowupRepository.java` | Repository |
| `ParallelFollowupResponse.java` | Response DTO |
| `CreateParallelFollowupRequest.java` | Request DTO |

**Entity: `ParallelFollowup`** — table `parallel_followups`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| ame_id | UUID | Target soul |
| suiveur_id | UUID | Follower |
| raison | RaisonSuiviParallele | Reason |
| statut | StatutSuiviParallele | EN_COURS, TERMINE, ABANDONNE |
| date_debut | LocalDate | |
| date_fin | LocalDate | |
| notes | String | |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/parallel-followups` | ALL | List my parallel followups |
| POST | `/api/v1/parallel-followups` | PASTEUR, RESPONSABLE | Create |
| PUT | `/api/v1/parallel-followups/{id}` | PASTEUR, RESPONSABLE | Update |
| PATCH | `/api/v1/parallel-followups/{id}/status` | PASTEUR, RESPONSABLE | Change status |
| GET | `/api/v1/parallel-followups/soul/{soulId}` | ALL | Followups for soul |

---

### 27. `platform` — 42 files

| File | Description |
|---|---|
| `PlatformModule.java` | Entity — table `platform_modules` |
| `MenuEntry.java` | Entity — table `menu_entries` |
| `ChurchSettings.java` | Entity — table `church_settings` |
| `CustomPage.java` | Entity — table `custom_pages` |
| `Feedback.java` | Entity — table `feedbacks` |
| `DictionaryEntry.java` | Entity — table `dictionary_entries` |
| `ConfigRevision.java` | Entity — table `config_revisions` |
| `PlatformConfigController.java` | Module/menu config |
| `SettingsController.java` | Church branding settings |
| `FeedbackController.java` | User feedback |
| `PageBuilderController.java` | Custom pages |
| `DictionaryController.java` | Dictionary |
| `PlatformMetaController.java` | Platform metadata |
| `BetaAdminController.java` | Beta admin tools |
| 7 services | Various service classes |
| 2 infrastructure files | ModuleGateConfig, ModuleGateFilter |
| 10+ DTOs | Request/response objects |

**Entities:**
- `PlatformModule` — table `platform_modules` (id, tenantId, code, nom, description, actif, visible, menuIcon, menuOrder, configJson, createdAt, updatedAt)
- `MenuEntry` — table `menu_entries` (id, tenantId, moduleId, label, path, icon, order, parentMenuId, actif, visible, createdAt, updatedAt)
- `ChurchSettings` — table `church_settings` (id, tenantId, churchName, slogan, logoUrl, primaryColor, secondaryColor, accentColor, welcomeMessage, createdAt, updatedAt)
- `CustomPage` — table `custom_pages` (id, tenantId, slug, titre, contenu, layout, actif, creePar, createdAt, updatedAt)
- `Feedback` — table `feedbacks` (id, tenantId, userId, type, message, statut, adminResponse, traitePar, dateTraitement, createdAt)
- `DictionaryEntry` — table `dictionary_entries` (id, tenantId, cle, valeur, categorie, createdAt, updatedAt)
- `ConfigRevision` — table `config_revisions` (id, tenantId, entity_type, entity_id, changement, utilisateur_id, createdAt)

**Key Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/platform/modules` | ALL | List modules |
| PUT | `/api/v1/platform/modules/{code}` | ADMIN | Toggle module |
| GET | `/api/v1/platform/menu` | ALL | Menu configuration |
| PUT | `/api/v1/platform/menu` | ADMIN | Update menu order |
| GET | `/api/v1/platform/settings` | ALL | Church branding settings |
| PUT | `/api/v1/platform/settings` | ADMIN | Update branding |
| POST | `/api/v1/platform/feedback` | ALL | Submit feedback |
| GET | `/api/v1/platform/feedback` | ADMIN | List feedback |
| GET | `/api/v1/platform/pages` | ALL | List custom pages |
| POST | `/api/v1/platform/pages` | ADMIN | Create page |
| PUT | `/api/v1/platform/pages/{slug}` | ADMIN | Update page |
| GET | `/api/v1/platform/pages/{slug}` | ALL | Render page |
| GET | `/api/v1/platform/dictionary` | ALL | Dictionary entries |
| POST | `/api/v1/platform/dictionary` | ADMIN | Create entry |
| GET | `/api/v1/platform/meta` | PUBLIC | Platform metadata |
| GET | `/api/v1/platform/meta/public-branding` | PUBLIC | Public branding (no auth) |
| POST | `/api/v1/platform/admin/beta/reset` | ADMIN | Beta reset |

---

### 28. `prayers` — 7 files

| File | Description |
|---|---|
| `Prayer.java` | Entity — table `prayers` |
| `PrayerController.java` | Controller |
| `PrayerService.java` | Service |
| `PrayerRepository.java` | Repository |
| `CreatePrayerRequest.java` | Request DTO |
| `UpdatePrayerRequest.java` | Request DTO |
| `PrayerResponse.java` | Response DTO |

**Entity: `Prayer`** — table `prayers`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| utilisateur_id | UUID | Requester |
| titre | String | NOT NULL |
| contenu | String | NOT NULL |
| statut | String | EN_ATTENTE, EN_COURS, REPONDUE, ARCHIVEE |
| confidentialite | String | PRIVEE, FAMILLE, PUBLIC |
| prie_pour | UUID | Person being prayed for |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/prayers` | ALL | List prayer requests |
| POST | `/api/v1/prayers` | ALL | Create prayer request |
| PUT | `/api/v1/prayers/{id}` | ALL | Update own request |
| PATCH | `/api/v1/prayers/{id}/status` | ALL | Change status |
| GET | `/api/v1/prayers/my` | ALL | My prayer requests |
| GET | `/api/v1/prayers/community` | ALL | Community prayers (public) |

---

### 29. `programs` — 8 files

| File | Description |
|---|---|
| `ProgramType.java` | Entity — table `program_types` |
| `ProgramSubType.java` | Entity — table `program_sub_types` |
| `ProgramController.java` | Controller |
| `ProgramService.java` | Service |
| `ProgramTypeRepository.java` | Repository |
| `ProgramSubTypeRepository.java` | Repository |
| `ProgramTypeRequest.java` | Request DTO |
| `ProgramTypeResponse.java` | Response DTO |

**Entities:**
- `ProgramType` — table `program_types` (id, tenantId, nom, description, actif, createdAt, updatedAt)
- `ProgramSubType` — table `program_sub_types` (id, tenantId, typeId, nom, description, actif, createdAt, updatedAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/programs` | ALL | List program types |
| POST | `/api/v1/programs` | ADMIN | Create program type |
| PUT | `/api/v1/programs/{id}` | ADMIN | Update program type |
| GET | `/api/v1/programs/{id}/sub-types` | ALL | Sub-types |
| POST | `/api/v1/programs/{id}/sub-types` | ADMIN | Create sub-type |
| PUT | `/api/v1/programs/sub-types/{id}` | ADMIN | Update sub-type |
| GET | `/api/v1/programs/active` | ALL | Active programs only |

---

### 30. `reports` — 15 files

| File | Description |
|---|---|
| `MakerReport.java` | Entity — table `maker_reports` |
| `FamilyReport.java` | Entity — table `family_reports` |
| `ReportCorrection.java` | Entity — table `report_corrections` |
| `MakerReportController.java` | Maker report CRUD |
| `ReportExportController.java` | Export reports |
| `ReportService.java` | Service |
| `MakerReportRepository.java` | Repository |
| `FamilyReportRepository.java` | Repository |
| `ReportCorrectionRepository.java` | Repository |
| 6 DTOs | Request/response objects |

**Entities:**
- `MakerReport` — table `maker_reports` (id, tenantId, ameId, faiseurId, familleId, semaine, presencesParCulte, observations, soumis, dateSoumission, valide, validePar, dateValidation, statut, statutValidation, pointsForts, axesAmelioration, encouragements, createdAt, updatedAt)
- `FamilyReport` — table `family_reports` (id, tenantId, familleId, chefFamilleId, semaine, nbPresents, nbAbsents, observations, risqueSignale, soumis, dateSoumission, statut, createdAt, updatedAt)
- `ReportCorrection` — table `report_corrections` (id, tenantId, reportType, reportId, correctedBy, ancienStatut, nouveauStatut, commentaire, date, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/reports/maker` | ALL | List maker reports |
| POST | `/api/v1/reports/maker` | FAISEUR | Submit maker report |
| PUT | `/api/v1/reports/maker/{id}` | FAISEUR | Update maker report |
| PATCH | `/api/v1/reports/maker/{id}/validate` | RESPONSABLE, PASTEUR | Validate report |
| GET | `/api/v1/reports/family` | ALL | List family reports |
| POST | `/api/v1/reports/family` | CHEF_DE_FAMILLE | Submit family report |
| PUT | `/api/v1/reports/family/{id}` | CHEF_DE_FAMILLE | Update family report |
| PATCH | `/api/v1/reports/family/{id}/validate` | RESPONSABLE, PASTEUR | Validate family report |
| GET | `/api/v1/reports/export` | ALL | Export reports |
| GET | `/api/v1/reports/my-pending` | ALL | My pending reports |

---

### 31. `search` — 2 files

| File | Description |
|---|---|
| `SearchController.java` | Global search |
| `SearchService.java` | Cross-module search |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/search` | ALL | Global search across all modules |

---

### 32. `souls` — 36 files

| File | Description |
|---|---|
| `Soul.java` | Entity — table `souls` |
| `SoulNote.java` | Entity — table `soul_notes` |
| `SoulTag.java` | Entity — table `soul_tags` |
| `SoulHistory.java` | Entity — table `soul_history` |
| `SoulExit.java` | Entity — table `soul_exits` |
| `SoulRetractionRequest.java` | Entity — table `soul_retraction_requests` |
| `SoulDepartment.java` | Entity — table `soul_departments` |
| `SoulDepartmentId.java` | Composite PK class |
| `SpiritualScore.java` | Entity — table `spiritual_score_history` |
| `SoulController.java` | Main controller |
| `SoulNoteController.java` | Notes sub-resource |
| `SoulTagController.java` | Tags sub-resource |
| `SoulService.java` | Main service (639 lines) |
| `SoulExitService.java` | Exit/reintegration service |
| `SoulRetractionRequestService.java` | Retraction workflow service |
| `SoulNoteService.java` | Notes service |
| `SoulTagService.java` | Tags service |
| `SpiritualScoreService.java` | Score computation (199 lines) |
| `WorkspaceScopeService.java` | Role-based data scoping |
| `SoulRepository.java` | Repository (62 lines, 20+ methods) |
| `SoulHistoryRepository.java` | Repository |
| `SoulNoteRepository.java` | Repository |
| `SoulTagRepository.java` | Repository |
| `SoulExitRepository.java` | Repository |
| `SoulRetractionRequestRepository.java` | Repository |
| `SoulDepartmentRepository.java` | Repository |
| `SpiritualScoreRepository.java` | Repository |
| 8 DTOs | Request/response objects |

**Entity: `Soul`** — table `souls` (core entity, ~40 fields)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| nom | String | NOT NULL |
| prenom | String | |
| email | String | |
| telephone | String | |
| adresse | String | |
| date_naissance | LocalDate | |
| profession | String | |
| niveau_etude | String | |
| nb_enfants | Integer | |
| type_disciple | TypeDisciple | NOT NULL: NOUVEAU, EN_CROISSANCE, MATURE, ANCIEN, PASTEUR |
| date_integration | LocalDate | NOT NULL |
| date_conversion | LocalDate | |
| statut | StatutAme | EN_INTEGRATION, ACTIF, EN_VEILLE, DECROCHE |
| faiseur_id | UUID | NOT NULL, assigned pastor |
| user_id | UUID | Linked user account |
| famille_id | UUID | Family membership |
| situation_familiale | String | |
| etat_spirituel | String | NOUVEAU_CONVERTI, EN_CROISSANCE, MATURE, EN_DIFFICULTE |
| niveau_croissance | Integer | 1–5 |
| notes_pasteur | String | Private pastoral notes |
| date_dernier_contact | LocalDateTime | |
| latitude | Double | Geolocation |
| longitude | Double | Geolocation |
| zone | String | Geographic zone |
| created_at / updated_at | LocalDateTime | |
| deleted | boolean | Soft delete |

**Sub-Entities:**
- `SoulNote` — table `soul_notes` (id, tenantId, ameId, auteurId, contenu, createdAt, updatedAt, deleted)
- `SoulTag` — table `soul_tags` (id, tenantId, soulId, tag, createdAt) — max 3 tags per soul, normalized lowercase
- `SoulHistory` — table `soul_history` (id, tenantId, ameId, typeEvenement, description, ancienStatut, nouveauStatut, ancienFaiseurId, nouveauFaiseurId, utilisateurId, metadata JSONB, createdAt)
- `SoulExit` — table `soul_exits` (id, tenantId, ameId, faiseurId, motif, motifDetail, peutReintegrer, dateSortie, createdAt)
- `SoulRetractionRequest` — table `soul_retraction_requests` (id, tenantId, ameId, demandeurId, justification, statut EN_ATTENTE/APPROUVEE/REJETEE, traitePar, dateTraitement, commentaireReponse, createdAt)
- `SoulDepartment` — table `soul_departments` (composite PK: soulId + departmentId, tenantId, dateAffectation, dateDesaffectation, actif, createdBy, origine MANUEL/SIGNUP/TRANSFERT)
- `SpiritualScore` — table `spiritual_score_history` (id, tenantId, soulId, semaine, scoreGlobal, sante, fidelite, engagement, participation, createdAt)

**Key Endpoints (SoulController):**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/souls` | ALL | List souls (scoped by role) |
| POST | `/api/v1/souls` | ALL | Create soul |
| GET | `/api/v1/souls/{id}` | ALL | Get soul |
| PUT | `/api/v1/souls/{id}` | ALL | Update soul |
| DELETE | `/api/v1/souls/{id}` | ADMIN, PASTEUR, RESPONSABLE | Soft delete |
| PATCH | `/api/v1/souls/{id}/restore` | ADMIN, PASTEUR, RESPONSABLE | Restore from trash |
| GET | `/api/v1/souls/trash` | ADMIN, PASTEUR, RESPONSABLE | List soft-deleted |
| GET | `/api/v1/souls/{id}/history` | ALL | Soul history |
| PATCH | `/api/v1/souls/{id}/reassign` | ADMIN, PASTEUR, RESPONSABLE | Reassign faiseur (via transfer workflow) |
| GET | `/api/v1/souls/by-faiseur/{faiseurId}` | ALL | Souls by faiseur |
| GET | `/api/v1/souls/by-famille/{familleId}` | ALL | Souls by family |
| GET | `/api/v1/souls/suggest-faiseur/{familleId}` | ALL | Auto-suggest least loaded faiseur |
| GET | `/api/v1/souls/en-difficulte` | ADMIN, PASTEUR, RESPONSABLE | At-risk souls |
| POST | `/api/v1/souls/retraction-request` | ALL | Request soul retraction |
| GET | `/api/v1/souls/retraction-requests` | ADMIN, PASTEUR, RESPONSABLE | List retraction requests |
| GET | `/api/v1/souls/{soulId}/retraction-requests` | ALL | Retraction requests for soul |
| PATCH | `/api/v1/souls/retraction-request/{id}/approve` | ADMIN, PASTEUR, RESPONSABLE | Approve retraction |
| PATCH | `/api/v1/souls/retraction-request/{id}/reject` | ADMIN, PASTEUR, RESPONSABLE | Reject retraction |
| POST | `/api/v1/souls/{id}/exit` | PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE, FAISEUR | Mark as exited |
| POST | `/api/v1/souls/{id}/reintegrate` | ADMIN, PASTEUR, RESPONSABLE | Reintegrate exited soul |
| GET | `/api/v1/souls/{id}/exits` | ALL | Exit history |
| GET | `/api/v1/souls/{id}/spiritual-score` | ALL | Compute spiritual score |
| GET | `/api/v1/souls/{id}/spiritual-score/history` | ALL | Score evolution |
| GET | `/api/v1/souls/{id}/pastoral-360` | ALL | Dossier pastoral 360° |
| GET | `/api/v1/souls/filter` | ALL | Advanced filter |

**SoulNoteController (`/api/v1/souls/{soulId}/notes`):**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | (list) | ALL | List notes for soul |
| POST | (create) | ALL | Create note |
| PUT | `/{noteId}` | ALL | Update note |
| DELETE | `/{noteId}` | ALL | Delete note (soft) |

**SoulTagController (`/api/v1/souls`):**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/{id}/tags` | ALL | Get soul tags |
| POST | `/{id}/tags` | ALL | Add tag (max 3) |
| DELETE | `/{id}/tags/{tag}` | ALL | Remove tag |
| GET | `/tags/available` | ALL | List all tags globally |

---

### 33. `tenants` — 3 files

| File | Description |
|---|---|
| `Tenant.java` | Entity — table `tenants` |
| `TenantRepository.java` | Repository |
| `TenantStatus.java` | Enum: ACTIVE, SUSPENDED, CANCELLED, PENDING_SETUP |

**Entity: `Tenant`** — table `tenants` (no tenant filter — this IS the tenant table)

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| name | String | NOT NULL |
| slug | String | NOT NULL, unique |
| status | TenantStatus | NOT NULL |
| plan | String | NOT NULL |
| created_at / updated_at | Instant | |

**Endpoints:** (managed via direct DB or admin CLI — no REST controller)

---

### 34. `trainings` — 21 files

| File | Description |
|---|---|
| `Course.java` | Entity — table `courses` |
| `CourseModule.java` | Entity — table `course_modules` |
| `CourseEnrollment.java` | Entity — table `course_enrollments` |
| `ModuleCompletion.java` | Entity — table `module_completions` |
| `QuizQuestion.java` | Entity — table `quiz_questions` |
| `Certificate.java` | Entity — table `certificates` |
| `TrainingController.java` | Controller |
| `TrainingService.java` | Service |
| `CourseRepository.java` | Repository |
| `CourseModuleRepository.java` | Repository |
| `CourseEnrollmentRepository.java` | Repository |
| `ModuleCompletionRepository.java` | Repository |
| `QuizQuestionRepository.java` | Repository |
| `CertificateRepository.java` | Repository |
| 7 DTOs | Request/response objects |

**Entities:**
- `Course` — table `courses` (id, tenantId, titre, description, categorie, dureeHeures, actif, creePar, createdAt, updatedAt)
- `CourseModule` — table `course_modules` (id, tenantId, courseId, titre, contenu, ordre, dureeMinutes, createdAt)
- `CourseEnrollment` — table `course_enrollments` (id, tenantId, courseId, utilisateurId, dateInscription, progression, statut, createdAt, updatedAt)
- `ModuleCompletion` — table `module_completions` (id, tenantId, moduleId, utilisateurId, completedAt, score, createdAt)
- `QuizQuestion` — table `quiz_questions` (id, tenantId, moduleId, question, options, reponseCorrecte, createdAt)
- `Certificate` — table `certificates` (id, tenantId, courseId, utilisateurId, numero, dateObtention, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/trainings/courses` | ALL | List courses |
| POST | `/api/v1/trainings/courses` | ADMIN | Create course |
| PUT | `/api/v1/trainings/courses/{id}` | ADMIN | Update course |
| GET | `/api/v1/trainings/courses/{id}` | ALL | Get course |
| GET | `/api/v1/trainings/courses/{id}/modules` | ALL | Course modules |
| POST | `/api/v1/trainings/courses/{id}/modules` | ADMIN | Add module |
| POST | `/api/v1/trainings/courses/{id}/enroll` | ALL | Enroll in course |
| GET | `/api/v1/trainings/my-courses` | ALL | My enrollments |
| PATCH | `/api/v1/trainings/modules/{id}/complete` | ALL | Mark module complete |
| POST | `/api/v1/trainings/modules/{id}/quiz` | ALL | Submit quiz |
| GET | `/api/v1/trainings/courses/{id}/certificate` | ALL | Get certificate |

---

### 35. `transfers` — 27 files

| File | Description |
|---|---|
| `TransferRequest.java` | Entity — table `transfer_requests` |
| `TransferDecision.java` | Entity — table `transfer_decisions` |
| `TransferWorkflowConfig.java` | Entity — table `transfer_workflow_configs` |
| `TransferWorkflowStep.java` | Entity — table `transfer_workflow_steps` |
| `TransferHistory.java` | Entity — table `transfer_history` |
| `TransferAttachment.java` | Entity — table `transfer_attachments` |
| `TransferController.java` | Main controller |
| `TransferAdminController.java` | Admin workflow config |
| `TransferWorkflowService.java` | Workflow engine |
| `TransferAdminService.java` | Admin operations |
| `TransferBridgeService.java` | Bridge (soul reassign → transfer) |
| `TransferExecutor.java` | Transfer execution |
| 6 repositories | Data access |
| 6 DTOs | Request/response objects |

**Entities:**
- `TransferRequest` — table `transfer_requests` (id, tenantId, ameId, sourceFamilleId, destinationFamilleId, sourceFaiseurId, destinationFaiseurId, motif, statut EN_ATTENTE/EN_COURS/VALIDE/REJETUE/ANNULEE, demandePar, dateDemande, createdAt, updatedAt)
- `TransferDecision` — table `transfer_decisions` (id, tenantId, transferRequestId, deciderId, decision DecisionType APPROUVE/REJET, commentaire, dateDecision, etape, createdAt)
- `TransferWorkflowConfig` — table `transfer_workflow_configs` (id, tenantId, nom, description, actif, circuitJson, createdBy, createdAt, updatedAt)
- `TransferWorkflowStep` — table `transfer_workflow_steps` (id, tenantId, configId, ordre, roleRequis, typeValidation, createdAt)
- `TransferHistory` — table `transfer_history` (id, tenantId, transferRequestId, action, ancienStatut, nouveauStatut, utilisateurId, commentaire, createdAt)
- `TransferAttachment` — table `transfer_attachments` (id, tenantId, transferRequestId, fileId, nom, createdAt)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/transfers` | ALL | List transfers |
| POST | `/api/v1/transfers` | PASTEUR, RESPONSABLE, CHEF_DE_FAMILLE | Request transfer |
| GET | `/api/v1/transfers/{id}` | ALL | Get transfer detail |
| PATCH | `/api/v1/transfers/{id}/decide` | PASTEUR, RESPONSABLE | Approve/reject |
| GET | `/api/v1/transfers/{id}/history` | ALL | Transfer history |
| PATCH | `/api/v1/transfers/{id}/cancel` | Requester | Cancel transfer |
| GET | `/api/v1/admin/transfers/workflows` | ADMIN | List workflow configs |
| POST | `/api/v1/admin/transfers/workflows` | ADMIN | Create workflow |
| PUT | `/api/v1/admin/transfers/workflows/{id}` | ADMIN | Update workflow |

---

### 36. `users` — 13 files

| File | Description |
|---|---|
| `User.java` | Entity — table `users` |
| `UserDepartment.java` | Entity — table `user_departments` |
| `UserDepartmentId.java` | Composite PK |
| `UserController.java` | Controller |
| `UserService.java` | Service |
| `UserRepository.java` | Repository |
| `UserDepartmentRepository.java` | Repository |
| `UserStatus.java` | Enum: ACTIF, INACTIF, EN_ATTENTE, SUSPENDU |
| 5 DTOs | Request/response objects |

**Entity: `User`** — table `users`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| email | String | NOT NULL, unique |
| password_hash | String | NOT NULL |
| first_name | String | NOT NULL |
| last_name | String | NOT NULL |
| telephone | String | |
| photo_url | String | |
| role | UserRole | NOT NULL |
| statut | UserStatus | ACTIF, INACTIF, EN_ATTENTE, SUSPENDU |
| deux_facteurs_actif | boolean | Default false |
| famille_geree_id | UUID | Family managed by CHEF_DE_FAMILLE |
| date_creation | Instant | |
| created_at / updated_at | Instant | |
| deleted | boolean | Soft delete |

**UserDepartment** — table `user_departments` (composite PK: userId + departmentId, tenantId, role, dateAffectation, actif)

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/users` | ALL | List users |
| POST | `/api/v1/users` | ADMIN | Create user |
| GET | `/api/v1/users/{id}` | ALL | Get user |
| PUT | `/api/v1/users/{id}` | ADMIN, self | Update user |
| DELETE | `/api/v1/users/{id}` | ADMIN | Soft delete user |
| PATCH | `/api/v1/users/{id}/role` | ADMIN | Change role |
| PATCH | `/api/v1/users/{id}/status` | ADMIN | Change status |
| PATCH | `/api/v1/users/{id}/2fa/enable` | self | Enable 2FA |
| PATCH | `/api/v1/users/{id}/2fa/disable` | self | Disable 2FA |
| PATCH | `/api/v1/users/{id}/promote-chef` | ADMIN | Promote to CHEF_DE_FAMILLE |
| PATCH | `/api/v1/users/{id}/transfer-init` | ADMIN | Initiate transfer |
| GET | `/api/v1/users/{id}/evaluations` | ALL | User evaluations |

---

### 37. `visits` — 7 files

| File | Description |
|---|---|
| `Visit.java` | Entity — table `visits` |
| `VisitController.java` | Controller |
| `VisitService.java` | Service |
| `VisitRepository.java` | Repository |
| `CreateVisitRequest.java` | Request DTO |
| `UpdateVisitRequest.java` | Request DTO |
| `VisitResponse.java` | Response DTO |

**Entity: `Visit`** — table `visits`

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | Multi-tenant filter |
| ame_id | UUID | Target soul |
| visiteur_id | UUID | Visitor |
| date_visite | LocalDate | NOT NULL |
| lieu | String | |
| type | String | DOMICILE, HOPITAL, MAISON, BUREAU |
| duree_minutes | Integer | |
| notes | String | |
| resultat | String | |
| created_at | LocalDateTime | |
| updated_at | LocalDateTime | |

**Endpoints:**
| Method | Path | Permission | Description |
|---|---|---|---|
| GET | `/api/v1/visits` | ALL | List visits |
| POST | `/api/v1/visits` | FAISEUR, CHEF_DE_FAMILLE, RESPONSABLE | Create visit |
| PUT | `/api/v1/visits/{id}` | FAISEUR, CHEF_DE_FAMILLE, RESPONSABLE | Update visit |
| GET | `/api/v1/visits/my` | ALL | My visits |
| GET | `/api/v1/visits/upcoming` | ALL | Upcoming visits |
| GET | `/api/v1/visits/soul/{soulId}` | ALL | Visits for a soul |

---

### 38. `workflow` — 1 file

| File | Description |
|---|---|
| `WorkflowService.java` | Scheduled automated tasks |

**Scheduled Tasks (via `@Scheduled`):**
- Automated absenteeism escalation (souls absent > N weeks → alert pasteur)
- Birthday reminders (notify CHEF_DE_FAMILLE of upcoming family birthdays)
- Spiritual score weekly snapshots (calls `SpiritualScoreService.snapshotAll()`)

---

## Cross-Cutting Patterns Summary

### Role-Based Access Matrix

| Capability | ADMIN | PASTEUR | RESPONSABLE | CHEF_DE_FAMILLE | FAISEUR | MEMBRE |
|---|---|---|---|---|---|---|
| Create soul | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| Delete soul | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| View all souls | ✓ | ✓ | scoped | scoped | scoped | ✗ |
| Manage departments | ✓ | ✓ | ✓ (own) | ✗ | ✗ | ✗ |
| Family reports | ✓ | ✓ | ✓ (validate) | ✓ (create) | ✗ | ✗ |
| Maker reports | ✓ | ✓ | ✓ (validate) | ✗ | ✓ (create) | ✗ |
| Transfer approve | ✓ | ✓ | ✓ | ✗ | ✗ | ✗ |
| Platform config | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| User management | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |

### Data Scoping (WorkspaceScopeService)

| Active Role | Visible Souls | Visible Families | Visible Departments |
|---|---|---|---|
| ADMIN | ALL | ALL | ALL |
| PASTEUR | ALL | ALL | ALL |
| RESPONSABLE | Souls in their departments | Families of those souls | Own departments only |
| CHEF_DE_FAMILLE | Souls in their managed family | Own managed family | None |
| FAISEUR | Their assigned disciples | Families of those souls | None |
| MEMBRE | None (empty) | None | None |

### API Base Paths

| Module | Base Path |
|---|---|
| admin | `/api/v1/admin` |
| ai | `/api/v1/ai` |
| alerts | `/api/v1/alerts` |
| appointments | `/api/v1/appointments` |
| audit | `/api/v1/audit` |
| authentication | `/api/v1/auth` |
| badges | `/api/v1/badges` |
| communications | `/api/v1/communications` |
| customfields | `/api/v1/custom-fields` |
| dashboard | `/api/v1/dashboard` |
| departments | `/api/v1/departments` |
| discipline | `/api/v1/souls/{soulId}/discipline` |
| evaluations | `/api/v1/evaluations` |
| evangelism | `/api/v1/evangelism` |
| events | `/api/v1/events` |
| families | `/api/v1/families` |
| favorites | `/api/v1/favorites` |
| files | `/api/v1/files` |
| finances | `/api/v1/finances` |
| interactions | `/api/v1/souls/{soulId}/interactions` |
| map | `/api/v1/map` |
| members | `/api/v1/members` |
| messages | `/api/v1/messages` |
| notifications | `/api/v1/notifications` |
| objectives | `/api/v1/objectives` |
| parallelfollowups | `/api/v1/parallel-followups` |
| platform | `/api/v1/platform` |
| prayers | `/api/v1/prayers` |
| programs | `/api/v1/programs` |
| reports | `/api/v1/reports` |
| search | `/api/v1/search` |
| souls | `/api/v1/souls` |
| trainings | `/api/v1/trainings` |
| transfers | `/api/v1/transfers` |
| users | `/api/v1/users` |
| visits | `/api/v1/visits` |
| workflow | (no REST — scheduled tasks) |

### Common Enum Values (18 shared enums)

| Enum | Values |
|---|---|
| TransferType | INTER_FAMILLE, INTER_FAISEUR, EXTERNE |
| TransferStatus | EN_ATTENTE, EN_COURS, VALIDE, REJETUE, ANNULEE |
| StatutAlerte | ACTIVE, LUE, TRAITEE, ANNULEE |
| RaisonSuiviParallele | ABSENCE, DIFFICULTE_SPIRITUELLE, PROBLEME_FAMILIAL, MALADIE, AUTRE |
| TypeNotification | ALERTE, RAPPEL, MISE_A_JOUR, SYSTEME |
| MotifSortie | DEMANDE_PERSONNELLE, CHANGEMENT_VILLE, PROBLEMES_FAMILIAUX, AUTRE |
| NiveauRisque | AUCUN, FAIBLE, MOYEN, ELEVE, CRITIQUE |
| StatutAme | EN_INTEGRATION, ACTIF, EN_VEILLE, DECROCHE |
| StatutSuiviParallele | EN_COURS, TERMINE, ABANDONNE |
| TypeDisciple | NOUVEAU, EN_CROISSANCE, MATURE, ANCIEN, PASTEUR |
| RaisonAbsence | MALADIE, VOYAGE, RAISON_PERSONNELLE, AUCUNE |
| ValidationMode | AUTOMATIQUE, MANUEL |
| DecisionType | APPROUVE, REJET |
| PrioriteTransfert | BASSE, MOYENNE, HAUTE, URGENTE |
| StatutValidation | EN_ATTENTE, APPROUVEE, REJETEE |
| CanalNotification | EMAIL, PUSH, SMS, IN_APP |
| StatutEntite | ACTIF, INACTIF, ARCHIVE |
| CategorieDiscipline | MORALE, SPIRITUELLE, FINANCIERE, DISCIPLINAIRE, ACCUEIL |
| GraviteDiscipline | FAIBLE, MOYENNE, FORTE, CRITIQUE |
