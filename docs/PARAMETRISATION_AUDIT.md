# Ledger de paramétrisation par église — sites figés et clés de permission cibles

> Généré le 2026-10-16 — colonne vertébrale du rollout « tout paramétrable par église »
> (plan validé le 2026-10-16). Ce fichier est le ledger d'avancement : chaque ligne est un site
> où l'autorisation est codée en DUR par rôle et doit migrer vers une PERMISSION configurable
> (`@authz.can('<CLÉ>', …)` côté backend, `hasPermission`/`canAccess` côté client).
> La règle **R7** d'`ArchitectureRulesTest` gèle le compteur backend (`plafond|R7|0` — abaissé
> de 581 à 570 par la tranche MEMBERS Phase 2, puis à 0 par la Phase 3) et
> rougit dès qu'un nouveau littéral de rôle apparaît ; chaque PR de migration l'abaisse.

## Totaux constatés

| Surface | Sites figés | Détail |
|---|---|---|
| Backend (gardes `@PreAuthorize`) | **0 restants** | 581 sites à l'origine → 11 convertis Phase 2 (MEMBERS) + 570 convertis Phase 3 (547 mécaniques + 23 guidées) ; R7 gelé à 0 |
| Frontend (littéraux de rôle) | **2298** occurrences dans 145 fichiers | **routes + garde SERVIES** ✅ (Phase 4 : `ROUTE_PERMISSIONS` 173 clés + `App.tsx` 185 props `permissions=`, repli `roles` conservé ; V261 113 clés UI) ; **intra-pages servi-first** ✅ (29 gardes `can/canAny`, V262 puis V270) ; **résiduel = 6 sites nommés** (liste blanche + plafond chiffré de `parametrisationGate.test.ts`) ; **ratchet CI** phase 8 : existence des clés servies + parité de matrice + liste blanche |
| Mobile (littéraux de rôle) | **1176** occurrences dans 54 fichiers | **routes + tiroir SERVIES** ✅ (Phase 5 : `routePermissions` 91 clés, **miroir exact du web sur les 79 routes communes — 0 divergence**) ; **intra-écrans servi-first** ✅ (9 `canServed`, V262 puis V270) ; **résiduel = 1 site** (libellé d'une portée calculée par le serveur) ; **ratchet CI** `served_permissions_parity_test.dart` (phase 8) |

Légende statut : ⬜ à migrer · 🟨 migration en cours · ✅ migré (garde littérale supprimée, équivalence prouvée).

### Conventions de clés cibles

- Clé = `<MODULE>_<ACTION>` en MAJUSCULES+underscore, module = paquet `com.discipolat.modules.<module>`.
- ACTION usuels : `READ`, `CREATE`, `UPDATE`, `DELETE`, plus `APPROVE`, `EXPORT`, `SYNC`, `EXECUTE` selon le verbe.
- Site de CLASSE : la garde héritée protège tous les endpoints ; à la migration, la remplacer par des clés
  par endpoint (`<MODULE>_<ACTION>`) — le `*` signale qu'aucune clé unique ne suffit pas.
- Les clés proposées ici sont des **propositions** ; la version canonique est le catalogue Flyway
  (`permissions`) généré en Phase 1. Une migration ne peut pas introduire une clé absente du catalogue.

## A. Backend — tous les sites `@PreAuthorize` à littéral de rôle

| # | Fichier | Ligne | Niveau | Méthode | Garde actuelle | Clé cible proposée | Statut |
|---|---|---|---|---|---|---|---|
| 1 | `backend/src/main/java/com/discipolat/modules/admin/api/AdminIntegrationController.java` | 31 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `ADMIN_*` | ✅ (phase 3) |
| 2 | `backend/src/main/java/com/discipolat/modules/admin/api/AdminStatsController.java` | 21 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `ADMIN_*` | ✅ (phase 3) |
| 3 | `backend/src/main/java/com/discipolat/modules/admin/api/BenchmarkController.java` | 34 | CLASSE | `—` | `"hasAnyRole('PASTEUR', 'ADMIN')"` | `ADMIN_*` | ✅ (phase 3) |
| 4 | `backend/src/main/java/com/discipolat/modules/adminRequests/api/AdminRequestController.java` | 22 | MÉTHODE | `list` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `ADMIN_REQUESTS_READ` | ✅ (phase 3) |
| 5 | `backend/src/main/java/com/discipolat/modules/adminRequests/api/AdminRequestController.java` | 42 | MÉTHODE | `process` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `ADMIN_REQUESTS_EXECUTE` | ✅ (phase 3) |
| 6 | `backend/src/main/java/com/discipolat/modules/adminRequests/api/AdminRequestController.java` | 58 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `ADMIN_REQUESTS_EXECUTE` | ✅ (phase 3) |
| 7 | `backend/src/main/java/com/discipolat/modules/ai/api/AiAssistantController.java` | 84 | MÉTHODE | `analyze` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `AI_EXECUTE` | ✅ (phase 3) |
| 8 | `backend/src/main/java/com/discipolat/modules/ai/api/AiAssistantController.java` | 93 | MÉTHODE | `encouragement` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `AI_EXECUTE` | ✅ (phase 3) |
| 9 | `backend/src/main/java/com/discipolat/modules/ai/api/AiCreditsController.java` | 21 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_ADMIN')"` | `AI_*` | ✅ (phase 3) |
| 10 | `backend/src/main/java/com/discipolat/modules/aid/api/EmergencyAidController.java` | 25 | MÉTHODE | `open` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `AID_EXECUTE` | ✅ (phase 3) |
| 11 | `backend/src/main/java/com/discipolat/modules/aid/api/EmergencyAidController.java` | 31 | MÉTHODE | `recent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `AID_EXECUTE` | ✅ (phase 3) |
| 12 | `backend/src/main/java/com/discipolat/modules/aid/api/EmergencyAidController.java` | 37 | MÉTHODE | `openRequests` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `AID_EXECUTE` | ✅ (phase 3) |
| 13 | `backend/src/main/java/com/discipolat/modules/aid/api/EmergencyAidController.java` | 43 | MÉTHODE | `collect` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `AID_EXECUTE` | ✅ (phase 3) |
| 14 | `backend/src/main/java/com/discipolat/modules/aid/api/EmergencyAidController.java` | 50 | MÉTHODE | `resolve` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `AID_EXECUTE` | ✅ (phase 3) |
| 15 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 30 | MÉTHODE | `findAll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `ALERTS_READ` | ✅ (phase 3) |
| 16 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 46 | MÉTHODE | `findActive` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `ALERTS_READ` | ✅ (phase 3) |
| 17 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 59 | MÉTHODE | `findById` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `ALERTS_READ` | ✅ (phase 3) |
| 18 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 66 | MÉTHODE | `createManual` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `ALERTS_CREATE` | ✅ (phase 3) |
| 19 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 72 | MÉTHODE | `resolve` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `ALERTS_EXECUTE` | ✅ (phase 3) |
| 20 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 78 | MÉTHODE | `acknowledge` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `ALERTS_EXECUTE` | ✅ (phase 3) |
| 21 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 85 | MÉTHODE | `resolveBatch` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `ALERTS_EXECUTE` | ✅ (phase 3) |
| 22 | `backend/src/main/java/com/discipolat/modules/alerts/api/AlertController.java` | 93 | MÉTHODE | `getStats` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `ALERTS_READ` | ✅ (phase 3) |
| 23 | `backend/src/main/java/com/discipolat/modules/alerts/api/SmartAlertController.java` | 12 | CLASSE | `—` | `"hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE')"` | `ALERTS_*` | ✅ (phase 3) |
| 24 | `backend/src/main/java/com/discipolat/modules/announcements/api/AnnouncementController.java` | 35 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `ANNOUNCEMENTS_CREATE` | ✅ (phase 3) |
| 25 | `backend/src/main/java/com/discipolat/modules/announcements/api/AnnouncementController.java` | 41 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `ANNOUNCEMENTS_UPDATE` | ✅ (phase 3) |
| 26 | `backend/src/main/java/com/discipolat/modules/announcements/api/AnnouncementController.java` | 47 | MÉTHODE | `schedule` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `ANNOUNCEMENTS_CREATE` | ✅ (phase 3) |
| 27 | `backend/src/main/java/com/discipolat/modules/announcements/api/AnnouncementController.java` | 54 | MÉTHODE | `publish` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `ANNOUNCEMENTS_CREATE` | ✅ (phase 3) |
| 28 | `backend/src/main/java/com/discipolat/modules/announcements/api/AnnouncementController.java` | 60 | MÉTHODE | `cancel` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `ANNOUNCEMENTS_CREATE` | ✅ (phase 3) |
| 29 | `backend/src/main/java/com/discipolat/modules/announcements/api/AnnouncementController.java` | 67 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `ANNOUNCEMENTS_DELETE` | ✅ (phase 3) |
| 30 | `backend/src/main/java/com/discipolat/modules/appointments/api/AppointmentController.java` | 15 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `APPOINTMENTS_*` | ✅ (phase 3) |
| 31 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 30 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR')"` | `AUDIT_*` | ✅ (phase 3) |
| 32 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 44 | MÉTHODE | `findAll` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 33 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 64 | MÉTHODE | `exportCsv` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_EXPORT` | ✅ (phase 3) |
| 34 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 82 | MÉTHODE | `findById` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 35 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 89 | MÉTHODE | `getRecentActivity` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 36 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 97 | MÉTHODE | `getAuditTrend` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 37 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 109 | MÉTHODE | `findEvents` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 38 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 133 | MÉTHODE | `exportEventsCsv` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_EXPORT` | ✅ (phase 3) |
| 39 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 155 | MÉTHODE | `verifyChain` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_EXECUTE` | ✅ (phase 3) |
| 40 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 166 | MÉTHODE | `getObjectHistory` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 41 | `backend/src/main/java/com/discipolat/modules/audit/api/AuditController.java` | 178 | MÉTHODE | `getSpaceHistory` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 42 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 14 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_*` | ✅ (phase 3) |
| 43 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 26 | MÉTHODE | `getAll` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 44 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 32 | MÉTHODE | `getByRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_READ` | ✅ (phase 3) |
| 45 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 38 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_UPDATE` | ✅ (phase 3) |
| 46 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 57 | MÉTHODE | `updateRWD` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_UPDATE` | ✅ (phase 3) |
| 47 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 72 | MÉTHODE | `catalog` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_EXECUTE` | ✅ (phase 3) |
| 48 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 80 | MÉTHODE | `roles` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_EXECUTE` | ✅ (phase 3) |
| 49 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 86 | MÉTHODE | `createRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_CREATE` | ✅ (phase 3) |
| 50 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 99 | MÉTHODE | `updateRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_UPDATE` | ✅ (phase 3) |
| 51 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 107 | MÉTHODE | `duplicateRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_EXECUTE` | ✅ (phase 3) |
| 52 | `backend/src/main/java/com/discipolat/modules/audit/api/PermissionController.java` | 120 | MÉTHODE | `deleteRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUDIT_DELETE` | ✅ (phase 3) |
| 53 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 31 | MÉTHODE | `listRules` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_READ` | ✅ (phase 3) |
| 54 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 41 | MÉTHODE | `listActive` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_READ` | ✅ (phase 3) |
| 55 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 47 | MÉTHODE | `getRule` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_READ` | ✅ (phase 3) |
| 56 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 54 | MÉTHODE | `createRule` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_CREATE` | ✅ (phase 3) |
| 57 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 65 | MÉTHODE | `updateRule` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_UPDATE` | ✅ (phase 3) |
| 58 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 71 | MÉTHODE | `toggleRule` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_EXECUTE` | ✅ (phase 3) |
| 59 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 77 | MÉTHODE | `deleteRule` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_DELETE` | ✅ (phase 3) |
| 60 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 86 | MÉTHODE | `trigger` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_EXECUTE` | ✅ (phase 3) |
| 61 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 98 | MÉTHODE | `listExecutions` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_READ` | ✅ (phase 3) |
| 62 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 106 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_READ` | ✅ (phase 3) |
| 63 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 114 | MÉTHODE | `listTriggers` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_READ` | ✅ (phase 3) |
| 64 | `backend/src/main/java/com/discipolat/modules/automations/api/AutomationController.java` | 137 | MÉTHODE | `listActions` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `AUTOMATIONS_READ` | ✅ (phase 3) |
| 65 | `backend/src/main/java/com/discipolat/modules/badges/api/BadgeController.java` | 15 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `BADGES_*` | ✅ (phase 3) |
| 66 | `backend/src/main/java/com/discipolat/modules/calendar/api/CalendarController.java` | 26 | MÉTHODE | `list` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `CALENDAR_READ` | ✅ (phase 3) |
| 67 | `backend/src/main/java/com/discipolat/modules/calendar/api/CalendarController.java` | 37 | MÉTHODE | `get` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `CALENDAR_READ` | ✅ (phase 3) |
| 68 | `backend/src/main/java/com/discipolat/modules/calendar/api/CalendarController.java` | 44 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `CALENDAR_CREATE` | ✅ (phase 3) |
| 69 | `backend/src/main/java/com/discipolat/modules/calendar/api/CalendarController.java` | 59 | MÉTHODE | `updateStatus` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `CALENDAR_UPDATE` | ✅ (phase 3) |
| 70 | `backend/src/main/java/com/discipolat/modules/calendar/api/CalendarController.java` | 64 | MÉTHODE | `getICal` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `CALENDAR_READ` | ✅ (phase 3) |
| 71 | `backend/src/main/java/com/discipolat/modules/calendar/api/CalendarController.java` | 77 | MÉTHODE | `getICalFeed` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `CALENDAR_READ` | ✅ (phase 3) |
| 72 | `backend/src/main/java/com/discipolat/modules/calendar/api/CalendarController.java` | 87 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `CALENDAR_DELETE` | ✅ (phase 3) |
| 73 | `backend/src/main/java/com/discipolat/modules/cercle/api/CercleFaiseursController.java` | 19 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR','FAISEUR')"` | `CERCLE_*` | ✅ (phase 3) |
| 74 | `backend/src/main/java/com/discipolat/modules/churchComparison/api/ChurchComparisonController.java` | 16 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `CHURCH_COMPARISON_*` | ✅ (phase 3) |
| 75 | `backend/src/main/java/com/discipolat/common/infrastructure/propagation/EntityChangeSseController.java` | 108 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN')"` | `COMMON_EXECUTE` | ✅ (phase 3) |
| 76 | `backend/src/main/java/com/discipolat/modules/communications/api/CommunicationController.java` | 37 | MÉTHODE | `listAll` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMMUNICATIONS_READ` | ✅ (phase 3) |
| 77 | `backend/src/main/java/com/discipolat/modules/communications/api/CommunicationController.java` | 43 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMMUNICATIONS_CREATE` | ✅ (phase 3) |
| 78 | `backend/src/main/java/com/discipolat/modules/communications/api/CommunicationController.java` | 50 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMMUNICATIONS_UPDATE` | ✅ (phase 3) |
| 79 | `backend/src/main/java/com/discipolat/modules/communications/api/CommunicationController.java` | 57 | MÉTHODE | `publish` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMMUNICATIONS_EXECUTE` | ✅ (phase 3) |
| 80 | `backend/src/main/java/com/discipolat/modules/communications/api/CommunicationController.java` | 63 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMMUNICATIONS_DELETE` | ✅ (phase 3) |
| 81 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 33 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_EXECUTE` | ✅ (phase 3) |
| 82 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 41 | MÉTHODE | `retentionPolicies` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_EXECUTE` | ✅ (phase 3) |
| 83 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 50 | MÉTHODE | `setRetentionPolicy` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_EXECUTE` | ✅ (phase 3) |
| 84 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 57 | MÉTHODE | `purgeAll` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_EXECUTE` | ✅ (phase 3) |
| 85 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 65 | MÉTHODE | `exports` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_EXPORT` | ✅ (phase 3) |
| 86 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 73 | MÉTHODE | `createExport` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_CREATE` | ✅ (phase 3) |
| 87 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 81 | MÉTHODE | `verifyAuditHash` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_EXECUTE` | ✅ (phase 3) |
| 88 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 87 | MÉTHODE | `listRequests` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_READ` | ✅ (phase 3) |
| 89 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 97 | MÉTHODE | `getRequest` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_READ` | ✅ (phase 3) |
| 90 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 113 | MÉTHODE | `processRequest` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_EXECUTE` | ✅ (phase 3) |
| 91 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 156 | MÉTHODE | `executeRetentionPolicy` | `"hasAnyRole('ADMIN','PASTEUR')"` | `COMPLIANCE_EXECUTE` | ✅ (phase 3) |
| 92 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 165 | MÉTHODE | `createRetentionPolicy` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_CREATE` | ✅ (phase 3) |
| 93 | `backend/src/main/java/com/discipolat/modules/compliance/api/ComplianceController.java` | 174 | MÉTHODE | `deleteRetentionPolicy` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `COMPLIANCE_DELETE` | ✅ (phase 3) |
| 94 | `backend/src/main/java/com/discipolat/modules/config/api/SpaceTemplateController.java` | 28 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `CONFIG_*` | ✅ (phase 3) |
| 95 | `backend/src/main/java/com/discipolat/modules/config/api/SpaceTemplateController.java` | 66 | MÉTHODE | `createTemplate` | `"hasRole('ADMIN')"` | `CONFIG_CREATE` | ✅ (phase 3) |
| 96 | `backend/src/main/java/com/discipolat/modules/config/api/SpaceTemplateController.java` | 72 | MÉTHODE | `updateTemplate` | `"hasRole('ADMIN')"` | `CONFIG_UPDATE` | ✅ (phase 3) |
| 97 | `backend/src/main/java/com/discipolat/modules/config/api/SpaceTemplateController.java` | 78 | MÉTHODE | `deleteTemplate` | `"hasRole('ADMIN')"` | `CONFIG_DELETE` | ✅ (phase 3) |
| 98 | `backend/src/main/java/com/discipolat/modules/config/api/SpaceTemplateController.java` | 88 | MÉTHODE | `createSpaceFromTemplate` | `"hasAnyRole('ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER', 'FAMILY_LEADER')"` | `CONFIG_CREATE` | ✅ (phase 3) |
| 99 | `backend/src/main/java/com/discipolat/modules/customfields/api/CustomFieldController.java` | 36 | MÉTHODE | `getAllDefinitions` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `CUSTOMFIELDS_READ` | ✅ (phase 3) |
| 100 | `backend/src/main/java/com/discipolat/modules/customfields/api/CustomFieldController.java` | 43 | MÉTHODE | `createDefinition` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `CUSTOMFIELDS_CREATE` | ✅ (phase 3) |
| 101 | `backend/src/main/java/com/discipolat/modules/customfields/api/CustomFieldController.java` | 62 | MÉTHODE | `updateDefinition` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `CUSTOMFIELDS_UPDATE` | ✅ (phase 3) |
| 102 | `backend/src/main/java/com/discipolat/modules/customfields/api/CustomFieldController.java` | 81 | MÉTHODE | `deleteDefinition` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `CUSTOMFIELDS_DELETE` | ✅ (phase 3) |
| 103 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 24 | MÉTHODE | `getKPI` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 104 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 34 | MÉTHODE | `getPresenceTrend` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 105 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 41 | MÉTHODE | `getFamilyRisk` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 106 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 48 | MÉTHODE | `getReportCompletion` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 107 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 54 | MÉTHODE | `getSummary` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 108 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 60 | MÉTHODE | `getMyMetrics` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 109 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 68 | MÉTHODE | `getPasteurDashboard` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 110 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 74 | MÉTHODE | `getChefFamilleDashboard` | `"hasAnyRole('CHEF_DE_FAMILLE', 'PASTEUR', 'ADMIN')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 111 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 81 | MÉTHODE | `getResponsableDashboard` | `"hasAnyRole('RESPONSABLE', 'PASTEUR', 'ADMIN')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 112 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 90 | MÉTHODE | `getCrmFaiseur` | `"hasAnyRole('FAISEUR', 'PASTEUR', 'ADMIN')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 113 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 98 | MÉTHODE | `getPasteurPresenceTrend` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 114 | `backend/src/main/java/com/discipolat/modules/dashboard/api/DashboardController.java` | 105 | MÉTHODE | `getPasteurKpis` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DASHBOARD_READ` | ✅ (phase 3) |
| 115 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/DataMigrationController.java` | 25 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR')"` | `DATA_MIGRATION_*` | ✅ (phase 3) |
| 116 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/LegacyMigrationController.java` | 41 | MÉTHODE | `maps` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `DATA_MIGRATION_EXECUTE` | ✅ (phase 3) |
| 117 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/LegacyMigrationController.java` | 48 | MÉTHODE | `dryRun` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `DATA_MIGRATION_EXECUTE` | ✅ (phase 3) |
| 118 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/LegacyMigrationController.java` | 55 | MÉTHODE | `migrate` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `DATA_MIGRATION_EXECUTE` | ✅ (phase 3) |
| 119 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/LegacyMigrationController.java` | 61 | MÉTHODE | `jobs` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `DATA_MIGRATION_EXECUTE` | ✅ (phase 3) |
| 120 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/LegacyMigrationController.java` | 67 | MÉTHODE | `job` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `DATA_MIGRATION_EXECUTE` | ✅ (phase 3) |
| 121 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/LegacyMigrationController.java` | 74 | MÉTHODE | `audit` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `DATA_MIGRATION_EXECUTE` | ✅ (phase 3) |
| 122 | `backend/src/main/java/com/discipolat/modules/dataMigration/api/LegacyMigrationController.java` | 81 | MÉTHODE | `rollback` | `"hasAnyRole('ADMIN', 'PASTEUR', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `DATA_MIGRATION_EXECUTE` | ✅ (phase 3) |
| 123 | `backend/src/main/java/com/discipolat/modules/demo/api/DemoRequestController.java` | 57 | MÉTHODE | `list` | `"hasAnyRole('ADMIN','PASTEUR')"` | `DEMO_READ` | ✅ (phase 3) |
| 124 | `backend/src/main/java/com/discipolat/modules/departmentKpi/api/DepartmentKpiController.java` | 16 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DEPARTMENT_KPI_*` | ✅ (phase 3) |
| 125 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentController.java` | 22 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DEPARTMENTS_*` | ✅ (phase 3) |
| 126 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentController.java` | 58 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DEPARTMENTS_CREATE` | ✅ (phase 3) |
| 127 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentController.java` | 65 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DEPARTMENTS_UPDATE` | ✅ (phase 3) |
| 128 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentController.java` | 77 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DEPARTMENTS_DELETE` | ✅ (phase 3) |
| 129 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentEventAttendanceController.java` | 32 | MÉTHODE | `eventAttendance` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DEPARTMENTS_EXECUTE` | ✅ (phase 3) |
| 130 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentEventAttendanceController.java` | 40 | MÉTHODE | `markEventAttendance` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `DEPARTMENTS_EXECUTE` | ✅ (phase 3) |
| 131 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentEventAttendanceController.java` | 50 | MÉTHODE | `markAllAttendance` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DEPARTMENTS_EXECUTE` | ✅ (phase 3) |
| 132 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentEventAttendanceController.java` | 59 | MÉTHODE | `exportAttendance` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DEPARTMENTS_EXPORT` | ✅ (phase 3) |
| 133 | `backend/src/main/java/com/discipolat/modules/departments/api/DepartmentManagementController.java` | 29 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DEPARTMENTS_*` | ✅ (phase 3) |
| 134 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 48 | MÉTHODE | `createJourney` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_CREATE` | ✅ (phase 3) |
| 135 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 71 | MÉTHODE | `createStage` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_CREATE` | ✅ (phase 3) |
| 136 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 99 | MÉTHODE | `createProgress` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_CREATE` | ✅ (phase 3) |
| 137 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 106 | MÉTHODE | `updateRequirement` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_UPDATE` | ✅ (phase 3) |
| 138 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 115 | MÉTHODE | `completeStage` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_EXECUTE` | ✅ (phase 3) |
| 139 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 138 | MÉTHODE | `createAssignment` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_CREATE` | ✅ (phase 3) |
| 140 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 145 | MÉTHODE | `endAssignment` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_EXECUTE` | ✅ (phase 3) |
| 141 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 166 | MÉTHODE | `scheduleMeeting` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_EXECUTE` | ✅ (phase 3) |
| 142 | `backend/src/main/java/com/discipolat/modules/discipleship/api/DiscipleshipController.java` | 173 | MÉTHODE | `completeMeeting` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `DISCIPLESHIP_EXECUTE` | ✅ (phase 3) |
| 143 | `backend/src/main/java/com/discipolat/modules/discipline/api/SoulDisciplineEventController.java` | 41 | MÉTHODE | `create` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `DISCIPLINE_CREATE` | ✅ (phase 3) |
| 144 | `backend/src/main/java/com/discipolat/modules/discipline/api/SoulDisciplineEventController.java` | 53 | MÉTHODE | `findAll` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `DISCIPLINE_READ` | ✅ (phase 3) |
| 145 | `backend/src/main/java/com/discipolat/modules/discipline/api/SoulDisciplineEventController.java` | 74 | MÉTHODE | `stats` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `DISCIPLINE_EXECUTE` | ✅ (phase 3) |
| 146 | `backend/src/main/java/com/discipolat/modules/discipline/api/SoulDisciplineEventController.java` | 80 | MÉTHODE | `findById` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `DISCIPLINE_READ` | ✅ (phase 3) |
| 147 | `backend/src/main/java/com/discipolat/modules/discipline/api/SoulDisciplineEventController.java` | 86 | MÉTHODE | `resolve` | `"hasAnyRole('PASTEUR', 'RESPONSABLE')"` | `DISCIPLINE_EXECUTE` | ✅ (phase 3) |
| 148 | `backend/src/main/java/com/discipolat/modules/discipline/api/SoulDisciplineEventController.java` | 95 | MÉTHODE | `delete` | `"hasAnyRole('PASTEUR', 'RESPONSABLE')"` | `DISCIPLINE_DELETE` | ✅ (phase 3) |
| 149 | `backend/src/main/java/com/discipolat/modules/dresscode/api/DressCodeController.java` | 49 | MÉTHODE | `create` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DRESSCODE_CREATE` | ✅ (phase 3) |
| 150 | `backend/src/main/java/com/discipolat/modules/dresscode/api/DressCodeController.java` | 77 | MÉTHODE | `update` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DRESSCODE_UPDATE` | ✅ (phase 3) |
| 151 | `backend/src/main/java/com/discipolat/modules/dresscode/api/DressCodeController.java` | 104 | MÉTHODE | `archive` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `DRESSCODE_EXECUTE` | ✅ (phase 3) |
| 152 | `backend/src/main/java/com/discipolat/modules/dresscode/api/DressCodeController.java` | 112 | MÉTHODE | `delete` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `DRESSCODE_DELETE` | ✅ (phase 3) |
| 153 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 36 | MÉTHODE | `getAll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_READ` | ✅ (phase 3) |
| 154 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 56 | MÉTHODE | `submit` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_EXECUTE` | ✅ (phase 3) |
| 155 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 73 | MÉTHODE | `submitOrUpdate` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_EXECUTE` | ✅ (phase 3) |
| 156 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 92 | MÉTHODE | `getMyEvaluationsFor` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_READ` | ✅ (phase 3) |
| 157 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 101 | MÉTHODE | `getMyEvaluations` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_READ` | ✅ (phase 3) |
| 158 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 110 | MÉTHODE | `getMyEvaluationsList` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_READ` | ✅ (phase 3) |
| 159 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 134 | MÉTHODE | `getPeopleToEvaluate` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_READ` | ✅ (phase 3) |
| 160 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 144 | MÉTHODE | `getEvaluationsForUser` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVALUATIONS_READ` | ✅ (phase 3) |
| 161 | `backend/src/main/java/com/discipolat/modules/evaluations/api/EvaluationController.java` | 153 | MÉTHODE | `getAllEvaluations` | `"hasRole('PASTEUR')"` | `EVALUATIONS_READ` | ✅ (phase 3) |
| 162 | `backend/src/main/java/com/discipolat/modules/evangelism/api/EvangelismController.java` | 17 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVANGELISM_*` | ✅ (phase 3) |
| 163 | `backend/src/main/java/com/discipolat/modules/evangelism/api/EvangelismScoringController.java` | 29 | MÉTHODE | `score` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVANGELISM_EXECUTE` | ✅ (phase 3) |
| 164 | `backend/src/main/java/com/discipolat/modules/evangelism/api/EvangelismScoringController.java` | 36 | MÉTHODE | `allScores` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVANGELISM_EXECUTE` | ✅ (phase 3) |
| 165 | `backend/src/main/java/com/discipolat/modules/events/api/ChurchEventController.java` | 25 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'DEPARTMENT_LEADER', 'FAMILY_LEADER')"` | `EVENTS_*` | ✅ (phase 3) |
| 166 | `backend/src/main/java/com/discipolat/modules/events/api/ChurchEventController.java` | 67 | MÉTHODE | `createEvent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER')"` | `EVENTS_CREATE` | ✅ (phase 3) |
| 167 | `backend/src/main/java/com/discipolat/modules/events/api/ChurchEventController.java` | 76 | MÉTHODE | `updateEvent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'DEPARTMENT_LEADER')"` | `EVENTS_UPDATE` | ✅ (phase 3) |
| 168 | `backend/src/main/java/com/discipolat/modules/events/api/ChurchEventController.java` | 85 | MÉTHODE | `deleteEvent` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `EVENTS_DELETE` | ✅ (phase 3) |
| 169 | `backend/src/main/java/com/discipolat/modules/events/api/ChurchEventController.java` | 97 | MÉTHODE | `archiveEvent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'DEPARTMENT_LEADER')"` | `EVENTS_EXECUTE` | ✅ (phase 3) |
| 170 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 29 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')"` | `EVENTS_*` | ✅ (phase 3) |
| 171 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 51 | MÉTHODE | `create` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_CREATE` | ✅ (phase 3) |
| 172 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 82 | MÉTHODE | `findById` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 173 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 89 | MÉTHODE | `findByDepartmentId` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 174 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 104 | MÉTHODE | `findAll` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 175 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 133 | MÉTHODE | `update` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_UPDATE` | ✅ (phase 3) |
| 176 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 167 | MÉTHODE | `delete` | `"hasAnyRole('PASTEUR', 'RESPONSABLE')"` | `EVENTS_DELETE` | ✅ (phase 3) |
| 177 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 174 | MÉTHODE | `register` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_CREATE` | ✅ (phase 3) |
| 178 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 182 | MÉTHODE | `unregister` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_EXECUTE` | ✅ (phase 3) |
| 179 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 189 | MÉTHODE | `markAttendance` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_EXECUTE` | ✅ (phase 3) |
| 180 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 213 | MÉTHODE | `checkin` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `EVENTS_EXECUTE` | ✅ (phase 3) |
| 181 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 271 | MÉTHODE | `getRegistrations` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 182 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 301 | MÉTHODE | `getTemplates` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 183 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 307 | MÉTHODE | `createTemplate` | `"hasRole('PASTEUR')"` | `EVENTS_CREATE` | ✅ (phase 3) |
| 184 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 313 | MÉTHODE | `updateTemplate` | `"hasRole('PASTEUR')"` | `EVENTS_UPDATE` | ✅ (phase 3) |
| 185 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 320 | MÉTHODE | `deleteTemplate` | `"hasRole('PASTEUR')"` | `EVENTS_DELETE` | ✅ (phase 3) |
| 186 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 327 | MÉTHODE | `toggleTemplate` | `"hasRole('PASTEUR')"` | `EVENTS_EXECUTE` | ✅ (phase 3) |
| 187 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 337 | MÉTHODE | `generateWeekProgram` | `"hasRole('PASTEUR')"` | `EVENTS_EXECUTE` | ✅ (phase 3) |
| 188 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 350 | MÉTHODE | `generateMonthProgram` | `"hasRole('PASTEUR')"` | `EVENTS_EXECUTE` | ✅ (phase 3) |
| 189 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 360 | MÉTHODE | `getWeekProgram` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 190 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 372 | MÉTHODE | `getEventStatistics` | `"hasRole('PASTEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 191 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 386 | MÉTHODE | `getConsolidatedUpcoming` | `"hasRole('PASTEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 192 | `backend/src/main/java/com/discipolat/modules/events/api/EventController.java` | 396 | MÉTHODE | `getConsolidatedByFamily` | `"hasRole('PASTEUR')"` | `EVENTS_READ` | ✅ (phase 3) |
| 193 | `backend/src/main/java/com/discipolat/modules/executiveInsights/api/ExecutiveInsightsController.java` | 12 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `EXECUTIVE_INSIGHTS_*` | ✅ (phase 3) |
| 194 | `backend/src/main/java/com/discipolat/modules/exports/api/SpaceExportController.java` | 66 | MÉTHODE | `exportTenant` | `"@authz.isPlatformSuperAdmin() or hasAnyRole('ADMIN','PASTEUR')"` | `EXPORTS_EXPORT` | ✅ (phase 3) |
| 195 | `backend/src/main/java/com/discipolat/modules/facerec/api/FaceRecognitionController.java` | 32 | MÉTHODE | `enroll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FACEREC_EXECUTE` | ✅ (phase 3) |
| 196 | `backend/src/main/java/com/discipolat/modules/facerec/api/FaceRecognitionController.java` | 43 | MÉTHODE | `identify` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `FACEREC_EXECUTE` | ✅ (phase 3) |
| 197 | `backend/src/main/java/com/discipolat/modules/facerec/api/FaceRecognitionController.java` | 63 | MÉTHODE | `templates` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FACEREC_EXECUTE` | ✅ (phase 3) |
| 198 | `backend/src/main/java/com/discipolat/modules/facerec/api/FaceRecognitionController.java` | 71 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FACEREC_EXECUTE` | ✅ (phase 3) |
| 199 | `backend/src/main/java/com/discipolat/modules/facerec/api/FaceRecognitionController.java` | 78 | MÉTHODE | `deactivate` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `FACEREC_EXECUTE` | ✅ (phase 3) |
| 200 | `backend/src/main/java/com/discipolat/modules/facerec/api/FaceRecognitionController.java` | 86 | MÉTHODE | `enrollBatch` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FACEREC_EXECUTE` | ✅ (phase 3) |
| 201 | `backend/src/main/java/com/discipolat/modules/facerec/api/FaceRecognitionController.java` | 106 | MÉTHODE | `identifyConfigurable` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `FACEREC_EXECUTE` | ✅ (phase 3) |
| 202 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 35 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FAMILIES_*` | ✅ (phase 3) |
| 203 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 98 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `FAMILIES_CREATE` | ✅ (phase 3) |
| 204 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 105 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `FAMILIES_UPDATE` | ✅ (phase 3) |
| 205 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 112 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR') && @perm.has('FAMILY','DELETE')"` | `FAMILIES_DELETE` | ✅ (phase 3) |
| 206 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 124 | MÉTHODE | `reassignChief` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `FAMILIES_EXECUTE` | ✅ (phase 3) |
| 207 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 138 | MÉTHODE | `getFamilyTree` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 208 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 146 | MÉTHODE | `getFaiseurPerformance` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 209 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 157 | MÉTHODE | `getFamilyHistory` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 210 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 165 | MÉTHODE | `compareFamilies` | `"hasRole('PASTEUR')"` | `FAMILIES_EXECUTE` | ✅ (phase 3) |
| 211 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 179 | MÉTHODE | `getChiefHistory` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 212 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 202 | MÉTHODE | `restore` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `FAMILIES_EXECUTE` | ✅ (phase 3) |
| 213 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 211 | MÉTHODE | `getRiskAssessment` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 214 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 218 | MÉTHODE | `setNiveauRisque` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `FAMILIES_EXECUTE` | ✅ (phase 3) |
| 215 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyController.java` | 228 | MÉTHODE | `getRiskHistory` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 216 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyMeetingController.java` | 22 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')"` | `FAMILIES_*` | ✅ (phase 3) |
| 217 | `backend/src/main/java/com/discipolat/modules/families/api/FamilyOSController.java` | 25 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FAMILIES_*` | ✅ (phase 3) |
| 218 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 42 | MÉTHODE | `getAppointments` | `"hasAnyRole('PASTOR_PRINCIPAL', 'PASTEUR', 'ADMIN')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 219 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 51 | MÉTHODE | `getAppointment` | `"hasAnyRole('PASTOR_PRINCIPAL', 'PASTEUR', 'ADMIN')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 220 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 58 | MÉTHODE | `createAppointment` | `"hasAnyRole('PASTOR_PRINCIPAL', 'ADMIN')"` | `FAMILIES_CREATE` | ✅ (phase 3) |
| 221 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 66 | MÉTHODE | `updateAppointment` | `"hasAnyRole('PASTOR_PRINCIPAL', 'ADMIN')"` | `FAMILIES_UPDATE` | ✅ (phase 3) |
| 222 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 74 | MÉTHODE | `endAppointment` | `"hasAnyRole('PASTOR_PRINCIPAL', 'ADMIN')"` | `FAMILIES_EXECUTE` | ✅ (phase 3) |
| 223 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 85 | MÉTHODE | `getTransfers` | `"hasAnyRole('PASTOR_PRINCIPAL', 'PASTEUR', 'ADMIN')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 224 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 93 | MÉTHODE | `createTransfer` | `"hasAnyRole('PASTOR_PRINCIPAL', 'ADMIN')"` | `FAMILIES_CREATE` | ✅ (phase 3) |
| 225 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 104 | MÉTHODE | `approveTransfer` | `"hasAnyRole('PASTOR_PRINCIPAL', 'ADMIN')"` | `FAMILIES_APPROVE` | ✅ (phase 3) |
| 226 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 112 | MÉTHODE | `rejectTransfer` | `"hasAnyRole('PASTOR_PRINCIPAL', 'ADMIN')"` | `FAMILIES_EXECUTE` | ✅ (phase 3) |
| 227 | `backend/src/main/java/com/discipolat/modules/families/api/PastorateController.java` | 124 | MÉTHODE | `getPastorHistory` | `"hasAnyRole('PASTOR_PRINCIPAL', 'PASTEUR', 'ADMIN')"` | `FAMILIES_READ` | ✅ (phase 3) |
| 228 | `backend/src/main/java/com/discipolat/modules/favorites/api/FavoriteController.java` | 16 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FAVORITES_*` | ✅ (phase 3) |
| 229 | `backend/src/main/java/com/discipolat/modules/files/api/BulkImportController.java` | 34 | MÉTHODE | `validateFamilies` | `"hasAnyRole('PASTEUR', 'ADMIN')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 230 | `backend/src/main/java/com/discipolat/modules/files/api/BulkImportController.java` | 43 | MÉTHODE | `validateUsers` | `"hasAnyRole('PASTEUR', 'ADMIN')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 231 | `backend/src/main/java/com/discipolat/modules/files/api/BulkImportController.java` | 52 | MÉTHODE | `validateSouls` | `"hasAnyRole('PASTEUR', 'ADMIN')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 232 | `backend/src/main/java/com/discipolat/modules/files/api/BulkImportController.java` | 63 | MÉTHODE | `importFamilies` | `"hasAnyRole('PASTEUR', 'ADMIN')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 233 | `backend/src/main/java/com/discipolat/modules/files/api/BulkImportController.java` | 80 | MÉTHODE | `importUsers` | `"hasAnyRole('PASTEUR', 'ADMIN')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 234 | `backend/src/main/java/com/discipolat/modules/files/api/BulkImportController.java` | 97 | MÉTHODE | `importSouls` | `"hasAnyRole('PASTEUR', 'ADMIN')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 235 | `backend/src/main/java/com/discipolat/modules/files/api/FileController.java` | 40 | MÉTHODE | `upload` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 236 | `backend/src/main/java/com/discipolat/modules/files/api/FileController.java` | 57 | MÉTHODE | `findById` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FILES_READ` | ✅ (phase 3) |
| 237 | `backend/src/main/java/com/discipolat/modules/files/api/FileController.java` | 63 | MÉTHODE | `download` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FILES_EXECUTE` | ✅ (phase 3) |
| 238 | `backend/src/main/java/com/discipolat/modules/files/api/FileController.java` | 155 | MÉTHODE | `findAll` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FILES_READ` | ✅ (phase 3) |
| 239 | `backend/src/main/java/com/discipolat/modules/files/api/FileController.java` | 181 | MÉTHODE | `update` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FILES_UPDATE` | ✅ (phase 3) |
| 240 | `backend/src/main/java/com/discipolat/modules/files/api/FileController.java` | 193 | MÉTHODE | `delete` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `FILES_DELETE` | ✅ (phase 3) |
| 241 | `backend/src/main/java/com/discipolat/modules/finances/api/FinanceController.java` | 24 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `FINANCES_*` | ✅ (phase 3) |
| 242 | `backend/src/main/java/com/discipolat/modules/finances/api/FinanceController.java` | 106 | MÉTHODE | `importStatement` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FINANCES_EXECUTE` | ✅ (phase 3) |
| 243 | `backend/src/main/java/com/discipolat/modules/finances/api/FinanceController.java` | 116 | MÉTHODE | `autoMatch` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FINANCES_EXECUTE` | ✅ (phase 3) |
| 244 | `backend/src/main/java/com/discipolat/modules/finances/api/FinanceController.java` | 124 | MÉTHODE | `unmatched` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FINANCES_EXECUTE` | ✅ (phase 3) |
| 245 | `backend/src/main/java/com/discipolat/modules/finances/api/FinanceController.java` | 132 | MÉTHODE | `manualMatch` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FINANCES_EXECUTE` | ✅ (phase 3) |
| 246 | `backend/src/main/java/com/discipolat/modules/finances/api/FinanceController.java` | 142 | MÉTHODE | `ledger` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FINANCES_EXECUTE` | ✅ (phase 3) |
| 247 | `backend/src/main/java/com/discipolat/modules/followUpRequests/api/FollowUpRequestController.java` | 53 | MÉTHODE | `pending` | `"hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')"` | `FOLLOW_UP_REQUESTS_EXECUTE` | ✅ (phase 3) |
| 248 | `backend/src/main/java/com/discipolat/modules/followUpRequests/api/FollowUpRequestController.java` | 59 | MÉTHODE | `assign` | `"hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')"` | `FOLLOW_UP_REQUESTS_EXECUTE` | ✅ (phase 3) |
| 249 | `backend/src/main/java/com/discipolat/modules/followUpRequests/api/FollowUpRequestController.java` | 69 | MÉTHODE | `updateStatus` | `"hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')"` | `FOLLOW_UP_REQUESTS_UPDATE` | ✅ (phase 3) |
| 250 | `backend/src/main/java/com/discipolat/modules/followUpRequests/api/FollowUpRequestController.java` | 75 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN','PASTEUR','CHEF_DE_FAMILLE')"` | `FOLLOW_UP_REQUESTS_EXECUTE` | ✅ (phase 3) |
| 251 | `backend/src/main/java/com/discipolat/modules/forms/api/FormController.java` | 17 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `FORMS_*` | ✅ (phase 3) |
| 252 | `backend/src/main/java/com/discipolat/modules/gantt/api/TeamTaskController.java` | 27 | MÉTHODE | `list` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `GANTT_READ` | ✅ (phase 3) |
| 253 | `backend/src/main/java/com/discipolat/modules/gantt/api/TeamTaskController.java` | 38 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `GANTT_EXECUTE` | ✅ (phase 3) |
| 254 | `backend/src/main/java/com/discipolat/modules/gantt/api/TeamTaskController.java` | 44 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `GANTT_CREATE` | ✅ (phase 3) |
| 255 | `backend/src/main/java/com/discipolat/modules/gantt/api/TeamTaskController.java` | 61 | MÉTHODE | `updateStatus` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `GANTT_UPDATE` | ✅ (phase 3) |
| 256 | `backend/src/main/java/com/discipolat/modules/gantt/api/TeamTaskController.java` | 67 | MÉTHODE | `updateProgression` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `GANTT_UPDATE` | ✅ (phase 3) |
| 257 | `backend/src/main/java/com/discipolat/modules/gdpr/api/GdprController.java` | 16 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR')"` | `GDPR_*` | ✅ (phase 3) |
| 258 | `backend/src/main/java/com/discipolat/modules/health/api/HealthController.java` | 24 | CLASSE | `—` | `"hasAnyRole('HEALTH_STAFF', 'HEALTH_LEAD', 'ADMIN', 'PASTEUR')"` | `HEALTH_*` | ✅ (phase 3) |
| 259 | `backend/src/main/java/com/discipolat/modules/health/api/HealthObservatoryController.java` | 24 | MÉTHODE | `observatory` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `HEALTH_EXECUTE` | ✅ (phase 3) |
| 260 | `backend/src/main/java/com/discipolat/modules/health/api/HealthObservatoryController.java` | 31 | MÉTHODE | `trend` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `HEALTH_EXECUTE` | ✅ (phase 3) |
| 261 | `backend/src/main/java/com/discipolat/modules/health/api/HealthObservatoryController.java` | 38 | MÉTHODE | `departmentScores` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `HEALTH_EXECUTE` | ✅ (phase 3) |
| 262 | `backend/src/main/java/com/discipolat/modules/imports/api/ImportController.java` | 25 | MÉTHODE | `validate` | `"hasRole('ADMIN')"` | `IMPORTS_EXECUTE` | ✅ (phase 3) |
| 263 | `backend/src/main/java/com/discipolat/modules/imports/api/ImportController.java` | 48 | MÉTHODE | `importData` | `"hasRole('ADMIN')"` | `IMPORTS_EXECUTE` | ✅ (phase 3) |
| 264 | `backend/src/main/java/com/discipolat/modules/imports/api/SpaceImportController.java` | 50 | MÉTHODE | `importFile` | `"@authz.isPlatformSuperAdmin() or hasAnyRole('ADMIN','PASTEUR')"` | `IMPORTS_EXECUTE` | ✅ (phase 3) |
| 265 | `backend/src/main/java/com/discipolat/modules/imports/api/SpaceImportController.java` | 62 | MÉTHODE | `importJson` | `"@authz.isPlatformSuperAdmin() or hasAnyRole('ADMIN','PASTEUR')"` | `IMPORTS_EXECUTE` | ✅ (phase 3) |
| 266 | `backend/src/main/java/com/discipolat/modules/integrations/api/ConnectorController.java` | 20 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `INTEGRATIONS_*` | ✅ (phase 3) |
| 267 | `backend/src/main/java/com/discipolat/modules/intelligence/api/IntelligenceController.java` | 14 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `INTELLIGENCE_*` | ✅ (phase 3) |
| 268 | `backend/src/main/java/com/discipolat/modules/interactions/api/InteractionController.java` | 24 | MÉTHODE | `findBySoul` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `INTERACTIONS_READ` | ✅ (phase 3) |
| 269 | `backend/src/main/java/com/discipolat/modules/interactions/api/InteractionController.java` | 30 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `INTERACTIONS_CREATE` | ✅ (phase 3) |
| 270 | `backend/src/main/java/com/discipolat/modules/interactions/api/InteractionController.java` | 39 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `INTERACTIONS_DELETE` | ✅ (phase 3) |
| 271 | `backend/src/main/java/com/discipolat/modules/interactions/api/InteractionController.java` | 47 | MÉTHODE | `myReminders` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `INTERACTIONS_EXECUTE` | ✅ (phase 3) |
| 272 | `backend/src/main/java/com/discipolat/modules/inventory/api/AssetController.java` | 27 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `INVENTORY_*` | ✅ (phase 3) |
| 273 | `backend/src/main/java/com/discipolat/modules/inventory/api/AssetMaintenanceController.java` | 18 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `INVENTORY_*` | ✅ (phase 3) |
| 274 | `backend/src/main/java/com/discipolat/modules/inventory/api/InventoryController.java` | 27 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `INVENTORY_*` | ✅ (phase 3) |
| 275 | `backend/src/main/java/com/discipolat/modules/kpiNarrative/api/KpiNarrativeController.java` | 39 | MÉTHODE | `generate` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `KPI_NARRATIVE_EXECUTE` | ✅ (phase 3) |
| 276 | `backend/src/main/java/com/discipolat/modules/kpiNarrative/api/KpiNarrativeController.java` | 51 | MÉTHODE | `generateAll` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `KPI_NARRATIVE_EXECUTE` | ✅ (phase 3) |
| 277 | `backend/src/main/java/com/discipolat/modules/leaveRequests/api/LeaveRequestController.java` | 58 | MÉTHODE | `approve` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `LEAVE_REQUESTS_APPROVE` | ✅ (phase 3) |
| 278 | `backend/src/main/java/com/discipolat/modules/leaveRequests/api/LeaveRequestController.java` | 65 | MÉTHODE | `reject` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `LEAVE_REQUESTS_EXECUTE` | ✅ (phase 3) |
| 279 | `backend/src/main/java/com/discipolat/modules/loadPrediction/api/LoadPredictionController.java` | 13 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE')"` | `LOAD_PREDICTION_*` | ✅ (phase 3) |
| 280 | `backend/src/main/java/com/discipolat/modules/map/api/KingdomMappingController.java` | 36 | MÉTHODE | `heatmap` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `MAP_EXECUTE` | ✅ (phase 3) |
| 281 | `backend/src/main/java/com/discipolat/modules/map/api/KingdomMappingController.java` | 78 | MÉTHODE | `sectors` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `MAP_EXECUTE` | ✅ (phase 3) |
| 282 | `backend/src/main/java/com/discipolat/modules/map/api/MapController.java` | 14 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `MAP_*` | ✅ (phase 3) |
| 283 | `backend/src/main/java/com/discipolat/modules/members/api/CompetenceMatchingController.java` | 62 | MÉTHODE | `match` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `MEMBERS_EXECUTE` | ✅ |
| 284 | `backend/src/main/java/com/discipolat/modules/members/api/GeofencingController.java` | 27 | CLASSE | `—` | `"hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE', 'MEMBRE')"` | `MEMBERS_EXECUTE_R4A` | ✅ |
| 285 | `backend/src/main/java/com/discipolat/modules/members/api/GeofencingController.java` | 104 | MÉTHODE | `historyAll` | `"hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE')"` | `MEMBERS_EXECUTE_HISTORYALL` | ✅ |
| 286 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 105 | MÉTHODE | `inbox` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `MEMBERS_EXECUTE` | ✅ |
| 287 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 112 | MÉTHODE | `updateRequestStatus` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `MEMBERS_EXECUTE` | ✅ |
| 288 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 121 | MÉTHODE | `scopedPresences` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `MEMBERS_EXECUTE` | ✅ |
| 289 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 132 | MÉTHODE | `departmentPresenceSheet` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `MEMBERS_EXECUTE_DEPARTMENTPRESENCESHEET` | ✅ |
| 290 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 141 | MÉTHODE | `submitDepartmentPresences` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `MEMBERS_EXECUTE_SUBMITDEPARTMENTPRESENCES` | ✅ |
| 291 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 151 | MÉTHODE | `qrCheckin` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `MEMBERS_EXECUTE_QRCHECKIN` | ✅ |
| 292 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 164 | MÉTHODE | `qrCheckinScan` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `MEMBERS_EXECUTE_QRCHECKINSCAN` | ✅ |
| 293 | `backend/src/main/java/com/discipolat/modules/members/api/MemberController.java` | 197 | MÉTHODE | `qrResolve` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `MEMBERS_EXECUTE_QRRESOLVE` | ✅ |
| 294 | `backend/src/main/java/com/discipolat/modules/mentoring/api/MentoringController.java` | 28 | MÉTHODE | `list` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `MENTORING_READ` | ✅ (phase 3) |
| 295 | `backend/src/main/java/com/discipolat/modules/mentoring/api/MentoringController.java` | 40 | MÉTHODE | `listAll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `MENTORING_READ` | ✅ (phase 3) |
| 296 | `backend/src/main/java/com/discipolat/modules/mentoring/api/MentoringController.java` | 47 | MÉTHODE | `get` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `MENTORING_READ` | ✅ (phase 3) |
| 297 | `backend/src/main/java/com/discipolat/modules/mentoring/api/MentoringController.java` | 53 | MÉTHODE | `generate` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `MENTORING_EXECUTE` | ✅ (phase 3) |
| 298 | `backend/src/main/java/com/discipolat/modules/mentoring/api/MentoringController.java` | 62 | MÉTHODE | `markAsRead` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `MENTORING_EXECUTE` | ✅ (phase 3) |
| 299 | `backend/src/main/java/com/discipolat/modules/mentoring/api/MentoringController.java` | 69 | MÉTHODE | `archive` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `MENTORING_EXECUTE` | ✅ (phase 3) |
| 300 | `backend/src/main/java/com/discipolat/modules/mentoring/api/MentoringController.java` | 76 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE')"` | `MENTORING_EXECUTE` | ✅ (phase 3) |
| 301 | `backend/src/main/java/com/discipolat/modules/moderation/api/ModerationController.java` | 15 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `MODERATION_*` | ✅ (phase 3) |
| 302 | `backend/src/main/java/com/discipolat/modules/neighborhoodHealth/api/NeighborhoodHealthController.java` | 13 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE')"` | `NEIGHBORHOOD_HEALTH_*` | ✅ (phase 3) |
| 303 | `backend/src/main/java/com/discipolat/modules/network/api/NetworkController.java` | 61 | MÉTHODE | `createResource` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `NETWORK_CREATE` | ✅ (phase 3) |
| 304 | `backend/src/main/java/com/discipolat/modules/network/api/NetworkController.java` | 74 | MÉTHODE | `deactivateResource` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `NETWORK_EXECUTE` | ✅ (phase 3) |
| 305 | `backend/src/main/java/com/discipolat/modules/network/api/NetworkController.java` | 108 | MÉTHODE | `createEvent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `NETWORK_CREATE` | ✅ (phase 3) |
| 306 | `backend/src/main/java/com/discipolat/modules/network/api/NetworkController.java` | 127 | MÉTHODE | `deactivateEvent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `NETWORK_EXECUTE` | ✅ (phase 3) |
| 307 | `backend/src/main/java/com/discipolat/modules/network/api/NetworkController.java` | 161 | MÉTHODE | `updateMyDirectoryEntry` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `NETWORK_UPDATE` | ✅ (phase 3) |
| 308 | `backend/src/main/java/com/discipolat/modules/network/api/NetworkController.java` | 168 | MÉTHODE | `toggleListing` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `NETWORK_EXECUTE` | ✅ (phase 3) |
| 309 | `backend/src/main/java/com/discipolat/modules/network/api/NetworkController.java` | 178 | MÉTHODE | `getNetworkStats` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `NETWORK_READ` | ✅ (phase 3) |
| 310 | `backend/src/main/java/com/discipolat/modules/notifications/api/NotificationTemplateController.java` | 24 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `NOTIFICATIONS_*` | ✅ (phase 3) |
| 311 | `backend/src/main/java/com/discipolat/modules/objectives/api/ObjectiveController.java` | 26 | MÉTHODE | `findAll` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `OBJECTIVES_READ` | ✅ (phase 3) |
| 312 | `backend/src/main/java/com/discipolat/modules/objectives/api/ObjectiveController.java` | 33 | MÉTHODE | `myProgress` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `OBJECTIVES_EXECUTE` | ✅ (phase 3) |
| 313 | `backend/src/main/java/com/discipolat/modules/objectives/api/ObjectiveController.java` | 39 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `OBJECTIVES_CREATE` | ✅ (phase 3) |
| 314 | `backend/src/main/java/com/discipolat/modules/objectives/api/ObjectiveController.java` | 45 | MÉTHODE | `toggle` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `OBJECTIVES_EXECUTE` | ✅ (phase 3) |
| 315 | `backend/src/main/java/com/discipolat/modules/objectives/api/ObjectiveController.java` | 51 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `OBJECTIVES_DELETE` | ✅ (phase 3) |
| 316 | `backend/src/main/java/com/discipolat/modules/parallelfollowups/api/ParallelFollowupController.java` | 20 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PARALLELFOLLOWUPS_*` | ✅ (phase 3) |
| 317 | `backend/src/main/java/com/discipolat/modules/parallelfollowups/api/ParallelFollowupController.java` | 30 | MÉTHODE | `findAll` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PARALLELFOLLOWUPS_READ` | ✅ (phase 3) |
| 318 | `backend/src/main/java/com/discipolat/modules/parallelfollowups/api/ParallelFollowupController.java` | 46 | MÉTHODE | `create` | `"hasAnyRole('PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PARALLELFOLLOWUPS_CREATE` | ✅ (phase 3) |
| 319 | `backend/src/main/java/com/discipolat/modules/parallelfollowups/api/ParallelFollowupController.java` | 58 | MÉTHODE | `close` | `"hasAnyRole('PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PARALLELFOLLOWUPS_EXECUTE` | ✅ (phase 3) |
| 320 | `backend/src/main/java/com/discipolat/modules/passport/api/PassportController.java` | 50 | MÉTHODE | `issuePassport` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PASSPORT_EXECUTE` | ✅ (phase 3) |
| 321 | `backend/src/main/java/com/discipolat/modules/passport/api/PassportController.java` | 64 | MÉTHODE | `getMemberPassport` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `PASSPORT_READ` | ✅ (phase 3) |
| 322 | `backend/src/main/java/com/discipolat/modules/passport/api/PassportController.java` | 71 | MÉTHODE | `addEntry` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR')"` | `PASSPORT_CREATE` | ✅ (phase 3) |
| 323 | `backend/src/main/java/com/discipolat/modules/passport/api/PassportController.java` | 79 | MÉTHODE | `revoke` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PASSPORT_EXECUTE` | ✅ (phase 3) |
| 324 | `backend/src/main/java/com/discipolat/modules/passport/api/PassportController.java` | 109 | MÉTHODE | `getVerifications` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `PASSPORT_READ` | ✅ (phase 3) |
| 325 | `backend/src/main/java/com/discipolat/modules/payments/api/PaymentController.java` | 122 | MÉTHODE | `recent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `PAYMENTS_EXECUTE` | ✅ (phase 3) |
| 326 | `backend/src/main/java/com/discipolat/modules/payments/api/PaymentController.java` | 128 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `PAYMENTS_EXECUTE` | ✅ (phase 3) |
| 327 | `backend/src/main/java/com/discipolat/modules/payments/api/PaymentController.java` | 135 | MÉTHODE | `dashboard` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PAYMENTS_EXECUTE` | ✅ (phase 3) |
| 328 | `backend/src/main/java/com/discipolat/modules/payments/api/PaymentController.java` | 167 | MÉTHODE | `recurringStats` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `PAYMENTS_EXECUTE` | ✅ (phase 3) |
| 329 | `backend/src/main/java/com/discipolat/modules/payments/api/PaymentWebhookController.java` | 292 | MÉTHODE | `listLogs` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PAYMENTS_READ` | ✅ (phase 3) |
| 330 | `backend/src/main/java/com/discipolat/modules/payments/api/PaymentWebhookController.java` | 308 | MÉTHODE | `logStats` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PAYMENTS_EXECUTE` | ✅ (phase 3) |
| 331 | `backend/src/main/java/com/discipolat/modules/people/api/PeopleController.java` | 29 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'DEPARTMENT_LEADER', 'FAMILY_LEADER')"` | `PEOPLE_*` | ✅ (phase 3) |
| 332 | `backend/src/main/java/com/discipolat/modules/people/api/PeopleController.java` | 68 | MÉTHODE | `registerPerson` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PEOPLE_CREATE` | ✅ (phase 3) |
| 333 | `backend/src/main/java/com/discipolat/modules/people/api/PeopleController.java` | 85 | MÉTHODE | `mergePersons` | `"hasRole('ADMIN')"` | `PEOPLE_EXECUTE` | ✅ (phase 3) |
| 334 | `backend/src/main/java/com/discipolat/modules/people/api/PeopleController.java` | 128 | MÉTHODE | `assignRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PEOPLE_EXECUTE` | ✅ (phase 3) |
| 335 | `backend/src/main/java/com/discipolat/modules/people/api/PeopleController.java` | 141 | MÉTHODE | `endRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PEOPLE_EXECUTE` | ✅ (phase 3) |
| 336 | `backend/src/main/java/com/discipolat/modules/people/api/PeopleController.java` | 150 | MÉTHODE | `transferPastor` | `"hasRole('PASTOR_PRINCIPAL')"` | `PEOPLE_EXECUTE` | ✅ (phase 3) |
| 337 | `backend/src/main/java/com/discipolat/modules/platform/api/ApiDocsController.java` | 21 | MÉTHODE | `getApiDocs` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 338 | `backend/src/main/java/com/discipolat/modules/platform/api/ApiDocsController.java` | 74 | MÉTHODE | `getOpenApiSpec` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 339 | `backend/src/main/java/com/discipolat/modules/platform/api/BrandingController.java` | 44 | MÉTHODE | `updateBranding` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_UPDATE` | ✅ (phase 3) |
| 340 | `backend/src/main/java/com/discipolat/modules/platform/api/BrandingController.java` | 65 | MÉTHODE | `uploadBrandingAsset` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 341 | `backend/src/main/java/com/discipolat/modules/platform/api/DictionaryController.java` | 53 | MÉTHODE | `allDictionaries` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 342 | `backend/src/main/java/com/discipolat/modules/platform/api/DictionaryController.java` | 59 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_CREATE` | ✅ (phase 3) |
| 343 | `backend/src/main/java/com/discipolat/modules/platform/api/DictionaryController.java` | 66 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_UPDATE` | ✅ (phase 3) |
| 344 | `backend/src/main/java/com/discipolat/modules/platform/api/DictionaryController.java` | 73 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_DELETE` | ✅ (phase 3) |
| 345 | `backend/src/main/java/com/discipolat/modules/platform/api/DictionaryController.java` | 80 | MÉTHODE | `reset` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 346 | `backend/src/main/java/com/discipolat/modules/platform/api/FeedbackController.java` | 42 | MÉTHODE | `list` | `"hasAnyRole('ADMIN','PASTEUR')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 347 | `backend/src/main/java/com/discipolat/modules/platform/api/FeedbackController.java` | 48 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN','PASTEUR')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 348 | `backend/src/main/java/com/discipolat/modules/platform/api/FeedbackController.java` | 54 | MÉTHODE | `updateStatus` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_UPDATE` | ✅ (phase 3) |
| 349 | `backend/src/main/java/com/discipolat/modules/platform/api/ModuleFeatureController.java` | 109 | MÉTHODE | `toggleModule` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 350 | `backend/src/main/java/com/discipolat/modules/platform/api/ModuleFeatureController.java` | 168 | MÉTHODE | `updateFeatureFlags` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_UPDATE` | ✅ (phase 3) |
| 351 | `backend/src/main/java/com/discipolat/modules/platform/api/OrganizationManagementController.java` | 106 | MÉTHODE | `createNode` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'REGION_ADMIN')"` | `PLATFORM_CREATE` | ✅ (phase 3) |
| 352 | `backend/src/main/java/com/discipolat/modules/platform/api/OrganizationManagementController.java` | 175 | MÉTHODE | `?` | `@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'CHURCH_ADMIN', " +` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 353 | `backend/src/main/java/com/discipolat/modules/platform/api/OrganizationManagementController.java` | 215 | MÉTHODE | `moveNode` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 354 | `backend/src/main/java/com/discipolat/modules/platform/api/OrganizationManagementController.java` | 270 | MÉTHODE | `deleteNode` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_DELETE` | ✅ (phase 3) |
| 355 | `backend/src/main/java/com/discipolat/modules/platform/api/OrganizationManagementController.java` | 309 | MÉTHODE | `setResponsible` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 356 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 39 | MÉTHODE | `listPages` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 357 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 45 | MÉTHODE | `createPage` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_CREATE` | ✅ (phase 3) |
| 358 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 51 | MÉTHODE | `updatePage` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_UPDATE` | ✅ (phase 3) |
| 359 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 57 | MÉTHODE | `deletePage` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_DELETE` | ✅ (phase 3) |
| 360 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 64 | MÉTHODE | `setPublished` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 361 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 73 | MÉTHODE | `preview` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 362 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 79 | MÉTHODE | `sources` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 363 | `backend/src/main/java/com/discipolat/modules/platform/api/PageBuilderController.java` | 85 | MÉTHODE | `options` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 364 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantAdminController.java` | 66 | MÉTHODE | `getDashboard` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 365 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantAdminController.java` | 115 | MÉTHODE | `listMembers` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 366 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantAdminController.java` | 175 | MÉTHODE | `updateMemberRole` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_UPDATE` | ✅ (phase 3) |
| 367 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantAdminController.java` | 225 | MÉTHODE | `revokeMember` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 368 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantAdminController.java` | 284 | MÉTHODE | `listRoles` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 369 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantAdminController.java` | 309 | MÉTHODE | `getModules` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN', 'PASTEUR')"` | `PLATFORM_READ` | ✅ (phase 3) |
| 370 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantAdminController.java` | 335 | MÉTHODE | `toggleModule` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 371 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantSettingsController.java` | 46 | MÉTHODE | `updateSettings` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_UPDATE` | ✅ (phase 3) |
| 372 | `backend/src/main/java/com/discipolat/modules/platform/api/TenantSettingsController.java` | 56 | MÉTHODE | `uploadBrandingAsset` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `PLATFORM_EXECUTE` | ✅ (phase 3) |
| 373 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 31 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_CREATE` | ✅ (phase 3) |
| 374 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 47 | MÉTHODE | `findById` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_READ` | ✅ (phase 3) |
| 375 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 53 | MÉTHODE | `findAll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_READ` | ✅ (phase 3) |
| 376 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 86 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_EXECUTE` | ✅ (phase 3) |
| 377 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 92 | MÉTHODE | `findByAmeId` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_READ` | ✅ (phase 3) |
| 378 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 99 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_UPDATE` | ✅ (phase 3) |
| 379 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 113 | MÉTHODE | `markAsAnswered` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_EXECUTE` | ✅ (phase 3) |
| 380 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 122 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_DELETE` | ✅ (phase 3) |
| 381 | `backend/src/main/java/com/discipolat/modules/prayers/api/PrayerController.java` | 131 | MÉTHODE | `getActionsDeGrace` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PRAYERS_READ` | ✅ (phase 3) |
| 382 | `backend/src/main/java/com/discipolat/modules/programs/api/ProgramController.java` | 26 | MÉTHODE | `findAll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `PROGRAMS_READ` | ✅ (phase 3) |
| 383 | `backend/src/main/java/com/discipolat/modules/programs/api/ProgramController.java` | 34 | MÉTHODE | `findActive` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `PROGRAMS_READ` | ✅ (phase 3) |
| 384 | `backend/src/main/java/com/discipolat/modules/programs/api/ProgramController.java` | 40 | MÉTHODE | `findById` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `PROGRAMS_READ` | ✅ (phase 3) |
| 385 | `backend/src/main/java/com/discipolat/modules/programs/api/ProgramController.java` | 47 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PROGRAMS_CREATE` | ✅ (phase 3) |
| 386 | `backend/src/main/java/com/discipolat/modules/programs/api/ProgramController.java` | 53 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PROGRAMS_UPDATE` | ✅ (phase 3) |
| 387 | `backend/src/main/java/com/discipolat/modules/programs/api/ProgramController.java` | 60 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PROGRAMS_DELETE` | ✅ (phase 3) |
| 388 | `backend/src/main/java/com/discipolat/modules/prophetic/api/PropheticJournalController.java` | 24 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PROPHETIC_CREATE` | ✅ (phase 3) |
| 389 | `backend/src/main/java/com/discipolat/modules/prophetic/api/PropheticJournalController.java` | 30 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `PROPHETIC_UPDATE` | ✅ (phase 3) |
| 390 | `backend/src/main/java/com/discipolat/modules/prophetic/api/PropheticJournalController.java` | 36 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `PROPHETIC_DELETE` | ✅ (phase 3) |
| 391 | `backend/src/main/java/com/discipolat/modules/quest/api/QuestController.java` | 32 | MÉTHODE | `profileFor` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `QUEST_EXECUTE` | ✅ (phase 3) |
| 392 | `backend/src/main/java/com/discipolat/modules/quest/api/QuestController.java` | 61 | MÉTHODE | `award` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `QUEST_EXECUTE` | ✅ (phase 3) |
| 393 | `backend/src/main/java/com/discipolat/modules/relations/api/HierarchyController.java` | 43 | MÉTHODE | `hierarchyOf` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `RELATIONS_EXECUTE` | ✅ (phase 3) |
| 394 | `backend/src/main/java/com/discipolat/modules/relations/api/RelationController.java` | 94 | MÉTHODE | `declareFor` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `RELATIONS_EXECUTE` | ✅ (phase 3) |
| 395 | `backend/src/main/java/com/discipolat/modules/relations/api/RelationController.java` | 120 | MÉTHODE | `relationsOf` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `RELATIONS_EXECUTE` | ✅ (phase 3) |
| 396 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 54 | MÉTHODE | `getMakerReports` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_READ` | ✅ (phase 3) |
| 397 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 72 | MÉTHODE | `submitMakerReport` | `"hasAnyRole('PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_EXECUTE` | ✅ (phase 3) |
| 398 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 80 | MÉTHODE | `saveDraft` | `"hasAnyRole('PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_EXECUTE` | ✅ (phase 3) |
| 399 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 87 | MÉTHODE | `getMakerReport` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_READ` | ✅ (phase 3) |
| 400 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 95 | MÉTHODE | `getPreFilledReport` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_READ` | ✅ (phase 3) |
| 401 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 105 | MÉTHODE | `getUrgentAidRequests` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `REPORTS_READ` | ✅ (phase 3) |
| 402 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 111 | MÉTHODE | `markAidAsTreated` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `REPORTS_EXECUTE` | ✅ (phase 3) |
| 403 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 120 | MÉTHODE | `correctReport` | `"hasRole('ADMIN')"` | `REPORTS_EXECUTE` | ✅ (phase 3) |
| 404 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 148 | MÉTHODE | `getCorrections` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `REPORTS_READ` | ✅ (phase 3) |
| 405 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 154 | MÉTHODE | `getFamilyReports` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_READ` | ✅ (phase 3) |
| 406 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 172 | MÉTHODE | `submitFamilyReport` | `"hasAnyRole('PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_EXECUTE` | ✅ (phase 3) |
| 407 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 179 | MÉTHODE | `getFamilyReportsByFamily` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_READ` | ✅ (phase 3) |
| 408 | `backend/src/main/java/com/discipolat/modules/reports/api/MakerReportController.java` | 193 | MÉTHODE | `validateFamilyReport` | `"hasAnyRole('PASTEUR', 'RESPONSABLE')"` | `REPORTS_EXECUTE` | ✅ (phase 3) |
| 409 | `backend/src/main/java/com/discipolat/modules/reports/api/ReportExportController.java` | 33 | MÉTHODE | `exportMakerReports` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_EXPORT` | ✅ (phase 3) |
| 410 | `backend/src/main/java/com/discipolat/modules/reports/api/ReportExportController.java` | 69 | MÉTHODE | `exportFamilyReports` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `REPORTS_EXPORT` | ✅ (phase 3) |
| 411 | `backend/src/main/java/com/discipolat/modules/reports/api/ReportExportController.java` | 108 | MÉTHODE | `exportConsolidatedPdf` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `REPORTS_EXPORT` | ✅ (phase 3) |
| 412 | `backend/src/main/java/com/discipolat/modules/reports/api/ReportExportController.java` | 133 | MÉTHODE | `exportExecutivePdf` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `REPORTS_EXPORT` | ✅ (phase 3) |
| 413 | `backend/src/main/java/com/discipolat/modules/reports/api/ReportExportController.java` | 172 | MÉTHODE | `exportMakerPdf` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `REPORTS_EXPORT` | ✅ (phase 3) |
| 414 | `backend/src/main/java/com/discipolat/modules/rewards/api/RewardCertificateController.java` | 44 | MÉTHODE | `issue` | `"hasAnyRole('ADMIN','PASTEUR')"` | `REWARDS_EXECUTE` | ✅ (phase 3) |
| 415 | `backend/src/main/java/com/discipolat/modules/rewards/api/RewardCertificateController.java` | 51 | MÉTHODE | `list` | `"hasAnyRole('ADMIN','PASTEUR')"` | `REWARDS_READ` | ✅ (phase 3) |
| 416 | `backend/src/main/java/com/discipolat/modules/sabbathDashboard/api/SabbathDashboardController.java` | 13 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE')"` | `SABBATH_DASHBOARD_*` | ✅ (phase 3) |
| 417 | `backend/src/main/java/com/discipolat/modules/search/api/SearchController.java` | 34 | MÉTHODE | `search` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SEARCH_READ` | ✅ (phase 3) |
| 418 | `backend/src/main/java/com/discipolat/modules/search/api/SearchController.java` | 51 | MÉTHODE | `autocomplete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SEARCH_EXECUTE` | ✅ (phase 3) |
| 419 | `backend/src/main/java/com/discipolat/modules/search/api/SearchController.java` | 65 | MÉTHODE | `getCompleteProfile` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SEARCH_READ` | ✅ (phase 3) |
| 420 | `backend/src/main/java/com/discipolat/modules/sermon/api/SermonAssistantController.java` | 21 | MÉTHODE | `outlines` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `SERMON_EXECUTE` | ✅ (phase 3) |
| 421 | `backend/src/main/java/com/discipolat/modules/skillsMatrix/api/SkillController.java` | 26 | MÉTHODE | `list` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SKILLS_MATRIX_READ` | ✅ (phase 3) |
| 422 | `backend/src/main/java/com/discipolat/modules/skillsMatrix/api/SkillController.java` | 32 | MÉTHODE | `matrix` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SKILLS_MATRIX_EXECUTE` | ✅ (phase 3) |
| 423 | `backend/src/main/java/com/discipolat/modules/skillsMatrix/api/SkillController.java` | 38 | MÉTHODE | `byMember` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SKILLS_MATRIX_EXECUTE` | ✅ (phase 3) |
| 424 | `backend/src/main/java/com/discipolat/modules/skillsMatrix/api/SkillController.java` | 44 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SKILLS_MATRIX_CREATE` | ✅ (phase 3) |
| 425 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 38 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE', 'TENANT_OWNER', 'TENANT_ADMIN')"` | `SOULS_*` | ✅ (phase 3) |
| 426 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 80 | MÉTHODE | `trash` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 427 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 99 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_CREATE` | ✅ (phase 3) |
| 428 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 106 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_UPDATE` | ✅ (phase 3) |
| 429 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 113 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_DELETE` | ✅ (phase 3) |
| 430 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 130 | MÉTHODE | `reassign` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 431 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 148 | MÉTHODE | `suggestFaiseur` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 432 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 156 | MÉTHODE | `findEnDifficulte` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_READ` | ✅ (phase 3) |
| 433 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 165 | MÉTHODE | `restore` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 434 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 172 | MÉTHODE | `createRetractionRequest` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_CREATE` | ✅ (phase 3) |
| 435 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 182 | MÉTHODE | `getRetractionRequests` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_READ` | ✅ (phase 3) |
| 436 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 194 | MÉTHODE | `getRetractionRequests` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_READ` | ✅ (phase 3) |
| 437 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 203 | MÉTHODE | `approveRetraction` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_APPROVE` | ✅ (phase 3) |
| 438 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 212 | MÉTHODE | `rejectRetraction` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 439 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 222 | MÉTHODE | `markAsExited` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 440 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 234 | MÉTHODE | `reintegrate` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 441 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 243 | MÉTHODE | `getExits` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_READ` | ✅ (phase 3) |
| 442 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 258 | MÉTHODE | `spiritualScore` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 443 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 264 | MÉTHODE | `spiritualScoreHistory` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 444 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 272 | MÉTHODE | `getPastoral360` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_READ` | ✅ (phase 3) |
| 445 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulController.java` | 280 | MÉTHODE | `filterSouls` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 446 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulNoteController.java` | 18 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_*` | ✅ (phase 3) |
| 447 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulNoteController.java` | 28 | MÉTHODE | `create` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_CREATE` | ✅ (phase 3) |
| 448 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulNoteController.java` | 41 | MÉTHODE | `findBySoulId` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_READ` | ✅ (phase 3) |
| 449 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulNoteController.java` | 48 | MÉTHODE | `update` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_UPDATE` | ✅ (phase 3) |
| 450 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulNoteController.java` | 57 | MÉTHODE | `delete` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_DELETE` | ✅ (phase 3) |
| 451 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulTagController.java` | 23 | MÉTHODE | `getTags` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_READ` | ✅ (phase 3) |
| 452 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulTagController.java` | 29 | MÉTHODE | `addTag` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_CREATE` | ✅ (phase 3) |
| 453 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulTagController.java` | 36 | MÉTHODE | `removeTag` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_DELETE` | ✅ (phase 3) |
| 454 | `backend/src/main/java/com/discipolat/modules/souls/api/SoulTagController.java` | 42 | MÉTHODE | `allTags` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `SOULS_EXECUTE` | ✅ (phase 3) |
| 455 | `backend/src/main/java/com/discipolat/modules/statuses/api/CustomStatusController.java` | 58 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `STATUSES_CREATE` | ✅ (phase 3) |
| 456 | `backend/src/main/java/com/discipolat/modules/statuses/api/CustomStatusController.java` | 66 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `STATUSES_UPDATE` | ✅ (phase 3) |
| 457 | `backend/src/main/java/com/discipolat/modules/statuses/api/CustomStatusController.java` | 72 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `STATUSES_DELETE` | ✅ (phase 3) |
| 458 | `backend/src/main/java/com/discipolat/modules/statuses/api/CustomStatusController.java` | 92 | MÉTHODE | `change` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `STATUSES_EXECUTE` | ✅ (phase 3) |
| 459 | `backend/src/main/java/com/discipolat/modules/streaming/api/LiveStreamController.java` | 46 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `STREAMING_CREATE` | ✅ (phase 3) |
| 460 | `backend/src/main/java/com/discipolat/modules/streaming/api/LiveStreamController.java` | 53 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `STREAMING_UPDATE` | ✅ (phase 3) |
| 461 | `backend/src/main/java/com/discipolat/modules/streaming/api/LiveStreamController.java` | 61 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `STREAMING_DELETE` | ✅ (phase 3) |
| 462 | `backend/src/main/java/com/discipolat/modules/streaming/api/LiveStreamController.java` | 68 | MÉTHODE | `goLive` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `STREAMING_CREATE` | ✅ (phase 3) |
| 463 | `backend/src/main/java/com/discipolat/modules/streaming/api/LiveStreamController.java` | 74 | MÉTHODE | `endStream` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `STREAMING_CREATE` | ✅ (phase 3) |
| 464 | `backend/src/main/java/com/discipolat/modules/succession/api/SuccessionPlanController.java` | 35 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `SUCCESSION_CREATE` | ✅ (phase 3) |
| 465 | `backend/src/main/java/com/discipolat/modules/succession/api/SuccessionPlanController.java` | 47 | MÉTHODE | `updateReadiness` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `SUCCESSION_UPDATE` | ✅ (phase 3) |
| 466 | `backend/src/main/java/com/discipolat/modules/succession/api/SuccessionPlanController.java` | 53 | MÉTHODE | `updateStatus` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `SUCCESSION_UPDATE` | ✅ (phase 3) |
| 467 | `backend/src/main/java/com/discipolat/modules/succession/api/SuccessionPlanController.java` | 59 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `SUCCESSION_DELETE` | ✅ (phase 3) |
| 468 | `backend/src/main/java/com/discipolat/modules/sync/api/SyncController.java` | 73 | MÉTHODE | `resolve` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `SYNC_EXECUTE` | ✅ (phase 3) |
| 469 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 61 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `TASKS_CREATE` | ✅ (phase 3) |
| 470 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 68 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `TASKS_UPDATE` | ✅ (phase 3) |
| 471 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 75 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TASKS_DELETE` | ✅ (phase 3) |
| 472 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 83 | MÉTHODE | `updateStatus` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `TASKS_UPDATE` | ✅ (phase 3) |
| 473 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 90 | MÉTHODE | `assign` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_EXECUTE` | ✅ (phase 3) |
| 474 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 105 | MÉTHODE | `createSubtask` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `TASKS_CREATE` | ✅ (phase 3) |
| 475 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 119 | MÉTHODE | `deleteAttachment` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_DELETE` | ✅ (phase 3) |
| 476 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 141 | MÉTHODE | `deleteComment` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_DELETE` | ✅ (phase 3) |
| 477 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 156 | MÉTHODE | `createDependency` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_CREATE` | ✅ (phase 3) |
| 478 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 163 | MÉTHODE | `deleteDependency` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_DELETE` | ✅ (phase 3) |
| 479 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 178 | MÉTHODE | `updateKanbanColumn` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_UPDATE` | ✅ (phase 3) |
| 480 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 192 | MÉTHODE | `reorder` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_EXECUTE` | ✅ (phase 3) |
| 481 | `backend/src/main/java/com/discipolat/modules/tasks/api/TaskController.java` | 239 | MÉTHODE | `createFromTemplate` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TASKS_CREATE` | ✅ (phase 3) |
| 482 | `backend/src/main/java/com/discipolat/modules/tenants/api/ConfigurationController.java` | 87 | MÉTHODE | `setConfigSource` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_EXECUTE` | ✅ (phase 3) |
| 483 | `backend/src/main/java/com/discipolat/modules/tenants/api/ConfigurationController.java` | 124 | MÉTHODE | `updateLocalConfig` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_UPDATE` | ✅ (phase 3) |
| 484 | `backend/src/main/java/com/discipolat/modules/tenants/api/ModuleCatalogController.java` | 105 | MÉTHODE | `toggleSpaceModule` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_EXECUTE` | ✅ (phase 3) |
| 485 | `backend/src/main/java/com/discipolat/modules/tenants/api/ModuleCatalogController.java` | 125 | MÉTHODE | `updateSpaceModuleConfig` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_UPDATE` | ✅ (phase 3) |
| 486 | `backend/src/main/java/com/discipolat/modules/tenants/api/ModuleCatalogController.java` | 138 | MÉTHODE | `updateSpaceModuleLimits` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_UPDATE` | ✅ (phase 3) |
| 487 | `backend/src/main/java/com/discipolat/modules/tenants/api/ModuleCatalogController.java` | 151 | MÉTHODE | `updateSpaceModuleOrder` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_UPDATE` | ✅ (phase 3) |
| 488 | `backend/src/main/java/com/discipolat/modules/tenants/api/TenantFeatureController.java` | 51 | MÉTHODE | `getFeatures` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN')"` | `TENANTS_READ` | ✅ (phase 3) |
| 489 | `backend/src/main/java/com/discipolat/modules/tenants/api/TenantFeatureController.java` | 61 | MÉTHODE | `getEnabledFeatures` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'ADMIN')"` | `TENANTS_READ` | ✅ (phase 3) |
| 490 | `backend/src/main/java/com/discipolat/modules/tenants/api/TenantFeatureController.java` | 71 | MÉTHODE | `updateFeature` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_UPDATE` | ✅ (phase 3) |
| 491 | `backend/src/main/java/com/discipolat/modules/tenants/api/TenantFeatureController.java` | 92 | MÉTHODE | `disableFeature` | `"hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')"` | `TENANTS_EXECUTE` | ✅ (phase 3) |
| 492 | `backend/src/main/java/com/discipolat/modules/testimonials/api/TestimonyController.java` | 65 | MÉTHODE | `approve` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TESTIMONIALS_CREATE` | ✅ (phase 3) |
| 493 | `backend/src/main/java/com/discipolat/modules/testimonials/api/TestimonyController.java` | 71 | MÉTHODE | `reject` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TESTIMONIALS_CREATE` | ✅ (phase 3) |
| 494 | `backend/src/main/java/com/discipolat/modules/tickets/api/TicketController.java` | 32 | MÉTHODE | `list` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TICKETS_READ` | ✅ (phase 3) |
| 495 | `backend/src/main/java/com/discipolat/modules/tontine/api/TontineController.java` | 14 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TONTINE_*` | ✅ (phase 3) |
| 496 | `backend/src/main/java/com/discipolat/modules/tontine/api/TontineController.java` | 29 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `TONTINE_CREATE` | ✅ (phase 3) |
| 497 | `backend/src/main/java/com/discipolat/modules/tontine/api/TontineController.java` | 35 | MÉTHODE | `addMember` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `TONTINE_CREATE` | ✅ (phase 3) |
| 498 | `backend/src/main/java/com/discipolat/modules/tontine/api/TontineController.java` | 41 | MÉTHODE | `markPaid` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TONTINE_EXECUTE` | ✅ (phase 3) |
| 499 | `backend/src/main/java/com/discipolat/modules/tontine/api/TontineController.java` | 50 | MÉTHODE | `nextRound` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TONTINE_EXECUTE` | ✅ (phase 3) |
| 500 | `backend/src/main/java/com/discipolat/modules/trainings/api/SermonTranscriptionController.java` | 19 | CLASSE | `—` | `"hasAnyRole('PASTEUR', 'ADMIN', 'RESPONSABLE')"` | `TRAININGS_*` | ✅ (phase 3) |
| 501 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 29 | MÉTHODE | `findAll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_READ` | ✅ (phase 3) |
| 502 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 35 | MÉTHODE | `findById` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_READ` | ✅ (phase 3) |
| 503 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 41 | MÉTHODE | `modules` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 504 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 48 | MÉTHODE | `quiz` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 505 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 58 | MÉTHODE | `enroll` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 506 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 64 | MÉTHODE | `myEnrollments` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 507 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 70 | MÉTHODE | `submitQuiz` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 508 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 78 | MÉTHODE | `myCertificates` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 509 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 88 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 510 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 98 | MÉTHODE | `createCourse` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TRAININGS_CREATE` | ✅ (phase 3) |
| 511 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 104 | MÉTHODE | `addModule` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TRAININGS_CREATE` | ✅ (phase 3) |
| 512 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 113 | MÉTHODE | `addQuestion` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TRAININGS_CREATE` | ✅ (phase 3) |
| 513 | `backend/src/main/java/com/discipolat/modules/trainings/api/TrainingController.java` | 123 | MÉTHODE | `completeModuleRead` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `TRAININGS_EXECUTE` | ✅ (phase 3) |
| 514 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferAdminController.java` | 23 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TRANSFERS_*` | ✅ (phase 3) |
| 515 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 30 | CLASSE | `—` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')"` | `TRANSFERS_*` | ✅ (phase 3) |
| 516 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 40 | MÉTHODE | `findAll` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')"` | `TRANSFERS_READ` | ✅ (phase 3) |
| 517 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 56 | MÉTHODE | `configurations` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')"` | `TRANSFERS_EXECUTE` | ✅ (phase 3) |
| 518 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 62 | MÉTHODE | `findById` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')"` | `TRANSFERS_READ` | ✅ (phase 3) |
| 519 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 68 | MÉTHODE | `create` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')"` | `TRANSFERS_CREATE` | ✅ (phase 3) |
| 520 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 75 | MÉTHODE | `update` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')"` | `TRANSFERS_UPDATE` | ✅ (phase 3) |
| 521 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 82 | MÉTHODE | `submit` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')"` | `TRANSFERS_EXECUTE` | ✅ (phase 3) |
| 522 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 88 | MÉTHODE | `decide` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE')"` | `TRANSFERS_EXECUTE` | ✅ (phase 3) |
| 523 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 95 | MÉTHODE | `cancel` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR')"` | `TRANSFERS_EXECUTE` | ✅ (phase 3) |
| 524 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 101 | MÉTHODE | `archive` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `TRANSFERS_EXECUTE` | ✅ (phase 3) |
| 525 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 107 | MÉTHODE | `history` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')"` | `TRANSFERS_EXECUTE` | ✅ (phase 3) |
| 526 | `backend/src/main/java/com/discipolat/modules/transfers/api/TransferController.java` | 113 | MÉTHODE | `decisions` | `"hasAnyRole('ADMIN','PASTEUR','RESPONSABLE','CHEF_DE_FAMILLE','FAISEUR','MEMBRE')"` | `TRANSFERS_EXECUTE` | ✅ (phase 3) |
| 527 | `backend/src/main/java/com/discipolat/modules/twin/api/DigitalTwinController.java` | 21 | MÉTHODE | `snapshot` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TWIN_EXECUTE` | ✅ (phase 3) |
| 528 | `backend/src/main/java/com/discipolat/modules/twin/api/DigitalTwinController.java` | 28 | MÉTHODE | `simulate` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `TWIN_EXECUTE` | ✅ (phase 3) |
| 529 | `backend/src/main/java/com/discipolat/modules/usageAnalytics/api/UsageAnalyticsController.java` | 38 | MÉTHODE | `summary` | `"hasAnyRole('ADMIN','PASTEUR')"` | `USAGE_ANALYTICS_EXECUTE` | ✅ (phase 3) |
| 530 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 154 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `USERS_CREATE` | ✅ (phase 3) |
| 531 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 185 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `USERS_UPDATE` | ✅ (phase 3) |
| 532 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 280 | MÉTHODE | `deactivate` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 533 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 287 | MÉTHODE | `activate` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 534 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 294 | MÉTHODE | `promoteToChefDeFamille` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 535 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 303 | MÉTHODE | `demoteFromChefDeFamille` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 536 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 310 | MÉTHODE | `findByRole` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `USERS_READ` | ✅ (phase 3) |
| 537 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 317 | MÉTHODE | `findByFamille` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `USERS_READ` | ✅ (phase 3) |
| 538 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 326 | MÉTHODE | `promoteToFaiseur` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 539 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 334 | MÉTHODE | `getFaiseurWorkload` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `USERS_READ` | ✅ (phase 3) |
| 540 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 348 | MÉTHODE | `getUserDetail` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `USERS_READ` | ✅ (phase 3) |
| 541 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 379 | MÉTHODE | `getFaiseurHistory` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `USERS_READ` | ✅ (phase 3) |
| 542 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 393 | MÉTHODE | `transferFaiseur` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 543 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 403 | MÉTHODE | `demote` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 544 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 412 | MÉTHODE | `hardDelete` | `"hasRole('ADMIN') && @perm.has('USER','DELETE')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 545 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 423 | MÉTHODE | `restore` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 546 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 432 | MÉTHODE | `addRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_CREATE` | ✅ (phase 3) |
| 547 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 445 | MÉTHODE | `removeRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_DELETE` | ✅ (phase 3) |
| 548 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 455 | MÉTHODE | `replaceRoles` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 549 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 466 | MÉTHODE | `setActiveRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USERS_EXECUTE` | ✅ (phase 3) |
| 550 | `backend/src/main/java/com/discipolat/modules/users/api/UserController.java` | 478 | MÉTHODE | `getEvaluationScores` | `"hasAnyRole('PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `USERS_READ` | ✅ (phase 3) |
| 551 | `backend/src/main/java/com/discipolat/modules/ussd/api/UssdController.java` | 91 | MÉTHODE | `stats` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `USSD_EXECUTE` | ✅ (phase 3) |
| 552 | `backend/src/main/java/com/discipolat/modules/visits/api/PastoralVisitController.java` | 69 | MÉTHODE | `autoGenerate` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `VISITS_EXECUTE` | ✅ (phase 3) |
| 553 | `backend/src/main/java/com/discipolat/modules/visits/api/VisitController.java` | 20 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE')"` | `VISITS_*` | ✅ (phase 3) |
| 554 | `backend/src/main/java/com/discipolat/modules/voicenotifications/api/VoiceNotificationController.java` | 38 | MÉTHODE | `testNotifyRole` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `VOICENOTIFICATIONS_EXECUTE` | ✅ (phase 3) |
| 555 | `backend/src/main/java/com/discipolat/modules/voicenotifications/api/VoiceNotificationController.java` | 82 | MÉTHODE | `testNotifyAll` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `VOICENOTIFICATIONS_EXECUTE` | ✅ (phase 3) |
| 556 | `backend/src/main/java/com/discipolat/modules/voicereports/api/VoiceReportController.java` | 23 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `VOICEREPORTS_CREATE` | ✅ (phase 3) |
| 557 | `backend/src/main/java/com/discipolat/modules/voicereports/api/VoiceReportController.java` | 29 | MÉTHODE | `recent` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `VOICEREPORTS_EXECUTE` | ✅ (phase 3) |
| 558 | `backend/src/main/java/com/discipolat/modules/voicereports/api/VoiceReportController.java` | 42 | MÉTHODE | `structured` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `VOICEREPORTS_EXECUTE` | ✅ (phase 3) |
| 559 | `backend/src/main/java/com/discipolat/modules/voicereports/api/VoiceReportController.java` | 49 | MÉTHODE | `actionItems` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR')"` | `VOICEREPORTS_EXECUTE` | ✅ (phase 3) |
| 560 | `backend/src/main/java/com/discipolat/modules/volunteers/api/VolunteerController.java` | 27 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `VOLUNTEERS_CREATE` | ✅ (phase 3) |
| 561 | `backend/src/main/java/com/discipolat/modules/volunteers/api/VolunteerController.java` | 33 | MÉTHODE | `update` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `VOLUNTEERS_UPDATE` | ✅ (phase 3) |
| 562 | `backend/src/main/java/com/discipolat/modules/volunteers/api/VolunteerController.java` | 39 | MÉTHODE | `delete` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `VOLUNTEERS_DELETE` | ✅ (phase 3) |
| 563 | `backend/src/main/java/com/discipolat/modules/webhooks/api/WebhookController.java` | 14 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WEBHOOKS_*` | ✅ (phase 3) |
| 564 | `backend/src/main/java/com/discipolat/modules/weeklyChallenges/api/WeeklyChallengeController.java` | 15 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE')"` | `WEEKLY_CHALLENGES_*` | ✅ (phase 3) |
| 565 | `backend/src/main/java/com/discipolat/modules/whatsapp/api/WhatsAppAdminController.java` | 21 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WHATSAPP_*` | ✅ (phase 3) |
| 566 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowAutomationController.java` | 26 | MÉTHODE | `list` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WORKFLOW_READ` | ✅ (phase 3) |
| 567 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowAutomationController.java` | 32 | MÉTHODE | `get` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WORKFLOW_READ` | ✅ (phase 3) |
| 568 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowAutomationController.java` | 38 | MÉTHODE | `create` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WORKFLOW_CREATE` | ✅ (phase 3) |
| 569 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowAutomationController.java` | 54 | MÉTHODE | `toggle` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WORKFLOW_EXECUTE` | ✅ (phase 3) |
| 570 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowAutomationController.java` | 60 | MÉTHODE | `delete` | `"hasRole('ADMIN')"` | `WORKFLOW_DELETE` | ✅ (phase 3) |
| 571 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowConfigController.java` | 27 | CLASSE | `—` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WORKFLOW_*` | ✅ (phase 3) |
| 572 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 50 | MÉTHODE | `createDefinition` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_CREATE` | ✅ (phase 3) |
| 573 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 62 | MÉTHODE | `updateDefinition` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_UPDATE` | ✅ (phase 3) |
| 574 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 69 | MÉTHODE | `deleteDefinition` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WORKFLOW_DELETE` | ✅ (phase 3) |
| 575 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 91 | MÉTHODE | `addStep` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_CREATE` | ✅ (phase 3) |
| 576 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 97 | MÉTHODE | `updateStep` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_UPDATE` | ✅ (phase 3) |
| 577 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 103 | MÉTHODE | `deleteStep` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_DELETE` | ✅ (phase 3) |
| 578 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 110 | MÉTHODE | `addTransition` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_CREATE` | ✅ (phase 3) |
| 579 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 117 | MÉTHODE | `deleteTransition` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_DELETE` | ✅ (phase 3) |
| 580 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 140 | MÉTHODE | `reset` | `"hasAnyRole('ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE')"` | `WORKFLOW_EXECUTE` | ✅ (phase 3) |
| 581 | `backend/src/main/java/com/discipolat/modules/workflow/api/WorkflowEngineController.java` | 177 | MÉTHODE | `runEscalations` | `"hasAnyRole('ADMIN', 'PASTEUR')"` | `WORKFLOW_EXECUTE` | ✅ (phase 3) |

