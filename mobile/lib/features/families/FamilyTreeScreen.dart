import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Arbre généalogique — branché sur /api/v1/families/{id}/tree.
class FamilyTreeScreen extends StatefulWidget {
  final String familyId;
  const FamilyTreeScreen({super.key, required this.familyId});

  @override
  State<FamilyTreeScreen> createState() => _FamilyTreeScreenState();
}

class _FamilyTreeScreenState extends State<FamilyTreeScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _tree = [];
  List<dynamic> _history = [];
  List<dynamic> _riskHistory = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _loadData();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        _apiService.get('/families/${widget.familyId}/tree'),
        _apiService.get('/families/${widget.familyId}/history'),
        _apiService.get('/families/${widget.familyId}/risk-history'),
      ]);
      if (mounted) {
        final tData = results[0].data;
        final hData = results[1].data;
        final rData = results[2].data;
        setState(() {
          _tree = tData is List
              ? tData
              : (tData is Map && tData['content'] is List
                  ? tData['content'] as List<dynamic>
                  : <dynamic>[]);
          _history = hData is List
              ? hData
              : (hData is Map && hData['content'] is List
                  ? hData['content'] as List<dynamic>
                  : <dynamic>[]);
          _riskHistory = rData is List
              ? rData
              : (rData is Map && rData['content'] is List
                  ? rData['content'] as List<dynamic>
                  : <dynamic>[]);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement de l\'arbre';
          _isLoading = false;
        });
      }
    }
  }

  Color _riskColor(String? level) {
    switch (level?.toUpperCase()) {
      case 'CRITIQUE':
        return Colors.red;
      case 'ELEVE':
        return Colors.orange;
      case 'MOYEN':
        return Colors.amber;
      case 'FAIBLE':
        return Colors.green;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Arbre généalogique'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Arbre'),
            Tab(text: 'Historique'),
            Tab(text: 'Risques'),
          ],
        ),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : TabBarView(
                  controller: _tabController,
                  children: [
                    _buildTreeTab(),
                    _buildHistoryTab(),
                    _buildRiskTab(),
                  ],
                ),
    );
  }

  Widget _buildTreeTab() {
    if (_tree.isEmpty) {
      return _buildEmpty('Aucun membre dans l\'arbre');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _tree.length,
        itemBuilder: (context, i) {
          final member = _tree[i] as Map<String, dynamic>;
          final nom = member['nom']?.toString() ?? member['name']?.toString() ?? 'Membre';
          final role = member['role']?.toString() ?? '';
          final niveau = member['niveau'] as int? ?? i;

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Center(
                    child: Text(
                      '${niveau + 1}',
                      style: TextStyle(color: AppColors.primary, fontWeight: FontWeight.bold, fontSize: 14),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(nom,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                      if (role.isNotEmpty)
                        Text(role,
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                    ],
                  ),
                ),
                Icon(Icons.account_tree, color: Colors.white.withValues(alpha: 0.2), size: 20),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildHistoryTab() {
    if (_history.isEmpty) {
      return _buildEmpty('Aucun historique');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _history.length,
        itemBuilder: (context, i) {
          final h = _history[i] as Map<String, dynamic>;
          final event = h['event']?.toString() ?? h['description']?.toString() ?? '';
          final dateStr = h['date']?.toString().substring(0, 10) ?? h['createdAt']?.toString().substring(0, 10) ?? '';

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Icon(Icons.history, color: Colors.blue, size: 18),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(event,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 13)),
                      if (dateStr.isNotEmpty)
                        Text(dateStr,
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                    ],
                  ),
                ),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildRiskTab() {
    if (_riskHistory.isEmpty) {
      return _buildEmpty('Aucun historique de risques');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _riskHistory.length,
        itemBuilder: (context, i) {
          final r = _riskHistory[i] as Map<String, dynamic>;
          final risque = r['risque']?.toString() ?? r['description']?.toString() ?? '';
          final niveau = r['niveau']?.toString() ?? 'MOYEN';
          final dateStr = r['date']?.toString().substring(0, 10) ?? r['createdAt']?.toString().substring(0, 10) ?? '';

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Icon(Icons.warning_amber, color: _riskColor(niveau), size: 20),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(risque,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 13)),
                      if (dateStr.isNotEmpty)
                        Text(dateStr,
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                    ],
                  ),
                ),
                StatusBadge(label: niveau, color: _riskColor(niveau)),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildEmpty(String message) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.account_tree, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text(message, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
