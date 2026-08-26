import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../app.dart';
import '../../data/services/api_service.dart';
import '../../l10n/app_localizations.dart';
import 'beta_badge.dart';
import 'feedback_sheet.dart';
import 'glass_theme.dart';

/// Éléments de navigation de chaque espace métier.
/// Le rôle actif détermine entièrement le contenu du menu :
/// un changement de rôle = un changement complet d'application.
class AppDrawer extends StatefulWidget {
  const AppDrawer({super.key});

  @override
  State<AppDrawer> createState() => _AppDrawerState();
}

class _AppDrawerState extends State<AppDrawer> {
  bool _showRoleMenu = false;

  // ── Navigation Admin/Pasteur : items opérationnels ──
  static const List<Map<String, Object>> _mainNav = [
    {'icon': Icons.dashboard_rounded, 'title': 'Tableau de bord', 'route': '/dashboard'},
    {'icon': Icons.dashboard_customize_rounded, 'title': 'Pilotage Pasteur', 'route': '/dashboard/pasteur'},
    {'icon': Icons.search_rounded, 'title': 'Recherche', 'route': '/search'},
    {'icon': Icons.map_rounded, 'title': 'Cartographie', 'route': '/map'},
    {'icon': Icons.favorite_rounded, 'title': 'Âmes', 'route': '/souls'},
    {'icon': Icons.group_rounded, 'title': 'Familles', 'route': '/families'},
    {'icon': Icons.people_rounded, 'title': 'CRM Faiseur', 'route': '/crm-faiseur'},
    {'icon': Icons.group_rounded, 'title': 'Dashboard Chef', 'route': '/dashboard/chef-famille'},
    {'icon': Icons.business_rounded, 'title': 'Dashboard Responsable', 'route': '/dashboard/responsable'},
    {'icon': Icons.business_rounded, 'title': 'Départements', 'route': '/departments'},
    {'icon': Icons.description_rounded, 'title': 'Rapports', 'route': '/reports'},
    {'icon': Icons.description_rounded, 'title': 'Rapport faiseur', 'route': '/reports/maker'},
    {'icon': Icons.group_work_rounded, 'title': 'Rapport famille', 'route': '/reports/family'},
    {'icon': Icons.book_rounded, 'title': 'Prières', 'route': '/prayers'},
    {'icon': Icons.favorite_border_rounded, 'title': 'Actions de grâce', 'route': '/prayers/actions-de-grace'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.route_rounded, 'title': 'Évangélisation', 'route': '/evangelism'},
    {'icon': Icons.flag_rounded, 'title': 'Objectifs', 'route': '/objectives'},
    {'icon': Icons.map_outlined, 'title': 'Visites', 'route': '/visits'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Suivis parallèles', 'route': '/parallel-followups'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.account_tree_rounded, 'title': 'Workflow transfert', 'route': '/admin/transfers'},
    {'icon': Icons.star_rounded, 'title': 'Évaluations', 'route': '/evaluations'},
    {'icon': Icons.gavel_rounded, 'title': 'Discipline', 'route': '/discipline'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.mail_rounded, 'title': 'Demandes', 'route': '/members/requests'},
    {'icon': Icons.folder_rounded, 'title': 'Documents', 'route': '/documents'},
    {'icon': Icons.calendar_today_rounded, 'title': 'Rendez-vous', 'route': '/appointments'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.emoji_events_rounded, 'title': 'Badges', 'route': '/badges'},
    {'icon': Icons.bolt_rounded, 'title': 'Quest (XP)', 'route': '/quest'},
    {'icon': Icons.volunteer_activism_rounded, 'title': 'Dîmes & offrandes', 'route': '/giving'},
    {'icon': Icons.savings_rounded, 'title': 'Tontines', 'route': '/tontines'},
    {'icon': Icons.mic_rounded, 'title': 'Rapports vocaux', 'route': '/voice-reports'},
    {'icon': Icons.record_voice_over_rounded, 'title': 'PasteurBot Vocal', 'route': '/voice-assistant'},
    {'icon': Icons.face_retouching_natural_rounded, 'title': 'Pointage facial', 'route': '/face-checkin'},
    {'icon': Icons.school_rounded, 'title': 'Formations', 'route': '/trainings'},
    {'icon': Icons.account_balance_wallet_rounded, 'title': 'Finances', 'route': '/finances'},
    {'icon': Icons.campaign_rounded, 'title': 'Annonces', 'route': '/communications'},
    {'icon': Icons.inventory_2_rounded, 'title': 'Inventaire', 'route': '/inventory'},
    {'icon': Icons.notifications_rounded, 'title': 'Notifications', 'route': '/notifications'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
  ];

  // ── Section Administration (groupée dans le drawer) ──
  static const List<Map<String, Object>> _adminNav = [
    {'icon': Icons.auto_fix_high_rounded, 'title': 'Workflows', 'route': '/workflows'},
    {'icon': Icons.inventory_2_rounded, 'title': 'Modules plateforme', 'route': '/admin/modules'},
    {'icon': Icons.menu_book_rounded, 'title': 'Menus plateforme', 'route': '/admin/menus'},
    {'icon': Icons.dashboard_customize_rounded, 'title': 'Pages personnalisées', 'route': '/admin/pages'},
    {'icon': Icons.settings_rounded, 'title': 'Paramètres église', 'route': '/admin/settings'},
    {'icon': Icons.text_fields_rounded, 'title': 'Champs personnalisés', 'route': '/admin/custom-fields'},
    {'icon': Icons.book_rounded, 'title': 'Dictionnaires', 'route': '/admin/dictionaries'},
    {'icon': Icons.language_rounded, 'title': 'Intégrations', 'route': '/admin/integrations'},
    {'icon': Icons.business_rounded, 'title': 'Églises (tenants)', 'route': '/admin/tenants'},
    {'icon': Icons.security_rounded, 'title': 'Sécurité', 'route': '/security-settings'},
    {'icon': Icons.people_rounded, 'title': 'Utilisateurs', 'route': '/users'},
    {'icon': Icons.shield_rounded, 'title': 'Permissions', 'route': '/permissions'},
    {'icon': Icons.history_rounded, 'title': 'Audit', 'route': '/audit'},
    {'icon': Icons.shield_rounded, 'title': 'Compliance RGPD', 'route': '/compliance'},
    {'icon': Icons.chat_rounded, 'title': 'WhatsApp Rappels', 'route': '/whatsapp-reminders'},
    {'icon': Icons.shield_rounded, 'title': 'Modération', 'route': '/moderation'},
    {'icon': Icons.upload_file_rounded, 'title': 'Migration données', 'route': '/data-migration'},
    {'icon': Icons.analytics_rounded, 'title': 'Analytics usage', 'route': '/usage-analytics'},
    {'icon': Icons.favorite_rounded, 'title': 'Encouragements', 'route': '/encouragements'},
    {'icon': Icons.compare_arrows_rounded, 'title': 'Benchmark églises', 'route': '/church-comparison'},
    {'icon': Icons.mood_rounded, 'title': 'Sabbath Dashboard', 'route': '/sabbath-dashboard'},
    {'icon': Icons.emoji_events_rounded, 'title': 'Récompenses', 'route': '/rewards'},
    {'icon': Icons.local_fire_department_rounded, 'title': 'Défis hebdo', 'route': '/weekly-challenges'},
    {'icon': Icons.show_chart_rounded, 'title': 'Croissance', 'route': '/growth-projection'},
    {'icon': Icons.speed_rounded, 'title': 'Prédiction charge', 'route': '/load-prediction'},
    {'icon': Icons.location_on_rounded, 'title': 'Santé quartiers', 'route': '/neighborhood-health'},
    {'icon': Icons.follow_the_signs_rounded, 'title': 'Demandes suivi', 'route': '/follow-up-requests'},
    {'icon': Icons.route_rounded, 'title': 'Parcours spirituel', 'route': '/discipleship-path'},
  ];

  // Computed: main + admin pour ADMIN ; main seul pour Pasteur
  static List<Map<String, Object>> get _fullNav => [..._mainNav, ..._adminNav];

  // Espace RESPONSABLE — gestion des départements, équipes, tâches, évaluations, discipline, progression
  static const List<Map<String, Object>> _responsableNav = [
    {'icon': Icons.dashboard_rounded, 'title': 'Dashboard Responsable', 'route': '/dashboard/responsable'},
    {'icon': Icons.business_rounded, 'title': 'Départements', 'route': '/departments'},
    {'icon': Icons.check_circle_rounded, 'title': 'Saisie présences', 'route': '/dashboard/responsable'},
    {'icon': Icons.star_rounded, 'title': 'Évaluations', 'route': '/evaluations'},
    {'icon': Icons.gavel_rounded, 'title': 'Discipline', 'route': '/discipline'},
    {'icon': Icons.trending_up_rounded, 'title': 'Progression', 'route': '/departments'},
    {'icon': Icons.mail_rounded, 'title': 'Demandes & présences', 'route': '/members/requests'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.description_rounded, 'title': 'Rapports', 'route': '/reports'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.favorite_border_rounded, 'title': 'Actions de grâce', 'route': '/prayers/actions-de-grace'},
    {'icon': Icons.folder_rounded, 'title': 'Documents', 'route': '/documents'},
    {'icon': Icons.campaign_rounded, 'title': 'Annonces', 'route': '/communications'},
    {'icon': Icons.inventory_2_rounded, 'title': 'Inventaire', 'route': '/inventory'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
    {'icon': Icons.security_rounded, 'title': 'Sécurité', 'route': '/security-settings'},
  ];    // Espace FAISEUR — discipolat, visites, prières, rapports, suivi, progression, présence, événements
  static const List<Map<String, Object>> _faiseurNav = [
    {'icon': Icons.people_rounded, 'title': 'CRM Faiseur', 'route': '/crm-faiseur'},
    {'icon': Icons.favorite_rounded, 'title': 'Mes disciples', 'route': '/souls'},
    {'icon': Icons.route_rounded, 'title': 'Évangélisation', 'route': '/evangelism'},
    {'icon': Icons.description_rounded, 'title': 'Rapport faiseur', 'route': '/reports/maker'},
    {'icon': Icons.map_outlined, 'title': 'Visites', 'route': '/visits'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Suivis parallèles', 'route': '/parallel-followups'},
    {'icon': Icons.flag_rounded, 'title': 'Objectifs', 'route': '/objectives'},
    {'icon': Icons.mic_rounded, 'title': 'Rapports vocaux', 'route': '/voice-reports'},
    {'icon': Icons.record_voice_over_rounded, 'title': 'PasteurBot Vocal', 'route': '/voice-assistant'},
    {'icon': Icons.book_rounded, 'title': 'Prières', 'route': '/prayers'},
    {'icon': Icons.favorite_border_rounded, 'title': 'Actions de grâce', 'route': '/prayers/actions-de-grace'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.description_rounded, 'title': 'Rapports', 'route': '/reports'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.search_rounded, 'title': 'Recherche', 'route': '/search'},
    {'icon': Icons.campaign_rounded, 'title': 'Annonces', 'route': '/communications'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.folder_rounded, 'title': 'Documents', 'route': '/documents'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
    {'icon': Icons.security_rounded, 'title': 'Sécurité', 'route': '/security-settings'},
  ];    // Espace CHEF DE FAMILLE — faiseurs, disciples, âmes, familles, rapports, prières, progression, alertes
  static const List<Map<String, Object>> _chefNav = [
    {'icon': Icons.dashboard_rounded, 'title': 'Dashboard Chef', 'route': '/dashboard/chef-famille'},
    {'icon': Icons.group_rounded, 'title': 'Familles', 'route': '/families'},
    {'icon': Icons.favorite_rounded, 'title': 'Disciples', 'route': '/souls'},
    {'icon': Icons.route_rounded, 'title': 'Évangélisation', 'route': '/evangelism'},
    {'icon': Icons.group_work_rounded, 'title': 'Rapport famille', 'route': '/reports/family'},
    {'icon': Icons.description_rounded, 'title': 'Rapports', 'route': '/reports'},
    {'icon': Icons.star_rounded, 'title': 'Évaluations', 'route': '/evaluations'},
    {'icon': Icons.book_rounded, 'title': 'Prières', 'route': '/prayers'},
    {'icon': Icons.favorite_border_rounded, 'title': 'Actions de grâce', 'route': '/prayers/actions-de-grace'},
    {'icon': Icons.trending_up_rounded, 'title': 'Progression', 'route': '/families'},
    {'icon': Icons.map_outlined, 'title': 'Visites', 'route': '/visits'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.mail_rounded, 'title': 'Demandes', 'route': '/members/requests'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.campaign_rounded, 'title': 'Annonces', 'route': '/communications'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.folder_rounded, 'title': 'Documents', 'route': '/documents'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
    {'icon': Icons.security_rounded, 'title': 'Sécurité', 'route': '/security-settings'},
  ];    // Espace MEMBRE — profil, départements, famille, faiseur, présence, progression, événements, prières, activités
  static const List<Map<String, Object>> _membreNav = [
    {'icon': Icons.dashboard_rounded, 'title': 'Mon tableau de bord', 'route': '/dashboard/membre'},
    {'icon': Icons.person_rounded, 'title': 'Mon profil', 'route': '/profile'},
    {'icon': Icons.check_circle_rounded, 'title': 'Mes présences', 'route': '/dashboard/membre'},
    {'icon': Icons.trending_up_rounded, 'title': 'Ma progression', 'route': '/dashboard/membre'},
    {'icon': Icons.timeline_rounded, 'title': 'Mes activités', 'route': '/dashboard/membre/activities'},
    {'icon': Icons.book_rounded, 'title': 'Prières', 'route': '/prayers'},
    {'icon': Icons.favorite_border_rounded, 'title': 'Actions de grâce', 'route': '/prayers/actions-de-grace'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.school_rounded, 'title': 'Formations', 'route': '/trainings'},
    {'icon': Icons.emoji_events_rounded, 'title': 'Badges', 'route': '/badges'},
    {'icon': Icons.bolt_rounded, 'title': 'Quest (XP)', 'route': '/quest'},
    {'icon': Icons.savings_rounded, 'title': 'Tontines', 'route': '/tontines'},
    {'icon': Icons.volunteer_activism_rounded, 'title': 'Dîmes & offrandes', 'route': '/giving'},
    {'icon': Icons.calendar_today_rounded, 'title': 'Rendez-vous', 'route': '/appointments'},
    {'icon': Icons.campaign_rounded, 'title': 'Annonces', 'route': '/communications'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.notifications_rounded, 'title': 'Notifications', 'route': '/notifications'},
    {'icon': Icons.security_rounded, 'title': 'Sécurité', 'route': '/security-settings'},
  ];

  /// Traduit le titre d'un élément de navigation via sa route.
  static String navTitle(String route, AppLocalizations l10n) {
    switch (route) {
      case '/dashboard': return l10n.navDashboard;
      case '/dashboard/pasteur': return l10n.navShepherdsPilot;
      case '/search': return l10n.navSearch;
      case '/map': return l10n.navMap;
      case '/souls': return l10n.navSouls;
      case '/families': return l10n.navFamilies;
      case '/crm-faiseur': return l10n.navCrmFaiseur;
      case '/dashboard/chef-famille': return l10n.navChefDashboard;
      case '/dashboard/responsable': return l10n.navRespDashboard;
      case '/departments': return l10n.navDepartments;
      case '/reports': return l10n.navReports;
      case '/reports/maker': return l10n.navMakerReport;
      case '/reports/family': return l10n.navFamilyReport;
      case '/prayers': return l10n.navPrayers;
      case '/prayers/actions-de-grace': return l10n.navGraceActions;
      case '/events': return l10n.navEvents;
      case '/evangelism': return l10n.navEvangelism;
      case '/objectives': return l10n.navObjectives;
      case '/visits': return l10n.navVisits;
      case '/parallel-followups': return l10n.navParallelFollowups;
      case '/transfers': return l10n.navTransfers;
      case '/admin/transfers': return l10n.navTransferWorkflow;
      case '/evaluations': return l10n.navEvaluations;
      case '/discipline': return l10n.navDiscipline;
      case '/alerts': return l10n.navAlerts;
      case '/members/requests': return l10n.navRequests;
      case '/documents': return l10n.navDocuments;
      case '/appointments': return l10n.navAppointments;
      case '/messages': return l10n.navMessaging;
      case '/badges': return l10n.navBadges;
      case '/quest': return l10n.navQuest;
      case '/giving': return l10n.navTithesOfferings;
      case '/tontines': return l10n.navTontines;
      case '/voice-reports': return l10n.navVoiceReports;
      case '/voice-assistant': return l10n.navVoiceAssistant;
      case '/face-checkin': return l10n.navFaceCheckin;
      case '/trainings': return l10n.navTrainings;
      case '/finances': return l10n.navFinances;
      case '/communications': return l10n.navCommunications;
      case '/inventory': return l10n.navInventory;
      case '/notifications': return l10n.navNotifications;
      case '/profile': return l10n.navProfile;
      case '/admin/modules': return l10n.navModules;
      case '/admin/menus': return l10n.navMenus;
      case '/admin/pages': return l10n.navCustomPages;
      case '/admin/settings': return l10n.navChurchSettings;
      case '/admin/custom-fields': return l10n.navCustomFields;
      case '/admin/dictionaries': return l10n.navDictionaries;
      case '/admin/integrations': return l10n.navIntegrations;
      case '/admin/tenants': return l10n.navChurches;
      case '/security-settings': return l10n.navSecurity;
      case '/users': return l10n.navUsers;
      case '/permissions': return l10n.navPermissions;
      case '/audit': return l10n.navAudit;
      case '/compliance': return l10n.navCompliance;
      case '/whatsapp-reminders': return l10n.navWhatsApp;
      case '/moderation': return l10n.navModeration;
      case '/data-migration': return l10n.navDataMigration;
      case '/usage-analytics': return l10n.navUsageAnalytics;
      case '/encouragements': return l10n.navEncouragements;
      case '/church-comparison': return l10n.navChurchBenchmark;
      case '/sabbath-dashboard': return l10n.navSabbath;
      case '/rewards': return l10n.navRewards;
      case '/weekly-challenges': return l10n.navWeeklyChallenges;
      case '/growth-projection': return l10n.navGrowthProjection;
      case '/load-prediction': return l10n.navLoadPrediction;
      case '/neighborhood-health': return l10n.navNeighborhoodHealth;
      case '/follow-up-requests': return l10n.navFollowUpRequests;
      case '/discipleship-path': return l10n.navDiscipleshipPath;
      case '/dashboard/membre': return l10n.navDashboard;
      case '/dashboard/membre/activities': return l10n.navDashboard;
      default: return route;
    }
  }

  /// Retourne les éléments de navigation de l'espace métier du rôle donné.
  static List<Map<String, Object>> _navForRole(String role) {
    switch (role) {
      case 'RESPONSABLE':
        return _responsableNav;
      case 'FAISEUR':
        return _faiseurNav;
      case 'CHEF_DE_FAMILLE':
        return _chefNav;
      case 'MEMBRE':
        return _membreNav;
      case 'PASTEUR':
        // Pasteur = vue complète, sans la configuration plateforme réservée Admin
        // (matrice des permissions, modules, menus, pages).
        return _fullNav.where((item) => !const {'/permissions', '/admin/modules', '/admin/menus', '/admin/pages', '/workflows'}.contains(item['route'])).toList();
      default:
        return _fullNav; // ADMIN
    }
  }

  static const _roleLabels = {
    'ADMIN': 'Admin',
    'PASTEUR': 'Pasteur',
    'RESPONSABLE': 'Responsable',
    'CHEF_DE_FAMILLE': 'Chef de famille',
    'FAISEUR': 'Faiseur',
    'MEMBRE': 'Membre',
  };

  static const _roleIcons = {
    'ADMIN': Icons.admin_panel_settings,
    'PASTEUR': Icons.church,
    'RESPONSABLE': Icons.assignment_ind,
    'CHEF_DE_FAMILLE': Icons.group,
    'FAISEUR': Icons.eco,
    'MEMBRE': Icons.person,
  };

  Color _roleColor(String role) {
    switch (role) {
      case 'ADMIN': return Colors.red;
      case 'PASTEUR': return Colors.green;
      case 'RESPONSABLE': return Colors.amber;
      case 'CHEF_DE_FAMILLE': return const Color(0xFFD4AF37);
      case 'FAISEUR': return Colors.teal;
      default: return Colors.grey;
    }
  }

  Future<void> _switchRole(String newRole) async {
    final auth = AuthState();
    try {
      final api = ApiService();
      final response = await api.post('/auth/switch-role', data: {'role': newRole});
      if (response.data != null) {
        await api.saveTokens(response.data);
        auth.switchActiveRole(newRole);
        if (mounted) {
          Navigator.pop(context);
          context.go(roleHome(newRole));
        }
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).roleSwitchFailed)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = AuthState();
    final activeRole = auth.activeRole;
    final allRoles = auth.roles;

    // Filtre STRICT par rôle actif : seul l'espace métier du rôle est affiché.
    final filteredItems = activeRole.isNotEmpty
        ? _navForRole(activeRole)
        : _fullNav;

    return Drawer(
      child: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFF0F172A), Color(0xFF030712)],
          ),
        ),
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            // Header with user info
            Container(
              padding: const EdgeInsets.fromLTRB(20, 48, 20, 20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [AppColors.primary.withValues(alpha: 0.2), Colors.transparent],
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                ),
                border: Border(bottom: BorderSide(color: Colors.white.withValues(alpha: 0.06))),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const GradientAvatar(text: 'DP', radius: 28, showGlow: true, showStatus: true),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                const Flexible(
                                  child: Text('Discipolat',
                                      style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold),
                                      overflow: TextOverflow.ellipsis),
                                ),
                                const SizedBox(width: 8),
                                // Badge BÊTA — uniquement en environnement bêta (serveur-driven)
                                const BetaBadge(),
                              ],
                            ),
                            const SizedBox(height: 2),
                            Text(
                              auth.firstName != null ? '${auth.firstName} ${auth.lastName}' : auth.email ?? '',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Active role badge
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: _roleColor(activeRole).withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(color: _roleColor(activeRole).withValues(alpha: 0.3)),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(_roleIcons[activeRole] ?? Icons.person, size: 14, color: _roleColor(activeRole)),
                        const SizedBox(width: 6),
                        Text(
                          _roleLabels[activeRole] ?? activeRole,
                          style: TextStyle(color: _roleColor(activeRole), fontSize: 11, fontWeight: FontWeight.w600),
                        ),
                      ],
                    ),
                  ),
                  // Role switcher (if multiple roles)
                  if (allRoles.length > 1) ...[
                    const SizedBox(height: 8),
                    InkWell(
                      onTap: () => setState(() => _showRoleMenu = !_showRoleMenu),
                      child: Row(
                        children: [
                          Icon(Icons.swap_horiz, color: Colors.white.withValues(alpha: 0.4), size: 16),
                          const SizedBox(width: 6),
                          Text(AppLocalizations.of(context).navChangeRole, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                          Icon(_showRoleMenu ? Icons.expand_less : Icons.expand_more, color: Colors.white.withValues(alpha: 0.4), size: 16),
                        ],
                      ),
                    ),
                    if (_showRoleMenu) ...[
                      const SizedBox(height: 8),
                      ...allRoles.map((role) => Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: InkWell(
                          onTap: role == activeRole ? null : () => _switchRole(role),
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                            decoration: BoxDecoration(
                              color: role == activeRole
                                  ? _roleColor(role).withValues(alpha: 0.15)
                                  : Colors.transparent,
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Row(
                              children: [
                                Icon(_roleIcons[role] ?? Icons.person, size: 18, color: _roleColor(role)),
                                const SizedBox(width: 8),
                                Text(
                                  _roleLabels[role] ?? role,
                                  style: TextStyle(
                                    color: role == activeRole ? _roleColor(role) : Colors.white.withValues(alpha: 0.6),
                                    fontWeight: role == activeRole ? FontWeight.bold : FontWeight.normal,
                                    fontSize: 13,
                                  ),
                                ),
                                const Spacer(),
                                if (role == activeRole)
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: _roleColor(role).withValues(alpha: 0.2),
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    child: Text(AppLocalizations.of(context).navActiveLabel, style: TextStyle(color: _roleColor(role), fontSize: 8, fontWeight: FontWeight.bold)),
                                  ),
                              ],
                            ),
                          ),
                        ),
                      )),
                    ],
                  ],
                ],
              ),
            ),

            const SizedBox(height: 8),

            // Navigation items — espace métier du rôle actif
            // Si le rôle est ADMIN, on affiche la section Administration séparément
            ..._buildNavWithAdminSection(context, filteredItems, activeRole),

            const Divider(color: Colors.white12, height: 24),

            // Retour testeur — disponible pour tout utilisateur authentifié
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFFF59E0B).withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.feedback_outlined, color: Color(0xFFF59E0B), size: 20),
              ),
              title: Text(AppLocalizations.of(context).feedbackTitle,
                style: const TextStyle(color: Colors.white70, fontSize: 14, fontWeight: FontWeight.w500),
              ),
              subtitle: Text(AppLocalizations.of(context).feedbackSubtitle,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.35), fontSize: 11),
              ),
              onTap: () {
                // Route lue AVANT de fermer le drawer (context fiable).
                final routeName = ModalRoute.of(context)?.settings.name;
                Navigator.pop(context);
                showFeedbackSheet(context, pageUrl: routeName);
              },
            ),
          ],
        ),
      ),
    );
  }

  List<Widget> _buildNavWithAdminSection(BuildContext context, List<Map<String, Object>> items, String activeRole) {
    final adminRoutes = _adminNav.map((e) => e['route']).toSet();
    final mainItems = items.where((item) => !adminRoutes.contains(item['route'])).toList();
    final adminItems = items.where((item) => adminRoutes.contains(item['route'])).toList();

    final widgets = <Widget>[];

    // Main nav items
    for (final item in mainItems) {
      final l10n = AppLocalizations.of(context);
      widgets.add(_navItem(context, item['icon'] as IconData, navTitle(item['route'] as String, l10n), item['route'] as String));
    }

    // Admin section header (only if admin items exist and role is ADMIN)
    if (adminItems.isNotEmpty && activeRole == 'ADMIN') {
      widgets.add(Padding(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
        child: Row(
          children: [
            const Icon(Icons.admin_panel_settings_rounded, size: 14, color: Colors.white38),
            const SizedBox(width: 8),
            Text(AppLocalizations.of(context).navAdminSection, style: const TextStyle(color: Colors.white38, fontSize: 11, fontWeight: FontWeight.w700, letterSpacing: 1.2)),
          ],
        ),
      ));
      for (final item in adminItems) {
        final l10n = AppLocalizations.of(context);
      widgets.add(_navItem(context, item['icon'] as IconData, navTitle(item['route'] as String, l10n), item['route'] as String));
      }
    }

    return widgets;
  }

  Widget _navItem(BuildContext context, IconData icon, String title, String route) {
    final isActive = ModalRoute.of(context)?.settings.name == route ||
        (route != '/dashboard' && ModalRoute.of(context)?.settings.name?.toString().contains(route) == true);

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 1),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: isActive ? AppColors.primary.withValues(alpha: 0.1) : null,
      ),
      child: ListTile(
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: isActive ? AppColors.primary.withValues(alpha: 0.15) : AppColors.primary.withValues(alpha: 0.05),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: isActive ? AppColors.primaryLight : Colors.white.withValues(alpha: 0.6), size: 20),
        ),
        title: Text(title,
          style: TextStyle(
            color: isActive ? Colors.white : Colors.white.withValues(alpha: 0.7),
            fontSize: 14,
            fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
          ),
        ),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        onTap: () {
          Navigator.pop(context);
          context.go(route);
        },
      ),
    );
  }
}
