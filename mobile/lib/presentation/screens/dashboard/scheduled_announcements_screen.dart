import 'package:flutter/material.dart';

class ScheduledAnnouncementsScreen extends StatelessWidget {
  const ScheduledAnnouncementsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final announcements = [
      {'title': 'Culte spécial de prière - Samedi 30 Août', 'status': 'PUBLISHED', 'target': 'ALL', 'pinned': true},
      {'title': 'Réunion des responsables', 'status': 'SCHEDULED', 'target': 'DEPARTMENT', 'pinned': false},
      {'title': 'Rappel: Don mensuel', 'status': 'DRAFT', 'target': 'ALL', 'pinned': false},
    ];

    Color statusColor(String s) {
      switch (s) {
        case 'PUBLISHED': return Colors.green;
        case 'SCHEDULED': return Colors.blue;
        case 'DRAFT': return Colors.grey;
        default: return Colors.red;
      }
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Annonces programmées'), backgroundColor: Colors.orange.shade600, foregroundColor: Colors.white),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.orange,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: announcements.length,
        itemBuilder: (context, i) {
          final a = announcements[i];
          final color = statusColor(a['status'] as String);
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            child: ListTile(
              leading: (a['pinned'] as bool) ? const Text('📌', style: TextStyle(fontSize: 24)) : const Icon(Icons.campaign, color: Colors.orange),
              title: Text(a['title'] as String, style: const TextStyle(fontWeight: FontWeight.bold)),
              subtitle: Row(children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(color: color.withOpacity(0.15), borderRadius: BorderRadius.circular(8)),
                  child: Text(a['status'] as String, style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold)),
                ),
                const SizedBox(width: 8),
                Text('Cible: ${a['target']}', style: TextStyle(fontSize: 11, color: Colors.grey.shade500)),
              ]),
            ),
          );
        },
      ),
    );
  }
}
