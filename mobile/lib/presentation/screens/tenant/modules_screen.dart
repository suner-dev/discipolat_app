import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../api/api_service.dart';

/// Écran de gestion des modules et fonctionnalités
class TenantModulesScreen extends ConsumerStatefulWidget {
  const TenantModulesScreen({super.key});

  @override
  ConsumerState<TenantModulesScreen> createState() => _TenantModulesScreenState();
}

class _TenantModulesScreenState extends ConsumerState<TenantModulesScreen> {
  bool _loading = true;
  bool _saving = false;
  List<_ModuleInfo> _modules = [];
  String? _message;
  String? _messageType;

  @override
  void initState() {
    super.initState();
    _loadModules();
  }

  Future<void> _loadModules() async {
    try {
      final response = await apiService.get('/admin/modules');
      setState(() {
        _modules = (response as Map<String, dynamic>).entries
            .map((e) => _ModuleInfo(
                  key: e.key,
                  enabled: e.value as bool,
                  label: _formatLabel(e.key),
                ))
            .toList();
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _loading = false;
        _message = 'Erreur lors du chargement';
        _messageType = 'error';
      });
    }
  }

  String _formatLabel(String key) {
    return key
        .replaceAll('_', ' ')
        .split(' ')
        .map((w) => w.isNotEmpty ? '${w[0].toUpperCase()}${w.substring(1)}' : w)
        .join(' ');
  }

  Future<void> _toggleModule(String key, bool enabled) async {
    setState(() => _saving = true);
    try {
      await apiService.put('/admin/modules/$key', {'enabled': !enabled});
      setState(() {
        _message = enabled ? 'Module desactive' : 'Module active';
        _messageType = 'success';
      });
      await Future.delayed(const Duration(seconds: 2));
      _loadModules();
    } catch (e) {
      setState(() {
        _message = 'Erreur: $e';
        _messageType = 'error';
      });
    } finally {
      setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Modules'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadModules,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (_message != null)
                      _buildMessage(),
                    if (_message != null) const SizedBox(height: 16),
                    ..._modules.map((mod) => _buildModuleCard(mod)),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildMessage() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _messageType == 'success'
            ? Colors.green[100]
            : Colors.red[100],
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        _message!,
        style: TextStyle(
          color: _messageType == 'success'
              ? Colors.green[800]
              : Colors.red[800],
        ),
      ),
    );
  }

  Widget _buildModuleCard(_ModuleInfo mod) {
    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    mod.label,
                    style: const TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    mod.enabled ? 'Active' : 'Desactive',
                    style: TextStyle(
                      fontSize: 12,
                      color: mod.enabled ? Colors.green[700] : Colors.grey,
                    ),
                  ),
                ],
              ),
            ),
            Switch(
              value: mod.enabled,
              onChanged: _saving ? null : (value) => _toggleModule(mod.key, mod.enabled),
              activeColor: Theme.of(context).primaryColor,
            ),
          ],
        ),
      ),
    );
  }
}

class _ModuleInfo {
  final String key;
  final bool enabled;
  final String label;

  _ModuleInfo({required this.key, required this.enabled, required this.label});
}
