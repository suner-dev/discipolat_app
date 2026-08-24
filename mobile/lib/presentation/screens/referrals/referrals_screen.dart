import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

class ReferralsScreen extends StatefulWidget {
  const ReferralsScreen({super.key});

  @override
  State<ReferralsScreen> createState() => _ReferralsScreenState();
}

class _ReferralsScreenState extends State<ReferralsScreen> {
  List<dynamic> referrals = [];
  Map<String, dynamic> stats = {};
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final res = await ApiService().get('/referrals');
      final statsRes = await ApiService().get('/referrals/stats');
      setState(() {
        referrals = (res.data['content'] ?? res.data ?? []) as List;
        stats = Map<String, dynamic>.from(statsRes.data);
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
        title: const Text('Parrainage'),
        actions: [
          IconButton(
            icon: const Icon(Icons.person_add),
            onPressed: () => _showInviteDialog(context),
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      _statCard('Total', '${stats['total'] ?? 0}', Colors.blue),
                      const SizedBox(width: 8),
                      _statCard('Inscrits', '${stats['inscrits'] ?? 0}', Colors.green),
                      const SizedBox(width: 8),
                      _statCard('Baptêmes', '${stats['baptemes'] ?? 0}', Colors.purple),
                      const SizedBox(width: 8),
                      _statCard('Mes parr.', '${stats['mesParrainages'] ?? 0}', Colors.orange),
                    ],
                  ),
                ),
                Expanded(
                  child: referrals.isEmpty
                      ? const Center(child: Text('Aucun parrainage'))
                      : ListView.builder(
                          itemCount: referrals.length,
                          itemBuilder: (context, index) {
                            final ref = referrals[index];
                            return Card(
                              margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                              child: ListTile(
                                leading: const Icon(Icons.person_add, color: Colors.green),
                                title: Text(ref['nomComplet'] ?? ''),
                                subtitle: Text(ref['statut'] ?? ''),
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
              Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
              const SizedBox(height: 4),
              Text(label, style: const TextStyle(fontSize: 10, color: Colors.grey)),
            ],
          ),
        ),
      ),
    );
  }

  void _showInviteDialog(BuildContext context) {
    final nomCtrl = TextEditingController();
    final telCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Inviter un proche'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: nomCtrl, decoration: const InputDecoration(labelText: 'Nom complet')),
            const SizedBox(height: 8),
            TextField(controller: telCtrl, decoration: const InputDecoration(labelText: 'Téléphone')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () async {
              if (nomCtrl.text.isNotEmpty) {
                await ApiService().post('/referrals', data: {
                  'nomComplet': nomCtrl.text,
                  'telephone': telCtrl.text,
                });
                Navigator.pop(ctx);
                _loadData();
              }
            },
            child: const Text('Envoyer'),
          ),
        ],
      ),
    );
  }
}
