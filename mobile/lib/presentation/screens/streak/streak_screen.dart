import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// StreakScreen — statistiques de série (streak) de l'utilisateur connecté.
/// Endpoint RÉEL : GET /api/v1/spiritual-challenges/my/stats
/// → {completed, totalPoints, streak, level, badges:[String]}.
class StreakScreen extends ConsumerStatefulWidget {
  const StreakScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  ConsumerState<StreakScreen> createState() => _StreakScreenState();
}

class _StreakScreenState extends ConsumerState<StreakScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _stats;
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadStats();
  }

  Future<void> _loadStats() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final res = await _api.get('/spiritual-challenges/my/stats');
      if (!mounted) return;
      setState(() {
        _stats = res.data is Map
            ? Map<String, dynamic>.from(res.data as Map)
            : null;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = AppLocalizations.of(context).streakError;
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.streakTitle),
        backgroundColor: Colors.deepPurple,
        foregroundColor: Colors.white,
        actions: [
          IconButton(onPressed: _loadStats, icon: const Icon(Icons.refresh)),
        ],
      ),
      body: _buildBody(l10n),
    );
  }

  Widget _buildBody(AppLocalizations l10n) {
    if (_isLoading) return const Center(child: CircularProgressIndicator());
    if (_error != null || _stats == null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.all(24),
              child: Text(_error ?? l10n.error, textAlign: TextAlign.center),
            ),
            ElevatedButton(onPressed: _loadStats, child: Text(l10n.retry)),
          ],
        ),
      );
    }

    final streak = _stats!['streak'] as int? ?? 0;
    final points = _stats!['totalPoints'] as int? ?? 0;
    final level = _stats!['level'] as int? ?? 1;
    final completed = _stats!['completed'] as int? ?? 0;
    final badges = (_stats!['badges'] as List? ?? <dynamic>[])
        .map((b) => b.toString())
        .toList();

    return RefreshIndicator(
      onRefresh: _loadStats,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _StreakHero(streak: streak, l10n: l10n),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: _StatCard(
                  icon: Icons.star,
                  color: Colors.amber,
                  label: l10n.points,
                  value: '$points',
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _StatCard(
                  icon: Icons.military_tech,
                  color: Colors.deepPurple,
                  label: l10n.level,
                  value: '$level',
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: _StatCard(
                  icon: Icons.task_alt,
                  color: Colors.green,
                  label: l10n.badgesTitle,
                  value: '$completed',
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          Text(l10n.myBadges,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          if (badges.isEmpty)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Text(l10n.noData, textAlign: TextAlign.center),
            )
          else
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: badges
                  .map((b) => Chip(
                        avatar: const Icon(Icons.emoji_events,
                            size: 18, color: Colors.amber),
                        label: Text(b),
                      ))
                  .toList(),
            ),
        ],
      ),
    );
  }
}

class _StreakHero extends StatelessWidget {
  const _StreakHero({required this.streak, required this.l10n});
  final int streak;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(vertical: 28),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Colors.deepPurple, Colors.indigo],
        ),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        children: [
          const Icon(Icons.local_fire_department, color: Colors.orange, size: 44),
          const SizedBox(height: 8),
          Text(l10n.currentStreak,
              style: const TextStyle(color: Colors.white70, fontSize: 14)),
          const SizedBox(height: 4),
          Text(
            '$streak ${streak == 1 ? l10n.streakDay : l10n.streakDays}',
            style: const TextStyle(
                color: Colors.white, fontSize: 30, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 6),
          Text(l10n.keepItUp,
              style: const TextStyle(color: Colors.white70, fontSize: 13)),
        ],
      ),
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard(
      {required this.icon, required this.color, required this.label, required this.value});
  final IconData icon;
  final Color color;
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16),
        child: Column(
          children: [
            Icon(icon, color: color),
            const SizedBox(height: 6),
            Text(value,
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
      ),
    );
  }
}
