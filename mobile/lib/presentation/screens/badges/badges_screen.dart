import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class BadgesScreen extends StatefulWidget {
  const BadgesScreen({super.key});

  @override
  State<BadgesScreen> createState() => _BadgesScreenState();
}

class _BadgesScreenState extends State<BadgesScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;
  List<dynamic> _myBadges = [];
  List<dynamic> _leaderboard = [];
  bool _isLoading = true;

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
    setState(() => _isLoading = true);
    try {
      final badgeRes = await _apiService.get('/badges/my');
      final lbRes = await _apiService.get('/badges/leaderboard');
      if (mounted) {
        setState(() {
          _myBadges = (badgeRes.data is Map ? badgeRes.data['content'] : badgeRes.data) as List<dynamic>? ?? [];
          _leaderboard = (lbRes.data is Map ? lbRes.data['content'] : lbRes.data) as List<dynamic>? ?? [];
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color _tierColor(String? tier) {
    switch (tier) {
      case 'OR': return Colors.amber;
      case 'ARGENT': return Colors.grey;
      case 'BRONZE': return Colors.brown;
      default: return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Badges'),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: const [
            Tab(text: 'Mes badges'),
            Tab(text: 'Classement'),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 3)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [
                  _buildMyBadges(),
                  _buildLeaderboard(),
                ],
              ),
            ),
    );
  }

  Widget _buildMyBadges() {
    if (_myBadges.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.emoji_events_outlined, size: 48, color: Colors.white.withValues(alpha: 0.3)),
            const SizedBox(height: 12),
            Text('Aucun badge obtenu', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
            const SizedBox(height: 6),
            Text('Continuez à progresser !', style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12)),
          ],
        ),
      );
    }
    return GridView.builder(
      padding: const EdgeInsets.all(12),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3, mainAxisSpacing: 8, crossAxisSpacing: 8,
      ),
      itemCount: _myBadges.length,
      itemBuilder: (context, index) {
        final b = _myBadges[index] as Map<String, dynamic>;
        final nom = b['nom'] ?? b['badgeNom'] ?? 'Badge';
        final tier = b['tier'] ?? b['niveau'] ?? 'BRONZE';
        final dateObtention = b['dateObtention'] ?? b['date'] ?? '';
        return GlassCard(
          padding: const EdgeInsets.all(10),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.emoji_events, color: _tierColor(tier), size: 30),
              const SizedBox(height: 6),
              Text(nom, style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w600), textAlign: TextAlign.center, maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: 2),
              Text(tier, style: TextStyle(color: _tierColor(tier), fontSize: 9, fontWeight: FontWeight.w600)),
            ],
          ),
        );
      },
    );
  }

  Widget _buildLeaderboard() {
    if (_leaderboard.isEmpty) {
      return Center(
        child: Text('Aucun classement disponible', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _leaderboard.length,
      itemBuilder: (context, index) {
        final entry = _leaderboard[index] as Map<String, dynamic>;
        final nom = entry['nom'] ?? entry['userName'] ?? '—';
        final score = entry['score'] ?? entry['points'] ?? 0;
        final rank = entry['rank'] ?? (index + 1);
        final medal = rank == 1 ? '🥇' : rank == 2 ? '🥈' : rank == 3 ? '🥉' : '$rank';
        return GlassCard(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              SizedBox(
                width: 32, height: 32,
                child: Center(child: Text(medal, style: TextStyle(fontSize: rank <= 3 ? 20 : 14, color: Colors.white))),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 14)),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.amber.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text('$score pts', style: const TextStyle(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 13)),
              ),
            ],
          ),
        );
      },
    );
  }
}
