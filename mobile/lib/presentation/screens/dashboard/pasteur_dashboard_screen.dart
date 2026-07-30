import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class PasteurDashboardScreen extends StatefulWidget {
  const PasteurDashboardScreen({super.key});

  @override
  State<PasteurDashboardScreen> createState() => _PasteurDashboardScreenState();
}

class _PasteurDashboardScreenState extends State<PasteurDashboardScreen> with SingleTickerProviderStateMixin {
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
      final response = await _apiService.get('/dashboard/pasteur');
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
    final croissance = _dashboard?['croissance'] as Map<String, dynamic>? ?? {};
    final departements = _dashboard?['departements'] as List<dynamic>? ?? [];
    final familles = _dashboard?['familles'] as List<dynamic>? ?? [];
    final faiseurs = _dashboard?['faiseurs'] as List<dynamic>? ?? [];
    final presences = _dashboard?['presences'] as Map<String, dynamic>? ?? {};
    final rapports = _dashboard?['rapports'] as Map<String, dynamic>? ?? {};
    final alertesActives = _dashboard?['alertesActives'] ?? 0;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Pilotage Pasteur'),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 8)
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
                      // Growth header
                      SectionTitle(title: 'Croissance', icon: Icons.trending_up),
                      const SizedBox(height: 8),
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2, childAspectRatio: 1.5, crossAxisSpacing: 10, mainAxisSpacing: 10,
                        ),
                        itemCount: 8,
                        itemBuilder: (_, i) {
                          final items = [
                            {'label': 'Âmes', 'value': '${croissance['totalAmes'] ?? 0}', 'icon': Icons.favorite, 'color': Colors.red},
                            {'label': 'Convertis', 'value': '${croissance['nouveauxConvertis'] ?? 0}', 'icon': Icons.people, 'color': Colors.green},
                            {'label': 'Arrivants', 'value': '${croissance['nouveauxArrivants'] ?? 0}', 'icon': Icons.person_add, 'color': Colors.blue},
                            {'label': 'Actifs', 'value': '${croissance['actifs'] ?? 0}', 'icon': Icons.check_circle, 'color': Colors.teal},
                            {'label': 'Intégration', 'value': '${croissance['enIntegration'] ?? 0}', 'icon': Icons.hourglass_top, 'color': Colors.amber},
                            {'label': 'Veille', 'value': '${croissance['enVeille'] ?? 0}', 'icon': Icons.bedtime, 'color': Colors.orange},
                            {'label': 'Décrochés', 'value': '${croissance['decroches'] ?? 0}', 'icon': Icons.cancel, 'color': Colors.red},
                            {'label': 'Conversion', 'value': '${croissance['tauxConversion'] ?? 0}%', 'icon': Icons.trending_up, 'color': Colors.purple},
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

                      // Presence + Reports
                      SectionTitle(title: 'Présences & Rapports', icon: Icons.bar_chart),
                      const SizedBox(height: 8),
                      Row(
                        children: [
                          Expanded(
                            child: GlassStatCard(
                              label: 'Présence globale',
                              value: '${presences['tauxGlobal'] ?? 0}%',
                              icon: Icons.trending_up,
                              gradientStart: Colors.green,
                              gradientEnd: Colors.teal,
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: GlassStatCard(
                              label: 'Rapports soumis',
                              value: '${rapports['soumis'] ?? 0}',
                              icon: Icons.description,
                              gradientStart: Colors.blue,
                              gradientEnd: Colors.indigo,
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: GlassStatCard(
                              label: 'Complétion',
                              value: '${rapports['tauxCompletion'] ?? 0}%',
                              icon: Icons.pie_chart,
                              gradientStart: Colors.teal,
                              gradientEnd: Colors.cyan,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),

                      // Alert
                      Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [
                              alertesActives > 0 ? Colors.red.withValues(alpha: 0.2) : Colors.green.withValues(alpha: 0.2),
                              Colors.transparent,
                            ],
                          ),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: (alertesActives > 0 ? Colors.red : Colors.green).withValues(alpha: 0.3)),
                        ),
                        child: Row(
                          children: [
                            Icon(
                              alertesActives > 0 ? Icons.warning : Icons.check_circle,
                              color: alertesActives > 0 ? Colors.red : Colors.green,
                              size: 32,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    '$alertesActives alerte${alertesActives > 1 ? 's' : ''} active${alertesActives > 1 ? 's' : ''}',
                                    style: TextStyle(
                                      color: alertesActives > 0 ? Colors.red : Colors.green,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 15,
                                    ),
                                  ),
                                  Text(
                                    alertesActives > 0 ? 'Attention requise' : 'Tout est sous contrôle',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // Departments list
                      if (departements.isNotEmpty) ...[
                        SectionTitle(title: 'Départements', icon: Icons.business),
                        const SizedBox(height: 8),
                        SizedBox(
                          height: 120,
                          child: ListView.builder(
                            scrollDirection: Axis.horizontal,
                            itemCount: departements.length,
                            itemBuilder: (_, i) {
                              final dept = departements[i] as Map<String, dynamic>;
                              return Container(
                                width: 180,
                                margin: const EdgeInsets.only(right: 12),
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  gradient: LinearGradient(
                                    colors: [AppColors.primary.withValues(alpha: 0.15), Colors.transparent],
                                    begin: Alignment.topLeft, end: Alignment.bottomRight,
                                  ),
                                  borderRadius: BorderRadius.circular(16),
                                  border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(dept['nom'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                                    const Spacer(),
                                    Row(
                                      children: [
                                        Text('${dept['totalFamilles'] ?? 0} fam. · ', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                                        Text('${dept['totalAmes'] ?? 0} âmes', style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 11)),
                                      ],
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                      ],
                      const SizedBox(height: 16),

                      // Families at risk
                      final famillesRisque = _dashboard?['famillesARisque'] as List<dynamic>? ?? [];
                      if (famillesRisque.isNotEmpty) ...[
                        SectionTitle(title: 'Familles à risque', icon: Icons.warning),
                        const SizedBox(height: 8),
                        ...famillesRisque.take(5).map((fr) {
                          final f = fr as Map<String, dynamic>;
                          return Container(
                            margin: const EdgeInsets.only(bottom: 6),
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.red.withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(12),
                              border: Border.all(color: Colors.red.withValues(alpha: 0.2)),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.warning_amber, color: Colors.red, size: 20),
                                const SizedBox(width: 10),
                                Expanded(child: Text(f['nom'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 13))),
                                Text('${f['tauxPresence']}%', style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
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
}
