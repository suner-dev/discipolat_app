import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// P3 #102 — Prédiction de charge (pics d'activité) sur 8 semaines.
class LoadPredictionScreen extends StatefulWidget {
  const LoadPredictionScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<LoadPredictionScreen> createState() => _LoadPredictionScreenState();
}

class _LoadPredictionScreenState extends State<LoadPredictionScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _data;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final res = await _api.get('/load-prediction');
      setState(() { _data = (res.data as Map<String, dynamic>?); _loading = false; });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  MaterialColor _levelColor(String niveau) {
    switch (niveau) {
      case 'CRITIQUE':
        return Colors.red;
      case 'ELEVE':
        return Colors.orange;
      case 'FAIBLE':
        return Colors.grey;
      default:
        return Colors.teal;
    }
  }

  @override
  Widget build(BuildContext context) {
    final semaines = (_data?['semaines'] as List<dynamic>?) ?? [];
    return Scaffold(
      appBar: AppBar(
        title: const Text('📊 Prédiction de charge'),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Card(
                    color: Colors.teal.shade50,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text('Charge de base hebdo : ${_data?['baselineChargeHebdo'] ?? '—'}', style: const TextStyle(fontWeight: FontWeight.bold)),
                        Text('Inscriptions moy./sem : ${_data?['moyenneInscriptionsHebdo'] ?? '—'}'),
                        Text('Rapports moy./sem : ${_data?['moyenneRapportsHebdo'] ?? '—'}'),
                        const SizedBox(height: 8),
                        Text('Jours forts : ${(_joursForts(_data)).join(', ')}', style: const TextStyle(fontSize: 12, color: Colors.black54)),
                      ]),
                    ),
                  ),
                  if (semaines.isEmpty)
                    const Padding(padding: EdgeInsets.all(24), child: Center(child: Text('Aucune prédiction disponible')))
                  else
                    ...semaines.map((w) {
                      final week = w as Map<String, dynamic>;
                      final niveau = week['niveau']?.toString() ?? 'NORMAL';
                      final color = _levelColor(niveau);
                      return Card(
                        margin: const EdgeInsets.only(top: 8),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                        child: Padding(
                          padding: const EdgeInsets.all(14),
                          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                              Expanded(child: Text('Semaine ${week['semaine']} (${week['debut']} → ${week['fin']})', style: const TextStyle(fontWeight: FontWeight.bold))),
                              Chip(label: Text(niveau, style: const TextStyle(fontSize: 11, color: Colors.white)), backgroundColor: color),
                            ]),
                            const SizedBox(height: 6),
                            LinearProgressIndicator(value: ((week['chargeEstimee'] as num?)?.toDouble() ?? 0) / 100, color: color, backgroundColor: Colors.grey.shade200),
                            const SizedBox(height: 6),
                            Text('Charge estimée : ${week['chargeEstimee']} • ${week['evenementsPlanifies']} événement(s)', style: const TextStyle(fontSize: 12)),
                            if (week['recommandation'] != null)
                              Padding(
                                padding: const EdgeInsets.only(top: 4),
                                child: Text(week['recommandation'].toString(), style: TextStyle(fontSize: 11, fontStyle: FontStyle.italic, color: color.shade700)),
                              ),
                          ]),
                        ),
                      );
                    }),
                ],
              ),
            ),
    );
  }

  static List<String> _joursForts(Map<String, dynamic>? data) {
    final jf = data?['joursForts'];
    return jf is List ? jf.map((e) => e.toString()).toList() : [];
  }
}
