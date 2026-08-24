import 'dart:convert';
import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// P19 — Observatoire de la Santé Spirituelle (mobile).
/// Affiche le score santé, tendance 6 mois, et scores par département.
class HealthObservatoryScreen extends StatefulWidget {
  const HealthObservatoryScreen({super.key});

  @override
  State<HealthObservatoryScreen> createState() => _HealthObservatoryScreenState();
}

class _HealthObservatoryScreenState extends State<HealthObservatoryScreen> {
  final _api = ApiService();
  Map<String, dynamic>? _observatory;
  Map<String, dynamic>? _trend;
  List<dynamic> _deptScores = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final obsRes = await _api.get('/health-observatory');
      final trendRes = await _api.get('/health-observatory/trend');
      final deptRes = await _api.get('/health-observatory/departments');
      setState(() {
        _observatory = obsRes.data;
        _trend = trendRes.data;
        _deptScores = deptRes.data as List<dynamic>;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('🏥 Observatoire Santé',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.tealAccent),
            onPressed: _loadData,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.tealAccent))
          : _error != null
              ? Center(child: Text('Erreur: $_error', style: const TextStyle(color: Colors.redAccent)))
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      _buildHealthScore(),
                      const SizedBox(height: 16),
                      _buildRiskDistribution(),
                      if (_trend != null) ...[
                        const SizedBox(height: 16),
                        _buildTrendCard(),
                      ],
                      if (_deptScores.isNotEmpty) ...[
                        const SizedBox(height: 16),
                        _buildDepartmentScores(),
                      ],
                      const SizedBox(height: 16),
                      _buildAtRiskSouls(),
                    ],
                  ),
                ),
    );
  }

  Widget _buildHealthScore() {
    final score = _observatory?['healthScore'] ?? 0;
    final color = score >= 70 ? Colors.green : score >= 45 ? Colors.amber : Colors.red;
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: color.withAlpha(15),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withAlpha(60)),
      ),
      child: Row(
        children: [
          SizedBox(
            width: 80,
            height: 80,
            child: Stack(
              alignment: Alignment.center,
              children: [
                CircularProgressIndicator(
                  value: score / 100,
                  strokeWidth: 8,
                  backgroundColor: Colors.white.withAlpha(15),
                  valueColor: AlwaysStoppedAnimation(color),
                ),
                Text('$score', style: TextStyle(
                    color: color, fontSize: 24, fontWeight: FontWeight.bold)),
              ],
            ),
          ),
          const SizedBox(width: 20),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Santé Pastorale', style: TextStyle(
                    color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text('${_observatory?['totalSouls'] ?? 0} âmes suivies',
                    style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 13)),
                Text('Prédiction : ${_observatory?['predictionHorizon'] ?? '2-3 semaines'}',
                    style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 11)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRiskDistribution() {
    return Row(
      children: [
        _riskTile('Critique', _observatory?['criticalCount'] ?? 0, Colors.red),
        _riskTile('Élevé', _observatory?['highCount'] ?? 0, Colors.orange),
        _riskTile('Moyen', _observatory?['mediumCount'] ?? 0, Colors.amber),
        _riskTile('Faible', _observatory?['lowCount'] ?? 0, Colors.green),
      ],
    );
  }

  Widget _riskTile(String label, int count, Color color) {
    return Expanded(
      child: Card(
        color: color.withAlpha(15),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Column(
            children: [
              Text('$count', style: TextStyle(
                  color: color, fontSize: 20, fontWeight: FontWeight.bold)),
              Text(label, style: TextStyle(
                  color: color.withAlpha(200), fontSize: 11)),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTrendCard() {
    final snapshots = _trend?['snapshots'] as List<dynamic>? ?? [];
    final trend = _trend?['trend'] ?? 'STABLE';
    final delta = _trend?['delta'] ?? 0;
    final trendColor = trend == 'AMÉLIORATION'
        ? Colors.green
        : trend == 'DÉGRADATION'
            ? Colors.red
            : Colors.blue;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.trending_up, color: trendColor, size: 20),
              const SizedBox(width: 8),
              Text('Tendance 6 mois',
                  style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
              const Spacer(),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: trendColor.withAlpha(20),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text('$trend ($delta)',
                    style: TextStyle(color: trendColor, fontSize: 12, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          // Mini bar chart of monthly health scores
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: snapshots.map((s) {
              final h = (s['healthScore'] ?? 0).toDouble();
              final barColor = h >= 70 ? Colors.green : h >= 45 ? Colors.amber : Colors.red;
              return Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 2),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.end,
                    children: [
                      Text('${h.toInt()}', style: TextStyle(color: barColor, fontSize: 9)),
                      const SizedBox(height: 2),
                      Container(
                        height: (h / 100) * 60,
                        decoration: BoxDecoration(
                          color: barColor.withAlpha(180),
                          borderRadius: BorderRadius.circular(3),
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        (s['month'] ?? '').toString().substring(5),
                        style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 9),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildDepartmentScores() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.business, color: Colors.cyanAccent, size: 20),
              SizedBox(width: 8),
              Text('Santé par département',
                  style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 12),
          ..._deptScores.map((d) {
            final score = d['healthScore'] ?? 0;
            final color = score >= 70 ? Colors.green : score >= 45 ? Colors.amber : Colors.red;
            return Padding(
              padding: const EdgeInsets.symmetric(vertical: 6),
              child: Row(
                children: [
                  Expanded(
                    flex: 3,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(d['departmentName'] ?? '',
                            style: const TextStyle(color: Colors.white, fontSize: 13)),
                        Text('${d['totalSouls'] ?? 0} âmes · ${d['atRiskCount'] ?? 0} à risque',
                            style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 11)),
                      ],
                    ),
                  ),
                  Expanded(
                    flex: 2,
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value: (score as int) / 100,
                        backgroundColor: Colors.white.withAlpha(15),
                        valueColor: AlwaysStoppedAnimation(color),
                        minHeight: 6,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text('$score', style: TextStyle(color: color, fontWeight: FontWeight.bold)),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildAtRiskSouls() {
    final souls = (_observatory?['soulsAtRisk'] as List<dynamic>?) ?? [];
    if (souls.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          color: Colors.green.withAlpha(10),
          borderRadius: BorderRadius.circular(16),
        ),
        child: const Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.check_circle, color: Colors.green, size: 24),
            SizedBox(width: 8),
            Text('Aucune âme à risque', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('⚠️ À risque (${souls.length})',
              style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          ...souls.take(10).map((s) {
            final riskScore = s['riskScore'] ?? 0;
            final color = riskScore >= 80 ? Colors.red : riskScore >= 55 ? Colors.orange : Colors.amber;
            return ListTile(
              dense: true,
              contentPadding: EdgeInsets.zero,
              leading: CircleAvatar(
                radius: 16,
                backgroundColor: color.withAlpha(30),
                child: Text('${riskScore}', style: TextStyle(color: color, fontSize: 12, fontWeight: FontWeight.bold)),
              ),
              title: Text(s['nom'] ?? '', style: const TextStyle(color: Colors.white, fontSize: 14)),
              subtitle: Text(s['intervention'] ?? '',
                  style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 11)),
            );
          }),
        ],
      ),
    );
  }
}
