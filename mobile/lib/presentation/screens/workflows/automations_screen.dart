import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Automations screen - configurable workflows
class AutomationsScreen extends StatefulWidget {
  const AutomationsScreen({super.key});

  @override
  State<AutomationsScreen> createState() => _AutomationsScreenState();
}

class _AutomationsScreenState extends State<AutomationsScreen> {
  List<dynamic> rules = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadRules();
  }

  final _api = ApiService();

  Future<void> _loadRules() async {
    try {
      final res = await _api.get('/automations');
      setState(() {
        rules = (res.data['content'] ?? res.data ?? []) as List;
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
        title: const Text('Automatisations'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : rules.isEmpty
              ? const Center(child: Text('Aucune automatisation'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: rules.length,
                  itemBuilder: (context, index) {
                    final rule = rules[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: Icon(
                          rule['statut'] == 'ACTIVE' ? Icons.play_circle : Icons.pause_circle,
                          color: rule['statut'] == 'ACTIVE' ? Colors.green : Colors.grey,
                        ),
                        title: Text(rule['titre'] ?? '', style: const TextStyle(fontWeight: FontWeight.w600)),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '⚡ ${rule['triggerEvent'] ?? ''} → 🎯 ${rule['actionType'] ?? ''}',
                              style: const TextStyle(fontSize: 12),
                            ),
                            Text(
                              '${rule['totalExécutions'] ?? 0} exécutions',
                              style: const TextStyle(fontSize: 11, color: Colors.grey),
                            ),
                          ],
                        ),
                        trailing: Switch(
                          value: rule['statut'] == 'ACTIVE',
                          onChanged: (val) async {
                            await _api.patch('/automations/${rule['id']}/toggle');
                            _loadRules();
                          },
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  void _showCreateDialog(BuildContext context) {
    final titreCtrl = TextEditingController();
    String trigger = 'ABSENCE_SOUTENUE';
    String action = 'ENVOYER_MESSAGE';
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nouvelle automatisation'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: titreCtrl, decoration: const InputDecoration(labelText: 'Titre')),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: trigger,
              decoration: const InputDecoration(labelText: 'QUAND (Déclencheur)'),
              items: const [
                DropdownMenuItem(value: 'ABSENCE_SOUTENUE', child: Text('Membre absent X semaines')),
                DropdownMenuItem(value: 'NOUVEAU_MEMBRE', child: Text('Nouveau membre')),
                DropdownMenuItem(value: 'RAPPORT_SOUMIS', child: Text('Rapport soumis')),
                DropdownMenuItem(value: 'SCORE_SPRITUEL_BAISSE', child: Text('Score en baisse')),
              ],
              onChanged: (v) { if (v != null) trigger = v; },
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: action,
              decoration: const InputDecoration(labelText: 'ALORS (Action)'),
              items: const [
                DropdownMenuItem(value: 'ENVOYER_MESSAGE', child: Text('Envoyer un message')),
                DropdownMenuItem(value: 'ENVOYER_EMAIL', child: Text('Envoyer un email')),
                DropdownMenuItem(value: 'CRÉER_ALERTE', child: Text('Créer une alerte')),
                DropdownMenuItem(value: 'NOTIFIER_ROLE', child: Text('Notifier un rôle')),
              ],
              onChanged: (v) { if (v != null) action = v; },
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (titreCtrl.text.isNotEmpty) {
                await _api.post('/automations', data: {
                  'titre': titreCtrl.text,
                  'triggerEvent': trigger,
                  'triggerParams': '{}',
                  'actionType': action,
                  'actionParams': '{}',
                });
                Navigator.pop(ctx);
                _loadRules();
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}
