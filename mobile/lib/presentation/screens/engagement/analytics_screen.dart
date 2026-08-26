import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Analytics d'engagement — branché sur GET /api/v1/engagement-analytics.
class EngagementAnalyticsScreen extends StatefulWidget {
  const EngagementAnalyticsScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<EngagementAnalyticsScreen> createState() => _EngagementAnalyticsScreenState();
}

class _EngagementAnalyticsScreenState extends State<EngagementAnalyticsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/engagement-analytics');
      final d = res.data;
      setState(() {
        _items = d is List ? d : (d is Map ? (d['content'] as List<dynamic>? ?? []) : <dynamic>[]);
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).engagementAnalyticsError; _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).engagementAnalyticsTitle), backgroundColor: Colors.deepOrange, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).engagementAnalyticsEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: GridView.builder(
                        padding: const EdgeInsets.all(16),
                        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.5, crossAxisSpacing: 10, mainAxisSpacing: 10),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final m = _items[i] as Map<String, dynamic>;
                          final change = (m['changePercentage'] as num?)?.toDouble() ?? 0;
                          return Card(
                            child: Padding(
                              padding: const EdgeInsets.all(10),
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: [
                                Text(m['metricCategory']?.toString().toUpperCase() ?? '', style: TextStyle(fontSize: 9, color: Colors.grey.shade600)),
                                Text('${((m['metricValue'] as num?) ?? 0).toStringAsFixed(1)}', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                                Text(m['metricName']?.toString() ?? '', maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11)),
                                Row(children: [
                                  Icon(change >= 0 ? Icons.trending_up : Icons.trending_down, size: 12, color: change >= 0 ? Colors.green : Colors.red),
                                  Text('${change >= 0 ? '+' : ''}${change.toStringAsFixed(1)}%', style: TextStyle(fontSize: 10, color: change >= 0 ? Colors.green : Colors.red)),
                                ]),
                              ]),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
