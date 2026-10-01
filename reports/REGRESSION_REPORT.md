# Rapport de régression — Discipolat (G6.4)

- Généré le : 2026-09-22 17:12
- Sources : `mvn test` (surefire), `vitest run`, `npx playwright test` (e2e/results.json)

## 1. Backend (JUnit / surefire)
- Total : **1275 tests** — 0 échecs, 0 erreurs, 0 sautés → **✅ VERT**

<details><summary>Détail par classe (143)</summary>

| Classe | Tests | Échecs | Erreurs | Sautés | |
|---|---|---|---|---|---|
| com.discipolat.common.domain.EntityBuilderDefaultsTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.PropagationChainIntegrationTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.PropagationConsistencyTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$ColumnMinimumCounts | 18 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$ColumnVerification | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$EntityToTableMapping | 1 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$ForeignKeys | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$GlobalChecks | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$ModuleCompleteness | 12 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$MultiTenancy | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$NotNullConstraints | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$TableExistence | 34 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest$TableStructureIntegrity | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.SchemaVerificationIntegrationTest | 0 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.TenantAutoFillIntegrationTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.TenantIsolationIntegrationTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.config.JwtTokenProviderTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.config.PerIpRateLimiterIntegrationTest | 13 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.propagation.EntityChangeBroadcasterTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.propagation.EntityChangeSseControllerTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.scheduler.ScheduledJobsTest | 12 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.security.SecurityUtilsTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.common.infrastructure.security.WorkspaceIsolationIntegrationTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.admin.api.BenchmarkControllerTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.ai.domain.AiAssistantServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.aid.EmergencyAidServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.alerts.domain.SmartAlertServiceTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.appointments.domain.AppointmentServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.audit.domain.AuditServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.audit.domain.PermissionServiceTest | 12 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.authentication.domain.AuthServiceTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.authentication.domain.TwoFactorServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.automations.domain.AutomationEngineIntegrationTest | 30 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.automations.domain.AutomationServiceTest | 16 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.badges.domain.BadgeServiceTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.communications.domain.CommunicationServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.core.service.OutboxPublisherStreamRelayTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.core.service.RealTimeServiceFirehoseTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.customfields.api.CustomFieldControllerTest | 12 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.customfields.domain.CustomFieldServiceTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.dashboard.domain.DashboardServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.dataMigration.LegacyMigrationEngineIntegrationTest | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.departments.api.DepartmentControllerTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.departments.domain.DepartmentDossierServiceTest | 16 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.departments.domain.DepartmentManagementServiceTest | 41 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.departments.domain.DepartmentReportingServiceTest | 16 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.departments.domain.DepartmentServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.departments.domain.DepartmentSettingsServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.evaluations.domain.EvaluationServiceTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.evangelism.ConversionScoringServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.events.domain.EventServiceTest | 15 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.facerec.FaceHasherTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.facerec.FaceRecognitionServiceTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.families.api.FamilyControllerTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.families.domain.FamilyRiskServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.families.service.FamilyOSServiceTest | 12 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.families.service.PastorateServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.families.service.PermissionResolverPastorateTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.files.domain.BulkImportServiceTest | 15 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.files.domain.EntityAttachmentServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.files.domain.FileServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.finances.domain.FinanceServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.health.SpiritualHealthServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.imports.domain.SpaceExportImportTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.inventory.domain.AssetQrServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.inventory.domain.InventoryServiceTest | 31 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.kpiNarrative.domain.KpiNarrativeServiceTest | 20 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.members.api.GeofencingControllerTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.members.domain.MemberPresenceSheetTest | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.members.domain.MemberQrPresenceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.mentoring.domain.MentoringServiceTest | 15 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.messages.domain.MessageServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.moderation.domain.ContentModerationServiceTest | 19 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.network.domain.NetworkServiceTest | 21 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.notifications.domain.AccessRequestServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.notifications.domain.NotificationServiceTest | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.notifications.domain.NotificationTemplateServiceTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.objectives.domain.ObjectiveServiceTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.parallelfollowups.domain.ParallelFollowupServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.passport.domain.PassportServiceTest | 15 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.passport.domain.PassportSignatureServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.payments.PaymentGatewayServiceTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.payments.WebhookHmacVerificationTest | 18 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.payments.domain.MobileMoneyProviderRegistryTest | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.payments.domain.PaymentProviderPropertiesTest | 18 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.people.PeopleCriticalPathIntegrationTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.api.BetaAdminControllerTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.api.DictionaryControllerTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.api.FeedbackControllerTest | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.api.PageBuilderControllerTest | 12 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.api.PlatformConfigControllerTest | 20 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.api.SettingsControllerTest | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.api.TenantAdminControllerMemberRoleTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.domain.BetaResetServiceTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.domain.ChurchSettingsServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.domain.ConfigRevisionServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.domain.DictionaryServiceTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.domain.PageBuilderServiceTest | 29 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.domain.PlatformConfigServiceTest | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.platform.infrastructure.ModuleGateFilterTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.programs.domain.ProgramServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.quest.QuestServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.reports.domain.ReportServiceTest | 12 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.sermon.SermonAssistantServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.souls.domain.SoulExitServiceTest | 3 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.souls.domain.SoulServiceTest | 11 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.souls.domain.WorkspaceScopeServiceTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.spaces.domain.SpaceBootstrapServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.spaces.domain.SpaceServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.spiritualChallenges.domain.SpiritualChallengeServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.statuses.domain.CustomStatusServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.sync.domain.SyncBatchServiceTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests$EscalationTests | 2 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests$ResourceIsolationTests | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests$RoleIsolationTests | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests$ScopeIsolationTests | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests$SecurityMatrixTests | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests$SuperAdminTests | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests$TenantIsolationTests | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.MultiTenantSecurityTests | 0 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.domain.ActiveTenantServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.domain.ModuleCatalogServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.domain.ModuleRouterTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.domain.SpaceModuleServiceTest | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tenants.domain.TenantServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.tontine.TontineServiceTest | 5 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.trainings.api.SermonTranscriptionControllerTest | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.trainings.domain.TrainingServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.transfers.domain.TransferBridgeServiceTest | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.transfers.domain.TransferWorkflowServiceTest | 16 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.twin.DigitalTwinServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.users.domain.UserServiceTest | 15 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.voicenotifications.config.WebSocketAuthInterceptorTest | 10 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.voicereports.VoiceReportServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.webhooks.WebhookServiceTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.workflow.api.WorkflowConfigControllerTest | 29 | 0 | 0 | 0 | ✅ |
| com.discipolat.modules.workflow.domain.WorkflowEngineServiceTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.security.RedisCacheConfigTest | 9 | 0 | 0 | 0 | ✅ |
| com.discipolat.security.SecurityHeadersFilterTest | 8 | 0 | 0 | 0 | ✅ |
| com.discipolat.security.SecurityHeadersTest | 7 | 0 | 0 | 0 | ✅ |
| com.discipolat.security.TenantAwareRedisManagerTest | 6 | 0 | 0 | 0 | ✅ |
| com.discipolat.security.TenantFileIsolationConfigTest | 4 | 0 | 0 | 0 | ✅ |
| com.discipolat.security.TenantIsolationIntegrationTest | 10 | 0 | 0 | 0 | ✅ |

