import type { UserRole } from '@/types';

/* ============================================================================
 * REGISTRE CENTRAL DES ACCÈS PAR RÔLE
 * --------------------------------------------------------------------------
 * Source de vérité unique partagée par :
 *  - ProtectedRoute (App.tsx) : contrôle d'accès aux routes ;
 *  - Sidebar : filtrage des menus configurables (backend) selon le rôle actif,
 *    afin qu'aucun menu visible ne puisse renvoyer vers un espace interdit.
 *
 * Une route absente de ce registre est accessible à tout utilisateur
 * authentifié (comportement de <ProtectedRoute> sans prop `roles`).
 * ========================================================================== */

export type AccessRole = UserRole | string;

/* ============================================================================
 * PARAMÉTRISATION PAR ÉGLISE (Phase 2-client) — ACCÈS PAR PERMISSIONS SERVIES
 * --------------------------------------------------------------------------
 * Tranche MEMBERS : pour les endpoints backend gardés par
 * `@authz.can('<CLÉ>')`, les routes correspondantes sont gardées côté client par
 * les CLÉS de permissions SERVIES (`GET /api/v1/me/permissions`, miroir exact du
 * serveur depuis la Phase 2-client — pont d'autorités actives inclus). Une
 * église qui redonne une clé à un rôle (surcouche tenant de la matrice V258)
 * remet l'UI à jour sans redéploiement : plus rien n'est figé.
 *
 * Les littéraux `ROUTE_ROLES` restent le REPLI de transition : route non
 * convertie, contexte tenant pas encore chargé, ou test monté hors TenantProvider.
 * Le serveur demeure seul juge : l'UI ne fait qu'aligner les boutons.
 * ========================================================================== */