## B. Frontend — fichiers avec littéraux de rôle

Migration : remplacer la comparaison au rôle (`hasRole('ADMIN')`, `activeRole === 'FAISEUR'`, etc.)
par `hasPermission('<CLÉ>')` ou `canAccess('<RESSOURCE>', '<ACTION>')` déjà exposés par `TenantContext`.
Le défaut codé `activeRole = 'FAISEUR'` (`AuthContext.tsx`) doit disparaître (valeur servie par le serveur).

| # | Fichier | Littéraux | Statut |
|---|---|---|---|
| 1 | `frontend/src/App.tsx` | 631 | ✅ routes/gardes servies (Phase 4 : 185 `<ProtectedRoute permissions=[...]>` posées sur toutes les routes restreintes ; `roles` conservé en repli de transition — served-first). Littéraux intra-composants résiduels |
| 2 | `frontend/src/lib/routeAccess.ts` | 591 | ✅ registre `ROUTE_PERMISSIONS` 173 clés servies + `canAccessPath`/`filterNavByAccess` ; verrou `routeAccessPermissions.test.ts` (registre ≡ catalogue, exhaustivité) |
| 3 | `frontend/src/__tests__/Sidebar.test.tsx` | 47 | ⬜ |
| 4 | `frontend/src/components/CommandPalette.tsx` | 41 | 🟨 (résultats filtrés par clés servies quand TenantContext prêt) |
| 5 | `frontend/src/workspaces.ts` | 39 | ⬜ |
| 6 | `frontend/src/__tests__/RoleWorkspaceRouting.develop1.test.tsx` | 39 | ⬜ |
| 7 | `frontend/src/__tests__/RoleWorkspaceRouting.test.tsx` | 39 | ⬜ |
| 8 | `frontend/src/pages/UsersPage.tsx` | 36 | ⬜ |
| 9 | `frontend/src/pages/DepartmentManagementPage.tsx` | 33 | ⬜ |
| 10 | `frontend/src/__tests__/workspaces.test.ts` | 31 | ⬜ |
| 11 | `frontend/src/types/index.ts` | 26 | ⬜ |
| 12 | `frontend/src/pages/NotFoundPage.tsx` | 25 | ⬜ |
| 13 | `frontend/src/components/users/UserDetailModal.tsx` | 23 | ⬜ |
| 14 | `frontend/src/pages/LoginPage.tsx` | 22 | ⬜ |
| 15 | `frontend/src/components/pasteur/PasteurUsersTab.tsx` | 20 | ⬜ |
| 16 | `frontend/src/__tests__/RouteGuards.test.tsx` | 19 | ⬜ |
| 17 | `frontend/src/pages/FamilyDetailPage.tsx` | 17 | ⬜ |
| 18 | `frontend/src/__tests__/CommandPalette.test.tsx` | 16 | ⬜ |
| 19 | `frontend/src/__tests__/routeAccessV3.test.ts` | 16 | ⬜ |
| 20 | `frontend/src/__tests__/SoulCustomFields.test.tsx` | 15 | ⬜ |
| 21 | `frontend/src/pages/DashboardPage.tsx` | 15 | ✅ blocs « Vue Responsable » / « Mes ames » servis (V270 `UI_DASHBOARD_RESPONSABLE_SECTION_ACCESS` / `UI_DASHBOARD_FAISER_SECTION_ACCESS`, matrices parité exacte) ; les `hasRole` restants sont les replis `legacy` des gardes |
| 22 | `frontend/src/pages/MemberDashboardPage.tsx` | 13 | ✅ `canManage` servi (V270 `UI_DASHBOARD_MEMBRE_MANAGE_ACCESS` {MEMBRE}) — `useServedAccess` était déjà importé mais inutilisé |
| 23 | `frontend/src/pages/PlatformPagesPage.tsx` | 13 | ⬜ |
| 24 | `frontend/src/pages/ProfilePage.tsx` | 13 | ⬜ |
| 25 | `frontend/src/__tests__/CommandPalette.develop1.test.tsx` | 12 | ⬜ |
| 26 | `frontend/src/i18n/ar.ts` | 12 | ⬜ |
| 27 | `frontend/src/i18n/en.ts` | 12 | ⬜ |
| 28 | `frontend/src/i18n/es.ts` | 12 | ⬜ |
| 29 | `frontend/src/i18n/fr.ts` | 12 | ⬜ |
| 30 | `frontend/src/i18n/pt.ts` | 12 | ⬜ |
| 31 | `frontend/src/i18n/sw.ts` | 12 | ⬜ |
| 32 | `frontend/src/components/pasteur/PasteurEvaluationsTab.tsx` | 11 | ⬜ |
| 33 | `frontend/src/__tests__/CommunicationsPage.test.tsx` | 10 | ⬜ |
| 34 | `frontend/src/__tests__/PastoralMinistryPage.test.tsx` | 10 | ⬜ |
| 35 | `frontend/src/pages/CommunicationsPage.tsx` | 10 | ⬜ |
| 36 | `frontend/src/pages/TransferCreatePage.tsx` | 10 | ⬜ |
| 37 | `frontend/src/contexts/AuthContext.tsx` | 9 | ⬜ |
| 38 | `frontend/src/pages/AdminCustomFieldsPage.tsx` | 9 | ⬜ |
| 39 | `frontend/src/pages/EvaluationsPage.tsx` | 9 | ⬜ |
| 40 | `frontend/src/pages/PassportPage.tsx` | 9 | ⬜ |
| 41 | `frontend/src/components/layout/Sidebar.tsx` | 9 | ⬜ |
| 42 | `frontend/src/__tests__/AuditPage.test.tsx` | 8 | ⬜ |
| 43 | `frontend/src/__tests__/LoginPage.test.tsx` | 8 | ⬜ |
| 44 | `frontend/src/__tests__/MyRelationsCard.test.tsx` | 8 | ⬜ |
| 45 | `frontend/src/__tests__/PasteurTabs.test.tsx` | 8 | ⬜ |
| 46 | `frontend/src/__tests__/PlatformMenusPage.test.tsx` | 8 | ⬜ |
| 47 | `frontend/src/pages/ApiDocsPage.tsx` | 8 | ⬜ |
| 48 | `frontend/src/pages/PastoralMinistryPage.tsx` | 8 | ⬜ |
| 49 | `frontend/src/pages/AdminNotificationTemplatesPage.tsx` | 7 | ⬜ |
| 50 | `frontend/src/pages/ObjectivesPage.tsx` | 7 | ⬜ |
| 51 | `frontend/src/pages/UserRolesPage.tsx` | 7 | ⬜ |
| 52 | `frontend/src/__tests__/LoginPageChurchPicker.test.tsx` | 6 | ⬜ |
| 53 | `frontend/src/__tests__/MemberRequestsPage.test.tsx` | 6 | ⬜ |
| 54 | `frontend/src/__tests__/TenantAdminInvitationsPage.test.tsx` | 6 | ⬜ |
| 55 | `frontend/src/__tests__/TrainingsPage.test.tsx` | 6 | ⬜ |
| 56 | `frontend/src/pages/MemberRequestsPage.tsx` | 6 | ✅ vue « Toute l'église » servie (V270 `UI_MEMBER_REQUESTS_GLOBAL_SCOPE_ACCESS` {ADMIN, PASTEUR}) |
| 57 | `frontend/src/pages/PlatformMenusPage.tsx` | 6 | ⬜ |
| 58 | `frontend/src/components/layout/Navbar.tsx` | 6 | ⬜ |
| 59 | `frontend/src/components/shared/AttachmentPicker.tsx` | 6 | ⬜ |
| 60 | `frontend/src/__tests__/AcceptInvitationPage.test.tsx` | 5 | ⬜ |
| 61 | `frontend/src/__tests__/App.platformRoutes.test.tsx` | 5 | ⬜ |
| 62 | `frontend/src/hooks/useInvitations.ts` | 5 | ⬜ |
| 63 | `frontend/src/pages/AdminWorkflowBuilderPage.tsx` | 5 | ⬜ |
| 64 | `frontend/src/pages/FamiliesPage.tsx` | 5 | ⬜ |
| 65 | `frontend/src/pages/ResponsableDashboardPage.tsx` | 5 | ✅ `canManage` servi (V270 `UI_DASHBOARD_RESPONSABLE_MANAGE_ACCESS` {RESPONSABLE}) |
| 66 | `frontend/src/navigation/NavigationContext.tsx` | 4 | 🟨 (filterNavByAccess — nav latérale pilotée par clés servies) |
| 67 | `frontend/src/__tests__/AdminNotificationTemplatesPage.test.tsx` | 4 | ⬜ |
| 68 | `frontend/src/__tests__/AuthJourneys.test.tsx` | 4 | ⬜ |
| 69 | `frontend/src/__tests__/DepartmentManagementSearchEvents.test.tsx` | 4 | ⬜ |
| 70 | `frontend/src/__tests__/PeopleDirectoryPage.test.tsx` | 4 | ⬜ |
| 71 | `frontend/src/__tests__/PostAuthRedirect.test.tsx` | 4 | ⬜ |
| 72 | `frontend/src/__tests__/RegisterPageJoinIssues.test.tsx` | 4 | ⬜ |
| 73 | `frontend/src/__tests__/SocialLoginButtons.test.tsx` | 4 | ⬜ |
| 74 | `frontend/src/pages/AppointmentsPage.tsx` | 4 | ⬜ |
| 75 | `frontend/src/pages/EventsPage.tsx` | 4 | ⬜ |
| 76 | `frontend/src/pages/FamilyCreatePage.tsx` | 4 | ⬜ |
| 77 | `frontend/src/pages/FollowUpRequestsPage.tsx` | 4 | ⬜ |
| 78 | `frontend/src/pages/PeopleDirectoryPage.tsx` | 4 | ✅ `canRegister` servi sur la clé du garde serveur `POST /people/register` (`PEOPLE_EXECUTE` {ADMIN, PASTEUR}) — conversion MANQUANTE, débusquée par le ratchet phase 8 |
| 79 | `frontend/src/pages/PrayersPage.tsx` | 4 | ⬜ |
| 80 | `frontend/src/pages/VisitsPage.tsx` | 4 | ⬜ |
| 81 | `frontend/src/__tests__/ChurchJoinChain.test.tsx` | 3 | ⬜ |
| 82 | `frontend/src/__tests__/CrmFaiseurPage.test.tsx` | 3 | ⬜ |
| 83 | `frontend/src/__tests__/EventsPage.test.tsx` | 3 | ⬜ |
| 84 | `frontend/src/__tests__/FamilyReportPage.test.tsx` | 3 | ⬜ |
| 85 | `frontend/src/__tests__/PlatformPagesPage.test.tsx` | 3 | ⬜ |
| 86 | `frontend/src/__tests__/ProfilePageGdpr.test.tsx` | 3 | ⬜ |
| 87 | `frontend/src/__tests__/TenantAdminDashboard.test.tsx` | 3 | ⬜ |
| 88 | `frontend/src/__tests__/TwoFactorChallengePage.test.tsx` | 3 | ⬜ |
| 89 | `frontend/src/__tests__/auditLabels.test.ts` | 3 | ⬜ |
| 90 | `frontend/src/pages/GivingPage.tsx` | 3 | ⬜ |
| 91 | `frontend/src/pages/PrayerSpacesPage.tsx` | 3 | ⬜ |
| 92 | `frontend/src/pages/TontinePage.tsx` | 3 | ⬜ |
| 93 | `frontend/src/pages/ai/AiDashboardPage.tsx` | 3 | ⬜ |
| 94 | `frontend/src/components/pages/PageBlockRenderer.tsx` | 3 | ⬜ |
| 95 | `frontend/src/components/pasteur/PasteurDepartmentsTab.tsx` | 3 | ⬜ |
| 96 | `frontend/src/lib/labels.ts` | 2 | ⬜ |
| 97 | `frontend/src/__tests__/AdminCustomFieldsPage.test.tsx` | 2 | ⬜ |
| 98 | `frontend/src/__tests__/CustomPageView.test.tsx` | 2 | ⬜ |
| 99 | `frontend/src/__tests__/DashboardPage.test.tsx` | 2 | ⬜ |
| 100 | `frontend/src/__tests__/SpaceOsPage.test.tsx` | 2 | ⬜ |
| 101 | `frontend/src/__tests__/TenantContextSwitch.test.tsx` | 2 | ⬜ |
| 102 | `frontend/src/__tests__/UserDetailModalHierarchy.test.tsx` | 2 | ⬜ |
| 103 | `frontend/src/pages/AcceptInvitationPage.tsx` | 2 | ⬜ |
| 104 | `frontend/src/pages/DepartmentDetailPage.tsx` | 2 | ⬜ |
| 105 | `frontend/src/pages/DepartmentsPage.tsx` | 2 | ⬜ |
| 106 | `frontend/src/pages/IntelligentSearchPage.tsx` | 2 | ⬜ |
| 107 | `frontend/src/pages/MarketplacePage.tsx` | 2 | ⬜ |
| 108 | `frontend/src/pages/Pastoral360Page.tsx` | 2 | ⬜ |
| 109 | `frontend/src/pages/RegisterPage.tsx` | 2 | ⬜ |
| 110 | `frontend/src/pages/ReportsPage.tsx` | 2 | ⬜ |
| 111 | `frontend/src/pages/SoulDetailPage.tsx` | 2 | ⬜ |
| 112 | `frontend/src/pages/SpaceImportExportPage.tsx` | 2 | ⬜ |
| 113 | `frontend/src/pages/TenantAdminMembersPage.tsx` | 2 | ⬜ |
| 114 | `frontend/src/pages/TenantOwnershipPage.tsx` | 2 | ⬜ |
| 115 | `frontend/src/pages/TrainingsPage.tsx` | 2 | ⬜ |
| 116 | `frontend/src/pages/TransferDetailPage.tsx` | 2 | ⬜ |
| 117 | `frontend/src/pages/WeeklyProgramPage.tsx` | 2 | ⬜ |
| 118 | `frontend/src/components/admin/SystemHealthWidget.tsx` | 2 | ⬜ |
| 119 | `frontend/src/components/relations/MyRelationsCard.tsx` | 2 | ⬜ |
| 120 | `frontend/src/components/departments/AssignmentsTab.tsx` | 2 | ⬜ |
| 121 | `frontend/src/components/common/CommandPalette.tsx` | 2 | 🟨 (canAccessPath ×3 — variantes tests/useKeyboardShortcuts) |
| 122 | `frontend/src/components/pasteur/PasteurAuditTab.tsx` | 2 | ⬜ |
| 123 | `frontend/src/components/pasteur/PasteurSoulsTab.tsx` | 2 | ⬜ |
| 124 | `frontend/src/lib/auditLabels.ts` | 1 | ⬜ |
| 125 | `frontend/src/__tests__/FamilyOsPanel.test.tsx` | 1 | ⬜ |
| 126 | `frontend/src/__tests__/Pastoral360Page.test.tsx` | 1 | ⬜ |
| 127 | `frontend/src/hooks/useCustomFieldForm.ts` | 1 | ⬜ |
| 128 | `frontend/src/hooks/useRealtimeSync.ts` | 1 | ⬜ |
| 129 | `frontend/src/pages/AdminDashboardPage.tsx` | 1 | ⬜ |
| 130 | `frontend/src/pages/AdminIntegrationsPage.tsx` | 1 | ⬜ |
| 131 | `frontend/src/pages/AdminSystemPage.tsx` | 1 | ⬜ |
| 132 | `frontend/src/pages/AuditPage.tsx` | 1 | ⬜ |
| 133 | `frontend/src/pages/ChefFamilleDashboardPage.tsx` | 1 | ✅ `canManage` servi (V270 `UI_DASHBOARD_CHEF_FAMILLE_MANAGE_ACCESS` {CHEF_DE_FAMILLE}) |
| 134 | `frontend/src/pages/DepartmentMemberDossierPage.tsx` | 1 | ⬜ |
| 135 | `frontend/src/pages/DocumentsPage.tsx` | 1 | ⬜ |
| 136 | `frontend/src/pages/PrayerJournalPage.tsx` | 1 | ⬜ |
| 137 | `frontend/src/pages/SoulCreatePage.tsx` | 1 | ⬜ |
| 138 | `frontend/src/pages/SuccessionPage.tsx` | 1 | ⬜ |
| 139 | `frontend/src/components/departments/ChecklistsTab.tsx` | 1 | ⬜ |
| 140 | `frontend/src/components/departments/types.ts` | 1 | ⬜ |
| 141 | `frontend/src/components/onboarding/steps/RolesStep.tsx` | 1 | ⬜ |
| 142 | `frontend/src/components/family/FamilyOsPanel.tsx` | 1 | ⬜ |
| 143 | `frontend/src/components/pasteur/PasteurCrmTab.tsx` | 1 | ⬜ |
| 144 | `frontend/src/components/pasteur/PasteurReportsTab.tsx` | 1 | ⬜ |
| 145 | `frontend/src/components/pasteur/PasteurTransfersTab.tsx` | 1 | ⬜ |

