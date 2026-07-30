import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  Map<String, dynamic>? _kpi;
  bool _isLoading = true;
  int _currentNavIndex = 0;

  late final AnimationController _animCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(vsync: this, duration: const Duration(milliseconds: 500));
    _fadeAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut);
    _loadKpi();
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadKpi() async {
    try {
      Map<String, dynamic> data;
      try {
        final response = await _apiService.get('/dashboard/kpi');
        data = response.data as Map<String, dynamic>;
      } catch (_) {
        final response = await _apiService.get('/dashboard/my-metrics');
        data = response.data as Map<String, dynamic>;
      }
      if (mounted) {
        setState(() { _kpi = data; _isLoading = false; });
        _animCtrl.forward();
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final hasFullKpi = _kpi?.containsKey('totalAmes') ?? false;
    final stats = _kpi != null ? (hasFullKpi ? [
      {'label': 'Âmes suivies', 'value': '${_kpi!['totalAmes'] ?? 0}', 'icon': Icons.favorite, 'gradient': [Colors.red, Colors.pink], 'trend': null},
      {'label': 'Taux de présence', 'value': '${(_kpi!['tauxPresenceGlobal'] ?? 0.0).toStringAsFixed(1)}%', 'icon': Icons.trending_up, 'gradient': [Colors.green, Colors.teal], 'trend': '+${(_kpi!['tendancePresence'] ?? 0).toStringAsFixed(1)}%'},
      {'label': 'Faiseurs', 'value': '${_kpi!['totalFaiseurs'] ?? 0}', 'icon': Icons.group, 'gradient': [Colors.blue, Colors.indigo], 'trend': null},
      {'label': 'Familles', 'value': '${_kpi!['totalFamilles'] ?? 0}', 'icon': Icons.home, 'gradient': [Colors.purple, Colors.deepPurple], 'trend': null},
      {'label': 'Alertes', 'value': '${_kpi!['alertesActives'] ?? 0}', 'icon': Icons.notifications_active, 'gradient': [Colors.orange, Colors.deepOrange], 'trend': null},
      {'label': 'Rapports', 'value': '${_kpi!['rapportsSoumis'] ?? 0}/${_kpi!['rapportsEnAttente'] ?? 0}', 'icon': Icons.description, 'gradient': [Colors.teal, Colors.cyan], 'trend': null},
    ] : [
      {'label': 'Mes âmes', 'value': '${_kpi!['totalAmes'] ?? _kpi!['totalAmesFamille'] ?? 0}', 'icon': Icons.favorite, 'gradient': [Colors.red, Colors.pink], 'trend': null},
      {'label': 'Rapport soumis', 'value': '${_kpi!['rapportSoumisCetteSemaine'] == true ? 'Oui' : 'Non'}', 'icon': Icons.description, 'gradient': [Colors.teal, Colors.cyan], 'trend': null},
      {'label': 'Faiseurs', 'value': '${_kpi!['totalFaiseursFamille'] ?? 0}', 'icon': Icons.group, 'gradient': [Colors.blue, Colors.indigo], 'trend': null},
    ]) : [];

    return Scaffold(
      appBar: AppBar(
        title: const Text('Tableau de bord'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadKpi),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : RefreshIndicator(
              onRefresh: _loadKpi,
              child: FadeTransition(
                opacity: _fadeAnim,
                child: SingleChildScrollView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      SectionTitle(title: 'Vue densemble', icon: Icons.dashboard),
                      const SizedBox(height: 8),
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2,
                          childAspectRatio: 1.4,
                          crossAxisSpacing: 12,
                          mainAxisSpacing: 12,
                        ),
                        itemCount: stats.length,
                        itemBuilder: (context, index) {
                          final stat = stats[index];
                          final gradient = stat['gradient'] as List<Color>;
                          return GlassStatCard(
                            label: stat['label'] as String,
                            value: stat['value'] as String,
                            icon: stat['icon'] as IconData,
                            gradientStart: gradient[0],
                            gradientEnd: gradient[1],
                            trend: stat['trend'] as String?,
                            trendUp: true,
                          );
                        },
                      ),
                      const SizedBox(height: 16),
                      SectionTitle(title: 'Alertes récentes', icon: Icons.warning_amber),
                      GlassCard(
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          children: [
                            Icon(Icons.check_circle_outline, color: Colors.green.withValues(alpha: 0.7), size: 48),
                            const SizedBox(height: 12),
                            const Text('Tout est sous contrôle', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w500)),
                            const SizedBox(height: 4),
                            Text('Aucune alerte active', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13)),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
      bottomNavigationBar: GlassBottomNav(currentIndex: _currentNavIndex, onTap: (i) {
        setState(() => _currentNavIndex = i);
        final routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
        if (i < routes.length) context.go(routes[i]);
      }),
    );
  }

}
