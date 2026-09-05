import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'presentation/screens/ai_assistant/ai_assistant_screen.dart';
import 'presentation/screens/ai_predictions/ai_predictions_screen.dart';
import 'presentation/screens/login/login_screen.dart';
import 'presentation/screens/login/register_screen.dart';
import 'presentation/screens/main_scaffold.dart';
import 'presentation/screens/dashboard/pasteur_dashboard_screen.dart';
import 'presentation/screens/dashboard/chef_famille_dashboard_screen.dart';
import 'presentation/screens/dashboard/responsable_dashboard_screen.dart';
import 'presentation/screens/dashboard/member_dashboard_screen.dart';
import 'presentation/screens/souls/souls_list_screen.dart';
import 'presentation/screens/souls/soul_detail_screen.dart';
import 'presentation/screens/reports/maker_report_screen.dart';
import 'presentation/screens/reports/family_report_screen.dart';
import 'presentation/screens/reports/reports_screen.dart';
import 'presentation/screens/reports/report_pdf_viewer_screen.dart';
import 'presentation/screens/families/families_list_screen.dart';
import 'presentation/screens/alerts/alerts_list_screen.dart';
import 'presentation/screens/alerts/smart_alerts_screen.dart';
import 'presentation/screens/notifications/notifications_screen.dart';
import 'presentation/screens/profile/profile_screen.dart';
import 'presentation/screens/prayers/prayers_list_screen.dart';
import 'presentation/screens/events/events_list_screen.dart';
import 'presentation/screens/departments/departments_list_screen.dart';
import 'presentation/screens/departments/department_detail_screen.dart';
import 'presentation/screens/departments/department_report_screen.dart';
import 'presentation/screens/departments/department_management_screen.dart';
import 'presentation/screens/departments/department_stats_screen.dart';
import 'presentation/screens/departments/department_member_dossier_screen.dart';
import 'presentation/screens/departments/department_tools_screen.dart';
import 'presentation/screens/evaluations/evaluations_screen.dart';
import 'presentation/screens/search/search_screen.dart';
import 'presentation/screens/souls/crm_faiseur_screen.dart';
import 'presentation/screens/souls/pastoral_360_screen.dart';
import 'presentation/screens/users/users_list_screen.dart';
import 'presentation/screens/users/user_detail_screen.dart';
import 'presentation/screens/users/permissions_screen.dart';
import 'presentation/screens/users/documents_screen.dart';
import 'presentation/screens/users/audit_screen.dart';
import 'presentation/screens/appointments/appointments_screen.dart';
import 'presentation/screens/visits/visits_screen.dart';
import 'presentation/screens/evangelism/evangelism_screen.dart';
import 'presentation/screens/objectives/objectives_screen.dart';
import 'presentation/screens/badges/badges_screen.dart';
import 'presentation/screens/quest/quest_screen.dart';
import 'presentation/screens/giving/giving_screen.dart';
import 'presentation/screens/tontine/tontine_screen.dart';
import 'presentation/screens/voice_reports/voice_report_screen.dart';
import 'presentation/screens/face_checkin/face_checkin_screen.dart';
import 'presentation/screens/ar_onboarding/ar_onboarding_screen.dart';
import 'presentation/screens/prophetic_journal/prophetic_journal_screen.dart';
import 'presentation/screens/trainings/trainings_screen.dart';
import 'presentation/screens/trainings/sermon_transcription_screen.dart';
import 'presentation/screens/messages/messages_screen.dart';
import 'presentation/screens/messages/video_conference_screen.dart';
import 'presentation/screens/parallel_followups/parallel_followups_screen.dart';
import 'presentation/screens/map/map_screen.dart';
import 'presentation/screens/map/soul_map_screen.dart';
import 'presentation/screens/members/member_requests_screen.dart';
import 'presentation/screens/transfers/transfers_list_screen.dart';
import 'presentation/screens/transfers/transfer_detail_screen.dart';
import 'presentation/screens/transfers/transfer_create_screen.dart';
import 'presentation/screens/transfers/transfer_admin_screen.dart';
import 'presentation/screens/platform/platform_modules_screen.dart';
import 'presentation/screens/platform/platform_menus_screen.dart';
import 'presentation/screens/platform/platform_pages_screen.dart';
import 'presentation/screens/finances/finance_screen.dart';
import 'presentation/screens/communications/communications_screen.dart';
import 'presentation/screens/onboarding/onboarding_screen.dart';
import 'presentation/screens/security/security_settings_screen.dart';
import 'presentation/screens/not_found_screen.dart';
import 'presentation/screens/platform/admin_settings_screen.dart';
import 'presentation/screens/platform/admin_custom_fields_screen.dart';
import 'presentation/screens/platform/admin_dictionaries_screen.dart';
import 'presentation/screens/platform/admin_integrations_screen.dart';
import 'presentation/screens/platform/admin_tenants_screen.dart';
import 'presentation/screens/admin/benchmark_screen.dart';
import 'presentation/screens/admin/admin_payment_dashboard_screen.dart';
import 'presentation/screens/admin/webhook_log_screen.dart';
import 'presentation/screens/inventory/inventory_screen.dart';
import 'presentation/screens/souls/soul_qr_screen.dart';
import 'presentation/screens/departments/qr_scanner_screen.dart';
import 'presentation/screens/departments/geofencing_screen.dart';
import 'presentation/screens/departments/presence_entry_screen.dart';
import 'presentation/screens/discipline/discipline_screen.dart';
import 'presentation/screens/prayers/actions_de_grace_screen.dart';
import 'presentation/screens/dashboard/member_activities_screen.dart';
import 'presentation/screens/dashboard/bi_dashboard_screen.dart';

