import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';

/// Écran de gestion de la structure organisationnelle
class OrganizationsScreen extends ConsumerStatefulWidget {
  const OrganizationsScreen({super.key});

  @override
  ConsumerState<OrganizationsScreen> createState() =>
      _OrganizationsScreenState();
}

class _OrganizationsScreenState extends ConsumerState<OrganizationsScreen> {
  final ApiService _apiService = ApiService();
  bool _loading = true;
  List<Map<String, dynamic>> _nodes = [];

  @override
  void initState() {
    super.initState();
    _loadOrganizations();
  }

  Future<void> _loadOrganizations() async {
    try {
      final response = await _apiService.get('/admin/org/tree');
      final data = response.data is Map
          ? Map<String, dynamic>.from(response.data as Map)
          : const <String, dynamic>{};
      setState(() {
        _nodes =
            (data['allNodes'] is List
                    ? (data['allNodes'] as List).whereType<Map>()
                    : const <Map>[])
                .map((item) => Map<String, dynamic>.from(item))
                .toList();
        _loading = false;
      });
    } catch (e) {
      debugPrint('Erreur chargement organisations: $e');
      setState(() => _loading = false);
    }
  }

  Future<void> _showCreateNodeDialog() async {
    final nameController = TextEditingController();
    String type = 'DEPARTMENT';
    String? parentId = _nodes.isEmpty ? null : _nodes.first['id']?.toString();
    final created = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('Ajouter une unité'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameController,
                  autofocus: true,
                  decoration: const InputDecoration(labelText: 'Nom'),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: type,
                  decoration: const InputDecoration(labelText: 'Type'),
                  items: const [
                    DropdownMenuItem(value: 'REGION', child: Text('Région')),
                    DropdownMenuItem(value: 'ROOT_CHURCH', child: Text('Église')),
                    DropdownMenuItem(
                      value: 'SUB_CHURCH',
                      child: Text('Sous-église'),
                    ),
                    DropdownMenuItem(value: 'CAMPUS', child: Text('Campus')),
                    DropdownMenuItem(
                      value: 'DEPARTMENT',
                      child: Text('Département'),
                    ),
                    DropdownMenuItem(value: 'GROUP', child: Text('Groupe')),
                  ],
                  onChanged: (value) => setState(() => type = value ?? type),
                ),
                if (_nodes.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  DropdownButtonFormField<String?>(
                    initialValue: parentId,
                    decoration: const InputDecoration(labelText: 'Parent'),
                    items: [
                      const DropdownMenuItem<String?>(
                        value: null,
                        child: Text('Racine'),
                      ),
                      ..._nodes.map(
                        (node) => DropdownMenuItem<String?>(
                          value: node['id']?.toString(),
                          child: Text(node['name']?.toString() ?? 'Unité'),
                        ),
                      ),
                    ],
                    onChanged: (value) => setState(() => parentId = value),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Annuler'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(
                dialogContext,
                nameController.text.trim().isNotEmpty,
              ),
              child: const Text('Créer'),
            ),
          ],
        ),
      ),
    );
    final name = nameController.text.trim();
    nameController.dispose();
    if (created != true || !mounted || name.isEmpty) return;
    try {
      await _apiService.post(
        '/admin/org/nodes',
        data: {
          'name': name,
          'type': type,
          if (parentId != null) 'parentId': parentId,
        },
      );
      await _loadOrganizations();
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(const SnackBar(content: Text('Unité créée')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Création impossible : $e')));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Structure'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: _showCreateNodeDialog,
            tooltip: 'Ajouter',
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadOrganizations,
              child: ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: _nodes.length,
                itemBuilder: (context, index) {
                  final node = _nodes[index];
                  return Card(
                    elevation: 1,
                    child: ListTile(
                      leading: CircleAvatar(
                        backgroundColor: _getTypeColor(node['type']),
                        child: Icon(
                          _getTypeIcon(node['type']),
                          color: Colors.white,
                        ),
                      ),
                      title: Text(
                        node['name'] ?? '',
                        style: const TextStyle(fontWeight: FontWeight.w600),
                      ),
                      subtitle: Text(
                        '${node['type'] ?? ''} - Niveau ${node['level'] ?? 0}',
                        style: const TextStyle(
                          color: Colors.grey,
                          fontSize: 12,
                        ),
                      ),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () => _showNodeDetails(node),
                    ),
                  );
                },
              ),
            ),
    );
  }

  void _showNodeDetails(Map<String, dynamic> node) {
    showDialog<void>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(node['name']?.toString() ?? 'Unité'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              _detailRow('Identifiant', node['id']?.toString()),
              _detailRow('Type', node['type']?.toString()),
              _detailRow('Niveau', node['level']?.toString()),
              _detailRow('Code', node['code']?.toString()),
              _detailRow('Parent', node['parentId']?.toString()),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext),
            child: const Text('Fermer'),
          ),
        ],
      ),
    );
  }

  Widget _detailRow(String label, String? value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 100,
            child: Text(
              label,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ),
          Expanded(child: Text(value?.isNotEmpty == true ? value! : '—')),
        ],
      ),
    );
  }

  Color _getTypeColor(String? type) {
    switch (type) {
      case 'ROOT_CHURCH':
        return Colors.indigo;
      case 'REGION':
        return Colors.teal;
      case 'CHURCH':
        return Colors.blue;
      case 'SUB_CHURCH':
        return Colors.green;
      case 'CAMPUS':
        return Colors.orange;
      case 'DEPARTMENT':
        return Colors.purple;
      case 'GROUP':
        return Colors.brown;
      default:
        return Colors.grey;
    }
  }

  IconData _getTypeIcon(String? type) {
    switch (type) {
      case 'ROOT_CHURCH':
        return Icons.category;
      case 'REGION':
        return Icons.public;
      case 'CHURCH':
        return Icons.church;
      case 'SUB_CHURCH':
        return Icons.group;
      case 'CAMPUS':
        return Icons.school;
      case 'DEPARTMENT':
        return Icons.business;
      case 'GROUP':
        return Icons.people;
      default:
        return Icons.folder;
    }
  }
}
