import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Défis hebdomadaires — branché sur GET /api/v1/weekly-challenges.
class WeeklyChallengesScreen extends StatefulWidget {
  const WeeklyChallengesScreen({super.key, this.apiService});
  final ApiService? apiService;

  @override
  State<WeeklyChallengesScreen> createState() => _WeeklyChallengesScreenState();
}

class _WeeklyChallengesScreenState extends State<WeeklyChallengesScreen> {
  late final ApiService _api = widget.apiService ?? ApiService();
  List<dynamic> _items = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() { _loading = true; _error = null; });
    try {
      final res = await _api.get('/weekly-challenges');
      final d = res.data;
      setState(() {
        _items = d is List ? d : (d is Map ? (d['content'] as List<dynamic>? ?? []) : <dynamic>[]);
        _loading = false;
      });
    } catch (e) {
      setState(() { _error = 'Impossible de charger les défis.'; _loading = false; });
    }
  }

  Color _difficultyColor(String? d) {
    switch (d) {
      case 'EASY': return Colors.green;
      case 'HARD': return Colors.red;
      default: return Colors.orange;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('🏆 Défis hebdomadaires'), backgroundColor: Colors.deepPurple, foregroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Padding(padding: const EdgeInsets.all(24), child: Text(_error!, textAlign: TextAlign.center)),
                  ElevatedButton(onPressed: _load, child: const Text('Réessayer')),
                ]))
              : _items.isEmpty
                  ? const Center(child: Text('Aucun défi actif pour le moment.'))
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView.builder(
                        padding: const EdgeInsets.all(16),
                        itemCount: _items.length,
                        itemBuilder: (context, i) {
                          final c = _items[i] as Map<String, dynamic>;
                          final progress = (c['progress'] as num?)?.toInt() ?? 0;
                          return Card(
                            margin: const EdgeInsets.only(bottom: 10),
                            child: ListTile(
                              leading: CircleAvatar(backgroundColor: _difficultyColor(c['difficulty']?.toString()).withValues(alpha: .15), child: Text('${c['xpReward'] ?? 50}', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold))),
                              title: Text(c['title']?.toString() ?? 'Défi', style: const TextStyle(fontWeight: FontWeight.w600)),
                              subtitle: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                if ((c['description'] ?? '').toString().isNotEmpty)
                                  Padding(padding: const EdgeInsets.only(top: 2, bottom: 6), child: Text(c['description'].toString(), maxLines: 2, overflow: TextOverflow.ellipsis)),
                                LinearProgressIndicator(value: progress / 100, minHeight: 5, borderRadius: BorderRadius.circular(3)),
                                const SizedBox(height: 2),
                                Text('$progress% · semaine ${c['weekNumber'] ?? '?'}', style: const TextStyle(fontSize: 11)),
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
