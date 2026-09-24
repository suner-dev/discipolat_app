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
        _users =
            (data['content'] is List
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

  Future<void> _showInviteDialog() async {
    final emailController = TextEditingController();
    String role = 'MEMBRE';
    final invited = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => StatefulBuilder(
        builder: (context, setState) => AlertDialog(
          title: const Text('Inviter un membre'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: emailController,
                autofocus: true,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(labelText: 'Adresse email'),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: role,
                decoration: const InputDecoration(labelText: 'Rôle'),
                items: const [
                  DropdownMenuItem(value: 'MEMBRE', child: Text('Membre')),
                  DropdownMenuItem(value: 'FAISEUR', child: Text('Faiseur')),
                  DropdownMenuItem(
                    value: 'CHEF_DE_FAMILLE',
                    child: Text('Chef de famille'),
                  ),
                  DropdownMenuItem(
                    value: 'RESPONSABLE',
                    child: Text('Responsable'),
                  ),
                ],
                onChanged: (value) => setState(() => role = value ?? role),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Annuler'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(
                dialogContext,
                emailController.text.trim().isNotEmpty,
              ),
              child: const Text('Envoyer'),
            ),
          ],
        ),
      ),
    );
    final email = emailController.text.trim();
    emailController.dispose();
    if (invited != true || !mounted || email.isEmpty) return;
    try {
      await _apiService.post(
        '/admin/invitations',
        data: {'email': email, 'role': role, 'scopeType': 'TENANT'},
      );
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Invitation envoyée à $email')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Invitation impossible : $e')));
      }
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
            onPressed: _showInviteDialog,
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
                        '${user['firstName'] ?? ''} ${user['lastName'] ?? ''}',
                      ),
                      subtitle: Text(user['email'] ?? ''),
                      trailing: Chip(
                        label: Text(
                          user['role'] ?? 'Membre',
                          style: const TextStyle(fontSize: 12),
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
    );
  }
}
