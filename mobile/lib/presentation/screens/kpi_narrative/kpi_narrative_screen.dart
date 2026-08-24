import 'package:flutter/material.dart';

/// P1 #46 — Drill-down narratif sur KPI
class KpiNarrativeScreen extends StatelessWidget {
  const KpiNarrativeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('📖 KPI Narratif'),
        backgroundColor: Colors.cyan.shade700,
        foregroundColor: Colors.white,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Main KPI narrative
          Card(
            color: Colors.cyan.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                const Text('Taux de présence', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                const SizedBox(height: 4),
                const Text('78% — +2% vs mois dernier', style: TextStyle(color: Colors.grey)),
                const SizedBox(height: 12),
                const Text(
                  'Le taux de présence a augmenté de 2% ce mois, porté principalement par le département '
                  'Accueil (+8%). Cependant, le département Jeunesse a connu une baisse de 12%, '
                  'concentrée le dimanche soir. La tendance sur 3 mois est positive (+5% global).',
                  style: TextStyle(fontSize: 14, height: 1.5),
                ),
                const SizedBox(height: 8),
                const Text('💡 Recommandation: Organiser un événement jeune-specific le dimanche soir.',
                    style: TextStyle(color: Colors.cyan, fontWeight: FontWeight.w500)),
              ]),
            ),
          ),
          const SizedBox(height: 16),
          // Drill-down categories
          const Text('Explorer par catégorie', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _drillItem('Présence par département', Icons.business, '7 départs analysés'),
          _drillItem('Présence par tranche d\'âge', Icons.group, '5 tranches analysées'),
          _drillItem('Présence par jour', Icons.calendar_today, '7 jours analysés'),
          _drillItem('Conversions par famille', Icons.family_restroom, '4 familles analysées'),
          const SizedBox(height: 16),
          // Narratives list
          const Text('Autres récits', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          _narrativeCard('Conversions', '+23% ce trimestre', 'Les conversions proviennent majoritairement des invitations personnelles (68%). Le pipeline d\'évangélisation est efficace.'),
          _narrativeCard('Générosité', '+8% ce mois', 'La générosité suit la tendance saisonnière. Les campagnes ciblées ont un impact mesurable.'),
          _narrativeCard('Rétention', '85% — stable', 'La rétention est stable. Les nouveaux membres qui restent > 3 mois ont 90% de chances de rester.'),
        ],
      ),
    );
  }

  Widget _drillItem(String title, IconData icon, String detail) {
    return Card(child: ListTile(
      leading: Icon(icon, color: Colors.cyan),
      title: Text(title),
      subtitle: Text(detail, style: const TextStyle(fontSize: 12)),
      trailing: const Icon(Icons.chevron_right),
    ));
  }

  Widget _narrativeCard(String title, String trend, String narrative) {
    return Card(child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
          const Spacer(),
          Text(trend, style: const TextStyle(color: Colors.green, fontWeight: FontWeight.w500)),
        ]),
        const SizedBox(height: 8),
        Text(narrative, style: const TextStyle(fontSize: 13, height: 1.4)),
      ]),
    ));
  }
}
