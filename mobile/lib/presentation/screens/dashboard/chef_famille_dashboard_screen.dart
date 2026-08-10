import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../app.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class ChefFamilleDashboardScreen extends StatefulWidget {
  const ChefFamilleDashboardScreen({super.key});

  @override
  State<ChefFamilleDashboardScreen> createState() => _ChefFamilleDashboardScreenState();
}

class _ChefFamilleDashboardScreenState extends State<ChefFamilleDashboardScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  Map<String, dynamic>? _dashboard;
  List<dynamic> _workload = [];
  bool _isLoading = true;

  late final AnimationController _animCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(vsync: this, duration: const Duration(milliseconds: 600));
    _fadeAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut);
    _loadData();
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    try {
      final familleId = AuthState().familleGereeId;
      final response = await _apiService.get('/dashboard/chef-famille',
          params: familleId != null ? {'familleId': familleId} : null);
      // Charge de travail des faiseurs de la famille (scopée côté serveur).
      List<dynamic> workload = [];
      try {
        final workloadRes = await _apiService.get('/users/faiseur-workload',
            params: familleId != null ? {'familleId': familleId} : null);
        workload = (workloadRes.data as List?) ?? [];
      } catch (_) {/* best-effort : la charge n'empêche pas l'affichage du dashboard */}
      if (mounted) {
        setState(() { _dashboard = response.data as Map<String, dynamic>?; _workload = workload; _isLoading = false; });
        _animCtrl.forward();
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final famille = _dashboard?['famille'] as Map<String, dynamic>? ?? {};
    final faiseurs = _dashboard?['faiseurs'] as List<dynamic>? ?? [];
    final disciples = _dashboard?['disciples'] as List<dynamic>? ?? [];
    final stats = _dashboard?['statistiques'] as Map<String, dynamic>? ?? {};

    return Scaffold(
      appBar: AppBar(
        title: Text(famille['nom'] ?? 'Ma famille'),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: FadeTransition(
                opacity: _fadeAnim,
                child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Stats grid
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2, childAspectRatio: 1.5, crossAxisSpacing: 10, mainAxisSpacing: 10,
                        ),
                        itemCount: 4,
                        itemBuilder: (_, i) {
                          final items = [
                            {'label': 'Disciples', 'value': '${stats['totalDisciples'] ?? 0}', 'icon': Icons.favorite, 'color': const Color(0xFFD4AF37)},
                            {'label': 'Faiseurs', 'value': '${stats['totalFaiseurs'] ?? 0}', 'icon': Icons.group, 'color': Colors.teal},
                            {'label': 'Actifs', 'value': '${stats['actifs'] ?? 0}', 'icon': Icons.check_circle, 'color': Colors.green},
                            {'label': 'Rapports', 'value': '${stats['rapportsSoumisSemaine'] ?? 0}/${stats['totalDisciples'] ?? 0}', 'icon': Icons.description, 'color': Colors.blue},
                          ];
                          final item = items[i];
                          return GlassStatCard(
                            label: item['label'] as String,
                            value: item['value'] as String,
                            icon: item['icon'] as IconData,
                            gradientStart: item['color'] as Color,
                            gradientEnd: (item['color'] as Color).withValues(alpha: 0.7),
                          );
                        },
                      ),
                      const SizedBox(height: 16),

                      // Charge de travail des faiseurs (US-14)
                      if (_workload.isNotEmpty) ...[
                        SectionTitle(title: 'Charge de travail des Faiseurs', icon: Icons.bar_chart),
                        const SizedBox(height: 8),
                        ..._workload.take(6).map((w) {
                          final charge = (w['charge'] as String?) ?? '';
                          final chargeColor = charge == 'SURCHARGÉ'
                              ? Colors.redAccent
                              : charge == 'LEGER' ? Colors.greenAccent : Colors.blueAccent;
                          return Container(
                            margin: const EdgeInsets.only(bottom: 6),
                            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.04),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Row(children: [
                              Expanded(child: Text('${w['faiseurName'] ?? '—'}',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12))),
                              if (charge.isNotEmpty)
                                Container(
                                  margin: const EdgeInsets.only(right: 6),
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: chargeColor.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Text(
                                    charge == 'SURCHARGÉ' ? 'Surchargé' : charge == 'LEGER' ? 'Léger' : 'Normal',
                                    style: TextStyle(color: chargeColor, fontSize: 9, fontWeight: FontWeight.w600),
                                  ),
                                ),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                decoration: BoxDecoration(
                                  color: AppColors.primary.withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Text('${w['soulCount'] ?? 0} âmes',
                                    style: TextStyle(color: AppColors.primary, fontSize: 10, fontWeight: FontWeight.w600)),
                              ),
                            ]),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // Faiseurs network
                      if (faiseurs.isNotEmpty) ...[
                        SectionTitle(title: 'Faiseurs (${faiseurs.length})', icon: Icons.account_tree),
                        const SizedBox(height: 8),
                        ...faiseurs.map((f) {
                          final faiseur = f as Map<String, dynamic>;
                          final faiseurDisciples = disciples.where((d) => (d as Map<String, dynamic>)['faiseurId'] == faiseur['id']).toList();
                          return Container(
                            margin: const EdgeInsets.only(bottom: 10),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.04),
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                            ),
                            child: ExpansionTile(
                              leading: CircleAvatar(
                                backgroundColor: Colors.teal.withValues(alpha: 0.2),
                                child: Text(
                                  (faiseur['nom'] as String? ?? '?').substring(0, 1).toUpperCase(),
                                  style: const TextStyle(color: Colors.teal, fontWeight: FontWeight.bold),
                                ),
                              ),
                              title: Text(faiseur['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                              subtitle: Text('${faiseur['totalAmes'] ?? 0} disciples · ${faiseur['actifs'] ?? 0} actifs',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                              children: faiseurDisciples.map((d) {
                                final disciple = d as Map<String, dynamic>;
                                return ListTile(
                                  leading: Container(
                                    width: 8, height: 8,
                                    decoration: BoxDecoration(
                                      shape: BoxShape.circle,
                                      color: disciple['statut'] == 'ACTIF' ? Colors.green
                                          : disciple['statut'] == 'EN_INTEGRATION' ? Colors.amber
                                          : disciple['statut'] == 'EN_VEILLE' ? Colors.blue : Colors.red,
                                    ),
                                  ),
                                  title: Text(disciple['nom'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 13)),
                                  subtitle: Text('Niv. ${disciple['niveauCroissance'] ?? 1} · ${disciple['type'] == 'NOUVEAU_CONVERTI' ? 'Converti' : 'Arrivant'}',
                                      style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                                  trailing: Text(
                                    disciple['rapportSemaine'] == true ? '✓' : '○',
                                    style: TextStyle(
                                      color: disciple['rapportSemaine'] == true ? Colors.green : Colors.amber,
                                      fontWeight: FontWeight.bold, fontSize: 16,
                                    ),
                                  ),
                                );
                              }).toList(),
                            ),
                          );
                        }),
                      ],
                      const SizedBox(height: 80),
                    ],
                  ),
                ),
              ),
            ),
    );
  }
}
