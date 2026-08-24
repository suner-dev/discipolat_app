import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Calendar Integration screen
class CalendarIntegrationScreen extends StatefulWidget {
  const CalendarIntegrationScreen({super.key});

  @override
  State<CalendarIntegrationScreen> createState() => _CalendarIntegrationScreenState();
}

class _CalendarIntegrationScreenState extends State<CalendarIntegrationScreen> {
  final _api = ApiService();
  List<dynamic> events = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadEvents();
  }

  Future<void> _loadEvents() async {
    try {
      final res = await _api.get('/calendar');
      setState(() {
        events = res.data is List ? res.data : [];
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
        title: const Text('Calendrier'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : events.isEmpty
              ? const Center(child: Text('Aucun événement'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: events.length,
                  itemBuilder: (context, index) {
                    final ev = events[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: const Icon(Icons.event, color: Colors.blue),
                        title: Text(ev['titre'] ?? ''),
                        subtitle: Text(
                          '${ev['lieu'] ?? ''} • ${ev['source'] ?? ''}',
                          style: const TextStyle(fontSize: 12),
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  void _showCreateDialog(BuildContext context) {
    final titreCtrl = TextEditingController();
    final lieuCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nouvel événement'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: titreCtrl, decoration: const InputDecoration(labelText: 'Titre')),
            const SizedBox(height: 8),
            TextField(controller: lieuCtrl, decoration: const InputDecoration(labelText: 'Lieu')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (titreCtrl.text.isNotEmpty) {
                await _api.post('/calendar', data: {
                  'titre': titreCtrl.text,
                  'lieu': lieuCtrl.text,
                  'source': 'INTERNE',
                  'début': DateTime.now().toIso8601String(),
                  'fin': DateTime.now().add(const Duration(hours: 1)).toIso8601String(),
                });
                Navigator.pop(ctx);
                _loadEvents();
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}
