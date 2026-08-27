import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #36 — Matrice de compétences — branché sur API réelle.
class SkillsMatrixScreen extends StatefulWidget {
  const SkillsMatrixScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<SkillsMatrixScreen> createState() => _SkillsMatrixScreenState();
}

class _SkillsMatrixScreenState extends State<SkillsMatrixScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _skills = [];
  List<dynamic> _gaps = [];
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
      final results = await Future.wait([
        _api.get('/api/v1/skills/matrix'),
        _api.get('/api/v1/skills', params: {'size': '50'}),
      ]);
      if (mounted) {
        final matrixData = results[0].data;
        final skillsData = results[1].data;
        setState(() {
          _skills = (matrixData is List ? matrixData : (matrixData is Map ? [matrixData] : []));
          final sd = skillsData;
          _gaps = (sd is Map && sd['content'] is List) ? sd['content'] as List<dynamic> : (sd is List ? sd : []);
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
    final totalSkills = _skills.length;
    final totalMembers = _gaps.length;
    final gapsCount = _skills.where((s) {
      final m = s as Map<String, dynamic>;
      final available = (m['available'] ?? m['disponibles'] ?? 0) as num;
      final needed = (m['needed'] ?? m['requis'] ?? 1) as num;
      return available < needed;
    }).length;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.skillsMatrixTitle),
        backgroundColor: Colors.purple.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
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
                      // Overview
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(l10n.overview, style: const TextStyle(fontWeight: FontWeight.bold)),
                          const SizedBox(height: 8),
                          Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
                            _skillStat('$totalSkills', Colors.purple),
                            _skillStat('$totalMembers', Colors.blue),
                            _skillStat('$gapsCount', Colors.orange),
                          ]),
                          Row(mainAxisAlignment: MainAxisAlignment.spaceAround, children: [
                            Text(l10n.skills, style: const TextStyle(fontSize: 12)),
                            Text(l10n.evaluatedMembers, style: const TextStyle(fontSize: 12)),
                            Text(l10n.gapsFound, style: const TextStyle(fontSize: 12)),
                          ]),
                        ]),
                      ),
                      const SizedBox(height: 16),
                      // Skills by department
                      Text(l10n.skillsByDepartment, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      ..._skills.map((s) {
                        final skill = s as Map<String, dynamic>;
                        final name = skill['nom'] ?? skill['name'] ?? skill['label'] ?? '';
                        final available = (skill['available'] ?? skill['disponibles'] ?? 0) as num;
                        final needed = (skill['needed'] ?? skill['requis'] ?? 1) as num;
                        final ratio = needed > 0 ? available / needed : 1.0;
                        final color = ratio >= 0.8 ? Colors.green : ratio >= 0.5 ? Colors.orange : Colors.red;
                        return Card(child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Row(children: [
                            Expanded(flex: 2, child: Text(name, style: const TextStyle(fontWeight: FontWeight.bold))),
                            Expanded(flex: 3, child: LinearProgressIndicator(value: ratio.clamp(0.0, 1.0), color: color)),
                            const SizedBox(width: 8),
                            Text('$available/$needed', style: TextStyle(color: color, fontWeight: FontWeight.bold)),
                          ]),
                        ));
                      }),
                      // Gaps
                      if (gapsCount > 0) ...[
                        const SizedBox(height: 16),
                        Text('🔍 ${l10n.gapsFound}', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                        ..._skills.where((s) {
                          final m = s as Map<String, dynamic>;
                          final available = (m['available'] ?? m['disponibles'] ?? 0) as num;
                          final needed = (m['needed'] ?? m['requis'] ?? 1) as num;
                          return available < needed;
                        }).map((s) {
                          final skill = s as Map<String, dynamic>;
                          final name = skill['nom'] ?? skill['name'] ?? '';
                          return ListTile(
                            leading: Icon(Icons.warning, color: Colors.orange, size: 18),
                            title: Text('$name — ${l10n.needsMoreMembers}', style: const TextStyle(fontSize: 13)),
                            dense: true,
                          );
                        }),
                      ],
                    ],
                  ),
                ),
    );
  }

  Widget _skillStat(String count, Color color) {
    return Text(count, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color));
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
