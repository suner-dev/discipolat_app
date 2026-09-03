import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Parcours de disciple — branché sur /api/v1/discipleship-paths.
class DiscipleshipPathsScreen extends StatefulWidget {
  const DiscipleshipPathsScreen({super.key});

  @override
  State<DiscipleshipPathsScreen> createState() => _DiscipleshipPathsScreenState();
}

class _DiscipleshipPathsScreenState extends State<DiscipleshipPathsScreen> {
  final _apiService = ApiService();
  List<dynamic> _paths = [];
  Map<String, dynamic>? _stats;
  bool _isLoading = true;
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
        _apiService.get('/discipleship-paths'),
        _apiService.get('/discipleship-paths/stats'),
      ]);
      if (mounted) {
        final pData = results[0].data;
        final sData = results[1].data;
        setState(() {
          _paths = pData is List
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
          _error = 'Erreur lors du chargement';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _createPath() async {
    final nomCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    final result = await showDialog<Map<String, String>>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF16213A),
        title: const Text('Nouveau parcours', style: TextStyle(color: Colors.white)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nomCtrl,
              style: const TextStyle(color: Colors.white),
              decoration: InputDecoration(
                hintText: 'Nom du parcours',
                hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                filled: true,
                fillColor: Colors.white.withValues(alpha: 0.06),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: descCtrl,
              style: const TextStyle(color: Colors.white),
              maxLines: 3,
              decoration: InputDecoration(
                hintText: 'Description',
                hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                filled: true,
                fillColor: Colors.white.withValues(alpha: 0.06),
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, {'nom': nomCtrl.text, 'description': descCtrl.text}),
            child: const Text('Créer'),
          ),
        ],
      ),
    );
    if (result != null && (result['nom'] ?? '').isNotEmpty) {
      try {
        await _apiService.post('/discipleship-paths', data: result);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Parcours créé'), backgroundColor: Colors.green),
          );
          _loadData();
        }
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Erreur lors de la création'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Color _progressColor(double progress) {
    if (progress >= 100) return Colors.green;
    if (progress >= 50) return Colors.blue;
    if (progress >= 25) return Colors.amber;
    return Colors.grey;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Parcours de disciple'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _createPath,
        icon: const Icon(Icons.add, size: 18),
        label: const Text('Nouveau parcours'),
        backgroundColor: AppColors.primary,
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
                      if (_paths.isEmpty)
                        _buildEmpty()
                      else
                        ..._paths.map((p) => _buildPathCard(p as Map<String, dynamic>)),
                      const SizedBox(height: 80),
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
              _statItem('Parcours', '${_stats!['total'] ?? _stats!['totalPaths'] ?? 0}', Colors.blue),
              const SizedBox(width: 12),
              _statItem('Participants', '${_stats!['participants'] ?? _stats!['totalParticipants'] ?? 0}', Colors.green),
              const SizedBox(width: 12),
              _statItem('Taux', '${_stats!['completionRate'] ?? 0}%', Colors.amber),
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
            Text(value, style: TextStyle(color: color, fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
          ],
        ),
      ),
    );
  }

  Widget _buildPathCard(Map<String, dynamic> path) {
    final nom = path['nom']?.toString() ?? path['name']?.toString() ?? 'Parcours';
    final description = path['description']?.toString() ?? '';
    final etapes = path['nbEtapes']?.toString() ?? path['steps']?.toString() ?? '';
    final progression = (path['progression'] as num?)?.toDouble() ?? 0;
    final participants = path['participants']?.toString() ?? '';

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 44, height: 44,
                decoration: BoxDecoration(
                  color: AppColors.primary.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(Icons.route, color: AppColors.primary, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(nom,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    Row(
                      children: [
                        if (etapes.isNotEmpty)
                          Text('$etapes étapes',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                        if (etapes.isNotEmpty && participants.isNotEmpty)
                          Text('  •  ', style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 11)),
                        if (participants.isNotEmpty)
                          Text('$participants participants',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                      ],
                    ),
                  ],
                ),
              ),
              StatusBadge(label: '${progression.round()}%', color: _progressColor(progression)),
            ],
          ),
          if (description.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(description,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                maxLines: 2,
                overflow: TextOverflow.ellipsis),
          ],
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.route, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun parcours disponible', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
