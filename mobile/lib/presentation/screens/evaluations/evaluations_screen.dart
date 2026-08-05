import 'package:flutter/material.dart';
import '../../widgets/glass_theme.dart';
import '../../widgets/app_drawer.dart';
import '../../../data/services/api_service.dart';

class EvaluationsScreen extends StatefulWidget {
  const EvaluationsScreen({super.key});

  @override
  State<EvaluationsScreen> createState() => _EvaluationsScreenState();
}

class _EvaluationsScreenState extends State<EvaluationsScreen> {
  final _apiService = ApiService();
  List<dynamic> _evaluations = [];
  Map<String, dynamic>? _stats;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final evalRes = await _apiService.get('/evaluations/me/list');
      final evalData = evalRes.data;
      if (mounted) {
        setState(() {
          _evaluations = (evalData is Map ? evalData['content'] : evalData) as List<dynamic>? ?? [];
          // Calculate stats
          if (_evaluations.isNotEmpty) {
            final Map<String, List<int>> cats = {};
            for (final e in _evaluations) {
              final cat = (e as Map)['categorie'] ?? 'AUTRE';
              final note = e['note'] ?? 0;
              cats.putIfAbsent(cat, () => []).add(note as int);
            }
            _stats = {};
            for (final entry in cats.entries) {
              final notes = entry.value;
              _stats![entry.key] = {
                'moyenne': notes.isNotEmpty ? (notes.reduce((a, b) => a + b) / notes.length).toStringAsFixed(1) : '0',
                'total': notes.length,
                'min': notes.isNotEmpty ? notes.reduce((a, b) => a < b ? a : b) : 0,
                'max': notes.isNotEmpty ? notes.reduce((a, b) => a > b ? a : b) : 0,
              };
            }
          }
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Color _catColor(String? cat) {
    switch (cat) {
      case 'PRESENCE': return Colors.green;
      case 'SPIRITUALITE': return Colors.purple;
      case 'FORMATION': return Colors.blue;
      case 'IMPLICATION': return Colors.orange;
      case 'FAISEUR': return Colors.teal;
      case 'CHEF_FAMILLE': return Colors.amber;
      case 'RESPONSABLE': return Colors.indigo;
      default: return Colors.grey;
    }
  }

  IconData _catIcon(String? cat) {
    switch (cat) {
      case 'PRESENCE': return Icons.check_circle;
      case 'SPIRITUALITE': return Icons.auto_awesome;
      case 'FORMATION': return Icons.school;
      case 'IMPLICATION': return Icons.star;
      default: return Icons.analytics;
    }
  }

  @override
  Widget build(BuildContext context) {
    final avgAll = _evaluations.isNotEmpty
        ? (_evaluations.map((e) => (e as Map)['note'] ?? 0).fold(0, (a, b) => a + b as int) / _evaluations.length).toStringAsFixed(1)
        : '—';

    return Scaffold(
      appBar: AppBar(
        title: const Text('Évaluations'),
        actions: [
          IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData),
        ],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : RefreshIndicator(
              onRefresh: _loadData,
              child: ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  // Global average
                  GlassCard(
                    padding: const EdgeInsets.all(20),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(16),
                          decoration: BoxDecoration(
                            color: Colors.amber.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Text(avgAll, style: const TextStyle(color: Colors.amber, fontSize: 28, fontWeight: FontWeight.bold)),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Note moyenne', style: TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w600)),
                              Text('${_evaluations.length} évaluations', style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 13)),
                            ],
                          ),
                        ),
                        const Icon(Icons.star, color: Colors.amber, size: 28),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Category breakdown
                  if (_stats != null && _stats!.isNotEmpty) ...[
                    Text('Par catégorie', style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 16, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 8),
                    ..._stats!.entries.map((entry) {
                      final cat = entry.value as Map;
                      final moyenne = cat['moyenne'];
                      final total = cat['total'];
                      return Container(
                        margin: const EdgeInsets.only(bottom: 8),
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: Colors.white.withValues(alpha: 0.04),
                          borderRadius: BorderRadius.circular(12),
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: _catColor(entry.key).withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Icon(_catIcon(entry.key), color: _catColor(entry.key), size: 18),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(entry.key, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 14)),
                                  Text('$total évaluations', style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                ],
                              ),
                            ),
                            Text('$moyenne/5', style: TextStyle(color: Colors.amber, fontWeight: FontWeight.bold, fontSize: 16)),
                          ],
                        ),
                      );
                    }),
                    const SizedBox(height: 12),
                  ],

                  // Individual evaluations
                  if (_evaluations.isNotEmpty) ...[
                    Text('Détail', style: TextStyle(color: Colors.white.withValues(alpha: 0.8), fontSize: 16, fontWeight: FontWeight.w600)),
                    const SizedBox(height: 8),
                    ..._evaluations.map((e) {
                      final eval = e as Map;
                      final categorie = eval['categorie'] ?? '—';
                      final note = eval['note'] ?? 0;
                      final commentaire = eval['commentaire'] ?? '';
                      final date = eval['createdAt']?.toString().substring(0, 10) ?? '';
                      return GlassCard(
                        margin: const EdgeInsets.only(bottom: 8),
                        padding: const EdgeInsets.all(12),
                        child: Row(
                          children: [
                            Container(
                              width: 40, height: 40,
                              decoration: BoxDecoration(
                                color: _catColor(categorie).withValues(alpha: 0.15),
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Center(child: Text('$note', style: TextStyle(color: _catColor(categorie), fontWeight: FontWeight.bold, fontSize: 16))),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(categorie, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w500, fontSize: 13)),
                                  if (commentaire.toString().isNotEmpty)
                                    Text(commentaire, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11), maxLines: 1, overflow: TextOverflow.ellipsis),
                                ],
                              ),
                            ),
                            Text(date, style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 10)),
                          ],
                        ),
                      );
                    }),
                  ],

                  if (_evaluations.isEmpty)
                    GlassCard(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        children: [
                          Icon(Icons.star_outline, size: 48, color: Colors.white.withValues(alpha: 0.3)),
                          const SizedBox(height: 12),
                          Text('Aucune évaluation', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
                        ],
                      ),
                    ),
                ],
              ),
            ),
    );
  }
}