</details>

## 2. Frontend (Vitest)
- Fichiers : 53 verts ; Tests : **382 passés** — ✅ 0 échec

## 3. Parcours critiques E2E (Playwright)
- stats : {"startTime": "2026-09-22T16:04:36.169Z", "duration": 115034.732, "expected": 8, "skipped": 0, "unexpected": 0, "flaky": 0}

| Projet | Parcours | |
|---|---|---|
| chromium | CP1 — inscription libre au nom de l’église → succès affiché | ✅ |
| chromium | CP2 — répertoire : fiche créée, « sans espace », affectation → disparaît | ✅ |
| chromium | CP3 — événement + dress code publiés visibles côté UI | ✅ |
| chromium | CP4 — matériel : inventaire API visible dans l’UI, prêt→dommage→maintenance | ✅ |
| chromium | CP5 — workflow : moteur vérifié côté serveur, automatisation réelle visible sur /workflow | ✅ |
| chromium | CP6 — invitation : lien reçu → acceptation UI → compte + membership réels | ✅ |
| chromium | CP7 — finance : écritures + rapprochement → solde visible sur /finances | ✅ |
| chromium | CP8 — rôle modifié → permissions recalculées côté serveur < 5 s | ✅ |

## 4. Mobile (Flutter)
- `flutter test` : voir sortie de la passe de validation (nombre de tests verts, 0 échec exigé pour le GO).

