import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Prayer Journal screen - personal prayer tracking
class PrayerJournalScreen extends StatefulWidget {
  const PrayerJournalScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<PrayerJournalScreen> createState() => _PrayerJournalScreenState();
}

class _PrayerJournalScreenState extends State<PrayerJournalScreen> {
  late final _api = widget.apiService ?? ApiService();
  List<dynamic> entries = [];
  bool loading = true;
  Map<String, dynamic> stats = {};

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final results = await Future.wait([
        _api.get('/prayer-journal'),
        _api.get('/prayer-journal/stats'),
      ]);
      final d0 = results[0].data;
      final rawEntries = d0 is Map<String, dynamic>
          ? (d0['content'] as List<dynamic>? ?? <dynamic>[])
          : (d0 is List ? d0 : <dynamic>[]);
      setState(() {
        entries = List<dynamic>.from(rawEntries);
        stats = results[1].data is Map ? results[1].data as Map<String, dynamic> : {};
        loading = false;
      });
    } catch (_) {
      setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).prayerJournalTitle),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : entries.isEmpty
              ? Center(child: Text(AppLocalizations.of(context).prayerJournalEmpty))
              : Column(
                  children: [
                    // Stats cards
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          _statCard(AppLocalizations.of(context).statTotal, '${stats['total'] ?? 0}', Colors.purple),
                          const SizedBox(width: 8),
                          _statCard(AppLocalizations.of(context).statOngoing, '${stats['enCours'] ?? 0}', Colors.amber),
                          const SizedBox(width: 8),
                          _statCard(AppLocalizations.of(context).statAnswered, '${stats['exaucees'] ?? 0}', Colors.green),
                        ],
                      ),
                    ),
                    // Entries list
                    Expanded(
                      child: ListView.builder(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        itemCount: entries.length,
                        itemBuilder: (context, index) {
                          final entry = entries[index];
                          return Card(
                            margin: const EdgeInsets.only(bottom: 8),
                            child: ListTile(
                              leading: Icon(
                                entry['statut'] == 'EXAUC_EE' ? Icons.check_circle : Icons.circle,
                                color: entry['statut'] == 'EXAUC_EE' ? Colors.green : Colors.purple,
                              ),
                              title: Text(
                                entry['contenu'] ?? '',
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                              subtitle: Text(
                                entry['categorie'] ?? '',
                                style: const TextStyle(fontSize: 12),
                              ),
                              trailing: entry['statut'] == 'EN_COURS'
                                  ? IconButton(
                                      icon: const Icon(Icons.check, color: Colors.green),
                                      onPressed: () async {
                                        await _api.patch('/prayer-journal/${entry['id']}/answered', data: {'reponse': 'Exaucee'});
                                        _loadData();
                                      },
                                    )
                                  : null,
                            ),
                          );
                        },
                      ),
                    ),
                  ],
                ),
    );
  }

  Widget _statCard(String label, String value, Color color) {
    return Expanded(
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            children: [
              Text(value, style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: color)),
              Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
            ],
          ),
        ),
      ),
    );
  }

  void _showCreateDialog(BuildContext context) {
    final ctrl = TextEditingController();
    String categorie = 'PRIERE';
    showDialog(
      context: context,
      builder: (ctx) {
        final l = AppLocalizations.of(context);
        return AlertDialog(
        title: Text(l.newPrayer),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: ctrl, decoration: InputDecoration(labelText: l.yourPrayer), maxLines: 4),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              value: categorie,
              decoration: InputDecoration(labelText: l.categoryLabel),
              items: [
                DropdownMenuItem(value: 'PRIERE', child: Text(l.catPrayer)),
                DropdownMenuItem(value: 'LOUANGE', child: Text(l.catPraise)),
                DropdownMenuItem(value: 'INTERCESSION', child: Text(l.catIntercession)),
                DropdownMenuItem(value: 'GRACE', child: Text(l.catGrace)),
              ],
              onChanged: (v) { if (v != null) categorie = v; },
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: Text(l.cancel)),
          FilledButton(
            onPressed: () async {
              if (ctrl.text.isNotEmpty) {
                await _api.post('/prayer-journal', data: {
                  'contenu': ctrl.text,
                  'categorie': categorie,
                  'visibilite': 'PRIVEE',
                });
                Navigator.pop(ctx);
                _loadData();
              }
            },
            child: Text(AppLocalizations.of(context).add),
          ),
        ],
        );
      },
    );
  }
}
