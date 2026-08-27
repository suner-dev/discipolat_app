import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Insights Exécutifs IA — branché sur `GET /api/v1/executive-insights`.
class ExecutiveInsightsScreen extends StatefulWidget {
  const ExecutiveInsightsScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<ExecutiveInsightsScreen> createState() => _ExecutiveInsightsScreenState();
}

class _ExecutiveInsightsScreenState extends State<ExecutiveInsightsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _insights = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = _insights.isEmpty;
      _error = null;
    });
    try {
      final res = await _api.get('/executive-insights');
      final data = res.data;
      final list = data is List ? data : (data is Map && data['content'] != null ? data['content'] : []);
      if (mounted) {
        setState(() {
          _insights = List<dynamic>.from(list);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = AppLocalizations.of(context).executiveInsightsError;
        });
      }
    }
  }

  Color _severityColor(String s) {
    switch (s) {
      case 'CRITICAL':
        return Colors.red;
      case 'WARNING':
        return Colors.orange;
      case 'OPPORTUNITY':
        return Colors.blue;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).executiveInsightsTitle), backgroundColor: Colors.indigo.shade600, foregroundColor: Colors.white),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _insights.isEmpty
              ? ListView(
                  padding: const EdgeInsets.all(24),
                  children: [
                    const SizedBox(height: 60),
                    const Icon(Icons.cloud_off, size: 56, color: Colors.grey),
                    const SizedBox(height: 12),
                    Center(child: Text(_error ?? 'Erreur', textAlign: TextAlign.center)),
                    const SizedBox(height: 16),
                    Center(
                      child: FilledButton.icon(
                        onPressed: _load,
                        icon: const Icon(Icons.refresh),
                        label: Text(AppLocalizations.of(context).retry),
                      ),
                    ),
                  ],
                )
              : RefreshIndicator(
                  onRefresh: _load,
                  child: _insights.isEmpty
                      ? ListView(
                          children: [
                            const SizedBox(height: 120),
                            const Icon(Icons.lightbulb_outline, size: 56, color: Colors.grey),
                            Center(child: Padding(padding: const EdgeInsets.all(8), child: Text(AppLocalizations.of(context).executiveInsightsEmpty, style: const TextStyle(color: Colors.grey)))),
                          ],
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _insights.length,
                          itemBuilder: (context, i) {
                            final insight = _insights[i];
                            final color = _severityColor(insight['severity']?.toString() ?? 'INFO');
                            return Card(
                              margin: const EdgeInsets.only(bottom: 12),
                              color: Colors.white,
                              child: Padding(
                                padding: const EdgeInsets.all(16),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(children: [
                                      Icon(Icons.lightbulb, color: color, size: 20),
                                      const SizedBox(width: 8),
                                      Expanded(child: Text(insight['title']?.toString() ?? '', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16))),
                                      Text(insight['metricValue']?.toString() ?? '', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: color)),
                                    ]),
                                    if (insight['description'] != null) ...[
                                      const SizedBox(height: 8),
                                      Text(insight['description'].toString(), style: TextStyle(color: Colors.grey.shade600)),
                                    ],
                                    if (insight['recommendedAction'] != null) ...[
                                      const SizedBox(height: 8),
                                      Container(
                                        padding: const EdgeInsets.all(8),
                                        decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                                        child: Row(children: [
                                          Icon(Icons.arrow_forward, color: color, size: 16),
                                          const SizedBox(width: 8),
                                          Expanded(child: Text(insight['recommendedAction'].toString(), style: TextStyle(color: color, fontSize: 13))),
                                        ]),
                                      ),
                                    ],
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                ),
    );
  }
}
