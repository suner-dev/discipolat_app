import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../widgets/secure_screen.dart';

/// StreakScreen — affiche les statistiques de streak (séries) de l'utilisateur
/// depuis l'endpoint GET /api/v1/spiritual-challenges/my-gamification
class StreakScreen extends ConsumerStatefulWidget {
  const StreakScreen({super.key});

  @override
  ConsumerState<StreakScreen> createState() => _StreakScreenState();
}

class _StreakScreenState extends ConsumerState<StreakScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _gamification;
  bool _isLoading = true;
  String? _error;
  int _currentNavIndex = 0;

  @override
  void initState() {
    super.initState();
    _loadGamification();
  }

  Future<void> _loadGamification() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final userId = await _getCurrentUserId();
      if (userId == null) throw Exception('User not authenticated');
      
      final res = await _apiService.get(
        '/spiritual-challenges/my-gamification',
        params: {'id': userId},
      );
      if (mounted) {
        setState(() {
          _gamification = res.data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = AppLocalizations.of(context).streakError;
          _isLoading = false;
        });
      }
    }
  }

  Future<String?> _getCurrentUserId() async {
    try {
      final me = await _apiService.get('/users/me');
      return (me.data as Map<String, dynamic>?)?['id'] as String?;
    } catch (_) {
      return null;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final streak = _gamification?['streak'] as int? ?? 0;
    final longestStreak = _gamification?['longestStreak'] as int? ?? 0;
    final totalCompleted = _gamification?['totalCompleted'] as int? ?? 0;
    final currentChallenge = _gamification?['currentChallenge'] as Map<String, dynamic>?;
    final recentActivity = _gamification?['recentActivity'] as List<dynamic>? ?? [];

    return SecureScreen(
      screenName: 'StreakScreen',
      auditAction: 'VIEW_STREAK',
      child: Scaffold(
        appBar: AppBar(
          title: Text(l10n.streakTitle),
          backgroundColor: Colors.redAccent,
          foregroundColor: Colors.white,
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              onPressed: _loadGamification,
              tooltip: l10n.refresh,
            ),
          ],
        ),
        drawer: const AppDrawer(),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? _buildErrorState()
                : RefreshIndicator(
                    onRefresh: _loadGamification,
                    child: SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _buildStreakHeader(streak, longestStreak, totalCompleted),
                          const SizedBox(height: 24),
                          if (currentChallenge != null) ...[
                            _buildCurrentChallenge(currentChallenge),
                            const SizedBox(height: 24),
                          ],
                          _buildRecentActivity(recentActivity),
                        ],
                      ),
                    ),
                  ),
        bottomNavigationBar: _buildBottomNav(),
      ),
    );
  }

  Widget _buildErrorState() {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: Colors.red, size: 48),
            const SizedBox(height: 16),
            Text(_error!, textAlign: TextAlign.center, style: const TextStyle(fontSize: 16)),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _loadGamification,
              icon: const Icon(Icons.refresh),
              label: Text(l10n.retry),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStreakHeader(int streak, int longestStreak, int totalCompleted) {
    return Column(
      children: [
        GlassCard(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              Text(
                AppLocalizations.of(context).currentStreak,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.6),
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '$streak',
                style: TextStyle(
                  color: streak > 0 ? Colors.orangeAccent : Colors.white54,
                  fontSize: 72,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                streak == 1
                    ? AppLocalizations.of(context).streakDay
                    : AppLocalizations.of(context).streakDays,
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.5),
                  fontSize: 16,
                ),
              ),
              if (streak > 0) ...[
                const SizedBox(height: 16),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: Colors.orangeAccent.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    AppLocalizations.of(context).keepItUp,
                    style: const TextStyle(
                      color: Colors.orangeAccent,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: _StatCard(
                icon: Icons.local_fire_department,
                label: l10n.longestStreak,
                value: '$longestStreak',
                color: Colors.redAccent,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _StatCard(
                icon: Icons.check_circle_outline,
                label: l10n.totalCompleted,
                value: '$totalCompleted',
                color: Colors.greenAccent,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

  Widget _buildCurrentChallenge(Map<String, dynamic> challenge) {
    final l10n = AppLocalizations.of(context);
    final titre = challenge['titre']?.toString() ?? l10n.challenge;
    final description = challenge['description']?.toString() ?? '';
    final objectifJours = (challenge['objectifJours'] as num?)?.toInt() ?? 7;
    final joursCompletes = (challenge['joursCompletes'] as num?)?.toInt() ?? 0;
    final progress = objectifJours > 0 ? joursCompletes / objectifJours : 0.0;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.currentChallenge,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        GlassCard(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                titre,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
              if (description.isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  description,
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.7),
                    fontSize: 13,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ],
              const SizedBox(height: 16),
              LinearProgressIndicator(
                value: progress.clamp(0.0, 1.0),
                minHeight: 8,
                borderRadius: BorderRadius.circular(4),
                backgroundColor: Colors.white12,
                valueColor: const AlwaysStoppedAnimation<Color>(Colors.redAccent),
              ),
              const SizedBox(height: 8),
              Text(
                '$joursCompletes / $objectifJours ${l10n.days}',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.5),
                  fontSize: 12,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildRecentActivity(List<dynamic> activities) {
    final l10n = AppLocalizations.of(context);
    
    if (activities.isEmpty) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            l10n.recentActivity,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 12),
          GlassCard(
            child: Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  l10n.noRecentActivity,
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.5)),
                ),
              ),
            ),
          ),
        ],
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.recentActivity,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 12),
        ...activities.map((activity) {
          final a = activity as Map<String, dynamic>;
          final date = a['date']?.toString() ?? '';
          final type = a['type']?.toString() ?? 'activity';
          final title = a['title']?.toString() ?? l10n.activity;
          final completed = a['completed'] as bool? ?? false;

          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: GlassCard(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Container(
                    width: 40,
                    height: 40,
                    decoration: BoxDecoration(
                      color: completed
                          ? Colors.green.withValues(alpha: 0.2)
                          : Colors.orange.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(
                      completed ? Icons.check : Icons.radio_button_unchecked,
                      color: completed ? Colors.greenAccent : Colors.orangeAccent,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w600,
                            fontSize: 14,
                          ),
                        ),
                        Text(
                          '$type • $date',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.5),
                            fontSize: 11,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (completed)
                    const Icon(Icons.check_circle, color: Colors.greenAccent, size: 24),
                ],
              ),
            ),
          );
        }),
      ],
    );
  }

  Widget _buildBottomNav() {
    const routes = ['/dashboard', '/souls', '/reports/maker', '/profile'];
    return GlassBottomNav(
      currentIndex: _currentNavIndex,
      onTap: (i) {
        setState(() => _currentNavIndex = i);
        if (i < routes.length) context.go(routes[i]);
      },
    );
  }
}

class _StatCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  final Color color;

  const _StatCard({
    required this.icon,
    required this.label,
    required this.value,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return GlassCard(
      padding: const EdgeInsets.all(12),
      child: Column(
        children: [
          Icon(icon, color: color, size: 24),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              color: color,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          Text(
            label,
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.5),
              fontSize: 10,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}
