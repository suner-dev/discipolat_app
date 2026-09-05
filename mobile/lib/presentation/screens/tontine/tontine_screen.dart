import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Tontine Numérique — groupes d'épargne solidaire et échéanciers.
/// Lecture ouverte à tous ; écritures réservées aux gestionnaires
/// (ADMIN / PASTEUR / RESPONSABLE), cohérent avec les permissions API.
class TontineScreen extends StatefulWidget {
  const TontineScreen({super.key});

  @override
  State<TontineScreen> createState() => _TontineScreenState();
}

class _TontineScreenState extends State<TontineScreen> {
  final _apiService = ApiService();
  List<dynamic> _groups = [];
  Map<String, dynamic>? _detail;
  Map<String, dynamic>? _stats;
  List<dynamic> _overdue = [];
  bool _isLoading = true;
  bool _canManage = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _isLoading = true);
    try {
      final roleRes = await _apiService.get('/auth/me');
      final roles = roleRes.data['roles'];
      final roleList = roles is List ? roles.map((e) => e.toString()).toList() : <String>[];
      _canManage = roleList.any((r) =>
          r == 'ADMIN' || r == 'PASTEUR' || r == 'RESPONSABLE' || r.endsWith('ROLE_ADMIN') ||
          r.endsWith('ROLE_PASTEUR') || r.endsWith('ROLE_RESPONSABLE'));
    } catch (_) {
      _canManage = false;
    }
    try {
      final res = await _apiService.get('/tontines');
      Map<String, dynamic>? stats;
      try {
        final statsRes = await _apiService.get('/tontines/stats');
        stats = statsRes.data is Map<String, dynamic>
            ? statsRes.data as Map<String, dynamic>
            : (statsRes.data is Map ? Map<String, dynamic>.from(statsRes.data as Map) : null);
      } catch (_) {}
      if (!mounted) return;
      setState(() {
        _groups = (res.data is List ? res.data : []) as List<dynamic>;
        _stats = stats;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _openDetail(String id) async {
    try {
      final results = await Future.wait([
        _apiService.get('/tontines/$id/dashboard'),
        _apiService.get('/tontines/$id/overdue'),
      ]);
      if (!mounted) return;
      setState(() {
        _detail = results[0].data as Map<String, dynamic>?;
        final overData = results[1].data;
        _overdue = overData is Map && overData['content'] is List
            ? overData['content'] as List<dynamic>
            : (overData is List ? overData : []);
      });
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Impossible de charger le détail'),
            backgroundColor: Color(0xFFC62828)));
      }
    }
  }

  Future<void> _markPaid(String groupId, String memberId) async {
    try {
      await _apiService.post('/tontines/$groupId/contributions/$memberId/pay');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Versement enregistré'),
            backgroundColor: Color(0xFF2E7D32)));
      }
      await _openDetail(groupId);
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Échec de l\'enregistrement'),
            backgroundColor: Color(0xFFC62828)));
      }
    }
  }

  Future<void> _nextRound(String groupId) async {
    try {
      final res = await _apiService.post('/tontines/$groupId/next-round');
      final beneficiary = res.data['beneficiary'];
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(
            content: Text(beneficiary != null
                ? 'Nouveau tour — bénéficiaire : $beneficiary'
                : 'Tour suivant activé'),
            backgroundColor: const Color(0xFF2E7D32)));
      }
      await _openDetail(groupId);
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text('Impossible de passer au tour suivant'),
            backgroundColor: Color(0xFFC62828)));
      }
    }
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Tontines')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                children: [
                  if (_stats != null) ...[
                    _buildStatsHeader(_stats!),
                    const SizedBox(height: 16),
                  ],
                  if (_groups.isEmpty)
                    const GlassCard(
                      child: Padding(
                        padding: EdgeInsets.all(20),
                        child: Center(
                          child: Text('Aucune tontine active pour le moment.',
                              textAlign: TextAlign.center,
                              style: TextStyle(color: Colors.white54)),
                        ),
                      ),
                    )
                  else
                    ..._groups.map(_buildGroupCard),
                  ..._buildDetail(),
                ],
              ),
            ),
    );
  }

  Widget _buildStatsHeader(Map<String, dynamic> stats) {
    final totalCollecte = (stats['totalCollecte'] ?? stats['totalCollected'] ?? 0) as num;
    final totalAttendu = (stats['totalAttendu'] ?? stats['totalExpected'] ?? 0) as num;
    final groupesActifs = (stats['groupesActifs'] ?? stats['activeGroups'] ?? 0) as num;
    final membresActifs = (stats['membresActifs'] ?? stats['activeMembers'] ?? 0) as num;
    final overdue = (stats['contributionsEnRetard'] ?? stats['overdueContributions'] ?? 0) as num;

    return Row(children: [
      Expanded(
        child: GlassCard(
          padding: const EdgeInsets.all(12),
          child: Column(children: [
            Icon(Icons.groups, color: const Color(0xFF42A5F5), size: 24),
            const SizedBox(height: 6),
            Text('${groupesActifs.toInt()}',
                style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
            Text('Groupes actifs', style: const TextStyle(color: Colors.white54, fontSize: 10)),
          ]),
        ),
      ),
      const SizedBox(width: 10),
      Expanded(
        child: GlassCard(
          padding: const EdgeInsets.all(12),
          child: Column(children: [
            Icon(Icons.monetization_on, color: const Color(0xFFFFB300), size: 24),
            const SizedBox(height: 6),
            Text('$totalCollecte / $totalAttendu',
                style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold),
                overflow: TextOverflow.ellipsis),
            Text('Collecté', style: const TextStyle(color: Colors.white54, fontSize: 10)),
          ]),
        ),
      ),
      const SizedBox(width: 10),
      Expanded(
        child: GlassCard(
          padding: const EdgeInsets.all(12),
          child: Column(children: [
            Icon(Icons.people_alt, color: const Color(0xFF4CAF50), size: 24),
            const SizedBox(height: 6),
            Text('${membresActifs.toInt()}',
                style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
            Text('Membres', style: const TextStyle(color: Colors.white54, fontSize: 10)),
          ]),
        ),
      ),
      const SizedBox(width: 10),
      Expanded(
        child: GlassCard(
          padding: const EdgeInsets.all(12),
          child: Column(children: [
            Icon(Icons.schedule, color: overdue > 0 ? const Color(0xFFC62828) : const Color(0xFF4CAF50), size: 24),
            const SizedBox(height: 6),
            Text('${overdue.toInt()}',
                style: const TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
            Text('En retard', style: const TextStyle(color: Colors.white54, fontSize: 10)),
          ]),
        ),
      ),
    ]);
  }

  Widget _buildGroupCard(dynamic g) {
    final m = g as Map<String, dynamic>;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: GlassCard(
        child: ListTile(
          contentPadding: EdgeInsets.zero,
          title: Text(m['name'] ?? 'Tontine',
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
          subtitle: Text(
            '${m['montantParTour']} / tour · ${(m['periodicite'] ?? '').toString().toLowerCase()}',
            style: const TextStyle(color: Colors.white54, fontSize: 12),
          ),
          trailing: Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFF42A5F5).withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(999),
            ),
            child: Text('Tour ${m['tourActuel'] ?? 1}',
                style: const TextStyle(
                    color: Color(0xFF42A5F5), fontSize: 11,
                    fontWeight: FontWeight.bold)),
          ),
          onTap: () => _openDetail(m['id'] as String),
        ),
      ),
    );
  }


  List<Widget> _buildDetail() {
    final d = _detail;
    if (d == null) return const [];
    final group = d['group'] as Map<String, dynamic>? ?? {};
    final members = (d['members'] as List<dynamic>? ?? []);
    return [
      const SizedBox(height: 8),
      GlassCard(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text('Échéancier — ${group['name'] ?? ''} (tour ${group['tourActuel'] ?? 1})',
                      style: const TextStyle(color: Colors.white,
                          fontWeight: FontWeight.w600)),
                ),
                if (_canManage)
                  IconButton(
                    tooltip: 'Tour suivant',
                    icon: const Icon(Icons.skip_next_rounded, color: Color(0xFFFFB300)),
                    onPressed: () => _nextRound(group['id'] as String),
                  ),
                IconButton(
                  tooltip: 'Fermer',
                  icon: const Icon(Icons.close_rounded, color: Colors.white54),
                  onPressed: () => setState(() => _detail = null),
                ),
              ],
            ),
            Text(
              'Collecté : ${d['totalCollecte'] ?? 0} / ${d['totalAttendu'] ?? 0}',
              style: const TextStyle(color: Colors.white54, fontSize: 12),
            ),
            const SizedBox(height: 8),
            ClipRRect(
              borderRadius: BorderRadius.circular(6),
              child: LinearProgressIndicator(
                value: ((d['progressPercent'] as num?) ?? 0).toDouble().clamp(0.0, 100.0) / 100,
                minHeight: 8,
                color: const Color(0xFFFFB300),
                backgroundColor: Colors.white12,
              ),
            ),
            const SizedBox(height: 12),
            ...members.map((e) {
              final mem = e as Map<String, dynamic>;
              final paye = mem['paye'] == true;
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    Icon(
                      paye ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
                      color: paye ? const Color(0xFF4CAF50) : Colors.white24,
                      size: 18,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(mem['nom'] ?? '',
                          style: const TextStyle(color: Colors.white)),
                    ),
                    if (!paye && _canManage)
                      TextButton(
                        onPressed: () =>
                            _markPaid(group['id'] as String, mem['id'] as String),
                        child: const Text('Marquer payé'),
                      ),
                  ],
                ),
              );
            }),
            // Overdue contributions
            if (_overdue.isNotEmpty) ...[
              const SizedBox(height: 12),
              const Divider(color: Colors.white12),
              const SizedBox(height: 8),
              Row(children: [
                Icon(Icons.schedule, size: 16, color: const Color(0xFFC62828)),
                const SizedBox(width: 6),
                const Text('Contributions en retard',
                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
              ]),
              const SizedBox(height: 8),
              ..._overdue.take(10).map((o) {
                final od = o is Map<String, dynamic> ? o : <String, dynamic>{};
                return Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Row(children: [
                    Icon(Icons.error_outline, size: 16, color: const Color(0xFFC62828)),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(od['nom']?.toString() ?? od['memberName']?.toString() ?? '',
                          style: const TextStyle(color: Colors.white, fontSize: 12)),
                    ),
                    Text(
                      '${od['montant'] ?? od['amount'] ?? ''} · ${od['tour'] ?? od['round'] ?? ''}',
                      style: const TextStyle(color: Colors.white54, fontSize: 11),
                    ),
                  ]),
                );
              }),
            ],
          ],
        ),
      ),
    ];
  }}
