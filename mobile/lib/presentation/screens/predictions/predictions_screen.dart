import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Prédictions IA — branché sur GET /api/v1/predictions.
class PredictionsScreen extends StatefulWidget {
  const PredictionsScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<PredictionsScreen> createState() => _PredictionsScreenState();
}

class _PredictionsScreenState extends State<PredictionsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/predictions');
      final d = res.data;
      setState(() {
        _items = d is List ? d : (d is Map ? (d['content'] as List<dynamic>? ?? []) : <dynamic>[]);
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).predictionsError; _loading = false; });
    }
  }

  Color _confidenceColor(String? c) {
    switch (c) {
      case 'HIGH': return Colors.green;
      case 'LOW': return Colors.orange;
      default: return Colors.blue;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).predictionsTitle), backgroundColor: Colors.deepPurple, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).predictionsEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final p = _items[i] as Map<String, dynamic>;
                          final confidence = p['confidence']?.toString() ?? 'MEDIUM';
                          final growth = (p['growthRate'] as num?)?.toDouble() ?? 0;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: _confidenceColor(confidence).withValues(alpha: .15), child: Icon(Icons.auto_graph, color: _confidenceColor(confidence), size: 20)),
                              title: Text(p['predictionType']?.toString() ?? 'Prédiction', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Text('Actuel ${((p['currentValue'] as num?) ?? 0).toStringAsFixed(0)} → Prévu ${((p['predictedValue'] as num?) ?? 0).toStringAsFixed(0)} (${growth >= 0 ? '+' : ''}${growth.toStringAsFixed(1)}%)', style: const TextStyle(fontSize: 12)),
                                if ((p['narrative'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2), child: Text(p['narrative'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 11, fontStyle: FontStyle.italic))),
                              ]),
                              isThreeLine: true,
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
