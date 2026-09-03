import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Plans de développement — branché sur /api/v1/development-plans.
class DevelopmentPlansScreen extends StatefulWidget {
  const DevelopmentPlansScreen({super.key});

  @override
  State<DevelopmentPlansScreen> createState() => _DevelopmentPlansScreenState();
}

class _DevelopmentPlansScreenState extends State<DevelopmentPlansScreen> {
  final _apiService = ApiService();
  List<dynamic> _plans = [];
  Map<String, dynamic>? _stats;
  bool _isLoading = true;
  String? _error;
  String? _selectedDeptId;

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
        _selectedDeptId == null
            ? _apiService.get('/development-plans')
            : _apiService.get('/development-plans/by-department/$_selectedDeptId'),
        _apiService.get('/development-plans/stats/me'),
      ]);
      if (mounted) {
        final pData = results[0].data;
        final sData = results[1].data;
        setState(() {
          _plans = pData is List
              ? pData
              : (pData is Map && pData['content'] is List
                  ? pData['content'] as List<dynamic>
                  : <dynamic>[]);
          _stats = sData is Map<String, dynamic> ? sData : null;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement des plans';
          _isLoading = false;
        });
      }
    }
  }

  Color _statusColor(String? statut) {
    switch (statut?.toUpperCase()) {
      case 'TERMINE':
      case 'COMPLETED':
        return Colors.green;
      case 'EN_COURS':
      case 'EN COURS':
        return Colors.blue;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
        return Colors.amber;
      case 'ANNULE':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Plans de développement'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
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
                      if (_plans.isEmpty)
                        _buildEmpty()
                      else
                        ..._plans.map((p) => _buildPlanCard(p as Map<String, dynamic>)),
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
          const Text('Mes statistiques', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          Row(
            children: [
              _statItem('Total', '${_stats!['total'] ?? 0}', Colors.blue),
              const SizedBox(width: 12),
              _statItem('En cours', '${_stats!['enCours'] ?? _stats!['active'] ?? 0}', Colors.amber),
              const SizedBox(width: 12),
              _statItem('Terminés', '${_stats!['termines'] ?? _stats!['completed'] ?? 0}', Colors.green),
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

  Widget _buildPlanCard(Map<String, dynamic> plan) {
    final titre = plan['titre']?.toString() ?? plan['nom']?.toString() ?? 'Plan';
    final statut = plan['statut']?.toString() ?? 'EN_ATTENTE';
    final progression = (plan['progression'] as num?)?.toDouble() ?? 0;
    final description = plan['description']?.toString() ?? '';

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.trending_up, color: _statusColor(statut), size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(titre,
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
              ),
              StatusBadge(label: statut, color: _statusColor(statut)),
            ],
          ),
          if (description.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(description,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                maxLines: 2,
                overflow: TextOverflow.ellipsis),
          ],
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(4),
                  child: LinearProgressIndicator(
                    value: progression / 100,
                    minHeight: 6,
                    backgroundColor: Colors.white.withValues(alpha: 0.08),
                    valueColor: AlwaysStoppedAnimation(_statusColor(statut)),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Text('${progression.round()}%',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12, fontWeight: FontWeight.bold)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.trending_up, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun plan de développement', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