import 'presentation/screens/security/mobile_security_settings_screen.dart';
import 'presentation/screens/forms/forms_screen.dart';
import 'presentation/screens/sermon_translations/sermon_translation_screen.dart';
import 'presentation/screens/spiritual_journal/spiritual_journal_screen.dart';
import 'presentation/screens/admin_requests/admin_requests_screen.dart';
import 'presentation/screens/dev_plans/dev_plan_screen.dart';
import 'presentation/screens/maker_tracking/maker_tracking_screen.dart';
import 'presentation/screens/growth_projection/growth_projection_screen.dart';
import 'presentation/screens/church_comparison/church_comparison_screen.dart';
import 'presentation/screens/volunteers/volunteers_screen.dart';
import 'presentation/screens/dashboard/load_prediction_screen.dart';
import 'presentation/screens/neighborhood_health/neighborhood_health_screen.dart';
import 'presentation/screens/sabbath_dashboard/sabbath_dashboard_screen.dart';
import 'presentation/screens/my_team/my_team_family_screen.dart';
import 'presentation/screens/follow_up_requests/follow_up_requests_screen.dart';
import 'presentation/screens/skill_matching/skill_matching_screen.dart';
import 'presentation/screens/dashboard/executive_insights_screen.dart';
import 'presentation/screens/ai_visit_notes/ai_visit_notes_screen.dart';
import 'presentation/screens/predictions/predictions_screen.dart';
import 'presentation/screens/predictions/health_observatory_screen.dart';
import 'presentation/screens/intelligence/intelligence_center_screen.dart';
import 'presentation/screens/intelligence/digital_twin_screen.dart';
import 'presentation/screens/engagement/analytics_screen.dart';
import 'presentation/screens/succession/succession_screen.dart';
import 'presentation/screens/family_meeting/family_meeting_screen.dart';
import 'presentation/screens/family_cohesion/family_cohesion_screen.dart';
import 'presentation/screens/family_resources/family_resources_screen.dart';
import 'presentation/screens/kpi_narrative/kpi_narrative_screen.dart';
import 'presentation/screens/bible_reading/bible_reading_screen.dart';
import 'presentation/screens/prayers/prayer_journal_screen.dart';
import 'presentation/screens/spiritual_challenges/spiritual_challenges_screen.dart';
import 'presentation/screens/reverse_mentoring/reverse_mentoring_screen.dart';
import 'presentation/screens/personal_objectives/personal_objectives_screen.dart';
import 'presentation/screens/directory/church_directory_screen.dart';
import 'presentation/screens/event_checklist/event_checklist_screen.dart';
import 'presentation/screens/skills_matrix/skills_matrix_screen.dart';
import 'presentation/screens/group_messages/group_messages_screen.dart';
import 'presentation/screens/broadcast/broadcast_screen.dart';
import 'presentation/screens/network/network_screen.dart';
import 'presentation/screens/gantt/team_gantt_screen.dart';
import 'presentation/screens/dashboard/discipleship_path_screen.dart';
import 'presentation/screens/dashboard/weekly_challenges_screen.dart';
import 'presentation/screens/voice_assistant/voice_assistant_screen.dart';
import 'presentation/screens/compliance/compliance_manager_screen.dart';
import 'presentation/screens/whatsapp/whatsapp_reminders_screen.dart';
import 'presentation/screens/data_migration/data_migration_screen.dart';
import 'presentation/screens/usage_analytics/usage_analytics_screen.dart';
import 'presentation/screens/encouragements/encouragements_screen.dart';
import 'presentation/screens/dashboard/moderation_screen.dart';
import 'presentation/screens/rewards/rewards_screen.dart';
import 'presentation/screens/calendar/calendar_screen.dart';
import 'presentation/screens/community/community_screen.dart';
import 'presentation/screens/leave_requests/leave_requests_screen.dart';
import 'presentation/screens/marketplace/marketplace_screen.dart';
import 'presentation/screens/streaming/streaming_screen.dart';
import 'presentation/screens/surveys/surveys_screen.dart';
import 'presentation/screens/testimonials/testimonials_screen.dart';
import 'presentation/screens/tickets/tickets_screen.dart';
import 'presentation/screens/messages/conversation_detail_screen.dart';
import 'presentation/screens/messages/enhanced_conversation_screen.dart';
import 'presentation/screens/dashboard/dashboard_screen.dart';
import 'presentation/screens/dashboard/kpi_drilldown_screen.dart';
import 'presentation/screens/dashboard/mentorat_ai_screen.dart';
import 'presentation/screens/dashboard/scheduled_announcements_screen.dart';
// ===== Feature-screen imports (routes referencing lib/features/*) =====
import 'features/aid/EmergencyAidScreen.dart';
import 'features/aid/AidExchangeScreen.dart';
import 'features/auth/TwoFactorStatusScreen.dart';
import 'features/ai/AiHealthScreen.dart';
import 'features/ai/PredictionsRiskScreen.dart';
import 'features/tasks/TeamTasksScreen.dart';
import 'features/sermons/SermonTranslationsScreen.dart';
import 'features/import/ImportDataScreen.dart';
import 'features/skills/MemberCompetencesScreen.dart';
import 'features/checkin/QrCheckinScreen.dart';
import 'features/network/NetworkStatsScreen.dart';
import 'features/voice/VoicesScreen.dart';
import 'features/workflow/WorkflowScreen.dart';
import 'features/face/FaceRecognitionScreen.dart';
import 'features/twin/TwinSnapshotScreen.dart';
import 'features/rewards/RewardCertificatesScreen.dart';
import 'features/sermons/SermonsScreen.dart';
import 'features/plans/DevelopmentPlansScreen.dart';
import 'features/forms/FormResponsesScreen.dart';
import 'features/departments/DepartmentReportsScreen.dart';
import 'features/families/FamilyTreeScreen.dart';
import 'features/announcements/AnnouncementScheduleScreen.dart';
import 'features/documents/DocumentDetailScreen.dart';
import 'features/tickets/TicketDetailScreen.dart';
import 'features/testimonies/TestimonyDetailScreen.dart';
import 'features/rewards/RewardsClaimsScreen.dart';
import 'features/users/UserRolesScreen.dart';
import 'features/compliance/ComplianceExportsScreen.dart';
import 'features/discipleship/DiscipleshipPathsScreen.dart';
import 'features/conversations/ConversationsScreen.dart';
import 'features/families/AiChatScreen.dart';
import 'features/visits/VisitNotesVerifyScreen.dart';
import 'features/encouragements/EncouragementDetailScreen.dart';
import 'features/referrals/ReferralsStatusScreen.dart';
import 'features/spiritual/SpiritualChallengesDetailScreen.dart';
import 'presentation/screens/cercle_faiseurs/cercle_faiseurs_screen.dart';
import 'presentation/screens/passport/passport_screen.dart';
import 'presentation/screens/spiritual_journey/spiritual_journey_screen.dart';
// Note: SkillMatchingScreen, QuestScreen, GeofencingScreen, MakerTrackingScreen, and FollowUpRequestsScreen
// are already imported from presentation/screens/ above — feature versions share the same class names.
import 'tenant_config.dart';

/// Auth state notifier — singleton that tracks the authenticated user
/// with full multi-role and multi-tenant support (roles + activeRole + orgId).
class AuthState {
  static final AuthState _instance = AuthState._internal();
  factory AuthState() => _instance;
  AuthState._internal();

  bool _isAuthenticated = false;
  String? _userId;
  String? _email;
  String? _userRole;
  List<String> _roles = [];
  String _activeRole = '';
  String? _firstName;
  String? _lastName;
  bool _estChefDeFamille = false;
  String? _familleGereeId;
  String? _orgId;

