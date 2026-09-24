import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';

class PlatformAdminDashboardScreen extends StatefulWidget {
  const PlatformAdminDashboardScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<PlatformAdminDashboardScreen> createState() =>
      _PlatformAdminDashboardScreenState();
}

class _PlatformAdminDashboardScreenState
    extends State<PlatformAdminDashboardScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _dashboard;
  List<Map<String, dynamic>> _plans = [];
  List<Map<String, dynamic>> _tenants = [];
  List<Map<String, dynamic>> _featureFlags = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (mounted) setState(() => _loading = true);
    try {
      final responses = await Future.wait([
        _api.get('/platform/admin/dashboard'),
        _api.get('/platform/admin/plans'),
        _api.get('/platform/admin/tenants',
            queryParameters: {'page': 0, 'size': 5}),
        _api.get('/platform/admin/feature-flags'),
      ]);
      if (!mounted) return;
      final dashboard = Map<String, dynamic>.from(responses[0].data as Map);
      final plans = List<dynamic>.from(responses[1].data as List);
      final tenantPage = Map<String, dynamic>.from(responses[2].data as Map);
      setState(() {
        _dashboard = dashboard;
        _plans = plans
            .whereType<Map>()
            .map((item) => Map<String, dynamic>.from(item))
            .toList();
        _tenants = List<dynamic>.from(tenantPage['content'] as List? ?? [])
            .whereType<Map>()
            .map((item) => Map<String, dynamic>.from(item))
            .toList();
        final flags = Map<String, dynamic>.from(responses[3].data as Map);
        _featureFlags = List<dynamic>.from(flags['details'] as List? ?? [])
            .whereType<Map>()
            .map((item) => Map<String, dynamic>.from(item))
            .toList();
        _error = null;
        _loading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Impossible de charger les données de la plateforme.';
          _loading = false;
        });
      }
    }
  }

  int _number(Object? value) => value is num ? value.toInt() : 0;

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'PlatformAdminDashboardScreen',
      auditAction: 'platformDashboardView',
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Administration plateforme'),
          actions: [
            IconButton(onPressed: _load, icon: const Icon(Icons.refresh)),
          ],
        ),
        drawer: const AppDrawer(),
        body: _loading
            ? const ShimmerLoading(itemCount: 6)
            : _error != null
                ? _ErrorState(message: _error!, onRetry: _load)
                : RefreshIndicator(
                    onRefresh: _load,
                    child: ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        _Header(
                            onProvision: () =>
                                context.go('/platform/onboarding')),
                        const SizedBox(height: 18),
                        _Metrics(dashboard: _dashboard),
                        const SizedBox(height: 18),
                        _SectionTitle(
                          title: 'Actions rapides',
                          child: Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: [
                              _ActionButton(
                                icon: Icons.rocket_launch_rounded,
                                label: 'Provisionner',
                                onTap: () => context.go('/platform/onboarding'),
                              ),
                              _ActionButton(
                                icon: Icons.business_rounded,
                                label: 'Gérer les églises',
                                onTap: () => context.go('/admin/tenants'),
                              ),
                              _ActionButton(
                                icon: Icons.manage_search_rounded,
                                label: 'Voir l’audit',
                                onTap: () => context.go('/platform/audit'),
                              ),
                              _ActionButton(
                                icon: Icons.assignment_turned_in_rounded,
                                label: 'Demandes d’églises',
                                onTap: () => context
                                    .go('/platform/registration-requests'),
                              ),
                              _ActionButton(
                                icon: Icons.login_rounded,
                                label: 'Impersonation',
                                onTap: () =>
                                    context.go('/platform/impersonation'),
                              ),
                              _ActionButton(
                                icon: Icons.refresh_rounded,
                                label: 'Actualiser',
                                onTap: _load,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 18),
                        _SectionTitle(
                          title: 'Dernières églises',
                          child: TextButton(
                            onPressed: () => context.go('/admin/tenants'),
                            child: const Text('Voir tout'),
                          ),
                        ),
                        ..._tenants.map(_tenantRow),
                        const SizedBox(height: 18),
                        _SectionTitle(
                          title: 'Plans actifs',
                          child: TextButton(
                            onPressed: () => context.go('/admin/tenants'),
                            child: const Text('Gérer'),
                          ),
                        ),
                        ..._plans.map(_planRow),
                        const SizedBox(height: 18),
                        const _SectionTitle(
                            title: 'Fonctionnalités', child: SizedBox.shrink()),
                        ..._featureFlags.map(_featureRow),
                      ],
                    ),
                  ),
      ),
    );
  }

  Widget _tenantRow(Map<String, dynamic> tenant) {
    final status = tenant['status']?.toString() ?? 'UNKNOWN';
    final color = status == 'ACTIVE'
        ? Colors.green
        : status == 'SUSPENDED'
            ? Colors.red
            : Colors.orange;
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: color.withValues(alpha: 0.2),
            foregroundColor: color,
            child: Text((tenant['name']?.toString().isNotEmpty ?? false)
                ? tenant['name'].toString()[0].toUpperCase()
                : '?'),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(tenant['name']?.toString() ?? 'Sans nom',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600)),
                const SizedBox(height: 4),
                Text('${tenant['slug'] ?? ''} · ${tenant['plan'] ?? ''}',
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.55),
                        fontSize: 12)),
              ],
            ),
          ),
          Text(status,
              style: TextStyle(
                  color: color, fontSize: 11, fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  Widget _featureRow(Map<String, dynamic> flag) {
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                    flag['name']?.toString() ??
                        flag['key']?.toString() ??
                        'Fonctionnalité',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.w600)),
                if (flag['description'] != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 3),
                    child: Text(flag['description'].toString(),
                        style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.5),
                            fontSize: 11)),
                  ),
              ],
            ),
          ),
          Switch(
            value: flag['enabled'] == true,
            onChanged: (value) async {
              try {
                await _api.put('/platform/admin/feature-flags/${flag['key']}',
                    data: {'enabled': value});
                if (mounted) setState(() => flag['enabled'] = value);
              } catch (_) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Mise à jour impossible')));
                }
              }
            },
          ),
        ],
      ),
    );
  }

  Widget _planRow(Map<String, dynamic> plan) {
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          const Icon(Icons.workspace_premium_rounded,
              color: Colors.indigoAccent),
          const SizedBox(width: 12),
          Expanded(
              child: Text(
                  plan['name']?.toString() ?? plan['key']?.toString() ?? 'Plan',
                  style: const TextStyle(
                      color: Colors.white, fontWeight: FontWeight.w600))),
          Text('${_number(plan['priceMonthly'])} / mois',
              style: const TextStyle(color: Colors.white70)),
          Switch(
            value: plan['isActive'] == true,
            onChanged: (value) async {
              try {
                await _api.post('/platform/admin/plans', data: {
                  'key': plan['key'],
                  'isActive': value,
                });
                if (mounted) setState(() => plan['isActive'] = value);
              } catch (_) {
                if (mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Modification impossible')));
                }
              }
            },
          ),
        ],
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.onProvision});

  final VoidCallback onProvision;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(18),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
                color: Colors.indigo.withValues(alpha: 0.2),
                borderRadius: BorderRadius.circular(14)),
            child: const Icon(Icons.shield_rounded,
                color: Colors.indigoAccent, size: 28),
          ),
          const SizedBox(width: 14),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Super Admin',
                    style: TextStyle(
                        color: Colors.white,
                        fontSize: 18,
                        fontWeight: FontWeight.bold)),
                SizedBox(height: 4),
                Text('Pilotage global de la plateforme',
                    style: TextStyle(color: Colors.white70, fontSize: 13)),
              ],
            ),
          ),
          IconButton(
              onPressed: onProvision,
              icon: const Icon(Icons.rocket_launch_rounded,
                  color: Colors.indigoAccent)),
        ],
      ),
    );
  }
}

