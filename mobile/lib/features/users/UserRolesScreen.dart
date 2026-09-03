import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Gestion des rôles utilisateur — branché sur /api/v1/users/{id}/roles.
class UserRolesScreen extends StatefulWidget {
  final String userId;
  const UserRolesScreen({super.key, required this.userId});

  @override
  State<UserRolesScreen> createState() => _UserRolesScreenState();
}

class _UserRolesScreenState extends State<UserRolesScreen> {
  final _apiService = ApiService();
  List<dynamic> _roles = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final res = await _apiService.get('/users/${widget.userId}/roles');
      if (mounted) {
        final data = res.data;
        setState(() {
          _roles = data is List
              ? data
              : (data is Map && data['roles'] is List
                  ? data['roles'] as List<dynamic>
                  : (data is Map && data['content'] is List
                      ? data['content'] as List<dynamic>
                      : <dynamic>[]));
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement des rôles';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _addRole(String role) async {
    try {
      await _apiService.post('/users/${widget.userId}/roles/$role');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Rôle $role ajouté'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de l\'ajout du rôle'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _removeRole(String role) async {
    try {
      await _apiService.delete('/users/${widget.userId}/roles/$role');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Rôle $role retiré'), backgroundColor: Colors.orange),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors du retrait du rôle'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _promoteChef() async {
    try {
      await _apiService.post('/users/${widget.userId}/promote-chef');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Promu au poste de chef'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la promotion'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _demoteChef() async {
    try {
      await _apiService.post('/users/${widget.userId}/demote-chef');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Retiré du poste de chef'), backgroundColor: Colors.orange),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors du retrait'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Color _roleColor(String role) {
    switch (role.toUpperCase()) {
      case 'ADMIN':
        return Colors.red;
      case 'CHEF':
      case 'PASTEUR':
        return Colors.amber;
      case 'LEADER':
        return Colors.blue;
      case 'MEMBRE':
        return Colors.green;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Rôles de l\'utilisateur'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showAddRoleDialog,
        icon: const Icon(Icons.person_add, size: 18),
        label: const Text('Ajouter un rôle'),
        backgroundColor: AppColors.primary,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text('Actions rapides',
                                style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                            const SizedBox(height: 12),
                            Row(
                              children: [
                                Expanded(
                                  child: FilledButton.icon(
                                    onPressed: _promoteChef,
                                    icon: const Icon(Icons.upgrade, size: 16),
                                    label: const Text('Promouvoir chef'),
                                    style: FilledButton.styleFrom(backgroundColor: Colors.amber),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: _demoteChef,
                    icon: const Icon(Icons.arrow_downward, size: 16),
                                    label: const Text('Retirer chef'),
                                    style: OutlinedButton.styleFrom(foregroundColor: Colors.orange),
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      if (_roles.isEmpty)
                        _buildEmpty()
                      else
                        ..._roles.map((r) {
                          final role = r is String ? r : (r as Map<String, dynamic>)['name']?.toString() ?? r['role']?.toString() ?? '';
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(14),
                            child: Row(
                              children: [
                                Container(
                                  width: 44,
                                  height: 44,
                                  decoration: BoxDecoration(
                                    color: _roleColor(role).withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: Icon(Icons.shield, color: _roleColor(role), size: 20),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Text(role, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                ),
                                StatusBadge(label: role, color: _roleColor(role)),
                                const SizedBox(width: 8),
                                IconButton(
                                  icon: Icon(Icons.delete_outline, color: Colors.white.withValues(alpha: 0.3), size: 20),
                                  onPressed: () => _removeRole(role),
                                ),
                              ],
                            ),
                          );
                        }),
                      const SizedBox(height: 80),
                    ],
                  ),
                ),
    );
  }

  void _showAddRoleDialog() {
    final roles = ['MEMBRE', 'LEADER', 'CHEF', 'PASTEUR', 'ADMIN'];
    showModalBottomSheet(
      context: context,
      backgroundColor: const Color(0xFF111827),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text('Ajouter un rôle', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
            ),
            ...roles.map((r) => ListTile(
                  leading: Icon(Icons.shield, color: _roleColor(r)),
                  title: Text(r, style: const TextStyle(color: Colors.white)),
                  onTap: () {
                    Navigator.pop(ctx);
                    _addRole(r);
                  },
                )),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.shield, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun rôle assigné', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
          const SizedBox(height: 12),
          Text(_error!, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)), textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: _loadData, icon: const Icon(Icons.refresh, size: 16), label: const Text('Réessayer')),
        ],
      ),
    );
  }
}
