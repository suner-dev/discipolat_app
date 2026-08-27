import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #42 — Indicateur de cohésion familiale — branché sur API réelle.
class FamilyCohesionScreen extends StatefulWidget {
  const FamilyCohesionScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<FamilyCohesionScreen> createState() => _FamilyCohesionScreenState();
}

class _FamilyCohesionScreenState extends State<FamilyCohesionScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _cohesion;
  List<dynamic> _families = [];
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
      // Try fetching cohesion data; family ID may come from user context
      final results = await Future.wait([
        _api.get('/api/v1/family-cohesion', params: {'userId': userId}),
        _api.get('/api/v1/families', params: {'size': '50'}),
      ]);
      if (mounted) {
        final cohesionData = results[0].data;
        setState(() {
          _cohesion = cohesionData is Map<String, dynamic> ? cohesionData : null;
          final familyData = results[1].data;
          _families = (familyData is Map && familyData['content'] is List)
              ? familyData['content'] as List<dynamic>
              : (cohesionData is List ? cohesionData : []);
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
    final score = (_cohesion?['score'] as num?)?.toDouble() ?? 0.0;
    final indicators = (_cohesion?['indicators'] as Map<String, dynamic>?) ?? {};

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.familyCohesionTitle),
        backgroundColor: Colors.pink.shade600,
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
                      // Score card
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(l10n.cohesionScore, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                          const SizedBox(height: 12),
                          Center(child: Text('${score.toStringAsFixed(1)}/10',
                              style: TextStyle(fontSize: 36, fontWeight: FontWeight.bold, color: Colors.pink.shade600))),
                          Center(child: Text(score >= 7 ? l10n.goodEffort : l10n.needsImprovement)),
                          const SizedBox(height: 12),
                          ...indicators.entries.map((e) => _indicatorBar(e.key, (e.value as num).toDouble() / 100.0,
                              (e.value as num).toDouble() >= 0.7 ? Colors.green : Colors.orange)),
                        ]),
                      ),
                      const SizedBox(height: 16),
                      Text(l10n.networkFamilies, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      if (_families.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.noFamilies, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._families.map((f) {
                          final family = f as Map<String, dynamic>;
                          final name = family['nom'] ?? family['name'] ?? '';
                          final familyScore = (family['cohesionScore'] as num?)?.toDouble() ?? 0.0;
                          return Card(child: ListTile(
                            title: Text(name, style: const TextStyle(fontWeight: FontWeight.bold)),
                            trailing: Text('${familyScore.toStringAsFixed(1)}/10',
                                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold,
                                    color: familyScore >= 7 ? Colors.green : Colors.orange)),
                          ));
                        }),
                    ],
                  ),
                ),
    );
  }

  Widget _indicatorBar(String label, double value, Color color) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
          Text(label, style: const TextStyle(fontSize: 12)),
          Text('${(value * 100).round()}%', style: TextStyle(fontSize: 12, color: color)),
        ]),
        LinearProgressIndicator(value: value.clamp(0.0, 1.0), color: color),
      ]),
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
