import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final _apiService = ApiService();
  int _currentNavIndex = 3;

  Future<void> _logout() async {
    await _apiService.clearTokens();
    if (mounted) context.go('/login');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Profil')),
      drawer: const AppDrawer(),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(children: [
          const SizedBox(height: 16),
          // Avatar
          const GradientAvatar(text: 'FD', radius: 48, showGlow: true, showStatus: true),
          const SizedBox(height: 16),
          const Text('Faiseur de disciples', style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Text('faiseur@discipolat.com', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13)),
          const SizedBox(height: 8),
          const StatusBadge(label: 'Actif', color: Colors.green, glowing: true),
          const SizedBox(height: 32),

          // Info card
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(children: [
              _profileRow(Icons.person_outline, 'Rôle', 'Faiseur de disciples'),
              const GlassDivider(),
              _profileRow(Icons.verified_user, 'Statut', 'Actif'),
              const GlassDivider(),
              _profileRow(Icons.calendar_today, 'Membre depuis', '14 juillet 2026'),
              const GlassDivider(),
              _profileRow(Icons.email_outlined, 'Email', 'faiseur@discipolat.com'),
            ]),
          ),
          const SizedBox(height: 16),

          // Logout
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _logout,
              icon: const Icon(Icons.logout, color: Colors.red),
              label: const Text('Déconnexion', style: TextStyle(color: Colors.red)),
              style: OutlinedButton.styleFrom(
                side: BorderSide(color: Colors.red.withValues(alpha: 0.3)),
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
            ),
          ),
        ]),
      ),
      bottomNavigationBar: GlassBottomNav(currentIndex: _currentNavIndex, onTap: (i) {
        setState(() => _currentNavIndex = i);
        final routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
        if (i < routes.length) context.go(routes[i]);
      }),
    );
  }

  Widget _profileRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(children: [
        Icon(icon, color: AppColors.primaryLight, size: 22),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
          Text(value, style: const TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.w500)),
        ])),
      ]),
    );
  }
}
