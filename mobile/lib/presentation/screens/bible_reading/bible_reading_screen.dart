import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #49/#60 — Plan de lecture biblique partagé — branché sur API réelle.
class BibleReadingScreen extends StatefulWidget {
  const BibleReadingScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<BibleReadingScreen> createState() => _BibleReadingScreenState();
}

class _BibleReadingScreenState extends State<BibleReadingScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _stats;
  List<dynamic> _plans = [];
  List<dynamic> _today = [];
  List<dynamic> _familyProgress = [];
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
        _api.get('/api/v1/bible-reading/stats'),
        _api.get('/api/v1/bible-reading/plans'),
        _api.get('/api/v1/bible-reading/today'),
        _api.get('/api/v1/bible-reading/family-progress'),
      ]);
      if (mounted) {
        setState(() {
          _stats = results[0].data as Map<String, dynamic>?;
          _plans = (results[1].data is List ? results[1].data : []) as List<dynamic>;
          _today = (results[2].data is List ? results[2].data : []) as List<dynamic>;
          _familyProgress = (results[3].data is List ? results[3].data : []) as List<dynamic>;
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
    final streak = _stats?['streak'] ?? 0;
    final totalRead = _stats?['totalRead'] ?? 0;
    final totalEntries = _stats?['totalEntries'] ?? 1;
    final progress = totalEntries > 0 ? totalRead / totalEntries : 0.0;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.bibleReadingTitle),
        backgroundColor: Colors.brown.shade700,
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
                      // Progress card
                      GlassCard(
                        padding: const EdgeInsets.all(16),
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(l10n.myProgress, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                          const SizedBox(height: 8),
                          LinearProgressIndicator(value: progress.clamp(0.0, 1.0)),
                          const SizedBox(height: 4),
                          Text('${(progress * 100).round()}% — $totalRead/$totalEntries ${l10n.days}'),
                          const SizedBox(height: 8),
                          Text('🔥 $streak ${l10n.consecutiveDays}',
                              style: TextStyle(color: Colors.brown.shade700, fontWeight: FontWeight.bold)),
                        ]),
                      ),
                      const SizedBox(height: 16),
                      // Today's reading
                      if (_today.isNotEmpty) ...[
                        Text(l10n.todaysReading, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                        const SizedBox(height: 8),
                        ..._today.map((e) {
                          final entry = e as Map<String, dynamic>;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Text(entry['referenceVerset'] ?? '',
                                  style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w500, color: Colors.white)),
                              if (entry['categorie'] != null || entry['theme'] != null)
                                Text('${entry['categorie'] ?? ''} — ${entry['theme'] ?? ''}',
                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 12)),
                              const SizedBox(height: 12),
                              Row(children: [
                                OutlinedButton.icon(
                                  onPressed: () async {
                                    await _api.post('/api/v1/bible-reading/entries/${entry['id']}/mark-read');
                                    _load();
                                  },
                                  icon: Icon(entry['lu'] == true ? Icons.check_circle : Icons.check, size: 16),
                                  label: Text(entry['lu'] == true ? l10n.readLabel : l10n.markAsRead),
                                ),
                                const SizedBox(width: 8),
                                OutlinedButton.icon(
                                  onPressed: () => _addNoteDialog(entry['id']),
                                  icon: const Icon(Icons.note_add, size: 16),
                                  label: Text(l10n.addNote),
                                ),
                              ]),
                            ]),
                          );
                        }),
                      ],
                      const SizedBox(height: 16),
                      // Plans
                      Text(l10n.availablePlans, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (_plans.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.noPlans, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._plans.map((p) {
                          final plan = p as Map<String, dynamic>;
                          final joursTotal = plan['joursTotal'] ?? 365;
                          final joursCompletes = plan['joursCompletes'] ?? 0;
                          final ratio = joursTotal > 0 ? joursCompletes / joursTotal : 0.0;
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(12),
                            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              Row(children: [
                                Expanded(child: Text(plan['titre'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white))),
                                Text('${(ratio * 100).round()}%', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
                              ]),
                              if (plan['description'] != null)
                                Text(plan['description'], style: TextStyle(fontSize: 12, color: Colors.white.withValues(alpha: 0.6))),
                              const SizedBox(height: 8),
                              LinearProgressIndicator(value: ratio.clamp(0.0, 1.0), color: Colors.green),
                            ]),
                          );
                        }),
                      // Family progress
                      if (_familyProgress.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        Text(l10n.familySharing, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                        const SizedBox(height: 8),
                        ..._familyProgress.map((f) {
                          final fp = f as Map<String, dynamic>;
                          final pct = fp['pourcentage'] ?? 0;
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 4),
                            child: Row(children: [
                              SizedBox(width: 80, child: Text(fp['planTitre'] ?? '', style: const TextStyle(fontSize: 13))),
                              Expanded(child: LinearProgressIndicator(value: (pct / 100.0).clamp(0.0, 1.0))),
                              const SizedBox(width: 8),
                              Text('$pct%', style: const TextStyle(fontSize: 12)),
                            ]),
                          );
                        }),
                      ],
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

  void _addNoteDialog(String entryId) {
    final ctrl = TextEditingController();
    showDialog(context: context, builder: (ctx) => AlertDialog(
      title: Text(AppLocalizations.of(context).addNote),
      content: TextField(controller: ctrl, maxLines: 5, decoration: const InputDecoration(border: OutlineInputBorder())),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: Text(AppLocalizations.of(context).cancel)),
        FilledButton(onPressed: () async {
          await _api.put('/api/v1/bible-reading/entries/$entryId/note', data: {'note': ctrl.text});
          if (mounted) Navigator.pop(ctx);
          _load();
        }, child: Text(AppLocalizations.of(context).save)),
      ],
    ));
  }
}
