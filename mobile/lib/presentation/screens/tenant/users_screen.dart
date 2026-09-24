import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';

/// Écran de gestion des utilisateurs
class TenantUsersScreen extends ConsumerStatefulWidget {
  const TenantUsersScreen({super.key});

  @override
  ConsumerState<TenantUsersScreen> createState() => _TenantUsersScreenState();
}

class _TenantUsersScreenState extends ConsumerState<TenantUsersScreen> {
  final ApiService _apiService = ApiService();
  bool _loading = true;
  List<Map<String, dynamic>> _users = [];

  @override
  void initState() {
    super.initState();
    _loadUsers();
  }

  Future<void> _loadUsers() async {
    try {
      final response = await _apiService.get('/admin/members?size=100');
      final data = response.data is Map
          ? Map<String, dynamic>.from(response.data as Map)
          : const <String, dynamic>{};
      setState(() {
        _users = (data['content'] is List
                ? (data['content'] as List).whereType<Map>()
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
        title: const Text('Utilisateurs'),
        actions: [
          IconButton(
            icon: const Icon(Icons.person_add),
            onPressed: () {
              // TODO: Ouvrir formulaire creation
            },
            tooltip: 'Ajouter un utilisateur',
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadUsers,
              child: ListView.builder(
                padding: const EdgeInsets.all(16),
                itemCount: _users.length,
                itemBuilder: (context, index) {
                  final user = _users[index];
                  return Card(
                    child: ListTile(
                      leading: CircleAvatar(
                        child: Text(
                          (user['firstName']?.toString() ?? '') +
                              (user['lastName']?.toString() ?? '')[0]
                                  .toUpperCase(),
                        ),
                      ),
                      title: Text(
                          '${user['firstName'] ?? ''} ${user['lastName'] ?? ''}'),
                      subtitle: Text(user['email'] ?? ''),
                      trailing: Chip(
                        label: Text(user['role'] ?? 'Membre',
                            style: const TextStyle(fontSize: 12)),
                      ),
                    ),
                  );
                },
              ),
            ),
    );
  }
}
