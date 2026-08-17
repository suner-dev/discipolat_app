import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// Administration des pages personnalisées du Page Builder (ADMIN).
///
/// Liste les pages (titre, adresse, rôles, version), permet de publier /
/// dépublier et de supprimer. L'éditeur complet des blocs reste une
/// fonctionnalité web (interface de configuration avancée) ; sur mobile
/// l'administrateur supervise l'état des pages et leur publication.
class PlatformPagesScreen extends StatefulWidget {
  const PlatformPagesScreen({super.key, this.apiService});

  /// Permet d'injecter un ApiService mocké dans les tests widget.
  final ApiService? apiService;

  @override
  State<PlatformPagesScreen> createState() => _PlatformPagesScreenState();
}

class _PlatformPagesScreenState extends State<PlatformPagesScreen> {
  late final ApiService _apiService = widget.apiService ?? ApiService();
  List<Map<String, dynamic>> _pages = [];
  bool _isLoading = true;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    try {
      final res = await _apiService.get('/pages');
      if (mounted) {
        setState(() {
          _pages = (res.data as List).map((e) => e as Map<String, dynamic>).toList();
          _isLoading = false;
        });
      }
    } catch (_) { if (mounted) setState(() => _isLoading = false); }
  }

  Future<void> _toggle(Map<String, dynamic> page, bool published) async {
    try {
      await _apiService.post('/pages/${page['id']}/publish', data: {'published': published});
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la publication')),
        );
      }
    }
  }

  Future<void> _delete(Map<String, dynamic> page) async {
    try {
      await _apiService.delete('/pages/${page['id']}');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Page « ${page['title']} » supprimée')),
        );
      }
      _load();
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Impossible de supprimer cette page')),
        );
      }
    }
  }

  Future<void> _confirmDelete(Map<String, dynamic> page) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: Text('Supprimer la page « ${page['title']} » ?'),
        content: const Text('Cette action est irréversible.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(c, false), child: const Text('Non')),
          TextButton(onPressed: () => Navigator.pop(c, true), child: const Text('Oui')),
        ],
      ),
    );
    if (ok == true) {
      await _delete(page);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Pages personnalisées')),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _load,
              child: _pages.isEmpty
                  ? ListView(physics: const AlwaysScrollableScrollPhysics(), children: const [
                      Padding(
                        padding: EdgeInsets.only(top: 120),
                        child: Column(children: [
                          Icon(Icons.dashboard_customize_rounded, size: 56, color: Colors.white24),
                          SizedBox(height: 12),
                          Text('Aucune page personnalisée', style: TextStyle(color: Colors.white54)),
                        ]),
                      ),
                    ])
                  : ListView(
                      padding: const EdgeInsets.all(16),
                      children: [
                        GlassCard(
                          padding: const EdgeInsets.all(16),
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Row(children: [
                              Icon(Icons.dashboard_customize_rounded, color: AppColors.primary, size: 20),
                              const SizedBox(width: 8),
                              const Text('Page Builder',
                                  style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                            ]),
                            const SizedBox(height: 4),
                            Text('Les pages sont composées de blocs (KPI, tableaux, listes…) configurés sur le web. Les données sont résolues côté serveur sur les entités réelles, scopées selon l’espace métier de chaque utilisateur. Chaque modification est versionnée.',
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                          ]),
                        ),
                        const SizedBox(height: 16),
                        for (final p in _pages) _pageCard(p),
                        const SizedBox(height: 24),
                      ],
                    ),
            ),
    );
  }

  Widget _pageCard(Map<String, dynamic> p) {
    final published = p['published'] == true;
    final enabled = p['enabled'] != false;
    final version = (p['version'] as num?)?.toInt() ?? 1;
    final roles = (p['roles'] as List? ?? <dynamic>[]).cast<String>();
    final slug = p['slug'] as String? ?? '';
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: published ? AppColors.primary.withValues(alpha: 0.2) : Colors.white.withValues(alpha: 0.05),
      child: Row(children: [
        Container(
          width: 38,
          height: 38,
          decoration: BoxDecoration(
            gradient: published
                ? const LinearGradient(colors: [Color(0xFF16A34A), Color(0xFF15803D)], begin: Alignment.topLeft, end: Alignment.bottomRight)
                : LinearGradient(colors: [Colors.white.withValues(alpha: 0.08), Colors.white.withValues(alpha: 0.04)]),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(Icons.dashboard_customize_rounded, color: published ? Colors.white : Colors.white38, size: 18),
        ),
        const SizedBox(width: 10),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Flexible(child: Text(p['title'] as String? ?? '',
                style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w600),
                overflow: TextOverflow.ellipsis)),
            const SizedBox(width: 6),
            if (published)
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: const Color(0xFF16A34A).withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text('Publiée · v$version',
                    style: const TextStyle(color: Color(0xFF4ADE80), fontSize: 8, fontWeight: FontWeight.bold)),
              )
            else
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.08),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Text('Brouillon',
                    style: TextStyle(color: Colors.white54, fontSize: 8, fontWeight: FontWeight.w600)),
              ),
            if (!enabled) ...[
              const SizedBox(width: 6),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                decoration: BoxDecoration(
                  color: Colors.redAccent.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Text('Désactivée',
                    style: TextStyle(color: Colors.redAccent, fontSize: 8, fontWeight: FontWeight.w600)),
              ),
            ],
          ]),
          const SizedBox(height: 2),
          Text(slug.isEmpty ? '' : '/pages/$slug',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10, fontFamily: 'monospace')),
          if (roles.isNotEmpty) ...[
            const SizedBox(height: 4),
            Wrap(spacing: 4, runSpacing: 4, children: [
              for (final r in roles)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.05),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppColors.primary.withValues(alpha: 0.2)),
                  ),
                  child: Text(r, style: TextStyle(color: AppColors.primaryLight, fontSize: 8, fontWeight: FontWeight.w500)),
                ),
            ]),
          ],
        ])),
        const SizedBox(width: 6),
        Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Switch(
            value: published,
            activeThumbColor: Colors.green,
            onChanged: (v) => _toggle(p, v),
          ),
          Row(mainAxisSize: MainAxisSize.min, children: [
            IconButton(
              visualDensity: VisualDensity.compact,
              icon: const Icon(Icons.delete_rounded, color: Colors.redAccent, size: 16),
              onPressed: () => _confirmDelete(p),
            ),
          ]),
        ]),
      ]),
    );
  }
}
