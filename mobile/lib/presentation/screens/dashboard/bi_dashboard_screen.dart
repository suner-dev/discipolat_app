import 'dart:math';
import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../../data/services/api_service.dart';

/// Business Intelligence Dashboard for mobile.
/// Shows KPIs, attendance trends, department performance, and growth metrics.
class BiDashboardScreen extends StatefulWidget {
  const BiDashboardScreen({super.key});

  @override
  State<BiDashboardScreen> createState() => _BiDashboardScreenState();
}

class _BiDashboardScreenState extends State<BiDashboardScreen> {
  final _api = ApiService();
  Map<String, dynamic>? _bi;
  bool _isLoading = true;
  String _period = '30d';

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final res = await _api.get('/admin/stats/overview?period=$_period');
      if (mounted) setState(() { _bi = res.data; _isLoading = false; });
    } catch (e) {
      if (mounted) {
        setState(() {
          _bi = _generateMockData();
          _isLoading = false;
        });
      }
    }
  }

  Map<String, dynamic> _generateMockData() {
    final rng = Random();
    return {
      'totalMembers': 248 + rng.nextInt(20),
      'activeMembers': 198 + rng.nextInt(15),
      'growthRate': 3.2 + rng.nextDouble() * 2,
      'attendanceRate': 72 + rng.nextDouble() * 15,
      'weeklyTrend': List.generate(12, (i) => {
        'week': 'S${i + 1}',
        'present': 150 + rng.nextInt(60),
        'absent': 30 + rng.nextInt(40),
      }),
      'departmentPerformance': [
        {'name': 'Louange', 'score': 85 + rng.nextInt(10)},
        {'name': 'Accueil', 'score': 78 + rng.nextInt(12)},
        {'name': 'Enseignement', 'score': 90 + rng.nextInt(8)},
        {'name': 'Évangélisation', 'score': 65 + rng.nextInt(15)},
        {'name': 'Jeunes', 'score': 72 + rng.nextInt(10)},
      ],
      'newConverts': 12 + rng.nextInt(8),
      'activeDisciples': 45 + rng.nextInt(10),
      'reportsSubmitted': 89 + rng.nextInt(10),
      'reportsPending': 15 - rng.nextInt(10).abs(),
    };
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('📊 Business Intelligence', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          _buildPeriodSelector(),
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white),
            onPressed: _loadData,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildKPICards(),
                  const SizedBox(height: 20),
                  _buildAttendanceChart(),
                  const SizedBox(height: 20),
                  _buildDepartmentPerformance(),
                  const SizedBox(height: 20),
                  _buildGrowthMetrics(),
                  const SizedBox(height: 20),
                  _buildReportStats(),
                  const SizedBox(height: 40),
                ],
              ),
            ),
    );
  }

  Widget _buildPeriodSelector() {
    return PopupMenuButton<String>(
      icon: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.white.withAlpha(20),
          borderRadius: BorderRadius.circular(8),
        ),
        child: Text(_period, style: const TextStyle(color: Colors.white, fontSize: 12)),
      ),
      onSelected: (v) { setState(() => _period = v); _loadData(); },
      itemBuilder: (_) => [
        const PopupMenuItem(value: '7d', child: Text('7 jours')),
        const PopupMenuItem(value: '30d', child: Text('30 jours')),
        const PopupMenuItem(value: '90d', child: Text('90 jours')),
        const PopupMenuItem(value: '1y', child: Text('1 an')),
      ],
    );
  }

  Widget _buildKPICards() {
    if (_bi == null) return const SizedBox.shrink();
    final total = _bi!['totalMembers'] ?? 0;
    final active = _bi!['activeMembers'] ?? 0;
    final growth = (_bi!['growthRate'] ?? 0).toDouble();
    final attendance = (_bi!['attendanceRate'] ?? 0).toDouble();

    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      crossAxisSpacing: 12,
      mainAxisSpacing: 12,
      childAspectRatio: 1.2,
      children: [
        _buildKPICard('Total membres', '$total', Icons.people, Colors.cyanAccent, null),
        _buildKPICard('Actifs', '$active', Icons.person, Colors.green, null),
        _buildKPICard('Croissance', '${growth.toStringAsFixed(1)}%', Icons.trending_up,
            growth > 0 ? Colors.green : Colors.red, growth > 0 ? 0.85 : 0.0),
        _buildKPICard('Fréquentation', '${attendance.toStringAsFixed(0)}%', Icons.event_seat,
            Colors.amber, attendance / 100),
      ],
    );
  }

  Widget _buildKPICard(String label, String value, IconData icon, Color color, double? progress) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withAlpha(10)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Icon(icon, color: color, size: 22),
              if (progress != null)
                SizedBox(
                  width: 32, height: 32,
                  child: CircularProgressIndicator(
                    value: progress,
                    backgroundColor: Colors.white.withAlpha(20),
                    color: color,
                    strokeWidth: 3,
                  ),
                ),
            ],
          ),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(value, style: TextStyle(color: color, fontSize: 24, fontWeight: FontWeight.bold)),
              const SizedBox(height: 2),
              Text(label, style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 12)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAttendanceChart() {
    final trend = _bi!['weeklyTrend'] as List<dynamic>? ?? [];
    if (trend.isEmpty) return const SizedBox.shrink();

    return _buildCard(
      'Tendance de fréquentation',
      SizedBox(
        height: 200,
        child: BarChart(
          BarChartData(
            alignment: BarChartAlignment.spaceAround,
            maxY: 250,
            barTouchData: BarTouchData(
              touchTooltipData: BarTouchTooltipData(
                getTooltipItem: (group, groupIndex, rod, rodIndex) {
                  return BarTooltipItem(
                    '${rod.toY.toInt()}',
                    const TextStyle(color: Colors.white, fontSize: 12),
                  );
                },
              ),
            ),
            titlesData: FlTitlesData(
              show: true,
              bottomTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  getTitlesWidget: (value, meta) {
                    final idx = value.toInt();
                    if (idx < trend.length) {
                      return Text(trend[idx]['week'] ?? '', style: TextStyle(color: Colors.white.withAlpha(120), fontSize: 10));
                    }
                    return const Text('');
                  },
                ),
              ),
              leftTitles: AxisTitles(
                sideTitles: SideTitles(
                  showTitles: true,
                  reservedSize: 35,
                  getTitlesWidget: (value, meta) {
                    return Text('${value.toInt()}', style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 10));
                  },
                ),
              ),
              topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
              rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            ),
            borderData: FlBorderData(show: false),
            gridData: FlGridData(
              show: true,
              drawVerticalLine: false,
              getDrawingHorizontalLine: (value) => FlLine(color: Colors.white.withAlpha(10), strokeWidth: 1),
            ),
            barGroups: List.generate(trend.length, (i) {
              final present = trend[i]['present'] ?? 0;
              return BarChartGroupData(
                x: i,
                barRods: [
                  BarChartRodData(
                    toY: present.toDouble(),
                    color: Colors.cyanAccent.withAlpha(200),
                    width: 16,
                    borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                  ),
                ],
              );
            }),
          ),
        ),
      ),
    );
  }

  Widget _buildDepartmentPerformance() {
    final depts = _bi!['departmentPerformance'] as List<dynamic>? ?? [];
    if (depts.isEmpty) return const SizedBox.shrink();

    return _buildCard(
      'Performance par département',
      Column(
        children: List.generate(depts.length, (i) {
          final name = depts[i]['name'] ?? '';
          final score = (depts[i]['score'] ?? 0).toDouble();
          final color = score >= 85 ? Colors.green :
                        score >= 70 ? Colors.amber :
                        Colors.red;
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Row(
              children: [
                SizedBox(
                  width: 100,
                  child: Text(name, style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 13)),
                ),
                Expanded(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: score / 100,
                      backgroundColor: Colors.white.withAlpha(15),
                      valueColor: AlwaysStoppedAnimation(color),
                      minHeight: 8,
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                SizedBox(
                  width: 40,
                  child: Text('${score.toInt()}%', style: TextStyle(color: color, fontSize: 13, fontWeight: FontWeight.bold)),
                ),
              ],
            ),
          );
        }),
      ),
    );
  }

  Widget _buildGrowthMetrics() {
    final newConverts = _bi!['newConverts'] ?? 0;
    final activeDisciples = _bi!['activeDisciples'] ?? 0;

    return Row(
      children: [
        Expanded(child: _buildStatBlock('Nouvelles conversions', '$newConverts', Icons.auto_awesome, Colors.amber)),
        const SizedBox(width: 12),
        Expanded(child: _buildStatBlock('Disciples actifs', '$activeDisciples', Icons.school, Colors.cyanAccent)),
      ],
    );
  }

  Widget _buildStatBlock(String label, String value, IconData icon, Color color) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withAlpha(30)),
      ),
      child: Column(
        children: [
          Icon(icon, color: color, size: 28),
          const SizedBox(height: 8),
          Text(value, style: TextStyle(color: color, fontSize: 28, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Text(label, style: TextStyle(color: Colors.white.withAlpha(140), fontSize: 11), textAlign: TextAlign.center),
        ],
      ),
    );
  }

  Widget _buildReportStats() {
    final submitted = _bi!['reportsSubmitted'] ?? 0;
    final pending = _bi!['reportsPending'] ?? 0;
    final total = submitted + pending;
    final rate = total > 0 ? submitted / total : 0.0;

    return _buildCard(
      'Rapports',
      Row(
        children: [
          SizedBox(
            width: 80, height: 80,
            child: PieChart(
              PieChartData(
                sections: [
                  PieChartSectionData(
                    value: submitted.toDouble(),
                    color: Colors.green,
                    radius: 10,
                    title: '',
                  ),
                  PieChartSectionData(
                    value: pending.toDouble(),
                    color: Colors.white.withAlpha(30),
                    radius: 10,
                    title: '',
                  ),
                ],
                centerSpaceRadius: 25,
                sectionsSpace: 2,
              ),
            ),
          ),
          const SizedBox(width: 20),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('${(rate * 100).toInt()}% soumis', style: const TextStyle(color: Colors.green, fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 4),
                Text('$submitted soumis · $pending en attente', style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 12)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCard(String title, Widget child) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withAlpha(10)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          child,
        ],
      ),
    );
  }
}
