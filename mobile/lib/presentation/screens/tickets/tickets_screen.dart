import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

class TicketsScreen extends StatefulWidget {
  const TicketsScreen({super.key});

  @override
  State<TicketsScreen> createState() => _TicketsScreenState();
}

class _TicketsScreenState extends State<TicketsScreen> {
  List<dynamic> tickets = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadTickets();
  }

  Future<void> _loadTickets() async {
    try {
      final res = await ApiService().get('/tickets');
      setState(() {
        tickets = (res.data['content'] ?? res.data ?? []) as List;
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
        title: const Text('Tickets & Support'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : tickets.isEmpty
              ? const Center(child: Text('Aucun ticket'))
              : ListView.builder(
                  itemCount: tickets.length,
                  itemBuilder: (context, index) {
                    final ticket = tickets[index];
                    return Card(
                      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      child: ListTile(
                        leading: Icon(
                          _statusIcon(ticket['statut']),
                          color: _statusColor(ticket['statut']),
                        ),
                        title: Text(ticket['titre'] ?? ''),
                        subtitle: Text(
                          ticket['description'] ?? '',
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        trailing: Chip(
                          label: Text(
                            ticket['priorite'] ?? '',
                            style: const TextStyle(fontSize: 10),
                          ),
                          backgroundColor: _prioriteColor(ticket['priorite']),
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  IconData _statusIcon(String? statut) {
    switch (statut) {
      case 'OUVERT': return Icons.circle;
      case 'EN_COURS': return Icons.access_time;
      case 'RESOLU': return Icons.check_circle;
      case 'FERME': return Icons.check_circle_outline;
      default: return Icons.circle;
    }
  }

  Color _statusColor(String? statut) {
    switch (statut) {
      case 'OUVERT': return Colors.blue;
      case 'EN_COURS': return Colors.orange;
      case 'RESOLU': return Colors.green;
      case 'FERME': return Colors.grey;
      default: return Colors.grey;
    }
  }

  Color _prioriteColor(String? priorite) {
    switch (priorite) {
      case 'CRITIQUE': return Colors.red.shade100;
      case 'HAUTE': return Colors.orange.shade100;
      case 'MOYENNE': return Colors.blue.shade100;
      case 'BASSE': return Colors.grey.shade100;
      default: return Colors.grey.shade100;
    }
  }

  void _showCreateDialog(BuildContext context) {
    final titreCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nouveau ticket'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: titreCtrl, decoration: const InputDecoration(labelText: 'Titre')),
            const SizedBox(height: 8),
            TextField(controller: descCtrl, decoration: const InputDecoration(labelText: 'Description'), maxLines: 3),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (titreCtrl.text.isNotEmpty) {
                await ApiService().post('/tickets', data: {
                  'titre': titreCtrl.text,
                  'description': descCtrl.text,
                  'categorie': 'QUESTION',
                  'priorite': 'MOYENNE',
                });
                Navigator.pop(ctx);
                _loadTickets();
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}
