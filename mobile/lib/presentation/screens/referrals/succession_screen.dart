import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Succession Planning screen
class SuccessionScreen extends StatefulWidget {
  const SuccessionScreen({super.key});

  @override
  State<SuccessionScreen> createState() => _SuccessionScreenState();
}

class _SuccessionScreenState extends State<SuccessionScreen> {
  final _api = ApiService();
  List<dynamic> plans = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadPlans();
  }

  Future<void> _loadPlans() async {
    try {
      final res = await _api.get('/succession');
      setState(() {
        plans = res.data is List ? res.data : [];
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
        title: const Text('Plan de Succession'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            onPressed: () => _showCreateDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : plans.isEmpty
              ? const Center(child: Text('Aucun plan de succession'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: plans.length,
                  itemBuilder: (context, index) {
                    final plan = plans[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ListTile(
                        leading: const Icon(Icons.account_tree, color: Colors.purple),
                        title: Text('Rôle: ${plan['rôleCible'] ?? ''}'),
                        subtitle: Text(
                          'Statut: ${plan['statut'] ?? ''} • Readiness: ${plan['readiness'] ?? ''}',
                          style: const TextStyle(fontSize: 12),
                        ),
                        trailing: Chip(
                          label: Text(plan['statut'] ?? '', style: const TextStyle(fontSize: 10)),
                          backgroundColor: Colors.purple.shade100,
                        ),
                      ),
                    );
                  },
                ),
    );
  }

  void _showCreateDialog(BuildContext context) {
    final candidatCtrl = TextEditingController();
    final roleCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nouveau plan'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: candidatCtrl, decoration: const InputDecoration(labelText: 'ID du candidat')),
            const SizedBox(height: 8),
            TextField(controller: roleCtrl, decoration: const InputDecoration(labelText: 'Rôle cible')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (candidatCtrl.text.isNotEmpty && roleCtrl.text.isNotEmpty) {
                await _api.post('/succession', data: {
                  'candidatId': candidatCtrl.text,
                  'rôleCible': roleCtrl.text,
                });
                Navigator.pop(ctx);
                _loadPlans();
              }
            },
            child: const Text('Créer'),
          ),
        ],
      ),
    );
  }
}
