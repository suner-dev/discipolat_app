import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../app.dart';
import '../../data/services/api_service.dart';
import 'glass_theme.dart';
class AppDrawer extends StatefulWidget {
  const AppDrawer({super.key});

  @override
  State<AppDrawer> createState() => _AppDrawerState();
}

class _AppDrawerState extends State<AppDrawer> {
  bool _showRoleMenu = false;

  // Navigation items with roles
  static const _navItems = [
    {'icon': Icons.dashboard_rounded, 'title': 'Tableau de bord', 'route': '/dashboard', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']},
    {'icon': Icons.dashboard_customize_rounded, 'title': 'Pilotage Pasteur', 'route': '/dashboard/pasteur', 'roles': ['ADMIN', 'PASTEUR']},
    {'icon': Icons.group_rounded, 'title': 'Dashboard Chef', 'route': '/dashboard/chef-famille', 'roles': ['PASTEUR', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.business_rounded, 'title': 'Dashboard Responsable', 'route': '/dashboard/responsable', 'roles': ['PASTEUR', 'RESPONSABLE']},
    {'icon': Icons.favorite_rounded, 'title': 'Âmes', 'route': '/souls', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.group_rounded, 'title': 'Familles', 'route': '/families', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.people_rounded, 'title': 'CRM Faiseur', 'route': '/crm-faiseur', 'roles': ['ADMIN', 'PASTEUR', 'FAISEUR']},
    {'icon': Icons.search_rounded, 'title': 'Recherche', 'route': '/search', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR']},
    {'icon': Icons.description_rounded, 'title': 'Rapport faiseur', 'route': '/reports/maker', 'roles': ['ADMIN', 'PASTEUR', 'FAISEUR']},
    {'icon': Icons.group_work_rounded, 'title': 'Rapport famille', 'route': '/reports/family', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.book_rounded, 'title': 'Prières', 'route': '/prayers', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.event_rounded, 'title': 'Événements', 'route': '/events', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.star_rounded, 'title': 'Évaluations', 'route': '/evaluations', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.swap_horiz_rounded, 'title': 'Suivis parallèles', 'route': '/parallel-followups', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'FAISEUR']},
    {'icon': Icons.business_rounded, 'title': 'Départements', 'route': '/departments', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE']},
    {'icon': Icons.warning_amber_rounded, 'title': 'Alertes', 'route': '/alerts', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR']},
    {'icon': Icons.notifications_rounded, 'title': 'Notifications', 'route': '/notifications', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']},
    {'icon': Icons.person_rounded, 'title': 'Profil', 'route': '/profile', 'roles': ['ADMIN', 'PASTEUR', 'RESPONSABLE', 'CHEF_DE_FAMILLE', 'FAISEUR', 'MEMBRE']},
  ];

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
          context.go('/dashboard');
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

    // Filter navigation items by user's roles
    final filteredItems = _navItems.where((item) {
      final itemRoles = item['roles'] as List<dynamic>;
      return auth.hasAnyRole(itemRoles.cast<String>());
    }).toList();

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
                            const Text('Discipolat', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
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

            // Navigation items
            ...filteredItems.map((item) => _navItem(
              context,
              item['icon'] as IconData,
              item['title'] as String,
              item['route'] as String,
            )),
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
