import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class PermissionsScreen extends StatefulWidget {
  const PermissionsScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<PermissionsScreen> createState() => _PermissionsScreenState();
}

class _PermissionsScreenState extends State<PermissionsScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _permissions = [];
  bool _isLoading = true;

  static const _roleLabels = {
    'ADMIN': 'Admin',
    'PASTEUR': 'Pasteur',
    'RESPONSABLE': 'Resp.',
    'FAISEUR': 'Faiseur',
    'CHEF_DE_FAMILLE': 'Chef',
    'MEMBRE': 'Membre',
  };

  static const _permLabels = {
    'USER_CREATE': 'Créer utilisateur',
    'USER_READ': 'Voir utilisateurs',
    'USER_UPDATE': 'Modifier utilisateur',
    'USER_DELETE': 'Supprimer utilisateur',
    'FAMILY_CREATE': 'Créer famille',
    'FAMILY_READ': 'Voir familles',
    'FAMILY_UPDATE': 'Modifier famille',
    'FAMILY_DELETE': 'Supprimer famille',
    'SOUL_CREATE': 'Créer âme',
    'SOUL_READ': 'Voir âmes',
    'SOUL_UPDATE': 'Modifier âme',
    'SOUL_DELETE': 'Supprimer âme',
    'REPORT_CREATE': 'Créer rapport',
    'REPORT_READ': 'Voir rapports',
    'REPORT_EXPORT': 'Exporter rapports',
    'REPORT_CORRECT': 'Corriger rapport',
    'AUDIT_READ': 'Voir audit',
    'BULK_IMPORT': 'Import en masse',
    'PERMISSION_MANAGE': 'Gérer permissions',
  };

  @override
  void initState() { super.initState(); _loadData(); }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/permissions');
      if (mounted) {
        _permissions = (res.data as List).cast<Map<String, dynamic>>();
        setState(() => _isLoading = false);
      }
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _togglePermission(String role, String permission, bool enabled) async {
    try {
      await _apiService.put('/permissions/$role/$permission', data: {'enabled': !enabled});
      _loadData();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la mise à jour')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    // Group permissions by permission name
    final permNames = _permissions.map((p) => p['permission'] as String).toSet().toList()..sort();
    final roleNames = _permissions.map((p) => p['role'] as String).toSet().toList()..sort();

    return Scaffold(
      appBar: AppBar(title: const Text('Permissions')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    GlassCard(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(children: [
                            Icon(Icons.shield, color: AppColors.primary, size: 20),
                            const SizedBox(width: 8),
                            const Text('Matrice des permissions',
                                style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                          ]),
                          const SizedBox(height: 4),
                          Text('Configuration fine des accès par rôle',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    if (_permissions.isEmpty)
                      GlassCard(
                        padding: const EdgeInsets.all(32),
                        child: Center(
                          child: Text('Aucune permission configurée',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4))),
                        ),
                      )
                    else ...[
                      // Table header
                      SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: DataTable(
                          headingRowColor: WidgetStatePropertyAll(Colors.white.withValues(alpha: 0.04)),
                          dataRowColor: WidgetStatePropertyAll(Colors.transparent),
                          headingTextStyle: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                          dataTextStyle: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11),
                          columnSpacing: 16,
                          horizontalMargin: 12,
                          columns: [
                            const DataColumn(label: Text('Permission')),
                            ...roleNames.map((role) => DataColumn(
                              label: Text(_roleLabels[role] ?? role, textAlign: TextAlign.center),
                              numeric: true,
                            )),
                          ],
                          rows: permNames.map((perm) {
                            return DataRow(cells: [
                              DataCell(
                                Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    Text(_permLabels[perm] ?? perm,
                                        style: const TextStyle(color: Colors.white, fontSize: 12)),
                                    Text(perm,
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 8)),
                                  ],
                                ),
                              ),
                              ...roleNames.map((role) {
                                final p = _permissions.firstWhere(
                                  (p) => p['permission'] == perm && p['role'] == role,
                                  orElse: () => {'enabled': false},
                                );
                                final enabled = p['enabled'] as bool? ?? false;
                                return DataCell(
                                  Center(
                                    child: GestureDetector(
                                      onTap: () => _togglePermission(role, perm, enabled),
                                      child: AnimatedContainer(
                                        duration: const Duration(milliseconds: 200),
                                        width: 40,
                                        height: 36,
                                        decoration: BoxDecoration(
                                          color: enabled
                                              ? Colors.green.withValues(alpha: 0.15)
                                              : Colors.white.withValues(alpha: 0.04),
                                          borderRadius: BorderRadius.circular(10),
                                          border: Border.all(
                                            color: enabled
                                                ? Colors.green.withValues(alpha: 0.3)
                                                : Colors.white.withValues(alpha: 0.06),
                                          ),
                                        ),
                                        child: Icon(
                                          enabled ? Icons.check : Icons.close,
                                          color: enabled ? Colors.green : Colors.white.withValues(alpha: 0.3),
                                          size: 18,
                                        ),
                                      ),
                                    ),
                                  ),
                                );
                              }),
                            ]);
                          }).toList(),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
    );
  }
}
