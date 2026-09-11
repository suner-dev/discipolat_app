import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/tenant_session.dart';
import '../../data/services/auth_service.dart';

/// TenantSelectionScreen — Ecran de selection du tenant (Sections 33-34 du prompt).
class TenantSelectionScreen extends ConsumerStatefulWidget {
  const TenantSelectionScreen({super.key});
  @override
  ConsumerState<TenantSelectionScreen> createState() => _TenantSelectionScreenState();
}

class _TenantSelectionScreenState extends ConsumerState<TenantSelectionScreen> {
  List<Map<String, dynamic>> _tenants = [];
  bool _isLoading = true;
  bool _isSwitching = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadTenants();
  }

  Future<void> _loadTenants() async {
    try {
      final authService = ref.read(authServiceProvider);
      final tenants = await authService.getMyTenants();
      if (mounted) {
        setState(() { _tenants = tenants; _isLoading = false; });
        if (tenants.length == 1) _selectTenant(tenants.first);
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  Future<void> _selectTenant(Map<String, dynamic> tenant) async {
    setState(() => _isSwitching = true);
    try {
      final session = ref.read(tenantSessionProvider);
      await session.setActiveTenant(tenant['id'], tenant['name'], tenant['role'] ?? 'MEMBER');
      if (mounted) {
        final role = tenant['role'] ?? 'MEMBER';
        if (role == 'PLATFORM_SUPER_ADMIN') {
          Navigator.of(context).pushReplacementNamed('/platform/admin');
        } else {
          Navigator.of(context).pushReplacementNamed('/home');
        }
      }
    } catch (e) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Erreur: ${e.toString()}')));
    } finally {
      if (mounted) setState(() => _isSwitching = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: _isLoading ? const Center(child: CircularProgressIndicator())
          : _error != null ? _buildError()
          : _buildTenantList(),
      ),
    );
  }

  Widget _buildError() {
    return Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
      const Icon(Icons.error_outline, size: 64, color: Colors.red),
      const SizedBox(height: 16), Text(_error ?? 'Erreur', textAlign: TextAlign.center),
      const SizedBox(height: 24),
      ElevatedButton.icon(onPressed: _loadTenants, icon: const Icon(Icons.refresh), label: const Text('Reessayer')),
    ]));
  }

  Widget _buildTenantList() {
    return Column(children: [
      Padding(padding: const EdgeInsets.all(24), child: Column(children: [
        const Icon(Icons.account_balance, size: 48, color: Colors.indigo),
        const SizedBox(height: 16),
        Text('Choisir une organisation', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 8),
        Text('Selectionnez l organisation avec laquelle travailler', textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Colors.grey[600])),
      ])),
      Expanded(child: ListView.builder(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: _tenants.length,
        itemBuilder: (context, index) {
          final t = _tenants[index];
          return Card(margin: const EdgeInsets.only(bottom: 12), elevation: 2,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            child: ListTile(
              contentPadding: const EdgeInsets.all(16),
              leading: CircleAvatar(backgroundColor: Colors.indigo[100],
                child: Text(t['name']?.isNotEmpty == true ? t['name'][0].toUpperCase() : '?',
                  style: const TextStyle(color: Colors.indigo, fontWeight: FontWeight.bold))),
              title: Text(t['name'] ?? 'Organisation', style: const TextStyle(fontWeight: FontWeight.w600)),
              subtitle: Chip(label: Text(t['role'] ?? 'MEMBER'), labelStyle: const TextStyle(fontSize: 11)),
              trailing: _isSwitching ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.chevron_right),
              onTap: _isSwitching ? null : () => _selectTenant(t),
            ));
        },
      )),
    ]);
  }
}
