import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Écran Quest — gamification, classement, défis — branché sur /api/v1/quest.
class QuestScreen extends StatefulWidget {
  const QuestScreen({super.key});

  @override
  State<QuestScreen> createState() => _QuestScreenState();
}

class _QuestScreenState extends State<QuestScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _leaderboard = [];
  Map<String, dynamic>? _stats;
  List<dynamic> _weeklyChallenges = [];
  List<dynamic> _contextualBadges = [];
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
        _apiService.get('/quest/leaderboard/groups'),
        _apiService.get('/quest/stats'),
        _apiService.get('/quest/weekly-challenges'),
        _apiService.get('/quest/contextual-badges'),
      ]);
      if (mounted) {
        final lbData = results[0].data;
        final stData = results[1].data;
        final wcData = results[2].data;
        final cbData = results[3].data;
        setState(() {
          _leaderboard = lbData is List
              ? lbData
              : (lbData is Map && lbData['content'] is List
                  ? lbData['content'] as List<dynamic>
                  : <dynamic>[]);
          _stats = stData is Map<String, dynamic> ? stData : null;
          _weeklyChallenges = wcData is List
              ? wcData
              : (wcData is Map && wcData['content'] is List
                  ? wcData['content'] as List<dynamic>
                  : <dynamic>[]);
          _contextualBadges = cbData is List
              ? cbData
              : (cbData is Map && cbData['content'] is List
                  ? cbData['content'] as List<dynamic>
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Quête'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Classement'),
            Tab(text: 'Défis'),
            Tab(text: 'Badges'),
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
                    _buildLeaderboardTab(),
                    _buildChallengesTab(),
                    _buildBadgesTab(),
                  ],
                ),
    );
  }

  Widget _buildLeaderboardTab() {
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_stats != null) ...[
            GlassCard(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Mes stats', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      _statItem('Points', '${_stats!['points'] ?? 0}', Colors.amber),
                      const SizedBox(width: 12),
                      _statItem('Niveau', '${_stats!['level'] ?? 1}', Colors.blue),
                      const SizedBox(width: 12),
                      _statItem('Rang', '${_stats!['rank'] ?? '-'}', Colors.green),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],
          const Text('Classement par groupe',
              style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 8),
          if (_leaderboard.isEmpty)
            _buildEmpty('Aucun classement disponible')
          else
            ..._leaderboard.asMap().entries.map((entry) {
              final i = entry.key;
              final group = entry.value as Map<String, dynamic>;
              final nom = group['nom']?.toString() ?? group['name']?.toString() ?? 'Groupe';
              final points = group['points']?.toString() ?? '0';
              final rank = i + 1;
              final rankColor = rank == 1 ? Colors.amber : rank == 2 ? Colors.grey : rank == 3 ? Colors.brown : Colors.white;

              return GlassCard(
                margin: const EdgeInsets.only(bottom: 8),
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    Container(
                      width: 32, height: 32,
                      decoration: BoxDecoration(
                        color: rankColor.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Center(
                        child: rank <= 3
                            ? Text(['🥇', '🥈', '🥉'][rank - 1], style: const TextStyle(fontSize: 16))
                            : Text('$rank', style: TextStyle(color: rankColor, fontWeight: FontWeight.bold, fontSize: 12)),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
                          Text('Points : $points',
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(color: Colors.amber.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
                      child: Text(points, style: const TextStyle(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 14)),
                    ),
                  ],
                ),
              );
            }),
        ],
      ),
    );
  }

  Widget _buildChallengesTab() {
    if (_weeklyChallenges.isEmpty) {
      return _buildEmpty('Aucun défi hebdomadaire');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _weeklyChallenges.length,
        itemBuilder: (context, i) {
          final ch = _weeklyChallenges[i] as Map<String, dynamic>;
          final titre = ch['titre']?.toString() ?? ch['nom']?.toString() ?? 'Défi';
          final description = ch['description']?.toString() ?? '';
          final points = ch['points']?.toString() ?? '';
          final progression = (ch['progression'] as num?)?.toDouble() ?? 0;

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.emoji_events, color: Colors.amber, size: 20),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(titre,
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                    ),
                    if (points.isNotEmpty)
                      StatusBadge(label: '$points pts', color: Colors.amber),
                  ],
                ),
                if (description.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  Text(description,
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
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
                          valueColor: const AlwaysStoppedAnimation(Colors.amber),
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
        },
      ),
    );
  }

  Widget _buildBadgesTab() {
    if (_contextualBadges.isEmpty) {
      return _buildEmpty('Aucun badge contextuel');
    }
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _contextualBadges.length,
        itemBuilder: (context, i) {
          final badge = _contextualBadges[i] as Map<String, dynamic>;
          final nom = badge['nom']?.toString() ?? badge['name']?.toString() ?? 'Badge';
          final description = badge['description']?.toString() ?? '';
          final earned = badge['gagne'] == true || badge['earned'] == true;

          return GlassCard(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(12),
            borderColor: earned ? Colors.amber.withValues(alpha: 0.3) : null,
            child: Row(
              children: [
                Container(
                  width: 44, height: 44,
                  decoration: BoxDecoration(
                    color: earned
                        ? Colors.amber.withValues(alpha: 0.15)
                        : Colors.white.withValues(alpha: 0.05),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Center(child: Text(earned ? '🏅' : '🔒', style: const TextStyle(fontSize: 20))),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(nom,
                          style: TextStyle(
                              color: earned ? Colors.white : Colors.white.withValues(alpha: 0.6),
                              fontWeight: FontWeight.w600, fontSize: 13)),
                      if (description.isNotEmpty)
                        Text(description,
                            style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis),
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

  Widget _buildEmpty(String message) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.extension, size: 48, color: Colors.white.withValues(alpha: 0.2)),
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