/** Routes converties : motif de chemin → clés permission servant la garde (any-of). */
export const ROUTE_PERMISSIONS: Record<string, string[]> = {
  // Tranche MEMBERS (Phase 2) — clés du catalogue V258, miroir des gardes `@authz.can`.
  '/members/requests': ['MEMBERS_EXECUTE'],         // inbox · statuts · présences scopées
  '/member-competences': ['MEMBERS_EXECUTE'],       // appariement des compétences
  '/qr-checkin': ['MEMBERS_EXECUTE_QRCHECKIN'],     // pointage QR (garde la plus large de l'écran)
  '/geofencing': ['MEMBERS_EXECUTE_R4A'],           // garde de classe GeofencingController
  // ——— Phase 4 : conversions served-first (matrice V258/V259 ≡ littéraux) ———
  '/dashboard/membre/activities': ["UI_DASHBOARD_MEMBRE_ACTIVITIES_ACCESS"],
  '/dashboard/membre': ["UI_DASHBOARD_MEMBRE_ACCESS"],
  '/dashboard/pasteur': ["DASHBOARD_EXECUTE"],
  '/dashboard/chef-famille': ["DASHBOARD_EXECUTE_GETCHEFFAMILLEDASHBOARD"],
  '/dashboard/responsable': ["DASHBOARD_EXECUTE_GETRESPONSABLEDASHBOARD"],
  '/souls/retractions': ["SOULS_EXECUTE_DELETE"],
  '/souls/new': ["SOULS_EXECUTE"],
  '/souls/:id/pastoral-360': ["SOULS_EXECUTE"],
  '/souls/:id/edit': ["SOULS_EXECUTE"],
  '/souls/:id': ["SOULS_EXECUTE"],
  '/souls': ["SOULS_EXECUTE"],
  '/families/compare': ["FAMILIES_EXECUTE_DELETE"],
  '/families/new': ["FAMILIES_EXECUTE_CREATE"],
  '/families/:id/faiseur-performance': ["FAMILIES_EXECUTE_R4A"],
  '/families/:id': ["FAMILIES_EXECUTE_R5A"],
  '/families': ["FAMILIES_EXECUTE_R5A"],
  '/departments/:id/report': ["DEPARTMENTS_EXECUTE"],
  '/departments/:id/manage': ["DEPARTMENTS_EXECUTE"],
  '/departments/:id/members/:memberId': ["DEPARTMENTS_EXECUTE"],
  '/departments/:id/stats': ["DEPARTMENTS_EXECUTE"],
  '/departments/:id/tools': ["DEPARTMENTS_EXECUTE"],
  '/departments/:id': ["DEPARTMENTS_EXECUTE"],
  '/organization': ["UI_ORGANIZATION_ACCESS"],
  '/pastoral': ["UI_PASTORAL_ACCESS"],
  '/spaces/:id': ["UI_SPACES_ACCESS"],
  '/departments': ["DEPARTMENTS_EXECUTE"],
  '/users': ["USERS_EXECUTE_CREATE"],
  '/skills-matrix': ["UI_SKILLS_MATRIX_ACCESS"],
  '/team-gantt': ["UI_TEAM_GANTT_ACCESS"],
  '/transfers': ["TRANSFERS_EXECUTE_CANCEL"],
  '/transfers/new': ["TRANSFERS_EXECUTE_CANCEL"],
  '/transfers/:id': ["TRANSFERS_EXECUTE_CANCEL"],
  '/surveys': ["UI_SURVEYS_ACCESS"],
  '/tickets': ["UI_TICKETS_ACCESS"],
  '/quest': ["UI_QUEST_ACCESS"],
  '/pastoral-visits': ["UI_PASTORAL_VISITS_ACCESS"],
  '/reports/maker': ["UI_REPORTS_MAKER_ACCESS"],
  '/reports/family': ["REPORTS_EXECUTE_GETMAKERREPORT"],
  '/reports/urgent-aid': ["REPORTS_EXECUTE_GETURGENTAIDREQUESTS"],
  '/discipline': ["UI_DISCIPLINE_ACCESS"],
  '/crm/faiseur': ["UI_CRM_FAISEUR_ACCESS"],
  '/cercle-faiseurs': ["UI_CERCLE_FAISEURS_ACCESS"],
  '/admin/dashboard': ["ADMIN_EXECUTE"],
  '/admin/members': ["ADMIN_EXECUTE"],
  '/admin/roles': ["ADMIN_EXECUTE"],
  '/admin/invitations': ["ADMIN_EXECUTE"],
  '/admin/spaces': ["ADMIN_EXECUTE"],
  '/tenant/settings': ["UI_TENANT_SETTINGS_ACCESS"],
  '/tenant/modules': ["UI_TENANT_MODULES_ACCESS"],
  '/tenant/join-management': ["UI_TENANT_JOIN_MANAGEMENT_ACCESS"],
  '/tenant/announcements': ["UI_TENANT_ANNOUNCEMENTS_ACCESS"],
  '/tenant/ownership': ["UI_TENANT_OWNERSHIP_ACCESS"],
  '/tenant/organization': ["UI_TENANT_ORGANIZATION_ACCESS"],
  '/tenant/organization/levels': ["UI_TENANT_ORGANIZATION_LEVELS_ACCESS"],
  '/tenant/organization/nodes/:nodeId': ["UI_TENANT_ORGANIZATION_NODES_ACCESS"],
  '/tenant/members/:userId/roles': ["UI_TENANT_MEMBERS_ROLES_ACCESS"],
  '/tenant/customization': ["UI_TENANT_CUSTOMIZATION_ACCESS"],
  '/admin/payments': ["ADMIN_EXECUTE"],
  '/admin/webhook-logs': ["ADMIN_EXECUTE"],
  '/admin/webhooks': ["ADMIN_EXECUTE"],
  '/admin/whatsapp': ["ADMIN_EXECUTE"],
  '/admin/connectors': ["ADMIN_EXECUTE"],
  '/admin/transfers': ["ADMIN_EXECUTE"],
  '/admin/settings': ["ADMIN_EXECUTE"],
  '/admin/modules': ["ADMIN_EXECUTE"],
  '/admin/menus': ["ADMIN_EXECUTE"],
  '/admin/pages': ["ADMIN_EXECUTE"],
  '/admin/custom-fields': ["ADMIN_EXECUTE"],
  '/admin/feedback': ["ADMIN_EXECUTE"],
  '/admin/dictionaries': ["ADMIN_EXECUTE"],
  '/admin/tenants': ["ADMIN_EXECUTE"],
  '/admin/notifications': ["ADMIN_EXECUTE"],
  '/admin/dress-codes': ["ADMIN_EXECUTE"],
  '/admin/system': ["ADMIN_EXECUTE"],
  '/admin/integrations': ["ADMIN_EXECUTE"],
  '/admin/gdpr': ["ADMIN_EXECUTE"],
  '/admin/compliance': ["ADMIN_EXECUTE"],
  '/admin': ["ADMIN_EXECUTE"],
  '/finances': ["UI_FINANCES_ACCESS"],
  '/health-observatory': ["UI_HEALTH_OBSERVATORY_ACCESS"],
  '/digital-twin': ["UI_DIGITAL_TWIN_ACCESS"],
  '/sermon-assistant': ["UI_SERMON_ASSISTANT_ACCESS"],
  '/events/program': ["EVENTS_EXECUTE_DELETEEVENT"],
  '/programs': ["PROGRAMS_EXECUTE_CREATE"],
  '/events/statistics': ["EVENTS_EXECUTE_DELETEEVENT"],
  '/prayers/spaces': ["UI_PRAYERS_SPACES_ACCESS"],
  '/audit': ["AUDIT_EXECUTE"],
  '/permissions': ["UI_PERMISSIONS_ACCESS"],
  '/transfer': ["TRANSFERS_EXECUTE"],
  '/api-docs': ["UI_API_DOCS_ACCESS"],
  '/kingdom-map': ["UI_KINGDOM_MAP_ACCESS"],
  '/search': ["SEARCH_EXECUTE"],
  '/parallel-followups': ["PARALLELFOLLOWUPS_EXECUTE_R5A"],
  '/alerts': ["ALERTS_EXECUTE"],
  '/documents': ["UI_DOCUMENTS_ACCESS"],
  '/map': ["UI_MAP_ACCESS"],
  '/evangelism': ["EVANGELISM_EXECUTE"],
  '/objectives': ["OBJECTIVES_EXECUTE_MYPROGRESS"],
  '/visits': ["UI_VISITS_ACCESS"],
  '/prophetic-journal': ["UI_PROPHETIC_JOURNAL_ACCESS"],
  '/voice-reports': ["VOICEREPORTS_EXECUTE"],
  '/evaluations': ["EVALUATIONS_EXECUTE_GETALL"],
  '/prayers/actions-de-grace': ["UI_PRAYERS_ACTIONS_DE_GRACE_ACCESS"],
  '/prayers': ["UI_PRAYERS_ACCESS"],
  '/events': ["EVENTS_EXECUTE_R6A"],
  '/communications': ["UI_COMMUNICATIONS_ACCESS"],
  '/badges': ["BADGES_EXECUTE"],
  '/giving': ["UI_GIVING_ACCESS"],
  '/tontines': ["TONTINE_EXECUTE"],
  '/trainings': ["TRAININGS_EXECUTE"],
  '/appointments': ["APPOINTMENTS_EXECUTE"],
  '/currency-settings': ["UI_CURRENCY_SETTINGS_ACCESS"],
  '/streaming': ["STREAMING_EXECUTE"],
  '/broadcast': ["UI_BROADCAST_ACCESS"],
  '/inventory': ["INVENTORY_EXECUTE"],
  '/department-kpis': ["DEPARTMENTKPI_EXECUTE"],
  '/rewards': ["UI_REWARDS_ACCESS"],
  '/marketplace': ["UI_MARKETPLACE_ACCESS"],
  '/community': ["UI_COMMUNITY_ACCESS"],
  '/network': ["UI_NETWORK_ACCESS"],
  '/passport': ["UI_PASSPORT_ACCESS"],
  '/ai-predictions': ["UI_AI_PREDICTIONS_ACCESS"],
  '/ai-dashboard': ["UI_AI_PREDICTIONS_ACCESS"],
  '/ai-assistant': ["UI_AI_ASSISTANT_2_ACCESS"],
  '/voice-assistant': ["UI_VOICE_ASSISTANT_2_ACCESS"],
  '/exports': ["UI_EXPORTS_ACCESS"],
  '/import': ["UI_IMPORT_ACCESS"],
  '/admin/import': ["ADMIN_EXECUTE"],
  '/admin/space-import-export': ["ADMIN_EXECUTE"],
  '/admin/legacy-migration': ["ADMIN_EXECUTE"],
  '/admin-requests': ["ADMINREQUESTS_EXECUTE"],
  '/admin/sync-conflicts': ["UI_ADMIN_SYNC_CONFLICTS_ACCESS"],
  '/emergency-aid': ["UI_EMERGENCY_AID_ACCESS"],
  '/aid-exchange': ["UI_AID_EXCHANGE_ACCESS"],
  '/2fa-status': ["UI_2FA_STATUS_ACCESS"],
  '/visit-notes/verify': ["UI_VISIT_NOTES_VERIFY_ACCESS"],
  '/ai-health': ["UI_AI_HEALTH_ACCESS"],
  '/ai-predictions/risks': ["UI_AI_PREDICTIONS_RISKS_ACCESS"],
  '/team-tasks': ["UI_TEAM_TASKS_ACCESS"],
  '/sermon-translations': ["UI_SERMON_TRANSLATIONS_ACCESS"],
  '/kpi-narrative': ["KPINARRATIVE_EXECUTE"],
  '/maker-tracking': ["UI_MAKER_TRACKING_ACCESS"],
  '/network/stats': ["NETWORK_EXECUTE"],
  '/voices': ["UI_VOICES_ACCESS"],
  '/workflow': ["WORKFLOW_EXECUTE_CREATE"],
  '/skill-matching': ["UI_SKILL_MATCHING_ACCESS"],
  '/face-recognition': ["UI_FACE_RECOGNITION_ACCESS"],
  '/twin-snapshot': ["UI_TWIN_SNAPSHOT_ACCESS"],
  '/reward-certificates': ["UI_REWARD_CERTIFICATES_ACCESS"],
  '/sermons': ["UI_SERMONS_ACCESS"],
  '/development-plans': ["UI_DEVELOPMENT_PLANS_ACCESS"],
  '/surveys/:id': ["UI_SURVEYS_2_ACCESS"],
  '/forms/:id/responses': ["FORMS_EXECUTE"],
  '/calendar-ical': ["UI_CALENDAR_ICAL_ACCESS"],
  '/department-reports': ["UI_DEPARTMENT_REPORTS_ACCESS"],
  '/family-tree': ["UI_FAMILY_TREE_ACCESS"],
  '/announcement-schedule': ["UI_ANNOUNCEMENT_SCHEDULE_ACCESS"],
  '/documents/:id': ["UI_DOCUMENTS_ACCESS"],
  '/tickets/:id': ["UI_TICKETS_2_ACCESS"],
  '/testimonies/:id': ["UI_TESTIMONIES_ACCESS"],
  '/rewards/claims': ["UI_REWARDS_CLAIMS_ACCESS"],
  // Phase 9 — /users/roles n'est plus qu'une redirection de compatibilité vers
  // /users (l'écran UserRolesPage appelait des endpoints inexistants et un
  // énuméré fermé de six rôles). Le verrou est aligné sur celui de /users, que
  // la garde de route applique et que le menu utilise pour masquer l'entrée.
  '/users/roles': ["USERS_EXECUTE_CREATE"],
  // Phase 9 — UserRolesPage réparée avec le paramètre :id (nouvelle route).
  '/users/:id/roles': ["USERS_EXECUTE"],
  '/admin/compliance-exports': ["UI_ADMIN_COMPLIANCE_EXPORTS_ACCESS"],
  '/conversations': ["UI_CONVERSATIONS_ACCESS"],
  '/courses': ["UI_COURSES_ACCESS"],
  '/equipment': ["UI_EQUIPMENT_ACCESS"],
  '/budgets': ["UI_BUDGETS_ACCESS"],
  '/discipleship': ["UI_DISCIPLESHIP_ACCESS"],
  '/tasks': ["TASKS_EXECUTE_CREATE"],
  '/health': ["HEALTH_EXECUTE_R4A"],
  '/finance-modules': ["UI_FINANCE_MODULES_ACCESS"],
  // V271 — écran « Archives » (consultation des snapshots d'événements).
  '/archives': ["UI_ARCHIVES_ACCESS"],
};

