import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';
import '../../../l10n/app_localizations.dart';

/// Objectifs personnels — branché sur GET /api/v1/personal-objectives.
class PersonalObjectivesScreen extends StatefulWidget {
  const PersonalObjectivesScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<PersonalObjectivesScreen> createState() => _PersonalObjectivesScreenState();
}

class _PersonalObjectivesScreenState extends State<PersonalObjectivesScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/personal-objectives');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      if (!mounted) return;
      setState(() { _error = AppLocalizations.of(context).personalObjectivesError; _loading = false; });
    }
  }

  Color _statutColor(String? s) => s == 'COMPLÉTÉ' || s == 'TERMINE' ? Colors.green : (s == 'EN_COURS' ? Colors.blue : Colors.grey);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(AppLocalizations.of(context).personalObjectivesTitle), backgroundColor: Colors.teal, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: Text(AppLocalizations.of(context).retry)),
                ]))
              : _items.isEmpty
                  ? Center(child: Text(AppLocalizations.of(context).personalObjectivesEmpty))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final o = _items[i] as Map<String, dynamic>;
                          final cible = (o['objectifCible'] as num?)?.toInt() ?? 1;
                          final progression = (o['progressionActuelle'] as num?)?.toInt() ?? 0;
                          final statut = o['statut']?.toString() ?? 'EN_COURS';
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: _statutColor(statut).withValues(alpha: .15), child: Icon(Icons.flag, color: _statutColor(statut), size: 20)),
                              title: Text(o['titre']?.toString() ?? 'Objectif', style: const TextStyle(fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                if ((o['description'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2, bottom: 4), child: Text(o['description'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis)),
                                LinearProgressIndicator(value: cible > 0 ? progression / cible : 0, minHeight: 5, borderRadius: BorderRadius.circular(3)),
                                Text('$progression / $cible', style: const TextStyle(fontSize: 11)),
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
