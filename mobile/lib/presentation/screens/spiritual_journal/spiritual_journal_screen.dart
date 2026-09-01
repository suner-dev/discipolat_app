import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../../app.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// P1 #55 — Journal spirituel personnel — branché sur API réelle.
class SpiritualJournalScreen extends StatefulWidget {
  const SpiritualJournalScreen({super.key, this.apiService});
  
  final ApiService? apiService;

  @override
  State<SpiritualJournalScreen> createState() => _SpiritualJournalScreenState();
}

class _SpiritualJournalScreenState extends State<SpiritualJournalScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _entries = [];
  Map<String, dynamic>? _stats;
  String _filter = 'all';
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
        _api.get('/spiritual-journals/by-author/$userId'),
        _api.get('/spiritual-journals/stats/$userId'),
      ]);
      if (mounted) {
        setState(() {
          _entries = (results[0].data is List ? results[0].data : []) as List<dynamic>;
          _stats = results[1].data as Map<String, dynamic>?;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  Future<void> _loadFiltered(String type) async {
    setState(() { _filter = type; _isLoading = true; });
    try {
      final userId = AuthState().userId;
      final url = type == 'all'
          ? '/spiritual-journals/by-author/$userId'
          : '/spiritual-journals/by-type/$userId/$type';
      final res = await _api.get(url);
      if (mounted) {
        setState(() {
          _entries = (res.data is List ? res.data : []) as List<dynamic>;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() { _error = e.toString(); _isLoading = false; });
    }
  }

  String _typeIcon(String? type) {
    switch (type) {
      case 'PRIERE': return '🙏';
      case 'REFLEXION': return '💡';
      case 'REMERCIEMENT': return '🎉';
      case 'LOUANGE': return '🎵';
      case 'LECON': return '📖';
      default: return '📝';
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final streak = _stats?['streak'] ?? 0;

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.spiritualJournalTitle),
        backgroundColor: Colors.purple.shade600,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _load)],
      ),
      body: Column(
        children: [
          // Streak banner
          if (streak > 0)
            Container(
              padding: const EdgeInsets.all(12),
              color: Colors.purple.shade50,
              child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
                const Icon(Icons.local_fire_department, color: Colors.orange),
                const SizedBox(width: 8),
                Text('🔥 $streak ${l10n.consecutiveDays}!',
                    style: TextStyle(color: Colors.purple.shade700, fontWeight: FontWeight.bold)),
              ]),
            ),
          // Filter chips
          SizedBox(
            height: 50,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              children: [
                _filterChip('all', l10n.all),
                _filterChip('PRIERE', '${l10n.prayer} 🙏'),
                _filterChip('REFLEXION', '${l10n.reflection} 💡'),
                _filterChip('REMERCIEMENT', '${l10n.thanksgiving} 🎉'),
                _filterChip('LOUANGE', '${l10n.praise} 🎵'),
                _filterChip('LECON', '${l10n.lesson} 📖'),
              ],
            ),
          ),
          // Entries
          Expanded(
            child: _isLoading
                ? const ShimmerLoading(itemCount: 4)
                : _error != null
                    ? Center(child: Text(l10n.error))
                    : RefreshIndicator(
                        onRefresh: _load,
                        child: _entries.isEmpty
                            ? Center(child: Text(l10n.noEntries, style: TextStyle(color: Colors.white.withValues(alpha: 0.5))))
                            : ListView.builder(
                                padding: const EdgeInsets.all(12),
                                itemCount: _entries.length,
                                itemBuilder: (ctx, i) {
                                  final e = _entries[i] as Map<String, dynamic>;
                                  return Card(
                                    child: ListTile(
                                      leading: Text(_typeIcon(e['typeEntree']), style: const TextStyle(fontSize: 28)),
                                      title: Text(e['titre'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
                                      subtitle: Text(e['dateEntree'] ?? ''),
                                      trailing: IconButton(
                                        icon: Icon(
                                          e['favori'] == true ? Icons.star : Icons.star_border,
                                          color: Colors.amber,
                                        ),
                                        onPressed: () async {
                                          await _api.post('/spiritual-journals/${e['id']}/toggle-favorite');
                                          _load();
                                        },
                                      ),
                                    ),
                                  );
                                },
                              ),
                      ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _showNewEntrySheet,
        backgroundColor: Colors.purple.shade600,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  Widget _filterChip(String value, String label) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: ChoiceChip(
        label: Text(label),
        selected: _filter == value,
        onSelected: (_) => _loadFiltered(value),
      ),
    );
  }

  void _showNewEntrySheet() {
    final titleCtrl = TextEditingController();
    final contentCtrl = TextEditingController();
    String typeEntry = 'PRIERE';
    final types = [
      ('PRIERE', '🙏 Prière'),
      ('REFLEXION', '💡 Réflexion'),
      ('REMERCIEMENT', '🎉 Remerciement'),
      ('LOUANGE', '🎵 Louange'),
      ('LECON', '📖 Leçon'),
    ];

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setSheetState) => Padding(
          padding: EdgeInsets.only(bottom: MediaQuery.of(ctx).viewInsets.bottom),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(AppLocalizations.of(context).newEntry, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                const SizedBox(height: 16),
                Wrap(
                  spacing: 8,
                  children: types.map((t) => ChoiceChip(
                    label: Text(t.$2),
                    selected: typeEntry == t.$1,
                    onSelected: (_) => setSheetState(() => typeEntry = t.$1),
                  )).toList(),
                ),
                const SizedBox(height: 12),
                TextField(controller: titleCtrl, decoration: const InputDecoration(labelText: 'Titre', border: OutlineInputBorder())),
                const SizedBox(height: 12),
                TextField(controller: contentCtrl, maxLines: 5, decoration: const InputDecoration(labelText: 'Contenu', border: OutlineInputBorder())),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    style: ElevatedButton.styleFrom(backgroundColor: Colors.purple.shade600),
                    onPressed: () async {
                      await _api.post('/spiritual-journals', data: {
                        'titre': titleCtrl.text,
                        'contenu': contentCtrl.text,
                        'typeEntree': typeEntry,
                        'auteurId': AuthState().userId,
                      });
                      if (mounted) Navigator.pop(ctx);
                      _load();
                    },
                    child: Text(AppLocalizations.of(context).save, style: const TextStyle(color: Colors.white)),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
