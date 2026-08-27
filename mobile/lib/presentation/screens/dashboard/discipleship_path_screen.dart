import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Parcours de discipolat — branché sur API réelle.
class DiscipleshipPathScreen extends StatefulWidget {
  const DiscipleshipPathScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<DiscipleshipPathScreen> createState() => _DiscipleshipPathScreenState();
}

class _DiscipleshipPathScreenState extends State<DiscipleshipPathScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _stages = [];
  int _currentStage = 0;
  bool _isLoading = true;
  String? _error;

  static const _stageIcons = {
    'DISCOVERY': '🌱',
    'FOUNDATION': '🏗️',
    'GROWTH': '🌳',
    'SERVICE': '🤝',
    'LEADERSHIP': '👑',
    'MATURITY': '🌟',
  };
  static const _stageColors = {
    'DISCOVERY': Colors.green,
    'FOUNDATION': Colors.blue,
    'GROWTH': Colors.purple,
    'SERVICE': Colors.orange,
    'LEADERSHIP': Colors.amber,
    'MATURITY': Colors.pink,
  };

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final userId = AuthState().userId;
      final res = await _api.get('/api/v1/discipleship-paths/member/$userId');
      if (mounted) {
        final data = res.data;
        if (data is List) {
          _stages = data;
        } else if (data is Map<String, dynamic>) {
          _stages = data['stages'] as List<dynamic>? ?? [];
          _currentStage = data['currentStage'] as int? ?? 0;
        }
        _isLoading = false;
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final progress = _stages.isNotEmpty ? _currentStage / _stages.length : 0.0;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.discipleshipPathTitle),
        backgroundColor: Colors.blue.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : _error != null
              ? _buildError(l10n)
              : Column(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(20),
                      child: Column(children: [
                        Text(l10n.progression, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                        const SizedBox(height: 8),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(4),
                          child: LinearProgressIndicator(
                            value: progress.clamp(0.0, 1.0),
                            backgroundColor: Colors.grey.shade200,
                            color: Colors.blue,
                            minHeight: 10,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text('${(progress * 100).round()}%', style: const TextStyle(color: Colors.blue, fontWeight: FontWeight.bold)),
                      ]),
                    ),
                    Expanded(
                      child: RefreshIndicator(
                        onRefresh: _load,
                        child: ListView.builder(
                          padding: const EdgeInsets.symmetric(horizontal: 16),
                          itemCount: _stages.length,
                          itemBuilder: (context, i) {
                            final s = _stages[i] as Map<String, dynamic>;
                            final key = s['key'] ?? s['cle'] ?? '';
                            final label = s['label'] ?? s['titre'] ?? key;
                            final desc = s['description'] ?? '';
                            final isCompleted = i < _currentStage;
                            final isCurrent = i == _currentStage;
                            final color = _stageColors[key] ?? Colors.blue;
                            final icon = _stageIcons[key] ?? '📌';
                            return Card(
                              margin: const EdgeInsets.only(bottom: 8),
                              color: isCurrent ? Colors.blue.shade50 : Colors.white,
                              child: ListTile(
                                leading: CircleAvatar(
                                  backgroundColor: color.withValues(alpha: isCurrent ? 1.0 : 0.3),
                                  child: Text(icon, style: const TextStyle(fontSize: 20)),
                                ),
                                title: Text(label, style: TextStyle(fontWeight: FontWeight.bold, color: isCurrent ? Colors.blue : Colors.black)),
                                subtitle: Text(desc),
                                trailing: isCompleted
                                    ? const Icon(Icons.check_circle, color: Colors.green)
                                    : isCurrent
                                        ? Container(
                                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                            decoration: BoxDecoration(color: Colors.blue, borderRadius: BorderRadius.circular(12)),
                                            child: Text(l10n.inProgress, style: const TextStyle(color: Colors.white, fontSize: 11)),
                                          )
                                        : const Icon(Icons.lock, color: Colors.grey),
                              ),
                            );
                          },
                        ),
                      ),
                    ),
                  ],
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
