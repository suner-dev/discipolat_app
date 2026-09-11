import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../api/api_service.dart';

/// Écran de gestion des utilisateurs
class TenantUsersScreen extends ConsumerStatefulWidget {
  const TenantUsersScreen({super.key});

  @override
  ConsumerState<TenantUsersScreen> createState() => _TenantUsersScreenState();
}

class _TenantUsersScreenState extends ConsumerState<TenantUsersScreen> {
  bool _loading = true;
  List<Map<String, dynamic>> _users = [];

  @override
  void initState() {
    super.initState();
    _loadUsers();
  }

  Future<void> _loadUsers() async {
    try {
      final response = await apiService.get('/admin/members?size=100');
      setState(() {
        _users = List<Map<String, dynamic>>.from(response['content'] ?? []);
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
                          (user['firstName']?.toString() ?? '')+
                              (user['lastName']?.toString() ?? '')[0]
                                  .toUpperCase(),
                        ),
                      ),
                      title: Text('${user['firstName'] ?? ''} ${user['lastName'] ?? ''}'),
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
