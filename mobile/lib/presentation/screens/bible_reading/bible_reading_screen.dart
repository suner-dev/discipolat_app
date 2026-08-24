import 'package:flutter/material.dart';

/// P1 #49/#60 — Plan de lecture biblique partagé
class BibleReadingScreen extends StatelessWidget {
  const BibleReadingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📖 Plan de Lecture Biblique'),
        backgroundColor: Colors.brown.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Progress
          Card(
            color: Colors.brown.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Mon progression', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 8),
                const LinearProgressIndicator(value: 0.65),
                const SizedBox(height: 4),
                const Text('65% — Jour 236/365'),
                const SizedBox(height: 8),
                const Text('🔥 Série: 12 jours consécutifs'),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Today's reading
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Lecture du jour', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 8),
                const Text('Jean 3:16-21', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w500)),
                const SizedBox(height: 4),
                const Text('Évangile — Thème: Amour de Dieu'),
                const SizedBox(height: 12),
                Row(children: [
                  OutlinedButton.icon(onPressed: () {}, icon: const Icon(Icons.check, size: 16), label: const Text('Marquer lu')),
                  const SizedBox(width: 8),
                  OutlinedButton.icon(onPressed: () {}, icon: const Icon(Icons.note_add, size: 16), label: const Text('Ajouter note')),
                ]),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Plans available
          const Text('Plans disponibles', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _planCard('Parcours 365 jours', 'Bible entière en 1 an', 0.65, Colors.green),
          _planCard('Psaumes & Proverbes', 'Sagesse quotidienne', 0.30, Colors.blue),
          _planCard('Évangiles', 'Matthieu → Jean', 0.80, Colors.teal),
          const SizedBox(height: 16),
          // Family sharing
          const Text('Partagé avec ma famille', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _memberProgress('Jean-Pierre', 0.72),
          _memberProgress('Marie', 0.58),
          _memberProgress('David', 0.45),
        ],
      ),
    );
  }

  Widget _planCard(String title, String desc, double progress, Color color) {
    return Card(child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(child: Text(title, style: const TextStyle(fontWeight: FontWeight.bold))),
          Text('${(progress * 100).toInt()}%', style: TextStyle(color: color, fontWeight: FontWeight.bold)),
        ]),
        Text(desc, style: const TextStyle(fontSize: 12, color: Colors.grey)),
        const SizedBox(height: 8),
        LinearProgressIndicator(value: progress, color: color),
      ]),
    ));
  }

  Widget _memberProgress(String name, double progress) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(children: [
        SizedBox(width: 80, child: Text(name, style: const TextStyle(fontSize: 13))),
        Expanded(child: LinearProgressIndicator(value: progress)),
        const SizedBox(width: 8),
        Text('${(progress * 100).toInt()}%', style: const TextStyle(fontSize: 12)),
      ]),
    );
  }
}
