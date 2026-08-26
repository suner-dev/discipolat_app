import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Écran de gamification : badges, scores, progression, classement.
class BadgesScreen extends StatefulWidget {
  const BadgesScreen({super.key});

  @override
  State<BadgesScreen> createState() => _BadgesScreenState();
}

class _BadgesScreenState extends State<BadgesScreen> with SingleTickerProviderStateMixin {
  final _apiService = ApiService();
  late TabController _tabController;

  Map<String, dynamic>? _profile;
  List<dynamic> _leaderboard = [];
  bool _isLoading = true;
  bool _isEvaluating = false;

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
          _profile = badgeRes.data as Map<String, dynamic>?;
          _leaderboard = (lbRes.data is List ? lbRes.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _evaluate() async {
    setState(() => _isEvaluating = true);
    try {
      final res = await _apiService.post('/badges/evaluate');
      final newBadges = (res.data is List ? res.data : []) as List<dynamic>;
      if (mounted) {
        HapticFeedback.heavyImpact();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(newBadges.isNotEmpty
                ? AppLocalizations.of(context).newBadgesEarned(newBadges.length)
                : AppLocalizations.of(context).noNewBadges),
            backgroundColor: newBadges.isNotEmpty ? Colors.green : Colors.grey.shade700,
          ),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(AppLocalizations.of(context).checkBadgesError), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isEvaluating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final badges = (_profile?['badges'] as List<dynamic>?) ?? [];
    final earned = badges.where((b) => b['gagne'] == true).toList();
    final total = badges.length;
    final totalBadges = (_profile?['totalBadges'] as num?)?.toInt() ?? 0;
    final scores = (_profile?['scores'] as Map<String, dynamic>?) ?? {};

    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).gamificationTitle(totalBadges)),
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          tabs: [
            Tab(text: AppLocalizations.of(context).tabMyBadges),
            Tab(text: AppLocalizations.of(context).tabLeaderboard),
          ],
        ),
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isEvaluating ? null : _evaluate,
        icon: _isEvaluating
            ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.stars, size: 16),
        label: Text(AppLocalizations.of(context).checkMyBadges),
        backgroundColor: AppColors.primary,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : TabBarView(
              controller: _tabController,
              children: [
                _buildMyBadgesTab(badges, earned, total, totalBadges, scores),
                _buildLeaderboardTab(),
              ],
            ),
    );
  }

  // ── My Badges Tab ────────────────────────────────────────────────

  Widget _buildMyBadgesTab(List<dynamic> badges, List<dynamic> earned, int total, int totalBadges, Map<String, dynamic> scores) {
    return RefreshIndicator(
      onRefresh: _loadData,
      child: ListView(
        padding: const EdgeInsets.all(12),
        children: [
          // Summary card
          GlassCard(
            padding: const EdgeInsets.all(16),
            child: Column(children: [
              Row(children: [
                // Circular progress
                SizedBox(width: 80, height: 80, child: Stack(alignment: Alignment.center, children: [
                  SizedBox(width: 80, height: 80, child: CircularProgressIndicator(
                    value: total > 0 ? totalBadges / total : 0,
                    strokeWidth: 8, backgroundColor: Colors.white.withValues(alpha: 0.1),
                    valueColor: const AlwaysStoppedAnimation(Colors.amber), strokeCap: StrokeCap.round)),
                  Column(mainAxisSize: MainAxisSize.min, children: [
                    Text('$totalBadges', style: const TextStyle(color: Colors.amber, fontSize: 24, fontWeight: FontWeight.bold)),
                    Text('/$total', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  ]),
                ])),
                const SizedBox(width: 16),
                Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                  Text(AppLocalizations.of(context).progressionLabel, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 16)),
                  Text(AppLocalizations.of(context).percentCompleted(total > 0 ? (totalBadges / total * 100).round() : 0),
                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                  const SizedBox(height: 4),
                  Text(AppLocalizations.of(context).badgesEarned(earned.length),
                      style: const TextStyle(color: Colors.amber, fontSize: 13, fontWeight: FontWeight.w600)),
                ])),
              ]),
              const SizedBox(height: 12),
              // Score breakdown
              if (scores.isNotEmpty) ...[
                Text(AppLocalizations.of(context).scoresPerCriteria, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11, fontWeight: FontWeight.w600)),
                const SizedBox(height: 8),
                ...scores.entries.map((e) => Padding(
                  padding: const EdgeInsets.only(bottom: 6),
                  child: Row(children: [
                    SizedBox(width: 90, child: Text(_criteriaLabel(e.key), style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 11))),
                    Expanded(child: ClipRRect(borderRadius: BorderRadius.circular(3), child: LinearProgressIndicator(
                      value: (e.value as num).toDouble() / 10, minHeight: 6,
                      backgroundColor: Colors.white.withValues(alpha: 0.08),
                      valueColor: const AlwaysStoppedAnimation(Colors.amber)))),
                    const SizedBox(width: 8),
                    Text('${(e.value as num).toInt()}', style: const TextStyle(color: Colors.amber, fontSize: 11, fontWeight: FontWeight.bold)),
                  ]),
                )),
              ],
            ]),
          ),
          const SizedBox(height: 16),

          // Earned badges
          if (earned.isNotEmpty) ...[
            Row(children: [
              const Icon(Icons.emoji_events, color: Colors.amber, size: 18),
              const SizedBox(width: 8),
              Text(AppLocalizations.of(context).earnedBadges(earned.length), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700, fontSize: 14)),
            ]),
            const SizedBox(height: 8),
            ...earned.map((b) => _badgeCard(b, earned: true)),
            const SizedBox(height: 16),
          ],

          // Locked badges
          Row(children: [
            Icon(Icons.lock_outline, color: Colors.white.withValues(alpha: 0.4), size: 18),
            const SizedBox(width: 8),
            Text(AppLocalizations.of(context).toUnlock, style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontWeight: FontWeight.w700, fontSize: 14)),
          ]),
          const SizedBox(height: 8),
          ...badges.where((b) => b['gagne'] != true).map((b) => _badgeCard(b, earned: false)),
          const SizedBox(height: 80),
        ],
      ),
    );
  }

  Widget _badgeCard(Map<String, dynamic> badge, {required bool earned}) {
    final progression = (badge['progression'] as num?)?.toDouble() ?? 0;
    final niveau = badge['niveau']?.toString() ?? '';
    final tierColor = _tierColor(niveau);

    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      borderColor: earned ? Colors.amber.withValues(alpha: 0.3) : null,
      child: Row(children: [
        // Badge icon
        Container(width: 44, height: 44, decoration: BoxDecoration(
          gradient: earned
              ? LinearGradient(colors: [tierColor, tierColor.withValues(alpha: 0.7)])
              : LinearGradient(colors: [Colors.white.withValues(alpha: 0.08), Colors.white.withValues(alpha: 0.04)]),
          borderRadius: BorderRadius.circular(10)),
          child: Center(child: Text(earned ? '🏅' : '🔒', style: const TextStyle(fontSize: 20))),
        ),
        const SizedBox(width: 12),
        Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Flexible(child: Text(badge['nom']?.toString() ?? '', style: TextStyle(
              color: earned ? Colors.white : Colors.white.withValues(alpha: 0.6),
              fontWeight: FontWeight.w600, fontSize: 13), overflow: TextOverflow.ellipsis)),
            if (niveau.isNotEmpty) ...[
              const SizedBox(width: 6),
              Container(padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(color: tierColor.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(6)),
                child: Text(niveau, style: TextStyle(color: tierColor, fontSize: 8, fontWeight: FontWeight.bold))),
            ],
          ]),
          const SizedBox(height: 4),
          Text(badge['description']?.toString() ?? '', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 10), maxLines: 2, overflow: TextOverflow.ellipsis),
          const SizedBox(height: 6),
          // Progress bar
          Row(children: [
            Expanded(child: ClipRRect(borderRadius: BorderRadius.circular(3), child: LinearProgressIndicator(
              value: progression / 100, minHeight: 5,
              backgroundColor: Colors.white.withValues(alpha: 0.08),
              valueColor: AlwaysStoppedAnimation(earned ? Colors.amber : Colors.white.withValues(alpha: 0.3))))),
            const SizedBox(width: 8),
            Text('${progression.round()}%', style: TextStyle(color: earned ? Colors.amber : Colors.white.withValues(alpha: 0.4), fontSize: 10, fontWeight: FontWeight.bold)),
          ]),
        ])),
      ]),
    );
  }

  // ── Leaderboard Tab ──────────────────────────────────────────────

  Widget _buildLeaderboardTab() {
    if (_leaderboard.isEmpty) {
      return Center(child: Column(mainAxisAlignment: MainAxisAlignment.center, children: [
        Icon(Icons.leaderboard, color: Colors.white.withValues(alpha: 0.15), size: 48),
        const SizedBox(height: 12),
        Text(AppLocalizations.of(context).noLeaderboard, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
      ]));
    }

    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: _leaderboard.length,
      itemBuilder: (_, i) {
        final entry = _leaderboard[i] as Map<String, dynamic>;
        final nom = entry['nom']?.toString() ?? '';
        final badges = (entry['badges'] as num?)?.toInt() ?? 0;
        final rank = i + 1;
        final rankColor = rank == 1 ? Colors.amber : rank == 2 ? Colors.grey : rank == 3 ? Colors.brown : Colors.white.withValues(alpha: 0.5);

        return GlassCard(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.all(12),
          child: Row(children: [
            // Rank
            Container(width: 32, height: 32, decoration: BoxDecoration(
              color: rankColor.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
              child: Center(child: rank <= 3
                  ? Text(['🥇', '🥈', '🥉'][rank - 1], style: const TextStyle(fontSize: 16))
                  : Text('$rank', style: TextStyle(color: rankColor, fontWeight: FontWeight.bold, fontSize: 12)))),
            const SizedBox(width: 12),
            // Name
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(nom, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 13)),
              Text('$badges ${AppLocalizations.of(context).badgesUnit}', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
            ])),
            // Score
            Container(padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: Colors.amber.withValues(alpha: 0.15), borderRadius: BorderRadius.circular(8)),
              child: Text('$badges', style: const TextStyle(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 14))),
          ]),
        );
      },
    );
  }

  // ── Helpers ──────────────────────────────────────────────────────

  Color _tierColor(String niveau) {
    switch (niveau.toUpperCase()) {
      case 'OR': return Colors.amber;
      case 'ARGENT': return Colors.grey.shade400;
      case 'BRONZE': return Colors.brown.shade300;
      default: return Colors.blue;
    }
  }

  String _criteriaLabel(String key) {
    final l10n = AppLocalizations.of(context);
    switch (key) {
      case 'VISITES': return l10n.criteriaVisits;
      case 'INTERACTIONS': return l10n.criteriaInteractions;
      case 'EVANGELISATION': return l10n.criteriaEvangelism;
      case 'PRESENCE': return l10n.criteriaAttendance;
      case 'FIDELITE': return l10n.criteriaLoyalty;
      default: return key;
    }
  }
}
