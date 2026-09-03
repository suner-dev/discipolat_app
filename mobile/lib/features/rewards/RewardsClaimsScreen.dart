import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Réclamations de récompenses — branché sur /api/v1/rewards.
class RewardsClaimsScreen extends StatefulWidget {
  const RewardsClaimsScreen({super.key});

  @override
  State<RewardsClaimsScreen> createState() => _RewardsClaimsScreenState();
}

class _RewardsClaimsScreenState extends State<RewardsClaimsScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _claims = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
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
      final res = await _apiService.get('/rewards/my-claims');
      if (mounted) {
        final data = res.data;
        setState(() {
          _claims = data is List
              ? data
              : (data is Map && data['content'] is List
                  ? data['content'] as List<dynamic>
                  : <dynamic>[]);
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

  Future<void> _claimReward(String rewardId) async {
    try {
      await _apiService.post('/rewards/claim', data: {'rewardId': rewardId});
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Récompense réclamée'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la réclamation'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Color _statusColor(String? statut) {
    switch (statut?.toUpperCase()) {
      case 'GRANTEE':
      case 'ATTRIBUEE':
        return Colors.green;
      case 'EN_ATTENTE':
      case 'EN ATTENTE':
      case 'PENDING':
        return Colors.amber;
      case 'REJETEE':
      case 'REJETE':
        return Colors.red;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mes réclamations'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Réclamations'),
            Tab(text: 'Disponibles'),
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
                    _buildClaimsTab(),
                    _buildAvailableTab(),
                  ],
                ),
    );
  }

  Widget _buildClaimsTab() {
    if (_claims.isEmpty) {
      return _buildEmpty('Aucune réclamation');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _claims.length,
        itemBuilder: (context, i) {
          final claim = _claims[i] as Map<String, dynamic>;
          final nom = claim['nom']?.toString() ?? claim['rewardName']?.toString() ?? 'Récompense';
          final statut = claim['statut']?.toString() ?? claim['status']?.toString() ?? '';
          final dateStr = claim['createdAt']?.toString().substring(0, 10) ?? '';
          final points = claim['points']?.toString() ?? '';

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(14),
            child: Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: Colors.amber.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(Icons.emoji_events, color: Colors.amber, size: 20),
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
                          if (dateStr.isNotEmpty)
                            Text(dateStr,
                                style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                          if (points.isNotEmpty) ...[
                            Text('  •  $points pts',
                                style: const TextStyle(color: Colors.amber, fontSize: 11, fontWeight: FontWeight.w600)),
                          ],
                        ],
                      ),
                    ],
                  ),
                ),
                if (statut.isNotEmpty)
                  StatusBadge(label: statut, color: _statusColor(statut)),
              ],
            ),
          );
        },
      ),
    );
  }

  Widget _buildAvailableTab() {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        GlassCard(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Réclamer une récompense',
                  style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
              const SizedBox(height: 8),
              Text('Saisissez l\'identifiant de la récompense à réclamer.',
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13)),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: () => _showClaimDialog(),
                  icon: const Icon(Icons.card_giftcard, size: 16),
                  label: const Text('Réclamer'),
                  style: FilledButton.styleFrom(backgroundColor: Colors.amber),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _showClaimDialog() {
    final ctrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF16213A),
        title: const Text('Réclamer une récompense', style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: ctrl,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            hintText: 'ID de la récompense',
            hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
            filled: true,
            fillColor: Colors.white.withValues(alpha: 0.06),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () {
              Navigator.pop(ctx);
              if (ctrl.text.isNotEmpty) _claimReward(ctrl.text);
            },
            child: const Text('Réclamer'),
          ),
        ],
      ),
    );
  }

  Widget _buildEmpty(String message) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.emoji_events, size: 48, color: Colors.white.withValues(alpha: 0.2)),
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
