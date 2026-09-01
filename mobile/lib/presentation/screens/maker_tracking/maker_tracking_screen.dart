import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #39 — Suivi de développement faiseur — branché sur API réelle.
class MakerTrackingScreen extends StatefulWidget {
  const MakerTrackingScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<MakerTrackingScreen> createState() => _MakerTrackingScreenState();
}

class _MakerTrackingScreenState extends State<MakerTrackingScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _resume;
  List<dynamic> _timeline = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final userId = AuthState().userId;
      final results = await Future.wait([
        _api.get('/maker-tracking/resume/$userId'),
        _api.get('/maker-tracking', params: {'faiseurId': userId, 'size': '20'}),
      ]);
      if (mounted) {
        final resumeData = results[0].data;
        final timelineData = results[1].data;
        setState(() {
          _resume = resumeData is Map<String, dynamic> ? resumeData : null;
          _timeline = (timelineData is Map && timelineData['content'] is List)
              ? timelineData['content'] as List<dynamic>
              : (timelineData is List ? timelineData : []);
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final formations = _resume?['formations'] ?? 0;
    final competences = _resume?['competences'] ?? 0;
    final ames = _resume?['ames'] ?? 0;
    final points = _resume?['points'] ?? 0;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.makerTrackingTitle),
        backgroundColor: Colors.green.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : _error != null
              ? _buildError(l10n)
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // Summary cards
                      Row(children: [
                        _summaryCard('$formations', Icons.school, Colors.blue),
                        const SizedBox(width: 8),
                        _summaryCard('$competences', Icons.psychology, Colors.purple),
                        const SizedBox(width: 8),
                        _summaryCard('$ames', Icons.people, Colors.green),
                      ]),
                      const SizedBox(height: 16),
                      // Points
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        child: Row(children: [
                          const Icon(Icons.star, color: Colors.amber, size: 32),
                          const SizedBox(width: 8),
                          Text('$points ${l10n.points}',
                              style: TextStyle(fontSize: 24, color: Colors.green.shade700, fontWeight: FontWeight.bold)),
                        ]),
                      ),
                      const SizedBox(height: 16),
                      // Timeline
                      Text(l10n.timeline, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (_timeline.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.noTimeline, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._timeline.map((item) {
                          final t = item as Map<String, dynamic>;
                          final type = t['type'] ?? '';
                          final title = t['titre'] ?? t['description'] ?? '';
                          final date = t['createdAt']?.toString().split('T').first ?? '';
                          final color = _typeColor(type.toString());
                          final icon = _typeIcon(type.toString());
                          return ListTile(
                            leading: CircleAvatar(
                              backgroundColor: color.withValues(alpha: 0.1),
                              child: Icon(icon, color: color, size: 18),
                            ),
                            title: Text(title, style: const TextStyle(fontSize: 14)),
                            subtitle: Text(date, style: const TextStyle(fontSize: 12)),
                          );
                        }),
                    ],
                  ),
                ),
    );
  }

  Widget _summaryCard(String count, IconData icon, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(12),
        child: Column(children: [
          Icon(icon, color: color, size: 28),
          const SizedBox(height: 4),
          Text(count, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
        ]),
      ),
    );
  }

  Color _typeColor(String type) {
    switch (type) {
      case 'FORMATION': return Colors.blue;
      case 'COMPETENCE': return Colors.purple;
      case 'ACCOMPAGNEMENT': return Colors.green;
      case 'DEFI': return Colors.orange;
      case 'CERTIFICAT': return Colors.amber;
      default: return Colors.blue;
    }
  }

  IconData _typeIcon(String type) {
    switch (type) {
      case 'FORMATION': return Icons.school;
      case 'COMPETENCE': return Icons.psychology;
      case 'ACCOMPAGNEMENT': return Icons.people;
      case 'DEFI': return Icons.emoji_events;
      case 'CERTIFICAT': return Icons.card_membership;
      default: return Icons.circle;
    }
  }

  Widget _buildError(AppLocalizations l10n) {
    return Center(child: Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
        const SizedBox(height: 12),
        Text(l10n.error, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
        const SizedBox(height: 12),
        FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh, size: 16), label: Text(l10n.retry)),
      ],
    ));
  }
}
