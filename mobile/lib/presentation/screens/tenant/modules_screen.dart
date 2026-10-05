import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter/services.dart';
import '../../../data/services/api_service.dart';

/// Écran de gestion des modules et fonctionnalités (TenantFeature API).
///
/// T-M13 (SPEC_ORGANISATION_MODULABLE_V3 §7.1) : sélecteur de **nœud** —
/// l'activation se fait alors PAR NŒUD (D, `GET/PUT
/// /tenant/organization/nodes/{id}/features`), réservé à l'admin du tenant
/// ou du nœud (P2). Sans sélection, comportement historique global.
class TenantModulesScreen extends ConsumerStatefulWidget {
  const TenantModulesScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  ConsumerState<TenantModulesScreen> createState() =>
      _TenantModulesScreenState();
}

class _TenantModulesScreenState extends ConsumerState<TenantModulesScreen> {
  late final ApiService _apiService;
  bool _loading = true;
  bool _saving = false;
  List<_ModuleInfo> _modules = [];
  String? _message;
  String? _messageType;
  // T-M13 : portée par nœud (D).
  List<Map<String, dynamic>> _nodes = const [];
  String? _selectedNodeId;
  List<Map<String, dynamic>> _nodeFeatures = const [];

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? ApiService();
    _loadModules();
    _loadNodes();
  }

  /// Liste des nœuds accessibles pour la portée par nœud ; silencieux si
  /// l'API tenant n'est pas disponible (le globale reste utilisable).
  Future<void> _loadNodes() async {
    try {
      final response = await _apiService.get('/tenant/organization/tree');
      final nodes = response.data is List
          ? (response.data as List)
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
          : const <Map<String, dynamic>>[];
      if (mounted) setState(() => _nodes = nodes);
    } catch (_) {
      // Pas de portée nœud sur cette session : on garde le globale.
    }
  }

  Future<void> _selectNode(String? nodeId) async {
    setState(() {
      _selectedNodeId = nodeId;
      _loading = true;
    });
    if (nodeId == null) {
      await _loadModules();
      return;
    }
    try {
      final response =
          await _apiService.get('/tenant/organization/nodes/$nodeId/features');
      setState(() {
        _nodeFeatures = response.data is List
            ? (response.data as List)
                .whereType<Map>()
                .map((e) => Map<String, dynamic>.from(e))
                .toList()
            : const [];
        _loading = false;
      });
    } catch (e) {
      setState(() {
        _loading = false;
        _message = 'Modules du nœud indisponibles: $e';
        _messageType = 'error';
      });
    }
  }

  /// Bascule sur CE nœud : PUT = remplacement de la liste complète, sinon les
  /// autres modules configurés seraient perdus (§5.3).
  Future<void> _toggleNodeFeature(String moduleCode, bool enabled) async {
    final nodeId = _selectedNodeId;
    if (nodeId == null) return;
    HapticFeedback.lightImpact();
    setState(() => _saving = true);
    final payload = <Map<String, dynamic>>[];
    for (final f in _nodeFeatures) {
      payload.add({
        'code': f['moduleCode'],
        'enabled':
            f['moduleCode'] == moduleCode ? enabled : f['enabled'] == true,
        if (f['configurationJson'] != null)
          'configurationJson': f['configurationJson'],
      });
    }
    if (!_nodeFeatures.any((f) => f['moduleCode'] == moduleCode)) {
      payload.add({'code': moduleCode, 'enabled': enabled});
    }
    try {
      final response = await _apiService.put(
        '/tenant/organization/nodes/$nodeId/features',
        data: {'modules': payload},
      );
      setState(() {
        _nodeFeatures = response.data is List
            ? (response.data as List)
                .whereType<Map>()
                .map((e) => Map<String, dynamic>.from(e))
                .toList()
            : _nodeFeatures;
        _message = enabled ? 'Module activé (nœud)' : 'Module désactivé (nœud)';
        _messageType = 'success';
      });
    } catch (e) {
      setState(() {
        _message = 'Erreur: $e';
        _messageType = 'error';
      });
    } finally {
      setState(() => _saving = false);
    }
  }

  bool _nodeModuleEnabled(String code) => _nodeFeatures
      .firstWhere((f) => f['moduleCode'] == code, orElse: () => const {})
      ['enabled'] == true;

  Future<void> _loadModules() async {
    try {
      final response = await _apiService.get('/admin/tenant-features');
      final values = response.data is List
          ? (response.data as List).whereType<Map>()
          : const <Map>[];
      setState(() {
        _modules = values
            .map((item) => _ModuleInfo.fromJson(
                  Map<String, dynamic>.from(item),
                ))
            .toList();
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
      await _apiService.put(
        '/admin/tenant-features/${mod.key}',
        data: {'enabled': newEnabled},
      );
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
                              const Icon(Icons.extension_off,
                                  size: 64, color: Colors.grey),
                              const SizedBox(height: 16),
                              const Text(
                                'Aucun module configuré',
                                style: TextStyle(
                                    fontSize: 18, fontWeight: FontWeight.w500),
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
                          _buildNodeScopeSelector(),
                          const SizedBox(height: 12),
                          if (_message != null) ...[
                            _buildMessage(),
                            const SizedBox(height: 16),
                          ],
                          if (_selectedNodeId == null)
                            ..._modules.map((mod) => _buildModuleCard(mod))
                          else
                            ..._buildNodeScopedCards(),
                        ],
                      ),
                    ),
            ),
    );
  }

  /// T-M13 : sélecteur de portée — global (historique) ou par nœud (D).
  Widget _buildNodeScopeSelector() {
    if (_nodes.isEmpty) return const SizedBox.shrink();
    return DropdownButtonFormField<String?>(
      initialValue: null,
      decoration: const InputDecoration(
        labelText: 'Portée des modules',
        border: OutlineInputBorder(),
      ),
      items: [
        const DropdownMenuItem<String?>(
            value: null, child: Text('Tenant (global)')),
        ..._nodes.map(
          (node) => DropdownMenuItem<String?>(
            value: node['id']?.toString(),
            child: Text([
              node['name']?.toString() ?? 'Unité',
              if (node['levelName'] != null) ' · ${node['levelName']}',
            ].join()),
          ),
        ),
      ],
      onChanged: _saving ? null : _selectNode,
    );
  }

  /// Cartes de modules en portée nœud : état résolu depuis `node_features`.
  List<Widget> _buildNodeScopedCards() {
    final known = _modules.map((m) => m.key).toSet();
    for (final f in _nodeFeatures) {
      final code = f['moduleCode']?.toString();
      if (code != null) known.add(code);
    }
    return [
      for (final code in known.where((c) => c.isNotEmpty))
        Card(
          elevation: 2,
          margin: const EdgeInsets.only(bottom: 12),
          child: SwitchListTile(
            title: Text(moduleLabelStatic(code)),
            subtitle: Text(
                _nodeModuleEnabled(code) ? 'Activé (nœud)' : 'Désactivé'),
            value: _nodeModuleEnabled(code),
            onChanged: _saving
                ? null
                : (v) => _toggleNodeFeature(code, v),
          ),
        ),
    ];
  }

  Widget _buildMessage() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: _messageType == 'success' ? Colors.green[100] : Colors.red[100],
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        _message!,
        style: TextStyle(
          color:
              _messageType == 'success' ? Colors.green[800] : Colors.red[800],
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
                          mod.limits.entries
                              .map((e) => '${e.key}: ${e.value}')
                              .join(', '),
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
              activeThumbColor: Theme.of(context).primaryColor,
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

  String get label => moduleLabelStatic(key);

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

/// Libellé FR d'un code module — partagé avec la fiche nœud V3 (T-M11).
String moduleLabelStatic(String key) {
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
          .map((w) =>
              w.isNotEmpty ? '${w[0].toUpperCase()}${w.substring(1)}' : w)
          .join(' ');
}
