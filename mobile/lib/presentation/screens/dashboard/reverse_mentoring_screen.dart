import 'package:flutter/material.dart';

class ReverseMentoringScreen extends StatelessWidget {
  const ReverseMentoringScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final requests = [
      {'requester': 'Chef Famille Ngono', 'topic': 'PASTORAL_CARE', 'status': 'PENDING', 'urgency': 4, 'desc': 'Conseil pour membre en difficulté'},
      {'requester': 'Faiseur Kotto', 'topic': 'LEADERSHIP', 'status': 'IN_PROGRESS', 'urgency': 3, 'desc': 'Gestion conflit dans le groupe', 'mentor': 'Pasteur Pierre'},
      {'requester': 'Responsable Marie', 'topic': 'FAMILY_ISSUE', 'status': 'COMPLETED', 'urgency': 5, 'desc': 'Séparation au sein d\'une famille'},
    ];

    Color statusColor(String s) {
      switch (s) {
        case 'COMPLETED': return Colors.green;
        case 'IN_PROGRESS': return Colors.blue;
        default: return Colors.orange;
      }
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Mentorat inversé'), backgroundColor: Colors.pink.shade600, foregroundColor: Colors.white),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.pink,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: requests.length,
        itemBuilder: (context, i) {
          final r = requests[i];
          final color = statusColor(r['status'] as String);
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(children: [
                    Text(r['requester'] as String, style: const TextStyle(fontWeight: FontWeight.bold)),
                    const Spacer(),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                      decoration: BoxDecoration(color: color.withOpacity(0.15), borderRadius: BorderRadius.circular(8)),
                      child: Text(r['status'] as String, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
                    ),
                  ]),
                  const SizedBox(height: 4),
                  Text(r['desc'] as String, style: TextStyle(color: Colors.grey.shade600, fontSize: 13)),
                  const SizedBox(height: 4),
                  Row(children: [
                    Row(children: List.generate(5, (j) => Container(
                      width: 8, height: 8, margin: const EdgeInsets.only(right: 2),
                      decoration: BoxDecoration(shape: BoxShape.circle, color: j < (r['urgency'] as int) ? Colors.red : Colors.grey.shade300),
                    ))),
                    const Spacer(),
                    if (r.containsKey('mentor')) Text('→ ${r['mentor']}', style: const TextStyle(color: Colors.blue, fontSize: 12)),
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
