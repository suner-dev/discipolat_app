import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Aide d'urgence — branché sur /api/v1/aid/emergency.
class EmergencyAidScreen extends StatefulWidget {
  const EmergencyAidScreen({super.key});

  @override
  State<EmergencyAidScreen> createState() => _EmergencyAidScreenState();
}

class _EmergencyAidScreenState extends State<EmergencyAidScreen> {
  final _apiService = ApiService();
  List<dynamic> _requests = [];
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
      final res = await _apiService.get('/aid/emergency/open');
      if (mounted) {
        final data = res.data;
        setState(() {
          _requests = data is List ? data : (data is Map && data['content'] is List ? data['content'] as List<dynamic> : <dynamic>[]);
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

  Future<void> _createRequest() async {
    final descCtrl = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF16213A),
        title: const Text('Nouvelle demande d\'aide', style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: descCtrl,
          style: const TextStyle(color: Colors.white),
          maxLines: 3,
          decoration: InputDecoration(
            hintText: 'Décrivez la situation...',
            hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
            filled: true,
            fillColor: Colors.white.withValues(alpha: 0.06),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, descCtrl.text),
            child: const Text('Envoyer'),
          ),
        ],
      ),
    );
    if (result != null && result.isNotEmpty) {
      try {
        await _apiService.post('/aid/emergency', data: {'description': result});
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Demande d\'aide créée'), backgroundColor: Colors.green),
          );
          _loadData();
        }
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Erreur lors de la création'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }

  Future<void> _collectAid(String id) async {
    try {
      await _apiService.post('/aid/emergency/$id/collect');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Aide marquée comme collectée'), backgroundColor: Colors.green),
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

  Future<void> _resolveAid(String id) async {
    try {
      await _apiService.post('/aid/emergency/$id/resolve');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Aide résolue'), backgroundColor: Colors.green),
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

  Color _urgencyColor(String? u) {
    switch (u) {
      case 'CRITIQUE':
        return Colors.red;
      case 'HAUTE':
        return Colors.orange;
      case 'MOYENNE':
        return Colors.amber;
      default:
        return Colors.grey;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Aide d\'urgence'),
        backgroundColor: Colors.red.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _createRequest,
        icon: const Icon(Icons.add, size: 18),
        label: const Text('Demander aide'),
        backgroundColor: Colors.red.shade700,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 4)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: _requests.isEmpty
                      ? _buildEmpty()
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _requests.length,
                          itemBuilder: (context, i) {
                            final r = _requests[i] as Map<String, dynamic>;
                            final urgence = r['niveauUrgence']?.toString() ?? 'MOYENNE';
                            final statut = r['statut']?.toString() ?? 'OUVERTE';
                            final id = r['id']?.toString() ?? '';
                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.warning_amber, color: _urgencyColor(urgence), size: 20),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(r['description']?.toString() ?? 'Demande d\'aide',
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                      ),
                                      StatusBadge(label: urgence, color: _urgencyColor(urgence)),
                                    ],
                                  ),
                                  const SizedBox(height: 8),
                                  Text('Statut: $statut',
                                      style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12)),
                                  if (statut == 'OUVERTE') ...[
                                    const SizedBox(height: 12),
                                    Row(
                                      children: [
                                        Expanded(
                                          child: FilledButton.icon(
                                            onPressed: () => _collectAid(id),
                                            icon: const Icon(Icons.check, size: 16),
                                            label: const Text('Collecter'),
                                            style: FilledButton.styleFrom(backgroundColor: Colors.orange),
                                          ),
                                        ),
                                        const SizedBox(width: 8),
                                        Expanded(
                                          child: FilledButton.icon(
                                            onPressed: () => _resolveAid(id),
                                            icon: const Icon(Icons.done_all, size: 16),
                                            label: const Text('Résoudre'),
                                            style: FilledButton.styleFrom(backgroundColor: Colors.green),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
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
          Icon(Icons.volunteer_activism, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune aide d\'urgence ouverte', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