  bool get isAuthenticated => _isAuthenticated;
  String? get userId => _userId;
  String? get email => _email;
  String? get userRole => _userRole;
  List<String> get roles => _roles;
  String get activeRole => _activeRole;
  String? get firstName => _firstName;
  String? get lastName => _lastName;
  bool get estChefDeFamille => _estChefDeFamille;
  String? get familleGereeId => _familleGereeId;
  String? get orgId => _orgId;
  bool get isMultiTenantActive => _orgId != null && _orgId!.isNotEmpty;

  /// Check if the current user has access to the current tenant/org
  bool hasCurrentTenantAccess() {
    return _orgId != null && _orgId!.isNotEmpty && TenantConfig.isMultiTenantActive;
  }

  void setAuthenticated(bool value, {Map<String, dynamic>? userData}) {
    _isAuthenticated = value;
    if (userData != null) {
      _userId = userData['userId'] as String?;
      _email = userData['email'] as String?;
      _userRole = userData['role'] as String?;
      _roles = userData['roles'] != null
          ? List<String>.from(userData['roles'] as List)
          : (_userRole != null ? [_userRole!] : []);
      _activeRole = userData['activeRole'] as String? ?? _userRole ?? '';
      _firstName = userData['firstName'] as String?;
      _lastName = userData['lastName'] as String?;
      _estChefDeFamille = userData['estChefDeFamille'] as bool? ?? false;
      _familleGereeId = userData['familleGereeId'] as String?;
      _orgId = userData['orgId'] as String?;
      // Persist orgId for tenant-aware API calls
      if (_orgId != null) {
        TenantConfig.setOrgId(_orgId!);
      }
    }
  }

  /// Switch the active role (does NOT call the API ; caller must call POST /auth/switch-role)
  void switchActiveRole(String newRole) {
    if (_roles.contains(newRole)) {
      _activeRole = newRole;
      _userRole = newRole;
    }
  }

  /// Check if the user has ANY of the specified roles (across all roles)
  bool hasAnyRole(List<String> checkRoles) {
    return _roles.any((r) => checkRoles.contains(r));
  }

  /// Check if the active role is one of the specified roles
  bool hasActiveRole(List<String> checkRoles) {
    return checkRoles.contains(_activeRole);
  }

  void logout() {
    _isAuthenticated = false;
    _userId = null;
    _email = null;
    _userRole = null;
    _roles = [];
    _activeRole = '';
    _firstName = null;
    _lastName = null;
    _estChefDeFamille = false;
    _familleGereeId = null;
    _orgId = null;
    // Clear tenant config on logout
    TenantConfig.clearOrgId();
  }
}

/// Tableau de bord racine de chaque espace métier.
/// Un changement de rôle = un changement complet de contexte de travail.
String roleHome(String role) {
  switch (role) {
    case 'FAISEUR':
      return '/crm-faiseur';
    case 'RESPONSABLE':
      return '/dashboard/responsable';
    case 'CHEF_DE_FAMILLE':
      return '/dashboard/chef-famille';      case 'MEMBRE':
      return '/dashboard/membre';
    default:
      return '/dashboard'; // ADMIN, PASTEUR
  }
}

/// List of roles allowed per route.
/// null = all authenticated users, [] = no one (public only).
Map<String, List<String>> _routeRoles = {
  '/dashboard': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/dashboard/membre': ['ADMIN', 'PASTEUR', 'MEMBRE'],
  '/dashboard/pasteur': ['ADMIN', 'PASTEUR'],
  '/dashboard/chef-famille': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/dashboard/responsable': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/souls': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/families': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/reports/maker': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/reports/family': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/reports/pdf/consolidated': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/reports/pdf/maker': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/prayers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/events': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/alerts': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/smart-alerts': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/bi-dashboard': ['ADMIN', 'PASTEUR'],
  '/sermons': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/geofencing': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'MEMBRE'],
  '/benchmark': ['ADMIN', 'PASTEUR'],
  '/inventory': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/workflows': ['ADMIN', 'PASTEUR'],
  '/video-conference': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/soul-map': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/profile': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/notifications': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/departments': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/manage': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/stats': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/members/:memberId': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/:id/tools': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/evaluations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/crm-faiseur': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/souls/:id/pastoral-360': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/search': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/users': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/permissions': ['ADMIN'],
  '/discipline': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/prayers/actions-de-grace': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/dashboard/membre/activities': ['ADMIN', 'PASTEUR', 'MEMBRE'],
  '/departments/:id/presences': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/departments/qr-scan': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/souls/:id/qr': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/documents': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/audit': ['ADMIN', 'PASTEUR'],
  '/appointments': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/visits': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/evangelism': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/objectives': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/badges': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/quest': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/giving': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/tontines': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/voice-reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/voice-assistant': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/ai-assistant': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/ai-predictions': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/prophetic-journal': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/compliance': ['ADMIN', 'PASTEUR'],
  '/whatsapp-reminders': ['ADMIN', 'PASTEUR'],
  '/discipleship-path': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/weekly-challenges': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/team-gantt': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/face-checkin': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/trainings': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/communications': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/messages': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/parallel-followups': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/map': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/members/requests': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/transfers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/admin': ['ADMIN', 'PASTEUR'],
  '/finances': ['ADMIN', 'PASTEUR'],
  '/admin/modules': ['ADMIN'],
  '/admin/menus': ['ADMIN'],
  '/admin/pages': ['ADMIN'],
  '/admin/settings': ['ADMIN'],
  '/admin/custom-fields': ['ADMIN'],
  '/admin/dictionaries': ['ADMIN'],
  '/admin/integrations': ['ADMIN'],
  '/admin/tenants': ['ADMIN'],
  '/admin/payments': ['ADMIN', 'PASTEUR'],
  '/admin/webhook-logs': ['ADMIN', 'PASTEUR'],
  '/security-settings': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/onboarding': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/health-observatory': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/digital-twin': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/calendar': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/community': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/leave-requests': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/marketplace': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/streaming': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/surveys': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/testimonials': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/tickets': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/conversation/:conversationId': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/conversation/enhanced/:conversationId': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/dashboard/main': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/kpi-drilldown': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/ai-mentoring/detail': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/predictions-ml': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/scheduled-announcements': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/mobile-security': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/onboarding-ar': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/forms': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/sermon-translations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'MEMBRE'],
  '/spiritual-journal': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/admin-requests': ['ADMIN'],
  '/dev-plans': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/maker-tracking': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/growth-projection': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/church-comparison': ['ADMIN', 'PASTEUR'],
  '/volunteers': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/skill-matching': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/executive-insights': ['ADMIN', 'PASTEUR'],
  '/ai-visit-notes': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/predictions': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/intelligence-center': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/engagement-analytics': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/ai-mentoring': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/succession': ['ADMIN', 'PASTEUR'],
  '/family-meeting': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/family-cohesion': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/family-resources': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/kpi-narrative': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/bible-reading': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/prayer-journal': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/spiritual-challenges': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/reverse-mentoring': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/personal-objectives': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/church-directory': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/event-checklist': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/skills-matrix': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/group-messages': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/broadcast': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/network': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/load-prediction': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/neighborhood-health': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/sabbath-dashboard': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/my-team-family': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/follow-up-requests': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/data-migration': ['ADMIN'],
  '/usage-analytics': ['ADMIN', 'PASTEUR'],
  '/encouragements': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/moderation': ['ADMIN'],
  '/cercle-faiseurs': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/passport': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/spiritual-journey': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/rewards': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/souls/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/transfers/new': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/transfers/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/users/:id': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/departments/:id/report': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  // Nouveaux modules
  '/emergency-aid': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/aid-exchange': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/2fa-status': ['ADMIN', 'PASTEUR'],
  '/ai-health': ['ADMIN', 'PASTEUR'],
  '/ai-predictions/risks': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/team-tasks': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/import-data': ['ADMIN', 'PASTEUR'],
  '/member-competences': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/qr-checkin': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR'],
  '/network/stats': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/voices': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/workflow': ['ADMIN', 'PASTEUR'],
  '/face-recognition': ['ADMIN', 'PASTEUR'],
  '/twin-snapshot': ['ADMIN', 'PASTEUR'],
  '/reward-certificates': ['ADMIN', 'PASTEUR'],
  '/development-plans': ['ADMIN', 'PASTEUR', 'CHEF_DE_FAMILLE'],
  '/department-reports': ['ADMIN', 'PASTEUR', 'RESPONSABLE'],
  '/family-tree': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE'],
  '/announcement-schedule': ['ADMIN', 'PASTEUR'],
  '/rewards/claims': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/user-roles': ['ADMIN'],
  '/compliance-exports': ['ADMIN'],
  '/discipleship-paths': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/visit-notes/verify': ['ADMIN', 'PASTEUR', 'FAISEUR'],
  '/encouragements/detail': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/referrals/status': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR'],
  '/spiritual-challenges/detail': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/conversations': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
  '/ai-chat': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE'],
};

