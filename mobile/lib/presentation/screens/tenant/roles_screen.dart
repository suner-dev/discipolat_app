import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';

class TenantRolesScreen extends ConsumerStatefulWidget {
  const TenantRolesScreen({super.key});

  @override
  ConsumerState<TenantRolesScreen> createState() => _TenantRolesScreenState();
}

class _TenantRolesScreenState extends ConsumerState<TenantRolesScreen> {
  final ApiService _apiService = ApiService();
  bool _loading = true;
  List<Map<String, dynamic>> _roles = [];

  @override
  void initState() {
    super.initState();
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
                    ),
                  );
                },
              ),
            ),
    );
  }
}