## C. Mobile — fichiers avec littéraux de rôle

| # | Fichier | Littéraux | Statut |
|---|---|---|---|
| 1 | `mobile/lib/app.dart` | 901 | ✅ routes/gardes servies (Phase 5 : cache servi `AuthState` + `routePermissions` **91 clés** miroir exact du web — chaque clé ajoutée seulement si littéral rôles mobile ≡ matrice serveur ; garde GoRouter priorité au servi ; `_routeRoles` repli). Littéraux intra-écrans résiduels |
| 2 | `mobile/lib/presentation/screens/users/users_list_screen.dart` | 50 | ✅ `hardDelete` désormais gardé par la clé du garde serveur (`USERS_EXECUTE_HARDDELETE` {ADMIN}) : c'était la SEULE action de la rangée sans garde, visible par le seul filtre de rôle de la LIGNE — donc visible à un PASTEUR qui prenait un 403. Les littéraux `role == ...` restants sont des sélecteurs de CIBLE (rôle de la personne affichée), par design |
| 3 | `mobile/lib/presentation/widgets/app_drawer.dart` | 37 | 🟨 (menu filtré par clés servies + écoute permissionsEpoch + re-pull au switch-role) |
| 4 | `mobile/lib/presentation/screens/users/user_detail_screen.dart` | 21 | ⬜ |
| 5 | `mobile/lib/presentation/screens/users/user_create_screen.dart` | 20 | ⬜ |
| 6 | `mobile/lib/presentation/screens/communications/communications_screen.dart` | 10 | ⬜ |
| 7 | `mobile/lib/presentation/screens/transfers/transfer_create_screen.dart` | 9 | ⬜ |
| 8 | `mobile/lib/presentation/screens/transfers/transfer_admin_screen.dart` | 8 | ⬜ |
| 9 | `mobile/lib/presentation/screens/dashboard/dashboard_screen.dart` | 8 | ⬜ |
| 10 | `mobile/lib/presentation/screens/platform/platform_menus_screen.dart` | 7 | ⬜ |
| 11 | `mobile/lib/features/users/UserRolesScreen.dart` | 6 | ⬜ |
| 12 | `mobile/lib/presentation/screens/tontine/tontine_screen.dart` | 6 | ⬜ |
| 13 | `mobile/lib/presentation/screens/reports/reports_screen.dart` | 6 | ✅ portée « tous les départements » servie (V270 `UI_REPORTS_DEPARTMENTS_GLOBAL_SCOPE_ACCESS` {ADMIN, PASTEUR} — clé dédiée, `DEPARTMENTS_EXECUTE` aurait élargi à RESPONSABLE) |
| 14 | `mobile/lib/presentation/screens/users/permissions_screen.dart` | 6 | ⬜ |
| 15 | `mobile/lib/presentation/screens/tenant/users_screen.dart` | 5 | ⬜ |
| 16 | `mobile/lib/l10n/app_localizations.dart` | 4 | ⬜ |
| 17 | `mobile/lib/presentation/screens/trainings/trainings_screen.dart` | 4 | ⬜ |
| 18 | `mobile/lib/presentation/screens/members/member_requests_screen.dart` | 4 | ⬜ |
| 19 | `mobile/lib/features/streaming/services/streaming_service.dart` | 3 | ⬜ |
| 20 | `mobile/lib/features/streaming/screens/stream_detail_screen.dart` | 3 | ⬜ |
| 21 | `mobile/lib/presentation/screens/follow_up_requests/follow_up_requests_screen.dart` | 3 | ⬜ |
| 22 | `mobile/lib/presentation/screens/passport/passport_screen.dart` | 3 | ⬜ |
| 23 | `mobile/lib/presentation/screens/departments/department_management_screen.dart` | 3 | ⬜ |
| 24 | `mobile/lib/presentation/screens/pastoral/pastoral_ministry_screen.dart` | 3 | ⬜ |
| 25 | `mobile/lib/presentation/screens/family_space/family_space_screen.dart` | 3 | ⬜ |
| 26 | `mobile/lib/presentation/screens/face_checkin/face_checkin_screen.dart` | 3 | ⬜ |
| 27 | `mobile/lib/presentation/screens/dashboard/member_dashboard_screen.dart` | 3 | ⬜ |
| 28 | `mobile/lib/features/import/SpaceConfigTransferScreen.dart` | 2 | ⬜ |
| 29 | `mobile/lib/data/services/invitation_admin_service.dart` | 2 | ⬜ |
| 30 | `mobile/lib/data/services/relation_service.dart` | 2 | ⬜ |
| 31 | `mobile/lib/presentation/screens/main_shell.dart` | 2 | ⬜ |
| 32 | `mobile/lib/presentation/screens/departments/department_create_screen.dart` | 2 | ⬜ |
| 33 | `mobile/lib/presentation/screens/departments/department_detail_screen.dart` | 2 | ⬜ |
| 34 | `mobile/lib/presentation/screens/login/register_screen.dart` | 2 | ⬜ |
| 35 | `mobile/lib/presentation/screens/souls/pastoral_360_screen.dart` | 2 | ⬜ |
| 36 | `mobile/lib/presentation/screens/evaluations/evaluations_screen.dart` | 2 | ⬜ |
| 37 | `mobile/lib/presentation/screens/finances/finance_screen.dart` | 2 | ⬜ |
| 38 | `mobile/lib/features/messages/screens/conversations_screen.dart` | 1 | ⬜ |
| 39 | `mobile/lib/presentation/screens/prayers/prayers_list_screen.dart` | 1 | ⬜ |
| 40 | `mobile/lib/presentation/screens/departments/department_member_dossier_screen.dart` | 1 | ⬜ |
| 41 | `mobile/lib/presentation/screens/departments/department_report_screen.dart` | 1 | ⬜ |
| 42 | `mobile/lib/presentation/screens/departments/department_tools_screen.dart` | 1 | ⬜ |
| 43 | `mobile/lib/presentation/screens/departments/departments_list_screen.dart` | 1 | ⬜ |
| 44 | `mobile/lib/presentation/screens/tenant/onboarding_step_forms.dart` | 1 | ⬜ |
| 45 | `mobile/lib/presentation/screens/tenant/tenant_selection_screen.dart` | 1 | ⬜ |
| 46 | `mobile/lib/presentation/screens/profile/my_relations_card.dart` | 1 | ⬜ |
| 47 | `mobile/lib/presentation/screens/profile/profile_screen.dart` | 1 | ⬜ |
| 48 | `mobile/lib/presentation/screens/families/family_create_screen.dart` | 1 | ⬜ |
| 49 | `mobile/lib/presentation/screens/souls/soul_create_screen.dart` | 1 | ⬜ |
| 50 | `mobile/lib/presentation/screens/users/audit_screen.dart` | 1 | ⬜ |
| 51 | `mobile/lib/presentation/screens/platform/admin_custom_fields_screen.dart` | 1 | ⬜ |
| 52 | `mobile/lib/presentation/screens/platform/platform_modules_screen.dart` | 1 | ⬜ |
| 53 | `mobile/lib/presentation/screens/platform/platform_pages_screen.dart` | 1 | ⬜ |
| 54 | `mobile/lib/core/tenant_session.dart` | 1 | ⬜ |

