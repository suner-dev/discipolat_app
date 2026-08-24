import 'package:flutter/material.dart';

/// P1 #44 — Projection de croissance IA
class GrowthProjectionScreen extends StatelessWidget {
  const GrowthProjectionScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📊 Projection de Croissance'),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Current vs projected
          Card(
            color: Colors.teal.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Projection 12 mois', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceAround,
                    children: [
                      _projColumn('Aujourd\'hui', '156', Colors.teal),
                      const Icon(Icons.arrow_forward, color: Colors.teal),
                      _projColumn('Projeté', '198', Colors.green),
                    ],
                  ),
                  const SizedBox(height: 12),
                  const LinearProgressIndicator(value: 0.79),
                  const SizedBox(height: 4),
                  const Text('+27% de croissance prévue', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Simulation
          const Text('Simulateur', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  _simRow('Conversions/mois', '3'),
                  _simRow('Baptêmes/mois', '2'),
                  _simRow('Transferts entrants', '1'),
                  _simRow('Retraits/mois', '-1'),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(backgroundColor: Colors.teal.shade700),
                      onPressed: () {},
                      icon: const Icon(Icons.play_arrow, color: Colors.white),
                      label: const Text('Simuler', style: TextStyle(color: Colors.white)),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Family breakdown
          const Text('Par famille', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _familyRow('Famille Grâce', 12, '+3', Colors.green),
          _familyRow('Famille Espoir', 8, '+1', Colors.blue),
          _familyRow('Famille Paix', 15, '+4', Colors.green),
          _familyRow('Famille Joie', 10, '-1', Colors.red),
        ],
      ),
    );
  }

  Widget _projColumn(String label, String value, Color color) {
    return Column(
      children: [
        Text(value, style: TextStyle(fontSize: 32, fontWeight: FontWeight.bold, color: color)),
        Text(label, style: const TextStyle(fontSize: 12)),
      ],
    );
  }

  Widget _simRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label),
          SizedBox(
            width: 80,
            child: TextField(
              controller: TextEditingController(text: value),
              textAlign: TextAlign.center,
              decoration: const InputDecoration(isDense: true, border: OutlineInputBorder()),
            ),
          ),
        ],
      ),
    );
  }

  Widget _familyRow(String name, int current, String delta, Color deltaColor) {
    return Card(
      child: ListTile(
        title: Text(name),
        subtitle: Text('Effectif: $current'),
        trailing: Text(delta, style: TextStyle(fontWeight: FontWeight.bold, color: deltaColor, fontSize: 16)),
      ),
    );
  }
}
