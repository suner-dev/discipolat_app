import 'package:flutter/material.dart';

class PredictionsMlScreen extends StatelessWidget {
  const PredictionsMlScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final predictions = [
      {'type': 'EFFECTIFS', 'current': 245, 'predicted': 278, 'growth': 13.5, 'trend': 'UP', 'confidence': 'HIGH'},
      {'type': 'PRÉSENCES', 'current': 180, 'predicted': 168, 'growth': -6.7, 'trend': 'DOWN', 'confidence': 'MEDIUM'},
      {'type': 'BAPTÊMES', 'current': 12, 'predicted': 18, 'growth': 50.0, 'trend': 'UP', 'confidence': 'MEDIUM'},
      {'type': 'DÉCROCHAGES', 'current': 5, 'predicted': 8, 'growth': 60.0, 'trend': 'UP', 'confidence': 'LOW'},
      {'type': 'FINANCES', 'current': 4250, 'predicted': 4680, 'growth': 10.1, 'trend': 'UP', 'confidence': 'HIGH'},
    ];

    return Scaffold(
      appBar: AppBar(title: const Text('Prédictions ML'), backgroundColor: Colors.purple.shade600, foregroundColor: Colors.white),
      body: GridView.builder(
        padding: const EdgeInsets.all(16),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.1, crossAxisSpacing: 12, mainAxisSpacing: 12),
        itemCount: predictions.length,
        itemBuilder: (context, i) {
          final p = predictions[i];
          final isUp = p['trend'] == 'UP';
          return Card(
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    Icon(isUp ? Icons.trending_up : Icons.trending_down, color: isUp ? Colors.green : Colors.red, size: 18),
                    const Spacer(),
                    Text(p['confidence'] as String, style: TextStyle(fontSize: 10, color: Colors.grey.shade500)),
                  ]),
                  const SizedBox(height: 4),
                  Text(p['type'] as String, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                  const Spacer(),
                  Text('${p['current']}', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                  Text('Prédit: ${p['predicted']}', style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                  Text('${(p['growth'] as double) > 0 ? '+' : ''}${p['growth']}%', style: TextStyle(fontSize: 12, color: isUp ? Colors.green : Colors.red)),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
