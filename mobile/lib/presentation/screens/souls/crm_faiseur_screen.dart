import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../app.dart';

class CrmFaiseurScreen extends StatefulWidget {
  const CrmFaiseurScreen({super.key});

  @override
  State<CrmFaiseurScreen> createState() => _CrmFaiseurScreenState();
}

class _CrmFaiseurScreenState extends State<CrmFaiseurScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  Map<String, dynamic>? _crm;
  List<Map<String, dynamic>> _disciplesByFaiseur = [];
  List<Map<String, dynamic>> _soulsEnDifficulte = [];
  bool _isLoading = true;
  String _filterStatus = 'all';

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
    try {
      final userId = AuthState().userId ?? '';
      final results = await Future.wait([
        _apiService.get('/dashboard/crm-faiseur'),
        _apiService.get('/souls/by-faiseur/$userId'),
        _apiService.get('/souls/en-difficulte'),
      ]);
      if (mounted) {
        final crmData = results[0].data;
        _crm = crmData is Map<String, dynamic> ? crmData : null;
        final byFaiseurData = results[1].data;
        _disciplesByFaiseur = (byFaiseurData is Map && byFaiseurData['content'] is List
            ? byFaiseurData['content'] as List
            : (byFaiseurData is List ? byFaiseurData : []))
            .cast<Map<String, dynamic>>();
        final enDiffData = results[2].data;
        _soulsEnDifficulte = (enDiffData is Map && enDiffData['content'] is List
            ? enDiffData['content'] as List
            : (enDiffData is List ? enDiffData : []))
            .cast<Map<String, dynamic>>();
        setState(() => _isLoading = false);
        _animCtrl.forward();
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  List<Map<String, dynamic>> get _disciples {
    final raw = (_crm?['disciples'] as List<dynamic>?) ?? [];
    final disciples = raw.cast<Map<String, dynamic>>();
    final list = _disciplesByFaiseur.isNotEmpty ? _disciplesByFaiseur : disciples;
    if (_filterStatus == 'all') return list;
    return list.where((d) => d['statut'] == _filterStatus).toList();
  }

  Map<String, dynamic> get _stats => (_crm?['statistiques'] as Map<String, dynamic>?) ?? {};

  List<Map<String, dynamic>> get _alertes {
    final raw = (_crm?['alertes'] as List<dynamic>?) ?? [];
    return raw.cast<Map<String, dynamic>>();
  }

  String get _greeting {
    final h = DateTime.now().hour;
    if (h < 12) return 'Bonjour';
    if (h < 17) return 'Bon après-midi';
    return 'Bonsoir';
  }

  List<PieChartSectionData> get _pieData {
    final data = <PieChartSectionData>[];
    final items = [
      {'name': 'Actifs', 'value': (_stats['actifs'] ?? 0) as num, 'color': const Color(0xFF22C55E)},
      {'name': 'Intégration', 'value': (_stats['enIntegration'] ?? 0) as num, 'color': const Color(0xFFF59E0B)},
      {'name': 'Veille', 'value': (_stats['enVeille'] ?? 0) as num, 'color': const Color(0xFF3B82F6)},
      {'name': 'Décrochés', 'value': (_stats['decroches'] ?? 0) as num, 'color': const Color(0xFFEF4444)},
    ];
    for (final item in items) {
      final val = item['value'] as num? ?? 0;
      if (val > 0) {
        data.add(PieChartSectionData(
          value: val.toDouble(),
          color: item['color'] as Color,
          radius: 40,
          title: '$val',
          titleStyle: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.white),
        ));
      }
    }
    return data;
  }

  @override
  Widget build(BuildContext context) {
    final firstName = AuthState().firstName ?? '';

    return Scaffold(
      appBar: AppBar(
        title: const Text('CRM Faiseur'),
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
                      // Header
                      Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [AppColors.primary.withValues(alpha: 0.15), Colors.transparent],
                            begin: Alignment.topLeft, end: Alignment.bottomRight,
                          ),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(color: Colors.white.withValues(alpha: 0.06)),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Icon(Icons.people, color: AppColors.primary, size: 18),
                                const SizedBox(width: 8),
                                Text(
                                  '$_greeting, $firstName',
                                  style: TextStyle(color: AppColors.primaryLight, fontSize: 11, fontWeight: FontWeight.w600),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                const Text('CRM ',
                                    style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                                Text('Faiseur',
                                    style: TextStyle(
                                      color: AppColors.primaryLight,
                                      fontSize: 24,
                                      fontWeight: FontWeight.bold,
                                      shadows: [Shadow(color: AppColors.primary.withValues(alpha: 0.5), blurRadius: 8)],
                                    )),
                              ],
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Suivi complet de vos disciples · ${DateFormat('EEEE d MMMM yyyy', 'fr_FR').format(DateTime.now())}',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // Stats row
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2, childAspectRatio: 2.2, crossAxisSpacing: 10, mainAxisSpacing: 10,
                        ),
                        itemCount: 4,
                        itemBuilder: (_, i) {
                          final items = [
                            {'label': 'Disciples', 'value': '${_stats['totalDisciples'] ?? 0}', 'icon': Icons.favorite, 'color': AppColors.primary},
                            {'label': 'Actifs', 'value': '${_stats['actifs'] ?? 0}', 'icon': Icons.check_circle, 'color': const Color(0xFF22C55E)},
                            {'label': 'Rapports soumis', 'value': '${_stats['rapportsSoumisSemaine'] ?? 0} / ${_stats['totalDisciples'] ?? 0}', 'icon': Icons.description, 'color': Colors.blue},
                            {'label': 'En difficulté', 'value': '${_stats['enDifficulte'] ?? 0}', 'icon': Icons.warning, 'color': Colors.red},
                          ];
                          final item = items[i];
                          return GlassStatCard(
                            label: item['label'] as String,
                            value: item['value'] as String,
                            icon: item['icon'] as IconData,
                            gradientStart: item['color'] as Color,
                            gradientEnd: (item['color'] as Color).withValues(alpha: 0.7),
                            onTap: () {
                              final label = item['label'] as String;
                              if (label == 'Disciples') {
                                setState(() => _filterStatus = 'all');
                              } else if (label == 'Actifs') {
                                setState(() => _filterStatus = 'ACTIF');
                              } else if (label == 'Rapports soumis') {
                                context.go('/reports/maker');
                              } else {
                                context.go('/alerts');
                              }
                            },
                          );
                        },
                      ),
                      const SizedBox(height: 16),

                      // Pie chart + Alerts + Quick actions
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Pie chart
                          Expanded(
                            child: GlassCard(
                              padding: const EdgeInsets.all(12),
                              child: Column(
                                children: [
                                  const Text('Répartition',
                                      style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                                  const SizedBox(height: 8),
                                  SizedBox(
                                    height: 140,
                                    child: _pieData.isEmpty
                                        ? Center(
                                            child: Text('Aucune donnée',
                                                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                          )
                                        : PieChart(
                                            PieChartData(
                                              sections: _pieData,
                                              centerSpaceRadius: 30,
                                              sectionsSpace: 2,
                                            ),
                                          ),
                                  ),
                                  const SizedBox(height: 8),
                                  // Legend
                                  ...(['Actifs', 'Intégration', 'Veille', 'Décrochés']
                                      .asMap()
                                      .entries
                                      .map((e) => GestureDetector(
                                            onTap: () => setState(() {
                                              _filterStatus = ['ACTIF', 'EN_INTEGRATION', 'EN_VEILLE', 'DECROCHE'][e.key];
                                            }),
                                            child: Padding(
                                              padding: const EdgeInsets.symmetric(vertical: 1),
                                              child: Row(
                                                children: [
                                                  Container(width: 8, height: 8,
                                                      decoration: BoxDecoration(
                                                        shape: BoxShape.circle,
                                                        color: [
                                                          const Color(0xFF22C55E),
                                                          const Color(0xFFF59E0B),
                                                          const Color(0xFF3B82F6),
                                                          const Color(0xFFEF4444),
                                                        ][e.key],
                                                      )),
                                                  const SizedBox(width: 6),
                                                  Text(e.value,
                                                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
                                                ],
                                              ),
                                            ),
                                          ))),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          // Alerts
                          Expanded(
                            child: GlassCard(
                              padding: const EdgeInsets.all(12),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.notifications_active, color: Colors.amber, size: 16),
                                      const SizedBox(width: 6),
                                      const Text('Alertes',
                                          style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                                      const Spacer(),
                                      Text('${_alertes.length}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
                                    ],
                                  ),
                                  const SizedBox(height: 8),
                                  if (_alertes.isNotEmpty)
                                    ..._alertes.take(3).map((alert) => GestureDetector(
                                      onTap: () {
                                        final soulId = alert['soulId']?.toString();
                                        if (soulId != null && soulId.isNotEmpty) {
                                          context.go('/souls/$soulId');
                                        } else {
                                          context.go('/alerts');
                                        }
                                      },
                                      child: Container(
                                      margin: const EdgeInsets.only(bottom: 4),
                                      padding: const EdgeInsets.all(8),
                                      decoration: BoxDecoration(
                                        color: (alert['priorite'] == 'HAUTE' ? Colors.red : Colors.amber).withValues(alpha: 0.1),
                                        borderRadius: BorderRadius.circular(8),
                                        border: Border.all(color: (alert['priorite'] == 'HAUTE' ? Colors.red : Colors.amber).withValues(alpha: 0.2)),
                                      ),
                                      child: Row(
                                        children: [
                                          Icon(Icons.warning_amber_rounded,
                                              size: 14,
                                              color: alert['priorite'] == 'HAUTE' ? Colors.red : Colors.amber),
                                          const SizedBox(width: 6),
                                          Expanded(
                                            child: Column(
                                              crossAxisAlignment: CrossAxisAlignment.start,
                                              children: [
                                                Text('${alert['soulNom'] ?? ''}',
                                                    style: const TextStyle(color: Colors.white, fontSize: 11),
                                                    overflow: TextOverflow.ellipsis),
                                                Text('${alert['message'] ?? ''}',
                                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 8),
                                                    maxLines: 1, overflow: TextOverflow.ellipsis),
                                              ],
                                            ),
                                          ),
                                        ],
                                      ),
                                      ),
                                    ))
                                  else
                                    Column(
                                      children: [
                                        Icon(Icons.check_circle, color: Colors.green, size: 32),
                                        const SizedBox(height: 4),
                                        Text('Aucune alerte',
                                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                      ],
                                    ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          // Quick actions
                          Expanded(
                            child: GlassCard(
                              padding: const EdgeInsets.all(12),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  const Text('Actions rapides',
                                      style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600)),
                                  const SizedBox(height: 8),
                                  _quickAction(Icons.description, 'Rapport', '/reports/maker'),
                                  const SizedBox(height: 6),
                                  _quickAction(Icons.person_add, 'Nouveau disciple', '/souls'),
                                  const SizedBox(height: 6),
                                  _quickAction(Icons.book, 'Demande de prière', '/prayers'),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),

                      // Filter tabs
                      SingleChildScrollView(
                        scrollDirection: Axis.horizontal,
                        child: Row(
                          children: [
                            _filterChip('Tous', 'all', '${_disciples.length}'),
                            const SizedBox(width: 6),
                            _filterChip('Actifs', 'ACTIF', '${_stats['actifs'] ?? 0}'),
                            const SizedBox(width: 6),
                            _filterChip('Intégration', 'EN_INTEGRATION', '${_stats['enIntegration'] ?? 0}'),
                            const SizedBox(width: 6),
                            _filterChip('Veille', 'EN_VEILLE', '${_stats['enVeille'] ?? 0}'),
                            const SizedBox(width: 6),
                            _filterChip('Décrochés', 'DECROCHE', '${_stats['decroches'] ?? 0}'),
                            const SizedBox(width: 6),
                            _filterChip('En difficulté', 'EN_DIFFICULTE', '${_soulsEnDifficulte.length}', alert: true),
                          ],
                        ),
                      ),
                      const SizedBox(height: 12),

                      // At-risk souls (en difficulté)
                      if (_filterStatus == 'EN_DIFFICULTE') ...[
                        if (_soulsEnDifficulte.isEmpty)
                          GlassCard(
                            padding: const EdgeInsets.all(32),
                            child: Column(
                              children: [
                                Icon(Icons.check_circle, color: Colors.green.withValues(alpha: 0.5), size: 48),
                                const SizedBox(height: 12),
                                Text('Aucune âme en difficulté',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 14)),
                              ],
                            ),
                          )
                        else
                          ..._soulsEnDifficulte.map(_buildDifficultSoulCard),
                        const SizedBox(height: 12),
                      ],

                      // Disciples list
                      if (_disciples.isEmpty)
                        GlassCard(
                          padding: const EdgeInsets.all(32),
                          child: Column(
                            children: [
                              Icon(Icons.favorite, color: Colors.white.withValues(alpha: 0.2), size: 48),
                              const SizedBox(height: 12),
                              Text('Aucun disciple trouvé',
                                  style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 14)),
                            ],
                          ),
                        )
                      else
                        ..._disciples.asMap().entries.map((entry) {
                          final d = entry.value;
                          final statutColor = d['statut'] == 'ACTIF'
                              ? AppColors.primary
                              : d['statut'] == 'EN_INTEGRATION'
                                  ? Colors.amber
                                  : d['statut'] == 'EN_VEILLE'
                                      ? Colors.blue
                                      : Colors.red;
                          final niveau = d['niveauCroissance'] as int? ?? 1;

                          return Container(
                            margin: const EdgeInsets.only(bottom: 8),
                            child: GlassCard(
                              padding: const EdgeInsets.all(12),
                              onTap: () => context.go('/souls/${d['id']}'),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      // Avatar
                                      Container(
                                        width: 40, height: 40,
                                        decoration: BoxDecoration(
                                          gradient: LinearGradient(
                                            colors: [statutColor, statutColor.withValues(alpha: 0.7)],
                                          ),
                                          borderRadius: BorderRadius.circular(10),
                                        ),
                                        child: Center(
                                          child: Text(
                                            (d['nom'] as String? ?? '?').substring(0, 1).toUpperCase(),
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                                          ),
                                        ),
                                      ),
                                      const SizedBox(width: 12),
                                      // Info
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Row(
                                              children: [
                                                Text(d['nom'] ?? '',
                                                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                                if (d['etatSpirituel'] == 'EN_DIFFICULTE')
                                                  Padding(
                                                    padding: const EdgeInsets.only(left: 4),
                                                    child: Icon(Icons.warning_amber, color: Colors.red, size: 12),
                                                  ),
                                              ],
                                            ),
                                            const SizedBox(height: 2),
                                            Row(
                                              children: [
                                                _smallBadge(statutColor, d['statut'] == 'ACTIF' ? 'Actif'
                                                    : d['statut'] == 'EN_INTEGRATION' ? 'Intégration'
                                                    : d['statut'] == 'EN_VEILLE' ? 'Veille' : 'Décroché'),
                                                const SizedBox(width: 6),
                                                // Stars
                                                ...List.generate(5, (s) => Icon(
                                                  s < niveau ? Icons.star : Icons.star_border,
                                                  size: 10,
                                                  color: s < niveau ? Colors.amber : Colors.white.withValues(alpha: 0.2),
                                                )),
                                              ],
                                            ),
                                          ],
                                        ),
                                      ),
                                      // Report status
                                      Column(
                                        crossAxisAlignment: CrossAxisAlignment.end,
                                        children: [
                                          _smallBadge(
                                            d['rapportSoumis'] == true ? Colors.green : Colors.amber,
                                            d['rapportSoumis'] == true ? 'Rapport ✓' : 'En attente',
                                          ),
                                          if (d['dateDernierContact'] != null) ...[
                                            const SizedBox(height: 4),
                                            Text(
                                              'Contact: ${_formatDate(d['dateDernierContact'] as String?)}',
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 8),
                                            ),
                                          ],
                                          if ((d['nbNotes'] ?? 0) > 0) ...[
                                            const SizedBox(height: 2),
                                            Text(
                                              '${d['nbNotes']} note${d['nbNotes'] > 1 ? 's' : ''}',
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 8),
                                            ),
                                          ],
                                        ],
                                      ),
                                    ],
                                  ),
                                  // Difficulties
                                  if (d['difficultes'] != null && (d['difficultes'] as String).isNotEmpty)
                                    Container(
                                      margin: const EdgeInsets.only(top: 8),
                                      padding: const EdgeInsets.all(8),
                                      decoration: BoxDecoration(
                                        color: Colors.red.withValues(alpha: 0.08),
                                        borderRadius: BorderRadius.circular(8),
                                        border: Border.all(color: Colors.red.withValues(alpha: 0.15)),
                                      ),
                                      child: Row(
                                        children: [
                                          Icon(Icons.warning_amber_rounded, color: Colors.red, size: 12),
                                          const SizedBox(width: 6),
                                          Expanded(
                                            child: Text(d['difficultes'] as String? ?? '',
                                                style: TextStyle(color: Colors.red.withValues(alpha: 0.8), fontSize: 10)),
                                          ),
                                        ],
                                      ),
                                    ),
                                ],
                              ),
                            ),
                          );
                        }),
                      const SizedBox(height: 80),
                    ],
                  ),
                ),
              ),
            ),
    );
  }

  Widget _filterChip(String label, String status, String count, {bool alert = false}) {
    final isActive = _filterStatus == status;
    final color = status == 'ACTIF' ? AppColors.primary
        : status == 'EN_INTEGRATION' ? Colors.amber
        : status == 'EN_VEILLE' ? Colors.blue
        : status == 'DECROCHE' ? Colors.red
        : status == 'EN_DIFFICULTE' ? Colors.deepOrange
        : Colors.white;
    return GestureDetector(
      onTap: () => setState(() => _filterStatus = status),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: isActive ? color.withValues(alpha: 0.15) : Colors.white.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: isActive ? color.withValues(alpha: 0.4) : Colors.white.withValues(alpha: 0.08)),
        ),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          Text(
            isActive && status == 'all' ? 'Tous ($count)' : label,
            style: TextStyle(
              color: isActive ? color : Colors.white.withValues(alpha: 0.5),
              fontSize: 11,
              fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
            ),
          ),
          if (alert) ...[
            const SizedBox(width: 4),
            Text('($count)',
                style: TextStyle(color: color.withValues(alpha: 0.8), fontSize: 11, fontWeight: FontWeight.w700)),
          ],
        ]),
      ),
    );
  }

  Widget _buildDifficultSoulCard(dynamic e) {
    final d = e as Map<String, dynamic>;
    final nom = d['nom']?.toString() ?? d['soulNom']?.toString() ?? '?';
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      child: GlassCard(
        padding: const EdgeInsets.all(12),
        onTap: () => context.go('/souls/${d['id'] ?? d['soulId']}'),
        child: Row(
          children: [
            Container(
              width: 40, height: 40,
              decoration: BoxDecoration(
                gradient: LinearGradient(colors: [Colors.deepOrange, Colors.deepOrange.withValues(alpha: 0.7)]),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Center(
                child: Text(nom.substring(0, 1).toUpperCase(),
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                  const SizedBox(height: 2),
                  Row(children: [
                    _smallBadge(Colors.deepOrange, 'En difficulté'),
                    if ((d['difficultes'] ?? '') != '') ...[
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text('${d['difficultes']}',
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10),
                            overflow: TextOverflow.ellipsis),
                      ),
                    ],
                  ]),
                ],
              ),
            ),
            Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3), size: 18),
          ],
        ),
      ),
    );
  }

  Widget _smallBadge(Color color, String label) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(label,
          style: TextStyle(color: color, fontSize: 8, fontWeight: FontWeight.w600)),
    );
  }

  Widget _quickAction(IconData icon, String label, String route) {
    return GestureDetector(
      onTap: () => context.go(route),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.04),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Row(
          children: [
            Icon(icon, color: AppColors.primaryLight, size: 14),
            const SizedBox(width: 6),
            Expanded(child: Text(label,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 10))),
            Icon(Icons.chevron_right, color: Colors.white.withValues(alpha: 0.3), size: 12),
          ],
        ),
      ),
    );
  }

  String _formatDate(String? dateStr) {
    if (dateStr == null) return '';
    try {
      final d = DateTime.parse(dateStr);
      return DateFormat('d MMM', 'fr_FR').format(d);
    } catch (_) {
      return dateStr;
    }
  }
}
