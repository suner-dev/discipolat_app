import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class EvangelismScreen extends StatefulWidget {
  const EvangelismScreen({super.key});

  @override
  State<EvangelismScreen> createState() => _EvangelismScreenState();
}

class _EvangelismScreenState extends State<EvangelismScreen> {
  final _apiService = ApiService();
  List<dynamic> _tracks = [];
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
      final tracksRes = await _apiService.get('/evangelism', params: {'size': '50'});
      final statsRes = await _apiService.get('/evangelism/stats');
      if (mounted) {
        setState(() {
          _tracks = (tracksRes.data is Map ? tracksRes.data['content'] : tracksRes.data) as List<dynamic>? ?? [];
          _stats = statsRes.data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color _stepColor(int etape) {
    if (etape <= 3) return Colors.orange;
    if (etape <= 7) return Colors.blue;
    return Colors.green;
  }

  String _stepLabel(int etape) {
    const steps = [
      'Nouvelle âme', 'Premier contact', 'Visite', 'Invitation',
      'Premier culte', 'Suivi', 'Baptême', 'Département',
      'Famille', 'Discipolat', 'Leader'
    ];
    return etape > 0 && etape <= steps.length ? steps[etape - 1] : 'Étape $etape';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Évangélisation')),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  if (_stats != null) ...[
                    _buildStatsCards(),
                    const SizedBox(height: 16),
                  ],
                  Text(
                    'Pipelines actifs',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 16, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 10),
                  if (_tracks.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.route, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucun pipeline actif', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    )
                  else
                    ..._tracks.map(_buildTrackCard),
                ],
              ),
            ),
    );
  }

  Widget _buildStatsCards() {
    final total = _stats!['total'] ?? 0;
    final active = _stats!['actifs'] ?? 0;
    final converted = _stats!['baptemes'] ?? 0;
    return Row(
      children: [
        _statMini('Total', '$total', Colors.blue),
        const SizedBox(width: 8),
        _statMini('Actifs', '$active', Colors.green),
        const SizedBox(width: 8),
        _statMini('Baptêmes', '$converted', Colors.purple),
      ],
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

  Widget _buildTrackCard(dynamic t) {
    final track = t as Map<String, dynamic>;
    final etape = track['etapeActuelle'] ?? 1;
    final nom = track['ameNom'] ?? track['soulNom'] ?? 'Inconnu';
    final faiseur = track['faiseurNom'] ?? '—';
    final progress = (etape / 11 * 100).round();

    return GlassCard(
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 36, height: 36,
                decoration: BoxDecoration(
                  color: _stepColor(etape).withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Center(
                  child: Text('$etape', style: TextStyle(color: _stepColor(etape), fontWeight: FontWeight.bold)),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    Text('Faiseur: $faiseur', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
                  ],
                ),
              ),
              Text('$progress%', style: TextStyle(color: _stepColor(etape), fontWeight: FontWeight.bold, fontSize: 13)),
            ],
          ),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: etape / 11,
              backgroundColor: Colors.white.withValues(alpha: 0.08),
              valueColor: AlwaysStoppedAnimation(_stepColor(etape)),
              minHeight: 6,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Étape $etape: ${_stepLabel(etape)}',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11),
          ),
        ],
      ),
    );
  }
}
