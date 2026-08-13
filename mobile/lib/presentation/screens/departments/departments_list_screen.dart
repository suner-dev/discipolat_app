import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class DepartmentsListScreen extends StatefulWidget {
  const DepartmentsListScreen({super.key});

  @override
  State<DepartmentsListScreen> createState() => _DepartmentsListScreenState();
}

class _DepartmentsListScreenState extends State<DepartmentsListScreen> {
  final _apiService = ApiService();
  List<dynamic> _departments = [];
  bool _isLoading = true;
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/departments', params: {'size': '50'});
      if (mounted) {
        setState(() {
          _departments = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  List<dynamic> get _filtered {
    if (_searchQuery.isEmpty) return _departments;
    return _departments.where((d) {
      final nom = (d as Map)['nom']?.toString().toLowerCase() ?? '';
      return nom.contains(_searchQuery.toLowerCase());
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Départements'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  // Stats row
                  Row(
                    children: [
                      _statMini('Total', '${_departments.length}', Colors.blue),
                      const SizedBox(width: 8),
                      _statMini('Actifs', '${_departments.where((d) => (d as Map)['actif'] != false).length}', Colors.green),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Search bar
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12),
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.06),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: TextField(
                      onChanged: (v) => setState(() => _searchQuery = v),
                      style: const TextStyle(color: Colors.white),
                      decoration: InputDecoration(
                        hintText: 'Rechercher un département...',
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                        prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 20),
                        border: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Department cards
                  if (_filtered.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.business_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucun département', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._filtered.map((d) {
                      final dept = d as Map;
                      final nom = dept['nom'] ?? 'Département';
                      final desc = dept['description'] ?? '';
                      final statut = dept['statut'] ?? 'ACTIF';
                      final responsable = dept['responsableNom'] ?? '—';
                      final nbMembres = dept['nombreMembres'] ?? '—';
                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(14),
                        onTap: () => context.go('/departments/${dept['id']}/manage'),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(10),
                                  decoration: BoxDecoration(
                                    color: Colors.amber.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: const Icon(Icons.business, color: Colors.amber, size: 22),
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
                                      if (responsable != '—')
                                        Text('Resp: $responsable', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                    ],
                                  ),
                                ),
                                StatusBadge(
                                  label: statut,
                                  color: statut == 'ACTIF' ? Colors.green : Colors.grey,
                                ),
                              ],
                            ),
                            if (desc.toString().isNotEmpty) ...[
                              const SizedBox(height: 8),
                              Text(desc, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12), maxLines: 2, overflow: TextOverflow.ellipsis),
                            ],
                            const SizedBox(height: 8),
                            Row(
                              children: [
                                Icon(Icons.people, size: 14, color: Colors.white.withValues(alpha: 0.4)),
                                const SizedBox(width: 4),
                                Text('$nbMembres membres', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                              ],
                            ),
                          ],
                        ),
                      );
                    }),
                ],
              ),
            ),
    );
  }

  Widget _statMini(String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(10),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontSize: 18, fontWeight: FontWeight.bold)),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
