import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

/// Matrice rôles × permissions, branchée sur le modèle CANONIQUE (Phase 9).
///
/// L'écran lisait `/permissions` (table `role_permissions` au format legacy
/// `role`/`permission` en texte) : V135 a remplacé cette table par la jointure
/// `(role_id, permission_id)`, la requête échoue donc en 42703
/// (`column "role" does not exist`) et l'écran affichait « Aucune permission
/// configurée » pour une église pourtant réglée. Les libellés de rôles et de
/// permissions étaient de plus écrits en dur, donc non paramétrables par
/// l'église — le contrat canonique fournit les deux.
///
/// Contrat utilisé :
/// - `GET /admin/roles` → projection éditeur `{id, key, label, permissions[],
///   local, effectiveForTenant}` (RoleManagementService#listRolesForEditor) ;
/// - `GET /admin/roles/permissions` → catalogue `{key, label, category}` ;
/// - `PUT /admin/roles/{id}/permissions` → corps `{permissionKeys: [...]}`
///   (matrice complète ; le serveur rejette 400 toute clé inconnue et invalide
///   le cache de permissions servies).
class PermissionsScreen extends StatefulWidget {
  const PermissionsScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<PermissionsScreen> createState() => _PermissionsScreenState();
}

class _RoleRow {
  const _RoleRow({required this.id, required this.key, required this.label, required this.permissions});
  final String id;
  final String key;
  final String label;
  final Set<String> permissions;
}

class _PermissionsScreenState extends State<PermissionsScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<_RoleRow> _roles = const [];
  Map<String, String> _permLabels = const {};
  bool _isLoading = true;
  bool _busy = false;

  @override
  void initState() { super.initState(); _loadData(); }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _apiService.get('/admin/roles'),
        _apiService.get('/admin/roles/permissions'),
      ]);
      final roles = (results[0].data as List).cast<Map<String, dynamic>>();
      final catalog = results[1].data as List;
      if (!mounted) return;
      setState(() {
        // Seules les lignes RÉELLEMENT exercées ici : quand l'église a une
        // surcouche locale, le rôle global homonyme ne décide plus rien et
        // paraitrait en double colonne dans la matrice.
        _roles = roles
            .where((r) => r['effectiveForTenant'] != false)
            .map((r) => _RoleRow(
                  id: r['id'] as String,
                  key: (r['key'] as String? ?? '').toUpperCase(),
                  label: r['label'] as String? ?? (r['key'] as String? ?? ''),
                  permissions: ((r['permissions'] as List?) ?? const [])
                      .map((e) => e.toString())
                      .toSet(),
                ))
            .toList();
        _permLabels = {
          for (final e in catalog.cast<Map<String, dynamic>>())
            (e['key'] as String).toString(): (e['label'] as String? ?? e['key'].toString()),
        };
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  /// Bascule une cellule : le contrat canonique remplace la matrice entière du
  /// rôle, on renvoie donc la liste complète moins/plus la clé concernée.
  Future<void> _togglePermission(_RoleRow role, String permission) async {
    if (_busy) return;
    final next = Set<String>.from(role.permissions);
    if (!next.remove(permission)) next.add(permission);
    setState(() => _busy = true);
    try {
      await _apiService.put('/admin/roles/${role.id}/permissions',
          data: {'permissionKeys': next.toList()});
      await _loadData();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la mise à jour')),
        );
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    // Une ligne par clé présente chez au moins un rôle exercé, plus les clés du
    // catalogue : la matrice doit rester lisible sans être un mur de 500 lignes.
    final permNames = <String>{
      for (final role in _roles) ...role.permissions,
    }.toList()..sort();

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

                    if (_roles.isEmpty || permNames.isEmpty)
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
                            // Libellé porté par la base : chaque église peut le
                            // renommer, rien n'est écrit en dur ici.
                            ..._roles.map((role) => DataColumn(
                              label: Text(role.label, textAlign: TextAlign.center),
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
                              ..._roles.map((role) {
                                final enabled = role.permissions.contains(perm);
                                return DataCell(
                                  Center(
                                    child: GestureDetector(
                                      onTap: () => _togglePermission(role, perm),
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
