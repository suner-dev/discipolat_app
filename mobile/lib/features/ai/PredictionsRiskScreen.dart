import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Prédictions de risques IA — branché sur /api/v1/ai-predictions/risks.
class PredictionsRiskScreen extends StatefulWidget {
  const PredictionsRiskScreen({super.key});

  @override
  State<PredictionsRiskScreen> createState() => _PredictionsRiskScreenState();
}

class _PredictionsRiskScreenState extends State<PredictionsRiskScreen> {
  final _apiService = ApiService();
  List<dynamic> _risks = [];
  bool _isLoading = true;
  String? _error;
  String _selectedType = 'all';
  final _types = const ['all', 'DROPOUT', 'ABSENCE', 'DEMOBILISATION', 'FAMILLE'];

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
      final path = _selectedType == 'all' ? '/ai-predictions/risks' : '/ai-predictions/type/$_selectedType';
      final res = await _apiService.get(path);
      if (mounted) {
        final data = res.data;
        setState(() {
          _risks = data is List ? data : (data is Map && data['content'] is List ? data['content'] as List<dynamic> : <dynamic>[]);
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

  Color _riskColor(String? level) {
    switch (level?.toUpperCase()) {
      case 'CRITIQUE':
        return Colors.red;
      case 'ELEVE':
        return Colors.orange;
      case 'MOYEN':
        return Colors.amber;
      case 'FAIBLE':
        return Colors.green;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Prédictions de risques'),
        backgroundColor: Colors.deepOrange,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
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
                            final selected = t == _selectedType;
                            return FilterChip(
                              label: Text(t == 'all' ? 'Tous' : t, style: TextStyle(fontSize: 12, color: selected ? Colors.white : Colors.white70)),
                              selected: selected,
                              selectedColor: Colors.deepOrange,
                              backgroundColor: Colors.white.withValues(alpha: 0.06),
                              onSelected: (_) {
                                setState(() => _selectedType = t);
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
                        child: _risks.isEmpty
                            ? _buildEmpty()
                            : ListView.builder(
                                padding: const EdgeInsets.all(16),
                                itemCount: _risks.length,
                                itemBuilder: (context, i) {
                                  final r = _risks[i] as Map<String, dynamic>;
                                  final level = r['riskLevel']?.toString() ?? r['niveau']?.toString() ?? 'MOYEN';
                                  final name = r['memberName']?.toString() ?? r['nom']?.toString() ?? 'Membre';
                                  final reason = r['reason']?.toString() ?? r['raison']?.toString() ?? '';
                                  return GlassCard(
                                    margin: const EdgeInsets.only(bottom: 10),
                                    padding: const EdgeInsets.all(14),
                                    child: Row(
                                      children: [
                                        Container(
                                          width: 48,
                                          height: 48,
                                          decoration: BoxDecoration(
                                            color: _riskColor(level).withValues(alpha: 0.15),
                                            borderRadius: BorderRadius.circular(12),
                                          ),
                                          child: Icon(Icons.warning_amber, color: _riskColor(level), size: 24),
                                        ),
                                        const SizedBox(width: 12),
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text(name,
                                                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                              if (reason.isNotEmpty)
                                                Text(reason,
                                                    style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12),
                                                    maxLines: 2,
                                                    overflow: TextOverflow.ellipsis),
                                            ],
                                          ),
                                        ),
                                        StatusBadge(label: level, color: _riskColor(level)),
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
          Icon(Icons.check_circle_outline, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun risque détecté', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
