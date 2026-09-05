import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../../data/local/locale_provider.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _userInfo;
  Map<String, dynamic>? _dashboard;
  bool _isLoading = true;
  int _currentNavIndex = 3;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final userRes = await _apiService.get('/users/me');
      final dashRes = await _apiService.get('/dashboard/my-metrics');
      if (mounted) {
        setState(() {
          _userInfo = userRes.data as Map<String, dynamic>?;
          _dashboard = dashRes.data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _logout() async {
    await _apiService.clearTokens();
    if (mounted) context.go('/login');
  }

  void _showLanguagePicker() {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(AppLocalizations.of(dialogContext).language),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: kSupportedLocales.map((loc) {
            final code = loc.languageCode;
            final isSelected =
                AppLocalizations.of(dialogContext).locale.languageCode == code;
            return ListTile(
              leading: Icon(
                isSelected ? Icons.radio_button_checked : Icons.radio_button_off,
                color: isSelected ? Colors.green : Colors.white38,
              ),
              title: Text(kLocaleNames[code] ?? code),
              onTap: () {
                ProviderScope.containerOf(dialogContext, listen: false)
                    .read(localeProvider.notifier)
                    .setLocale(code);
                Navigator.of(dialogContext).pop();
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text(AppLocalizations.of(context).languageChanged)),
                );
              },
            );
          }).toList(),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final firstName = _userInfo?['firstName'] ?? '—';
    final lastName = _userInfo?['lastName'] ?? '—';
    final email = _userInfo?['email'] ?? '—';
    final role = _userInfo?['role'] ?? 'MEMBRE';
    final phone = _userInfo?['phone'] ?? '—';
    final initials = '${(firstName as String).isNotEmpty ? firstName[0] : ''}${(lastName as String).isNotEmpty ? lastName[0] : ''}';

    return SecureScreen(
      screenName: 'ProfileScreen',
      auditAction: AuditActions.viewProfiles,
      child: Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).profileTitle),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    const SizedBox(height: 16),
                    // Avatar
                    GradientAvatar(text: initials, radius: 48, showGlow: true, showStatus: true),
                    const SizedBox(height: 16),
                    Text('$firstName $lastName', style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 4),
                    Text(email, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 13)),
                    const SizedBox(height: 8),
                    StatusBadge(label: role, color: Colors.green, glowing: true),
                    const SizedBox(height: 24),

                    // Stats cards
                    Row(
                      children: [
                        _statCard(Icons.star, AppLocalizations.of(context).profileScore, '${_dashboard?['scoreSpirituel'] ?? '—'}', Colors.amber),
                        const SizedBox(width: 8),
                        _statCard(Icons.check_circle, AppLocalizations.of(context).profilePresence, '${_dashboard?['tauxPresence'] ?? '—'}', Colors.green),
                        const SizedBox(width: 8),
                        _statCard(Icons.trending_up, AppLocalizations.of(context).profileProgression, '${_dashboard?['progression'] ?? '—'}', Colors.blue),
                      ],
                    ),
                    const SizedBox(height: 16),

                    // Personal info
                    GlassCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(AppLocalizations.of(context).profilePersonalInfo, style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 14, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 12),
                          _profileRow(Icons.person_outline, AppLocalizations.of(context).profileRole, role),
                          const GlassDivider(),
                          _profileRow(Icons.phone_outlined, AppLocalizations.of(context).profilePhone, phone),
                          const GlassDivider(),
                          _profileRow(Icons.email_outlined, AppLocalizations.of(context).profileEmail, email),
                          const GlassDivider(),
                          _profileRow(Icons.calendar_today, AppLocalizations.of(context).profileRegisteredOn, _userInfo?['createdAt']?.toString().substring(0, 10) ?? '—'),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Spiritual info
                    GlassCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(AppLocalizations.of(context).profileSpiritualInfo, style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 14, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 12),
                          _profileRow(Icons.auto_awesome, AppLocalizations.of(context).profileSpiritualScore, '${_dashboard?['scoreSpirituel'] ?? '—'}/100'),
                          const GlassDivider(),
                          _profileRow(Icons.family_restroom, AppLocalizations.of(context).profileFamily, _dashboard?['famille'] ?? '—'),
                          const GlassDivider(),
                          _profileRow(Icons.business, AppLocalizations.of(context).profileDepartment, _dashboard?['departement'] ?? '—'),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Quick actions
                    GlassCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(AppLocalizations.of(context).profileQuickActions, style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 14, fontWeight: FontWeight.w600)),
                          const SizedBox(height: 12),
                          _actionRow(Icons.security, AppLocalizations.of(context).securityTitle, () => context.go('/security-settings')),
                          const GlassDivider(),
                          _actionRow(Icons.language, AppLocalizations.of(context).language, () => _showLanguagePicker()),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Logout
                    SizedBox(
                      width: double.infinity,
                      child: OutlinedButton.icon(
                        onPressed: _logout,
                        icon: const Icon(Icons.logout, color: Colors.red),
                        label: Text(AppLocalizations.of(context).profileLogout, style: const TextStyle(color: Colors.red)),
                        style: OutlinedButton.styleFrom(
                          side: BorderSide(color: Colors.red.withValues(alpha: 0.3)),
                          padding: const EdgeInsets.symmetric(vertical: 14),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
      bottomNavigationBar: GlassBottomNav(
        currentIndex: _currentNavIndex,
        onTap: (i) {
          setState(() => _currentNavIndex = i);
          final routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
          if (i < routes.length) context.go(routes[i]);
        },
      ),
    ),
    );
  }

  Widget _statCard(IconData icon, String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 4),
            Text(value, style: TextStyle(color: color, fontSize: 18, fontWeight: FontWeight.bold)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
          ],
        ),
      ),
    );
  }

  Widget _profileRow(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(icon, color: Colors.white38, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                Text(value, style: const TextStyle(color: Colors.white, fontSize: 14)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _actionRow(IconData icon, String label, VoidCallback onTap) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 10),
        child: Row(
          children: [
            Icon(icon, color: Colors.white38, size: 20),
            const SizedBox(width: 12),
            Expanded(child: Text(label, style: const TextStyle(color: Colors.white, fontSize: 14))),
            Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3), size: 20),
          ],
        ),
      ),
    );
  }
}
