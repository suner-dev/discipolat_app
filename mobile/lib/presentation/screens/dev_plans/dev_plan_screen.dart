import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #37 — Plan de développement individuel — branché sur API réelle.
class DevelopmentPlanScreen extends StatefulWidget {
  const DevelopmentPlanScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<DevelopmentPlanScreen> createState() => _DevelopmentPlanScreenState();
}

class _DevelopmentPlanScreenState extends State<DevelopmentPlanScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _plans = [];
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
      final res = await _api.get('/api/v1/development-plans/by-member/$userId');
      if (mounted) {
        setState(() {
          _plans = (res.data is List ? res.data : []) as List<dynamic>;
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
    final activePlans = _plans.where((p) => (p as Map<String, dynamic>)['statut'] != 'TERMINE').toList();
    final completedPlans = _plans.where((p) => (p as Map<String, dynamic>)['statut'] == 'TERMINE').toList();
    final progress = _plans.isNotEmpty ? activePlans.length / _plans.length : 0.0;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.devPlanTitle),
        backgroundColor: Colors.deepOrange.shade600,
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
                      // Progress overview
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(l10n.globalProgress, style: const TextStyle(fontWeight: FontWeight.bold)),
                          const SizedBox(height: 8),
                          LinearProgressIndicator(value: progress.clamp(0.0, 1.0)),
                          const SizedBox(height: 4),
                          Text('${(progress * 100).round()}% — ${activePlans.length} ${l10n.activeObjectives} / ${_plans.length}'),
                        ]),
                      ),
                      const SizedBox(height: 16),
                      if (activePlans.isNotEmpty) ...[
                        Text(l10n.activeObjectives, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                        ...activePlans.map((p) => _planItem(p as Map<String, dynamic>, l10n)),
                      ],
                      if (completedPlans.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        Text(l10n.completedObjectives, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                        ...completedPlans.map((p) => _planItem(p as Map<String, dynamic>, l10n)),
                      ],
                      if (_plans.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.noPlans, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        ),
                    ],
                  ),
                ),
    );
  }

  Widget _planItem(Map<String, dynamic> plan, AppLocalizations l10n) {
    final progress = (plan['progression'] ?? 0) / 100.0;
    final priorite = plan['priorite'] ?? 'MOYENNE';
    final color = priorite == 'HAUTE' ? Colors.red : priorite == 'MOYENNE' ? Colors.orange : Colors.blue;
    final isCompleted = plan['statut'] == 'TERMINE';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            Expanded(child: Text(plan['titre'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold))),
            Chip(label: Text(priorite, style: const TextStyle(fontSize: 11)), backgroundColor: color.withValues(alpha: 0.1)),
          ]),
          const SizedBox(height: 8),
          LinearProgressIndicator(value: progress.clamp(0.0, 1.0), backgroundColor: Colors.grey.shade200, color: isCompleted ? Colors.green : color),
          const SizedBox(height: 4),
          Text('${(progress * 100).round()}%', style: TextStyle(color: Colors.grey.shade600)),
        ]),
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
