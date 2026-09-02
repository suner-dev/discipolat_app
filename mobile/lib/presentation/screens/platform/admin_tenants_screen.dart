import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Administration des églises (tenants multi-tenant).
class AdminTenantsScreen extends StatefulWidget {
  const AdminTenantsScreen({super.key});

  @override
  State<AdminTenantsScreen> createState() => _AdminTenantsScreenState();
}

class _AdminTenantsScreenState extends State<AdminTenantsScreen> {
  final _apiService = ApiService();
  List<dynamic> _tenants = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/tenants');
      _tenants = (res.data is List ? res.data : []) as List<dynamic>;
    } catch (_) {}
    if (mounted) setState(() => _isLoading = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).tenantsTitle),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _tenants.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.business, color: Colors.white.withValues(alpha: 0.15), size: 48),
                      const SizedBox(height: 12),
                      Text(AppLocalizations.of(context).noTenants,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                    ],
                  ),
                )
              : ListView.builder(
                  padding: const EdgeInsets.all(12),
                  itemCount: _tenants.length,
                  itemBuilder: (_, i) => _tenantCard(_tenants[i] as Map<String, dynamic>),
                ),
    );
  }

  Widget _tenantCard(Map<String, dynamic> t) {
    final l10n = AppLocalizations.of(context);
    final name = t['name']?.toString() ?? '';
    final domain = t['domain']?.toString() ?? '';
    final tenantId = t['id']?.toString() ?? '';
    final status = t['status']?.toString() ?? 'ACTIVE';
    final isActive = status == 'ACTIVE';
    final isSuspended = status == 'SUSPENDED';
    final statusColor = isActive
        ? Colors.green
        : isSuspended
            ? Colors.red
            : Colors.grey;
    final statusLabel = isActive
        ? l10n.statusActive
        : isSuspended
            ? l10n.statusSuspended
            : status;
    final initial = name.isNotEmpty ? name.substring(0, 1).toUpperCase() : '?';

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          Container(
            width: 40,
            height: 40,
            decoration: BoxDecoration(
              gradient: LinearGradient(
                  colors: [statusColor, statusColor.withValues(alpha: 0.7)]),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Center(
              child: Text(initial,
                  style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                      fontSize: 16)),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name,
                    style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.w600,
                        fontSize: 14)),
                Text(domain.isNotEmpty ? domain : tenantId,
                    style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.4),
                        fontSize: 11)),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: statusColor.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Text(statusLabel,
                style: TextStyle(
                    color: statusColor,
                    fontSize: 10,
                    fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}
