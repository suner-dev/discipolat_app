import 'package:flutter/material.dart';
import '../../../data/services/api_service.dart';

/// Skills Matrix screen - member competency evaluation
class SkillsMatrixScreen extends StatefulWidget {
  const SkillsMatrixScreen({super.key});

  @override
  State<SkillsMatrixScreen> createState() => _SkillsMatrixScreenState();
}

class _SkillsMatrixScreenState extends State<SkillsMatrixScreen> {
  final _api = ApiService();
  List<dynamic> evaluations = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    _loadEvaluations();
  }

  Future<void> _loadEvaluations() async {
    try {
      final res = await _api.get('/skills-matrix');
      setState(() {
        evaluations = res.data is List ? res.data : [];
        loading = false;
      });
    } catch (_) {
      setState(() => loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    // Group by member
    final Map<String, List<dynamic>> byMember = {};
    for (final e in evaluations) {
      final memberId = e['membreId'] ?? '';
      byMember.putIfAbsent(memberId, () => []).add(e);
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Matrice de Compétences')),
      body: loading
          ? const Center(child: CircularProgressIndicator())
          : evaluations.isEmpty
              ? const Center(child: Text('Aucune évaluation'))
              : ListView.builder(
                  padding: const EdgeInsets.all(16),
                  itemCount: byMember.keys.length,
                  itemBuilder: (context, index) {
                    final memberId = byMember.keys.elementAt(index);
                    final evals = byMember[memberId]!;
                    return Card(
                      margin: const EdgeInsets.only(bottom: 12),
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Membre: ${memberId.toString().substring(0, 8)}...',
                                style: const TextStyle(fontWeight: FontWeight.bold)),
                            const SizedBox(height: 8),
                            Wrap(
                              spacing: 8,
                              runSpacing: 8,
                              children: evals.map((e) => Chip(
                                label: Text('${e['compétence']}: ${e['niveau']}', style: const TextStyle(fontSize: 12)),
                                backgroundColor: Colors.teal.shade50,
                              )).toList(),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
