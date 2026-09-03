import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Jumeau numérique — branché sur GET /api/v1/twin/snapshot.
class TwinSnapshotScreen extends StatefulWidget {
  const TwinSnapshotScreen({super.key});

  @override
  State<TwinSnapshotScreen> createState() => _TwinSnapshotScreenState();
}

class _TwinSnapshotScreenState extends State<TwinSnapshotScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _snapshot;
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
      final res = await _apiService.get('/twin/snapshot');
      if (mounted) {
        setState(() {
          _snapshot = res.data is Map<String, dynamic> ? res.data as Map<String, dynamic> : null;
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Jumeau numérique'),
        backgroundColor: Colors.cyan.shade700,
        foregroundColor: Colors.white,
        actions: [IconButton(icon: const Icon(Icons.refresh), onPressed: _loadData)],
      ),
      drawer: const AppDrawer(),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _buildError()
              : RefreshIndicator(
                  onRefresh: _loadData,
                  child: _snapshot == null
                      ? _buildEmpty()
                      : ListView(
                          padding: const EdgeInsets.all(16),
                          children: [
                            GlassCard(
                              padding: const EdgeInsets.all(20),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Row(
                                    children: [
                                      Icon(Icons.account_tree, color: Colors.cyan.shade300, size: 28),
                                      const SizedBox(width: 12),
                                      const Text('Snapshot du jumeau numérique',
                                          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18)),
                                    ],
                                  ),
                                  const SizedBox(height: 20),
                                  ...(_snapshot!.entries.map((e) => _buildEntry(e.key, e.value))),
                                ],
                              ),
                            ),
                          ],
                        ),
                ),
    );
  }

  Widget _buildEntry(String key, dynamic value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: GlassCard(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(_labelFor(key),
                style: TextStyle(color: Colors.white.withValues(alpha: 0.5), fontSize: 12, fontWeight: FontWeight.w600)),
            const SizedBox(height: 4),
            _buildValue(value),
          ],
        ),
      ),
    );
  }

  Widget _buildValue(dynamic value) {
    if (value is Map) {
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: (value as Map<String, dynamic>).entries.map((e) => Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Row(
                children: [
                  Text('${e.key}: ', style: TextStyle(color: Colors.white.withValues(alpha: 0.6), fontSize: 13)),
                  Expanded(child: Text('${e.value}', style: const TextStyle(color: Colors.white, fontSize: 13))),
                ],
              ),
            )).toList(),
      );
    }
    return Text('$value', style: const TextStyle(color: Colors.white, fontSize: 14));
  }

  String _labelFor(String key) {
    switch (key.toLowerCase()) {
      case 'member':
      case 'membre':
        return 'Membre';
      case 'stats':
      case 'statistiques':
        return 'Statistiques';
      case 'health':
      case 'sante':
        return 'Santé spirituelle';
      case 'progression':
        return 'Progression';
      case 'engagement':
        return 'Engagement';
      default:
        return key;
    }
  }

  Widget _buildEmpty() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(Icons.account_tree, size: 48, color: Colors.white.withValues(alpha: 0.2)),
          const SizedBox(height: 12),
          Text('Aucune donnée disponible', style: TextStyle(color: Colors.white.withValues(alpha: 0.5))),
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
