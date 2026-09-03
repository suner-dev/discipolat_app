import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Reconnaissance faciale — branché sur /api/v1/face.
class FaceRecognitionScreen extends StatefulWidget {
  const FaceRecognitionScreen({super.key});

  @override
  State<FaceRecognitionScreen> createState() => _FaceRecognitionScreenState();
}

class _FaceRecognitionScreenState extends State<FaceRecognitionScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _stats;
  List<dynamic> _templates = [];
  bool _isLoading = true;
  String? _error;
  String? _identifyResult;

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
      final results = await Future.wait([
        _apiService.get('/face/stats'),
        _apiService.get('/face/templates'),
      ]);
      if (mounted) {
        final sData = results[0].data;
        final tData = results[1].data;
        setState(() {
          _stats = sData is Map<String, dynamic> ? sData : null;
          _templates = tData is List ? tData : (tData is Map && tData['content'] is List ? tData['content'] as List<dynamic> : <dynamic>[]);
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

  Future<void> _enroll() async {
    final nameCtrl = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF16213A),
        title: const Text('Enrôler un visage', style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: nameCtrl,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            hintText: 'Nom du membre',
            hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
            filled: true,
            fillColor: Colors.white.withValues(alpha: 0.06),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, nameCtrl.text), child: const Text('Enrôler')),
        ],
      ),
    );
    if (result != null && result.isNotEmpty) {
      try {
        await _apiService.post('/face/enroll', data: {'nom': result});
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Visage enrôlé'), backgroundColor: Colors.green),
          );
          _loadData();
        }
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Erreur lors de l\'enrôlement'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Future<void> _identify() async {
    try {
      final res = await _apiService.post('/face/identify-configurable', data: {});
      if (mounted) {
        final data = res.data;
        setState(() {
          _identifyResult = data is Map ? (data['nom'] ?? data['message'] ?? 'Identifié') : 'Identifié';
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Résultat: $_identifyResult'), backgroundColor: Colors.green),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erreur lors de l\'identification'), backgroundColor: Colors.red),
        );
      }
    }
  }

  Future<void> _deleteTemplate(String id) async {
    try {
      await _apiService.delete('/face/templates/$id');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Template supprimé'), backgroundColor: Colors.orange),
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Reconnaissance faciale'),
        backgroundColor: Colors.indigo.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      if (_stats != null) _buildStatsCard(),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: _enroll,
                              icon: const Icon(Icons.person_add, size: 16),
                              label: const Text('Enrôler'),
                              style: FilledButton.styleFrom(backgroundColor: Colors.indigo.shade700),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: _identify,
                              icon: const Icon(Icons.face, size: 16),
                              label: const Text('Identifier'),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      const Text('Templates enregistrés',
                          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
                      const SizedBox(height: 8),
                      if (_templates.isEmpty)
                        _buildEmpty()
                      else
                        ..._templates.map((t) {
                          final tmpl = t as Map<String, dynamic>;
                          final id = tmpl['id']?.toString() ?? '';
                          return GlassCard(
                            margin: const EdgeInsets.only(bottom: 8),
                            padding: const EdgeInsets.all(14),
                            child: Row(
                              children: [
                                Icon(Icons.face, color: Colors.indigo.shade300, size: 24),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Text(tmpl['nom']?.toString() ?? tmpl['name']?.toString() ?? 'Template',
                                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                ),
                                IconButton(
                                  icon: Icon(Icons.delete_outline, color: Colors.white.withValues(alpha: 0.3), size: 20),
                                  onPressed: () => _deleteTemplate(id),
                                ),
                              ],
                            ),
                          );
                        }),
                    ],
                  ),
                ),
    );
  }

  Widget _buildStatsCard() {
    return GlassCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Statistiques', style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16)),
          const SizedBox(height: 12),
          Row(
            children: [
              _statItem('Templates', '${_stats!['totalTemplates'] ?? _stats!['total'] ?? 0}', Colors.indigo),
              const SizedBox(width: 12),
              _statItem('Identifications', '${_stats!['totalIdentifications'] ?? 0}', Colors.green),
            ],
          ),
        ],
      ),
    );
  }

  Widget _statItem(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: color.withValues(alpha: 0.1), borderRadius: BorderRadius.circular(12)),
        child: Column(
          children: [
            Text(value, style: TextStyle(color: color, fontSize: 20, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            Text(label, style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 11)),
          ],
        ),
      ),
    );
  }

  Widget _buildEmpty() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Text('Aucun template enregistré', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
