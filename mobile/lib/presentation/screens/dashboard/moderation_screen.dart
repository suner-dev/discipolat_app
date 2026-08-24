import 'package:flutter/material.dart';

class ModerationScreen extends StatelessWidget {
  const ModerationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final items = [
      {'content': 'Merci Seigneur pour cette bénédiction...', 'source': 'TÉMOIGNAGE', 'status': 'APPROVED', 'risk': 'LOW', 'confidence': 0.95},
      {'content': 'Message suspect avec liens commerciaux', 'source': 'MESSAGE', 'status': 'PENDING', 'risk': 'HIGH', 'confidence': 0.82},
      {'content': 'Rapport de prière pour la famille Mbarga', 'source': 'RAPPORT', 'status': 'APPROVED', 'risk': 'LOW', 'confidence': 0.98},
    ];

    Color riskColor(String r) {
      switch (r) {
        case 'CRITICAL': return Colors.red;
        case 'HIGH': return Colors.orange;
        case 'MEDIUM': return Colors.amber;
        default: return Colors.green;
      }
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Modération IA'), backgroundColor: Colors.red.shade600, foregroundColor: Colors.white),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: items.length,
        itemBuilder: (context, i) {
          final item = items[i];
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    Icon(
                      item['status'] == 'APPROVED' ? Icons.check_circle : item['status'] == 'REJECTED' ? Icons.cancel : Icons.warning,
                      color: item['status'] == 'APPROVED' ? Colors.green : item['status'] == 'REJECTED' ? Colors.red : Colors.amber,
                      size: 20,
                    ),
                    const SizedBox(width: 8),
                    Expanded(child: Text(item['content'] as String, style: const TextStyle(fontSize: 14))),
                  ]),
                  const SizedBox(height: 8),
                  Row(children: [
                    Chip(label: Text(item['source'] as String, style: const TextStyle(fontSize: 11)), backgroundColor: Colors.grey.shade200),
                    const SizedBox(width: 8),
                    Chip(label: Text(item['risk'] as String, style: TextStyle(fontSize: 11, color: riskColor(item['risk'] as String))), backgroundColor: riskColor(item['risk'] as String).withOpacity(0.15)),
                    const Spacer(),
                    if (item['status'] == 'PENDING') ...[
                      TextButton(onPressed: () {}, child: const Text('Approuver', style: TextStyle(color: Colors.green))),
                      TextButton(onPressed: () {}, child: const Text('Rejeter', style: TextStyle(color: Colors.red))),
                    ],
                  ]),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