class _Metrics extends StatelessWidget {
  const _Metrics({required this.dashboard});

  final Map<String, dynamic>? dashboard;

  int _number(Object? value) => value is num ? value.toInt() : 0;

  @override
  Widget build(BuildContext context) {
    final tenants = dashboard?['tenants'];
    final users = dashboard?['users'];
    final organizations = dashboard?['organizations'];
    final activity = dashboard?['activity'];
    final tenantMap = tenants is Map ? tenants : const {};
    final userMap = users is Map ? users : const {};
    final organizationMap = organizations is Map ? organizations : const {};
    final activityMap = activity is Map ? activity : const {};
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 10,
      mainAxisSpacing: 10,
      childAspectRatio: 1.7,
      children: [
        _MetricCard(
            label: 'Églises',
            value: _number(tenantMap['total']),
            detail: '${_number(tenantMap['active'])} actives',
            color: Colors.indigo),
        _MetricCard(
            label: 'Utilisateurs',
            value: _number(userMap['total']),
            detail: '${_number(userMap['active'])} actifs',
            color: Colors.teal),
        _MetricCard(
            label: 'Départements',
            value: _number(organizationMap['departments']),
            detail: '${_number(organizationMap['churches'])} églises',
            color: Colors.orange),
        _MetricCard(
            label: 'Audit 7 jours',
            value: _number(activityMap['auditLogsLast7Days']),
            detail: 'actions récentes',
            color: Colors.purple),
      ],
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard(
      {required this.label,
      required this.value,
      required this.detail,
      required this.color});

  final String label;
  final int value;
  final String detail;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Text(label,
              style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
          const SizedBox(height: 4),
          Text('$value',
              style: TextStyle(
                  color: color, fontSize: 24, fontWeight: FontWeight.bold)),
          Text(detail,
              style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.45), fontSize: 11)),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
            child: Text(title,
                style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold))),
        child,
      ],
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton(
      {required this.icon, required this.label, required this.onTap});

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onTap,
      icon: Icon(icon, size: 17),
      label: Text(label),
      style: OutlinedButton.styleFrom(
          foregroundColor: Colors.white,
          side: BorderSide(color: Colors.white.withValues(alpha: 0.2))),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded,
                color: Colors.redAccent, size: 48),
            const SizedBox(height: 12),
            Text(message,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white)),
            const SizedBox(height: 16),
            ElevatedButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh),
                label: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }
}
