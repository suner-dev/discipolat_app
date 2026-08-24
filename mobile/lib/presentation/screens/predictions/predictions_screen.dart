import 'package:flutter/material.dart';

/// P1 #63 — Prédictions effectifs et engagement (séries historiques → ML)
class PredictionsScreen extends StatelessWidget {
  const PredictionsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('🔮 Prédictions ML'),
        backgroundColor: Colors.violet.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Model overview
          Card(
            color: Colors.violet.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Modèle prédictif', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 8),
                _predRow('Effectifs dans 6 mois', '198 (+27%)', Colors.green),
                _predRow('Baptêmes prévus', '12', Colors.blue),
                _predRow('Risque décrochage', '8 membres', Colors.orange),
                _predRow('Présence prédite', '82%', Colors.green),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Prédictions par département', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _deptPred('Louange', 18, 22, Colors.green),
          _deptPred('Accueil', 25, 28, Colors.green),
          _deptPred('Jeunesse', 32, 29, Colors.red),
          _deptPred('Enfants', 15, 17, Colors.green),
          _deptPred('Technique', 8, 10, Colors.green),
          const SizedBox(height: 16),
          // Confidence
          const Text('Confiance du modèle', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Card(child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const LinearProgressIndicator(value: 0.78),
              const SizedBox(height: 4),
              const Text('78% de confiance — basé sur 12 mois de données'),
            ]),
          )),
        ],
      ),
    );
  }

  Widget _predRow(String label, String value, Color color) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
        Text(label),
        Text(value, style: TextStyle(fontWeight: FontWeight.bold, color: color)),
      ]),
    );
  }

  Widget _deptPred(String name, int current, int predicted, Color color) {
    return Card(
      child: ListTile(
        title: Text(name),
        subtitle: Text('Actuel: $current'),
        trailing: Row(mainAxisSize: MainAxisSize.min, children: [
          Text('→ $predicted', style: TextStyle(fontWeight: FontWeight.bold, color: color, fontSize: 16)),
          const SizedBox(width: 8),
          Icon(predicted > current ? Icons.trending_up : Icons.trending_down, color: color, size: 18),
        ]),
      ),
    );
  }
}