/** Routes restreintes : motif de chemin (segments `:param` dynamiques) → rôles autorisés. */
export const ROUTE_ROLES: Record<string, AccessRole[]> = {
  // Tableaux de bord
  '/dashboard/membre/activities': ['MEMBRE'],
  '/dashboard/membre': ['MEMBRE'],
  '/dashboard/pasteur': ['ADMIN', 'PASTEUR'],
  '/dashboard/chef-famille': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/dashboard/responsable': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],

  // Âmes & familles
  '/souls/retractions': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/souls/new': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls/:id/pastoral-360': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls/:id/edit': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/souls': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families/compare': ['ADMIN', 'PASTEUR'],
  '/families/new': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/families/:id/faiseur-performance': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],

  // Départements & RH
  '/departments/:id/report': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/manage': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/members/:memberId': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/stats': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/tools': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/organization': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  // §G4.3 — pastorat : lecture bergers/administration, écritures PASTOR_PRINCIPAL/ADMIN.
  '/pastoral': ['ADMIN', 'PASTEUR', 'PASTOR_PRINCIPAL'],
  '/spaces/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/departments': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/users': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/skills-matrix': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/team-gantt': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/transfers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/transfers/new': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/transfers/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/surveys': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/tickets': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/quest': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/pastoral-visits': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'],

  // Rapports & suivi
  '/reports/maker': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/reports/family': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/reports/urgent-aid': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/discipline': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/members/requests': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],

  // Espaces métiers
  '/crm/faiseur': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/cercle-faiseurs': ['ADMIN', 'PASTEUR', 'FAISEUR'],

  // Administration — console tenant admin (G5.5 §58, parité garde ProtectedRoute scope="tenant")
  '/admin/dashboard': ['ADMIN', 'PASTEUR'],
  '/admin/members': ['ADMIN', 'PASTEUR'],
  '/admin/roles': ['ADMIN', 'PASTEUR'],
  '/admin/invitations': ['ADMIN', 'PASTEUR'],
  '/admin/spaces': ['ADMIN', 'PASTEUR'],
  '/tenant/settings': ['ADMIN', 'PASTEUR'],
  '/tenant/modules': ['ADMIN', 'PASTEUR'],
  '/tenant/join-management': ['ADMIN', 'PASTEUR'],
  '/tenant/announcements': ['ADMIN', 'PASTEUR'],
  // T-W7 : écran Propriété & délégation (F13). La garde de ROUTE est ici
  // alignée sur ADMIN/PASTEUR ; la garde de MÉTIER (qui peut céder la
  // propriété) reste côté serveur : `@authz.isTenantOwner()`.
  '/tenant/ownership': ['ADMIN', 'PASTEUR'],
  // T-W5 : réseau d'une dénomination (§1.3) — organigramme racine + enfants.
  '/tenant/organization': ['ADMIN', 'PASTEUR'],
  // SPEC_ORGANISATION_MODULABLE_V3 §6 / T-W15 — écrans « modulable » scopés tenant.
  // Le super-admin ne doit PAS joindre /tenant/organization/* (redirection V2).
  '/tenant/organization/levels': ['ADMIN', 'PASTEUR'],
  '/tenant/organization/nodes/:nodeId': ['ADMIN', 'PASTEUR'],
  '/tenant/members/:userId/roles': ['ADMIN', 'PASTEUR'],
  // LOT 2 §GR + §LB — écran « tout est paramétrable ».
  '/tenant/customization': ['ADMIN', 'PASTEUR'],

  // Administration
  '/admin/payments': ['ADMIN', 'PASTEUR'],
  '/admin/webhook-logs': ['ADMIN', 'PASTEUR'],
  '/admin/webhooks': ['ADMIN', 'PASTEUR'],
  '/admin/whatsapp': ['ADMIN', 'PASTEUR'],
  '/admin/connectors': ['ADMIN', 'PASTEUR'],
  '/admin/transfers': ['ADMIN', 'PASTEUR'],
  '/admin/settings': ['ADMIN', 'PASTEUR'],
  '/admin/modules': ['ADMIN', 'PASTEUR'],
  '/admin/menus': ['ADMIN', 'PASTEUR'],
  '/admin/pages': ['ADMIN', 'PASTEUR'],
  '/admin/custom-fields': ['ADMIN', 'PASTEUR'],
  '/admin/feedback': ['ADMIN', 'PASTEUR'],
  '/admin/dictionaries': ['ADMIN', 'PASTEUR'],
  '/admin/tenants': ['ADMIN', 'PASTEUR'],
  '/admin/notifications': ['ADMIN', 'PASTEUR'],
  '/admin/dress-codes': ['ADMIN', 'PASTEUR'],
  '/admin/system': ['ADMIN', 'PASTEUR'],
  '/admin/integrations': ['ADMIN', 'PASTEUR'],
  '/admin/gdpr': ['ADMIN', 'PASTEUR'],
  '/admin/compliance': ['ADMIN', 'PASTEUR'],
  '/admin': ['ADMIN', 'PASTEUR'],
  '/finances': ['ADMIN', 'PASTEUR'],
  '/health-observatory': ['ADMIN', 'PASTEUR'],
  '/digital-twin': ['ADMIN', 'PASTEUR'],
  '/sermon-assistant': ['ADMIN', 'PASTEUR'],
  '/events/program': ['ADMIN', 'PASTEUR'],
  '/programs': ['ADMIN', 'PASTEUR'],
  '/events/statistics': ['ADMIN', 'PASTEUR'],
  '/prayers/spaces': ['ADMIN', 'PASTEUR'],
  '/audit': ['ADMIN', 'PASTEUR'],
  '/permissions': ['ADMIN', 'PASTEUR'],
  // T-W6 (F6) : le membre passe d'une église à une autre SANS se réinscrire.
  // Accessible à tout membre connecté : la décision transfert/adhésion est
  // prise côté backend selon la racine (`root_tenant_id`), pas ici.
  '/transfer': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/api-docs': ['ADMIN', 'PASTEUR'],
  '/kingdom-map': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],

  // Modules accessibles à tous les rôles opérationnels
  '/search': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/parallel-followups': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/alerts': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/documents': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/map': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/evangelism': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/objectives': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/visits': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/prophetic-journal': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/voice-reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/evaluations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],

  // Modules ouverts à tous les rôles (opérationnels + membre)
  '/prayers/actions-de-grace': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/prayers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/events': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/communications': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/badges': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/giving': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/tontines': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/trainings': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/appointments': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],

  // Nouvelles modules P1/P3
  '/currency-settings': ['ADMIN'],
  '/streaming': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/broadcast': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/inventory': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/department-kpis': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/rewards': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/marketplace': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/community': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/network': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/passport': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/ai-predictions': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  // /ai-dashboard adossé à la MÊME capacité servie (UI_AI_PREDICTIONS_ACCESS) que
  // /ai-predictions : le catalogue servi accorde RESPONSABLE, donc le repli legacy
  // doit le refléter (parité B1 — sinon matrice servie ≠ littéral). Le gating de
  // production passe par la clé servie (RESPONSABLE déjà admis) ; ceci aligne
  // seulement le repli pré-cache, n'élargit ni ne retire aucun accès réel.
  '/ai-dashboard': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],

  // IA, Voice, Exports, Imports - accessibles à tous les rôles
  '/ai-assistant': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/voice-assistant': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/exports': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/import': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/admin/import': ['ADMIN', 'PASTEUR'],
  '/admin/space-import-export': ['ADMIN', 'PASTEUR'],
  '/admin/legacy-migration': ['ADMIN', 'PASTEUR'],

  // Nouveaux modules issus de l'audit des endpoints orphelins
  '/admin-requests': ['ADMIN', 'PASTEUR'],
  '/admin/sync-conflicts': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/emergency-aid': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/aid-exchange': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/2fa-status': ['ADMIN', 'PASTEUR'],
  '/visit-notes/verify': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/ai-health': ['ADMIN', 'PASTEUR'],
  '/ai-predictions/risks': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/team-tasks': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/sermon-translations': ['ADMIN', 'PASTEUR'],
  '/kpi-narrative': ['ADMIN', 'PASTEUR'],
  '/maker-tracking': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/member-competences': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/qr-checkin': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/network/stats': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/voices': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/workflow': ['ADMIN', 'PASTEUR'],
  '/skill-matching': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/face-recognition': ['ADMIN', 'PASTEUR'],
  '/twin-snapshot': ['ADMIN', 'PASTEUR'],
  '/reward-certificates': ['ADMIN', 'PASTEUR'],
  '/sermons': ['ADMIN', 'PASTEUR'],
  '/development-plans': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/surveys/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/forms/:id/responses': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/calendar-ical': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/department-reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/family-tree': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/announcement-schedule': ['ADMIN', 'PASTEUR'],
  '/documents/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/tickets/:id': ['ADMIN', 'PASTEUR'],
  '/testimonies/:id': ['ADMIN', 'PASTEUR'],
  '/rewards/claims': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  // Phase 9 — parité avec '/users' : la route ne fait plus que rediriger vers lui.
  '/users/roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  // Phase 9 — UserRolesPage avec paramètre :id : accessible ADMIN/PASTEUR.
  '/users/:id/roles': ['ADMIN', 'PASTEUR'],
  '/admin/compliance-exports': ['ADMIN'],
  '/conversations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/courses': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/equipment': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/budgets': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  // V233-V236 — pages web des contrats services (mêmes gardes que App.tsx).
  '/discipleship': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/tasks': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/health': ['ADMIN', 'PASTEUR', 'HEALTH_STAFF', 'HEALTH_LEAD'],
  '/finance-modules': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  // V271 — parité avec la matrice servie de UI_ARCHIVES_ACCESS ; V272 élargit
  // cette matrice à 6 rôles (= les détenteurs de la garde serveur
  // EVENTS_EXECUTE_R6A2 sur /church-events) : + DEPARTMENT_LEADER, FAMILY_LEADER.
  // La liste ci-dessous suit : 6 rôles côté UI = 6 rôles côté R6A2, parité exacte.
  '/archives': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'DEPARTMENT_LEADER', 'FAMILY_LEADER'],
};

