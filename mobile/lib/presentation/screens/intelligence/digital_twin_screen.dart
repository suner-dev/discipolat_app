import 'dart:convert';
import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// P17 — Jumeau Numérique de l'Église (mobile).
/// Simulations what-if de croissance avec scénarios prédéfinis.
class DigitalTwinScreen extends StatefulWidget {
  const DigitalTwinScreen({super.key});

  @override
  State<DigitalTwinScreen> createState() => _DigitalTwinScreenState();
}

class _DigitalTwinScreenState extends State<DigitalTwinScreen> {
  final _api = ApiService();
  Map<String, dynamic>? _result;
  bool _isLoading = false;

  // Simulation parameters
  double _faiseurMultiplier = 1.5;
  int _retentionGain = 10;
  double _pipelineBoost = 1.2;
  int _months = 12;

  @override
  void initState() {
    super.initState();
    _runSimulation();
  }

  Future<void> _runSimulation() async {
    setState(() => _isLoading = true);
    try {
      final res = await _api.post('/twin/simulate', data: {
        'faiseurMultiplier': _faiseurMultiplier,
        'retentionGainPercent': _retentionGain,
        'pipelineBoost': _pipelineBoost,
        'months': _months,
      });
      setState(() {
        _result = res.data;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('🔮 Jumeau Numérique',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.cyanAccent),
            onPressed: _runSimulation,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : RefreshIndicator(
              onRefresh: _runSimulation,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  // Scenario presets
                  _buildScenarioPresets(),
                  const SizedBox(height: 16),
                  // Parameters
                  _buildParameters(),
                  const SizedBox(height: 16),
                  // Results
                  if (_result != null) ...[
                    _buildProjectedStats(),
                    const SizedBox(height: 16),
                    _buildProjectionChart(),
                    const SizedBox(height: 16),
                    _buildRecommendation(),
                  ],
                ],
              ),
            ),
    );
  }

