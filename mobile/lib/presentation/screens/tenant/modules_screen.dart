import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import '../../../api/api_service.dart';

/// Écran de gestion des modules et fonctionnalités (TenantFeature API)
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
      final response = await apiService.get('/admin/tenant-features');
      setState(() {
        _modules = (response as List).map((e) => _ModuleInfo.fromJson(e)).toList();
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _loading = false;
        _message = 'Erreur lors du chargement: $e';
        _messageType = 'error';
      });
    }
  }

  Future<void> _toggleModule(_ModuleInfo mod) async {
    HapticFeedback.lightImpact();
    setState(() => _saving = true);
    try {
      final newEnabled = !mod.enabled;
      await apiService.put('/admin/tenant-features/${mod.key}', {'enabled': newEnabled});
      setState(() {
        _message = newEnabled ? 'Module activé' : 'Module désactivé';
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
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadModules,
            tooltip: 'Actualiser',
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadModules,
              child: _modules.isEmpty
                  ? ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      children: [
                        const SizedBox(height: 100),
                        Center(
                          child: Column(
                            children: [
                              const Icon(Icons.extension_off, size: 64, color: Colors.grey),
                              const SizedBox(height: 16),
                              const Text(
                                'Aucun module configuré',
                                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500),
                              ),
                              const SizedBox(height: 8),
                              const Padding(
                                padding: EdgeInsets.symmetric(horizontal: 32),
                                child: Text(
                                  'Les modules de base (people, events, notifications) sont activés automatiquement.',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(color: Colors.grey),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    )
                  : SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          if (_message != null) ...[
                            _buildMessage(),
                            const SizedBox(height: 16),
                          ],
                          ..._modules.map((mod) => _buildModuleCard(mod)),
                        ],
                      ),
                    ),
            ),
    );
  }

  Widget _buildMessage() {
    return Container(
      width: double.infinity,
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
          fontSize: 14,
        ),
      ),
    );
  }

  Widget _buildModuleCard(_ModuleInfo mod) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 12),
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
                  Row(
                    children: [
                      Text(
                        mod.enabled ? 'Activé' : 'Désactivé',
                        style: TextStyle(
                          fontSize: 12,
                          color: mod.enabled ? Colors.green[700] : Colors.grey,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      if (mod.limits.isNotEmpty) ...[
                        const SizedBox(width: 8),
                        Text(
                          mod.limits.entries.map((e) => '${e.key}: ${e.value}').join(', '),
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.grey[600],
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    mod.key,
                    style: TextStyle(
                      fontSize: 11,
                      color: Colors.grey[500],
                      fontFamily: 'monospace',
                    ),
                  ),
                ],
              ),
            ),
            Switch(
              value: mod.enabled,
              onChanged: _saving ? null : (value) => _toggleModule(mod),
              activeColor: Theme.of(context).primaryColor,
            ),
          ],
        ),
      ),
    );
  }
}

class _ModuleInfo {
  final String id;
  final String tenantId;
  final String key;
  final bool enabled;
  final Map<String, dynamic> configuration;
  final Map<String, dynamic> limits;
  final String createdAt;
  final String updatedAt;

  String get label => _formatLabelStatic(key);

  static String _formatLabelStatic(String key) {
    const labels = {
      'people': 'Membres',
      'events': 'Événements',
      'notifications': 'Notifications',
      'dashboard': 'Tableau de bord',
      'org': 'Organisation',
      'families': 'Familles',
      'groups': 'Groupes',
      'discipleship': 'Discipleship',
      'academy': 'Académie',
      'finance': 'Finances',
      'media': 'Médias',
      'pastoral': 'Pastoral',
      'prayer': 'Prière',
      'assets': 'Matériel',
      'workflow': 'Workflows',
      'custom_fields': 'Champs personnalisés',
      'dress_code': 'Dress Code',
      'health': 'Santé / Infirmerie',
      'reports': 'Rapports',
      'analytics': 'Analytics',
      'messaging': 'Messagerie',
      'documents': 'Documents',
      'calendar': 'Calendrier',
      'forms': 'Formulaires',
      'marketplace': 'Marketplace',
      'ai': 'Intelligence Artificielle',
      'chat': 'Chat',
      'payments': 'Paiements',
    };
    return labels[key] ??
        key
            .replaceAll('_', ' ')
            .split(' ')
            .map((w) => w.isNotEmpty ? '${w[0].toUpperCase()}${w.substring(1)}' : w)
            .join(' ');
  }

  _ModuleInfo({
    required this.id,
    required this.tenantId,
    required this.key,
    required this.enabled,
    required this.configuration,
    required this.limits,
    required this.createdAt,
    required this.updatedAt,
  });

  factory _ModuleInfo.fromJson(Map<String, dynamic> json) {
    return _ModuleInfo(
      id: json['id']?.toString() ?? '',
      tenantId: json['tenantId']?.toString() ?? '',
      key: json['moduleCode']?.toString() ?? '',
      enabled: json['enabled'] ?? false,
      configuration: Map<String, dynamic>.from(json['configuration'] ?? {}),
      limits: Map<String, dynamic>.from(json['limits'] ?? {}),
      createdAt: json['createdAt']?.toString() ?? '',
      updatedAt: json['updatedAt']?.toString() ?? '',
    );
  }
}