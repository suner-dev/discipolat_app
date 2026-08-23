import 'package:flutter/material.dart';

class RewardsScreen extends StatelessWidget {
  const RewardsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Récompenses'),
        backgroundColor: Colors.amber.shade600,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Points card
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: LinearGradient(colors: [Colors.amber.shade300, Colors.amber.shade600]),
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Row(
              children: [
                Text('⭐', style: TextStyle(fontSize: 40)),
                SizedBox(width: 16),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Mes points', style: TextStyle(color: Colors.white70, fontSize: 12)),
                    Text('1 250', style: TextStyle(color: Colors.white, fontSize: 28, fontWeight: FontWeight.bold)),
                  ],
                ),
                Spacer(),
                Column(
                  children: [
                    Text('2', style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                    Text('Obtenues', style: TextStyle(color: Colors.white70, fontSize: 10)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),
          const Text('Récompenses disponibles', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          _rewardCard('⭐', 'Fidèle Inébranlable', '500 pts', true),
          _rewardCard('🔥', 'Évangéliste', '1 000 pts', true),
          _rewardCard('📜', 'Mentor d\'excellence', '2 000 pts', false),
          _rewardCard('🪑', 'Siège VIP', '300 pts', false),
          _rewardCard('👕', 'T-shirt Discipolat', '1 500 pts', false),
          _rewardCard('🏆', 'Parrain d\'or', '800 pts', false),
        ],
      ),
    );
  }

  Widget _rewardCard(String icon, String name, String points, bool claimed) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Text(icon, style: const TextStyle(fontSize: 28)),
        title: Text(name, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Text(points, style: TextStyle(color: Colors.amber.shade600, fontWeight: FontWeight.bold)),
        trailing: claimed
            ? Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: Colors.green.shade50,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text('Obtenu', style: TextStyle(fontSize: 11, color: Colors.green)),
              )
            : Icon(Icons.lock_outline, color: Colors.grey.shade400, size: 20),
      ),
    );
  }
}