  Widget _buildScenarioPresets() {
    final scenarios = [
      ('Stagnation', 1.0, 0, 1.0, Colors.grey),
      ('Croissance douce', 1.3, 5, 1.1, Colors.green),
      ('Réveil faiseurs', 2.0, 0, 1.0, Colors.blue),
      ('Réveil + rétention', 1.8, 15, 1.15, Colors.purple),
      ('Réveil spirituel', 2.5, 20, 1.4, Colors.orange),
    ];

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('⚡ Scénarios rapides',
              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: scenarios.map((s) {
              final (label, fm, rg, pb, color) = s;
              return GestureDetector(
                onTap: () {
                  setState(() {
                    _faiseurMultiplier = fm;
                    _retentionGain = rg;
                    _pipelineBoost = pb;
                  });
                  _runSimulation();
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: color.withAlpha(20),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: color.withAlpha(60)),
                  ),
                  child: Text(label, style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.bold)),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildParameters() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('⚙️ Paramètres', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          _sliderParam('Multiplicateur faiseurs', _faiseurMultiplier, 0.5, 5.0, (v) => _faiseurMultiplier = v),
          _sliderParam('Gain rétention (%)', _retentionGain.toDouble(), 0, 80, (v) => _retentionGain = v.toInt()),
          _sliderParam('Boost pipeline', _pipelineBoost, 0.5, 5.0, (v) => _pipelineBoost = v),
          Row(
            children: [
              const Text('Horizon', style: TextStyle(color: Colors.white70, fontSize: 13)),
              const Spacer(),
              ...[6, 12, 24, 36].map((m) => Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: GestureDetector(
                  onTap: () { setState(() => _months = m); _runSimulation(); },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: _months == m ? Colors.cyanAccent.withAlpha(30) : Colors.white.withAlpha(10),
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: _months == m ? Colors.cyanAccent : Colors.white.withAlpha(30)),
                    ),
                    child: Text('${m}m', style: TextStyle(
                        color: _months == m ? Colors.cyanAccent : Colors.white70,
                        fontSize: 12, fontWeight: FontWeight.bold)),
                  ),
                ),
              )),
            ],
          ),
        ],
      ),
    );
  }

  Widget _sliderParam(String label, double value, double min, double max, ValueChanged<double> onChanged) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          SizedBox(width: 130, child: Text(label, style: const TextStyle(color: Colors.white70, fontSize: 12))),
          Expanded(
            child: Slider(
              value: value.clamp(min, max),
              min: min, max: max,
              activeColor: Colors.cyanAccent,
              inactiveColor: Colors.white.withAlpha(30),
              onChanged: (v) { setState(() => onChanged(v)); },
              onChangeEnd: (_) => _runSimulation(),
            ),
          ),
          SizedBox(width: 40, child: Text(
              value == value.toInt().toDouble() ? '${value.toInt()}' : value.toStringAsFixed(1),
              style: const TextStyle(color: Colors.cyanAccent, fontSize: 12, fontWeight: FontWeight.bold),
              textAlign: TextAlign.right)),
        ],
      ),
    );
  }

  Widget _buildProjectedStats() {
    final projected = _result?['projectedTotal'] ?? 0;
    final growth = _result?['growthPercent'] ?? 0;
    final leaders = _result?['neededLeaders'] ?? 0;
    final currentLeaders = _result?['currentLeaders'] ?? 0;
    final gap = _result?['leaderGap'] ?? 0;

    return Row(
      children: [
        _statCard('Projeté', '$projected', 'âmes', Colors.cyanAccent),
        _statCard('Croissance', '+$growth%', '', Colors.green),
        _statCard('Leaders nécessaires', '$leaders', '(${gap > 0 ? '+$gap manquants' : 'suffisant'})',
            gap > 0 ? Colors.orange : Colors.green),
      ],
    );
  }

  Widget _statCard(String label, String value, String sub, Color color) {
    return Expanded(
      child: Card(
        color: color.withAlpha(15),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            children: [
              Text(value, style: TextStyle(color: color, fontSize: 20, fontWeight: FontWeight.bold)),
              Text(label, style: TextStyle(color: color.withAlpha(200), fontSize: 10)),
              if (sub.isNotEmpty) Text(sub, style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 9)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildProjectionChart() {
    final projection = _result?['projection'] as List<dynamic>? ?? [];
    if (projection.isEmpty) return const SizedBox.shrink();

    final maxVal = projection.map((p) => (p['souls'] as num).toDouble()).reduce((a, b) => a > b ? a : b);
    final base = (_result?['baseline']?['totalSouls'] ?? 1).toDouble();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('📈 Projection mois par mois',
              style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          SizedBox(
            height: 150,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: projection.map((p) {
                final souls = (p['souls'] as num).toDouble();
                final pct = maxVal > 0 ? souls / maxVal : 0;
                final color = souls >= base ? Colors.green : Colors.red;
                return Expanded(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 1),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        if (p['month'] % (_months > 18 ? 6 : 3) == 0 || p['month'] == projection.length)
                          Text('${souls.toInt()}', style: TextStyle(color: color, fontSize: 8)),
                        const SizedBox(height: 2),
                        Container(
                          height: (pct * 120).clamp(4.0, 120.0),
                          decoration: BoxDecoration(
                            color: color.withAlpha(180),
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Mois 1', style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 10)),
              Text('Mois $_months', style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 10)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendation() {
    final rec = _result?['recommendation'] ?? '';
    if (rec.toString().isEmpty) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.cyanAccent.withAlpha(10),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.cyanAccent.withAlpha(40)),
      ),
      child: Row(
        children: [
          const Icon(Icons.lightbulb, color: Colors.cyanAccent, size: 24),
          const SizedBox(width: 12),
          Expanded(
            child: Text(rec, style: const TextStyle(color: Colors.white, fontSize: 13)),
          ),
        ],
      ),
    );
  }
}
