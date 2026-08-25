import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Défis spirituels — branché sur GET /api/v1/spiritual-challenges.
class SpiritualChallengesScreen extends StatefulWidget {
  const SpiritualChallengesScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<SpiritualChallengesScreen> createState() => _SpiritualChallengesScreenState();
}

class _SpiritualChallengesScreenState extends State<SpiritualChallengesScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() { super.initState(); _load(); }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/spiritual-challenges');
      final d = res.data;
      setState(() { _items = d is List ? d : <dynamic>[]; _loading = false; });
    } catch (_) {
      setState(() { _error = 'Impossible de charger les défis spirituels.'; _loading = false; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('🔥 Défis spirituels'), backgroundColor: Colors.redAccent, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: const Text('Réessayer')),
                ]))
              : _items.isEmpty
                  ? const Center(child: Text('Aucun défi en cours.'))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final c = _items[i] as Map<String, dynamic>;
                          final objectif = (c['objectifJours'] as num?)?.toInt() ?? 7;
                          final completes = (c['joursComplétés'] as num?)?.toInt() ?? 0;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: Colors.redAccent.withValues(alpha: .15), child: Text('$completes/$objectif', style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold))),
                              title: Text(c['titre']?.toString() ?? 'Défi', style: const TextStyle(fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                if ((c['description'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2, bottom: 4), child: Text(c['description'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis)),
                                LinearProgressIndicator(value: objectif > 0 ? completes / objectif : 0, minHeight: 5, borderRadius: BorderRadius.circular(3)),
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
