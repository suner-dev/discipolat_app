import 'package:flutter/material.dart';

/// P1 #50 — Journal de prière personnel
class PrayerJournalScreen extends StatefulWidget {
  const PrayerJournalScreen({super.key});
  @override
  State<PrayerJournalScreen> createState() => _PrayerJournalScreenState();
}

class _PrayerJournalScreenState extends State<PrayerJournalScreen> {
  String _filter = 'Toutes';

  final _prayers = [
    {'title': 'Guérison maman', 'status': 'En attente', 'date': '20 août', 'color': Colors.orange},
    {'title': 'Emploi David', 'status': 'Répondu!', 'date': '15 août', 'color': Colors.green},
    {'title': 'Sagesse leadership', 'status': 'En attente', 'date': '10 août', 'color': Colors.orange},
    {'title': 'Protection famille', 'status': 'Répondu!', 'date': '5 août', 'color': Colors.green},
    {'title': 'Réveil église', 'status': 'En attente', 'date': '1 août', 'color': Colors.orange},
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🙏 Journal de Prière'),
        backgroundColor: Colors.deepPurple.shade600,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showNewPrayerSheet(context),
        backgroundColor: Colors.deepPurple.shade600,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: Row(children: [
              _filterChip('Toutes'),
              _filterChip('En attente'),
              _filterChip('Répondues'),
            ]),
          ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              itemCount: _prayers.length,
              itemBuilder: (ctx, i) {
                final p = _prayers[i];
                final show = _filter == 'Toutes' || 
                    (_filter == 'En attente' && p['status'] == 'En attente') ||
                    (_filter == 'Répondues' && p['status'] == 'Répondu!');
                if (!show) return const SizedBox.shrink();
                return Card(child: ListTile(
                  leading: Icon(Icons.favorite, color: p['color'] as Color),
                  title: Text(p['title'].toString(), style: const TextStyle(fontWeight: FontWeight.bold)),
                  subtitle: Text(p['date'].toString()),
                  trailing: Chip(label: Text(p['status'].toString(), style: const TextStyle(fontSize: 11))),
                ));
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _filterChip(String label) {
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(label: Text(label), selected: _filter == label, onSelected: (_) => setState(() => _filter = label)),
    );
  }

  void _showNewPrayerSheet(BuildContext ctx) {
    showModalBottomSheet(
      context: ctx, isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.6, expand: false,
        builder: (ctx, _) => Padding(
          padding: const EdgeInsets.all(16),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            const Text('Nouvelle prière', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            const TextField(decoration: InputDecoration(labelText: 'Titre', border: OutlineInputBorder())),
            const SizedBox(height: 12),
            const TextField(maxLines: 3, decoration: InputDecoration(labelText: 'Détails de la prière', border: OutlineInputBorder())),
            const Spacer(),
            SizedBox(width: double.infinity, child: ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: Colors.deepPurple.shade600),
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Ajouter', style: TextStyle(color: Colors.white)),
            )),
          ]),
        ),
      ),
    );
  }
}
