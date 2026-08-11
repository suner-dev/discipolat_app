import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../app.dart';
import '../../data/services/api_service.dart';
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

  // Navigation complète — Admin / Pasteur (super-utilisateurs)
  static const List<Map<String, Object>> _fullNav = [
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
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.route_rounded, 'title': 'Évangélisation', 'route': '/evangelism'},
    {'icon': Icons.flag_rounded, 'title': 'Objectifs', 'route': '/objectives'},
    {'icon': Icons.map_outlined, 'title': 'Visites', 'route': '/visits'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Suivis parallèles', 'route': '/parallel-followups'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.account_tree_rounded, 'title': 'Workflow transfert', 'route': '/admin/transfers'},
    {'icon': Icons.star_rounded, 'title': 'Évaluations', 'route': '/evaluations'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.mail_rounded, 'title': 'Demandes', 'route': '/members/requests'},
    {'icon': Icons.folder_rounded, 'title': 'Documents', 'route': '/documents'},
    {'icon': Icons.calendar_today_rounded, 'title': 'Rendez-vous', 'route': '/appointments'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.emoji_events_rounded, 'title': 'Badges', 'route': '/badges'},
    {'icon': Icons.school_rounded, 'title': 'Formations', 'route': '/trainings'},
    {'icon': Icons.notifications_rounded, 'title': 'Notifications', 'route': '/notifications'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
    {'icon': Icons.people_rounded, 'title': 'Utilisateurs', 'route': '/users'},
    {'icon': Icons.shield_rounded, 'title': 'Permissions', 'route': '/permissions'},
    {'icon': Icons.history_rounded, 'title': 'Audit', 'route': '/audit'},
    {'icon': Icons.inventory_2_rounded, 'title': 'Modules plateforme', 'route': '/admin/modules'},
    {'icon': Icons.menu_book_rounded, 'title': 'Menus plateforme', 'route': '/admin/menus'},
  ];

  // Espace RESPONSABLE — gestion des départements uniquement
  static const List<Map<String, Object>> _responsableNav = [
    {'icon': Icons.dashboard_rounded, 'title': 'Dashboard Responsable', 'route': '/dashboard/responsable'},
    {'icon': Icons.business_rounded, 'title': 'Départements', 'route': '/departments'},
    {'icon': Icons.mail_rounded, 'title': 'Demandes & présences', 'route': '/members/requests'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.description_rounded, 'title': 'Rapports', 'route': '/reports'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.folder_rounded, 'title': 'Documents', 'route': '/documents'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
  ];

  // Espace FAISEUR — discipolat uniquement
  static const List<Map<String, Object>> _faiseurNav = [
    {'icon': Icons.people_rounded, 'title': 'CRM Faiseur', 'route': '/crm-faiseur'},
    {'icon': Icons.favorite_rounded, 'title': 'Mes disciples', 'route': '/souls'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.description_rounded, 'title': 'Rapports', 'route': '/reports'},
    {'icon': Icons.description_rounded, 'title': 'Rapport faiseur', 'route': '/reports/maker'},
    {'icon': Icons.book_rounded, 'title': 'Prières', 'route': '/prayers'},
    {'icon': Icons.map_outlined, 'title': 'Visites', 'route': '/visits'},
    {'icon': Icons.route_rounded, 'title': 'Évangélisation', 'route': '/evangelism'},
    {'icon': Icons.flag_rounded, 'title': 'Objectifs', 'route': '/objectives'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Suivis parallèles', 'route': '/parallel-followups'},
    {'icon': Icons.search_rounded, 'title': 'Recherche', 'route': '/search'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
  ];

  // Espace CHEF DE FAMILLE — gestion de la famille uniquement
  static const List<Map<String, Object>> _chefNav = [
    {'icon': Icons.dashboard_rounded, 'title': 'Dashboard Chef', 'route': '/dashboard/chef-famille'},
    {'icon': Icons.group_rounded, 'title': 'Familles', 'route': '/families'},
    {'icon': Icons.favorite_rounded, 'title': 'Disciples', 'route': '/souls'},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Transferts', 'route': '/transfers'},
    {'icon': Icons.description_rounded, 'title': 'Rapports', 'route': '/reports'},
    {'icon': Icons.group_work_rounded, 'title': 'Rapport famille', 'route': '/reports/family'},
    {'icon': Icons.star_rounded, 'title': 'Évaluations', 'route': '/evaluations'},
    {'icon': Icons.book_rounded, 'title': 'Prières', 'route': '/prayers'},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events'},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts'},
    {'icon': Icons.mail_rounded, 'title': 'Demandes', 'route': '/members/requests'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile'},
  ];

  // Espace MEMBRE — personnel
  static const List<Map<String, Object>> _membreNav = [
    {'icon': Icons.person_rounded, 'title': 'Mon espace', 'route': '/profile'},
    {'icon': Icons.school_rounded, 'title': 'Formations', 'route': '/trainings'},
    {'icon': Icons.emoji_events_rounded, 'title': 'Badges', 'route': '/badges'},
    {'icon': Icons.calendar_today_rounded, 'title': 'Rendez-vous', 'route': '/appointments'},
    {'icon': Icons.chat_rounded, 'title': 'Messagerie', 'route': '/messages'},
    {'icon': Icons.notifications_rounded, 'title': 'Notifications', 'route': '/notifications'},
  ];

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
        // (matrice des permissions, modules, menus).
        return _fullNav.where((item) => !const {'/permissions', '/admin/modules', '/admin/menus'}.contains(item['route'])).toList();
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
          const SnackBar(content: Text('Échec du changement de rôle')),
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
                          Text('Changer de rôle', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
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
                                    child: Text('ACTIF', style: TextStyle(color: _roleColor(role), fontSize: 8, fontWeight: FontWeight.bold)),
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
            ...filteredItems.map((item) => _navItem(
              context,
              item['icon'] as IconData,
              item['title'] as String,
              item['route'] as String,
            )),

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
              title: const Text('Un retour ?',
                style: TextStyle(color: Colors.white70, fontSize: 14, fontWeight: FontWeight.w500),
              ),
              subtitle: Text('Bug, suggestion, problème…',
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
