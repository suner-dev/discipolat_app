import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #28 — Broadcast ciblé avec accusé de lecture — branché sur API réelle.
class BroadcastScreen extends StatefulWidget {
  const BroadcastScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<BroadcastScreen> createState() => _BroadcastScreenState();
}

class _BroadcastScreenState extends State<BroadcastScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _broadcasts = [];
  Map<String, dynamic>? _stats;
  bool _isLoading = true;
  String? _error;
  String _filterTarget = 'ALL';

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _isLoading = true; _error = null; });
    try {
      final res = await _api.get('/broadcast');
      final statsRes = await _api.get('/broadcast/stats');
      if (mounted) {
        setState(() {
          _broadcasts = (res.data is List ? res.data : (res.data is Map ? res.data['content'] : [])) as List<dynamic>;
          _stats = statsRes.data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  Future<void> _showCreateDialog() async {
    final titleCtrl = TextEditingController();
    final contentCtrl = TextEditingController();
    String target = 'ALL';
    final l10n = AppLocalizations.of(context);

    final result = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.newBroadcast),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: titleCtrl,
                decoration: const InputDecoration(labelText: 'Titre', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: contentCtrl,
                maxLines: 3,
                decoration: const InputDecoration(labelText: 'Contenu', border: OutlineInputBorder()),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                value: target,
                decoration: const InputDecoration(labelText: 'Cible', border: OutlineInputBorder()),
                items: const [
                  DropdownMenuItem(value: 'ALL', child: Text('Tous les membres')),
                  DropdownMenuItem(value: 'DEPARTMENT', child: Text('Par département')),
                  DropdownMenuItem(value: 'FAMILY', child: Text('Par famille')),
                  DropdownMenuItem(value: 'ROLE', child: Text('Par rôle')),
                ],
                onChanged: (v) => target = v ?? 'ALL',
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Créer'),
          ),
        ],
      ),
    );

    if (result == true && titleCtrl.text.isNotEmpty && contentCtrl.text.isNotEmpty) {
      try {
        await _api.post('/broadcast', data: {
          'titre': titleCtrl.text,
          'contenu': contentCtrl.text,
          'cible': target,
        });
        _load();
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Broadcast créé avec succès')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Erreur: ${e.toString().split(':').first}')),
          );
        }
      }
    }
  }

  List<dynamic> get _filteredBroadcasts {
    if (_filterTarget == 'ALL') return _broadcasts;
    return _broadcasts.where((b) {
      final item = b as Map<String, dynamic>;
      return (item['target']?.toString() ?? item['cible']?.toString() ?? '') == _filterTarget;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.broadcastTitle),
        backgroundColor: Colors.deepOrange.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _showCreateDialog,
        backgroundColor: Colors.deepOrange.shade700,
        icon: const Icon(Icons.send, color: Colors.white),
        label: Text(l10n.newBroadcast, style: const TextStyle(color: Colors.white)),
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 6)
          : _error != null
              ? Center(child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
                    const SizedBox(height: 12),
                    Text(l10n.broadcastError, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                    const SizedBox(height: 12),
                    FilledButton.icon(onPressed: _load, icon: const Icon(Icons.refresh, size: 16), label: Text(l10n.retry)),
                  ],
                ))
              : RefreshIndicator(
                  onRefresh: _load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      // Stats
                      Row(children: [
                        _statCard(l10n.sent, '${_stats?['totalSent'] ?? _broadcasts.length}', Colors.deepOrange),
                        const SizedBox(width: 8),
                        _statCard(l10n.readRate, '${_stats?['readRate'] ?? 0}%', Colors.green),
                      ]),
                      const SizedBox(height: 16),
                      // Recent broadcasts
                      Text(l10n.recentBroadcasts, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      if (_filteredBroadcasts.isEmpty)
                        Padding(
                          padding: const EdgeInsets.all(24),
                          child: Center(child: Text(l10n.broadcastEmpty, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)))),
                        )
                      else
                        ..._filteredBroadcasts.map((b) {
                          final item = b as Map<String, dynamic>;
                          final title = item['titre']?.toString() ?? item['title']?.toString() ?? '';
                          final target = item['target']?.toString() ?? item['audience']?.toString() ?? '';
                          final readRate = (item['readRate'] as num?)?.toDouble() ?? 0;
                          final date = item['createdAt']?.toString().split('T').first ?? '';
                          return _broadcastCard(title, target, date, '${(readRate * 100).round()}% ${l10n.readLabel}', readRate);
                        }),
                      const SizedBox(height: 16),
                      // Targeting
                      Text(l10n.targeting, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      const SizedBox(height: 8),
                      Wrap(spacing: 8, children: [
                        _targetChip(l10n.allMembers, 'ALL'),
                        _targetChip(l10n.byDepartment, 'DEPARTMENT'),
                        _targetChip(l10n.byFamily, 'FAMILY'),
                        _targetChip(l10n.byRole, 'ROLE'),
                      ]),
                    ],
                  ),
                ),
    );
  }

  Widget _statCard(String label, String value, Color color) {
    return Expanded(
      child: GlassCard(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text(label, style: TextStyle(fontSize: 11, color: Colors.white.withValues(alpha: 0.5))),
          Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
        ]),
      ),
    );
  }

  Widget _broadcastCard(String title, String target, String date, String readRate, double ratio) {
    return GlassCard(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white))),
          Text(readRate, style: const TextStyle(fontSize: 12, color: Colors.green)),
        ]),
        if (target.isNotEmpty || date.isNotEmpty)
          Text('$target • $date', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 12)),
        const SizedBox(height: 8),
        LinearProgressIndicator(value: ratio.clamp(0.0, 1.0), color: Colors.green),
      ]),
    );
  }

  Widget _targetChip(String label, String target) {
    final isSelected = _filterTarget == target;
    return ActionChip(
      label: Text(label),
      onPressed: () => setState(() => _filterTarget = target),
      backgroundColor: isSelected ? Colors.deepOrange.withValues(alpha: 0.3) : Colors.white.withValues(alpha: 0.08),
    );
  }
}
