import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Statistiques du département — données réelles : effectif, présence,
/// tâches, discipline, équipes, postes, charge de travail.
class DepartmentStatsScreen extends StatefulWidget {
  final String departmentId;

  const DepartmentStatsScreen({super.key, required this.departmentId});

  @override
  State<DepartmentStatsScreen> createState() => _DepartmentStatsScreenState();
}

class _DepartmentStatsScreenState extends State<DepartmentStatsScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _stats;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _apiService.get('/departments/${widget.departmentId}/stats');
      if (mounted) {
        setState(() {
          _stats = res.data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final stats = _stats ?? {};
    final e = stats['effectif'] as Map<String, dynamic>? ?? {};
    final presence = stats['presence'] as Map<String, dynamic>? ?? {};
    final taches = stats['taches'] as Map<String, dynamic>? ?? {};
    final equipes = stats['equipes'] as Map<String, dynamic>? ?? {};
    final affectations = stats['affectations'] as Map<String, dynamic>? ?? {};
    final discipline = stats['disciplineParCategorie'] as Map<String, dynamic>? ?? {};
    final charge = (stats['chargeParMembre'] as List<dynamic>?) ?? [];

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).departmentStats),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  // KPI row
                  Row(
                    children: [
                      _statMini(AppLocalizations.of(context).kpiMembers, '${e['total'] ?? 0}', Colors.amber),
                      const SizedBox(width: 8),
                      _statMini(AppLocalizations.of(context).kpiActive, '${e['actifs'] ?? 0}', Colors.green),
                      const SizedBox(width: 8),
                      _statMini(AppLocalizations.of(context).kpiNew, '${e['nouveaux30j'] ?? 0}', Colors.blue),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      _statMini(AppLocalizations.of(context).kpiPresence, '${presence['taux'] ?? 0}%', Colors.purple),
                      const SizedBox(width: 8),
                      _statMini(AppLocalizations.of(context).kpiOverdueTasks, '${taches['enRetard'] ?? 0}', Colors.red),
                      const SizedBox(width: 8),
                      _statMini(AppLocalizations.of(context).kpiTeams, '${equipes['actives'] ?? 0}', Colors.orange),
                    ],
                  ),
                  const SizedBox(height: 12),

                  // Effectif
                  _section(
                    title: AppLocalizations.of(context).memberBreakdown,
                    child: Column(
                      children: [
                        _bar(AppLocalizations.of(context).kpiActive, e['actifs'] ?? 0, e['total'] ?? 0, Colors.green),
                        _bar(AppLocalizations.of(context).integrating, e['enIntegration'] ?? 0, e['total'] ?? 0, Colors.blue),
                        _bar(AppLocalizations.of(context).standby, e['enVeille'] ?? 0, e['total'] ?? 0, Colors.amber),
                        _bar(AppLocalizations.of(context).droppedOut, e['decroches'] ?? 0, e['total'] ?? 0, Colors.red),
                      ],
                    ),
                  ),

                  // Évolution effectif (12 mois)
                  _section(
                    title: AppLocalizations.of(context).headcountEvolution,
                    child: Column(
                      children: (stats['evolutionEffectif'] as List<dynamic>? ?? [])
                          .map((m) => _evolutionRow(m as Map<String, dynamic>))
                          .toList(),
                    ),
                  ),

                  // Présence
                  _section(
                    title: AppLocalizations.of(context).attendanceSection,
                    child: Column(
                      children: [
                        _bar(AppLocalizations.of(context).present, presence['presents'] ?? 0, presence['total'] ?? 0, Colors.green),
                        _bar(AppLocalizations.of(context).absent, presence['absents'] ?? 0, presence['total'] ?? 0, Colors.red),
                      ],
                    ),
                  ),

                  // Tâches par statut
                  _section(
                    title: AppLocalizations.of(context).tasksByStatus,
                    child: Column(
                      children: (taches['parStatut'] as Map<String, dynamic>? ?? {})
                          .entries
                          .map((entry) => _bar(
                                entry.key.replaceAll('_', ' '),
                                entry.value,
                                taches['total'] ?? 0,
                                Colors.blue,
                              ))
                          .toList(),
                    ),
                  ),

                  // Discipline
                  _section(
                    title: AppLocalizations.of(context).disciplinaryCategory,
                    child: discipline.isEmpty
                        ? _empty(AppLocalizations.of(context).noDisciplinary)
                        : Column(
                            children: discipline.entries
                                .map((entry) => _bar(
                                      entry.key.replaceAll('_', ' '),
                                      entry.value,
                                      discipline.values.reduce((a, b) => (a as num) + (b as num)),
                                      Colors.red,
                                    ))
                                .toList(),
                          ),
                  ),

                  // Charge de travail
                  _section(
                    title: AppLocalizations.of(context).workloadPerMember,
                    child: charge.isEmpty
                        ? _empty(AppLocalizations.of(context).noAssignedTasks)
                        : Column(
                            children: charge.map((c) {
                              final m = c as Map<String, dynamic>;
                              return Padding(
                                padding: const EdgeInsets.symmetric(vertical: 4),
                                child: Row(
                                  children: [
                                    Expanded(
                                      child: Text(
                                        '${m['memberNom'] ?? '—'}',
                                        style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 12),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                    Text(
                                      '${m['tachesOuvertes']} ${AppLocalizations.of(context).tasksUnit}',
                                      style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600),
                                    ),
                                    if ((m['enRetard'] ?? 0) > 0)
                                      Padding(
                                        padding: const EdgeInsets.only(left: 8),
                                        child:                                      StatusBadge(label: '${m['enRetard']} ${AppLocalizations.of(context).retardUnit}', color: Colors.red),
                                      ),
                                  ],
                                ),
                              );
                            }).toList(),
                          ),
                  ),

                  // Organisation
                  _section(
                    title: AppLocalizations.of(context).organizationSection,
                    child: Column(
                      children: [
                        _bar(AppLocalizations.of(context).activeAssignments, affectations['actives'] ?? 0, affectations['actives'] ?? 0, Colors.blue),
                        _bar(AppLocalizations.of(context).activePositions, stats['postesActifs'] ?? 0, stats['postesActifs'] ?? 0, Colors.orange),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
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

  Widget _section({required String title, required Widget child}) {
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }

  Widget _bar(String label, dynamic value, dynamic total, Color color) {
    final v = (value is num) ? value.toDouble() : 0.0;
    final t = (total is num) ? total.toDouble() : 1.0;
    final ratio = t > 0 ? (v / t).clamp(0.0, 1.0) : 0.0;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(
            width: 110,
            child: Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 12)),
          ),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: ratio,
                minHeight: 8,
                backgroundColor: Colors.white.withValues(alpha: 0.08),
                valueColor: AlwaysStoppedAnimation<Color>(color),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Text('$v', style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
        ],
      ),
    );
  }

  Widget _evolutionRow(Map<String, dynamic> m) {
    final ajoutes = (m['ajoutes'] as num?)?.toInt() ?? 0;
    final sortis = (m['sortis'] as num?)?.toInt() ?? 0;
    final solde = (m['solde'] as num?)?.toInt() ?? 0;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          SizedBox(
            width: 70,
            child: Text('${m['mois']}', style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11)),
          ),
          Icon(Icons.arrow_upward, size: 13, color: ajoutes > 0 ? Colors.green : Colors.white24),
          Text('$ajoutes', style: const TextStyle(color: Colors.white, fontSize: 12)),
          const SizedBox(width: 10),
          Icon(Icons.arrow_downward, size: 13, color: sortis > 0 ? Colors.red : Colors.white24),
          Text('$sortis', style: const TextStyle(color: Colors.white, fontSize: 12)),
          const Spacer(),
          Text(
            solde >= 0 ? '+$solde' : '$solde',
            style: TextStyle(color: solde >= 0 ? Colors.greenAccent : Colors.redAccent, fontSize: 12, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }

  Widget _empty(String message) {
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Text(message, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
    );
  }
}
