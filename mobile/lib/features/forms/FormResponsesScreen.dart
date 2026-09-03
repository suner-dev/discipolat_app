import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Réponses aux formulaires — branché sur /api/v1/forms/{templateId}/responses.
class FormResponsesScreen extends StatefulWidget {
  final String templateId;
  const FormResponsesScreen({super.key, required this.templateId});

  @override
  State<FormResponsesScreen> createState() => _FormResponsesScreenState();
}

class _FormResponsesScreenState extends State<FormResponsesScreen> {
  final _apiService = ApiService();
  List<dynamic> _responses = [];
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
        _apiService.get('/forms/${widget.templateId}/responses'),
        _apiService.get('/forms/${widget.templateId}/stats'),
      ]);
      if (mounted) {
        final rData = results[0].data;
        final sData = results[1].data;
        setState(() {
          _responses = rData is List
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
          _error = 'Erreur lors du chargement des réponses';
          _isLoading = false;
        });
      }
    }
  }

  Color _statusColor(String? statut) {
    switch (statut?.toUpperCase()) {
      case 'COMPLETED':
      case 'COMPLETE':
        return Colors.green;
      case 'PARTIAL':
      case 'PARTIEL':
        return Colors.amber;
      case 'PENDING':
      case 'EN_ATTENTE':
        return Colors.blue;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Réponses du formulaire'),
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
                      if (_responses.isEmpty)
                        _buildEmpty()
                      else
                        ..._responses.map((r) => _buildResponseCard(r as Map<String, dynamic>)),
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
              _statItem('Total', '${_stats!['total'] ?? 0}', Colors.blue),
              const SizedBox(width: 12),
              _statItem('Complètes', '${_stats!['completed'] ?? 0}', Colors.green),
              const SizedBox(width: 12),
              _statItem('En attente', '${_stats!['pending'] ?? 0}', Colors.amber),
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

  Widget _buildResponseCard(Map<String, dynamic> response) {
    final auteur = response['auteur']?.toString() ?? response['memberName']?.toString() ?? 'Anonyme';
    final statut = response['statut']?.toString() ?? 'PENDING';
    final dateStr = response['createdAt']?.toString().substring(0, 10) ?? '';
    final score = response['score']?.toString();

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      child: Row(
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: BoxDecoration(
              color: _statusColor(statut).withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(Icons.description, color: _statusColor(statut), size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(auteur,
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                Row(
                  children: [
                    if (dateStr.isNotEmpty)
                      Text(dateStr,
                          style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                    if (score != null) ...[
                      Text('  •  Score : $score',
                          style: const TextStyle(color: Colors.amber, fontSize: 11, fontWeight: FontWeight.w600)),
                    ],
                  ],
                ),
              ],
            ),
          ),
          StatusBadge(label: statut, color: _statusColor(statut)),
        ],
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.description, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune réponse', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
