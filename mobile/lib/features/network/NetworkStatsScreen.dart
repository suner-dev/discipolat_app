import 'package:flutter/material.dart';
import '../../data/services/api_service.dart';
import '../../../presentation/widgets/glass_theme.dart';
import '../../../presentation/widgets/app_drawer.dart';

/// Statistiques du réseau — branché sur GET /api/v1/network/stats.
class NetworkStatsScreen extends StatefulWidget {
  const NetworkStatsScreen({super.key});

  @override
  State<NetworkStatsScreen> createState() => _NetworkStatsScreenState();
}

class _NetworkStatsScreenState extends State<NetworkStatsScreen> {
  final _apiService = ApiService();
  Map<String, dynamic>? _stats;
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
      final res = await _apiService.get('/network/stats');
      if (mounted) {
        setState(() {
          _stats = res.data is Map<String, dynamic> ? res.data as Map<String, dynamic> : null;
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
        title: const Text('Statistiques réseau'),
        backgroundColor: Colors.teal,
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
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      GlassCard(
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Icon(Icons.hub, color: Colors.teal, size: 24),
                                const SizedBox(width: 8),
                                const Text('Réseau inter-églises',
                                    style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 18)),
                              ],
                            ),
                            const SizedBox(height: 20),
                            if (_stats != null)
                              ...(_stats!.entries.map((e) => Padding(
                                    padding: const EdgeInsets.only(bottom: 12),
                                    child: GlassCard(
                                      padding: const EdgeInsets.all(14),
                                      child: Row(
                                        children: [
                                          _iconForStat(e.key),
                                          const SizedBox(width: 12),
                                          Expanded(
                                            child: Text(_labelForStat(e.key),
                                                style: TextStyle(color: Colors.white.withValues(alpha: 0.7), fontSize: 14)),
                                          ),
                                          Text('${e.value}',
                                              style: const TextStyle(color: Colors.teal, fontSize: 18, fontWeight: FontWeight.bold)),
                                        ],
                                      ),
                                    ),
                                  ))),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
    );
  }

  Widget _iconForStat(String key) {
    switch (key.toLowerCase()) {
      case 'churches':
      case 'eglises':
        return Icon(Icons.church, color: Colors.teal, size: 20);
      case 'members':
      case 'membres':
        return Icon(Icons.people, color: Colors.blue, size: 20);
      case 'connections':
      case 'connexions':
        return Icon(Icons.cable, color: Colors.green, size: 20);
      default:
        return Icon(Icons.analytics, color: Colors.amber, size: 20);
    }
  }

  String _labelForStat(String key) {
    switch (key.toLowerCase()) {
      case 'churches':
        return 'Églises';
      case 'eglises':
        return 'Églises';
      case 'members':
        return 'Membres';
      case 'membres':
        return 'Membres';
      case 'connections':
        return 'Connexions';
      case 'connexions':
        return 'Connexions';
      default:
        return key;
    }
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
