import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../core/format.dart';
import '../../../../l10n/app_localizations.dart';

/// Bénévoles — branché sur GET /api/v1/volunteers.
class VolunteersScreen extends StatefulWidget {
  const VolunteersScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<VolunteersScreen> createState() => _VolunteersScreenState();
}

class _VolunteersScreenState extends State<VolunteersScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/volunteers');
      final d = res.data;
      setState(() {
        _items = d is List ? d : (d is Map ? (d['content'] as List<dynamic>? ?? []) : <dynamic>[]);
        _loading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).volunteersError; _loading = false; });
    }
  }

  Color _statutColor(String? s) => s == 'ACTIF' ? Colors.green : (s == 'SUSPENDU' ? Colors.orange : Colors.grey);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).volunteersTitle), backgroundColor: Colors.teal, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).volunteersEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final v = _items[i] as Map<String, dynamic>;
                          final statut = v['statut']?.toString() ?? 'ACTIF';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: _statutColor(statut).withValues(alpha: .15), child: Icon(Icons.volunteer_activism, color: _statutColor(statut), size: 20)),
                              title: Text('Bénévole #${(v['membreId'] ?? '').toString().substring(0, v['membreId']?.toString().length.clamp(0, 8) ?? 0)}', style: const TextStyle(fontWeight: FontWeight.w600)),
                              subtitle: Text('${v['heuresMois'] ?? 0} h/mois · ${v['nbEvenements'] ?? 0} événements · ${v['disponibilite'] ?? '?'}'),
                              trailing: Container(padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2), decoration: BoxDecoration(color: _statutColor(statut).withValues(alpha: .12), borderRadius: BorderRadius.circular(10)), child: Text(formatLabel(statut), style: TextStyle(fontSize: 10, color: _statutColor(statut), fontWeight: FontWeight.bold))),
                            ),
                          );
                        },
                      ),
                    ),
    );
  }
}
