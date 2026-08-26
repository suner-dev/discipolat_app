import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// P3 #106 — Tableau de bord sabbatique : état spirituel sur 12 axes de maturité.
class SabbathDashboardScreen extends StatefulWidget {
  const SabbathDashboardScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<SabbathDashboardScreen> createState() => _SabbathDashboardScreenState();
}

class _SabbathDashboardScreenState extends State<SabbathDashboardScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _data;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final res = await _api.get('/sabbath-dashboard');
      setState(() { _data = res.data as Map<String, dynamic>?; _loading = false; });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  MaterialColor _axisColor(String niveau) {
    switch (niveau) {
      case 'MATURE':
        return Colors.green;
      case 'EMBRYONNAIRE':
        return Colors.red;
      default:
        return Colors.orange;
    }
  }

  @override
  Widget build(BuildContext context) {
    final axes = (_data?['axes'] as List<dynamic>?) ?? [];
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).sabbathTitle),
        backgroundColor: Colors.deepPurple.shade700,
        foregroundColor: Colors.white,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Card(
                    color: Colors.deepPurple.shade50,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Text(AppLocalizations.of(context).globalMaturity, style: const TextStyle(fontSize: 12)),
                        Row(crossAxisAlignment: CrossAxisAlignment.end, children: [
                          Text('${_data?['maturiteGlobale'] ?? '—'}', style: const TextStyle(fontSize: 40, fontWeight: FontWeight.bold)),
                          const Padding(padding: EdgeInsets.only(bottom: 8), child: Text('/100')),
                        ]),
                        LinearProgressIndicator(value: ((_data?['maturiteGlobale'] as num?)?.toDouble() ?? 0) / 100),
                        const SizedBox(height: 8),
                        if (_data?['saisonSpirituelle'] != null)
                          Text(_data!['saisonSpirituelle'].toString(), style: const TextStyle(fontWeight: FontWeight.bold)),
                      ]),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 16,
                    runSpacing: 8,
                    children: [
                      _kpi(AppLocalizations.of(context).activeSouls, '${_data?['amesActives'] ?? '—'}'),
                      _kpi(AppLocalizations.of(context).activeMakers, '${_data?['faiseursActifs'] ?? '—'}'),
                      _kpi(AppLocalizations.of(context).familiesAtRisk, '${_data?['famillesARisque'] ?? '—'}'),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(AppLocalizations.of(context).twelveAxes, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  ...axes.map((a) {
                    final axis = a as Map<String, dynamic>;
                    final score = (axis['score'] as num?)?.toInt() ?? 0;
                    final color = _axisColor(axis['niveau']?.toString() ?? '');
                    return Padding(
                      padding: const EdgeInsets.symmetric(vertical: 6),
                      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
                          Expanded(child: Text(axis['axe'].toString(), style: const TextStyle(fontSize: 13))),
                          Text('$score', style: TextStyle(fontWeight: FontWeight.bold, color: color.shade700)),
                        ]),
                        const SizedBox(height: 3),
                        LinearProgressIndicator(value: score / 100, color: color, backgroundColor: Colors.grey.shade200),
                      ]),
                    );
                  }),
                  if (_data?['orientationPastorale'] != null)
                    Card(
                      color: Colors.amber.shade50,
                      margin: const EdgeInsets.only(top: 12),
                      child: Padding(
                        padding: const EdgeInsets.all(14),
                        child: Text('🧭 ${_data!['orientationPastorale']}', style: const TextStyle(fontStyle: FontStyle.italic)),
                      ),
                    ),
                ],
              ),
            ),
    );
  }

  Widget _kpi(String label, String value) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      Text(label, style: const TextStyle(fontSize: 11, color: Colors.black54)),
      Text(value, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
    ]);
  }
}
