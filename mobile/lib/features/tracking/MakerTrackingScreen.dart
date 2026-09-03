import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Suivi du faiseur — branché sur /api/v1/maker-tracking.
class MakerTrackingScreen extends StatefulWidget {
  const MakerTrackingScreen({super.key});

  @override
  State<MakerTrackingScreen> createState() => _MakerTrackingScreenState();
}

class _MakerTrackingScreenState extends State<MakerTrackingScreen> {
  final _apiService = ApiService();
  List<dynamic> _events = [];
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
      final res = await _apiService.get('/maker-tracking');
      if (mounted) {
        final data = res.data;
        setState(() {
          _events = data is List ? data : (data is Map && data['content'] is List ? data['content'] as List<dynamic> : <dynamic>[]);
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

  Future<void> _addEvent() async {
    final descCtrl = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF16213A),
        title: const Text('Nouvel événement', style: TextStyle(color: Colors.white)),
        content: TextField(
          controller: descCtrl,
          style: const TextStyle(color: Colors.white),
          decoration: InputDecoration(
            hintText: 'Description...',
            hintStyle: TextStyle(color: Colors.white.withValues(alpha: 0.4)),
            filled: true,
            fillColor: Colors.white.withValues(alpha: 0.06),
            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, descCtrl.text), child: const Text('Ajouter')),
        ],
      ),
    );
    if (result != null && result.isNotEmpty) {
      try {
        await _apiService.post('/maker-tracking', data: {'description': result});
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Événement ajouté'), backgroundColor: Colors.green),
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

  Future<void> _deleteEvent(String id) async {
    try {
      await _apiService.delete('/maker-tracking/$id');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Événement supprimé'), backgroundColor: Colors.orange),
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
        title: const Text('Suivi faiseur'),
        backgroundColor: Colors.cyan,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _addEvent,
        icon: const Icon(Icons.add, size: 18),
        label: const Text('Ajouter'),
        backgroundColor: Colors.cyan,
      ),
      body: _isLoading
          ? const ShimmerLoading(itemCount: 5)
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: _events.isEmpty
                      ? _buildEmpty()
                      : ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: _events.length,
                          itemBuilder: (context, i) {
                            final e = _events[i] as Map<String, dynamic>;
                            final id = e['id']?.toString() ?? '';
                            final dateStr = e['createdAt']?.toString().substring(0, 16) ?? '';
                            return GlassCard(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(14),
                              child: Row(
                                children: [
                                  Container(
                                    width: 40,
                                    height: 40,
                                    decoration: BoxDecoration(
                                      color: Colors.cyan.withValues(alpha: 0.15),
                                      borderRadius: BorderRadius.circular(10),
                                    ),
                                    child: const Icon(Icons.gps_fixed, color: Colors.cyan, size: 20),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment: CrossAxisAlignment.start,
                                      children: [
                                        Text(e['description']?.toString() ?? 'Événement',
                                            style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600, fontSize: 14)),
                                        if (dateStr.isNotEmpty)
                                          Text(dateStr, style: TextStyle(color: Colors.white.withValues(alpha: 0.4), fontSize: 11)),
                                      ],
                                    ),
                                  ),
                                  IconButton(
                                    icon: Icon(Icons.delete_outline, color: Colors.white.withValues(alpha: 0.3), size: 20),
                                    onPressed: () => _deleteEvent(id),
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
          Icon(Icons.gps_off, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucun événement de suivi', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
