import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../widgets/glass_theme.dart';

/// P3 #109 — Analytics d'usage mobile
class UsageAnalyticsScreen extends StatefulWidget {
  const UsageAnalyticsScreen({super.key});

  @override
  State<UsageAnalyticsScreen> createState() => _UsageAnalyticsScreenState();
}

class _UsageAnalyticsScreenState extends State<UsageAnalyticsScreen> {
  final ApiService _api = ApiService();
  Map<String, dynamic>? _summary;
  bool _isLoading = true;
  String _period = '7d';

  @override
  void initState() {
    super.initState();
    _loadSummary();
  }

  Future<void> _loadSummary() async {
    setState(() => _isLoading = true);
    try {
      final res = await _api.get('/usage-analytics/summary', params: {'period': _period});
      setState(() {
        _summary = res.data as Map<String, dynamic>?;
        _isLoading = false;
      });
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Analytics d\'usage'),
        actions: [
          PopupMenuButton<String>(
            initialValue: _period,
            onSelected: (v) { setState(() => _period = v); _loadSummary(); },
            itemBuilder: (_) => [
              const PopupMenuItem(value: '1d', child: Text('24 heures')),
              const PopupMenuItem(value: '7d', child: Text('7 jours')),
              const PopupMenuItem(value: '30d', child: Text('30 jours')),
            ],
          ),
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadSummary),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _summary == null
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.bar_chart, size: 64, color: Colors.white.withValues(alpha: 0.2)),
                      const SizedBox(height: 12),
                      Text('Aucune donnée disponible',
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadSummary,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // ── Stats Grid ──
                      GridView.count(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        crossAxisCount: 2,
                        crossAxisSpacing: 12,
                        mainAxisSpacing: 12,
                        childAspectRatio: 1.5,
                        children: [
                          _statCard('Événements', '${_summary!['totalEvents'] ?? 0}', Icons.visibility, Colors.cyan),
                          _statCard('Utilisateurs actifs', '${_summary!['activeUsers'] ?? 0}', Icons.people, Colors.green),
                          _statCard('Mobile', '${_mobilePct()}%', Icons.phone_android, Colors.blue),
                          _statCard('Desktop', '${_desktopPct()}%', Icons.computer, Colors.purple),
                        ],
                      ),

                      const SizedBox(height: 20),

                      // ── Top Pages ──
                      const Text('Pages les plus vues',
                          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                      const SizedBox(height: 8),
                      ...(_topPages().map((p) => GlassCard(
                        margin: const EdgeInsets.only(bottom: 6),
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                        child: Row(
                          children: [
                            Icon(Icons.pageview, color: Colors.cyan.withValues(alpha: 0.7), size: 16),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(p['page'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 12),
                                  maxLines: 1, overflow: TextOverflow.ellipsis),
                            ),
                            Text('${p['views'] ?? 0}', style: const TextStyle(color: Colors.cyan, fontWeight: FontWeight.bold, fontSize: 12)),
                          ],
                        ),
                      ))),

                      const SizedBox(height: 20),

                      // ── Top Actions ──
                      const Text('Actions fréquentes',
                          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 14)),
                      const SizedBox(height: 8),
                      ...(_topActions().map((a) => GlassCard(
                        margin: const EdgeInsets.only(bottom: 6),
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                        child: Row(
                          children: [
                            Icon(Icons.touch_app, color: Colors.green.withValues(alpha: 0.7), size: 16),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(a['action'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 12),
                                  maxLines: 1, overflow: TextOverflow.ellipsis),
                            ),
                            Text('${a['count'] ?? 0}', style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold, fontSize: 12)),
                          ],
                        ),
                      ))),
                    ],
                  ),
                ),
    );
  }

  int _mobilePct() {
    final bd = _summary!['byDevice'] as Map<String, dynamic>? ?? {};
    final m = (bd['mobile'] ?? 0) as int;
    final d = (bd['desktop'] ?? 0) as int;
    final t = (bd['tablet'] ?? 0) as int;
    final total = m + d + t;
    return total > 0 ? (m * 100 / total).round() : 0;
  }

  int _desktopPct() {
    final bd = _summary!['byDevice'] as Map<String, dynamic>? ?? {};
    final m = (bd['mobile'] ?? 0) as int;
    final d = (bd['desktop'] ?? 0) as int;
    final t = (bd['tablet'] ?? 0) as int;
    final total = m + d + t;
    return total > 0 ? (d * 100 / total).round() : 0;
  }

  List<Map<String, dynamic>> _topPages() {
    return (_summary!['topPages'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
  }

  List<Map<String, dynamic>> _topActions() {
    return (_summary!['topActions'] as List<dynamic>? ?? []).cast<Map<String, dynamic>>();
  }

  Widget _statCard(String label, String value, IconData icon, Color color) {
    return GlassCard(
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: color.withValues(alpha: 0.7), size: 18),
          const Spacer(),
          Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 20)),
          const SizedBox(height: 2),
          Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 10)),
        ],
      ),
    );
  }
}
