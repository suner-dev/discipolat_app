import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// IA Mentorat screen - AI suggestions for family leaders
class MentoratAiScreen extends StatefulWidget {
  const MentoratAiScreen({super.key});

  @override
  State<MentoratAiScreen> createState() => _MentoratAiScreenState();
}

class _MentoratAiScreenState extends State<MentoratAiScreen> {
  final _api = ApiService();
  List<dynamic> suggestions = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadSuggestions();
  }

  Future<void> _loadSuggestions() async {
    try {
      final res = await _api.get('/mentoring/all');
      setState(() {
        suggestions = res.data is List ? res.data : [];
        loading = false;
      });
    } catch (_) {
      setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mentorat IA'),
        actions: [
          IconButton(
            icon: const Icon(Icons.auto_awesome),
            onPressed: () async {
              await _api.post('/mentoring/generate', data: {
                'faiseurs': [],
              });
              _loadSuggestions();
            },
          ),
        ],
      ),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : suggestions.isEmpty
              ? const Center(child: Text('Aucune suggestion — générez-en'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: suggestions.length,
                  itemBuilder: (context, index) {
                    final s = suggestions[index];
                    return Card(
                      margin: const EdgeInsets.only(bottom: 8),
                      child: ExpansionTile(
                        leading: Icon(
                          _prioriteIcon(s['priorité']),
                          color: _prioriteColor(s['priorité']),
                        ),
                        title: Text(s['titre'] ?? '', style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                        subtitle: Text(
                          s['analyse'] ?? '',
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 12),
                        ),
                        children: [
                          Padding(
                            padding: const EdgeInsets.all(16),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Container(
                                  padding: const EdgeInsets.all(8),
                                  decoration: BoxDecoration(
                                    color: Colors.green.shade50,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.lightbulb, size: 16, color: Colors.green),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(s['actionRecommandée'] ?? '', style: const TextStyle(fontSize: 13)),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(height: 8),
                                if (s['raisonnement'] != null)
                                  Text(
                                    '💡 ${s['raisonnement']}',
                                    style: const TextStyle(fontSize: 12, color: Colors.grey),
                                  ),
                                const SizedBox(height: 8),
                                Text(
                                  'Confiance: ${((s['confiance'] ?? 0) * 100).toInt()}%',
                                  style: const TextStyle(fontSize: 11, color: Colors.grey),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    );
                  },
                ),
    );
  }

  IconData _prioriteIcon(String? p) {
    switch (p) {
      case 'HAUTE': return Icons.warning;
      case 'MOYENNE': return Icons.info;
      default: return Icons.check_circle;
    }
  }

  Color _prioriteColor(String? p) {
    switch (p) {
      case 'HAUTE': return Colors.red;
      case 'MOYENNE': return Colors.orange;
      default: return Colors.grey;
    }
  }
}
