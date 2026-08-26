import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';
import '../../../app.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';

class ChefFamilleDashboardScreen extends StatefulWidget {
  final ApiService? apiService;
  const ChefFamilleDashboardScreen({super.key, this.apiService});

  @override
  State<ChefFamilleDashboardScreen> createState() => _ChefFamilleDashboardScreenState();
}

class _ChefFamilleDashboardScreenState extends State<ChefFamilleDashboardScreen> with SingleTickerProviderStateMixin {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  Map<String, dynamic>? _dashboard;
  List<dynamic> _workload = [];
  List<dynamic> _alerts = [];
  List<dynamic> _upcomingVisits = [];
  List<dynamic> _prayers = [];
  List<dynamic> _events = [];
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

      // Charge de travail des faiseurs
      List<dynamic> workload = [];
      try {
        final workloadRes = await _apiService.get('/users/faiseur-workload',
            params: familleId != null ? {'familleId': familleId} : null);
        workload = (workloadRes.data as List?) ?? [];
      } catch (_) {}

      // Alertes
      List<dynamic> alerts = [];
      try {
        final alertRes = await _apiService.get('/alerts', params: {'familleId': familleId, 'size': 5});
        alerts = (alertRes.data as Map?)?['content'] as List<dynamic>? ?? [];
      } catch (_) {}

      // Visites à venir
      List<dynamic> visits = [];
      try {
        final visitRes = await _apiService.get('/visits/upcoming');
        visits = (visitRes.data as List?) ?? [];
      } catch (_) {}

      // Prières
      List<dynamic> prayers = [];
      try {
        final prayerRes = await _apiService.get('/prayers', params: {'familleId': familleId, 'size': 5});
        prayers = (prayerRes.data as Map?)?['content'] as List<dynamic>? ?? [];
      } catch (_) {}

      // Événements
      List<dynamic> events = [];
      try {
        final eventRes = await _apiService.get('/events', params: {'size': 6});
        events = (eventRes.data as Map?)?['content'] as List<dynamic>? ?? [];
      } catch (_) {}

