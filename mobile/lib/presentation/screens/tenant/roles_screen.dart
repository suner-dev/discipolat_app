import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';
import '../../../data/models/organization_v3_models.dart';

/// Rôles du tenant.
///
/// T-M12 (SPEC_ORGANISATION_MODULABLE_V3 §7.1) : capacité ≠ intitulé (B) —
/// édition du libellé affiché PAR NŒUD via `PUT /tenant/roles/{id}/titles`,
/// et lecture des intitulés existants. L'autorisation ne dépend JAMAIS du
/// libellé (garde §11) : cet écran ne touche pas aux permissions.
class TenantRolesScreen extends ConsumerStatefulWidget {
  const TenantRolesScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  ConsumerState<TenantRolesScreen> createState() => _TenantRolesScreenState();
}

class _TenantRolesScreenState extends ConsumerState<TenantRolesScreen> {
  late final ApiService _apiService;
  bool _loading = true;
  List<Map<String, dynamic>> _roles = [];

  @override
  void initState() {
    super.initState();
    _apiService = widget.apiService ?? ApiService();
    _loadRoles();
  }

  Future<void> _loadRoles() async {
    try {
      final response = await _apiService.get('/admin/roles/overview');
      setState(() {
        _roles = (response.data is List
                ? (response.data as List).whereType<Map>()
                : const <Map>[])
            .map((item) => Map<String, dynamic>.from(item))
            .toList();
        _loading = false;
      });
    } catch (e) {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Roles'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadRoles,
              child: ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: _roles.length,
                itemBuilder: (context, index) {
                  final role = _roles[index];
                  return Card(
                    child: ListTile(
                      title: Text(role['label'] ?? 'Role'),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(role['key'] ?? '',
                              style: const TextStyle(
                                  color: Colors.grey, fontSize: 12)),
                          if (role['permissions'] != null)
                            Text('${role['permissions'].length} permissions',
                                style: const TextStyle(
                                    color: Colors.grey, fontSize: 12)),
                        ],
                      ),
                      trailing: role['isSystem'] == true
                          ? const Chip(
                              label: Text('Systeme',
                                  style: TextStyle(fontSize: 10)),
                            )
                          : const Icon(Icons.edit, size: 20),
                      // T-M12 : ouvrir la matrice d'intitulés par nœud.
                      onTap: () => _showTitlesSheet(role),
                    ),
                  );
                },
              ),
            ),
    );
  }

  /// Intitulés (B) du rôle : liste par nœud + libellé effectif + édition.
  void _showTitlesSheet(Map<String, dynamic> role) {
    final roleId = role['id']?.toString() ?? '';
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => _RoleTitlesSheet(
        apiService: _apiService,
        roleId: roleId,
        roleLabel: role['label']?.toString() ?? 'Rôle',
      ),
    );
  }
}

class _RoleTitlesSheet extends StatefulWidget {
  const _RoleTitlesSheet({
    required this.apiService,
    required this.roleId,
    required this.roleLabel,
  });

  final ApiService apiService;
  final String roleId;
  final String roleLabel;

  @override
  State<_RoleTitlesSheet> createState() => _RoleTitlesSheetState();
}

class _RoleTitlesSheetState extends State<_RoleTitlesSheet> {
  bool _loading = true;
  String? _error;
  List<RoleTitle> _titles = const [];
  List<Map<String, dynamic>> _nodes = const [];
  String? _effectiveLabel;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final results = await Future.wait([
        widget.apiService.get('/tenant/roles/${widget.roleId}/titles'),
        widget.apiService.get('/tenant/organization/tree'),
      ]);
      final rawTitles = results[0].data is List
          ? (results[0].data as List).whereType<Map>().toList()
          : const <Map>[];
      final nodes = results[1].data is List
          ? (results[1].data as List)
              .whereType<Map>()
              .map((e) => Map<String, dynamic>.from(e))
              .toList()
          : const <Map<String, dynamic>>[];
      // Le serveur ajoute une ligne marqueur {resolved:true, effectiveLabel}.
      String? effective;
      final titles = <RoleTitle>[];
      for (final t in rawTitles) {
        final map = Map<String, dynamic>.from(t);
        if (map['resolved'] == true) {
          effective = map['effectiveLabel']?.toString();
        } else {
          titles.add(RoleTitle.fromJson(map));
        }
      }
      setState(() {
        _titles = titles;
        _nodes = nodes;
        _effectiveLabel = effective;
        _loading = false;
        _error = null;
      });
    } catch (e) {
      setState(() {
        _loading = false;
        _error = 'Chargement des intitulés impossible : $e';
      });
    }
  }

  Future<void> _editTitle({String? nodeId, String? currentLabel}) async {
    final nodeName = nodeId == null
        ? 'défaut (tout le tenant)'
        : _nodes.firstWhere((n) => n['id'] == nodeId,
                orElse: () => const {})['name'] ??
            'nœud';
    final controller = TextEditingController(text: currentLabel ?? '');
    final label = await showDialog<String>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text('Intitulé — $nodeName'),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(labelText: 'Libellé affiché'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Annuler'),
          ),
          FilledButton(
            onPressed: () =>
                Navigator.pop(dialogContext, controller.text.trim()),
            child: const Text('Enregistrer'),
          ),
        ],
      ),
    );
    controller.dispose();
    if (label == null || label.isEmpty || !mounted) return;
    try {
      // Sous-ensemble du contrat web UpsertTitleRequest(nodeId, label, …) ;
      // labelPlural null = laissé tel quel côté serveur.
      await widget.apiService.put(
        '/tenant/roles/${widget.roleId}/titles',
        data: {'nodeId': nodeId, 'label': label},
      );
      await _load();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('Enregistrement impossible : $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final byNode = {
      for (final t in _titles) t.nodeId: t,
    };
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: _loading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Intitulés — ${widget.roleLabel}',
                        style: const TextStyle(
                            fontSize: 16, fontWeight: FontWeight.bold)),
                    if (_error != null)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        child: Text(_error!,
                            style: const TextStyle(color: Colors.red)),
                      ),
                    if (_effectiveLabel != null)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 4),
                        child: Text('Libellé effectif : $_effectiveLabel',
                            style: const TextStyle(color: Colors.grey)),
                      ),
                    const SizedBox(height: 8),
                    // Défaut tenant (nodeId null).
                    ListTile(
                      dense: true,
                      title: const Text('Défaut (tout le tenant)'),
                      subtitle: Text(byNode[null]?.label ?? 'non défini'),
                      trailing: const Icon(Icons.edit, size: 18),
                      onTap: () => _editTitle(
                          nodeId: null, currentLabel: byNode[null]?.label),
                    ),
                    const Divider(height: 24),
                    const Text('Par nœud',
                        style: TextStyle(fontWeight: FontWeight.w600)),
                    if (_nodes.isEmpty)
                      const Padding(
                        padding: EdgeInsets.symmetric(vertical: 8),
                        child: Text('Aucun nœud accessible.',
                            style: TextStyle(color: Colors.grey)),
                      )
                    else
                      ..._nodes.map((node) {
                        final id = node['id']?.toString();
                        return ListTile(
                          dense: true,
                          title: Text(node['name']?.toString() ?? 'Unité'),
                          subtitle: Text(
                              byNode[id]?.label ?? 'reprise du défaut'),
                          trailing: const Icon(Icons.edit, size: 18),
                          onTap: () => _editTitle(
                              nodeId: id, currentLabel: byNode[id]?.label),
                        );
                      }),
                  ],
                ),
              ),
      ),
    );
  }
}
