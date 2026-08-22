import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

/// Discipolat Quest — gamification : XP, niveaux, quêtes hebdo, classement.
class QuestScreen extends StatefulWidget {
  const QuestScreen({super.key});

  @override
  State<QuestScreen> createState() => _QuestScreenState();
}

class _QuestScreenState extends State<QuestScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;

  Map<String, dynamic>? _profile;
  List<dynamic> _quests = [];
  List<dynamic> _leaderboard = [];
  bool _isLoading = true;

  static const _levelColors = [
    Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
    Color(0xFF2196F3), Color(0xFF00BCD4), Color(0xFF009688),
    Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFF44336),
  ];

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
      final results = await Future.wait([
        _apiService.get('/quest/profile'),
        _apiService.get('/quest/quests'),
      ]);
      List<dynamic> board = [];
      try {
        final lbRes = await _apiService.get('/quest/leaderboard');
        board = (lbRes.data is List ? lbRes.data : []) as List<dynamic>;
      } catch (_) {}
      if (mounted) {
        setState(() {
          _profile = results[0].data as Map<String, dynamic>?;
          _quests = (results[1].data is List ? results[1].data : []) as List<dynamic>;
          _leaderboard = board;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color get _levelColor {
    final level = ((_profile?['level'] as num?) ?? 1).toInt();
    return _levelColors[(level - 1).clamp(0, _levelColors.length - 1)];
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Discipolat Quest'),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: _levelColor,
          tabs: const [
            Tab(icon: Icon(Icons.flag), text: 'Quêtes'),
            Tab(icon: Icon(Icons.emoji_events), text: 'Classement'),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _loadData,
              child: TabBarView(
                controller: _tabController,
                children: [_buildQuestsTab(), _buildLeaderboardTab()],
              ),
            ),
    );
  }

  Widget _buildQuestsTab() {
    final profile = _profile ?? {};
    final totalXp = (profile['totalXp'] as num?) ?? 0;
    final level = (profile['level'] as num?) ?? 1;
    final xpInLevel = (profile['xpInLevel'] as num?) ?? 0;
    final xpForNextLevel = (profile['xpForNextLevel'] as num?) ?? 500;
    final title = (profile['title'] as String?) ?? 'Graine nouvelle';
    final progress = xpForNextLevel > 0 ? xpInLevel / xpForNextLevel : 0.0;
    final allCompleted = _quests.isNotEmpty && _quests.every((q) => q['completed'] == true);

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      children: [
        // Carte niveau
        GlassCard(
          child: Column(
            children: [
              Row(
                children: [
                  Container(
                    width: 56,
                    height: 56,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      gradient: LinearGradient(colors: [_levelColor, _levelColor.withValues(alpha: 0.6)]),
                      boxShadow: [BoxShadow(color: _levelColor.withValues(alpha: 0.4), blurRadius: 16)],
                    ),
                    child: Center(
                      child: Text('$level',
                          style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(title,
                            style: const TextStyle(color: Colors.white, fontSize: 17, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 4),
                        Text('${totalXp.toInt()} XP au total',
                            style: TextStyle(color: Colors.white70, fontSize: 13)),
                      ],
                    ),
                  ),
                  if (allCompleted)
                    const Icon(Icons.workspace_premium, color: Color(0xFFFFD54F), size: 30),
                ],
              ),
              const SizedBox(height: 14),
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: LinearProgressIndicator(
                  value: progress.clamp(0.0, 1.0),
                  minHeight: 10,
                  backgroundColor: Colors.white12,
                  valueColor: AlwaysStoppedAnimation<Color>(_levelColor),
                ),
              ),
              const SizedBox(height: 6),
              Align(
                alignment: Alignment.centerRight,
                child: Text('$xpInLevel / $xpForNextLevel XP → niveau ${level + 1}',
                    style: const TextStyle(color: Colors.white54, fontSize: 11)),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        // Quêtes hebdo
        ..._quests.map((q) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: GlassCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text((q['label'] as String?) ?? '',
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                        ),
                        Text('+${q['xpReward']} XP',
                            style: TextStyle(color: _levelColor, fontWeight: FontWeight.bold, fontSize: 12)),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Stack(
                      children: [
                        Container(
                          height: 8,
                          decoration: BoxDecoration(
                            color: Colors.white12,
                            borderRadius: BorderRadius.circular(6),
                          ),
                        ),
                        FractionallySizedBox(
                          widthFactor: ((q['progressPercent'] as num?) ?? 0).toDouble().clamp(0.0, 100.0) / 100,
                          child: Container(
                            height: 8,
                            decoration: BoxDecoration(
                              color: q['completed'] == true ? const Color(0xFF4CAF50) : const Color(0xFFFFB300),
                              borderRadius: BorderRadius.circular(6),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text('${q['done']} / ${q['target']}${q['completed'] == true ? ' — terminé ✓' : ''}',
                        style: const TextStyle(color: Colors.white54, fontSize: 12)),
                  ],
                ),
              ),
            )),
        if (_quests.isEmpty)
          const GlassCard(
            child: Center(
              child: Padding(
                padding: EdgeInsets.all(20),
                child: Text('Aucune quête cette semaine.\nAgissez pour lancer votre progression !',
                    textAlign: TextAlign.center, style: TextStyle(color: Colors.white54)),
              ),
            ),
          ),
      ],
    );
  }

  Widget _buildLeaderboardTab() {
    if (_leaderboard.isEmpty) {
      return ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: const [
          GlassCard(
            child: Center(
              child: Padding(
                padding: EdgeInsets.all(20),
                child: Text('Le classement est vide — soyez le premier à gagner de l\'XP !',
                    textAlign: TextAlign.center, style: TextStyle(color: Colors.white54)),
              ),
            ),
          ),
        ],
      );
    }
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
      itemCount: _leaderboard.length,
      itemBuilder: (context, i) {
        final e = _leaderboard[i] as Map<String, dynamic>;
        final rank = ((e['rank'] as num?) ?? i + 1).toInt();
        final medal = {1: '🥇', 2: '🥈', 3: '🥉'}[rank];
        return Padding(
          padding: const EdgeInsets.only(bottom: 10),
          child: GlassCard(
            child: Row(
              children: [
                Text(medal ?? '#$rank', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(e['title'] ?? 'Disciple',
                          style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600)),
                      Text('Niveau ${e['level']}',
                          style: const TextStyle(color: Colors.white54, fontSize: 12)),
                    ],
                  ),
                ),
                Text('${e['totalXp']} XP',
                    style: TextStyle(color: _levelColor, fontWeight: FontWeight.bold)),
              ],
            ),
          ),
        );
      },
    );
  }
}