## D. Avancement par phase (mise à jour à chaque PR)

| Phase | Lot | Sites concernés | Statut |
|---|---|---|---|
| 0 | Ratchet R7 + ce ledger | — (outillage) | ✅ |
| 1 | Catalogue V258 + matrice par défaut + template | toutes les clés cibles | ✅ (V258 installée, chaîne PG16 verte 11/11, test équivalence 12/12 `AuthorizationServiceEquivalenceBridgeTest`) |
| 2 | Tranche de référence MEMBERS | sites `modules/members` + FE membres + mobile | ✅ fullstack (BE : 11/11 gardes → `@authz.can`, R7 581→570, suite 2285 verte ; client : `GET /me/permissions` rendu miroir EXACT du pont — `PermissionResolver` +5e source `permissionKeysForActiveAuthorities`, gate 77/77 ; FE : `ROUTE_PERMISSIONS`/`canAccessPath` + ProtectedRoute servie, vitest 1023/1023 + verrou `routeAccessPermissions.test.ts` 7/7 ; mobile : cache servi `AuthState.refreshServedPermissions`, garde GoRouter + drawer filtrés, flutter 682/682 + verrou `served_permissions_mobile_test.dart` 9/9) |
| 3 | Conversion BE roulante (par module) | 570 sites → objectif 0 | ✅ (547 conversions mécaniques + 23 guidées = 570 ; R7 570→0 ; V259 : 21 clés nouvelles + 4 rôles globaux + 9 paires mortes réactivées + héritage one-shot global→surcouche locale ; gates : ArchitectureRulesTest 1/1, EquivalenceBridge 15/15, PermissionResolverPastorate 4/4, chaîne PG16 11/11 jusqu'à v259) |
| 4 | Conversion FE | 2298 occurrences → objectif 0 | ✅ **routes + garde servies** (registre `ROUTE_PERMISSIONS` 173 clés + `App.tsx` 185 props `permissions=` served-first, repli `roles` conservé ; V261 : 113 clés UI + 395 paires ; verrous Java `TestPermissionMatrixSeedGuardTest` + FE `routeAccessPermissions.test.ts`) ; gates : tsc 0 err, vitest sériel 1024/1024, seed guard 1/1, chaîne PG16 verte v261, suite BE H2 2299/0/0. **Résiduel intra-pages ✅ (21 fichiers)** : gardes boutons/sections converties en `useServedAccess().can('CLÉ', legacy)` served-first (miroir exact ProtectedRoute) ; V262 mint 12 clés UI + 32 paires ≡ littéraux legacy, 20 sites réutilisent des clés existantes dont la matrice servie ≡ le littéral (inventaire `.e2e-tmp/param/reuse_matrix.txt`) ; les sélecteurs de contexte-workspace (`activeRole` → vue, cible d'une ligne de données) restent littéraux PAR DESIGN —PermissionResolver agrège tous les rôles, la conversion élargirait l'UI au-delà du workspace choisi |
| 5 | Conversion mobile | 1176 occurrences → objectif 0 | ✅ **routes + tiroir servies** (`routePermissions` 91 clés miroir web, équivalence rôles mobile ≡ matrice prouvée contre `fe_route_map.json` ; garde GoRouter + drawer filtrés par clés servies, repli `_routeRoles`) ; gates : flutter analyze 528 lints (= base, 0 nouvelle err), flutter test 684/684. **Résiduel intra-écrans ✅ (9 fichiers)** : `AuthState.canServed(clé, {legacy})` (sémantique du garde GoRouter L1674) posé sur trainings/communications/users/dashboard/reports/app_drawer/tontine/passport ; matrices ≡ littéraux vérifiées dans le seed avant conversion |
| 6 | Paramétrage non-autorisation (statuts, workflows, quotas) | audit séparé | ✅ **audit + branchement** : (1) *workflows transferts* — déjà paramétrables par église (row `workflow_configs` V98, `rolesInitiateurs`/`rolesValidateurs` en chaînes libres comparées au rôle actif ; le `['PASTEUR']` des services n'est qu'un défaut à la création, pas une garde) ; (2) *quotas* — déjà servies par plan (`QuotaService`/`SaasPlanService`, fail-closed `QUOTA_*`, API quotas dans TenantContext + mobile tenant_session) ; (3) *vocabulaires de rôles figés* — branchés sur le catalogue SERVI (`GET /admin/roles/overview`, rôles custom inclus) en **union additive** : FE `useRoleVocabulary` (CommunicationsPage cible ROLE, AdminWorkflowBuilderPage initiateurs/validateurs, AdminCustomFieldsPage lecture/écriture, PlatformMenusPage rôles visibles) + mobile `AuthState().rolesVocabulary` (transfer_admin, platform_menus, communications) avec cache `_servedRoleKeys` amorce/refresh/ràz-session comme les permissions ; (4) *ciblage par rôle custom* — `CommunicationService.targetUserIds` résout désormais les clés hors enum par les memberships ACTIFS du tenant (additif : hors contexte tenant = comportement historique ignorer ; filtre actif-non-supprimé identique à la cible TOUS) + 2 tests unitaires ; (5) *contrats enum restants* (documentés, non modifiables sans migrer le contrat JPA — **migrés en phase 7**, voir ligne 7) : `User.role/roles` (enum `UserRole`) → création d'utilisateur FE/mobile bornée à l'enum — l'attribution de rôles custom passe déjà par le catalogue (`/admin/roles`, invitations via `useAssignableRoles`, `TenantAdminMembersPage` servi) ; `Objective.role` (enum) → select ObjectivesPage borné ; statuts d'entités (`Communication.statut`, `UserStatus`, etc.) = machines à états de contrat, traduits à l'affichage, non des listes de réglage ; (6) **re-gel R3 délibéré** — l'arête `objet|R3|communications -> tenants` a été ajoutée explicitement au gel (`architecture-freeze.txt`) : le ciblage par rôle custom impose que `CommunicationService` lise les `TenantMembership` (repo du contexte tenants), précédent : ~30 modules déjà gelés avec la même arête (events, families, messages, notifications, passport, payments, people, platform…) ; garde : `ArchitectureRulesTest` 1/1 vert puis suite BE complète verte |
| 7 | Contrats enum restants (P7a/P7b) | `Objective.role` (ciblage objectifs + progression) + création `User` (rôle custom) | ✅ **contrats terminés sur les deux joints documentés** — ciblage d'objectif et affectation à la création acceptent les clés du catalogue servi ; les champs de compte de base `role`/`roles`/`activeRole` et leurs endpoints (switch-role, addRole/replaceRoles, filtre `?role=`, import CSV) restent l'enum socle **par design** (cf ligne 6, point (5)) ; rejet d'une clé custom sur ces endpoints = erreur de validation polie (400 try/catch ou handler), jamais 500 observé côté UI puisque les sélecteurs bornés ne les exposent pas. (P7a) `objectives.role` enum→clé texte : V263 (`VARCHAR(50)` + CHECK sémantique liste fermée → motif syntaxique `^[A-Z][A-Z0-9_]*$`, les 8 objectifs par défaut V22 restent conformes), entité/repo/DTO en `String`, `ObjectiveService.requireKnownRoleKey` = **union additive** noms enum ∪ catalogue servi du tenant (rejet 4xx `INVALID_/UNKNOWN_OBJECTIVE_ROLE`, plus de 500), `activeRole()` en `String` + `parseLegacyRole` — l'ancien `UserRole.valueOf(getCurrentUserRole())` non gardé (null → NPE, clé hors enum → 500) est neutralisé ; le JWT ne portant aujourd'hui que des rôles socle, c'est un **durcissement préventif**, pas un plantage observé en production, `resolveScope` garde la sémantique historique par rôle socle et clé custom → périmètre restreint (aucun élargissement de vue) ; FE : types `role: string` + `ObjectivesPage` `useRoleVocabulary(ROLE_BASE)` ; mobile objectives : déjà agnostique du rôle (0 modif). (P7b) `CreateUserRequest.customRole` (clé facultative) → `UserController.applyCustomRoleMembership` : clé du catalogue → entité `Role` → `TenantMembership` ACTIVE portée TENANT créée si le nouvel utilisateur n'en a aucune (UK V135 `user_id,tenant_id` jamais dropping = une membership par user/tenant ; mise à jour sinon) → permissions effectives via `PermissionResolver` source #1 + `bumpPermissions(USER_CREATE_CUSTOM_ROLE)` ; FE `UsersPage` sélecteur rôle custom (`useRoleVocabulary`, union additive) + mobile `user_create_screen` (`AuthState().rolesVocabulary`, sélecteur masqué tant que le catalogue est vide). **Re-gel R3 délibéré** `objet|R3|objectives -> tenants` (ObjectiveService lit `RoleRepository`, précédent ~30 modules avec l'arête vers tenants) ; 3 tests unitaires dans `ObjectiveServiceTest` + 3 tests API `UserControllerCustomRoleTest` (couverture P7b : membership ACTIVE créée + bump, clé inconnue → 400 `UNKNOWN_CUSTOM_ROLE`, sans customRole → comportement historique strict). Gates : suite BE complète 2307/0/0 (13 skipped, dont les 3 tests API P7b), tsc 0 err, vitest 1024/1024, debt:audit 13 (= plafond), flutter analyze 0 err (529 lints), flutter test 684/684, chaîne PG16 11/11 jusqu'à v263 |
| 8 | **Audit profond + ratchets CI** (interne) | ré-audit des 3 surfaces + gaps de CI | ✅ **constats** : (1) `DressCodePage` consommait `DRESS_CODE_CREATE|UPDATE|ARCHIVE`, clés **absentes du catalogue** — les 3 boutons n'ont jamais été rendus, pour aucun rôle (défaut né avec le module G3.4, jamais vu par un test) ; (2) 6 gardes d'UI encore figées sur un littéral de rôle actif (`MemberDashboardPage`, `ResponsableDashboardPage`, `ChefFamilleDashboardPage`, `DashboardPage` ×2 blocs, `MemberRequestsPage`) ; (3) `PeopleDirectoryPage.canRegister` non converti ; (4) mobile `hardDelete` non gardé (visible à un PASTEUR qui prenait un 403) ; (5) `reports_screen` portée départements figée alors que la garde voisine était servie. **Vérifié SANS correction** : R7 backend à 0 (616 `@authz.can`, 0 littéral en `@PreAuthorize`) ; **0 clé consommée hors catalogue** côté registre ; **0 divergence de matrice** sur les 173 routes et les 29 gardes intra-pages ; **miroir mobile/web exact sur les 79 routes communes** ; `/me/permissions` + invalidation temps réel (`useRealtimeBus` → `refreshContext`, mobile `permissionsEpoch`) en place. **Correctifs** : V270 (7 clés UI, matrices parité exacte des littéraux remplacés), suppression définitive branchée sur `DRESSCODE_EXECUTE_DELETE`, `useServedAccess` aligné sur la sémantique de `ProtectedRoute`/`canServed` (divergence documentée supprimée). **Ratchets ajoutés** : `frontend/src/__tests__/parametrisationGate.test.ts` (8 tests — existence des clés servies lues dans les MIGRATIONS, parité de matrice routes + intra-pages, liste blanche du résiduel + plafond chiffré 6) et `mobile/test/served_permissions_parity_test.dart` (5 tests, résiduel 1). Le ratchet a débusqué (3) dès sa première exécution. Gates : tsc 0 err, vitest 1032/1032, test Java du seed H2 vert, `served_permissions_parity_test` 5/5 |

## E. Méthodologie de génération (reproductible)

```bash
# sites BE (détection commentaire/ligne suivante = niveau classe vs méthode)
grep -rn '@PreAuthorize' backend/src/main/java --include='*.java' | grep -E 'hasAnyRole\(|hasRole\('
# occurrences FE / mobile : mots-clés rôles (ADMIN|PASTEUR|RESPONSABLE|…|MEMBRE) aux mots-frontières
```
Ce ledger est régénéré en début de chaque phase ; les lignes migrées sont marquées ✅ puis
le plafond R7 est abaissé (`mvn -o test -Dtest=ArchitectureRulesTest -Darchitecture.freeze.update=true`).

