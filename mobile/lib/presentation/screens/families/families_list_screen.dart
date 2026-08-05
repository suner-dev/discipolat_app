import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class FamiliesListScreen extends StatefulWidget {
  const FamiliesListScreen({super.key});

  @override
  State<FamiliesListScreen> createState() => _FamiliesListScreenState();
}

class _FamiliesListScreenState extends State<FamiliesListScreen> {
  final _apiService = ApiService();
  List<dynamic> _families = [];
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
      final res = await _apiService.get('/families', params: {'size': '50'});
      if (mounted) {
        setState(() {
          _families = (res.data is Map ? res.data['content'] : res.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  List<dynamic> get _filtered {
    if (_searchQuery.isEmpty) return _families;
    return _families.where((f) {
      final nom = (f as Map)['nom']?.toString().toLowerCase() ?? '';
      return nom.contains(_searchQuery.toLowerCase());
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Familles'),
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
                  // Stats
                  Row(
                    children: [
                      _statMini('Total', '${_families.length}', Colors.purple),
                      const SizedBox(width: 8),
                      _statMini('Actives', '${_families.where((f) => (f as Map)['actif'] != false).length}', Colors.green),
                    ],
                  ),
                  const SizedBox(height: 12),
                  // Search
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
                        hintText: 'Rechercher une famille...',
                        hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                        prefixIcon: Icon(Icons.search, color: Colors.white.withValues(alpha: 0.4), size: 20),
                        border: InputBorder.none,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Family cards
                  if (_filtered.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.family_restroom_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucune famille', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._filtered.map((f) {
                      final family = f as Map;
                      final nom = family['nom'] ?? 'Famille';
                      final chefId = family['chefFamilleId']?.toString().substring(0, 8) ?? '—';
                      final departement = family['departementNom'] ?? '—';
                      final statut = family['statut'] ?? 'ACTIVE';
                      final nbDisciples = family['nombreDisciples'] ?? '—';
                      final dateCreation = family['createdAt']?.toString().substring(0, 10) ?? '';

                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 10),
                        padding: const EdgeInsets.all(14),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                GradientAvatar(
                                  text: nom.toString().substring(0, nom.toString().length.clamp(0, 2)),
                                  radius: 24,
                                  gradientStart: Colors.purple,
                                  gradientEnd: Colors.indigo,
                                ),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 15)),
                                      Text('Chef: $chefId...', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                    ],
                                  ),
                                ),
                                StatusBadge(
                                  label: statut,
                                  color: statut == 'ACTIVE' ? Colors.green : Colors.grey,
                                ),
                              ],
                            ),
                            const SizedBox(height: 10),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceAround,
                              children: [
                                _miniStat(Icons.people, 'Disciples', '$nbDisciples'),
                                _miniStat(Icons.business, 'Département', departement),
                                _miniStat(Icons.calendar_today, 'Créée', dateStr(dateCreation)),
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

  String dateStr(String d) => d.length >= 10 ? d.substring(5, 7) + '/' + d.substring(8, 10) : d;

  Widget _miniStat(IconData icon, String label, String value) {
    return Column(
      children: [
        Icon(icon, color: Colors.white.withValues(alpha: 0.4), size: 16),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
      ],
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