final appRouter = GoRouter(
  initialLocation: '/onboarding',
  redirect: (context, state) async {
    final auth = AuthState();
    final loginRoute = '/login';
    final onboardingRoute = '/onboarding';
    final isLoginRoute = state.matchedLocation == loginRoute;

    // Onboarding — première connexion uniquement (non authentifié).
    // Une fois complété, l'utilisateur est redirigé vers le login.
    if (!auth.isAuthenticated) {
      bool onboardingComplete = false;
      try {
        onboardingComplete = await SharedPreferences.getInstance()
            .then((prefs) => prefs.getBool('onboarding_complete') ?? false);
      } catch (_) {
        // Storage unavailable (tests) — skip onboarding, go to login
      }
      // La visite AR fait partie du parcours de première connexion.
      const arOnboardingRoute = '/onboarding-ar';
      if (!onboardingComplete &&
          state.matchedLocation != onboardingRoute &&
          state.matchedLocation != arOnboardingRoute) {
        return onboardingRoute;
      }
      if (onboardingComplete && state.matchedLocation == onboardingRoute) {
        return loginRoute;
      }
      return null;
    }

    // If authenticated and on login/onboarding page → redirect to dashboard
    if (auth.isAuthenticated && (isLoginRoute || state.matchedLocation == onboardingRoute)) {
      return '/dashboard';
    }

    // Garde par rôle ACTIF — isolation stricte des espaces métiers.
    // Un utilisateur multi-rôles n'accède qu'à l'espace métier courant :
    // changer de rôle change complètement l'application.
    if (auth.isAuthenticated && auth.activeRole.isNotEmpty) {
      final role = auth.activeRole;

      // 1) Chaque route n'est accessible que si le rôle ACTIF est autorisé.
      //    Priorité à la route exacte (ex. /admin/modules réservé ADMIN), puis
      //    repli sur le premier segment (ex. /souls/:id/pastoral-360).
      final segments = state.matchedLocation.split('/');
      final basePath = '/${segments.length > 1 ? segments[1] : ''}';
      final allowedRoles = _routeRoles[state.matchedLocation] ?? _routeRoles[basePath];
      if (allowedRoles != null && !auth.hasActiveRole(allowedRoles)) {
        // L'Admin actif dispose des capacités du Pasteur (super-utilisateurs
        // qui partagent la vue complète de l'application).
        if (!(role == 'ADMIN' && allowedRoles.contains('PASTEUR'))) {
          return roleHome(role); // Ramené vers son propre espace métier
        }
      }

      // 2) Isolation des tableaux de bord : les rôles opérationnels sont
      //    ramenés vers leur propre espace et ne peuvent pas ouvrir le
      //    dashboard d'un autre métier (responsable, chef, faiseur, membre).
      if (!auth.hasActiveRole(['ADMIN', 'PASTEUR'])) {
        final home = roleHome(role);
        final location = state.matchedLocation;
        final otherHomes = ['/dashboard/responsable', '/dashboard/chef-famille', '/crm-faiseur'];
        if (location == '/dashboard' || (otherHomes.contains(location) && location != home)) {
          return home;
        }
      }
    }

    return null; // allow navigation
  },
  routes: [
    GoRoute(
      path: '/onboarding',
      name: 'onboarding',
      builder: (context, state) => const OnboardingScreen(),
    ),
    GoRoute(
      path: '/security-settings',
      name: 'security-settings',
      builder: (context, state) => const SecuritySettingsScreen(),
    ),
    GoRoute(
      path: '/mobile-security',
      name: 'mobile-security',
      builder: (context, state) => const MobileSecuritySettingsScreen(),
    ),
    GoRoute(
      path: '/login',
      name: 'login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/register',
      name: 'register',
      builder: (context, state) => const RegisterScreen(),
    ),
    GoRoute(
      path: '/dashboard',
      name: 'dashboard',
      builder: (context, state) => const MainScaffold(),
    ),
    GoRoute(
      path: '/dashboard/pasteur',
      name: 'pasteur-dashboard',
      builder: (context, state) => const PasteurDashboardScreen(),
    ),
    GoRoute(
      path: '/dashboard/chef-famille',
      name: 'chef-famille-dashboard',
      builder: (context, state) => const ChefFamilleDashboardScreen(),
    ),
    GoRoute(
      path: '/dashboard/responsable',
      name: 'responsable-dashboard',
      builder: (context, state) => const ResponsableDashboardScreen(),
    ),
    GoRoute(
      path: '/dashboard/membre',
      name: 'membre-dashboard',
      builder: (context, state) => const MemberDashboardScreen(),
    ),
    GoRoute(
      path: '/souls',
      name: 'souls',
      builder: (context, state) => SoulsListScreen(
        statutFilter: state.uri.queryParameters['statut'],
        typeFilter: state.uri.queryParameters['typeDisciple'],
      ),
    ),
    GoRoute(
      path: '/souls/:id',
      name: 'soul-detail',
      builder: (context, state) => SoulDetailScreen(
        soulId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/reports',
      name: 'reports',
      builder: (context, state) => const ReportsScreen(),
    ),
    GoRoute(
      path: '/reports/maker',
      name: 'maker-report',
      builder: (context, state) => const MakerReportScreen(),
    ),
    GoRoute(
      path: '/reports/family',
      name: 'family-report',
      builder: (context, state) => const FamilyReportScreen(),
    ),
    GoRoute(
      path: '/reports/pdf/consolidated',
      name: 'report-pdf-consolidated',
      builder: (context, state) => const ReportPdfViewerScreen(reportType: 'consolidated'),
    ),
    GoRoute(
      path: '/reports/pdf/maker',
      name: 'report-pdf-maker',
      builder: (context, state) => const ReportPdfViewerScreen(reportType: 'maker'),
    ),
    GoRoute(
      path: '/families',
      name: 'families',
      builder: (context, state) => const FamiliesListScreen(),
    ),
    GoRoute(
      path: '/alerts',
      name: 'alerts',
      builder: (context, state) => const AlertsListScreen(),
    ),
    GoRoute(
      path: '/smart-alerts',
      name: 'smart-alerts',
      builder: (context, state) => const SmartAlertsScreen(),
    ),
    GoRoute(
      path: '/bi-dashboard',
      name: 'bi-dashboard',
      builder: (context, state) => const BiDashboardScreen(),
    ),
    GoRoute(
      path: '/notifications',
      name: 'notifications',
      builder: (context, state) => const NotificationsScreen(),
    ),
    GoRoute(
      path: '/profile',
      name: 'profile',
      builder: (context, state) => const ProfileScreen(),
    ),
    GoRoute(
      path: '/prayers',
      name: 'prayers',
      builder: (context, state) => const PrayersListScreen(),
    ),
    GoRoute(
      path: '/events',
      name: 'events',
      builder: (context, state) => const EventsListScreen(),
    ),
    GoRoute(
      path: '/departments',
      name: 'departments',
      builder: (context, state) => const DepartmentsListScreen(),
    ),
    GoRoute(
      path: '/departments/:id',
      name: 'department-detail',
      builder: (context, state) => DepartmentDetailScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/report',
      name: 'department-report',
      builder: (context, state) => DepartmentReportScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/manage',
      name: 'department-manage',
      builder: (context, state) => DepartmentManagementScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/stats',
      name: 'department-stats',
      builder: (context, state) => DepartmentStatsScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/members/:memberId',
      name: 'department-member-dossier',
      builder: (context, state) => DepartmentMemberDossierScreen(
        departmentId: state.pathParameters['id']!,
        memberId: state.pathParameters['memberId']!,
      ),
    ),
    GoRoute(
      path: '/departments/:id/tools',
      name: 'department-tools',
      builder: (context, state) => DepartmentToolsScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/evaluations',
      name: 'evaluations',
      builder: (context, state) => const EvaluationsScreen(),
    ),
    GoRoute(
      path: '/crm-faiseur',
      name: 'crm-faiseur',
      builder: (context, state) => const CrmFaiseurScreen(),
    ),
    GoRoute(
      path: '/souls/:id/pastoral-360',
      name: 'soul-pastoral-360',
      builder: (context, state) => Pastoral360Screen(
        soulId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/search',
      name: 'search',
      builder: (context, state) => const SearchScreen(),
    ),
    GoRoute(
      path: '/users',
      name: 'users',
      builder: (context, state) => const UsersListScreen(),
    ),
    GoRoute(
      path: '/users/:id',
      name: 'user-detail',
      builder: (context, state) => UserDetailScreen(
        userId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/permissions',
      name: 'permissions',
      builder: (context, state) => const PermissionsScreen(),
    ),
    GoRoute(
      path: '/documents',
      name: 'documents',
      builder: (context, state) => const DocumentsScreen(),
    ),
    GoRoute(
      path: '/audit',
      name: 'audit',
      builder: (context, state) => const AuditScreen(),
    ),
    GoRoute(
      path: '/appointments',
      name: 'appointments',
      builder: (context, state) => const AppointmentsScreen(),
    ),
    GoRoute(
      path: '/visits',
      name: 'visits',
      builder: (context, state) => const VisitsScreen(),
    ),
    GoRoute(
      path: '/evangelism',
      name: 'evangelism',
      builder: (context, state) => const EvangelismScreen(),
    ),
    GoRoute(
      path: '/objectives',
      name: 'objectives',
      builder: (context, state) => const ObjectivesScreen(),
    ),
    GoRoute(
      path: '/parallel-followups',
      name: 'parallel-followups',
      builder: (context, state) => const ParallelFollowupsScreen(),
    ),
    GoRoute(
      path: '/badges',
      name: 'badges',
      builder: (context, state) => const BadgesScreen(),
    ),
    GoRoute(
      path: '/quest',
      name: 'quest',
      builder: (context, state) => const QuestScreen(),
    ),
    GoRoute(
      path: '/giving',
      name: 'giving',
      builder: (context, state) => const GivingScreen(),
    ),
    GoRoute(
      path: '/tontines',
      name: 'tontines',
      builder: (context, state) => const TontineScreen(),
    ),
    GoRoute(
      path: '/voice-reports',
      name: 'voice-reports',
      builder: (context, state) => const VoiceReportScreen(),
    ),
    GoRoute(
      path: '/face-checkin',
      name: 'face-checkin',
      builder: (context, state) {
        final enroll = state.uri.queryParameters['enroll'] == '1';
        return FaceCheckinScreen(enrollMode: enroll);
      },
    ),
    GoRoute(
      path: '/onboarding-ar',
      name: 'onboarding-ar',
      builder: (context, state) => const ArOnboardingScreen(),
    ),
    GoRoute(
      path: '/trainings',
      name: 'trainings',
      builder: (context, state) => const TrainingsScreen(),
    ),
    GoRoute(
      path: '/sermons',
      name: 'sermons',
      builder: (context, state) => const SermonTranscriptionScreen(),
    ),
    GoRoute(
      path: '/geofencing',
      name: 'geofencing',
      builder: (context, state) => const GeofencingScreen(),
    ),
    GoRoute(
      path: '/messages',
      name: 'messages',
      builder: (context, state) => const MessagesScreen(),
    ),
    GoRoute(
      path: '/video-conference',
      name: 'video-conference',
      builder: (context, state) => const VideoConferenceScreen(),
    ),
    GoRoute(
      path: '/map',
      name: 'map',
      builder: (context, state) => const MapScreen(),
    ),
    GoRoute(
      path: '/soul-map',
      name: 'soul-map',
      builder: (context, state) => const SoulMapScreen(),
    ),
    GoRoute(
      path: '/members/requests',
      name: 'member-requests',
      builder: (context, state) => const MemberRequestsScreen(),
    ),
    GoRoute(
      path: '/transfers',
      name: 'transfers',
      builder: (context, state) => const TransfersListScreen(),
    ),
    GoRoute(
      path: '/transfers/new',
      name: 'transfer-create',
      builder: (context, state) => const TransferCreateScreen(),
    ),
    GoRoute(
      path: '/transfers/:id',
      name: 'transfer-detail',
      builder: (context, state) => TransferDetailScreen(
        transferId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/admin/transfers',
      name: 'transfer-admin',
      builder: (context, state) => const TransferAdminScreen(),
    ),
    GoRoute(
      path: '/admin/modules',
      name: 'platform-modules',
      builder: (context, state) => const PlatformModulesScreen(),
    ),
    GoRoute(
      path: '/admin/menus',
      name: 'platform-menus',
      builder: (context, state) => const PlatformMenusScreen(),
    ),
    GoRoute(
      path: '/admin/pages',
      name: 'platform-pages',
      builder: (context, state) => const PlatformPagesScreen(),
    ),
    GoRoute(
      path: '/admin/settings',
      name: 'admin-settings',
      builder: (context, state) => const AdminSettingsScreen(),
    ),
    GoRoute(
      path: '/admin/custom-fields',
      name: 'admin-custom-fields',
      builder: (context, state) => const AdminCustomFieldsScreen(),
    ),
    GoRoute(
      path: '/admin/dictionaries',
      name: 'admin-dictionaries',
      builder: (context, state) => const AdminDictionariesScreen(),
    ),
    GoRoute(
      path: '/admin/integrations',
      name: 'admin-integrations',
      builder: (context, state) => const AdminIntegrationsScreen(),
    ),
    GoRoute(
      path: '/admin/tenants',
      name: 'admin-tenants',
      builder: (context, state) => const AdminTenantsScreen(),
    ),
    GoRoute(
      path: '/admin/payments',
      name: 'admin-payments',
      builder: (context, state) => const AdminPaymentDashboardScreen(),
    ),
    GoRoute(
      path: '/admin/webhook-logs',
      name: 'admin-webhook-logs',
      builder: (context, state) => const WebhookLogScreen(),
    ),
    GoRoute(
      path: '/benchmark',
      name: 'benchmark',
      builder: (context, state) => const BenchmarkScreen(),
    ),
    GoRoute(
      path: '/inventory',
      name: 'inventory',
      builder: (context, state) => const InventoryScreen(),
    ),
    GoRoute(
      path: '/discipline',
      name: 'discipline',
      builder: (context, state) => const DisciplineScreen(),
    ),
    GoRoute(
      path: '/prayers/actions-de-grace',
      name: 'actions-de-grace',
      builder: (context, state) => const ActionsDeGraceScreen(),
    ),
    GoRoute(
      path: '/dashboard/membre/activities',
      name: 'membre-activities',
      builder: (context, state) => const MemberActivitiesScreen(),
    ),
    GoRoute(
      path: '/departments/qr-scan',
      name: 'qr-scanner',
      builder: (context, state) => const QrScannerScreen(),
    ),
    GoRoute(
      path: '/souls/:id/qr',
      name: 'soul-qr',
      builder: (context, state) => SoulQrScreen(
        soulId: state.pathParameters['id']!,
        soulNom: state.uri.queryParameters['nom'] ?? '',
      ),
    ),
    GoRoute(
      path: '/departments/:id/presences',
      name: 'presence-entry',
      builder: (context, state) => PresenceEntryScreen(
        departmentId: state.pathParameters['id']!,
      ),
    ),
    GoRoute(
      path: '/finances',
      name: 'finances',
      builder: (context, state) => const FinanceScreen(),
    ),
    GoRoute(
      path: '/communications',
      name: 'communications',
      builder: (context, state) => const CommunicationsScreen(),
    ),
    // P1 New screens
    GoRoute(
      path: '/forms',
      name: 'forms',
      builder: (context, state) => const FormsScreen(),
    ),
    GoRoute(
      path: '/sermon-translations',
      name: 'sermon-translations',
      builder: (context, state) => const SermonTranslationScreen(),
    ),
    GoRoute(
      path: '/spiritual-journal',
      name: 'spiritual-journal',
      builder: (context, state) => const SpiritualJournalScreen(),
    ),
    GoRoute(
      path: '/admin-requests',
      name: 'admin-requests',
      builder: (context, state) => const AdminRequestsScreen(),
    ),
    GoRoute(
      path: '/dev-plans',
      name: 'dev-plans',
      builder: (context, state) => const DevelopmentPlanScreen(),
    ),
    GoRoute(
      path: '/maker-tracking',
      name: 'maker-tracking',
      builder: (context, state) => const MakerTrackingScreen(),
    ),
    GoRoute(
      path: '/growth-projection',
      name: 'growth-projection',
      builder: (context, state) => const GrowthProjectionScreen(),
    ),
    GoRoute(
      path: '/church-comparison',
      name: 'church-comparison',
      builder: (context, state) => const ChurchComparisonScreen(),
    ),
    GoRoute(
      path: '/volunteers',
      name: 'volunteers',
      builder: (context, state) => const VolunteersScreen(),
    ),
    GoRoute(
      path: '/skill-matching',
      name: 'skill-matching',
      builder: (context, state) => const SkillMatchingScreen(),
    ),
    GoRoute(path: '/executive-insights', name: 'executive-insights', builder: (ctx, s) => const ExecutiveInsightsScreen()),
    GoRoute(path: '/ai-visit-notes', name: 'ai-visit-notes', builder: (ctx, s) => const AiVisitNotesScreen()),
    GoRoute(path: '/predictions', name: 'predictions', builder: (ctx, s) => const PredictionsScreen()),
    GoRoute(path: '/health-observatory', name: 'health-observatory', builder: (ctx, s) => const HealthObservatoryScreen()),
    GoRoute(path: '/intelligence-center', name: 'intelligence-center', builder: (ctx, s) => const IntelligenceCenterScreen()),
    GoRoute(path: '/digital-twin', name: 'digital-twin', builder: (ctx, s) => const DigitalTwinScreen()),
    GoRoute(path: '/engagement-analytics', name: 'engagement-analytics', builder: (ctx, s) => const EngagementAnalyticsScreen()),
    GoRoute(path: '/ai-mentoring', name: 'ai-mentoring', builder: (ctx, s) => const MentoratAiScreen()),
    GoRoute(path: '/succession', name: 'succession', builder: (ctx, s) => const SuccessionScreen()),
    GoRoute(path: '/family-meeting', name: 'family-meeting', builder: (ctx, s) => const FamilyMeetingScreen()),
    GoRoute(path: '/family-cohesion', name: 'family-cohesion', builder: (ctx, s) => const FamilyCohesionScreen()),
    GoRoute(path: '/family-resources', name: 'family-resources', builder: (ctx, s) => const FamilyResourcesScreen()),
    GoRoute(path: '/kpi-narrative', name: 'kpi-narrative', builder: (ctx, s) => const KpiNarrativeScreen()),
    GoRoute(path: '/bible-reading', name: 'bible-reading', builder: (ctx, s) => const BibleReadingScreen()),
    GoRoute(path: '/prayer-journal', name: 'prayer-journal', builder: (ctx, s) => const PrayerJournalScreen()),
    GoRoute(path: '/spiritual-challenges', name: 'spiritual-challenges', builder: (ctx, s) => const SpiritualChallengesScreen()),
    GoRoute(path: '/reverse-mentoring', name: 'reverse-mentoring', builder: (ctx, s) => const ReverseMentoringScreen()),
    GoRoute(path: '/personal-objectives', name: 'personal-objectives', builder: (ctx, s) => const PersonalObjectivesScreen()),
    GoRoute(path: '/church-directory', name: 'church-directory', builder: (ctx, s) => const ChurchDirectoryScreen()),
    GoRoute(path: '/event-checklist', name: 'event-checklist', builder: (ctx, s) => const EventChecklistScreen()),
    GoRoute(path: '/skills-matrix', name: 'skills-matrix', builder: (ctx, s) => const SkillsMatrixScreen()),
    GoRoute(path: '/group-messages', name: 'group-messages', builder: (ctx, s) => const GroupMessagesScreen()),
    GoRoute(path: '/broadcast', name: 'broadcast', builder: (ctx, s) => const BroadcastScreen()),
    GoRoute(path: '/network', name: 'network', builder: (ctx, s) => const NetworkScreen()),
    // ===== P3 — Innovation / futuriste =====
    GoRoute(path: '/load-prediction', name: 'load-prediction', builder: (ctx, s) => const LoadPredictionScreen()),
    GoRoute(path: '/neighborhood-health', name: 'neighborhood-health', builder: (ctx, s) => const NeighborhoodHealthScreen()),
    GoRoute(path: '/sabbath-dashboard', name: 'sabbath-dashboard', builder: (ctx, s) => const SabbathDashboardScreen()),
    GoRoute(path: '/my-team-family', name: 'my-team-family', builder: (ctx, s) => const MyTeamFamilyScreen()),
    GoRoute(path: '/follow-up-requests', name: 'follow-up-requests', builder: (ctx, s) => const FollowUpRequestsScreen()),
    // P1 routes
    GoRoute(path: '/discipleship-path', name: 'discipleship-path', builder: (ctx, s) => const DiscipleshipPathScreen()),
    GoRoute(path: '/weekly-challenges', name: 'weekly-challenges', builder: (ctx, s) => const WeeklyChallengesScreen()),
    GoRoute(path: '/team-gantt', name: 'team-gantt', builder: (ctx, s) => const TeamGanttScreen()),
    // P0 new screens
    GoRoute(path: '/voice-assistant', name: 'voice-assistant', builder: (ctx, s) => const VoiceAssistantScreen()),
    GoRoute(path: '/ai-assistant', name: 'ai-assistant', builder: (ctx, s) => const AiAssistantScreen()),
    GoRoute(path: '/ai-predictions', name: 'ai-predictions', builder: (ctx, s) => const AiPredictionsScreen()),
    GoRoute(path: '/prophetic-journal', name: 'prophetic-journal', builder: (ctx, s) => const PropheticJournalScreen()),
    GoRoute(path: '/compliance', name: 'compliance', builder: (ctx, s) => const ComplianceManagerScreen()),
    GoRoute(path: '/whatsapp-reminders', name: 'whatsapp-reminders', builder: (ctx, s) => const WhatsAppRemindersScreen()),
    GoRoute(path: '/data-migration', name: 'data-migration', builder: (ctx, s) => const DataMigrationScreen()),
    GoRoute(path: '/usage-analytics', name: 'usage-analytics', builder: (ctx, s) => const UsageAnalyticsScreen()),
    GoRoute(path: '/encouragements', name: 'encouragements', builder: (ctx, s) => const EncouragementsScreen()),
    GoRoute(path: '/moderation', name: 'moderation', builder: (ctx, s) => const ModerationScreen()),
    GoRoute(path: '/rewards', name: 'rewards', builder: (ctx, s) => const RewardsScreen()),
    GoRoute(path: '/calendar', name: 'calendar', builder: (ctx, s) => const CalendarIntegrationScreen()),
    GoRoute(path: '/community', name: 'community', builder: (ctx, s) => const CommunityScreen()),
    GoRoute(path: '/leave-requests', name: 'leave-requests', builder: (ctx, s) => const LeaveRequestsScreen()),
    GoRoute(path: '/marketplace', name: 'marketplace', builder: (ctx, s) => const MarketplaceScreen()),
    GoRoute(path: '/streaming', name: 'streaming', builder: (ctx, s) => const StreamingScreen()),
    GoRoute(path: '/surveys', name: 'surveys', builder: (ctx, s) => const SurveysScreen()),
    GoRoute(path: '/testimonials', name: 'testimonials', builder: (ctx, s) => const TestimonialsScreen()),
    GoRoute(path: '/tickets', name: 'tickets', builder: (ctx, s) => const TicketsScreen()),
    GoRoute(
      path: '/conversation/:conversationId',
      name: 'conversation-detail',
      builder: (ctx, s) => ConversationDetailScreen(
        conversationId: s.pathParameters['conversationId']!,
        title: s.uri.queryParameters['title'] ?? 'Conversation',
      ),
    ),
    GoRoute(
      path: '/conversation/enhanced/:conversationId',
      name: 'conversation-enhanced',
      builder: (ctx, s) => EnhancedConversationScreen(
        conversationId: s.pathParameters['conversationId']!,
        title: s.uri.queryParameters['title'] ?? 'Conversation',
        isGroup: s.uri.queryParameters['isGroup'] == '1',
      ),
    ),
    GoRoute(path: '/dashboard/main', name: 'dashboard-main', builder: (ctx, s) => const DashboardScreen()),
    GoRoute(path: '/kpi-drilldown', name: 'kpi-drilldown', builder: (ctx, s) => const KpiDrilldownScreen()),
    GoRoute(path: '/ai-mentoring/detail', name: 'ai-mentoring-detail', builder: (ctx, s) => const MentoratAiScreen()),
    GoRoute(path: '/predictions-ml', name: 'predictions-ml', builder: (ctx, s) => const AiPredictionsScreen()),
    GoRoute(path: '/scheduled-announcements', name: 'scheduled-announcements', builder: (ctx, s) => const ScheduledAnnouncementsScreen()),
    // ===== Nouveaux écrans issus de l'audit des endpoints orphelins =====
    GoRoute(path: '/admin-requests', name: 'admin-requests', builder: (ctx, s) => const AdminRequestsScreen()),
    GoRoute(path: '/emergency-aid', name: 'emergency-aid', builder: (ctx, s) => const EmergencyAidScreen()),
    GoRoute(path: '/aid-exchange', name: 'aid-exchange', builder: (ctx, s) => const AidExchangeScreen()),
    GoRoute(path: '/2fa-status', name: '2fa-status', builder: (ctx, s) => const TwoFactorStatusScreen()),
    GoRoute(path: '/ai-health', name: 'ai-health', builder: (ctx, s) => const AiHealthScreen()),
    GoRoute(path: '/ai-predictions/risks', name: 'ai-predictions-risks', builder: (ctx, s) => const PredictionsRiskScreen()),
    GoRoute(path: '/team-tasks', name: 'team-tasks', builder: (ctx, s) => const TeamTasksScreen()),
    GoRoute(path: '/sermon-translations', name: 'sermon-translations', builder: (ctx, s) => const SermonTranslationsScreen()),
    GoRoute(path: '/import-data', name: 'import-data', builder: (ctx, s) => const ImportDataScreen()),
    GoRoute(path: '/maker-tracking', name: 'maker-tracking', builder: (ctx, s) => const MakerTrackingScreen()),
    GoRoute(path: '/member-competences', name: 'member-competences', builder: (ctx, s) => const MemberCompetencesScreen()),
    GoRoute(path: '/qr-checkin', name: 'qr-checkin', builder: (ctx, s) => const QrCheckinScreen()),
    GoRoute(path: '/network/stats', name: 'network-stats', builder: (ctx, s) => const NetworkStatsScreen()),
    GoRoute(path: '/voices', name: 'voices', builder: (ctx, s) => const VoicesScreen()),
    GoRoute(path: '/workflow', name: 'workflow', builder: (ctx, s) => const WorkflowScreen()),
    GoRoute(path: '/skill-matching', name: 'skill-matching', builder: (ctx, s) => const SkillMatchingScreen()),
    GoRoute(path: '/face-recognition', name: 'face-recognition', builder: (ctx, s) => const FaceRecognitionScreen()),
    GoRoute(path: '/twin-snapshot', name: 'twin-snapshot', builder: (ctx, s) => const TwinSnapshotScreen()),
    GoRoute(path: '/reward-certificates', name: 'reward-certificates', builder: (ctx, s) => const RewardCertificatesScreen()),
    GoRoute(path: '/sermons-library', name: 'sermons-library', builder: (ctx, s) => const SermonsScreen()),
    GoRoute(path: '/development-plans', name: 'development-plans', builder: (ctx, s) => const DevelopmentPlansScreen()),
        GoRoute(path: '/form-responses', name: 'form-responses', builder: (ctx, s) => FormResponsesScreen(templateId: s.uri.queryParameters['templateId'] ?? '')),
    GoRoute(path: '/department-reports', name: 'department-reports', builder: (ctx, s) => DepartmentReportsScreen(departmentId: s.uri.queryParameters['departmentId'] ?? '')),
    GoRoute(path: '/family-tree', name: 'family-tree', builder: (ctx, s) => FamilyTreeScreen(familyId: s.uri.queryParameters['familyId'] ?? '')),
    GoRoute(path: '/announcement-schedule', name: 'announcement-schedule', builder: (ctx, s) => AnnouncementScheduleScreen(announcementId: s.uri.queryParameters['announcementId'] ?? '')),
    GoRoute(path: '/documents/:id', name: 'document-detail', builder: (ctx, s) => DocumentDetailScreen(documentId: s.pathParameters['id']!)),
    GoRoute(path: '/tickets/:id', name: 'ticket-detail', builder: (ctx, s) => TicketDetailScreen(ticketId: s.pathParameters['id']!)),
    GoRoute(path: '/testimonies/:id', name: 'testimony-detail', builder: (ctx, s) => TestimonyDetailScreen(testimonyId: s.pathParameters['id']!)),
    GoRoute(path: '/rewards/claims', name: 'rewards-claims', builder: (ctx, s) => const RewardsClaimsScreen()),
    GoRoute(path: '/user-roles', name: 'user-roles', builder: (ctx, s) => UserRolesScreen(userId: s.uri.queryParameters['userId'] ?? '')),
    GoRoute(path: '/compliance-exports', name: 'compliance-exports', builder: (ctx, s) => const ComplianceExportsScreen()),
    GoRoute(path: '/quest', name: 'quest', builder: (ctx, s) => const QuestScreen()),
    GoRoute(path: '/discipleship-paths', name: 'discipleship-paths', builder: (ctx, s) => const DiscipleshipPathsScreen()),
    GoRoute(path: '/geo-tracking', name: 'geo-tracking', builder: (ctx, s) => const GeofencingScreen()),
    GoRoute(path: '/visit-notes/verify', name: 'visit-notes-verify', builder: (ctx, s) => VisitNotesVerifyScreen(memberId: s.uri.queryParameters['memberId'] ?? '')),
    GoRoute(path: '/encouragements/detail', name: 'encouragements-detail', builder: (ctx, s) => EncouragementDetailScreen(soulId: s.uri.queryParameters['soulId'] ?? '')),
    GoRoute(path: '/referrals/status', name: 'referrals-status', builder: (ctx, s) => ReferralsStatusScreen(referralId: s.uri.queryParameters['referralId'] ?? '')),
    GoRoute(path: '/spiritual-challenges/detail', name: 'spiritual-challenges-detail', builder: (ctx, s) => SpiritualChallengesDetailScreen(challengeId: s.uri.queryParameters['challengeId'] ?? '')),
    // ===== Nouvelles routes — Cercle Faiseurs, Passeport Spirituel, Parcours Spirituel =====
    GoRoute(path: '/cercle-faiseurs', name: 'cercle-faiseurs', builder: (ctx, s) => const CercleFaiseursScreen()),
    GoRoute(path: '/passport', name: 'passport', builder: (ctx, s) => const PassportScreen()),
    GoRoute(path: '/spiritual-journey', name: 'spiritual-journey', builder: (ctx, s) => const SpiritualJourneyScreen()),
    GoRoute(path: '/conversations', name: 'conversations', builder: (ctx, s) => const ConversationsScreen()),
    GoRoute(path: '/ai-chat', name: 'ai-chat', builder: (ctx, s) => const AiChatScreen()),
  ],
  errorBuilder: (context, state) => NotFoundScreen(path: state.matchedLocation),
);