      if (mounted) {
        setState(() {
          _dashboard = response.data as Map<String, dynamic>?;
          _workload = workload;
          _alerts = alerts;
          _upcomingVisits = visits;
          _prayers = prayers;
          _events = events;
          _isLoading = false;
        });
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

    final activeAlerts = _alerts.where((a) => (a as Map<String, dynamic>)['statut'] == 'ACTIVE').toList();

    // Progression stats
    final progression = disciples.isNotEmpty
        ? (disciples.map((d) => ((d as Map<String, dynamic>)['niveauCroissance'] ?? 1) as int).reduce((a, b) => a + b) / disciples.length).toStringAsFixed(1)
        : '0';

    // Growth distribution
    final byStatut = <String, int>{};
    for (final d in disciples) {
      final s = (d as Map<String, dynamic>)['statut'] as String? ?? 'ACTIF';
      byStatut[s] = (byStatut[s] ?? 0) + 1;
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(famille['nom'] ?? AppLocalizations.of(context).dashMyFamily),
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
                      // ==================== STATS GRID ====================
                      GridView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                          crossAxisCount: 2, childAspectRatio: 1.5, crossAxisSpacing: 10, mainAxisSpacing: 10,
                        ),
                        itemCount: 8,
                        itemBuilder: (_, i) {
                          final items = [
                            {'label': 'Disciples', 'value': '${stats['totalDisciples'] ?? 0}', 'icon': Icons.favorite, 'color': const Color(0xFFD4AF37)},
                            {'label': 'Faiseurs', 'value': '${stats['totalFaiseurs'] ?? 0}', 'icon': Icons.group, 'color': Colors.teal},
                            {'label': 'Actifs', 'value': '${stats['actifs'] ?? 0}', 'icon': Icons.check_circle, 'color': Colors.green},
                            {'label': 'Rapports', 'value': '${stats['rapportsSoumisSemaine'] ?? 0}/${stats['totalDisciples'] ?? 0}', 'icon': Icons.description, 'color': Colors.blue},
                            {'label': 'Alertes', 'value': '${activeAlerts.length}', 'icon': Icons.warning_amber, 'color': Colors.red},
                            {'label': 'Visites', 'value': '${_upcomingVisits.length}', 'icon': Icons.map_outlined, 'color': Colors.cyan},
                            {'label': 'Prières', 'value': '${_prayers.length}', 'icon': Icons.book, 'color': Colors.indigo},
                            {'label': 'Progression', 'value': progression, 'icon': Icons.trending_up, 'color': Colors.purple},
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
                              if (label == 'Disciples' || label == 'Actifs') {
                                context.go('/souls');
                              } else if (label == 'Faiseurs') {
                                context.go('/users');
                              } else if (label == 'Rapports') {
                                context.go('/reports/family');
                              } else if (label == 'Alertes') {
                                context.go('/alerts');
                              } else if (label == 'Visites') {
                                context.go('/visits');
                              } else if (label == 'Prières') {
                                context.go('/prayers');
                              } else {
                                context.go('/families');
                              }
                            },
                          );
                        },
                      ),
                      const SizedBox(height: 16),

                      // ==================== RÉPARTITION DES DISCIPLES ====================
                      SectionTitle(title: AppLocalizations.of(context).dashDisciplesSplit, icon: Icons.pie_chart),
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceAround,
                          children: [
                            _statusPill(AppLocalizations.of(context).dashActiveLabel, byStatut['ACTIF'] ?? 0, Colors.green),
                            _statusPill(AppLocalizations.of(context).dashPresenceLabel, byStatut['EN_INTEGRATION'] ?? 0, Colors.amber),
                            _statusPill(AppLocalizations.of(context).dashTransferLabel, byStatut['EN_VEILLE'] ?? 0, Colors.blue),
                            _statusPill(AppLocalizations.of(context).dashDroppedLabel, byStatut['DECROCHE'] ?? 0, Colors.red),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),

                      // ==================== ALERTES ACTIVES ====================
                      if (activeAlerts.isNotEmpty) ...[
                        SectionTitle(
                          title: AppLocalizations.of(context).dashActiveAlertsTitle,
                          icon: Icons.warning_amber,
                          trailing: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                            decoration: BoxDecoration(color: Colors.red.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(12)),
                            child: Text('${activeAlerts.length}', style: const TextStyle(color: Colors.red, fontSize: 12, fontWeight: FontWeight.bold)),
                          ),
                        ),
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
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== VISITES À VENIR ====================
                      if (_upcomingVisits.isNotEmpty) ...[
                        SectionTitle(title: AppLocalizations.of(context).dashUpcomingVisits, icon: Icons.map_outlined),
                        ..._upcomingVisits.take(4).map((v) {
                          final visit = v as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/visits'),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(color: Colors.cyan.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                                  child: const Icon(Icons.map_outlined, color: Colors.cyan, size: 18),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(visit['soulNom'] ?? visit['titre'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                                      Text('${visit['datePrevue'] ?? '—'} · ${visit['motif'] ?? ''}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== CHARGE DE TRAVAIL DES FAISEURS ====================
                      if (_workload.isNotEmpty) ...[
                        SectionTitle(title: AppLocalizations.of(context).dashMakerWorkload, icon: Icons.bar_chart),
                        ..._workload.take(6).map((w) {
                          final charge = (w['charge'] as String?) ?? '';
                          final chargeColor = charge == 'SURCHARGÉ'
                              ? Colors.redAccent
                              : charge == 'LEGER' ? Colors.greenAccent : Colors.blueAccent;
                          return GestureDetector(
                            onTap: () => context.go('/users'),
                            child: Container(
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
                                      charge == 'SURCHARGÉ' ? AppLocalizations.of(context).dashSurcharge : charge == 'LEGER' ? AppLocalizations.of(context).dashLightLoad : AppLocalizations.of(context).dashNormalLoad,
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
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== FAISEURS & DISCIPLES ====================
                      if (faiseurs.isNotEmpty) ...[
                        SectionTitle(title: AppLocalizations.of(context).dashMakersCount(faiseurs.length), icon: Icons.account_tree),
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
                                final discipleId = disciple['id']?.toString();
                                return ListTile(
                                  onTap: (discipleId != null && discipleId.isNotEmpty)
                                      ? () => context.go('/souls/$discipleId')
                                      : null,
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
                      const SizedBox(height: 16),

                      // ==================== PRIÈRES ====================
                      if (_prayers.isNotEmpty) ...[
                        SectionTitle(title: AppLocalizations.of(context).dashRecentPrayers, icon: Icons.book),
                        ..._prayers.take(3).map((p) {
                          final prayer = p as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/prayers'),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(color: Colors.indigo.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                                  child: const Icon(Icons.book, color: Colors.indigo, size: 18),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(prayer['titre'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                                          maxLines: 1, overflow: TextOverflow.ellipsis),
                                      Text('${prayer['categorie'] ?? ''} · ${prayer['statut'] ?? ''}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          );
                        }),
                        const SizedBox(height: 16),
                      ],

                      // ==================== ÉVÉNEMENTS À VENIR ====================
                      if (_events.isNotEmpty) ...[
                        SectionTitle(title: AppLocalizations.of(context).dashUpcomingEvents, icon: Icons.event),
                        ..._events.take(4).map((ev) {
                          final event = ev as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            onTap: () => context.go('/events'),
                            child: Row(
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(color: AppColors.primary.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                                  child: Icon(Icons.event, color: AppColors.primaryLight, size: 18),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Text(event['titre'] ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13),
                                          maxLines: 1, overflow: TextOverflow.ellipsis),
                                      Text('${event['dateDebut'] ?? '—'}${event['lieu'] != null ? ' · ${event['lieu']}' : ''}',
                                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                    ],
                                  ),
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

  Widget _statusPill(String label, int count, Color color) {
    return Column(
      children: [
        Text('$count', style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 20)),
        const SizedBox(height: 2),
        Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10)),
      ],
    );
  }
}
