import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #35 — Matching membres ↔ compétences — branché sur API réelle.
class SkillMatchingScreen extends StatefulWidget {
  const SkillMatchingScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<SkillMatchingScreen> createState() => _SkillMatchingScreenState();
}

class _SkillMatchingScreenState extends State<SkillMatchingScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _matches = [];
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
      final res = await _api.get('/skill-matching');
      if (mounted) {
        final data = res.data;
        setState(() {
          _matches = (data is Map && data['content'] is List)
              ? data['content'] as List<dynamic>
              : (data is List ? data : []);
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

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.skillMatchingTitle),
        backgroundColor: Colors.amber.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await _load(); // Refresh matches
        },
        backgroundColor: Colors.amber.shade700,
        child: const Icon(Icons.auto_awesome, color: Colors.white),
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError(l10n)
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // AI Match button
                      GlassCard(
                        padding: const EdgeInsets.all(12),
                        child: ListTile(
                          leading: const Icon(Icons.psychology, color: Colors.amber),
                          title: Text(l10n.launchAiMatching),
                          subtitle: Text(l10n.matchingSubtitle),
                          trailing: const Icon(Icons.play_arrow),
                          onTap: _load,
                        ),
                      ),
                      const SizedBox(height: 16),
                      // Matches
                      Text(l10n.proposals, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (_matches.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.noMatches, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._matches.map((m) {
                          final match = m as Map<String, dynamic>;
                          final name = match['membreNom'] ?? match['nom'] ?? '';
                          final dept = match['departement'] ?? '';
                          final skill = match['competence'] ?? '';
                          final score = match['score'] ?? 0;
                          final scoreColor = (score as num) >= 80 ? Colors.green : Colors.orange;
                          return Card(child: ListTile(
                            leading: CircleAvatar(
                              backgroundColor: scoreColor.withValues(alpha: 0.1),
                              child: Text('$score', style: TextStyle(color: scoreColor, fontWeight: FontWeight.bold, fontSize: 12)),
                            ),
                            title: Text(name),
                            subtitle: Text('$dept — $skill'),
                            trailing: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                IconButton(icon: const Icon(Icons.check_circle, color: Colors.green), onPressed: () {}),
                                IconButton(icon: const Icon(Icons.cancel, color: Colors.red), onPressed: () {}),
                              ],
                            ),
                          ));
                        }),
                    ],
                  ),
                ),
    );
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
