import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Personal Objectives screen - member-defined spiritual goals
class PersonalObjectivesScreen extends StatefulWidget {
  const PersonalObjectivesScreen({super.key});

  @override
  State<PersonalObjectivesScreen> createState() => _PersonalObjectivesScreenState();
}

class _PersonalObjectivesScreenState extends State<PersonalObjectivesScreen> {
  final _api = ApiService();
  List<dynamic> objectives = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadObjectives();
  }

  Future<void> _loadObjectives() async {
    try {
      final res = await _api.get('/personal-objectives');
      setState(() {
        objectives = res.data is List ? res.data : [];
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
        title: const Text('Objectifs Spirituels'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : objectives.isEmpty
              ? const Center(child: Text('Aucun objectif'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: objectives.length,
                  itemBuilder: (context, index) {
                    final obj = objectives[index];
                    final progress = obj['objectifCible'] > 0
                        ? (obj['progressionActuelle'] / obj['objectifCible']).clamp(0.0, 1.0)
                        : 0.0;
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: obj['statut'] == 'ATTEINT' ? Colors.green.shade100 : Colors.teal.shade100,
                          child: Icon(
                            obj['statut'] == 'ATTEINT' ? Icons.check_circle : Icons.track_changes,
                            color: obj['statut'] == 'ATTEINT' ? Colors.green : Colors.teal,
                          ),
                        ),
                        title: Text(obj['titre'] ?? ''),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${obj['progressionActuelle']}/${obj['objectifCible']} — ${obj['catégorie'] ?? ''}', style: const TextStyle(fontSize: 12)),
                            const SizedBox(height: 4),
                            LinearProgressIndicator(value: progress.toDouble(), color: Colors.teal),
                          ],
                        ),
                        trailing: obj['statut'] == 'EN_COURS'
                            ? IconButton(
                                icon: const Icon(Icons.add_circle, color: Colors.teal),
                                onPressed: () async {
                                  await _api.patch('/personal-objectives/${obj['id']}/progress');
                                  _loadObjectives();
                                },
                              )
                            : null,
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
        title: const Text('Nouvel objectif'),
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
                await _api.post('/personal-objectives', data: {
                  'titre': titreCtrl.text,
                  'description': descCtrl.text,
                  'catégorie': 'AUTRE',
                  'objectifCible': 1,
                });
                Navigator.pop(ctx);
                _loadObjectives();
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}
