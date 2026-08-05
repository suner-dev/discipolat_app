import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class ObjectivesScreen extends StatefulWidget {
  const ObjectivesScreen({super.key});

  @override
  State<ObjectivesScreen> createState() => _ObjectivesScreenState();
}

class _ObjectivesScreenState extends State<ObjectivesScreen> {
  final _apiService = ApiService();
  List<dynamic> _objectives = [];
  List<dynamic> _progress = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final objRes = await _apiService.get('/objectives');
      final progRes = await _apiService.get('/objectives/my-progress');
      if (mounted) {
        setState(() {
          _objectives = (objRes.data is Map ? objRes.data['content'] : objRes.data) as List<dynamic>? ?? [];
          _progress = (progRes.data is Map ? progRes.data['content'] : progRes.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Objectifs')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  if (_progress.isNotEmpty) ...[
                    Text('Ma progression', style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 16, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 10),
                    ..._progress.map(_buildProgressCard),
                    const SizedBox(height: 16),
                  ],
                  Text('Objectifs définis', style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 16, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 10),
                  if (_objectives.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.flag_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucun objectif défini', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._objectives.map(_buildObjectiveCard),
                ],
              ),
            ),
    );
  }

  Widget _buildProgressCard(dynamic p) {
    final prog = p as Map<String, dynamic>;
    final type = prog['type'] ?? '—';
    final actuel = prog['actuel'] ?? 0;
    final cible = prog['cible'] ?? 1;
    final ratio = cible > 0 ? (actuel / cible).clamp(0.0, 1.0) : 0.0;
    final pct = (ratio * 100).round();

    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(child: Text(type, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14))),
              Text('$actuel/$cible', style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
            ],
          ),
          const SizedBox(height: 8),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: ratio,
              backgroundColor: Colors.white.withValues(alpha: 0.08),
              valueColor: AlwaysStoppedAnimation(pct >= 100 ? Colors.green : Colors.amber),
              minHeight: 6,
            ),
          ),
          const SizedBox(height: 4),
          Text('$pct%', style: TextStyle(
            color: pct >= 100 ? Colors.green : Colors.amber,
            fontSize: 11, fontWeight: FontWeight.w600,
          )),
        ],
      ),
    );
  }

  Widget _buildObjectiveCard(dynamic o) {
    final obj = o as Map<String, dynamic>;
    final type = obj['type'] ?? '—';
    final cible = obj['cible'] ?? 0;
    final isActive = obj['actif'] ?? true;

    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: (isActive ? Colors.blue : Colors.grey).withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(Icons.flag, color: isActive ? Colors.blue : Colors.grey, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(type, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                Text('Cible: $cible', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: (isActive ? Colors.green : Colors.red).withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Text(
              isActive ? 'Actif' : 'Inactif',
              style: TextStyle(color: isActive ? Colors.green : Colors.red, fontSize: 10, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}
