import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Demandes de suivi — branché sur /api/v1/follow-up-requests.
class FollowUpRequestsScreen extends StatefulWidget {
  const FollowUpRequestsScreen({super.key});

  @override
  State<FollowUpRequestsScreen> createState() => _FollowUpRequestsScreenState();
}

class _FollowUpRequestsScreenState extends State<FollowUpRequestsScreen> {
  final _apiService = ApiService();
  List<dynamic> _requests = [];
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
        _apiService.get('/follow-up-requests/pending'),
        _apiService.get('/follow-up-requests/stats'),
      ]);
      if (mounted) {
        final rData = results[0].data;
        final sData = results[1].data;
        setState(() {
          _requests = rData is List
              ? rData
              : (rData is Map && rData['content'] is List
                  ? rData['content'] as List<dynamic>
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

  Future<void> _assignRequest(String id) async {
    try {
      await _apiService.post('/follow-up-requests/$id/assign');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Demande assignée'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de l\'assignation'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Color _priorityColor(String? priorite) {
    switch (priorite?.toUpperCase()) {
      case 'URGENTE':
      case 'URGENT':
        return Colors.red;
      case 'HAUTE':
      case 'HIGH':
        return Colors.orange;
      case 'MOYENNE':
      case 'MEDIUM':
        return Colors.amber;
      case 'BASSE':
      case 'LOW':
        return Colors.green;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Demandes de suivi'),
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
                      if (_requests.isEmpty)
                        _buildEmpty()
                      else
                        ..._requests.map((r) => _buildRequestCard(r as Map<String, dynamic>)),
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
              _statItem('En attente', '${_stats!['pending'] ?? _stats!['enAttente'] ?? 0}', Colors.amber),
              const SizedBox(width: 12),
              _statItem('Assignées', '${_stats!['assigned'] ?? _stats!['assignees'] ?? 0}', Colors.blue),
              const SizedBox(width: 12),
              _statItem('Terminées', '${_stats!['completed'] ?? _stats!['terminees'] ?? 0}', Colors.green),
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

  Widget _buildRequestCard(Map<String, dynamic> request) {
    final membre = request['membre']?.toString() ?? request['memberName']?.toString() ?? 'Membre';
    final motif = request['motif']?.toString() ?? request['reason']?.toString() ?? '';
    final priorite = request['priorite']?.toString() ?? request['priority']?.toString() ?? '';
    final assigneA = request['assigneA']?.toString() ?? request['assignedTo']?.toString() ?? '';
    final dateStr = request['createdAt']?.toString().substring(0, 10) ?? '';
    final id = request['id']?.toString() ?? '';

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
                  color: _priorityColor(priorite).withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(Icons.person_add, color: _priorityColor(priorite), size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(membre,
                        style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    if (dateStr.isNotEmpty)
                      Text(dateStr,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                  ],
                ),
              ),
              if (priorite.isNotEmpty)
                StatusBadge(label: priorite, color: _priorityColor(priorite)),
            ],
          ),
          if (motif.isNotEmpty) ...[
            const SizedBox(height: 8),
            Text(motif,
                style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12),
                maxLines: 2,
                overflow: TextOverflow.ellipsis),
          ],
          if (assigneA.isNotEmpty) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.person, color: Colors.white.withValues(alpha: 0.3), size: 14),
                const SizedBox(width: 4),
                Text('Assigné à : $assigneA',
                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
              ],
            ),
          ],
          if (id.isNotEmpty && assigneA.isEmpty) ...[
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                onPressed: () => _assignRequest(id),
                icon: const Icon(Icons.check, size: 16),
                label: const Text('Prendre en charge'),
                style: FilledButton.styleFrom(backgroundColor: AppColors.primary),
              ),
            ),
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
          Icon(Icons.check_circle_outline, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune demande en attente', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
