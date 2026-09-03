import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Narratifs KPI — branché sur /api/v1/kpi-narrative.
class KpiNarrativeScreen extends StatefulWidget {
  const KpiNarrativeScreen({super.key});

  @override
  State<KpiNarrativeScreen> createState() => _KpiNarrativeScreenState();
}

class _KpiNarrativeScreenState extends State<KpiNarrativeScreen> {
  final _apiService = ApiService();
  List<dynamic> _narratives = [];
  bool _isLoading = true;
  String? _error;
  bool _isGenerating = false;
  String _selectedType = 'all';
  final _types = const [
    {'value': 'all', 'label': 'Tous'},
    {'value': 'CROISSANCE', 'label': 'Croissance'},
    {'value': 'PRESENCE', 'label': 'Présence'},
    {'value': 'EVANGELISATION', 'label': 'Évangélisation'},
    {'value': 'DISCIPLEMAT', 'label': 'Discipulat'},
  ];

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final path = _selectedType == 'all'
          ? '/kpi-narrative/type/all'
          : '/kpi-narrative/type/$_selectedType';
      final res = await _apiService.get(path);
      if (mounted) {
        final data = res.data;
        setState(() {
          _narratives = data is List
              ? data
              : (data is Map && data['content'] is List
                  ? data['content'] as List<dynamic>
                  : <dynamic>[]);
          _isLoading = false;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _error = 'Erreur lors du chargement';
          _isLoading = false;
        });
      }
    }
  }

  Future<void> _generateAll() async {
    setState(() => _isGenerating = true);
    try {
      await _apiService.post('/kpi-narrative/generate-all');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Narratifs générés avec succès'), backgroundColor: Colors.green),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de la génération'), backgroundColor: Colors.red),
        );
      }
    } finally {
      if (mounted) setState(() => _isGenerating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Narratifs KPI'),
        backgroundColor: AppColors.primary,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _isGenerating ? null : _generateAll,
        icon: _isGenerating
            ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
            : const Icon(Icons.auto_awesome, size: 18),
        label: Text(_isGenerating ? 'Génération...' : 'Générer tout'),
        backgroundColor: Colors.purple,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                      child: SizedBox(
                        height: 36,
                        child: ListView.separated(
                          scrollDirection: Axis.horizontal,
                          itemCount: _types.length,
                          separatorBuilder: (_, __) => const SizedBox(width: 8),
                          itemBuilder: (context, i) {
                            final t = _types[i];
                            final selected = t['value'] == _selectedType;
                            return FilterChip(
                              label: Text(t['label']!, style: TextStyle(fontSize: 12, color: selected ? Colors.white : Colors.white70)),
                              selected: selected,
                              selectedColor: Colors.purple,
                              backgroundColor: Colors.white.withValues(alpha: 0.06),
                              onSelected: (_) {
                                setState(() => _selectedType = t['value']!);
                                _loadData();
                              },
                            );
                          },
                        ),
                      ),
                    ),
                    Expanded(
                      child: RefreshIndicator(
                        onRefresh: _loadData,
                        child: _narratives.isEmpty
                            ? _buildEmpty()
                            : ListView.builder(
                                padding: const EdgeInsets.all(16),
                                itemCount: _narratives.length,
                                itemBuilder: (context, i) {
                                  final n = _narratives[i] as Map<String, dynamic>;
                                  final titre = n['titre']?.toString() ?? n['nom']?.toString() ?? 'Narratif';
                                  final contenu = n['contenu']?.toString() ?? n['texte']?.toString() ?? '';
                                  final type = n['type']?.toString() ?? '';
                                  final periode = n['periode']?.toString() ?? '';
                                  final dateStr = n['createdAt']?.toString().substring(0, 10) ?? '';

                                  return GlassCard(
                                    margin: const EdgeInsets.only(bottom: 10),
                                    padding: const EdgeInsets.all(14),
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          children: [
                                            Icon(Icons.auto_awesome, color: Colors.purple, size: 18),
                                            const SizedBox(width: 8),
                                            Expanded(
                                              child: Text(titre,
                                                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                            ),
                                          ],
                                        ),
                                        if (type.isNotEmpty || periode.isNotEmpty) ...[
                                          const SizedBox(height: 8),
                                          Row(
                                            children: [
                                              if (type.isNotEmpty) StatusBadge(label: type, color: Colors.purple),
                                              if (type.isNotEmpty && periode.isNotEmpty) const SizedBox(width: 8),
                                              if (periode.isNotEmpty)
                                                Text(periode,
                                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                              if (periode.isEmpty && dateStr.isNotEmpty)
                                                Text(dateStr,
                                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                            ],
                                          ),
                                        ],
                                        if (contenu.isNotEmpty) ...[
                                          const SizedBox(height: 10),
                                          Text(contenu,
                                              style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 13, height: 1.4),
                                              maxLines: 6,
                                              overflow: TextOverflow.ellipsis),
                                        ],
                                      ],
                                    ),
                                  );
                                },
                              ),
                      ),
                    ),
                  ],
                ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.auto_awesome, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun narratif disponible', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
          const SizedBox(height: 8),
          Text('Appuyez sur "Générer tout" pour créer des narratifs',
              style: TextStyle(color: Colors.white.withValues(alpha: 0.3), fontSize: 12)),
        ],
      ),
    );
  }

  Widget _buildError() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.error_outline, color: Colors.white.withValues(alpha: 0.3), size: 48),
          const SizedBox(height: 12),
          Text(_error!, style: TextStyle(color: Colors.white.withValues(alpha: 0.5)), textAlign: TextAlign.center),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: _loadData, icon: const Icon(Icons.refresh, size: 16), label: const Text('Réessayer')),
        ],
      ),
    );
  }
}
