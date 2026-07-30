import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class ResponsableDashboardScreen extends StatefulWidget {
  const ResponsableDashboardScreen({super.key});

  @override
  State<ResponsableDashboardScreen> createState() => _ResponsableDashboardScreenState();
}

class _ResponsableDashboardScreenState extends State<ResponsableDashboardScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  Map<String, dynamic>? _dashboard;
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
      final response = await _apiService.get('/dashboard/responsable');
      if (mounted) {
        setState(() { _dashboard = response.data as Map<String, dynamic>?; _isLoading = false; });
        _animCtrl.forward();
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final stats = _dashboard?['statistiques'] as Map<String, dynamic>? ?? {};
    final departements = _dashboard?['departements'] as List<dynamic>? ?? [];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Espace Responsable'),
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
                      // Global stats
                      SectionTitle(title: 'Vue densemble', icon: Icons.dashboard),
                      const SizedBox(height: 8),
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 3, childAspectRatio: 1.2, crossAxisSpacing: 8, mainAxisSpacing: 8,
                        ),
                        itemCount: 6,
                        itemBuilder: (_, i) {
                          final items = [
                            {'label': 'Âmes', 'value': '${stats['totalAmes'] ?? 0}', 'icon': Icons.favorite, 'color': Colors.amber},
                            {'label': 'Actifs', 'value': '${stats['totalActifs'] ?? 0}', 'icon': Icons.check_circle, 'color': Colors.green},
                            {'label': 'Familles', 'value': '${stats['totalFamilles'] ?? 0}', 'icon': Icons.home, 'color': Colors.blue},
                            {'label': 'Faiseurs', 'value': '${stats['totalFaiseurs'] ?? 0}', 'icon': Icons.group, 'color': Colors.purple},
                            {'label': 'Rapports', 'value': '${stats['rapportsSoumis'] ?? 0}', 'icon': Icons.description, 'color': Colors.teal},
                            {'label': 'Taux', 'value': '${stats['tauxCompletion'] ?? 0}%', 'icon': Icons.pie_chart, 'color': Colors.cyan},
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

                      // Progress bar
                      Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.04),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Text('Progression des rapports',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
                                Text('${stats['rapportsSoumis'] ?? 0} / ${stats['rapportsAttendus'] ?? 0}',
                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13)),
                              ],
                            ),
                            const SizedBox(height: 8),
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: LinearProgressIndicator(
                                value: (stats['rapportsAttendus'] ?? 0) > 0
                                    ? (stats['rapportsSoumis'] ?? 0) / (stats['rapportsAttendus'] ?? 1)
                                    : 0,
                                backgroundColor: Colors.white.withValues(alpha: 0.1),
                                valueColor: AlwaysStoppedAnimation<Color>(
                                  (stats['tauxCompletion'] ?? 0) >= 70 ? Colors.green : Colors.amber,
                                ),
                                minHeight: 8,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // Department cards
                      if (departements.isNotEmpty) ...[
                        SectionTitle(title: 'Mes départements', icon: Icons.business),
                        const SizedBox(height: 8),
                        ...departements.map((d) {
                          final dept = d as Map<String, dynamic>;
                          return Container(
                            margin: const EdgeInsets.only(bottom: 10),
                            padding: const EdgeInsets.all(16),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.04),
                              borderRadius: BorderRadius.circular(16),
                              border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                            ),
                            child: Column(
                              children: [
                                Row(
                                  children: [
                                    Icon(Icons.business, color: AppColors.primaryLight, size: 20),
                                    const SizedBox(width: 8),
                                    Expanded(child: Text(dept['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 15))),
                                  ],
                                ),
                                const SizedBox(height: 12),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                                  children: [
                                    _statChip('Familles', '${dept['totalFamilles'] ?? 0}', Colors.blue),
                                    _statChip('Âmes', '${dept['totalAmes'] ?? 0}', Colors.green),
                                    _statChip('Faiseurs', '${dept['totalFaiseurs'] ?? 0}', Colors.purple),
                                    _statChip('Présence', '${dept['tauxPresence'] ?? 0}%', Colors.amber),
                                  ],
                                ),
                              ],
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

  Widget _statChip(String label, String value, Color color) {
    return Column(
      children: [
        Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 16)),
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 9)),
      ],
    );
  }
}
