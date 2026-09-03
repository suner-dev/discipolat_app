import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Compétences du membre — branché sur /api/v1/members/competences.
class MemberCompetencesScreen extends StatefulWidget {
  const MemberCompetencesScreen({super.key});

  @override
  State<MemberCompetencesScreen> createState() => _MemberCompetencesScreenState();
}

class _MemberCompetencesScreenState extends State<MemberCompetencesScreen> {
  final _apiService = ApiService();
  List<dynamic> _competences = [];
  bool _isLoading = true;
  String? _error;

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
      final res = await _apiService.get('/members/competences/mine');
      if (mounted) {
        final data = res.data;
        setState(() {
          _competences = data is List ? data : (data is Map && data['content'] is List ? data['content'] as List<dynamic> : <dynamic>[]);
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

  Future<void> _addCompetence() async {
    final nameCtrl = TextEditingController();
    String level = 'DEBUTANT';
    final result = await showDialog<Map<String, String>>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: const Color(0xFF16213A),
          title: const Text('Ajouter une compétence', style: TextStyle(color: Colors.white)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameCtrl,
                style: const TextStyle(color: Colors.white),
                decoration: InputDecoration(
                  hintText: 'Nom de la compétence',
                  hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
                  filled: true,
                  fillColor: Colors.white.withValues(alpha: 0.06),
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
              ),
              const SizedBox(height: 12),
              DropdownButton<String>(
                value: level,
                isExpanded: true,
                dropdownColor: const Color(0xFF1E2A4A),
                style: const TextStyle(color: Colors.white),
                items: const [
                  DropdownMenuItem(value: 'DEBUTANT', child: Text('Débutant')),
                  DropdownMenuItem(value: 'INTERMEDIAIRE', child: Text('Intermédiaire')),
                  DropdownMenuItem(value: 'AVANCE', child: Text('Avancé')),
                  DropdownMenuItem(value: 'EXPERT', child: Text('Expert')),
                ],
                onChanged: (v) => setDialogState(() => level = v ?? level),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
            FilledButton(
              onPressed: () => Navigator.pop(ctx, {'nom': nameCtrl.text, 'niveau': level}),
              child: const Text('Ajouter'),
            ),
          ],
        ),
      ),
    );
    if (result != null && result['nom']!.isNotEmpty) {
      try {
        await _apiService.post('/members/competences', data: result);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Compétence ajoutée'), backgroundColor: Colors.green),
          );
          _loadData();
        }
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Erreur'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Future<void> _deleteCompetence(String id) async {
    try {
      await _apiService.delete('/members/competences/$id');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Compétence supprimée'), backgroundColor: Colors.orange),
        );
        _loadData();
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Color _levelColor(String? l) {
    switch (l) {
      case 'EXPERT':
        return Colors.purple;
      case 'AVANCE':
        return Colors.blue;
      case 'INTERMEDIAIRE':
        return Colors.teal;
      case 'DEBUTANT':
        return Colors.grey;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Mes compétences'),
        backgroundColor: Colors.purple,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _addCompetence,
        icon: const Icon(Icons.add, size: 18),
        label: const Text('Ajouter'),
        backgroundColor: Colors.purple,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: _competences.isEmpty
                      ? _buildEmpty()
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _competences.length,
                          itemBuilder: (context, i) {
                            final c = _competences[i] as Map<String, dynamic>;
                            final niveau = c['niveau']?.toString() ?? 'DEBUTANT';
                            final id = c['id']?.toString() ?? '';
                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              child: Row(
                                children: [
                                  Container(
                                    width: 44,
                                    height: 44,
                                    decoration: BoxDecoration(
                                      color: _levelColor(niveau).withValues(alpha: 0.15),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: Icon(Icons.psychology, color: _levelColor(niveau), size: 22),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(c['nom']?.toString() ?? 'Compétence',
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                        Text('Niveau: $niveau',
                                            style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                      ],
                                    ),
                                  ),
                                  StatusBadge(label: niveau, color: _levelColor(niveau)),
                                  const SizedBox(width: 8),
                                  IconButton(
                                    icon: Icon(Icons.delete_outline, color: Colors.white.withValues(alpha: 0.3), size: 20),
                                    onPressed: () => _deleteCompetence(id),
                                  ),
                                ],
                              ),
                            );
                          },
                        ),
                ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.psychology, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune compétence enregistrée', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
