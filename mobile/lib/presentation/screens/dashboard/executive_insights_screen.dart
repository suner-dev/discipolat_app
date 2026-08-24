import 'package:flutter/material.dart';

class ExecutiveInsightsScreen extends StatefulWidget {
  const ExecutiveInsightsScreen({super.key});

  @override
  State<ExecutiveInsightsScreen> createState() => _ExecutiveInsightsScreenState();
}

class _ExecutiveInsightsScreenState extends State<ExecutiveInsightsScreen> {
  final List<Map<String, dynamic>> _insights = [
    {'title': 'Tendance de présence en baisse', 'description': 'La présence moyenne a diminué de 8% sur les 3 dernières semaines.', 'severity': 'WARNING', 'metric': '62%', 'change': '-8%', 'action': 'Organiser un événement ciblé jeunesse.'},
    {'title': 'Opportunité : Nouveau groupe', 'description': 'Le quartier Nord a 15 membres sans groupe de maison.', 'severity': 'OPPORTUNITY', 'metric': '15', 'change': '+15', 'action': 'Identifier un leader potentiel.'},
    {'title': 'Finances en hausse', 'description': 'Les dons ont augmenté de 12%. Tendance positive.', 'severity': 'INFO', 'metric': '€4,250', 'change': '+12%', 'action': 'Continuer la communication.'},
    {'title': '5 membres à risque décrochage', 'description': 'Cinq membres sans participation depuis 3+ semaines.', 'severity': 'CRITICAL', 'metric': '5', 'change': '+2', 'action': 'Contacter immédiatement.'},
  ];

  Color _severityColor(String s) {
    switch (s) {
      case 'CRITICAL': return Colors.red;
      case 'WARNING': return Colors.orange;
      case 'OPPORTUNITY': return Colors.blue;
      default: return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Insights Exécutifs IA'), backgroundColor: Colors.indigo.shade600, foregroundColor: Colors.white),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _insights.length,
        itemBuilder: (context, i) {
          final insight = _insights[i];
          final color = _severityColor(insight['severity']);
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    Icon(Icons.lightbulb, color: color, size: 20),
                    const SizedBox(width: 8),
                    Expanded(child: Text(insight['title'], style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16))),
                    Text(insight['metric'], style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: color)),
                  ]),
                  const SizedBox(height: 8),
                  Text(insight['description'], style: TextStyle(color: Colors.grey.shade600)),
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                    child: Row(children: [
                      Icon(Icons.arrow_forward, color: color, size: 16),
                      const SizedBox(width: 8),
                      Expanded(child: Text(insight['action'], style: TextStyle(color: color, fontSize: 13))),
                    ]),
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
