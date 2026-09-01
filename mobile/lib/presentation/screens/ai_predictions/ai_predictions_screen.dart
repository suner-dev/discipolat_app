import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../../l10n/app_localizations.dart';

/// Prédictions & Analyse IA — branché sur GET/POST /api/v1/ai-predictions.
/// Accessible à tous les rôles authentifiés (le tenant est résolu côté serveur).
class AiPredictionsScreen extends StatefulWidget {
  const AiPredictionsScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<AiPredictionsScreen> createState() => _AiPredictionsScreenState();
}

class _AiPredictionsScreenState extends State<AiPredictionsScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  bool _generating = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await _api.get('/ai-predictions');
      final d = res.data;
      setState(() {
        _items = d is List ? d : <dynamic>[];
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = AppLocalizations.of(context).predictionsError;
        _loading = false;
      });
    }
  }

  Future<void> _generate() async {
    setState(() {
      _generating = true;
      _error = null;
    });
    try {
      final res = await _api.post('/ai-predictions/generate');
      final d = res.data;
      setState(() {
        _items = d is List ? d : <dynamic>[];
        _generating = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _error = AppLocalizations.of(context).predictionsError;
        _generating = false;
      });
    }
  }

  String _typeLabel(String type) {
    switch (type) {
      case 'GROWTH_FORECAST':
        return 'Croissance';
      case 'CHURN_RISK':
        return 'Risque de décrochage';
      case 'ATTENDANCE_TREND':
        return 'Tendance de présence';
      case 'GIVING_TREND':
        return 'Tendance des dons';
      case 'ENGAGEMENT_SCORE':
        return "Score d'engagement";
      case 'DEPARTMENT_PERFORMANCE':
        return 'Performance département';
      default:
        return type;
    }
  }

  IconData _typeIcon(String type) {
    switch (type) {
      case 'GROWTH_FORECAST':
        return Icons.trending_up;
      case 'CHURN_RISK':
        return Icons.warning_amber_rounded;
      case 'ATTENDANCE_TREND':
        return Icons.bar_chart;
      case 'GIVING_TREND':
        return Icons.paid;
      default:
        return Icons.auto_awesome;
    }
  }

  Color _riskColor(String risk) {
    switch (risk) {
      case 'CRITICAL':
        return Colors.red;
      case 'HIGH':
        return Colors.orange;
      case 'MEDIUM':
        return Colors.amber;
      default:
        return Colors.green;
    }
  }

  Color _typeColor(String type) {
    switch (type) {
      case 'GROWTH_FORECAST':
        return Colors.green;
      case 'CHURN_RISK':
        return Colors.red;
      case 'ATTENDANCE_TREND':
        return Colors.blue;
      case 'GIVING_TREND':
        return Colors.teal;
      default:
        return Colors.purple;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Prédictions & Analyse IA',
            style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.deepPurple,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            tooltip: 'Régénérer les prédictions',
            onPressed: _generating ? null : _generate,
            icon: _generating
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                        strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.refresh),
          ),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Padding(
                        padding: const EdgeInsets.all(24),
                        child: Text(_error!, textAlign: TextAlign.center),
                      ),
                      ElevatedButton(
                          onPressed: _load,
                          child: Text(AppLocalizations.of(context).retry)),
                    ],
                  ),
                )
              : _items.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.auto_awesome,
                              size: 48, color: Colors.deepPurple),
                          const SizedBox(height: 12),
                          const Text(
                            'Aucune prédiction disponible',
                            style: TextStyle(
                                fontSize: 16, fontWeight: FontWeight.w600),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'Générez des prédictions IA pour analyser la croissance de votre église.',
                            style: TextStyle(
                                color: Colors.grey.shade600, fontSize: 13),
                            textAlign: TextAlign.center,
                          ),
                          const SizedBox(height: 16),
                          ElevatedButton.icon(
                            onPressed: _generate,
                            icon: _generating
                                ? const SizedBox(
                                    width: 16,
                                    height: 16,
                                    child: CircularProgressIndicator(
                                        strokeWidth: 2))
                                : const Icon(Icons.auto_awesome),
                            label: const Text('Générer les prédictions'),
                          ),
                        ],
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length + 1,
                        itemBuilder: (context, i) {
                          if (i == 0) {
                            return _buildStatsHeader();
                          }
                          final p =
                              _items[i - 1] as Map<String, dynamic>;
                          return _buildPredictionCard(p);
                        },
                      ),
                    ),
    );
  }

  Widget _buildStatsHeader() {
    if (_items.isEmpty) return const SizedBox.shrink();
    var risks = 0;
    for (final p in _items) {
      final r = p['riskLevel']?.toString() ?? '';
      if (r == 'HIGH' || r == 'CRITICAL') risks++;
    }
    final avgConf = _items.fold<double>(
        0, (acc, p) => acc + ((p['confidenceScore'] as num?)?.toDouble() ?? 0));
    final avg = _items.isNotEmpty ? avgConf / _items.length : 0.0;
    return Card(
      color: Colors.deepPurple.withValues(alpha: 0.06),
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            _statColumn('Prédictions', '${_items.length}',
                Icons.query_stats, Colors.deepPurple),
            _statColumn('À risque', '$risks', Icons.warning_amber_rounded,
                risks > 0 ? Colors.red : Colors.green),
            _statColumn('Confiance', '${(avg * 100).round()}%', Icons.shield,
                Colors.blue),
          ],
        ),
      ),
    );
  }

  Widget _statColumn(
      String label, String value, IconData icon, Color color) {
    return Expanded(
      child: Column(
        children: [
          Icon(icon, color: color, size: 20),
          const SizedBox(height: 4),
          Text(value,
              style:
                  TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: color)),
          Text(label,
              style: TextStyle(color: Colors.grey.shade600, fontSize: 11)),
        ],
      ),
    );
  }

  Widget _buildPredictionCard(Map<String, dynamic> p) {
    final type = p['predictionType']?.toString() ?? '';
    final metric = p['metricName']?.toString() ?? type;
    final current = (p['currentValue'] as num?)?.toDouble() ?? 0;
    final predicted = (p['predictedValue'] as num?)?.toDouble() ?? 0;
    final confidence = (p['confidenceScore'] as num?)?.toDouble() ?? 0;
    final risk = p['riskLevel']?.toString() ?? 'LOW';
    final explanation = p['explanation']?.toString() ?? '';
    final color = _typeColor(type);
    final riskColor = _riskColor(risk);

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: color.withValues(alpha: .15),
                  child: Icon(_typeIcon(type), color: color, size: 18),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(metric,
                          style: const TextStyle(
                              fontSize: 14, fontWeight: FontWeight.w600),
                          overflow: TextOverflow.ellipsis),
                      Text(_typeLabel(type),
                          style: TextStyle(
                              fontSize: 11, color: Colors.grey.shade600)),
                    ],
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: riskColor.withValues(alpha: .12),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    risk == 'CRITICAL'
                        ? 'CRITIQUE'
                        : risk,
                    style: TextStyle(
                        color: riskColor,
                        fontSize: 10,
                        fontWeight: FontWeight.bold),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _valueColumn('ACTUEL',
                      _formatNum(current), Colors.grey.shade600),
                ),
                const Icon(Icons.arrow_forward, size: 18, color: color),
                Expanded(
                  child: _valueColumn('PRÉVU', _formatNum(predicted), color,
                      bold: true),
                ),
                const Spacer(),
                Text('${(confidence * 100).round()}% confiance',
                    style: TextStyle(
                        fontSize: 11,
                        color: Colors.grey.shade600,
                        fontWeight: FontWeight.w600)),
              ],
            ),
            const SizedBox(height: 6),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: confidence.clamp(0, 1),
                minHeight: 5,
                backgroundColor: Colors.grey.shade200,
                color: color,
              ),
            ),
            if (explanation.isNotEmpty) ...[
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: Colors.grey.shade100,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  explanation,
                  style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey.shade700,
                      fontStyle: FontStyle.italic,
                      height: 1.35),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _valueColumn(String label, String value, Color color,
      {bool bold = false}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label,
            style: TextStyle(fontSize: 10, color: Colors.grey.shade500)),
        Text(value,
            style: TextStyle(
                fontSize: 15,
                fontWeight: bold ? FontWeight.bold : FontWeight.w500,
                color: color)),
      ],
    );
  }

  String _formatNum(double v) {
    if (v == v.roundToDouble()) return v.toStringAsFixed(0);
    return v.toStringAsFixed(1);
  }
}