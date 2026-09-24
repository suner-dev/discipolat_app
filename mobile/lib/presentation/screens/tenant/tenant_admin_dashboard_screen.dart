import 'package:flutter/material.dart';

import '../../../data/services/api_service.dart';
import '../../../data/services/tenant_admin_usage_service.dart';
import '../../../models/quota_usage.dart';
import '../../../models/tenant_admin_dashboard.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

class TenantAdminDashboardScreen extends StatefulWidget {
  const TenantAdminDashboardScreen({super.key, this.apiService, this.service});

  final ApiService? apiService;
  final TenantAdminUsageService? service;

  @override
  State<TenantAdminDashboardScreen> createState() =>
      _TenantAdminDashboardScreenState();
}

class _TenantAdminDashboardScreenState
    extends State<TenantAdminDashboardScreen> {
  late final TenantAdminUsageService _service = widget.service ??
      TenantAdminUsageService(apiService: widget.apiService ?? ApiService());
  TenantAdminUsageSnapshot? _snapshot;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    if (mounted) {
      setState(() {
        _loading = true;
      });
    }
    final snapshot = await _service.load();
    if (!mounted) return;
    setState(() {
      _snapshot = snapshot;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'TenantAdminDashboardScreen',
      auditAction: AuditActions.viewAdmin,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Tableau de bord administrateur'),
          actions: [
            IconButton(
              onPressed: _loading ? null : _load,
              icon: const Icon(Icons.refresh),
            ),
          ],
        ),
        drawer: const AppDrawer(),
        body: _loading
            ? const Center(child: CircularProgressIndicator())
            : _buildContent(context),
      ),
    );
  }

  Widget _buildContent(BuildContext context) {
    final snapshot = _snapshot;
    final dashboard = snapshot?.dashboard;
    final usage = snapshot?.quotaUsage;

    return RefreshIndicator(
      onRefresh: _load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            dashboard?.tenantName ?? 'Organisation',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 16),
          if (snapshot?.dashboardFailed == true)
            const _StatusCard(
              text: 'Données du tableau de bord indisponibles',
              color: Colors.red,
            ),
          if (snapshot?.settingsFailed == true) ...[
            const SizedBox(height: 8),
            const _StatusCard(
              text: 'Statut des analytics indisponible',
              color: Colors.orange,
            ),
          ],
          const SizedBox(height: 12),
          _sectionTitle('Vue d’ensemble'),
          const SizedBox(height: 8),
          _buildDashboardMetrics(dashboard),
          const SizedBox(height: 20),
          _sectionTitle('Utilisation des quotas'),
          const SizedBox(height: 8),
          _buildQuotaUsage(usage),
          const SizedBox(height: 20),
          _sectionTitle('Analytics'),
          const SizedBox(height: 8),
          _buildAnalyticsStatus(snapshot?.analyticsEnabled),
          const SizedBox(height: 20),
          _sectionTitle('Membres par rôle'),
          const SizedBox(height: 8),
          _buildRoleMetrics(dashboard?.membersByRole),
          const SizedBox(height: 32),
        ],
      ),
    );
  }

  Widget _buildDashboardMetrics(TenantAdminDashboardData? dashboard) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 8,
      mainAxisSpacing: 8,
      childAspectRatio: 1.8,
      children: [
        _MetricCard('Utilisateurs', _number(dashboard?.totalUsers)),
        _MetricCard('Utilisateurs actifs', _number(dashboard?.activeUsers)),
        _MetricCard('Membres', _number(dashboard?.totalMemberships)),
        _MetricCard('Églises', _number(dashboard?.churchCount)),
        _MetricCard('Départements', _number(dashboard?.departmentCount)),
        _MetricCard('Sous-églises', _number(dashboard?.subChurchCount)),
        _MetricCard('Campus', _number(dashboard?.campusCount)),
        _MetricCard('Groupes', _number(dashboard?.groupCount)),
      ],
    );
  }

  Widget _buildQuotaUsage(QuotaUsage? usage) {
    if (usage == null) {
      return const _StatusCard(
        text: 'Données de consommation indisponibles',
        color: Colors.red,
      );
    }

    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 8,
      mainAxisSpacing: 8,
      childAspectRatio: 1.55,
      children: [
        _quotaCard('Utilisateurs', usage.metric('users')),
        _quotaCard('Églises', usage.metric('churches')),
        _quotaCard('Départements', usage.metric('departments')),
        _quotaCard('Campus', usage.metric('campuses')),
        _quotaCard('Groupes', usage.metric('groups')),
        _quotaCard('Stockage', usage.metric('storage'), suffix: ' Mo'),
        _quotaCard('Crédits IA', usage.metric('aiRequests')),
        _quotaCard('Cours', usage.metric('courses')),
        _quotaCard('Messages', usage.metric('messages')),
      ],
    );
  }

  Widget _buildAnalyticsStatus(bool? enabled) {
    if (enabled == false) {
      return const _StatusCard(
        text: 'Analytics désactivés par le tenant',
        color: Colors.orange,
      );
    }
    if (enabled == true) {
      return const _StatusCard(
        text: 'Analytics disponibles',
        color: Colors.green,
      );
    }
    return const _StatusCard(
      text: 'Statut des analytics indisponible',
      color: Colors.orange,
    );
  }

  Widget _buildRoleMetrics(Map<String, num>? roles) {
    if (roles == null || roles.isEmpty) {
      return const _StatusCard(
        text: 'Données des rôles indisponibles',
        color: Colors.orange,
      );
    }
    return Column(
      children: [
        ...roles.entries.map(
          (entry) => ListTile(
            contentPadding: EdgeInsets.zero,
            title: Text(entry.key),
            trailing: Text(entry.value.toString()),
          ),
        ),
      ],
    );
  }

  Widget _quotaCard(String label, QuotaMetric? metric, {String suffix = ''}) {
    final value = _quotaValue(metric, suffix);
    final progress = metric?.usagePercent;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(label, style: const TextStyle(fontSize: 12)),
            const SizedBox(height: 6),
            Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 17),
            ),
            const SizedBox(height: 5),
            Text(
              _quotaDetail(metric),
              style: const TextStyle(fontSize: 10, color: Colors.black54),
            ),
            if (progress != null) ...[
              const SizedBox(height: 6),
              LinearProgressIndicator(
                value: (progress / 100).clamp(0.0, 1.0).toDouble(),
              ),
            ],
          ],
        ),
      ),
    );
  }

  String _quotaValue(QuotaMetric? metric, String suffix) {
    if (metric == null || !metric.hasData || metric.used == null) {
      return 'Indisponible';
    }
    final used = '${metric.used}$suffix';
    if (metric.isUnlimited) return '$used / Illimité';
    if (metric.limit == null) return '$used / Limite inconnue';
    return '$used / ${metric.limit}$suffix';
  }

  String _quotaDetail(QuotaMetric? metric) {
    if (metric == null || !metric.hasData) return 'Donnée non fournie';
    if (metric.isUnlimited) return 'Limite illimitée explicitement';
    if (metric.limit == null) return 'Limite non fournie';
    if (metric.limit == 0) return 'Limite nulle';
    final percent = metric.usagePercent;
    if (percent == null) return 'Pourcentage indisponible';
    return '${percent.toStringAsFixed(1)}% utilisé';
  }

  Widget _sectionTitle(String title) {
    return Text(title, style: const TextStyle(fontWeight: FontWeight.bold));
  }

  String _number(num? value) {
    return value == null ? 'Indisponible' : value.toString();
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard(this.label, this.value);

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(label, style: const TextStyle(fontSize: 12)),
            const SizedBox(height: 6),
            Text(
              value,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Card(
      color: color.withValues(alpha: 0.1),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Text(text, style: TextStyle(color: color)),
      ),
    );
  }
}
