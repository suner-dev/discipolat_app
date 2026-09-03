import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Prédictions ML — branché sur `GET /api/v1/ai-predictions`.
class PredictionsMlScreen extends StatefulWidget {
  const PredictionsMlScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<PredictionsMlScreen> createState() => _PredictionsMlScreenState();
}

class _PredictionsMlScreenState extends State<PredictionsMlScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _predictions = [];
  bool _isLoading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = _predictions.isEmpty;
      _error = null;
    });
    try {
      // Use the standard /api/v1/ai-predictions endpoint
      final res = await _api.get('/ai-predictions');
      final data = res.data;
      final list = data is List ? data : (data is Map && data['content'] != null ? data['content'] : []);
      if (mounted) {
        setState(() {
          _predictions = List<dynamic>.from(list);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _error = AppLocalizations.of(context).predictionsError;
        });
      }
    }
  }

  String _typeLabel(String t) {
    switch (t) {
      case 'WORKFORCE_GROWTH':
        return 'Effectifs';
      case 'ATTENDANCE':
        return 'Présences';
      case 'BAPTISMS':
        return 'Baptêmes';
      case 'DROPOUT':
        return 'Décrochages';
      case 'ENGAGEMENT':
        return 'Engagement';
      default:
        return 'Finances';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).predictionsMlTitle), backgroundColor: Colors.purple.shade600, foregroundColor: Colors.white),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null && _predictions.isEmpty
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
                  child: _predictions.isEmpty
                      ? ListView(
                          children: [
                            const SizedBox(height: 120),
                            const Icon(Icons.insights, size: 56, color: Colors.grey),
                            Center(child: Padding(padding: const EdgeInsets.all(8), child: Text(AppLocalizations.of(context).predictionsMlEmpty, style: const TextStyle(color: Colors.grey)))),
                          ],
                        )
                      : GridView.builder(
                          padding: const EdgeInsets.all(16),
                          physics: const AlwaysScrollableScrollPhysics(),
                          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, childAspectRatio: 1.1, crossAxisSpacing: 12, mainAxisSpacing: 12),
                          itemCount: _predictions.length,
                          itemBuilder: (context, i) {
                            final p = _predictions[i];
                            final isUp = p['trend']?.toString() == 'UP';
                            final current = (p['currentValue'] as num?)?.toDouble() ?? 0;
                            final predicted = (p['predictedValue'] as num?)?.toDouble() ?? 0;
                            final growth = (p['growthRate'] as num?)?.toDouble() ?? 0;
                            return Card(
                              color: Colors.white,
                              child: Padding(
                                padding: const EdgeInsets.all(12),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(children: [
                                      Icon(isUp ? Icons.trending_up : Icons.trending_down, color: isUp ? Colors.green : Colors.red, size: 18),
                                      const Spacer(),
                                      Text(p['confidence']?.toString() ?? '', style: TextStyle(fontSize: 10, color: Colors.grey.shade500)),
                                    ]),
                                    const SizedBox(height: 4),
                                    Text(_typeLabel(p['predictionType']?.toString() ?? ''), style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                                    const Spacer(),
                                    Text(current.toStringAsFixed(0), style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                                    Text(AppLocalizations.of(context).predictedValue(predicted), style: TextStyle(fontSize: 12, color: Colors.grey.shade600)),
                                    Text('${growth >= 0 ? '+' : ''}${growth.toStringAsFixed(1)}%', style: TextStyle(fontSize: 12, color: isUp ? Colors.green : Colors.red)),
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
