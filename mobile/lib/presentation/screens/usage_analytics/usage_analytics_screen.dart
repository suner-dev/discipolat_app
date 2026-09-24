import 'package:flutter/material.dart';

import '../../../data/services/api_service.dart';
import '../../../data/services/usage_analytics_service.dart';
import '../../../models/usage_analytics_summary.dart';

class UsageAnalyticsScreen extends StatefulWidget {
  const UsageAnalyticsScreen({super.key, this.apiService, this.service});

  final ApiService? apiService;
  final UsageAnalyticsService? service;

  @override
  State<UsageAnalyticsScreen> createState() => _UsageAnalyticsScreenState();
}

class _UsageAnalyticsScreenState extends State<UsageAnalyticsScreen> {
  late final UsageAnalyticsService _service = widget.service ??
      UsageAnalyticsService(apiService: widget.apiService ?? ApiService());
  UsageAnalyticsSummary? _summary;
  bool _loading = true;
  bool _disabled = false;
  bool _unavailable = false;
  String _period = '7d';

  @override
  void initState() {
    super.initState();
    _loadSummary();
  }

  Future<void> _loadSummary() async {
    if (mounted) {
      setState(() {
        _loading = true;
        _disabled = false;
        _unavailable = false;
        _summary = null;
      });
    }

    try {
      final analyticsEnabled = await _service.fetchAnalyticsEnabled();
      if (analyticsEnabled == false) {
        if (mounted) {
          setState(() {
            _disabled = true;
            _loading = false;
          });
        }
        return;
      }
      if (analyticsEnabled != true) {
        if (mounted) {
          setState(() {
            _unavailable = true;
            _loading = false;
          });
        }
        return;
      }
      final summary =
          await _service.fetchSummary(days: _daysForPeriod(_period));
      if (!mounted) return;
      setState(() {
        _summary = summary;
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _unavailable = true;
        _loading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Analytics d\'usage'),
        actions: [
          PopupMenuButton<String>(
            initialValue: _period,
            onSelected: (value) {
              setState(() => _period = value);
              _loadSummary();
            },
            itemBuilder: (_) => const [
              PopupMenuItem(value: '1d', child: Text('24 heures')),
              PopupMenuItem(value: '7d', child: Text('7 jours')),
              PopupMenuItem(value: '30d', child: Text('30 jours')),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loading ? null : _loadSummary,
          ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_disabled) {
      return const Center(
        child: _UsageMessage(
          text: 'Analytics désactivés par le tenant',
          color: Colors.orange,
        ),
      );
    }
    if (_unavailable || _summary == null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const _UsageMessage(
              text: 'Données d’analytics indisponibles',
              color: Colors.red,
            ),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: _loadSummary,
              child: const Text('Réessayer'),
            ),
          ],
        ),
      );
    }

    final summary = _summary!;
    return RefreshIndicator(
      onRefresh: _loadSummary,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          GridView.count(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            crossAxisCount: 2,
            crossAxisSpacing: 12,
            mainAxisSpacing: 12,
            childAspectRatio: 1.5,
            children: [
              _statCard(
                'Événements',
                _number(summary.totalEvents),
                Icons.visibility,
                Colors.cyan,
              ),
              _statCard(
                'Pages vues',
                _number(summary.pageViews),
                Icons.pageview,
                Colors.green,
              ),
              _statCard(
                'Utilisateurs uniques',
                _number(summary.activeUsers),
                Icons.people,
                Colors.blue,
              ),
              _statCard(
                'Durée moyenne',
                _duration(summary.averageDurationSeconds),
                Icons.timer,
                Colors.purple,
              ),
            ],
          ),
          const SizedBox(height: 20),
          const Text(
            'Pages les plus vues',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          ),
          const SizedBox(height: 8),
          if (summary.topPages.isEmpty)
            const _UsageMessage(
              text: 'Données des pages indisponibles',
              color: Colors.orange,
            )
          else
            ...summary.topPages.entries.map(
              (entry) => Card(
                child: ListTile(
                  leading: const Icon(Icons.pageview, color: Colors.cyan),
                  title: Text(entry.key),
                  trailing: Text(entry.value.toString()),
                ),
              ),
            ),
          const SizedBox(height: 20),
          const Text(
            'Appareils',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          ),
          const SizedBox(height: 8),
          if (summary.byDevice.isEmpty)
            const _UsageMessage(
              text: 'Données des appareils indisponibles',
              color: Colors.orange,
            )
          else
            ...summary.byDevice.entries.map(
              (entry) => ListTile(
                title: Text(entry.key),
                trailing:
                    Text(_percentage(summary.percentageForDevice(entry.key))),
              ),
            ),
        ],
      ),
    );
  }

  Widget _statCard(String label, String value, IconData icon, Color color) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, color: color, size: 18),
            const SizedBox(height: 8),
            Text(
              value,
              style: TextStyle(
                color: color,
                fontWeight: FontWeight.bold,
                fontSize: 20,
              ),
            ),
            const SizedBox(height: 2),
            Text(label, style: const TextStyle(fontSize: 10)),
          ],
        ),
      ),
    );
  }

  String _number(num? value) =>
      value == null ? 'Indisponible' : value.toString();

  String _duration(num? value) {
    return value == null ? 'Indisponible' : '${value.toString()} s';
  }

  String _percentage(double? value) {
    return value == null ? 'Indisponible' : '${value.toStringAsFixed(1)}%';
  }

  int _daysForPeriod(String period) {
    switch (period) {
      case '1d':
        return 1;
      case '30d':
        return 30;
      default:
        return 7;
    }
  }
}

class _UsageMessage extends StatelessWidget {
  const _UsageMessage({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Text(text,
          textAlign: TextAlign.center, style: TextStyle(color: color)),
    );
  }
}
