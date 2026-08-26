import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Comparaison d'églises — branché sur GET /api/v1/church-comparisons.
class ChurchComparisonScreen extends StatefulWidget {
  const ChurchComparisonScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<ChurchComparisonScreen> createState() => _ChurchComparisonScreenState();
}

class _ChurchComparisonScreenState extends State<ChurchComparisonScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/church-comparisons');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).churchComparisonError; _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).churchComparisonTitle), backgroundColor: Colors.pinkAccent.shade400, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).churchComparisonEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final c = _items[i] as Map<String, dynamic>;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: Padding(
                              padding: const EdgeInsets.all(12),
                              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Text(c['nomEglise']?.toString() ?? 'Église', style: const TextStyle(fontWeight: FontWeight.bold)),
                                const SizedBox(height: 6),
                                Wrap(spacing: 8, runSpacing: 4, children: [
                                  _chip('Effectif', '${c['effectif'] ?? 0}'),
                                  _chip('Présence', '${((c['tauxPresence'] as num?) ?? 0).toStringAsFixed(0)}%'),
                                  _chip('Conversion', '${((c['tauxConversion'] as num?) ?? 0).toStringAsFixed(1)}%'),
                                  _chip('Rétention', '${((c['tauxRetention'] as num?) ?? 0).toStringAsFixed(0)}%'),
                                  _chip('Score', '${((c['scoreSpirituelMoyen'] as num?) ?? 0).toStringAsFixed(0)}'),
                                ]),
                              ]),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }

  Widget _chip(String label, String value) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(color: Colors.pink.withValues(alpha: .08), borderRadius: BorderRadius.circular(8)),
      child: Text('$label: $value', style: const TextStyle(fontSize: 11)),
    );
  }
}
