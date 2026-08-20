import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../../app.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class DashboardScreen extends StatefulWidget {
  final ApiService? apiService;
  const DashboardScreen({super.key, this.apiService});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> with SingleTickerProviderStateMixin {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  Map<String, dynamic>? _kpi;
  List<dynamic> _alerts = [];
  bool _isLoading = true;
  int _currentNavIndex = 0;

  late final AnimationController _animCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(vsync: this, duration: const Duration(milliseconds: 500));
    _fadeAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut);
    _loadData();
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      Map<String, dynamic> data;
      try {
        final response = await _apiService.get('/dashboard/kpi');
        data = response.data as Map<String, dynamic>;
      } catch (_) {
        final response = await _apiService.get('/dashboard/my-metrics');
        data = response.data as Map<String, dynamic>;
      }

      // Load alerts (best-effort)
      List<dynamic> alerts = [];
      try {
        final alertRes = await _apiService.get('/alerts', params: {'size': 5});
        alerts = (alertRes.data as Map?)?['content'] as List<dynamic>? ?? [];
      } catch (_) {}

      if (mounted) {
        setState(() {
          _kpi = data;
          _alerts = alerts;
          _isLoading = false;
        });
        _animCtrl.forward();
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final activeRole = AuthState().activeRole;
    final isAdminPasteur = activeRole == 'ADMIN' || activeRole == 'PASTEUR';
    final hasFullKpi = _kpi?.containsKey('totalAmes') ?? false;

    final stats = _kpi != null ? (hasFullKpi ? [
      {'label': 'Âmes suivies', 'value': '${_kpi!['totalAmes'] ?? 0}', 'icon': Icons.favorite, 'gradient': [Colors.red, Colors.pink], 'route': '/souls'},
      {'label': 'Taux de présence', 'value': '${(_kpi!['tauxPresenceGlobal'] ?? 0.0).toStringAsFixed(1)}%', 'icon': Icons.trending_up, 'gradient': [Colors.green, Colors.teal], 'route': '/departments'},
      {'label': 'Faiseurs', 'value': '${_kpi!['totalFaiseurs'] ?? 0}', 'icon': Icons.group, 'gradient': [Colors.blue, Colors.indigo], 'route': '/users'},
      {'label': 'Familles', 'value': '${_kpi!['totalFamilles'] ?? 0}', 'icon': Icons.home, 'gradient': [Colors.purple, Colors.deepPurple], 'route': '/families'},
      {'label': 'Alertes', 'value': '${_kpi!['alertesActives'] ?? 0}', 'icon': Icons.notifications_active, 'gradient': [Colors.orange, Colors.deepOrange], 'route': '/alerts'},
      {'label': 'Rapports', 'value': '${_kpi!['rapportsSoumis'] ?? 0}/${_kpi!['rapportsEnAttente'] ?? 0}', 'icon': Icons.description, 'gradient': [Colors.teal, Colors.cyan], 'route': '/reports'},
    ] : [
      {'label': 'Mes âmes', 'value': '${_kpi!['totalAmes'] ?? _kpi!['totalAmesFamille'] ?? 0}', 'icon': Icons.favorite, 'gradient': [Colors.red, Colors.pink], 'route': '/souls'},
      {'label': 'Rapport soumis', 'value': _kpi!['rapportSoumisCetteSemaine'] == true ? 'Oui' : 'Non', 'icon': Icons.description, 'gradient': [Colors.teal, Colors.cyan], 'route': '/reports/maker'},
      {'label': 'Faiseurs', 'value': '${_kpi!['totalFaiseursFamille'] ?? 0}', 'icon': Icons.group, 'gradient': [Colors.blue, Colors.indigo], 'route': '/users'},
    ]) : [];

    final activeAlerts = _alerts.where((a) => (a as Map<String, dynamic>)['statut'] == 'ACTIVE').toList();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Tableau de bord'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
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
                      // ==================== KPI CARDS ====================
                      SectionTitle(title: 'Vue d\'ensemble', icon: Icons.dashboard),
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
                            onTap: () => context.go(stat['route'] as String),
                          );
                        },
                      ),
                      const SizedBox(height: 16),

                      // ==================== ALERTES ====================
                      SectionTitle(title: 'Alertes', icon: Icons.warning_amber),
                      if (activeAlerts.isNotEmpty)
                        ...activeAlerts.take(3).map((a) {
                          final alert = a as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/alerts'),
                            borderColor: Colors.red.withValues(alpha: 0.3),
                            child: Row(
                              children: [
                                Icon(Icons.warning_amber_rounded, color: Colors.red, size: 18),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(alert['titre'] ?? alert['message'] ?? '',
                                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                                          maxLines: 1, overflow: TextOverflow.ellipsis),
                                      Text(alert['message'] ?? '',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                                          maxLines: 1, overflow: TextOverflow.ellipsis),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          );
                        })
                      else
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

                      // ==================== ACTIONS RAPIDES (ADMIN/PASTEUR) ====================
                      if (isAdminPasteur) ...[
                        const SizedBox(height: 16),
                        SectionTitle(title: 'Actions rapides', icon: Icons.flash_on),
                        GridView.count(
                          shrinkWrap: true,
                          physics: const NeverScrollableScrollPhysics(),
                          crossAxisCount: 4,
                          crossAxisSpacing: 8,
                          mainAxisSpacing: 8,
                          childAspectRatio: 0.9,
                          children: [
                            _quickAction(Icons.dashboard_customize, 'Pasteur', () => context.go('/dashboard/pasteur')),
                            _quickAction(Icons.favorite, 'Âmes', () => context.go('/souls')),
                            _quickAction(Icons.business, 'Départements', () => context.go('/departments')),
                            _quickAction(Icons.description, 'Rapports', () => context.go('/reports')),
                            _quickAction(Icons.event, 'Événements', () => context.go('/events')),
                            _quickAction(Icons.warning_amber, 'Alertes', () => context.go('/alerts')),
                            _quickAction(Icons.person_search, 'Recherche', () => context.go('/search')),
                            _quickAction(Icons.person, 'Profil', () => context.go('/profile')),
                          ],
                        ),
                      ],

                      const SizedBox(height: 80),
                    ],
                  ),
                ),
              ),
            ),
      bottomNavigationBar: GlassBottomNav(currentIndex: _currentNavIndex, onTap: (i) {
        setState(() => _currentNavIndex = i);
        final role = AuthState().activeRole;
        final List<String> routes;
        switch (role) {
          case 'RESPONSABLE':
            routes = ['/dashboard/responsable', '/departments', '/members/requests', '/profile'];
          case 'FAISEUR':
            routes = ['/crm-faiseur', '/souls', '/reports/maker', '/profile'];
          case 'CHEF_DE_FAMILLE':
            routes = ['/dashboard/chef-famille', '/families', '/reports/family', '/profile'];
          case 'MEMBRE':
            routes = ['/dashboard/membre', '/trainings', '/badges', '/appointments'];
          default:
            routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
        }
        if (i < routes.length) context.go(routes[i]);
      }),
    );
  }

  Widget _quickAction(IconData icon, String label, VoidCallback onTap) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.white.withValues(alpha: 0.08)),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: AppColors.primaryLight, size: 22),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 10)),
          ],
        ),
      ),
    );
  }
}
