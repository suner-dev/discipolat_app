import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Broadcast screen - targeted messaging
class BroadcastScreen extends StatefulWidget {
  const BroadcastScreen({super.key});

  @override
  State<BroadcastScreen> createState() => _BroadcastScreenState();
}

class _BroadcastScreenState extends State<BroadcastScreen> {
  final _api = ApiService();
  List<dynamic> messages = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadMessages();
  }

  Future<void> _loadMessages() async {
    try {
      final res = await _api.get('/broadcast');
      setState(() {
        messages = (res.data['content'] ?? res.data ?? []) as List;
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
        title: const Text('Diffusion / Broadcast'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : messages.isEmpty
              ? const Center(child: Text('Aucun broadcast'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: messages.length,
                  itemBuilder: (context, index) {
                    final msg = messages[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: Icon(
                          msg['statut'] == 'ENVOYÉ' ? Icons.send : Icons.drafts,
                          color: msg['statut'] == 'ENVOYÉ' ? Colors.green : Colors.grey,
                        ),
                        title: Text(msg['titre'] ?? ''),
                        subtitle: Text(
                          msg['contenu'] ?? '',
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: Chip(
                          label: Text(msg['statut'] ?? '', style: const TextStyle(fontSize: 10)),
                          backgroundColor: msg['statut'] == 'ENVOYÉ' ? Colors.green.shade100 : Colors.grey.shade100,
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  void _showCreateDialog(BuildContext context) {
    final titreCtrl = TextEditingController();
    final contenuCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nouveau broadcast'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: titreCtrl, decoration: const InputDecoration(labelText: 'Titre')),
            const SizedBox(height: 8),
            TextField(controller: contenuCtrl, decoration: const InputDecoration(labelText: 'Message'), maxLines: 4),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (titreCtrl.text.isNotEmpty && contenuCtrl.text.isNotEmpty) {
                final res = await _api.post('/broadcast', data: {
                  'titre': titreCtrl.text,
                  'contenu': contenuCtrl.text,
                  'cible': 'TOUS',
                });
                await _api.patch('/broadcast/${res.data['id']}/send');
                Navigator.pop(ctx);
                _loadMessages();
              }
            },
            child: const Text('Envoyer'),
          ),
        ],
      ),
    );
  }
}
