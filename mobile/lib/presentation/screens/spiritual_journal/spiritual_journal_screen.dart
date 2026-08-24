import 'package:flutter/material.dart';

/// P1 #55 — Journal spirituel personnel: prières, réflexions, remerciements
class SpiritualJournalScreen extends StatefulWidget {
  const SpiritualJournalScreen({super.key});
  @override
  State<SpiritualJournalScreen> createState() => _SpiritualJournalScreenState();
}

class _SpiritualJournalScreenState extends State<SpiritualJournalScreen> {
  String _filter = 'Tous';
  final _filters = ['Tous', 'Prière', 'Réflexion', 'Remerciement', 'Louange', 'Leçon'];

  final _entries = [
    {'type': '🙏', 'title': 'Matin de prière', 'date': '24 août', 'fav': true},
    {'type': '💡', 'title': 'Réflexion sur la Parole', 'date': '23 août', 'fav': false},
    {'type': '🎉', 'title': 'Remerciement — Guérison maman', 'date': '22 août', 'fav': true},
    {'type': '🎵', 'title': 'Louange — Moment spécial', 'date': '21 août', 'fav': false},
    {'type': '📖', 'title': 'Leçon — Jean 3:16', 'date': '20 août', 'fav': false},
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📓 Journal Spirituel'),
        backgroundColor: Colors.purple.shade600,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.local_fire_department),
            onPressed: () {},
            tooltip: '🔥 Série: 5 jours',
          ),
        ],
      ),
      body: Column(
        children: [
          // Streak banner
          Container(
            padding: const EdgeInsets.all(12),
            color: Colors.purple.shade50,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.local_fire_department, color: Colors.orange),
                const SizedBox(width: 8),
                Text('🔥 Série de 5 jours consécutifs !',
                    style: TextStyle(color: Colors.purple.shade700, fontWeight: FontWeight.bold)),
              ],
            ),
          ),
          // Filter chips
          SizedBox(
            height: 50,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 12),
              children: _filters.map((f) => Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: ChoiceChip(
                  label: Text(f),
                  selected: _filter == f,
                  onSelected: (_) => setState(() => _filter = f),
                ),
              )).toList(),
            ),
          ),
          // Entries
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(12),
              itemCount: _entries.length,
              itemBuilder: (ctx, i) {
                final e = _entries[i];
                return Card(
                  child: ListTile(
                    leading: Text(e['type'].toString(), style: const TextStyle(fontSize: 28)),
                    title: Text(e['title'].toString(), style: const TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text(e['date'].toString()),
                    trailing: IconButton(
                      icon: Icon(
                        e['fav'] == true ? Icons.star : Icons.star_border,
                        color: Colors.amber,
                      ),
                      onPressed: () {},
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showNewEntrySheet(context),
        backgroundColor: Colors.purple.shade600,
        child: const Icon(Icons.add, color: Colors.white),
      ),
    );
  }

  void _showNewEntrySheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => DraggableScrollableSheet(
        initialChildSize: 0.7,
        minChildSize: 0.4,
        maxChildSize: 0.9,
        expand: false,
        builder: (ctx, controller) => Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Nouvelle entrée', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
              const SizedBox(height: 16),
              Wrap(
                spacing: 8,
                children: ['🙏 Prière', '💡 Réflexion', '🎉 Remerciement', '🎵 Louange', '📖 Leçon']
                    .map((t) => ChoiceChip(label: Text(t), selected: false, onSelected: (_) {}))
                    .toList(),
              ),
              const SizedBox(height: 12),
              const TextField(maxLines: 1, decoration: InputDecoration(labelText: 'Titre', border: OutlineInputBorder())),
              const SizedBox(height: 12),
              const TextField(maxLines: 5, decoration: InputDecoration(labelText: 'Contenu', border: OutlineInputBorder())),
              const Spacer(),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  style: ElevatedButton.styleFrom(backgroundColor: Colors.purple.shade600),
                  onPressed: () => Navigator.pop(ctx),
                  child: const Text('Enregistrer', style: TextStyle(color: Colors.white)),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
