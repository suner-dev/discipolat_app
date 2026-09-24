import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../../data/services/api_service.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/secure_screen.dart';

class AdminTenantsScreen extends StatefulWidget {
  const AdminTenantsScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<AdminTenantsScreen> createState() => _AdminTenantsScreenState();
}

class _AdminTenantsScreenState extends State<AdminTenantsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  final _search = TextEditingController();
  List<Map<String, dynamic>> _tenants = [];
  bool _loading = true;
  bool _saving = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _search.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    if (mounted) setState(() => _loading = true);
    try {
      final response = await _api.get(
        '/platform/admin/tenants',
        queryParameters: {
          'page': 0,
          'size': 100,
          if (_search.text.trim().isNotEmpty) 'search': _search.text.trim(),
        },
      );
      final data = Map<String, dynamic>.from(response.data as Map);
      final content = List<dynamic>.from(data['content'] as List? ?? []);
      if (!mounted) return;
      setState(() {
        _tenants = content
            .whereType<Map>()
            .map((item) => Map<String, dynamic>.from(item))
            .toList();
        _error = null;
        _loading = false;
      });
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Impossible de charger les églises.';
          _loading = false;
        });
      }
    }
  }

  Future<void> _changeStatus(Map<String, dynamic> tenant, String action) async {
    final id = tenant['id']?.toString();
    if (id == null || id.isEmpty) return;
    final verb = action == 'archive'
        ? 'archiver'
        : action == 'suspend'
            ? 'suspendre'
            : 'reactiver';
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text('Confirmer le changement'),
        content: Text('Voulez-vous $verb « ${tenant['name']} » ?'),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Annuler')),
          ElevatedButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Confirmer')),
        ],
      ),
    );
    if (confirmed != true) return;
    if (mounted) setState(() => _saving = true);
    try {
      await _api.post('/platform/admin/tenants/$id/$action');
      await _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Action impossible')));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return SecureScreen(
      screenName: 'AdminTenantsScreen',
      auditAction: 'platformTenantsView',
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Églises de la plateforme'),
          actions: [
            IconButton(
                onPressed: _saving ? null : _load,
                icon: const Icon(Icons.refresh)),
            IconButton(
                onPressed: () => context.go('/platform/onboarding'),
                icon: const Icon(Icons.add)),
          ],
        ),
        drawer: const AppDrawer(),
        body: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
              child: TextField(
                controller: _search,
                onSubmitted: (_) => _load(),
                decoration: InputDecoration(
                  hintText: 'Rechercher une église',
                  prefixIcon: const Icon(Icons.search),
                  suffixIcon: IconButton(
                      onPressed: _load, icon: const Icon(Icons.arrow_forward)),
                ),
              ),
            ),
            Expanded(
              child: _loading
                  ? const ShimmerLoading(itemCount: 6)
                  : _error != null
                      ? _ErrorState(message: _error!, onRetry: _load)
                      : _tenants.isEmpty
                          ? const Center(
                              child: Text('Aucune église trouvée',
                                  style: TextStyle(color: Colors.white70)))
                          : RefreshIndicator(
                              onRefresh: _load,
                              child: ListView.builder(
                                padding: const EdgeInsets.all(12),
                                itemCount: _tenants.length,
                                itemBuilder: (_, index) =>
                                    _tenantCard(_tenants[index]),
                              ),
                            ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _tenantCard(Map<String, dynamic> tenant) {
    final status = tenant['status']?.toString() ?? 'UNKNOWN';
    final color = switch (status) {
      'ACTIVE' => Colors.green,
      'SUSPENDED' => Colors.red,
      'CANCELLED' => Colors.grey,
      _ => Colors.orange,
    };
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: color.withValues(alpha: 0.18),
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
                        color: Colors.white.withValues(alpha: 0.5),
                        fontSize: 12)),
              ],
            ),
          ),
          Text(status,
              style: TextStyle(
                  color: color, fontSize: 10, fontWeight: FontWeight.bold)),
          const SizedBox(width: 4),
          PopupMenuButton<String>(
            enabled: !_saving,
            onSelected: (action) => _changeStatus(tenant, action),
            itemBuilder: (_) => [
              if (status == 'ACTIVE')
                const PopupMenuItem(value: 'suspend', child: Text('Suspendre')),
              if (status == 'SUSPENDED' || status == 'CANCELLED')
                const PopupMenuItem(
                    value: 'reactivate', child: Text('Réactiver')),
              if (status != 'CANCELLED')
                const PopupMenuItem(value: 'archive', child: Text('Archiver')),
            ],
          ),
        ],
      ),
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
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.error_outline, color: Colors.redAccent, size: 42),
          const SizedBox(height: 12),
          Text(message, style: const TextStyle(color: Colors.white)),
          const SizedBox(height: 12),
          OutlinedButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Réessayer')),
        ],
      ),
    );
  }
}
