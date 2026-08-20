import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import '../../../data/services/api_service.dart';

/// Benchmark screen — anonymous cross-church comparison.
/// Compares this church's metrics against anonymized peer averages.
class BenchmarkScreen extends StatefulWidget {
  const BenchmarkScreen({super.key});

  @override
  State<BenchmarkScreen> createState() => _BenchmarkScreenState();
}

class _BenchmarkScreenState extends State<BenchmarkScreen> {
  final _api = ApiService();
  Map<String, dynamic>? _benchmark;
  Map<String, dynamic>? _trends;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _api.get('/benchmark'),
        _api.get('/benchmark/trends'),
      ]);
      if (mounted) {
        setState(() {
          _benchmark = results[0].data;
          _trends = results[1].data;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0A0A1A),
      appBar: AppBar(
        title: const Text('🏆 Benchmark', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Colors.white),
            onPressed: _loadData,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator(color: Colors.cyanAccent))
          : _benchmark == null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.leaderboard, color: Colors.white.withAlpha(50), size: 64),
                      const SizedBox(height: 16),
                      Text('Données non disponibles', style: TextStyle(color: Colors.white.withAlpha(150), fontSize: 16)),
                    ],
                  ),
                )
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      _buildPercentileCards(),
                      const SizedBox(height: 20),
                      _buildComparisonCards(),
                      const SizedBox(height: 20),
                      _buildAttendanceTrendChart(),
                      const SizedBox(height: 20),
                      _buildGrowthTrendChart(),
                      const SizedBox(height: 20),
                      _buildInsights(),
                      const SizedBox(height: 40),
                    ],
                  ),
                ),
    );
  }

  Widget _buildPercentileCards() {
    final percentiles = _benchmark!['percentile'] as Map<String, dynamic>? ?? {};
    if (percentiles.isEmpty) return const SizedBox.shrink();

    final items = [
      ('Fréquentation', (percentiles['attendanceRate'] ?? 0).toDouble(), Colors.cyanAccent),
      ('Croissance', (percentiles['growthRate'] ?? 0).toDouble(), Colors.green),
      ('Rapports', (percentiles['reportsSubmitted'] ?? 0).toDouble(), Colors.amber),
      ('Bénévolat', (percentiles['volunteerRate'] ?? 0).toDouble(), Colors.purpleAccent),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Votre position', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),
        GridView.count(
          crossAxisCount: 2,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: 1.3,
          children: items.map((item) {
            final (label, value, color) = item;
            return _buildPercentileCard(label, value, color);
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildPercentileCard(String label, double percentile, Color color) {
    final rank = percentile >= 75 ? 'Top 25%' :
                 percentile >= 50 ? 'Moyen' :
                 percentile >= 25 ? 'À améliorer' : 'Bas';
    final rankColor = percentile >= 75 ? Colors.green :
                      percentile >= 50 ? Colors.amber :
                      Colors.red;

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(6),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withAlpha(30)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 12)),
          Row(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text('${percentile.toInt()}', style: TextStyle(color: color, fontSize: 28, fontWeight: FontWeight.bold)),
              const SizedBox(width: 4),
              Padding(
                padding: const EdgeInsets.only(bottom: 4),
                child: Text('e %', style: TextStyle(color: color.withAlpha(150), fontSize: 12)),
              ),
            ],
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: rankColor.withAlpha(30),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(rank, style: TextStyle(color: rankColor, fontSize: 10, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  Widget _buildComparisonCards() {
    final current = _benchmark!['currentChurch'] as Map<String, dynamic>? ?? {};
    final average = _benchmark!['averagePeers'] as Map<String, dynamic>? ?? {};
    if (current.isEmpty || average.isEmpty) return const SizedBox.shrink();

    final metrics = [
      ('Membres', current['totalMembers'] ?? 0, average['totalMembers'] ?? 0, Icons.people, Colors.cyanAccent),
      ('Fréquentation', current['attendanceRate'] ?? 0, average['attendanceRate'] ?? 0, Icons.event_seat, Colors.green),
      ('Croissance', current['growthRate'] ?? 0, average['growthRate'] ?? 0, Icons.trending_up, Colors.amber),
      ('Conversions', current['newConverts'] ?? 0, average['newConverts'] ?? 0, Icons.auto_awesome, Colors.purpleAccent),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Comparaison', style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
        const SizedBox(height: 12),
        ...metrics.map((m) {
          final (label, yours, avg, icon, color) = m;
          final isAbove = (yours is num ? yours.toDouble() : 0) > (avg is num ? avg.toDouble() : 0);
          return Container(
            margin: const EdgeInsets.only(bottom: 8),
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(6),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                Icon(icon, color: color, size: 20),
                const SizedBox(width: 12),
                Expanded(child: Text(label, style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 14))),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(
                      yours is double ? yours.toStringAsFixed(1) : '$yours',
                      style: TextStyle(color: isAbove ? Colors.green : Colors.red, fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                    Text(
                      'moy: ${avg is double ? avg.toStringAsFixed(1) : avg}',
                      style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 11),
                    ),
                  ],
                ),
                const SizedBox(width: 8),
                Icon(
                  isAbove ? Icons.arrow_upward : Icons.arrow_downward,
                  color: isAbove ? Colors.green : Colors.red,
                  size: 16,
                ),
              ],
            ),
          );
        }),
      ],
    );
  }

  Widget _buildAttendanceTrendChart() {
    final trends = _trends?['attendanceTrend'] as List<dynamic>? ?? [];
    if (trends.isEmpty) return const SizedBox.shrink();

    return _buildCard(
      'Tendance fréquentation',
      SizedBox(
        height: 180,
        child: LineChart(
          LineChartData(
            gridData: FlGridData(show: true, drawVerticalLine: false, getDrawingHorizontalLine: (v) => FlLine(color: Colors.white.withAlpha(10))),
            titlesData: FlTitlesData(
              leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 30,
                getTitlesWidget: (v, _) => Text('${v.toInt()}', style: TextStyle(color: Colors.white.withAlpha(80), fontSize: 10)))),
              bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true,
                getTitlesWidget: (v, _) => Text(trends[v.toInt()]['month'] ?? '', style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 10)))),
              topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
              rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            ),
            borderData: FlBorderData(show: false),
            lineBarsData: [
              LineChartBarData(
                spots: List.generate(trends.length, (i) => FlSpot(i.toDouble(), (trends[i]['current'] ?? 0).toDouble())),
                isCurved: true,
                color: Colors.cyanAccent,
                barWidth: 2,
                dotData: const FlDotData(show: false),
              ),
              LineChartBarData(
                spots: List.generate(trends.length, (i) => FlSpot(i.toDouble(), (trends[i]['average'] ?? 0).toDouble())),
                isCurved: true,
                color: Colors.white.withAlpha(60),
                barWidth: 2,
                dotData: const FlDotData(show: false),
                dashArray: [5, 5],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildGrowthTrendChart() {
    final trends = _trends?['growthTrend'] as List<dynamic>? ?? [];
    if (trends.isEmpty) return const SizedBox.shrink();

    return _buildCard(
      'Tendance croissance',
      SizedBox(
        height: 180,
        child: BarChart(
          BarChartData(
            alignment: BarChartAlignment.spaceAround,
            gridData: FlGridData(show: true, drawVerticalLine: false, getDrawingHorizontalLine: (v) => FlLine(color: Colors.white.withAlpha(10))),
            titlesData: FlTitlesData(
              leftTitles: AxisTitles(sideTitles: SideTitles(showTitles: true, reservedSize: 30,
                getTitlesWidget: (v, _) => Text('${v.toInt()}', style: TextStyle(color: Colors.white.withAlpha(80), fontSize: 10)))),
              bottomTitles: AxisTitles(sideTitles: SideTitles(showTitles: true,
                getTitlesWidget: (v, _) => Text(trends[v.toInt()]['month'] ?? '', style: TextStyle(color: Colors.white.withAlpha(100), fontSize: 10)))),
              topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
              rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            ),
            borderData: FlBorderData(show: false),
            barGroups: List.generate(trends.length, (i) {
              return BarChartGroupData(
                x: i,
                barRods: [
                  BarChartRodData(toY: (trends[i]['current'] ?? 0).toDouble(), color: Colors.green.withAlpha(200), width: 12, borderRadius: const BorderRadius.vertical(top: Radius.circular(3))),
                  BarChartRodData(toY: (trends[i]['average'] ?? 0).toDouble(), color: Colors.white.withAlpha(40), width: 12, borderRadius: const BorderRadius.vertical(top: Radius.circular(3))),
                ],
              );
            }),
          ),
        ),
      ),
    );
  }

  Widget _buildInsights() {
    final current = _benchmark!['currentChurch'] as Map<String, dynamic>? ?? {};
    final average = _benchmark!['averagePeers'] as Map<String, dynamic>? ?? {};

    final List<String> insights = [];
    final attendanceRate = (current['attendanceRate'] ?? 0).toDouble();
    final avgAttendance = (average['attendanceRate'] ?? 0).toDouble();
    if (attendanceRate > avgAttendance) {
      insights.add('✅ Votre fréquentation dépasse la moyenne de ${(attendanceRate - avgAttendance).toStringAsFixed(1)}%');
    } else {
      insights.add('⚠️ Votre fréquentation est inférieure à la moyenne de ${(avgAttendance - attendanceRate).toStringAsFixed(1)}%');
    }

    final growthRate = (current['growthRate'] ?? 0).toDouble();
    final avgGrowth = (average['growthRate'] ?? 0).toDouble();
    if (growthRate > avgGrowth) {
      insights.add('🚀 Votre croissance est supérieure à la moyenne');
    }

    return _buildCard(
      'Insights',
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: insights.map((insight) => Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: Text(insight, style: TextStyle(color: Colors.white.withAlpha(180), fontSize: 13, height: 1.4)),
        )).toList(),
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