function splitSegments(path: string): string[] {
  if (!path) return [];
  return path.split('?')[0].split('#')[0].split('/').filter(Boolean);
}

function matchesPattern(patternSegments: string[], pathSegments: string[]): boolean {
  if (patternSegments.length !== pathSegments.length) return false;
  return patternSegments.every(
    (seg, i) => seg.startsWith(':') || seg === pathSegments[i]
  );
}

/**
 * Rôles autorisés pour un chemin donné.
 * Retourne `null` si la route n'est pas restreinte (accessible à tous).
 */
function bestMatch<T>(registry: Record<string, T>, path: string): T | null {
  const pathSegs = splitSegments(path);
  let bestMatchKey: string | null = null;
  let bestLen = -1;
  for (const pattern of Object.keys(registry)) {
    const patSegs = splitSegments(pattern);
    if (patSegs.length > bestLen && matchesPattern(patSegs, pathSegs)) {
      bestMatchKey = pattern;
      bestLen = patSegs.length;
    }
  }
  return bestMatchKey !== null ? registry[bestMatchKey] : null;
}

export function rolesForPath(path: string): AccessRole[] | null {
  return bestMatch(ROUTE_ROLES, path);
}

/** Clés permission servant une route convertie ; `null` si la route reste par rôle. */
export function permissionsForPath(path: string): string[] | null {
  return bestMatch(ROUTE_PERMISSIONS, path);
}

