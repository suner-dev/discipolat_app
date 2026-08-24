import 'package:flutter/material.dart';

class DiscipleshipPathScreen extends StatelessWidget {
  const DiscipleshipPathScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final stages = [
      {'key': 'DISCOVERY', 'label': 'Découverte', 'icon': '🌱', 'color': Colors.green, 'desc': 'Premiers pas dans la foi'},
      {'key': 'FOUNDATION', 'label': 'Fondations', 'icon': '🏗️', 'color': Colors.blue, 'desc': 'Bases de la foi'},
      {'key': 'GROWTH', 'label': 'Croissance', 'icon': '🌳', 'color': Colors.purple, 'desc': 'Approfondissement spirituel'},
      {'key': 'SERVICE', 'label': 'Service', 'icon': '🤝', 'color': Colors.orange, 'desc': 'Service communautaire'},
      {'key': 'LEADERSHIP', 'label': 'Leadership', 'icon': '👑', 'color': Colors.amber, 'desc': 'Former les autres'},
      {'key': 'MATURITY', 'label': 'Maturité', 'icon': '🌟', 'color': Colors.pink, 'desc': 'Plénitude spirituelle'},
    ];
    const currentStage = 2;

    return Scaffold(
      appBar: AppBar(title: const Text('Parcours de discipolat'), backgroundColor: Colors.blue.shade600, foregroundColor: Colors.white),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            child: Column(children: [
              const Text('Progression', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
              const SizedBox(height: 8),
              ClipRRect(borderRadius: BorderRadius.circular(4), child: LinearProgressIndicator(value: 0.35, backgroundColor: Colors.grey.shade200, color: Colors.blue, minHeight: 10)),
              const SizedBox(height: 4),
              const Text('35%', style: TextStyle(color: Colors.blue, fontWeight: FontWeight.bold)),
            ]),
          ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: stages.length,
              itemBuilder: (context, i) {
                final s = stages[i];
                final isCompleted = i < currentStage;
                final isCurrent = i == currentStage;
                return Card(
                  margin: const EdgeInsets.only(bottom: 8),
                  color: isCurrent ? Colors.blue.shade50 : Colors.white,
                  child: ListTile(
                    leading: CircleAvatar(
                      backgroundColor: (s['color'] as Color).withOpacity(isCurrent ? 1 : 0.3),
                      child: Text(s['icon'] as String, style: const TextStyle(fontSize: 20)),
                    ),
                    title: Text(s['label'] as String, style: TextStyle(fontWeight: FontWeight.bold, color: isCurrent ? Colors.blue : Colors.black)),
                    subtitle: Text(s['desc'] as String),
                    trailing: isCompleted ? const Icon(Icons.check_circle, color: Colors.green) :
                        isCurrent ? Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(color: Colors.blue, borderRadius: BorderRadius.circular(12)),
                          child: const Text('En cours', style: TextStyle(color: Colors.white, fontSize: 11)),
                        ) : const Icon(Icons.lock, color: Colors.grey),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
