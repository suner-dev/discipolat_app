import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Spiritual Challenges screen
class SpiritualChallengesScreen extends StatefulWidget {
  const SpiritualChallengesScreen({super.key});

  @override
  State<SpiritualChallengesScreen> createState() => _SpiritualChallengesScreenState();
}

class _SpiritualChallengesScreenState extends State<SpiritualChallengesScreen> {
  final _api = ApiService();
  List<dynamic> challenges = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadChallenges();
  }

  Future<void> _loadChallenges() async {
    try {
      final res = await _api.get('/spiritual-challenges');
      setState(() {
        challenges = (res.data['content'] ?? res.data ?? []) as List;
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
        title: const Text('Défis Spirituels'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : challenges.isEmpty
              ? const Center(child: Text('Aucun défi spirituel'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: challenges.length,
                  itemBuilder: (context, index) {
                    final c = challenges[index];
                    final progress = c['objectifJours'] > 0
                        ? (c['joursComplétés'] / c['objectifJours']).clamp(0.0, 1.0)
                        : 0.0;
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Expanded(
                                  child: Text(c['titre'] ?? '', style: const TextStyle(fontWeight: FontWeight.bold)),
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: c['statut'] == 'TERMINÉ' ? Colors.green.shade100 : Colors.orange.shade100,
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Text(c['statut'] ?? '', style: const TextStyle(fontSize: 10)),
                                ),
                              ],
                            ),
                            if (c['description'] != null) ...[
                              const SizedBox(height: 4),
                              Text(c['description'], style: const TextStyle(fontSize: 12, color: Colors.grey), maxLines: 2),
                            ],
                            const SizedBox(height: 8),
                            LinearProgressIndicator(
                              value: progress.toDouble(),
                              backgroundColor: Colors.grey.shade200,
                              color: Colors.orange,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              '${c['joursComplétés']}/${c['objectifJours']} jours',
                              style: const TextStyle(fontSize: 12),
                            ),
                            if (c['statut'] == 'EN_COURS') ...[
                              const SizedBox(height: 8),
                              Align(
                                alignment: Alignment.centerRight,
                                child: ElevatedButton.icon(
                                  onPressed: () async {
                                    await _api.patch('/spiritual-challenges/${c['id']}/progress');
                                    _loadChallenges();
                                  },
                                  icon: const Icon(Icons.add, size: 16),
                                  label: const Text('+1 jour'),
                                  style: ElevatedButton.styleFrom(backgroundColor: Colors.orange),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  void _showCreateDialog(BuildContext context) {
    final titreCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nouveau défi'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: titreCtrl, decoration: const InputDecoration(labelText: 'Titre')),
            const SizedBox(height: 8),
            TextField(controller: descCtrl, decoration: const InputDecoration(labelText: 'Description'), maxLines: 2),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (titreCtrl.text.isNotEmpty) {
                await _api.post('/spiritual-challenges', data: {
                  'titre': titreCtrl.text,
                  'description': descCtrl.text,
                  'type': 'AUTRE',
                  'objectifJours': 7,
                });
                Navigator.pop(ctx);
                _loadChallenges();
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}
