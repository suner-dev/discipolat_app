import 'package:flutter/material.dart';

class StreamingScreen extends StatelessWidget {
  const StreamingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Streaming & Live'),
        backgroundColor: Colors.purple.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Live indicator
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [Colors.red.shade400, Colors.red.shade700],
              ),
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Row(
              children: [
                CircleAvatar(
                  backgroundColor: Colors.white,
                  radius: 8,
                  child: Icon(Icons.circle, size: 12, color: Colors.red),
                ),
                SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('EN DIRECT', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                    Text('Prières du matin — 47 spectateurs', style: TextStyle(color: Colors.white70, fontSize: 12)),
                  ],
                ),
                Spacer(),
                Icon(Icons.play_circle, color: Colors.white, size: 32),
              ],
            ),
          ),
          const SizedBox(height: 16),
          // Stats
          Row(
            children: [
              _statCard(Icons.wifi, '1', 'En direct', Colors.red),
              const SizedBox(width: 12),
              _statCard(Icons.people, '47', 'Spectateurs', Colors.blue),
              const SizedBox(width: 12),
              _statCard(Icons.visibility, '128', 'Vues totales', Colors.green),
            ],
          ),
          const SizedBox(height: 20),
          const Text('Prochains streams', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          _streamCard('Culte du dimanche', 'Planifié — 30 août', Icons.video_library, Colors.purple),
          _streamCard('Étude biblique', 'Terminé — 20 août', Icons.check_circle, Colors.grey),
        ],
      ),
    );
  }

  Widget _statCard(IconData icon, String value, String label, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 4),
            Text(value, style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
            Text(label, style: TextStyle(fontSize: 10, color: color)),
          ],
        ),
      ),
    );
  }

  Widget _streamCard(String title, String subtitle, IconData icon, Color color) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: CircleAvatar(backgroundColor: color.withOpacity(0.1), child: Icon(icon, color: color)),
        title: Text(title),
        subtitle: Text(subtitle, style: const TextStyle(fontSize: 12)),
        trailing: const Icon(Icons.chevron_right),
      ),
    );
  }
}
