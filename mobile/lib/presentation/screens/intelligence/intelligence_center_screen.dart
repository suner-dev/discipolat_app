import 'package:flutter/material.dart';

/// P1 #64 — Centre d'intelligence organisationnelle (50+ KPIs temps réel)
class IntelligenceCenterScreen extends StatelessWidget {
  const IntelligenceCenterScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🏛️ Centre d\'Intelligence'),
        backgroundColor: Colors.blueGrey.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Real-time alert
          Card(
            color: Colors.amber.shade50,
            child: ListTile(
              leading: const Icon(Icons.warning_amber, color: Colors.amber),
              title: const Text('Alerte: 3 membres à risque', style: TextStyle(fontWeight: FontWeight.bold)),
              subtitle: const Text('Présence en baisse depuis 3 semaines'),
            ),
          ),
          const SizedBox(height: 16),
          // KPI categories
          const Text('📈 Aperçu rapide', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          _kpiGrid([
            ('Effectifs', '156', Colors.blue),
            ('Présence', '78%', Colors.green),
            ('Conversions', '12/mois', Colors.teal),
            ('Générosité', '45K€', Colors.green),
            ('Rétention', '85%', Colors.green),
            ('Décrochage', '3%', Colors.orange),
            ('Bénévoles', '24', Colors.purple),
            ('Événements', '8/mois', Colors.blue),
          ]),
          const SizedBox(height: 16),
          // Early warnings
          const Text('⚠️ Signes avant-coureurs', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _warningItem('3 membres sans présence depuis 4+ semaines', Colors.red),
          _warningItem('Département Jeunesse en baisse -12%', Colors.orange),
          _warningItem('2 demandes de transfert en cours', Colors.amber),
          const SizedBox(height: 16),
          // Quick actions
          const Text('🚀 Actions rapides', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _actionItem(Icons.people, 'Envoyer relance décrochage', Colors.teal),
          _actionItem(Icons.bar_chart, 'Rapport mensuel auto', Colors.blue),
          _actionItem(Icons.share, 'Partager dashboard', Colors.grey),
        ],
      ),
    );
  }

  Widget _kpiGrid(List<(String, String, Color)> items) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 2.2),
      itemCount: items.length,
      itemBuilder: (ctx, i) {
        final (label, value, color) = items[i];
        return Card(
          child: Padding(
            padding: const EdgeInsets.all(8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
              Text(label, style: TextStyle(fontSize: 11, color: Colors.grey.shade600)),
              Text(value, style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: color)),
            ]),
          ),
        );
      },
    );
  }

  Widget _warningItem(String text, Color color) {
    return ListTile(
      leading: Icon(Icons.circle, color: color, size: 8),
      title: Text(text, style: const TextStyle(fontSize: 13)),
      dense: true,
    );
  }

  Widget _actionItem(IconData icon, String text, Color color) {
    return Card(
      child: ListTile(
        leading: Icon(icon, color: color),
        title: Text(text),
        trailing: const Icon(Icons.chevron_right, size: 20),
      ),
    );
  }
}