/**
 * Vérifie qu'un rôle actif peut accéder à un chemin.
 * `true` si la route n'est pas restreinte ou si le rôle est autorisé.
 * Un ADMIN dispose des accès PASTEUR (équivalence métier).
 */
export function canRoleAccessPath(path: string, role: string | null | undefined): boolean {
  if (!role) return true;
  const allowed = rolesForPath(path);
  if (!allowed) return true;
  if (allowed.includes(role)) return true;
  if (role === 'ADMIN' && allowed.includes('PASTEUR')) return true;
  return false;
}

/** Filtre une liste d'items de navigation {href} selon le rôle actif. */
export function filterNavByRole<T extends { href: string }>(items: T[], role: string | null | undefined): T[] {
  return items.filter((item) => canRoleAccessPath(item.href, role));
}

/**
 * PARAMÉTRISATION PAR ÉGLISE (Phase 2-client) — accès d'abord par CLÉ SERVIE.
 * `hasPermission` est la lecture du cache de permissions servies (TenantContext) :
 * une route convertie est accordée/refusée par ses clés, sans littéral de rôle.
 * Sans checker (contexte non chargé, test hors provider) ou sans route convertie,
 * repli strict sur le registre par rôle — comportement transition identique.
 */
export function canAccessPath(
  path: string,
  role: string | null | undefined,
  hasPermission?: ((key: string) => boolean) | undefined,
): boolean {
  const keys = permissionsForPath(path);
  if (keys && hasPermission) return keys.some((k) => hasPermission(k));
  return canRoleAccessPath(path, role);
}

/** Filtre de navigation par clé servie, avec repli par rôle (voir {@link canAccessPath}). */
export function filterNavByAccess<T extends { href: string }>(
  items: T[],
  role: string | null | undefined,
  hasPermission?: ((key: string) => boolean) | undefined,
): T[] {
  return items.filter((item) => canAccessPath(item.href, role, hasPermission));
}
