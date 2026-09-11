import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../data/services/api_service.dart';

class AiFamilyCohesionScreen extends ConsumerStatefulWidget {
  const AiFamilyCohesionScreen({super.key});
  @override
  ConsumerState<AiFamilyCohesionScreen> createState() => _AiFamilyCohesionScreenState();
}

class _AiFamilyCohesionScreenState extends ConsumerState<AiFamilyCohesionScreen> {
  List<Map<String, dynamic>> _families = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    try {
      final api = ref.read(apiServiceProvider);
      final res = await api.get('/ai/module/families/cohesion');
      if (mounted) {
        setState(() {
          _families = List<Map<String, dynamic>>.from(res ?? []);
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Cohésion Familiale'),
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _families.isEmpty
              ? const Center(child: Text('Aucune famille à analyser'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: _families.length,
                  itemBuilder: (context, index) {
                    final f = _families[index];
                    final score = (f['cohesionScore'] ?? 0) as int;
                    final level = f['cohesionLevel'] ?? 'INCONNU';
                    return Card(
                      margin: const EdgeInsets.only(bottom: 12),
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundColor: _scoreColor(score),
                          child: Text('$score', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                        ),
                        title: Text(f['familyName'] ?? 'Famille'),
                        subtitle: Text('${f['memberCount'] ?? 0} membres · $level'),
                        trailing: const Icon(Icons.chevron_right),
                        onTap: () => _showFamilyDetails(f['familyId']),
                      ),
                    );
                  },
                ),
    );
  }

  Color _scoreColor(int score) {
    if (score >= 75) return Colors.green;
    if (score >= 50) return Colors.orange;
    return Colors.red;
  }

  void _showFamilyDetails(String familyId) async {
    try {
      final api = ref.read(apiServiceProvider);
      final res = await api.get('/ai/module/family/$familyId/cohesion');
      if (mounted && res is Map) {
        showModalBottomSheet(
          context: context,
          builder: (ctx) => Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(res['familyName'] ?? '', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                const SizedBox(height: 8),
                Text('Score: ${res['cohesionScore'] ?? '—'} (${res['cohesionLevel'] ?? '—'})'),
                Text('Membres: ${res['memberCount'] ?? '—'}'),
                Text('Présence moyenne: ${res['averagePresence'] ?? '—')}%'),
                const SizedBox(height: 12),
                const Text('Recommandations:', style: TextStyle(fontWeight: FontWeight.bold)),
                ...((res['recommendations'] ?? []) as List).map((r) => Text('• $r')),
              ],
            ),
          ),
        );
      }
    } catch (e) {
      // Ignore
    }
  }
}
