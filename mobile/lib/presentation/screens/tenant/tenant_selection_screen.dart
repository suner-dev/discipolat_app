import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../core/tenant_session.dart';

class TenantSelectionScreen extends ConsumerStatefulWidget {
  const TenantSelectionScreen({super.key});

  @override
  ConsumerState<TenantSelectionScreen> createState() =>
      _TenantSelectionScreenState();
}

class _TenantSelectionScreenState extends ConsumerState<TenantSelectionScreen> {
  final _api = ApiService();
  bool _loading = true;
  bool _switching = false;
  List<Map<String, dynamic>> _tenants = [];
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadTenants();
  }

  Future<void> _loadTenants() async {
    try {
      final response = await _api.get('/tenant-switcher/my-tenants');
      final values =
          response.data is List ? response.data as List : <dynamic>[];
      final tenants = values
          .whereType<Map>()
          .map((item) {
            final data = Map<String, dynamic>.from(item);
            return <String, dynamic>{
              'id': data['tenantId']?.toString() ?? '',
              'name': data['tenantName']?.toString() ?? 'Organisation',
              'slug': data['tenantSlug']?.toString() ?? '',
              'role': data['role']?.toString() ?? 'MEMBRE',
              'scopeType': data['scopeType']?.toString() ?? 'TENANT',
              'scopeId': data['scopeId']?.toString(),
              'status': data['status']?.toString() ?? 'ACTIVE',
            };
          })
          .where((tenant) => (tenant['id'] as String).isNotEmpty)
          .toList();
      await ref.read(tenantSessionProvider).loadTenants(tenants);
      if (mounted) {
        setState(() {
          _tenants = tenants;
          _loading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Impossible de charger vos organisations';
          _loading = false;
        });
      }
    }
  }

  Future<void> _switchTenant(String tenantId) async {
    if (_switching) return;
    setState(() => _switching = true);
    try {
      final response = await _api
          .post('/tenant-switcher/switch', data: {'tenantId': tenantId});
      if (response.data is Map && response.data['accessToken'] != null) {
        await _api.saveTokens(Map<String, dynamic>.from(response.data as Map));
      }
      final session = ref.read(tenantSessionProvider);
      await session.switchTenant(tenantId);
      final contextResponse = await _api.get('/tenant-switcher/context');
      if (contextResponse.data is Map) {
        await session.loadContext(
          Map<String, dynamic>.from(contextResponse.data as Map),
        );
      }
      if (!mounted) return;
      final auth = AuthState();
      context.go(roleHome(auth.activeRole,
          isPlatformSuperAdmin: auth.isPlatformSuperAdmin));
    } catch (_) {
      if (mounted) {
        setState(() => _switching = false);
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Impossible de changer d’organisation')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    if (_error != null) {
      return Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 48, color: Colors.red),
              const SizedBox(height: 12),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              ElevatedButton(
                  onPressed: _loadTenants, child: const Text('Réessayer')),
            ],
          ),
        ),
      );
    }
    if (_tenants.isEmpty) {
      return const Scaffold(
          body: Center(child: Text('Aucune organisation accessible')));
    }
    if (_tenants.length == 1) {
      WidgetsBinding.instance.addPostFrameCallback(
          (_) => _switchTenant(_tenants[0]['id'] as String));
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return Scaffold(
      appBar: AppBar(
          title: const Text('Choisir votre organisation'), centerTitle: true),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _tenants.length,
        itemBuilder: (context, index) {
          final tenant = _tenants[index];
          final name = tenant['name'] as String;
          return Card(
            elevation: 2,
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: Theme.of(context).primaryColor,
                child: Text(name.isNotEmpty ? name[0].toUpperCase() : '?',
                    style: const TextStyle(
                        color: Colors.white, fontWeight: FontWeight.bold)),
              ),
              title: Text(name,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold, fontSize: 16)),
              subtitle: Text(tenant['role'] as String),
              onTap: _switching
                  ? null
                  : () => _switchTenant(tenant['id'] as String),
              trailing: const Icon(Icons.arrow_forward_ios, size: 16),
            ),
          );
        },
      ),
    );
  }
}
