import 'package:flutter/material.dart';

/// P1 #41 — Réunion de famille automatisée (ordre du jour auto)
class FamilyMeetingScreen extends StatelessWidget {
  const FamilyMeetingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🏠 Réunion de Famille'),
        backgroundColor: Colors.green.shade700,
        foregroundColor: Colors.white,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {},
        backgroundColor: Colors.green.shade700,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Next meeting
          Card(
            color: Colors.green.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Prochaine réunion', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 4),
                const Text('Dimanche 31 août 2025, 17h00', style: TextStyle(color: Colors.grey)),
                const SizedBox(height: 12),
                const Text('Ordre du jour généré par IA:', style: TextStyle(fontWeight: FontWeight.w500)),
                const SizedBox(height: 4),
                _agendaItem(1, 'Alertes: 2 membres en décrochage'),
                _agendaItem(2, 'Rapports en attente: 3'),
                _agendaItem(3, 'Événements à venir: 2 cette semaine'),
                _agendaItem(4, 'Témoignage: Famille Grâce'),
                _agendaItem(5, 'Prière collective'),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Past meetings
          const Text('Réunions précédentes', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _pastMeeting('24 août 2025', '8 participants', 'Cohésion: 8/10'),
          _pastMeeting('17 août 2025', '6 participants', 'Cohésion: 7/10'),
          _pastMeeting('10 août 2025', '10 participants', 'Cohésion: 9/10'),
        ],
      ),
    );
  }

  Widget _agendaItem(int num, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(children: [
        CircleAvatar(radius: 10, backgroundColor: Colors.green.shade100, child: Text('$num', style: TextStyle(fontSize: 10, color: Colors.green.shade700))),
        const SizedBox(width: 8),
        Expanded(child: Text(text, style: const TextStyle(fontSize: 13))),
      ]),
    );
  }

  Widget _pastMeeting(String date, String participants, String cohesion) {
    return Card(child: ListTile(
      leading: const Icon(Icons.check_circle, color: Colors.green),
      title: Text(date),
      subtitle: Text('$participants • $cohesion'),
      trailing: const Icon(Icons.chevron_right),
    ));
  }
}
