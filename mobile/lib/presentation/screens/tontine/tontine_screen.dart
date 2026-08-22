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
      if (!mounted) return;
      setState(() {
        _groups = (res.data is List ? res.data : []) as List<dynamic>;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _openDetail(String id) async {
    try {
      final res = await _apiService.get('/tontines/$id');
      if (!mounted) return;
      setState(() => _detail = res.data as Map<String, dynamic>?);
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
          ],
        ),
      ),
    ];
  }}
