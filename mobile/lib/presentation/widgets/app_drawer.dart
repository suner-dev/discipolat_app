import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'glass_theme.dart';

class AppDrawer extends StatelessWidget {
  const AppDrawer({super.key});

  @override
  Widget build(BuildContext context) {
    return Drawer(
      child: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              const Color(0xFF0F172A),
              const Color(0xFF030712),
            ],
          ),
        ),
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            // Glass header
            Container(
              padding: const EdgeInsets.fromLTRB(20, 48, 20, 24),
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
                  const GradientAvatar(text: 'DP', radius: 30, showGlow: true, showStatus: true),
                  const SizedBox(height: 16),
                  const Text('Discipolat', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  Text('Gestion du discipolat', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
                ],
              ),
            ),

            const SizedBox(height: 8),

            // Menu items
            _navItem(context, Icons.dashboard_rounded, 'Tableau de bord', '/dashboard'),
            _navItem(context, Icons.favorite_rounded, 'Âmes', '/souls'),
            _navItem(context, Icons.group_rounded, 'Familles', '/families'),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
              child: Container(height: 1, color: Colors.white.withValues(alpha: 0.06)),
            ),
            _navItem(context, Icons.description_rounded, 'Rapport faiseur', '/reports/maker'),
            _navItem(context, Icons.group_work_rounded, 'Rapport famille', '/reports/family'),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
              child: Container(height: 1, color: Colors.white.withValues(alpha: 0.06)),
            ),
            _navItem(context, Icons.warning_amber_rounded, 'Alertes', '/alerts'),
            _navItem(context, Icons.notifications_rounded, 'Notifications', '/notifications'),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
              child: Container(height: 1, color: Colors.white.withValues(alpha: 0.06)),
            ),
            _navItem(context, Icons.person_rounded, 'Profil', '/profile'),
          ],
        ),
      ),
    );
  }

  Widget _navItem(BuildContext context, IconData icon, String title, String route) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 1),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: AppColors.primary.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: AppColors.primaryLight, size: 20),
        ),
        title: Text(title, style: const TextStyle(color: Colors.white, fontSize: 14)),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        onTap: () {
          Navigator.pop(context);
          context.go(route);
        },
      ),
    );
  }
}
