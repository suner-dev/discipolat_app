import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../api/api_service.dart';
import '../../core/tenant_session.dart';

/// Écran de gestion de la structure organisationnelle
class OrganizationsScreen extends ConsumerStatefulWidget {
  const OrganizationsScreen({super.key});

  @override
  ConsumerState<OrganizationsScreen> createState() => _OrganizationsScreenState();
}

class _OrganizationsScreenState extends ConsumerState<OrganizationsScreen> {
  bool _loading = true;
  List<Map<String, dynamic>> _nodes = [];

  @override
  void initState() {
    super.initState();
    _loadOrganizations();
  }

  Future<void> _loadOrganizations() async {
    try {
      final response = await apiService.get('/admin/org/tree');
      setState(() {
        _nodes = List<Map<String, dynamic>>.from(response['allNodes'] ?? []);
        _loading = false;
      });
    } catch (e) {
      debugPrint('Erreur chargement organisations: $e');
      setState(() => _loading = false);
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
            onPressed: () {
              // TODO: Ouvrir formulaire creation node
            },
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
                      title: Text(node['name'] ?? '',
                          style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text(
                        '${node['type'] ?? ''} - Niveau ${node['level'] ?? 0}',
                        style: const TextStyle(color: Colors.grey, fontSize: 12),
                      ),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () {
                        // TODO: Ouvrir detail du node
                      },
                    ),
                  );
                },
              ),
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
