import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// P1 #44 / P3 #103 — Projection de croissance IA + prophétie (données réelles).
class GrowthProjectionScreen extends StatefulWidget {
  const GrowthProjectionScreen({super.key, this.apiService});

  final ApiService? apiService;

  @override
  State<GrowthProjectionScreen> createState() => _GrowthProjectionScreenState();
}

class _GrowthProjectionScreenState extends State<GrowthProjectionScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  Map<String, dynamic>? _prophecy;
  List<dynamic> _projections = [];
  bool _loading = true;

  final _tauxCtrl = TextEditingController(text: '12');
  final _moisCtrl = TextEditingController(text: '12');

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _tauxCtrl.dispose();
    _moisCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    try {
      final prophecy = await _api.get('/growth-projections/prophecy');
      final list = await _api.get('/growth-projections');
      setState(() {
        _prophecy = prophecy.data as Map<String, dynamic>?;
        _projections = (list.data as List<dynamic>?) ?? [];
        _loading = false;
      });
    } catch (_) {
      setState(() => _loading = false);
    }
  }

  Future<void> _simulate() async {
    try {
      await _api.post('/growth-projections/simulate', data: {
        'nom': 'Simulation mobile',
        'typeProjection': 'EGLISE',
        'effectifActuel': (_prophecy?['effectifActuel'] as num?)?.toInt() ?? 100,
        'tauxCroissanceAnnuel': num.tryParse(_tauxCtrl.text) ?? 12,
        'moisProjection': int.tryParse(_moisCtrl.text) ?? 12,
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).simulationSaved)));
        _load();
      }
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(AppLocalizations.of(context).simulationFailed)));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(AppLocalizations.of(context).growthProjectionTitle),
        backgroundColor: Colors.teal.shade700,
        foregroundColor: Colors.white,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (_prophecy != null)
                    Card(
                      color: Colors.teal.shade50,
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child:                        Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(AppLocalizations.of(context).growthProphecyTitle, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                          const SizedBox(height: 8),
                          Text(AppLocalizations.of(context).projectedAnnualGrowth('${_prophecy?['croissanceAnnuellePct'] ?? '—'}')),
                          Text(AppLocalizations.of(context).headcountIn12Months('${_prophecy?['effectifProjete12Mois'] ?? '—'}')),
                          Text(AppLocalizations.of(context).leadersNeededCount('${_prophecy?['besoinsLeaders'] ?? '—'}')),
                          if (_prophecy?['message'] != null)
                            Padding(
                              padding: const EdgeInsets.only(top: 6),
                              child: Text(_prophecy!['message'].toString(), style: const TextStyle(fontStyle: FontStyle.italic, fontSize: 12, color: Colors.black54)),
                            ),
                        ]),
                      ),
                    ),
                  const SizedBox(height: 16),
                  Text(AppLocalizations.of(context).simulator, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(children: [
                        TextField(controller: _tauxCtrl, keyboardType: TextInputType.number, decoration: InputDecoration(labelText: AppLocalizations.of(context).annualGrowthRate, border: const OutlineInputBorder(), isDense: true)),
                        const SizedBox(height: 10),
                        TextField(controller: _moisCtrl, keyboardType: TextInputType.number, decoration: InputDecoration(labelText: AppLocalizations.of(context).horizonMonths, border: const OutlineInputBorder(), isDense: true)),
                        const SizedBox(height: 12),
                        SizedBox(
                          width: double.infinity,
                          child: ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(backgroundColor: Colors.teal.shade700),
                            onPressed: _simulate,
                            icon: const Icon(Icons.play_arrow, color: Colors.white),
                            label: Text(AppLocalizations.of(context).simulate, style: const TextStyle(color: Colors.white)),
                          ),
                        ),
                      ]),
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (_projections.isNotEmpty) ...[
                    Text(AppLocalizations.of(context).savedProjections, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                    ..._projections.map((p) {
                      final proj = p as Map<String, dynamic>;
                      return Card(
                        child: ListTile(
                          title: Text(proj['nom']?.toString() ?? '', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                          subtitle: Text('${proj['effectifActuel']} → ${proj['effectifProjete']} membres (${proj['tauxCroissanceAnnuel']}%/an)', style: const TextStyle(fontSize: 12)),
                          trailing: Chip(label: Text(proj['typeProjection']?.toString() ?? '', style: const TextStyle(fontSize: 10))),
                        ),
                      );
                    }),
                  ],
                ],
              ),
            ),
    );
  }
}
