import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Matching de compétences — branché sur /api/v1/skill-matching.
class SkillMatchingScreen extends StatefulWidget {
  const SkillMatchingScreen({super.key});

  @override
  State<SkillMatchingScreen> createState() => _SkillMatchingScreenState();
}

class _SkillMatchingScreenState extends State<SkillMatchingScreen> {
  final _apiService = ApiService();
  List<dynamic> _matches = [];
  Map<String, dynamic>? _stats;
  bool _isLoading = true;
  bool _isRunning = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        _apiService.get('/skill-matching'),
        _apiService.get('/skill-matching/stats'),
      ]);
      if (mounted) {
        final mData = results[0].data;
        final sData = results[1].data;
        setState(() {
          _matches = mData is List ? mData : (mData is Map && mData['content'] is List ? mData['content'] as List<dynamic> : <dynamic>[]);
          _stats = sData is Map<String, dynamic> ? sData : null;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _runMatching() async {
    setState(() => _isRunning = true);
    try {
      await _apiService.post('/skill-matching/run');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Matching lancé'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors du lancement'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isRunning = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Matching de compétences'),
        backgroundColor: Colors.blue.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isRunning ? null : _runMatching,
        icon: _isRunning
            ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.play_arrow, size: 18),
        label: Text(_isRunning ? 'En cours...' : 'Lancer le matching'),
        backgroundColor: Colors.blue.shade700,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (_stats != null) _buildStatsCard(),
                      const SizedBox(height: 16),
                      if (_matches.isEmpty)
                        _buildEmpty()
                      else
                        ..._matches.map((m) => _buildMatchCard(m as Map<String, dynamic>)),
                    ],
                  ),
                ),
    );
  }

  Widget _buildStatsCard() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Statistiques', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          Row(
            children: [
              _statItem('Matches', '${_stats!['totalMatches'] ?? _stats!['total'] ?? 0}', Colors.blue),
              const SizedBox(width: 12),
              _statItem('Taux', '${_stats!['matchRate'] ?? _stats!['rate'] ?? 0}%', Colors.green),
            ],
          ),
        ],
      ),
    );
  }

  Widget _statItem(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: color.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
          ],
        ),
      ),
    );
  }

  Widget _buildMatchCard(Map<String, dynamic> m) {
    final score = (m['score'] as num?)?.toDouble() ?? 0;
    final name = m['memberName']?.toString() ?? m['nom']?.toString() ?? 'Membre';
    final skill = m['skill']?.toString() ?? m['competence']?.toString() ?? '';
    final color = score >= 0.8 ? Colors.green : (score >= 0.5 ? Colors.amber : Colors.grey);
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Center(
              child: Text('${(score * 100).round()}%',
                  style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.bold)),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                if (skill.isNotEmpty)
                  Text(skill, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
              ],
            ),
          ),
          StatusBadge(label: '${(score * 100).round()}%', color: color),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.extension, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun match trouvé', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
          const SizedBox(height: 12),
          Text(_error!, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)), textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: _loadData, icon: const Icon(Icons.refresh, size: 16), label: const Text('Réessayer')),
        ],
      ),
    );
  }
}
